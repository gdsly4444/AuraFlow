package com.catclaw.aura.data.aura.auth

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object AuraSignature {

    fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }

    fun buildSignPayload(
        method: String,
        path: String,
        timestamp: String,
        nonce: String,
        body: ByteArray,
    ): String {
        val bodyHash = sha256Hex(body)
        return listOf(method.uppercase(), path, timestamp, nonce, bodyHash).joinToString("\n")
    }

    fun computeSignature(secret: String, payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
