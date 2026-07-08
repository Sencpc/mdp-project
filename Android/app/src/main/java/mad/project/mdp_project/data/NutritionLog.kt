package mad.project.mdp_project.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "nutrition_logs")
data class NutritionLog(
    @PrimaryKey val id: Int = 0,
    val userId: Int,
    val foodName: String,
    val calories: Int,
    val consumedAt: Long = System.currentTimeMillis()
)
