package app.campfire.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

/**
 * The little the fire keeps between evenings: whether the welcome has been seen, which sparks
 * have already flown (so questions don't repeat until a pool is spent), and the sealed ember —
 * the question that waits for the next lighting, and the day it may be opened on.
 */
data class FireSettings(
    val onboarded: Boolean = false,
    val usedIds: Set<Int> = emptySet(),
    val emberId: Int = 0,
    /** The day the ember may be opened, as epoch millis at midnight. 0: no ember waiting. */
    val emberFrom: Long = 0,
)

class SettingsStore(private val context: Context) {
    private val onboardedKey = booleanPreferencesKey("onboarded")
    private val usedKey = stringPreferencesKey("usedQuestionIds")
    private val emberIdKey = intPreferencesKey("emberId")
    private val emberFromKey = longPreferencesKey("emberFrom")

    val settings: Flow<FireSettings> = context.dataStore.data.map { p ->
        FireSettings(
            onboarded = p[onboardedKey] ?: false,
            usedIds = (p[usedKey] ?: "").split(",").filter { it.isNotBlank() }.map { it.toInt() }.toSet(),
            emberId = p[emberIdKey] ?: 0,
            emberFrom = p[emberFromKey] ?: 0,
        )
    }

    suspend fun onboarded() = settings.first().onboarded

    suspend fun seeWelcome() = context.dataStore.edit { it[onboardedKey] = true }

    suspend fun saveUsed(ids: Set<Int>) = context.dataStore.edit { p ->
        p[usedKey] = ids.joinToString(",")
    }

    suspend fun saveEmber(id: Int, fromDay: Long) = context.dataStore.edit { p ->
        p[emberIdKey] = id
        p[emberFromKey] = fromDay
    }

    suspend fun clearEmber() = context.dataStore.edit { p ->
        p[emberIdKey] = 0
        p[emberFromKey] = 0
    }
}
