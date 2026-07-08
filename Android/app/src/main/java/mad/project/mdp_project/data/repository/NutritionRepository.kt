package mad.project.mdp_project.data.repository

import android.util.Log
import mad.project.mdp_project.data.NutritionLog
import mad.project.mdp_project.data.NutritionLogDao
import mad.project.mdp_project.data.remote.AnalyzeResponse
import mad.project.mdp_project.data.remote.ApiService
import mad.project.mdp_project.data.remote.NutritionRequest
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
     * Step A: Analyze only — send image to Gemini, get food_name + calories back.
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
                Log.d(TAG, "AI analysis: ${body.food_name} = ${body.calories} kcal")
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
    suspend fun logMeal(userId: Int, foodName: String, calories: Int): Result<NutritionLog> {
        return try {
            val response = apiService.createNutritionLog(
                NutritionRequest(
                    userId = userId,
                    food_name = foodName,
                    calories = calories
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val log = NutritionLog(
                    id = body.id,
                    userId = body.userId,
                    foodName = body.food_name,
                    calories = body.calories,
                    consumedAt = body.consumed_at ?: System.currentTimeMillis()
                )
                nutritionLogDao.insert(log)
                Log.d(TAG, "Meal logged: ${log.foodName} = ${log.calories} kcal")
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
}
