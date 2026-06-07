package com.catclaw.aura.data.network.config

/**
 * 逻辑 Base URL 常量，与 [com.catclaw.aura.AuraApplication] 里 [com.catclaw.aura.data.network.config.NetworkConfig.baseUrls] 的 key 对应。
 *
 * 测试时可设为 JSONPlaceholder：`https://jsonplaceholder.typicode.com/`
 */
object NetworkConstants {

    /** Default host for app APIs. Override URL in [AuraApplication]. */
    const val BASE_URL_MAIN = "main"

    /** Alibaba Cloud DashScope OpenAI-compatible endpoint (百炼). */
    const val BASE_URL_DASHSCOPE = "dashscope"

    /** Mapbox HTTP APIs (Geocoding, etc.). Trailing slash required. */
    const val BASE_URL_MAPBOX_API = "mapbox_api"

    /** Example secondary host (Mapbox REST, analytics, etc.). */
    const val BASE_URL_SECONDARY = "secondary"

    /** Aura App Server (configured via local.properties → BuildConfig). */
    const val BASE_URL_AURA = "aura"
}
