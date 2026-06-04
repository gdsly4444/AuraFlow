package com.catclaw.aura.ui.map

import com.catclaw.aura.service.moment.WorkflowPhase

data class HomeGeneratingStatus(
    val activeCount: Int,
    val primaryPhase: WorkflowPhase,
)
