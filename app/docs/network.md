# NetworkClient 使用说明

基于 **Retrofit + OkHttp** 的通用 HTTP 封装。业务层只需传入 **Base URL 键 + 路径 + 参数**，在 [HttpCallback](../src/main/java/com/catclaw/aura/data/network/callback/HttpCallback.kt) 里处理 JSON 或异常。

详细 API 注释见 [`NetworkClient.kt`](../src/main/java/com/catclaw/aura/data/network/NetworkClient.kt)。

---

## 初始化

在 [`AuraApplication`](../src/main/java/com/catclaw/aura/AuraApplication.kt) 中调用一次 `NetworkClient.init`：

```kotlin
NetworkClient.init(
    NetworkConfig(
        baseUrls = mapOf(
            NetworkConstants.BASE_URL_MAIN to "https://jsonplaceholder.typicode.com/",
            NetworkConstants.BASE_URL_SECONDARY to "https://api.mapbox.com/",
        ),
        commonHeaders = mapOf("Accept" to "application/json"),
        enableLogging = BuildConfig.DEBUG,
    ),
)
```

| 配置项 | 说明 |
|--------|------|
| `baseUrls` | 多个环境的根地址，key 自定义，URL 建议以 `/` 结尾 |
| `commonHeaders` | 每个请求都会带的 Header |
| `customInterceptors` | 额外 OkHttp 拦截器 |
| `enableLogging` | Debug 下打印请求/响应 Body |

运行时追加 Base URL：

```kotlin
NetworkClient.registerBaseUrl("staging", "https://staging.example.com/")
```

---

## HttpCallback

```kotlin
interface HttpCallback {
    fun onSuccess(json: String)   // HTTP 2xx，响应体原文（可为 JSON 字符串）
    fun onFailed(exception: Exception)  // 网络错误、超时、HTTP 非 2xx 等
}
```

**注意**：两个方法都在 **主线程** 回调，可直接更新 UI。

---

## GET 请求

### 方法签名

```kotlin
NetworkClient.get(
    baseUrlKey: String = NetworkConstants.BASE_URL_MAIN,
    path: String,
    queryParams: Map<String, String> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    callback: HttpCallback,
)
```

### URL 规则

| `path` 写法 | 实际请求地址 |
|-------------|--------------|
| `"posts/1"` + `BASE_URL_MAIN` | `{BASE_URL_MAIN}posts/1` |
| `"https://httpbin.org/get"` | 完整 URL，忽略 `baseUrlKey` |

### 示例 1：相对路径

```kotlin
NetworkClient.get(
    baseUrlKey = NetworkConstants.BASE_URL_MAIN,
    path = "posts/1",
    callback = object : HttpCallback {
        override fun onSuccess(json: String) {
            // json 示例: {"userId":1,"id":1,"title":"...","body":"..."}
        }
        override fun onFailed(exception: Exception) {
            // exception.message 含 HTTP 状态或网络原因
        }
    },
)
```

对照：https://jsonplaceholder.typicode.com/posts/1

### 示例 2：Query 参数

```kotlin
NetworkClient.get(
    baseUrlKey = NetworkConstants.BASE_URL_MAIN,
    path = "comments",
    queryParams = mapOf("postId" to "1"),
    callback = object : HttpCallback { /* … */ },
)
```

等价于：`GET …/comments?postId=1`

### 示例 3：绝对 URL + 自定义 Header

```kotlin
NetworkClient.get(
    path = "https://httpbin.org/get",
    queryParams = mapOf("hello" to "aura"),
    headers = mapOf("X-Custom" to "test"),
    callback = object : HttpCallback { /* … */ },
)
```

---

## POST 请求

### 1. postJson（`application/json`）

将 `bodyParams` 转为 JSON 对象发送。空 Map 发送 `{}`。

```kotlin
NetworkClient.postJson(
    baseUrlKey: String = NetworkConstants.BASE_URL_MAIN,
    path: String,
    bodyParams: Map<String, Any?> = emptyMap(),
    headers: Map<String, String> = emptyMap(),
    callback: HttpCallback,
)
```

**示例：创建文章（JSONPlaceholder）**

```kotlin
NetworkClient.postJson(
    baseUrlKey = NetworkConstants.BASE_URL_MAIN,
    path = "posts",
    bodyParams = mapOf(
        "title" to "Aura test",
        "body" to "hello network",
        "userId" to 1,
    ),
    callback = object : HttpCallback {
        override fun onSuccess(json: String) {
            // 返回带 id 的假创建结果
        }
        override fun onFailed(exception: Exception) { }
    },
)
```

对照：`POST https://jsonplaceholder.typicode.com/posts`

**示例：绝对 URL**

```kotlin
NetworkClient.postJson(
    path = "https://httpbin.org/post",
    bodyParams = mapOf("name" to "Aura"),
    callback = object : HttpCallback { /* … */ },
)
```

### 2. postForm（`application/x-www-form-urlencoded`）

表单字段，值为字符串：

```kotlin
NetworkClient.postForm(
    baseUrlKey = NetworkConstants.BASE_URL_MAIN,
    path = "login",
    formParams = mapOf(
        "username" to "demo",
        "password" to "secret",
    ),
    callback = object : HttpCallback { /* … */ },
)
```

---

## 公共测试 API（无需 Key）

| 服务 | Base URL | GET 示例 path | POST 示例 path |
|------|----------|---------------|----------------|
| [JSONPlaceholder](https://jsonplaceholder.typicode.com/) | `https://jsonplaceholder.typicode.com/` | `posts/1` | `posts` |
| [HTTPBin](https://httpbin.org/) | 使用绝对 URL | `https://httpbin.org/get` | `https://httpbin.org/post` |
| [ReqRes](https://reqres.in/) | `https://reqres.in/api/` | `users/2` | `users` |

Debug 包可在 Logcat 中查看 OkHttp 请求/响应日志。

---

## 错误处理

| 场景 | `onFailed` 中的异常 |
|------|---------------------|
| 无网络 / DNS 失败 | `IOException`（Network unreachable） |
| 超时 | `IOException`（Request timed out） |
| HTTP 4xx / 5xx | `IOException`，message 含状态码与 error body |
| 未调用 `init` | 调用时直接崩溃（`IllegalStateException`） |
| 未知 `baseUrlKey` | `IllegalArgumentException` |

---

## 在 ViewModel 中使用（建议）

```kotlin
class ExampleViewModel : ViewModel() {
    fun loadPost() {
        NetworkClient.get(
            path = "posts/1",
            callback = object : HttpCallback {
                override fun onSuccess(json: String) {
                    // 解析后 updateState；已在主线程
                }
                override fun onFailed(exception: Exception) {
                    // updateState(Error) 或 sendEvent
                }
            },
        )
    }
}
```

后续可将 `NetworkClient` 包一层 Repository，ViewModel 只依赖 Repository + 协程/Flow。

---

## 目录结构

```
app/src/main/java/com/catclaw/aura/data/network/
  NetworkClient.kt       # 对外入口：get / postJson / postForm
  RetrofitProvider.kt    # OkHttp + Retrofit 构建
  api/DynamicApiService.kt
  callback/HttpCallback.kt
  config/NetworkConfig.kt, NetworkConstants.kt
  interceptor/CommonHeadersInterceptor.kt
```
