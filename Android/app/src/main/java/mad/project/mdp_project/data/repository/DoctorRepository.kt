package mad.project.mdp_project.data.repository

import kotlinx.coroutines.flow.Flow
import mad.project.mdp_project.data.DoctorDao
import mad.project.mdp_project.data.DoctorEntity
import mad.project.mdp_project.data.remote.RetrofitClient
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Repository for doctor data.
 *
 * Single responsibility: Manages local doctor data from Room Database.
 * Consultation-related operations have been moved to ConsultationRepository.
 */
class DoctorRepository(
    private val doctorDao: DoctorDao
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

    /**
     * Sync doctors from backend and save to local database.
     */
    suspend fun syncDoctors() {
        try {
            val response = RetrofitClient.apiService.getDoctors()
            if (response.isSuccessful) {
                val doctorList = response.body()?.data ?: emptyList()
                val entities = doctorList.map { dto ->
                    // Convert availableTime (Unix ms) to LocalDateTime
                    val dateTime = Instant.ofEpochMilli(dto.availableTime)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime()

                    DoctorEntity(
                        id = dto.id,
                        doctorName = dto.displayName ?: dto.name,
                        category = dto.category,
                        description = dto.description ?: "Experienced specialist.",
                        rating = dto.rating,
                        availableTime = dateTime,
                        profileIcon = dto.profileIcon,
                        satusehatId = dto.satusehatId
                    )
                }
                
                if (entities.isNotEmpty()) {
                    // Update the local DB
                    doctorDao.insertAll(entities)
                }
            }
        } catch (_: Exception) {
            // Network failure: silently ignore, rely on local cached doctors
        }
    }
}

