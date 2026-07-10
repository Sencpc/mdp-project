package mad.project.mdp_project.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String,
    val password: String,
    val fullName: String = "",
    val height: Float? = null,
    val weight: Float? = null,
    val birthDate: Long? = null, // Store as timestamp
    val bloodType: String? = null,
    val conditions: String = "", // Comma-separated or JSON string
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val profilePicturePath: String? = null,
    val chatSummary: String? = null
)
