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
import mad.project.mdp_project.data.ReviewEntity
import mad.project.mdp_project.data.repository.ConsultationRepository

/**
 * ViewModel for the Consultation History screen.
 *
 * Manages:
 * - Listing completed and all consultations
 * - Auto-completing past consultations
 * - Submitting reviews for completed consultations
 */
class ConsultationHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ConsultationRepository(db.consultationDao(), db.reviewDao())

    val completedConsultations: StateFlow<List<ConsultationEntity>> =
        repository.getCompletedConsultations()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allConsultations: StateFlow<List<ConsultationEntity>> =
        repository.getAllConsultations()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Auto-complete past consultations when history screen opens
        viewModelScope.launch {
            repository.markPastConsultationsCompleted()
        }
    }

    /**
     * Submits a review for a completed consultation.
     */
    fun submitReview(consultationId: Int, doctorId: Int, rating: Float, comment: String) {
        viewModelScope.launch {
            val review = ReviewEntity(
                consultationId = consultationId,
                doctorId = doctorId,
                rating = rating,
                comment = comment
            )
            repository.submitReview(review)
        }
    }

    /**
     * Checks if a consultation already has a review.
     */
    suspend fun hasReview(consultationId: Int): Boolean =
        repository.hasReview(consultationId)
}
