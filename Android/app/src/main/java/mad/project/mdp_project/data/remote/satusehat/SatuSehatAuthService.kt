package mad.project.mdp_project.data.remote.satusehat

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit interface for SATUSEHAT OAuth2 authentication.
 *
 * Endpoint: POST /oauth2/v1/accesstoken?grant_type=client_credentials
 * Body: application/x-www-form-urlencoded with client_id and client_secret
 *
 * Documentation: https://satusehat.kemkes.go.id/platform/docs/id/master-data/master-sarana-index/rest-api-msi/
 * Section: "Autentikasi"
 */
interface SatuSehatAuthService {

    @FormUrlEncoded
    @POST("oauth2/v1/accesstoken")
    suspend fun getAccessToken(
        @Query("grant_type") grantType: String = "client_credentials",
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String
    ): SatuSehatTokenResponse
}
