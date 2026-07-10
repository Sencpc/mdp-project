package mad.project.mdp_project.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import mad.project.mdp_project.MainActivity
import mad.project.mdp_project.R

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "HabitReminderChannel"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_HABIT_NAME = "extra_habit_name"
        const val EXTRA_USE_RINGTONE = "extra_use_ringtone"
        const val EXTRA_USE_VIBRATION = "extra_use_vibration"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getIntExtra(EXTRA_HABIT_ID, -1)
        val habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: "Habit Reminder"
        
        // Always fetch latest global setting from SessionManager to be sure
        val sessionManager = mad.project.mdp_project.data.SessionManager(context)
        val globalType = sessionManager.getNotificationType()
        
        val (useRingtone, useVibration) = when (globalType) {
            "Silent" -> Pair(false, false)
            "Vibrate Only" -> Pair(false, true)
            "Ringtone Only" -> Pair(true, false)
            "Vibrate & Ringtone" -> Pair(true, true)
            else -> Pair(true, true)
        }

        createNotificationChannel(context, useRingtone, useVibration)

        // Intent to open app when notification is tapped
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, habitId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle("Habit Reminder")
            .setContentText("It's time for: $habitName")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        var defaults = 0
        if (useRingtone) defaults = defaults or NotificationCompat.DEFAULT_SOUND
        if (useVibration) defaults = defaults or NotificationCompat.DEFAULT_VIBRATE
        if (defaults != 0) {
            builder.setDefaults(defaults)
        }

        val notification = builder.build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(habitId, notification)
    }

    private fun createNotificationChannel(context: Context, useRingtone: Boolean, useVibration: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Delete old channel to force importance/sound/vibe update
            // (Android channels are immutable once created)
            notificationManager.deleteNotificationChannel(CHANNEL_ID)

            val importance = if (useRingtone || useVibration) {
                NotificationManager.IMPORTANCE_HIGH
            } else {
                NotificationManager.IMPORTANCE_LOW // Low = No sound/popup
            }

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Habit Reminders",
                importance
            ).apply {
                description = "Notifications for habit reminders"
                enableVibration(useVibration)
                if (!useRingtone) {
                    setSound(null, null)
                }
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
