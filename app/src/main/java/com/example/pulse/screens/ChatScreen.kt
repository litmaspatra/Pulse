package com.example.pulse.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pulse.data.Message
import com.example.pulse.data.PinStorage
import com.example.pulse.viewmodel.ChatViewModel
import com.example.pulse.viewmodel.ChatViewModelFactory
import java.io.ByteArrayOutputStream

@Composable
fun ChatScreen(onDisconnected: () -> Unit) {
    val context = LocalContext.current
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(PinStorage(context))
    )

    val pairingState by chatViewModel.pairingState.collectAsStateWithLifecycle()
    val isPaired by chatViewModel.isPaired.collectAsStateWithLifecycle()
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val roomCode by chatViewModel.roomCode.collectAsStateWithLifecycle()
    val userId by chatViewModel.userId.collectAsStateWithLifecycle()
    val imageError by chatViewModel.imageError.collectAsStateWithLifecycle()

    if (!isPaired) {
        PairingScreen(
            pairingState = pairingState,
            roomCode = roomCode,
            onCreateRoom = { chatViewModel.createRoom() },
            onJoinRoom = { code -> chatViewModel.joinRoom(code) },
            onErrorDismiss = { chatViewModel.resetPairingError() }
        )
    } else {
        ChatContent(
            messages = messages,
            currentUserId = userId ?: "",
            imageError = imageError,
            onDismissImageError = { chatViewModel.clearImageError() },
            onSendMessage = { chatViewModel.sendMessage(it) },
            onSendImage = { bytes -> chatViewModel.sendImage(bytes) },
            onDisconnect = {
                chatViewModel.disconnect()
                onDisconnected()
            }
        )
    }
}

@Composable
private fun ChatContent(
    messages: List<Message>,
    currentUserId: String,
    imageError: String?,
    onDismissImageError: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendImage: (ByteArray) -> Unit,
    onDisconnect: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showDisconnectConfirm by remember { mutableStateOf(false) }
    var isCompressing by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isCompressing = true
            val bytes = uriToCompressedJpegBytes(context, uri)
            isCompressing = false
            if (bytes != null) {
                onSendImage(bytes)
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showDisconnectConfirm) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirm = false },
            title = { Text("Disconnect?") },
            text = { Text("You'll leave this chat. Your partner can stay, and the chat history is kept if you reconnect later with the same code.") },
            confirmButton = {
                TextButton(onClick = {
                    showDisconnectConfirm = false
                    onDisconnect()
                }) {
                    Text("Disconnect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(imageError) {
        if (imageError != null) {
            kotlinx.coroutines.delay(3000)
            onDismissImageError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Surface(
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pulse",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Options"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Disconnect") },
                            onClick = {
                                showMenu = false
                                showDisconnectConfirm = true
                            }
                        )
                    }
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
                    isOwn = message.senderId == currentUserId
                )
            }
        }

        if (imageError != null) {
            Snackbar(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                action = {
                    TextButton(onClick = onDismissImageError) { Text("Dismiss") }
                }
            ) {
                Text(imageError)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isCompressing) {
                Box(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                }
            } else {
                IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = "Attach photo",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
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
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    }
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            )

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank())
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    isOwn: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        if (message.imageBase64 != null) {
            val bitmap = remember(message.imageBase64) {
                decodeBase64ToBitmap(message.imageBase64)
            }
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 220.dp)
                        .clip(RoundedCornerShape(14.dp))
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Text(
                    text = "Photo couldn't be loaded",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isOwn) 18.dp else 4.dp,
                            bottomEnd = if (isOwn) 4.dp else 18.dp
                        )
                    )
                    .background(
                        if (isOwn) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.text,
                    color = if (isOwn)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

private fun decodeBase64ToBitmap(base64: String): Bitmap? {
    return try {
        val bytes = android.util.Base64.decode(base64, android.util.Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
        null
    }
}

/**
 * Reads an image from the given content URI, downsamples it aggressively,
 * and re-encodes as JPEG. Since this now lives directly in Realtime Database
 * (no Storage product), we compress harder than we would for Storage to
 * keep each message small and stay within the free-tier database budget.
 */
private fun uriToCompressedJpegBytes(
    context: android.content.Context,
    uri: android.net.Uri,
    maxDimension: Int = 800,
    quality: Int = 60
): ByteArray? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        if (original == null) return null

        val scale = minOf(
            1f,
            maxDimension.toFloat() / maxOf(original.width, original.height)
        )
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                original,
                (original.width * scale).toInt(),
                (original.height * scale).toInt(),
                true
            )
        } else {
            original
        }

        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        outputStream.toByteArray()
    } catch (_: Exception) {
        null
    }
}