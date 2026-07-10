package mad.project.mdp_project.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for healthcare facility data cached from SATUSEHAT MSI API.
 */
@Dao
interface FacilityDao {

    @Query("""
        SELECT * FROM facilities 
        WHERE jenisSaranaKode IN ('103', '104') 
        AND operasional = 1 
        AND statusAktif = 1 
        ORDER BY nama ASC
    """)
    fun getActiveFacilities(): Flow<List<FacilityEntity>>

    @Query("""
        SELECT * FROM facilities 
        WHERE nama LIKE '%' || :query || '%' 
        AND operasional = 1 
        AND statusAktif = 1 
        ORDER BY nama ASC
    """)
    fun searchFacilities(query: String): Flow<List<FacilityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(facilities: List<FacilityEntity>)

    @Query("SELECT COUNT(*) FROM facilities")
    suspend fun getCount(): Int

    @Query("SELECT MAX(lastSyncedAt) FROM facilities")
    suspend fun getLastSyncTime(): Long?

    @Query("DELETE FROM facilities")
    suspend fun deleteAll()
}
