package com.catclaw.aura.data.scenedescription.model

import android.net.Uri

/**
 * Structured capture metadata before media encoding (no Base64).
 */
data class SceneCapturePayload(
    val capturedAtEpochMs: Long,
    val location: LocationPayload?,
    val nowPlaying: NowPlayingPayload,
    val video: VideoPayload,
    val audio: AudioPayload,
    val captureErrors: List<String>,
)

data class LocationPayload(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val provider: String?,
    val errorMessage: String?,
)

data class NowPlayingPayload(
    val isMusicActive: Boolean,
    val title: String?,
    val artist: String?,
    val album: String?,
    val packageName: String?,
    val statusMessage: String,
)

data class VideoPayload(
    val clipUri: Uri?,
    val posterUri: Uri?,
    val durationMs: Long,
    val isSuccess: Boolean,
    val errorMessage: String?,
)

data class AudioPayload(
    val uri: Uri?,
    val durationMs: Long,
    val isSuccess: Boolean,
    val errorMessage: String?,
)
