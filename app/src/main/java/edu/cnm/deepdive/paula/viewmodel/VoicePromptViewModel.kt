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
import edu.cnm.deepdive.paula.model.entity.VoicePrompt
import edu.cnm.deepdive.paula.service.VoicePromptRepository
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing Paula's companion voice prompts, video selections, and playback state.
 */
@HiltViewModel
class VoicePromptViewModel @Inject constructor(
    private val voicePromptRepository: VoicePromptRepository
) : ViewModel() {

    private val _selectedPrompt = MutableStateFlow<VoicePrompt?>(null)
    val selectedPrompt: StateFlow<VoicePrompt?> = _selectedPrompt.asStateFlow()

    private val _throwable = MutableLiveData<Throwable>()
    val throwable: LiveData<Throwable> get() = _throwable

    val prompts: StateFlow<List<VoicePrompt>> = voicePromptRepository.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            try {
                voicePromptRepository.ensureDefaultPrompts()
            } catch (e: Throwable) {
                _throwable.postValue(e)
            }
        }
    }

    fun selectPrompt(prompt: VoicePrompt) {
        _selectedPrompt.value = prompt
    }

    fun selectRandomPrompt() {
        val currentList = prompts.value
        if (currentList.isNotEmpty()) {
            val random = currentList.random()
            _selectedPrompt.value = random
        }
    }

    fun markPlayed(prompt: VoicePrompt) {
        viewModelScope.launch {
            try {
                voicePromptRepository.markPlayed(prompt)
            } catch (e: Throwable) {
                _throwable.postValue(e)
            }
        }
    }

    fun getAudioResourceId(prompt: VoicePrompt): Int =
        voicePromptRepository.getAudioResourceId(prompt.audioFileName)

    fun getVideoResourceId(prompt: VoicePrompt): Int =
        voicePromptRepository.getVideoResourceId(prompt.triggerEvent)

}
