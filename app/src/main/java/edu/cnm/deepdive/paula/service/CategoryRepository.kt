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

import edu.cnm.deepdive.paula.model.dao.CategoryDao
import edu.cnm.deepdive.paula.model.entity.Category
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow

/**
 * Service repository providing access to chore and routine category records.
 */
@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {

    fun getAll(): Flow<List<Category>> =
        categoryDao.selectAll()

    fun get(categoryId: Long): Flow<Category?> =
        categoryDao.select(categoryId)

    suspend fun save(category: Category): Category {
        return if (category.id == 0L) {
            val id = categoryDao.insert(category)
            category.copy(id = id)
        } else {
            categoryDao.update(category)
            category
        }
    }

    suspend fun remove(category: Category): Int =
        categoryDao.delete(category)

}
