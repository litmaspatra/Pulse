package com.example.pulse.data

import android.content.Context
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.concurrent.TimeUnit

data class RoomInfo(
    val roomCode: String = "",
    val createdAt: Long = 0L,
    val member1Id: String? = null,
    val member1Name: String? = null,
    val member2Id: String? = null,
    val member2Name: String? = null
)

class ChatRepository(private val context: Context) {

    private val db = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance()

    companion object {
        private const val ROOM_TTL_MS = 3L * 24 * 60 * 60 * 1000 // 3 days
    }

    // --- Room lifecycle ---

    suspend fun createRoom(roomCode: String, uid: String): Boolean {
        return try {
            val roomData = mapOf(
                "createdAt" to System.currentTimeMillis(),
                "member1/id" to uid,
                "member1/name" to DeviceIdProvider.getDeviceName()
            )
            db.child("rooms").child(roomCode).setValue(roomData).await()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun joinRoom(roomCode: String, uid: String): JoinResult {
        return try {
            val snapshot = db.child("rooms").child(roomCode).get().await()
            if (!snapshot.exists()) return JoinResult.NOT_FOUND
            if (snapshot.child("member2/id").exists()) return JoinResult.FULL

            val createdAt = snapshot.child("createdAt").getLong(Long.MAX_VALUE) ?: Long.MAX_VALUE
            if (System.currentTimeMillis() - createdAt > ROOM_TTL_MS) return JoinResult.EXPIRED

            // Check if this same device is already member1 (reconnecting)
            val m1Id = snapshot.child("member1/id").getValue(String::class.java)
            if (m1Id == uid) return JoinResult.RECONNECT_MEMBER1

            val updates = mapOf(
                "member2/id" to uid,
                "member2/name" to DeviceIdProvider.getDeviceName()
            )
            db.child("rooms").child(roomCode).updateChildren(updates).await()
            JoinResult.SUCCESS
        } catch (_: Exception) {
            JoinResult.FAILED
        }
    }

    enum class JoinResult { SUCCESS, NOT_FOUND, FULL, EXPIRED, RECONNECT_MEMBER1, FAILED }

    fun roomInfoFlow(roomCode: String): Flow<RoomInfo?> = callbackFlow {
        val ref = db.child("rooms").child(roomCode)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(null)
                    return
                }
                val info = RoomInfo(
                    roomCode = roomCode,
                    createdAt = snapshot.child("createdAt").getLong(0L) ?: 0L,
                    member1Id = snapshot.child("member1/id").getValue(String::class.java),
                    member1Name = snapshot.child("member1/name").getValue(String::class.java),
                    member2Id = snapshot.child("member2/id").getValue(String::class.java),
                    member2Name = snapshot.child("member2/name").getValue(String::class.java)
                )
                trySend(info)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun isPairedFlow(roomCode: String): Flow<Boolean> = callbackFlow {
        val ref = db.child("rooms").child(roomCode).child("member2/id")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.exists())
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Get the other person's device name for the header. */
    suspend fun getPartnerName(roomCode: String, myUid: String): String? {
        return try {
            val snapshot = db.child("rooms").child(roomCode).get().await()
            val m1Id = snapshot.child("member1/id").getValue(String::class.java)
            if (m1Id == myUid) {
                snapshot.child("member2/name").getValue(String::class.java)
            } else {
                snapshot.child("member1/name").getValue(String::class.java)
            }
        } catch (_: Exception) { null }
    }

    // --- Messages ---

    suspend fun sendMessage(roomCode: String, message: Message): Boolean {
        return try {
            val key = db.child("rooms").child(roomCode)
                .child("messages").push().key ?: return false
            val msg = message.copy(id = key)
            db.child("rooms").child(roomCode)
                .child("messages").child(key)
                .setValue(msg).await()
            true
        } catch (_: Exception) { false }
    }

    suspend fun editMessage(roomCode: String, messageId: String, newText: String): Boolean {
        return try {
            val originalSnap = db.child("rooms").child(roomCode)
                .child("messages").child(messageId).get().await()
            val originalText = originalSnap.child("text").getValue(String::class.java) ?: ""
            val updates = mapOf(
                "text" to newText,
                "isEdited" to true,
                "originalText" to originalText
            )
            db.child("rooms").child(roomCode)
                .child("messages").child(messageId)
                .updateChildren(updates).await()
            true
        } catch (_: Exception) { false }
    }

    fun messagesFlow(roomCode: String): Flow<List<Message>> = callbackFlow {
        val ref = db.child("rooms").child(roomCode).child("messages")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { child ->
                    child.getValue(Message::class.java)
                }.sortedBy { it.timestamp }
                trySend(messages)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // --- Media upload ---

    /**
     * Uploads an image file to Firebase Storage, returns the download URL.
     * Call from a coroutine.
     */
    suspend fun uploadImage(roomCode: String, file: File): String? {
        return try {
            val ref = storage.reference
                .child("rooms/$roomCode/images/${file.name}")
            ref.putFile(android.net.Uri.fromFile(file)).await()
            ref.downloadUrl.await().toString()
        } catch (_: Exception) { null }
    }

    /**
     * Uploads a voice note file to Firebase Storage, returns the download URL.
     */
    suspend fun uploadVoiceNote(roomCode: String, file: File): String? {
        return try {
            val ref = storage.reference
                .child("rooms/$roomCode/voice/${file.name}")
            ref.putFile(android.net.Uri.fromFile(file)).await()
            ref.downloadUrl.await().toString()
        } catch (_: Exception) { null }
    }

    // --- Disconnect ---

    suspend fun leaveRoom(roomCode: String, myUid: String): Boolean {
        return try {
            val snapshot = db.child("rooms").child(roomCode).get().await()
            val m1Id = snapshot.child("member1/id").getValue(String::class.java)
            val slot = if (m1Id == myUid) "member1" else "member2"
            db.child("rooms").child(roomCode)
                .child(slot).removeValue().await()
            true
        } catch (_: Exception) { false }
    }
}