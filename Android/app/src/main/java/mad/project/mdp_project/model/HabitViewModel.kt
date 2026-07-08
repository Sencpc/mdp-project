package mad.project.mdp_project.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.Habit
import mad.project.mdp_project.data.SessionManager
import mad.project.mdp_project.data.remote.RetrofitClient
import mad.project.mdp_project.data.repository.HabitRepository

class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SessionManager(application)
    private val habitRepository: HabitRepository

    val habits: Flow<List<Habit>>

    init {
        val db = AppDatabase.getDatabase(application)
        habitRepository = HabitRepository(db.habitDao(), RetrofitClient.apiService)
        habits = habitRepository.getHabitsForUser(sessionManager.getUserId())

        // Sync dari server
        viewModelScope.launch {
            habitRepository.syncFromServer(sessionManager.getUserId())
        }

        // Evaluate streaks & schedule reminders
        viewModelScope.launch {
            habits.collect { habitList ->
                val now = System.currentTimeMillis()
                habitList.forEach { habit ->
                    // 1. Evaluate Streaks
                    if (now > habit.endTime) {
                        var newStreak = if (habit.isCompleted) habit.streak + 1 else 0
                        var newEndTime = habit.endTime
                        var newStartTime = habit.startTime
                        
                        val daysMissed = ((now - habit.endTime) / (24 * 60 * 60 * 1000L)).toInt()
                        if (daysMissed > 0) {
                            newStreak = 0 // Reset if missed multiple days
                        }

                        while (now > newEndTime) {
                            newEndTime += (24 * 60 * 60 * 1000L)
                            newStartTime += (24 * 60 * 60 * 1000L)
                        }

                        val updatedHabit = habit.copy(
                            streak = newStreak,
                            isCompleted = false,
                            startTime = newStartTime,
                            endTime = newEndTime
                        )
                        habitRepository.updateHabit(updatedHabit)
                    }

                    // 2. Schedule Reminders (System Notification)
                    if (habit.reminderTime != null) {
                        mad.project.mdp_project.service.ReminderScheduler.scheduleReminder(
                            application, habit.id, habit.name, habit.reminderTime!!
                        )
                    } else {
                        mad.project.mdp_project.service.ReminderScheduler.cancelReminder(
                            application, habit.id
                        )
                    }
                }
            }
        }
    }

    private fun getTenPMEndTime(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 22)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun addHabit(name: String, subtitle: String, category: String, reminderTime: Long? = null) {
        val userId = sessionManager.getUserId()
        if (userId != -1) {
            viewModelScope.launch {
                val newHabit = Habit(
                    userId = userId,
                    name = name,
                    subtitle = subtitle,
                    category = category,
                    startTime = System.currentTimeMillis(),
                    endTime = getTenPMEndTime(),
                    reminderTime = reminderTime
                )
                habitRepository.addHabit(newHabit)
            }
        }
    }

    fun updateHabit(id: Int, name: String, subtitle: String, category: String, reminderTime: Long? = null) {
        viewModelScope.launch {
            val existingHabit = habitRepository.getHabitById(id)
            if (existingHabit != null) {
                val updatedHabit = existingHabit.copy(
                    name = name,
                    subtitle = subtitle,
                    category = category,
                    startTime = System.currentTimeMillis(),
                    endTime = getTenPMEndTime(),
                    reminderTime = reminderTime
                )
                habitRepository.updateHabit(updatedHabit)
            }
        }
    }

    fun toggleHabitCompletion(habit: Habit, isCompleted: Boolean) {
        viewModelScope.launch {
            habitRepository.toggleHabitCompletion(habit.id, isCompleted)
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habit)
        }
    }

    fun deleteHabitById(id: Int) {
        viewModelScope.launch {
            val existingHabit = habitRepository.getHabitById(id)
            if (existingHabit != null) {
                habitRepository.deleteHabit(existingHabit)
            }
        }
    }
}
