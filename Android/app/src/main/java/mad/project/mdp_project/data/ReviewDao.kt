package mad.project.mdp_project.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for user-submitted consultation reviews.
 */
@Dao
interface ReviewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)

    @Query("SELECT * FROM reviews WHERE doctorId = :doctorId ORDER BY createdAt DESC")
    fun getReviewsForDoctor(doctorId: Int): Flow<List<ReviewEntity>>

    @Query("SELECT AVG(rating) FROM reviews WHERE doctorId = :doctorId")
    fun getAverageRating(doctorId: Int): Flow<Float?>

    @Query("SELECT COUNT(*) FROM reviews WHERE consultationId = :consultationId")
    suspend fun hasReview(consultationId: Int): Int
}
