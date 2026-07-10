package mad.project.mdp_project.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.repository.ConsultationRepository

/**
 * PeriodicWorkRequest worker that auto-completes past consultations.
 *
 * Runs every 6 hours to transition "Upcoming" consultations whose
 * consultationTime has passed to "Completed" status.
 */
class ConsultationStatusWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(applicationContext)
        val repository = ConsultationRepository(db.consultationDao(), db.reviewDao())
        repository.markPastConsultationsCompleted()
        return Result.success()
    }
}
