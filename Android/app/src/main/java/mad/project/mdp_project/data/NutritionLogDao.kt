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

    @Query("SELECT * FROM nutrition_logs WHERE userId = :userId AND consumedAt >= :startOfDay AND consumedAt < :endOfDay ORDER BY consumedAt ASC")
    fun getLogsForUserByDate(userId: Int, startOfDay: Long, endOfDay: Long): Flow<List<NutritionLog>>

    @Query("SELECT * FROM nutrition_logs WHERE userId = :userId AND consumedAt >= :startOfWeek ORDER BY consumedAt ASC")
    fun getWeeklyLogs(userId: Int, startOfWeek: Long): Flow<List<NutritionLog>>

    @Query("UPDATE nutrition_logs SET mealType = :mealType WHERE id = :id")
    suspend fun updateMealType(id: Int, mealType: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: NutritionLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<NutritionLog>)
}
