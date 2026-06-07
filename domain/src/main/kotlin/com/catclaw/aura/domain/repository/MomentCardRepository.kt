package com.catclaw.aura.domain.repository

import com.catclaw.aura.domain.model.MomentCard
import kotlinx.coroutines.flow.Flow

interface MomentCardRepository {
    fun observeCards(): Flow<List<MomentCard>>
    suspend fun getCard(id: String): MomentCard?
    suspend fun save(card: MomentCard)
    suspend fun delete(cardId: String)
    suspend fun refreshFromServer(page: Int = 1, pageSize: Int = 10): Result<Int>
    suspend fun enrichWithLocalPlayback(card: MomentCard): MomentCard
}
