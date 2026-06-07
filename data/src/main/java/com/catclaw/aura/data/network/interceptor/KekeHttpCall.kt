package com.catclaw.aura.data.network.interceptor

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * Blocking OkHttp execute with keke-tagged logs. Use only on [kotlinx.coroutines.Dispatchers.IO].
 */
object KekeHttpCall {

    const val TAG = "keke"
    private const val BODY_PEEK_BYTES = 2048L
    private const val BODY_LOG_MAX_CHARS = 512

    fun execute(client: OkHttpClient, request: Request): Response {
        val method = request.method
        val url = request.url
        Log.i(TAG, "→ $method $url")
        return try {
            val response = client.newCall(request).execute()
            val bodySnippet = peekBodySnippet(response)
            if (response.isSuccessful) {
                Log.i(TAG, "← ${response.code} $method $url $bodySnippet")
            } else {
                Log.w(TAG, "← ${response.code} $method $url $bodySnippet")
            }
            response
        } catch (e: Exception) {
            Log.e(TAG, "request failed: ${e.javaClass.simpleName} $method $url", e)
            throw e
        }
    }

    private fun peekBodySnippet(response: Response): String =
        runCatching {
            response.peekBody(BODY_PEEK_BYTES).string()
                .replace('\n', ' ')
                .trim()
                .take(BODY_LOG_MAX_CHARS)
                .ifBlank { "(empty)" }
        }.getOrElse { "(binary)" }
}
