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
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import edu.cnm.deepdive.paula.R
import edu.cnm.deepdive.paula.adapter.RoutineAdapter
import edu.cnm.deepdive.paula.databinding.FragmentRoutineBinding
import edu.cnm.deepdive.paula.model.entity.Routine
import edu.cnm.deepdive.paula.viewmodel.RoutineViewModel
import edu.cnm.deepdive.paula.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Fragment presenting recurring habits, streak progression, and daily check-ins.
 */
@AndroidEntryPoint
class RoutineFragment : Fragment() {

    private var _binding: FragmentRoutineBinding? = null
    private val binding get() = _binding!!

    private val routineViewModel: RoutineViewModel by viewModels()
    private val userViewModel: UserViewModel by activityViewModels()

    private lateinit var routineAdapter: RoutineAdapter
    private var allRoutines: List<Routine> = emptyList()

    private enum class Filter { ALL, PENDING, COMPLETED }
    private var currentFilter: Filter = Filter.ALL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoutineBinding.inflate(inflater, container, false)
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
        routineAdapter = RoutineAdapter(
            onRoutineClick = { routine ->
                showEditRoutineDialog(routine)
            },
            onCheckInClick = { routine ->
                routineViewModel.toggleCompletion(routine)
            }
        )
        binding.routinesRecyclerView.adapter = routineAdapter
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
        binding.addRoutine.setOnClickListener {
            showCreateRoutineDialog()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    userViewModel.currentUser.collect { user ->
                        if (user != null) {
                            routineViewModel.setUserId(user.id)
                        }
                    }
                }
                launch {
                    routineViewModel.routines.collect { routines ->
                        allRoutines = routines
                        applyFilter()
                    }
                }
            }
        }
    }

    private fun applyFilter() {
        val today = LocalDate.now()
        val filtered = when (currentFilter) {
            Filter.ALL -> allRoutines
            Filter.PENDING -> allRoutines.filter { it.lastCompletedDate != today }
            Filter.COMPLETED -> allRoutines.filter { it.lastCompletedDate == today }
        }
        routineAdapter.submitList(filtered)
        binding.emptyStateContainer.isVisible = filtered.isEmpty()
    }

    private fun showCreateRoutineDialog() {
        val input = TextInputEditText(requireContext()).apply {
            hint = getString(R.string.hint_routine_name)
            setSingleLine()
        }
        val container = FrameLayout(requireContext()).apply {
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, (8 * resources.displayMetrics.density).toInt(), padding, 0)
            addView(input)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_new_routine_title)
            .setView(container)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isNotBlank()) {
                    val userId = userViewModel.currentUser.value?.id ?: 1L
                    val routine = Routine(
                        userId = userId,
                        name = name,
                        frequency = "DAILY"
                    )
                    routineViewModel.save(routine)
                }
            }
            .show()
    }

    private fun showEditRoutineDialog(routine: Routine) {
        val input = TextInputEditText(requireContext()).apply {
            setText(routine.name)
            hint = getString(R.string.hint_routine_name)
            setSingleLine()
        }
        val container = FrameLayout(requireContext()).apply {
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, (8 * resources.displayMetrics.density).toInt(), padding, 0)
            addView(input)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(routine.name)
            .setView(container)
            .setNeutralButton(R.string.action_delete) { _, _ ->
                routineViewModel.remove(routine)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isNotBlank()) {
                    val updated = routine.copy(name = name)
                    routineViewModel.save(updated)
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
