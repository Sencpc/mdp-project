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

    /**
     * User Conflict: Prevents the SAME USER from having two consultations
     * at the exact same date/time.
     *
     * Scoped to userId because different users on the same device
     * should not block each other.
     */
    @Query("""
        SELECT COUNT(*) FROM consultations 
        WHERE userId = :userId
        AND consultationTime = :dateTime 
        AND status = 'Upcoming'
    """)
    suspend fun countUserConflicts(userId: Int, dateTime: String): Int

    /**
     * Doctor Conflict: Prevents booking the SAME DOCTOR at the same time,
     * regardless of facility.
     *
     * Business rule: A doctor cannot be in two places at once.
     * Therefore the conflict is scoped to doctorId + dateTime only,
     * NOT doctorId + facility + dateTime.
     */
    @Query("""
        SELECT COUNT(*) FROM consultations 
        WHERE doctorId = :doctorId 
        AND consultationTime = :dateTime 
        AND status = 'Upcoming'
    """)
    suspend fun countDoctorConflicts(doctorId: Int, dateTime: String): Int
}
