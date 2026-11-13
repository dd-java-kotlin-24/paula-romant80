package edu.cnm.deepdive.paula.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import edu.cnm.deepdive.paula.model.entity.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg categories: Category): List<Long>

    @Update
    suspend fun update(category: Category): Int

    @Delete
    suspend fun delete(category: Category): Int

    @Query("SELECT * FROM category ORDER BY name ASC")
    fun selectAll(): Flow<List<Category>>

    @Query("SELECT * FROM category WHERE category_id = :categoryId")
    fun select(categoryId: Long): Flow<Category?>

}