package mad.project.mdp_project.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(tableName = "sleep_logs")
data class SleepLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val startTime: Long,
    val endTime: Long,
    val quality: Float,
    val date: Long = System.currentTimeMillis()
) {
    fun getDurationMillis(): Long = endTime - startTime

    fun getTotalSleepHours(): Double {
        return getDurationMillis().toDouble() / (1000 * 60 * 60.0)
    }

    fun getFormattedDuration(): String {
        val totalMinutes = getDurationMillis() / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return String.format(Locale.getDefault(), "%dh %dm", hours, minutes)
    }
}
