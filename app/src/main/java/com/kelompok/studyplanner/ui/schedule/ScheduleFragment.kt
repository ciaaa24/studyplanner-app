package com.kelompok.studyplanner.ui.schedule

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.firestore.ListenerRegistration
import com.kelompok.studyplanner.R
import com.kelompok.studyplanner.data.CourseRepository
import com.kelompok.studyplanner.data.ScheduleRepository
import com.kelompok.studyplanner.databinding.DialogScheduleBinding
import com.kelompok.studyplanner.databinding.FragmentScheduleBinding
import com.kelompok.studyplanner.model.Course
import com.kelompok.studyplanner.model.Schedule
import com.kelompok.studyplanner.util.DateUtil

class ScheduleFragment : Fragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!

    private var scheduleListener: ListenerRegistration? = null
    private var courseListener: ListenerRegistration? = null

    private var all: List<Schedule> = emptyList()
    private var courses: List<Course> = emptyList()
    private var selectedDay = DateUtil.todayDayOfWeek()

    private val dayChipIds = listOf(
        R.id.chipDay1, R.id.chipDay2, R.id.chipDay3, R.id.chipDay4,
        R.id.chipDay5, R.id.chipDay6, R.id.chipDay7
    )

    private val adapter = ScheduleAdapter(showDelete = true, onDelete = { confirmDelete(it) })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvSchedule.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSchedule.adapter = adapter

        binding.chipGroupDays.check(dayChipIds[selectedDay - 1])
        binding.chipGroupDays.setOnCheckedStateChangeListener { _, checkedIds ->
            val checked = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val index = dayChipIds.indexOf(checked)
            if (index >= 0) {
                selectedDay = index + 1
                applyFilter()
            }
        }

        binding.btnAddSchedule.setOnClickListener { showAddDialog() }

        scheduleListener = ScheduleRepository.listen { list ->
            all = list
            applyFilter()
        }
        courseListener = CourseRepository.listen { list -> courses = list }

        applyFilter()
    }

    private fun applyFilter() {
        val b = _binding ?: return
        val shown = all.filter { it.dayOfWeek == selectedDay }
        adapter.submitList(shown)
        b.tvEmpty.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE
        b.tvDayTitle.text = DateUtil.dayNames[selectedDay - 1]
    }

    private fun confirmDelete(schedule: Schedule) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus jadwal?")
            .setMessage("${schedule.courseName} (${schedule.startTime} - ${schedule.endTime})")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ -> ScheduleRepository.delete(schedule.id) }
            .show()
    }

    private fun pickTime(hour: Int, minute: Int, onPicked: (Int, Int) -> Unit) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(hour)
            .setMinute(minute)
            .build()
        picker.addOnPositiveButtonClickListener { onPicked(picker.hour, picker.minute) }
        picker.show(childFragmentManager, "time")
    }

    private fun showAddDialog() {
        val ctx = requireContext()
        val d = DialogScheduleBinding.inflate(LayoutInflater.from(ctx))

        var startH = 8
        var startM = 0
        var endH = 9
        var endM = 40

        d.actCourse.setAdapter(
            ArrayAdapter(ctx, android.R.layout.simple_list_item_1, courses.map { it.name })
        )
        d.spDay.adapter = ArrayAdapter(
            ctx, android.R.layout.simple_spinner_dropdown_item, DateUtil.dayNames
        )
        d.spDay.setSelection(selectedDay - 1)

        d.btnStart.text = "Mulai: " + DateUtil.hm(startH, startM)
        d.btnEnd.text = "Selesai: " + DateUtil.hm(endH, endM)
        d.btnStart.setOnClickListener {
            pickTime(startH, startM) { h, m ->
                startH = h
                startM = m
                d.btnStart.text = "Mulai: " + DateUtil.hm(h, m)
            }
        }
        d.btnEnd.setOnClickListener {
            pickTime(endH, endM) { h, m ->
                endH = h
                endM = m
                d.btnEnd.text = "Selesai: " + DateUtil.hm(h, m)
            }
        }

        val dialog = MaterialAlertDialogBuilder(ctx)
            .setTitle("Tambah jadwal kuliah")
            .setView(d.root)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Simpan", null)
            .create()
        dialog.show()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val name = d.actCourse.text.toString().trim()
            if (name.isEmpty()) {
                d.tilCourse.error = "Mata kuliah wajib diisi"
                return@setOnClickListener
            }
            if (startH * 60 + startM >= endH * 60 + endM) {
                Toast.makeText(ctx, "Jam selesai harus setelah jam mulai", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val day = d.spDay.selectedItemPosition + 1
            val room = d.etRoom.text.toString().trim()
            val start = DateUtil.hm(startH, startM)
            val end = DateUtil.hm(endH, endM)

            fun saveWith(courseId: String?, courseName: String) {
                ScheduleRepository.add(
                    Schedule(
                        courseId = courseId,
                        courseName = courseName,
                        dayOfWeek = day,
                        startTime = start,
                        endTime = end,
                        room = room
                    )
                )
            }

            // Nama yang belum ada di daftar mata kuliah otomatis dibuat sebagai mata kuliah baru.
            val existing = courses.firstOrNull { it.name.equals(name, ignoreCase = true) }
            if (existing != null) {
                saveWith(existing.id, existing.name)
            } else {
                CourseRepository.add(name, "") { newId -> saveWith(newId, name) }
            }
            dialog.dismiss()
        }
    }

    override fun onDestroyView() {
        scheduleListener?.remove()
        courseListener?.remove()
        scheduleListener = null
        courseListener = null
        _binding = null
        super.onDestroyView()
    }
}
