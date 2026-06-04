package com.catclaw.aura.data.ambient

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.catclaw.aura.data.ambient.capture.AmbientAudioCapture
import com.catclaw.aura.data.ambient.capture.AmbientLocationCapture
import com.catclaw.aura.data.ambient.capture.AmbientNowPlayingProbe
import com.catclaw.aura.data.ambient.capture.AmbientVideoCapture
import com.catclaw.aura.domain.model.AmbientMoment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Orchestrates parallel ambient capture modules when the user triggers sampling.
 *
 * Video stays on [Dispatchers.Main] (CameraX); audio/location run on [Dispatchers.IO].
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
        val nowPlaying = nowPlayingProbe.probe()

        val videoDeferred = async(Dispatchers.Main.immediate) {
            videoCapture.capture(lifecycleOwner, previewView)
        }
        val audioDeferred = async(Dispatchers.IO) { audioCapture.capture() }
        val locationDeferred = async(Dispatchers.IO) { locationCapture.capture() }

        AmbientMoment(
            capturedAtEpochMs = capturedAt,
            video = videoDeferred.await(),
            audio = audioDeferred.await(),
            nowPlaying = nowPlaying,
            location = locationDeferred.await(),
        )
    }
}
