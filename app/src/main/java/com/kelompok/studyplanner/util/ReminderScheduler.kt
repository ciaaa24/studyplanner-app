package com.kelompok.studyplanner.util

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.Timestamp
import com.kelompok.studyplanner.model.Schedule
import com.kelompok.studyplanner.model.Task
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Menjadwalkan notifikasi lewat WorkManager (waktu tidak selalu presisi).
 *
 * 1. Pengingat deadline tugas: SELALU tiga sekaligus, yaitu 1 hari, 3 jam, dan 1 jam sebelum deadline.
 * 2. Pengingat jadwal kuliah: tiap minggu, [CLASS_LEAD_MINUTES] menit sebelum jam mulai kuliah.
 */
object ReminderScheduler {

    /** Pengingat deadline (dalam jam). Ketiganya aktif untuk setiap tugas. */
    val DEADLINE_REMINDER_HOURS = listOf(24, 3, 1)

    /** Berapa menit sebelum kuliah dimulai pengingat dikirim. Ubah angka ini jika ingin lebih awal. */
    const val CLASS_LEAD_MINUTES = 15

    private const val HOUR_MS = 3_600_000L
    private const val MINUTE_MS = 60_000L
    private const val TAG_CLASS = "class_reminder"

    private fun taskWorkName(taskId: String, hours: Int) = "reminder_${taskId}_$hours"
    private fun classWorkName(scheduleId: String) = "class_$scheduleId"

    // ---------------------------------------------------------------- Deadline tugas

    /** Pasang ketiga pengingat deadline. Pengingat yang waktunya sudah lewat dilewati. */
    fun scheduleTask(context: Context, taskId: String, title: String, deadline: Timestamp) {
        val wm = WorkManager.getInstance(context.applicationContext)
        val deadlineMs = deadline.toDate().time
        val now = System.currentTimeMillis()

        for (hours in DEADLINE_REMINDER_HOURS) {
            val name = taskWorkName(taskId, hours)
            val delay = deadlineMs - hours * HOUR_MS - now
            if (delay <= 0) {
                wm.cancelUniqueWork(name) // sudah lewat, atau deadline dimajukan
                continue
            }
            val label = if (hours >= 24) "1 hari lagi" else "$hours jam lagi"
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(
                    workDataOf(
                        ReminderWorker.KEY_TITLE to "Deadline mendekat: $title",
                        ReminderWorker.KEY_TEXT to "Batas waktu $label. Yuk selesaikan.",
                        ReminderWorker.KEY_EXPIRES_AT to deadlineMs
                    )
                )
                .build()
            wm.enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
        }
    }

    fun cancelTask(context: Context, taskId: String) {
        val wm = WorkManager.getInstance(context.applicationContext)
        for (hours in DEADLINE_REMINDER_HOURS) wm.cancelUniqueWork(taskWorkName(taskId, hours))
        wm.cancelUniqueWork("reminder_$taskId") // nama lama (versi sebelum 3 pengingat)
    }

    /** Samakan jadwal pengingat dengan daftar tugas di Firestore (dipanggil tiap data berubah). */
    fun syncTasks(context: Context, tasks: List<Task>) {
        for (t in tasks) {
            if (t.id.isEmpty()) continue
            if (t.isDone) cancelTask(context, t.id) else scheduleTask(context, t.id, t.title, t.deadline)
        }
    }

    // ---------------------------------------------------------------- Jadwal kuliah

    /** Pasang pengingat kuliah berikutnya. Setelah berbunyi, worker memasang minggu depannya. */
    fun scheduleClass(context: Context, schedule: Schedule) {
        if (schedule.id.isEmpty()) return
        val now = System.currentTimeMillis()
        val trigger = nextClassReminderTime(schedule.dayOfWeek, schedule.startTime, now + MINUTE_MS)
            ?: return
        val classStart = trigger + CLASS_LEAD_MINUTES * MINUTE_MS

        val place = if (schedule.room.isBlank()) "" else " di ${schedule.room}"
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(trigger - now, TimeUnit.MILLISECONDS)
            .addTag(TAG_CLASS)
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_TITLE to "Kuliah $CLASS_LEAD_MINUTES menit lagi: ${schedule.courseName}",
                    ReminderWorker.KEY_TEXT to "Mulai ${schedule.startTime}$place. Jangan lupa bersiap.",
                    ReminderWorker.KEY_EXPIRES_AT to classStart,
                    ReminderWorker.KEY_REPEAT_WEEKLY to true,
                    ReminderWorker.KEY_SCHEDULE_ID to schedule.id,
                    ReminderWorker.KEY_COURSE_NAME to schedule.courseName,
                    ReminderWorker.KEY_DAY to schedule.dayOfWeek,
                    ReminderWorker.KEY_START to schedule.startTime,
                    ReminderWorker.KEY_ROOM to schedule.room
                )
            )
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(classWorkName(schedule.id), ExistingWorkPolicy.REPLACE, request)
    }

    /** Ganti semua pengingat kuliah dengan daftar jadwal terbaru (jadwal yang dihapus ikut hilang). */
    fun syncClasses(context: Context, schedules: List<Schedule>) {
        WorkManager.getInstance(context.applicationContext).cancelAllWorkByTag(TAG_CLASS)
        schedules.forEach { scheduleClass(context, it) }
    }

    /** Dipakai saat logout supaya pengingat akun lama tidak muncul di akun lain. */
    fun cancelAll(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelAllWork()
    }

    /**
     * Waktu (millis) pengingat kuliah berikutnya yang lebih besar dari [after],
     * yaitu [CLASS_LEAD_MINUTES] menit sebelum jam mulai pada hari yang sesuai.
     * dayOfWeek: 1 = Senin ... 7 = Minggu. Mengembalikan null jika format jam salah.
     */
    fun nextClassReminderTime(dayOfWeek: Int, startTime: String, after: Long): Long? {
        val parts = startTime.split(":")
        val hour = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: return null
        val targetDay = (dayOfWeek % 7) + 1 // Senin(1) -> Calendar.MONDAY(2), Minggu(7) -> SUNDAY(1)

        val day = Calendar.getInstance()
        repeat(8) {
            if (day.get(Calendar.DAY_OF_WEEK) == targetDay) {
                val start = day.clone() as Calendar
                start.set(Calendar.HOUR_OF_DAY, hour)
                start.set(Calendar.MINUTE, minute)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)
                val trigger = start.timeInMillis - CLASS_LEAD_MINUTES * MINUTE_MS
                if (trigger > after) return trigger
            }
            day.add(Calendar.DAY_OF_MONTH, 1)
        }
        return null
    }
}
