package com.catclaw.aura.data.aura

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.auraUserDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "aura_user",
)

class AuraUserIdStore(
    context: Context,
) {
    private val dataStore = context.applicationContext.auraUserDataStore

    private val userIdKey = stringPreferencesKey("user_id")

    suspend fun getUserId(): String {
        val existing = dataStore.data.map { prefs -> prefs[userIdKey] }.first()
        if (!existing.isNullOrBlank()) return existing
        val created = UUID.randomUUID().toString()
        dataStore.edit { prefs -> prefs[userIdKey] = created }
        return created
    }

    /** Blocking read for workflow threads that already run off the main thread. */
    fun getUserIdBlocking(): String = runBlocking { getUserId() }
}
