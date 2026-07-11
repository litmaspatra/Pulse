// FILE: app/src/main/java/com/example/pulse/viewmodel/JournalViewModel.kt
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class JournalStats(
    val totalEntries: Int = 0,
    val totalWords: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0
)

class JournalViewModel(
    private val repository: JournalRepository
) : ViewModel() {

    private val _entries = MutableStateFlow<List<JournalEntry>>(emptyList())
    val entries: StateFlow<List<JournalEntry>> = _entries.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _archivedEntries = MutableStateFlow<List<JournalEntry>>(emptyList())
    val archivedEntries: StateFlow<List<JournalEntry>> = _archivedEntries.asStateFlow()

    private val _isArchiveLoading = MutableStateFlow(true)
    val isArchiveLoading: StateFlow<Boolean> = _isArchiveLoading.asStateFlow()

    private val _trashEntries = MutableStateFlow<List<JournalEntry>>(emptyList())
    val trashEntries: StateFlow<List<JournalEntry>> = _trashEntries.asStateFlow()

    private val _isTrashLoading = MutableStateFlow(true)
    val isTrashLoading: StateFlow<Boolean> = _isTrashLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _stats = MutableStateFlow(JournalStats())
    val stats: StateFlow<JournalStats> = _stats.asStateFlow()

    private val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    init {
        loadEntries()
    }

    fun loadEntries() {
        viewModelScope.launch(Dispatchers.IO) {
            val list = repository.listEntries()
            _entries.value = list
            // Stats include archived entries (still "written"), exclude trash.
            _stats.value = computeStats(list + repository.listArchivedEntries())
            _isLoading.value = false
        }
    }

    fun loadArchived() {
        viewModelScope.launch(Dispatchers.IO) {
            _archivedEntries.value = repository.listArchivedEntries()
            _isArchiveLoading.value = false
        }
    }

    fun loadTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            _trashEntries.value = repository.listTrashEntries()
            _isTrashLoading.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun filteredEntries(): List<JournalEntry> {
        val query = _searchQuery.value.trim()
        if (query.isBlank()) return _entries.value
        return _entries.value.filter {
            it.content.contains(query, ignoreCase = true) ||
                    it.dateLabel.contains(query, ignoreCase = true)
        }
    }

    private fun computeStats(list: List<JournalEntry>): JournalStats {
        if (list.isEmpty()) return JournalStats()

        val totalWords = list.sumOf { it.wordCount }
        val dayKeys = list.map { dayKeyFormat.format(it.timestampMillis) }.toSortedSet()

        var longest = 1
        var run = 1
        val sortedDates = dayKeys.map { dayKeyFormat.parse(it)!!.time }.sorted()
        for (i in 1 until sortedDates.size) {
            val diffDays = (sortedDates[i] - sortedDates[i - 1]) / (24 * 60 * 60 * 1000)
            run = if (diffDays == 1L) run + 1 else 1
            if (run > longest) longest = run
        }

        val cal = Calendar.getInstance()
        val todayKey = dayKeyFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayKey = dayKeyFormat.format(cal.time)

        var current = 0
        if (dayKeys.contains(todayKey) || dayKeys.contains(yesterdayKey)) {
            val checkCal = Calendar.getInstance()
            if (!dayKeys.contains(todayKey)) checkCal.add(Calendar.DAY_OF_YEAR, -1)
            while (dayKeys.contains(dayKeyFormat.format(checkCal.time))) {
                current++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            }
        }

        return JournalStats(
            totalEntries = list.size,
            totalWords = totalWords,
            currentStreak = current,
            longestStreak = longest
        )
    }

    fun newEntryFileName(): String = repository.newEntryFileName()

    fun trashEntry(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.moveToTrash(fileName)
            loadEntries()
        }
    }

    fun archiveEntry(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.archiveEntry(fileName)
            loadEntries()
        }
    }

    fun unarchiveEntry(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.unarchiveEntry(fileName)
            loadArchived()
            loadEntries()
        }
    }

    fun restoreFromTrash(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.restoreFromTrash(fileName)
            loadTrash()
            loadEntries()
        }
    }

    fun permanentlyDelete(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.permanentlyDelete(fileName)
            loadTrash()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.emptyTrash()
            loadTrash()
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