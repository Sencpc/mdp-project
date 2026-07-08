package mad.project.mdp_project.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.ConsultationEntity
import mad.project.mdp_project.data.repository.DoctorRepository
import java.time.LocalDateTime

class ConsultationViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = DoctorRepository(db.doctorDao(), db.consultationDao())

    private val _consultationSaved = MutableStateFlow(false)
    val consultationSaved = _consultationSaved.asStateFlow()

    fun confirmConsultation(
        doctorId: Int,
        doctorName: String,
        category: String,
        consultationTime: LocalDateTime
    ) {
        viewModelScope.launch {
            val consultation = ConsultationEntity(
                doctorId = doctorId,
                doctorName = doctorName,
                category = category,
                consultationTime = consultationTime,
                status = "Upcoming"
            )
            repository.insertConsultation(consultation)
            _consultationSaved.value = true
        }
    }

    fun resetSavedState() {
        _consultationSaved.value = false
    }
}
