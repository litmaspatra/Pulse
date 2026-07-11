package com.example.pulse.data

import android.content.Context
import java.io.File

class JournalRepository(
    private val context: Context
) {

    private val journalDirectory: File by lazy {

        File(
            context.getExternalFilesDir(null),
            "Journal"
        ).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    fun getJournalDirectory(): File = journalDirectory

}