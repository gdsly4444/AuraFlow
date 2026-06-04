package com.catclaw.aura.data.network.callback

/**
 * 通用 HTTP 请求回调，[com.catclaw.aura.data.network.NetworkClient] 的所有请求均使用此接口。
 *
 * **线程**： [onSuccess] 与 [onFailed] 均在主线程调用，可直接更新 UI。
 *
 * **成功**： [onSuccess] 的 [json] 为 HTTP 2xx 响应体原文（通常为 JSON 字符串，需自行解析）。
 *
 * **失败**： [onFailed] 的 [exception] 可能为网络不可用、超时、HTTP 非 2xx（message 含状态码与 error body）等。
 *
 * 使用示例见 `app/docs/network.md`。
 */
interface HttpCallback {

    fun onSuccess(json: String)

    fun onFailed(exception: Exception)
}
