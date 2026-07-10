package mad.project.mdp_project.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for SATUSEHAT MSI healthcare facility data.
 *
 * Source of truth: SATUSEHAT MSI API (cached locally for offline access).
 * Every field maps to a documented MSI response field from:
 * https://satusehat.kemkes.go.id/platform/docs/id/master-data/master-sarana-index/rest-api-msi/
 *
 * Primary key: kodeSatusehat — a stable, government-assigned 10-digit unique code.
 */
@Entity(tableName = "facilities")
data class FacilityEntity(
    @PrimaryKey
    val kodeSatusehat: String,          // MSI: kode_satusehat (10-digit, stable identifier)
    val kodeSarana: String,              // MSI: kode_sarana
    val nama: String,                    // MSI: nama (facility name)
    val alamat: String = "",             // MSI: alamat (address)
    val telp: String = "",               // MSI: telp (phone)
    val email: String = "",              // MSI: email
    val jenisSaranaKode: String = "",     // MSI: jenis_sarana.kode (e.g., "103", "104")
    val jenisSaranaNama: String = "",     // MSI: jenis_sarana.nama (e.g., "Klinik", "Rumah Sakit")
    val provinsiNama: String = "",       // MSI: provinsi.nama
    val kabkotaNama: String = "",        // MSI: kabkota.nama
    val latitude: String = "",           // MSI: latitude
    val longitude: String = "",          // MSI: longitude
    val operasional: Boolean = false,    // MSI: operasional
    val statusAktif: Boolean = false,    // MSI: status_aktif
    val lastSyncedAt: Long = System.currentTimeMillis()
)
