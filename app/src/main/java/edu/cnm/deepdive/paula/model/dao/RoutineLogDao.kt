package edu.cnm.deepdive.paula.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import edu.cnm.deepdive.paula.model.entity.RoutineLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface RoutineLogDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(routineLog: RoutineLog): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg routineLogs: RoutineLog): List<Long>

    @Update
    suspend fun update(routineLog: RoutineLog): Int

    @Delete
    suspend fun delete(routineLog: RoutineLog): Int

    @Query("SELECT * FROM routine_log WHERE routine_id = :routineId ORDER BY completed_date DESC")
    fun selectForRoutine(routineId: Long): Flow<List<RoutineLog>>

    @Query("SELECT * FROM routine_log WHERE routine_id = :routineId AND completed_date = :completedDate")
    suspend fun findByRoutineAndDate(routineId: Long, completedDate: LocalDate): RoutineLog?

    @Query("SELECT * FROM routine_log ORDER BY completed_at DESC")
    fun selectRecent(): Flow<List<RoutineLog>>

    @Query("SELECT COUNT(*) FROM routine_log WHERE routine_id = :routineId")
    fun getCompletionCount(routineId: Long): Flow<Int>

    @Query("SELECT * FROM routine_log WHERE log_id = :logId")
    fun select(logId: Long): Flow<RoutineLog?>

}
