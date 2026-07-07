package mad.project.mdp_project.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.User
import mad.project.mdp_project.data.remote.RetrofitClient
import mad.project.mdp_project.data.repository.UserRepository

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository: UserRepository

    // UI State
    private val _registerResult = MutableLiveData<Result<User>>()
    val registerResult: LiveData<Result<User>> = _registerResult

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        val db = AppDatabase.getDatabase(application)
        userRepository = UserRepository(db.userDao(), RetrofitClient.apiService)
    }

    fun register(fullName: String, username: String, password: String) {
        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            _registerResult.value = Result.failure(Exception("Harap isi semua field"))
            return
        }

        if (password.length < 8) {
            _registerResult.value = Result.failure(Exception("Password minimal 8 karakter"))
            return
        }

        _isLoading.value = true
        viewModelScope.launch {
            val result = userRepository.register(username, password, fullName)
            _registerResult.value = result
            _isLoading.value = false
        }
    }
}
