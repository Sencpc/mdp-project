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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.ConsultationEntity
import mad.project.mdp_project.data.Habit
import mad.project.mdp_project.data.NutritionLog
import mad.project.mdp_project.data.SessionManager
import mad.project.mdp_project.data.SleepLog
import mad.project.mdp_project.data.User
import mad.project.mdp_project.data.remote.RetrofitClient
import mad.project.mdp_project.data.repository.ConsultationRepository
import mad.project.mdp_project.data.repository.HabitRepository
import mad.project.mdp_project.data.repository.NutritionRepository
import mad.project.mdp_project.data.repository.SleepRepository
import mad.project.mdp_project.data.repository.UserRepository
import mad.project.mdp_project.service.ScreenTimeService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val userId = sessionManager.getUserId()

    private val db = AppDatabase.getDatabase(application)
    private val api = RetrofitClient.apiService
    private val userRepository = UserRepository(db.userDao(), api)
    private val habitRepository = HabitRepository(db.habitDao(), api)
    private val sleepRepository = SleepRepository(db.sleepLogDao(), api)
    private val nutritionRepository = NutritionRepository(db.nutritionLogDao(), api)
    private val consultationRepository = ConsultationRepository(db.consultationDao(), db.reviewDao())

    val user: StateFlow<User?> = userRepository.getUserById(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val habits: StateFlow<List<Habit>> = habitRepository.getHabitsForUser(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val habitsWithReminder: StateFlow<List<Habit>> = habitRepository.getHabitsWithReminder(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sleepLogs: StateFlow<List<SleepLog>> = sleepRepository.getSleepLogsForUser(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val upcomingConsultations: StateFlow<List<ConsultationEntity>> = consultationRepository.getUpcomingConsultations()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val weeklyNutritionLogs: StateFlow<List<NutritionLog>> = run {
        val cal = Calendar.getInstance()
        // Go back to Monday of this week
        var dow = cal.get(Calendar.DAY_OF_WEEK)
        if (dow == Calendar.SUNDAY) dow = 8
        cal.add(Calendar.DAY_OF_YEAR, -(dow - Calendar.MONDAY))
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        nutritionRepository.getWeeklyLogs(userId, cal.timeInMillis)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    }

    // Screen time data for dashboard
    private val _screenTimeValue = MutableLiveData("0h 0m")
    val screenTimeValue: LiveData<String> = _screenTimeValue

    private val _screenTimeComparison = MutableLiveData("")
    val screenTimeComparison: LiveData<String> = _screenTimeComparison

    private val _screenTimeStatus = MutableLiveData("")
    val screenTimeStatus: LiveData<String> = _screenTimeStatus

    init {
        // Sync data dari server saat dashboard dibuka
        viewModelScope.launch {
            habitRepository.syncFromServer(userId)
            sleepRepository.syncFromServer(userId)
            nutritionRepository.syncFromServer(userId)
        }
        // Load screen time data
        loadScreenTimeData()
    }

    fun loadScreenTimeData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

                val todayTotalMs = getTodayScreenTime(usageStatsManager)
                val weeklyAvgMs = getWeeklyAverage(context)

                // Format today's screen time
                val totalMinutes = todayTotalMs / (1000 * 60)
                val hours = totalMinutes / 60
                val minutes = totalMinutes % 60
                _screenTimeValue.postValue("${hours}h ${minutes}m")

                // Comparison vs average
                if (weeklyAvgMs > 0) {
                    val diffMs = todayTotalMs - weeklyAvgMs
                    val diffMinutes = Math.abs(diffMs) / (1000 * 60)
                    val diffHours = diffMinutes / 60
                    val diffMins = diffMinutes % 60
                    val diffStr = if (diffHours > 0) "${diffHours}h ${diffMins}m" else "${diffMins}m"
                    val sign = if (diffMs >= 0) "+" else "-"
                    _screenTimeComparison.postValue("${sign}${diffStr} vs avg")
                } else {
                    _screenTimeComparison.postValue("")
                }

                // Status vs daily limit
                val limitMs = ScreenTimeService.getDailyLimitMs(context)
                val limitStr = ScreenTimeService.formatLimit(context)
                if (todayTotalMs >= limitMs) {
                    _screenTimeStatus.postValue("You've exceeded your daily limit of $limitStr.")
                } else {
                    val remainingMs = limitMs - todayTotalMs
                    val remMinutes = remainingMs / (1000 * 60)
                    val remH = remMinutes / 60
                    val remM = remMinutes % 60
                    val remStr = if (remH > 0) "${remH}h ${remM}m" else "${remM}m"
                    _screenTimeStatus.postValue("$remStr remaining until your $limitStr limit.")
                }

            } catch (e: Exception) {
                _screenTimeValue.postValue("--")
                _screenTimeComparison.postValue("")
                _screenTimeStatus.postValue("Grant usage access permission")
            }
        }
    }

    private fun getTodayScreenTime(usageStatsManager: UsageStatsManager): Long {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val events = usageStatsManager.queryEvents(startTime, now)

        val foregroundTimes = mutableMapOf<String, Long>()
        val lastForegroundTimestamp = mutableMapOf<String, Long>()

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    lastForegroundTimestamp[event.packageName] = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val fgStart = lastForegroundTimestamp.remove(event.packageName)
                    if (fgStart != null) {
                        val duration = event.timeStamp - fgStart
                        foregroundTimes[event.packageName] =
                            (foregroundTimes[event.packageName] ?: 0L) + duration
                    }
                }
            }
        }

        // For apps still in the foreground, count time up to now
        for ((pkg, fgStart) in lastForegroundTimestamp) {
            val duration = now - fgStart
            foregroundTimes[pkg] = (foregroundTimes[pkg] ?: 0L) + duration
        }

        return foregroundTimes.values.sum()
    }

    private fun getWeeklyAverage(context: Context): Long {
        // Try Room DB first for accurate historical data
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val endDate = dateFormat.format(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startDate = dateFormat.format(calendar.time)

        val logs = kotlinx.coroutines.runBlocking {
            db.screenTimeLogDao().getLogsForRange(userId, startDate, endDate)
        }

        return if (logs.isNotEmpty()) {
            logs.sumOf { it.totalScreenTimeMs } / logs.size
        } else {
            0L
        }
    }

    fun getCompletedHabitsCount(): Int {
        return habits.value.count { it.isCompleted }
    }

    fun getTotalHabitsCount(): Int {
        return habits.value.size
    }

    fun getLatestSleepDuration(): String {
        val latest = sleepLogs.value.firstOrNull() ?: return "--"
        return latest.getFormattedDuration()
    }
}

