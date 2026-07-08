package mad.project.mdp_project.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "doctors")
data class DoctorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val doctorName: String,
    val category: String,
    val description: String,
    val rating: Double,
    val availableTime: LocalDateTime,
    val profileIcon: String
)
