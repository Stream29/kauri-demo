package dev.kauri.api

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ContractTest {
    @Test
    fun noteRoundTripPreservesUnicode() {
        val note = Note("Kauri 🌿\n你好，Native", "/app/note.txt")
        assertEquals(note, Json.decodeFromString<Note>(Json.encodeToString(note)))
    }

    @Test
    fun noteSizeIsBounded() {
        assertEquals("", validateNote(""))
        assertEquals(MAX_NOTE_LENGTH, validateNote("a".repeat(MAX_NOTE_LENGTH)).length)
        assertFailsWith<IllegalArgumentException> { validateNote("a".repeat(MAX_NOTE_LENGTH + 1)) }
    }
}
