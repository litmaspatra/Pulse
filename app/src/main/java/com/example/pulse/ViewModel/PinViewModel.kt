package com.example.pulse.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pulse.data.PinStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PinViewModel(
    private val pinStorage: PinStorage
) : ViewModel() {

    private val _savedPin = MutableStateFlow<String?>(null)
    val savedPin: StateFlow<String?> = _savedPin.asStateFlow()

    init {
        viewModelScope.launch {
            pinStorage.savedPinFlow.collect { pin ->
                _savedPin.value = pin
            }
        }
    }

    fun savePin(pin: String) {
        viewModelScope.launch {
            pinStorage.savePin(pin)
        }
    }

    fun validatePin(enteredPin: String): Boolean {
        return enteredPin == _savedPin.value
    }

    fun hasPin(): Boolean {
        return !_savedPin.value.isNullOrEmpty()
    }
}