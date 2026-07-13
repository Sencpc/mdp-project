package mad.project.mdp_project.model

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mad.project.mdp_project.data.User
import mad.project.mdp_project.data.repository.UserRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    // Swaps the background executor used by the Architecture Components with a different one which executes each task synchronously.
    @get:Rule
    val instantExecutorRule: InstantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk()
        viewModel = LoginViewModel(userRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login with empty fields emits failure result`() = runTest {
        viewModel.login("", "")
        
        val result = viewModel.loginResult.value
        assertTrue(result?.isFailure == true)
        assertEquals("Harap isi semua field", result?.exceptionOrNull()?.message)
    }

    @Test
    fun `login with valid credentials emits success result`() = runTest {
        val mockUser = User(
            id = 1,
            username = "testuser",
            password = "password",
            fullName = "Test User"
        )
        
        // Mock the repository behavior
        coEvery { userRepository.login("testuser", "password") } returns Result.success(mockUser)

        viewModel.login("testuser", "password")
        
        // Advance coroutines since we use viewModelScope.launch
        testDispatcher.scheduler.advanceUntilIdle()
        
        val result = viewModel.loginResult.value
        assertTrue(result?.isSuccess == true)
        assertEquals(mockUser, result?.getOrNull())
    }
    
    @Test
    fun `login sets isLoading to true and then false`() = runTest {
        val mockUser = User(
            id = 1,
            username = "testuser",
            password = "password",
            fullName = "Test User"
        )
        
        coEvery { userRepository.login("testuser", "password") } returns Result.success(mockUser)
        
        viewModel.login("testuser", "password")
        
        // Before coroutines finish, isLoading should be true
        assertTrue(viewModel.isLoading.value == true)
        
        // Let the coroutine finish
        testDispatcher.scheduler.advanceUntilIdle()
        
        // After finishing, isLoading should be false
        assertTrue(viewModel.isLoading.value == false)
    }
}
