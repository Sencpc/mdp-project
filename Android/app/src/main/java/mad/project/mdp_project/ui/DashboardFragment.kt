package mad.project.mdp_project.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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
        viewModel.aiSummary.observe(viewLifecycleOwner) { summary ->
            binding.tvAiInsight.text = summary
        }
        viewModel.summaryLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.btnGetSummary.visibility = View.GONE
                binding.pbSummaryLoading.visibility = View.VISIBLE
                binding.tvAiInsight.text = "Generating insight..."
            } else {
                binding.btnGetSummary.visibility = View.GONE
                binding.pbSummaryLoading.visibility = View.GONE
            }
        }
        viewModel.pillarSleep.observe(viewLifecycleOwner) { met ->
            binding.ivPillarSleep.setColorFilter(if (met) android.graphics.Color.parseColor("#4CAF50") else android.graphics.Color.parseColor("#BDBDBD"))
        }
        viewModel.pillarFood.observe(viewLifecycleOwner) { met ->
            binding.ivPillarFood.setColorFilter(if (met) android.graphics.Color.parseColor("#4CAF50") else android.graphics.Color.parseColor("#BDBDBD"))
        }
        viewModel.pillarScreen.observe(viewLifecycleOwner) { met ->
            binding.ivPillarScreen.setColorFilter(if (met) android.graphics.Color.parseColor("#4CAF50") else android.graphics.Color.parseColor("#BDBDBD"))
        }
        viewModel.pillarHabits.observe(viewLifecycleOwner) { met ->
            binding.ivPillarHabits.setColorFilter(if (met) android.graphics.Color.parseColor("#4CAF50") else android.graphics.Color.parseColor("#BDBDBD"))
        }

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

                // Observe Weekly Nutrition Logs for Calorie Chart
                launch {
                    viewModel.weeklyNutritionLogs.collectLatest { logs ->
                        val user = viewModel.user.value
                        
                        if (viewModel.aiSummary.value.isNullOrEmpty() && logs.isNotEmpty()) {
                            viewModel.fetchAiSummary()
                        }

                        // Calculate daily calorie baseline using Mifflin-St Jeor
                        val baseline = if (user?.height != null && user.weight != null) {
                            val heightCm = user.height!!.toDouble()
                            val weightKg = user.weight!!.toDouble()
                            val age = if (user.birthDate != null) {
                                val ageDifMs = System.currentTimeMillis() - user.birthDate!!
                                val ageDate = java.util.Date(ageDifMs)
                                val cal = java.util.Calendar.getInstance()
                                cal.time = ageDate
                                Math.abs(cal.get(java.util.Calendar.YEAR) - 1970)
                            } else 25 // default age

                            // Average of male and female Mifflin-St Jeor
                            val male = 10 * weightKg + 6.25 * heightCm - 5 * age + 5
                            val female = 10 * weightKg + 6.25 * heightCm - 5 * age - 161
                            ((male + female) / 2).toInt()
                        } else {
                            2000 // Default fallback
                        }

                        val now = System.currentTimeMillis()
                        val weekAgo = now - (7 * 24 * 60 * 60 * 1000L)
                        val recentLogs = logs.filter { it.consumedAt in weekAgo..now }

                        // Group logs by day-of-week
                        val dailyCalories = mutableMapOf<Int, Int>()
                        recentLogs.forEach { log ->
                            val logCal = java.util.Calendar.getInstance()
                            logCal.timeInMillis = log.consumedAt
                            val dow = logCal.get(java.util.Calendar.DAY_OF_WEEK)
                            dailyCalories[dow] = (dailyCalories[dow] ?: 0) + log.calories
                        }

                        // Update chart
                        binding.calorieChartView.setData(dailyCalories, baseline)

                        // Update weekly total and avg/day text
                        val totalWeek = recentLogs.sumOf { it.calories }
                        val daysWithData = dailyCalories.size.coerceAtLeast(1)
                        val avgPerDay = totalWeek / daysWithData
                        binding.tvWeeklyCalories.text = "${String.format(java.util.Locale.getDefault(), "%,d", avgPerDay)} kcal"
                        binding.tvCaloriesBaselineLabel.text = "avg/day (target: $baseline)"
                    }
                }

                // Observe Sleep Logs for Dashboard
                launch {
                    viewModel.sleepLogs.collectLatest { logs ->
                        if (logs.isNotEmpty()) {
                            val now = System.currentTimeMillis()
                            val weekAgo = now - (7 * 24 * 60 * 60 * 1000L)
                            val recentLogs = logs.filter { it.date in weekAgo..now }
                            
                            val avgSleep = if (recentLogs.isNotEmpty()) {
                                recentLogs.map { (it.endTime - it.startTime).toDouble() / (1000 * 60 * 60) }.average()
                            } else 0.0
                            
                            // Re-calculate average quality (now natively 1-5 scale)
                            val avgQuality = if (logs.isNotEmpty()) {
                                logs.map { it.quality }.average().toFloat()
                            } else 0f
                            
                            // Display average sleep per day
                            val hours = avgSleep.toInt()
                            val minutes = ((avgSleep - hours) * 60).toInt()
                            binding.tvSleepValue.text = "${hours}h ${minutes}m"
                            
                            // Display average sleep quality
                            binding.tvSleepStatus.text = String.format(java.util.Locale.getDefault(), "Quality: %.1f/5", avgQuality)
                            // Draw chart for the current week (Monday to Sunday)
                            val dailySleep = mutableMapOf<Int, Double>()
                            recentLogs.forEach { log ->
                                val logCal = java.util.Calendar.getInstance()
                                logCal.timeInMillis = log.date
                                val dow = logCal.get(java.util.Calendar.DAY_OF_WEEK)
                                val durationHours = (log.endTime - log.startTime).toDouble() / (1000 * 60 * 60)
                                dailySleep[dow] = (dailySleep[dow] ?: 0.0) + durationHours
                            }
                            
                            binding.sleepChartView.setData(dailySleep, 7.0) // 7.0 hours as recommended baseline
                            binding.sleepChartView.visibility = View.VISIBLE
                        } else {
                            binding.tvSleepValue.text = "0h 0m"
                            binding.tvSleepStatus.text = "Quality: 0/5"
                            binding.sleepChartView.visibility = View.INVISIBLE
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
        
        // Collect all reminders across all habits
        val allReminders = mutableListOf<Pair<mad.project.mdp_project.data.Habit, Long>>()
        reminders.forEach { habit ->
            habit.reminders.forEach { time ->
                allReminders.add(Pair(habit, time))
            }
        }
        
        // Sort by time
        allReminders.sortBy { it.second }
        
        // Take at most 3 upcoming reminders
        allReminders.filter { pair ->
            val reminderCal = java.util.Calendar.getInstance().apply { timeInMillis = pair.second }
            val nowCal = java.util.Calendar.getInstance()
            
            val currentHour = nowCal.get(java.util.Calendar.HOUR_OF_DAY)
            val currentMinute = nowCal.get(java.util.Calendar.MINUTE)
            val reminderHour = reminderCal.get(java.util.Calendar.HOUR_OF_DAY)
            val reminderMinute = reminderCal.get(java.util.Calendar.MINUTE)
            
            // Only show future reminders
            (reminderHour > currentHour) || (reminderHour == currentHour && reminderMinute > currentMinute)
        }.take(3).forEach { pair ->
            val habit = pair.first
            val time = pair.second
            
            val reminderView = layoutInflater.inflate(R.layout.item_dashboard_reminder, container, false)
            
            val tvName = reminderView.findViewById<TextView>(R.id.tv_reminder_name)
            val tvTime = reminderView.findViewById<TextView>(R.id.tv_reminder_time)
            val indicator = reminderView.findViewById<View>(R.id.view_indicator)
            
            tvName.text = habit.name
            
            val reminderCal = java.util.Calendar.getInstance().apply { timeInMillis = time }
            tvTime.text = timeFormat.format(reminderCal.time)
            
            // It will always be green now since we filtered for future only
            indicator.setBackgroundResource(R.drawable.bg_circle_green)
            
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
            val tvFacility = consultView.findViewById<TextView>(R.id.tv_consult_facility)
            val tvStatus = consultView.findViewById<TextView>(R.id.tv_consult_status)

            tvDoctorName.text = consultation.doctorName
            tvCategory.text = consultation.category

            val formattedDate = consultation.consultationTime.format(dateFormatter)
            val formattedTime = consultation.consultationTime.format(timeFormatter)
            tvTime.text = "$formattedDate • $formattedTime"

            if (consultation.facilityName.isNotBlank()) {
                tvFacility.text = consultation.facilityName
                tvFacility.visibility = View.VISIBLE
                consultView.findViewById<View>(R.id.ic_facility).visibility = View.VISIBLE
            } else {
                tvFacility.visibility = View.GONE
                consultView.findViewById<View>(R.id.ic_facility).visibility = View.GONE
            }

            tvStatus.text = consultation.status

            container.addView(consultView)
        }
    }

    private fun setupClickListeners() {
        binding.btnGetSummary.setOnClickListener {
            viewModel.fetchAiSummary()
        }
        binding.profileCircle.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_nav_profile)
        }
        binding.ivHistory.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_nav_history)
        }
        binding.btnConsult.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_nav_personal_doctor)
        }
        binding.btnChatbot.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_nav_chatbot)
        }
        binding.btnFotoCalori.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_nav_scanner)
        }
        binding.cardCalories.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_nav_nutrition_log)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
