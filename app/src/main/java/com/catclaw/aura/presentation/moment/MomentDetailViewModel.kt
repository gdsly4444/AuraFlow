package com.catclaw.aura.presentation.moment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.catclaw.aura.di.AppContainer
import com.catclaw.aura.domain.model.MomentCard
import com.catclaw.aura.domain.repository.MomentCardRepository
import com.catclaw.aura.domain.usecase.GetMomentCardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MomentDetailViewModel(
    private val cardId: String,
    private val getMomentCardUseCase: GetMomentCardUseCase,
    private val momentCardRepository: MomentCardRepository,
) : ViewModel() {

    private val _card = MutableStateFlow<MomentCard?>(null)
    val card: StateFlow<MomentCard?> = _card.asStateFlow()

    init {
        viewModelScope.launch {
            val card = getMomentCardUseCase(cardId) ?: return@launch
            _card.value = momentCardRepository.enrichWithLocalPlayback(card)
        }
    }

    class Factory(
        private val container: AppContainer,
        private val cardId: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(MomentDetailViewModel::class.java))
            return MomentDetailViewModel(
                cardId,
                container.getMomentCardUseCase,
                container.momentCardRepository,
            ) as T
        }
    }

    companion object {
        const val CARD_ID_ARG = "card_id"
    }
}
