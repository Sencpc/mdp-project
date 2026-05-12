package mad.project.mdp_project

import android.app.Application
import mad.project.mdp_project.data.AppDatabase

class App : Application() {
    companion object {
        lateinit var db: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.getDatabase(this)
    }
}