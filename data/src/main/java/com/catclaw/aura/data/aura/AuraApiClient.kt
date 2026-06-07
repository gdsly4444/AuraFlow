package com.catclaw.aura.data.aura

import com.catclaw.aura.data.BuildConfig
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object AuraApiClient {

    fun create(config: AuraApiConfig): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
        // Aura API uses [KekeHttpCall] for request/response logs; avoid duplicate interceptors.
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                },
            )
        }
        return builder.build()
    }
}
