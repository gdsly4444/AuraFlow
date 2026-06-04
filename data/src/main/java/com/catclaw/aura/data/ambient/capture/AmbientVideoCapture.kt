package com.catclaw.aura.data.ambient.capture

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.catclaw.aura.domain.model.VideoCaptureResult
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Plan B motion clip: poster frame at trigger + ~3s silent video (no audio focus).
 */
class AmbientVideoCapture(
    private val context: Context,
    private val durationMs: Long = 3_000L,
) {

    suspend fun capture(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
    ): VideoCaptureResult {
        return try {
            val outcome = recordMotionClip(lifecycleOwner, previewView)
            VideoCaptureResult(
                uri = outcome.clipUri.toString(),
                posterUri = outcome.posterUri.toString(),
                durationMs = durationMs,
            )
        } catch (e: Exception) {
            VideoCaptureResult(errorMessage = e.message ?: "视频采集失败")
        }
    }

    private suspend fun recordMotionClip(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
    ): MotionClipOutcome = suspendCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            var activeRecording: Recording? = null
            var cameraProvider: ProcessCameraProvider? = null
            try {
                cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply {
                    surfaceProvider = previewView.surfaceProvider
                }
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.LOWEST))
                    .build()
                val videoCapture = VideoCapture.withOutput(recorder)
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    videoCapture,
                    imageCapture,
                )

                val timestamp = System.currentTimeMillis()
                val posterFile = File(context.cacheDir, "ambient_poster_$timestamp.jpg")
                val posterOptions = ImageCapture.OutputFileOptions.Builder(posterFile).build()

                imageCapture.takePicture(
                    posterOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            startSilentRecording(
                                videoCapture = videoCapture,
                                cameraProvider = cameraProvider,
                                timestamp = timestamp,
                                posterUri = Uri.fromFile(posterFile),
                                continuation = continuation,
                                onRecordingStarted = { activeRecording = it },
                            )
                        }

                        override fun onError(exception: ImageCaptureException) {
                            cameraProvider?.unbindAll()
                            continuation.resumeWithException(
                                IllegalStateException("封面拍摄失败: ${exception.message}"),
                            )
                        }
                    },
                )
            } catch (e: Exception) {
                cameraProvider?.unbindAll()
                continuation.resumeWithException(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun startSilentRecording(
        videoCapture: VideoCapture<Recorder>,
        cameraProvider: ProcessCameraProvider?,
        timestamp: Long,
        posterUri: Uri,
        continuation: kotlin.coroutines.Continuation<MotionClipOutcome>,
        onRecordingStarted: (Recording) -> Unit,
    ) {
        val outputFile = File(context.cacheDir, "ambient_video_$timestamp.mp4")
        val outputOptions = FileOutputOptions.Builder(outputFile).build()
        var activeRecording: Recording? = null

        try {
            // Do not call withAudioEnabled() — default is video-only (no mic / no audio focus).
            activeRecording = videoCapture.output
                .prepareRecording(context, outputOptions)
                .start(ContextCompat.getMainExecutor(context)) { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        cameraProvider?.unbindAll()
                        if (event.hasError()) {
                            continuation.resumeWithException(
                                IllegalStateException("Video error: ${event.error}"),
                            )
                        } else {
                            continuation.resume(
                                MotionClipOutcome(
                                    posterUri = posterUri,
                                    clipUri = Uri.fromFile(outputFile),
                                ),
                            )
                        }
                    }
                }
            onRecordingStarted(activeRecording)

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    activeRecording?.stop()
                } catch (e: Exception) {
                    cameraProvider?.unbindAll()
                    continuation.resumeWithException(e)
                }
            }, durationMs)
        } catch (e: Exception) {
            cameraProvider?.unbindAll()
            continuation.resumeWithException(e)
        }
    }

    private data class MotionClipOutcome(
        val posterUri: Uri,
        val clipUri: Uri,
    )
}
