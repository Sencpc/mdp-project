package mad.project.mdp_project.data.repository

import kotlinx.coroutines.flow.Flow
import mad.project.mdp_project.data.ConsultationDao
import mad.project.mdp_project.data.ConsultationEntity
import mad.project.mdp_project.data.DoctorDao
import mad.project.mdp_project.data.DoctorEntity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DoctorRepository(
    private val doctorDao: DoctorDao,
    private val consultationDao: ConsultationDao
) {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /**
     * Get available doctors filtered by category and search query.
     * Time filter: availableTime > now + 1 hour (computed here, filtered in SQL).
     */
    fun getAvailableDoctors(category: String?, query: String?): Flow<List<DoctorEntity>> {
        val minTime = LocalDateTime.now().plusHours(1).format(formatter)

        return when {
            // Has search query + category filter
            !query.isNullOrBlank() && !category.isNullOrBlank() && category != "All" -> {
                doctorDao.searchDoctorsByCategory(query, category, minTime)
            }
            // Has search query only
            !query.isNullOrBlank() -> {
                doctorDao.searchDoctors(query, minTime)
            }
            // Has category filter only
            !category.isNullOrBlank() && category != "All" -> {
                doctorDao.getAvailableDoctorsByCategory(category, minTime)
            }
            // No filters
            else -> {
                doctorDao.getAvailableDoctors(minTime)
            }
        }
    }

    suspend fun getDoctorById(doctorId: Int): DoctorEntity? {
        return doctorDao.getDoctorById(doctorId)
    }

    fun getUpcomingConsultations(): Flow<List<ConsultationEntity>> {
        return consultationDao.getUpcomingConsultations()
    }

    suspend fun insertConsultation(consultation: ConsultationEntity) {
        consultationDao.insertConsultation(consultation)
    }
}
