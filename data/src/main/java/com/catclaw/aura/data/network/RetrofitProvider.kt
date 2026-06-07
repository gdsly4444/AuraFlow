package com.catclaw.aura.data.network

import com.catclaw.aura.data.network.api.DynamicApiService
import com.catclaw.aura.data.network.config.NetworkConfig
import com.catclaw.aura.data.network.interceptor.CommonHeadersInterceptor
import com.catclaw.aura.data.network.interceptor.KekeRequestLoggingInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * Builds a shared [OkHttpClient] and [Retrofit] instance from [NetworkConfig].
 * Multiple logical base URLs are resolved to absolute URLs in [NetworkClient], not here.
 */
internal object RetrofitProvider {

    private const val PLACEHOLDER_BASE_URL = "https://network-placeholder.invalid/"

    fun create(config: NetworkConfig): Pair<OkHttpClient, DynamicApiService> {
        val okHttpClient = buildOkHttpClient(config)
        val retrofit = Retrofit.Builder()
            .baseUrl(PLACEHOLDER_BASE_URL)
            .client(okHttpClient)
            .build()
        return okHttpClient to retrofit.create(DynamicApiService::class.java)
    }

    private fun buildOkHttpClient(config: NetworkConfig): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(config.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(config.writeTimeoutSeconds, TimeUnit.SECONDS)
            .apply {
                addInterceptor(KekeRequestLoggingInterceptor())
                if (config.commonHeaders.isNotEmpty()) {
                    addInterceptor(CommonHeadersInterceptor(config.commonHeaders))
                }
                config.customInterceptors.forEach { addInterceptor(it) }
                if (config.enableLogging) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        },
                    )
                }
            }
            .build()
    }
}
