package com.catclaw.aura.data.workflow

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.catclaw.aura.data.R

object MomentWorkflowNotifications {

    const val NOTIFICATION_ID = 42
    private const val CHANNEL_ID = "moment_workflow"

    fun build(context: Context, activeCount: Int): Notification {
        ensureChannel(context)
        val launchIntent = requireNotNull(
            context.packageManager.getLaunchIntentForPackage(context.packageName),
        )
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle(context.getString(R.string.moment_workflow_notification_title))
            .setContentText(
                context.getString(R.string.moment_workflow_notification_text, activeCount),
            )
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    fun activeCount(store: MomentWorkflowStore): Int =
        store.activeWorkflows.value.size

    fun update(context: Context, store: MomentWorkflowStore) {
        val count = activeCount(store)
        val nm = context.getSystemService(NotificationManager::class.java)
        if (count == 0) {
            nm.cancel(NOTIFICATION_ID)
            context.stopService(Intent(context, MomentWorkflowService::class.java))
        } else {
            nm.notify(NOTIFICATION_ID, build(context, count))
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.moment_workflow_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
