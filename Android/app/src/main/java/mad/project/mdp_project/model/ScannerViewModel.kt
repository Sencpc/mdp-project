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
import mad.project.mdp_project.data.NutritionLog
import mad.project.mdp_project.data.SessionManager
import mad.project.mdp_project.data.remote.RetrofitClient
import mad.project.mdp_project.data.repository.NutritionRepository
import java.io.ByteArrayOutputStream
import java.util.Calendar

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val sessionManager = SessionManager(application)
    private val userId = sessionManager.getUserId()

    private val repository = NutritionRepository(db.nutritionLogDao(), RetrofitClient.apiService)

    private val _scanResult = MutableStateFlow<NutritionLog?>(null)
    val scanResult: StateFlow<NutritionLog?> = _scanResult.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Today's total calories from scanned food
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
     * Compress a Bitmap and send it to the backend AI scanner.
     * The bitmap should already be cropped to the targeting square.
     */
    fun scanImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _isScanning.value = true
            _error.value = null
            _scanResult.value = null

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

                val result = repository.scanImage(userId, imageBytes)
                result.onSuccess { log ->
                    _scanResult.value = log
                }.onFailure { e ->
                    _error.value = e.message ?: "Failed to scan food image."
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred."
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun clearResult() {
        _scanResult.value = null
        _error.value = null
    }
}
