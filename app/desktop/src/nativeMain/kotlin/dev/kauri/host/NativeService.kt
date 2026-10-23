package dev.kauri.host

import dev.kauri.api.DesktopService
import dev.kauri.api.Note
import dev.kauri.api.Pulse
import dev.kauri.api.validateNote
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class NativeService(private val directory: String) : DesktopService {
    private val mutex = Mutex()
    private val path = "$directory/note.txt"

    override suspend fun systemInfo() = platformSystemInfo()

    override suspend fun readNote(): Note = mutex.withLock {
        Note(platformReadText(path).orEmpty(), path)
    }

    override suspend fun saveNote(text: String): Note = mutex.withLock {
        validateNote(text)
        platformWriteText(directory, path, text)
        Note(text, path)
    }

    override fun pulses() = flow {
        var sequence = 0
        while (true) {
            emit(Pulse(++sequence, platformUptime()))
            delay(1_000)
        }
    }
}
