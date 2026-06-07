package com.catclaw.aura.data.aura.media

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

class CaptureUploadFile(
    private val context: Context,
) {
    private val appContext = context.applicationContext

    fun resolve(uriString: String?, fallbackName: String): File? {
        if (uriString.isNullOrBlank()) return null
        val uri = Uri.parse(uriString)
        return when (uri.scheme) {
            "file" -> uri.path?.let { File(it) }?.takeIf { it.exists() && it.isFile }
            else -> copyToTemp(uri, fallbackName)
        }
    }

    fun deleteIfLocal(uriString: String?) {
        if (uriString.isNullOrBlank()) return
        val uri = Uri.parse(uriString)
        if (uri.scheme == "file") {
            uri.path?.let { File(it).delete() }
        }
    }

    private fun copyToTemp(uri: Uri, fallbackName: String): File? {
        val input = appContext.contentResolver.openInputStream(uri) ?: return null
        val dest = File(appContext.cacheDir, "upload_${System.currentTimeMillis()}_$fallbackName")
        return try {
            input.use { inStream ->
                FileOutputStream(dest).use { out -> inStream.copyTo(out) }
            }
            dest
        } catch (_: Exception) {
            dest.delete()
            null
        }
    }
}
