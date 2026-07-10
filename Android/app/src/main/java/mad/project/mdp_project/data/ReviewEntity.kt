package mad.project.mdp_project.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for user-submitted consultation reviews.
 *
 * Source of truth: Room Database (local only).
 * SATUSEHAT has no rating/review system.
 *
 * Relationships:
 * - consultationId → consultations.id (one review per consultation)
 * - doctorId → doctors.id (for aggregating reviews per doctor)
 */
@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val consultationId: Int,    // FK → consultations.id
    val doctorId: Int,          // FK → doctors.id
    val rating: Float,          // 1.0 to 5.0
    val comment: String = "",   // Free-text review
    val createdAt: Long = System.currentTimeMillis()
)
