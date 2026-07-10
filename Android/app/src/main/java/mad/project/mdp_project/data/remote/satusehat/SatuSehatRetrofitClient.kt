package mad.project.mdp_project.data.remote.satusehat

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit client for SATUSEHAT MSI API.
 *
 * Separate from the existing RetrofitClient because:
 * - Different base URL (SATUSEHAT vs app backend)
 * - Different auth mechanism (OAuth2 Bearer token vs none)
 * - Independent lifecycle
 *
 * Uses an OkHttp Interceptor to automatically inject the Bearer token
 * into every MSI API request.
 */
object SatuSehatRetrofitClient {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    // Auth service uses a plain client (no auth interceptor — it IS the auth endpoint)
    private val authRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(SatuSehatConfig.BASE_URL)
        .client(
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
        )
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val authService: SatuSehatAuthService =
        authRetrofit.create(SatuSehatAuthService::class.java)

    val authManager: SatuSehatAuthManager = SatuSehatAuthManager(authService)

    // MSI service uses a client with auth interceptor
    private val msiOkHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(SatuSehatAuthInterceptor(authManager))
        .build()

    private val msiRetrofit: Retrofit = Retrofit.Builder()
        .baseUrl(SatuSehatConfig.BASE_URL)
        .client(msiOkHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val msiService: SatuSehatMsiService =
        msiRetrofit.create(SatuSehatMsiService::class.java)
}

/**
 * OkHttp interceptor that automatically injects the SATUSEHAT Bearer token.
 *
 * Skips token injection for the auth endpoint itself to avoid circular dependency.
 */
private class SatuSehatAuthInterceptor(
    private val authManager: SatuSehatAuthManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth header for the token endpoint itself
        if (originalRequest.url.encodedPath.contains("oauth2")) {
            return chain.proceed(originalRequest)
        }

        // Skip if credentials are not configured (graceful degradation)
        if (!authManager.isConfigured()) {
            return chain.proceed(originalRequest)
        }

        val token = runBlocking { authManager.getValidToken() }

        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}
