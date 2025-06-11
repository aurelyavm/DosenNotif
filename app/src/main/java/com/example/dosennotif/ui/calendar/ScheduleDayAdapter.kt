package com.example.dosennotif.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dosennotif.databinding.ItemScheduleDayBinding
import com.example.dosennotif.model.Schedule

class ScheduleDayAdapter(
    private var schedulesByDay: Map<String, List<Schedule>> = emptyMap()
) : RecyclerView.Adapter<ScheduleDayAdapter.DayViewHolder>() {

    private val sortedDays: List<String>
        get() = schedulesByDay.keys.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val binding = ItemScheduleDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DayViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = sortedDays[position]
        val schedulesForDay = schedulesByDay[day] ?: emptyList()
        holder.bind(day, schedulesForDay)
    }

    override fun getItemCount(): Int = schedulesByDay.size

    fun updateSchedules(newSchedulesByDay: Map<String, List<Schedule>>?) {
        schedulesByDay = newSchedulesByDay ?: emptyMap()
        notifyDataSetChanged()
    }

    inner class DayViewHolder(private val binding: ItemScheduleDayBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(day: String, schedules: List<Schedule>) {
            binding.tvDayHeader.text = day

            val scheduleItemAdapter = ScheduleItemAdapter(schedules)
            binding.rvDaySchedules.apply {
                layoutManager = LinearLayoutManager(binding.root.context)
                adapter = scheduleItemAdapter
                // Disable nested scrolling for smooth parent scrolling
                isNestedScrollingEnabled = false
            }
        }
    }
}