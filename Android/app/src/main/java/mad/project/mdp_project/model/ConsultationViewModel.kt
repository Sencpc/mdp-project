package mad.project.mdp_project.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.ConsultationEntity
import mad.project.mdp_project.data.FacilityEntity
import mad.project.mdp_project.data.remote.satusehat.SatuSehatRetrofitClient
import mad.project.mdp_project.data.repository.ConsultationRepository
import mad.project.mdp_project.data.repository.FacilityRepository
import mad.project.mdp_project.worker.ConsultationReminderWorker
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class ConsultationViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val consultationRepository = ConsultationRepository(db.consultationDao(), db.reviewDao())
    private val facilityRepository = FacilityRepository(db.facilityDao(), SatuSehatRetrofitClient.msiService)

    private val _consultationSaved = MutableStateFlow(false)
    val consultationSaved = _consultationSaved.asStateFlow()

    /**
     * List of active facilities from MSI API (cached in Room).
     * Used by ScheduleConsultationScreen for the facility selector.
     */
    val facilities: StateFlow<List<FacilityEntity>> = facilityRepository.getActiveFacilities()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _facilitySearchQuery = MutableStateFlow("")
    val facilitySearchQuery: StateFlow<String> = _facilitySearchQuery.asStateFlow()

    init {
        // Sync facility data from MSI API on ViewModel creation
        viewModelScope.launch {
            facilityRepository.syncFacilities()
        }
    }

    fun setFacilitySearchQuery(query: String) {
        _facilitySearchQuery.value = query
    }

    fun confirmConsultation(
        doctorId: Int,
        doctorName: String,
        category: String,
        consultationTime: LocalDateTime,
        facilityKodeSatusehat: String = "",
        facilityName: String = "",
        profileIcon: String = ""
    ) {
        viewModelScope.launch {
            val consultation = ConsultationEntity(
                doctorId = doctorId,
                doctorName = doctorName,
                category = category,
                consultationTime = consultationTime,
                status = "Upcoming",
                facilityKodeSatusehat = facilityKodeSatusehat,
                facilityName = facilityName,
                profileIcon = profileIcon
            )
            val consultationId = consultationRepository.insertConsultation(consultation)
            _consultationSaved.value = true

            // Schedule notification worker 1 hour before
            val now = LocalDateTime.now()
            val reminderTime = consultationTime.minusHours(1)
            if (reminderTime.isAfter(now)) {
                val delayMs = Duration.between(now, reminderTime).toMillis()

                val data = Data.Builder()
                    .putString("doctorName", doctorName)
                    .putString("facilityName", facilityName)
                    .putString("consultationTime", consultationTime.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy • h:mm a")))
                    .putInt("consultationId", consultationId.toInt())
                    .build()

                val workRequest = OneTimeWorkRequestBuilder<ConsultationReminderWorker>()
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .setInputData(data)
                    .build()

                WorkManager.getInstance(getApplication()).enqueue(workRequest)
            }
        }
    }

    fun resetSavedState() {
        _consultationSaved.value = false
    }
}

