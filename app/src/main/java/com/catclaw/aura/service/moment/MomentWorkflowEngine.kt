package com.catclaw.aura.service.moment

import android.app.Application
import android.net.Uri
import com.catclaw.aura.data.ambient.model.AmbientMoment
import com.catclaw.aura.data.ambient.model.AudioCaptureResult
import com.catclaw.aura.data.ambient.model.LocationSnapshot
import com.catclaw.aura.data.ambient.model.NowPlayingInfo
import com.catclaw.aura.data.ambient.model.VideoCaptureResult
import com.catclaw.aura.data.moment.ArchivedMomentMedia
import com.catclaw.aura.data.moment.MomentCardRepository
import com.catclaw.aura.data.moment.MomentMediaArchiver
import com.catclaw.aura.data.moment.model.MomentCaptureSnapshot
import com.catclaw.aura.data.moment.model.MomentCard
import com.catclaw.aura.data.scenedescription.SceneDescriptionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.io.File

class MomentWorkflowEngine(
    application: Application,
    private val store: MomentWorkflowStore,
    private val cardRepository: MomentCardRepository,
    private val onActivityChanged: () -> Unit,
) {
    private val appContext = application.applicationContext
    private val archiver = MomentMediaArchiver(appContext)
    private val sceneDescriptionRepository = SceneDescriptionRepository(application)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val workChannel = Channel<MomentCaptureSnapshot>(Channel.UNLIMITED)

    init {
        repeat(MAX_CONCURRENT) {
            scope.launch {
                for (snapshot in workChannel) {
                    runWorkflow(snapshot)
                }
            }
        }
    }

    fun enqueue(snapshot: MomentCaptureSnapshot) {
        store.upsert(
            ActiveWorkflow(
                workflowId = snapshot.workflowId,
                phase = WorkflowPhase.QUEUED,
                createdAtEpochMs = snapshot.capturedAtEpochMs,
            ),
        )
        onActivityChanged()
        workChannel.trySend(snapshot)
    }

    private suspend fun runWorkflow(snapshot: MomentCaptureSnapshot) {
        var archived: ArchivedMomentMedia? = null
        try {
            updatePhase(snapshot, WorkflowPhase.ARCHIVING_MEDIA)
            archived = archiver.archive(snapshot)
            updatePhase(
                snapshot,
                WorkflowPhase.GENERATING_DESCRIPTION,
                posterPreviewPath = archived.posterPath,
            )
            val moment = snapshot.toAmbientMoment(archived)
            val description = sceneDescriptionRepository.generateFrom(moment)
            cardRepository.save(
                MomentCard(
                    id = snapshot.workflowId,
                    createdAtEpochMs = snapshot.capturedAtEpochMs,
                    posterPath = archived.posterPath,
                    videoPath = archived.videoPath,
                    audioPath = archived.audioPath,
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
                    sceneDescription = description.text,
                    sceneDescriptionError = description.errorMessage,
                    captureErrorSummary = snapshot.captureErrorSummary(),
                ),
            )
            store.remove(snapshot.workflowId)
            onActivityChanged()
        } catch (e: Exception) {
            if (archived == null) {
                archived = runCatching { archiver.archive(snapshot) }.getOrNull()
            }
            cardRepository.save(
                MomentCard(
                    id = snapshot.workflowId,
                    createdAtEpochMs = snapshot.capturedAtEpochMs,
                    posterPath = archived?.posterPath,
                    videoPath = archived?.videoPath,
                    audioPath = archived?.audioPath,
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
                    sceneDescription = null,
                    sceneDescriptionError = e.message ?: "生成失败",
                    captureErrorSummary = snapshot.captureErrorSummary(),
                ),
            )
            store.remove(snapshot.workflowId)
            onActivityChanged()
        }
    }

    private fun updatePhase(
        snapshot: MomentCaptureSnapshot,
        phase: WorkflowPhase,
        posterPreviewPath: String? = null,
    ) {
        store.upsert(
            ActiveWorkflow(
                workflowId = snapshot.workflowId,
                phase = phase,
                createdAtEpochMs = snapshot.capturedAtEpochMs,
                posterPreviewPath = posterPreviewPath,
            ),
        )
        onActivityChanged()
    }

    private fun MomentCaptureSnapshot.toAmbientMoment(archived: ArchivedMomentMedia): AmbientMoment {
        fun pathToUri(path: String?): Uri? = path?.let { Uri.fromFile(File(it)) }
        return AmbientMoment(
            capturedAtEpochMs = capturedAtEpochMs,
            video = VideoCaptureResult(
                uri = pathToUri(archived.videoPath),
                posterUri = pathToUri(archived.posterPath),
                durationMs = videoDurationMs,
                errorMessage = videoError,
            ),
            audio = AudioCaptureResult(
                uri = pathToUri(archived.audioPath),
                durationMs = audioDurationMs,
                errorMessage = audioError,
            ),
            nowPlaying = NowPlayingInfo(
                isMusicActive = musicActive,
                title = musicTitle,
                artist = musicArtist,
                album = musicAlbum,
                packageName = musicPackageName,
                statusMessage = musicStatusMessage,
            ),
            location = if (latitude != null && longitude != null) {
                LocationSnapshot(
                    latitude = latitude,
                    longitude = longitude,
                    accuracyMeters = locationAccuracyMeters,
                    provider = locationProvider,
                    errorMessage = locationError,
                )
            } else {
                null
            },
        )
    }

    companion object {
        const val MAX_CONCURRENT = 5
    }
}
