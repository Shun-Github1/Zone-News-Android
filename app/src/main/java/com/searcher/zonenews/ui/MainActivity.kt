package com.searcher.zonenews.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.app.LocaleManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.activity.OnBackPressedCallback
import android.view.ViewGroup
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.app.ActivityCompat
import android.widget.FrameLayout
import com.google.android.material.card.MaterialCardView

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.searcher.zonenews.R
import com.searcher.zonenews.base.BaseActivity
import com.searcher.zonenews.databinding.ActivityMainBinding
import com.searcher.zonenews.entry.MainSettingEntry
import com.searcher.zonenews.selfview.popup.SettingPopupWindow
import com.searcher.zonenews.ui.mainfrag.AdviceFrag
import com.searcher.zonenews.ui.mainfrag.HomeFrag
import com.searcher.zonenews.ui.mainfrag.MyFrag
import com.searcher.zonenews.ui.mainfrag.SearchFrag
import com.searcher.zonenews.utils.KLog
import com.searcher.zonenews.utils.MVUtils
import com.searcher.zonenews.utils.PermissionManager
import com.searcher.zonenews.utils.SharedPreferenceUtils
import com.searcher.zonenews.utils.ToastUtils
import com.searcher.zonenews.utils.WeakHandler


import com.hjq.shape.view.ShapeButton
import com.jaeger.library.StatusBarUtil
import razerdp.basepopup.BasePopupWindow
import com.searcher.zonenews.utils.HapticFeedbackHelper
import com.searcher.zonenews.utils.ThemeManager
import com.searcher.zonenews.widget.CompactNewsWidgetProvider
import com.searcher.zonenews.widget.DetailedNewsWidgetProvider
import com.searcher.zonenews.widget.WidgetDataProvider
import eightbitlab.com.blurview.BlurView

import androidx.core.content.ContextCompat


class MainActivity : BaseActivity() {
    private lateinit var mViewBinding: ActivityMainBinding
    private var fragmentList: ArrayList<Fragment> = ArrayList()
    private val mHandler: WeakHandler = WeakHandler()
    var mBottomView: FrameLayout? = null
    private var isBottomBarHidden: Boolean = false
    private val autoShowRunnable = Runnable { showBottomBar() }
    var lastFragment: Fragment? = null
    private var homeFrag: HomeFrag? = null
    private var adviceFrag: AdviceFrag? = null
    private var searchFrag: SearchFrag? = null
    private var myFrag: MyFrag? = null
    private var mFragmentMgr: FragmentManager? = null
    private lateinit var mSettingPopupWindow: SettingPopupWindow
    
    // Track current fragment index to survive recreate()
    private var currentFragmentIndex = 0
    
    // Sliding indicator for bottom navigation
    private var bottomNavIndicator: View? = null
    
    // Track current locale to detect language changes
    private var currentLocale: String? = null
    
    // Broadcast receiver for per-app language changes (Android 13+)
    private var localeChangeReceiver: BroadcastReceiver? = null
    

    
    companion object {
        private const val KEY_CURRENT_FRAGMENT = "current_fragment_index"
        const val KEY_PRESERVED_FRAGMENT_INDEX = "preserved_fragment_index"
        
        /**
         * Get the current fragment index from MainActivity
         * Used by fragments to preserve navigation state before activity recreation
         */
        fun getCurrentFragmentIndex(activity: MainActivity?): Int {
            return activity?.currentFragmentIndex ?: 0
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        // Force the app theme before any view creation
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        
        // Check if user is logged in - if not, redirect to LoginActivity
        if (!SharedPreferenceUtils.getBoolean(this, "isLogin")) {
            val intent = Intent(this, com.searcher.zonenews.ui.login.LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        // Store current locale on creation
        currentLocale = getCurrentLocaleString()
        
        // Check if locale has changed since last app start or via onConfigurationChanged
        val savedLocale = SharedPreferenceUtils.getString(this, "saved_locale")
        val languageJustChangedFlag = SharedPreferenceUtils.getBoolean(this, "language_just_changed")
        
        Log.d("MainActivity", "onCreate - saved locale: '$savedLocale', current locale: '$currentLocale', flag: $languageJustChangedFlag")
        
        if ((!savedLocale.isNullOrEmpty() && savedLocale != currentLocale) || languageJustChangedFlag) {
            Log.d("MainActivity", "Language change detected - clearing cache and forcing refresh")
            
            // 1. Save the new locale using commit() for reliability before potential process death/restart
            val prefs = getSharedPreferences(SharedPreferenceUtils.FILE_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString("saved_locale", currentLocale).commit()
            
            // 2. Clear language-sensitive caches
            WidgetDataProvider.clearCache()
            
            // 3. Keep the flag for fragments to see
            prefs.edit().putBoolean("language_just_changed", true).commit()
            
            // 4. Force all widgets to refresh immediately
            refreshAllWidgets()
        } else if (savedLocale.isNullOrEmpty()) {
            // First launch - save current locale
            Log.d("MainActivity", "First launch - saving current locale: $currentLocale")
            SharedPreferenceUtils.saveString(this, "saved_locale", currentLocale)
        } else {
            Log.d("MainActivity", "No language change detected")
            // Make sure the flag is cleared if no change is active
            SharedPreferenceUtils.saveBoolean(this, "language_just_changed", false)
        }
        
        // Register broadcast receiver for per-app language changes (Android 13+)
        // This is critical for detecting changes from "System Default" to a specific language
        // where the resulting locale tag may be identical
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeChangedAction = "android.app.action.LOCALE_CHANGED"
            localeChangeReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == localeChangedAction) {
                        Log.d("MainActivity", "ACTION_LOCALE_CHANGED broadcast received - triggering restart")
                        triggerLanguageChangeRestart()
                    }
                }
            }
            registerReceiver(
                localeChangeReceiver,
                IntentFilter(localeChangedAction),
                Context.RECEIVER_NOT_EXPORTED
            )
        }
        
        mViewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)
        // Consistent status bar style
        applyStatusBarStyle()
        
