package com.example.pulse.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pulse.data.PinStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val pinStorage: PinStorage
) : ViewModel() {

    private val _accentColorName = MutableStateFlow<String?>(null)
    val accentColorName: StateFlow<String?> = _accentColorName.asStateFlow()

    private val _fontOptionName = MutableStateFlow<String?>(null)
    val fontOptionName: StateFlow<String?> = _fontOptionName.asStateFlow()

    init {
        viewModelScope.launch { pinStorage.accentColorFlow.collect { _accentColorName.value = it } }
        viewModelScope.launch { pinStorage.fontOptionFlow.collect { _fontOptionName.value = it } }
    }

    fun setAccentColor(name: String) {
        viewModelScope.launch { pinStorage.saveAccentColor(name) }
    }

    fun setFontOption(name: String) {
        viewModelScope.launch { pinStorage.saveFontOption(name) }
    }
}

class SettingsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(PinStorage(context)) as T
    }
}