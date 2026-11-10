package com.kelompok.studyplanner.data

import com.google.firebase.Firebase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.firestore

/**
 * Semua path Firestore ada di sini.
 * Struktur: users/{uid}/(tasks | courses | schedules | focusSessions)/{id}
 */
object FirestoreRefs {
    private val db get() = Firebase.firestore

    fun users(): CollectionReference = db.collection("users")

    fun userDoc(): DocumentReference {
        val uid = AuthRepository.uid ?: throw IllegalStateException("Pengguna belum login")
        return users().document(uid)
    }

    fun tasks(): CollectionReference = userDoc().collection("tasks")
    fun courses(): CollectionReference = userDoc().collection("courses")
    fun schedules(): CollectionReference = userDoc().collection("schedules")
    fun focusSessions(): CollectionReference = userDoc().collection("focusSessions")
}
