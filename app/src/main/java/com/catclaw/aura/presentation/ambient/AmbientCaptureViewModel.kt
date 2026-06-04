package com.catclaw.aura.presentation.ambient

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.catclaw.aura.di.AppContainer
import com.catclaw.aura.domain.usecase.CaptureAmbientMomentUseCase
import com.catclaw.aura.presentation.base.BaseViewModel
import kotlinx.coroutines.launch

class AmbientCaptureViewModel(
    private val captureAmbientMomentUseCase: CaptureAmbientMomentUseCase,
) : BaseViewModel<AmbientCaptureUiState, AmbientCaptureUiEvent>(AmbientCaptureUiState()) {

    fun capture() {
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
                val result = captureAmbientMomentUseCase()
                updateState {
                    it.copy(
                        isCapturing = false,
                        moment = result.moment,
                        workflowId = result.workflowId,
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
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AmbientCaptureViewModel::class.java))
            return AmbientCaptureViewModel(container.captureAmbientMomentUseCase) as T
        }
    }
}
