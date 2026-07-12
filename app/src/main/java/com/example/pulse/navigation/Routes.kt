package com.example.pulse.navigation

object Routes {
    // Main NavHost
    const val JOURNAL = "journal"
    const val JOURNAL_EDITOR = "journal_editor/{fileName}"
    const val CALENDAR = "calendar"
    const val ARCHIVE = "archive"
    const val TRASH = "trash"
    const val TRACKER = "tracker"
    const val SETTINGS = "settings"
    const val SETTINGS_APPEARANCE = "settings_appearance"
    const val SETTINGS_SECURITY = "settings_security"
    const val SETTINGS_BACKUP = "settings_backup"
    const val CHANGE_PIN = "change_pin"
    const val CHANGE_PIN_CONFIRM = "change_pin_confirm"
    const val CREATE_SECOND_PIN = "create_second_pin"
    const val CONFIRM_SECOND_PIN = "confirm_second_pin"

    fun journalEditor(fileName: String) = "journal_editor/$fileName"
}