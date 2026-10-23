@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.kauri.host

import dev.kauri.api.SystemInfo
import platform.Foundation.NSFileManager
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserName
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

actual fun platformSystemInfo(): SystemInfo {
    val info = NSProcessInfo.processInfo
    return SystemInfo(
        user = NSUserName(),
        operatingSystem = info.operatingSystemVersionString,
        architecture = "arm64",
        processors = info.processorCount.toInt(),
        memoryBytes = info.physicalMemory.toLong(),
        processId = info.processIdentifier,
    )
}

actual fun platformReadText(path: String): String? =
    if (NSFileManager.defaultManager.fileExistsAtPath(path)) {
        checkNotNull(NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)) {
            "Cannot read the app note"
        }
    } else null

actual fun platformWriteText(directory: String, path: String, text: String) {
    check(NSFileManager.defaultManager.createDirectoryAtPath(directory, true, null, null)) {
        "Cannot create the app data directory"
    }
    check(NSString.create(string = text).writeToFile(path, true, NSUTF8StringEncoding, null)) {
        "Cannot save the app note"
    }
}

actual fun platformUptime(): Long = NSProcessInfo.processInfo.systemUptime.toLong()
