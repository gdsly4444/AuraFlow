package com.catclaw.aura.data.ambient

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.catclaw.aura.data.ambient.capture.AmbientAudioCapture
import com.catclaw.aura.data.ambient.capture.AmbientLocationCapture
import com.catclaw.aura.data.ambient.capture.AmbientNowPlayingProbe
import com.catclaw.aura.data.ambient.capture.AmbientVideoCapture
import com.catclaw.aura.data.ambient.model.AmbientMoment
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Orchestrates parallel ambient capture modules when the user triggers sampling.
 */
class AmbientCaptureCoordinator(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val videoCapture = AmbientVideoCapture(appContext)
    private val audioCapture = AmbientAudioCapture(appContext)
    private val nowPlayingProbe = AmbientNowPlayingProbe(appContext)
    private val locationCapture = AmbientLocationCapture(appContext)

    suspend fun captureAll(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
    ): AmbientMoment = coroutineScope {
        val capturedAt = System.currentTimeMillis()
        // Probe before mic/video so recording is less likely to disturb playback metadata.
        val nowPlaying = nowPlayingProbe.probe()

        val videoDeferred = async {
            videoCapture.capture(lifecycleOwner, previewView)
        }
        val audioDeferred = async { audioCapture.capture() }
        val locationDeferred = async { locationCapture.capture() }

        AmbientMoment(
            capturedAtEpochMs = capturedAt,
            video = videoDeferred.await(),
            audio = audioDeferred.await(),
            nowPlaying = nowPlaying,
            location = locationDeferred.await(),
        )
    }
}
