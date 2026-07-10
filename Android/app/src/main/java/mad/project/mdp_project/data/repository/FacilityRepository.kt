package mad.project.mdp_project.data.repository

import mad.project.mdp_project.data.FacilityDao
import mad.project.mdp_project.data.FacilityEntity
import mad.project.mdp_project.data.remote.satusehat.SatuSehatMsiService
import mad.project.mdp_project.data.remote.satusehat.SatuSehatConfig
import kotlinx.coroutines.flow.Flow

/**
 * Repository for healthcare facility data from SATUSEHAT MSI API.
 *
 * Single responsibility: Manages MSI facility data with offline-first caching.
 * This is the ONLY repository that interacts with SATUSEHAT APIs.
 *
 * Strategy: Stale-while-revalidate
 * - Always read from Room (instant, offline-capable)
 * - Sync from MSI API in background when cache is older than CACHE_DURATION_MS
 * - On network failure: silently use stale cached data
 */
class FacilityRepository(
    private val facilityDao: FacilityDao,
    private val msiService: SatuSehatMsiService
) {
    companion object {
        private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val DEFAULT_PAGE_SIZE = 50
    }

    /**
     * Returns active, operational facilities (hospitals + clinics) from Room cache.
     */
    fun getActiveFacilities(): Flow<List<FacilityEntity>> =
        facilityDao.getActiveFacilities()

    /**
     * Searches facilities by name in Room cache.
     */
    fun searchFacilities(query: String): Flow<List<FacilityEntity>> =
        facilityDao.searchFacilities(query)

    /**
     * Syncs facility data from MSI API into Room cache.
     *
     * Fetches both hospitals (jenis_sarana=104) and clinics (jenis_sarana=103).
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
            // Fetch hospitals
            syncFacilityType(SatuSehatConfig.JENIS_SARANA_RUMAH_SAKIT)
            // Fetch clinics
            syncFacilityType(SatuSehatConfig.JENIS_SARANA_KLINIK)
        } catch (_: Exception) {
            // Network failure: silently use stale cached data.
            // This is intentional — offline-first means we never crash on network errors.
        }
    }

    private suspend fun syncFacilityType(jenisSarana: Int) {
        val response = msiService.getFacilities(
            limit = DEFAULT_PAGE_SIZE,
            page = 1,
            jenisSarana = jenisSarana
        )

        val entities = response.data.map { sarana ->
            FacilityEntity(
                kodeSatusehat = sarana.kodeSatusehat,
                kodeSarana = sarana.kodeSarana,
                nama = sarana.nama,
                alamat = sarana.alamat ?: "",
                telp = sarana.telp ?: "",
                email = sarana.email ?: "",
                jenisSaranaKode = sarana.jenisSarana?.kode ?: "",
                jenisSaranaNama = sarana.jenisSarana?.nama ?: "",
                provinsiNama = sarana.provinsi?.nama ?: "",
                kabkotaNama = sarana.kabkota?.nama ?: "",
                latitude = sarana.latitude ?: "",
                longitude = sarana.longitude ?: "",
                operasional = sarana.operasional,
                statusAktif = sarana.statusAktif,
                lastSyncedAt = System.currentTimeMillis()
            )
        }

        if (entities.isNotEmpty()) {
            facilityDao.insertAll(entities)
        }
    }
}
