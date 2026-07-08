package mad.project.mdp_project.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.ChatMessage
import mad.project.mdp_project.data.SessionManager
import mad.project.mdp_project.data.remote.RetrofitClient
import mad.project.mdp_project.data.repository.ChatRepository

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatMessageDao()
    private val sessionManager = SessionManager(application)
    private val userId = sessionManager.getUserId()

    private val repository = ChatRepository(chatDao, RetrofitClient.apiService)

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val messages: StateFlow<List<ChatMessage>> = chatDao.getMessagesForUser(userId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            _isLoading.value = true
            repository.sendMessage(userId, text)
            _isLoading.value = false
        }
    }

    fun sendQuickAction(action: String) {
        sendMessage(action)
    }
}
