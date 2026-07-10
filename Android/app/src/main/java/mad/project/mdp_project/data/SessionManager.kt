package mad.project.mdp_project.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_USERNAME = "username"
        const val KEY_NOTIFICATION_TYPE = "notification_type"
    }

    fun saveSession(userId: Int, username: String) {
        prefs.edit().apply {
            putInt(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            apply()
        }
    }

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)
    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun saveNotificationType(type: String) {
        prefs.edit().putString(KEY_NOTIFICATION_TYPE, type).apply()
    }

    fun getNotificationType(): String {
        return prefs.getString(KEY_NOTIFICATION_TYPE, "Vibrate & Ringtone") ?: "Vibrate & Ringtone"
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
