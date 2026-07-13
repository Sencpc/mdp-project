package mad.project.mdp_project.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class UserDaoTest {

    private lateinit var userDao: UserDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).allowMainThreadQueries().build()
        userDao = db.userDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetUserByUsername() = runBlocking {
        val user = User(
            username = "testuser",
            password = "password123",
            fullName = "Test User Full"
        )
        userDao.insertUser(user)
        
        val retrievedUser = userDao.getUserByUsername("testuser")
        
        assertNotNull(retrievedUser)
        assertEquals("testuser", retrievedUser?.username)
        assertEquals("password123", retrievedUser?.password)
        assertEquals("Test User Full", retrievedUser?.fullName)
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetUserByIdOnce() = runBlocking {
        // ID is auto-generated, so we just insert and retrieve by username to get the ID, then query by ID.
        val user = User(
            username = "id_test",
            password = "pwd",
            fullName = "ID Test User"
        )
        userDao.insertUser(user)
        
        val insertedUser = userDao.getUserByUsername("id_test")
        assertNotNull(insertedUser)
        
        val id = insertedUser!!.id
        val retrievedUserById = userDao.getUserByIdOnce(id)
        
        assertNotNull(retrievedUserById)
        assertEquals(id, retrievedUserById?.id)
        assertEquals("id_test", retrievedUserById?.username)
    }

    @Test
    @Throws(Exception::class)
    fun updateUser() = runBlocking {
        val user = User(
            username = "update_test",
            password = "old_password",
            fullName = "Old Name"
        )
        userDao.insertUser(user)
        
        val insertedUser = userDao.getUserByUsername("update_test")
        assertNotNull(insertedUser)
        
        val updatedUser = insertedUser!!.copy(
            password = "new_password",
            fullName = "New Name"
        )
        userDao.updateUser(updatedUser)
        
        val retrievedUpdatedUser = userDao.getUserByIdOnce(insertedUser.id)
        assertNotNull(retrievedUpdatedUser)
        assertEquals("new_password", retrievedUpdatedUser?.password)
        assertEquals("New Name", retrievedUpdatedUser?.fullName)
    }

    @Test
    @Throws(Exception::class)
    fun getNonExistentUserReturnsNull() = runBlocking {
        val user = userDao.getUserByUsername("does_not_exist")
        assertNull(user)
    }
}
