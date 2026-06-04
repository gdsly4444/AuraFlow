package com.catclaw.aura.presentation.map

import com.catclaw.aura.presentation.base.BaseViewModel

/**
 * ViewModel for [MapFragment]. Holds initial camera configuration and future map business logic.
 */
class MapViewModel : BaseViewModel<MapUiState, MapUiEvent>(MapUiState()) {

    fun onMapReady() {
        // Hook for analytics, style loading callbacks, etc.
    }
}
