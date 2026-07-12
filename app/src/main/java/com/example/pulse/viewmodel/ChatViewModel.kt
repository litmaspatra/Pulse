package com.example.pulse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pulse.data.AuthRepository
import com.example.pulse.data.ChatRepository
import com.example.pulse.data.DeviceIdProvider
import com.example.pulse.data.Message
import com.example.pulse.data.PinStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

sealed class PairingState {
    object Idle : PairingState()
    object Loading : PairingState()
    object WaitingForPartner : PairingState()
    object Paired : PairingState()
    data class Error(val message: String) : PairingState()
}

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val pinStorage: PinStorage
) : ViewModel() {

    // --- Pairing ---
    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Idle)
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    // --- Messages ---
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // --- Room ---
    private val _roomCode = MutableStateFlow<String?>(null)
    val roomCode: StateFlow<String?> = _roomCode.asStateFlow()

    private val _isPaired = MutableStateFlow(false)
    val isPaired: StateFlow<Boolean> = _isPaired.asStateFlow()

    private val _partnerName = MutableStateFlow<String?>(null)
    val partnerName: StateFlow<String?> = _partnerName.asStateFlow()

    val deviceName: String = DeviceIdProvider.getDeviceName()

    // --- User ---
    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    // --- Reply ---
    private val _replyingTo = MutableStateFlow<Message?>(null)
    val replyingTo: StateFlow<Message?> = _replyingTo.asStateFlow()

    // --- Edit ---
    private val _editingMessage = MutableStateFlow<Message?>(null)
    val editingMessage: StateFlow<Message?> = _editingMessage.asStateFlow()

    // --- Errors ---
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _sessionExpired = MutableStateFlow(false)
    val sessionExpired: StateFlow<Boolean> = _sessionExpired.asStateFlow()

    // --- Voice recording ---
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // --- Internal ---
    private var memberSlot: String? = null
    private var hasCheckedRoom = false

    init {
        viewModelScope.launch {
            _userId.value = authRepository.ensureSignedIn()
        }
        viewModelScope.launch {
            pinStorage.memberSlotFlow.collect { memberSlot = it }
        }
        // On init, check if there's a saved room. Only restore if PAIRED.
        viewModelScope.launch {
            pinStorage.roomCodeFlow.collect { code ->
                if (!code.isNullOrEmpty() && !hasCheckedRoom) {
                    hasCheckedRoom = true
                    _roomCode.value = code
                    tryRestoreRoom(code)
                }
            }
        }
    }

    private suspend fun tryRestoreRoom(code: String) {
        // Wait for auth to finish so we have a uid
        val uid = _userId.value ?: run {
            _userId.first { it != null }
            _userId.value!!
        }

        val result = chatRepository.restoreRoom(code, uid)
        if (result == ChatRepository.RestoreResult.RESTORE_CONNECTED) {
            // Room is still active with 2 members — restore session
            val slot = pinStorage.memberSlotFlow.first()
            memberSlot = slot
            _pairingState.value = PairingState.Paired
            _isPaired.value = true
            listenToMessages(code)
            _partnerName.value = chatRepository.getPartnerName(code, uid)
        } else {
            // Room not paired, expired, or we're not in it — clear everything
            clearRoomState()
        }
    }

    private fun clearRoomState() {
        _roomCode.value = null
        _isPaired.value = false
        _messages.value = emptyList()
        _partnerName.value = null
        _pairingState.value = PairingState.Idle
        memberSlot = null
        hasCheckedRoom = false
        viewModelScope.launch { pinStorage.clearRoomData() }
    }

    // --- Create / Join ---

    fun createRoom() {
        val uid = _userId.value ?: return
        _pairingState.value = PairingState.Loading
        val code = (100000..999999).random().toString()

        viewModelScope.launch {
            if (chatRepository.createRoom(code, uid)) {
                _roomCode.value = code
                memberSlot = "member1"
                pinStorage.saveRoomCode(code)
                pinStorage.saveMemberSlot("member1")
                _pairingState.value = PairingState.WaitingForPartner
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
            when (chatRepository.joinRoom(code, uid)) {
                ChatRepository.JoinResult.SUCCESS -> {
                    _roomCode.value = code
                    memberSlot = "member2"
                    pinStorage.saveRoomCode(code)
                    pinStorage.saveMemberSlot("member2")
                    _pairingState.value = PairingState.Paired
                    _isPaired.value = true
                    listenToMessages(code)
                    _partnerName.value = chatRepository.getPartnerName(code, uid)
                }
                ChatRepository.JoinResult.NOT_FOUND ->
                    _pairingState.value = PairingState.Error("Room not found. Check the code.")
                ChatRepository.JoinResult.FULL ->
                    _pairingState.value = PairingState.Error("Room is full or that's your own code.")
                ChatRepository.JoinResult.EXPIRED ->
                    _pairingState.value = PairingState.Error("Room has expired (3-day limit).")
                ChatRepository.JoinResult.FAILED ->
                    _pairingState.value = PairingState.Error("Couldn't join. Check your connection.")
            }
        }
    }

    private fun listenForPartner(code: String) {
        viewModelScope.launch {
            chatRepository.isPairedFlow(code)
                .catch { handleSessionError() }
                .collect { paired ->
                    if (paired && !_isPaired.value) {
                        _pairingState.value = PairingState.Paired
                        _isPaired.value = true
                        listenToMessages(code)
                        val uid = _userId.value ?: return@collect
                        _partnerName.value = chatRepository.getPartnerName(code, uid)
                    }
                }
        }
    }

    private fun listenToMessages(code: String) {
        viewModelScope.launch {
            chatRepository.messagesFlow(code)
                .catch { handleSessionError() }
                .collect { _messages.value = it }
        }
    }

    private fun handleSessionError() {
        clearRoomState()
        _sessionExpired.value = true
    }

    // --- Send text ---

    fun sendMessage(text: String) {
        val code = _roomCode.value ?: return
        val uid = _userId.value ?: return
        if (text.isBlank()) return

        val message = Message(
            text = text.trim(),
            senderId = uid,
            timestamp = System.currentTimeMillis(),
            replyToMessageId = _replyingTo.value?.id,
            replyToText = _replyingTo.value?.let { it.text.take(100) },
            replyToSenderId = _replyingTo.value?.senderId
        )
        _replyingTo.value = null

        viewModelScope.launch { chatRepository.sendMessage(code, message) }
    }

    // --- Send image (via Firebase Storage, not base64) ---

    fun sendImage(file: File) {
        val code = _roomCode.value ?: return
        val uid = _userId.value ?: return

        viewModelScope.launch {
            _error.value = null
            val url = chatRepository.uploadImage(code, file)
            if (url != null) {
                chatRepository.sendMessage(code, Message(
                    text = "", senderId = uid,
                    timestamp = System.currentTimeMillis(),
                    mediaUrl = url, mediaType = MediaType.IMAGE
                ))
            } else {
                _error.value = "Couldn't send the image. Check your connection."
            }
        }
    }

    // --- Send voice note ---

    fun sendVoiceNote(file: File, durationSec: Int) {
        val code = _roomCode.value ?: return
        val uid = _userId.value ?: return

        viewModelScope.launch {
            _error.value = null
            val url = chatRepository.uploadVoiceNote(code, file)
            if (url != null) {
                chatRepository.sendMessage(code, Message(
                    text = "", senderId = uid,
                    timestamp = System.currentTimeMillis(),
                    mediaUrl = url, mediaType = MediaType.VOICE,
                    mediaDurationSec = durationSec
                ))
            } else {
                _error.value = "Couldn't send the voice note. Try again."
            }
        }
    }

    // --- Reply ---

    fun startReply(message: Message) { _replyingTo.value = message }
    fun cancelReply() { _replyingTo.value = null }

    // --- Edit ---

    fun startEdit(message: Message) {
        if (message.senderId != _userId.value) return
        _editingMessage.value = message
    }

    fun cancelEdit() { _editingMessage.value = null }

    fun confirmEdit(newText: String) {
        val msg = _editingMessage.value ?: return
        val code = _roomCode.value ?: return
        if (newText.isBlank()) return

        viewModelScope.launch {
            if (!chatRepository.editMessage(code, msg.id, newText.trim())) {
                _error.value = "Couldn't edit message."
            }
            _editingMessage.value = null
        }
    }

    // --- Voice recording state (actual recording handled by the screen) ---

    fun setRecording(recording: Boolean) { _isRecording.value = recording }

    // --- Dismiss states ---

    fun acknowledgeSessionExpired() { _sessionExpired.value = false }
    fun clearError() { _error.value = null }
    fun resetPairingError() { _pairingState.value = PairingState.Idle }

    // --- Disconnect (waits for Firebase cleanup before navigating) ---

    fun disconnect(onComplete: () -> Unit = {}) {
        val code = _roomCode.value
        val uid = _userId.value

        viewModelScope.launch {
            if (code != null && uid != null) {
                chatRepository.leaveRoom(code, uid)
            }
            clearRoomState()
            onComplete()
        }
    }
}

class ChatViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChatViewModel(
            ChatRepository(context),
            AuthRepository(),
            PinStorage(context)
        ) as T
    }
}