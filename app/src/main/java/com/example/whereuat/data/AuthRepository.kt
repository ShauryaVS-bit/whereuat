package com.example.whereuat.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    suspend fun signInAnonymouslyIfNeeded(): String {
        val current = auth.currentUser
        if (current != null) return current.uid
        return auth.signInAnonymously().await().user?.uid.orEmpty()
    }
}
