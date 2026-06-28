package com.example.pulse.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

/**
 * Wraps Firebase Anonymous Auth. Every device gets a real, server-verifiable
 * uid the first time the app runs. This uid is what Realtime Database security
 * rules check against — it's the foundation that makes "private" rooms possible.
 *
 * The user never sees a login screen; sign-in happens silently in the background.
 */
class AuthRepository {

    private val auth = FirebaseAuth.getInstance()

    val currentUid: String?
        get() = auth.currentUser?.uid

    /**
     * Signs in anonymously if not already signed in. Returns the stable uid
     * for this device. Safe to call every app launch — Firebase Auth persists
     * the session locally, so this is a no-op after the first run.
     */
    suspend fun ensureSignedIn(): String? {
        val existing = auth.currentUser
        if (existing != null) return existing.uid

        return try {
            val result = auth.signInAnonymously().await()
            result.user?.uid
        } catch (_: Exception) {
            null
        }
    }
}