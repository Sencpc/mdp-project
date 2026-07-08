package mad.project.mdp_project.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.SessionManager
import mad.project.mdp_project.data.SleepLog
import mad.project.mdp_project.data.remote.RetrofitClient
import mad.project.mdp_project.data.repository.SleepRepository

class SleepViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val userId = sessionManager.getUserId()

    private val db = AppDatabase.getDatabase(application)
    private val sleepRepository = SleepRepository(db.sleepLogDao(), RetrofitClient.apiService)

    val sleepLogs: StateFlow<List<SleepLog>> = sleepRepository.getSleepLogsForUser(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _addResult = MutableLiveData<Result<SleepLog>>()
    val addResult: LiveData<Result<SleepLog>> = _addResult

    init {
        viewModelScope.launch {
            sleepRepository.syncFromServer(userId)
        }
    }

    fun addSleepLog(startTime: Long, endTime: Long) {
        if (endTime <= startTime) {
            _addResult.value = Result.failure(Exception("Waktu selesai harus lebih besar dari waktu mulai"))
            return
        }

        viewModelScope.launch {
            val durationHours = (endTime - startTime).toDouble() / (1000 * 60 * 60)
            var calculatedQuality = 1.0f
            if (durationHours < 7.0) {
                calculatedQuality = (durationHours / 7.0).toFloat()
            } else if (durationHours > 9.0) {
                calculatedQuality = (9.0 / durationHours).toFloat()
            }
            if (calculatedQuality < 0f) calculatedQuality = 0f
            if (calculatedQuality > 1.0f) {
                calculatedQuality = 1.0f
            }

            val sleepLog = SleepLog(
                userId = userId,
                startTime = startTime,
                endTime = endTime,
                quality = calculatedQuality
            )
            val result = sleepRepository.addSleepLog(sleepLog)
            _addResult.value = result
        }
    }

    // ========== Statistik / Non-CRUD Features ==========

    /**
     * Rata-rata durasi tidur dalam jam (7 hari terakhir).
     */
    fun getAverageSleepHours(): Double {
        val logs = sleepLogs.value
        if (logs.isEmpty()) return 0.0
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        val recentLogs = logs.filter { it.date >= weekAgo }
        if (recentLogs.isEmpty()) return 0.0
        return recentLogs.map { (it.endTime - it.startTime).toDouble() / (1000 * 60 * 60) }.average()
    }

    /**
     * Rata-rata kualitas tidur (skala 0-5).
     */
    fun getAverageQuality(): Float {
        val logs = sleepLogs.value
        if (logs.isEmpty()) return 0f
        return logs.map { it.quality }.average().toFloat() * 5f
    }

    /**
     * Total tidur minggu ini dalam jam.
     */
    fun getTotalSleepThisWeek(): Double {
        val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        return sleepLogs.value
            .filter { it.date >= weekAgo }
            .sumOf { (it.endTime - it.startTime).toDouble() / (1000 * 60 * 60) }
    }

    /**
     * Streak — berapa hari berturut-turut tidur >= 7 jam.
     */
    fun getSleepStreak(): Int {
        val sortedLogs = sleepLogs.value.sortedByDescending { it.date }
        var streak = 0
        for (log in sortedLogs) {
            val hours = (log.endTime - log.startTime).toDouble() / (1000 * 60 * 60)
            if (hours >= 7) {
                streak++
            } else {
                break
            }
        }
        return streak
    }
}