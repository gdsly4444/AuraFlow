package com.catclaw.aura.data.ambient.capture

import java.util.concurrent.atomic.AtomicReference

/**
 * In-memory snapshot updated by [AmbientMediaNotificationListener].
 */
object AmbientNowPlayingCache {

    private val snapshot = AtomicReference<CachedNowPlaying?>(null)

    fun update(entry: CachedNowPlaying) {
        snapshot.set(entry)
    }

    fun clear(packageName: String) {
        val current = snapshot.get() ?: return
        if (current.packageName == packageName) {
            snapshot.set(null)
        }
    }

    fun get(maxAgeMs: Long = DEFAULT_MAX_AGE_MS): CachedNowPlaying? {
        val entry = snapshot.get() ?: return null
        if (System.currentTimeMillis() - entry.updatedAtMs > maxAgeMs) return null
        return entry
    }

    data class CachedNowPlaying(
        val packageName: String,
        val title: String?,
        val artist: String?,
        val album: String?,
        val isPlaying: Boolean,
        val updatedAtMs: Long = System.currentTimeMillis(),
    )

    private const val DEFAULT_MAX_AGE_MS = 60_000L
}
