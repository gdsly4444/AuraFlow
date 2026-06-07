package com.catclaw.aura.data.aura.auth

import com.catclaw.aura.data.aura.AuraApiConfig
import java.util.UUID

object AuraAuthHeaders {

    fun signed(
        config: AuraApiConfig,
        method: String,
        path: String,
        body: ByteArray = ByteArray(0),
    ): Map<String, String> {
        val timestamp = (System.currentTimeMillis() / 1000L).toString()
        val nonce = UUID.randomUUID().toString()
        val payload = AuraSignature.buildSignPayload(method, path, timestamp, nonce, body)
        val signature = AuraSignature.computeSignature(config.apiSecret, payload)
        return mapOf(
            "Authorization" to "Bearer ${config.apiToken}",
            "X-App-Package" to config.appPackage,
            "X-App-Version" to config.appVersion,
            "X-Timestamp" to timestamp,
            "X-Nonce" to nonce,
            "X-Signature" to signature,
        )
    }
}
