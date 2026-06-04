package com.catclaw.aura.ui.moment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.catclaw.aura.AuraApplication
import com.catclaw.aura.data.moment.model.MomentCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MomentDetailViewModel(
    application: Application,
    private val cardId: String,
) : AndroidViewModel(application) {

    private val _card = MutableStateFlow<MomentCard?>(null)
    val card: StateFlow<MomentCard?> = _card.asStateFlow()

    init {
        viewModelScope.launch {
            _card.value = (application as AuraApplication).momentCardRepository.getCard(cardId)
        }
    }

    companion object {
        const val CARD_ID_ARG = "card_id"
    }
}
