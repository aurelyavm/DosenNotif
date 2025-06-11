package com.example.dosennotif.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dosennotif.R
import com.example.dosennotif.databinding.FragmentHomeBinding
import com.example.dosennotif.model.Schedule
import com.example.dosennotif.utils.LocationUtils
import com.example.dosennotif.utils.Resource
import com.example.dosennotif.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HomeViewModel
    private lateinit var scheduleAdapter: ScheduleAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        setupRecyclerView()

        setGreeting()

        binding.btnRefresh.setOnClickListener {
            refreshData()
        }

        observeViewModel()

        requestLocationUpdates()
    }

    private fun refreshData() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvNoSchedule.visibility = View.GONE
        binding.btnRefresh.visibility = View.GONE

        viewModel.loadUserData()
    }

    private fun setupRecyclerView() {
        scheduleAdapter = ScheduleAdapter(emptyList())
        binding.rvTodaySchedule.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = scheduleAdapter
        }
    }

    private fun setGreeting() {
        val calendar = Calendar.getInstance()
        val timeOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        val greeting = when {
            timeOfDay < 12 -> getString(R.string.good_morning)
            timeOfDay < 17 -> getString(R.string.good_afternoon)
            else -> getString(R.string.good_evening)
        }

        binding.tvGreeting.text = greeting
    }

    private fun observeViewModel() {
        viewModel.userData.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.tvName.text = it.name
            }
        }

        viewModel.distanceFromCampus.observe(viewLifecycleOwner) { distance ->
            binding.tvDistance.text = String.format("%.2f km", distance)

            val notificationMinutes = LocationUtils.getNotificationDelay(distance)
            binding.tvNotificationInfo.text = getString(
                R.string.notification_will_appear,
                notificationMinutes
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scheduleState.collectLatest { state ->
                when (state) {
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.tvNoSchedule.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE

                        viewModel.todaySchedules.observe(viewLifecycleOwner) { schedules ->
                            updateScheduleList(schedules)
                        }
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.tvNoSchedule.text = getString(R.string.api_error)
                        binding.tvNoSchedule.visibility = View.VISIBLE
                        binding.rvTodaySchedule.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun updateScheduleList(schedules: List<Schedule>?) {
        if (schedules.isNullOrEmpty()) {
            binding.tvNoSchedule.visibility = View.VISIBLE
            binding.rvTodaySchedule.visibility = View.GONE
            binding.btnRefresh.visibility = View.VISIBLE
        } else {
            binding.tvNoSchedule.visibility = View.GONE
            binding.rvTodaySchedule.visibility = View.VISIBLE
            binding.btnRefresh.visibility = View.GONE
            scheduleAdapter.updateSchedules(schedules)
        }
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        lifecycleScope.launch {
            val location = LocationUtils.getLastLocation(requireContext())
            location?.let {
                viewModel.updateCurrentLocation(it)
            }
        }

        lifecycleScope.launch {
            LocationUtils.getLocationUpdates(requireContext()).collect { location ->
                viewModel.updateCurrentLocation(location)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}