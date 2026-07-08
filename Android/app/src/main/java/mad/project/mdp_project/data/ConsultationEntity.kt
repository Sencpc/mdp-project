package mad.project.mdp_project.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "consultations")
data class ConsultationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val doctorId: Int,
    val doctorName: String,
    val category: String,
    val consultationTime: LocalDateTime,
    val status: String = "Upcoming"
)
