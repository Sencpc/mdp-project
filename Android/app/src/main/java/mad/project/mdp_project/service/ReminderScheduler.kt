package mad.project.mdp_project.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object ReminderScheduler {

    private const val TAG = "ReminderScheduler"

    /**
     * Schedule a reminder notification for a habit.
     *
     * @param context Application context
     * @param requestCode Unique request code for this specific reminder (e.g. habitId * 100 + index)
     * @param habitId Unique ID of the habit
     * @param habitName Name of the habit to display in notification
     * @param reminderTimeMillis The stored reminder time (hour & minute extracted from this)
     */
    fun scheduleReminder(
        context: Context,
        requestCode: Int,
        habitId: Int,
        habitName: String,
        reminderTimeMillis: Long,
        useRingtone: Boolean = true,
        useVibration: Boolean = true
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_HABIT_ID, habitId)
            putExtra(ReminderReceiver.EXTRA_HABIT_NAME, habitName)
            putExtra(ReminderReceiver.EXTRA_USE_RINGTONE, useRingtone)
            putExtra(ReminderReceiver.EXTRA_USE_VIBRATION, useVibration)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Extract hour & minute from the reminderTimeMillis
        val reminderCal = Calendar.getInstance().apply { timeInMillis = reminderTimeMillis }
        val hour = reminderCal.get(Calendar.HOUR_OF_DAY)
        val minute = reminderCal.get(Calendar.MINUTE)

        // Set the alarm for today at that hour:minute
        val triggerCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the time already passed today, schedule for tomorrow
        if (triggerCal.timeInMillis <= System.currentTimeMillis()) {
            triggerCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerCal.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerCal.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Reminder scheduled for habit '$habitName' (ID: $habitId, Req: $requestCode) at ${triggerCal.time}")
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot schedule exact alarm: ${e.message}")
            // Fallback to inexact alarm
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerCal.timeInMillis,
                pendingIntent
            )
        }
    }

    /**
     * Cancel all previously scheduled reminders for a habit.
     */
    fun cancelAllReminders(context: Context, habitId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (i in 0 until 24) {
            val requestCode = habitId * 100 + i
            val intent = Intent(context, ReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pendingIntent)
        }
        Log.d(TAG, "All reminders cancelled for habit ID: $habitId")
    }
}
