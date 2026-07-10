package mad.project.mdp_project.data.repository

import kotlinx.coroutines.flow.Flow
import mad.project.mdp_project.data.ConsultationDao
import mad.project.mdp_project.data.ConsultationEntity
import mad.project.mdp_project.data.ReviewDao
import mad.project.mdp_project.data.ReviewEntity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Repository for consultation bookings and reviews.
 *
 * Single responsibility: Manages local consultation lifecycle
 * (booking → upcoming → completed → reviewed).
 *
 * Reviews are co-located here because they are scoped to a consultation —
 * you can only review after a consultation is completed.
 */
class ConsultationRepository(
    private val consultationDao: ConsultationDao,
    private val reviewDao: ReviewDao
) {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    // ========== Consultations ==========

    fun getUpcomingConsultations(): Flow<List<ConsultationEntity>> =
        consultationDao.getUpcomingConsultations()

    fun getCompletedConsultations(): Flow<List<ConsultationEntity>> =
        consultationDao.getCompletedConsultations()

    fun getAllConsultations(): Flow<List<ConsultationEntity>> =
        consultationDao.getAllConsultations()

    suspend fun insertConsultation(consultation: ConsultationEntity): Long {
        return consultationDao.insertConsultation(consultation)
    }

    /**
     * Transitions all past "Upcoming" consultations to "Completed".
     * Called periodically by ConsultationStatusWorker and on history screen open.
     */
    suspend fun markPastConsultationsCompleted() {
        val now = LocalDateTime.now().format(formatter)
        consultationDao.markPastConsultationsCompleted(now)
    }

    // ========== Reviews ==========

    fun getReviewsForDoctor(doctorId: Int): Flow<List<ReviewEntity>> =
        reviewDao.getReviewsForDoctor(doctorId)

    fun getAverageRating(doctorId: Int): Flow<Float?> =
        reviewDao.getAverageRating(doctorId)

    suspend fun submitReview(review: ReviewEntity) {
        reviewDao.insertReview(review)
    }

    /**
     * Returns true if the given consultation already has a review.
     * Prevents duplicate reviews per consultation.
     */
    suspend fun hasReview(consultationId: Int): Boolean =
        reviewDao.hasReview(consultationId) > 0
}
