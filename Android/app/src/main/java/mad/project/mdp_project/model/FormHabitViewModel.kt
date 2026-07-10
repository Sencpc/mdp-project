package mad.project.mdp_project.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.Habit
import mad.project.mdp_project.data.SessionManager
import mad.project.mdp_project.data.remote.RetrofitClient
import mad.project.mdp_project.data.repository.HabitRepository

class FormHabitViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: HabitRepository
    private val sessionManager = SessionManager(application)

    val habitName = MutableLiveData<String>("")
    val habitSubtitle = MutableLiveData<String>("")
    val habitCategory = MutableLiveData<String>("Mental")
    val reminders = MutableLiveData<List<Long>>(emptyList())
    val enableNotification = MutableLiveData<Boolean>(true)
    val useRingtone = MutableLiveData<Boolean>(true)
    val useVibration = MutableLiveData<Boolean>(true)
    
    val isLoading = MutableLiveData<Boolean>(false)
    val isEditMode = MutableLiveData<Boolean>(false)
    private var currentHabitId: Int = -1

    init {
        val db = AppDatabase.getDatabase(application)
        repository = HabitRepository(db.habitDao(), RetrofitClient.apiService)
    }

    fun loadHabit(habitId: Int) {
        if (habitId == -1) {
            isEditMode.value = false
            return
        }
        currentHabitId = habitId
        isEditMode.value = true
        viewModelScope.launch {
            val habit = repository.getHabitById(habitId)
            habit?.let {
                habitName.value = it.name
                habitSubtitle.value = it.subtitle
                habitCategory.value = it.category
                reminders.value = it.reminders
                enableNotification.value = it.enableNotification
                useRingtone.value = it.useRingtone
                useVibration.value = it.useVibration
            }
        }
    }

    fun saveHabit(onSuccess: () -> Unit) {
        val name = habitName.value ?: ""
        if (name.isBlank() || isLoading.value == true) return

        isLoading.value = true
        viewModelScope.launch {
            if (isEditMode.value == true) {
                val existing = repository.getHabitById(currentHabitId)
                existing?.let {
                    val updated = it.copy(
                        name = name,
                        subtitle = habitSubtitle.value ?: "",
                        category = habitCategory.value ?: "Mental",
                        reminders = reminders.value ?: emptyList(),
                        enableNotification = enableNotification.value ?: true,
                        useRingtone = useRingtone.value ?: true,
                        useVibration = useVibration.value ?: true
                    )
                    repository.updateHabit(updated)
                }
            } else {
                val newHabit = Habit(
                    userId = sessionManager.getUserId(),
                    name = name,
                    subtitle = habitSubtitle.value ?: "",
                    category = habitCategory.value ?: "Mental",
                    startTime = System.currentTimeMillis(),
                    endTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000L), // Default 24h
                    reminders = reminders.value ?: emptyList(),
                    enableNotification = enableNotification.value ?: true,
                    useRingtone = useRingtone.value ?: true,
                    useVibration = useVibration.value ?: true
                )
                repository.addHabit(newHabit)
            }
            isLoading.value = false
            onSuccess()
        }
    }
}
