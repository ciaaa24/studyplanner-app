package com.kelompok.studyplanner.data

import android.util.Log
import com.google.firebase.firestore.ListenerRegistration
import com.kelompok.studyplanner.model.Schedule

object ScheduleRepository {
    private const val TAG = "ScheduleRepository"

    /** Diurutkan berdasarkan jam mulai (di sisi aplikasi, jadi tidak butuh index Firestore). */
    fun listen(onChange: (List<Schedule>) -> Unit): ListenerRegistration {
        return FirestoreRefs.schedules()
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "Gagal memuat jadwal", error)
                    return@addSnapshotListener
                }
                val list = snap?.toObjects(Schedule::class.java) ?: emptyList()
                onChange(list.sortedBy { it.startTime })
            }
    }

    fun add(schedule: Schedule) {
        FirestoreRefs.schedules().document().set(schedule)
    }

    fun delete(scheduleId: String) {
        FirestoreRefs.schedules().document(scheduleId).delete()
    }
}
