@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package dev.kauri.host

import kotlinx.coroutines.runBlocking
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.assertEquals

class NativeServiceTest {
    @Test
    fun notePersistsAcrossServiceInstances() = runBlocking {
        val directory = "${NSTemporaryDirectory()}kauri-test-${NSUUID().UUIDString}"
        try {
            val service = NativeService(directory)
            assertEquals("", service.readNote().text)
            service.saveNote("Native ↔ Wasm 🌿")
            assertEquals("Native ↔ Wasm 🌿", NativeService(directory).readNote().text)
            assertEquals("Kotlin/Native", service.systemInfo().runtime)
        } finally {
            NSFileManager.defaultManager.removeItemAtPath(directory, null)
        }
    }

}
