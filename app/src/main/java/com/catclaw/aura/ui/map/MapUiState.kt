package com.catclaw.aura.ui.map

/**
 * UI state for the map screen. Camera fields are consumed when [MapFragment] creates [com.mapbox.maps.MapView].
 */
data class MapUiState(
    val centerLongitude: Double = DEFAULT_CENTER_LONGITUDE,
    val centerLatitude: Double = DEFAULT_CENTER_LATITUDE,
    val zoom: Double = DEFAULT_ZOOM,
    val pitch: Double = 0.0,
    val bearing: Double = 0.0,
) {
    companion object {
        const val DEFAULT_CENTER_LONGITUDE = -98.0
        const val DEFAULT_CENTER_LATITUDE = 39.5
        const val DEFAULT_ZOOM = 2.0
    }
}
