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
    val ORGANIZATION_ID = mad.project.mdp_project.BuildConfig.SATUSEHAT_ORGANIZATION_ID

    // Staging credentials
    val CLIENT_ID = mad.project.mdp_project.BuildConfig.SATUSEHAT_CLIENT_ID
    val CLIENT_SECRET = mad.project.mdp_project.BuildConfig.SATUSEHAT_CLIENT_SECRET

    // MSI facility type codes (from documentation)
    const val JENIS_SARANA_PRAKTEK_MANDIRI = 101
    const val JENIS_SARANA_PUSKESMAS = 102
    const val JENIS_SARANA_KLINIK = 103
    const val JENIS_SARANA_RUMAH_SAKIT = 104
}
