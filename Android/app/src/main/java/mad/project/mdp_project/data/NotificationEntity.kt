package mad.project.mdp_project.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val type: String, // "HABIT", "SCREEN_TIME", "AI_INSIGHT"
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false
)
