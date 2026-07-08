package mad.project.mdp_project.model

import android.app.Application
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.ScreenTimeLog
import mad.project.mdp_project.data.SessionManager
import mad.project.mdp_project.service.ScreenTimeService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

    // Category breakdowns
    private val _socialMediaTime = MutableLiveData("0m")
    val socialMediaTime: LiveData<String> = _socialMediaTime

    private val _productivityTime = MutableLiveData("0m")
    val productivityTime: LiveData<String> = _productivityTime

    private val _entertainmentTime = MutableLiveData("0m")
    val entertainmentTime: LiveData<String> = _entertainmentTime

    // Progress values (0-100)
    private val _totalProgress = MutableLiveData(0)
    val totalProgress: LiveData<Int> = _totalProgress

    private val _socialProgress = MutableLiveData(0)
    val socialProgress: LiveData<Int> = _socialProgress

    private val _productivityProgress = MutableLiveData(0)
    val productivityProgress: LiveData<Int> = _productivityProgress

    private val _entertainmentProgress = MutableLiveData(0)
    val entertainmentProgress: LiveData<Int> = _entertainmentProgress

    // Raw ms values for progress calculation
    private val _totalMs = MutableLiveData(0L)
    val totalMs: LiveData<Long> = _totalMs



    /**
     * Ambil data penggunaan app hari ini menggunakan queryEvents() untuk
     * data real-time. queryEvents() memberikan event MOVE_TO_FOREGROUND dan
     * MOVE_TO_BACKGROUND individual, sehingga kita bisa menghitung waktu
     * secara akurat termasuk app yang sedang aktif saat ini.
     */
    fun loadTodayUsage() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

                val now = System.currentTimeMillis()
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startTime = calendar.timeInMillis

                val events = usageStatsManager.queryEvents(startTime, now)

                val pm = context.packageManager

                // Track foreground time per package using events
                // Key: packageName, Value: accumulated foreground time in ms
                val foregroundTimes = mutableMapOf<String, Long>()
                // Track when each app was last moved to foreground
                val lastForegroundTimestamp = mutableMapOf<String, Long>()

                val event = UsageEvents.Event()
                while (events.hasNextEvent()) {
                    events.getNextEvent(event)

                    when (event.eventType) {
                        UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                            // App came to foreground — record the timestamp
                            lastForegroundTimestamp[event.packageName] = event.timeStamp
                        }
                        UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                            // App went to background — calculate foreground duration
                            val fgStart = lastForegroundTimestamp.remove(event.packageName)
                            if (fgStart != null) {
                                val duration = event.timeStamp - fgStart
                                foregroundTimes[event.packageName] =
                                    (foregroundTimes[event.packageName] ?: 0L) + duration
                            }
                        }
                    }
                }

                // For apps still in the foreground (no MOVE_TO_BACKGROUND yet),
                // count time up to right now — this is what makes it truly real-time
                for ((pkg, fgStart) in lastForegroundTimestamp) {
                    val duration = now - fgStart
                    foregroundTimes[pkg] = (foregroundTimes[pkg] ?: 0L) + duration
                }

                // Now calculate totals and categories
                var totalMs = 0L
                var socialMs = 0L
                var productivityMs = 0L
                var entertainmentMs = 0L

                val sortedApps = foregroundTimes.entries
                    .filter { it.value > 0 }
                    .sortedByDescending { it.value }

                val topApps = sortedApps.take(10).map { (pkg, timeMs) ->
                    val appName = try {
                        pm.getApplicationLabel(
                            pm.getApplicationInfo(pkg, 0)
                        ).toString()
                    } catch (e: Exception) {
                        pkg
                    }
                    AppUsageInfo(
                        packageName = pkg,
                        appName = appName,
                        totalTimeInForeground = timeMs
                    )
                }

                // Classify all apps and accumulate times
                for ((pkg, timeMs) in foregroundTimes) {
                    if (timeMs <= 0) continue
                    totalMs += timeMs

                    when (ScreenTimeService.classifyPackage(pkg)) {
                        "social" -> socialMs += timeMs
                        "productivity" -> productivityMs += timeMs
                        "entertainment" -> entertainmentMs += timeMs
                    }
                }

                _appUsageList.postValue(topApps)
                _totalMs.postValue(totalMs)

                // Format total screen time
                val totalMinutes = totalMs / (1000 * 60)
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                _totalScreenTime.postValue("${hours}h ${minutes}m")

                // Format category times
                _socialMediaTime.postValue(formatMs(socialMs))
                _productivityTime.postValue(formatMs(productivityMs))
                _entertainmentTime.postValue(formatMs(entertainmentMs))

                // Calculate progress percentages using user-configured limit
                val dailyGoalMs = ScreenTimeService.getDailyLimitMs(context)
                val progressPercent = if (dailyGoalMs > 0) {
                    ((totalMs.toDouble() / dailyGoalMs) * 100).toInt().coerceIn(0, 100)
                } else 0
                _totalProgress.postValue(progressPercent)
                _socialProgress.postValue(calcCategoryProgress(socialMs, totalMs))
                _productivityProgress.postValue(calcCategoryProgress(productivityMs, totalMs))
                _entertainmentProgress.postValue(calcCategoryProgress(entertainmentMs, totalMs))

                // Also persist to Room
                persistToRoom(totalMs, socialMs, productivityMs, entertainmentMs, topApps)

            } catch (e: Exception) {
                // Permission belum diberikan atau error lain
                _appUsageList.postValue(emptyList())
                _totalScreenTime.postValue("Need Permission")
                _socialMediaTime.postValue("--")
                _productivityTime.postValue("--")
                _entertainmentTime.postValue("--")
            }
        }
    }

    /**
     * Load data 7 hari terakhir dan hitung rata-rata.
     */
    fun loadWeeklyAverage() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()

                // First try Room DB for more accurate data
                val sessionManager = SessionManager(context)
                val userId = sessionManager.getUserId()
                val db = AppDatabase.getDatabase(context)

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val calendar = Calendar.getInstance()
                val endDate = dateFormat.format(calendar.time)
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val startDate = dateFormat.format(calendar.time)

                val logs = db.screenTimeLogDao().getLogsForRange(userId, startDate, endDate)

                if (logs.isNotEmpty()) {
                    val avgMs = logs.sumOf { it.totalScreenTimeMs } / logs.size
                    _dailyAverage.postValue(formatMs(avgMs))
                } else {
                    // Fall back to UsageStatsManager
                    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                    val cal = Calendar.getInstance()
                    val endTime = cal.timeInMillis
                    cal.add(Calendar.DAY_OF_YEAR, -7)
                    val startTime = cal.timeInMillis

                    val stats = usageStatsManager.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY, startTime, endTime
                    )

                    val totalMs = stats.sumOf { it.totalTimeInForeground }
                    val avgMs = totalMs / 7
                    _dailyAverage.postValue(formatMs(avgMs))
                }

            } catch (e: Exception) {
                _dailyAverage.postValue("--")
            }
        }
    }

    private fun persistToRoom(
        totalMs: Long,
        socialMs: Long,
        productivityMs: Long,
        entertainmentMs: Long,
        topApps: List<AppUsageInfo>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val sessionManager = SessionManager(context)
                val userId = sessionManager.getUserId()
                if (userId == -1) return@launch

                val db = AppDatabase.getDatabase(context)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = dateFormat.format(Calendar.getInstance().time)

                val existingLog = db.screenTimeLogDao().getLogForDate(userId, todayStr)

                val log = ScreenTimeLog(
                    id = existingLog?.id ?: 0,
                    userId = userId,
                    date = todayStr,
                    totalScreenTimeMs = totalMs,
                    socialMediaMs = socialMs,
                    productivityMs = productivityMs,
                    entertainmentMs = entertainmentMs,
                    topAppsJson = topApps.joinToString("|") { "${it.appName}:${it.totalTimeInForeground}" }
                )

                db.screenTimeLogDao().insertOrUpdate(log)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun formatMs(ms: Long): String {
        val totalMinutes = ms / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    private fun calcCategoryProgress(categoryMs: Long, totalMs: Long): Int {
        if (totalMs == 0L) return 0
        return ((categoryMs.toDouble() / totalMs) * 100).toInt().coerceIn(0, 100)
    }
}
