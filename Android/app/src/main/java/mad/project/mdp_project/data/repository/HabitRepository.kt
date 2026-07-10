package mad.project.mdp_project.data.repository

import android.util.Log
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import mad.project.mdp_project.data.Habit
import mad.project.mdp_project.data.HabitDao
import mad.project.mdp_project.data.HabitSeeder
import mad.project.mdp_project.data.remote.ApiService
import mad.project.mdp_project.data.remote.HabitRequest

class HabitRepository(
    private val habitDao: HabitDao,
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "HabitRepository"
    }

    suspend fun seedStandardHabits(userId: Int) {
        val standardHabits = HabitSeeder.getStandardHabits(userId)
        
        // Special cleanup: Delete duplicates or Indonesian versions
        val allHabits = habitDao.getHabitsForUserOnce(userId)
        allHabits.forEach { h ->
            if ((h.name == "Drink Water" || h.name == "Minum Air") && 
                h.subtitle == "Minum 8 gelas air setiap hari untuk tetap terhidrasi") {
                deleteHabit(h)
                Log.d(TAG, "Deleted Indonesian Drink Water habit for user $userId")
            }
        }

        standardHabits.forEach { habit ->
            val existing = habitDao.getHabitByName(userId, habit.name)

            if (existing == null) {
                addHabit(habit)
                Log.d(TAG, "Seeded standard habit: ${habit.name} for user $userId")
            } else {
                // Update reminders if it's a standard habit but has different reminders
                if (existing.reminders != habit.reminders) {
                    val updated = existing.copy(reminders = habit.reminders)
                    updateHabit(updated)
                    Log.d(TAG, "Updated reminders for standard habit: ${habit.name}")
                }
            }
        }
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
                    endTime = habit.endTime,
                    reminders = habit.reminders
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
                    endTime = habit.endTime,
                    reminders = habit.reminders
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
            // 1. Delete lokal (Room)
            habitDao.deleteHabit(habit)
            Log.d(TAG, "Habit deleted locally: ${habit.id}")

            // 2. Sync ke server (Gunakan NonCancellable agar tidak terputus saat navigasi)
            withContext(NonCancellable) {
                try {
                    val response = apiService.deleteHabit(habit.id)
                    if (response.isSuccessful) {
                        Log.d(TAG, "Habit deleted on server: ${habit.id}")
                    } else {
                        Log.e(TAG, "Gagal delete di server: Code=${response.code()}, Msg=${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Gagal sync delete ke server (Network error): ${e.message}")
                }
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
                    // Ambil habit lokal yang ada untuk menjaga data reminderTime
                    val existingLocal = habitDao.getHabitById(apiHabit.id)
                    
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
                        deletedAt = apiHabit.deletedAt,
                        reminders = apiHabit.reminders ?: existingLocal?.reminders ?: emptyList()
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
