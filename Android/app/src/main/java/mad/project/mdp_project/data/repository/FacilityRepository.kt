package mad.project.mdp_project.data.repository

import kotlinx.coroutines.flow.Flow
import mad.project.mdp_project.data.DoctorDao
import mad.project.mdp_project.data.FacilityDao
import mad.project.mdp_project.data.FacilityEntity
import mad.project.mdp_project.data.remote.ApiService

/**
 * Repository for healthcare facility data.
 *
 * Single responsibility: Manages facility data with offline-first caching.
 * Fetches data from the backend GET /api/facilities endpoint (which internally
 * handles SATUSEHAT OAuth2 and MSI API).
 *
 * Strategy: Stale-while-revalidate
 * - Always read from Room (instant, offline-capable)
 * - Sync from API in background when cache is older than CACHE_DURATION_MS
 * - On network failure: silently use stale cached data
 */
class FacilityRepository(
    private val facilityDao: FacilityDao,
    private val doctorDao: DoctorDao,
    private val apiService: ApiService
) {
    companion object {
        private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    /**
     * Returns active, operational facilities (hospitals + clinics) from Room cache.
     */
    fun getActiveFacilities(): Flow<List<FacilityEntity>> =
        facilityDao.getActiveFacilities()

    /**
     * Returns facilities for a specific doctor based on their mapped IDs.
     */
    fun getFacilitiesByIds(ids: List<String>): Flow<List<FacilityEntity>> =
        facilityDao.getFacilitiesByIds(ids)

    /**
     * Syncs facility data from Backend API into Room cache.
     *
     * Only syncs if the cache is older than CACHE_DURATION_MS.
     *
     * @param forceRefresh If true, ignores cache age and always syncs.
     */
    suspend fun syncFacilities(forceRefresh: Boolean = false) {
        if (!forceRefresh) {
            val lastSync = facilityDao.getLastSyncTime() ?: 0
            if (System.currentTimeMillis() - lastSync < CACHE_DURATION_MS) return
        }

        try {
            val response = apiService.getFacilities()
            if (response.isSuccessful) {
                val facilities = response.body()?.data ?: emptyList()
                val entities = facilities.map { it.toEntity() }
                if (entities.isNotEmpty()) {
                    facilityDao.insertAll(entities)
                    // Automatically assign to doctors once synced
                    assignFacilitiesToDoctors()
                }
            }
        } catch (_: Exception) {
            // Network failure: silently use stale cached data.
            // This is intentional — offline-first means we never crash on network errors.
        }
    }

    /**
     * Called after facility sync completes.
     * Assigns each doctor to 2–3 facilities from the synced facility cache.
     *
     * Distribution strategy (deterministic, based on doctorId):
     * - Uses modular arithmetic to evenly distribute doctors across facilities.
     * - Each doctor gets a primary facility + 1–2 secondary facilities.
     * - This ensures every facility has at least one doctor, and no doctor has zero facilities.
     */
    private suspend fun assignFacilitiesToDoctors() {
        val allFacilities = facilityDao.getActiveFacilitiesOnce()
        if (allFacilities.isEmpty()) return

        val allDoctors = doctorDao.getAllDoctorsOnce()
        for (doctor in allDoctors) {
            if (doctor.supportedFacilityIds.isNotEmpty()) continue // Already mapped

            val facilityCount = allFacilities.size
            if (facilityCount == 0) continue

            val primaryIdx = (doctor.id - 1) % facilityCount
            val secondaryIdx = (doctor.id) % facilityCount
            val tertiaryIdx = (doctor.id + 1) % facilityCount

            val assignedIds = listOf(primaryIdx, secondaryIdx, tertiaryIdx)
                .distinct()
                .take(if (doctor.id % 3 == 0) 3 else 2) // Some doctors get 2, some get 3
                .map { allFacilities[it].kodeSatusehat }

            doctorDao.updateSupportedFacilities(doctor.id, assignedIds)
        }
    }
}

