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

import edu.cnm.deepdive.paula.model.dao.TaskDao
import edu.cnm.deepdive.paula.model.entity.Task
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Service repository providing access to task CRUD operations and state changes.
 */
@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {

    fun get(taskId: Long): Flow<Task?> =
        taskDao.select(taskId)

    fun getAll(): Flow<List<Task>> =
        taskDao.selectAll()

    fun getByUser(userId: Long): Flow<List<Task>> =
        taskDao.selectByUser(userId)

    fun getPendingByUser(userId: Long): Flow<List<Task>> =
        taskDao.selectPendingByUser(userId)

    fun getCompletedByUser(userId: Long): Flow<List<Task>> =
        taskDao.selectCompletedByUser(userId)

    suspend fun findById(taskId: Long): Task? =
        taskDao.findById(taskId)

    suspend fun save(task: Task): Task {
        return if (task.id == 0L) {
            val id = taskDao.insert(task)
            task.copy(id = id)
        } else {
            taskDao.update(task)
            task
        }
    }

    suspend fun toggleComplete(task: Task): Task {
        val completed = !task.isCompleted
        val updated = task.copy(
            isCompleted = completed,
            completedAt = if (completed) Instant.now() else null
        )
        taskDao.update(updated)
        return updated
    }

    suspend fun remove(task: Task): Int =
        taskDao.delete(task)

}
