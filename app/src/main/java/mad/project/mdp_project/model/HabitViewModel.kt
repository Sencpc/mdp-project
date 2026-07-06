package mad.project.mdp_project.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.Habit
import mad.project.mdp_project.data.SessionManager

class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val habitDao = AppDatabase.getDatabase(application).habitDao()
    private val sessionManager = SessionManager(application)
    
    val habits: Flow<List<Habit>> = habitDao.getHabitsForUser(sessionManager.getUserId())

    fun addHabit(name: String, subtitle: String, category: String) {
        val userId = sessionManager.getUserId()
        if (userId != -1) {
            viewModelScope.launch {
                val newHabit = Habit(
                    userId = userId,
                    name = name,
                    subtitle = subtitle,
                    category = category,
                    startTime = System.currentTimeMillis(),
                    endTime = System.currentTimeMillis() + 3600000 // Default 1 hour
                )
                habitDao.insertHabit(newHabit)
            }
        }
    }

    fun toggleHabitCompletion(habit: Habit, isCompleted: Boolean) {
        viewModelScope.launch {
            habitDao.updateHabitCompletion(habit.id, isCompleted)
        }
    }
}
