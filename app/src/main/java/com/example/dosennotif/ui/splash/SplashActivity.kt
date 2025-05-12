package com.example.dosennotif.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.firebase.auth.FirebaseAuth
import com.example.dosennotif.databinding.ActivitySplashBinding
import com.example.dosennotif.ui.auth.LoginActivity
import com.example.dosennotif.ui.main.MainActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var auth: FirebaseAuth

    // Splash screen display duration
    private val SPLASH_DISPLAY_LENGTH: Long = 2000 // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        // Apply new SplashScreen API for Android 12+
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Set version info dynamically
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toString()
            }
            binding.tvVersion.text = "Version $versionName ($versionCode)"
        } catch (e: Exception) {
            binding.tvVersion.text = "Version 1.0.0"
        }

        // Keep the splash screen visible for Android 12+ devices
        // until we're done loading
        splashScreen.setKeepOnScreenCondition { false }

        // Setup animation
        setupAnimation()

        // Handle navigation after delay
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, SPLASH_DISPLAY_LENGTH)
    }

    private fun setupAnimation() {
        // Fade-in animation for logo and text
        val fadeIn = AlphaAnimation(0.0f, 1.0f)
        fadeIn.duration = 1000

        binding.ivLogo.startAnimation(fadeIn)
        binding.tvAppName.startAnimation(fadeIn)
        binding.tvTagline.startAnimation(fadeIn)

        // Delayed fade-in for progress bar
        val fadeInDelayed = AlphaAnimation(0.0f, 1.0f)
        fadeInDelayed.duration = 500
        fadeInDelayed.startOffset = 1000

        binding.progressBar.startAnimation(fadeInDelayed)

        // Animation listener to display version info after animations
        fadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                binding.tvVersion.alpha = 0f
                binding.tvVersion.animate().alpha(1f).duration = 500
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }

    private fun navigateToNextScreen() {
        // Check if user is already logged in
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // User is logged in, go to MainActivity
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            // User is not logged in, go to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Apply slide transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        // Close this activity
        finish()
    }
}