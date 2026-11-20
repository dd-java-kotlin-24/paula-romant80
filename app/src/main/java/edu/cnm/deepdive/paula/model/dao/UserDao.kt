/*
 *  Copyright 2026 CNM Ingenuity, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.cnm.deepdive.paula.model.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import edu.cnm.deepdive.paula.model.entity.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(user: User): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg users: User): List<Long>

    @Update
    suspend fun update(user: User): Int

    @Delete
    suspend fun delete(user: User): Int

    @Query("SELECT * FROM user_profile WHERE user_id = :userId")
    fun select(userId: Long): Flow<User?>

    @Query("SELECT * FROM user_profile WHERE oauth_key = :oauthKey")
    fun selectByOauthKey(oauthKey: String): Flow<User?>

    @Query("SELECT * FROM user_profile WHERE user_id = :userId")
    suspend fun findById(userId: Long): User?

    @Query("SELECT * FROM user_profile WHERE oauth_key = :oauthKey")
    suspend fun findByOauthKey(oauthKey: String): User?

    @Query("SELECT * FROM user_profile ORDER BY display_name ASC")
    fun selectAll(): Flow<List<User>>

}
