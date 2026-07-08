package mad.project.mdp_project.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import mad.project.mdp_project.data.User
import mad.project.mdp_project.data.UserDao
import mad.project.mdp_project.data.remote.ApiService
import mad.project.mdp_project.data.remote.LoginRequest
import mad.project.mdp_project.data.remote.RegisterRequest
import mad.project.mdp_project.data.remote.UpdateUserRequest

class UserRepository(
    private val userDao: UserDao,
    private val apiService: ApiService
) {
    companion object {
        private const val TAG = "UserRepository"
    }

    /**
     * Register: kirim ke remote dulu, lalu simpan ke Room lokal.
     * @return Result<User> — berhasil atau gagal
     */
    suspend fun register(username: String, password: String, fullName: String): Result<User> {
        return try {
            // 1. Kirim ke server
            val response = apiService.register(RegisterRequest(username, password, fullName))

            if (response.isSuccessful && response.body() != null) {
                val apiUser = response.body()!!
                // 2. Simpan ke Room lokal
                val localUser = User(
                    id = apiUser.id,
                    username = apiUser.username,
                    password = password,
                    fullName = apiUser.fullName ?: ""
                )
                userDao.insertUser(localUser)
                Log.d(TAG, "Register berhasil: id=${localUser.id}")
                Result.success(localUser)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Register gagal"
                Log.e(TAG, "Register gagal: $errorMsg")
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Register error (network?): ${e.message}")
            // Fallback: simpan ke Room saja jika server tidak tersedia
            val existingUser = userDao.getUserByUsername(username)
            if (existingUser != null) {
                return Result.failure(Exception("Username sudah digunakan"))
            }
            val localUser = User(
                username = username,
                password = password,
                fullName = fullName
            )
            userDao.insertUser(localUser)
            val savedUser = userDao.getUserByUsername(username)
            Log.d(TAG, "Register fallback ke lokal: id=${savedUser?.id}")
            Result.success(savedUser ?: localUser)
        }
    }

    /**
     * Login: cek remote dulu, fallback ke lokal.
     * @return Result<User>
     */
    suspend fun login(username: String, password: String): Result<User> {
        return try {
            // 1. Coba login ke server
            val response = apiService.login(LoginRequest(username, password))

            if (response.isSuccessful && response.body() != null) {
                val apiUser = response.body()!!
                // 2. Simpan/update di Room lokal
                val localUser = User(
                    id = apiUser.id,
                    username = apiUser.username,
                    password = password,
                    fullName = apiUser.fullName ?: ""
                )
                userDao.insertUser(localUser)
                Log.d(TAG, "Login via API berhasil: id=${localUser.id}")
                Result.success(localUser)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                val code = response.code()
                Log.e(TAG, "Login API gagal: Code=$code, Msg=$errorMsg, coba lokal...")
                loginLocal(username, password)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login API error (Network/Moshi): ${e.message}, fallback ke lokal")
            loginLocal(username, password)
        }
    }

    private suspend fun loginLocal(username: String, password: String): Result<User> {
        val user = userDao.getUserByUsername(username)
        return if (user != null && user.password == password) {
            Log.d(TAG, "Login lokal berhasil: id=${user.id}")
            Result.success(user)
        } else {
            Result.failure(Exception("Username atau password salah"))
        }
    }

    /**
     * Get user by ID dari Room (real-time via Flow).
     */
    fun getUserById(userId: Int): Flow<User?> {
        return userDao.getUserById(userId)
    }

    /**
     * Get user by ID sekali (one-shot).
     */
    suspend fun getUserByIdOnce(userId: Int): User? {
        return userDao.getUserByIdOnce(userId)
    }

    /**
     * Update user — update lokal dulu, lalu sync ke server.
     */
    suspend fun updateUser(user: User): Result<User> {
        return try {
            // 1. Update lokal
            userDao.updateUser(user)

            // 2. Sync ke server
            try {
                apiService.updateUser(user.id, UpdateUserRequest(
                    fullName = user.fullName,
                    height = user.height,
                    weight = user.weight,
                    birthDate = user.birthDate,
                    bloodType = user.bloodType,
                    conditions = user.conditions,
                    emergencyContactName = user.emergencyContactName,
                    emergencyContactPhone = user.emergencyContactPhone,
                    profilePicturePath = user.profilePicturePath
                ))
                Log.d(TAG, "User synced ke server")
            } catch (e: Exception) {
                Log.w(TAG, "Gagal sync user ke server: ${e.message}")
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
