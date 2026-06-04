package com.catclaw.aura.ui.ambient

import com.catclaw.aura.data.ambient.model.AmbientMoment

data class AmbientCaptureUiState(
    val isCapturing: Boolean = false,
    val moment: AmbientMoment? = null,
    val errorMessage: String? = null,
    val workflowId: String? = null,
    val workflowSubmitted: Boolean = false,
)
