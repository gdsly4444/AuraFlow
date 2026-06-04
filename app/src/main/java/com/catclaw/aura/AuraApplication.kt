package com.catclaw.aura

import android.app.Application
import com.catclaw.aura.data.moment.MomentCardRepository
import com.catclaw.aura.data.moment.MomentMediaArchiver
import com.catclaw.aura.data.moment.local.AuraDatabase
import com.catclaw.aura.data.network.NetworkClient
import com.catclaw.aura.data.network.config.NetworkConfig
import com.catclaw.aura.data.network.config.NetworkConstants
import com.catclaw.aura.service.moment.MomentWorkflowEngine
import com.catclaw.aura.service.moment.MomentWorkflowNotifications
import com.catclaw.aura.service.moment.MomentWorkflowStore

class AuraApplication : Application() {

    lateinit var momentCardRepository: MomentCardRepository
        private set

    lateinit var workflowStore: MomentWorkflowStore
        private set

    private var workflowEngine: MomentWorkflowEngine? = null

    override fun onCreate() {
        super.onCreate()
        val database = AuraDatabase.get(this)
        momentCardRepository = MomentCardRepository(
            database.momentCardDao(),
            MomentMediaArchiver(this),
        )
        workflowStore = MomentWorkflowStore()

        NetworkClient.init(
            NetworkConfig(
                baseUrls = mapOf(
                    NetworkConstants.BASE_URL_MAIN to "https://jsonplaceholder.typicode.com/",
                    NetworkConstants.BASE_URL_SECONDARY to "https://jsonplaceholder.typicode.com/",
                    NetworkConstants.BASE_URL_DASHSCOPE to
                        "https://dashscope.aliyuncs.com/compatible-mode/v1/",
                ),
                commonHeaders = mapOf(
                    "Accept" to "application/json",
                ),
                enableLogging = BuildConfig.DEBUG,
            ),
        )
    }

    fun getOrCreateWorkflowEngine(): MomentWorkflowEngine {
        return workflowEngine ?: MomentWorkflowEngine(
            application = this,
            store = workflowStore,
            cardRepository = momentCardRepository,
            onActivityChanged = { updateWorkflowNotification() },
        ).also { workflowEngine = it }
    }

    fun updateWorkflowNotification() {
        MomentWorkflowNotifications.update(applicationContext, workflowStore)
    }
}
