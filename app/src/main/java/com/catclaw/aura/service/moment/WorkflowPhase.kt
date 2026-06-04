package com.catclaw.aura.service.moment

enum class WorkflowPhase {
    QUEUED,
    ARCHIVING_MEDIA,
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
