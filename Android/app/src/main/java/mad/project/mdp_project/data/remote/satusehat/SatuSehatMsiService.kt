package mad.project.mdp_project.data.remote.satusehat

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for SATUSEHAT Master Sarana Index (MSI) REST API.
 *
 * Only endpoints that are actually used in this application are defined here.
 * All endpoints require Bearer token authentication (handled by SatuSehatAuthInterceptor).
 *
 * Documentation: https://satusehat.kemkes.go.id/platform/docs/id/master-data/master-sarana-index/rest-api-msi/
 */
interface SatuSehatMsiService {

    /**
     * Search/list healthcare facilities (Fasyankes).
     *
     * Required params: limit, page, jenis_sarana
     * Optional params: nama, kode_provinsi, kode_kabkota, status_aktif, etc.
     *
     * Facility type codes (jenis_sarana):
     * - 101 = Praktek Mandiri
     * - 102 = PUSKESMAS
     * - 103 = Klinik
     * - 104 = Rumah Sakit
     */
    @GET("masterdata/v1/mastersaranaindex/mastersarana")
    suspend fun getFacilities(
        @Query("limit") limit: Int,
        @Query("page") page: Int,
        @Query("jenis_sarana") jenisSarana: Int,
        @Query("nama") nama: String? = null,
        @Query("kode_provinsi") kodeProvinsi: String? = null,
        @Query("kode_kabkota") kodeKabkota: String? = null,
        @Query("status_aktif") statusAktif: String? = "true"
    ): MsiResponse

    /**
     * List all facility types.
     *
     * Returns the reference list of jenis_sarana codes (101-104 etc.).
     */
    @GET("masterdata/v1/mastersaranaindex/jenissarana")
    suspend fun getFacilityTypes(
        @Query("limit") limit: Int,
        @Query("page") page: Int
    ): MsiJenisSaranaResponse
}
