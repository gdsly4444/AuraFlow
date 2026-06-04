package com.catclaw.aura.data.network.config

import okhttp3.Interceptor

/**
 * Global network configuration: multiple base URLs, timeouts, shared headers, logging.
 *
 * @param baseUrls Map of logical key → root URL (must end with `/`), e.g. `"main" to "https://api.example.com/"`.
 * @param commonHeaders Applied to every request via [com.catclaw.aura.data.network.interceptor.CommonHeadersInterceptor].
 * @param customInterceptors Added to [okhttp3.OkHttpClient] after common headers, before logging.
 * @param enableLogging When true, adds OkHttp body logging (typically tied to debug builds).
 */
data class NetworkConfig(
    val baseUrls: Map<String, String>,
    val connectTimeoutSeconds: Long = 30L,
    val readTimeoutSeconds: Long = 30L,
    val writeTimeoutSeconds: Long = 30L,
    val commonHeaders: Map<String, String> = emptyMap(),
    val customInterceptors: List<Interceptor> = emptyList(),
    val enableLogging: Boolean = true,
) {
    init {
        require(baseUrls.isNotEmpty()) { "At least one base URL must be registered." }
        baseUrls.forEach { (key, url) ->
            require(key.isNotBlank()) { "Base URL key must not be blank." }
            require(url.startsWith("http://") || url.startsWith("https://")) {
                "Base URL for '$key' must start with http:// or https://"
            }
        }
    }
}
