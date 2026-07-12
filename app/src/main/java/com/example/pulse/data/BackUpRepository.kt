// FILE: app/src/main/java/com/example/pulse/data/BackupRepository.kt
package com.example.pulse.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ImportCounts(val imported: Int, val skipped: Int)

class BackupRepository(
    private val context: Context,
    private val journalRepository: JournalRepository
) {
    private val exportsDir: File by lazy {
        File(context.getExternalFilesDir(null), "Exports").apply { if (!exists()) mkdirs() }
    }

    private val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /** Bundles active + archived entries into one shareable text file, returns a content:// Uri. */
    fun exportJournal(): Uri? {
        return try {
            val entries = journalRepository.listEntries() + journalRepository.listArchivedEntries()
            val builder = StringBuilder("PULSE_JOURNAL_EXPORT_V1\n")
            entries.forEach { entry ->
                builder.append("---ENTRY---\n")
                builder.append("FILENAME: ${entry.fileName}\n")
                builder.append("---CONTENT---\n")
                builder.append(entry.content)
                if (!entry.content.endsWith("\n")) builder.append("\n")
                builder.append("---END---\n")
            }

            val file = File(exportsDir, "pulse_export_${timestampFormat.format(Date())}.txt")
            file.writeText(builder.toString())

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            null
        }
    }

    /** Parses a previously-exported bundle. Skips any entry whose filename already exists locally. */
    fun importJournal(uri: Uri): ImportCounts? {
        return try {
            val text = context.contentResolver.openInputStream(uri)
                ?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: return null
            if (!text.startsWith("PULSE_JOURNAL_EXPORT_V1")) return null

            var imported = 0
            var skipped = 0

            text.split("---ENTRY---\n").drop(1).forEach { block ->
                val fileName = block.lineSequence()
                    .firstOrNull { it.startsWith("FILENAME: ") }
                    ?.removePrefix("FILENAME: ")?.trim()
                    ?: return@forEach

                val contentMarker = "---CONTENT---\n"
                val endMarker = "---END---"
                val contentStart = block.indexOf(contentMarker)
                val contentEnd = block.indexOf(endMarker)
                if (contentStart == -1 || contentEnd == -1 || contentEnd <= contentStart) return@forEach

                val content = block.substring(contentStart + contentMarker.length, contentEnd)
                    .removeSuffix("\n")

                val targetFile = File(journalRepository.journalDirectory, fileName)
                if (targetFile.exists()) {
                    skipped++
                } else {
                    journalRepository.saveEntry(fileName, content)
                    imported++
                }
            }

            ImportCounts(imported, skipped)
        } catch (_: Exception) {
            null
        }
    }
}