package com.searcher.zonenews.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.os.ConfigurationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Language manager for handling app language preferences
 */
@Singleton
class LanguageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "language_preferences", Context.MODE_PRIVATE
    )
    
    /**
     * Get current app language for API calls
     */
    fun getCurrentLanguageCode(): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ConfigurationCompat.getLocales(context.resources.configuration)[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }

        // Use full language tag if possible (e.g. zh-HK, zh-TW, en-US)
        val localeTag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            locale?.toLanguageTag() ?: "en"
        } else {
            val language = locale?.language ?: "en"
            val country = try { locale?.country } catch (_: Throwable) { null }
            if (!country.isNullOrEmpty()) "$language-$country" else language
        }
        return Language.getBackendCode(localeTag)
    }
    
    /**
     * Get current language enum
     */
    fun getCurrentLanguage(): Language {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ConfigurationCompat.getLocales(context.resources.configuration)[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }

        val localeTag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            locale?.toLanguageTag() ?: "en"
        } else {
            val language = locale?.language ?: "en"
            val country = try { locale?.country } catch (_: Throwable) { null }
            if (!country.isNullOrEmpty()) "$language-$country" else language
        }
        return Language.fromLocale(localeTag)
    }
    
    /**
     * Get saved language preference
     */
    fun getSavedLanguage(): Language {
        val savedCode = sharedPreferences.getString("selected_language", null)
        return if (savedCode != null) {
            Language.values().find { it.code == savedCode } ?: getCurrentLanguage()
        } else {
            getCurrentLanguage()
        }
    }
    
    /**
     * Set language preference
     */
    fun setLanguage(language: Language) {
        sharedPreferences.edit()
            .putString("selected_language", language.code)
            .apply()
    }
}


