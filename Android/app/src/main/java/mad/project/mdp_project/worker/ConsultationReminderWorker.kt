package mad.project.mdp_project.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import mad.project.mdp_project.util.NotificationHelper

/**
 * OneTimeWorkRequest worker that fires a consultation reminder notification.
 *
 * Scheduled when a consultation is booked.
 * Fires 1 hour before the consultation time.
 *
 * Input data:
 * - "doctorName" (String): Name of the doctor
 * - "facilityName" (String): Name of the facility
 * - "consultationTime" (String): Formatted time string for display
 * - "consultationId" (Int): Used as notification ID for uniqueness
 */
class ConsultationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val doctorName = inputData.getString("doctorName") ?: "your doctor"
        val facilityName = inputData.getString("facilityName") ?: ""
        val timeString = inputData.getString("consultationTime") ?: "soon"
        val consultationId = inputData.getInt("consultationId", 0)

        NotificationHelper.createNotificationChannel(applicationContext)
        NotificationHelper.showReminderNotification(
            context = applicationContext,
            notificationId = consultationId,
            doctorName = doctorName,
            facilityName = facilityName,
            timeString = timeString
        )

        return Result.success()
    }
}
