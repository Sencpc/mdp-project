package mad.project.mdp_project.model

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mad.project.mdp_project.data.User
import mad.project.mdp_project.data.UserDao
import mad.project.mdp_project.data.remote.ApiService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var userDao: UserDao
    private lateinit var apiService: ApiService
    private lateinit var viewModel: ProfileViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userDao = mockk()
        apiService = mockk()
        
        val mockUser = User(
            id = 1,
            username = "profileuser",
            password = "password",
            fullName = "Profile User"
        )
        
        // Mock the dao methods that are called during initialization
        coEvery { userDao.getUserByIdOnce(1) } returns mockUser
        coEvery { userDao.getUserById(1) } returns flowOf(mockUser)
        
        viewModel = ProfileViewModel(userDao, 1, apiService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `calculateAge returns correct age`() {
        val calendar = Calendar.getInstance()
        
        // Let's say birthdate is exactly 20 years ago today
        calendar.add(Calendar.YEAR, -20)
        val birthDateMs = calendar.timeInMillis
        
        val age = viewModel.calculateAge(birthDateMs)
        
        assertEquals(20, age)
    }

    @Test
    fun `updateDraft modifies draftUser but not user flow immediately`() = runTest {
        // Let initialization finish
        testDispatcher.scheduler.advanceUntilIdle()
        
        val initialDraft = viewModel.draftUser.value
        assertEquals("Profile User", initialDraft?.fullName)
        
        // Update draft
        viewModel.updateDraft { it.copy(fullName = "Updated Name") }
        
        // Check draft is updated
        val updatedDraft = viewModel.draftUser.value
        assertEquals("Updated Name", updatedDraft?.fullName)
    }
}
