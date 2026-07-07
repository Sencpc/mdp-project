package mad.project.mdp_project.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val message: String,
    val isFromBot: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
