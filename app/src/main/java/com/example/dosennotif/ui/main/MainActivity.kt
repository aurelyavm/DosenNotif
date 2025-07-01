package com.example.dosennotif.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.dosennotif.R
import com.example.dosennotif.databinding.ActivityMainBinding
import com.example.dosennotif.service.RealtimeScheduleService
import com.example.dosennotif.ui.auth.LoginActivity
import com.example.dosennotif.utils.NotificationUtils
import com.example.dosennotif.utils.AssetsJsonUploader
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    private val LOCATION_PERMISSION_REQUEST_CODE = 100
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Check if user is logged in
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Setup navigation
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        // Setup notifications
        NotificationUtils.createNotificationChannel(this)
        // ✅ TAMBAHKAN DI SINI - setelah create channel, sebelum request permissions
        NotificationUtils.clearExpiredAlarms()
        // Request required permissions
        requestRequiredPermissions()

        // Setup assets upload system
        setupAssetsUpload()
    }

    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startRealtimeScheduleService()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        var allGranted = true
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false
                break
            }
        }

        if (allGranted) {
            startRealtimeScheduleService()
        } else {
            Log.w("MainActivity", "Permission denied. RealtimeScheduleService not started.")
        }
    }

    private fun startRealtimeScheduleService() {
        Log.d("MainActivity", "Starting RealtimeScheduleService...")
        val serviceIntent = Intent(this, RealtimeScheduleService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    // ======================================
    // ASSETS UPLOAD SYSTEM
    // ======================================

    /**
     * Setup assets upload system untuk offline functionality
     * Menggunakan JSON files dari assets folder
     */
    private fun setupAssetsUpload() {
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "🔍 Initializing assets upload system...")

                val uploader = AssetsJsonUploader(this@MainActivity)

                // Check what files are available in assets
                val availableFiles = uploader.checkAssetsFiles()
                Log.d("MainActivity", "📁 Available JSON files: $availableFiles")

                // Show file info for each available file
                availableFiles.forEach { fileName ->
                    val info = uploader.getAssetFileInfo(fileName)
                    Log.d("MainActivity", info)
                }

                // Check if we have all required files for format A (4 separate files)
                val requiredFiles = listOf("prodi_3.json", "prodi_4.json", "prodi_6.json", "prodi_58.json")
                val hasAllFiles = requiredFiles.all { it in availableFiles }

                if (hasAllFiles) {
                    // Format A: 4 separate files
                    Log.d("MainActivity", "📤 All 4 files found, starting upload...")
                    showUploadStatus("📤 Uploading schedule data...", true)

                    val success = uploader.uploadAllFromAssets()

                    if (success) {
                        Toast.makeText(this@MainActivity, "✅ Schedule data uploaded!", Toast.LENGTH_SHORT).show()
                        Log.d("MainActivity", "🎉 Assets upload successful!")
                        showUploadStatus("✅ Offline mode ready", false)
                    } else {
                        Toast.makeText(this@MainActivity, "❌ Upload failed", Toast.LENGTH_SHORT).show()
                        Log.e("MainActivity", "❌ Assets upload failed")
                        showUploadStatus("❌ Upload failed, using fallback", false)
                    }

                } else if (availableFiles.contains("api_responses.json")) {
                    // Format B: Single combined file
                    Log.d("MainActivity", "📤 Combined file found, starting upload...")
                    showUploadStatus("📤 Uploading combined data...", true)

                    val success = uploader.uploadFromCombinedJson()

                    if (success) {
                        Toast.makeText(this@MainActivity, "✅ Combined data uploaded!", Toast.LENGTH_SHORT).show()
                        Log.d("MainActivity", "🎉 Combined upload successful!")
                        showUploadStatus("✅ Offline mode ready", false)
                    } else {
                        Toast.makeText(this@MainActivity, "❌ Combined upload failed", Toast.LENGTH_SHORT).show()
                        Log.e("MainActivity", "❌ Combined upload failed")
                        showUploadStatus("❌ Upload failed, using fallback", false)
                    }

                } else {
                    // No valid files found
                    val missingFiles = requiredFiles.filter { it !in availableFiles }
                    Log.w("MainActivity", "⚠️ Missing required files: $missingFiles")
                    Log.w("MainActivity", "⚠️ Available files: $availableFiles")

                    Toast.makeText(
                        this@MainActivity,
                        "⚠️ No schedule data files found. App will use fallback data.",
                        Toast.LENGTH_LONG
                    ).show()

                    showUploadStatus("⚠️ No data files, using fallback", false)
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Error in assets upload setup: ${e.message}", e)
                Toast.makeText(
                    this@MainActivity,
                    "⚠️ Setup error, app will still work with fallback data",
                    Toast.LENGTH_SHORT
                ).show()
                showUploadStatus("⚠️ Setup error, using fallback", false)
            }
        }
    }

    /**
     * Show upload status to user
     */
    private fun showUploadStatus(message: String, isLoading: Boolean) {
        runOnUiThread {
            Log.d("MainActivity", "📱 Status: $message")

            if (!isLoading) {
                // Only show final status as toast to avoid spam
                // Loading states are just logged
            }
        }
    }

    // ======================================
    // DEVELOPER TOOLS (OPTIONAL)
    // ======================================

    /**
     * Force refresh assets upload (untuk testing/debugging)
     * Bisa dipanggil dari developer options
     */
    fun refreshAssetsUpload() {
        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "🔄 Force refreshing assets upload...")
                showUploadStatus("🔄 Refreshing data...", true)

                val uploader = AssetsJsonUploader(this@MainActivity)

                val success = uploader.uploadAllFromAssets()

                if (success) {
                    Toast.makeText(this@MainActivity, "✅ Data refreshed!", Toast.LENGTH_SHORT).show()
                    Log.d("MainActivity", "✅ Refresh successful!")
                    showUploadStatus("✅ Data refreshed", false)
                } else {
                    Toast.makeText(this@MainActivity, "❌ Refresh failed", Toast.LENGTH_SHORT).show()
                    Log.e("MainActivity", "❌ Refresh failed")
                    showUploadStatus("❌ Refresh failed", false)
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Error refreshing assets: ${e.message}", e)
                Toast.makeText(this@MainActivity, "❌ Refresh error", Toast.LENGTH_SHORT).show()
                showUploadStatus("❌ Refresh error", false)
            }
        }
    }

    /**
     * Get assets info untuk display di UI (bisa dipanggil dari profile)
     */
    fun getAssetsInfo(callback: (String) -> Unit) {
        lifecycleScope.launch {
            try {
                val uploader = AssetsJsonUploader(this@MainActivity)
                val files = uploader.checkAssetsFiles()

                if (files.isNotEmpty()) {
                    val fileInfos = files.map { fileName ->
                        uploader.getAssetFileInfo(fileName)
                    }

                    val info = "📁 Assets Files:\n" + fileInfos.joinToString("\n")
                    runOnUiThread { callback(info) }
                } else {
                    runOnUiThread { callback("❌ No JSON files found in assets") }
                }
            } catch (e: Exception) {
                runOnUiThread { callback("❌ Error getting assets info: ${e.message}") }
            }
        }
    }

    /**
     * Test read specific lecturer dari assets
     */
    fun testLecturerFromAssets(nidn: String, callback: (String) -> Unit) {
        lifecycleScope.launch {
            try {
                val uploader = AssetsJsonUploader(this@MainActivity)
                val schedules = uploader.testReadLecturerFromAssets(nidn)

                val info = if (schedules.isNotEmpty()) {
                    "🎯 Found ${schedules.size} schedules for lecturer $nidn:\n" +
                            schedules.take(3).joinToString("\n") { schedule ->
                                val mata_kuliah = schedule["nama_mata_kuliah"] ?: "Unknown"
                                val hari = schedule["hari"] ?: "Unknown"
                                val jam = schedule["jam_mulai"] ?: "Unknown"
                                "• $mata_kuliah ($hari $jam)"
                            } + if (schedules.size > 3) "\n... and ${schedules.size - 3} more" else ""
                } else {
                    "❌ No schedules found for lecturer $nidn"
                }

                runOnUiThread { callback(info) }
            } catch (e: Exception) {
                runOnUiThread { callback("❌ Error testing lecturer: ${e.message}") }
            }
        }
    }
}