package com.catclaw.aura.domain.usecase

import com.catclaw.aura.domain.model.HomeListEntry
import com.catclaw.aura.domain.model.WorkflowPhase
import com.catclaw.aura.domain.repository.MomentCardRepository
import com.catclaw.aura.domain.repository.MomentWorkflowRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveHomeListUseCase(
    private val cardRepository: MomentCardRepository,
    private val workflowRepository: MomentWorkflowRepository,
) {
    operator fun invoke(): Flow<List<HomeListEntry>> = combine(
        workflowRepository.observeActiveWorkflows(),
        cardRepository.observeCards(),
    ) { active, cards ->
        val activeIds = active.map { it.workflowId }.toSet()
        val inProgress = active
            .filter { it.phase != WorkflowPhase.COMPLETED }
            .map { HomeListEntry.InProgress(it) }
        val completed = cards
            .filter { it.id !in activeIds }
            .map { HomeListEntry.Completed(it) }
        (inProgress + completed).sortedByDescending { it.sortKey }
    }
}
