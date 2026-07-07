package mad.project.mdp_project

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import mad.project.mdp_project.data.AppDatabase
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
}