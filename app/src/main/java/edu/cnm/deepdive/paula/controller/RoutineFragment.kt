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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import edu.cnm.deepdive.paula.R
import edu.cnm.deepdive.paula.adapter.RoutineAdapter
import edu.cnm.deepdive.paula.databinding.FragmentRoutineBinding
import edu.cnm.deepdive.paula.model.entity.Routine
import edu.cnm.deepdive.paula.service.ReminderManager
import edu.cnm.deepdive.paula.viewmodel.RoutineViewModel
import edu.cnm.deepdive.paula.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

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

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.RIGHT
        ) {
            private val completeBackground = ColorDrawable(0xFF2E7D32.toInt())
            private val checkIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_task_alt)

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val routine = routineAdapter.currentList.getOrNull(position) ?: return

                val wasDoneToday = routine.lastCompletedDate == LocalDate.now()
                routineViewModel.toggleCompletion(routine)
                routineAdapter.notifyItemChanged(position)

                val messageRes = if (!wasDoneToday) {
                    R.string.snackbar_routine_checked
                } else {
                    R.string.snackbar_routine_unchecked
                }
                Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_SHORT)
                    .setAction(R.string.action_undo) {
                        routineViewModel.toggleCompletion(routine)
                    }
                    .show()
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

                    checkIcon?.let { icon ->
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
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.routinesRecyclerView)
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
        showRoutineDialog(null)
    }

    private fun showEditRoutineDialog(routine: Routine) {
        showRoutineDialog(routine)
    }

    private fun showRoutineDialog(existing: Routine?) {
        val context = requireContext()
        val density = resources.displayMetrics.density
        var selectedTime: String? = existing?.reminderTime

        val nameInput = TextInputEditText(context).apply {
            setText(existing?.name.orEmpty())
            hint = getString(R.string.hint_routine_name)
            setSingleLine()
        }

        val descriptionInput = TextInputEditText(context).apply {
            setText(existing?.description.orEmpty())
            hint = getString(R.string.hint_routine_description)
        }

        val frequencyOptions = listOf(
            "DAILY" to getString(R.string.frequency_daily),
            "WEEKDAYS" to getString(R.string.frequency_weekdays),
            "WEEKENDS" to getString(R.string.frequency_weekends)
        )
        val frequencyGroup = RadioGroup(context).apply {
            orientation = LinearLayout.HORIZONTAL
            val currentFreq = existing?.frequency ?: "DAILY"
            frequencyOptions.forEachIndexed { index, (code, label) ->
                val radio = RadioButton(context).apply {
                    id = View.generateViewId()
                    text = label
                    tag = code
                    setPadding(0, 0, (12 * density).toInt(), 0)
                }
                addView(radio)
                if (code == currentFreq || (index == 0 && frequencyOptions.none { it.first == currentFreq })) {
                    check(radio.id)
                }
            }
        }

        val timeButton = MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = if (!selectedTime.isNullOrBlank()) {
                getString(R.string.label_reminder_time) + ": " + selectedTime
            } else {
                getString(R.string.hint_set_reminder)
            }
            setIconResource(R.drawable.ic_calendar_today)
        }

        timeButton.setOnClickListener {
            val initialHour = selectedTime?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 8
            val initialMinute = selectedTime?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(initialHour)
                .setMinute(initialMinute)
                .setTitleText(R.string.label_reminder_time)
                .build()

            picker.addOnPositiveButtonClickListener {
                val formatted = String.format(Locale.getDefault(), "%02d:%02d", picker.hour, picker.minute)
                selectedTime = formatted
                timeButton.text = getString(R.string.label_reminder_time) + ": " + formatted
            }
            picker.show(parentFragmentManager, "routine_time_picker")
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val paddingH = (24 * density).toInt()
            val paddingV = (8 * density).toInt()
            setPadding(paddingH, paddingV, paddingH, paddingV)
            addView(nameInput)
            addView(descriptionInput)
            addView(frequencyGroup)
            addView(timeButton)
        }

        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(existing?.name ?: getString(R.string.dialog_new_routine_title))
            .setView(container)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = nameInput.text?.toString()?.trim().orEmpty()
                if (name.isNotBlank()) {
                    val description = descriptionInput.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                    val checkedRadio = frequencyGroup.findViewById<RadioButton>(frequencyGroup.checkedRadioButtonId)
                    val frequency = (checkedRadio?.tag as? String) ?: "DAILY"
                    val userId = userViewModel.currentUser.value?.id ?: 1L
                    val routineToSave = existing?.copy(
                        name = name,
                        description = description,
                        frequency = frequency,
                        reminderTime = selectedTime
                    ) ?: Routine(
                        userId = userId,
                        name = name,
                        description = description,
                        frequency = frequency,
                        reminderTime = selectedTime
                    )
                    val appContext = context.applicationContext
                    routineViewModel.save(routineToSave) { saved ->
                        if (selectedTime != null) {
                            ReminderManager.schedule(appContext, saved)
                        } else if (existing != null) {
                            ReminderManager.cancel(appContext, saved.id)
                        }
                    }
                }
            }

        if (existing != null) {
            builder.setNeutralButton(R.string.action_delete) { _, _ ->
                routineViewModel.remove(existing)
                ReminderManager.cancel(context.applicationContext, existing.id)
            }
        }

        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
