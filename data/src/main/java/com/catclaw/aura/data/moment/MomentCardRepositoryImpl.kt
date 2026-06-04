package com.catclaw.aura.data.moment

import com.catclaw.aura.data.moment.local.MomentCardDao
import com.catclaw.aura.data.moment.local.toDomain
import com.catclaw.aura.data.moment.local.toEntity
import com.catclaw.aura.domain.model.MomentCard
import com.catclaw.aura.domain.repository.MomentCardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MomentCardRepositoryImpl(
    private val dao: MomentCardDao,
    private val mediaArchiver: MomentMediaArchiver,
) : MomentCardRepository {

    override fun observeCards(): Flow<List<MomentCard>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getCard(id: String): MomentCard? =
        dao.getById(id)?.toDomain()

    override suspend fun save(card: MomentCard) {
        dao.insert(card.toEntity())
    }

    override suspend fun delete(cardId: String) {
        dao.deleteById(cardId)
        mediaArchiver.deleteArchive(cardId)
    }
}
