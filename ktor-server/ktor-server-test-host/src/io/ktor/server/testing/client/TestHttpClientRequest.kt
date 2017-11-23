package io.ktor.server.testing.client

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.response.*
import io.ktor.client.utils.*
import io.ktor.content.*
import io.ktor.http.*
import javax.net.ssl.*

class TestHttpClientRequest(
        override val call: HttpClientCall,
        private val engine: TestHttpClientEngine,
        builder: HttpRequestBuilder
) : BaseHttpRequest {

    override val method: HttpMethod = builder.method

    override val url: Url = builder.url.build()

    override val headers: Headers = builder.headers.build()

    override val sslContext: SSLContext? = builder.sslContext

    suspend override fun execute(content: OutgoingContent): BaseHttpResponse {
        val response = engine.runRequest(method, url.fullPath, headers, content).response
        return TestHttpClientResponse(call, response.status()!!, response.headers.allValues(), response.byteContent!!)
    }
}
