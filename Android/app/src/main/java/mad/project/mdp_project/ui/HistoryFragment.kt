package mad.project.mdp_project.ui

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mad.project.mdp_project.R
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.SessionManager
import java.util.Calendar

class HistoryFragment : Fragment() {

    private lateinit var sessionManager: SessionManager
    private lateinit var db: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_history, container, false)

        sessionManager = SessionManager(requireContext())
        db = AppDatabase.getDatabase(requireContext())
        val userId = sessionManager.getUserId()

        view.findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            findNavController().popBackStack()
        }

        val tvAvgSleep = view.findViewById<TextView>(R.id.tv_avg_sleep)
        val tvAvgCalories = view.findViewById<TextView>(R.id.tv_avg_calories)
        val tvAvgScreen = view.findViewById<TextView>(R.id.tv_avg_screen)
        val tvAvgHabits = view.findViewById<TextView>(R.id.tv_avg_habits)
        val tvChatSummary = view.findViewById<TextView>(R.id.tv_chat_summary)

        lifecycleScope.launch(Dispatchers.IO) {
            val user = db.userDao().getUserByIdOnce(userId)
            val sleepLogs = db.sleepLogDao().getSleepLogsForUser(userId).first()
            val nutritionLogs = db.nutritionLogDao().getLogsForUser(userId).first()
            val habits = db.habitDao().getHabitsForUser(userId).first()

            // Screen time native calculation
            val context = requireContext()
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val startTime = calendar.timeInMillis
            val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            
            var totalScreenTimeMs = 0L
            if (stats != null) {
                for (usageStats in stats) {
                    totalScreenTimeMs += usageStats.totalTimeInForeground
                }
            }

            withContext(Dispatchers.Main) {
                // Sleep Avg
                if (sleepLogs.isNotEmpty()) {
                    val totalSleep = sleepLogs.sumOf { it.endTime - it.startTime }
                    val avgSleep = totalSleep / sleepLogs.size
                    val hours = avgSleep / (1000 * 60 * 60)
                    val minutes = (avgSleep % (1000 * 60 * 60)) / (1000 * 60)
                    tvAvgSleep.text = "${hours}h ${minutes}m"
                }

                // Calories Avg
                if (nutritionLogs.isNotEmpty()) {
                    val totalCal = nutritionLogs.sumOf { it.calories }
                    val firstDate = nutritionLogs.minOfOrNull { it.consumedAt } ?: System.currentTimeMillis()
                    val lastDate = nutritionLogs.maxOfOrNull { it.consumedAt } ?: System.currentTimeMillis()
                    val daysSpan = Math.max(1, ((lastDate - firstDate) / (1000 * 60 * 60 * 24)).toInt())
                    val avgCal = totalCal / daysSpan
                    tvAvgCalories.text = "$avgCal kcal"
                }

                // Screen Avg
                val screenHours = totalScreenTimeMs / (1000 * 60 * 60)
                val screenMinutes = (totalScreenTimeMs % (1000 * 60 * 60)) / (1000 * 60)
                tvAvgScreen.text = "${screenHours}h ${screenMinutes}m"

                // Habits completion status
                if (habits.isNotEmpty()) {
                    val completed = habits.count { it.isCompleted }
                    tvAvgHabits.text = "$completed / ${habits.size}"
                }

                // Chat Summary
                if (user != null && !user.chatSummary.isNullOrBlank()) {
                    tvChatSummary.text = user.chatSummary
                } else {
                    tvChatSummary.text = "No profile summary generated yet. Chat with the AI assistant to build your personalized profile!"
                }
            }
        }

        return view
    }
}
