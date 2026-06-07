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
import com.catclaw.aura.data.network.interceptor.KekeHttpCall
import com.catclaw.aura.domain.usecase.RefreshMomentListUseCase
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    observeHomeListUseCase: ObserveHomeListUseCase,
    observeGeneratingStatusUseCase: ObserveGeneratingStatusUseCase,
    private val deleteMomentCardUseCase: DeleteMomentCardUseCase,
    private val refreshMomentListUseCase: RefreshMomentListUseCase,
) : ViewModel() {

    init {
        viewModelScope.launch(Dispatchers.IO) {
            refreshMomentListUseCase()
                .onSuccess { count ->
                    Log.i(KekeHttpCall.TAG, "HomeViewModel refresh done count=$count")
                }
                .onFailure { e ->
                    Log.e(KekeHttpCall.TAG, "HomeViewModel refresh failed", e)
                }
        }
    }

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
                container.refreshMomentListUseCase,
            ) as T
        }
    }
}
