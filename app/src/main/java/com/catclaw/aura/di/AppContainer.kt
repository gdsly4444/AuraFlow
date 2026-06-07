package com.catclaw.aura.di

import android.content.Context
import com.catclaw.aura.data.ambient.AmbientCaptureCoordinator
import com.catclaw.aura.data.ambient.AmbientCapturePortImpl
import com.catclaw.aura.data.aura.AuraUserIdStore
import com.catclaw.aura.data.aura.media.AuraMediaDownloader
import com.catclaw.aura.data.aura.remote.AuraApiRemote
import com.catclaw.aura.data.moment.MomentCardRepositoryImpl
import com.catclaw.aura.data.moment.MomentMediaArchiver
import com.catclaw.aura.data.moment.local.AuraDatabase
import com.catclaw.aura.data.network.NetworkClient
import com.catclaw.aura.data.network.config.NetworkConfig
import com.catclaw.aura.data.network.config.NetworkConstants
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
import com.catclaw.aura.domain.usecase.RefreshMomentListUseCase
import com.catclaw.aura.domain.usecase.StartMomentWorkflowUseCase

/**
 * Manual composition root. Heavy dependencies are lazy; [warmUp] preloads Room on a background thread.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val workflowStore: MomentWorkflowStore = MomentWorkflowStore()

    private val auraApiRemote: AuraApiRemote by lazy { AuraApiRemote.createDefault() }

    private val auraUserIdStore: AuraUserIdStore by lazy { AuraUserIdStore(appContext) }

    private val auraMediaDownloader: AuraMediaDownloader by lazy {
        AuraMediaDownloader(appContext, auraApiRemote, auraUserIdStore)
    }

    val momentCardRepository: MomentCardRepository by lazy {
        MomentCardRepositoryImpl(
            database.momentCardDao(),
            mediaArchiver,
            auraApiRemote,
            auraUserIdStore,
            auraMediaDownloader,
        )
    }

    val momentWorkflowRepository: MomentWorkflowRepository by lazy {
        MomentWorkflowRepositoryImpl(appContext, workflowEngine, workflowStore)
    }

    val ambientCapturePortImpl: AmbientCapturePortImpl by lazy {
        AmbientCapturePortImpl(captureCoordinator)
    }

    val ambientCapturePort: AmbientCapturePort
        get() = ambientCapturePortImpl

    val observeHomeListUseCase: ObserveHomeListUseCase by lazy {
        ObserveHomeListUseCase(momentCardRepository, momentWorkflowRepository)
    }

    val observeGeneratingStatusUseCase: ObserveGeneratingStatusUseCase by lazy {
        ObserveGeneratingStatusUseCase(momentWorkflowRepository)
    }

    val refreshMomentListUseCase: RefreshMomentListUseCase by lazy {
        RefreshMomentListUseCase(momentCardRepository)
    }

    val deleteMomentCardUseCase: DeleteMomentCardUseCase by lazy {
        DeleteMomentCardUseCase(momentCardRepository)
    }

    val getMomentCardUseCase: GetMomentCardUseCase by lazy {
        GetMomentCardUseCase(momentCardRepository)
    }

    val captureAmbientMomentUseCase: CaptureAmbientMomentUseCase by lazy {
        CaptureAmbientMomentUseCase(ambientCapturePort, startMomentWorkflowUseCase)
    }

    val workflowEngine: MomentWorkflowEngine by lazy {
        ensureNetworkInitialized()
        MomentWorkflowEngine.createDefault(
            appContext,
            workflowStore,
            momentCardRepository,
            WorkflowNotificationScheduler(appContext, workflowStore),
        ).also { WorkflowRuntime.engine = it }
    }

    private val startMomentWorkflowUseCase: StartMomentWorkflowUseCase by lazy {
        StartMomentWorkflowUseCase(momentWorkflowRepository)
    }

    private val database: AuraDatabase by lazy {
        AuraDatabase.get(appContext)
    }

    private val mediaArchiver: MomentMediaArchiver by lazy {
        MomentMediaArchiver(appContext)
    }

    private val captureCoordinator: AmbientCaptureCoordinator by lazy {
        AmbientCaptureCoordinator(appContext)
    }

    init {
        WorkflowRuntime.store = workflowStore
    }

    /** Opens Room and touches the card repository — call from a background thread at startup. */
    fun warmUp() {
        momentCardRepository
    }

    private fun ensureNetworkInitialized() {
        if (NetworkClient.isInitialized) return
        synchronized(NetworkClient::class.java) {
            if (NetworkClient.isInitialized) return
            NetworkClient.init(
                NetworkConfig(
                    baseUrls = mapOf(
                        NetworkConstants.BASE_URL_MAIN to "https://jsonplaceholder.typicode.com/",
                        NetworkConstants.BASE_URL_SECONDARY to "https://jsonplaceholder.typicode.com/",
                        NetworkConstants.BASE_URL_DASHSCOPE to
                            "https://dashscope.aliyuncs.com/compatible-mode/v1/",
                        NetworkConstants.BASE_URL_MAPBOX_API to "https://api.mapbox.com/",
                    ),
                    commonHeaders = mapOf("Accept" to "application/json"),
                    enableLogging = com.catclaw.aura.data.BuildConfig.DEBUG,
                ),
            )
        }
    }
}
