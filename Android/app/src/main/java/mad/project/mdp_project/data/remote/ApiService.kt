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

    // ========== AI CALORIE SCANNER ==========
    @Multipart
    @POST("api/nutrition/scan")
    suspend fun scanFood(
        @Part image: MultipartBody.Part,
        @Part("userId") userId: RequestBody
    ): Response<NutritionResponse>

    // ========== AI CHATBOT ==========
    @POST("api/chat")
    suspend fun sendChatMessage(@Body request: ChatRequest): Response<ChatResponse>
}
