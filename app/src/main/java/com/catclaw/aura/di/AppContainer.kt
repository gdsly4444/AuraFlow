package com.catclaw.aura.di

import android.content.Context
import com.catclaw.aura.data.ambient.AmbientCaptureCoordinator
import com.catclaw.aura.data.ambient.AmbientCapturePortImpl
import com.catclaw.aura.data.moment.MomentCardRepositoryImpl
import com.catclaw.aura.data.moment.MomentMediaArchiver
import com.catclaw.aura.data.moment.local.AuraDatabase
import com.catclaw.aura.data.network.NetworkClient
import com.catclaw.aura.data.network.config.NetworkConfig
import com.catclaw.aura.data.network.config.NetworkConstants
import com.catclaw.aura.data.scenedescription.SceneDescriptionRepository
import com.catclaw.aura.data.workflow.MomentWorkflowEngine
import com.catclaw.aura.data.workflow.MomentWorkflowRepositoryImpl
import com.catclaw.aura.data.workflow.MomentWorkflowStore
import com.catclaw.aura.data.workflow.WorkflowNotificationScheduler
import com.catclaw.aura.data.workflow.WorkflowRuntime
import com.catclaw.aura.domain.repository.AmbientCapturePort
import com.catclaw.aura.domain.repository.MomentCardRepository
import com.catclaw.aura.domain.repository.MomentWorkflowRepository
import com.catclaw.aura.domain.usecase.CaptureAmbientMomentUseCase
import com.catclaw.aura.domain.usecase.DeleteMomentCardUseCase
import com.catclaw.aura.domain.usecase.GetMomentCardUseCase
import com.catclaw.aura.domain.usecase.ObserveGeneratingStatusUseCase
import com.catclaw.aura.domain.usecase.ObserveHomeListUseCase
import com.catclaw.aura.domain.usecase.StartMomentWorkflowUseCase

/**
 * Manual composition root (Hilt is incompatible with this project's AGP 9 setup).
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val momentCardRepository: MomentCardRepository
    val momentWorkflowRepository: MomentWorkflowRepository
    val ambientCapturePort: AmbientCapturePort
    val ambientCapturePortImpl: AmbientCapturePortImpl
    val observeHomeListUseCase: ObserveHomeListUseCase
    val observeGeneratingStatusUseCase: ObserveGeneratingStatusUseCase
    val deleteMomentCardUseCase: DeleteMomentCardUseCase
    val getMomentCardUseCase: GetMomentCardUseCase
    val captureAmbientMomentUseCase: CaptureAmbientMomentUseCase

    val workflowEngine: MomentWorkflowEngine
    val workflowStore: MomentWorkflowStore

    init {
        NetworkClient.init(
            NetworkConfig(
                baseUrls = mapOf(
                    NetworkConstants.BASE_URL_MAIN to "https://jsonplaceholder.typicode.com/",
                    NetworkConstants.BASE_URL_SECONDARY to "https://jsonplaceholder.typicode.com/",
                    NetworkConstants.BASE_URL_DASHSCOPE to
                        "https://dashscope.aliyuncs.com/compatible-mode/v1/",
                ),
                commonHeaders = mapOf("Accept" to "application/json"),
                enableLogging = com.catclaw.aura.data.BuildConfig.DEBUG,
            ),
        )

        val database = AuraDatabase.get(appContext)
        val archiver = MomentMediaArchiver(appContext)
        momentCardRepository = MomentCardRepositoryImpl(database.momentCardDao(), archiver)

        workflowStore = MomentWorkflowStore()
        val notificationScheduler = WorkflowNotificationScheduler(appContext, workflowStore)
        val sceneDescriptionRepository = SceneDescriptionRepository(appContext)
        workflowEngine = MomentWorkflowEngine(
            appContext,
            workflowStore,
            momentCardRepository,
            sceneDescriptionRepository,
            notificationScheduler,
        )
        momentWorkflowRepository = MomentWorkflowRepositoryImpl(
            appContext,
            workflowEngine,
            workflowStore,
        )
        WorkflowRuntime.engine = workflowEngine
        WorkflowRuntime.store = workflowStore

        val captureCoordinator = AmbientCaptureCoordinator(appContext)
        ambientCapturePortImpl = AmbientCapturePortImpl(captureCoordinator)
        ambientCapturePort = ambientCapturePortImpl

        val startWorkflow = StartMomentWorkflowUseCase(momentWorkflowRepository)
        observeHomeListUseCase = ObserveHomeListUseCase(momentCardRepository, momentWorkflowRepository)
        observeGeneratingStatusUseCase = ObserveGeneratingStatusUseCase(momentWorkflowRepository)
        deleteMomentCardUseCase = DeleteMomentCardUseCase(momentCardRepository)
        getMomentCardUseCase = GetMomentCardUseCase(momentCardRepository)
        captureAmbientMomentUseCase = CaptureAmbientMomentUseCase(ambientCapturePort, startWorkflow)
    }
}
