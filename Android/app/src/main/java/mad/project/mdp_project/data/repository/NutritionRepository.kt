package mad.project.mdp_project.data.repository

import android.util.Log
import mad.project.mdp_project.data.NutritionLog
import mad.project.mdp_project.data.NutritionLogDao
import mad.project.mdp_project.data.remote.ApiService
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
     * Send compressed image bytes to the backend AI scanner.
     * On success, save the result to Room and return it.
     */
    suspend fun scanImage(userId: Int, imageBytes: ByteArray): Result<NutritionLog> {
        return try {
            val imagePart = MultipartBody.Part.createFormData(
                "image",
                "food_scan.jpg",
                imageBytes.toRequestBody("image/jpeg".toMediaType())
            )
            val userIdPart = userId.toString().toRequestBody("text/plain".toMediaType())

            val response = apiService.scanFood(imagePart, userIdPart)

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
                Log.d(TAG, "Scan result saved: ${log.foodName} = ${log.calories} kcal")
                Result.success(log)
            } else {
                val errorMsg = "AI scan failed: ${response.code()}"
                Log.e(TAG, errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Scan network error: ${e.message}")
            Result.failure(e)
        }
    }
}
