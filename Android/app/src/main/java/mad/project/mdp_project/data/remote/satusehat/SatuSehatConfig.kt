package mad.project.mdp_project.data.remote.satusehat

/**
 * SATUSEHAT MSI API constants.
 *
 * OAuth2 credentials have been moved to the backend (WebServices).
 * Android no longer communicates directly with SATUSEHAT OAuth2 endpoints.
 *
 * Only facility type codes are retained for local reference.
 */
object SatuSehatConfig {
    // MSI facility type codes (from documentation)
    const val JENIS_SARANA_PRAKTEK_MANDIRI = 101
    const val JENIS_SARANA_PUSKESMAS = 102
    const val JENIS_SARANA_KLINIK = 103
    const val JENIS_SARANA_RUMAH_SAKIT = 104
}

