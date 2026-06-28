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
        val ROOM_CODE = stringPreferencesKey("room_code")
        val MEMBER_SLOT = stringPreferencesKey("member_slot")
    }

    // PIN
    suspend fun savePin(pin: String) {
        context.dataStore.edit { it[Keys.PIN] = pin }
    }

    val savedPinFlow: Flow<String?> = context.dataStore.data.map { it[Keys.PIN] }

    // Room code (persist across app restarts)
    suspend fun saveRoomCode(code: String) {
        context.dataStore.edit { it[Keys.ROOM_CODE] = code }
    }

    val roomCodeFlow: Flow<String?> = context.dataStore.data.map { it[Keys.ROOM_CODE] }

    // Which slot this device holds in the room: "member1" or "member2".
    // Needed so we know which node to clear when disconnecting.
    suspend fun saveMemberSlot(slot: String) {
        context.dataStore.edit { it[Keys.MEMBER_SLOT] = slot }
    }

    val memberSlotFlow: Flow<String?> = context.dataStore.data.map { it[Keys.MEMBER_SLOT] }

    // Clear room-related data on disconnect, keep the PIN
    suspend fun clearRoomData() {
        context.dataStore.edit {
            it.remove(Keys.ROOM_CODE)
            it.remove(Keys.MEMBER_SLOT)
        }
    }
}