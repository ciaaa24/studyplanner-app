package com.kelompok.studyplanner.ui.tasks

import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kelompok.studyplanner.R
import com.kelompok.studyplanner.databinding.ItemTaskBinding
import com.kelompok.studyplanner.model.Task
import com.kelompok.studyplanner.util.DateUtil

private object TaskDiff : DiffUtil.ItemCallback<Task>() {
    override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean = oldItem == newItem
}

class TaskAdapter(
    private val onToggle: (Task, Boolean) -> Unit,
    private val onClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.VH>(TaskDiff) {

    class VH(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val task = getItem(position)
        val b = holder.binding
        val ctx = b.root.context

        // Lepas listener dulu agar tidak terpicu saat isChecked diatur dari kode.
        b.cbDone.setOnCheckedChangeListener(null)
        b.cbDone.isChecked = task.isDone
        b.cbDone.setOnCheckedChangeListener { _, checked -> onToggle(task, checked) }

        b.tvTitle.text = task.title
        b.tvTitle.alpha = if (task.isDone) 0.6f else 1f
        b.tvTitle.paintFlags = if (task.isDone) {
            b.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            b.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        val course = task.courseName
        if (course.isNullOrBlank()) {
            b.tvCourse.visibility = View.GONE
        } else {
            b.tvCourse.visibility = View.VISIBLE
            b.tvCourse.text = course
        }

        val (bgColor, fgColor, label) = when (task.priority) {
            "tinggi" -> Triple(R.color.sp_danger_bg, R.color.sp_danger, "Tinggi")
            "rendah" -> Triple(R.color.sp_ok_bg, R.color.sp_ok, "Rendah")
            else -> Triple(R.color.sp_warn_bg, R.color.sp_warn, "Sedang")
        }
        b.tvPriority.text = label
        b.tvPriority.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, bgColor))
        b.tvPriority.setTextColor(ContextCompat.getColor(ctx, fgColor))

        val overdue = !task.isDone && task.deadline.toDate().time < System.currentTimeMillis()
        b.tvDeadline.text = if (task.isDone) "Selesai" else DateUtil.relativeDeadline(task.deadline)
        b.tvDeadline.setTextColor(
            ContextCompat.getColor(ctx, if (overdue) R.color.sp_danger else R.color.sp_text_secondary)
        )

        b.root.setOnClickListener { onClick(task) }
    }
}
