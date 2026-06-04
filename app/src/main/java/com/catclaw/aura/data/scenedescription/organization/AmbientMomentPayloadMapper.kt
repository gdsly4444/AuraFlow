package com.catclaw.aura.data.scenedescription.organization

import com.catclaw.aura.data.ambient.model.AmbientMoment
import com.catclaw.aura.data.scenedescription.model.AudioPayload
import com.catclaw.aura.data.scenedescription.model.LocationPayload
import com.catclaw.aura.data.scenedescription.model.NowPlayingPayload
import com.catclaw.aura.data.scenedescription.model.SceneCapturePayload
import com.catclaw.aura.data.scenedescription.model.VideoPayload

object AmbientMomentPayloadMapper {

    fun map(moment: AmbientMoment): SceneCapturePayload {
        val errors = buildList {
            moment.video.errorMessage?.let { add("video: $it") }
            moment.audio.errorMessage?.let { add("audio: $it") }
            moment.location?.errorMessage?.let { add("location: $it") }
        }
        return SceneCapturePayload(
            capturedAtEpochMs = moment.capturedAtEpochMs,
            location = moment.location?.let {
                LocationPayload(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracyMeters = it.accuracyMeters,
                    provider = it.provider,
                    errorMessage = it.errorMessage,
                )
            },
            nowPlaying = NowPlayingPayload(
                isMusicActive = moment.nowPlaying.isMusicActive,
                title = moment.nowPlaying.title,
                artist = moment.nowPlaying.artist,
                album = moment.nowPlaying.album,
                packageName = moment.nowPlaying.packageName,
                statusMessage = moment.nowPlaying.statusMessage,
            ),
            video = VideoPayload(
                clipUri = moment.video.uri,
                posterUri = moment.video.posterUri,
                durationMs = moment.video.durationMs,
                isSuccess = moment.video.isSuccess,
                errorMessage = moment.video.errorMessage,
            ),
            audio = AudioPayload(
                uri = moment.audio.uri,
                durationMs = moment.audio.durationMs,
                isSuccess = moment.audio.isSuccess,
                errorMessage = moment.audio.errorMessage,
            ),
            captureErrors = errors,
        )
    }
}
