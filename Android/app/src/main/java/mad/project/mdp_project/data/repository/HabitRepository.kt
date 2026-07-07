package mad.project.mdp_project.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import mad.project.mdp_project.data.Habit
import mad.project.mdp_project.data.HabitDao
import mad.project.mdp_project.data.remote.ApiService
import mad.project.mdp_project.data.remote.HabitRequest

class HabitRepository(
    private val habitDao: HabitDao,
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "HabitRepository"
    }

    /**
     * Get habits dari Room (real-time via Flow).
     */
    fun getHabitsForUser(userId: Int): Flow<List<Habit>> {
        return habitDao.getHabitsForUser(userId)
    }

    suspend fun getHabitById(habitId: Int): Habit? {
        return habitDao.getHabitById(habitId)
    }

    fun getHabitsWithReminder(userId: Int): Flow<List<Habit>> {
        return habitDao.getHabitsWithReminder(userId)
    }

    /**
     * Add habit: simpan ke Room, lalu sync ke server.
     */
    suspend fun addHabit(habit: Habit): Result<Habit> {
        return try {
            // 1. Simpan ke Room
            val localId = habitDao.insertHabit(habit).toInt()

            // 2. Sync ke server
            try {
                val response = apiService.createHabit(HabitRequest(
                    userId = habit.userId,
                    name = habit.name,
                    category = habit.category,
                    subtitle = habit.subtitle,
                    startTime = habit.startTime,
                    endTime = habit.endTime
                ))
                
                if (response.isSuccessful && response.body() != null) {
                    val serverHabit = response.body()!!
                    if (localId != serverHabit.id) {
                        // Replace the temporary local ID with the server's ID
                        val habitWithServerId = habit.copy(id = serverHabit.id)
                        habitDao.insertHabit(habitWithServerId)
                        habitDao.deleteHabit(habit.copy(id = localId))
                        Log.d(TAG, "Replaced local habit ID $localId with server ID ${serverHabit.id}")
                    }
                }
                Log.d(TAG, "Habit synced ke server")
            } catch (e: Exception) {
                Log.w(TAG, "Gagal sync habit ke server: ${e.message}")
            }

            Result.success(habit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update habit completion status.
     */
    suspend fun toggleHabitCompletion(habitId: Int, isCompleted: Boolean) {
        try {
            habitDao.updateHabitCompletion(habitId, isCompleted)
            try {
                apiService.updateHabit(habitId, HabitRequest(
                    userId = 0, name = "", startTime = 0, endTime = 0,
                    isCompleted = isCompleted
                ))
            } catch (e: Exception) {
                Log.w(TAG, "Gagal sync completion ke server: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error toggle completion: ${e.message}")
        }
    }

    /**
     * Update habit.
     */
    suspend fun updateHabit(habit: Habit) {
        try {
            habitDao.updateHabit(habit)
            try {
                apiService.updateHabit(habit.id, HabitRequest(
                    userId = habit.userId,
                    name = habit.name,
                    category = habit.category,
                    subtitle = habit.subtitle,
                    isCompleted = habit.isCompleted,
                    streak = habit.streak,
                    startTime = habit.startTime,
                    endTime = habit.endTime
                ))
            } catch (e: Exception) {
                Log.w(TAG, "Gagal sync update habit ke server: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error update habit: ${e.message}")
        }
    }

    /**
     * Delete habit (soft delete).
     */
    suspend fun deleteHabit(habit: Habit) {
        try {
            habitDao.deleteHabit(habit)
            try {
                apiService.deleteHabit(habit.id)
            } catch (e: Exception) {
                Log.w(TAG, "Gagal sync delete ke server: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error delete habit: ${e.message}")
        }
    }

    /**
     * Sync dari server ke lokal — untuk refresh data.
     */
    suspend fun syncFromServer(userId: Int) {
        try {
            val response = apiService.getHabitsForUser(userId)
            if (response.isSuccessful && response.body() != null) {
                val serverHabits = response.body()!!
                serverHabits.forEach { apiHabit ->
                    val localHabit = Habit(
                        id = apiHabit.id,
                        userId = apiHabit.userId,
                        name = apiHabit.name,
                        category = apiHabit.category,
                        subtitle = apiHabit.subtitle,
                        isCompleted = apiHabit.isCompleted,
                        streak = apiHabit.streak,
                        startTime = apiHabit.startTime,
                        endTime = apiHabit.endTime,
                        createdAt = apiHabit.createdAt ?: System.currentTimeMillis(),
                        deletedAt = apiHabit.deletedAt
                    )
                    habitDao.insertHabit(localHabit)
                }
                Log.d(TAG, "Synced ${serverHabits.size} habits dari server")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal sync habits dari server: ${e.message}")
        }
    }
}
