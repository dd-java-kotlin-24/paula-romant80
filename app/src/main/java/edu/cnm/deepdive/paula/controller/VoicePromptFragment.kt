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
package edu.cnm.deepdive.paula.controller

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import edu.cnm.deepdive.paula.R
import edu.cnm.deepdive.paula.databinding.FragmentVoicePromptBinding
import edu.cnm.deepdive.paula.model.entity.VoicePrompt
import edu.cnm.deepdive.paula.viewmodel.RoutineViewModel
import edu.cnm.deepdive.paula.viewmodel.VoicePromptViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * Controller for Paula's companion avatar screen, managing audio affirmations,
 * video playback, outfit customizations, and live habit streak celebrations.
 */
@AndroidEntryPoint
class VoicePromptFragment : Fragment() {

    private var _binding: FragmentVoicePromptBinding? = null
    private val binding get() = _binding!!

    private val voicePromptViewModel: VoicePromptViewModel by viewModels()
    private val routineViewModel: RoutineViewModel by activityViewModels()

    private var mediaPlayer: MediaPlayer? = null
    private var isAudioPlaying = false
    private var isVideoPlaying = false
    private var activePrompt: VoicePrompt? = null

    private val sharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVoicePromptBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyPreferences()
        setupListeners()
        observeViewModels()
    }

    override fun onResume() {
        super.onResume()
        applyPreferences()
    }

    private fun applyPreferences() {
        // Outfit customization
        val outfit = sharedPreferences.getString("avatar_outfit", "classic")
        val outfitDrawable = when (outfit) {
            "striped" -> R.drawable.paula_avatar_grey_hoodie
            "sweater" -> R.drawable.paula_avatar_pastel_pink
            "summer" -> R.drawable.paula_avatar_vibrant_magenta
            else -> R.drawable.paula_avatar_purple_hoodie
        }
        binding.avatarImage.setImageResource(outfitDrawable)

        // Captions visibility preference
        val captionsEnabled = sharedPreferences.getBoolean("captions_enabled", true)
        binding.textSpeechBubble.isVisible = captionsEnabled
    }

    private fun setupListeners() {
        binding.buttonPlayVoice.setOnClickListener {
            if (isAudioPlaying) {
                stopAudio()
            } else {
                val prompt = activePrompt ?: voicePromptViewModel.prompts.value.firstOrNull()
                if (prompt != null) {
                    playAudio(prompt)
                }
            }
        }

        binding.buttonTutorial.setOnClickListener {
            if (isVideoPlaying) {
                stopVideo()
            } else {
                val prompt = activePrompt ?: voicePromptViewModel.prompts.value.firstOrNull()
                if (prompt != null) {
                    playVideo(prompt)
                }
            }
        }

        binding.avatarCard.setOnClickListener {
            voicePromptViewModel.selectRandomPrompt()
        }
    }

    private fun observeViewModels() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    voicePromptViewModel.prompts.collect { prompts ->
                        if (activePrompt == null && prompts.isNotEmpty()) {
                            activePrompt = prompts.first()
                            updatePromptDisplay(prompts.first())
                        }
                    }
                }
                launch {
                    voicePromptViewModel.selectedPrompt.collect { prompt ->
                        if (prompt != null) {
                            activePrompt = prompt
                            updatePromptDisplay(prompt)
                            if (isAudioPlaying) {
                                playAudio(prompt)
                            } else if (isVideoPlaying) {
                                playVideo(prompt)
                            }
                        }
                    }
                }
                launch {
                    routineViewModel.routines.collect { routines ->
                        updateStreakDisplay(routines)
                    }
                }
            }
        }
    }

    private fun updatePromptDisplay(prompt: VoicePrompt) {
        binding.textSpeechBubble.text = "\"${prompt.transcript}\""
        if (!isAudioPlaying && !isVideoPlaying) {
            binding.textAudioStatus.setText(R.string.prompt_audio_subtitle)
        }
    }

    private fun updateStreakDisplay(routines: List<edu.cnm.deepdive.paula.model.entity.Routine>) {
        val today = LocalDate.now()
        val bestCurrentStreak = routines.maxOfOrNull { it.currentStreak } ?: 0
        val completedToday = routines.count { it.lastCompletedDate == today }
        val totalRoutines = routines.size

        if (bestCurrentStreak > 0) {
            binding.textStreakTitle.text = getString(R.string.streak_active_title, bestCurrentStreak)
        } else {
            binding.textStreakTitle.setText(R.string.streak_none_title)
        }

        binding.textStreakDetail.text = if (totalRoutines > 0) {
            getString(R.string.streak_habits_completed, completedToday, totalRoutines)
        } else {
            getString(R.string.streak_habits_zero)
        }
    }

    private fun isQuietHoursActive(): Boolean {
        val quietHoursEnabled = sharedPreferences.getBoolean("quiet_hours_enabled", true)
        if (!quietHoursEnabled) return false
        val now = LocalTime.now()
        return now.isAfter(LocalTime.of(21, 59)) || now.isBefore(LocalTime.of(7, 0))
    }

    private fun playAudio(prompt: VoicePrompt) {
        if (isVideoPlaying) {
            stopVideo()
        }
        stopAudio()

        val voiceEnabled = sharedPreferences.getBoolean("voice_enabled", true)
        if (!voiceEnabled) {
            binding.textAudioStatus.setText(R.string.status_voice_disabled)
            return
        }

        if (isQuietHoursActive()) {
            binding.textAudioStatus.setText(R.string.status_quiet_hours)
            return
        }

        val rawResId = voicePromptViewModel.getAudioResourceId(prompt)
        mediaPlayer = MediaPlayer.create(requireContext(), rawResId).apply {
            setOnCompletionListener {
                onAudioComplete()
            }
            start()
        }

        isAudioPlaying = true
        binding.buttonPlayVoice.setText(R.string.action_stop_voice)
        binding.buttonPlayVoice.setIconResource(R.drawable.ic_stop)
        binding.textAudioStatus.setText(R.string.status_playing_voice)
        voicePromptViewModel.markPlayed(prompt)
    }

    private fun stopAudio() {
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.stop()
                }
            } catch (ignored: IllegalStateException) {}
            it.release()
        }
        mediaPlayer = null
        isAudioPlaying = false
        binding.buttonPlayVoice.setText(R.string.action_play_greeting)
        binding.buttonPlayVoice.setIconResource(R.drawable.ic_play_arrow)
        if (!isVideoPlaying) {
            binding.textAudioStatus.setText(R.string.prompt_audio_subtitle)
        }
    }

    private fun onAudioComplete() {
        stopAudio()
    }

    private fun playVideo(prompt: VoicePrompt) {
        if (isAudioPlaying) {
            stopAudio()
        }
        stopVideo()

        if (isQuietHoursActive()) {
            binding.textAudioStatus.setText(R.string.status_quiet_hours)
            return
        }

        val rawVideoId = voicePromptViewModel.getVideoResourceId(prompt)
        val uri = Uri.parse("android.resource://${requireContext().packageName}/$rawVideoId")

        binding.avatarImage.isVisible = false
        binding.avatarVideo.isVisible = true
        binding.avatarVideo.setVideoURI(uri)
        binding.avatarVideo.setOnCompletionListener {
            onVideoComplete()
        }
        binding.avatarVideo.start()

        isVideoPlaying = true
        binding.buttonTutorial.setText(R.string.action_stop_voice)
        binding.buttonTutorial.setIconResource(R.drawable.ic_stop)
        binding.textAudioStatus.setText(R.string.status_playing_video)
        voicePromptViewModel.markPlayed(prompt)
    }

    private fun stopVideo() {
        if (binding.avatarVideo.isPlaying) {
            binding.avatarVideo.stopPlayback()
        }
        binding.avatarVideo.isVisible = false
        binding.avatarImage.isVisible = true
        isVideoPlaying = false
        binding.buttonTutorial.setText(R.string.action_watch_tutorial)
        binding.buttonTutorial.setIconResource(R.drawable.ic_ondemand_video)
        if (!isAudioPlaying) {
            binding.textAudioStatus.setText(R.string.prompt_audio_subtitle)
        }
    }

    private fun onVideoComplete() {
        stopVideo()
    }

    override fun onStop() {
        super.onStop()
        stopAudio()
        stopVideo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopAudio()
        stopVideo()
        _binding = null
    }

}
