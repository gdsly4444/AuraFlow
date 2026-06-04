package com.catclaw.aura.data.scenedescription.config

/**
 * Tunables for DashScope scene description requests.
 *
 * [SceneDescriptionMediaPreference]: qwen3-omni-flash accepts only **one** non-text
 * modality per request (image OR video OR audio). Default sends compressed poster.
 */
data class SceneDescriptionConfig(
    val model: String = "qwen3-omni-flash",
    val mediaPreference: SceneDescriptionMediaPreference = SceneDescriptionMediaPreference.POSTER_FIRST,
    val maxDescriptionChars: Int = 200,
    /** Skip encoding when raw file size exceeds this (bytes). */
    val maxRawMediaBytes: Long = 7L * 1024 * 1024,
    /** Longest edge for poster JPEG sent to the API (reduces multi‑MB camera frames). */
    val posterMaxEdgePx: Int = 1024,
    val posterJpegQuality: Int = 80,
)

enum class SceneDescriptionMediaPreference {
    /** Poster image, else video, else audio. */
    POSTER_FIRST,
    VIDEO_FIRST,
    AUDIO_FIRST,
}
