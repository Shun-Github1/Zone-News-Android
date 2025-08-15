package com.anssy.znewspro.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.anssy.znewspro.R
import com.anssy.znewspro.utils.ThemeManager
import com.anssy.znewspro.utils.Utils

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    
    private lateinit var themePreference: ListPreference
    
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        
        // Initialize theme preference
        themePreference = findPreference("theme_mode")!!
        updateThemePreferenceSummary()
        
        // Set app version
        val versionPreference: Preference? = findPreference("app_version")
        versionPreference?.summary = "v${Utils.getVersionName(requireContext())}"
    }
    
    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }
    
    override fun onPause() {
        super.onPause()
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }
    
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            "theme_mode" -> {
                val themeMode = sharedPreferences?.getString(key, "system") ?: "system"
                ThemeManager.applyTheme(themeMode)
                updateThemePreferenceSummary()
                
                // Restart the main activity with proper flags to clear all fragments
                val intent = android.content.Intent(requireContext(), com.anssy.znewspro.ui.MainActivity::class.java)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }
    
    private fun updateThemePreferenceSummary() {
        val currentValue = themePreference.value ?: "system"
        val summaryText = when (currentValue) {
            "light" -> getString(R.string.theme_light)
            "dark" -> getString(R.string.theme_dark)
            "system" -> getString(R.string.theme_system)
            else -> getString(R.string.theme_system)
        }
        themePreference.summary = summaryText
    }
}
