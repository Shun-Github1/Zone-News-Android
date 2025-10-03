package com.anssy.znewspro.utils

/**
 * Language enum for the application
 * Maps locale codes to backend language codes
 */
enum class Language(val code: String, val displayName: String) {
    ENGLISH_UK(Constants.LANGUAGE_ENGLISH_UK, "English (UK)"),
    SIMPLIFIED_CHINESE(Constants.LANGUAGE_SIMPLIFIED_CHINESE, "简体中文"),
    TRADITIONAL_CHINESE(Constants.LANGUAGE_TRADITIONAL_CHINESE, "繁體中文"),
    ENGLISH_US(Constants.LANGUAGE_ENGLISH_US, "English (US)"),
    CHINESE_TAIWAN(Constants.LANGUAGE_CHINESE_TAIWAN, "繁體中文 (台灣)");
    
    companion object {
        /**
         * Get language from locale string or tag (e.g., en, en-US, zh-HK, zh-Hant-HK)
         */
        fun fromLocale(locale: String): Language {
            val norm = locale.trim().replace('_', '-').lowercase()
            return when {
                // English
                norm == "en" || norm.startsWith("en-") -> ENGLISH_UK

                // Traditional Chinese: Hant script or HK/MO regions
                norm.startsWith("zh-hant") || norm.endsWith("-hk") || norm.endsWith("-mo") -> TRADITIONAL_CHINESE

                // Taiwan variant
                norm.endsWith("-tw") -> CHINESE_TAIWAN

                // Simplified Chinese: Hans or CN region or generic zh
                norm.startsWith("zh-hans") || norm.endsWith("-cn") || norm == "zh" -> SIMPLIFIED_CHINESE

                // Specific explicit tags
                norm == "zh-cn" -> SIMPLIFIED_CHINESE
                norm == "zh-hk" -> TRADITIONAL_CHINESE
                norm == "zh-tw" -> CHINESE_TAIWAN
                else -> ENGLISH_UK
            }
        }

        /**
         * Get backend language code with fallback support per API changes:
         * - en-US → en-UK
         * - zh-TW → zh-HK (backend uses zh-HK; treat TW as Traditional)
         */
        fun getBackendCode(locale: String): String {
            val norm = locale.trim().replace('_', '-').lowercase()
            return when {
                norm == "en-us" -> Constants.LANGUAGE_ENGLISH_UK
                // All Traditional Chinese variants should use zh-HK on backend
                norm == "zh-tw" || norm.endsWith("-tw") || norm.startsWith("zh-hant") || norm.endsWith("-hk") || norm.endsWith("-mo") -> Constants.LANGUAGE_TRADITIONAL_CHINESE
                else -> fromLocale(locale).code
            }
        }
    }
}
