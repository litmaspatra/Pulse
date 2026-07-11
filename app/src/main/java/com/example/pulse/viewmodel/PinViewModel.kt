package com.example.pulse.viewmodel

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

    private val _primaryPin = MutableStateFlow<String?>(null)
    val primaryPin: StateFlow<String?> = _primaryPin.asStateFlow()

    private val _secondPin = MutableStateFlow<String?>(null)
    val secondPin: StateFlow<String?> = _secondPin.asStateFlow()

    private var pendingPin: String? = null

    init {

        viewModelScope.launch {
            pinStorage.primaryPinFlow.collect { pin ->
                _primaryPin.value = pin
            }
        }

        viewModelScope.launch {
            pinStorage.secondPinFlow.collect { pin ->
                _secondPin.value = pin
            }
        }

    }

    fun savePrimaryPin(pin: String) {
        _primaryPin.value = pin

        viewModelScope.launch {
            pinStorage.savePrimaryPin(pin)
        }
    }

    fun saveSecondPin(pin: String) {
        _secondPin.value = pin

        viewModelScope.launch {
            pinStorage.saveSecondPin(pin)
        }
    }

    fun startPinCreation(pin: String) {
        pendingPin = pin
    }

    fun confirmPin(pin: String): Boolean {
        val matches = pendingPin == pin
        if (matches) {
            savePrimaryPin(pin)
            pendingPin = null
        }
        return matches
    }

    fun clearPendingPin() {
        pendingPin = null
    }

    fun validatePin(enteredPin: String): PinType {

        return when (enteredPin) {

            _primaryPin.value -> PinType.PRIMARY

            _secondPin.value -> PinType.SECOND

            else -> PinType.INVALID
        }
    }

    fun hasPin(): Boolean {
        return !_primaryPin.value.isNullOrEmpty()
    }
}