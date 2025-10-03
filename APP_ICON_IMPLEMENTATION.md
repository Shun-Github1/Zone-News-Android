# App Icon Switching Implementation

## Overview
The ZnewsPro app now uses a single activity alias with dynamic icon switching to prevent the dual icon issue during installation.

## Key Changes

### 1. AndroidManifest.xml
- **Removed** the LAUNCHER intent filter from MainActivity
- **Replaced** two activity aliases with a single alias: `MainActivityLauncher`
- **Set** MainActivity as `exported="false"` since it's accessed through the alias

### 2. AppIconManager.kt
- **Uses ShortcutManager** for dynamic icon switching (Android 7.1+)
- **Falls back** to preference-only storage for older Android versions
- **Initializes shortcuts** on app startup
- **Supports** both "default" (pg_logo) and "alternate" (pg_logo_alt) icons

### 3. BaseApplication.kt
- **Initializes shortcuts** before restoring saved icon preference
- **Ensures** proper icon state on app startup

### 4. Cleaned Up Resources
- **Removed** unused ic_launcher.xml and ic_launcher_round.xml files
- **Removed** unused ic_launcher webp files
- **Removed** unused ic_launcher_background.xml and ic_launcher_foreground.xml

## How It Works

1. **Single Entry Point**: Only one activity alias has the LAUNCHER intent filter
2. **Dynamic Shortcuts**: Uses Android's ShortcutManager API for icon switching
3. **Preference Persistence**: Icon choice is saved and restored on app restart
4. **Backward Compatibility**: Works on older Android versions (preference-only)

## Benefits

- ✅ **No dual icons** during installation
- ✅ **Clean launcher experience** 
- ✅ **Modern Android approach** using ShortcutManager
- ✅ **Backward compatible** with older Android versions
- ✅ **Maintains existing UI** for icon selection

## Usage

Users can still switch icons through the "My" fragment settings:
- Tap on "App Icon" section
- Choose between "Light" (default) and "Dark" (alternate) icons
- Icon change takes effect immediately on supported devices

## Technical Notes

- **ShortcutManager** requires Android 7.1+ (API 25+) for dynamic icon changes
- **Older devices** will only save the preference (icon change on next app restart)
- **Activity alias** ensures single launcher entry point
- **Component enabling/disabling** is no longer needed
