package mad.project.mdp_project.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import mad.project.mdp_project.data.NutritionLog
import mad.project.mdp_project.data.NutritionLogDao
import mad.project.mdp_project.data.remote.AnalyzeResponse
import mad.project.mdp_project.data.remote.ApiService
import mad.project.mdp_project.data.remote.NutritionRequest
import mad.project.mdp_project.data.remote.UpdateMealTypeRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class NutritionRepository(
    private val nutritionLogDao: NutritionLogDao,
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "NutritionRepository"
    }

    /**
     * Step A: Analyze only — send image to Gemini, get food_name + calories + meal_type back.
     * Does NOT save to any database. Returns the AI prediction for user review.
     */
    suspend fun analyzeImage(imageBytes: ByteArray): Result<AnalyzeResponse> {
        return try {
            val imagePart = MultipartBody.Part.createFormData(
                "image",
                "food_scan.jpg",
                imageBytes.toRequestBody("image/jpeg".toMediaType())
            )

            val response = apiService.analyzeFood(imagePart)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Log.d(TAG, "AI analysis: ${body.food_name} = ${body.calories} kcal (${body.meal_type})")
                Result.success(body)
            } else {
                val errorMsg = "AI analysis failed: ${response.code()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Analyze network error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Step B: Confirm & Log — called only when user presses "Log Meal".
     * Saves to the backend MySQL database via POST /api/nutrition,
     * then caches the result in local Room.
     */
    suspend fun logMeal(userId: Int, foodName: String, calories: Int, mealType: String? = null): Result<NutritionLog> {
        return try {
            val response = apiService.createNutritionLog(
                NutritionRequest(
                    userId = userId,
                    food_name = foodName,
                    calories = calories,
                    meal_type = mealType
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val log = NutritionLog(
                    id = body.id,
                    userId = body.userId,
                    foodName = body.food_name,
                    calories = body.calories,
                    mealType = body.meal_type ?: "additional",
                    consumedAt = body.consumed_at ?: System.currentTimeMillis()
                )
                nutritionLogDao.insert(log)
                Log.d(TAG, "Meal logged: ${log.foodName} = ${log.calories} kcal (${log.mealType})")
                Result.success(log)
            } else {
                val errorMsg = "Failed to log meal: ${response.code()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Log meal network error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Update the meal type of a nutrition log (reclassification).
     * Updates on the backend first, then syncs local Room.
     */
    suspend fun updateMealType(logId: Int, newMealType: String): Result<Unit> {
        return try {
            val response = apiService.updateNutritionLog(
                logId,
                UpdateMealTypeRequest(meal_type = newMealType)
            )

            if (response.isSuccessful) {
                nutritionLogDao.updateMealType(logId, newMealType)
                Log.d(TAG, "Meal type updated: log $logId -> $newMealType")
                Result.success(Unit)
            } else {
                val errorMsg = "Failed to update meal type: ${response.code()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update meal type error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Sync all nutrition logs from the server to local Room cache.
     */
    suspend fun syncFromServer(userId: Int) {
        try {
            val response = apiService.getNutritionLogsForUser(userId)
            if (response.isSuccessful && response.body() != null) {
                val logs = response.body()!!.map { body ->
                    NutritionLog(
                        id = body.id,
                        userId = body.userId,
                        foodName = body.food_name,
                        calories = body.calories,
                        mealType = body.meal_type ?: "additional",
                        consumedAt = body.consumed_at ?: System.currentTimeMillis()
                    )
                }
                nutritionLogDao.insertAll(logs)
                Log.d(TAG, "Synced ${logs.size} nutrition logs from server")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync nutrition logs error: ${e.message}")
        }
    }

    fun getLogsForDate(userId: Int, startOfDay: Long, endOfDay: Long): Flow<List<NutritionLog>> {
        return nutritionLogDao.getLogsForUserByDate(userId, startOfDay, endOfDay)
    }

    fun getWeeklyLogs(userId: Int, startOfWeek: Long): Flow<List<NutritionLog>> {
        return nutritionLogDao.getWeeklyLogs(userId, startOfWeek)
    }
}
