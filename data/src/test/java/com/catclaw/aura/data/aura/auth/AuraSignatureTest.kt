package com.catclaw.aura.data.aura.auth

import org.junit.Assert.assertEquals
import org.junit.Test

class AuraSignatureTest {

    @Test
    fun signerRoundtrip_matchesServerTestVector() {
        val body = """{"hello":"world"}""".toByteArray(Charsets.UTF_8)
        val payload = AuraSignature.buildSignPayload(
            method = "POST",
            path = "/api/v1/records",
            timestamp = "1717564800",
            nonce = "nonce-1",
            body = body,
        )
        val signature = AuraSignature.computeSignature("secret", payload)
        assertEquals(64, signature.length)
        assertEquals(
            "POST\n/api/v1/records\n1717564800\nnonce-1\n" +
                AuraSignature.sha256Hex(body),
            payload,
        )
    }

    @Test
    fun multipartUpload_usesEmptyBodyHash() {
        val payload = AuraSignature.buildSignPayload(
            method = "POST",
            path = "/api/v1/media/audio",
            timestamp = "1717564800",
            nonce = "nonce-2",
            body = ByteArray(0),
        )
        assertEquals(
            "POST\n/api/v1/media/audio\n1717564800\nnonce-2\n" +
                AuraSignature.sha256Hex(ByteArray(0)),
            payload,
        )
    }
}
