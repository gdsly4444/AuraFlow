package com.catclaw.aura.data.moment.model

import com.catclaw.aura.data.ambient.model.AmbientMoment
import java.io.Serializable

/**
 * Serializable capture payload passed to [com.catclaw.aura.service.moment.MomentWorkflowService].
 */
data class MomentCaptureSnapshot(
    val workflowId: String,
    val capturedAtEpochMs: Long,
    val posterUri: String?,
    val videoUri: String?,
    val audioUri: String?,
    val videoDurationMs: Long,
    val audioDurationMs: Long,
    val videoError: String?,
    val audioError: String?,
    val musicActive: Boolean,
    val musicTitle: String?,
    val musicArtist: String?,
    val musicAlbum: String?,
    val musicPackageName: String?,
    val musicStatusMessage: String,
    val latitude: Double?,
    val longitude: Double?,
    val locationAccuracyMeters: Float?,
    val locationProvider: String?,
    val locationError: String?,
) : Serializable {

    companion object {
        fun from(moment: AmbientMoment, workflowId: String): MomentCaptureSnapshot {
            val location = moment.location
            return MomentCaptureSnapshot(
                workflowId = workflowId,
                capturedAtEpochMs = moment.capturedAtEpochMs,
                posterUri = moment.video.posterUri?.toString(),
                videoUri = moment.video.uri?.toString(),
                audioUri = moment.audio.uri?.toString(),
                videoDurationMs = moment.video.durationMs,
                audioDurationMs = moment.audio.durationMs,
                videoError = moment.video.errorMessage,
                audioError = moment.audio.errorMessage,
                musicActive = moment.nowPlaying.isMusicActive,
                musicTitle = moment.nowPlaying.title,
                musicArtist = moment.nowPlaying.artist,
                musicAlbum = moment.nowPlaying.album,
                musicPackageName = moment.nowPlaying.packageName,
                musicStatusMessage = moment.nowPlaying.statusMessage,
                latitude = location?.latitude,
                longitude = location?.longitude,
                locationAccuracyMeters = location?.accuracyMeters,
                locationProvider = location?.provider,
                locationError = location?.errorMessage,
            )
        }
    }

    fun captureErrorSummary(): String? {
        val parts = buildList {
            videoError?.let { add("video: $it") }
            audioError?.let { add("audio: $it") }
            locationError?.let { add("location: $it") }
        }
        return parts.joinToString("; ").ifBlank { null }
    }
}
