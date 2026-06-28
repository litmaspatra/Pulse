package com.example.pulse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pulse.data.ChatRepository
import com.example.pulse.data.Message
import com.example.pulse.data.PinStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class PairingState {
    object Idle : PairingState()
    object Loading : PairingState()
    object Paired : PairingState()
    data class Error(val message: String) : PairingState()
    object WaitingForPartner : PairingState()
}

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val pinStorage: PinStorage
) : ViewModel() {

    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Idle)
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _roomCode = MutableStateFlow<String?>(null)
    val roomCode: StateFlow<String?> = _roomCode.asStateFlow()

    private val _isPaired = MutableStateFlow(false)
    val isPaired: StateFlow<Boolean> = _isPaired.asStateFlow()

    // Stable user ID for this device — stored in DataStore
    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    init {
        viewModelScope.launch {
            // Load or generate a stable user ID
            pinStorage.userIdFlow.collect { id ->
                if (id == null) {
                    val newId = UUID.randomUUID().toString().take(8)
                    pinStorage.saveUserId(newId)
                    _userId.value = newId
                } else {
                    _userId.value = id
                }
            }
        }
        viewModelScope.launch {
            // Restore saved room code if any
            pinStorage.roomCodeFlow.collect { code ->
                if (!code.isNullOrEmpty() && _roomCode.value == null) {
                    _roomCode.value = code
                    listenToRoom(code)
                }
            }
        }
    }

    // Generate a new 6-digit room code and create the room
    fun createRoom() {
        val uid = _userId.value ?: return
        _pairingState.value = PairingState.Loading

        val code = (100000..999999).random().toString()

        viewModelScope.launch {
            val success = chatRepository.createRoom(code, uid)
            if (success) {
                _roomCode.value = code
                pinStorage.saveRoomCode(code)
                _pairingState.value = PairingState.WaitingForPartner
                listenForPartner(code)
            } else {
                _pairingState.value = PairingState.Error("Failed to create room. Try again.")
            }
        }
    }

    // Join an existing room with entered code
    fun joinRoom(code: String) {
        val uid = _userId.value ?: return
        _pairingState.value = PairingState.Loading

        viewModelScope.launch {
            val exists = chatRepository.roomExists(code)
            if (!exists) {
                _pairingState.value = PairingState.Error("Room not found. Check the code.")
                return@launch
            }
            val success = chatRepository.joinRoom(code, uid)
            if (success) {
                _roomCode.value = code
                pinStorage.saveRoomCode(code)
                _pairingState.value = PairingState.Paired
                _isPaired.value = true
                listenToRoom(code)
            } else {
                _pairingState.value = PairingState.Error("Room is full or already paired.")
            }
        }
    }

    private fun listenForPartner(code: String) {
        viewModelScope.launch {
            chatRepository.isPairedFlow(code).collect { paired ->
                if (paired) {
                    _pairingState.value = PairingState.Paired
                    _isPaired.value = true
                    listenToRoom(code)
                }
            }
        }
    }

    private fun listenToRoom(code: String) {
        viewModelScope.launch {
            chatRepository.messagesFlow(code).collect { msgs ->
                _messages.value = msgs
            }
        }
    }

    fun sendMessage(text: String) {
        val code = _roomCode.value ?: return
        val uid = _userId.value ?: return
        if (text.isBlank()) return

        val message = Message(
            text = text.trim(),
            senderId = uid,
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            chatRepository.sendMessage(code, message)
        }
    }

    fun resetPairingError() {
        _pairingState.value = PairingState.Idle
    }
}

class ChatViewModelFactory(
    private val pinStorage: PinStorage
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChatViewModel(ChatRepository(), pinStorage) as T
    }
}