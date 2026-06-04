package com.catclaw.aura.domain.usecase

import com.catclaw.aura.domain.repository.MomentCardRepository

class DeleteMomentCardUseCase(
    private val cardRepository: MomentCardRepository,
) {
    suspend operator fun invoke(cardId: String) {
        cardRepository.delete(cardId)
    }
}
