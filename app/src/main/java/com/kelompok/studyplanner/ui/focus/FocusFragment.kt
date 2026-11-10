package com.kelompok.studyplanner.ui.focus

import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.ListenerRegistration
import com.kelompok.studyplanner.R
import com.kelompok.studyplanner.data.FocusRepository
import com.kelompok.studyplanner.data.TaskRepository
import com.kelompok.studyplanner.databinding.FragmentFocusBinding
import com.kelompok.studyplanner.model.Task
import com.kelompok.studyplanner.util.NotificationHelper
import java.util.Locale

class FocusFragment : Fragment() {

    private enum class Mode(val minutes: Int, val phase: String) {
        FOCUS(25, "Waktunya fokus"),
        BREAK(5, "Istirahat singkat"),
        LONG_BREAK(15, "Istirahat panjang")
    }

    private var _binding: FragmentFocusBinding? = null
    private val binding get() = _binding!!

    private var mode = Mode.FOCUS
    private var timeLeftMs = Mode.FOCUS.minutes * 60_000L
    private var isRunning = false
    private var sessionCount = 0
    private var timer: CountDownTimer? = null

    private var tasks: List<Task> = emptyList()
    private var selectedTaskId: String? = null
    private var taskListener: ListenerRegistration? = null
    private var focusListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFocusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.spTask.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                selectedTaskId = tasks.getOrNull(position - 1)?.id
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        bindTaskSpinner()

        binding.chipGroupMode.setOnCheckedStateChangeListener { _, ids ->
            val newMode = when (ids.firstOrNull()) {
                R.id.chipBreak -> Mode.BREAK
                R.id.chipLong -> Mode.LONG_BREAK
                else -> Mode.FOCUS
            }
            if (newMode != mode) switchMode(newMode)
        }

        binding.btnStartPause.setOnClickListener { if (isRunning) pauseTimer() else startTimer() }
        binding.btnReset.setOnClickListener { resetTimer() }
        binding.btnSkip.setOnClickListener { advancePhase() }

        taskListener = TaskRepository.listen { list ->
            tasks = list.filter { !it.isDone }.sortedBy { it.deadline.toDate().time }
            bindTaskSpinner()
        }
        focusListener = FocusRepository.listenToday { minutes, sessions ->
            _binding?.tvTotalToday?.text = "Hari ini: $minutes menit ($sessions sesi)"
        }

        updateUi()
    }

    private fun bindTaskSpinner() {
        val b = _binding ?: return
        val keep = selectedTaskId
        val names = listOf("Tanpa tugas tertentu") + tasks.map { it.title }
        b.spTask.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item, names
        )
        b.spTask.setSelection(tasks.indexOfFirst { it.id == keep } + 1)
    }

    private fun startTimer() {
        if (timeLeftMs <= 0) timeLeftMs = mode.minutes * 60_000L
        isRunning = true
        timer?.cancel()
        timer = object : CountDownTimer(timeLeftMs, 250) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftMs = millisUntilFinished
                updateUi()
            }

            override fun onFinish() {
                timeLeftMs = 0
                onPhaseFinished()
            }
        }.start()
        updateUi()
    }

    private fun pauseTimer() {
        timer?.cancel()
        isRunning = false
        updateUi()
    }

    private fun resetTimer() {
        timer?.cancel()
        isRunning = false
        timeLeftMs = mode.minutes * 60_000L
        updateUi()
    }

    private fun switchMode(newMode: Mode) {
        timer?.cancel()
        isRunning = false
        mode = newMode
        timeLeftMs = newMode.minutes * 60_000L
        updateUi()
    }

    private fun onPhaseFinished() {
        val finished = mode
        isRunning = false

        if (finished == Mode.FOCUS) {
            FocusRepository.add(selectedTaskId, Mode.FOCUS.minutes)
            sessionCount++
        }

        context?.let { ctx ->
            val title = if (finished == Mode.FOCUS) "Sesi fokus selesai" else "Istirahat selesai"
            val text = if (finished == Mode.FOCUS) {
                "Saatnya istirahat sebentar."
            } else {
                "Ayo mulai sesi fokus berikutnya."
            }
            NotificationHelper.show(ctx.applicationContext, 1001, title, text)
        }
        advancePhase()
    }

    /** Pindah ke fase berikutnya (fokus -> istirahat -> fokus ...). Timer tidak otomatis jalan. */
    private fun advancePhase() {
        timer?.cancel()
        isRunning = false
        mode = when (mode) {
            Mode.FOCUS -> if (sessionCount > 0 && sessionCount % 4 == 0) Mode.LONG_BREAK else Mode.BREAK
            else -> Mode.FOCUS
        }
        timeLeftMs = mode.minutes * 60_000L
        _binding?.chipGroupMode?.check(
            when (mode) {
                Mode.FOCUS -> R.id.chipFocus
                Mode.BREAK -> R.id.chipBreak
                Mode.LONG_BREAK -> R.id.chipLong
            }
        )
        updateUi()
    }

    private fun updateUi() {
        val b = _binding ?: return
        val total = mode.minutes * 60_000L
        val seconds = (timeLeftMs + 999) / 1000

        b.tvTime.text = String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
        b.tvPhase.text = mode.phase
        b.progressRing.setProgressCompat((timeLeftMs * 1000 / total).toInt(), false)
        b.btnStartPause.text = if (isRunning) "Jeda" else "Mulai"
        b.btnStartPause.setIconResource(if (isRunning) R.drawable.ic_pause else R.drawable.ic_play)
        b.root.keepScreenOn = isRunning // layar tetap menyala selama timer berjalan
        b.tvSession.text = "Sesi ke-${sessionCount % 4 + 1} dari 4"
    }

    override fun onDestroyView() {
        taskListener?.remove()
        focusListener?.remove()
        taskListener = null
        focusListener = null
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }
}
