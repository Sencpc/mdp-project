package mad.project.mdp_project.data.remote

data class DoctorListResponse(
    val success: Boolean,
    val data: List<DoctorResponseDto>,
    val message: String? = null
)

data class DoctorResponseDto(
    val id: Int,
    val satusehatId: String,
    val name: String,
    val displayName: String?,
    val gender: String?,
    val birthDate: String?,
    val city: String?,
    val category: String,
    val description: String?,
    val rating: Double,
    val profileIcon: String,
    val availableTime: Long
)
