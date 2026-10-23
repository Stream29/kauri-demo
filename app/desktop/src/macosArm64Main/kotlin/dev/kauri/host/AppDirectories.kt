@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.kauri.host

import kotlinx.cinterop.*
import platform.Foundation.*
import kotlin.uuid.Uuid

internal actual fun appDataDirectory(): String = "${NSHomeDirectory()}/Library/Application Support/Kauri"
internal actual fun newTestDirectory(): String = "${NSTemporaryDirectory()}kauri-${Uuid.random()}"
internal actual fun makeAppDirectory(path: String) {
    check(NSFileManager.defaultManager.createDirectoryAtPath(path, true, null, null)) {
        "Cannot create application directory"
    }
}
internal actual fun removeAppDirectory(path: String) {
    check(NSFileManager.defaultManager.removeItemAtPath(path, null)) { "Cannot remove test directory" }
}
internal actual fun writeAppBytes(path: String, bytes: ByteArray) {
    val data = if (bytes.isEmpty()) NSData() else bytes.usePinned {
        NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong())
    }
    check(data.writeToFile(path, true)) { "Cannot write application output" }
}
