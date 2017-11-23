package io.ktor.client.request

import io.ktor.client.call.*
import io.ktor.client.response.*
import io.ktor.client.utils.*
import io.ktor.content.*
import io.ktor.http.*
import javax.net.ssl.*


interface BaseHttpRequest {

    val call: HttpClientCall

    val method: HttpMethod

    val url: Url

    val headers: Headers

    val sslContext: SSLContext?

    suspend fun execute(content: OutgoingContent): BaseHttpResponse
}

