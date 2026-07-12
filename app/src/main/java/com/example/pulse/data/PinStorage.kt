package com.example.pulse.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "pulse_preferences")

class PinStorage(private val context: Context) {
    private object Keys {
        val PRIMARY_PIN = stringPreferencesKey("primary_pin")
        val SECOND_PIN = stringPreferencesKey("second_pin")
        val ROOM_CODE = stringPreferencesKey("room_code")
        val MEMBER_SLOT = stringPreferencesKey("member_slot")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val FONT_OPTION = stringPreferencesKey("font_option")
        val SECURITY_QUESTION = stringPreferencesKey("security_question")
        val SECURITY_ANSWER = stringPreferencesKey("security_answer")
        val LOCK_TIMEOUT = stringPreferencesKey("lock_timeout")
        val PRIVATE_CHAT_ENABLED = stringPreferencesKey("private_chat_enabled")
        val LAST_FOREGROUND_TIME = longPreferencesKey("last_foreground_time")
    }

    // --- PIN ---

    suspend fun savePrimaryPin(pin: String) {
        context.dataStore.edit { it[Keys.PRIMARY_PIN] = pin }
    }
    val primaryPinFlow: Flow<String?> = context.dataStore.data.map { it[Keys.PRIMARY_PIN] }

    suspend fun saveSecondPin(pin: String) {
        context.dataStore.edit { it[Keys.SECOND_PIN] = pin }
    }
    val secondPinFlow: Flow<String?> = context.dataStore.data.map { it[Keys.SECOND_PIN] }

    suspend fun clearSecondPin() {
        context.dataStore.edit { it.remove(Keys.SECOND_PIN) }
    }

    // --- Room ---

    suspend fun saveRoomCode(code: String) {
        context.dataStore.edit { it[Keys.ROOM_CODE] = code }
    }
    val roomCodeFlow: Flow<String?> = context.dataStore.data.map { it[Keys.ROOM_CODE] }

    suspend fun saveMemberSlot(slot: String) {
        context.dataStore.edit { it[Keys.MEMBER_SLOT] = slot }
    }
    val memberSlotFlow: Flow<String?> = context.dataStore.data.map { it[Keys.MEMBER_SLOT] }

    suspend fun clearRoomData() {
        context.dataStore.edit {
            it.remove(Keys.ROOM_CODE)
            it.remove(Keys.MEMBER_SLOT)
        }
    }

    // --- Appearance ---

    suspend fun saveAccentColor(name: String) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR] = name }
    }
    val accentColorFlow: Flow<String?> = context.dataStore.data.map { it[Keys.ACCENT_COLOR] }

    suspend fun saveFontOption(name: String) {
        context.dataStore.edit { it[Keys.FONT_OPTION] = name }
    }
    val fontOptionFlow: Flow<String?> = context.dataStore.data.map { it[Keys.FONT_OPTION] }

    // --- Security ---

    suspend fun saveSecurityQuestion(question: String, answer: String) {
        context.dataStore.edit {
            it[Keys.SECURITY_QUESTION] = question
            it[Keys.SECURITY_ANSWER] = answer.lowercase().trim()
        }
    }
    val securityQuestionFlow: Flow<String?> =
        context.dataStore.data.map { it[Keys.SECURITY_QUESTION] }
    val securityAnswerFlow: Flow<String?> =
        context.dataStore.data.map { it[Keys.SECURITY_ANSWER] }

    suspend fun saveLockTimeout(timeout: LockTimeout) {
        context.dataStore.edit { it[Keys.LOCK_TIMEOUT] = timeout.name }
    }
    val lockTimeoutFlow: Flow<LockTimeout> =
        context.dataStore.data.map { prefs ->
            LockTimeout.fromName(prefs[Keys.LOCK_TIMEOUT])
        }

    // --- Private chat toggle ---

    val isPrivateChatEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[Keys.PRIVATE_CHAT_ENABLED] == "true"
        }

    suspend fun enablePrivateChat() {
        context.dataStore.edit { it[Keys.PRIVATE_CHAT_ENABLED] = "true" }
    }

    suspend fun disablePrivateChat() {
        context.dataStore.edit { it[Keys.PRIVATE_CHAT_ENABLED] = "false" }
    }

    // --- Foreground time (for lock timeout) ---

    suspend fun saveLastForegroundTime(time: Long) {
        context.dataStore.edit { it[Keys.LAST_FOREGROUND_TIME] = time }
    }

    val lastForegroundTimeFlow: Flow<Long> =
        context.dataStore.data.map { prefs ->
            prefs[Keys.LAST_FOREGROUND_TIME] ?: 0L
        }
}