# Navigation elements

## Inventory

| Fragment Class      | Title     | Layout                | Top-level? | Superclass               |
|:--------------------|:----------|:----------------------|:----------:|:-------------------------|
| RoutineFragment     | Routines  | fragment_routine      |    Yes     | Fragment                 |
| TaskFragment        | Tasks     | fragment_task         |    Yes     | Fragment                 |
| VoicePromptFragment | Paula     | fragment_voice_prompt |    Yes     | Fragment                 |
| TaskEditFragment    | Edit Task | fragment_task_edit    |     No     | DialogFragment           |
| SettingsFragment    | Settings  | (none)                |     No     | PreferenceFragmentCompat |

# Organization

- All fragment classes are either already present in the `edu.cnm.deepdive.paula.controller` package, or should be created in that package.
- All referenced layouts are either existing layout resources, or should be created, using a root element of `ConstraintLayout` for fragment subclasses, and an XML resource with a `PreferenceScreen` root element for `PreferenceFragmentCompat` subclasses.

## Widget

- **Navigation Widget:** `com.google.android.material.bottomnavigation.BottomNavigationView`
- **Resource ID:** `@+id/bottom_navigation`
- **Menu Resource:** `@menu/bottom_nav_menu`

## Host

- **Host Activity:** `edu.cnm.deepdive.paula.controller.MainActivity`
- **Container View:** `androidx.fragment.app.FragmentContainerView`
- **Container Resource ID:** `@+id/nav_host_fragment`
- **NavHost Delegate:** `androidx.navigation.fragment.NavHostFragment`
- **Navigation Graph:** `@navigation/nav_graph`

# Navigation view and fragment synchronization

The navigation view (the bottom navigation bar) and the active fragment display are kept in continuous synchronization through Android Jetpack Navigation's `NavigationUI` integration. Instead of requiring manual click listeners and manual fragment transactions, the menu items in the bottom navigation menu share matching resource identifiers with the destination fragments defined in the navigation graph.

When a user taps an icon on the bottom bar, the navigation controller automatically transitions the host container to the corresponding fragment. In reverse, whenever the user navigates through app events or presses the system Back button, the navigation controller notifies the bottom navigation bar so it immediately updates its highlighted tab to reflect the active screen. This maintains a single source of truth for navigation state, guarantees that the user always knows where they are in the app, and provides a predictable, standard back-stack experience.

# General

The application follows Google's recommended Modern Android Single-Activity Architecture. A single main activity serves as the primary structural frame and lifecycle host, while individual screens and workflows are implemented as lightweight, modular fragments.

This design keeps the app fast and memory-efficient by avoiding the overhead of launching multiple separate activities. It also allows persistent UI scaffolding—such as the centered app header and the bottom navigation bar—to remain smoothly anchored on screen while fragment contents transition underneath. Centralizing navigation in a dedicated navigation graph ensures robust back-stack management, seamless deep linking capabilities, and proper lifecycle handling across configuration changes such as screen rotations.
