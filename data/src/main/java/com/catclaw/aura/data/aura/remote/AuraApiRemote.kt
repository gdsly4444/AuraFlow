package com.catclaw.aura.data.aura.remote

import com.catclaw.aura.data.aura.AuraApiClient
import com.catclaw.aura.data.aura.AuraApiConfig
import com.catclaw.aura.data.aura.auth.AuraAuthHeaders
import com.catclaw.aura.data.network.interceptor.KekeHttpCall
import com.catclaw.aura.domain.model.MomentCaptureSnapshot
import java.io.File
import java.time.Instant
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class AuraApiRemote(
    private val config: AuraApiConfig,
    private val client: OkHttpClient,
) {
    val isConfigured: Boolean get() = config.isConfigured

    companion object {
        fun createDefault(): AuraApiRemote {
            val config = AuraApiConfig.fromBuildConfig()
            return AuraApiRemote(config, AuraApiClient.create(config))
        }
    }

    fun health(): Boolean {
        if (!isConfigured) return false
        val path = "/api/v1/health"
        val request = Request.Builder()
            .url("${config.baseUrl}$path")
            .get()
            .build()
        return KekeHttpCall.execute(client, request).use { it.isSuccessful }
    }

    fun uploadAudio(userId: String, file: File): MediaUploadResult =
        uploadMedia(userId, "/api/v1/media/audio", file, guessAudioMediaType(file))

    fun uploadVideo(userId: String, file: File): MediaUploadResult =
        uploadMedia(userId, "/api/v1/media/video", file, guessVideoMediaType(file))

    fun generateScene(
        userId: String,
        audioMediaId: String,
        videoMediaId: String,
        snapshot: MomentCaptureSnapshot,
    ): SceneGenerateResult {
        requireConfigured()
        val path = "/api/v1/scene/generate"
        val bodyBytes = AuraSceneRequestBuilder.buildJson(userId, audioMediaId, videoMediaId, snapshot)
        val headers = AuraAuthHeaders.signed(config, "POST", path, bodyBytes)
        val request = Request.Builder()
            .url("${config.baseUrl}$path")
            .post(bodyBytes.toRequestBody("application/json".toMediaType()))
            .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return KekeHttpCall.execute(client, request).use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw parseApiError(raw, response.code)
            parseSceneGenerate(raw)
        }
    }

    fun fetchRecords(userId: String, page: Int, pageSize: Int): RecordListResponse {
        requireConfigured()
        val path = "/api/v1/records"
        val url = "${config.baseUrl}$path".toHttpUrl().newBuilder()
            .addQueryParameter("user_id", userId)
            .addQueryParameter("page", page.toString())
            .addQueryParameter("page_size", pageSize.toString())
            .build()
        val headers = AuraAuthHeaders.signed(config, "GET", path)
        val request = Request.Builder()
            .url(url)
            .get()
            .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return KekeHttpCall.execute(client, request).use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw parseApiError(raw, response.code)
            parseRecordList(raw)
        }
    }

    fun downloadSignedGet(fullUrl: String, userId: String, dest: File) {
        requireConfigured()
        val httpUrl = fullUrl.toHttpUrl()
        val path = httpUrl.encodedPath
        val url = httpUrl.newBuilder()
            .apply {
                if (httpUrl.queryParameter("user_id") == null) {
                    addQueryParameter("user_id", userId)
                }
            }
            .build()
        val headers = AuraAuthHeaders.signed(config, "GET", path)
        val request = Request.Builder()
            .url(url)
            .get()
            .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
            .build()
        KekeHttpCall.execute(client, request).use { response ->
            if (!response.isSuccessful) {
                val raw = response.body?.string().orEmpty()
                throw parseApiError(raw, response.code)
            }
            dest.parentFile?.mkdirs()
            response.body?.byteStream()?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: throw AuraApiException(null, "Empty download body")
        }
    }

    private fun uploadMedia(
        userId: String,
        path: String,
        file: File,
        mediaType: String,
    ): MediaUploadResult {
        requireConfigured()
        val url = "${config.baseUrl}$path".toHttpUrl().newBuilder()
            .addQueryParameter("user_id", userId)
            .build()
        val multipart = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody(mediaType.toMediaType()),
            )
            .build()
        val headers = AuraAuthHeaders.signed(config, "POST", path)
        val request = Request.Builder()
            .url(url)
            .post(multipart)
            .apply { headers.forEach { (k, v) -> addHeader(k, v) } }
            .build()
        return KekeHttpCall.execute(client, request).use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) throw parseApiError(raw, response.code)
            val json = JSONObject(raw)
            MediaUploadResult(
                mediaId = json.getString("media_id"),
                mediaUrl = json.getString("media_url"),
                kind = json.getString("kind"),
            )
        }
    }

    private fun requireConfigured() {
        check(isConfigured) { "Aura API is not configured (AURA_API_BASE_URL / TOKEN / SECRET)" }
    }

    private fun parseSceneGenerate(raw: String): SceneGenerateResult {
        val json = JSONObject(raw)
        return SceneGenerateResult(
            recordId = json.getString("record_id"),
            color = json.getString("color"),
            description = json.getString("description"),
            summaryModel = json.optString("summary_model").ifBlank { null },
            attachedMediaKind = json.optString("attached_media_kind").ifBlank { null },
            audioMediaUrl = json.optString("audio_media_url").ifBlank { null },
            videoMediaUrl = json.optString("video_media_url").ifBlank { null },
            status = json.getString("status"),
            createdAtEpochMs = parseIsoEpochMs(json.getString("created_at")),
        )
    }

    private fun parseRecordList(raw: String): RecordListResponse {
        val json = JSONObject(raw)
        val itemsArray = json.getJSONArray("items")
        val items = buildList {
            for (i in 0 until itemsArray.length()) {
                add(parseRecordItem(itemsArray.getJSONObject(i)))
            }
        }
        return RecordListResponse(
            items = items,
            total = json.getInt("total"),
            page = json.getInt("page"),
            pageSize = json.getInt("page_size"),
        )
    }

    private fun parseRecordItem(json: JSONObject): RecordListItem {
        val location = json.optJSONObject("location")
        return RecordListItem(
            recordId = json.getString("record_id"),
            title = json.optString("title").ifBlank { null },
            summary = json.optString("summary").ifBlank { null },
            color = json.optString("color").ifBlank { null },
            thumbnailUrl = json.optString("thumbnail_url").ifBlank { null },
            audioUrl = json.optString("audio_url").ifBlank { null },
            videoUrl = json.optString("video_url").ifBlank { null },
            latitude = location?.optNullableDouble("lat"),
            longitude = location?.optNullableDouble("lng"),
            locationPlaceName = location?.resolvePlaceLabel(),
            locationAccuracyMeters = location?.optNullableFloat("accuracy_meters"),
            locationProvider = location?.optString("provider")?.ifBlank { null },
            status = json.getString("status"),
            createdAtEpochMs = parseIsoEpochMs(json.getString("created_at")),
        )
    }

    private fun JSONObject.optNullableDouble(key: String): Double? =
        if (has(key) && !isNull(key)) getDouble(key) else null

    private fun JSONObject.optNullableFloat(key: String): Float? =
        if (has(key) && !isNull(key)) getDouble(key).toFloat() else null

    /** Server stores [place_name]; [address] is optional. */
    private fun JSONObject.resolvePlaceLabel(): String? {
        val placeName = optString("place_name").ifBlank { null }
        val address = optString("address").ifBlank { null }
        return placeName ?: address
    }

    private fun parseIsoEpochMs(iso: String): Long =
        runCatching { Instant.parse(iso).toEpochMilli() }.getOrDefault(System.currentTimeMillis())

    private fun parseApiError(raw: String, httpCode: Int): AuraApiException {
        return try {
            val json = JSONObject(raw)
            AuraApiException(
                code = json.optString("code").ifBlank { null },
                message = json.optString("message", raw).ifBlank { "HTTP $httpCode" },
            )
        } catch (_: Exception) {
            AuraApiException(null, raw.ifBlank { "HTTP $httpCode" })
        }
    }

    private fun guessAudioMediaType(file: File): String =
        when (file.extension.lowercase()) {
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            else -> "audio/mp4"
        }

    private fun guessVideoMediaType(file: File): String =
        when (file.extension.lowercase()) {
            "webm" -> "video/webm"
            else -> "video/mp4"
        }
}
