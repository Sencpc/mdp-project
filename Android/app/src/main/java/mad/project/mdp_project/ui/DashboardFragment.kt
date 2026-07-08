package mad.project.mdp_project.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mad.project.mdp_project.R
import mad.project.mdp_project.data.ConsultationEntity
import mad.project.mdp_project.databinding.FragmentDashboardBinding
import mad.project.mdp_project.model.DashboardViewModel
import java.time.format.DateTimeFormatter

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // Refresh screen time data when returning to dashboard
        // (e.g. after changing daily limit on the Screen Time page)
        viewModel.loadScreenTimeData()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe User Data
                launch {
                    viewModel.user.collectLatest { user ->
                        user?.let {
                            binding.tvGreeting.text = "Good morning, ${it.fullName.ifEmpty { it.username }}."

                            if (!it.profilePicturePath.isNullOrEmpty()) {
                                binding.profileCircle.load(Uri.parse(it.profilePicturePath)) {
                                    crossfade(true)
                                    placeholder(R.drawable.bg_circle_green)
                                    error(R.drawable.bg_circle_green)
                                }
                                binding.profileCircle.setPadding(0, 0, 0, 0)
                            } else {
                                binding.profileCircle.setImageResource(R.drawable.bg_circle_green)
                                binding.profileCircle.setPadding(0, 0, 0, 0)
                            }
                        }
                    }
                }

                // Observe Habits — update progress circle, habit count, and streak
                launch {
                    viewModel.habits.collectLatest { habits ->
                        val completed = habits.count { it.isCompleted }
                        val total = habits.size
                        val progress = if (total > 0) (completed * 100 / total) else 0

                        // Update progress circle
                        binding.circularProgressBar.progress = progress
                        binding.tvProgressPercent.text = "${progress}%"

                        // Update habits completed text
                        binding.tvHabitsCompleted.text = "$completed of $total Habits\nCompleted"

                        // Update daily streak
                        // Show max individual streak; add +1 if all habits completed today
                        val maxStreak = habits.maxOfOrNull { it.streak } ?: 0
                        val displayStreak = if (completed == total && total > 0) maxStreak + 1 else maxStreak
                        binding.tvDailyStreak.text = "$displayStreak Day Streak"

                        // Update status message
                        binding.tvProgressStatus.text = when {
                            total == 0 -> "Add some habits to get started!"
                            completed == total -> "All habits completed! Amazing! 🎉"
                            else -> "You're on track. Keep it up!"
                        }
                    }
                }

                // Observe Reminders
                launch {
                    viewModel.habitsWithReminder.collectLatest { reminders ->
                        updateRemindersUI(reminders)
                    }
                }

                // Observe Upcoming Consultations
                launch {
                    viewModel.upcomingConsultations.collectLatest { consultations ->
                        updateConsultationsUI(consultations)
                    }
                }

                // Observe Sleep Logs for Dashboard
                launch {
                    viewModel.sleepLogs.collectLatest { logs ->
                        if (logs.isNotEmpty()) {
                            val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                            val recentLogs = logs.filter { it.date >= weekAgo }
                            
                            val avgSleep = if (recentLogs.isNotEmpty()) {
                                recentLogs.map { (it.endTime - it.startTime).toDouble() / (1000 * 60 * 60) }.average()
                            } else 0.0
                            
                            // Re-calculate average quality using 6 hours standard for the dashboard
                            val avgQuality = if (logs.isNotEmpty()) {
                                logs.map { it.quality }.average().toFloat() * 5f
                            } else 0f
                            
                            // Display average sleep per day
                            val hours = avgSleep.toInt()
                            val minutes = ((avgSleep - hours) * 60).toInt()
                            binding.tvSleepValue.text = "${hours}h ${minutes}m"
                            
                            // Display average sleep quality
                            binding.tvSleepStatus.text = String.format(java.util.Locale.getDefault(), "Quality: %.1f/5", avgQuality)
                            
                            // Draw chart for the current week (Monday to Sunday)
                            binding.llSleepChart.removeAllViews()
                            
                            val todayCal = java.util.Calendar.getInstance()
                            val todayYear = todayCal.get(java.util.Calendar.YEAR)
                            val todayDayOfYear = todayCal.get(java.util.Calendar.DAY_OF_YEAR)
                            
                            val startOfWeekCal = java.util.Calendar.getInstance()
                            var dayOfWeek = startOfWeekCal.get(java.util.Calendar.DAY_OF_WEEK)
                            if (dayOfWeek == java.util.Calendar.SUNDAY) {
                                dayOfWeek = 8 // Treat Sunday as the end of the week
                            }
                            // Subtract days to get back to Monday (day 2)
                            startOfWeekCal.add(java.util.Calendar.DAY_OF_YEAR, -(dayOfWeek - 2))
                            
                            for (i in 0..6) {
                                val dayCal = startOfWeekCal.clone() as java.util.Calendar
                                dayCal.add(java.util.Calendar.DAY_OF_YEAR, i)
                                val targetYear = dayCal.get(java.util.Calendar.YEAR)
                                val targetDay = dayCal.get(java.util.Calendar.DAY_OF_YEAR)
                                
                                val isToday = (targetYear == todayYear && targetDay == todayDayOfYear)
                                
                                val logsForDay = logs.filter { log ->
                                    val logCal = java.util.Calendar.getInstance()
                                    logCal.timeInMillis = log.date
                                    logCal.get(java.util.Calendar.YEAR) == targetYear &&
                                    logCal.get(java.util.Calendar.DAY_OF_YEAR) == targetDay
                                }
                                
                                val dayQuality = if (logsForDay.isNotEmpty()) logsForDay.map { it.quality }.average().toFloat() else 0.0f
                                
                                val bar = View(requireContext())
                                val lp = android.widget.LinearLayout.LayoutParams(0, 0, 1f)
                                lp.marginEnd = if (i == 6) 0 else 8
                                
                                val maxDp = 40
                                val density = resources.displayMetrics.density
                                val heightPx = (dayQuality * maxDp * density).toInt()
                                lp.height = if (heightPx < 4) (2 * density).toInt() else heightPx
                                
                                bar.layoutParams = lp
                                bar.setBackgroundColor(if (isToday) android.graphics.Color.parseColor("#004B4F") else android.graphics.Color.parseColor("#E0E0E0"))
                                
                                binding.llSleepChart.addView(bar)
                            }
                        } else {
                            binding.tvSleepValue.text = "0h 0m"
                            binding.tvSleepStatus.text = "Quality: 0/5"
                            binding.llSleepChart.removeAllViews()
                        }
                    }
                }
            }
        }

        // Observe Screen Time data (LiveData from DashboardViewModel)
        viewModel.screenTimeValue.observe(viewLifecycleOwner) { value ->
            binding.tvScreenValue.text = value
        }
        viewModel.screenTimeComparison.observe(viewLifecycleOwner) { comparison ->
            binding.tvScreenComparison.text = comparison
        }
        viewModel.screenTimeStatus.observe(viewLifecycleOwner) { status ->
            binding.tvScreenStatus.text = status
        }
    }

    private fun updateRemindersUI(reminders: List<mad.project.mdp_project.data.Habit>) {
        val container = binding.llRemindersContainer
        // Keep the "no reminders" text view, remove others
        for (i in container.childCount - 1 downTo 0) {
            val view = container.getChildAt(i)
            if (view.id != R.id.tv_no_reminders) {
                container.removeViewAt(i)
            }
        }
        
        if (reminders.isEmpty()) {
            binding.tvNoReminders.visibility = View.VISIBLE
            return
        }
        
        binding.tvNoReminders.visibility = View.GONE
        
        val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        
        // Take at most 3 upcoming reminders
        reminders.take(3).forEach { habit ->
            val reminderView = layoutInflater.inflate(R.layout.item_dashboard_reminder, container, false)
            
            val tvName = reminderView.findViewById<TextView>(R.id.tv_reminder_name)
            val tvTime = reminderView.findViewById<TextView>(R.id.tv_reminder_time)
            val indicator = reminderView.findViewById<View>(R.id.view_indicator)
            
            tvName.text = habit.name
            
            habit.reminderTime?.let { time ->
                val reminderCal = java.util.Calendar.getInstance().apply { timeInMillis = time }
                tvTime.text = timeFormat.format(reminderCal.time)
                
                val nowCal = java.util.Calendar.getInstance()
                val currentHour = nowCal.get(java.util.Calendar.HOUR_OF_DAY)
                val currentMinute = nowCal.get(java.util.Calendar.MINUTE)
                val reminderHour = reminderCal.get(java.util.Calendar.HOUR_OF_DAY)
                val reminderMinute = reminderCal.get(java.util.Calendar.MINUTE)
                
                val isPast = (currentHour > reminderHour) || (currentHour == reminderHour && currentMinute >= reminderMinute)
                
                if (isPast) {
                    indicator.setBackgroundResource(R.drawable.bg_circle_gray)
                } else {
                    indicator.setBackgroundResource(R.drawable.bg_circle_green)
                }
            }
            
            container.addView(reminderView)
        }
    }

    private fun updateConsultationsUI(consultations: List<ConsultationEntity>) {
        val container = binding.llConsultationsContainer
        // Keep the "no consultations" text view, remove others
        for (i in container.childCount - 1 downTo 0) {
            val view = container.getChildAt(i)
            if (view.id != R.id.tv_no_consultations) {
                container.removeViewAt(i)
            }
        }

        if (consultations.isEmpty()) {
            binding.tvNoConsultations.visibility = View.VISIBLE
            return
        }

        binding.tvNoConsultations.visibility = View.GONE

        val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

        consultations.take(3).forEach { consultation ->
            val consultView = layoutInflater.inflate(R.layout.item_dashboard_consultation, container, false)

            val tvDoctorName = consultView.findViewById<TextView>(R.id.tv_consult_doctor_name)
            val tvCategory = consultView.findViewById<TextView>(R.id.tv_consult_category)
            val tvTime = consultView.findViewById<TextView>(R.id.tv_consult_time)
            val tvStatus = consultView.findViewById<TextView>(R.id.tv_consult_status)

            tvDoctorName.text = consultation.doctorName
            tvCategory.text = consultation.category

            val formattedDate = consultation.consultationTime.format(dateFormatter)
            val formattedTime = consultation.consultationTime.format(timeFormatter)
            tvTime.text = "$formattedDate • $formattedTime"

            tvStatus.text = consultation.status

            container.addView(consultView)
        }
    }

    private fun setupClickListeners() {
        binding.profileCircle.setOnClickListener {
            findNavController().navigate(R.id.action_nav_dashboard_to_nav_profile)
        }
        binding.btnConsult.setOnClickListener {
            findNavController().navigate(R.id.action_nav_dashboard_to_nav_personal_doctor)
        }
        binding.btnChatbot.setOnClickListener {
            findNavController().navigate(R.id.action_nav_dashboard_to_nav_chatbot)
        }
        binding.btnFotoCalori.setOnClickListener {
            findNavController().navigate(R.id.action_nav_dashboard_to_nav_scanner)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
