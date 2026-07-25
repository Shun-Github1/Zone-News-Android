package com.searcher.zonenews.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages tutorial state across the app
 * Tracks which tutorials have been shown and provides methods to reset them
 */
object TutorialManager {
    
    private const val PREFS_NAME = "tutorial_prefs"
    
    // Tutorial keys for each page
    const val TUTORIAL_HOME = "tutorial_home_shown"
    const val TUTORIAL_YOUR_FEED = "tutorial_your_feed_shown"
    const val TUTORIAL_NEWS_DETAIL = "tutorial_news_detail_shown"
    const val WELCOME_POSTER = "welcome_poster_shown"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * Get a possibly account-specific key for a tutorial
     */
    private fun getAccountSpecificKey(tutorialKey: String, accountId: String?): String {
        return if (accountId != null && accountId.isNotEmpty()) {
            "${accountId}_${tutorialKey}"
        } else {
            tutorialKey
        }
    }

    /**
     * Check if a specific tutorial has been shown
     */
    fun hasTutorialBeenShown(context: Context, tutorialKey: String, accountId: String? = null): Boolean {
        val finalAccountId = accountId ?: SharedPreferenceUtils.getString(context, "current_account_id")
        return getPrefs(context).getBoolean(getAccountSpecificKey(tutorialKey, finalAccountId), false)
    }
    
    /**
     * Mark a specific tutorial as shown
     */
    fun markTutorialAsShown(context: Context, tutorialKey: String, accountId: String? = null) {
        val finalAccountId = accountId ?: SharedPreferenceUtils.getString(context, "current_account_id")
        getPrefs(context).edit().putBoolean(getAccountSpecificKey(tutorialKey, finalAccountId), true).commit()
    }
    
    /**
     * Reset a specific tutorial so it will show again
     */
    fun resetTutorial(context: Context, tutorialKey: String, accountId: String? = null) {
        getPrefs(context).edit().putBoolean(getAccountSpecificKey(tutorialKey, accountId), false).commit()
    }
    
    /**
     * Reset all tutorials for a specific account so they will show again
     */
    fun resetAllTutorials(context: Context, accountId: String? = null) {
        val finalAccountId = accountId ?: SharedPreferenceUtils.getString(context, "current_account_id")
        getPrefs(context).edit()
            .putBoolean(getAccountSpecificKey(TUTORIAL_HOME, finalAccountId), false)
            .putBoolean(getAccountSpecificKey(TUTORIAL_YOUR_FEED, finalAccountId), false)
            .putBoolean(getAccountSpecificKey(TUTORIAL_NEWS_DETAIL, finalAccountId), false)
            .commit()
    }
    
    /**
     * Check if welcome poster has been shown
     */
    fun hasWelcomePosterBeenShown(context: Context, accountId: String? = null): Boolean {
        val finalAccountId = accountId ?: SharedPreferenceUtils.getString(context, "current_account_id")
        val key = getAccountSpecificKey(WELCOME_POSTER, finalAccountId)
        val shown = getPrefs(context).getBoolean(key, false)
        return shown
    }
    
    /**
     * Mark welcome poster as shown
     */
    fun markWelcomePosterShown(context: Context, accountId: String? = null) {
        val finalAccountId = accountId ?: SharedPreferenceUtils.getString(context, "current_account_id")
        val key = getAccountSpecificKey(WELCOME_POSTER, finalAccountId)
        getPrefs(context).edit().putBoolean(key, true).commit()
    }
    
    /**
     * Check if app is freshly installed or tutorial hasn't been shown for this account
     */
    fun isFirstLaunch(context: Context, accountId: String? = null): Boolean {
        return !hasTutorialBeenShown(context, TUTORIAL_HOME, accountId) &&
               !hasTutorialBeenShown(context, TUTORIAL_YOUR_FEED, accountId) &&
               !hasTutorialBeenShown(context, TUTORIAL_NEWS_DETAIL, accountId)
    }
}
