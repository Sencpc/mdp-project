package mad.project.mdp_project.model

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.AppDatabase
import mad.project.mdp_project.data.SessionManager
import mad.project.mdp_project.data.remote.AnalyzeResponse
import mad.project.mdp_project.data.remote.RetrofitClient
import mad.project.mdp_project.data.repository.NutritionRepository
import java.io.ByteArrayOutputStream
import java.util.Calendar

/**
 * Holds the intermediate AI prediction that the user reviews before confirming.
 */
data class ScanPreview(
    val foodName: String,
    val calories: Int,
    val mealType: String = "additional"
)

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val sessionManager = SessionManager(application)
    private val userId = sessionManager.getUserId()

    private val repository = NutritionRepository(db.nutritionLogDao(), RetrofitClient.apiService)

    // Step A result: AI prediction awaiting user confirmation
    private val _scanPreview = MutableStateFlow<ScanPreview?>(null)
    val scanPreview: StateFlow<ScanPreview?> = _scanPreview.asStateFlow()

    // Loading states
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _isLogging = MutableStateFlow(false)
    val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()

    // Signals the fragment to navigate back after successful log
    private val _logSuccess = MutableStateFlow(false)
    val logSuccess: StateFlow<Boolean> = _logSuccess.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Today's total calories from confirmed meals
    val todayCalories: StateFlow<Int> = run {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        db.nutritionLogDao().getTodayCalories(userId, cal.timeInMillis)
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
    }

    /**
     * Step A: Compress the bitmap and send to /api/nutrition/analyze.
     * Returns the AI prediction without saving to any database.
     */
    fun scanImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _isScanning.value = true
            _error.value = null
            _scanPreview.value = null

            try {
                // Scale down to max 800x800 while maintaining aspect ratio
                val maxSize = 800
                val scale = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height, 1f)
                val scaledBitmap = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        bitmap,
                        (bitmap.width * scale).toInt(),
                        (bitmap.height * scale).toInt(),
                        true
                    )
                } else {
                    bitmap
                }

                // Compress to JPEG at 80% quality
                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val imageBytes = outputStream.toByteArray()

                val result = repository.analyzeImage(imageBytes)
                result.onSuccess { analysis ->
                    _scanPreview.value = ScanPreview(
                        foodName = analysis.food_name,
                        calories = analysis.calories,
                        mealType = analysis.meal_type ?: "additional"
                    )
                }.onFailure { e ->
                    _error.value = e.message ?: "Failed to analyze food image."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred."
            } finally {
                _isScanning.value = false
            }
        }
    }

    /**
     * Step B: User pressed "Log Meal" — save to backend + Room.
     */
    fun confirmLog() {
        val preview = _scanPreview.value ?: return

        viewModelScope.launch {
            _isLogging.value = true
            _error.value = null

            val result = repository.logMeal(userId, preview.foodName, preview.calories, preview.mealType)
            result.onSuccess {
                _logSuccess.value = true
            }.onFailure { e ->
                _error.value = e.message ?: "Failed to log meal."
            }

            _isLogging.value = false
        }
    }

    /**
     * User pressed "Cancel" — discard the preview without saving.
     */
    fun cancelScan() {
        _scanPreview.value = null
        _error.value = null
        _logSuccess.value = false
    }
}
