package mad.project.mdp_project.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsultationDao {

    @Query("""
        SELECT * FROM consultations 
        WHERE status = 'Upcoming' 
        ORDER BY consultationTime ASC
    """)
    fun getUpcomingConsultations(): Flow<List<ConsultationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsultation(consultation: ConsultationEntity): Long

    @Query("""
        SELECT * FROM consultations 
        WHERE status = 'Completed' 
        ORDER BY consultationTime DESC
    """)
    fun getCompletedConsultations(): Flow<List<ConsultationEntity>>

    @Query("SELECT * FROM consultations ORDER BY consultationTime DESC")
    fun getAllConsultations(): Flow<List<ConsultationEntity>>

    @Query("UPDATE consultations SET status = 'Completed' WHERE status = 'Upcoming' AND consultationTime < :now")
    suspend fun markPastConsultationsCompleted(now: String)
}
