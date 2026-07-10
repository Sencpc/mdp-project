package mad.project.mdp_project.data.remote

import com.squareup.moshi.JsonClass

// ========== AUTH ==========
@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val username: String,
    val password: String,
    val fullName: String
)

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val username: String,
    val password: String
)

// ========== USER ==========
@JsonClass(generateAdapter = true)
data class UserResponse(
    val id: Int,
    val username: String,
    val fullName: String? = null,
    val password: String? = null,
    val height: Float? = null,
    val weight: Float? = null,
    val birthDate: Long? = null,
    val bloodType: String? = null,
    val conditions: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateUserRequest(
    val fullName: String? = null,
    val height: Float? = null,
    val weight: Float? = null,
    val birthDate: Long? = null,
    val bloodType: String? = null,
    val conditions: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null
)

// ========== HABITS ==========
@JsonClass(generateAdapter = true)
data class HabitRequest(
    val userId: Int,
    val name: String,
    val category: String = "Focus",
    val subtitle: String = "",
    val isCompleted: Boolean = false,
    val streak: Int = 0,
    val startTime: Long,
    val endTime: Long,
    val reminders: List<Long> = emptyList()
)

@JsonClass(generateAdapter = true)
data class HabitResponse(
    val id: Int,
    val userId: Int,
    val name: String,
    val category: String = "Focus",
    val subtitle: String = "",
    val isCompleted: Boolean = false,
    val streak: Int = 0,
    val startTime: Long,
    val endTime: Long,
    val reminders: List<Long>? = emptyList(),
    val createdAt: Long? = null,
    val deletedAt: Long? = null
)

// ========== SLEEP ==========
@JsonClass(generateAdapter = true)
data class SleepLogRequest(
    val userId: Int,
    val startTime: Long,
    val endTime: Long,
    val quality: Float
)

@JsonClass(generateAdapter = true)
data class SleepLogResponse(
    val id: Int,
    val userId: Int,
    val startTime: Long,
    val endTime: Long,
    val quality: Float,
    val date: Long? = null
)

// ========== NUTRITION ==========
@JsonClass(generateAdapter = true)
data class NutritionRequest(
    val userId: Int,
    val food_name: String,
    val calories: Int,
    val image_url: String? = null,
    val meal_type: String? = null
)

@JsonClass(generateAdapter = true)
data class NutritionResponse(
    val id: Int,
    val userId: Int,
    val food_name: String,
    val calories: Int,
    val image_url: String? = null,
    val consumed_at: Long? = null,
    val meal_type: String? = null
)

// Response from /api/nutrition/analyze — AI prediction only, not saved to DB yet
@JsonClass(generateAdapter = true)
data class AnalyzeResponse(
    val food_name: String,
    val calories: Int,
    val meal_type: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateMealTypeRequest(
    val meal_type: String
)

// ========== GENERIC ==========
@JsonClass(generateAdapter = true)
data class MessageResponse(
    val message: String? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class ErrorResponse(
    val error: String
)

// ========== CHATBOT ==========
@JsonClass(generateAdapter = true)
data class ChatRequest(
    val userId: Int,
    val message: String,
    val timezone: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatResponse(
    val reply: String? = null,
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class DailySummaryRequest(
    val sleepHours: Double,
    val calories: Int,
    val screenTimeMinutes: Int,
    val habitsCompleted: Int,
    val habitsTotal: Int
)

@JsonClass(generateAdapter = true)
data class DailySummaryResponse(
    val summary: String,
    val error: String? = null
)
