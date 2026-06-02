package com.catclaw.aura.ui.map

import com.catclaw.aura.ui.base.BaseViewModel

/**
 * ViewModel for [MapFragment]. Holds initial camera configuration and future map business logic.
 */
class MapViewModel : BaseViewModel<MapUiState, MapUiEvent>(MapUiState()) {

    fun onMapReady() {
        // Hook for analytics, style loading callbacks, etc.
    }
}
