@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.kauri.host

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.uuid.Uuid

internal actual fun appDataDirectory(): String =
    "${getenv("XDG_DATA_HOME")?.toKString() ?: "${checkNotNull(getenv("HOME")).toKString()}/.local/share"}/Kauri"
internal actual fun newTestDirectory(): String = "/tmp/kauri-${Uuid.random()}"

internal actual fun makeAppDirectory(path: String) {
    require(path.startsWith('/'))
    var prefix = ""
    for (part in path.split('/').filter { it.isNotEmpty() }) {
        prefix += "/$part"
        if (mkdir(prefix, 0x1c0u) != 0) {
            memScoped {
                val info = alloc<stat>()
                check(stat(prefix, info.ptr) == 0 && info.st_mode and S_IFMT.convert<UInt>() == S_IFDIR.convert<UInt>()) {
                    "Cannot create application directory"
                }
            }
        }
    }
}

internal actual fun removeAppDirectory(path: String) {
    val directory = opendir(path) ?: error("Cannot open test directory")
    try {
        while (true) {
            val entry = readdir(directory) ?: break
            val name = entry.pointed.d_name.toKString()
            if (name == "." || name == "..") continue
            val child = "$path/$name"
            memScoped {
                val info = alloc<stat>()
                check(lstat(child, info.ptr) == 0)
                if (info.st_mode and S_IFMT.convert<UInt>() == S_IFDIR.convert<UInt>()) removeAppDirectory(child)
                else check(unlink(child) == 0)
            }
        }
    } finally { closedir(directory) }
    check(rmdir(path) == 0)
}

internal actual fun writeAppBytes(path: String, bytes: ByteArray) {
    val file = checkNotNull(fopen(path, "wb")) { "Cannot open application output" }
    try {
        if (bytes.isNotEmpty()) bytes.usePinned {
            check(fwrite(it.addressOf(0), 1u, bytes.size.convert(), file).toLong() == bytes.size.toLong()) {
                "Cannot write application output"
            }
        }
    } finally { check(fclose(file) == 0) { "Cannot close application output" } }
}
