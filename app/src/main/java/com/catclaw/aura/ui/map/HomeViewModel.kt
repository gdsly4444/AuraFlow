package com.catclaw.aura.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.catclaw.aura.AuraApplication
import com.catclaw.aura.service.moment.WorkflowPhase
import com.catclaw.aura.ui.moment.MomentListItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val app = application as AuraApplication

    val generatingStatus: StateFlow<HomeGeneratingStatus?> =
        app.workflowStore.activeWorkflows.map { active ->
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
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val listItems: StateFlow<List<MomentListItem>> = combine(
        app.workflowStore.activeWorkflows,
        app.momentCardRepository.observeCards(),
    ) { active, cards ->
        val activeIds = active.map { it.workflowId }.toSet()
        val inProgress = active
            .filter { it.phase != com.catclaw.aura.service.moment.WorkflowPhase.COMPLETED }
            .map { MomentListItem.InProgress(it) }
        val completed = cards
            .filter { it.id !in activeIds }
            .map { MomentListItem.Completed(it) }
        (inProgress + completed).sortedByDescending { it.sortKey }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun deleteCard(cardId: String) {
        viewModelScope.launch {
            app.momentCardRepository.delete(cardId)
        }
    }
}
