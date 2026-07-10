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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.NutritionLog
import mad.project.mdp_project.data.SessionManager
import mad.project.mdp_project.data.remote.RetrofitClient
import mad.project.mdp_project.data.repository.NutritionRepository
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionLogViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val sessionManager = SessionManager(application)
    private val userId = sessionManager.getUserId()

    private val repository = NutritionRepository(db.nutritionLogDao(), RetrofitClient.apiService)

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
