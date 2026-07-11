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

    private val _securityQuestion = MutableStateFlow<String?>(null)
    val securityQuestion: StateFlow<String?> = _securityQuestion.asStateFlow()

    private var securityAnswer: String? = null

    private var pendingPrimaryPin: String? = null
    private var pendingSecondPin: String? = null

    init {
        viewModelScope.launch { pinStorage.primaryPinFlow.collect { _primaryPin.value = it } }
        viewModelScope.launch { pinStorage.secondPinFlow.collect { _secondPin.value = it } }
        viewModelScope.launch { pinStorage.securityQuestionFlow.collect { _securityQuestion.value = it } }
        viewModelScope.launch { pinStorage.securityAnswerFlow.collect { securityAnswer = it } }
    }

    fun startPinCreation(pin: String) { pendingPrimaryPin = pin }

    fun confirmPin(pin: String): Boolean {
        val matches = pendingPrimaryPin == pin
        if (matches) {
            _primaryPin.value = pin
            viewModelScope.launch { pinStorage.savePrimaryPin(pin) }
            pendingPrimaryPin = null
        }
        return matches
    }

    fun startSecondPinCreation(pin: String) { pendingSecondPin = pin }

    fun confirmSecondPin(pin: String): Boolean {
        val matches = pendingSecondPin == pin
        if (matches) {
            _secondPin.value = pin
            viewModelScope.launch { pinStorage.saveSecondPin(pin) }
            pendingSecondPin = null
        }
        return matches
    }

    fun clearSecondPin() {
        _secondPin.value = null
        viewModelScope.launch { pinStorage.clearSecondPin() }
    }

    fun validateSecondPinEntry(pin: String): Boolean =
        pin.isNotEmpty() && pin == _secondPin.value

    fun saveSecurityQuestion(question: String, answer: String) {
        _securityQuestion.value = question
        securityAnswer = answer.lowercase().trim()
        viewModelScope.launch { pinStorage.saveSecurityQuestion(question, answer) }
    }

    fun verifySecurityAnswer(answer: String): Boolean =
        securityAnswer != null && answer.lowercase().trim() == securityAnswer

    fun validatePin(enteredPin: String): PinType {
        return when (enteredPin) {
            _primaryPin.value -> PinType.PRIMARY
            _secondPin.value -> PinType.SECOND
            else -> PinType.INVALID
        }
    }

    fun hasPin(): Boolean = !_primaryPin.value.isNullOrEmpty()
}