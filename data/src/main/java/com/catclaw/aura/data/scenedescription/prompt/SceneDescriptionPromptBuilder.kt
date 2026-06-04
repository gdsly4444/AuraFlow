package com.catclaw.aura.data.scenedescription.prompt

import com.catclaw.aura.data.scenedescription.config.SceneDescriptionConfig
import com.catclaw.aura.data.scenedescription.model.EncodedMediaKind
import com.catclaw.aura.data.scenedescription.model.EncodedSceneMedia
import com.catclaw.aura.data.scenedescription.model.EncodedSceneMediaSet
import com.catclaw.aura.data.scenedescription.model.SceneCapturePayload
import org.json.JSONArray
import org.json.JSONObject

class SceneDescriptionPromptBuilder(
    private val config: SceneDescriptionConfig = SceneDescriptionConfig(),
) {

    fun buildRequestJson(
        payload: SceneCapturePayload,
        media: EncodedSceneMediaSet,
    ): String {
        val userContent = JSONArray().apply {
            media.selected?.let { put(mediaPart(it)) }
            put(textPart(buildMetadataText(payload, media)))
        }
        val messages = JSONArray().apply {
            put(
                JSONObject().apply {
                    put("role", "system")
                    put("content", SYSTEM_PROMPT.format(config.maxDescriptionChars))
                },
            )
            put(
                JSONObject().apply {
                    put("role", "user")
                    put("content", userContent)
                },
            )
        }
        return JSONObject().apply {
            put("model", config.model)
            put("messages", messages)
            put("modalities", JSONArray().put("text"))
            put("stream", true)
            put(
                "stream_options",
                JSONObject().apply {
                    put("include_usage", false)
                },
            )
        }.toString()
    }

    private fun buildMetadataText(
        payload: SceneCapturePayload,
        media: EncodedSceneMediaSet,
    ): String {
        val loc = payload.location
        val locationLine = if (loc != null && loc.errorMessage == null) {
            buildString {
                append("lat=${loc.latitude}, lon=${loc.longitude}, accuracy_m=${loc.accuracyMeters}")
                loc.placeName?.let { append(", place=$it") }
                loc.placeFeatureType?.let { append(", place_type=$it") }
            }
        } else {
            "unknown"
        }
        val music = payload.nowPlaying
        val track = listOfNotNull(music.title, music.artist, music.album)
            .joinToString(" / ")
            .ifBlank { "unknown" }
        val mediaNotes = buildList {
            val selected = media.selected
            if (selected == null) {
                add("omni_single_modality_none_attached")
            } else {
                add("omni_attached=${selected.kind.name.lowercase()}")
            }
            add("video_uri_available=${payload.video.isSuccess}")
            add("audio_uri_available=${payload.audio.isSuccess}")
            addAll(media.skippedNotes)
        }
        val attachmentHint = when (media.selected?.kind) {
            EncodedMediaKind.VIDEO -> "请优先根据附带的短视频画面理解场景（约 ${payload.video.durationMs}ms），再结合下方元数据。"
            EncodedMediaKind.POSTER -> "请根据附带的封面静帧与元数据理解场景。"
            EncodedMediaKind.AUDIO -> "请根据附带的音频与元数据理解场景。"
            null -> "未附带多媒体文件，仅根据元数据描述；缺失项写「未知」。"
        }
        return """
            |$attachmentHint
            |以下是同一次「环境瞬间」采样的结构化元数据。
            |captured_at_ms=${payload.capturedAtEpochMs}
            |location=$locationLine
            |now_playing_active=${music.isMusicActive}
            |track=$track
            |now_playing_status=${music.statusMessage}
            |video_success=${payload.video.isSuccess} duration_ms=${payload.video.durationMs}
            |audio_success=${payload.audio.isSuccess} duration_ms=${payload.audio.durationMs}
            |capture_errors=${payload.captureErrors.joinToString("; ").ifBlank { "none" }}
            |media_notes=${mediaNotes.joinToString("; ").ifBlank { "none" }}
        """.trimMargin()
    }

    private fun textPart(text: String): JSONObject =
        JSONObject().apply {
            put("type", "text")
            put("text", text)
        }

    private fun mediaPart(media: EncodedSceneMedia): JSONObject =
        when (media.kind) {
            EncodedMediaKind.POSTER -> imageUrlPart(media)
            EncodedMediaKind.VIDEO -> videoUrlPart(media)
            EncodedMediaKind.AUDIO -> inputAudioPart(media)
        }

    private fun imageUrlPart(media: EncodedSceneMedia): JSONObject =
        JSONObject().apply {
            put("type", "image_url")
            put(
                "image_url",
                JSONObject().apply {
                    put("url", media.dashScopePayload())
                },
            )
        }

    private fun videoUrlPart(media: EncodedSceneMedia): JSONObject =
        JSONObject().apply {
            put("type", "video_url")
            put(
                "video_url",
                JSONObject().apply {
                    put("url", media.dashScopePayload())
                },
            )
        }

    private fun inputAudioPart(media: EncodedSceneMedia): JSONObject =
        JSONObject().apply {
            put("type", "input_audio")
            put(
                "input_audio",
                JSONObject().apply {
                    put("data", media.dashScopePayload())
                    put("format", audioFormatForMime(media.mimeType))
                },
            )
        }

    private fun audioFormatForMime(mimeType: String): String =
        when (mimeType) {
            "audio/mp4", "audio/m4a" -> "m4a"
            "audio/mpeg" -> "mp3"
            "audio/wav", "audio/x-wav" -> "wav"
            else -> "m4a"
        }

    companion object {
        private const val SYSTEM_PROMPT =
            "你是环境瞬间采样助手。若用户提供了短视频，必须以视频画面为主要依据描述场景动态与氛围；" +
                "静帧或音频仅为补充。结合元数据用简洁中文输出（约 80～%d 字）。" +
                "不得编造未提供的信息；缺失项写「未知」。不要输出 JSON 或列表，只输出一段描述正文。"
    }
}
