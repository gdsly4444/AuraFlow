package com.catclaw.aura.data.moment

import com.catclaw.aura.data.moment.local.MomentCardDao
import com.catclaw.aura.data.moment.local.toDomain
import com.catclaw.aura.data.moment.local.toEntity
import com.catclaw.aura.data.moment.model.MomentCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MomentCardRepository(
    private val dao: MomentCardDao,
    private val mediaArchiver: MomentMediaArchiver,
) {
    fun observeCards(): Flow<List<MomentCard>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getCard(id: String): MomentCard? =
        dao.getById(id)?.toDomain()

    suspend fun save(card: MomentCard) {
        dao.insert(card.toEntity())
    }

    suspend fun delete(cardId: String) {
        dao.deleteById(cardId)
        mediaArchiver.deleteArchive(cardId)
    }
}
