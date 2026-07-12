// FILE: app/src/main/java/com/example/pulse/data/LockTimeout.kt
package com.example.pulse.data

enum class LockTimeout(val label: String, val millis: Long) {
    IMMEDIATE("Immediately", 0L),
    THIRTY_SECONDS("After 30 seconds", 30_000L),
    ONE_MINUTE("After 1 minute", 60_000L),
    FIVE_MINUTES("After 5 minutes", 5 * 60_000L);

    companion object {
        fun fromName(name: String?): LockTimeout = entries.find { it.name == name } ?: IMMEDIATE
    }
}