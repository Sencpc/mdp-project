package mad.project.mdp_project.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.ConsultationEntity
import mad.project.mdp_project.data.Habit
import mad.project.mdp_project.data.SessionManager
import mad.project.mdp_project.data.SleepLog
import mad.project.mdp_project.data.User
import mad.project.mdp_project.data.remote.RetrofitClient
import mad.project.mdp_project.data.repository.DoctorRepository
import mad.project.mdp_project.data.repository.HabitRepository
import mad.project.mdp_project.data.repository.SleepRepository
import mad.project.mdp_project.data.repository.UserRepository

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val sessionManager = SessionManager(application)
    private val userId = sessionManager.getUserId()

    private val db = AppDatabase.getDatabase(application)
    private val api = RetrofitClient.apiService

    private val userRepository = UserRepository(db.userDao(), api)
    private val habitRepository = HabitRepository(db.habitDao(), api)
    private val sleepRepository = SleepRepository(db.sleepLogDao(), api)
    private val doctorRepository = DoctorRepository(db.doctorDao(), db.consultationDao())

    val user: StateFlow<User?> = userRepository.getUserById(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val habits: StateFlow<List<Habit>> = habitRepository.getHabitsForUser(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val habitsWithReminder: StateFlow<List<Habit>> = habitRepository.getHabitsWithReminder(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val sleepLogs: StateFlow<List<SleepLog>> = sleepRepository.getSleepLogsForUser(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val upcomingConsultations: StateFlow<List<ConsultationEntity>> = doctorRepository.getUpcomingConsultations()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Sync data dari server saat dashboard dibuka
        viewModelScope.launch {
            habitRepository.syncFromServer(userId)
            sleepRepository.syncFromServer(userId)
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
