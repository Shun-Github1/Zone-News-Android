package com.anssy.znewspro.utils

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * Utility class to manage dynamic app icon changes using shortcuts
 */
object AppIconManager {
    
    private const val TAG = "AppIconManager"
    
    // Activity alias component names
    private const val DEFAULT_LAUNCHER_ALIAS = "com.anssy.znewspro.MainActivityLauncher"
    private const val ALTERNATE_LAUNCHER_ALIAS = "com.anssy.znewspro.MainActivityLauncherAlt"
    
    // Preference key for storing the selected icon
    private const val PREF_SELECTED_ICON = "selected_app_icon"
    
    // Icon resource names - these are the actual mipmap resources that exist
    private const val DEFAULT_ICON = "ic_launcher"
    private const val ALTERNATE_ICON = "ic_launcher_alt"
    
    /**
     * Change the app icon to the specified type
     * @param context Application context
     * @param iconType "default" or "alternate"
     * @return true if successful, false otherwise
     */
    fun changeAppIcon(context: Context, iconType: String): Boolean {
        return try {
            val packageManager = context.packageManager
            
            when (iconType) {
                "default" -> {
                    // Enable default launcher alias, disable alternate
                    packageManager.setComponentEnabledSetting(
                        ComponentName(context, DEFAULT_LAUNCHER_ALIAS),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    packageManager.setComponentEnabledSetting(
                        ComponentName(context, ALTERNATE_LAUNCHER_ALIAS),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    Log.d(TAG, "App icon changed to default")
                }
                "alternate" -> {
                    // Enable alternate launcher alias, disable default
                    packageManager.setComponentEnabledSetting(
                        ComponentName(context, ALTERNATE_LAUNCHER_ALIAS),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    packageManager.setComponentEnabledSetting(
                        ComponentName(context, DEFAULT_LAUNCHER_ALIAS),
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                    Log.d(TAG, "App icon changed to alternate")
                }
                else -> {
                    Log.e(TAG, "Invalid icon type: $iconType")
                    return false
                }
            }
            
            // Save the selection
            SharedPreferenceUtils.saveString(context, PREF_SELECTED_ICON, iconType)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error changing app icon", e)
            false
        }
    }
    

    
    /**
     * Get the currently selected app icon type
     * @param context Application context
     * @return "default" or "alternate"
     */
    fun getCurrentIconType(context: Context): String {
        return SharedPreferenceUtils.getString(context, PREF_SELECTED_ICON).ifEmpty { "default" }
    }
    
    /**
     * Check if the app icon change was successful
     * @param context Application context
     * @param iconType The icon type that was requested
     * @return true if the icon is currently active
     */
    fun isIconActive(context: Context, iconType: String): Boolean {
        return try {
            val packageManager = context.packageManager
            
            when (iconType) {
                "default" -> {
                    val componentName = ComponentName(context, DEFAULT_LAUNCHER_ALIAS)
                    packageManager.getComponentEnabledSetting(componentName) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                }
                "alternate" -> {
                    val componentName = ComponentName(context, ALTERNATE_LAUNCHER_ALIAS)
                    packageManager.getComponentEnabledSetting(componentName) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking icon status", e)
            false
        }
    }
    
    /**
     * Restore the app icon to the last saved preference
     * @param context Application context
     * @return true if successful, false otherwise
     */
    fun restoreSavedIcon(context: Context): Boolean {
        val savedIcon = getCurrentIconType(context)
        return changeAppIcon(context, savedIcon)
    }
    
    /**
     * Initialize the app icon state based on saved preference
     * @param context Application context
     */
    fun initializeAppIcon(context: Context) {
        val savedIcon = getCurrentIconType(context)
        if (savedIcon == "alternate") {
            // If alternate icon was saved, enable it
            changeAppIcon(context, "alternate")
        } else {
            // Ensure default icon is enabled
            changeAppIcon(context, "default")
        }
    }
    
    /**
     * Get the current icon resource ID
     * @param context Application context
     * @return Resource ID of the current icon
     */
    fun getCurrentIconResourceId(context: Context): Int {
        val iconType = getCurrentIconType(context)
        val iconName = when (iconType) {
            "default" -> DEFAULT_ICON
            "alternate" -> ALTERNATE_ICON
            else -> DEFAULT_ICON
        }
        return context.resources.getIdentifier(iconName, "mipmap", context.packageName)
    }
    

}
