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
import com.example.dosennotif.model.Schedule
import com.example.dosennotif.ui.auth.LoginActivity
import com.example.dosennotif.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

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

        // Refresh schedule button
        binding.btnRefreshSchedule.setOnClickListener {
            viewModel.refreshSchedule()
            Toast.makeText(requireContext(), R.string.refreshing_schedule, Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeUserData() {
        // Observe user data
        viewModel.userData.observe(viewLifecycleOwner) { user ->
            user?.let {
                // Update profile info
                binding.tvName.text = it.name
                binding.tvNidn.text = it.nidn
                binding.tvEmail.text = it.email
            }
        }

        // Observe schedules for stats
        viewModel.userSchedules.observe(viewLifecycleOwner) { schedules ->
            updateScheduleStats(schedules)
        }

        // Observe next class
        viewModel.nextClass.observe(viewLifecycleOwner) { nextClass ->
            updateNextClassInfo(nextClass)
        }
    }

    private fun updateScheduleStats(schedules: List<Schedule>?) {
        if (schedules.isNullOrEmpty()) {
            binding.tvThisWeekCount.text = "0"
            binding.tvTodayCount.text = "0"
            return
        }

        val today = Calendar.getInstance()
        val currentDayOfWeek = today.get(Calendar.DAY_OF_WEEK)

        // Count today's classes
        val todayDayName = getDayName(currentDayOfWeek)
        val todayClasses = schedules.count { schedule ->
            schedule.getFormattedDay().equals(todayDayName, ignoreCase = true)
        }

        // Count this week's total classes
        val thisWeekCount = schedules.size

        binding.tvTodayCount.text = todayClasses.toString()
        binding.tvThisWeekCount.text = thisWeekCount.toString()
    }

    private fun updateNextClassInfo(nextClass: Schedule?) {
        if (nextClass == null) {
            binding.layoutNextClassInfo.visibility = View.GONE
            binding.tvNoNextClass.visibility = View.VISIBLE
        } else {
            binding.layoutNextClassInfo.visibility = View.VISIBLE
            binding.tvNoNextClass.visibility = View.GONE

            binding.tvNextClassName.text = nextClass.nama_mata_kuliah
            binding.tvNextClassTime.text = getNextClassTimeText(nextClass)
            binding.tvNextClassRoom.text = getString(R.string.room_format, nextClass.ruang)
        }
    }

    private fun getNextClassTimeText(schedule: Schedule): String {
        val today = Calendar.getInstance()
        val currentDayOfWeek = today.get(Calendar.DAY_OF_WEEK)
        val scheduleDayOfWeek = schedule.getDayOfWeekNumber()

        val daysUntil = when {
            scheduleDayOfWeek > currentDayOfWeek -> scheduleDayOfWeek - currentDayOfWeek
            scheduleDayOfWeek < currentDayOfWeek -> (7 - currentDayOfWeek) + scheduleDayOfWeek
            else -> {
                // Same day - check if class time has passed
                val now = Calendar.getInstance()
                val classTime = Calendar.getInstance().apply {
                    val (hour, minute) = schedule.jam_mulai.split(":").map { it.toInt() }
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                }
                if (now.before(classTime)) 0 else 7 // Today if not passed, next week if passed
            }
        }

        val dayText = when (daysUntil) {
            0 -> "Today"
            1 -> "Tomorrow"
            else -> schedule.getFormattedDay()
        }

        return "$dayText, ${schedule.getTimeRange()}"
    }

    private fun getDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.SUNDAY -> "Minggu"
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}