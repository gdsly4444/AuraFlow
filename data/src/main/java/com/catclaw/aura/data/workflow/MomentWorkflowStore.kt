package com.catclaw.aura.data.workflow

import com.catclaw.aura.domain.model.ActiveWorkflow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MomentWorkflowStore {

    private val _activeWorkflows = MutableStateFlow<List<ActiveWorkflow>>(emptyList())
    val activeWorkflows: StateFlow<List<ActiveWorkflow>> = _activeWorkflows.asStateFlow()

    fun upsert(workflow: ActiveWorkflow) {
        _activeWorkflows.update { list ->
            val index = list.indexOfFirst { it.workflowId == workflow.workflowId }
            if (index >= 0) {
                list.toMutableList().apply { set(index, workflow) }
            } else {
                list + workflow
            }
        }
    }

    fun remove(workflowId: String) {
        _activeWorkflows.update { list -> list.filterNot { it.workflowId == workflowId } }
    }

    fun activeCount(): Int = _activeWorkflows.value.size
}
