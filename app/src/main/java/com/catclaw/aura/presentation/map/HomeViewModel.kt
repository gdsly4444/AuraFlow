package com.catclaw.aura.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.catclaw.aura.di.AppContainer
import com.catclaw.aura.domain.model.HomeGeneratingStatus
import com.catclaw.aura.domain.model.HomeListEntry
import com.catclaw.aura.domain.usecase.DeleteMomentCardUseCase
import com.catclaw.aura.domain.usecase.ObserveGeneratingStatusUseCase
import com.catclaw.aura.domain.usecase.ObserveHomeListUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    observeHomeListUseCase: ObserveHomeListUseCase,
    observeGeneratingStatusUseCase: ObserveGeneratingStatusUseCase,
    private val deleteMomentCardUseCase: DeleteMomentCardUseCase,
) : ViewModel() {

    val generatingStatus: StateFlow<HomeGeneratingStatus?> =
        observeGeneratingStatusUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null,
            )

    val listItems: StateFlow<List<HomeListEntry>> =
        observeHomeListUseCase()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    fun deleteCard(cardId: String) {
        viewModelScope.launch {
            deleteMomentCardUseCase(cardId)
        }
    }

    class Factory(
        private val container: AppContainer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(HomeViewModel::class.java))
            return HomeViewModel(
                container.observeHomeListUseCase,
                container.observeGeneratingStatusUseCase,
                container.deleteMomentCardUseCase,
            ) as T
        }
    }
}
