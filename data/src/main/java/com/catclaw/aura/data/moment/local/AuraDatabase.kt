package com.catclaw.aura.data.moment.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [MomentCardEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AuraDatabase : RoomDatabase() {

    abstract fun momentCardDao(): MomentCardDao

    companion object {
        @Volatile
        private var instance: AuraDatabase? = null

        fun get(context: Context): AuraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AuraDatabase::class.java,
                    "aura.db",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
