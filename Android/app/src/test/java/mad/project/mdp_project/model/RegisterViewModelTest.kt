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
class RegisterViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    
    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mockk()
        viewModel = RegisterViewModel(userRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `register with empty fields emits failure result`() = runTest {
        viewModel.register("", "", "")
        
        val result = viewModel.registerResult.value
        assertTrue(result?.isFailure == true)
        assertEquals("Harap isi semua field", result?.exceptionOrNull()?.message)
    }

    @Test
    fun `register with short password emits failure result`() = runTest {
        viewModel.register("Full Name", "username", "short")
        
        val result = viewModel.registerResult.value
        assertTrue(result?.isFailure == true)
        assertEquals("Password minimal 8 karakter", result?.exceptionOrNull()?.message)
    }

    @Test
    fun `register with valid credentials emits success result`() = runTest {
        val mockUser = User(
            id = 1,
            username = "newuser",
            password = "password123",
            fullName = "New User"
        )
        
        coEvery { userRepository.register("newuser", "password123", "New User") } returns Result.success(mockUser)

        viewModel.register("New User", "newuser", "password123")
        
        testDispatcher.scheduler.advanceUntilIdle()
        
        val result = viewModel.registerResult.value
        assertTrue(result?.isSuccess == true)
        assertEquals(mockUser, result?.getOrNull())
    }
}
