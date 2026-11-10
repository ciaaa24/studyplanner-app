package com.kelompok.studyplanner.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.kelompok.studyplanner.model.FocusSession
import com.kelompok.studyplanner.util.DateUtil
import java.util.Date

object FocusRepository {
    private const val TAG = "FocusRepository"

    /** Simpan satu sesi fokus yang SELESAI. */
    fun add(taskId: String?, minutes: Int) {
        val startedAt = Timestamp(Date(System.currentTimeMillis() - minutes * 60_000L))
        FirestoreRefs.focusSessions().document().set(
            FocusSession(taskId = taskId, durationMinutes = minutes, startedAt = startedAt)
        )
    }

    /** Total menit dan jumlah sesi fokus hari ini, diperbarui realtime. */
    fun listenToday(onChange: (totalMinutes: Int, sessions: Int) -> Unit): ListenerRegistration {
        val startOfToday = Timestamp(Date(DateUtil.startOfToday()))
        return FirestoreRefs.focusSessions()
            .whereGreaterThanOrEqualTo("startedAt", startOfToday)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w(TAG, "Gagal memuat sesi fokus", error)
                    return@addSnapshotListener
                }
                val list = snap?.toObjects(FocusSession::class.java) ?: emptyList()
                onChange(list.sumOf { it.durationMinutes }, list.size)
            }
    }
}
