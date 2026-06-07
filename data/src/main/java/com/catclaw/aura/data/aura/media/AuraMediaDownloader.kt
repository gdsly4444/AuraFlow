package com.catclaw.aura.data.aura.media

import android.content.Context
import com.catclaw.aura.data.aura.AuraUserIdStore
import com.catclaw.aura.data.aura.remote.AuraApiRemote
import com.catclaw.aura.domain.model.MomentCard
import java.io.File

enum class PlaybackKind(val fileName: String) {
    THUMBNAIL("thumbnail.jpg"),
    VIDEO("clip.mp4"),
    AUDIO("audio.m4a"),
}

class AuraMediaDownloader(
    context: Context,
    private val api: AuraApiRemote,
    private val userIdStore: AuraUserIdStore,
) {
    private val cacheRoot = File(context.applicationContext.filesDir, "playback_cache")

    fun localFile(recordId: String, kind: PlaybackKind): File =
        File(File(cacheRoot, recordId), kind.fileName)

    suspend fun ensureCached(recordId: String, url: String?, kind: PlaybackKind): File? {
        if (url.isNullOrBlank() || !api.isConfigured) return null
        val dest = localFile(recordId, kind)
        if (dest.exists() && dest.length() > 0L) return dest
        val userId = userIdStore.getUserId()
        api.downloadSignedGet(url, userId, dest)
        return dest.takeIf { it.exists() }
    }

    suspend fun enrichWithLocalPlayback(card: MomentCard): MomentCard {
        val poster = ensureCached(card.id, card.thumbnailUrl, PlaybackKind.THUMBNAIL)
        val video = ensureCached(card.id, card.videoUrl, PlaybackKind.VIDEO)
        val audio = ensureCached(card.id, card.audioUrl, PlaybackKind.AUDIO)
        return card.copy(
            posterPath = poster?.absolutePath ?: card.posterPath,
            videoPath = video?.absolutePath ?: card.videoPath,
            audioPath = audio?.absolutePath ?: card.audioPath,
        )
    }

    fun deleteCache(recordId: String) {
        File(cacheRoot, recordId).deleteRecursively()
    }
}
