package mad.project.mdp_project.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response models for the backend's GET /api/facilities endpoint.
 *
 * The backend handles SATUSEHAT OAuth2 authentication and MSI API calls,
 * then returns structured facility data to Android.
 *
 * Android never communicates directly with SATUSEHAT OAuth2 endpoints.
 */

@JsonClass(generateAdapter = true)
data class FacilityApiResponse(
    val success: Boolean,
    val data: List<FacilityApiData> = emptyList(),
    val message: String? = null
)

@JsonClass(generateAdapter = true)
data class FacilityApiData(
    @Json(name = "kode_satusehat") val kodeSatusehat: String,
    @Json(name = "kode_sarana") val kodeSarana: String,
    val nama: String,
    val alamat: String? = null,
    val telp: String? = null,
    val email: String? = null,
    val longitude: String? = null,
    val latitude: String? = null,
    val operasional: Boolean = false,
    @Json(name = "jenis_sarana") val jenisSarana: FacilityJenisSarana? = null,
    val provinsi: FacilityProvinsi? = null,
    val kabkota: FacilityKabkota? = null,
    @Json(name = "status_aktif") val statusAktif: Boolean = false
) {
    fun toEntity(): mad.project.mdp_project.data.FacilityEntity = mad.project.mdp_project.data.FacilityEntity(
        kodeSatusehat = kodeSatusehat,
        kodeSarana = kodeSarana,
        nama = nama,
        alamat = alamat ?: "",
        telp = telp ?: "",
        email = email ?: "",
        jenisSaranaKode = jenisSarana?.kode ?: "",
        jenisSaranaNama = jenisSarana?.nama ?: "",
        provinsiNama = provinsi?.nama ?: "",
        kabkotaNama = kabkota?.nama ?: "",
        latitude = latitude ?: "",
        longitude = longitude ?: "",
        operasional = operasional,
        statusAktif = statusAktif,
        lastSyncedAt = System.currentTimeMillis()
    )
}

@JsonClass(generateAdapter = true)
data class FacilityJenisSarana(
    val kode: String? = null,
    val nama: String? = null
)

@JsonClass(generateAdapter = true)
data class FacilityProvinsi(
    val kode: Int? = null,
    val nama: String? = null
)

@JsonClass(generateAdapter = true)
data class FacilityKabkota(
    val kode: Int? = null,
    val nama: String? = null
)
