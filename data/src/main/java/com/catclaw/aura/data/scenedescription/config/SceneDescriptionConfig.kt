package com.catclaw.aura.data.scenedescription.config

/**
 * Tunables for DashScope scene description requests.
 *
 * [SceneDescriptionMediaPreference]: qwen3-omni-flash accepts only **one** non-text
 * modality per request (image OR video OR audio). Default attaches the motion clip (mp4).
 */
data class SceneDescriptionConfig(
    val model: String = "qwen3-omni-flash",
    val mediaPreference: SceneDescriptionMediaPreference = SceneDescriptionMediaPreference.VIDEO_FIRST,
    val maxDescriptionChars: Int = 200,
    /** Skip encoding when raw file size exceeds this (bytes). DashScope suggests &lt; 10MB raw. */
    val maxRawMediaBytes: Long = 10L * 1024 * 1024,
    /** Longest edge for poster JPEG sent to the API (reduces multi‑MB camera frames). */
    val posterMaxEdgePx: Int = 1024,
    val posterJpegQuality: Int = 80,
)

enum class SceneDescriptionMediaPreference {
    /** Motion clip (mp4), else poster, else audio. */
    VIDEO_FIRST,
    /** Poster image, else video, else audio. */
    POSTER_FIRST,
    AUDIO_FIRST,
}
