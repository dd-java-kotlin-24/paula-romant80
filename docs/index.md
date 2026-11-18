---
title: Overview
description: "Summary of in-progress or completed project."
order: 0
---

{% include ddc-abbreviations.md %}

## Page contents
{:.no_toc}

- ToC
{:toc}


## Summary

### P.A.U.L.A. (Plan, Act, Uplift: Life Achieved)

**P.A.U.L.A.** is a multi-user, offline-first Android task manager, daily habit tracker, and uplifting lifestyle companion built with 100% idiomatic **Kotlin**. Developed in loving memory of Paula Elizabeth Kleczka—who championed an organized living space, folded blankets, and caring for family—the application bridges structured productivity with genuine personal connection for **any user**. 

The app streamlines everyday responsibilities by combining customizable to-do lists and recurring habit checklists with an interactive 2D companion avatar that provides real voice encouragement. Designed as a **multi-user platform**, individuals, roommates, or family members can each maintain their own private chores, routines, and habit streaks on a shared device, ensuring complete data privacy and personalized progress. Users organize daily household chores, manage personal or family responsibilities, and build long-term positive habits while receiving spoken audio affirmations, visual milestone celebrations, and accessible subtitles as items are completed.

## Intended users and user stories

### User Personas

- **The Multi-User Household & Family Member:**
  An individual, parent, or roommate managing domestic responsibilities and cleaning routines on a personal or shared Android device who needs their chores and habit streaks segregated from other members of the home.

- **The Routine & Habit Builder:**
  A student or working professional striving to build structured daily habits (such as tidying up a workspace, studying, or morning routines) through positive auditory reinforcement and visual streak tracking.

- **The Encouragement & Executive Function Seeker:**
  A user who experiences task fatigue, ADHD, or executive dysfunction and benefits from warm auditory feedback, visual companion check-ins, and celebrated micro-wins rather than cold, corporate checklists.

### User Stories

- As a user, I want to sign in to my own private profile so that my tasks, recurring routines, and streak records remain completely isolated from other users on the device.
- As a household manager, I want to create recurring daily chores (like folding blankets and tidying common spaces) so that my domestic routines stay consistent.
- As a habit builder, I want to track my active streak and longest streak so that I am motivated to keep my positive habits going every single day.
- As an encouragement seeker, I want to hear uplifting spoken voice affirmations when I check off chores so that completing everyday tasks feels rewarding and positive.
- As a user with hearing impairments or operating in quiet environments, I want on-screen subtitle captions displayed whenever audio affirmations play.
- As a user, I want to configure nighttime Quiet Hours so that alarms and voice prompts never disturb my rest.
- As a user, I want to attach a photo taken with my device camera to completed chores so that I have visual proof of my clean, tidy spaces.

## Functionality

- **Multi-User Profile & Data Isolation:**
  Supports distinct user accounts. All tasks, recurring routines, and streak history logs are partitioned by the authenticated user ID in local Room storage, preventing data collisions across multiple users on the same device.

- **Modern Idiomatic Kotlin Architecture:**
  Engineered exclusively in Kotlin using Modern Android Development (MAD) standards: Single-Activity architecture with Jetpack Navigation, ViewBinding, Kotlin Coroutines, reactive `StateFlow` and `Flow` streams, Room ORM via KSP, and Hilt dependency injection.

- **To-Do Lists & Daily Habits (User-Scoped):**
  Create, edit, prioritize, and check off one-time chores or repeating daily routines (like folding blankets or tidying a room). Each item supports notes, due dates, priority stars, and category tags.

- **Paula's 2D Companion Avatar:**
  A friendly illustrated character on the main screen that dynamically reacts to your day. She greets you upon sign-in, rests during quiet hours, and celebrates when you finish your chores.

- **Real Spoken Voice Encouragement:**
  Plays short, AAPT-compiled audio clips of Paula's authentic voice. Users hear warm greetings, gentle routine reminders, and celebratory praise when clearing daily lists or reaching streak milestones.

- **Dynamic Swipe Lists (RecyclerView & ListAdapter):**
  Displays user tasks in responsive Material 3 cards with smooth swipe-to-complete gestures powered by `ItemTouchHelper` and high-performance `DiffUtil` calculations.

- **Custom Settings & Quiet Hours:**
  A straightforward preferences screen to adjust voice volume, toggle subtitles, and set nighttime "quiet hours" so audio prompts never disturb rest.

- **100% Offline-First Persistence:**
  Saves all tasks, routines, streaks, and preferences directly on the device using Google Room and SQLite. No continuous internet connection or external server is required, ensuring complete privacy, zero hosting costs, and instantaneous load times.

