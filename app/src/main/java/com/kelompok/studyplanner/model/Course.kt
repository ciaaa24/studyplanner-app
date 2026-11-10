package com.kelompok.studyplanner.model

import com.google.firebase.firestore.DocumentId

/** Dokumen: users/{uid}/courses/{courseId} */
data class Course(
    @DocumentId val id: String = "",
    val name: String = "",
    val lecturer: String = ""
)
