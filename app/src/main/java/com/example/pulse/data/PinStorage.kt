package com.example.pulse.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(
    name = "pulse_preferences"
)

class PinStorage(
    private val context: Context
) {
    private object Keys {
        val PIN = stringPreferencesKey("pin")
        val USER_ID = stringPreferencesKey("user_id")
        val ROOM_CODE = stringPreferencesKey("room_code")
    }

    // PIN
    suspend fun savePin(pin: String) {
        context.dataStore.edit { it[Keys.PIN] = pin }
    }

    val savedPinFlow: Flow<String?> = context.dataStore.data.map { it[Keys.PIN] }

    // User ID (stable device identity)
    suspend fun saveUserId(id: String) {
        context.dataStore.edit { it[Keys.USER_ID] = id }
    }

    val userIdFlow: Flow<String?> = context.dataStore.data.map { it[Keys.USER_ID] }

    // Room code (persist across app restarts)
    suspend fun saveRoomCode(code: String) {
        context.dataStore.edit { it[Keys.ROOM_CODE] = code }
    }

    val roomCodeFlow: Flow<String?> = context.dataStore.data.map { it[Keys.ROOM_CODE] }
}