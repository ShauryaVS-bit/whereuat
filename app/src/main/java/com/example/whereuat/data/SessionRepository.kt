package com.example.whereuat.data

import com.example.whereuat.model.AppUser
import com.example.whereuat.model.EventSession
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class SessionRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun createSession(name: String, ownerId: String): String {
        val doc = db.collection("sessions").document()
        val payload = mapOf(
            "id" to doc.id,
            "name" to name,
            "members" to listOf(mapOf("id" to ownerId, "displayName" to "Me"))
        )
        doc.set(payload).await()
        return doc.id
    }

    suspend fun joinSession(sessionId: String, user: AppUser) {
        db.collection("sessions").document(sessionId).update(
            "members",
            com.google.firebase.firestore.FieldValue.arrayUnion(
                mapOf("id" to user.id, "displayName" to user.displayName)
            )
        ).await()
    }

    fun observeSession(sessionId: String): Flow<EventSession?> = callbackFlow {
        val reg = db.collection("sessions").document(sessionId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val membersAny = snapshot.get("members") as? List<Map<String, String>> ?: emptyList()
                val members = membersAny.map { AppUser(id = it["id"].orEmpty(), displayName = it["displayName"].orEmpty()) }
                trySend(EventSession(id = snapshot.id, name = snapshot.getString("name").orEmpty(), members = members))
            }
        awaitClose { reg.remove() }
    }
}
