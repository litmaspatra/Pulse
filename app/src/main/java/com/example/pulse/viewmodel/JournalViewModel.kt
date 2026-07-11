package com.example.pulse.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pulse.data.JournalEntry
import com.example.pulse.data.JournalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JournalViewModel(
    private val repository: JournalRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<JournalEntry>>(emptyList())
    val entries: StateFlow<List<JournalEntry>> = _entries.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadEntries()
    }

    fun loadEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.listEntries()
            _entries.value = list
            _isLoading.value = false
        }
    }

    fun todayFileName(): String = repository.todayFileName()

    fun deleteEntry(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteEntry(fileName)
            loadEntries()
        }
    }
}

class JournalViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return JournalViewModel(JournalRepository(context)) as T
    }
}