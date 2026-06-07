package com.catclaw.aura.data.workflow

import android.content.Context
import com.catclaw.aura.data.aura.AuraApiClient
import com.catclaw.aura.data.aura.AuraApiConfig
import com.catclaw.aura.data.aura.AuraUserIdStore
import com.catclaw.aura.data.aura.media.CaptureUploadFile
import com.catclaw.aura.data.aura.remote.AuraApiRemote
import com.catclaw.aura.data.aura.remote.AuraRecordMapper
import com.catclaw.aura.domain.model.ActiveWorkflow
import com.catclaw.aura.domain.model.MomentCaptureSnapshot
import com.catclaw.aura.domain.model.MomentCard
import com.catclaw.aura.domain.model.WorkflowPhase
import com.catclaw.aura.domain.repository.MomentCardRepository
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
    private val auraApi: AuraApiRemote,
    private val userIdStore: AuraUserIdStore,
    private val captureUploadFile: CaptureUploadFile,
    private val notificationScheduler: WorkflowNotificationScheduler,
) {
    private val appContext = context.applicationContext
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
        if (!auraApi.isConfigured) {
            failWorkflow(snapshot, "Aura API 未配置（请设置 AURA_API_BASE_URL / TOKEN / SECRET）")
            return
        }
        try {
            val userId = userIdStore.getUserId()
            updatePhase(snapshot, WorkflowPhase.UPLOADING_AUDIO)

            val audioFile = withContext(Dispatchers.IO) {
                captureUploadFile.resolve(snapshot.audioUri, "audio.m4a")
                    ?: error("音频文件不可用")
            }
            val audioMediaId = withContext(Dispatchers.IO) {
                auraApi.uploadAudio(userId, audioFile).mediaId
            }

            updatePhase(snapshot, WorkflowPhase.UPLOADING_VIDEO)
            val videoFile = withContext(Dispatchers.IO) {
                captureUploadFile.resolve(snapshot.videoUri, "clip.mp4")
                    ?: error("视频文件不可用")
            }
            val videoMediaId = withContext(Dispatchers.IO) {
                auraApi.uploadVideo(userId, videoFile).mediaId
            }

            withContext(Dispatchers.IO) {
                deleteCaptureFiles(snapshot)
                if (isTempUpload(audioFile)) audioFile.delete()
                if (isTempUpload(videoFile)) videoFile.delete()
            }

            updatePhase(
                snapshot,
                WorkflowPhase.GENERATING_DESCRIPTION,
                posterPreviewPath = snapshot.posterUri,
            )
            val sceneResult = withContext(Dispatchers.IO) {
                auraApi.generateScene(userId, audioMediaId, videoMediaId, snapshot)
            }

            val card = AuraRecordMapper.fromSceneResult(sceneResult, snapshot)
            cardRepository.save(card)
            cardRepository.refreshFromServer(page = 1, pageSize = 10)
            store.remove(snapshot.workflowId)
            notificationScheduler.onActivityChanged()
        } catch (e: Exception) {
            failWorkflow(snapshot, e.message ?: "生成失败")
        }
    }

    private suspend fun failWorkflow(snapshot: MomentCaptureSnapshot, message: String) {
        updatePhase(
            snapshot,
            WorkflowPhase.FAILED,
            errorMessage = message,
        )
        cardRepository.save(
            MomentCard(
                id = snapshot.workflowId,
                createdAtEpochMs = snapshot.capturedAtEpochMs,
                posterPath = snapshot.posterUri,
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
                sceneDescription = null,
                sceneDescriptionError = message,
                captureErrorSummary = snapshot.captureErrorSummary(),
            ),
        )
        store.remove(snapshot.workflowId)
        notificationScheduler.onActivityChanged()
    }

    private fun deleteCaptureFiles(snapshot: MomentCaptureSnapshot) {
        captureUploadFile.deleteIfLocal(snapshot.posterUri)
        captureUploadFile.deleteIfLocal(snapshot.videoUri)
        captureUploadFile.deleteIfLocal(snapshot.audioUri)
    }

    private fun isTempUpload(file: File): Boolean =
        file.parentFile?.absolutePath == appContext.cacheDir.absolutePath

    private fun updatePhase(
        snapshot: MomentCaptureSnapshot,
        phase: WorkflowPhase,
        posterPreviewPath: String? = null,
        errorMessage: String? = null,
    ) {
        store.upsert(
            ActiveWorkflow(
                workflowId = snapshot.workflowId,
                phase = phase,
                createdAtEpochMs = snapshot.capturedAtEpochMs,
                posterPreviewPath = posterPreviewPath,
                errorMessage = errorMessage,
            ),
        )
        notificationScheduler.onActivityChanged()
    }

    companion object {
        const val MAX_CONCURRENT = 2

        fun createDefault(
            context: Context,
            store: MomentWorkflowStore,
            cardRepository: MomentCardRepository,
            notificationScheduler: WorkflowNotificationScheduler,
        ): MomentWorkflowEngine {
            val appContext = context.applicationContext
            val config = AuraApiConfig.fromBuildConfig()
            val client = AuraApiClient.create(config)
            val auraApi = AuraApiRemote(config, client)
            val userIdStore = AuraUserIdStore(appContext)
            val captureUploadFile = CaptureUploadFile(appContext)
            return MomentWorkflowEngine(
                appContext,
                store,
                cardRepository,
                auraApi,
                userIdStore,
                captureUploadFile,
                notificationScheduler,
            )
        }
    }
}
