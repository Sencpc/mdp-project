package mad.project.mdp_project.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import mad.project.mdp_project.data.SleepLog
import mad.project.mdp_project.data.SleepLogDao
import mad.project.mdp_project.data.remote.ApiService
import mad.project.mdp_project.data.remote.SleepLogRequest

class SleepRepository(
    private val sleepLogDao: SleepLogDao,
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "SleepRepository"
    }

    /**
     * Get sleep logs dari Room (real-time via Flow).
     */
    fun getSleepLogsForUser(userId: Int): Flow<List<SleepLog>> {
        return sleepLogDao.getSleepLogsForUser(userId)
    }

    /**
     * Add sleep log: simpan ke Room, lalu sync ke server.
     */
    suspend fun addSleepLog(sleepLog: SleepLog): Result<SleepLog> {
        return try {
            // 1. Simpan ke Room
            sleepLogDao.insertSleepLog(sleepLog)

            // 2. Sync ke server
            try {
                apiService.createSleepLog(SleepLogRequest(
                    userId = sleepLog.userId,
                    startTime = sleepLog.startTime,
                    endTime = sleepLog.endTime,
                    quality = sleepLog.quality
                ))
                Log.d(TAG, "Sleep log synced ke server")
            } catch (e: Exception) {
                Log.w(TAG, "Gagal sync sleep log ke server: ${e.message}")
            }

            Result.success(sleepLog)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sync dari server ke lokal.
     */
    suspend fun syncFromServer(userId: Int) {
        try {
            val response = apiService.getSleepLogsForUser(userId)
            if (response.isSuccessful && response.body() != null) {
                val serverLogs = response.body()!!
                serverLogs.forEach { apiLog ->
                    val localLog = SleepLog(
                        id = apiLog.id,
                        userId = apiLog.userId,
                        startTime = apiLog.startTime,
                        endTime = apiLog.endTime,
                        quality = apiLog.quality,
                        date = apiLog.date ?: System.currentTimeMillis()
                    )
                    sleepLogDao.insertSleepLog(localLog)
                }
                Log.d(TAG, "Synced ${serverLogs.size} sleep logs dari server")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal sync sleep logs dari server: ${e.message}")
        }
    }
}
