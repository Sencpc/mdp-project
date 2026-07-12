package mad.project.mdp_project.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import mad.project.mdp_project.MainActivity
import mad.project.mdp_project.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ScreenTimeService : Service() {

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 1
        private const val NUDGE_NOTIFICATION_ID = 2000
        const val CHANNEL_ID = "ScreenTimeServiceChannel"
        const val NUDGE_CHANNEL_ID = "ScreenTimeNudgeChannel"
        private const val CHECK_INTERVAL_MS = 60_000L // Check every 60 seconds

        private const val PREFS_NAME = "screen_time_nudge_prefs"
        private const val KEY_NUDGE_MODE = "nudge_mode"
        private const val KEY_LAST_NUDGE_DATE = "last_nudge_date"
        private const val KEY_LIMIT_HOURS = "limit_hours"
        private const val KEY_LIMIT_MINUTES = "limit_minutes"
        private const val KEY_LAST_PERIODIC_THRESHOLD = "last_periodic_threshold"
        private const val KEY_LAST_PERIODIC_DATE = "last_periodic_date"

        // Default limit: 4 hours 0 minutes
        private const val DEFAULT_LIMIT_HOURS = 4
        private const val DEFAULT_LIMIT_MINUTES = 0

        // Periodic mode constants
        private const val PERIODIC_START_HOURS = 4
        private const val PERIODIC_INTERVAL_HOURS = 2

        // Nudge mode values
        const val MODE_NONE = "none"
        const val MODE_CUSTOM = "custom"
        const val MODE_PERIODIC = "periodic"

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

        // ── Nudge Mode ──────────────────────────────────────────────────

        fun getNudgeMode(context: Context): String {
            return getPrefs(context).getString(KEY_NUDGE_MODE, MODE_NONE) ?: MODE_NONE
        }

        fun setNudgeMode(context: Context, mode: String) {
            getPrefs(context).edit()
                .putString(KEY_NUDGE_MODE, mode)
                // Reset tracking state so notifications can re-evaluate
                .remove(KEY_LAST_NUDGE_DATE)
                .remove(KEY_LAST_PERIODIC_THRESHOLD)
                .remove(KEY_LAST_PERIODIC_DATE)
                .apply()
        }

        // ── Legacy compat: isDailyLimitEnabled maps to MODE_CUSTOM ──────

        fun isDailyLimitEnabled(context: Context): Boolean {
            return getNudgeMode(context) == MODE_CUSTOM
        }

        fun setDailyLimitEnabled(context: Context, enabled: Boolean) {
            setNudgeMode(context, if (enabled) MODE_CUSTOM else MODE_NONE)
        }

        // ── Custom limit getters / setters ──────────────────────────────

        /**
         * Returns the daily limit in milliseconds, based on user-configured hours and minutes.
         */
        fun getDailyLimitMs(context: Context): Long {
            val prefs = getPrefs(context)
            val hours = prefs.getInt(KEY_LIMIT_HOURS, DEFAULT_LIMIT_HOURS)
            val minutes = prefs.getInt(KEY_LIMIT_MINUTES, DEFAULT_LIMIT_MINUTES)
            return ((hours * 60L) + minutes) * 60L * 1000L
        }

        fun getLimitHours(context: Context): Int {
            return getPrefs(context).getInt(KEY_LIMIT_HOURS, DEFAULT_LIMIT_HOURS)
        }

        fun getLimitMinutes(context: Context): Int {
            return getPrefs(context).getInt(KEY_LIMIT_MINUTES, DEFAULT_LIMIT_MINUTES)
        }

        fun setDailyLimit(context: Context, hours: Int, minutes: Int) {
            getPrefs(context).edit()
                .putInt(KEY_LIMIT_HOURS, hours)
                .putInt(KEY_LIMIT_MINUTES, minutes)
                // Reset the nudge date so the notification can re-fire
                // if the new limit is already exceeded
                .remove(KEY_LAST_NUDGE_DATE)
                .apply()
        }

        fun formatLimit(context: Context): String {
            val h = getLimitHours(context)
            val m = getLimitMinutes(context)
            return "${h}h ${m}m"
        }

        // ── Periodic threshold tracking ─────────────────────────────────

        private fun getLastPeriodicThreshold(context: Context): Int {
            return getPrefs(context).getInt(KEY_LAST_PERIODIC_THRESHOLD, 0)
        }

        private fun setLastPeriodicThreshold(context: Context, hours: Int) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = dateFormat.format(Calendar.getInstance().time)
            getPrefs(context).edit()
                .putInt(KEY_LAST_PERIODIC_THRESHOLD, hours)
                .putString(KEY_LAST_PERIODIC_DATE, todayStr)
                .apply()
        }

        /**
         * Resets the periodic threshold if the stored date is not today.
         */
        private fun resetPeriodicIfNewDay(context: Context) {
            val prefs = getPrefs(context)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = dateFormat.format(Calendar.getInstance().time)
            val lastDate = prefs.getString(KEY_LAST_PERIODIC_DATE, null)
            if (lastDate != todayStr) {
                prefs.edit()
                    .putInt(KEY_LAST_PERIODIC_THRESHOLD, 0)
                    .putString(KEY_LAST_PERIODIC_DATE, todayStr)
                    .remove(KEY_LAST_NUDGE_DATE)
                    .apply()
            }
        }

        private fun getPrefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val checkRunnable = object : Runnable {
        override fun run() {
            checkScreenTimeAndNotify()
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Time Tracking")
            .setContentText("Monitoring your daily screen time")
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setOngoing(true)
            .build()

        startForeground(FOREGROUND_NOTIFICATION_ID, notification)

        // Start periodic checking
        handler.removeCallbacks(checkRunnable)
        handler.post(checkRunnable)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)
    }

    /**
     * Checks total screen time for today and sends a notification
     * based on the active nudge mode (custom or periodic).
     */
    private fun checkScreenTimeAndNotify() {
        try {
            val mode = getNudgeMode(this)
            if (mode == MODE_NONE) return

            // Reset periodic tracking at day boundary
            resetPeriodicIfNewDay(this)

            val totalMs = getTodayTotalScreenTime()

            when (mode) {
                MODE_CUSTOM -> handleCustomNudge(totalMs)
                MODE_PERIODIC -> handlePeriodicNudge(totalMs)
            }
        } catch (e: Exception) {
            // Silently fail — permissions may not be granted yet
            e.printStackTrace()
        }
    }

    /**
     * Custom mode: fires a single notification when the user-configured
     * limit is reached. Only notifies once per day.
     */
    private fun handleCustomNudge(totalMs: Long) {
        val prefs = getPrefs(this)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = dateFormat.format(Calendar.getInstance().time)
        val lastNudgeDate = prefs.getString(KEY_LAST_NUDGE_DATE, null)
        if (lastNudgeDate == todayStr) return // Already notified today

        val dailyLimitMs = getDailyLimitMs(this)
        if (dailyLimitMs <= 0) return

        if (totalMs >= dailyLimitMs) {
            sendCustomNudgeNotification(totalMs)
            prefs.edit().putString(KEY_LAST_NUDGE_DATE, todayStr).apply()
        }
    }

    /**
     * Periodic mode: sends a notification every 2 hours of screen time,
     * starting from hour 4 (i.e., at 4h, 6h, 8h, 10h, …).
     * Each threshold fires only once per day.
     */
    private fun handlePeriodicNudge(totalMs: Long) {
        val totalHours = (totalMs / (1000.0 * 60.0 * 60.0)).toInt()

        if (totalHours < PERIODIC_START_HOURS) return

        // Determine the highest threshold crossed:
        // thresholds are 4, 6, 8, 10, ...
        // = PERIODIC_START_HOURS + n * PERIODIC_INTERVAL_HOURS  where n >= 0
        val stepsAboveStart = (totalHours - PERIODIC_START_HOURS) / PERIODIC_INTERVAL_HOURS
        val currentThreshold = PERIODIC_START_HOURS + (stepsAboveStart * PERIODIC_INTERVAL_HOURS)

        val lastThreshold = getLastPeriodicThreshold(this)

        if (currentThreshold > lastThreshold) {
            sendPeriodicNudgeNotification(totalMs, currentThreshold)
            setLastPeriodicThreshold(this, currentThreshold)
        }
    }

    /**
     * Calculate total screen time for today using UsageEvents for accuracy.
     */
    private fun getTodayTotalScreenTime(): Long {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val events = usageStatsManager.queryEvents(startTime, now)

        val foregroundTimes = mutableMapOf<String, Long>()
        val lastForegroundTimestamp = mutableMapOf<String, Long>()

        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    lastForegroundTimestamp[event.packageName] = event.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val fgStart = lastForegroundTimestamp.remove(event.packageName)
                    if (fgStart != null) {
                        val duration = event.timeStamp - fgStart
                        foregroundTimes[event.packageName] =
                            (foregroundTimes[event.packageName] ?: 0L) + duration
                    }
                }
            }
        }

        // For apps still in the foreground, count time up to now
        for ((pkg, fgStart) in lastForegroundTimestamp) {
            val duration = now - fgStart
            foregroundTimes[pkg] = (foregroundTimes[pkg] ?: 0L) + duration
        }

        return foregroundTimes.values.sum()
    }

    /**
     * Sends the custom limit notification (same as the old Daily Limit Alert).
     */
    private fun sendCustomNudgeNotification(totalMs: Long) {
        val totalMinutes = totalMs / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        val limitStr = formatLimit(this)

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, NUDGE_NOTIFICATION_ID, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NUDGE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle("Screen Time Limit Reached")
            .setContentText("You've reached your daily screen time limit of $limitStr.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("You've used your phone for ${hours}h ${minutes}m today — that's past your $limitStr goal. Consider taking a break and doing something offline! 🌿")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NUDGE_NOTIFICATION_ID, notification)
    }

    /**
     * Sends a periodic nudge notification for the given threshold.
     * Uses a unique notification ID per threshold so Android doesn't collapse them.
     */
    private fun sendPeriodicNudgeNotification(totalMs: Long, thresholdHours: Int) {
        val totalMinutes = totalMs / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val notificationId = NUDGE_NOTIFICATION_ID + thresholdHours
        val pendingIntent = PendingIntent.getActivity(
            this, notificationId, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NUDGE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(getString(R.string.nudge_periodic_title))
            .setContentText(getString(R.string.nudge_periodic_text, thresholdHours))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(getString(R.string.nudge_periodic_big_text, thresholdHours, thresholdHours))
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Low-priority channel for the foreground service
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Screen Time Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used for tracking screen time in the background"
            }
            manager.createNotificationChannel(serviceChannel)

            // High-priority channel for nudge alerts
            val nudgeChannel = NotificationChannel(
                NUDGE_CHANNEL_ID,
                "Screen Time Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when you reach your daily screen time limit"
                enableVibration(true)
            }
            manager.createNotificationChannel(nudgeChannel)
        }
    }
}

