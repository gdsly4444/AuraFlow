package com.catclaw.aura

import android.app.Application
import com.catclaw.aura.data.network.NetworkClient
import com.catclaw.aura.data.network.config.NetworkConfig
import com.catclaw.aura.data.network.config.NetworkConstants

class AuraApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NetworkClient.init(
            NetworkConfig(
                baseUrls = mapOf(
                    NetworkConstants.BASE_URL_MAIN to "https://jsonplaceholder.typicode.com/",
                    NetworkConstants.BASE_URL_SECONDARY to "https://jsonplaceholder.typicode.com/",
                ),
                commonHeaders = mapOf(
                    "Accept" to "application/json",
                ),
                enableLogging = BuildConfig.DEBUG,
            ),
        )
    }
}
