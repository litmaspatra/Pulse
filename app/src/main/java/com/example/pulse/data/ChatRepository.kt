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

    enum class JoinResult { SUCCESS, NOT_FOUND, FULL, EXPIRED, FAILED }

    enum class RestoreResult {
        CLEAR,
        RESTORE_CONNECTED
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
        } catch (_: Exception) { false }
    }

    suspend fun joinRoom(roomCode: String, uid: String): JoinResult {
        return try {
            val snapshot = db.child("rooms").child(roomCode).get().await()
            if (!snapshot.exists()) return JoinResult.NOT_FOUND
            if (snapshot.child("member2/id").exists()) return JoinResult.FULL

            val createdAt = snapshot.child("createdAt").getLong(Long.MAX_VALUE) ?: Long.MAX_VALUE
            if (System.currentTimeMillis() - createdAt > ROOM_TTL_MS) return JoinResult.EXPIRED

            val m1Id = snapshot.child("member1/id").getValue(String::class.java)
            if (m1Id == uid) return JoinResult.FULL // Can't join your own room

            val updates = mapOf(
                "member2/id" to uid,
                "member2/name" to DeviceIdProvider.getDeviceName()
            )
            db.child("rooms").child(roomCode).updateChildren(updates).await()
            JoinResult.SUCCESS
        } catch (_: Exception) { JoinResult.FAILED }
    }

    /**
     * Called on app launch when a room code is saved locally.
     * Returns RESTORE_CONNECTED if the room still has 2 members (restore session).
     * Returns CLEAR if the room is empty, expired, or we're not in it (wipe local data).
     */
    suspend fun restoreRoom(roomCode: String, uid: String): RestoreResult {
        return try {
            val snapshot = db.child("rooms").child(roomCode).get().await()
            if (!snapshot.exists()) return RestoreResult.CLEAR

            val createdAt = snapshot.child("createdAt").getLong(0L) ?: 0L
            if (System.currentTimeMillis() - createdAt > ROOM_TTL_MS) return RestoreResult.CLEAR

            val m1Id = snapshot.child("member1/id").getValue(String::class.java)
            val m2Id = snapshot.child("member2/id").getValue(String::class.java)

            val isMember = (m1Id == uid || m2Id == uid)
            val isPaired = m2Id != null

            if (isMember && isPaired) RestoreResult.RESTORE_CONNECTED
            else RestoreResult.CLEAR
        } catch (_: Exception) { RestoreResult.CLEAR }
    }

    fun roomInfoFlow(roomCode: String): Flow<RoomInfo?> = callbackFlow {
        val ref = db.child("rooms").child(roomCode)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) { trySend(null); return }
                trySend(RoomInfo(
                    roomCode = roomCode,
                    createdAt = snapshot.child("createdAt").getLong(0L) ?: 0L,
                    member1Id = snapshot.child("member1/id").getValue(String::class.java),
                    member1Name = snapshot.child("member1/name").getValue(String::class.java),
                    member2Id = snapshot.child("member2/id").getValue(String::class.java),
                    member2Name = snapshot.child("member2/name").getValue(String::class.java)
                ))
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun isPairedFlow(roomCode: String): Flow<Boolean> = callbackFlow {
        val ref = db.child("rooms").child(roomCode).child("member2/id")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) { trySend(snapshot.exists()) }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun getPartnerName(roomCode: String, myUid: String): String? {
        return try {
            val snapshot = db.child("rooms").child(roomCode).get().await()
            val m1Id = snapshot.child("member1/id").getValue(String::class.java)
            if (m1Id == myUid) snapshot.child("member2/name").getValue(String::class.java)
            else snapshot.child("member1/name").getValue(String::class.java)
        } catch (_: Exception) { null }
    }

    // --- Messages ---

    suspend fun sendMessage(roomCode: String, message: Message): Boolean {
        return try {
            val key = db.child("rooms").child(roomCode)
                .child("messages").push().key ?: return false
            db.child("rooms").child(roomCode)
                .child("messages").child(key)
                .setValue(message.copy(id = key)).await()
            true
        } catch (_: Exception) { false }
    }

    suspend fun editMessage(roomCode: String, messageId: String, newText: String): Boolean {
        return try {
            val originalSnap = db.child("rooms").child(roomCode)
                .child("messages").child(messageId).get().await()
            val originalText = originalSnap.child("text").getValue(String::class.java) ?: ""
            db.child("rooms").child(roomCode)
                .child("messages").child(messageId)
                .updateChildren(mapOf(
                    "text" to newText,
                    "isEdited" to true,
                    "originalText" to originalText
                )).await()
            true
        } catch (_: Exception) { false }
    }

    fun messagesFlow(roomCode: String): Flow<List<Message>> = callbackFlow {
        val ref = db.child("rooms").child(roomCode).child("messages")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                    .sortedBy { it.timestamp })
            }
            override fun onCancelled(error: DatabaseError) { close(error.toException()) }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // --- Media upload (Firebase Storage, not base64) ---

    suspend fun uploadImage(roomCode: String, file: File): String? {
        return try {
            val ref = storage.reference.child("rooms/$roomCode/images/${file.name}")
            ref.putFile(android.net.Uri.fromFile(file)).await()
            ref.downloadUrl.await().toString()
        } catch (_: Exception) { null }
    }

    suspend fun uploadVoiceNote(roomCode: String, file: File): String? {
        return try {
            val ref = storage.reference.child("rooms/$roomCode/voice/${file.name}")
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
            db.child("rooms").child(roomCode).child(slot).removeValue().await()
            true
        } catch (_: Exception) { false }
    }
}