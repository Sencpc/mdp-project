package mad.project.mdp_project.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.ChatMessage
import mad.project.mdp_project.data.SessionManager

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatMessageDao()
    private val sessionManager = SessionManager(application)
    private val userId = sessionManager.getUserId()

    val messages: StateFlow<List<ChatMessage>> = chatDao.getMessagesForUser(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            // Insert user message
            val userMessage = ChatMessage(userId = userId, message = text, isFromBot = false)
            chatDao.insertMessage(userMessage)

            // Simulate bot response
            simulateBotResponse(text)
        }
    }

    private suspend fun simulateBotResponse(userText: String) {
        val response = when {
            userText.contains("sleep", ignoreCase = true) -> 
                "To sleep better, try maintaining a consistent schedule, avoiding caffeine late in the day, and creating a dark, cool environment."
            userText.contains("screen time", ignoreCase = true) -> 
                "You can check your screen time in the Screen Time section. Reducing usage an hour before bed can also improve your sleep."
            userText.contains("feeling", ignoreCase = true) || userText.contains("sluggish", ignoreCase = true) ->
                "I'm sorry to hear that. Feeling sluggish can be due to many factors like hydration or sleep. Would you like some tips to boost your energy?"
            else -> "I'm here to support your wellness journey. How can I assist you today?"
        }

        val botMessage = ChatMessage(userId = userId, message = response, isFromBot = true)
        chatDao.insertMessage(botMessage)
    }

    fun sendQuickAction(action: String) {
        sendMessage(action)
    }
}
