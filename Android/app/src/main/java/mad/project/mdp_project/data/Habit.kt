package mad.project.mdp_project.data

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Parcelize
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    var name: String,
    var category: String = "Focus",
    var subtitle: String = "",
    var isCompleted: Boolean = false,
    var streak: Int = 0,
    var startTime: Long,
    var endTime: Long,
    val createdAt: Long = System.currentTimeMillis(),
    var deletedAt: Long? = null,
    var reminders: List<Long> = emptyList(),
    var useRingtone: Boolean = true,
    var useVibration: Boolean = true
) : Parcelable {

    fun getDurationMillis(): Long = endTime - startTime

    fun getFormattedStartTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(startTime))
    }

    fun getFormattedEndTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(endTime))
    }

    fun getDurationString(): String {
        val duration = getDurationMillis()
        val hours = duration / (1000 * 60 * 60)
        val minutes = (duration % (1000 * 60 * 60)) / (1000 * 60)
        return String.format(Locale.getDefault(), "%dh %dm", hours, minutes)
    }
}
