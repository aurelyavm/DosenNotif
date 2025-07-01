package com.example.dosennotif.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
        binding.tvApiStatus.visibility = View.GONE

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
                        binding.tvApiStatus.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE

                        // TAMBAHAN: Indikator status sumber data
                        showDataSourceStatus(state.data)

                        viewModel.todaySchedules.observe(viewLifecycleOwner) { schedules ->
                            updateScheduleList(schedules)
                        }
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.tvNoSchedule.text = getString(R.string.api_error)
                        binding.tvNoSchedule.visibility = View.VISIBLE
                        binding.rvTodaySchedule.visibility = View.GONE
                        binding.tvApiStatus.visibility = View.VISIBLE
                        binding.tvApiStatus.text = "⚠️ Menggunakan data cadangan"
                        binding.tvApiStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark))
                    }
                }
            }
        }
    }

    // ======================================
    // FUNGSI BARU: STATUS INDICATOR
    // ======================================

    /**
     * Menampilkan status sumber data yang digunakan
     */
    private fun showDataSourceStatus(schedules: List<Schedule>) {
        if (schedules.isNotEmpty()) {
            binding.tvApiStatus.visibility = View.VISIBLE

            val firstSchedule = schedules.first()
            val dataSource = detectDataSource(firstSchedule, schedules)

            when (dataSource) {
                DataSource.API_REAL -> {
                    binding.tvApiStatus.text = "✅ Data Terkini dari Server"
                    binding.tvApiStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
                    )
                }
                DataSource.ASSETS_BACKUP -> {
                    binding.tvApiStatus.text = "📦 Data dari Assets Backup"
                    binding.tvApiStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
                    )
                }
                DataSource.INDIVIDUAL_BACKUP -> {
                    binding.tvApiStatus.text = "📂 Data dari Backup Individual"
                    binding.tvApiStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
                    )
                }
                DataSource.MOCK_DATA -> {
                    binding.tvApiStatus.text = "📱 Mode Demo - Data Simulasi"
                    binding.tvApiStatus.setTextColor(
                        ContextCompat.getColor(requireContext(), android.R.color.holo_blue_bright)
                    )
                }
            }
        } else {
            binding.tvApiStatus.visibility = View.GONE
        }
    }

    /**
     * Detect data source berdasarkan karakteristik data
     */
    private fun detectDataSource(firstSchedule: Schedule, allSchedules: List<Schedule>): DataSource {
        // Mock data detection
        if (firstSchedule.id_dosen.startsWith("mock_") ||
            firstSchedule.kode_mata_kuliah.startsWith("MOCK") ||
            firstSchedule.nama_mata_kuliah.contains("Demo")) {
            return DataSource.MOCK_DATA
        }

        // Assets backup detection (real course codes dari JSON)
        if (firstSchedule.kode_mata_kuliah.startsWith("INF") ||
            firstSchedule.kode_mata_kuliah.startsWith("SIF") ||
            firstSchedule.kode_mata_kuliah.startsWith("TIF") ||
            firstSchedule.ruang.contains("VCR") ||
            firstSchedule.ruang.contains("FIK")) {
            return DataSource.ASSETS_BACKUP
        }

        // Individual backup (usually fewer schedules, different naming)
        if (allSchedules.size < 10 &&
            !firstSchedule.kode_mata_kuliah.startsWith("INF")) {
            return DataSource.INDIVIDUAL_BACKUP
        }

        // Default: assume real API
        return DataSource.API_REAL
    }

    /**
     * Enum untuk jenis data source
     */
    private enum class DataSource {
        API_REAL,
        ASSETS_BACKUP,
        INDIVIDUAL_BACKUP,
        MOCK_DATA
    }

    // ======================================
    // FUNGSI EXISTING
    // ======================================

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