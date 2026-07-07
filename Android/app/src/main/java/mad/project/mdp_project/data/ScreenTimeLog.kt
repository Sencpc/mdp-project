package mad.project.mdp_project.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "screen_time_logs")
data class ScreenTimeLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val date: String, // yyyy-MM-dd format
    val totalScreenTimeMs: Long = 0L,
    val socialMediaMs: Long = 0L,
    val productivityMs: Long = 0L,
    val entertainmentMs: Long = 0L,
    val topAppsJson: String = "[]" // JSON array of top apps
) {
    fun getFormattedTotal(): String = formatMs(totalScreenTimeMs)
    fun getFormattedSocial(): String = formatMs(socialMediaMs)
    fun getFormattedProductivity(): String = formatMs(productivityMs)
    fun getFormattedEntertainment(): String = formatMs(entertainmentMs)

    private fun formatMs(ms: Long): String {
        val totalMinutes = ms / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
