package com.kelompok.studyplanner.model

import com.google.firebase.firestore.DocumentId

/** Dokumen: users/{uid}/schedules/{scheduleId}. dayOfWeek: 1 = Senin ... 7 = Minggu. */
data class Schedule(
    @DocumentId val id: String = "",
    val courseId: String? = null,
    val courseName: String = "",
    val dayOfWeek: Int = 1,
    val startTime: String = "08:00", // format HH:mm
    val endTime: String = "09:40",
    val room: String = ""
)
