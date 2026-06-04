package com.catclaw.aura.domain.usecase

import com.catclaw.aura.domain.model.MomentCaptureSnapshot
import com.catclaw.aura.domain.repository.MomentWorkflowRepository

class StartMomentWorkflowUseCase(
    private val workflowRepository: MomentWorkflowRepository,
) {
    operator fun invoke(snapshot: MomentCaptureSnapshot) {
        workflowRepository.enqueue(snapshot)
    }
}