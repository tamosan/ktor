package io.ktor.cio

import kotlinx.coroutines.experimental.io.*

abstract class ByteChannelTransformer(
        val input: ByteReadChannel,
        val output: ByteWriteChannel
) {

    abstract fun transform(data: ByteBuffer)
}