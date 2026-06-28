package com.example.pulse.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class Message(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val timestamp: Long = 0L
)

class ChatRepository {

    private val db = FirebaseDatabase.getInstance().reference

    suspend fun roomExists(roomCode: String): Boolean {
        return try {
            val snapshot = db.child("rooms").child(roomCode).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createRoom(roomCode: String, userId: String): Boolean {
        return try {
            db.child("rooms").child(roomCode)
                .child("members").child("member1")
                .setValue(userId).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun joinRoom(roomCode: String, userId: String): Boolean {
        return try {
            val snapshot = db.child("rooms").child(roomCode)
                .child("members").get().await()
            if (!snapshot.exists()) return false
            if (snapshot.childrenCount >= 2L) return false
            db.child("rooms").child(roomCode)
                .child("members").child("member2")
                .setValue(userId).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendMessage(roomCode: String, message: Message): Boolean {
        return try {
            val key = db.child("rooms").child(roomCode)
                .child("messages").push().key ?: return false
            val msg = message.copy(id = key)
            db.child("rooms").child(roomCode)
                .child("messages").child(key)
                .setValue(msg).await()
            true
        } catch (e: Exception) {
            false
        }
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

    fun isPairedFlow(roomCode: String): Flow<Boolean> = callbackFlow {
        val ref = db.child("rooms").child(roomCode).child("members")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.childrenCount >= 2L)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}