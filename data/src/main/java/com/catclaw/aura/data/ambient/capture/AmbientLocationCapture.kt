package com.catclaw.aura.data.ambient.capture

import android.content.Context
import android.util.Log
import com.catclaw.aura.domain.model.LocationSnapshot
import com.mapbox.common.location.AccuracyLevel
import com.mapbox.common.location.DeviceLocationProvider
import com.mapbox.common.location.DeviceLocationProviderType
import com.mapbox.common.location.GetLocationCallback
import com.mapbox.common.location.IntervalSettings
import com.mapbox.common.location.Location
import com.mapbox.common.location.LocationObserver
import com.mapbox.common.location.LocationProviderRequest
import com.mapbox.common.location.LocationServiceFactory
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Single fix via Mapbox [LocationService], then reverse geocoding on a background thread.
 */
class AmbientLocationCapture(
    context: Context,
) {

    private val reverseGeocoding = MapboxReverseGeocoding(context)

    suspend fun capture(): LocationSnapshot? {
        val fix = withContext(Dispatchers.Main) {
            try {
                captureGpsFix()
            } catch (e: TimeoutCancellationException) {
                null to "定位超时"
            } catch (e: SecurityException) {
                null to "缺少定位权限"
            } catch (e: Exception) {
                null to (e.message ?: "定位失败")
            }
        }
        return when (val snapshot = fix.first) {
            null -> failureSnapshot(fix.second ?: "无法获取当前位置")
            else -> withContext(Dispatchers.IO) { enrichWithAddress(snapshot) }
        }
    }

    private suspend fun captureGpsFix(): Pair<LocationSnapshot?, String?> {
        val location = captureWithMapbox(timeoutMs = 8_000L) ?: return null to null
        val fix = LocationSnapshot(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.horizontalAccuracy?.toFloat(),
            provider = location.source ?: PROVIDER_LABEL,
        )
        Log.i(
            TAG,
            "GPS fix lat=${fix.latitude}, lon=${fix.longitude}, accuracy=${fix.accuracyMeters}m",
        )
        return fix to null
    }

    private suspend fun captureWithMapbox(timeoutMs: Long): Location? {
        val locationService = LocationServiceFactory.getOrCreate()
        val request = LocationProviderRequest.Builder()
            .accuracy(AccuracyLevel.HIGHEST)
            .displacement(0f)
            .interval(
                IntervalSettings.Builder()
                    .interval(0L)
                    .minimumInterval(0L)
                    .maximumInterval(0L)
                    .build(),
            )
            .build()

        val providerResult = locationService.getDeviceLocationProvider(
            DeviceLocationProviderType.ANDROID,
            request,
            /* allowUserDefined = */ false,
        )
        if (!providerResult.isValue) {
            val message = providerResult.error?.message ?: "无法创建 Mapbox 定位提供者"
            throw IllegalStateException(message)
        }
        val provider = providerResult.value!!

        return withTimeout(timeoutMs) {
            getLastLocation(provider) ?: awaitNextLocationUpdate(provider)
        }
    }

    private suspend fun getLastLocation(provider: DeviceLocationProvider): Location? =
        suspendCancellableCoroutine { continuation ->
            val cancelable = provider.getLastLocation(
                GetLocationCallback { location ->
                    continuation.resume(location)
                },
            )
            continuation.invokeOnCancellation { cancelable.cancel() }
        }

    private suspend fun awaitNextLocationUpdate(
        provider: DeviceLocationProvider,
    ): Location? = suspendCancellableCoroutine { continuation ->
        val observer = object : LocationObserver {
            override fun onLocationUpdateReceived(locations: MutableList<Location>) {
                val fix = locations.firstOrNull() ?: return
                provider.removeLocationObserver(this)
                continuation.resume(fix)
            }
        }
        provider.addLocationObserver(observer)
        continuation.invokeOnCancellation {
            provider.removeLocationObserver(observer)
        }
        provider.getLastLocation(GetLocationCallback { })
    }

    private suspend fun enrichWithAddress(fix: LocationSnapshot): LocationSnapshot {
        val geo = reverseGeocoding.resolve(fix.latitude, fix.longitude)
        return fix.copy(
            placeName = geo.placeName,
            placeFeatureType = geo.featureType,
            geocodingError = geo.errorMessage,
        )
    }

    private fun failureSnapshot(message: String): LocationSnapshot =
        LocationSnapshot(
            latitude = 0.0,
            longitude = 0.0,
            accuracyMeters = null,
            provider = null,
            errorMessage = message,
        )

    private companion object {
        const val TAG = "AmbientLocationCapture"
        const val PROVIDER_LABEL = "mapbox-android"
    }
}
