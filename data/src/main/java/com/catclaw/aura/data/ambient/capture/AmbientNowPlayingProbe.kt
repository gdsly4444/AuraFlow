package com.catclaw.aura.data.ambient.capture

import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.catclaw.aura.domain.model.NowPlayingInfo

/**
 * Resolves now-playing metadata via notification listener + media sessions, with
 * [AudioManager.isMusicActive] as fallback when details are unavailable.
 */
class AmbientNowPlayingProbe(
    private val context: Context,
) {

    fun probe(): NowPlayingInfo {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isMusicActive = audioManager.isMusicActive
        val listenerEnabled = NotificationListenerAccess.isEnabled(context)

        if (!listenerEnabled) {
            return buildWithoutListenerAccess(isMusicActive)
        }

        fromActiveSessions()?.let { return it }
        fromNotificationCache(isMusicActive)?.let { return it }

        return if (isMusicActive) {
            NowPlayingInfo(
                isMusicActive = true,
                statusMessage = "检测到音乐播放，但暂无曲目缓存（请确认音乐 App 已显示播放通知）",
            )
        } else {
            NowPlayingInfo(
                isMusicActive = false,
                statusMessage = "当前未检测到音乐播放",
            )
        }
    }

    private fun buildWithoutListenerAccess(isMusicActive: Boolean): NowPlayingInfo {
        return if (isMusicActive) {
            NowPlayingInfo(
                isMusicActive = true,
                statusMessage = "检测到音乐播放；请在系统设置中为 Aura 开启「通知使用权」以读取曲目",
            )
        } else {
            NowPlayingInfo(
                isMusicActive = false,
                statusMessage = "未检测到音乐；读取曲目需在设置中开启 Aura 的「通知使用权」",
            )
        }
    }

    private fun fromActiveSessions(): NowPlayingInfo? {
        val sessionManager =
            context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val componentName = NotificationListenerAccess.componentName(context)
        return try {
            val controllers = sessionManager.getActiveSessions(componentName)
            val playing = controllers.firstOrNull { controller ->
                controller.playbackState?.state == PlaybackState.STATE_PLAYING
            } ?: controllers.firstOrNull() ?: return null

            val metadata = playing.metadata
            val sessionPlaying =
                playing.playbackState?.state == PlaybackState.STATE_PLAYING
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            NowPlayingInfo(
                isMusicActive = sessionPlaying || audioManager.isMusicActive,
                title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE),
                artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST),
                album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM),
                packageName = playing.packageName,
                statusMessage = "已从媒体会话读取",
            )
        } catch (_: SecurityException) {
            null
        }
    }

    private fun fromNotificationCache(isMusicActive: Boolean): NowPlayingInfo? {
        val cached = AmbientNowPlayingCache.get() ?: return null
        return NowPlayingInfo(
            isMusicActive = isMusicActive || cached.isPlaying,
            title = cached.title,
            artist = cached.artist,
            album = cached.album,
            packageName = cached.packageName,
            statusMessage = if (cached.isPlaying) {
                "已从播放通知读取"
            } else {
                "已从通知读取（可能已暂停）"
            },
        )
    }
}
