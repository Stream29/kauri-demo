@file:OptIn(kotlinx.io.InternalIoApi::class)

package dev.kauri.host

import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.io.*
import kotlin.test.*

private class CountingSource : RawSource {
    val length = 8L * 1024 * 1024
    var consumed = 0L
    var closed = false
    private val chunk = ByteArray(8192) { (it % 251).toByte() }

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        check(!closed)
        require(byteCount >= 0)
        if (byteCount == 0L) return 0
        if (consumed == length) return -1
        val count = minOf(byteCount, length - consumed, chunk.size.toLong()).toInt()
        sink.write(chunk, 0, count)
        consumed += count
        return count.toLong()
    }

    override fun close() { closed = true }
}

class StreamingResponseTest {
    @Test fun stalledAndCancelledClientDoesNotReadWholeResource() = runBlocking {
        val source = CountingSource()
        val channel = ByteChannel()
        val writer = async(start = CoroutineStart.UNDISPATCHED) {
            runCatching { source.use { channel.writeBuffer(it) } }.exceptionOrNull()
        }
        try {
            withTimeout(5_000) {
                yield()
                assertFalse(writer.isCompleted)
                assertTrue(source.consumed in 1L until source.length)
                assertTrue(source.consumed <= 2L * 1024 * 1024)
                val paused = source.consumed
                println("STREAMING_BACKPRESSURE consumed=$paused of ${source.length}")
                delay(20)
                assertEquals(paused, source.consumed)
                channel.cancel()
                assertNotNull(writer.await())
                assertTrue(source.closed)
                assertEquals(paused, source.consumed)
            }
        } finally {
            channel.cancel()
            writer.cancelAndJoin()
        }
    }

    @Test fun normalEofStreamsAndClosesWithoutAccumulatingWholeBody() = runBlocking {
        val source = CountingSource()
        val channel = ByteChannel()
        val writer = launch {
            source.use { channel.writeBuffer(it) }
            channel.flushAndClose()
        }
        try {
            withTimeout(5_000) {
                val bytes = ByteArray(1024)
                var received = 0L
                while (true) {
                    val count = channel.readAvailable(bytes)
                    if (count == -1) break
                    var valid = true
                    repeat(count) { if (bytes[it] != ((received + it) % 8192 % 251).toByte()) valid = false }
                    assertTrue(valid)
                    received += count
                }
                writer.join()
                assertEquals(source.length, received)
                assertTrue(source.closed)
            }
        } finally {
            channel.cancel()
            writer.cancelAndJoin()
        }
    }
}
