package com.example.dosennotif.ui.notification

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.dosennotif.databinding.FragmentNotificationBinding
import com.example.dosennotif.model.ScheduleNotification
import com.example.dosennotif.utils.Resource
import com.example.dosennotif.viewmodel.NotificationViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationFragment : Fragment() {

    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: NotificationViewModel
    private lateinit var notificationAdapter: NotificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[NotificationViewModel::class.java]

        // Setup RecyclerView
        setupRecyclerView()

        // Observe notifications
        observeNotifications()
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(emptyList()) { notification ->
            // Hanya mark as read, tanpa buka detail
            viewModel.markNotificationAsRead(notification.id)
        }

        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationAdapter
        }
    }

    private fun observeNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notificationsState.collectLatest { state ->
                when (state) {
                    is Resource.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.tvNoNotifications.visibility = View.GONE
                    }
                    is Resource.Success -> {
                        binding.progressBar.visibility = View.GONE
                        updateNotificationList(state.data)
                    }
                    is Resource.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.tvNoNotifications.text = state.message
                        binding.tvNoNotifications.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun updateNotificationList(notifications: List<ScheduleNotification>) {
        if (notifications.isEmpty()) {
            binding.tvNoNotifications.visibility = View.VISIBLE
            binding.rvNotifications.visibility = View.GONE
        } else {
            binding.tvNoNotifications.visibility = View.GONE
            binding.rvNotifications.visibility = View.VISIBLE
            notificationAdapter.updateNotifications(notifications)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh notifications when returning to this fragment
        viewModel.loadNotifications()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}