@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.kauri.host

import dev.kauri.api.SystemInfo
import kotlinx.cinterop.*
import platform.posix.*

actual fun platformSystemInfo(): SystemInfo = memScoped {
    val system = alloc<utsname>()
    check(uname(system.ptr) == 0)
    SystemInfo(
        user = getenv("USER")?.toKString() ?: getuid().toString(),
        operatingSystem = "${system.sysname.toKString()} ${system.release.toKString()}",
        architecture = system.machine.toKString(),
        processors = sysconf(_SC_NPROCESSORS_ONLN).toInt(),
        memoryBytes = sysconf(_SC_PHYS_PAGES).toLong() * sysconf(_SC_PAGESIZE).toLong(),
        processId = getpid(),
    )
}

actual fun platformUptime(): Long = memScoped {
    val time = alloc<timespec>()
    check(clock_gettime(CLOCK_MONOTONIC, time.ptr) == 0)
    time.tv_sec
}

actual fun platformReadText(path: String): String? {
    val file = fopen(path, "rb") ?: run {
        check(errno == ENOENT) { "Cannot open the app note (errno=$errno)" }
        return null
    }
    try {
        check(fseek(file, 0, SEEK_END) == 0)
        val length = ftell(file).toInt()
        check(length in 0..131_072) { "Invalid app note length" }
        rewind(file)
        if (length == 0) return ""
        val bytes = ByteArray(length)
        bytes.usePinned { check(fread(it.addressOf(0), 1.convert(), length.convert(), file).toInt() == length) }
        return bytes.decodeToString(throwOnInvalidSequence = true)
    } finally { fclose(file) }
}

private fun createDataDirectory(directory: String) {
    require(directory.startsWith('/'))
    var path = ""
    directory.split('/').filter { it.isNotEmpty() }.forEach {
        require(it != "." && it != "..")
        path += "/$it"
        check(mkdir(path, 448.convert()) == 0 || errno == EEXIST) { "Cannot create app directory" }
    }
}

actual fun platformWriteText(directory: String, path: String, text: String) {
    createDataDirectory(directory)
    val temporary = "$path.${kotlin.uuid.Uuid.random()}.tmp"
    val file = checkNotNull(fopen(temporary, "wb")) { "Cannot create app note" }
    var closed = false
    try {
        check(chmod(temporary, 384.convert()) == 0)
        val bytes = text.encodeToByteArray()
        if (bytes.isNotEmpty()) {
            bytes.usePinned {
                check(fwrite(it.addressOf(0), 1.convert(), bytes.size.convert(), file).toInt() == bytes.size)
            }
        }
        check(fflush(file) == 0)
        check(fsync(fileno(file)) == 0)
        val result = fclose(file)
        closed = true
        check(result == 0)
        check(rename(temporary, path) == 0) { "Cannot atomically replace the app note" }
    } finally {
        if (!closed) fclose(file)
        unlink(temporary)
    }
}
