package com.kelompok.studyplanner.data

import android.util.Log
import com.google.firebase.firestore.ListenerRegistration
import com.kelompok.studyplanner.model.Course

object CourseRepository {
    private const val TAG = "CourseRepository"

    fun listen(onChange: (List<Course>) -> Unit): ListenerRegistration {
        return FirestoreRefs.courses()
            .orderBy("name")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "Gagal memuat mata kuliah", error)
                    return@addSnapshotListener
                }
                onChange(snap?.toObjects(Course::class.java) ?: emptyList())
            }
    }

    fun add(name: String, lecturer: String, onSaved: (String) -> Unit = {}) {
        val ref = FirestoreRefs.courses().document()
        ref.set(Course(name = name, lecturer = lecturer))
        onSaved(ref.id)
    }
}
