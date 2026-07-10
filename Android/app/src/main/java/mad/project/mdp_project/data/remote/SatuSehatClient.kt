package mad.project.mdp_project.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Direct SATUSEHAT API client for Android.
 *
 * Handles OAuth2 token acquisition and facility fetching
 * directly from the device, bypassing the backend proxy
 * (which gets blocked by SATUSEHAT's WAF on Railway's datacenter IPs).
 */
object SatuSehatClient {

    private const val BASE_URL = "https://api-satusehat-stg.dto.kemkes.go.id"
    private const val CLIENT_ID = "FQXAVyiIequtAQS8O2NNFumJiOAb1SoYrDJFgezYzFCHwr0q"
    private const val CLIENT_SECRET = "bHKectYOJQ5PH0rbGCUhsu69xQJx1EHVUN1aGvSvF49vOtxiAp8DGdRBtA5JuVj0"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    /**
     * Fetches an OAuth2 access token from SATUSEHAT.
     */
    private fun getAccessToken(): String? {
        val body = FormBody.Builder()
            .add("client_id", CLIENT_ID)
            .add("client_secret", CLIENT_SECRET)
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/oauth2/v1/accesstoken?grant_type=client_credentials")
            .post(body)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null

        val json = response.body?.string() ?: return null
        val adapter = moshi.adapter(SatuSehatTokenResponse::class.java)
        return adapter.fromJson(json)?.accessToken
    }

    /**
     * Fetches Surabaya hospitals (jenis_sarana=104) directly from SATUSEHAT MSI API.
     * Returns parsed facility data or empty list on failure.
     */
    fun fetchSurabayaFacilities(): List<FacilityApiData> {
        val token = getAccessToken() ?: return emptyList()

        val url = "$BASE_URL/masterdata/v1/mastersaranaindex/mastersarana" +
                "?limit=50&page=1&jenis_sarana=104&status_aktif=true" +
                "&kode_provinsi=35&kode_kabkota=3578&kode_kecamatan=357804"

        val request = Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer $token")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return emptyList()

        val json = response.body?.string() ?: return emptyList()
        val adapter = moshi.adapter(SatuSehatFacilityResponse::class.java)
        val parsed = adapter.fromJson(json) ?: return emptyList()

        if (parsed.statusCode != 200) return emptyList()

        return parsed.data.map { raw ->
            FacilityApiData(
                kodeSatusehat = raw.kodeSatusehat,
                kodeSarana = raw.kodeSarana,
                nama = raw.nama,
                alamat = raw.alamat,
                telp = raw.telp,
                email = raw.email,
                longitude = raw.longitude,
                latitude = raw.latitude,
                operasional = raw.operasional,
                jenisSarana = raw.jenisSarana,
                provinsi = raw.provinsi,
                kabkota = raw.kabkota,
                statusAktif = raw.statusAktif
            )
        }
    }
}

// ─── SATUSEHAT response models ───

@JsonClass(generateAdapter = true)
data class SatuSehatTokenResponse(
    @Json(name = "access_token") val accessToken: String? = null
)

@JsonClass(generateAdapter = true)
data class SatuSehatFacilityResponse(
    @Json(name = "status_code") val statusCode: Int = 0,
    val data: List<SatuSehatFacilityData> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SatuSehatFacilityData(
    @Json(name = "kode_satusehat") val kodeSatusehat: String,
    @Json(name = "kode_sarana") val kodeSarana: String,
    val nama: String,
    val alamat: String? = null,
    val telp: String? = null,
    val email: String? = null,
    val longitude: Double? = null,
    val latitude: Double? = null,
    val operasional: Boolean = false,
    @Json(name = "jenis_sarana") val jenisSarana: FacilityJenisSarana? = null,
    val provinsi: FacilityProvinsi? = null,
    val kabkota: FacilityKabkota? = null,
    @Json(name = "status_aktif") val statusAktif: Boolean = false
)
