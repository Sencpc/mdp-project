package mad.project.mdp_project.data.remote.satusehat

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * MSI API response models.
 *
 * Every field in these classes is mapped directly from the official SATUSEHAT MSI documentation:
 * https://satusehat.kemkes.go.id/platform/docs/id/master-data/master-sarana-index/rest-api-msi/
 *
 * No fields have been invented or assumed.
 */

// ========== OAuth2 Token ==========

@JsonClass(generateAdapter = true)
data class SatuSehatTokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "expires_in") val expiresIn: Int
)

// ========== MSI Master Sarana Response ==========

@JsonClass(generateAdapter = true)
data class MsiResponse(
    @Json(name = "status_code") val statusCode: Int,
    val message: String,
    val page: Int,
    @Json(name = "total_page") val totalPage: Int,
    val data: List<MsiSaranaData>
)

@JsonClass(generateAdapter = true)
data class MsiSaranaData(
    @Json(name = "kode_satusehat") val kodeSatusehat: String,
    @Json(name = "kode_sarana") val kodeSarana: String,
    val nama: String,
    val telp: String? = null,
    val email: String? = null,
    val website: String? = null,
    val longitude: String? = null,
    val latitude: String? = null,
    val operasional: Boolean = false,
    @Json(name = "wilayah_perairan_darat") val wilayahPerairanDarat: String? = null,
    @Json(name = "wilayah_karakteristik") val wilayahKarakteristik: String? = null,
    @Json(name = "sarana_administrasi") val saranaAdministrasi: MsiSaranaAdministrasi? = null,
    val alamat: String? = null,
    val provinsi: MsiProvinsi? = null,
    val kabkota: MsiKabKota? = null,
    @Json(name = "jenis_sarana") val jenisSarana: MsiJenisSarana? = null,
    val subjenis: MsiSubJenis? = null,
    @Json(name = "kelas_sarana") val kelasSarana: MsiKelasSarana? = null,
    @Json(name = "status_sarana") val statusSarana: String? = null,
    @Json(name = "status_aktif") val statusAktif: Boolean = false
)

@JsonClass(generateAdapter = true)
data class MsiSaranaAdministrasi(
    val kode: String? = null,
    val nama: String? = null,
    @Json(name = "kode_sarana") val kodeSarana: String? = null,
    @Json(name = "status_aktif") val statusAktif: Boolean? = null,
    @Json(name = "status_sarana") val statusSarana: String? = null
)

@JsonClass(generateAdapter = true)
data class MsiProvinsi(
    val kode: Int? = null,
    val nama: String? = null,
    @Json(name = "kode_bps") val kodeBps: String? = null,
    @Json(name = "kode_lama") val kodeLama: String? = null
)

@JsonClass(generateAdapter = true)
data class MsiKabKota(
    val kode: Int? = null,
    val nama: String? = null,
    @Json(name = "kode_bps") val kodeBps: String? = null,
    @Json(name = "kode_lama") val kodeLama: String? = null
)

@JsonClass(generateAdapter = true)
data class MsiJenisSarana(
    val kode: String? = null,
    val nama: String? = null,
    @Json(name = "nama_alt") val namaAlt: String? = null
)

@JsonClass(generateAdapter = true)
data class MsiSubJenis(
    val kode: String? = null,
    val nama: String? = null,
    @Json(name = "nama_alt") val namaAlt: String? = null
)

@JsonClass(generateAdapter = true)
data class MsiKelasSarana(
    val kode: String? = null,
    val nama: String? = null
)

// ========== MSI Jenis Sarana Response ==========

@JsonClass(generateAdapter = true)
data class MsiJenisSaranaResponse(
    @Json(name = "status_code") val statusCode: Int,
    val message: String,
    val page: Int,
    @Json(name = "total_page") val totalPage: Int,
    val data: List<MsiJenisSarana>
)
