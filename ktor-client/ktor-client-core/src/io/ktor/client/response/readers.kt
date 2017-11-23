package io.ktor.client.response

import io.ktor.cio.*
import io.ktor.client.call.*
import io.ktor.client.utils.*
import java.nio.charset.*


suspend fun BaseHttpResponse.readText(): String = call.receive()

suspend fun BaseHttpResponse.readText(charset: Charset): String = readBytes().toString(charset)

suspend fun BaseHttpResponse.readBytes(count: Int): ByteArray {
    val result = ByteArray(count)
    receiveContent().readChannel().readFully(result)
    return result
}

suspend fun BaseHttpResponse.readBytes(): ByteArray =
        receiveContent().readChannel().toByteArray()

suspend fun BaseHttpResponse.discardRemaining() = HttpClientDefaultPool.use { buffer ->
    receiveContent().readChannel().pass(buffer) {
        it.clear()
    }
}
