package com.example.pulse.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
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
    private object PreferencesKeys {
        val PIN = stringPreferencesKey("pin")
    }
    suspend fun savePin(pin: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PIN] = pin
        }
    }
    val savedPinFlow: Flow<String?> =
        context.dataStore.data.map { preferences ->
            preferences[PreferencesKeys.PIN]
        }
}