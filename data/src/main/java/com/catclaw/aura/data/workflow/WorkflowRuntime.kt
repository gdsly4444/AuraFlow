package com.catclaw.aura.data.workflow

/**
 * Set from [com.catclaw.aura.di.AppContainer] at app startup.
 * Avoids a compile-time dependency from :data on the :app module.
 */
object WorkflowRuntime {
    lateinit var engine: MomentWorkflowEngine
    lateinit var store: MomentWorkflowStore
}
