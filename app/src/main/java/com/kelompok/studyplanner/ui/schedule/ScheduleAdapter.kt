package com.kelompok.studyplanner.ui.schedule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kelompok.studyplanner.databinding.ItemScheduleBinding
import com.kelompok.studyplanner.model.Schedule

private object ScheduleDiff : DiffUtil.ItemCallback<Schedule>() {
    override fun areItemsTheSame(oldItem: Schedule, newItem: Schedule): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Schedule, newItem: Schedule): Boolean =
        oldItem == newItem
}

class ScheduleAdapter(
    private val showDelete: Boolean,
    private val onDelete: (Schedule) -> Unit
) : ListAdapter<Schedule, ScheduleAdapter.VH>(ScheduleDiff) {

    class VH(val binding: ItemScheduleBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemScheduleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        val b = holder.binding

        b.tvStart.text = item.startTime
        b.tvEnd.text = item.endTime
        b.tvCourse.text = item.courseName
        if (item.room.isBlank()) {
            b.tvRoom.visibility = View.GONE
        } else {
            b.tvRoom.visibility = View.VISIBLE
            b.tvRoom.text = item.room
        }

        b.btnDelete.visibility = if (showDelete) View.VISIBLE else View.GONE
        b.btnDelete.setOnClickListener { onDelete(item) }
    }
}
