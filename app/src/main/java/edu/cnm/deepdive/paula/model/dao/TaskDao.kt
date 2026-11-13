package edu.cnm.deepdive.paula.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import edu.cnm.deepdive.paula.model.entity.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(task: Task): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg tasks: Task): List<Long>

    @Update
    suspend fun update(task: Task): Int

    @Delete
    suspend fun delete(task: Task): Int

    @Query("SELECT * FROM task ORDER BY is_completed ASC, due_date ASC, priority DESC")
    fun selectAll(): Flow<List<Task>>

    @Query("SELECT * FROM task WHERE is_completed = 0 ORDER BY due_date ASC, priority DESC")
    fun selectPending(): Flow<List<Task>>

    @Query("SELECT * FROM task WHERE is_completed = 1 ORDER BY completed_at DESC")
    fun selectCompleted(): Flow<List<Task>>

    @Query("SELECT * FROM task WHERE category_id = :categoryId ORDER BY is_completed ASC, due_date ASC")
    fun selectByCategory(categoryId: Long): Flow<List<Task>>

    @Query("SELECT * FROM task WHERE task_id = :taskId")
    fun select(taskId: Long): Flow<Task?>

    @Query("SELECT * FROM task WHERE task_id = :taskId")
    suspend fun findById(taskId: Long): Task?

}
