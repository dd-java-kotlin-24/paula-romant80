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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import edu.cnm.deepdive.paula.R
import edu.cnm.deepdive.paula.databinding.FragmentTaskEditBinding
import edu.cnm.deepdive.paula.model.entity.Category
import edu.cnm.deepdive.paula.model.entity.Task
import edu.cnm.deepdive.paula.viewmodel.TaskViewModel
import edu.cnm.deepdive.paula.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Dialog controller for creating and modifying task items with category, priority, and due dates.
 */
@AndroidEntryPoint
class TaskEditFragment : DialogFragment() {

    private var _binding: FragmentTaskEditBinding? = null
    private val binding get() = _binding!!

    private val args: TaskEditFragmentArgs by navArgs()
    private val taskViewModel: TaskViewModel by viewModels()
    private val userViewModel: UserViewModel by activityViewModels()

    private var currentTask: Task? = null
    private var selectedDueDate: Instant? = null
    private var selectedCategoryId: Long? = null
    private var availableCategories: List<Category> = emptyList()

    private val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withZone(ZoneId.of("UTC"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTaskEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val width = (resources.displayMetrics.widthPixels * 0.92).toInt()
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInitialMode()
        setupListeners()
        observeViewModel()
    }

    private fun setupInitialMode() {
        if (args.taskId > 0L) {
            binding.headerTitle.setText(R.string.title_task_edit)
            binding.deleteButton.isVisible = true
        } else {
            binding.headerTitle.setText(R.string.title_task_new)
            binding.deleteButton.isVisible = false
            binding.chipPriorityMedium.isChecked = true
        }
    }

    private fun setupListeners() {
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.saveButton.setOnClickListener {
            saveTask()
        }

        binding.deleteButton.setOnClickListener {
            confirmDelete()
        }

        binding.dueDateInput.setOnClickListener {
            showDatePicker()
        }

        binding.dueDateLayout.setStartIconOnClickListener {
            showDatePicker()
        }

        binding.dueDateLayout.setEndIconOnClickListener {
            selectedDueDate = null
            binding.dueDateInput.text = null
        }

        binding.categorySelector.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = if (position == 0) {
                null
            } else {
                availableCategories.getOrNull(position - 1)?.id
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    taskViewModel.categories.collect { categories ->
                        availableCategories = categories
                        setupCategoryDropdown()
                    }
                }
                if (args.taskId > 0L) {
                    launch {
                        taskViewModel.editingTask.collect { task ->
                            if (task != null && currentTask == null) {
                                currentTask = task
                                populateFields(task)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupCategoryDropdown() {
        val categoryNames = mutableListOf(getString(R.string.category_none))
        categoryNames.addAll(availableCategories.map { it.name })
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryNames)
        binding.categorySelector.setAdapter(adapter)

        val activeCatId = currentTask?.categoryId ?: selectedCategoryId
        if (activeCatId != null) {
            val matchingCat = availableCategories.find { it.id == activeCatId }
            if (matchingCat != null) {
                selectedCategoryId = matchingCat.id
                binding.categorySelector.setText(matchingCat.name, false)
            }
        }
    }

    private fun populateFields(task: Task) {
        binding.titleInput.setText(task.title)
        binding.notesInput.setText(task.notes)
        selectedDueDate = task.dueDate
        if (task.dueDate != null) {
            binding.dueDateInput.setText(dateFormatter.format(task.dueDate))
        } else {
            binding.dueDateInput.text = null
        }

        when (task.priority) {
            2 -> binding.chipPriorityHigh.isChecked = true
            0 -> binding.chipPriorityLow.isChecked = true
            else -> binding.chipPriorityMedium.isChecked = true
        }

        if (task.categoryId != null) {
            val matchingCat = availableCategories.find { it.id == task.categoryId }
            if (matchingCat != null) {
                selectedCategoryId = matchingCat.id
                binding.categorySelector.setText(matchingCat.name, false)
            }
        } else {
            selectedCategoryId = null
            binding.categorySelector.setText(getString(R.string.category_none), false)
        }
    }

    private fun showDatePicker() {
        val currentSelection = selectedDueDate?.toEpochMilli()
            ?: MaterialDatePicker.todayInUtcMilliseconds()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.label_due_date)
            .setSelection(currentSelection)
            .build()

        picker.addOnPositiveButtonClickListener { epochMilli ->
            selectedDueDate = Instant.ofEpochMilli(epochMilli)
            binding.dueDateInput.setText(dateFormatter.format(selectedDueDate))
        }

        picker.show(parentFragmentManager, "task_due_date_picker")
    }

    private fun saveTask() {
        val title = binding.titleInput.text?.toString()?.trim().orEmpty()
        if (title.isBlank()) {
            binding.titleLayout.error = getString(R.string.error_task_title_required)
            binding.titleInput.requestFocus()
            return
        } else {
            binding.titleLayout.error = null
        }

        val priority = when (binding.priorityChipGroup.checkedChipId) {
            R.id.chip_priority_high -> 2
            R.id.chip_priority_low -> 0
            else -> 1
        }
        val notes = binding.notesInput.text?.toString()?.trim()

        binding.saveButton.isEnabled = false

        val userId = userViewModel.currentUser.value?.id ?: 1L
        val taskToSave = currentTask?.copy(
            title = title,
            categoryId = selectedCategoryId,
            dueDate = selectedDueDate,
            priority = priority,
            notes = if (notes.isNullOrBlank()) null else notes
        ) ?: Task(
            userId = userId,
            title = title,
            categoryId = selectedCategoryId,
            dueDate = selectedDueDate,
            priority = priority,
            notes = if (notes.isNullOrBlank()) null else notes
        )

        taskViewModel.save(taskToSave) {
            dismiss()
        }
    }

    private fun confirmDelete() {
        val task = currentTask ?: return
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_task_title)
            .setMessage(R.string.dialog_delete_task_message)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                taskViewModel.remove(task)
                dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
