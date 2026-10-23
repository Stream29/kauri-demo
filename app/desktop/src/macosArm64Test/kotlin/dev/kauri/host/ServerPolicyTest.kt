package dev.kauri.host

import kotlin.test.*

class ServerPolicyTest {
    @Test fun rejectsUnsafeHttpResourceNames() {
        assertTrue(safeResourcePath("index.html"))
        assertTrue(safeResourcePath("fonts/regular.ttf"))
        listOf("../secret", ".", "..", "/secret", "a//b", "a\\b", "%2e%2e", "a/../b").forEach {
            assertFalse(safeResourcePath(it), it)
        }
    }
}
