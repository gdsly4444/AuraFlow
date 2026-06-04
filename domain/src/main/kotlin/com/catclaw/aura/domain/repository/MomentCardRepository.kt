package com.catclaw.aura.domain.repository

import com.catclaw.aura.domain.model.MomentCard
import kotlinx.coroutines.flow.Flow

interface MomentCardRepository {
    fun observeCards(): Flow<List<MomentCard>>
    suspend fun getCard(id: String): MomentCard?
    suspend fun save(card: MomentCard)
    suspend fun delete(cardId: String)
}
