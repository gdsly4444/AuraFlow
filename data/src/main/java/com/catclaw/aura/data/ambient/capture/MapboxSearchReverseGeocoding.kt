package com.catclaw.aura.data.ambient.capture

import android.content.Context
import android.util.Log
import com.catclaw.aura.data.BuildConfig
import com.mapbox.geojson.Point
import com.mapbox.search.ApiType
import com.mapbox.search.NewQueryType
import com.mapbox.search.ResponseInfo
import com.mapbox.search.ReverseGeoOptions
import com.mapbox.search.SearchCallback
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.mapbox.search.common.IsoCountryCode
import com.mapbox.search.common.IsoLanguageCode
import com.mapbox.search.result.SearchResult
import kotlin.coroutines.resume
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Reverse geocoding via Mapbox Search SDK (Search Box API).
 */
class MapboxSearchReverseGeocoding(
    @Suppress("UNUSED_PARAMETER") context: Context,
) {
    private val searchEngine: SearchEngine = SearchEngine.createSearchEngineWithBuiltInDataProviders(
        ApiType.SEARCH_BOX,
        SearchEngineSettings(),
    )

    suspend fun resolvePrecise(latitude: Double, longitude: Double): MapboxReverseGeocoding.Result? {
        if (BuildConfig.MAPBOX_ACCESS_TOKEN.isBlank()) return null
        Log.i(TAG, "SDK precise reverse at lat=$latitude, lon=$longitude")
        return coroutineScope {
            PRECISION_QUERY_TYPES
                .map { queryType ->
                    async {
                        val results = searchByType(latitude, longitude, queryType) ?: return@async null
                        logRawResults(queryType, results)
                        pickBestResult(results)
                    }
                }
                .mapNotNull { it.await() }
                .firstOrNull()
        }
    }

    suspend fun resolveCoarseAdmin(
        latitude: Double,
        longitude: Double,
    ): MapboxReverseGeocoding.Result? {
        if (BuildConfig.MAPBOX_ACCESS_TOKEN.isBlank()) return null
        val byType = linkedMapOf<String, String>()
        for (queryType in COARSE_QUERY_TYPES) {
            val results = searchByType(latitude, longitude, queryType) ?: continue
            for (result in results) {
                val name = result.name.trim()
                if (name.isNotBlank()) byType.putIfAbsent(queryType, name)
            }
        }
        val ordered = COARSE_QUERY_TYPES.mapNotNull { byType[it] }
        val label = PlaceLabelHeuristics.composeAdminLabel(ordered) ?: return null
        Log.i(TAG, "SDK coarse admin: $label")
        return MapboxReverseGeocoding.Result(
            placeName = label,
            featureType = "admin",
            errorMessage = null,
        )
    }

    private suspend fun searchByType(
        latitude: Double,
        longitude: Double,
        queryType: String,
    ): List<SearchResult>? = suspendCancellableCoroutine { continuation ->
        val options = ReverseGeoOptions.Builder(Point.fromLngLat(longitude, latitude))
            .countries(listOf(IsoCountryCode.CHINA))
            .languages(listOf(IsoLanguageCode.CHINESE))
            .limit(1)
            .newTypes(listOf(queryType))
            .build()
        val task = searchEngine.search(
            options,
            object : SearchCallback {
                override fun onResults(results: List<SearchResult>, responseInfo: ResponseInfo) {
                    continuation.resume(results)
                }

                override fun onError(e: Exception) {
                    Log.w(TAG, "Search SDK type=$queryType failed: ${e.message}")
                    continuation.resume(null)
                }
            },
        )
        continuation.invokeOnCancellation { task.cancel() }
    }

    private fun pickBestResult(results: List<SearchResult>): MapboxReverseGeocoding.Result? {
        for (result in results) {
            if (isCoarseAdministrativeOnly(result)) continue
            val type = inferFeatureType(result)
            val label = formatLabel(result, type) ?: continue
            if (PlaceLabelHeuristics.isCoarseAdminLabel(label, type)) continue
            Log.i(TAG, "Search SDK picked: type=$type, label=$label")
            return MapboxReverseGeocoding.Result(
                placeName = label,
                featureType = type,
                errorMessage = null,
            )
        }
        return null
    }

    private fun isCoarseAdministrativeOnly(result: SearchResult): Boolean {
        val typeLabel = result.newTypes.joinToString(",") { it.toString().lowercase() }
        val hasFineType = typeLabel.contains("poi") ||
            typeLabel.contains("address") ||
            typeLabel.contains("street")
        if (hasFineType) return false
        return typeLabel.contains("place") ||
            typeLabel.contains("region") ||
            typeLabel.contains("district") ||
            typeLabel.contains("country") ||
            typeLabel.contains("locality") ||
            typeLabel.contains("neighborhood")
    }

    private fun inferFeatureType(result: SearchResult): String {
        val typeLabel = result.newTypes.joinToString(",") { it.toString().lowercase() }
        return when {
            typeLabel.contains("poi") -> "poi"
            typeLabel.contains("address") -> "address"
            typeLabel.contains("street") -> "street"
            else -> "place"
        }
    }

    private fun formatLabel(result: SearchResult, type: String): String? {
        val name = result.name.trim()
        val formatted = result.address?.formattedAddress().orEmpty().trim()
        val street = result.address?.street.orEmpty().trim()
        val house = result.address?.houseNumber.orEmpty().trim()
        val streetLine = listOf(house, street).filter { it.isNotBlank() }.joinToString(" ")
        return when (type) {
            "poi" -> when {
                name.isNotBlank() && formatted.isNotBlank() -> "$name（$formatted）"
                name.isNotBlank() && streetLine.isNotBlank() -> "$name（$streetLine）"
                name.isNotBlank() -> name
                formatted.isNotBlank() -> formatted
                else -> null
            }
            "address", "street" -> formatted.ifBlank {
                streetLine.ifBlank { name.ifBlank { null } }
            }
            else -> formatted.ifBlank { name.ifBlank { null } }
        }
    }

    private fun logRawResults(queryType: String, results: List<SearchResult>) {
        results.forEachIndexed { index, result ->
            Log.d(
                TAG,
                "raw[$queryType][$index] name=${result.name} types=${result.newTypes} " +
                    "formatted=${result.address?.formattedAddress()}",
            )
        }
    }

    private companion object {
        const val TAG = "MapboxSearchReverseGeocoding"

        val PRECISION_QUERY_TYPES = listOf(
            NewQueryType.POI,
            NewQueryType.ADDRESS,
            NewQueryType.STREET,
        )

        val COARSE_QUERY_TYPES = listOf(
            NewQueryType.PLACE,
            NewQueryType.DISTRICT,
            NewQueryType.NEIGHBORHOOD,
        )
    }
}
