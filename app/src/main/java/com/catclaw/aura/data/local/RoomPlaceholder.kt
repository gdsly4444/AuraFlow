package com.catclaw.aura.data.local

/**
 * Room local persistence layer.
 *
 * Dependencies: `room-runtime`, `room-ktx` (add `room-compiler` via KSP when entities exist).
 * When you add entities, create:
 *
 * ```
 * @Entity data class ...
 * @Dao interface ...
 * @Database(entities = [...], version = 1)
 * abstract class AppDatabase : RoomDatabase()
 * ```
 *
 * in this package and expose it via a repository.
 */
