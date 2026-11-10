package com.kelompok.studyplanner.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.auth

object AuthRepository {
    private val auth get() = Firebase.auth

    val uid: String? get() = auth.currentUser?.uid
    val isLoggedIn: Boolean get() = auth.currentUser != null

    fun register(
        name: String,
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onResult(false, "Gagal membuat akun")
                    return@addOnSuccessListener
                }
                FirestoreRefs.users().document(user.uid)
                    .set(mapOf("name" to name, "email" to email))
                onResult(true, null)
            }
            .addOnFailureListener { onResult(false, friendlyError(it)) }
    }

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, friendlyError(it)) }
    }

    fun logout() {
        auth.signOut()
    }

    fun fetchName(onResult: (String) -> Unit) {
        val id = uid ?: return
        FirestoreRefs.users().document(id).get()
            .addOnSuccessListener { onResult(it.getString("name") ?: "") }
            .addOnFailureListener { onResult("") }
    }

    private fun friendlyError(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "Kata sandi terlalu lemah (minimal 6 karakter)"
        is FirebaseAuthUserCollisionException -> "Email sudah terdaftar"
        is FirebaseAuthInvalidUserException -> "Akun tidak ditemukan"
        is FirebaseAuthInvalidCredentialsException -> "Email atau kata sandi salah"
        is FirebaseNetworkException -> "Tidak ada koneksi internet"
        else -> e.message ?: "Terjadi kesalahan"
    }
}
