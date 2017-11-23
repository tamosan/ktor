package io.ktor.http

import io.ktor.http.*

interface HttpMessage {
    val headers: Headers
}

interface HttpMessageBuilder {
    val headers: HeadersBuilder
}

