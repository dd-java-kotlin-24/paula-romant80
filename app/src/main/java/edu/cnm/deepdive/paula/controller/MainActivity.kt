package edu.cnm.deepdive.paula.controller

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import edu.cnm.deepdive.paula.R
import edu.cnm.deepdive.paula.databinding.ActivityMainBinding
import edu.cnm.deepdive.paula.viewmodel.UserViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val userViewModel: UserViewModel by viewModels()
    private lateinit var navController: NavController

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (userViewModel.currentUser.value == null) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val savedOauthKey = prefs.getString(PREF_ACTIVE_OAUTH_KEY, DEFAULT_OAUTH_KEY) ?: DEFAULT_OAUTH_KEY
            val savedDisplayName = prefs.getString(PREF_ACTIVE_DISPLAY_NAME, DEFAULT_DISPLAY_NAME) ?: DEFAULT_DISPLAY_NAME
            userViewModel.loadOrCreate(savedOauthKey, savedDisplayName)
        }
        requestNotificationPermission()
        setupLayout()
        setupNavigation()
        observeCurrentUser()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupLayout() {
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarLayout.setPadding(0, systemBars.top, 0, 0)
            binding.bottomNavigation.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
    }

    private fun setupNavigation() {
        val navHostFragment = binding.navHostFragment.getFragment<NavHostFragment>()
        navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_routines, R.id.nav_tasks, R.id.nav_voice_prompts)
        )
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)
        binding.toolbar.inflateMenu(R.menu.main_options)
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_switch_profile -> {
                    showProfileSwitcherDialog()
                    true
                }
                else -> NavigationUI.onNavDestinationSelected(item, navController)
            }
        }
    }

    private fun observeCurrentUser() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                userViewModel.currentUser.collect { user ->
                    binding.toolbar.subtitle = user?.displayName
                    if (user != null) {
                        prefs.edit()
                            .putString(PREF_ACTIVE_OAUTH_KEY, user.oauthKey)
                            .putString(PREF_ACTIVE_DISPLAY_NAME, user.displayName)
                            .apply()
                    }
                }
            }
        }
    }

    companion object {
        private const val PREF_ACTIVE_OAUTH_KEY = "active_user_oauth_key"
        private const val PREF_ACTIVE_DISPLAY_NAME = "active_user_display_name"
        private const val DEFAULT_OAUTH_KEY = "default"
        private const val DEFAULT_DISPLAY_NAME = "Paula User"
    }

    private fun showProfileSwitcherDialog() {
        val users = userViewModel.users.value
        val currentUser = userViewModel.currentUser.value
        val names = users.map { it.displayName }.toTypedArray()
        val currentIndex = users.indexOfFirst { it.id == currentUser?.id }

        val builder = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_switch_profile_title)
            .setSingleChoiceItems(names, currentIndex) { dialog, which ->
                val selectedUser = users.getOrNull(which)
                if (selectedUser != null) {
                    userViewModel.setCurrentUser(selectedUser)
                    Toast.makeText(
                        this,
                        getString(R.string.toast_profile_switched, selectedUser.displayName),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                dialog.dismiss()
            }
            .setNeutralButton(R.string.action_add_profile) { _, _ ->
                showAddProfileDialog()
            }
            .setNegativeButton(R.string.action_cancel, null)

        if (currentUser != null && currentUser.oauthKey != DEFAULT_OAUTH_KEY) {
            builder.setPositiveButton(R.string.action_delete) { _, _ ->
                val fallback = users.firstOrNull { it.oauthKey == DEFAULT_OAUTH_KEY }
                    ?: users.firstOrNull { it.id != currentUser.id }
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialog_delete_profile_title)
                    .setMessage(getString(R.string.dialog_delete_profile_message, currentUser.displayName))
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.action_delete) { _, _ ->
                        userViewModel.deleteUser(currentUser, fallback) {
                            if (fallback != null) {
                                Toast.makeText(
                                    this,
                                    getString(R.string.toast_profile_switched, fallback.displayName),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    .show()
            }
        }
        builder.show()
    }

    private fun showAddProfileDialog() {
        val input = TextInputEditText(this).apply {
            hint = getString(R.string.hint_profile_name)
            setSingleLine()
        }
        val container = FrameLayout(this).apply {
            val paddingH = (24 * resources.displayMetrics.density).toInt()
            val paddingV = (8 * resources.displayMetrics.density).toInt()
            setPadding(paddingH, paddingV, paddingH, paddingV)
            addView(input)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_add_profile_title)
            .setView(container)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_save) { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isNotBlank()) {
                    val existing = userViewModel.users.value.firstOrNull {
                        it.displayName.equals(name, ignoreCase = true)
                    }
                    if (existing != null) {
                        userViewModel.setCurrentUser(existing)
                        Toast.makeText(
                            this,
                            getString(R.string.toast_profile_exists, existing.displayName),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val oauthKey = UUID.randomUUID().toString()
                        userViewModel.loadOrCreate(oauthKey, name) { newUser ->
                            Toast.makeText(
                                this,
                                getString(R.string.toast_profile_switched, newUser.displayName),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .show()
    }
}
