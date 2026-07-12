package mad.project.mdp_project.ui

import android.app.AppOpsManager
import android.app.TimePickerDialog
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

    /** Guards against toggle listener feedback loops during programmatic changes. */
    private var isUpdatingToggles = false

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
        setupNudgeToggles()
        observeViewModel()
        startAutoRefresh()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the limit display and toggle state when returning
        syncTogglesToCurrentMode()
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
        binding.btnPermission.setOnClickListener {
            // Buka pengaturan Usage Access jika belum diberi permission
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    // ── Nudge toggles ────────────────────────────────────────────────

    /**
     * Wires up the Custom and Periodic nudge toggle switches with
     * mutual-exclusion logic, and the "Set Limit" row that opens a
     * time picker dialog.
     */
    private fun setupNudgeToggles() {
        // Set initial toggle state from preferences
        syncTogglesToCurrentMode()

        // Custom toggle
        binding.switchCustom.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingToggles) return@setOnCheckedChangeListener
            isUpdatingToggles = true

            if (isChecked) {
                // Enable custom mode, disable periodic
                binding.switchPeriodic.isChecked = false
                ScreenTimeService.setNudgeMode(requireContext(), ScreenTimeService.MODE_CUSTOM)
            } else {
                // If periodic isn't on either, go to none
                if (!binding.switchPeriodic.isChecked) {
                    ScreenTimeService.setNudgeMode(requireContext(), ScreenTimeService.MODE_NONE)
                }
            }
            updateSetLimitVisibility()
            updateLimitDisplay()
            isUpdatingToggles = false
        }

        // Periodic toggle
        binding.switchPeriodic.setOnCheckedChangeListener { _, isChecked ->
            if (isUpdatingToggles) return@setOnCheckedChangeListener
            isUpdatingToggles = true

            if (isChecked) {
                // Enable periodic mode, disable custom
                binding.switchCustom.isChecked = false
                ScreenTimeService.setNudgeMode(requireContext(), ScreenTimeService.MODE_PERIODIC)
            } else {
                // If custom isn't on either, go to none
                if (!binding.switchCustom.isChecked) {
                    ScreenTimeService.setNudgeMode(requireContext(), ScreenTimeService.MODE_NONE)
                }
            }
            updateSetLimitVisibility()
            updateLimitDisplay()
            isUpdatingToggles = false
        }

        // "Set Limit" row — tapping opens a time picker (only relevant for custom mode)
        binding.layoutSetLimit.setOnClickListener {
            showLimitTimePicker()
        }
    }

    /**
     * Reads the persisted nudge mode and reflects it on the two switches.
     * Also updates the Set Limit row visibility and goal label.
     */
    private fun syncTogglesToCurrentMode() {
        val mode = ScreenTimeService.getNudgeMode(requireContext())
        isUpdatingToggles = true
        binding.switchCustom.isChecked = (mode == ScreenTimeService.MODE_CUSTOM)
        binding.switchPeriodic.isChecked = (mode == ScreenTimeService.MODE_PERIODIC)
        isUpdatingToggles = false
        updateSetLimitVisibility()
        updateLimitDisplay()
    }

    /**
     * Shows or hides the "Set Limit" row based on whether Custom mode is active.
     */
    private fun updateSetLimitVisibility() {
        binding.layoutSetLimit.visibility =
            if (binding.switchCustom.isChecked) View.VISIBLE else View.GONE
    }

    /**
     * Shows a TimePickerDialog configured for selecting a duration (hours + minutes)
     * for the custom screen time limit.
     */
    private fun showLimitTimePicker() {
        val context = requireContext()
        val currentHours = ScreenTimeService.getLimitHours(context)
        val currentMinutes = ScreenTimeService.getLimitMinutes(context)

        val picker = TimePickerDialog(
            context,
            { _, selectedHour, selectedMinute ->
                // Ensure at least 1 minute is set
                val finalHour = if (selectedHour == 0 && selectedMinute == 0) 0 else selectedHour
                val finalMinute = if (selectedHour == 0 && selectedMinute == 0) 1 else selectedMinute

                ScreenTimeService.setDailyLimit(context, finalHour, finalMinute)
                updateLimitDisplay()

                // Notify the ViewModel to recalculate progress with the new limit
                viewModel.loadTodayUsage()
            },
            currentHours,
            currentMinutes,
            true // 24-hour format works well for duration
        )
        picker.setTitle("Set Reminder Limit (hours : minutes)")
        picker.show()
    }

    /**
     * Updates the "Set Limit" value label and the goal label on the progress bar,
     * depending on the active nudge mode.
     */
    private fun updateLimitDisplay() {
        val context = requireContext()
        val mode = ScreenTimeService.getNudgeMode(context)

        when (mode) {
            ScreenTimeService.MODE_CUSTOM -> {
                val limitStr = ScreenTimeService.formatLimit(context)
                binding.tvLimitValue.text = limitStr
                binding.tvGoalLabel.text = "Goal: $limitStr"
            }
            ScreenTimeService.MODE_PERIODIC -> {
                binding.tvGoalLabel.text = "Goal: 4h (periodic)"
            }
            else -> {
                // No mode active — show default limit on the progress bar
                val limitStr = ScreenTimeService.formatLimit(context)
                binding.tvLimitValue.text = limitStr
                binding.tvGoalLabel.text = "Goal: $limitStr"
            }
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
            if (progress >= 100) {
                binding.progressTotal.setIndicatorColor(android.graphics.Color.RED)
            } else {
                binding.progressTotal.setIndicatorColor(android.graphics.Color.parseColor("#003538"))
            }
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

