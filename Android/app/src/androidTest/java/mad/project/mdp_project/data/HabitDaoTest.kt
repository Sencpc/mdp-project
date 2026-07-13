package mad.project.mdp_project.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class HabitDaoTest {

    private lateinit var habitDao: HabitDao
    private lateinit var userDao: UserDao
    private lateinit var db: AppDatabase
    private var testUserId = 0

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).allowMainThreadQueries().build()
        habitDao = db.habitDao()
        userDao = db.userDao()
        
        // Add a test user to satisfy foreign key constraints if any (even if not enforced, good practice)
        val user = User(username = "habituser", password = "123", fullName = "Habit User")
        userDao.insertUser(user)
        val insertedUser = userDao.getUserByUsername("habituser")
        testUserId = insertedUser!!.id
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetHabitByName() = runBlocking {
        val habit = Habit(
            userId = testUserId,
            name = "Read Book",
            subtitle = "Read 10 pages",
            category = "Education",
            startTime = 0L,
            endTime = 1000L
        )
        habitDao.insertHabit(habit)
        
        val retrievedHabit = habitDao.getHabitByName(testUserId, "Read Book")
        assertNotNull(retrievedHabit)
        assertEquals("Read Book", retrievedHabit?.name)
        assertEquals("Education", retrievedHabit?.category)
    }

    @Test
    @Throws(Exception::class)
    fun getHabitsForUserReturnsFlow() = runBlocking {
        val habit1 = Habit(userId = testUserId, name = "Habit 1", subtitle = "", category = "", startTime = 0L, endTime = 0L)
        val habit2 = Habit(userId = testUserId, name = "Habit 2", subtitle = "", category = "", startTime = 0L, endTime = 0L)
        
        habitDao.insertHabit(habit1)
        habitDao.insertHabit(habit2)
        
        val habitsList = habitDao.getHabitsForUser(testUserId).first()
        assertEquals(2, habitsList.size)
        assertTrue(habitsList.any { it.name == "Habit 1" })
        assertTrue(habitsList.any { it.name == "Habit 2" })
    }

    @Test
    @Throws(Exception::class)
    fun updateHabitCompletion() = runBlocking {
        val habit = Habit(userId = testUserId, name = "Workout", subtitle = "", category = "", startTime = 0L, endTime = 0L)
        habitDao.insertHabit(habit)
        
        val insertedHabit = habitDao.getHabitByName(testUserId, "Workout")
        assertNotNull(insertedHabit)
        
        habitDao.updateHabitCompletion(insertedHabit!!.id, true)
        
        val updatedHabit = habitDao.getHabitById(insertedHabit.id)
        assertNotNull(updatedHabit)
        assertTrue(updatedHabit!!.isCompleted)
    }

    @Test
    @Throws(Exception::class)
    fun deletedHabitIsNotReturned() = runBlocking {
        val habit = Habit(userId = testUserId, name = "Drink Water", subtitle = "", category = "", startTime = 0L, endTime = 0L)
        habitDao.insertHabit(habit)
        
        val insertedHabit = habitDao.getHabitByName(testUserId, "Drink Water")
        assertNotNull(insertedHabit)
        
        // Soft delete (if your app uses deletedAt, or hard delete if it uses @Delete)
        // Since HabitDao has a deleteHabit @Delete method, we will use that.
        habitDao.deleteHabit(insertedHabit!!)
        
        val habits = habitDao.getHabitsForUserOnce(testUserId)
        assertTrue(habits.isEmpty())
    }
}
