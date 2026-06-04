package com.catclaw.aura.presentation.ambient

sealed interface AmbientCaptureUiEvent {
    data class ShowMessage(val message: String) : AmbientCaptureUiEvent
}
