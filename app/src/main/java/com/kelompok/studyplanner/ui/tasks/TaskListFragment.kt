package com.kelompok.studyplanner.ui.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.ListenerRegistration
import com.kelompok.studyplanner.R
import com.kelompok.studyplanner.data.TaskRepository
import com.kelompok.studyplanner.databinding.FragmentTaskListBinding
import com.kelompok.studyplanner.model.Task
import com.kelompok.studyplanner.util.DateUtil

class TaskListFragment : Fragment() {

    private enum class Filter { ALL, TODAY, UPCOMING, OVERDUE, DONE }

    private var _binding: FragmentTaskListBinding? = null
    private val binding get() = _binding!!

    private var listener: ListenerRegistration? = null
    private var allTasks: List<Task> = emptyList()
    private var filter = Filter.ALL

    private val adapter = TaskAdapter(
        onToggle = { task, done -> TaskActions.setDone(requireContext(), task, done) },
        onClick = { task -> TaskActions.openEditor(requireContext(), task.id) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTasks.adapter = adapter

        binding.fabAdd.setOnClickListener { TaskActions.openEditor(requireContext(), null) }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            filter = when (checkedIds.firstOrNull()) {
                R.id.chipToday -> Filter.TODAY
                R.id.chipUpcoming -> Filter.UPCOMING
                R.id.chipOverdue -> Filter.OVERDUE
                R.id.chipDone -> Filter.DONE
                else -> Filter.ALL
            }
            applyFilter()
        }

        listener = TaskRepository.listen { list ->
            allTasks = list
            applyFilter()
        }
    }

    /** Filter dilakukan di aplikasi, jadi tidak perlu membuat index di Firestore. */
    private fun applyFilter() {
        val b = _binding ?: return
        val now = System.currentTimeMillis()
        val startToday = DateUtil.startOfToday()
        val endToday = DateUtil.endOfToday()

        val shown = when (filter) {
            Filter.ALL -> allTasks.sortedWith(
                compareBy<Task>({ it.isDone }, { it.deadline.toDate().time })
            )
            Filter.TODAY -> allTasks.filter {
                !it.isDone && it.deadline.toDate().time in startToday until endToday
            }
            Filter.UPCOMING -> allTasks.filter { !it.isDone && it.deadline.toDate().time >= endToday }
            Filter.OVERDUE -> allTasks.filter { !it.isDone && it.deadline.toDate().time < now }
            Filter.DONE -> allTasks.filter { it.isDone }
        }

        adapter.submitList(shown)
        b.tvEmpty.visibility = if (shown.isEmpty()) View.VISIBLE else View.GONE

        val active = allTasks.count { !it.isDone }
        val overdue = allTasks.count { !it.isDone && it.deadline.toDate().time < now }
        b.tvSubtitle.text = "$active tugas aktif, $overdue terlambat"
    }

    override fun onDestroyView() {
        listener?.remove()
        listener = null
        _binding = null
        super.onDestroyView()
    }
}
