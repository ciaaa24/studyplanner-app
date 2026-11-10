package com.kelompok.studyplanner.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtil {
    private val localeId: Locale = Locale.forLanguageTag("id-ID")
    private const val DAY_MS = 24L * 60 * 60 * 1000

    val dayNames = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")

    fun startOfDay(millis: Long): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun startOfToday(): Long = startOfDay(System.currentTimeMillis())

    fun endOfToday(): Long = startOfToday() + DAY_MS

    /** 1 = Senin ... 7 = Minggu */
    fun todayDayOfWeek(): Int {
        val c = Calendar.getInstance()
        return ((c.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
    }

    fun hm(hour: Int, minute: Int): String = String.format(localeId, "%02d:%02d", hour, minute)

    fun fullDate(millis: Long): String =
        SimpleDateFormat("EEEE, d MMMM yyyy", localeId).format(Date(millis))

    fun shortDate(millis: Long): String =
        SimpleDateFormat("EEE, d MMM yyyy", localeId).format(Date(millis))

    /** Contoh: "Hari ini, 23:59", "Besok, 08:00", "Sabtu, 17 Okt, 16:00". */
    fun relativeDeadline(ts: Timestamp): String {
        val d = ts.toDate()
        val time = SimpleDateFormat("HH:mm", localeId).format(d)
        val diff = ((startOfDay(d.time) - startOfToday()) / DAY_MS).toInt()
        return when (diff) {
            0 -> "Hari ini, $time"
            1 -> "Besok, $time"
            -1 -> "Kemarin, $time"
            else -> SimpleDateFormat("EEEE, d MMM, HH:mm", localeId).format(d)
        }
    }
}
