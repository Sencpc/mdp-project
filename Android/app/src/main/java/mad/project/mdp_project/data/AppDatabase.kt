package mad.project.mdp_project.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class, Habit::class, SleepLog::class, ScreenTimeLog::class,
        ChatMessage::class, DoctorEntity::class, ConsultationEntity::class,
        NutritionLog::class, FacilityEntity::class, ReviewEntity::class
    ],
    version = 15,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun habitDao(): HabitDao
    abstract fun sleepLogDao(): SleepLogDao
    abstract fun screenTimeLogDao(): ScreenTimeLogDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun doctorDao(): DoctorDao
    abstract fun consultationDao(): ConsultationDao
    abstract fun nutritionLogDao(): NutritionLogDao
    abstract fun facilityDao(): FacilityDao
    abstract fun reviewDao(): ReviewDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mdp_project_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true) // Memperbaiki crash akibat perubahan skema (User fields)
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        // Seed default admin user
                        database.userDao().insertUser(
                            User(username = "admin", password = "admin123", fullName = "Admin User")
                        )

                        // Seed doctors
                        database.doctorDao().insertAll(DoctorSeeder.getSeedDoctors())
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Re-seed doctors on every open to ensure fresh available times
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        val count = database.doctorDao().getCount()
                        if (count == 0) {
                            database.doctorDao().insertAll(DoctorSeeder.getSeedDoctors())
                        }
                    }
                }
            }
        }
    }
}
