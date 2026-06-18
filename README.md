# Activity Launcher Shizuku Plugin

A Shizuku-based plugin for [Activity Launcher](https://github.com/butzist/ActivityLauncher) that allows launching private (non-exported) activities.

## Requirements
- Android 5.0 (API 21) or higher.
- [Shizuku](https://shizuku.rikka.app/) installed and running.

## Installation
1. Download and install the latest APK.
2. Open Shizuku and ensure the service is started.
3. Grant the "Activity Launcher Shizuku Plugin" permission in Shizuku.

## Usage
Once installed and authorized, Activity Launcher will automatically detect this plugin and offer Shizuku as a launch option for private activities.

### Manual Intent Usage (for developers)
The plugin handles the following intent:

#### Launch Activity
- **Action**: `activitylauncher.intent.action.LAUNCH_ACTIVITY`
- **Extras**:
    - `extra_intent`: (String) Target intent encoded as a URI string (e.g., `intent:#Intent;component=com.android.settings/.Settings;end`) using `Intent.URI_INTENT_SCHEME`.

## Technical Details

### Assistant Method
To launch private activities with high compatibility, this plugin uses the "Assistant Method". This technique involves temporarily setting the target activity as the system's default Assistant via `Settings.Secure.ASSISTANT` and then triggering an assist request. 

This method was first described by [Tornaco](https://github.com/Tornaco) for use in tools like Thanox and App Ops to bypass Android's activity launch restrictions without requiring root (when used alongside Shizuku for permission management).

## License
GPL v3 License
