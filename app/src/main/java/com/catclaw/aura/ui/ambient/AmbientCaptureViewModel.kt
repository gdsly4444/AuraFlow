package com.catclaw.aura.ui.ambient

import android.app.Application
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.catclaw.aura.data.ambient.AmbientCaptureCoordinator
import com.catclaw.aura.ui.base.BaseViewModel
import kotlinx.coroutines.launch

class AmbientCaptureViewModel(
    application: Application,
) : BaseViewModel<AmbientCaptureUiState, AmbientCaptureUiEvent>(AmbientCaptureUiState()) {

    private val coordinator = AmbientCaptureCoordinator(application)

    fun capture(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
    ) {
        if (uiState.value.isCapturing) return
        viewModelScope.launch {
            updateState { it.copy(isCapturing = true, errorMessage = null) }
            try {
                val moment = coordinator.captureAll(lifecycleOwner, previewView)
                updateState { it.copy(isCapturing = false, moment = moment) }
            } catch (e: Exception) {
                updateState {
                    it.copy(
                        isCapturing = false,
                        errorMessage = e.message ?: "采样失败",
                    )
                }
                sendEvent(AmbientCaptureUiEvent.ShowMessage("采样失败"))
            }
        }
    }

    class Factory(
        private val application: Application,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AmbientCaptureViewModel::class.java)) {
                "Unknown ViewModel: ${modelClass.name}"
            }
            return AmbientCaptureViewModel(application) as T
        }
    }
}
