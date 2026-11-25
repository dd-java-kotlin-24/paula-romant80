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
import edu.cnm.deepdive.paula.model.entity.Routine
import edu.cnm.deepdive.paula.service.CategoryRepository
import edu.cnm.deepdive.paula.service.RoutineRepository
import jakarta.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel managing routine habits, streak progression, and active user scoping.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RoutineViewModel @Inject constructor(
    private val routineRepository: RoutineRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _userId = MutableStateFlow(0L)
    val userId: StateFlow<Long> = _userId.asStateFlow()

    private val _throwable = MutableLiveData<Throwable>()
    val throwable: LiveData<Throwable> get() = _throwable

    val editingRoutineId: Long = savedStateHandle["routineId"] ?: 0L

    val categories: StateFlow<List<Category>> = categoryRepository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val routines: StateFlow<List<Routine>> = _userId
        .flatMapLatest { id ->
            if (id > 0L) {
                routineRepository.getActiveByUser(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val editingRoutine: StateFlow<Routine?> = if (editingRoutineId > 0L) {
        routineRepository.get(editingRoutineId)
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

    fun save(routine: Routine, onComplete: ((Routine) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val saved = routineRepository.save(routine)
                onComplete?.invoke(saved)
            } catch (e: Throwable) {
                _throwable.postValue(e)
            }
        }
    }

    fun recordCompletion(
        routine: Routine,
        notes: String? = null,
        photoUri: String? = null,
        onComplete: ((Routine) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val updated = routineRepository.recordCompletion(
                    routine = routine,
                    notes = notes,
                    photoUri = photoUri
                )
                onComplete?.invoke(updated)
            } catch (e: Throwable) {
                _throwable.postValue(e)
            }
        }
    }

    fun removeCompletion(
        routine: Routine,
        onComplete: ((Routine) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val updated = routineRepository.removeCompletion(routine)
                onComplete?.invoke(updated)
            } catch (e: Throwable) {
                _throwable.postValue(e)
            }
        }
    }

    fun toggleCompletion(
        routine: Routine,
        onComplete: ((Routine) -> Unit)? = null
    ) {
        val today = LocalDate.now()
        if (routine.lastCompletedDate == today) {
            removeCompletion(routine, onComplete)
        } else {
            recordCompletion(routine, onComplete = onComplete)
        }
    }

    fun toggleActive(routine: Routine) {
        viewModelScope.launch {
            try {
                val updated = routine.copy(isActive = !routine.isActive)
                routineRepository.save(updated)
            } catch (e: Throwable) {
                _throwable.postValue(e)
            }
        }
    }

    fun remove(routine: Routine) {
        viewModelScope.launch {
            try {
                routineRepository.remove(routine)
            } catch (e: Throwable) {
                _throwable.postValue(e)
            }
        }
    }

}
