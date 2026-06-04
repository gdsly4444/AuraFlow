package com.catclaw.aura.domain.usecase

import com.catclaw.aura.domain.model.AmbientMoment
import com.catclaw.aura.domain.model.MomentCaptureSnapshot
import com.catclaw.aura.domain.repository.AmbientCapturePort
import java.util.UUID

class CaptureAmbientMomentUseCase(
    private val capturePort: AmbientCapturePort,
    private val startWorkflow: StartMomentWorkflowUseCase,
) {
    data class Result(
        val moment: AmbientMoment,
        val workflowId: String,
        val snapshot: MomentCaptureSnapshot,
    )

    suspend operator fun invoke(): Result {
        val moment = capturePort.capture()
        val workflowId = UUID.randomUUID().toString()
        val snapshot = MomentCaptureSnapshot.from(moment, workflowId)
        startWorkflow(snapshot)
        return Result(moment, workflowId, snapshot)
    }
}
