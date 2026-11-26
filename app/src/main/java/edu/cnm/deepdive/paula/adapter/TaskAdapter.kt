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

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import edu.cnm.deepdive.paula.R
import edu.cnm.deepdive.paula.databinding.ItemTaskCardBinding
import edu.cnm.deepdive.paula.model.entity.Task
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * ListAdapter presenting Task entities with DiffUtil animation, completion toggles,
 * and click interactions.
 */
class TaskAdapter(
    private val onTaskClick: (Task) -> Unit,
    private val onTaskChecked: (Task, Boolean) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(TaskDiffCallback) {

    private var categoryNames: Map<Long, String> = emptyMap()

    fun setCategories(categories: Map<Long, String>) {
        categoryNames = categories
        notifyDataSetChanged()
    }

    private val dateFormatter = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(
        private val binding: ItemTaskCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            // Task Title & Completion Strikethrough
            binding.taskTitle.text = task.title
            if (task.isCompleted) {
                binding.taskTitle.paintFlags = binding.taskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.taskTitle.alpha = 0.55f
            } else {
                binding.taskTitle.paintFlags = binding.taskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.taskTitle.alpha = 1.0f
            }

            // Checkbox with listener detach to prevent view recycling glitches
            binding.completedCheck.setOnCheckedChangeListener(null)
            binding.completedCheck.isChecked = task.isCompleted
            binding.completedCheck.setOnCheckedChangeListener { _, isChecked ->
                onTaskChecked(task, isChecked)
            }

            // Notes Preview
            if (!task.notes.isNullOrBlank()) {
                binding.taskNotes.text = task.notes
                binding.taskNotes.visibility = View.VISIBLE
            } else {
                binding.taskNotes.visibility = View.GONE
            }

            // Due Date
            if (task.dueDate != null) {
                binding.dueDate.text = dateFormatter.format(task.dueDate)
                binding.dueDate.visibility = View.VISIBLE
            } else {
                binding.dueDate.visibility = View.GONE
            }

            // Priority Indicator
            if (task.priority > 1) {
                val context = binding.root.context
                val text = if (task.priority >= 3) {
                    context.getString(R.string.priority_high)
                } else {
                    context.getString(R.string.priority_medium)
                }
                binding.priorityBadge.text = text
                binding.priorityBadge.visibility = View.VISIBLE
            } else {
                binding.priorityBadge.visibility = View.GONE
            }

            // Category Chip
            val categoryName = task.categoryId?.let { categoryNames[it] }
            if (!categoryName.isNullOrBlank()) {
                binding.categoryChip.text = categoryName
                binding.categoryChip.isVisible = true
            } else {
                binding.categoryChip.isVisible = false
            }

            // Photo Proof Indicator
            binding.photoIndicator.isVisible = !task.photoUri.isNullOrBlank()

            // Entire Card Click
            binding.root.setOnClickListener {
                onTaskClick(task)
            }
        }
    }

    object TaskDiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean =
            oldItem == newItem
    }

}
