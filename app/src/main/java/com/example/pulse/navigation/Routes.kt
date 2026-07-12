// FILE: app/src/main/java/com/example/pulse/navigation/Routes.kt
package com.example.pulse.navigation

object Routes {
    const val SPLASH = "splash"
    const val PIN = "pin"
    const val CREATE_PIN = "create_pin"
    const val CONFIRM_PIN = "confirm_pin"
    const val SECURITY_QUESTION_SETUP = "security_question_setup"
    const val FORGOT_PIN = "forgot_pin"
    const val RESET_PIN = "reset_pin"
    const val RESET_CONFIRM_PIN = "reset_confirm_pin"
    const val CREATE_SECOND_PIN = "create_second_pin"
    const val CONFIRM_SECOND_PIN = "confirm_second_pin"
    const val CHANGE_PIN = "change_pin"
    const val CHANGE_PIN_CONFIRM = "change_pin_confirm"
    const val CHAT = "chat"
    const val CHAT_SETTINGS = "chat_settings"
    const val TRACKER = "tracker"
    const val JOURNAL = "journal"
    const val JOURNAL_EDITOR = "journal_editor/{fileName}"
    const val SETTINGS = "settings"
    const val CALENDAR = "calendar"
    const val ARCHIVE = "archive"
    const val TRASH = "trash"

    fun journalEditor(fileName: String) = "journal_editor/$fileName"
}