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
package edu.cnm.deepdive.paula.service

import edu.cnm.deepdive.paula.model.dao.UserDao
import edu.cnm.deepdive.paula.model.entity.User
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Service repository providing access to user profile persistence operations.
 */
@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {

    fun get(userId: Long): Flow<User?> =
        userDao.select(userId)

    fun getByOauthKey(oauthKey: String): Flow<User?> =
        userDao.selectByOauthKey(oauthKey)

    fun getAll(): Flow<List<User>> =
        userDao.selectAll()

    suspend fun findById(userId: Long): User? =
        userDao.findById(userId)

    suspend fun getOrCreate(oauthKey: String, displayName: String): User {
        return userDao.findByOauthKey(oauthKey) ?: run {
            val newUser = User(
                oauthKey = oauthKey,
                displayName = displayName,
                createdAt = Instant.now()
            )
            val id = userDao.insert(newUser)
            newUser.copy(id = id)
        }
    }

    suspend fun save(user: User): User {
        return if (user.id == 0L) {
            val id = userDao.insert(user)
            user.copy(id = id)
        } else {
            userDao.update(user)
            user
        }
    }

    suspend fun remove(user: User): Int =
        userDao.delete(user)

}
