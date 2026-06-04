package com.catclaw.aura.ui.ambient

sealed interface AmbientCaptureUiEvent {
    data class ShowMessage(val message: String) : AmbientCaptureUiEvent
}
