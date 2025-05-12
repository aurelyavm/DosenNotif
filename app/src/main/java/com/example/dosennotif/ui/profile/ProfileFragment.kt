package com.example.dosennotif.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.dosennotif.R
import com.example.dosennotif.databinding.FragmentProfileBinding
import com.example.dosennotif.model.NotificationPreferences
import com.example.dosennotif.ui.auth.LoginActivity
import com.example.dosennotif.utils.Resource
import com.example.dosennotif.viewmodel.ProfileViewModel

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProfileViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[ProfileViewModel::class.java]

        // Setup UI interactions
        setupListeners()

        // Observe user data
        observeUserData()
    }

    private fun setupListeners() {
        // Logout button
        binding.btnLogout.setOnClickListener {
            viewModel.signOut()

            // Navigate to login screen
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        // Save settings button
        binding.btnSaveDistanceSettings.setOnClickListener {
            saveSettings()
        }

        // Notification toggle switches
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            updateNotificationToggles(isChecked)
        }
    }

    private fun observeUserData() {
        viewModel.userData.observe(viewLifecycleOwner) { user ->
            user?.let {
                // Update profile info
                binding.tvName.text = it.name
                binding.tvNidn.text = getString(R.string.nidn_format, it.nidn)
                binding.tvEmail.text = it.email

                // Update notification settings
                val prefs = it.notificationPreferences
                binding.switchNotifications.isChecked = prefs.enabled
                binding.switchSound.isChecked = prefs.soundEnabled
                binding.switchVibration.isChecked = prefs.vibrationEnabled

                // Update distance thresholds
                updateDistanceThresholds(prefs.distanceThresholds)

                // Update UI state based on notification enabled
                updateNotificationToggles(prefs.enabled)
            }
        }

        // Observe saving state
        viewModel.savingState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> {
                    binding.btnSaveDistanceSettings.isEnabled = false
                    binding.btnSaveDistanceSettings.text = getString(R.string.saving)
                }
                is Resource.Success -> {
                    binding.btnSaveDistanceSettings.isEnabled = true
                    binding.btnSaveDistanceSettings.text = getString(R.string.save_settings)
                    Toast.makeText(requireContext(), R.string.settings_saved, Toast.LENGTH_SHORT).show()
                }
                is Resource.Error -> {
                    binding.btnSaveDistanceSettings.isEnabled = true
                    binding.btnSaveDistanceSettings.text = getString(R.string.save_settings)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateDistanceThresholds(thresholds: Map<String, Int>) {
        binding.etDistance0to10.setText(thresholds["0-10"]?.toString() ?: "30")
        binding.etDistance10to20.setText(thresholds["10-20"]?.toString() ?: "60")
        binding.etDistance20to30.setText(thresholds["20-30"]?.toString() ?: "90")
        binding.etDistance30to40.setText(thresholds["30-40"]?.toString() ?: "120")
        binding.etDistance40to50.setText(thresholds["40-50"]?.toString() ?: "150")
    }

    private fun updateNotificationToggles(enabled: Boolean) {
        binding.switchSound.isEnabled = enabled
        binding.switchVibration.isEnabled = enabled
        binding.cardDistanceSettings.alpha = if (enabled) 1.0f else 0.5f
        binding.etDistance0to10.isEnabled = enabled
        binding.etDistance10to20.isEnabled = enabled
        binding.etDistance20to30.isEnabled = enabled
        binding.etDistance30to40.isEnabled = enabled
        binding.etDistance40to50.isEnabled = enabled
        binding.btnSaveDistanceSettings.isEnabled = enabled
    }

    private fun saveSettings() {
        // Get current values
        val notificationsEnabled = binding.switchNotifications.isChecked
        val soundEnabled = binding.switchSound.isChecked
        val vibrationEnabled = binding.switchVibration.isChecked

        // Get distance thresholds
        val distanceThresholds = mapOf(
            Pair("0-10", binding.etDistance0to10.text.toString().toIntOrNull() ?: 30),
            Pair("10-20", binding.etDistance10to20.text.toString().toIntOrNull() ?: 60),
            Pair("20-30", binding.etDistance20to30.text.toString().toIntOrNull() ?: 90),
            Pair("30-40", binding.etDistance30to40.text.toString().toIntOrNull() ?: 120),
            Pair("40-50", binding.etDistance40to50.text.toString().toIntOrNull() ?: 150)
        )

        // Create new preferences object
        val newPreferences = NotificationPreferences(
            enabled = notificationsEnabled,
            soundEnabled = soundEnabled,
            vibrationEnabled = vibrationEnabled,
            distanceThresholds = distanceThresholds
        )

        // Update preferences
        viewModel.updateNotificationPreferences(newPreferences)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}