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

/** Result of booking validation */
sealed class BookingValidationResult {
    data object Valid : BookingValidationResult()
    data class Invalid(val message: String) : BookingValidationResult()
}

suspend fun validateBooking(
    userId: Int,
    doctorId: Int,
    consultationTime: LocalDateTime,
    consultationDao: ConsultationDao
): BookingValidationResult {
    val now = LocalDateTime.now()
    
    // Rule 1: Must be at least 2 hours ahead
    if (consultationTime.isBefore(now.plusHours(2))) {
        return BookingValidationResult.Invalid("Booking must be at least 2 hours in advance")
    }
    
    // Rule 2: Max 30 days ahead
    if (consultationTime.toLocalDate().isAfter(now.toLocalDate().plusDays(30))) {
        return BookingValidationResult.Invalid("Cannot book more than 30 days in advance")
    }
    
    // Rule 3: No Sundays
    if (consultationTime.dayOfWeek == java.time.DayOfWeek.SUNDAY) {
        return BookingValidationResult.Invalid("Consultations are not available on Sundays")
    }
    
    // Rule 4: Working hours 08:00–20:00
    val hour = consultationTime.hour
    if (hour < 8 || hour >= 20) {
        return BookingValidationResult.Invalid("Consultations available 08:00–20:00 only")
    }
    
    // Rule 5: Break time 12:00–12:59
    if (hour == 12) {
        return BookingValidationResult.Invalid("12:00–13:00 is break time")
    }
    
    // Rule 6: 30-minute intervals
    if (consultationTime.minute != 0 && consultationTime.minute != 30) {
        return BookingValidationResult.Invalid("Booking must be in 30-minute intervals")
    }
    
    val formattedTime = consultationTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    // Rule 7: User time conflict
    val userConflicts = consultationDao.countUserConflicts(userId, formattedTime)
    if (userConflicts > 0) {
        return BookingValidationResult.Invalid("You already have a consultation at this time")
    }
    
    // Rule 8: Doctor time conflict
    val doctorConflicts = consultationDao.countDoctorConflicts(doctorId, formattedTime)
    if (doctorConflicts > 0) {
        return BookingValidationResult.Invalid("This doctor is already booked at this time")
    }
    
    return BookingValidationResult.Valid
}
