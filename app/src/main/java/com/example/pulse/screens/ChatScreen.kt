package com.example.pulse.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.pulse.data.Message
import com.example.pulse.data.MediaType
import com.example.pulse.viewmodel.ChatViewModel
import com.example.pulse.viewmodel.ChatViewModelFactory
import com.example.pulse.viewmodel.PairingState
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onExitChat: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModelFactory(context))

    val pairingState by chatViewModel.pairingState.collectAsStateWithLifecycle()
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val roomCode by chatViewModel.roomCode.collectAsStateWithLifecycle()
    val userId by chatViewModel.userId.collectAsStateWithLifecycle()
    val partnerName by chatViewModel.partnerName.collectAsStateWithLifecycle()
    val replyingTo by chatViewModel.replyingTo.collectAsStateWithLifecycle()
    val editingMessage by chatViewModel.editingMessage.collectAsStateWithLifecycle()
    val error by chatViewModel.error.collectAsStateWithLifecycle()
    val sessionExpired by chatViewModel.sessionExpired.collectAsStateWithLifecycle()
    val isRecording by chatViewModel.isRecording.collectAsStateWithLifecycle()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var editTextField by remember { mutableStateOf("") }
    var isExiting by remember { mutableStateOf(false) }

    // Back from pairing = exit. Back from chat = exit.
    androidx.activity.compose.BackHandler {
        if (isExiting) return@BackHandler
        isExiting = true
        chatViewModel.disconnect { onExitChat() }
    }

    // Session expired dialog
    if (sessionExpired) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { chatViewModel.acknowledgeSessionExpired() },
            title = { Text("Session Expired") },
            text = { Text("The room has expired or the other person disconnected.") },
            confirmButton = {
                TextButton(onClick = {
                    chatViewModel.acknowledgeSessionExpired()
                    if (!isExiting) {
                        isExiting = true
                        onExitChat()
                    }
                }) { Text("OK") }
            }
        )
    }

    // Settings bottom sheet
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            ChatSettingsSheetContent(
                partnerName = partnerName,
                roomCode = roomCode,
                onDisconnect = {
                    showSettingsSheet = false
                    if (!isExiting) {
                        isExiting = true
                        chatViewModel.disconnect { onExitChat() }
                    }
                },
                onDismiss = { showSettingsSheet = false }
            )
        }
    }

    // Edit message bottom sheet
    if (showEditSheet && editingMessage != null) {
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false; chatViewModel.cancelEdit() },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Edit Message", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = editTextField,
                    onValueChange = { editTextField = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    maxLines = 3
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showEditSheet = false; chatViewModel.cancelEdit() }) { Text("Cancel") }
                    TextButton(onClick = {
                        chatViewModel.confirmEdit(editTextField)
                        showEditSheet = false
                    }, enabled = editTextField.isNotBlank()) { Text("Save") }
                }
            }
        }
    }

    val isPaired = pairingState is PairingState.Paired

    androidx.compose.material3.Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isPaired) partnerName ?: "Connected"
                        else "Private Chat"
                    )
                },
                actions = {
                    if (isPaired) {
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!isPaired) {
                PairingScreen(
                    pairingState = pairingState,
                    roomCode = roomCode,
                    onCreateRoom = { chatViewModel.createRoom() },
                    onJoinRoom = { code -> chatViewModel.joinRoom(code) },
                    onErrorDismiss = { chatViewModel.resetPairingError() }
                )
            } else {
                ChatMessagesContent(
                    messages = messages,
                    currentUserId = userId ?: "",
                    replyingTo = replyingTo,
                    error = error,
                    isRecording = isRecording,
                    onDismissError = { chatViewModel.clearError() },
                    onCancelReply = { chatViewModel.cancelReply() },
                    onSendMessage = { chatViewModel.sendMessage(it) },
                    onSendImage = { file -> chatViewModel.sendImage(file) },
                    onSendVoiceNote = { file, duration -> chatViewModel.sendVoiceNote(file, duration) },
                    onStartEdit = { msg ->
                        editTextField = msg.text
                        showEditSheet = true
                        chatViewModel.startEdit(msg)
                    },
                    onStartReply = { chatViewModel.startReply(it) },
                    onCopyText = { /* handled via clipboard manager inside bubble */ },
                    onSetRecording = { chatViewModel.setRecording(it) }
                )
            }
        }
    }
}

