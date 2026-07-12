// FILE: app/src/main/java/com/example/pulse/viewmodel/SettingsViewModel.kt
package com.example.pulse.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pulse.data.BackupRepository
import com.example.pulse.data.JournalRepository
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

    private val _accentColorName = MutableStateFlow<String?>(null)
    val accentColorName: StateFlow<String?> = _accentColorName.asStateFlow()

    private val _fontOptionName = MutableStateFlow<String?>(null)
    val fontOptionName: StateFlow<String?> = _fontOptionName.asStateFlow()

    private val _lockTimeoutName = MutableStateFlow<String?>(null)
    val lockTimeoutName: StateFlow<String?> = _lockTimeoutName.asStateFlow()

    private val _exportResult = MutableStateFlow<Uri?>(null)
    val exportResult: StateFlow<Uri?> = _exportResult.asStateFlow()

    private val _importResult = MutableStateFlow<ImportOutcome?>(null)
    val importResult: StateFlow<ImportOutcome?> = _importResult.asStateFlow()

    init {
        viewModelScope.launch { pinStorage.accentColorFlow.collect { _accentColorName.value = it } }
        viewModelScope.launch { pinStorage.fontOptionFlow.collect { _fontOptionName.value = it } }
        // FIX: pinStorage.lockTimeoutFlow emits a LockTimeout enum, not a
        // String. Assigning it directly into a MutableStateFlow<String?>
        // was a type mismatch that broke the build. .name converts the
        // enum to the same String key the rest of this screen expects.
        viewModelScope.launch { pinStorage.lockTimeoutFlow.collect { _lockTimeoutName.value = it.name } }
    }

    fun setAccentColor(name: String) {
        viewModelScope.launch { pinStorage.saveAccentColor(name) }
    }

    fun setFontOption(name: String) {
        viewModelScope.launch { pinStorage.saveFontOption(name) }
    }

    fun setLockTimeout(name: String) {
        viewModelScope.launch { pinStorage.saveLockTimeout(com.example.pulse.data.LockTimeout.fromName(name)) }
    }

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