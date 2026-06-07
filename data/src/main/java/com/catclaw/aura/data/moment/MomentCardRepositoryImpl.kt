package com.catclaw.aura.data.moment

import android.util.Log
import com.catclaw.aura.data.aura.AuraUserIdStore
import com.catclaw.aura.data.network.interceptor.KekeHttpCall
import com.catclaw.aura.data.aura.media.AuraMediaDownloader
import com.catclaw.aura.data.aura.remote.AuraApiRemote
import com.catclaw.aura.data.aura.remote.AuraRecordMapper
import com.catclaw.aura.data.moment.local.MomentCardDao
import com.catclaw.aura.data.moment.local.toDomain
import com.catclaw.aura.data.moment.local.toEntity
import com.catclaw.aura.domain.model.MomentCard
import com.catclaw.aura.domain.repository.MomentCardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class MomentCardRepositoryImpl(
    private val dao: MomentCardDao,
    private val mediaArchiver: MomentMediaArchiver,
    private val auraApi: AuraApiRemote,
    private val userIdStore: AuraUserIdStore,
    private val mediaDownloader: AuraMediaDownloader,
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
        mediaDownloader.deleteCache(cardId)
    }

    override suspend fun refreshFromServer(page: Int, pageSize: Int): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!auraApi.isConfigured) {
                    Log.w(KekeHttpCall.TAG, "refreshFromServer skipped: Aura API not configured")
                    return@runCatching 0
                }
                val userId = userIdStore.getUserId()
                val response = auraApi.fetchRecords(userId, page, pageSize)
                val entities = response.items.map { item ->
                    val mapped = AuraRecordMapper.fromListItem(item)
                    val merged = dao.getById(item.recordId)?.toDomain()?.let { existing ->
                        mapped.copy(
                            locationPlaceName = mapped.locationPlaceName ?: existing.locationPlaceName,
                            latitude = mapped.latitude ?: existing.latitude,
                            longitude = mapped.longitude ?: existing.longitude,
                            locationAccuracyMeters = mapped.locationAccuracyMeters ?: existing.locationAccuracyMeters,
                            locationProvider = mapped.locationProvider ?: existing.locationProvider,
                            musicTitle = mapped.musicTitle ?: existing.musicTitle,
                            musicArtist = mapped.musicArtist ?: existing.musicArtist,
                        )
                    } ?: mapped
                    merged.toEntity()
                }
                if (page == 1) {
                    dao.replaceAll(entities)
                } else if (entities.isNotEmpty()) {
                    dao.insertAll(entities)
                }
                Log.i(KekeHttpCall.TAG, "refreshFromServer ok items=${response.items.size} total=${response.total}")
                response.items.size
            }.onFailure { e ->
                Log.e(KekeHttpCall.TAG, "refreshFromServer failed", e)
            }
        }

    override suspend fun enrichWithLocalPlayback(card: MomentCard): MomentCard =
        withContext(Dispatchers.IO) {
            mediaDownloader.enrichWithLocalPlayback(card)
        }
}
