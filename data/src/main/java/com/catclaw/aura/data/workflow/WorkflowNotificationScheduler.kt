package com.catclaw.aura.data.workflow

import android.content.Context

class WorkflowNotificationScheduler(
    private val context: Context,
    private val store: MomentWorkflowStore,
) {
    fun onActivityChanged() {
        MomentWorkflowNotifications.update(context, store)
    }
}
