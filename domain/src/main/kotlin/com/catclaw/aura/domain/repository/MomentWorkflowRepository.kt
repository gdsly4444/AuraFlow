package com.catclaw.aura.domain.repository

import com.catclaw.aura.domain.model.ActiveWorkflow
import com.catclaw.aura.domain.model.MomentCaptureSnapshot
import kotlinx.coroutines.flow.Flow

interface MomentWorkflowRepository {
    fun observeActiveWorkflows(): Flow<List<ActiveWorkflow>>
    fun enqueue(snapshot: MomentCaptureSnapshot)
    fun activeCount(): Int
}
