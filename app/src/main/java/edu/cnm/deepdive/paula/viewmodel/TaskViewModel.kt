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
package edu.cnm.deepdive.paula.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.cnm.deepdive.paula.model.entity.Category
import edu.cnm.deepdive.paula.model.entity.Task
import edu.cnm.deepdive.paula.service.CategoryRepository
import edu.cnm.deepdive.paula.service.TaskRepository
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing task presentation state, active user scoping, and CRUD events.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _userId = MutableStateFlow(0L)
    val userId: StateFlow<Long> = _userId.asStateFlow()

    private val _throwable = MutableLiveData<Throwable>()
    val throwable: LiveData<Throwable> get() = _throwable

    val editingTaskId: Long = savedStateHandle["taskId"] ?: 0L

    val categories: StateFlow<List<Category>> = categoryRepository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val tasks: StateFlow<List<Task>> = _userId
        .flatMapLatest { id ->
            if (id > 0L) {
                taskRepository.getByUser(id)
            } else {
                taskRepository.getAll()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val editingTask: StateFlow<Task?> = if (editingTaskId > 0L) {
        taskRepository.get(editingTaskId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null
            )
    } else {
        MutableStateFlow(null)
    }

    fun setUserId(id: Long) {
        _userId.value = id
    }

    fun save(task: Task, onComplete: ((Task) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val saved = taskRepository.save(task)
                onComplete?.invoke(saved)
            } catch (e: Throwable) {
                _throwable.postValue(e)
            }
        }
    }

    fun toggleComplete(task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.toggleComplete(task)
            } catch (e: Throwable) {
                _throwable.postValue(e)
            }
        }
    }

    fun remove(task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.remove(task)
            } catch (e: Throwable) {
                _throwable.postValue(e)
            }
        }
    }

    fun createCategory(name: String, onCreated: (Category) -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            try {
                val saved = categoryRepository.save(
                    Category(name = trimmed, colorHex = "#6750A4")
                )
                onCreated(saved)
            } catch (e: Throwable) {
                _throwable.postValue(e)
            }
        }
    }

}
