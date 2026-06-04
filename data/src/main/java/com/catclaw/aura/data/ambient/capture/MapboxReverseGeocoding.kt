package com.catclaw.aura.data.ambient.capture

import android.content.Context
import android.util.Log
import com.catclaw.aura.data.BuildConfig
import com.catclaw.aura.data.network.NetworkClient
import com.catclaw.aura.data.network.config.NetworkConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Resolves coordinates to a place label with a fast parallel lookup strategy.
 */
class MapboxReverseGeocoding(
    context: Context,
) {

    private val searchSdk = MapboxSearchReverseGeocoding(context)

    data class Result(
        val placeName: String?,
        val featureType: String?,
        val errorMessage: String?,
    )

    suspend fun resolve(latitude: Double, longitude: Double): Result =
        withContext(Dispatchers.IO) {
            val token = BuildConfig.MAPBOX_ACCESS_TOKEN.trim()
            if (token.isEmpty()) {
                return@withContext Result(
                    placeName = null,
                    featureType = null,
                    errorMessage = "未配置 MAPBOX_ACCESS_TOKEN",
                )
            }
            try {
                withTimeout(RESOLVE_TIMEOUT_MS) {
                    resolveInternal(latitude, longitude, token)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.w(TAG, "Reverse geocode timed out after ${RESOLVE_TIMEOUT_MS}ms")
                Result(
                    placeName = null,
                    featureType = null,
                    errorMessage = "地址解析超时",
                )
            } catch (e: Exception) {
                val message = e.message ?: "逆地理编码失败"
                Log.w(TAG, "Place lookup failed: $message", e)
                Result(placeName = null, featureType = null, errorMessage = message)
            }
        }

    private suspend fun resolveInternal(
        latitude: Double,
        longitude: Double,
        token: String,
    ): Result = coroutineScope {
        if (!NetworkClient.isInitialized) {
            val sdkOnly = searchSdk.resolvePrecise(latitude, longitude)
                ?: searchSdk.resolveCoarseAdmin(latitude, longitude)
            return@coroutineScope sdkOnly ?: Result(
                placeName = null,
                featureType = null,
                errorMessage = "NetworkClient 未初始化",
            )
        }
        val encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8.name())

        val sdkPrecise = async { searchSdk.resolvePrecise(latitude, longitude) }
        val httpPoi = async { searchBoxReversePoi(longitude, latitude, encodedToken) }
        val httpStreet = async { searchBoxReverseAddressStreet(longitude, latitude, encodedToken) }
        val geoStreet = async { geocodingStreetAddressFallback(longitude, latitude, encodedToken) }

        listOf(sdkPrecise, httpPoi, httpStreet, geoStreet)
            .forEach { deferred ->
                val candidate = deferred.await()
                if (isPreciseResult(candidate)) {
                    return@coroutineScope candidate!!
                }
            }

        val sdkCoarse = async { searchSdk.resolveCoarseAdmin(latitude, longitude) }
        val boxCoarse = async { searchBoxAdminFallback(longitude, latitude, encodedToken) }
        val geoCoarse = async { geocodingAdminFallback(longitude, latitude, encodedToken) }

        listOf(sdkCoarse, boxCoarse, geoCoarse)
            .forEach { deferred ->
                val candidate = deferred.await()
                if (candidate != null && !candidate.placeName.isNullOrBlank()) {
                    return@coroutineScope candidate
                }
            }

        Result(
            placeName = null,
            featureType = null,
            errorMessage = "无法解析地址，请检查网络与定位权限",
        )
    }

    private fun isPreciseResult(result: Result?): Boolean {
        if (result == null || result.placeName.isNullOrBlank()) return false
        return result.featureType != "admin"
    }

    private fun searchBoxReversePoi(
        longitude: Double,
        latitude: Double,
        encodedToken: String,
    ): Result? = searchBoxReverseTyped(longitude, latitude, encodedToken, types = "poi", limit = 3)

    private fun searchBoxReverseAddressStreet(
        longitude: Double,
        latitude: Double,
        encodedToken: String,
    ): Result? = searchBoxReverseTyped(
        longitude,
        latitude,
        encodedToken,
        types = "address,street",
        limit = 3,
    )

    private fun searchBoxReverseTyped(
        longitude: Double,
        latitude: Double,
        encodedToken: String,
        types: String,
        limit: Int,
    ): Result? {
        val path = buildString {
            append("search/searchbox/v1/reverse")
            append("?longitude=$longitude")
            append("&latitude=$latitude")
            append("&language=zh")
            append("&country=cn")
            append("&types=$types")
            append("&limit=$limit")
            append("&access_token=$encodedToken")
        }
        val body = httpGet(path) ?: return null
        val picked = pickBestSearchBoxFeature(body) ?: return null
        Log.i(TAG, "Search Box types=$types → ${picked.label}")
        return Result(
            placeName = picked.label,
            featureType = picked.featureType,
            errorMessage = null,
        )
    }

    private fun searchBoxAdminFallback(
        longitude: Double,
        latitude: Double,
        encodedToken: String,
    ): Result? {
        val path = buildString {
            append("search/searchbox/v1/reverse")
            append("?longitude=$longitude")
            append("&latitude=$latitude")
            append("&language=zh")
            append("&country=cn")
            append("&limit=10")
            append("&access_token=$encodedToken")
        }
        val body = httpGet(path) ?: return null
        val label = pickBestAdminFromSearchBox(body) ?: return null
        Log.i(TAG, "Search Box admin fallback: $label")
        return Result(placeName = label, featureType = "admin", errorMessage = null)
    }

    private fun geocodingAdminFallback(
        longitude: Double,
        latitude: Double,
        encodedToken: String,
    ): Result? {
        val path =
            "geocoding/v5/mapbox.places/$longitude,$latitude.json" +
                "?language=zh&worldview=cn&country=cn&limit=5&access_token=$encodedToken"
        val body = httpGet(path) ?: return null
        val label = pickBestAdminFromGeocoding(body) ?: return null
        Log.i(TAG, "Geocoding admin fallback: $label")
        return Result(placeName = label, featureType = "admin", errorMessage = null)
    }

    private fun geocodingStreetAddressFallback(
        longitude: Double,
        latitude: Double,
        encodedToken: String,
    ): Result? {
        for (types in listOf("address", "street")) {
            val path =
                "geocoding/v5/mapbox.places/$longitude,$latitude.json" +
                    "?language=zh&worldview=cn&country=cn&limit=3&types=$types&access_token=$encodedToken"
            val body = httpGet(path) ?: continue
            val label = pickBestGeocodingFeature(body) ?: continue
            Log.i(TAG, "Geocoding types=$types: $label")
            return Result(placeName = label, featureType = types, errorMessage = null)
        }
        return null
    }

    private fun pickBestGeocodingFeature(json: String): String? {
        val features = JSONObject(json).optJSONArray("features") ?: return null
        var best: String? = null
        var bestRank = Int.MAX_VALUE
        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val label = feature.optString("place_name").takeIf { it.isNotBlank() }
                ?: feature.optString("text").takeIf { it.isNotBlank() }
                ?: continue
            val id = feature.optString("id")
            val type = id.substringBefore('.').ifBlank { "address" }
            if (PlaceLabelHeuristics.isCoarseAdminLabel(label, type)) continue
            val rank = FEATURE_PRIORITY.indexOf(type).takeIf { it >= 0 } ?: FEATURE_PRIORITY.size
            if (rank < bestRank) {
                bestRank = rank
                best = label
            }
        }
        return best
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
            val label = formatSearchBoxLabel(props) ?: continue
            if (PlaceLabelHeuristics.isCoarseAdminLabel(label, type)) continue
            val rank = FEATURE_PRIORITY.indexOf(type).takeIf { it >= 0 } ?: FEATURE_PRIORITY.size
            val combinedRank = rank * 10 + addressAccuracyRank(props)
            if (best == null || combinedRank < best.combinedRank) {
                best = PickedFeature(type, label, combinedRank)
            }
        }
        return best
    }

    private fun pickBestAdminFromSearchBox(json: String): String? {
        val features = JSONObject(json).optJSONArray("features") ?: return null
        var best: String? = null
        for (i in 0 until features.length()) {
            val props = features.getJSONObject(i).optJSONObject("properties") ?: continue
            val label = PlaceLabelHeuristics.composeFromSearchBoxContext(props) ?: continue
            if (best == null || label.length > best.length) best = label
        }
        return best
    }

    private fun pickBestAdminFromGeocoding(json: String): String? {
        val features = JSONObject(json).optJSONArray("features") ?: return null
        var best: String? = null
        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val label = PlaceLabelHeuristics.composeFromGeocodingFeature(feature) ?: continue
            if (best == null || label.length > best.length) best = label
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
        const val RESOLVE_TIMEOUT_MS = 10_000L

        val FEATURE_PRIORITY = listOf(
            "poi",
            "address",
            "street",
        )
    }
}
