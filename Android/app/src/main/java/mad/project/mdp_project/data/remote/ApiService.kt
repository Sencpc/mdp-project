package mad.project.mdp_project.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ========== AUTH ==========
    @POST("api/users/register")
    suspend fun register(@Body request: RegisterRequest): Response<UserResponse>

    @POST("api/users/login")
    suspend fun login(@Body request: LoginRequest): Response<UserResponse>

    // ========== USER ==========
    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") userId: Int): Response<UserResponse>

    @PUT("api/users/{id}")
    suspend fun updateUser(@Path("id") userId: Int, @Body request: UpdateUserRequest): Response<UserResponse>

    // ========== HABITS ==========
    @POST("api/habits")
    suspend fun createHabit(@Body request: HabitRequest): Response<HabitResponse>

    @GET("api/habits/user/{userId}")
    suspend fun getHabitsForUser(@Path("userId") userId: Int): Response<List<HabitResponse>>

    @PUT("api/habits/{id}")
    suspend fun updateHabit(@Path("id") habitId: Int, @Body request: HabitRequest): Response<HabitResponse>

    @DELETE("api/habits/{id}")
    suspend fun deleteHabit(@Path("id") habitId: Int): Response<MessageResponse>

    // ========== SLEEP LOGS ==========
    @POST("api/sleep")
    suspend fun createSleepLog(@Body request: SleepLogRequest): Response<SleepLogResponse>

    @GET("api/sleep/user/{userId}")
    suspend fun getSleepLogsForUser(@Path("userId") userId: Int): Response<List<SleepLogResponse>>

    // ========== NUTRITION ==========
    @POST("api/nutrition")
    suspend fun createNutritionLog(@Body request: NutritionRequest): Response<NutritionResponse>

    @GET("api/nutrition/user/{userId}")
    suspend fun getNutritionLogsForUser(@Path("userId") userId: Int): Response<List<NutritionResponse>>

    @PUT("api/nutrition/{id}")
    suspend fun updateNutritionLog(@Path("id") logId: Int, @Body request: UpdateMealTypeRequest): Response<NutritionResponse>

    // ========== AI CALORIE SCANNER ==========
    @Multipart
    @POST("api/nutrition/analyze")
    suspend fun analyzeFood(
        @Part image: MultipartBody.Part
    ): Response<AnalyzeResponse>

    // ========== AI CHATBOT ==========
    @POST("api/chat")
    suspend fun sendChatMessage(@Body request: ChatRequest): Response<ChatResponse>

    @DELETE("api/chat/user/{userId}/history")
    suspend fun clearChatHistory(@Path("userId") userId: Int): Response<MessageResponse>

    @DELETE("api/chat/user/{userId}/memory")
    suspend fun resetAiMemory(@Path("userId") userId: Int): Response<MessageResponse>

    @POST("api/dashboard/weekly-summary")
    suspend fun getDailySummary(@Body request: DailySummaryRequest): Response<DailySummaryResponse>

    // ========== FACILITIES (from Backend → SATUSEHAT MSI) ==========
    @GET("api/facilities")
    suspend fun getFacilities(): Response<FacilityApiResponse>
}
