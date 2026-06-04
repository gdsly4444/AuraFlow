package com.catclaw.aura.data.workflow

import android.content.Context
import android.net.Uri
import com.catclaw.aura.data.moment.MomentMediaArchiver
import com.catclaw.aura.data.scenedescription.SceneDescriptionRepository
import com.catclaw.aura.domain.model.ActiveWorkflow
import com.catclaw.aura.domain.model.AmbientMoment
import com.catclaw.aura.domain.model.AudioCaptureResult
import com.catclaw.aura.domain.model.LocationSnapshot
import com.catclaw.aura.domain.model.MomentCaptureSnapshot
import com.catclaw.aura.domain.model.MomentCard
import com.catclaw.aura.domain.model.NowPlayingInfo
import com.catclaw.aura.domain.model.VideoCaptureResult
import com.catclaw.aura.domain.model.WorkflowPhase
import com.catclaw.aura.domain.repository.MomentCardRepository
import com.catclaw.aura.data.moment.ArchivedMomentMedia
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MomentWorkflowEngine(
    context: Context,
    private val store: MomentWorkflowStore,
    private val cardRepository: MomentCardRepository,
    private val sceneDescriptionRepository: SceneDescriptionRepository,
    private val notificationScheduler: WorkflowNotificationScheduler,
) {
    private val appContext = context.applicationContext
    private val archiver = MomentMediaArchiver(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val workChannel = Channel<MomentCaptureSnapshot>(Channel.UNLIMITED)
    private val workersStarted = AtomicBoolean(false)

    fun enqueue(snapshot: MomentCaptureSnapshot) {
        ensureWorkersStarted()
        store.upsert(
            ActiveWorkflow(
                workflowId = snapshot.workflowId,
                phase = WorkflowPhase.QUEUED,
                createdAtEpochMs = snapshot.capturedAtEpochMs,
            ),
        )
        notificationScheduler.onActivityChanged()
        workChannel.trySend(snapshot)
    }

    private fun ensureWorkersStarted() {
        if (!workersStarted.compareAndSet(false, true)) return
        repeat(MAX_CONCURRENT) {
            scope.launch {
                for (snapshot in workChannel) {
                    runWorkflow(snapshot)
                }
            }
        }
    }

    private suspend fun runWorkflow(snapshot: MomentCaptureSnapshot) {
        var archived: ArchivedMomentMedia? = null
        try {
            updatePhase(snapshot, WorkflowPhase.ARCHIVING_MEDIA)
            archived = withContext(Dispatchers.IO) { archiver.archive(snapshot) }
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
                    locationPlaceName = snapshot.locationPlaceName,
                    sceneDescription = description.text,
                    sceneDescriptionError = description.errorMessage,
                    captureErrorSummary = snapshot.captureErrorSummary(),
                ),
            )
            store.remove(snapshot.workflowId)
            notificationScheduler.onActivityChanged()
        } catch (e: Exception) {
            if (archived == null) {
                archived = runCatching {
                    withContext(Dispatchers.IO) { archiver.archive(snapshot) }
                }.getOrNull()
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
                    locationPlaceName = snapshot.locationPlaceName,
                    sceneDescription = null,
                    sceneDescriptionError = e.message ?: "生成失败",
                    captureErrorSummary = snapshot.captureErrorSummary(),
                ),
            )
            store.remove(snapshot.workflowId)
            notificationScheduler.onActivityChanged()
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
        notificationScheduler.onActivityChanged()
    }

    private fun MomentCaptureSnapshot.toAmbientMoment(archived: ArchivedMomentMedia): AmbientMoment {
        fun pathToUriString(path: String?): String? = path?.let { Uri.fromFile(File(it)).toString() }
        return AmbientMoment(
            capturedAtEpochMs = capturedAtEpochMs,
            video = VideoCaptureResult(
                uri = pathToUriString(archived.videoPath),
                posterUri = pathToUriString(archived.posterPath),
                durationMs = videoDurationMs,
                errorMessage = videoError,
            ),
            audio = AudioCaptureResult(
                uri = pathToUriString(archived.audioPath),
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
                    latitude = requireNotNull(latitude),
                    longitude = requireNotNull(longitude),
                    accuracyMeters = locationAccuracyMeters,
                    provider = locationProvider,
                    placeName = locationPlaceName,
                    placeFeatureType = null,
                    errorMessage = locationError,
                )
            } else {
                null
            },
        )
    }

    companion object {
        /** Limit parallel AI encode + network to reduce jank on mid-range devices. */
        const val MAX_CONCURRENT = 2
    }
}
