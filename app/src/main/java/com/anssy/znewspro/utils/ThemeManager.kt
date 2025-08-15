package com.anssy.znewspro.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager

object ThemeManager {
    
    private const val THEME_MODE_KEY = "theme_mode"
    private const val THEME_LIGHT = "light"
    private const val THEME_DARK = "dark"
    private const val THEME_SYSTEM = "system"
    
    /**
     * Apply the saved theme from SharedPreferences
     */
    fun applySavedTheme(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val themeMode = prefs.getString(THEME_MODE_KEY, THEME_SYSTEM) ?: THEME_SYSTEM
        applyTheme(themeMode)
    }
    
    /**
     * Apply the specified theme mode
     */
    fun applyTheme(themeMode: String) {
        when (themeMode) {
            THEME_LIGHT -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            THEME_DARK -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            THEME_SYSTEM -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                } else {
                    // For devices below Android 10, default to light mode
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }
        }
    }
    
    /**
     * Get the current theme mode from SharedPreferences
     */
    fun getCurrentTheme(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(THEME_MODE_KEY, THEME_SYSTEM) ?: THEME_SYSTEM
    }
    
    /**
     * Save theme mode to SharedPreferences
     */
    fun saveTheme(context: Context, themeMode: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(THEME_MODE_KEY, themeMode).apply()
    }
    
    /**
     * Check if dark mode is currently active
     */
    fun isDarkModeActive(context: Context): Boolean {
        val currentNightMode = context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
}
