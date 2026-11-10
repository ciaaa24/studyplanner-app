package com.kelompok.studyplanner.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Dokumen: users/{uid}/tasks/{taskId}
 * Semua properti punya nilai default supaya Firestore bisa membuat objek (wajib).
 */
data class Task(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val courseId: String? = null,
    val courseName: String? = null,
    val deadline: Timestamp = Timestamp.now(),
    val priority: String = "sedang", // rendah / sedang / tinggi
    // Anotasi ini penting: tanpa itu Kotlin menyimpan field "isDone" sebagai "done".
    @get:PropertyName("isDone") @set:PropertyName("isDone") var isDone: Boolean = false,
    val createdAt: Timestamp = Timestamp.now()
)
