package com.catclaw.aura.data.network

import android.os.Handler
import android.os.Looper
import com.catclaw.aura.data.network.api.DynamicApiService
import com.catclaw.aura.data.network.callback.HttpCallback
import com.catclaw.aura.data.network.config.NetworkConfig
import com.catclaw.aura.data.network.config.NetworkConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * App-wide HTTP entry point built on Retrofit + OkHttp.
 *
 * ## Flow
 * 1. Call [init] once from [com.catclaw.aura.AuraApplication].
 * 2. Invoke [get] / [postJson] / [postForm] with a [baseUrlKey], relative [path] (or absolute URL), and params.
 * 3. Receive [HttpCallback.onSuccess] with raw JSON text, or [HttpCallback.onFailed] with an [Exception].
 *
 * ## Multiple base URLs
 * Register keys in [NetworkConfig.baseUrls] (e.g. [NetworkConstants.BASE_URL_MAIN]).
 * Pass the key on each request; [resolveUrl] combines base + path. Absolute paths skip the base.
 *
 * ## Interceptors
 * Configured in [RetrofitProvider]: common headers + optional logging. Add custom interceptors by
 * extending [RetrofitProvider] or passing a custom [OkHttpClient] in a future revision.
 *
 * ## 文档
 * 完整 GET / POST 用法与测试 API 见 `app/docs/network.md`。
 */
object NetworkClient {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var config: NetworkConfig
    private lateinit var apiService: DynamicApiService
    private lateinit var okHttpClient: OkHttpClient

    val isInitialized: Boolean
        get() = this::config.isInitialized

    /**
     * Registers base URLs and builds Retrofit/OkHttp. Must run before any request.
     */
    fun init(networkConfig: NetworkConfig) {
        config = networkConfig
        val (client, service) = RetrofitProvider.create(networkConfig)
        okHttpClient = client
        apiService = service
    }

    /**
     * Adds or replaces a base URL at runtime (e.g. after loading remote config).
     */
    fun registerBaseUrl(key: String, baseUrl: String) {
        checkInitialized()
        val normalized = normalizeBaseUrl(baseUrl)
        config = config.copy(baseUrls = config.baseUrls + (key to normalized))
    }

    /**
     * Returns the OkHttp client for advanced use (custom interceptors on one-off calls).
     */
    fun okHttpClient(): OkHttpClient {
        checkInitialized()
        return okHttpClient
    }

    /**
     * 发起 GET 请求。
     *
     * @param baseUrlKey [NetworkConfig.baseUrls] 中注册的 key，默认 [NetworkConstants.BASE_URL_MAIN]。
     * @param path 相对路径（如 `"posts/1"`）或完整 URL（以 `http://` / `https://` 开头时忽略 baseUrlKey）。
     * @param queryParams URL 查询参数，如 `mapOf("postId" to "1")` → `?postId=1`。
     * @param headers 仅本次请求附加的 Header（会与全局 Header 合并）。
     * @param callback 主线程回调；成功时 [HttpCallback.onSuccess] 参数为响应体字符串（通常为 JSON）。
     *
     * ```
     * NetworkClient.get(
     *     path = "posts/1",
     *     queryParams = mapOf("expand" to "profile"),
     *     callback = object : HttpCallback {
     *         override fun onSuccess(json: String) { }
     *         override fun onFailed(exception: Exception) { }
     *     },
     * )
     * ```
     */
    fun get(
        baseUrlKey: String = NetworkConstants.BASE_URL_MAIN,
        path: String,
        queryParams: Map<String, String> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        callback: HttpCallback,
    ) {
        execute(callback) {
            val url = resolveUrl(baseUrlKey, path)
            val response = apiService.get(url, queryParams, headers)
            response.toJsonOrThrow()
        }
    }

