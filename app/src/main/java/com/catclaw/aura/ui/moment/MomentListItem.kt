package com.catclaw.aura.ui.moment

import com.catclaw.aura.data.moment.model.MomentCard
import com.catclaw.aura.service.moment.ActiveWorkflow

sealed class MomentListItem {
    abstract val sortKey: Long
    abstract val id: String

    data class InProgress(
        val workflow: ActiveWorkflow,
    ) : MomentListItem() {
        override val sortKey: Long = workflow.createdAtEpochMs
        override val id: String = workflow.workflowId
    }

    data class Completed(
        val card: MomentCard,
    ) : MomentListItem() {
        override val sortKey: Long = card.createdAtEpochMs
        override val id: String = card.id
    }
}
