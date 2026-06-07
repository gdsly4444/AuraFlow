package com.catclaw.aura.domain.usecase

import com.catclaw.aura.domain.repository.MomentCardRepository

class RefreshMomentListUseCase(
    private val cardRepository: MomentCardRepository,
) {
    suspend operator fun invoke(page: Int = 1, pageSize: Int = 10): Result<Int> =
        cardRepository.refreshFromServer(page, pageSize)
}
