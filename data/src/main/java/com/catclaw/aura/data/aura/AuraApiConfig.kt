package com.catclaw.aura.data.aura

import com.catclaw.aura.data.BuildConfig

data class AuraApiConfig(
    val baseUrl: String,
    val apiToken: String,
    val apiSecret: String,
    val appPackage: String,
    val appVersion: String,
) {
    val isConfigured: Boolean =
        baseUrl.isNotBlank() && apiToken.isNotBlank() && apiSecret.isNotBlank()

    companion object {
        fun fromBuildConfig(): AuraApiConfig = AuraApiConfig(
            baseUrl = BuildConfig.AURA_API_BASE_URL.trimEnd('/'),
            apiToken = BuildConfig.AURA_API_TOKEN,
            apiSecret = BuildConfig.AURA_API_SECRET,
            appPackage = BuildConfig.AURA_APP_PACKAGE,
            appVersion = BuildConfig.AURA_APP_VERSION,
        )
    }
}
