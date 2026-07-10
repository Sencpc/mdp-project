package mad.project.mdp_project.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import mad.project.mdp_project.R

/**
 * Utility for managing consultation notifications.
 *
 * Creates the notification channel on init and provides
 * a builder for consultation reminder notifications.
 */
object NotificationHelper {

    private const val CHANNEL_ID = "consultation_reminders"
    private const val CHANNEL_NAME = "Consultation Reminders"
    private const val CHANNEL_DESCRIPTION = "Reminders for upcoming doctor consultations"

    /**
     * Creates the notification channel (required for Android 8.0+).
     * Safe to call multiple times — the system ignores duplicate channel creation.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Shows a consultation reminder notification.
     */
    fun showReminderNotification(
        context: Context,
        notificationId: Int,
        doctorName: String,
        facilityName: String,
        timeString: String
    ) {
        val title = "Upcoming Consultation"
        val body = buildString {
            append("Your consultation with $doctorName is at $timeString.")
            if (facilityName.isNotBlank()) {
                append(" Location: $facilityName")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}
