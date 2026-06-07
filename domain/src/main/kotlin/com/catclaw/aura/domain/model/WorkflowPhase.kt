package com.catclaw.aura.domain.model

enum class WorkflowPhase {
    QUEUED,
    UPLOADING_AUDIO,
    UPLOADING_VIDEO,
    GENERATING_DESCRIPTION,
    COMPLETED,
    FAILED,
}

data class ActiveWorkflow(
    val workflowId: String,
    val phase: WorkflowPhase,
    val createdAtEpochMs: Long,
    val posterPreviewPath: String? = null,
    val errorMessage: String? = null,
)
