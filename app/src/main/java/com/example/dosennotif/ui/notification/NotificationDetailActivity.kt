package com.example.dosennotif.ui.notification

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.dosennotif.databinding.ActivityNotificationDetailBinding
import com.example.dosennotif.viewmodel.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationDetailBinding
    private lateinit var viewModel: NotificationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[NotificationViewModel::class.java]

        // Get notification ID from intent
        val notificationId = intent.getStringExtra("notification_id")
        if (notificationId != null) {
            // Load notification details
            viewModel.getNotificationById(notificationId)
        } else {
            finish()
            return
        }

        // Set up back button
        binding.ivBack.setOnClickListener {
            finish()
        }

        // Set up campus map button
        binding.btnCampusMap.setOnClickListener {
            openCampusMap()
        }

        // Observe selected notification
        observeSelectedNotification()
    }

    private fun observeSelectedNotification() {
        viewModel.selectedNotification.observe(this) { notification ->
            if (notification == null) {
                binding.progressBar.visibility = View.VISIBLE
                binding.cardNotificationDetail.visibility = View.GONE
            } else {
                binding.progressBar.visibility = View.GONE
                binding.cardNotificationDetail.visibility = View.VISIBLE

                // Set notification details
                binding.tvNotificationTitle.text = notification.title
                binding.tvNotificationMessage.text = notification.message

                // Format schedule time
                val scheduleDate = Date(notification.actualScheduleTime)
                val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val dayOfWeek = dayFormat.format(scheduleDate)
                val startTime = timeFormat.format(scheduleDate)

                // Calculate end time (assuming 100 minutes duration)
                val endTimeDate = Date(notification.actualScheduleTime + (100 * 60 * 1000))
                val endTime = timeFormat.format(endTimeDate)

                binding.tvScheduleTime.text = "$dayOfWeek, $startTime - $endTime"
                binding.tvRoom.text = notification.room
                binding.tvClass.text = "Kelas ${notification.className}"
            }
        }
    }

    private fun openCampusMap() {
        // Open Google Maps with UPN Veteran Jakarta location :
        val gmmIntentUri = Uri.parse("geo:-6.315588522917615,106.83980697680275?q=UPN+Veteran+Jakarta")
        // jarak 10+ km:
        //val gmmIntentUri = Uri.parse("geo:-8.706162356585414,115.17742866094957?q=UPN+Veteran+Jakarta")
        // jarak 20+ km -- done ss notif
        //val gmmIntentUri = Uri.parse("geo:-8.606946753381965, 115.1950018243033?q=UPN+Veteran+Jakarta")
        //jarak 30+ km -- done ss notif
        //val gmmIntentUri = Uri.parse("geo:-8.527464337179357,115.19189769412824?q=UPN+Veteran+Jakarta")
        //jarak 40+ km
        //val gmmIntentUri = Uri.parse("geo:-8.448345800051587, 115.1672666046926?q=UPN+Veteran+Jakarta")
        //jarak 50+ km
        //val gmmIntentUri = Uri.parse("geo:-8.325071867130365, 115.18532365335058?q=UPN+Veteran+Jakarta")
        //jarak 30+ km (versi udah di rumah)
        //val gmmIntentUri = Uri.parse("geo:-6.221157021315558,106.5623033?q=UPN+Veteran+Jakarta")

        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)


        mapIntent.setPackage("com.google.android.apps.maps")

        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.clearSelectedNotification()
    }
}