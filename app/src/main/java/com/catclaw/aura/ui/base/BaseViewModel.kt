package com.catclaw.aura.ui.base

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Base [ViewModel] exposing [StateFlow] for UI state and [SharedFlow] for one-off events.
 */
abstract class BaseViewModel<UiState, UiEvent>(initialState: UiState) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    protected fun updateState(reducer: (UiState) -> UiState) {
        _uiState.value = reducer(_uiState.value)
    }

    protected fun sendEvent(event: UiEvent) {
        _uiEvent.tryEmit(event)
    }
}
