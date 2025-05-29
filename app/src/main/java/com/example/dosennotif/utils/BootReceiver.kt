package com.example.dosennotif.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.dosennotif.service.RealtimeScheduleService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent?.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        Log.d("BootReceiver", "Device boot completed, starting RealtimeScheduleService")

        val serviceIntent = Intent(context, RealtimeScheduleService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
