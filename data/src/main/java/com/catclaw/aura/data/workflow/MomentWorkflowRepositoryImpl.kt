package com.catclaw.aura.data.workflow

import android.content.Context
import com.catclaw.aura.domain.model.ActiveWorkflow
import com.catclaw.aura.domain.model.MomentCaptureSnapshot
import com.catclaw.aura.domain.repository.MomentWorkflowRepository
import kotlinx.coroutines.flow.Flow

class MomentWorkflowRepositoryImpl(
    private val context: Context,
    private val engine: MomentWorkflowEngine,
    private val store: MomentWorkflowStore,
) : MomentWorkflowRepository {

    override fun observeActiveWorkflows(): Flow<List<ActiveWorkflow>> =
        store.activeWorkflows

    override fun enqueue(snapshot: MomentCaptureSnapshot) {
        MomentWorkflowService.startWorkflow(context, snapshot)
    }

    override fun activeCount(): Int = store.activeCount()
}
