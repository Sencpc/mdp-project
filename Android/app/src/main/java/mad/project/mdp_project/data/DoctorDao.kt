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
        WHERE availableTime > :minTime 
        ORDER BY availableTime ASC
    """)
    fun getAvailableDoctors(minTime: String): Flow<List<DoctorEntity>>

    @Query("""
        SELECT * FROM doctors 
        WHERE category = :category AND availableTime > :minTime 
        ORDER BY availableTime ASC
    """)
    fun getAvailableDoctorsByCategory(category: String, minTime: String): Flow<List<DoctorEntity>>

    @Query("""
        SELECT * FROM doctors 
        WHERE (doctorName LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%') 
        AND availableTime > :minTime 
        ORDER BY availableTime ASC
    """)
    fun searchDoctors(query: String, minTime: String): Flow<List<DoctorEntity>>

    @Query("""
        SELECT * FROM doctors 
        WHERE (doctorName LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%') 
        AND category = :category 
        AND availableTime > :minTime 
        ORDER BY availableTime ASC
    """)
    fun searchDoctorsByCategory(query: String, category: String, minTime: String): Flow<List<DoctorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(doctors: List<DoctorEntity>)

    @Query("SELECT COUNT(*) FROM doctors")
    suspend fun getCount(): Int

    @Query("SELECT * FROM doctors WHERE id = :doctorId")
    suspend fun getDoctorById(doctorId: Int): DoctorEntity?
}
