package com.catclaw.aura.data.aura.remote

import com.catclaw.aura.domain.model.MomentCaptureSnapshot
import org.json.JSONArray
import org.json.JSONObject

object AuraSceneRequestBuilder {

    fun buildJson(userId: String, audioMediaId: String, videoMediaId: String, snapshot: MomentCaptureSnapshot): ByteArray {
        val root = JSONObject()
        root.put("user_id", userId)
        root.put("audio_media_id", audioMediaId)
        root.put("video_media_id", videoMediaId)
        root.put("title", JSONObject.NULL)
        root.put("captured_at_ms", snapshot.capturedAtEpochMs)

        if (snapshot.latitude != null && snapshot.longitude != null) {
            val location = JSONObject()
            location.put("lat", snapshot.latitude)
            location.put("lng", snapshot.longitude)
            snapshot.locationPlaceName?.let { location.put("place_name", it) }
            snapshot.locationAccuracyMeters?.let { location.put("accuracy_meters", it.toDouble()) }
            snapshot.locationProvider?.let { location.put("provider", it) }
            root.put("location", location)
        }

        val nowPlaying = JSONObject()
        nowPlaying.put("is_music_active", snapshot.musicActive)
        snapshot.musicTitle?.let { nowPlaying.put("title", it) }
        snapshot.musicArtist?.let { nowPlaying.put("artist", it) }
        snapshot.musicAlbum?.let { nowPlaying.put("album", it) }
        snapshot.musicPackageName?.let { nowPlaying.put("package_name", it) }
        nowPlaying.put("status_message", snapshot.musicStatusMessage)
        root.put("now_playing", nowPlaying)

        val videoMeta = JSONObject()
        videoMeta.put("duration_ms", snapshot.videoDurationMs)
        videoMeta.put("is_success", snapshot.videoError == null)
        snapshot.videoError?.let { videoMeta.put("error_message", it) } ?: videoMeta.put("error_message", JSONObject.NULL)
        root.put("video_meta", videoMeta)

        val audioMeta = JSONObject()
        audioMeta.put("duration_ms", snapshot.audioDurationMs)
        audioMeta.put("is_success", snapshot.audioError == null)
        snapshot.audioError?.let { audioMeta.put("error_message", it) } ?: audioMeta.put("error_message", JSONObject.NULL)
        root.put("audio_meta", audioMeta)

        val captureErrors = JSONArray()
        snapshot.captureErrorSummary()?.split("; ")?.forEach { captureErrors.put(it) }
        root.put("capture_errors", captureErrors)

        return root.toString().toByteArray(Charsets.UTF_8)
    }
}
