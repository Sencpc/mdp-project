package mad.project.mdp_project.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
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

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository

    // UI State
    private val _loginResult = MutableLiveData<Result<User>>()
    val loginResult: LiveData<Result<User>> = _loginResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Smart loading: only show overlay after 500ms delay
    private val _showLoadingOverlay = MutableLiveData(false)
    val showLoadingOverlay: LiveData<Boolean> = _showLoadingOverlay

    private var delayedLoadingJob: Job? = null

    init {
        val db = AppDatabase.getDatabase(application)
        userRepository = UserRepository(db.userDao(), RetrofitClient.apiService)
    }

    fun login(username: String, password: String) {
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
