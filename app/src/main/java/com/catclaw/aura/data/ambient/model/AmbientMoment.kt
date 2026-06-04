package com.catclaw.aura.data.ambient.model

import android.net.Uri

/**
 * Aggregated result of a single ambient capture trigger.
 */
data class AmbientMoment(
    val capturedAtEpochMs: Long,
    val video: VideoCaptureResult,
    val audio: AudioCaptureResult,
    val nowPlaying: NowPlayingInfo,
    val location: LocationSnapshot?,
)

data class VideoCaptureResult(
    val uri: Uri? = null,
    val posterUri: Uri? = null,
    val durationMs: Long = 0L,
    val errorMessage: String? = null,
) {
    val isSuccess: Boolean get() = uri != null && errorMessage == null
}

data class AudioCaptureResult(
    val uri: Uri? = null,
    val durationMs: Long = 0L,
    val errorMessage: String? = null,
) {
    val isSuccess: Boolean get() = uri != null && errorMessage == null
}

data class NowPlayingInfo(
    val isMusicActive: Boolean,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val packageName: String? = null,
    val statusMessage: String,
)

data class LocationSnapshot(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val provider: String?,
    val errorMessage: String? = null,
) {
    val isSuccess: Boolean get() = errorMessage == null
}
