package com.catclaw.aura.presentation.moment

import android.content.Context
import com.catclaw.aura.R
import com.catclaw.aura.domain.model.WorkflowPhase

fun WorkflowPhase.label(context: Context): String {
    val res = when (this) {
        WorkflowPhase.QUEUED -> R.string.moment_phase_queued
        WorkflowPhase.ARCHIVING_MEDIA -> R.string.moment_phase_archiving
        WorkflowPhase.GENERATING_DESCRIPTION -> R.string.moment_phase_generating
        WorkflowPhase.FAILED -> R.string.moment_phase_failed
        WorkflowPhase.COMPLETED -> R.string.moment_phase_done
    }
    return context.getString(res)
}
