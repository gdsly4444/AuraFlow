package com.catclaw.aura.data.scenedescription.organization

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.catclaw.aura.data.scenedescription.config.SceneDescriptionConfig
import com.catclaw.aura.data.scenedescription.config.SceneDescriptionMediaPreference
import com.catclaw.aura.data.scenedescription.model.EncodedMediaKind
import com.catclaw.aura.data.scenedescription.model.EncodedSceneMedia
import com.catclaw.aura.data.scenedescription.model.EncodedSceneMediaSet
import com.catclaw.aura.data.scenedescription.model.SceneCapturePayload
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

class SceneMediaEncoder(
    private val contentResolver: ContentResolver,
    private val config: SceneDescriptionConfig = SceneDescriptionConfig(),
) {

    fun encode(payload: SceneCapturePayload): EncodedSceneMediaSet {
        val skipped = mutableListOf<String>()
        val selected = when (config.mediaPreference) {
            SceneDescriptionMediaPreference.POSTER_FIRST -> {
                encodePoster(payload)
                    ?: encodeVideo(payload, skipped)
                    ?: encodeAudio(payload, skipped)
            }
            SceneDescriptionMediaPreference.VIDEO_FIRST -> {
                encodeVideo(payload, skipped)
                    ?: encodePoster(payload)
                    ?: encodeAudio(payload, skipped)
            }
            SceneDescriptionMediaPreference.AUDIO_FIRST -> {
                encodeAudio(payload, skipped)
                    ?: encodePoster(payload)
                    ?: encodeVideo(payload, skipped)
            }
        }
        if (selected == null) {
            skipped.add("no_attachable_media")
        }
        return EncodedSceneMediaSet(selected = selected, skippedNotes = skipped)
    }

    private fun encodePoster(payload: SceneCapturePayload): EncodedSceneMedia? {
        val uri = payload.video.posterUri ?: return null
        return try {
            val bytes = compressPoster(uri)
            if (bytes.size.toLong() > config.maxRawMediaBytes) {
                return null
            }
            toEncodedMedia(EncodedMediaKind.POSTER, "image/jpeg", bytes)
        } catch (e: IOException) {
            null
        }
    }

    private fun encodeVideo(
        payload: SceneCapturePayload,
        skipped: MutableList<String>,
    ): EncodedSceneMedia? {
        if (!payload.video.isSuccess) return null
        val uri = payload.video.clipUri ?: return null
        return encodeUri(uri, EncodedMediaKind.VIDEO, "video/mp4") { skipped.add(it) }
    }

    private fun encodeAudio(
        payload: SceneCapturePayload,
        skipped: MutableList<String>,
    ): EncodedSceneMedia? {
        if (!payload.audio.isSuccess) return null
        val uri = payload.audio.uri ?: return null
        return encodeUri(uri, EncodedMediaKind.AUDIO, "audio/mp4") { skipped.add(it) }
    }

    private fun compressPoster(uri: Uri): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        } ?: throw IOException("Cannot open $uri")
        val sampleSize = calculateSampleSize(
            bounds.outWidth,
            bounds.outHeight,
            config.posterMaxEdgePx,
        )
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: throw IOException("Cannot decode $uri")
        val scaled = scaleToMaxEdge(bitmap, config.posterMaxEdgePx)
        if (scaled !== bitmap) {
            bitmap.recycle()
        }
        return ByteArrayOutputStream().use { stream ->
            scaled.compress(Bitmap.CompressFormat.JPEG, config.posterJpegQuality, stream)
            if (scaled !== bitmap) {
                scaled.recycle()
            }
            stream.toByteArray()
        }
    }

    private fun encodeUri(
        uri: Uri,
        kind: EncodedMediaKind,
        mimeType: String,
        onSkip: (String) -> Unit,
    ): EncodedSceneMedia? {
        return try {
            val bytes = readBytes(uri)
            if (bytes.size.toLong() > config.maxRawMediaBytes) {
                onSkip("${kind.name.lowercase()}: file too large (${bytes.size} bytes)")
                return null
            }
            toEncodedMedia(kind, mimeType, bytes)
        } catch (e: IOException) {
            onSkip("${kind.name.lowercase()}: ${e.message ?: "read failed"}")
            null
        }
    }

    private fun toEncodedMedia(
        kind: EncodedMediaKind,
        mimeType: String,
        bytes: ByteArray,
    ): EncodedSceneMedia {
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return EncodedSceneMedia(
            kind = kind,
            mimeType = mimeType,
            dataUri = "data:$mimeType;base64,$base64",
        )
    }

    private fun readBytes(uri: Uri): ByteArray =
        contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IOException("Cannot open $uri")

    private fun calculateSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sampleSize = 1
        var w = width
        var h = height
        while (max(w, h) > maxEdge * 2) {
            sampleSize *= 2
            w /= 2
            h /= 2
        }
        return sampleSize
    }

    private fun scaleToMaxEdge(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / longest
        val targetW = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val targetH = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
