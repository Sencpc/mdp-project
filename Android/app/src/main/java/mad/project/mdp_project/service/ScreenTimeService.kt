package mad.project.mdp_project.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import mad.project.mdp_project.R

class ScreenTimeService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "ScreenTimeServiceChannel"

        fun classifyPackage(pkg: String): String {
            val lowerPkg = pkg.lowercase()
            return when {
                lowerPkg.contains("facebook") || lowerPkg.contains("instagram") || 
                lowerPkg.contains("whatsapp") || lowerPkg.contains("twitter") || 
                lowerPkg.contains("tiktok") || lowerPkg.contains("snapchat") ||
                lowerPkg.contains("discord") || lowerPkg.contains("telegram") -> "social"
                
                lowerPkg.contains("youtube") || lowerPkg.contains("netflix") || 
                lowerPkg.contains("spotify") || lowerPkg.contains("hulu") || 
                lowerPkg.contains("disney") || lowerPkg.contains("twitch") -> "entertainment"
                
                lowerPkg.contains("gmail") || lowerPkg.contains("docs") || 
                lowerPkg.contains("drive") || lowerPkg.contains("slack") || 
                lowerPkg.contains("teams") || lowerPkg.contains("office") -> "productivity"
                
                else -> "other"
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Time Tracking")
            .setContentText("Monitoring your app usage")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Time Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used for tracking screen time in the background"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
