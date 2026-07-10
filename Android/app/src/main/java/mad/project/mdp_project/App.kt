package mad.project.mdp_project

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.service.ReminderReceiver
import mad.project.mdp_project.service.ScreenTimeService
import mad.project.mdp_project.worker.ConsultationStatusWorker
import java.util.concurrent.TimeUnit

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

        // Create notification channels
        createReminderNotificationChannel()
        createScreenTimeNudgeChannel()

        // Schedule periodic workers
        scheduleConsultationStatusWorker()
    }

    private fun scheduleConsultationStatusWorker() {
        val workRequest = PeriodicWorkRequestBuilder<ConsultationStatusWorker>(
            6, TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "consultation_status_updater",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
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

    private fun createScreenTimeNudgeChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ScreenTimeService.NUDGE_CHANNEL_ID,
                "Screen Time Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when you reach your daily screen time limit"
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}