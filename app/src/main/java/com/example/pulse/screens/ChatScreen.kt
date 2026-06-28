package com.example.pulse.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pulse.data.Message
import com.example.pulse.data.PinStorage
import com.example.pulse.viewmodel.ChatViewModel
import com.example.pulse.viewmodel.ChatViewModelFactory
import com.example.pulse.viewmodel.PairingState

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(PinStorage(context))
    )

    val pairingState by chatViewModel.pairingState.collectAsStateWithLifecycle()
    val isPaired by chatViewModel.isPaired.collectAsStateWithLifecycle()
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val roomCode by chatViewModel.roomCode.collectAsStateWithLifecycle()
    val userId by chatViewModel.userId.collectAsStateWithLifecycle()

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
            onSendMessage = { chatViewModel.sendMessage(it) }
        )
    }
}

@Composable
private fun ChatContent(
    messages: List<Message>,
    currentUserId: String,
    onSendMessage: (String) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
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
                Text(text = "🌱", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
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

        if (showEmojiPicker) {
            EmojiPicker(
                onEmojiSelected = { emoji ->
                    inputText += emoji
                    showEmojiPicker = false
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { showEmojiPicker = !showEmojiPicker }) {
                Icon(
                    imageVector = Icons.Default.EmojiEmotions,
                    contentDescription = "Emoji",
                    tint = if (showEmojiPicker)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
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

@Composable
private fun EmojiPicker(
    onEmojiSelected: (String) -> Unit
) {
    val emojis = listOf(
        "❤️", "🌱", "✨", "😊", "😂", "🥰", "😍", "🤗",
        "😘", "🙏", "👍", "👎", "🔥", "💯", "🎉", "😭",
        "😅", "🤣", "😇", "🥺", "😢", "😤", "😴", "🤔"
    )

    Surface(
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(8),
            modifier = Modifier.padding(8.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(emojis.size) { index ->
                IconButton(
                    onClick = { onEmojiSelected(emojis[index]) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Text(text = emojis[index], fontSize = 22.sp)
                }
            }
        }
    }
}