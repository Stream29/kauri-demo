@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.kauri.host

import kotlinx.cinterop.*
import platform.windows.*
import kotlin.uuid.Uuid

private fun environment(name: String): String = memScoped {
    val buffer = allocArray<UShortVar>(32768)
    check(GetEnvironmentVariableW(name, buffer, 32768u) in 1u..32767u) { "Missing application data location" }
    buffer.toKStringFromUtf16()
}
internal actual fun appDataDirectory(): String = "${environment("LOCALAPPDATA")}\\Kauri"
internal actual fun newTestDirectory(): String = "${environment("TEMP")}\\kauri-${Uuid.random()}"
internal actual fun makeAppDirectory(path: String) {
    val result = SHCreateDirectoryExW(null, path, null)
    check(result == 0 || result == ERROR_ALREADY_EXISTS.toInt()) { "Cannot create application directory" }
}
internal actual fun removeAppDirectory(path: String): Unit = memScoped {
    val data = alloc<WIN32_FIND_DATAW>()
    val search = FindFirstFileW("$path\\*", data.ptr)
    if (search != INVALID_HANDLE_VALUE) {
        try {
            do {
                val name = data.cFileName.toKStringFromUtf16()
                if (name != "." && name != "..") {
                    val child = "$path\\$name"
                    if (data.dwFileAttributes and FILE_ATTRIBUTE_DIRECTORY.toUInt() != 0u &&
                        data.dwFileAttributes and FILE_ATTRIBUTE_REPARSE_POINT.toUInt() == 0u) removeAppDirectory(child)
                    else if (data.dwFileAttributes and FILE_ATTRIBUTE_DIRECTORY.toUInt() != 0u) {
                        check(RemoveDirectoryW(child) != 0)
                    } else check(DeleteFileW(child) != 0)
                }
            } while (FindNextFileW(search, data.ptr) != 0)
            check(GetLastError() == ERROR_NO_MORE_FILES.toUInt())
        } finally { FindClose(search) }
    }
    check(RemoveDirectoryW(path) != 0) { "Cannot remove test directory" }
}

internal actual fun writeAppBytes(path: String, bytes: ByteArray) = memScoped {
    val file = CreateFileW(path, GENERIC_WRITE.toUInt(), 0u, null, CREATE_ALWAYS.toUInt(), FILE_ATTRIBUTE_NORMAL.toUInt(), null)
    check(file != INVALID_HANDLE_VALUE) { "Cannot open application output" }
    try {
        val written = alloc<UIntVar>()
        if (bytes.isNotEmpty()) bytes.usePinned {
            check(WriteFile(file, it.addressOf(0), bytes.size.toUInt(), written.ptr, null) != 0 &&
                written.value == bytes.size.toUInt()) { "Cannot write application output" }
        }
    } finally { check(CloseHandle(file) != 0) { "Cannot close application output" } }
}
