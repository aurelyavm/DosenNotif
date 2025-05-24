package com.example.dosennotif.ui.notification

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.dosennotif.R
import com.example.dosennotif.databinding.ItemNotificationBinding
import com.example.dosennotif.model.ScheduleNotification
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private var notifications: List<ScheduleNotification>,
    private val onItemClick: (ScheduleNotification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }

    override fun getItemCount(): Int = notifications.size

    fun updateNotifications(newNotifications: List<ScheduleNotification>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: ScheduleNotification) {
            binding.apply {
                tvNotificationTitle.text = notification.title
                tvNotificationMessage.text = notification.message
                tvNotificationTime.text = formatNotificationTime(notification.createdAt)

                // Set read indicator
                if (notification.isRead) {
                    viewReadIndicator.visibility = View.INVISIBLE
                } else {
                    viewReadIndicator.visibility = View.VISIBLE
                    viewReadIndicator.background.setTint(
                        ContextCompat.getColor(itemView.context, R.color.colorPrimary)
                    )
                }

                // Set click listener
                root.setOnClickListener {
                    onItemClick(notification)
                }
            }
        }

        private fun formatNotificationTime(timestamp: Long): String {
            val now = Calendar.getInstance()
            val notificationTime = Calendar.getInstance().apply {
                timeInMillis = timestamp
            }

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeString = timeFormat.format(Date(timestamp))

            return when {
                // Today - same date
                isSameDay(now, notificationTime) -> {
                    "Today, $timeString"
                }

                // Yesterday
                isYesterday(now, notificationTime) -> {
                    "Yesterday, $timeString"
                }

                // This week (within 7 days)
                isThisWeek(now, notificationTime) -> {
                    val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                    val dayName = dayFormat.format(Date(timestamp))
                    "$dayName, $timeString"
                }

                // This year
                isSameYear(now, notificationTime) -> {
                    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                    val dateString = dateFormat.format(Date(timestamp))
                    "$dateString, $timeString"
                }

                // Different year
                else -> {
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    val dateString = dateFormat.format(Date(timestamp))
                    "$dateString, $timeString"
                }
            }
        }

        private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }

        private fun isYesterday(currentTime: Calendar, notificationTime: Calendar): Boolean {
            val yesterday = Calendar.getInstance().apply {
                timeInMillis = currentTime.timeInMillis
                add(Calendar.DAY_OF_YEAR, -1)
            }

            return isSameDay(yesterday, notificationTime)
        }

        private fun isThisWeek(currentTime: Calendar, notificationTime: Calendar): Boolean {
            val diffInDays = (currentTime.timeInMillis - notificationTime.timeInMillis) / (1000 * 60 * 60 * 24)
            return diffInDays in 2..6 // 2-6 days ago (yesterday already handled)
        }

        private fun isSameYear(cal1: Calendar, cal2: Calendar): Boolean {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
        }
    }
}