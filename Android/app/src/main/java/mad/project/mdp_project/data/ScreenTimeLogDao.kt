package mad.project.mdp_project.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreenTimeLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(log: ScreenTimeLog)

    @Query("SELECT * FROM screen_time_logs WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getLogForDate(userId: Int, date: String): ScreenTimeLog?

    @Query("SELECT * FROM screen_time_logs WHERE userId = :userId AND date = :date LIMIT 1")
    fun getLogForDateFlow(userId: Int, date: String): Flow<ScreenTimeLog?>

    @Query("SELECT * FROM screen_time_logs WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getLogsForRange(userId: Int, startDate: String, endDate: String): List<ScreenTimeLog>

    @Query("SELECT * FROM screen_time_logs WHERE userId = :userId ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentLogs(userId: Int, limit: Int = 7): List<ScreenTimeLog>
}
