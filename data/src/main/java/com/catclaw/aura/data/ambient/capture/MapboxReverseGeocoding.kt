package com.catclaw.aura.data.ambient.capture

import android.util.Log
import com.catclaw.aura.data.BuildConfig
import com.catclaw.aura.data.network.NetworkClient
import com.catclaw.aura.data.network.config.NetworkConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Resolves coordinates to a precise place label (POI or street address).
 *
 * - Primary: Mapbox Search Box `/reverse` (POI + address; Geocoding v5 no longer returns POI).
 * - Fallback: Geocoding v5 `types=address` for rooftop/street-level address when Search Box is empty.
 */
class MapboxReverseGeocoding {

    data class Result(
        val placeName: String?,
        /** e.g. poi, address, street — for prompts and debugging. */
        val featureType: String?,
        val errorMessage: String?,
    )

    suspend fun resolve(latitude: Double, longitude: Double): Result = withContext(Dispatchers.IO) {
        val token = BuildConfig.MAPBOX_ACCESS_TOKEN.trim()
        if (token.isEmpty()) {
            return@withContext Result(
                placeName = null,
                featureType = null,
                errorMessage = "未配置 MAPBOX_ACCESS_TOKEN",
            )
        }
        if (!NetworkClient.isInitialized) {
            return@withContext Result(
                placeName = null,
                featureType = null,
                errorMessage = "NetworkClient 未初始化",
            )
        }
        try {
            val encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8.name())
            searchBoxReverse(longitude, latitude, encodedToken)
                ?: geocodingAddressFallback(longitude, latitude, encodedToken)
                ?: Result(placeName = null, featureType = null, errorMessage = "附近未找到 POI 或门牌地址")
        } catch (e: Exception) {
            val message = e.message ?: "逆地理编码失败"
            Log.w(TAG, "Place lookup failed: $message", e)
            Result(placeName = null, featureType = null, errorMessage = message)
        }
    }

    private fun searchBoxReverse(
        longitude: Double,
        latitude: Double,
        encodedToken: String,
    ): Result? {
        val path = buildString {
            append("search/searchbox/v1/reverse")
            append("?longitude=$longitude")
            append("&latitude=$latitude")
            append("&language=zh")
            append("&limit=5")
            append("&access_token=$encodedToken")
        }
        val body = httpGet(path) ?: return null
        val picked = pickBestSearchBoxFeature(body) ?: return null
        Log.i(TAG, "Search Box reverse: type=${picked.featureType}, label=${picked.label}")
        return Result(
            placeName = picked.label,
            featureType = picked.featureType,
            errorMessage = null,
        )
    }

    private fun geocodingAddressFallback(
        longitude: Double,
        latitude: Double,
        encodedToken: String,
    ): Result? {
        val path =
            "geocoding/v5/mapbox.places/$longitude,$latitude.json" +
                "?language=zh&limit=1&types=address&access_token=$encodedToken"
        val body = httpGet(path) ?: return null
        val features = JSONObject(body).optJSONArray("features") ?: return null
        if (features.length() == 0) return null
        val feature = features.getJSONObject(0)
        val label = feature.optString("place_name").takeIf { it.isNotBlank() }
            ?: feature.optString("text").takeIf { it.isNotBlank() }
            ?: return null
        Log.i(TAG, "Geocoding address fallback: $label")
        return Result(placeName = label, featureType = "address", errorMessage = null)
    }

    private fun httpGet(path: String): String? {
        val url = NetworkClient.resolveUrl(NetworkConstants.BASE_URL_MAPBOX_API, path)
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .build()
        NetworkClient.okHttpClient().newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = parseApiError(body, response.code)
                Log.w(TAG, "HTTP ${response.code} for $path: $message")
                return null
            }
            return body
        }
    }

    private fun pickBestSearchBoxFeature(json: String): PickedFeature? {
        val features = JSONObject(json).optJSONArray("features") ?: return null
        var best: PickedFeature? = null
        for (i in 0 until features.length()) {
            val props = features.getJSONObject(i).optJSONObject("properties") ?: continue
            val type = props.optString("feature_type")
            val rank = FEATURE_PRIORITY.indexOf(type).takeIf { it >= 0 } ?: FEATURE_PRIORITY.size
            val label = formatSearchBoxLabel(props) ?: continue
            val combinedRank = rank * 10 + addressAccuracyRank(props)
            if (best == null || combinedRank < best.combinedRank) {
                best = PickedFeature(type, label, combinedRank)
            }
        }
        return best
    }

    private fun formatSearchBoxLabel(props: JSONObject): String? {
        val type = props.optString("feature_type")
        val name = props.optString("name_preferred")
            .ifBlank { props.optString("name") }
            .trim()
        val fullAddress = props.optString("full_address").trim()
        val address = props.optString("address").trim()
        val placeFormatted = props.optString("place_formatted").trim()
        return when (type) {
            "poi" -> when {
                name.isNotBlank() && fullAddress.isNotBlank() -> "$name（$fullAddress）"
                name.isNotBlank() && address.isNotBlank() -> "$name（$address）"
                name.isNotBlank() -> name
                fullAddress.isNotBlank() -> fullAddress
                else -> null
            }
            "address" -> fullAddress.ifBlank {
                listOfNotNull(
                    address.takeIf { it.isNotBlank() },
                    placeFormatted.takeIf { it.isNotBlank() },
                ).joinToString("，").ifBlank { name }
            }
            "street" -> when {
                name.isNotBlank() && placeFormatted.isNotBlank() -> "$name，$placeFormatted"
                name.isNotBlank() -> name
                fullAddress.isNotBlank() -> fullAddress
                else -> null
            }
            else -> fullAddress.ifBlank { name.ifBlank { null } }
        }
    }

    /** Lower is better (rooftop / parcel preferred for buildings). */
    private fun addressAccuracyRank(props: JSONObject): Int {
        val accuracy = props.optJSONObject("coordinates")?.optString("accuracy").orEmpty()
        return when (accuracy) {
            "rooftop" -> 0
            "parcel" -> 1
            "point" -> 2
            "interpolated" -> 3
            "intersection" -> 4
            "street" -> 5
            "approximate" -> 6
            else -> if (props.optString("feature_type") == "poi") 0 else 7
        }
    }

    private fun parseApiError(body: String, code: Int): String {
        return runCatching {
            JSONObject(body).optString("message").ifBlank { null }
        }.getOrNull() ?: "HTTP $code"
    }

    private data class PickedFeature(
        val featureType: String,
        val label: String,
        val combinedRank: Int,
    )

    private companion object {
        const val TAG = "MapboxReverseGeocoding"

        /** Prefer POI, then door number, then street. */
        val FEATURE_PRIORITY = listOf(
            "poi",
            "address",
            "street",
            "neighborhood",
            "locality",
            "place",
        )
    }
}
