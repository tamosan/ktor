package io.ktor.util

import io.ktor.cio.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.io.*
import kotlinx.io.core.ByteOrder
import kotlinx.io.pool.*
import java.nio.*
import java.nio.ByteBuffer
import java.util.zip.*

private const val GZIP_MAGIC = 0x8b1f
private val headerPadding = ByteArray(7)

private fun Deflater.deflate(outBuffer: ByteBuffer) {
    if (outBuffer.hasRemaining()) {
        val written = deflate(outBuffer.array(), outBuffer.arrayOffset() + outBuffer.position(), outBuffer.remaining())
        outBuffer.position(outBuffer.position() + written)
    }
}

private fun Deflater.setInput(buffer: ByteBuffer) {
    require(buffer.hasArray()) { "buffer need to be array-backed" }
    setInput(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining())
}

private fun Checksum.updateKeepPosition(buffer: ByteBuffer) {
    require(buffer.hasArray()) { "buffer need to be array-backed" }
    update(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining())
}

private fun putGzipHeader(buffer: ByteBuffer) {
    buffer.putShort(GZIP_MAGIC.toShort())
    buffer.put(Deflater.DEFLATED.toByte())
    buffer.put(headerPadding)
}

private fun putGzipTrailer(crc: Checksum, deflater: Deflater, trailing: ByteBuffer) {
    trailing.putInt(crc.value.toInt())
    trailing.putInt(deflater.totalIn)
}

private suspend fun ByteWriteChannel.deflateWhile(deflater: Deflater, buffer: ByteBuffer, predicate: () -> Boolean) {
    while (predicate()) {
        buffer.clear()
        deflater.deflate(buffer)
        buffer.flip()
        writeFully(buffer)
    }
}

fun ByteReadChannel.deflated(
        gzip: Boolean = true,
        pool: ObjectPool<ByteBuffer> = EmptyByteBufferPool
): ByteReadChannel = writer(Unconfined) {
    channel.writeByteOrder = ByteOrder.LITTLE_ENDIAN
    val crc = CRC32()
    val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
    val input = pool.borrow()
    val compressed = pool.borrow()

    if (gzip) {
        channel.write { putGzipHeader(it) }
    }

    while (!isClosedForRead) {
        input.clear()
        if (readAvailable(input) <= 0) continue
        input.flip()

        crc.updateKeepPosition(input)
        deflater.setInput(input)
        channel.deflateWhile(deflater, compressed) { !deflater.needsInput() }
    }

    deflater.finish()
    channel.deflateWhile(deflater, compressed) { !deflater.finished() }

    if (gzip) {
        channel.write { putGzipTrailer(crc, deflater, it) }
    }

    pool.recycle(input)
    pool.recycle(compressed)
}.channel

// TODO: use joinTo
fun ByteWriteChannel.deflated(
        gzip: Boolean = true,
        pool: ObjectPool<ByteBuffer> = EmptyByteBufferPool
): ByteWriteChannel = reader(Unconfined) {
    channel.deflated(gzip, pool).copyAndClose(this@deflated)
}.channel