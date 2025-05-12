package com.example.dosennotif.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dosennotif.databinding.ItemScheduleItemBinding
import com.example.dosennotif.model.Schedule

class ScheduleItemAdapter(
    private val schedules: List<Schedule>
) : RecyclerView.Adapter<ScheduleItemAdapter.ScheduleItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleItemViewHolder {
        val binding = ItemScheduleItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScheduleItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleItemViewHolder, position: Int) {
        holder.bind(schedules[position])
    }

    override fun getItemCount(): Int = schedules.size

    inner class ScheduleItemViewHolder(private val binding: ItemScheduleItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(schedule: Schedule) {
            binding.apply {
                tvScheduleTime.text = schedule.getTimeRange()
                tvCourseName.text = schedule.nama_mata_kuliah

                // Format details
                val details = "Kelas ${schedule.kelas} • ${schedule.ruang} • ${schedule.nama_program_studi}"
                tvCourseDetails.text = details
            }
        }
    }
}