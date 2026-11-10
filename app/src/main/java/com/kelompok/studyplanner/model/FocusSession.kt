package com.kelompok.studyplanner.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/** Dokumen: users/{uid}/focusSessions/{sessionId} */
data class FocusSession(
    @DocumentId val id: String = "",
    val taskId: String? = null,
    val durationMinutes: Int = 0,
    val startedAt: Timestamp = Timestamp.now()
)
