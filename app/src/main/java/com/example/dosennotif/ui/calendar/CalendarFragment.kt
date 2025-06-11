package com.example.dosennotif.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.example.dosennotif.R
import com.example.dosennotif.databinding.FragmentCalendarBinding
import com.example.dosennotif.model.Schedule
import com.example.dosennotif.utils.Resource
import com.example.dosennotif.viewmodel.CalendarViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CalendarViewModel
    private lateinit var scheduleAdapter: ScheduleDayAdapter

    private var selectedDay: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[CalendarViewModel::class.java]

        setupPeriodSpinner()
        setupDayFilter()
        setupRecyclerView()

        binding.btnRefresh.setOnClickListener {
            refreshData()
        }

        observeViewModel()
    }

    private fun refreshData() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvNoSchedule.visibility = View.GONE
        binding.btnRefresh.visibility = View.GONE

        viewModel.loadUserData()
    }

    private fun setupPeriodSpinner() {
        val periodLabels = viewModel.availablePeriods.map { it.second }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            periodLabels
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerPeriod.adapter = adapter

        binding.spinnerPeriod.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val periodId = viewModel.availablePeriods[position].first
                viewModel.selectPeriod(periodId)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDayFilter() {
        binding.chipAll.isChecked = true

        binding.chipGroupDays.setOnCheckedChangeListener { group, checkedId ->
            selectedDay = when (checkedId) {
                R.id.chipMonday -> "Senin"
                R.id.chipTuesday -> "Selasa"
                R.id.chipWednesday -> "Rabu"
                R.id.chipThursday -> "Kamis"
                R.id.chipFriday -> "Jumat"
                R.id.chipSaturday -> "Sabtu"
                R.id.chipSunday -> "Minggu"
                else -> null
            }

            filterSchedules()
        }
    }

    private fun setupRecyclerView() {
        scheduleAdapter = ScheduleDayAdapter(emptyMap())
        binding.rvSchedules.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = scheduleAdapter
        }
    }

    private fun filterSchedules() {
        val schedulesByDay = viewModel.schedulesByDay.value ?: return

        if (selectedDay == null) {
            scheduleAdapter.updateSchedules(schedulesByDay)

            if (schedulesByDay.isEmpty()) {
                binding.tvNoSchedule.text = getString(R.string.no_schedule_available)
                binding.tvNoSchedule.visibility = View.VISIBLE
                binding.rvSchedules.visibility = View.GONE
                binding.btnRefresh.visibility = View.VISIBLE
            } else {
                binding.tvNoSchedule.visibility = View.GONE
                binding.rvSchedules.visibility = View.VISIBLE
                binding.btnRefresh.visibility = View.GONE
            }
        } else {
            val filteredSchedules = schedulesByDay.filter { it.key == selectedDay }

            if (filteredSchedules.isEmpty()) {
                binding.tvNoSchedule.text = getString(R.string.no_schedule_for_day, selectedDay)
                binding.tvNoSchedule.visibility = View.VISIBLE
                binding.rvSchedules.visibility = View.GONE
                binding.btnRefresh.visibility = View.VISIBLE
            } else {
                binding.tvNoSchedule.visibility = View.GONE
                binding.rvSchedules.visibility = View.VISIBLE
                binding.btnRefresh.visibility = View.GONE
                scheduleAdapter.updateSchedules(filteredSchedules)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.selectedPeriod.observe(viewLifecycleOwner) { period ->
            val position = viewModel.availablePeriods.indexOfFirst { it.first == period }
            if (position >= 0 && binding.spinnerPeriod.selectedItemPosition != position) {
                binding.spinnerPeriod.setSelection(position)
            }
        }

        viewModel.schedulesByDay.observe(viewLifecycleOwner) { schedulesByDay ->
            if (schedulesByDay.isEmpty()) {
                binding.tvNoSchedule.visibility = View.VISIBLE
                binding.rvSchedules.visibility = View.GONE
            } else {
                binding.tvNoSchedule.visibility = View.GONE
                binding.rvSchedules.visibility = View.VISIBLE
                filterSchedules()
            }
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
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.tvNoSchedule.text = getString(R.string.api_error)
                        binding.tvNoSchedule.visibility = View.VISIBLE
                        binding.rvSchedules.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}