package mad.project.mdp_project.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Room entity for consultation bookings.
 *
 * Source of truth: Room Database (local only).
 *
 * Relationships:
 * - userId → from SessionManager (logged-in user)
 * - doctorId → doctors.id
 * - facilityKodeSatusehat → facilities.kodeSatusehat (stable MSI 10-digit code)
 * - facilityName is a denormalized copy of facilities.nama for display (avoids JOIN)
 */
@Entity(tableName = "consultations")
data class ConsultationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int = -1,                 // From SessionManager.getUserId()
    val doctorId: Int,
    val doctorName: String,
    val category: String,
    val consultationTime: LocalDateTime,
    val status: String = "Upcoming",
    val facilityKodeSatusehat: String = "",  // Stable FK → facilities.kodeSatusehat
    val facilityName: String = "",            // Denormalized display name (read optimization)
    val profileIcon: String = ""              // Doctor's icon for history card rendering
)
