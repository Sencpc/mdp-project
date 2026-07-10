package mad.project.mdp_project.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.ConsultationEntity
import mad.project.mdp_project.data.FacilityEntity
import mad.project.mdp_project.data.SessionManager
import mad.project.mdp_project.data.remote.RetrofitClient
import mad.project.mdp_project.data.repository.BookingValidationResult
import mad.project.mdp_project.data.repository.ConsultationRepository
import mad.project.mdp_project.data.repository.FacilityRepository
import mad.project.mdp_project.data.repository.validateBooking
import mad.project.mdp_project.worker.ConsultationReminderWorker
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

sealed class BookingUiState {
    data object Idle : BookingUiState()
    data object Saving : BookingUiState()
    data class Error(val message: String) : BookingUiState()
    data object Success : BookingUiState()
}

enum class TimeSlotState {
    AVAILABLE,
    SELECTED,
    BREAK,
    TOO_SOON,
    BOOKED
}

data class TimeSlot(
    val hour: Int,
    val minute: Int,
    val state: TimeSlotState
)

class ConsultationViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val consultationDao = db.consultationDao()
    private val consultationRepository = ConsultationRepository(consultationDao, db.reviewDao())
    private val facilityRepository = FacilityRepository(db.facilityDao(), db.doctorDao(), RetrofitClient.apiService)
    private val sessionManager = SessionManager(application)

    private val _bookingState = MutableStateFlow<BookingUiState>(BookingUiState.Idle)
    val bookingState: StateFlow<BookingUiState> = _bookingState.asStateFlow()

    private val _facilities = MutableStateFlow<List<FacilityEntity>>(emptyList())
    val facilities: StateFlow<List<FacilityEntity>> = _facilities.asStateFlow()

    fun loadFacilitiesForDoctor(doctorId: Int) {
        viewModelScope.launch {
            // Sync facilities from backend → Room (skips if cache is fresh)
            facilityRepository.syncFacilities()

            val doctor = db.doctorDao().getDoctorById(doctorId)
            if (doctor != null && doctor.supportedFacilityIds.isNotEmpty()) {
                facilityRepository.getFacilitiesByIds(doctor.supportedFacilityIds).collect {
                    _facilities.value = it
                }
            } else {
                // Fallback: if no facilities mapped yet, show all
                facilityRepository.getActiveFacilities().collect {
                    _facilities.value = it
                }
            }
        }
    }

    suspend fun generateTimeSlots(selectedDate: LocalDate, doctorId: Int): List<TimeSlot> {
        val now = LocalDateTime.now()
        val slots = mutableListOf<TimeSlot>()
        val userId = sessionManager.getUserId()
        
        for (hour in 8 until 20) {
            for (minute in listOf(0, 30)) {
                val slotTime = LocalDateTime.of(selectedDate, LocalTime.of(hour, minute))
                val formattedTime = slotTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                
                // Determine state
                val state = when {
                    hour == 12 -> TimeSlotState.BREAK
                    slotTime.isBefore(now.plusHours(2)) -> TimeSlotState.TOO_SOON
                    else -> {
                        val userConflicts = consultationDao.countUserConflicts(userId, formattedTime)
                        val doctorConflicts = consultationDao.countDoctorConflicts(doctorId, formattedTime)
                        if (userConflicts > 0 || doctorConflicts > 0) {
                            TimeSlotState.BOOKED
                        } else {
                            TimeSlotState.AVAILABLE
                        }
                    }
                }
                slots.add(TimeSlot(hour, minute, state))
            }
        }
        return slots
    }

    fun validateAndBook(
        doctorId: Int,
        doctorName: String,
        category: String,
        consultationTime: LocalDateTime,
        facilityKodeSatusehat: String = "",
        facilityName: String = "",
        profileIcon: String = ""
    ) {
        viewModelScope.launch {
            _bookingState.value = BookingUiState.Saving
            val userId = sessionManager.getUserId()
            
            if (userId == -1) {
                _bookingState.value = BookingUiState.Error("User not logged in")
                return@launch
            }

            val result = validateBooking(userId, doctorId, consultationTime, consultationDao)
            
            if (result is BookingValidationResult.Invalid) {
                _bookingState.value = BookingUiState.Error(result.message)
                return@launch
            }

            // Valid! Proceed to save
            val consultation = ConsultationEntity(
                userId = userId,
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
            _bookingState.value = BookingUiState.Success

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

    fun resetState() {
        _bookingState.value = BookingUiState.Idle
    }
}

