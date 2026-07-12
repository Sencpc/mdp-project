package mad.project.mdp_project.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DoctorDao {

    @Query("""
        SELECT * FROM doctors 
        ORDER BY availableTime ASC
    """)
    fun getAvailableDoctors(): Flow<List<DoctorEntity>>

    @Query("""
        SELECT * FROM doctors 
        WHERE category = :category 
        ORDER BY availableTime ASC
    """)
    fun getAvailableDoctorsByCategory(category: String): Flow<List<DoctorEntity>>

    @Query("""
        SELECT * FROM doctors 
        WHERE (doctorName LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%') 
        ORDER BY availableTime ASC
    """)
    fun searchDoctors(query: String): Flow<List<DoctorEntity>>

    @Query("""
        SELECT * FROM doctors 
        WHERE (doctorName LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%') 
        AND category = :category 
        ORDER BY availableTime ASC
    """)
    fun searchDoctorsByCategory(query: String, category: String): Flow<List<DoctorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(doctors: List<DoctorEntity>)

    @Query("SELECT COUNT(*) FROM doctors")
    suspend fun getCount(): Int

    @Query("SELECT * FROM doctors WHERE id = :doctorId")
    suspend fun getDoctorById(doctorId: Int): DoctorEntity?

    /** Used by post-sync facility mapping (suspend, not Flow) */
    @Query("SELECT * FROM doctors")
    suspend fun getAllDoctorsOnce(): List<DoctorEntity>

    /** Update supported facility IDs for a doctor after facility sync */
    @Query("UPDATE doctors SET supportedFacilityIds = :facilityIds WHERE id = :doctorId")
    suspend fun updateSupportedFacilities(doctorId: Int, facilityIds: List<String>)
}
