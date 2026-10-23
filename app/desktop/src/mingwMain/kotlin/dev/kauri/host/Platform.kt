@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.kauri.host

import dev.kauri.api.SystemInfo
import kotlinx.cinterop.*
import platform.windows.*

actual fun platformSystemInfo(): SystemInfo = memScoped {
    val nameBuffer = allocArray<UShortVar>(256)
    val nameSize = alloc<UIntVar>()
    nameSize.value = 256u
    val user = if (GetUserNameW(nameBuffer, nameSize.ptr) != 0) {
        nameBuffer.toKStringFromUtf16()
    } else {
        val envBuf = allocArray<UShortVar>(256)
        val len = GetEnvironmentVariableW("USERNAME", envBuf, 256u)
        if (len > 0u) envBuf.toKStringFromUtf16() else "unknown"
    }

    val sysInfo = alloc<SYSTEM_INFO>()
    GetSystemInfo(sysInfo.ptr)

    val arch = when (sysInfo.wProcessorArchitecture.toInt()) {
        9 -> "x86_64"
        12 -> "aarch64"
        0 -> "x86"
        5 -> "arm"
        else -> "unknown"
    }

    val processors = sysInfo.dwNumberOfProcessors.toInt()

    val memStatus = alloc<MEMORYSTATUSEX>()
    memStatus.dwLength = sizeOf<MEMORYSTATUSEX>().toUInt()
    val memoryBytes = if (GlobalMemoryStatusEx(memStatus.ptr) != 0) {
        memStatus.ullTotalPhys.toLong()
    } else {
        0L
    }

    val pid = GetCurrentProcessId().toInt()

    SystemInfo(
        user = user,
        operatingSystem = "Windows",
        architecture = arch,
        processors = processors,
        memoryBytes = memoryBytes,
        processId = pid,
        runtime = "Kotlin/Native"
    )
}

actual fun platformUptime(): Long = (GetTickCount64() / 1000uL).toLong()

actual fun platformReadText(path: String): String? {
    val normalizedPath = path.replace('/', '\\')
    val hFile = CreateFileW(
        normalizedPath,
        GENERIC_READ.toUInt(),
        (FILE_SHARE_READ.toUInt() or FILE_SHARE_WRITE.toUInt()),
        null,
        OPEN_EXISTING.toUInt(),
        FILE_ATTRIBUTE_NORMAL.toUInt(),
        null
    )
    if (hFile == null || hFile == INVALID_HANDLE_VALUE) {
        val error = GetLastError()
        check(error == ERROR_FILE_NOT_FOUND.toUInt() || error == ERROR_PATH_NOT_FOUND.toUInt()) {
            "Cannot read the app note (error=$error)"
        }
        return null
    }
    return try {
        memScoped {
            val maxBytes = 128 * 1024
            val size = alloc<LARGE_INTEGER>()
            check(GetFileSizeEx(hFile, size.ptr) != 0 && size.QuadPart in 0..maxBytes.toLong()) {
                "Invalid app note length"
            }
            val buffer = allocArray<ByteVar>(maxBytes)
            val bytesRead = alloc<UIntVar>()
            val success = ReadFile(
                hFile,
                buffer,
                maxBytes.toUInt(),
                bytesRead.ptr,
                null
            )
            check(success != 0 && bytesRead.value.toLong() == size.QuadPart) { "Cannot read the app note" }
            buffer.readBytes(bytesRead.value.toInt()).decodeToString(throwOnInvalidSequence = true)
        }
    } finally {
        CloseHandle(hFile)
    }
}

private fun createDataDirectory(directory: String) {
    var current = ""
    for (part in directory.replace('/', '\\').split('\\')) {
        if (part.isEmpty() || part == ".") continue
        current = if (current.isEmpty()) part else "$current\\$part"
        if (current.endsWith(":")) continue
        check(CreateDirectoryW(current, null) != 0 || GetLastError() == ERROR_ALREADY_EXISTS.toUInt()) {
            "Cannot create application directory"
        }
    }
}

actual fun platformWriteText(directory: String, path: String, text: String) {
    createDataDirectory(directory)
    val normalizedPath = path.replace('/', '\\')
    val normalizedDir = directory.replace('/', '\\')
    val tempPath = "$normalizedDir\\note-${kotlin.uuid.Uuid.random()}.tmp"
    val bytes = text.encodeToByteArray()

    val hFile = CreateFileW(
        tempPath,
        GENERIC_WRITE.toUInt(),
        0u,
        null,
        CREATE_NEW.toUInt(),
        FILE_ATTRIBUTE_NORMAL.toUInt(),
        null
    )
    if (hFile == null || hFile == INVALID_HANDLE_VALUE) {
        error("Failed to create temporary file: $tempPath")
    }
    var complete = false
    try {
        if (bytes.isNotEmpty()) {
            memScoped {
                bytes.usePinned { pinned ->
                    val bytesWritten = alloc<UIntVar>()
                    val success = WriteFile(
                        hFile,
                        pinned.addressOf(0),
                        bytes.size.toUInt(),
                        bytesWritten.ptr,
                        null
                    )
                    if (success == 0 || bytesWritten.value != bytes.size.toUInt()) {
                        error("Failed to write temporary file: $tempPath")
                    }
                }
            }
        }
        check(FlushFileBuffers(hFile) != 0) { "Cannot flush the app note" }
        complete = true
    } finally {
        CloseHandle(hFile)
        if (!complete) DeleteFileW(tempPath)
    }

    val replaced = ReplaceFileW(
        normalizedPath,
        tempPath,
        null,
        0u,
        null,
        null
    )
    if (replaced == 0) {
        val moved = MoveFileExW(
            tempPath,
            normalizedPath,
            (MOVEFILE_REPLACE_EXISTING.toUInt() or MOVEFILE_WRITE_THROUGH.toUInt())
        )
        if (moved == 0) {
            DeleteFileW(tempPath)
            error("Failed to replace file $normalizedPath with $tempPath")
        }
    }
}
