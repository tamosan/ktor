package io.ktor.cio

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.io.*
import kotlinx.io.pool.*
import java.io.*
import java.nio.ByteBuffer

class InputStreamFromReadChannel(val channel: ReadChannel, val bufferPool: ByteBufferPool = NoPool) : InputStream() {
    private val singleByte = bufferPool.allocate(1)
    override fun read(): Int = runBlocking(Unconfined) {
        singleByte.buffer.clear()

        while (true) {
            val count = channel.read(singleByte.buffer)
            if (count == -1)
                return@runBlocking -1
            else if (count == 1)
                break
        }

        singleByte.buffer.flip()
        singleByte.buffer.get().toInt() and 0xff
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int = runBlocking(Unconfined) {
        val bb = ByteBuffer.wrap(b, off, len)
        channel.read(bb)
    }

    override fun close() {
        super.close()
        channel.close()
        bufferPool.release(singleByte)
    }
}

private class ReadChannelFromInputStream(val input: InputStream) : ReadChannel {
    override suspend fun read(dst: ByteBuffer): Int {
        val count = input.read(dst.array(), dst.arrayOffset() + dst.position(), dst.remaining())
        if (count > 0) {
            dst.position(dst.position() + count)
        }
        return count
    }

    override fun close() {
        input.close()
    }
}

private class ByteReadChannelInputStream(private val channel: ByteReadChannel) : InputStream() {
    override fun read(): Int = runBlocking(Unconfined) {
        try {
            channel.readByte().toInt() and 0xff
        } finally {
            -1
        }
    }

    override fun read(array: ByteArray, offset: Int, length: Int): Int = runBlocking(Unconfined) {
        channel.readAvailable(array, offset, length)
    }
}

fun ByteReadChannel.toInputStream(): InputStream = ByteReadChannelInputStream(this)

fun InputStream.toByteReadChannel(pool: ObjectPool<ByteBuffer> = EmptyByteBufferPool): ByteReadChannel = writer(Unconfined) {
    val buffer = pool.borrow()
    while (true) {
        buffer.clear()
        val readCount = read(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining())
        if (readCount < 0) break
        if (readCount == 0) continue

        buffer.position(buffer.position() + readCount)
        buffer.flip()
        channel.writeFully(buffer)
    }

    pool.recycle(buffer)
    close()
}.channel

fun ReadChannel.toInputStream(): InputStream = InputStreamFromReadChannel(this)
fun InputStream.toReadChannel(): ReadChannel = ReadChannelFromInputStream(this)
