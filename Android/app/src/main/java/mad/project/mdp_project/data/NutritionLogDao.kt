package mad.project.mdp_project.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NutritionLogDao {
    @Query("SELECT * FROM nutrition_logs WHERE userId = :userId ORDER BY consumedAt DESC")
    fun getLogsForUser(userId: Int): Flow<List<NutritionLog>>

    @Query("SELECT COALESCE(SUM(calories), 0) FROM nutrition_logs WHERE userId = :userId AND consumedAt >= :startOfDay")
    fun getTodayCalories(userId: Int, startOfDay: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: NutritionLog)
}