    /**
     * 发起 POST 请求，Body 为 JSON（`application/json`）。
     *
     * @param bodyParams 转为 JSON 对象的字段；`Map<String, Any?>`，空 Map 发送 `{}`。
     * @param baseUrlKey path 相对路径时使用的根地址 key。
     * @param path 相对或绝对 URL，规则同 [get]。
     * @param headers 仅本次请求附加的 Header。
     * @param callback 主线程回调。
     *
     * ```
     * NetworkClient.postJson(
     *     path = "posts",
     *     bodyParams = mapOf("title" to "test", "body" to "hello", "userId" to 1),
     *     callback = object : HttpCallback {
     *         override fun onSuccess(json: String) { }
     *         override fun onFailed(exception: Exception) { }
     *     },
     * )
     * ```
     */
    fun postJson(
        baseUrlKey: String = NetworkConstants.BASE_URL_MAIN,
        path: String,
        bodyParams: Map<String, Any?> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        callback: HttpCallback,
    ) {
        execute(callback) {
            val url = resolveUrl(baseUrlKey, path)
            val json = JSONObject(bodyParams).toString()
            val body = json.toRequestBody(JSON_MEDIA_TYPE)
            val response = apiService.postJson(url, body, headers)
            response.toJsonOrThrow()
        }
    }

    /**
     * 发起 POST 请求，Body 为表单（`application/x-www-form-urlencoded`）。
     *
     * @param formParams 表单键值对，值均为 String。
     * @param baseUrlKey、path、headers、callback 含义同 [postJson]。
     *
     * ```
     * NetworkClient.postForm(
     *     path = "login",
     *     formParams = mapOf("username" to "demo", "password" to "secret"),
     *     callback = object : HttpCallback { … },
     * )
     * ```
     */
    fun postForm(
        baseUrlKey: String = NetworkConstants.BASE_URL_MAIN,
        path: String,
        formParams: Map<String, String>,
        headers: Map<String, String> = emptyMap(),
        callback: HttpCallback,
    ) {
        execute(callback) {
            val url = resolveUrl(baseUrlKey, path)
            val response = apiService.postForm(url, formParams, headers)
            response.toJsonOrThrow()
        }
    }

    internal fun resolveUrl(baseUrlKey: String, path: String): String {
        checkInitialized()
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path
        }
        val baseUrl = config.baseUrls[baseUrlKey]
            ?: throw IllegalArgumentException("Unknown base URL key: $baseUrlKey")
        return normalizeBaseUrl(baseUrl).trimEnd('/') + "/" + path.trimStart('/')
    }

    private fun execute(callback: HttpCallback, block: suspend () -> String) {
        checkInitialized()
        scope.launch {
            try {
                val json = block()
                deliverSuccess(callback, json)
            } catch (exception: Exception) {
                deliverFailure(callback, wrapException(exception))
            }
        }
    }

    private suspend fun retrofit2.Response<okhttp3.ResponseBody>.toJsonOrThrow(): String {
        if (!isSuccessful) {
            val errorBody = errorBody()?.string().orEmpty()
            throw IOException(
                buildString {
                    append("HTTP ")
                    append(code())
                    append(": ")
                    append(message())
                    if (errorBody.isNotEmpty()) {
                        append(". Body: ")
                        append(errorBody)
                    }
                },
            )
        }
        return body()?.string().orEmpty()
    }

    private fun wrapException(exception: Exception): Exception {
        return when (exception) {
            is UnknownHostException -> IOException("Network unreachable: ${exception.message}", exception)
            is SocketTimeoutException -> IOException("Request timed out: ${exception.message}", exception)
            is IOException -> exception
            else -> IOException(exception.message ?: exception.toString(), exception)
        }
    }

    private fun deliverSuccess(callback: HttpCallback, json: String) {
        mainHandler.post { callback.onSuccess(json) }
    }

    private fun deliverFailure(callback: HttpCallback, exception: Exception) {
        mainHandler.post { callback.onFailed(exception) }
    }

    private fun checkInitialized() {
        check(isInitialized) { "NetworkClient.init() must be called before making requests." }
    }

    private fun normalizeBaseUrl(url: String): String {
        return if (url.endsWith("/")) url else "$url/"
    }

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
}
