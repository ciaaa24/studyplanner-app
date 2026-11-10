package com.kelompok.studyplanner.util

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.kelompok.studyplanner.model.Schedule

/** Dijalankan WorkManager saat waktu pengingat tiba, lalu menampilkan notifikasi. */
class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val text = inputData.getString(KEY_TEXT) ?: ""

        // Jangan tampilkan pengingat basi (misalnya HP baru menyala setelah kuliah/deadline lewat).
        val expiresAt = inputData.getLong(KEY_EXPIRES_AT, 0L)
        if (expiresAt <= 0L || System.currentTimeMillis() <= expiresAt) {
            NotificationHelper.show(applicationContext, id.hashCode(), title, text)
        }

        // Pengingat kuliah berulang tiap minggu: pasang untuk minggu berikutnya.
        if (inputData.getBoolean(KEY_REPEAT_WEEKLY, false)) {
            val scheduleId = inputData.getString(KEY_SCHEDULE_ID)
            if (!scheduleId.isNullOrEmpty()) {
                ReminderScheduler.scheduleClass(
                    applicationContext,
                    Schedule(
                        id = scheduleId,
                        courseName = inputData.getString(KEY_COURSE_NAME) ?: "",
                        dayOfWeek = inputData.getInt(KEY_DAY, 1),
                        startTime = inputData.getString(KEY_START) ?: "08:00",
                        room = inputData.getString(KEY_ROOM) ?: ""
                    )
                )
            }
        }
        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_TEXT = "text"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_REPEAT_WEEKLY = "repeat_weekly"
        const val KEY_SCHEDULE_ID = "schedule_id"
        const val KEY_COURSE_NAME = "course_name"
        const val KEY_DAY = "day"
        const val KEY_START = "start"
        const val KEY_ROOM = "room"
    }
}
