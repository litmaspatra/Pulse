package com.example.pulse.data

data class Message(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val timestamp: Long = 0L,

    // --- Media ---
    // Download URL from Firebase Storage. Null for plain text.
    val mediaUrl: String? = null,
    val mediaType: MediaType? = null, // IMAGE, VOICE
    val mediaDurationSec: Int? = null, // Only for voice notes

    // --- Reply ---
    val replyToMessageId: String? = null,
    val replyToText: String? = null,
    val replyToSenderId: String? = null,

    // --- Edit ---
    val isEdited: Boolean = false,
    val originalText: String? = null
)

enum class MediaType {
    IMAGE,
    VOICE
}