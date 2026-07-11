// FILE: app/src/main/java/com/example/pulse/data/JournalEntry.kt
package com.example.pulse.data

data class JournalEntry(
    val fileName: String,
    val dateLabel: String,
    val timestampMillis: Long,
    val preview: String,
    val content: String = "",
    val wordCount: Int = 0
)