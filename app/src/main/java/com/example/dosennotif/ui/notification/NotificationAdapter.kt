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

            return when {
                // Today
                now.get(Calendar.DATE) == notificationTime.get(Calendar.DATE) &&
                        now.get(Calendar.MONTH) == notificationTime.get(Calendar.MONTH) &&
                        now.get(Calendar.YEAR) == notificationTime.get(Calendar.YEAR) -> {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    "Today, ${timeFormat.format(Date(timestamp))}"
                }

                // Yesterday
                now.get(Calendar.DATE) - notificationTime.get(Calendar.DATE) == 1 &&
                        now.get(Calendar.MONTH) == notificationTime.get(Calendar.MONTH) &&
                        now.get(Calendar.YEAR) == notificationTime.get(Calendar.YEAR) -> {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    "Yesterday, ${timeFormat.format(Date(timestamp))}"
                }

                // This week
                now.get(Calendar.WEEK_OF_YEAR) == notificationTime.get(Calendar.WEEK_OF_YEAR) &&
                        now.get(Calendar.YEAR) == notificationTime.get(Calendar.YEAR) -> {
                    val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    "${dayFormat.format(Date(timestamp))}, ${timeFormat.format(Date(timestamp))}"
                }

                // Other
                else -> {
                    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    dateFormat.format(Date(timestamp))
                }
            }
        }
    }
}