package dev.kauri.api

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.annotations.Rpc
import kotlinx.serialization.Serializable

@Rpc
interface DesktopService {
    suspend fun systemInfo(): SystemInfo
    suspend fun readNote(): Note
    suspend fun saveNote(text: String): Note
    fun pulses(): Flow<Pulse>
}

@Serializable
data class SystemInfo(
    val user: String,
    val operatingSystem: String,
    val architecture: String,
    val processors: Int,
    val memoryBytes: Long,
    val processId: Int,
    val runtime: String = "Kotlin/Native",
)

@Serializable
data class Note(val text: String, val path: String)

@Serializable
data class Pulse(val sequence: Int, val uptimeSeconds: Long)

const val MAX_NOTE_LENGTH = 16_384

fun validateNote(text: String): String {
    require(text.length <= MAX_NOTE_LENGTH) { "Note exceeds $MAX_NOTE_LENGTH characters" }
    return text
}
