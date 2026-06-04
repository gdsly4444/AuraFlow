package com.catclaw.aura.ui.ambient

import android.app.Application
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.catclaw.aura.data.ambient.AmbientCaptureCoordinator
import com.catclaw.aura.data.moment.model.MomentCaptureSnapshot
import com.catclaw.aura.service.moment.MomentWorkflowService
import com.catclaw.aura.ui.base.BaseViewModel
import kotlinx.coroutines.launch
import java.util.UUID

class AmbientCaptureViewModel(
    private val application: Application,
) : BaseViewModel<AmbientCaptureUiState, AmbientCaptureUiEvent>(AmbientCaptureUiState()) {

    private val coordinator = AmbientCaptureCoordinator(application)

    fun capture(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
    ) {
        if (uiState.value.isCapturing) return
        viewModelScope.launch {
            updateState {
                it.copy(
                    isCapturing = true,
                    errorMessage = null,
                    workflowId = null,
                    workflowSubmitted = false,
                )
            }
            try {
                val moment = coordinator.captureAll(lifecycleOwner, previewView)
                val workflowId = UUID.randomUUID().toString()
                val snapshot = MomentCaptureSnapshot.from(moment, workflowId)
                MomentWorkflowService.startWorkflow(application, snapshot)
                updateState {
                    it.copy(
                        isCapturing = false,
                        moment = moment,
                        workflowId = workflowId,
                        workflowSubmitted = true,
                    )
                }
                sendEvent(AmbientCaptureUiEvent.ShowMessage("采样完成，正在后台生成卡片"))
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
