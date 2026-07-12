package com.example.pulse.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pulse.data.BackupRepository
import com.example.pulse.data.JournalRepository
import com.example.pulse.data.LockTimeout
import com.example.pulse.data.PinStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ImportOutcome {
    data class Success(val imported: Int, val skipped: Int) : ImportOutcome()
    object Failed : ImportOutcome()
}

class SettingsViewModel(
    private val pinStorage: PinStorage,
    private val backupRepository: BackupRepository
) : ViewModel() {

    // --- Appearance ---
    private val _accentColorName = MutableStateFlow<String?>(null)
    val accentColorName: StateFlow<String?> = _accentColorName.asStateFlow()

    private val _fontOptionName = MutableStateFlow<String?>(null)
    val fontOptionName: StateFlow<String?> = _fontOptionName.asStateFlow()

    // --- Security ---
    private val _lockTimeoutName = MutableStateFlow<String?>(null)
    val lockTimeoutName: StateFlow<String?> = _lockTimeoutName.asStateFlow()

    // --- Backup ---
    private val _exportResult = MutableStateFlow<Uri?>(null)
    val exportResult: StateFlow<Uri?> = _exportResult.asStateFlow()

    private val _importResult = MutableStateFlow<ImportOutcome?>(null)
    val importResult: StateFlow<ImportOutcome?> = _importResult.asStateFlow()

    // --- Private chat (8-tap secret) ---
    private val _isPrivateChatEnabled = MutableStateFlow(false)
    val isPrivateChatEnabled: StateFlow<Boolean> = _isPrivateChatEnabled.asStateFlow()

    private val _tapCount = MutableStateFlow(0)
    val tapCount: StateFlow<Int> = _tapCount.asStateFlow()

    init {
        viewModelScope.launch { pinStorage.accentColorFlow.collect { _accentColorName.value = it } }
        viewModelScope.launch { pinStorage.fontOptionFlow.collect { _fontOptionName.value = it } }
        viewModelScope.launch { pinStorage.lockTimeoutFlow.collect { _lockTimeoutName.value = it.name } }
        viewModelScope.launch { pinStorage.isPrivateChatEnabledFlow.collect { _isPrivateChatEnabled.value = it } }
    }

    // --- Appearance ---

    fun setAccentColor(name: String) {
        viewModelScope.launch { pinStorage.saveAccentColor(name) }
    }

    fun setFontOption(name: String) {
        viewModelScope.launch { pinStorage.saveFontOption(name) }
    }

    // --- Security ---

    fun setLockTimeout(name: String) {
        viewModelScope.launch { pinStorage.saveLockTimeout(LockTimeout.fromName(name)) }
    }

    // --- Backup ---

    fun exportJournal() {
        viewModelScope.launch(Dispatchers.IO) {
            _exportResult.value = backupRepository.exportJournal()
        }
    }

    fun clearExportResult() { _exportResult.value = null }

    fun importJournal(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = backupRepository.importJournal(uri)
            _importResult.value = if (result != null) {
                ImportOutcome.Success(result.imported, result.skipped)
            } else {
                ImportOutcome.Failed
            }
        }
    }

    fun clearImportResult() { _importResult.value = null }

    // --- Private chat 8-tap ---

    /**
     * Call this each time the user taps the version text.
     * After 8 taps within ~3 seconds, enables private chat.
     * If already enabled, does nothing.
     */
    fun onVersionTap() {
        if (_isPrivateChatEnabled.value) return

        val newCount = _tapCount.value + 1
        _tapCount.value = newCount

        if (newCount >= 8) {
            viewModelScope.launch { pinStorage.enablePrivateChat() }
            _tapCount.value = 0
        }

        // Reset counter after 3 seconds of no taps
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            // Only reset if no new taps came in
            if (_tapCount.value == newCount) _tapCount.value = 0
        }
    }

    fun disablePrivateChat() {
        viewModelScope.launch { pinStorage.disablePrivateChat() }
    }
}

class SettingsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val journalRepository = JournalRepository(context)
        return SettingsViewModel(
            PinStorage(context),
            BackupRepository(context, journalRepository)
        ) as T
    }
}