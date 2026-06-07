package com.catclaw.aura.data.network.interceptor

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Logs request start and response callback with tag [TAG] for Logcat filtering.
 */
class KekeRequestLoggingInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val method = request.method
        val url = request.url
        val startMs = System.currentTimeMillis()
        Log.i(TAG, "→ $method $url")

        return try {
            val response = chain.proceed(request)
            val elapsedMs = System.currentTimeMillis() - startMs
            if (response.isSuccessful) {
                Log.i(TAG, "← $method $url ${response.code} ${elapsedMs}ms")
            } else {
                Log.w(TAG, "← $method $url ${response.code} ${elapsedMs}ms (failed)")
            }
            response
        } catch (e: Exception) {
            val elapsedMs = System.currentTimeMillis() - startMs
            Log.e(TAG, "← $method $url error ${elapsedMs}ms: ${e.javaClass.simpleName}", e)
            throw e
        }
    }

    companion object {
        const val TAG = "keke"
    }
}
