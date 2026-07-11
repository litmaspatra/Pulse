package com.example.pulse.data

data class JournalEntry(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
)