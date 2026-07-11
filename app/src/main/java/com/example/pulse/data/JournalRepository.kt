// FILE: app/src/main/java/com/example/pulse/data/JournalRepository.kt
package com.example.pulse.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class EntryMeta(val createdLabel: String, val editedLabel: String)

class JournalRepository(context: Context) {

    val journalDirectory: File by lazy {
        File(context.getExternalFilesDir(null), "Journal").apply {
            if (!exists()) mkdirs()
        }
    }

    // Hidden subfolders keep archived/trashed .md files out of the active
    // listing automatically (listFiles with isFile already excludes
    // directories, but keeping them physically separate is clearer too).
    private val archiveDirectory: File by lazy {
        File(journalDirectory, ".archive").apply { if (!exists()) mkdirs() }
    }
    private val trashDirectory: File by lazy {
        File(journalDirectory, ".trash").apply { if (!exists()) mkdirs() }
    }

    private val newFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    private val legacyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormatWithTime = SimpleDateFormat("EEEE, MMMM d, yyyy 'at' h:mm a", Locale.US)
    private val displayFormatDateOnly = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
    private val timestampRegex = Regex("^(\\d{8}_\\d{6})")

    fun newEntryFileName(): String {
        val base = newFormat.format(Date())
        var candidate = "$base.md"
        var suffix = 1
        while (File(journalDirectory, candidate).exists()) {
            candidate = "${base}_${suffix++}.md"
        }
        return candidate
    }

    private fun parseFileName(fileName: String): Date? {
        val dateStr = fileName.removeSuffix(".md")
        val match = timestampRegex.find(dateStr)
        if (match != null) {
            try { return newFormat.parse(match.groupValues[1]) } catch (_: Exception) {}
        }
        return try { legacyFormat.parse(dateStr) } catch (_: Exception) { null }
    }

    fun formatFileNameToLabel(fileName: String): String {
        val dateStr = fileName.removeSuffix(".md")
        val date = parseFileName(fileName) ?: return dateStr
        return if (timestampRegex.containsMatchIn(dateStr))
            displayFormatWithTime.format(date)
        else
            displayFormatDateOnly.format(date)
    }

    private fun wordCount(content: String): Int =
        content.trim().let { if (it.isEmpty()) 0 else it.split(Regex("\\s+")).size }

    private fun listEntriesIn(dir: File): List<JournalEntry> {
        val files = dir.listFiles { f -> f.isFile && f.name.endsWith(".md") } ?: emptyArray()
        return files.mapNotNull { file ->
            val date = parseFileName(file.name) ?: return@mapNotNull null
            val content = try { file.readText() } catch (_: Exception) { "" }
            JournalEntry(
                fileName = file.name,
                dateLabel = formatFileNameToLabel(file.name),
                timestampMillis = date.time,
                preview = content.trim().lineSequence().firstOrNull()?.take(80).orEmpty(),
                content = content,
                wordCount = wordCount(content)
            )
        }.sortedByDescending { it.timestampMillis }
    }

    fun listEntries(): List<JournalEntry> = listEntriesIn(journalDirectory)
    fun listArchivedEntries(): List<JournalEntry> = listEntriesIn(archiveDirectory)
    fun listTrashEntries(): List<JournalEntry> = listEntriesIn(trashDirectory)

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

    private fun moveFile(from: File, to: File): Boolean {
        return try {
            if (!from.exists()) return false
            to.parentFile?.let { if (!it.exists()) it.mkdirs() }
            if (from.renameTo(to)) {
                true
            } else {
                // renameTo can fail across filesystems/volumes — fall back to copy+delete.
                to.writeBytes(from.readBytes())
                from.delete()
            }
        } catch (_: Exception) {
            false
        }
    }

    fun archiveEntry(fileName: String): Boolean =
        moveFile(File(journalDirectory, fileName), File(archiveDirectory, fileName))

    fun unarchiveEntry(fileName: String): Boolean =
        moveFile(File(archiveDirectory, fileName), File(journalDirectory, fileName))

    /** Soft delete: moves the file to Trash instead of deleting it outright. */
    fun moveToTrash(fileName: String): Boolean {
        val activeFile = File(journalDirectory, fileName)
        val source = if (activeFile.exists()) activeFile else File(archiveDirectory, fileName)
        return moveFile(source, File(trashDirectory, fileName))
    }

    fun restoreFromTrash(fileName: String): Boolean =
        moveFile(File(trashDirectory, fileName), File(journalDirectory, fileName))

    fun permanentlyDelete(fileName: String): Boolean {
        return try { File(trashDirectory, fileName).delete() } catch (_: Exception) { false }
    }

    fun emptyTrash(): Boolean {
        return try {
            trashDirectory.listFiles()?.forEach { it.delete() }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun entryMeta(fileName: String): EntryMeta {
        val created = parseFileName(fileName) ?: Date()
        val file = File(journalDirectory, fileName)
        val edited = if (file.exists()) Date(file.lastModified()) else created
        return EntryMeta(
            createdLabel = displayFormatWithTime.format(created),
            editedLabel = displayFormatWithTime.format(edited)
        )
    }

    /**
     * Active + archived entries grouped by day-of-month, for the Calendar
     * view. Trashed entries are excluded.
     */
    fun entriesForMonth(year: Int, month: Int): Map<Int, List<JournalEntry>> {
        val all = listEntries() + listArchivedEntries()
        val cal = Calendar.getInstance()
        return all.filter { entry ->
            cal.timeInMillis = entry.timestampMillis
            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
        }.groupBy { entry ->
            cal.timeInMillis = entry.timestampMillis
            cal.get(Calendar.DAY_OF_MONTH)
        }
    }
}