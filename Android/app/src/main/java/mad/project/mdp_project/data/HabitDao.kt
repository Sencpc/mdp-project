package mad.project.mdp_project.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE userId = :userId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getHabitsForUser(userId: Int): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    suspend fun getHabitById(habitId: Int): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)
    
    @Query("UPDATE habits SET isCompleted = :isCompleted WHERE id = :habitId")
    suspend fun updateHabitCompletion(habitId: Int, isCompleted: Boolean)

    @Query("SELECT * FROM habits WHERE userId = :userId AND deletedAt IS NULL AND reminderTime IS NOT NULL ORDER BY reminderTime ASC")
    fun getHabitsWithReminder(userId: Int): Flow<List<Habit>>
}
