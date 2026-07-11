// FILE: app/src/main/java/com/example/pulse/viewmodel/CalendarViewModel.kt
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
import java.util.Calendar

class CalendarViewModel(
    private val repository: JournalRepository
) : ViewModel() {

    private val cal = Calendar.getInstance()

    private val _year = MutableStateFlow(cal.get(Calendar.YEAR))
    val year: StateFlow<Int> = _year.asStateFlow()

    private val _month = MutableStateFlow(cal.get(Calendar.MONTH)) // 0-based
    val month: StateFlow<Int> = _month.asStateFlow()

    private val _entriesByDay = MutableStateFlow<Map<Int, List<JournalEntry>>>(emptyMap())
    val entriesByDay: StateFlow<Map<Int, List<JournalEntry>>> = _entriesByDay.asStateFlow()

    private val _selectedDay = MutableStateFlow<Int?>(null)
    val selectedDay: StateFlow<Int?> = _selectedDay.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _entriesByDay.value = repository.entriesForMonth(_year.value, _month.value)
        }
    }

    fun nextMonth() {
        if (_month.value == 11) { _month.value = 0; _year.value++ } else { _month.value++ }
        _selectedDay.value = null
        load()
    }

    fun prevMonth() {
        if (_month.value == 0) { _month.value = 11; _year.value-- } else { _month.value-- }
        _selectedDay.value = null
        load()
    }

    fun selectDay(day: Int) {
        _selectedDay.value = if (_selectedDay.value == day) null else day
    }
}

class CalendarViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CalendarViewModel(JournalRepository(context)) as T
    }
}