## Persistent data

Because P.A.U.L.A. is an offline-first, multi-user application, all data is stored locally on the device using **Google Room ORM and SQLite** structured in strict **Third Normal Form (3NF)**:

* **User Profiles (`user_profile`):** Stores user account primary keys (`user_id`), unique authentication tokens/OAuth keys (`oauth_key`), display names, and creation timestamps to partition all application data per user.
* **Task Items (`task`):** Stores user-scoped task titles, descriptions, due dates, priority levels (1–3), category tags, completion status, completion timestamps, local photo URIs, and tutorial links (`user_id` FK).
* **Daily Routines & Habits (`routine`):** Tracks user-scoped recurring habit checklists (such as making the bed and folding blankets), frequencies (Daily/Weekly), reminder times, and active/best streak counts (`user_id` FK).
* **Routine Completion Logs (`routine_log`):** Stores historical audit records (completion date, exact epoch timestamp, notes, photo attachments) linked to parent routines (`routine_id` FK) with duplicate-prevention constraints to power streak calculations.
* **Categories (`category`):** Organizes chores and habits into distinct colored buckets (e.g., *Household*, *Family*, *Wellness*, *Focus*) with Material icon identifiers.
* **Voice Prompt Mappings (`voice_prompt`):** Universal system catalog mapping trigger events (e.g., `MORNING_ROUTINE`, `TASK_CHECKED`) to raw audio filenames, subtitle transcripts, and authenticity flags.
* **User Preferences:** Preserves quiet-hour windows, master volume levels, caption toggles, and color theme settings using Android `SharedPreferences`.

## Device/external services

### Client Device Services
* **Audio Playback (`MediaPlayer` / `SoundPool`):** Plays recorded audio clips of Paula's voice for greetings, reminders, and celebration milestones.
* **Device Camera (`Camera API` / `ActivityResultContracts.TakePicture`):** Allows users to capture photos of clean spaces, family moments, or finished chores to attach as proof of completion.
* **Reminders & Alerts (`AlarmManager` & `NotificationManager`):** Schedules daily routine reminders and respects user-defined quiet hours.
* **Tactile Feedback (`Vibrator API`):** Provides gentle haptic vibrations when checking off tasks or completing habits.

### External Services
* **Google Identity Services / Google Sign-In:** Provides secure authentication credentials to populate the multi-user profile table and isolate user data.
* **YouTube Video Service:** Provides quick access to helpful instructional video tutorials (such as blanket-folding techniques, laundry tips, and kitchen tidying guides) linked directly to relevant household tasks and routines.

## Stretch goals and possible enhancements

1. **Custom Color Themes (Least Complex):**
   Add quick theme toggles featuring Paula's favorite colors—pink, purple, and black—for app accents and cards.

2. **Visual Streak Calendar & Heatmap (Moderate Complexity):**
   A monthly calendar grid that highlights consistency and habit streaks with visual color intensity.

3. **In-App Voice Clip Import (Moderate Complexity):**
   A tool inside settings allowing users to record or import their own personalized audio affirmations directly onto the device.

4. **Animated 2D Avatar Reactions (High Complexity):**
   Upgrade the static avatar illustrations to smooth 2D vector animations (such as waving arms or smiling) when tasks are cleared.

5. **Multi-User Encrypted Cloud Backup & Sync (Most Complex):**
   Allow users to export and synchronize encrypted backups of their tasks, streaks, and photos across multiple personal Android devices.

## Documents & Specifications

* [**P.A.U.L.A. Master Implementation Plan & Academic Criteria Audit (PDF)**](pdf/PAULA_Master_Plan.pdf)  
  A 2-page master roadmap detailing the 4-sprint milestone execution matrix, 5-stage technical build plan, pure Kotlin MVVM architecture, multi-user data scoping, and full compliance verification against Nick Bennett's 5 Core Technical Pillars and academic rubrics.

* [**Database Entity-Relationship Diagram (ERD) Specification (PDF)**](pdf/PAULA_ERD_Specification.pdf)  
  A technical Crow's Foot ERD specification detailing all 6 Room SQLite entities (`User`, `Category`, `Task`, `Routine`, `RoutineLog`, `VoicePrompt`), column data types, foreign keys, cascade rules, multi-user isolation, and Mermaid diagram definition.

* [**Consumer Guide & System Flow Overview (PDF)**](pdf/PAULA_Simple_Guide.pdf)  
  A 1-page visual overview detailing multi-user sign-in flows, user interaction flows, offline-first persistence, and Paula's voice encouragement trigger system.
