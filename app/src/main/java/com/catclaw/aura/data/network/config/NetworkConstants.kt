package com.catclaw.aura.data.network.config

/**
 * 逻辑 Base URL 常量，与 [com.catclaw.aura.AuraApplication] 里 [com.catclaw.aura.data.network.config.NetworkConfig.baseUrls] 的 key 对应。
 *
 * 测试时可设为 JSONPlaceholder：`https://jsonplaceholder.typicode.com/`
 */
object NetworkConstants {

    /** Default host for app APIs. Override URL in [AuraApplication]. */
    const val BASE_URL_MAIN = "main"

    /** Example secondary host (Mapbox REST, analytics, etc.). */
    const val BASE_URL_SECONDARY = "secondary"
}
