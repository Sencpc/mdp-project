package mad.project.mdp_project.data.repository

import android.util.Log
import mad.project.mdp_project.data.ChatMessage
import mad.project.mdp_project.data.ChatMessageDao
import mad.project.mdp_project.data.remote.ApiService
import mad.project.mdp_project.data.remote.ChatRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatRepository(
    private val chatDao: ChatMessageDao,
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "ChatRepository"
    }

    /**
     * Send a message: save locally first for instant UI, then call the backend API.
     * On success, save the AI reply locally. On failure, insert an error bubble.
     */
    suspend fun sendMessage(userId: Int, text: String) {
        // 1. Save user message to Room immediately (instant UI update)
        val userMessage = ChatMessage(
            userId = userId,
            message = text,
            isFromBot = false
        )
        chatDao.insertMessage(userMessage)

        // 2. Call backend API
        try {
            val timezoneId = java.util.TimeZone.getDefault().id
            val response = apiService.sendChatMessage(ChatRequest(userId = userId, message = text, timezone = timezoneId))

            if (response.isSuccessful && response.body()?.reply != null) {
                val aiReply = response.body()!!.reply!!
                val botMessage = ChatMessage(
                    userId = userId,
                    message = aiReply,
                    isFromBot = true
                )
                chatDao.insertMessage(botMessage)
                Log.d(TAG, "AI reply received and saved")
            } else {
                val errorMsg = response.body()?.error ?: "Failed to get a response."
                Log.e(TAG, "API error: $errorMsg")
                chatDao.insertMessage(
                    ChatMessage(userId = userId, message = "Sorry, I couldn't respond right now. Please try again.", isFromBot = true)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error: ${e.message}")
            chatDao.insertMessage(
                ChatMessage(userId = userId, message = "Unable to connect to the server. Please check your internet connection.", isFromBot = true)
            )
        }
    }

    suspend fun clearHistory(userId: Int) {
        // Clear local messages first
        chatDao.deleteMessagesForUser(userId)

        // Clear backend history
        try {
            apiService.clearChatHistory(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Network error during clear history: ${e.message}")
        }
    }

    suspend fun resetMemory(userId: Int) {
        // Clear backend memory
        try {
            apiService.resetAiMemory(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Network error during reset memory: ${e.message}")
        }
    }
}