        // Restore fragment state if activity was recreated
        // First check for preserved fragment index (from settings reset), then saved state
        val preservedIndex = SharedPreferenceUtils.getInt(this, KEY_PRESERVED_FRAGMENT_INDEX, -1)
        if (preservedIndex >= 0) {
            // Use preserved fragment index and clear it
            currentFragmentIndex = preservedIndex
            SharedPreferenceUtils.deleteInt(this, KEY_PRESERVED_FRAGMENT_INDEX)
        } else if (savedInstanceState != null) {
            currentFragmentIndex = savedInstanceState.getInt(KEY_CURRENT_FRAGMENT, 0)
        }
        
        initView()
        initClick(savedInstanceState)
        addEventListener()
        setupBackPressHandler()
        

        
        
        // Request notification permission if not just resetting tutorials
        // This prevents re-triggering permission dialogs when the user manually re-launches tips
        val tutorialsResetFlag = SharedPreferenceUtils.getBoolean(this, "tutorials_just_reset")
        val skipPosterFlag = SharedPreferenceUtils.getBoolean(this, "skip_welcome_poster_once")
        
        if (!tutorialsResetFlag && !skipPosterFlag) {
            requestNotificationPermission()
        } else {
            Log.d("MainActivity", "Skipping notification permission request due to tutorial/poster reset flow")
            // We don't clear tutorials_just_reset here because it's needed by other components (e.g. NewsDetailActivity)
            // but we can ensure it doesn't stay around forever if needed. 
            // Actually, MyFrag sets it, and NewsDetailActivity/Fragments should handle their own logic.
        }

        // Handle widget click (explicit intent with article ID extra)
        // This runs before parseDeepLink since it's more reliable
        handleWidgetIntent(intent)

        // Force all widgets to refresh their PendingIntents
        // This fixes stale PendingIntents after cache/data clear
        refreshAllWidgets()

