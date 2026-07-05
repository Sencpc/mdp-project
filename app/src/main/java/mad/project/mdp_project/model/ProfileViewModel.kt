package mad.project.mdp_project.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.User
import mad.project.mdp_project.data.UserDao
import java.util.Calendar

class ProfileViewModel(private val userDao: UserDao, private val userId: Int) : ViewModel() {

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            _user.value = userDao.getUserById(userId)
        }
    }

    fun updateHeight(height: Float) {
        val currentUser = _user.value ?: return
        viewModelScope.launch {
            val updatedUser = currentUser.copy(height = height)
            userDao.updateUser(updatedUser)
            _user.value = updatedUser
        }
    }

    fun updateWeight(weight: Float) {
        val currentUser = _user.value ?: return
        viewModelScope.launch {
            val updatedUser = currentUser.copy(weight = weight)
            userDao.updateUser(updatedUser)
            _user.value = updatedUser
        }
    }

    fun updateBirthDate(birthDate: Long) {
        val currentUser = _user.value ?: return
        viewModelScope.launch {
            val updatedUser = currentUser.copy(birthDate = birthDate)
            userDao.updateUser(updatedUser)
            _user.value = updatedUser
        }
    }

    fun updateBloodType(bloodType: String) {
        val currentUser = _user.value ?: return
        viewModelScope.launch {
            val updatedUser = currentUser.copy(bloodType = bloodType)
            userDao.updateUser(updatedUser)
            _user.value = updatedUser
        }
    }

    fun updateConditions(conditions: List<String>) {
        val currentUser = _user.value ?: return
        viewModelScope.launch {
            val updatedUser = currentUser.copy(conditions = conditions.joinToString(","))
            userDao.updateUser(updatedUser)
            _user.value = updatedUser
        }
    }

    fun updateEmergencyContact(name: String, phone: String) {
        val currentUser = _user.value ?: return
        viewModelScope.launch {
            val updatedUser = currentUser.copy(
                emergencyContactName = name,
                emergencyContactPhone = phone
            )
            userDao.updateUser(updatedUser)
            _user.value = updatedUser
        }
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

    class Factory(private val userDao: UserDao, private val userId: Int) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProfileViewModel(userDao, userId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
