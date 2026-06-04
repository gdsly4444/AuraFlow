package com.catclaw.aura.domain.usecase

import com.catclaw.aura.domain.model.HomeGeneratingStatus
import com.catclaw.aura.domain.model.WorkflowPhase
import com.catclaw.aura.domain.repository.MomentWorkflowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveGeneratingStatusUseCase(
    private val workflowRepository: MomentWorkflowRepository,
) {
    operator fun invoke(): Flow<HomeGeneratingStatus?> =
        workflowRepository.observeActiveWorkflows().map { active ->
            val working = active.filter { it.phase != WorkflowPhase.COMPLETED }
            if (working.isEmpty()) {
                null
            } else {
                val primary = working.maxByOrNull { it.createdAtEpochMs } ?: working.first()
                HomeGeneratingStatus(
                    activeCount = working.size,
                    primaryPhase = primary.phase,
                )
            }
        }
}
