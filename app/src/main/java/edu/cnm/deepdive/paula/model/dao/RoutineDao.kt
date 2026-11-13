package edu.cnm.deepdive.paula.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import edu.cnm.deepdive.paula.model.entity.Routine
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(routine: Routine): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg routines: Routine): List<Long>

    @Update
    suspend fun update(routine: Routine): Int

    @Delete
    suspend fun delete(routine: Routine): Int

    @Query("SELECT * FROM routine ORDER BY name ASC")
    fun selectAll(): Flow<List<Routine>>

    @Query("SELECT * FROM routine WHERE is_active = 1 ORDER BY name ASC")
    fun selectActive(): Flow<List<Routine>>

    @Query("SELECT * FROM routine WHERE routine_id = :routineId")
    fun select(routineId: Long): Flow<Routine?>

    @Query("SELECT * FROM routine WHERE routine_id = :routineId")
    suspend fun findById(routineId: Long): Routine?

    @Query("SELECT * FROM routine WHERE category_id = :categoryId ORDER BY name ASC")
    fun selectByCategory(categoryId: Long): Flow<List<Routine>>

}
