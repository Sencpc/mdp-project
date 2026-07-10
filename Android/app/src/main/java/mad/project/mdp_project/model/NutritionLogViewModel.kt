package mad.project.mdp_project.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.NutritionLog
import mad.project.mdp_project.data.SessionManager
import mad.project.mdp_project.data.remote.RetrofitClient
import mad.project.mdp_project.data.repository.NutritionRepository
import mad.project.mdp_project.data.repository.UserRepository
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionLogViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val sessionManager = SessionManager(application)
    private val userId = sessionManager.getUserId()

    private val repository = NutritionRepository(db.nutritionLogDao(), RetrofitClient.apiService)
    private val userRepository = UserRepository(db.userDao(), RetrofitClient.apiService)

    // Calculate recommended calories dynamically from user height, weight, and age
    val recommendedCalories: StateFlow<Int> = userRepository.getUserById(userId)
        .map { user ->
            if (user?.height != null && user.weight != null) {
                val heightCm = user.height!!.toDouble()
                val weightKg = user.weight!!.toDouble()
                val age = if (user.birthDate != null) {
                    val ageDifMs = System.currentTimeMillis() - user.birthDate!!
                    val ageDate = java.util.Date(ageDifMs)
                    val cal = java.util.Calendar.getInstance()
                    cal.time = ageDate
                    Math.abs(cal.get(java.util.Calendar.YEAR) - 1970)
                } else 25

                val male = 10 * weightKg + 6.25 * heightCm - 5 * age + 5
                val female = 10 * weightKg + 6.25 * heightCm - 5 * age - 161
                ((male + female) / 2).toInt()
            } else {
                2000
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 2000)

    // Currently selected date (start of day in millis)
    private val _selectedDate = MutableStateFlow(getStartOfToday())
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    // Logs for the selected date, auto-updates when date changes
    val logsForDate: StateFlow<List<NutritionLog>> = _selectedDate
        .flatMapLatest { startOfDay ->
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000L
            repository.getLogsForDate(userId, startOfDay, endOfDay)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _updateError = MutableStateFlow<String?>(null)
    val updateError: StateFlow<String?> = _updateError.asStateFlow()

    init {
        // Sync nutrition data from server
        viewModelScope.launch {
            repository.syncFromServer(userId)
        }
    }

    /**
     * Navigate to the next or previous day.
     * @param offset +1 for next day, -1 for previous day
     */
    fun navigateDate(offset: Int) {
        _selectedDate.value = _selectedDate.value + offset * 24 * 60 * 60 * 1000L
    }

    /**
     * Update the meal type of a nutrition log entry.
     * Auto-saves to backend + local Room.
     */
    fun updateMealType(logId: Int, newMealType: String) {
        viewModelScope.launch {
            _updateError.value = null
            val result = repository.updateMealType(logId, newMealType)
            result.onFailure { e ->
                _updateError.value = e.message
            }
        }
    }

    fun clearError() {
        _updateError.value = null
    }

    /**
     * Check if the selected date is today.
     */
    fun isToday(): Boolean {
        return _selectedDate.value == getStartOfToday()
    }

    private fun getStartOfToday(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
