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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.cnm.deepdive.paula.model.entity.User
import edu.cnm.deepdive.paula.service.UserRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing active user account session and multi-user profile switching.
 */
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _throwable = MutableLiveData<Throwable>()
    val throwable: LiveData<Throwable> get() = _throwable

    val users: StateFlow<List<User>> = userRepository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun setCurrentUser(user: User?) {
        _currentUser.value = user
    }

    fun loadOrCreate(oauthKey: String, displayName: String, onLoaded: ((User) -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val user = userRepository.getOrCreate(oauthKey, displayName)
                _currentUser.value = user
                onLoaded?.invoke(user)
            } catch (e: Throwable) {
                _throwable.postValue(e)
            }
        }
    }

    fun deleteUser(user: User, fallback: User?, onDeleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                userRepository.remove(user)
                if (_currentUser.value?.id == user.id && fallback != null) {
                    _currentUser.value = fallback
                }
                onDeleted?.invoke()
            } catch (e: Throwable) {
                _throwable.postValue(e)
            }
        }
    }

}
