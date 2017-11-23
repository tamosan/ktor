package io.ktor.client.request

import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.util.*
import javax.net.ssl.*


class HttpRequestBuilder : HttpMessageBuilder {
    val url = UrlBuilder()
    var method = HttpMethod.Get
    override val headers = HeadersBuilder(caseInsensitiveKey = true)
    var body: Any = EmptyContent
    var sslContext: SSLContext? = null

    fun headers(block: HeadersBuilder.() -> Unit) = headers.apply(block)

    fun url(block: UrlBuilder.() -> Unit) = url.block()
}

fun HttpRequestBuilder.takeFrom(builder: HttpRequestBuilder): HttpRequestBuilder {
    method = builder.method
    body = builder.body
    sslContext = builder.sslContext
    url.takeFrom(builder.url)
    headers.appendAll(builder.headers)

    return this
}
