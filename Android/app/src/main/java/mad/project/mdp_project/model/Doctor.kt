package mad.project.mdp_project.model

data class Doctor(
    val id: Int,
    val name: String,
    val specialty: String,
    val rating: Float,
    val reviewsCount: Int,
    val description: String,
    val nextAvailable: String,
    val isHighlyRecommended: Boolean = false
)
