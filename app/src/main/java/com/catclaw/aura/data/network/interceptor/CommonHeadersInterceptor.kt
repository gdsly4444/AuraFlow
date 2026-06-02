package com.catclaw.aura.data.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds [headers] to every outgoing request. Per-request headers from [NetworkClient] can override these.
 */
class CommonHeadersInterceptor(
    private val headers: Map<String, String>,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()
        headers.forEach { (name, value) ->
            requestBuilder.header(name, value)
        }
        return chain.proceed(requestBuilder.build())
    }
}
