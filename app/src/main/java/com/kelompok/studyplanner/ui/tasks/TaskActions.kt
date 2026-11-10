package com.kelompok.studyplanner.ui.tasks

import android.content.Context
import android.content.Intent
import com.kelompok.studyplanner.data.TaskRepository
import com.kelompok.studyplanner.model.Task
import com.kelompok.studyplanner.util.ReminderScheduler

/** Aksi bersama yang dipakai Beranda dan daftar Tugas. */
object TaskActions {

    fun setDone(context: Context, task: Task, done: Boolean) {
        TaskRepository.setDone(task.id, done)
        val app = context.applicationContext
        if (done) {
            ReminderScheduler.cancelTask(app, task.id)
        } else {
            ReminderScheduler.scheduleTask(app, task.id, task.title, task.deadline)
        }
    }

    /** taskId null = tambah tugas baru, terisi = ubah tugas. */
    fun openEditor(context: Context, taskId: String?) {
        val intent = Intent(context, AddEditTaskActivity::class.java)
        if (taskId != null) {
            intent.putExtra(AddEditTaskActivity.EXTRA_TASK_ID, taskId)
        }
        context.startActivity(intent)
    }
}
