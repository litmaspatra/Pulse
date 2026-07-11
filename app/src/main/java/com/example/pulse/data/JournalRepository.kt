package com.example.pulse.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class JournalRepository(context: Context) {

    val journalDirectory: File by lazy {
        File(context.getExternalFilesDir(null), "Journal").apply {
            if (!exists()) mkdirs()
        }
    }

    // Filename format: 2026-07-11.md — one entry per calendar day.
    private val fileNameFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)

    fun todayFileName(): String = "${fileNameFormat.format(Date())}.md"

    fun formatFileNameToLabel(fileName: String): String {
        val dateStr = fileName.removeSuffix(".md")
        val date = try { fileNameFormat.parse(dateStr) } catch (_: Exception) { null }
        return date?.let { displayFormat.format(it) } ?: dateStr
    }

    fun listEntries(): List<JournalEntry> {
        val files = journalDirectory.listFiles { f -> f.isFile && f.name.endsWith(".md") }
            ?: emptyArray()

        return files.mapNotNull { file ->
            val dateStr = file.name.removeSuffix(".md")
            val date = try { fileNameFormat.parse(dateStr) } catch (_: Exception) { null }
                ?: return@mapNotNull null

            val content = try { file.readText() } catch (_: Exception) { "" }

            JournalEntry(
                fileName = file.name,
                dateLabel = displayFormat.format(date),
                timestampMillis = date.time,
                preview = content.trim().lineSequence().firstOrNull()?.take(80).orEmpty(),
                content = content
            )
        }.sortedByDescending { it.timestampMillis }
    }

    fun readEntry(fileName: String): String {
        val file = File(journalDirectory, fileName)
        return if (file.exists()) {
            try { file.readText() } catch (_: Exception) { "" }
        } else ""
    }

    fun saveEntry(fileName: String, content: String): Boolean {
        return try {
            File(journalDirectory, fileName).writeText(content)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun deleteEntry(fileName: String): Boolean {
        return try {
            File(journalDirectory, fileName).delete()
        } catch (_: Exception) {
            false
        }
    }
}