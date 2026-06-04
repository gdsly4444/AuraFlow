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
                    put("content", systemPrompt())
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
            EncodedMediaKind.VIDEO ->
                "请优先观看附带的短视频（约 ${payload.video.durationMs}ms），捕捉画面中的情绪氛围；" +
                    "若出现人物，勿认定其身份，而是引导「我」回想遇见对方时的心境。"
            EncodedMediaKind.POSTER ->
                "请凝视附带的静帧画面，感受那一刻的温度与情绪余韵，再结合元数据。"
            EncodedMediaKind.AUDIO ->
                "请聆听环境声与背景音乐，从声音里辨认情绪，再结合元数据。"
            null -> "未附带影像/音频，仅依元数据书写；对未知信息留白，不编造具体往事。"
        }
        return """
            |【写作任务】为用户写下一段可触动感官与回忆的环境瞬间文字，像写给未来的自己，而非客观监控报告。
            |$attachmentHint
            |
            |【人物出现时的写法】若画面/声音中可感知到他人（面孔、背影、交谈、擦肩等）：
            |— 禁止编造姓名、关系、对话或具体相识经过；
            |— 用温柔、留白的问句或意象，帮助「我」回到相遇当下的情绪（例如孤独、雀跃、释然、仓促、感激、不安），
            |  如「那个轮廓，是否曾在你某种心绪里出现过？」；
            |— 若看不清人物，不要强行写人。
            |
            |【可唤起的线索】地点锚定「在哪里」；曲目锚定「当时听歌的心境」；光影与动静锚定「身体感受」。
            |
            |【结构化元数据】
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

    private fun systemPrompt(): String {
        val max = config.maxDescriptionChars
        val min = (max * 0.45).toInt().coerceAtLeast(100)
        return """
            |你是 Aura 的「记忆书写者」，把一次环境瞬间采样化成能触动情绪、勾起回忆的中文散文。
            |
            |身份与语气：第二人称「你」或含蓄的第一人称「我」，温暖、具体、有人文关怀；避免冷冰冰的字段罗列和监控口吻。
            |
            |依据优先级：① 短视频画面（若有）— 光线、动静、距离感、人群与表情氛围；② 背景音乐（若有）— 与心境的呼应；③ 地点名称（若有）— 作为记忆锚点；④ 其余元数据。不得捏造未提供的具体往事、人名、关系或剧情。
            |
            |画面中有人时：只写可观察到的氛围（如并肩、擦肩、远处注视），绝不断言对方是谁。用留白问句引导读者回想「遇见那个人时，自己正处在怎样的情感里」——例如眷恋、孤独、雀跃、释然、仓促、感激或不安。若看不清人物，不要强行写人。
            |
            |禁止：JSON、列表、标题、技术字段复述、过度夸张、虚假的具体回忆。
            |
            |输出：一段连贯散文，约 $min～$max 字；可有一句收束的余韵，但不要使用「总之」「综上所述」等论文腔。
        """.trimMargin()
    }
}
