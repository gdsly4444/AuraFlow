package com.catclaw.aura.data.ambient.capture

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

object NotificationListenerAccess {

    fun componentName(context: Context): ComponentName =
        ComponentName(context, AmbientMediaNotificationListener::class.java)

    fun isEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        val target = componentName(context).flattenToString()
        return enabled.split(':').any { it.equals(target, ignoreCase = true) }
    }

    fun settingsIntent(): Intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
}
