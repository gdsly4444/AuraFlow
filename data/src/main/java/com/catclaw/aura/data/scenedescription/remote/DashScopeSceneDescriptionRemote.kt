package com.catclaw.aura.data.scenedescription.remote

import android.util.Log
import com.catclaw.aura.data.network.NetworkClient
import com.catclaw.aura.data.network.config.NetworkConstants
import com.catclaw.aura.domain.model.SceneDescription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class DashScopeSceneDescriptionRemote {

    suspend fun generate(jsonBody: String, apiKey: String): SceneDescription =
        withContext(Dispatchers.IO) {
            try {
                val url = NetworkClient.resolveUrl(
                    NetworkConstants.BASE_URL_DASHSCOPE,
                    "chat/completions",
                )
                val request = Request.Builder()
                    .url(url)
                    .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                    .header("Authorization", "Bearer $apiKey")
                    .header("Accept", "text/event-stream")
                    .build()
                val client = NetworkClient.okHttpClient().newBuilder()
                    .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
                    .build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val message = parseApiError(body, response.code)
                        Log.w(TAG, "DashScope HTTP ${response.code}: $message")
                        return@withContext SceneDescription(errorMessage = message)
                    }
                    val result = parseStreamOrJson(body)
                    if (!result.isSuccess) {
                        Log.w(TAG, "DashScope parse: ${result.errorMessage}")
                    } else {
                        Log.i(TAG, "DashScope ok, chars=${result.text?.length ?: 0}")
                    }
                    result
                }
            } catch (e: Exception) {
                val message = parseApiError(e.message.orEmpty(), null)
                    .ifBlank { e.message ?: "网络请求失败" }
                Log.e(TAG, "DashScope request failed: $message", e)
                SceneDescription(errorMessage = message)
            }
        }

    private fun parseStreamOrJson(body: String): SceneDescription {
        if (body.contains("data:")) {
            return parseSseBody(body)
        }
        return parseSuccessJson(body)
    }

    private fun parseSseBody(body: String): SceneDescription {
        val content = StringBuilder()
        body.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (!trimmed.startsWith("data:")) return@forEach
            val data = trimmed.removePrefix("data:").trim()
            if (data.isEmpty() || data == "[DONE]") return@forEach
            try {
                val chunk = JSONObject(data)
                chunk.optJSONObject("error")?.let { err ->
                    return SceneDescription(
                        errorMessage = err.optString("message", "百炼 API 错误"),
                    )
                }
                val choices = chunk.optJSONArray("choices") ?: return@forEach
                if (choices.length() == 0) return@forEach
                val choice = choices.getJSONObject(0)
                val delta = choice.optJSONObject("delta")
                val piece = delta?.optString("content")
                if (!piece.isNullOrEmpty()) {
                    content.append(piece)
                }
                val message = choice.optJSONObject("message")
                val full = message?.optString("content")
                if (!full.isNullOrEmpty()) {
                    content.clear()
                    content.append(full)
                }
            } catch (_: Exception) {
                // skip malformed SSE lines
            }
        }
        val text = content.toString().trim()
        return if (text.isNotBlank()) {
            SceneDescription(text = text)
        } else {
            SceneDescription(errorMessage = "流式响应无正文")
        }
    }

    private fun parseSuccessJson(body: String): SceneDescription {
        return try {
            val root = JSONObject(body)
            root.optJSONObject("error")?.let { err ->
                return SceneDescription(
                    errorMessage = err.optString("message", "百炼 API 错误"),
                )
            }
            val choices = root.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return SceneDescription(errorMessage = "响应无 choices")
            }
            val message = choices.getJSONObject(0).optJSONObject("message")
            val content = message?.optString("content")?.trim()
            if (content.isNullOrBlank()) {
                SceneDescription(errorMessage = "模型返回空内容")
            } else {
                SceneDescription(
                    text = content,
                    model = root.optString("model").ifBlank { null },
                )
            }
        } catch (e: Exception) {
            SceneDescription(errorMessage = "解析响应失败: ${e.message}")
        }
    }

    private fun parseApiError(bodyOrMessage: String, httpCode: Int?): String {
        val jsonStart = bodyOrMessage.indexOf('{')
        if (jsonStart >= 0) {
            try {
                val root = JSONObject(bodyOrMessage.substring(jsonStart))
                val err = root.optJSONObject("error")
                val message = err?.optString("message")
                if (!message.isNullOrBlank()) {
                    return message
                }
            } catch (_: Exception) {
                // fall through
            }
        }
        return if (httpCode != null) {
            "HTTP $httpCode: ${bodyOrMessage.take(200)}"
        } else {
            bodyOrMessage.take(200)
        }
    }

    companion object {
        private const val TAG = "SceneDescription"
        private const val READ_TIMEOUT_SEC = 120L
        private const val WRITE_TIMEOUT_SEC = 120L
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
