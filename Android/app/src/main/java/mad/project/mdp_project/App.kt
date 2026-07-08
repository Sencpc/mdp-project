package mad.project.mdp_project

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.service.ReminderReceiver
import mad.project.mdp_project.service.ScreenTimeService

class App : Application() {
    companion object {
        lateinit var db: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(this)

        // Start the screen time tracking service
        startScreenTimeService()

        // Create notification channel for habit reminders
        createReminderNotificationChannel()
    }

    private fun startScreenTimeService() {
        try {
            val serviceIntent = Intent(this, ScreenTimeService::class.java)
            ContextCompat.startForegroundService(this, serviceIntent)
        } catch (e: Exception) {
            // Service may fail to start if permissions are not granted yet
            // The fragment will retry when opened
            e.printStackTrace()
        }
    }

    private fun createReminderNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderReceiver.CHANNEL_ID,
                "Habit Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for habit reminders"
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}