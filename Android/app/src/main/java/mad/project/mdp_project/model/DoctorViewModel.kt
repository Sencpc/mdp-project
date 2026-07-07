package mad.project.mdp_project.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import mad.project.mdp_project.data.remote.RetrofitClient

class DoctorViewModel(application: Application) : AndroidViewModel(application) {

    private val _doctors = MutableLiveData<List<Doctor>>()
    val doctors: LiveData<List<Doctor>> = _doctors

    private val _filteredDoctors = MutableLiveData<List<Doctor>>()
    val filteredDoctors: LiveData<List<Doctor>> = _filteredDoctors

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var currentFilter: String = "All"
    private var currentQuery: String = ""

    init {
        loadDoctors()
    }

    /**
     * Load daftar dokter — dari data statis karena ini bukan API operasi CRUD,
     * tapi untuk demo penggunaan ViewModel + LiveData yang proper.
     */
    private fun loadDoctors() {
        _isLoading.value = true
        viewModelScope.launch {
            // Data dokter (bisa diganti dengan API call nanti)
            val doctorList = listOf(
                Doctor(
                    1, "Dr. Sarah Jenkins", "General Practice", 4.9f, 128,
                    "Experienced general practitioner focusing on holistic health and preventative care for adults...",
                    "Today, 2:00 PM", true
                ),
                Doctor(
                    2, "Dr. Marcus Chen", "Therapy", 4.8f, 95,
                    "Specializing in cognitive behavioral therapy and stress management techniques...",
                    "Tomorrow, 10:00 AM"
                ),
                Doctor(
                    3, "Dr. Elena Rodriguez", "Nutrition", 4.7f, 150,
                    "Expert in clinical nutrition and metabolic health, helping patients achieve sustainable weight goals...",
                    "Monday, 3:30 PM"
                ),
                Doctor(
                    4, "Dr. James Park", "General Practice", 4.6f, 87,
                    "Board-certified family medicine physician with focus on chronic disease management...",
                    "Tuesday, 9:00 AM"
                ),
                Doctor(
                    5, "Dr. Lisa Wang", "Therapy", 4.9f, 203,
                    "Licensed psychologist specializing in anxiety disorders, trauma, and mindfulness-based therapy...",
                    "Wednesday, 1:00 PM", true
                )
            )
            _doctors.value = doctorList
            _filteredDoctors.value = doctorList
            _isLoading.value = false
        }
    }

    fun setFilter(filter: String) {
        currentFilter = filter
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        currentQuery = query
        applyFilters()
    }

    private fun applyFilters() {
        val allDoctors = _doctors.value ?: return
        val filtered = allDoctors.filter { doctor ->
            val matchesQuery = doctor.name.lowercase().contains(currentQuery.lowercase()) ||
                    doctor.specialty.lowercase().contains(currentQuery.lowercase())

            val matchesCategory = when (currentFilter) {
                "General Practice" -> doctor.specialty == "General Practice"
                "Therapy" -> doctor.specialty == "Therapy"
                "Nutrition" -> doctor.specialty == "Nutrition"
                else -> true // "All"
            }

            matchesQuery && matchesCategory
        }
        _filteredDoctors.value = filtered
    }
}
