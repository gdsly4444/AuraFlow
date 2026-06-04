package com.catclaw.aura.domain.model

sealed class HomeListEntry {
    abstract val sortKey: Long
    abstract val id: String

    data class InProgress(
        val workflow: ActiveWorkflow,
    ) : HomeListEntry() {
        override val sortKey: Long = workflow.createdAtEpochMs
        override val id: String = workflow.workflowId
    }

    data class Completed(
        val card: MomentCard,
    ) : HomeListEntry() {
        override val sortKey: Long = card.createdAtEpochMs
        override val id: String = card.id
    }
}

data class HomeGeneratingStatus(
    val activeCount: Int,
    val primaryPhase: WorkflowPhase,
)
