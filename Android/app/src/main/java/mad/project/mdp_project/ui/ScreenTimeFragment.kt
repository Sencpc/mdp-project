package mad.project.mdp_project.ui

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mad.project.mdp_project.databinding.FragmentScreenTimeBinding
import mad.project.mdp_project.model.ScreenTimeViewModel
import mad.project.mdp_project.service.ScreenTimeService

class ScreenTimeFragment : Fragment() {
    private var _binding: FragmentScreenTimeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ScreenTimeViewModel by viewModels()
    private lateinit var appUsageAdapter: AppUsageAdapter

    companion object {
        private const val REFRESH_INTERVAL_MS = 10_000L // 10 seconds
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScreenTimeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        setupRecyclerView()
        observeViewModel()
        startAutoRefresh()
    }

    private fun setupRecyclerView() {
        appUsageAdapter = AppUsageAdapter(requireContext().packageManager)
        binding.rvAppUsage.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            adapter = appUsageAdapter
        }
    }

    /**
     * Auto-refreshes screen time data every 10 seconds while the fragment
     * is in the RESUMED state. Automatically pauses when navigating away
     * and resumes when coming back.
     */
    private fun startAutoRefresh() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    if (hasUsageStatsPermission()) {
                        viewModel.loadTodayUsage()
                        viewModel.loadWeeklyAverage()
                        ensureServiceRunning()
                    }
                    delay(REFRESH_INTERVAL_MS)
                }
            }
        }
    }

    private fun setupUI() {
        binding.btnNotification.setOnClickListener {
            // Buka pengaturan Usage Access jika belum diberi permission
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun observeViewModel() {
        viewModel.totalScreenTime.observe(viewLifecycleOwner) { time ->
            // Update the time display in layoutTime
            // The layout has hardcoded TextViews inside layoutTime,
            // we update them via the parent LinearLayout
            updateTimeDisplay(binding.layoutTime, time)
        }

        viewModel.dailyAverage.observe(viewLifecycleOwner) { avg ->
            // Could update a subtitle text if available
            binding.tvSubtitle.text = "Daily average: $avg"
        }

        // Progress bar observers
        viewModel.totalProgress.observe(viewLifecycleOwner) { progress ->
            binding.progressTotal.progress = progress
        }

        // App usage list observer
        viewModel.appUsageList.observe(viewLifecycleOwner) { list ->
            appUsageAdapter.submitList(list, viewModel.totalMs.value ?: 0L)
        }

        // When totalMs changes, we might want to update the adapter so progress bars recalculate
        viewModel.totalMs.observe(viewLifecycleOwner) { totalMs ->
            appUsageAdapter.submitList(viewModel.appUsageList.value ?: emptyList(), totalMs)
        }
    }

    private fun updateTimeDisplay(layout: ViewGroup, timeStr: String) {
        // Parse "Xh Ym" format
        val regex = Regex("(\\d+)h\\s*(\\d+)m")
        val match = regex.find(timeStr)
        if (match != null && layout.childCount >= 4) {
            val hours = match.groupValues[1]
            val minutes = match.groupValues[2]
            // layoutTime has 4 children: hours, "h", minutes, "m"
            (layout.getChildAt(0) as? TextView)?.text = hours
            (layout.getChildAt(2) as? TextView)?.text = minutes
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val context = requireContext()
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun ensureServiceRunning() {
        val context = requireContext()
        val serviceIntent = Intent(context, ScreenTimeService::class.java)
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
