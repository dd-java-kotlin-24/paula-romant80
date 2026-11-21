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
package edu.cnm.deepdive.paula.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.cnm.deepdive.paula.R
import edu.cnm.deepdive.paula.databinding.ItemRoutineCardBinding
import edu.cnm.deepdive.paula.model.entity.Routine
import java.time.LocalDate

/**
 * ListAdapter presenting Routine habits with streak counters, frequency tags, and daily check-ins.
 */
class RoutineAdapter(
    private val onRoutineClick: (Routine) -> Unit,
    private val onCheckInClick: (Routine) -> Unit
) : ListAdapter<Routine, RoutineAdapter.RoutineViewHolder>(RoutineDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutineViewHolder {
        val binding = ItemRoutineCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RoutineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoutineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RoutineViewHolder(
        private val binding: ItemRoutineCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(routine: Routine) {
            val context = binding.root.context
            val today = LocalDate.now()
            val isDoneToday = routine.lastCompletedDate == today

            binding.routineName.text = routine.name

            // Description
            if (!routine.description.isNullOrBlank()) {
                binding.routineDescription.text = routine.description
                binding.routineDescription.isVisible = true
            } else {
                binding.routineDescription.isVisible = false
            }

            // Streak Badge
            if (routine.currentStreak > 0) {
                binding.streakBadge.text = context.getString(R.string.streak_format, routine.currentStreak)
                binding.streakBadge.alpha = 1.0f
            } else {
                binding.streakBadge.text = context.getString(R.string.streak_none)
                binding.streakBadge.alpha = 0.6f
            }

            // Frequency
            binding.frequencyBadge.text = routine.frequency.lowercase().replaceFirstChar { it.uppercase() }

            // Reminder Time
            if (!routine.reminderTime.isNullOrBlank()) {
                binding.reminderTime.text = routine.reminderTime
                binding.reminderTime.isVisible = true
            } else {
                binding.reminderTime.isVisible = false
            }

            // Check-in Button State
            if (isDoneToday) {
                binding.checkInButton.text = context.getString(R.string.action_checked_in)
                binding.checkInButton.setIconResource(R.drawable.ic_task_alt)
                binding.checkInButton.alpha = 0.75f
            } else {
                binding.checkInButton.text = context.getString(R.string.action_check_in)
                binding.checkInButton.setIconResource(R.drawable.ic_task_alt)
                binding.checkInButton.alpha = 1.0f
            }

            // Click Handlers
            binding.cardView.setOnClickListener {
                onRoutineClick(routine)
            }
            binding.checkInButton.setOnClickListener {
                onCheckInClick(routine)
            }
        }
    }

    object RoutineDiffCallback : DiffUtil.ItemCallback<Routine>() {
        override fun areItemsTheSame(oldItem: Routine, newItem: Routine): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Routine, newItem: Routine): Boolean {
            return oldItem == newItem
        }
    }

}
