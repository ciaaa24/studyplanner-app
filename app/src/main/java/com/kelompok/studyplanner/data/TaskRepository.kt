package com.kelompok.studyplanner.data

import android.util.Log
import com.google.firebase.firestore.ListenerRegistration
import com.kelompok.studyplanner.model.Task

object TaskRepository {
    private const val TAG = "TaskRepository"

    /** Mendengarkan perubahan tugas secara realtime. Panggil remove() di onDestroy. */
    fun listen(onChange: (List<Task>) -> Unit): ListenerRegistration {
        return FirestoreRefs.tasks()
            .orderBy("deadline")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "Gagal memuat tugas", error)
                    return@addSnapshotListener
                }
                onChange(snap?.toObjects(Task::class.java) ?: emptyList())
            }
    }

    fun get(taskId: String, onResult: (Task?) -> Unit) {
        FirestoreRefs.tasks().document(taskId).get()
            .addOnSuccessListener { onResult(it.toObject(Task::class.java)) }
            .addOnFailureListener { onResult(null) }
    }

    /** Tambah (id kosong) atau ubah (id terisi). Callback berisi id dokumen. */
    fun save(task: Task, onSaved: (String) -> Unit) {
        val ref = if (task.id.isEmpty()) {
            FirestoreRefs.tasks().document()
        } else {
            FirestoreRefs.tasks().document(task.id)
        }
        ref.set(task)
        onSaved(ref.id)
    }

    fun setDone(taskId: String, done: Boolean) {
        FirestoreRefs.tasks().document(taskId).update("isDone", done)
    }

    fun delete(taskId: String) {
        FirestoreRefs.tasks().document(taskId).delete()
    }
}
