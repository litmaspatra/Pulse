package com.example.pulse.data

data class JournalEntry(
    val fileName: String,       // e.g. "2026-07-11.md"
    val dateLabel: String,      // e.g. "Saturday, July 11, 2026"
    val timestampMillis: Long,  // for sorting, newest first
    val preview: String,        // first line of content, for the list view
    val content: String = ""
)