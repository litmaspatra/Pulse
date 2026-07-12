package com.example.pulse.data

import android.content.Context
import com.google.firebase.installations.FirebaseInstallations
import kotlinx.coroutines.tasks.await

/**
 * Provides a stable, unique ID for this app installation.
 * Used to identify devices in chat rooms so reconnection
 * within 3 days restores history.
 */
object DeviceIdProvider {

    private var cachedId: String? = null

    suspend fun getId(context: Context): String {
        cachedId?.let { return it }
        return try {
            val id = FirebaseInstallations.getInstance().id.await()
            cachedId = id
            id
        } catch (_: Exception) {
            // Fallback: generate and store locally
            getLocalId(context)
        }
    }

    fun getDeviceName(): String {
        return android.os.Build.MODEL ?: "Unknown Device"
    }

    private fun getLocalId(context: Context): String {
        val prefs = context.getSharedPreferences("pulse_device", Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        cachedId = id
        return id
    }
}