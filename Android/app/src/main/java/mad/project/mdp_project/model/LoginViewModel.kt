package mad.project.mdp_project.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.User
import mad.project.mdp_project.data.remote.RetrofitClient
import mad.project.mdp_project.data.repository.UserRepository
import android.content.Context

class LoginViewModel(private val userRepository: UserRepository) : ViewModel() {

    // UI State
    private val _loginResult = MutableLiveData<Result<User>>()
    val loginResult: LiveData<Result<User>> = _loginResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Smart loading: only show overlay after 500ms delay
    private val _showLoadingOverlay = MutableLiveData(false)
    val showLoadingOverlay: LiveData<Boolean> = _showLoadingOverlay

    private var delayedLoadingJob: Job? = null

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
                val db = AppDatabase.getDatabase(context)
                val userRepository = UserRepository(db.userDao(), RetrofitClient.apiService)
                @Suppress("UNCHECKED_CAST")
                return LoginViewModel(userRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    fun login(username: String, password: String) {
        if (_isLoading.value == true) return

        if (username.isEmpty() || password.isEmpty()) {
            _loginResult.value = Result.failure(Exception("Harap isi semua field"))
            return
        }

        _isLoading.value = true

        // Start a delayed job: show overlay only if request takes > 500ms
        delayedLoadingJob = viewModelScope.launch {
            delay(500L)
            _showLoadingOverlay.value = true
        }

        viewModelScope.launch {
            val result = userRepository.login(username, password)
            // Cancel the delayed overlay job if it hasn't fired yet
            delayedLoadingJob?.cancel()
            _showLoadingOverlay.value = false
            _loginResult.value = result
            _isLoading.value = false
        }
    }
}
