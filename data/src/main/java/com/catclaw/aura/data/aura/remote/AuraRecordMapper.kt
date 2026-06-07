package com.catclaw.aura.data.aura.remote

import com.catclaw.aura.domain.model.MomentCard

object AuraRecordMapper {

    fun fromListItem(item: RecordListItem): MomentCard = MomentCard(
        id = item.recordId,
        createdAtEpochMs = item.createdAtEpochMs,
        posterPath = null,
        videoPath = null,
        audioPath = null,
        videoDurationMs = 0,
        audioDurationMs = 0,
        musicTitle = null,
        musicArtist = null,
        musicAlbum = null,
        musicActive = false,
        musicStatusMessage = "",
        musicPackageName = null,
        latitude = item.latitude,
        longitude = item.longitude,
        locationAccuracyMeters = item.locationAccuracyMeters,
        locationProvider = item.locationProvider,
        locationPlaceName = item.locationPlaceName,
        sceneDescription = item.summary,
        sceneDescriptionError = null,
        captureErrorSummary = null,
        themeColor = item.color,
        thumbnailUrl = item.thumbnailUrl,
        videoUrl = item.videoUrl,
        audioUrl = item.audioUrl,
        serverStatus = item.status,
    )

    fun fromSceneResult(
        result: SceneGenerateResult,
        snapshot: com.catclaw.aura.domain.model.MomentCaptureSnapshot,
    ): MomentCard = MomentCard(
        id = result.recordId,
        createdAtEpochMs = result.createdAtEpochMs,
        posterPath = null,
        videoPath = null,
        audioPath = null,
        videoDurationMs = snapshot.videoDurationMs,
        audioDurationMs = snapshot.audioDurationMs,
        musicTitle = snapshot.musicTitle,
        musicArtist = snapshot.musicArtist,
        musicAlbum = snapshot.musicAlbum,
        musicActive = snapshot.musicActive,
        musicStatusMessage = snapshot.musicStatusMessage,
        musicPackageName = snapshot.musicPackageName,
        latitude = snapshot.latitude,
        longitude = snapshot.longitude,
        locationAccuracyMeters = snapshot.locationAccuracyMeters,
        locationProvider = snapshot.locationProvider,
        locationPlaceName = snapshot.locationPlaceName,
        sceneDescription = result.description,
        sceneDescriptionError = null,
        captureErrorSummary = snapshot.captureErrorSummary(),
        themeColor = result.color,
        thumbnailUrl = null,
        videoUrl = result.videoMediaUrl,
        audioUrl = result.audioMediaUrl,
        serverStatus = result.status,
    )
}
