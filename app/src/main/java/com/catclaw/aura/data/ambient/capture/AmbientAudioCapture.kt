package com.catclaw.aura.data.ambient.capture

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import com.catclaw.aura.data.ambient.model.AudioCaptureResult
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Records microphone audio up to [maxDurationMs] (default 5s).
 */
class AmbientAudioCapture(
    private val context: Context,
    private val maxDurationMs: Long = 5_000L,
) {

    suspend fun capture(): AudioCaptureResult = withContext(Dispatchers.IO) {
        val outputFile = File(
            context.cacheDir,
            "ambient_audio_${System.currentTimeMillis()}.m4a",
        )
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        try {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setOutputFile(outputFile.absolutePath)
            recorder.prepare()
            recorder.start()
            delay(maxDurationMs)
            recorder.stop()
            AudioCaptureResult(
                uri = Uri.fromFile(outputFile),
                durationMs = maxDurationMs,
            )
        } catch (e: Exception) {
            AudioCaptureResult(errorMessage = e.message ?: "音频采集失败")
        } finally {
            runCatching { recorder.release() }
        }
    }
}
