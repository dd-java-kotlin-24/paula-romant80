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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import edu.cnm.deepdive.paula.databinding.DialogRoutineEditBinding
import edu.cnm.deepdive.paula.databinding.FragmentRoutineBinding
import edu.cnm.deepdive.paula.model.entity.Routine
import edu.cnm.deepdive.paula.service.CelebrationManager
import edu.cnm.deepdive.paula.service.ReminderManager
import edu.cnm.deepdive.paula.service.RoutineRepository
import edu.cnm.deepdive.paula.service.VoiceCelebrationHelper
import edu.cnm.deepdive.paula.viewmodel.RoutineViewModel
import edu.cnm.deepdive.paula.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.time.DayOfWeek
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

    private var pendingCameraUri: Uri? = null
    private var activeDialogBinding: DialogRoutineEditBinding? = null
    private var activeDialogPhotoUri: Uri? = null

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) {
            activeDialogPhotoUri = pendingCameraUri
            updateDialogPhotoView(activeDialogBinding, activeDialogPhotoUri)
        }
    }

    private val pickPhotoLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            copyToAppStorageAndAttach(uri)
        }
    }

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
                val wasDoneToday = routine.lastCompletedDate == LocalDate.now()
                routineViewModel.toggleCompletion(routine)
                if (!wasDoneToday) {
                    CelebrationManager.celebrateRoutineStreak(this@RoutineFragment, routine, routineAdapter.currentList)
                }
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
                if (!wasDoneToday) {
                    CelebrationManager.celebrateRoutineStreak(this@RoutineFragment, routine, routineAdapter.currentList)
                }

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
        val dialogBinding = DialogRoutineEditBinding.inflate(layoutInflater)
        var selectedTime: String? = existing?.reminderTime

        activeDialogBinding = dialogBinding
        activeDialogPhotoUri = existing?.photoUri?.let { Uri.parse(it) }
        updateDialogPhotoView(dialogBinding, activeDialogPhotoUri)

        dialogBinding.headerTitle.text = existing?.name ?: getString(R.string.dialog_new_routine_title)
        dialogBinding.nameInput.setText(existing?.name.orEmpty())
        dialogBinding.descriptionInput.setText(existing?.description.orEmpty())

        val dayChips = listOf(
            dialogBinding.chipDayMon to "Mon",
            dialogBinding.chipDayTue to "Tue",
            dialogBinding.chipDayWed to "Wed",
            dialogBinding.chipDayThu to "Thu",
            dialogBinding.chipDayFri to "Fri",
            dialogBinding.chipDaySat to "Sat",
            dialogBinding.chipDaySun to "Sun"
        )

        fun updateCustomDaysVisibility(isCustom: Boolean) {
            dialogBinding.customDaysGroup.isVisible = isCustom
        }

        val existingFreq = existing?.frequency.orEmpty().uppercase()
        when (existingFreq) {
            "WEEKDAYS" -> {
                dialogBinding.chipFrequencyWeekdays.isChecked = true
                updateCustomDaysVisibility(false)
            }
            "WEEKENDS" -> {
                dialogBinding.chipFrequencyWeekends.isChecked = true
                updateCustomDaysVisibility(false)
            }
            "DAILY", "" -> {
                dialogBinding.chipFrequencyDaily.isChecked = true
                updateCustomDaysVisibility(false)
            }
            else -> {
                dialogBinding.chipFrequencyCustom.isChecked = true
                updateCustomDaysVisibility(true)
                val scheduledDays = RoutineRepository.parseScheduledDays(existingFreq)
                dialogBinding.chipDayMon.isChecked = scheduledDays.contains(DayOfWeek.MONDAY)
                dialogBinding.chipDayTue.isChecked = scheduledDays.contains(DayOfWeek.TUESDAY)
                dialogBinding.chipDayWed.isChecked = scheduledDays.contains(DayOfWeek.WEDNESDAY)
                dialogBinding.chipDayThu.isChecked = scheduledDays.contains(DayOfWeek.THURSDAY)
                dialogBinding.chipDayFri.isChecked = scheduledDays.contains(DayOfWeek.FRIDAY)
                dialogBinding.chipDaySat.isChecked = scheduledDays.contains(DayOfWeek.SATURDAY)
                dialogBinding.chipDaySun.isChecked = scheduledDays.contains(DayOfWeek.SUNDAY)
            }
        }

        dialogBinding.frequencyChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val isCustom = checkedIds.contains(R.id.chip_frequency_custom)
            updateCustomDaysVisibility(isCustom)
            if (isCustom && dayChips.none { it.first.isChecked }) {
                dialogBinding.chipDayMon.isChecked = true
                dialogBinding.chipDayWed.isChecked = true
                dialogBinding.chipDayFri.isChecked = true
            }
        }

        // Photo action bindings
        dialogBinding.btnTakePhoto.setOnClickListener { launchCamera() }
        dialogBinding.btnRetakePhoto.setOnClickListener { launchCamera() }
        dialogBinding.btnChoosePhoto.setOnClickListener {
            pickPhotoLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        dialogBinding.btnRemovePhoto.setOnClickListener {
            activeDialogPhotoUri = null
            updateDialogPhotoView(dialogBinding, null)
        }
        dialogBinding.photoPreview.setOnClickListener {
            activeDialogPhotoUri?.let { showFullScreenPhoto(it) }
        }

        if (!selectedTime.isNullOrBlank()) {
            dialogBinding.reminderTimeInput.setText(selectedTime)
        }

        val openTimePicker = View.OnClickListener {
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
                dialogBinding.reminderTimeInput.setText(formatted)
            }
            picker.show(parentFragmentManager, "routine_time_picker")
        }

        dialogBinding.reminderTimeInput.setOnClickListener(openTimePicker)
        dialogBinding.reminderTimeLayout.setStartIconOnClickListener(openTimePicker)
        dialogBinding.reminderTimeLayout.setEndIconOnClickListener {
            selectedTime = null
            dialogBinding.reminderTimeInput.text = null
        }

        val builder = MaterialAlertDialogBuilder(context)
            .setView(dialogBinding.root)

        if (existing != null) {
            builder.setNeutralButton(R.string.action_delete) { _, _ ->
                routineViewModel.remove(existing)
                ReminderManager.cancel(context.applicationContext, existing.id)
            }
        }

        val dialog = builder.create()
        dialog.setOnDismissListener {
            activeDialogBinding = null
        }

        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnSave.setOnClickListener {
            val name = dialogBinding.nameInput.text?.toString()?.trim().orEmpty()
            if (name.isBlank()) {
                dialogBinding.nameLayout.error = getString(R.string.error_task_title_required)
                return@setOnClickListener
            }
            dialogBinding.nameLayout.error = null
            val description = dialogBinding.descriptionInput.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }
            val frequency = when (dialogBinding.frequencyChipGroup.checkedChipId) {
                R.id.chip_frequency_weekdays -> "WEEKDAYS"
                R.id.chip_frequency_weekends -> "WEEKENDS"
                R.id.chip_frequency_custom -> {
                    val selectedDays = dayChips.filter { it.first.isChecked }.map { it.second }
                    if (selectedDays.isEmpty()) {
                        Snackbar.make(dialogBinding.root, R.string.error_frequency_required, Snackbar.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    selectedDays.joinToString(", ")
                }
                else -> "DAILY"
            }
            val userId = userViewModel.currentUser.value?.id ?: 1L
            val routineToSave = existing?.copy(
                name = name,
                description = description,
                frequency = frequency,
                reminderTime = selectedTime,
                photoUri = activeDialogPhotoUri?.toString()
            ) ?: Routine(
                userId = userId,
                name = name,
                description = description,
                frequency = frequency,
                reminderTime = selectedTime,
                photoUri = activeDialogPhotoUri?.toString()
            )
            val appContext = context.applicationContext
            routineViewModel.save(routineToSave) { saved ->
                if (selectedTime != null) {
                    ReminderManager.schedule(appContext, saved)
                } else if (existing != null) {
                    ReminderManager.cancel(appContext, saved.id)
                }
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateDialogPhotoView(dialogBinding: DialogRoutineEditBinding?, uri: Uri?) {
        if (dialogBinding == null) return
        if (uri == null) {
            dialogBinding.photoPreview.setImageDrawable(null)
            dialogBinding.photoPreviewCard.isVisible = false
            dialogBinding.photoActionContainer.isVisible = true
        } else {
            val bitmap = loadScaledBitmap(uri)
            if (bitmap != null) {
                dialogBinding.photoPreview.setImageBitmap(bitmap)
            } else {
                dialogBinding.photoPreview.setImageURI(uri)
            }
            dialogBinding.photoPreviewCard.isVisible = true
            dialogBinding.photoActionContainer.isVisible = false
        }
    }

    private fun launchCamera() {
        try {
            val context = requireContext()
            val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
            val photoFile = File.createTempFile("routine_${System.currentTimeMillis()}_", ".jpg", picturesDir)
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, photoFile)
            pendingCameraUri = uri
            takePhotoLauncher.launch(uri)
        } catch (_: Exception) {
            Toast.makeText(requireContext(), R.string.error_photo_attach, Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToAppStorageAndAttach(sourceUri: Uri) {
        try {
            val context = requireContext()
            val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
            val targetFile = File(picturesDir, "routine_photo_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val authority = "${context.packageName}.fileprovider"
            val permanentUri = FileProvider.getUriForFile(context, authority, targetFile)
            activeDialogPhotoUri = permanentUri
            updateDialogPhotoView(activeDialogBinding, permanentUri)
        } catch (_: Exception) {
            Toast.makeText(requireContext(), R.string.error_photo_attach, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFullScreenPhoto(uri: Uri) {
        val imageView = android.widget.ImageView(requireContext()).apply {
            adjustViewBounds = true
            scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            val bitmap = loadScaledBitmap(uri, 1920, 1920)
            if (bitmap != null) {
                setImageBitmap(bitmap)
            } else {
                setImageURI(uri)
            }
        }
        MaterialAlertDialogBuilder(requireContext())
            .setView(imageView)
            .setPositiveButton(R.string.action_close, null)
            .show()
    }

    @Suppress("DEPRECATION")
    private fun loadScaledBitmap(uri: Uri, maxWidth: Int = 1080, maxHeight: Int = 1080): Bitmap? {
        val context = context ?: return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    val size = info.size
                    val widthRatio = size.width.toFloat() / maxWidth
                    val heightRatio = size.height.toFloat() / maxHeight
                    val maxRatio = maxOf(widthRatio, heightRatio)
                    if (maxRatio > 1.0f) {
                        val targetWidth = (size.width / maxRatio).toInt().coerceAtLeast(1)
                        val targetHeight = (size.height / maxRatio).toInt().coerceAtLeast(1)
                        decoder.setTargetSize(targetWidth, targetHeight)
                    }
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(stream, null, options)
                    var inSampleSize = 1
                    if (options.outHeight > maxHeight || options.outWidth > maxWidth) {
                        val halfHeight = options.outHeight / 2
                        val halfWidth = options.outWidth / 2
                        while ((halfHeight / inSampleSize) >= maxHeight && (halfWidth / inSampleSize) >= maxWidth) {
                            inSampleSize *= 2
                        }
                    }
                    val decodeOptions = BitmapFactory.Options().apply {
                        this.inSampleSize = inSampleSize
                    }
                    context.contentResolver.openInputStream(uri)?.use { secondStream ->
                        BitmapFactory.decodeStream(secondStream, null, decodeOptions)
                    }
                }
            }
        } catch (_: Throwable) {
            null
        }
    }

    override fun onStop() {
        super.onStop()
        VoiceCelebrationHelper.stop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        VoiceCelebrationHelper.stop()
        _binding = null
    }

}
