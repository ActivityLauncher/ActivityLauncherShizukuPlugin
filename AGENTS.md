# Project Overview
Activity Launcher Shizuku Plugin is an Android plugin for Activity Launcher that enables launching private activities using Shizuku for execution.

# Tech Stack
- **Language**: Kotlin
- **Build System**: Gradle (Kotlin DSL)
- **Minimum SDK**: 21
- **Target SDK**: 36
- **Compile SDK**: 36

## Key Libraries
- **Shizuku API**: For executing system-level actions.
- **HiddenApiBypass**: For accessing restricted Android APIs.
- **AndroidX AppCompat**: For backward compatibility.

# Project Structure
The project follows a simplified structure focused on plugin functionality.

## Root Directory
- `app/`: Main application module

## App Module (`app/src/main/java/net/activitylauncher/plugin/shizuku/`)
- `ShizukuLauncher`: Core logic for Shizuku-based activity launching, including the Assistant method and raw fallback.
- `LauncherActivity`: The primary entry point for handling intents from Activity Launcher.
- `ShizukuPluginApp`: Application class for initializing system-level bypasses.

# Development Guidelines
- **Shizuku Integration**: Always check for Shizuku binder and permissions before performing actions.
- **Launch Strategies**: 
    - Priority 1: Assistant Method (Higher compatibility for some private activities).
    - Priority 2: Raw Shizuku (Direct `IActivityManager` call).

# Verification
Always verify that the project builds and that the `LauncherActivity` correctly handles the defined intent actions.

## Relevant Commands
- **Build Debug APK**: `./gradlew app:assembleDebug`
