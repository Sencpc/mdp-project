package mad.project.mdp_project.model

import android.app.Application
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.Calendar

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeInForeground: Long // in milliseconds
) {
    fun getFormattedTime(): String {
        val totalMinutes = totalTimeInForeground / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}

class ScreenTimeViewModel(application: Application) : AndroidViewModel(application) {

    private val _appUsageList = MutableLiveData<List<AppUsageInfo>>(emptyList())
    val appUsageList: LiveData<List<AppUsageInfo>> = _appUsageList

    private val _totalScreenTime = MutableLiveData("0h 0m")
    val totalScreenTime: LiveData<String> = _totalScreenTime

    private val _dailyAverage = MutableLiveData("0h 0m")
    val dailyAverage: LiveData<String> = _dailyAverage

    /**
     * Ambil data penggunaan app hari ini.
     * Membutuhkan permission PACKAGE_USAGE_STATS.
     */
    fun loadTodayUsage() {
        try {
            val context = getApplication<Application>()
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startTime = calendar.timeInMillis

            val stats: List<UsageStats> = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, startTime, endTime
            )

            val pm = context.packageManager
            val usageList = stats
                .filter { it.totalTimeInForeground > 0 }
                .sortedByDescending { it.totalTimeInForeground }
                .take(10)
                .map { stat ->
                    val appName = try {
                        pm.getApplicationLabel(
                            pm.getApplicationInfo(stat.packageName, 0)
                        ).toString()
                    } catch (e: Exception) {
                        stat.packageName
                    }
                    AppUsageInfo(
                        packageName = stat.packageName,
                        appName = appName,
                        totalTimeInForeground = stat.totalTimeInForeground
                    )
                }

            _appUsageList.value = usageList

            // Hitung total screen time
            val totalMs = usageList.sumOf { it.totalTimeInForeground }
            val totalMinutes = totalMs / (1000 * 60)
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            _totalScreenTime.value = "${hours}h ${minutes}m"

        } catch (e: Exception) {
            // Permission belum diberikan atau error lain
            _appUsageList.value = emptyList()
            _totalScreenTime.value = "Need Permission"
        }
    }

    /**
     * Load data 7 hari terakhir dan hitung rata-rata.
     */
    fun loadWeeklyAverage() {
        try {
            val context = getApplication<Application>()
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            val calendar = Calendar.getInstance()
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val startTime = calendar.timeInMillis

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, startTime, endTime
            )

            val totalMs = stats.sumOf { it.totalTimeInForeground }
            val avgMs = totalMs / 7
            val avgMinutes = avgMs / (1000 * 60)
            val hours = avgMinutes / 60
            val minutes = avgMinutes % 60
            _dailyAverage.value = "${hours}h ${minutes}m"

        } catch (e: Exception) {
            _dailyAverage.value = "--"
        }
    }
}
