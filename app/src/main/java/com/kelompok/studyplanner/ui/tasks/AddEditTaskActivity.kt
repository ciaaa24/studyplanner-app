package com.kelompok.studyplanner.ui.tasks

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.kelompok.studyplanner.R
import com.kelompok.studyplanner.data.CourseRepository
import com.kelompok.studyplanner.data.TaskRepository
import com.kelompok.studyplanner.databinding.ActivityAddEditTaskBinding
import com.kelompok.studyplanner.model.Course
import com.kelompok.studyplanner.model.Task
import com.kelompok.studyplanner.ui.common.CourseDialog
import com.kelompok.studyplanner.util.DateUtil
import com.kelompok.studyplanner.util.ReminderScheduler
import com.kelompok.studyplanner.util.applySystemBarsPadding
import java.util.Calendar
import java.util.TimeZone

class AddEditTaskActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TASK_ID = "extra_task_id"
    }

    private lateinit var binding: ActivityAddEditTaskBinding

    private var taskId: String? = null
    private var editingTask: Task? = null

    private var courses: List<Course> = emptyList()
    private var selectedCourseId: String? = null
    private var courseListener: ListenerRegistration? = null

    // Deadline bawaan: besok pukul 23:59
    private val deadline: Calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.spCourse.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                // Daftar belum dimuat: jangan timpa pilihan yang sudah ada (mode ubah).
                if (courses.isEmpty()) return
                selectedCourseId = courses.getOrNull(position - 1)?.id
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        bindCourseSpinner()
        courseListener = CourseRepository.listen { list ->
            courses = list
            bindCourseSpinner()
        }

        updateDeadlineButtons()
        binding.btnDate.setOnClickListener { pickDate() }
        binding.btnTime.setOnClickListener { pickTime() }
        binding.btnAddCourse.setOnClickListener {
            CourseDialog.show(this) { newId -> selectedCourseId = newId }
        }
        binding.btnSave.setOnClickListener { save() }
        binding.btnDelete.setOnClickListener { confirmDelete() }

        taskId = intent.getStringExtra(EXTRA_TASK_ID)
        val id = taskId
        if (id != null) {
            binding.toolbar.title = "Ubah tugas"
            binding.btnSave.text = "Simpan perubahan"
            binding.btnDelete.visibility = View.VISIBLE
            TaskRepository.get(id) { task ->
                if (task == null) {
                    Toast.makeText(this, "Tugas tidak ditemukan", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    fillForm(task)
                }
            }
        }
    }

    private fun fillForm(task: Task) {
        editingTask = task
        binding.etTitle.setText(task.title)
        binding.etDescription.setText(task.description)
        deadline.time = task.deadline.toDate()
        updateDeadlineButtons()

        binding.chipGroupPriority.check(
            when (task.priority) {
                "rendah" -> R.id.chipLow
                "tinggi" -> R.id.chipHigh
                else -> R.id.chipMedium
            }
        )

        selectedCourseId = task.courseId
        bindCourseSpinner()
    }

    private fun bindCourseSpinner() {
        val keep = selectedCourseId
        val names = listOf("Tanpa mata kuliah") + courses.map { it.name }
        binding.spCourse.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        binding.spCourse.setSelection(courses.indexOfFirst { it.id == keep } + 1)
    }

    private fun updateDeadlineButtons() {
        binding.btnDate.text = DateUtil.shortDate(deadline.timeInMillis)
        binding.btnTime.text = DateUtil.hm(
            deadline.get(Calendar.HOUR_OF_DAY),
            deadline.get(Calendar.MINUTE)
        )
    }

    private fun pickDate() {
        // MaterialDatePicker memakai tengah malam UTC, jadi perlu dikonversi dari/ke zona lokal.
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                deadline.get(Calendar.YEAR),
                deadline.get(Calendar.MONTH),
                deadline.get(Calendar.DAY_OF_MONTH)
            )
        }
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Tanggal deadline")
            .setSelection(utc.timeInMillis)
            .build()
        picker.addOnPositiveButtonClickListener { selection ->
            val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = selection
            }
            deadline.set(
                picked.get(Calendar.YEAR),
                picked.get(Calendar.MONTH),
                picked.get(Calendar.DAY_OF_MONTH)
            )
            updateDeadlineButtons()
        }
        picker.show(supportFragmentManager, "date")
    }

    private fun pickTime() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(deadline.get(Calendar.HOUR_OF_DAY))
            .setMinute(deadline.get(Calendar.MINUTE))
            .setTitleText("Jam deadline")
            .build()
        picker.addOnPositiveButtonClickListener {
            deadline.set(Calendar.HOUR_OF_DAY, picker.hour)
            deadline.set(Calendar.MINUTE, picker.minute)
            updateDeadlineButtons()
        }
        picker.show(supportFragmentManager, "time")
    }

    private fun selectedPriority(): String = when (binding.chipGroupPriority.checkedChipId) {
        R.id.chipLow -> "rendah"
        R.id.chipHigh -> "tinggi"
        else -> "sedang"
    }

    private fun save() {
        binding.tilTitle.error = null
        val title = binding.etTitle.text.toString().trim()
        if (title.isEmpty()) {
            binding.tilTitle.error = "Judul wajib diisi"
            return
        }
        if (taskId != null && editingTask == null) {
            Toast.makeText(this, "Data masih dimuat, coba lagi sebentar", Toast.LENGTH_SHORT).show()
            return
        }

        val old = editingTask
        val course = courses.firstOrNull { it.id == selectedCourseId }

        val task = Task(
            id = taskId ?: "",
            title = title,
            description = binding.etDescription.text.toString().trim(),
            courseId = course?.id,
            courseName = course?.name,
            deadline = Timestamp(deadline.time),
            priority = selectedPriority(),
            isDone = old?.isDone ?: false,
            createdAt = old?.createdAt ?: Timestamp.now()
        )

        TaskRepository.save(task) { savedId ->
            // Tiga pengingat (1 hari, 3 jam, 1 jam sebelum deadline) selalu dipasang untuk tugas yang belum selesai.
            if (!task.isDone) {
                ReminderScheduler.scheduleTask(applicationContext, savedId, task.title, task.deadline)
            } else {
                ReminderScheduler.cancelTask(applicationContext, savedId)
            }
            finish()
        }
    }

    private fun confirmDelete() {
        val id = taskId ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle("Hapus tugas?")
            .setMessage("Tugas ini akan dihapus permanen.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                TaskRepository.delete(id)
                ReminderScheduler.cancelTask(applicationContext, id)
                finish()
            }
            .show()
    }

    override fun onDestroy() {
        courseListener?.remove()
        super.onDestroy()
    }
}
