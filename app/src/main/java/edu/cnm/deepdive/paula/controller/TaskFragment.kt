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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import edu.cnm.deepdive.paula.R
import edu.cnm.deepdive.paula.adapter.TaskAdapter
import edu.cnm.deepdive.paula.databinding.FragmentTaskBinding
import edu.cnm.deepdive.paula.model.entity.Task
import edu.cnm.deepdive.paula.viewmodel.TaskViewModel
import edu.cnm.deepdive.paula.viewmodel.UserViewModel
import kotlinx.coroutines.launch

/**
 * Controller displaying the interactive task list with completion toggles,
 * category/status filters, and navigation to task creation and editing.
 */
@AndroidEntryPoint
class TaskFragment : Fragment() {

    private var _binding: FragmentTaskBinding? = null
    private val binding get() = _binding!!

    private val taskViewModel: TaskViewModel by viewModels()
    private val userViewModel: UserViewModel by activityViewModels()

    private lateinit var taskAdapter: TaskAdapter
    private var allTasks: List<Task> = emptyList()

    private enum class Filter { ALL, PENDING, COMPLETED }
    private var currentFilter: Filter = Filter.ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFilters()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskClick = { task ->
                val action = TaskFragmentDirections.actionTasksToTaskEdit().apply {
                    taskId = task.id
                }
                findNavController().navigate(action)
            },
            onTaskChecked = { task, _ ->
                taskViewModel.toggleComplete(task)
            }
        )
        binding.tasksRecyclerView.adapter = taskAdapter

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            private val completeBackground = ColorDrawable(0xFF2E7D32.toInt())
            private val deleteBackground = ColorDrawable(0xFFC62828.toInt())
            private val completeIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_task_alt)
            private val deleteIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_close)

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val task = taskAdapter.currentList.getOrNull(position) ?: return

                if (direction == ItemTouchHelper.RIGHT) {
                    taskViewModel.toggleComplete(task)
                    taskAdapter.notifyItemChanged(position)
                    val messageRes = if (!task.isCompleted) {
                        R.string.snackbar_task_completed
                    } else {
                        R.string.snackbar_task_pending
                    }
                    Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_SHORT)
                        .setAction(R.string.action_undo) {
                            taskViewModel.toggleComplete(task)
                        }
                        .show()
                } else if (direction == ItemTouchHelper.LEFT) {
                    taskViewModel.remove(task)
                    Snackbar.make(binding.root, R.string.snackbar_task_deleted, Snackbar.LENGTH_LONG)
                        .setAction(R.string.action_undo) {
                            taskViewModel.save(task)
                        }
                        .show()
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val itemHeight = itemView.bottom - itemView.top

                if (dX > 0) {
                    completeBackground.setBounds(
                        itemView.left,
                        itemView.top,
                        itemView.left + dX.toInt(),
                        itemView.bottom
                    )
                    completeBackground.draw(c)

                    completeIcon?.let { icon ->
                        val iconMargin = (itemHeight - icon.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + icon.intrinsicHeight
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = iconLeft + icon.intrinsicWidth
                        if (iconRight < itemView.left + dX.toInt()) {
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                            icon.setTint(Color.WHITE)
                            icon.draw(c)
                        }
                    }
                } else if (dX < 0) {
                    deleteBackground.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    deleteBackground.draw(c)

                    deleteIcon?.let { icon ->
                        val iconMargin = (itemHeight - icon.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconBottom = iconTop + icon.intrinsicHeight
                        val iconRight = itemView.right - iconMargin
                        val iconLeft = iconRight - icon.intrinsicWidth
                        if (iconLeft > itemView.right + dX.toInt()) {
                            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                            icon.setTint(Color.WHITE)
                            icon.draw(c)
                        }
                    }
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.tasksRecyclerView)
    }

    private fun setupFilters() {
        binding.filterChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when {
                checkedIds.contains(R.id.chip_pending) -> Filter.PENDING
                checkedIds.contains(R.id.chip_completed) -> Filter.COMPLETED
                else -> Filter.ALL
            }
            applyFilter()
        }
    }

    private fun setupListeners() {
        binding.addTask.setOnClickListener {
            val action = TaskFragmentDirections.actionTasksToTaskEdit().apply {
                taskId = 0L
            }
            findNavController().navigate(action)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    userViewModel.currentUser.collect { user ->
                        if (user != null) {
                            taskViewModel.setUserId(user.id)
                        }
                    }
                }
                launch {
                    taskViewModel.tasks.collect { tasks ->
                        allTasks = tasks
                        applyFilter()
                    }
                }
            }
        }
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            Filter.ALL -> allTasks
            Filter.PENDING -> allTasks.filter { !it.isCompleted }
            Filter.COMPLETED -> allTasks.filter { it.isCompleted }
        }
        taskAdapter.submitList(filtered)
        binding.emptyStateContainer.isVisible = filtered.isEmpty()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
