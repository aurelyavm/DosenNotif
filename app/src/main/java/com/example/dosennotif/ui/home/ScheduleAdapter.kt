package com.example.dosennotif.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.dosennotif.databinding.ItemScheduleBinding
import com.example.dosennotif.model.Schedule

class ScheduleAdapter(
    private var schedules: List<Schedule>
) : RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemScheduleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScheduleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(schedules[position])
    }

    override fun getItemCount(): Int = schedules.size

    fun updateSchedules(newSchedules: List<Schedule>) {
        schedules = newSchedules
        notifyDataSetChanged()
    }

    inner class ScheduleViewHolder(private val binding: ItemScheduleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(schedule: Schedule) {
            binding.apply {
                tvScheduleTime.text = schedule.getTimeRange()
                tvCourseName.text = schedule.nama_mata_kuliah
                tvRoom.text = schedule.ruang
                tvClass.text = "Kelas ${schedule.kelas}"
                tvDepartment.text = schedule.nama_program_studi
            }
        }
    }
}