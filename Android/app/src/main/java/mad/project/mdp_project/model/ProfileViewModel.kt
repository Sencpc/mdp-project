package mad.project.mdp_project.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.User
import mad.project.mdp_project.data.UserDao
import mad.project.mdp_project.data.remote.ApiService
import mad.project.mdp_project.data.repository.UserRepository
import java.util.Calendar

class ProfileViewModel(private val userDao: UserDao, private val userId: Int, private val apiService: ApiService) : ViewModel() {

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    private val userRepository = UserRepository(userDao, apiService)

    // Data dari database (Source of truth)
    val user: StateFlow<User?> = userRepository.getUserById(userId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    // Data sementara untuk di-edit (Draft)
    private val _draftUser = MutableStateFlow<User?>(null)
    val draftUser: StateFlow<User?> = _draftUser.asStateFlow()

    // Flag agar draft hanya diinisialisasi sekali dari DB
    private var isDraftInitialized = false

    init {
        Log.d(TAG, "ViewModel created for userId=$userId")
        viewModelScope.launch {
            // Query langsung ke database, tidak melalui StateFlow
            val dbUser = userRepository.getUserByIdOnce(userId)
            if (dbUser != null) {
                Log.d(TAG, "DB user loaded: id=${dbUser.id}, fullName=${dbUser.fullName}")
                _draftUser.value = dbUser
                isDraftInitialized = true
            } else {
                Log.e(TAG, "User NOT FOUND in DB for userId=$userId — session mungkin sudah tidak valid")
            }
        }
    }

    /**
     * Memperbarui data draft di memori.
     * Ini akan segera memperbarui UI Profile Screen tanpa menyimpan ke DB.
     */
    fun updateDraft(update: (User) -> User) {
        val current = _draftUser.value
        if (current != null) {
            _draftUser.value = update(current)
            Log.d(TAG, "Draft updated: ${_draftUser.value}")
        } else {
            Log.w(TAG, "updateDraft called but draftUser is null — user data not loaded yet")
        }
    }

    /**
     * Menyimpan seluruh perubahan dari draft ke database Room + sync ke server.
     * @param onComplete callback yang dipanggil setelah penyimpanan berhasil.
     */
    fun saveChanges(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            _draftUser.value?.let {
                userRepository.updateUser(it)
                Log.d(TAG, "Changes saved via repository for userId=${it.id}")
            }
            onComplete?.invoke()
        }
    }

    /**
     * Membatalkan perubahan draft dan kembali ke data dari database.
     */
    fun resetDraft() {
        _draftUser.value = user.value
        isDraftInitialized = user.value != null
    }

    fun calculateAge(birthDate: Long?): Int {
        if (birthDate == null) return 0
        val dob = Calendar.getInstance().apply { timeInMillis = birthDate }
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age
    }

    class Factory(private val userDao: UserDao, private val userId: Int, private val apiService: ApiService) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(userDao, userId, apiService) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
