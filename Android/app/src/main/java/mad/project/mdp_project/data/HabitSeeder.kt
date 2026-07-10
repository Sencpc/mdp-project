package mad.project.mdp_project.data

import java.util.Calendar

object HabitSeeder {

    fun getStandardHabits(userId: Int): List<Habit> {
        val habits = mutableListOf<Habit>()

        // 1. Drink Water
        habits.add(
            Habit(
                userId = userId,
                name = "Drink Water",
                subtitle = "Stay hydrated by drinking 8 glasses of water daily",
                category = "Nutrition",
                startTime = getTodayTime(6, 0),
                endTime = getTodayTime(22, 0),
                reminders = getWaterReminders()
            )
        )

        // 2. Eat Healthy
        habits.add(
            Habit(
                userId = userId,
                name = "Eat Healthy",
                subtitle = "Maintain a balanced diet 3 times a day",
                category = "Nutrition",
                startTime = getTodayTime(6, 0),
                endTime = getTodayTime(20, 0),
                reminders = listOf(
                    getTodayTime(6, 0),  // Pagi
                    getTodayTime(13, 0), // Siang
                    getTodayTime(19, 0)  // Malam
                )
            )
        )

        // 3. Night Sleep
        habits.add(
            Habit(
                userId = userId,
                name = "Night Sleep",
                subtitle = "goodnight 🌙",
                category = "Sleep",
                startTime = getTodayTime(22, 0),
                endTime = getTodayTime(6, 0) + (24 * 60 * 60 * 1000L),
                reminders = listOf(
                    getTodayTime(22, 0),
                    getTodayTime(6, 0) + (24 * 60 * 60 * 1000L)
                )
            )
        )

        return habits
    }

    private fun getTodayTime(hour: Int, minute: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getWaterReminders(): List<Long> {
        val reminders = mutableListOf<Long>()
        // Start at 6 AM, every 2 hours, 8 times
        // 6, 8, 10, 12, 14, 16, 18, 20
        for (i in 0 until 8) {
            reminders.add(getTodayTime(6 + (i * 2), 0))
        }
        return reminders
    }
}
