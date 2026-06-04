package com.catclaw.aura.domain.usecase

import com.catclaw.aura.domain.model.MomentCard
import com.catclaw.aura.domain.repository.MomentCardRepository

class GetMomentCardUseCase(
    private val cardRepository: MomentCardRepository,
) {
    suspend operator fun invoke(cardId: String): MomentCard? =
        cardRepository.getCard(cardId)
}
