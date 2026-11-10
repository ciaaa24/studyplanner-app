package com.kelompok.studyplanner.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.ListenerRegistration
import com.kelompok.studyplanner.R
import com.kelompok.studyplanner.data.AuthRepository
import com.kelompok.studyplanner.data.ScheduleRepository
import com.kelompok.studyplanner.data.TaskRepository
import com.kelompok.studyplanner.databinding.ActivityMainBinding
import com.kelompok.studyplanner.ui.auth.LoginActivity
import com.kelompok.studyplanner.ui.focus.FocusFragment
import com.kelompok.studyplanner.ui.home.HomeFragment
import com.kelompok.studyplanner.ui.schedule.ScheduleFragment
import com.kelompok.studyplanner.ui.tasks.TaskListFragment
import com.kelompok.studyplanner.util.NotificationHelper
import com.kelompok.studyplanner.util.ReminderScheduler
import com.kelompok.studyplanner.util.applySystemBarsPadding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val tabs = listOf("home", "tasks", "schedule", "focus")

    private var taskSync: ListenerRegistration? = null
    private var scheduleSync: ListenerRegistration? = null

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AuthRepository.isLoggedIn) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarsPadding()

        NotificationHelper.createChannel(this)
        askNotificationPermission()

        // Selama aplikasi dibuka, pengingat disamakan dengan data Firestore:
        // 3 pengingat deadline per tugas + pengingat tiap jadwal kuliah.
        val app = applicationContext
        taskSync = TaskRepository.listen { ReminderScheduler.syncTasks(app, it) }
        scheduleSync = ScheduleRepository.listen { ReminderScheduler.syncClasses(app, it) }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> showTab("home")
                R.id.nav_tasks -> showTab("tasks")
                R.id.nav_schedule -> showTab("schedule")
                R.id.nav_focus -> showTab("focus")
            }
            true
        }

        if (savedInstanceState == null) {
            showTab("home")
        }
    }

    /**
     * Semua tab dibuat sekali lalu hanya di-show/hide, jadi timer fokus
     * tetap berjalan saat pengguna pindah ke tab lain.
     */
    private fun showTab(tag: String) {
        val fm = supportFragmentManager
        val tx = fm.beginTransaction().setReorderingAllowed(true)
        for (t in tabs) {
            val existing = fm.findFragmentByTag(t)
            if (t == tag) {
                if (existing == null) {
                    tx.add(R.id.container, createFragment(t), t)
                } else {
                    tx.show(existing)
                }
            } else if (existing != null) {
                tx.hide(existing)
            }
        }
        tx.commit()
    }

    private fun createFragment(tag: String): Fragment = when (tag) {
        "tasks" -> TaskListFragment()
        "schedule" -> ScheduleFragment()
        "focus" -> FocusFragment()
        else -> HomeFragment()
    }

    override fun onDestroy() {
        taskSync?.remove()
        scheduleSync?.remove()
        super.onDestroy()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
