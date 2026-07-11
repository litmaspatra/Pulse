// FILE: app/src/main/java/com/example/pulse/viewmodel/ChatViewModel.kt
package com.example.pulse.viewmodel

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pulse.data.AuthRepository
import com.example.pulse.data.ChatRepository
import com.example.pulse.data.Message
import com.example.pulse.data.PinStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class PairingState {
    object Idle : PairingState()
    object Loading : PairingState()
    object Paired : PairingState()
    data class Error(val message: String) : PairingState()
    object WaitingForPartner : PairingState()
}

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
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

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _imageError = MutableStateFlow<String?>(null)
    val imageError: StateFlow<String?> = _imageError.asStateFlow()

    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    private var memberSlot: String? = null
    private var hasRestoredRoom = false

    private val maxBase64Length = 700_000

    init {
        viewModelScope.launch {
            val uid = authRepository.ensureSignedIn()
            _userId.value = uid
        }
        viewModelScope.launch {
            pinStorage.memberSlotFlow.collect { slot ->
                memberSlot = slot
            }
        }
        viewModelScope.launch {
            pinStorage.roomCodeFlow.collect { code ->
                if (!code.isNullOrEmpty() && !hasRestoredRoom) {
                    hasRestoredRoom = true
                    _roomCode.value = code
                    // FIX: don't assume "a saved room code" means "paired".
                    // A room saved from a previous attempt (creator only,
                    // partner never joined) was previously treated as fully
                    // paired and dropped you straight into chat. Now we
                    // verify actual membership before showing chat.
                    listenForPartner(code)
                }
            }
        }
    }

    fun createRoom() {
        val uid = _userId.value ?: return
        _pairingState.value = PairingState.Loading

        val code = (100000..999999).random().toString()

        viewModelScope.launch {
            val success = chatRepository.createRoom(code, uid)
            if (success) {
                _roomCode.value = code
                memberSlot = "member1"
                pinStorage.saveRoomCode(code)
                pinStorage.saveMemberSlot("member1")
                listenForPartner(code)
            } else {
                _pairingState.value = PairingState.Error("Failed to create room. Try again.")
            }
        }
    }

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
                memberSlot = "member2"
                pinStorage.saveRoomCode(code)
                pinStorage.saveMemberSlot("member2")
                _pairingState.value = PairingState.Paired
                _isPaired.value = true
                listenToRoom(code)
            } else {
                _pairingState.value = PairingState.Error("Room is full or already paired.")
            }
        }
    }

    /**
     * Shows the waiting/code screen and only flips to Paired once the room
     * genuinely has 2 members — whether that's freshly (just created a room)
     * or on restore (app relaunched into an existing, possibly-unfinished
     * room). This is the fix for "create room skips straight to a mystery chat."
     */
    private fun listenForPartner(code: String) {
        if (_pairingState.value !is PairingState.Paired) {
            _pairingState.value = PairingState.WaitingForPartner
        }
        viewModelScope.launch {
            chatRepository.isPairedFlow(code)
                .catch { handleSessionError() }
                .collect { paired ->
                    if (paired) {
                        _pairingState.value = PairingState.Paired
                        _isPaired.value = true
                        listenToRoom(code)
                    } else if (!_isPaired.value) {
                        _pairingState.value = PairingState.WaitingForPartner
                    }
                }
        }
    }

    private fun listenToRoom(code: String) {
        viewModelScope.launch {
            chatRepository.messagesFlow(code)
                .catch { handleSessionError() }
                .collect { msgs ->
                    _messages.value = msgs
                }
        }
    }

    private fun handleSessionError() {
        memberSlot = null
        hasRestoredRoom = false
        _roomCode.value = null
        _isPaired.value = false
        _messages.value = emptyList()
        _pairingState.value = PairingState.Idle
        _sessionExpired.value = true
        viewModelScope.launch {
            pinStorage.clearRoomData()
        }
    }

    fun acknowledgeSessionExpired() {
        _sessionExpired.value = false
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

    fun sendImage(imageBytes: ByteArray) {
        val code = _roomCode.value ?: return
        val uid = _userId.value ?: return

        val encoded = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        if (encoded.length > maxBase64Length) {
            _imageError.value = "That photo is too large even after compression. Try a different one."
            return
        }

        val message = Message(
            text = "",
            senderId = uid,
            timestamp = System.currentTimeMillis(),
            imageBase64 = encoded
        )

        viewModelScope.launch {
            val success = chatRepository.sendMessage(code, message)
            if (!success) {
                _imageError.value = "Couldn't send the photo. Check your connection and try again."
            }
        }
    }

    fun clearImageError() {
        _imageError.value = null
    }

    fun resetPairingError() {
        _pairingState.value = PairingState.Idle
    }

    fun disconnect() {
        val code = _roomCode.value
        val slot = memberSlot

        viewModelScope.launch {
            if (code != null && slot != null) {
                chatRepository.leaveRoom(code, slot)
            }
            pinStorage.clearRoomData()
            memberSlot = null
            hasRestoredRoom = false
            _roomCode.value = null
            _isPaired.value = false
            _messages.value = emptyList()
            _pairingState.value = PairingState.Idle
        }
    }
}

class ChatViewModelFactory(
    private val pinStorage: PinStorage
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChatViewModel(ChatRepository(), AuthRepository(), pinStorage) as T
    }
}