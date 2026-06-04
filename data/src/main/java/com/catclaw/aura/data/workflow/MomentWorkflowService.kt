package com.catclaw.aura.data.workflow

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.catclaw.aura.domain.model.MomentCaptureSnapshot

class MomentWorkflowService : Service() {

    private lateinit var engine: MomentWorkflowEngine
    private lateinit var store: MomentWorkflowStore

    override fun onCreate() {
        super.onCreate()
        engine = WorkflowRuntime.engine
        store = WorkflowRuntime.store
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val snapshot = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra(EXTRA_SNAPSHOT, MomentCaptureSnapshot::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra(EXTRA_SNAPSHOT) as? MomentCaptureSnapshot
                }
                if (snapshot != null) {
                    val count = MomentWorkflowNotifications.activeCount(store) + 1
                    startForeground(
                        MomentWorkflowNotifications.NOTIFICATION_ID,
                        MomentWorkflowNotifications.build(this, count.coerceAtLeast(1)),
                    )
                    engine.enqueue(snapshot)
                }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        private const val ACTION_START = "com.catclaw.aura.moment.START"
        private const val EXTRA_SNAPSHOT = "snapshot"

        fun startWorkflow(context: Context, snapshot: MomentCaptureSnapshot) {
            val intent = Intent(context, MomentWorkflowService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_SNAPSHOT, snapshot)
            }
            context.startForegroundService(intent)
        }
    }
}
