package com.anssy.znewspro.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.app.ActivityCompat
import android.widget.FrameLayout
import com.google.android.material.card.MaterialCardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseActivity
import com.anssy.znewspro.databinding.ActivityMainBinding
import com.anssy.znewspro.entry.MainSettingEntry
import com.anssy.znewspro.selfview.popup.SettingPopupWindow
import com.anssy.znewspro.ui.mainfrag.AdviceFrag
import com.anssy.znewspro.ui.mainfrag.HomeFrag
import com.anssy.znewspro.ui.mainfrag.MyFrag
import com.anssy.znewspro.ui.mainfrag.SearchFrag
import com.anssy.znewspro.utils.KLog
import com.anssy.znewspro.utils.MVUtils
import com.anssy.znewspro.utils.PermissionManager
import com.anssy.znewspro.utils.SharedPreferenceUtils
import com.anssy.znewspro.utils.ToastUtils
import com.anssy.znewspro.utils.WeakHandler


import com.hjq.shape.view.ShapeButton
import com.jaeger.library.StatusBarUtil
import razerdp.basepopup.BasePopupWindow
import com.anssy.znewspro.utils.HapticFeedbackHelper
import com.anssy.znewspro.utils.ThemeManager
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import androidx.core.content.ContextCompat


class MainActivity : BaseActivity() {
    private lateinit var mViewBinding: ActivityMainBinding
    private var fragmentList: ArrayList<Fragment> = ArrayList()
    private var isExit: Boolean = false
    private val mHandler: WeakHandler = WeakHandler(Handler.Callback { msg ->
        if (msg.what != 0) {
            return@Callback true
        }
        isExit = false
        true
    })
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
    
    companion object {
        private const val KEY_CURRENT_FRAGMENT = "current_fragment_index"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        // Force the app theme before any view creation
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        
        // Check if user is logged in - if not, redirect to LoginActivity
        if (!SharedPreferenceUtils.getBoolean(this, "isLogin")) {
            val intent = Intent(this, com.anssy.znewspro.ui.login.LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        mViewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)
        // Consistent status bar style
        applyStatusBarStyle()
        
        // Restore fragment state if activity was recreated
        if (savedInstanceState != null) {
            currentFragmentIndex = savedInstanceState.getInt(KEY_CURRENT_FRAGMENT, 0)
        }
        
        initView()
        initClick(savedInstanceState)
        addEventListener()
        
        // Request notification permission
        requestNotificationPermission()
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
        val targetIndex = when (fragmentToShow) {
            "special" -> 1
            "my" -> 2
            "search" -> 3
            else -> {
                // If no intent override and this is a fresh start (not recreation), use landing page preference
                if (savedInstanceState == null) {
                    getLandingPageIndex()
                } else {
                    currentFragmentIndex // Use saved state if recreating
                }
            }
        }
        
        // Update current fragment index
        currentFragmentIndex = targetIndex
        
        // Preload all fragments on startup to prevent lag when switching
        preloadAllFragments(targetIndex)
        
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
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
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
        blurView.setupWith(rootView, RenderScriptBlur(this))
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


    // androidx.appcompat.app.AppCompatActivity
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode != 4) {
            return super.onKeyDown(keyCode, event)
        }
        
        // Check if SearchFrag is active and in search mode
        if (lastFragment == searchFrag && searchFrag?.onBackPressed() == true) {
            return true // SearchFrag consumed the back press
        }
        
        exit()
        return false
    }

    private fun exit() {
        if (!this.isExit) {
            this.isExit = true
            ToastUtils.showShortToast(this, getString(R.string.exit_app_message))
            mHandler.sendEmptyMessageDelayed(0, 2000)
            return
        }
        finishAffinity()
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