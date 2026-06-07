package com.catclaw.aura.data.aura.remote

data class MediaUploadResult(
    val mediaId: String,
    val mediaUrl: String,
    val kind: String,
)

data class SceneGenerateResult(
    val recordId: String,
    val color: String,
    val description: String,
    val summaryModel: String?,
    val attachedMediaKind: String?,
    val audioMediaUrl: String?,
    val videoMediaUrl: String?,
    val status: String,
    val createdAtEpochMs: Long,
)

data class RecordListItem(
    val recordId: String,
    val title: String?,
    val summary: String?,
    val color: String?,
    val thumbnailUrl: String?,
    val audioUrl: String?,
    val videoUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
    val locationPlaceName: String?,
    val locationAccuracyMeters: Float?,
    val locationProvider: String?,
    val status: String,
    val createdAtEpochMs: Long,
)

data class RecordListResponse(
    val items: List<RecordListItem>,
    val total: Int,
    val page: Int,
    val pageSize: Int,
)

class AuraApiException(
    val code: String?,
    message: String,
) : Exception(message)
