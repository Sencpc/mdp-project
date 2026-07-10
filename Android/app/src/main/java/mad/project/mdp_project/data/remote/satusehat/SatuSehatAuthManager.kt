package mad.project.mdp_project.data.remote.satusehat

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe OAuth2 token manager for SATUSEHAT API.
 *
 * Handles:
 * - Token caching in memory
 * - Expiry detection (with 60-second safety margin)
 * - Auto-refresh when token is expired
 * - Thread-safe access via Mutex
 *
 * This is an infrastructure component. It is used internally by
 * SatuSehatRetrofitClient's AuthInterceptor. ViewModels should
 * never access this directly.
 */
class SatuSehatAuthManager(
    private val authService: SatuSehatAuthService,
    private val clientId: String = SatuSehatConfig.CLIENT_ID,
    private val clientSecret: String = SatuSehatConfig.CLIENT_SECRET
) {
    private val mutex = Mutex()

    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var expiresAt: Long = 0L

    companion object {
        // Refresh token 60 seconds before actual expiry for safety
        private const val EXPIRY_MARGIN_MS = 60_000L
    }

    /**
     * Returns a valid access token, refreshing if necessary.
     *
     * @throws Exception if credentials are empty or auth request fails.
     */
    suspend fun getValidToken(): String {
        // Fast path: return cached token if still valid
        cachedToken?.let { token ->
            if (System.currentTimeMillis() < expiresAt - EXPIRY_MARGIN_MS) {
                return token
            }
        }

        // Slow path: refresh token with mutex to prevent concurrent refreshes
        return mutex.withLock {
            // Double-check after acquiring lock (another coroutine may have refreshed)
            cachedToken?.let { token ->
                if (System.currentTimeMillis() < expiresAt - EXPIRY_MARGIN_MS) {
                    return@withLock token
                }
            }

            require(clientId.isNotBlank()) {
                "SATUSEHAT client_id is not configured. Set SatuSehatConfig.CLIENT_ID."
            }
            require(clientSecret.isNotBlank()) {
                "SATUSEHAT client_secret is not configured. Set SatuSehatConfig.CLIENT_SECRET."
            }

            val response = authService.getAccessToken(
                clientId = clientId,
                clientSecret = clientSecret
            )

            cachedToken = response.accessToken
            expiresAt = System.currentTimeMillis() + (response.expiresIn * 1000L)

            response.accessToken
        }
    }

    /**
     * Invalidates the cached token, forcing a refresh on next call.
     */
    fun invalidateToken() {
        cachedToken = null
        expiresAt = 0L
    }

    /**
     * Returns true if credentials are configured (non-empty).
     */
    fun isConfigured(): Boolean {
        return clientId.isNotBlank() && clientSecret.isNotBlank()
    }
}
