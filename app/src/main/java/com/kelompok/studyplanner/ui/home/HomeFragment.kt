package com.kelompok.studyplanner.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.ListenerRegistration
import com.kelompok.studyplanner.R
import com.kelompok.studyplanner.data.AuthRepository
import com.kelompok.studyplanner.data.ScheduleRepository
import com.kelompok.studyplanner.data.TaskRepository
import com.kelompok.studyplanner.databinding.FragmentHomeBinding
import com.kelompok.studyplanner.model.Task
import com.kelompok.studyplanner.ui.auth.LoginActivity
import com.kelompok.studyplanner.ui.schedule.ScheduleAdapter
import com.kelompok.studyplanner.ui.tasks.TaskActions
import com.kelompok.studyplanner.ui.tasks.TaskAdapter
import com.kelompok.studyplanner.util.DateUtil
import com.kelompok.studyplanner.util.ReminderScheduler

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var taskListener: ListenerRegistration? = null
    private var scheduleListener: ListenerRegistration? = null

    private val upcomingAdapter = TaskAdapter(
        onToggle = { task, done -> TaskActions.setDone(requireContext(), task, done) },
        onClick = { task -> TaskActions.openEditor(requireContext(), task.id) }
    )
    private val scheduleAdapter = ScheduleAdapter(showDelete = false, onDelete = { })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvDate.text = DateUtil.fullDate(System.currentTimeMillis())

        binding.rvUpcoming.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUpcoming.adapter = upcomingAdapter
        binding.rvSchedule.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSchedule.adapter = scheduleAdapter

        AuthRepository.fetchName { name ->
            _binding?.tvGreeting?.text =
                if (name.isBlank()) "Halo, semangat belajar!" else "Halo, $name!"
        }

        binding.btnStartFocus.setOnClickListener {
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNav)
                .selectedItemId = R.id.nav_focus
        }
        binding.btnLogout.setOnClickListener { confirmLogout() }

        taskListener = TaskRepository.listen { tasks -> renderTasks(tasks) }
        scheduleListener = ScheduleRepository.listen { all ->
            val b = _binding ?: return@listen
            val today = all.filter { it.dayOfWeek == DateUtil.todayDayOfWeek() }
            scheduleAdapter.submitList(today)
            b.tvEmptySchedule.visibility = if (today.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun renderTasks(tasks: List<Task>) {
        val b = _binding ?: return
        val now = System.currentTimeMillis()
        val startToday = DateUtil.startOfToday()
        val endToday = DateUtil.endOfToday()

        val pending = tasks.filter { !it.isDone }.sortedBy { it.deadline.toDate().time }
        val todayCount = pending.count { it.deadline.toDate().time in startToday until endToday }
        val overdue = pending.count { it.deadline.toDate().time < now }

        b.tvTodayCount.text = "$todayCount tugas"
        b.tvOverdue.text = if (overdue > 0) "$overdue tugas terlambat" else "Tidak ada yang terlambat"

        val upcoming = pending.take(3)
        upcomingAdapter.submitList(upcoming)
        b.tvEmptyUpcoming.visibility = if (upcoming.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Keluar dari akun?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Keluar") { _, _ ->
                ReminderScheduler.cancelAll(requireContext()) // pengingat akun ini tidak boleh muncul di akun lain
                AuthRepository.logout()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                requireActivity().finish()
            }
            .show()
    }

    override fun onDestroyView() {
        taskListener?.remove()
        scheduleListener?.remove()
        taskListener = null
        scheduleListener = null
        _binding = null
        super.onDestroyView()
    }
}
