package mad.project.mdp_project.data.remote.satusehat

/**
 * SATUSEHAT MSI API configuration constants.
 *
 * Base URLs and credential placeholders for the SATUSEHAT Master Sarana Index API.
 * Documentation: https://satusehat.kemkes.go.id/platform/docs/id/master-data/master-sarana-index/rest-api-msi/
 *
 * In production, credentials should be injected via BuildConfig fields.
 */
object SatuSehatConfig {
    // Staging environment base URL (from official SATUSEHAT documentation)
    const val BASE_URL = "https://api-satusehat-stg.dto.kemkes.go.id/"

    // OAuth2 token endpoint path
    const val AUTH_PATH = "oauth2/v1/accesstoken"

    // Organization ID for reference
    const val ORGANIZATION_ID = "f17fcfc5-9d03-4821-af6e-b02bd844b683"

    // Staging credentials
    const val CLIENT_ID = "CXaAyZAAaGAx8szZib7PGmV0BJVqvfKhcFZCBPQcp83KjOw3"
    const val CLIENT_SECRET = "cZKYNGrXMj4bwBfsQXKjXjlYUE8UOOMfGiJkOhnRpGT9DytkxQlF4hWHQfyyqZJn"

    // MSI facility type codes (from documentation)
    const val JENIS_SARANA_PRAKTEK_MANDIRI = 101
    const val JENIS_SARANA_PUSKESMAS = 102
    const val JENIS_SARANA_KLINIK = 103
    const val JENIS_SARANA_RUMAH_SAKIT = 104
}