@Composable
private fun ChatSettingsSheetContent(
    partnerName: String?,
    roomCode: String?,
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Chat Settings", style = MaterialTheme.typography.titleLarge)
        if (partnerName != null) {
            Text("Connected to: $partnerName", style = MaterialTheme.typography.bodyMedium)
        }
        if (roomCode != null) {
            Text("Room: $roomCode", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        androidx.compose.material3.OutlinedButton(
            onClick = onDisconnect, modifier = Modifier.fillMaxWidth()
        ) { Text("Disconnect") }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatMessagesContent(
    messages: List<Message>,
    currentUserId: String,
    replyingTo: Message?,
    error: String?,
    isRecording: Boolean,
    onDismissError: () -> Unit,
    onCancelReply: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendImage: (File) -> Unit,
    onSendVoiceNote: (File, Int) -> Unit,
    onStartEdit: (Message) -> Unit,
    onStartReply: (Message) -> Unit,
    onCopyText: () -> Unit,
    onSetRecording: (Boolean) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var isCompressing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isCompressing = true
            scope.launch {
                val file = uriToCompressedJpegFile(context, uri)
                isCompressing = false
                if (file != null) onSendImage(file)
            }
        }
    }

    // Voice recording
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recordingStart by remember { mutableLongStateOf(0L) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceRecording(context) { rec, file ->
                recorder = rec
                recordingFile = file
                recordingStart = System.currentTimeMillis()
                onSetRecording(true)
            }
        }
    }

    fun stopAndSend() {
        val rec = recorder ?: return
        val file = recordingFile ?: return
        val durationSec = ((System.currentTimeMillis() - recordingStart) / 1000).toInt().coerceAtLeast(1)
        try { rec.stop() } catch (_: Exception) {}
        recorder = null
        recordingFile = null
        onSetRecording(false)
        if (durationSec >= 1) onSendVoiceNote(file, durationSec)
    }

    DisposableEffect(Unit) {
        onDispose {
            try { recorder?.stop() } catch (_: Exception) {}
            recorder?.release()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    LaunchedEffect(error) {
        if (error != null) { delay(3000); onDismissError() }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        // Reply preview bar
        if (replyingTo != null) {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Replying to ${if (replyingTo.senderId == currentUserId) "yourself" else "them"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                    Text(replyingTo.text.take(60),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
                IconButton(onClick = onCancelReply) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel reply", modifier = Modifier.size(18.dp))
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    isOwn = message.senderId == currentUserId,
                    onLongPress = { msg ->
                        if (msg.senderId == currentUserId && msg.text.isNotBlank()) {
                            onStartEdit(msg)
                        }
                    },
                    onReply = { onStartReply(it) },
                    onCopy = { text ->
                        clipboardManager.setText(AnnotatedString(text))
                    }
                )
            }
        }

        if (error != null) {
            Snackbar(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                action = { TextButton(onClick = onDismissError) { Text("Dismiss") } }
            ) { Text(error) }
        }

        // Input bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCompressing) {
                Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                }
            } else if (isRecording) {
                IconButton(onClick = { stopAndSend() }) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop recording",
                        tint = MaterialTheme.colorScheme.error)
                }
            } else {
                IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach photo",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                IconButton(onClick = {
                    val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.RECORD_AUDIO
                    else Manifest.permission.RECORD_AUDIO
                    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                        startVoiceRecording(context) { rec, file ->
                            recorder = rec; recordingFile = file
                            recordingStart = System.currentTimeMillis()
                            onSetRecording(true)
                        }
                    } else {
                        permissionLauncher.launch(permission)
                    }
                }) {
                    Icon(Icons.Default.Mic, contentDescription = "Voice note",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Message...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputText.isNotBlank()) { onSendMessage(inputText); inputText = "" }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) { onSendMessage(inputText); inputText = "" }
                },
                enabled = inputText.isNotBlank()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send",
                    tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isOwn: Boolean,
    onLongPress: (Message) -> Unit,
    onReply: (Message) -> Unit,
    onCopy: (String) -> Unit
) {
    var showActions by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!isOwn) Modifier.combinedClickable(
                onClick = {},
                onLongClick = { showActions = true }
            ) else Modifier)
    ) {
        // Reply preview
        if (message.replyToText != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (isOwn) 0.85f else 0.85f)
                    .padding(start = if (isOwn) 48.dp else 0.dp, end = if (isOwn) 0.dp else 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Text(message.replyToText, style = MaterialTheme.typography.bodySmall,
                    maxLines = 2, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
        ) {
            Box(
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                when {
                    message.mediaType == MediaType.IMAGE && message.mediaUrl != null -> {
                        AsyncImage(
                            model = message.mediaUrl,
                            contentDescription = "Photo",
                            modifier = Modifier
                                .widthIn(max = 220.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                    message.mediaType == MediaType.VOICE && message.mediaUrl != null -> {
                        // Voice note placeholder — in production, use ExoPlayer to play
                        Box(
                            modifier = Modifier
                                .widthIn(min = 150.dp, max = 220.dp)
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isOwn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Mic, contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(" ${message.mediaDurationSec ?: 0}s",
                                    color = if (isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(
                                    topStart = 18.dp, topEnd = 18.dp,
                                    bottomStart = if (isOwn) 18.dp else 4.dp,
                                    bottomEnd = if (isOwn) 4.dp else 18.dp
                                ))
                                .background(if (isOwn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text(
                                    text = message.text,
                                    color = if (isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (message.isEdited) {
                                    Text("edited",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isOwn) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.align(if (isOwn) Alignment.End else Alignment.Start)
                                    )
                                }
                            }
                        }
                    }
                }

                // Action buttons (reply, copy, edit) — show on long press for other's messages
                if (showActions) {
                    androidx.compose.material3.DropdownMenu(
                        expanded = showActions,
                        onDismissRequest = { showActions = false }
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Reply") },
                            onClick = { showActions = false; onReply(message) },
                            leadingIcon = { Icon(Icons.Default.Reply, null) }
                        )
                        if (message.text.isNotBlank()) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Copy") },
                                onClick = { showActions = false; onCopy(message.text) },
                                leadingIcon = { Icon(Icons.Default.CopyAll, null) }
                            )
                        }
                        if (isOwn && message.text.isNotBlank()) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = { showActions = false; onLongPress(message) },
                                leadingIcon = { Icon(Icons.Default.Edit, null) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Helpers ---

private fun startVoiceRecording(
    context: android.content.Context,
    onStarted: (MediaRecorder, File) -> Unit
) {
    try {
        val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        val recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        onStarted(recorder, file)
    } catch (_: Exception) {}
}

private suspend fun uriToCompressedJpegFile(
    context: android.content.Context,
    uri: android.net.Uri,
    maxDimension: Int = 800,
    quality: Int = 60
): File? {
    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val original = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (original == null) return@withContext null

            val scale = minOf(1f, maxDimension.toFloat() / maxOf(original.width, original.height))
            val scaled = if (scale < 1f) {
                android.graphics.Bitmap.createScaledBitmap(
                    original, (original.width * scale).toInt(), (original.height * scale).toInt(), true
                )
            } else original

            val file = File(context.cacheDir, "img_${System.currentTimeMillis()}.jpg")
            java.io.FileOutputStream(file).use { out ->
                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, out)
            }
            if (scaled !== original) scaled.recycle()
            file
        } catch (_: Exception) { null }
    }
}