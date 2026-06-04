package com.catclaw.aura.data.moment

import android.content.Context
import android.net.Uri
import com.catclaw.aura.data.moment.model.MomentCaptureSnapshot
import java.io.File
import java.io.FileInputStream
import java.io.IOException

data class ArchivedMomentMedia(
    val cardId: String,
    val posterPath: String?,
    val videoPath: String?,
    val audioPath: String?,
)

class MomentMediaArchiver(
    context: Context,
) {
    private val appContext = context.applicationContext

    fun archive(snapshot: MomentCaptureSnapshot): ArchivedMomentMedia {
        val dir = File(appContext.filesDir, "moments/${snapshot.workflowId}")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return ArchivedMomentMedia(
            cardId = snapshot.workflowId,
            posterPath = copyTo(snapshot.posterUri, File(dir, "poster.jpg")),
            videoPath = copyTo(snapshot.videoUri, File(dir, "clip.mp4")),
            audioPath = copyTo(snapshot.audioUri, File(dir, "audio.m4a")),
        )
    }

    /**
     * Copies [sourceUriString] when present. Returns null if source is missing or unreadable
     * (partial archive is allowed).
     */
    private fun copyTo(sourceUriString: String?, dest: File): String? {
        if (sourceUriString.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(sourceUriString)
            openInput(uri)?.use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null
            dest.absolutePath
        } catch (_: IOException) {
            null
        }
    }

    private fun openInput(uri: Uri) = when (uri.scheme) {
        "file" -> uri.path?.let { FileInputStream(File(it)) }
        else -> appContext.contentResolver.openInputStream(uri)
    }

    fun deleteArchive(workflowId: String) {
        File(appContext.filesDir, "moments/$workflowId").deleteRecursively()
    }
}
