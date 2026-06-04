package com.catclaw.aura.data.moment.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MomentCardDao {

    @Query("SELECT * FROM moment_cards ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<MomentCardEntity>>

    @Query("SELECT * FROM moment_cards WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): MomentCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MomentCardEntity)

    @Query("DELETE FROM moment_cards WHERE id = :id")
    suspend fun deleteById(id: String)
}