        // Handle deep link with a slight delay to ensure fragments and view hierarchy are stable
        // This prevents race conditions where NewsDetailActivity might be launched before MainActivity is ready
        mHandler.postDelayed({
            if (!isFinishing) {
                parseDeepLink(intent)
            }
        }, 500)
    }
    
    override fun onResume() {
        super.onResume()
        
        // Final fallback check for language/locale changes when returning from system settings
        // onConfigurationChanged might not fire if the resulting locale string is identical,
        // but our getCurrentLocaleString() now tracks the source (system vs app) to force detection.
        val newLocale = getCurrentLocaleString()
        if (currentLocale != null && currentLocale != newLocale) {
            Log.d("MainActivity", "Locale change detected in onResume - old: '$currentLocale', new: '$newLocale'")
            
            val prefs = getSharedPreferences(SharedPreferenceUtils.FILE_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean("language_just_changed", true).commit()
            
            // Re-trigger restart flow
            currentLocale = newLocale
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.putExtra(KEY_CURRENT_FRAGMENT, currentFragmentIndex)
            startActivity(intent)
            finishAffinity()
        }
    }
    
    
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        
        // Since we declared android:configChanges="locale|layoutDirection" in the manifest,
        // Android only calls this method when a locale change HAS ACTUALLY OCCURRED.
        // We don't need to compare locale strings - just trigger the restart unconditionally.
        Log.d("MainActivity", "onConfigurationChanged called - locale change detected, triggering restart")
        
        triggerLanguageChangeRestart()
    }
    
    /**
     * Get current locale identifier including source (system vs app-specific)
     */
    private fun getCurrentLocaleString(config: android.content.res.Configuration? = null): String {
        val usedConfig = config ?: resources.configuration
        val localeTag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val locales = usedConfig.locales
            if (locales.size() > 0) locales[0].toLanguageTag() else "en"
        } else {
            @Suppress("DEPRECATION")
            usedConfig.locale.toLanguageTag()
        }
        
        // Add prefix to distinguish source on Android 13+
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = getSystemService(LocaleManager::class.java)
            val appLocales = localeManager?.applicationLocales
            if (appLocales == null || appLocales.isEmpty) {
                "system_$localeTag"
            } else {
                "app_$localeTag"
            }
        } else {
            localeTag
        }
    }
    
    /**
     * Trigger app restart when language change is detected
     * Used by both onConfigurationChanged and BroadcastReceiver
     */
    private fun triggerLanguageChangeRestart() {
        triggerAppRestart("language_just_changed")
    }

    /**
     * Generic app restart and cache clearing mechanism
     * @param flagKey Optional SharedPreferences boolean key to set to true before restart
     */
    fun triggerAppRestart(flagKey: String? = null) {
        Log.d("MainActivity", "triggerAppRestart() called with flagKey: $flagKey")
        
        val prefs = getSharedPreferences(SharedPreferenceUtils.FILE_NAME, Context.MODE_PRIVATE)
        if (flagKey != null) {
            prefs.edit().putBoolean(flagKey, true).commit()
        }
        
        // Clear widget cache to ensure widgets are also refreshed
        WidgetDataProvider.clearCache()
        refreshAllWidgets()
        
        // Update current locale to match reality
        currentLocale = getCurrentLocaleString()
        
        // Restart the app from the root activity
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.putExtra(KEY_CURRENT_FRAGMENT, currentFragmentIndex)
        startActivity(intent)
        finishAffinity()
    }
    
    override fun onDestroy() {
        // Unregister locale change receiver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && localeChangeReceiver != null) {
            try {
                unregisterReceiver(localeChangeReceiver)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error unregistering locale receiver", e)
            }
        }
        super.onDestroy()
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        
        // Handle fragment navigation from NewsDetailActivity
        val fragmentToShow = intent?.getStringExtra("fragment")
        if (fragmentToShow != null) {
            val targetIndex = when (fragmentToShow) {
                "home" -> 0
                "special" -> 1
                "my" -> 2
                "search" -> 3
                else -> null
            }
            
            if (targetIndex != null && targetIndex != currentFragmentIndex) {
                // Switch to the requested fragment
                val fragmentTags = listOf("A", "B", "E", "C")
                val radioButtonIds = listOf(R.id.home_rb, R.id.special_rb, R.id.my_rb, R.id.search_rb)
                
                mViewBinding.mainRg.check(radioButtonIds[targetIndex])
                setFragment(fragmentList[targetIndex], fragmentTags[targetIndex])
            }
            
            // Clear the fragment extra to prevent re-processing
            intent?.removeExtra("fragment")
            return
        }

        // Handle widget click (explicit intent with article ID extra)
        handleWidgetIntent(intent)

        parseDeepLink(intent)
    }

    /**
     * Handle explicit intent from widget click
     * Widgets use explicit intents with article ID as extra instead of deep links
     * This is more reliable when the app process is killed
     */
    private fun handleWidgetIntent(intent: Intent?) {
        val articleId = intent?.getStringExtra("widget_article_id")
        if (!articleId.isNullOrEmpty()) {
            Log.d("MainActivity", "handleWidgetIntent: Opening article from widget: $articleId")
            // Clear the extra to prevent re-processing
            intent.removeExtra("widget_article_id")
            // Launch NewsDetailActivity with the article ID
            val detailIntent = Intent(this, com.searcher.zonenews.ui.newsdetail.NewsDetailActivity::class.java)
            detailIntent.putExtra("id", articleId)
            startActivity(detailIntent)
        }
    }

    /**
     * Force all widgets to refresh their RemoteViews and PendingIntents
     * This is necessary after the app starts because the launcher may have
     * cached stale PendingIntents from before a cache/data clear
     */
    private fun refreshAllWidgets() {
        try {
            // Trigger a full refresh fetch for all widgets
            val intent = Intent(WidgetDataProvider.ACTION_WIDGET_REFRESH)
            // Specify the package to ensure only our app receives it
            intent.`package` = packageName
            sendBroadcast(intent)
            
            // Also explicitly request provider update
            DetailedNewsWidgetProvider.requestUpdate(this)
            CompactNewsWidgetProvider.requestUpdate(this)
            Log.d("MainActivity", "Triggered full widget refresh")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error refreshing widgets", e)
        }
    }

    private fun parseDeepLink(intent: Intent?) {
        if (intent == null || intent.data == null) {
            Log.d("MainActivity", "parseDeepLink: Intent or Data is null")
            return
        }
        try {
            val uri = intent.data
            Log.d("MainActivity", "parseDeepLink: Processing URI: $uri")
            
            // Handle custom scheme deep links: znews://article/{id} or zonenews://article/{id}
            if (uri?.scheme == "znews" || uri?.scheme == "zonenews") {
                // Clear intent data to prevent double-processing (e.g. on rotation)
                intent.data = null
                
                if (uri.authority == "article") {
                    // znews://article/{id}
                    val pathSegments = uri.pathSegments
                    if (!pathSegments.isNullOrEmpty()) {
                        val articleId = pathSegments[0]
                        Log.d("MainActivity", "parseDeepLink: Found Article ID from custom scheme: $articleId")
                        if (articleId.isNotEmpty()) {
                            val detailIntent = Intent(this, com.searcher.zonenews.ui.newsdetail.NewsDetailActivity::class.java)
                            detailIntent.putExtra("id", articleId)
                            startActivity(detailIntent)
                        }
                    } else {
                         Log.e("MainActivity", "parseDeepLink: Path segments empty or null")
                    }
                } else {
                     Log.d("MainActivity", "parseDeepLink: Unknown authority: ${uri.authority}")
                }
            }
            // Handle HTTPS App Links: https://zonenews.io/...
            else if (uri?.scheme == "https" && (uri.host == "zonenews.io" || uri.host == "www.zonenews.io")) {
                // Clear intent data to prevent double-processing (e.g. on rotation)
                intent.data = null
                
                val pathSegments = uri.pathSegments ?: emptyList()
                val path = uri.path ?: "/"
                Log.d("MainActivity", "parseDeepLink: HTTPS App Link path: $path, segments: $pathSegments")
                
                when {
                    // article/{id}
                    pathSegments.size >= 2 && pathSegments[0] == "article" -> {
                        val articleId = pathSegments[1]
                        Log.d("MainActivity", "parseDeepLink: Opening article: $articleId")
                        if (articleId.isNotEmpty()) {
                            val detailIntent = Intent(this, com.searcher.zonenews.ui.newsdetail.NewsDetailActivity::class.java)
                            detailIntent.putExtra("id", articleId)
                            startActivity(detailIntent)
                        }
                    }
                    // levity/article/{id}
                    pathSegments.size >= 3 && pathSegments[0] == "levity" && pathSegments[1] == "article" -> {
                        val articleId = pathSegments[2]
                        Log.d("MainActivity", "parseDeepLink: Opening levity article: $articleId")
                        if (articleId.isNotEmpty()) {
                            val detailIntent = Intent(this, com.searcher.zonenews.ui.activity.LevityDetailActivity::class.java)
                            detailIntent.putExtra("id", articleId)
                            startActivity(detailIntent)
                        }
                    }
                    // levity or recap -> Advice tab (index 1)
                    path == "/levity" || path == "/recap" -> {
                        navigateToTab(1)
                    }
                    // search -> Search tab (index 3)
                    path == "/search" -> {
                        navigateToTab(3)
                    }
                    // personal or account -> My tab (index 2)
                    path == "/personal" || path.startsWith("/account") -> {
                        navigateToTab(2)
                    }
                    // login
                    path == "/login" -> {
                        startActivity(Intent(this, com.searcher.zonenews.ui.login.LoginActivity::class.java))
                    }
                    // register
                    path == "/register" -> {
                        startActivity(Intent(this, com.searcher.zonenews.ui.login.RegisterActivity::class.java))
                    }
                    // topics
                    path.startsWith("/topics") -> {
                        startActivity(Intent(this, com.searcher.zonenews.ui.topicmodify.TopicSelectionActivity::class.java))
                    }
                    // Home sections: / , /home, /china, /hk, /world, /today
                    path == "/" || path == "/home" || path == "/china" || path == "/hk" || path == "/world" || path == "/today" -> {
                        navigateToTab(0)
                    }
                    else -> {
                        Log.d("MainActivity", "parseDeepLink: Unhandled path: $path")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "parseDeepLink: Error parsing deep link", e)
        }
    }

    /**
     * Helper to navigate to a specific tab index
     */
    private fun navigateToTab(targetIndex: Int) {
        if (targetIndex >= 0 && targetIndex < fragmentList.size) {
            val fragmentTags = listOf("A", "B", "E", "C")
            val radioButtonIds = listOf(R.id.home_rb, R.id.special_rb, R.id.my_rb, R.id.search_rb)
            
            mViewBinding.mainRg.check(radioButtonIds[targetIndex])
            setFragment(fragmentList[targetIndex], fragmentTags[targetIndex])
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_FRAGMENT, currentFragmentIndex)
    }

    private fun initView() {
        mBottomView = mViewBinding.mainBottomCard
        mFragmentMgr = this.supportFragmentManager
        
        // Initialize sliding indicator
        bottomNavIndicator = mViewBinding.bottomNavIndicator
        
        // Setup liquid glass blur effect for bottom navigation bar
        setupBottomBarBlur()
        
        // Clear fragmentList to avoid duplicates
        fragmentList.clear()
        
        // Check if fragments already exist in FragmentManager (recreation scenario)
        val existingHomeFrag = mFragmentMgr?.findFragmentByTag("A") as? HomeFrag
        val existingAdviceFrag = mFragmentMgr?.findFragmentByTag("B") as? AdviceFrag
        val existingMyFrag = mFragmentMgr?.findFragmentByTag("E") as? MyFrag
        val existingSearchFrag = mFragmentMgr?.findFragmentByTag("C") as? SearchFrag
        
        // Use existing fragments if available (recreation), otherwise create new ones
        homeFrag = existingHomeFrag ?: HomeFrag.getInstance()
        fragmentList.add(homeFrag!!)
        adviceFrag = existingAdviceFrag ?: AdviceFrag.getInstance()
        fragmentList.add(adviceFrag!!)
        myFrag = existingMyFrag ?: MyFrag.getInstance()
        fragmentList.add(myFrag!!)
        searchFrag = existingSearchFrag ?: SearchFrag.getInstance()
        fragmentList.add(searchFrag!!)
        lastFragment = homeFrag
        mSettingPopupWindow = SettingPopupWindow(this)
        mSettingPopupWindow.onDismissListener = object : BasePopupWindow.OnDismissListener() {
            override fun onDismiss() {
                Log.e("xxx", "设置弹框消失")
            }

        }
    }

    fun showSettingPop(view: View) {
        mSettingPopupWindow
            .setAlignBackground(true)
            .setOffsetY(10)
            .showPopupWindow(view)
    }

    private fun initClick(savedInstanceState: Bundle?) {
        // Only clear existing fragments if this is NOT a recreation
        // During recreation, we need to preserve fragment state
        if (savedInstanceState == null) {
            // Clear any existing fragments to ensure clean state on fresh start
            val transaction = mFragmentMgr?.beginTransaction()
            val existingFragments = mFragmentMgr?.fragments
            existingFragments?.forEach { fragment ->
                if (fragment != null) {
                    transaction?.remove(fragment)
                }
            }
            transaction?.commit()
        }
        
        // Check if we should show a specific fragment based on intent first
        val fragmentToShow = intent.getStringExtra("fragment")
        val intentFragmentIndex = intent.getIntExtra(KEY_CURRENT_FRAGMENT, -1)
        
        val targetIndex = when {
            // First priority: explicit fragment name from intent
            fragmentToShow != null -> when (fragmentToShow) {
                "home" -> 0
                "special" -> 1
                "my" -> 2
                "search" -> 3
                else -> -1
            }
            // Second priority: fragment index from intent (e.g., from app restart)
            intentFragmentIndex >= 0 -> intentFragmentIndex
            // Third priority: saved state (recreation)
            savedInstanceState != null -> currentFragmentIndex
            // Default: landing page preference
            else -> getLandingPageIndex()
        }
        
        // Update current fragment index
        currentFragmentIndex = targetIndex
        
        // Preload all fragments on startup to prevent lag when switching
        preloadAllFragments(targetIndex)

        // If landing page is Levity Mode and user is Pro, launch LevityFeedActivity
        val landingPage = SharedPreferenceUtils.getString(this, "landing_page")
        if (landingPage == "levity") {
            // We need to check if user is pro. In MainActivity, we can check via myEntry if available or use an intent delay
            // Since initView is called on startup, we check the cached status in SharedPreference if available, 
            // or better, rely on the fact that ONLY pro users can save "levity" as a preference.
            // However, to be safe, we can launch it and LevityFeedActivity will handle its own checks if needed,
            // but the plan says "If 'levity' and user is pro, launch LevityFeedActivity".
            // Let's check MyFormationEntry from SharedPrefs if it exists, or just launch it since free users can't set it.
            val intent = Intent(this, com.searcher.zonenews.ui.activity.LevityFeedActivity::class.java)
            startActivity(intent)
        }
        
        // Update tab backgrounds after setting initial checked state
        mViewBinding.mainRg.post {
            updateTabBackgrounds()
        }
        
        // Update lastFragment reference to match the current fragment after recreation
        // This ensures the refresh logic works correctly after theme/language changes
        lastFragment = fragmentList[currentFragmentIndex]
        
        // Reset bottom bar state after recreation to ensure auto fade-in works
        if (savedInstanceState != null) {
            resetBottomBarState()
        }
    }
    
    /**
     * Preload all fragments on startup to prevent lag when switching between tabs.
     * All fragments are added to the FragmentManager but only the target fragment is shown.
     */
    private fun preloadAllFragments(targetIndex: Int) {
        val transaction = mFragmentMgr?.beginTransaction() ?: return
        
        // Fragment tags and their corresponding indices
        val fragmentTags = listOf("A", "B", "E", "C") // Home, Advice, My, Search
        
        // Add all fragments to the FragmentManager if they don't already exist
        fragmentList.forEachIndexed { index, fragment ->
            val tag = fragmentTags[index]
            val existingFragment = mFragmentMgr?.findFragmentByTag(tag)
            
            if (existingFragment == null || !existingFragment.isAdded) {
                // Fragment doesn't exist, add it (hidden by default)
                transaction.add(R.id.main_container, fragment, tag)
                if (index != targetIndex) {
                    transaction.hide(fragment)
                }
                Log.d("MainActivity", "Preloading fragment: ${fragment.javaClass.simpleName} (tag: $tag)")
            } else {
                // Fragment already exists (recreation scenario), use the existing one
                // Update our reference to use the existing fragment
                when (index) {
                    0 -> homeFrag = existingFragment as? HomeFrag ?: homeFrag
                    1 -> adviceFrag = existingFragment as? AdviceFrag ?: adviceFrag
                    2 -> myFrag = existingFragment as? MyFrag ?: myFrag
                    3 -> searchFrag = existingFragment as? SearchFrag ?: searchFrag
                }
                // Update fragmentList to use existing fragment
                fragmentList[index] = existingFragment
                
                // Ensure it's hidden if not the target
                if (index != targetIndex) {
                    transaction.hide(existingFragment)
                }
            }
        }
        
        // Show the target fragment
        val targetFragment = fragmentList[targetIndex]
        val targetTag = fragmentTags[targetIndex]
        val existingTargetFragment = mFragmentMgr?.findFragmentByTag(targetTag)
        
        val fragmentToShow = existingTargetFragment as? Fragment ?: targetFragment
        transaction.show(fragmentToShow)
        lastFragment = fragmentToShow
        
        // Update the checked state of the RadioGroup
        when (targetIndex) {
            0 -> mViewBinding.mainRg.check(R.id.home_rb)
            1 -> mViewBinding.mainRg.check(R.id.special_rb)
            2 -> mViewBinding.mainRg.check(R.id.my_rb)
            3 -> mViewBinding.mainRg.check(R.id.search_rb)
        }
        
        transaction.commitAllowingStateLoss()
        
        // Update logo visibility
        updateLogoVisibility()
        
        Log.d("MainActivity", "All fragments preloaded. Showing fragment at index: $targetIndex")
    }
    
    /**
     * Get the landing page index based on saved preference
     * Returns: 0 = Home (Today), 1 = Advice (Your Feed), 2 = My, 3 = Search
     */
    private fun getLandingPageIndex(): Int {
        val landingPage = SharedPreferenceUtils.getString(this, "landing_page")
        return when (landingPage) {
            "today", "hongKong", "china" -> 0 // Home tab
            "yourFeed" -> 1 // Advice tab (Your Feed)
            else -> 0 // Default to Home (Today)
        }
    }

    private fun setFragment(fragment: Fragment, tag: String?) {
        setFragmentWithAnimation(fragment, tag, true)
    }
    
    private fun setFragmentWithAnimation(fragment: Fragment, tag: String?, animate: Boolean) {
        Log.d("MainActivity", "setFragment called - fragment: ${fragment.javaClass.simpleName}, tag: $tag, lastFragment: ${lastFragment?.javaClass?.simpleName}")
        
        // Store previous fragment before updating
        val previousFragment = lastFragment
        
        val transaction = mFragmentMgr!!.beginTransaction()
        
        // Add fade animations if requested and we're switching to a different fragment
        if (animate && lastFragment != null && lastFragment != fragment) {
            transaction.setCustomAnimations(
                R.anim.fade_in,
                R.anim.fade_out
            )
        }
        
        // Hide current fragment if it exists and is added
        if (lastFragment != null && lastFragment!!.isAdded) {
            transaction.hide(lastFragment!!)
        }
        
        // Check if target fragment already exists (should always be true after preloading)
        val existingFragment = mFragmentMgr!!.findFragmentByTag(tag)
        if (existingFragment != null && existingFragment.isAdded) {
            // Fragment exists, just show it
            transaction.show(existingFragment)
            Log.d("MainActivity", "Showing existing fragment: ${existingFragment.javaClass.simpleName}")
            this.lastFragment = existingFragment as? Fragment ?: fragment
        } else {
            // Fragment doesn't exist (fallback - should rarely happen after preloading)
            // Add it as a fallback
            transaction.add(R.id.main_container, fragment, tag)
            Log.d("MainActivity", "Adding new fragment (fallback): ${fragment.javaClass.simpleName}")
            this.lastFragment = fragment
        }
        
        transaction.commitAllowingStateLoss()
        
        // Update current fragment index for state persistence
        currentFragmentIndex = fragmentList.indexOf(this.lastFragment ?: fragment)
        Log.d("MainActivity", "Fragment transition completed - currentFragmentIndex: $currentFragmentIndex")
        
        // Hide/show logo based on current fragment with animation
        updateLogoVisibility(previousFragment)
    }
    
    /**
     * Check if a fragment should show the logo
     */
    private fun shouldShowLogo(fragment: Fragment?): Boolean {
        if (fragment == null) return true // Default to showing logo
        return fragment is HomeFrag || fragment is AdviceFrag
    }
    
    private fun updateLogoVisibility(previousFragment: Fragment? = null) {
        val logo = mViewBinding.brandLogo
        val shouldShow = shouldShowLogo(lastFragment)
        val previousShouldShow = shouldShowLogo(previousFragment)
        
        // If both previous and current fragments show the logo, keep it fixed (no animation)
        if (previousShouldShow && shouldShow) {
            // Both show logo - keep it visible and fixed
            logo.visibility = View.VISIBLE
            logo.alpha = 1f
            return
        }
        
        // If both hide the logo, keep it hidden (no animation)
        if (!previousShouldShow && !shouldShow) {
            logo.visibility = View.GONE
            logo.alpha = 0f
            return
        }
        
        // Transitioning between show/hide states - animate
        logo.clearAnimation()
        if (shouldShow) {
            // Fade in: transitioning from hide to show
            logo.visibility = View.VISIBLE
            logo.alpha = 0f
            logo.animate()
                .alpha(1f)
                .setDuration(300) // Match fragment fade animation duration
                .start()
        } else {
            // Fade out: transitioning from show to hide
            logo.animate()
                .alpha(0f)
                .setDuration(300) // Match fragment fade animation duration
                .withEndAction {
                    logo.visibility = View.GONE
                }
                .start()
        }
    }

    fun hideBottomBar() {
        val bar = mBottomView ?: return
        if (isBottomBarHidden) return
        bar.clearAnimation()
        cancelBottomBarAutoShow()
        val params = bar.layoutParams
        val bottomMargin = if (params is ViewGroup.MarginLayoutParams) params.bottomMargin else 0
        val translationDistance = (bar.height + bottomMargin).toFloat()
        bar.animate().cancel()
        bar.animate()
            .translationY(translationDistance)
            .alpha(0f)
            .setDuration(200)
            .start()
        isBottomBarHidden = true
    }

    fun showBottomBar() {
        val bar = mBottomView ?: return
        if (!isBottomBarHidden) return
        bar.clearAnimation()
        bar.animate().cancel()
        bar.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(200)
            .start()
        isBottomBarHidden = false
    }

    fun scheduleBottomBarAutoShow(delayMs: Long = 2500) {
        val bar = mBottomView ?: return
        bar.removeCallbacks(autoShowRunnable)
        bar.postDelayed(autoShowRunnable, delayMs)
    }

    fun cancelBottomBarAutoShow() {
        mBottomView?.removeCallbacks(autoShowRunnable)
    }
    
    /**
     * Reset bottom bar state after recreation
     */
    fun resetBottomBarState() {
        val bar = mBottomView ?: return
        bar.clearAnimation()
        bar.animate().cancel()
        bar.translationY = 0f
        bar.alpha = 1f
        isBottomBarHidden = false
        // Schedule auto-show after a short delay to ensure proper initialization
        scheduleBottomBarAutoShow(1000)
    }
    
    /**
     * Setup blur effect for bottom navigation bar to achieve liquid glass appearance
     */
    private fun setupBottomBarBlur() {
        val blurView = mViewBinding.mainBottomBlurView ?: return
        val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val glassOverlay = findViewById<View>(R.id.mainBottomGlassOverlay)
        val cardView = mBottomView
        
        // Ensure FrameLayout is transparent
        cardView?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        
        // Setup fully rounded corners outline for clipping and elevation for shadow (pill-shaped)
        cardView?.post {
            val cornerRadiusPx = (28 * resources.displayMetrics.density).toInt()
            cardView?.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx.toFloat())
                }
            }
            cardView?.clipToOutline = true
            
            // Add subtle elevation for shadow effect (2dp for slight shadow)
            cardView?.elevation = 2 * resources.displayMetrics.density
        }
        
        // Set rounded background drawable on BlurView for proper clipping
        blurView.setBackgroundResource(R.drawable.bottom_nav_rounded_background)
        
        // Configure blur view to blur content behind the navigation bar
        @Suppress("DEPRECATION")
        blurView.setupWith(rootView, eightbitlab.com.blurview.RenderScriptBlur(this))
            .setBlurRadius(20f) // Blur radius for frosted glass effect
            .setBlurAutoUpdate(true) // Automatically update blur when content changes
        
        // Set translucent background drawable on the overlay view based on theme
        // Using drawable instead of solid color to ensure rounded corners work properly
        val glassOverlayDrawable = if (ThemeManager.isDarkModeActive(this)) {
            ContextCompat.getDrawable(this, R.drawable.bottom_nav_glass_overlay_dark)
        } else {
            ContextCompat.getDrawable(this, R.drawable.bottom_nav_glass_overlay_light)
        }
        
        // Apply translucent background drawable to overlay view for liquid glass effect
        glassOverlay?.background = glassOverlayDrawable
        
        // Setup selected tab highlight backgrounds
        updateTabBackgrounds()
        
        // Setup sliding indicator
        setupSlidingIndicator()
    }
    
    /**
     * Setup the sliding indicator for bottom navigation
     */
    private fun setupSlidingIndicator() {
        val indicator = bottomNavIndicator ?: return
        val isDarkMode = ThemeManager.isDarkModeActive(this)
        val selectedDrawable = if (isDarkMode) {
            ContextCompat.getDrawable(this, R.drawable.bottom_nav_tab_selected_dark)
        } else {
            ContextCompat.getDrawable(this, R.drawable.bottom_nav_tab_selected_light)
        }
        
        indicator.background = selectedDrawable
        
        // Wait for layout to be measured, then position indicator
        mViewBinding.mainRg.post {
            updateIndicatorPosition(animate = false)
        }
    }
    
    /**
     * Update the position of the sliding indicator to match the selected tab
     */
    private fun updateIndicatorPosition(animate: Boolean = true) {
        val indicator = bottomNavIndicator ?: return
        val radioGroup = mViewBinding.mainRg
        
        // Find the checked RadioButton
        val checkedId = radioGroup.checkedRadioButtonId
        val checkedButton = radioGroup.findViewById<RadioButton>(checkedId) ?: return
        
        // Get the RadioButton's position relative to the RadioGroup
        val buttonLeft = checkedButton.left.toFloat()
        val buttonWidth = checkedButton.width.toFloat()
        
        // Calculate indicator position
        // The indicator and RadioGroup are both children of the same FrameLayout
        // buttonLeft is relative to RadioGroup, so we add RadioGroup's position
        // The indicator has a 4dp margin, so we need to account for that
        val radioGroupLeft = radioGroup.left.toFloat()
        val indicatorMargin = 4 * resources.displayMetrics.density // 4dp margin
        val targetX = radioGroupLeft + buttonLeft - indicatorMargin
        val targetWidth = buttonWidth
        
        // Update indicator width and position
        if (animate) {
            // Cancel any ongoing animations
            indicator.animate().cancel()
            
            // Get current width for smooth animation
            val currentWidth = if (indicator.width > 0) indicator.width.toFloat() else targetWidth
            val currentX = indicator.translationX
            
            // Use ValueAnimator to animate both width and position together
            val widthAnimator = android.animation.ValueAnimator.ofFloat(currentWidth, targetWidth)
            val xAnimator = android.animation.ValueAnimator.ofFloat(currentX, targetX)
            
            widthAnimator.duration = 250
            widthAnimator.interpolator = android.view.animation.DecelerateInterpolator()
            widthAnimator.addUpdateListener { animator ->
                val width = animator.animatedValue as Float
                val params = indicator.layoutParams
                params.width = width.toInt()
                indicator.layoutParams = params
            }
            
            xAnimator.duration = 250
            xAnimator.interpolator = android.view.animation.DecelerateInterpolator()
            xAnimator.addUpdateListener { animator ->
                indicator.translationX = animator.animatedValue as Float
            }
            
            // Start both animations together
            widthAnimator.start()
            xAnimator.start()
        } else {
            // Set position immediately (for initial setup)
            indicator.translationX = targetX
            val layoutParams = indicator.layoutParams
            layoutParams.width = targetWidth.toInt()
            indicator.layoutParams = layoutParams
            indicator.visibility = View.VISIBLE
        }
    }
    
    /**
     * Update RadioButton backgrounds - now just remove backgrounds since indicator handles highlighting
     */
    private fun updateTabBackgrounds() {
        val radioGroup = mViewBinding.mainRg
        val radioButtons = listOf(
            radioGroup.findViewById<RadioButton>(R.id.home_rb),
            radioGroup.findViewById<RadioButton>(R.id.special_rb),
            radioGroup.findViewById<RadioButton>(R.id.my_rb),
            radioGroup.findViewById<RadioButton>(R.id.search_rb)
        )
        
        // Remove backgrounds from all tabs - indicator will show selection
        radioButtons.forEach { radioButton ->
            radioButton?.background = null
        }
        
        // Update indicator position when selection changes
        updateIndicatorPosition(animate = true)
    }

    private fun addEventListener() {
        // Set up RadioGroup checked change listener to update tab backgrounds
        mViewBinding.mainRg.setOnCheckedChangeListener { _, _ ->
            updateTabBackgrounds()
        }
        
        // Use post to ensure RadioButtons are fully initialized
        mViewBinding.mainRg.post {
            // Set individual click listeners for each RadioButton to enable refresh functionality
            mViewBinding.mainRg.findViewById<RadioButton>(R.id.home_rb).setOnClickListener { view ->
                // Provide haptic feedback for navigation
                HapticFeedbackHelper.performNavigationHaptic(view)
                
                Log.d("MainActivity", "Home button clicked, lastFragment: ${lastFragment?.javaClass?.simpleName}")
                if (currentFragmentIndex == 0) {
                    // Already on home fragment, refresh it
                    (fragmentList[0] as? HomeFrag)?.refreshData()
                } else {
                    mViewBinding.mainRg.check(R.id.home_rb)
                    setFragment(fragmentList[0], "A")
                }
            }

            mViewBinding.mainRg.findViewById<RadioButton>(R.id.special_rb).setOnClickListener { view ->
                // Provide haptic feedback for navigation
                HapticFeedbackHelper.performNavigationHaptic(view)
                
                Log.d("MainActivity", "Special button clicked, lastFragment: ${lastFragment?.javaClass?.simpleName}")
                if (lastFragment == fragmentList[1]) {
                    // Refresh the advice fragment
                    Log.d("MainActivity", "Refreshing advice fragment")
                    (fragmentList[1] as? AdviceFrag)?.refreshData()
                } else {
                    Log.d("MainActivity", "Navigating to advice fragment")
                    mViewBinding.mainRg.check(R.id.special_rb)
                    setFragment(fragmentList[1], "B")
                }
            }

            mViewBinding.mainRg.findViewById<RadioButton>(R.id.my_rb).setOnClickListener { view ->
                // Provide haptic feedback for navigation
                HapticFeedbackHelper.performNavigationHaptic(view)
                
                Log.d("MainActivity", "My button clicked, lastFragment: ${lastFragment?.javaClass?.simpleName}")
                if (lastFragment == fragmentList[2]) {
                    // Refresh the my fragment
                    Log.d("MainActivity", "Refreshing my fragment")
                    (fragmentList[2] as? MyFrag)?.refreshData()
                } else {
                    Log.d("MainActivity", "Navigating to my fragment")
                    mViewBinding.mainRg.check(R.id.my_rb)
                    setFragment(fragmentList[2], "E")
                }
            }

            mViewBinding.mainRg.findViewById<RadioButton>(R.id.search_rb).setOnClickListener { view ->
                // Provide haptic feedback for navigation
                HapticFeedbackHelper.performNavigationHaptic(view)
                
                Log.d("MainActivity", "Search button clicked, lastFragment: ${lastFragment?.javaClass?.simpleName}")
                if (lastFragment == fragmentList[3]) {
                    // Refresh the search fragment
                    Log.d("MainActivity", "Refreshing search fragment")
                    (fragmentList[3] as? SearchFrag)?.refreshData()
                } else {
                    Log.d("MainActivity", "Navigating to search fragment")
                    mViewBinding.mainRg.check(R.id.search_rb)
                    setFragment(fragmentList[3], "C")
                }
            }
        }
    }


    /**
     * Setup back press handler using OnBackPressedDispatcher.
     * This integrates properly with Android's predictive back gesture system.
     * The callback stays always enabled to avoid registration overhead that causes lag.
     */
    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Check if SearchFrag is active and in search mode
                if (lastFragment == searchFrag && searchFrag?.onBackPressed() == true) {
                    return // SearchFrag consumed the back press
                }
                
                // Double back to exit functionality removed as requested.
                // Fallback to default back behavior (usually finishes the activity)
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        })
    }

    private fun requestNotificationPermission() {
        if (!PermissionManager.hasNotificationPermission(this)) {
            PermissionManager.requestNotificationPermission(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            PermissionManager.NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity", "Notification permission granted")
                    ToastUtils.showShortToast(this, "Notification permission granted")
                } else {
                    Log.d("MainActivity", "Notification permission denied")
                    ToastUtils.showShortToast(this, "Notification permission denied. You can enable it in settings.")
                }
            }
        }
    }

}
