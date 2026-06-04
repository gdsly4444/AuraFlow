package com.catclaw.aura.data.ambient

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.catclaw.aura.domain.model.AmbientMoment
import com.catclaw.aura.domain.repository.AmbientCapturePort

class AmbientCapturePortImpl(
    private val coordinator: AmbientCaptureCoordinator,
) : AmbientCapturePort {

    @Volatile
    private var session: CaptureSession? = null

    fun bindCaptureSession(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        session = CaptureSession(lifecycleOwner, previewView)
    }

    fun clearCaptureSession() {
        session = null
    }

    override suspend fun capture(): AmbientMoment {
        val bound = session
            ?: error("Camera session not bound — call bindCaptureSession() before capture")
        return coordinator.captureAll(bound.lifecycleOwner, bound.previewView)
    }

    private data class CaptureSession(
        val lifecycleOwner: LifecycleOwner,
        val previewView: PreviewView,
    )
}
