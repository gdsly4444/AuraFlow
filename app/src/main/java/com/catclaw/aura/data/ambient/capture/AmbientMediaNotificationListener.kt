package com.catclaw.aura.data.ambient.capture

import android.app.Notification
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.catclaw.aura.data.ambient.capture.AmbientNowPlayingCache.CachedNowPlaying

/**
 * Reads media-style playback notifications and caches track metadata for ambient sampling.
 * User must enable notification access in system settings.
 */
class AmbientMediaNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        activeNotifications?.forEach { handleNotification(it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        handleNotification(sbn)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        AmbientNowPlayingCache.clear(sbn.packageName)
        val replacement = activeNotifications
            ?.mapNotNull { parseMediaNotification(it) }
            ?.firstOrNull { it.isPlaying }
            ?: activeNotifications?.mapNotNull { parseMediaNotification(it) }?.firstOrNull()
        if (replacement != null) {
            AmbientNowPlayingCache.update(replacement)
        }
    }

    private fun handleNotification(sbn: StatusBarNotification) {
        val parsed = parseMediaNotification(sbn) ?: return
        AmbientNowPlayingCache.update(parsed)
    }

    private fun parseMediaNotification(sbn: StatusBarNotification): CachedNowPlaying? {
        if (!isMediaNotification(sbn)) return null

        val token = extractMediaSessionToken(sbn.notification) ?: return parseFromExtras(sbn)
        return try {
            val controller = MediaController(this, token)
            val metadata = controller.metadata
            val state = controller.playbackState?.state
            val isPlaying = state == PlaybackState.STATE_PLAYING
            CachedNowPlaying(
                packageName = sbn.packageName,
                title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
                    ?: sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM),
                isPlaying = isPlaying,
            )
        } catch (_: Exception) {
            parseFromExtras(sbn)
        }
    }

    private fun parseFromExtras(sbn: StatusBarNotification): CachedNowPlaying? {
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val artist = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        if (title.isNullOrBlank() && artist.isNullOrBlank()) return null
        return CachedNowPlaying(
            packageName = sbn.packageName,
            title = title,
            artist = artist,
            album = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            isPlaying = sbn.notification.flags and Notification.FLAG_ONGOING_EVENT != 0,
        )
    }

    private fun isMediaNotification(sbn: StatusBarNotification): Boolean {
        val notification = sbn.notification
        if (notification.category == Notification.CATEGORY_TRANSPORT) return true
        return extractMediaSessionToken(notification) != null
    }

    private fun extractMediaSessionToken(notification: Notification): MediaSession.Token? {
        val extras = notification.extras
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(Notification.EXTRA_MEDIA_SESSION) as? MediaSession.Token
        }
    }
}
