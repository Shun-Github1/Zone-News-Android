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
    var mBottomView: MaterialCardView? = null
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
    
    companion object {
        private const val KEY_CURRENT_FRAGMENT = "current_fragment_index"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        // Force the app theme before any view creation
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
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
            else -> currentFragmentIndex // Use saved state if no intent override
        }
        
        // Update current fragment index
        currentFragmentIndex = targetIndex
        
        // Show the appropriate fragment (no animation for initial setup)
        when (currentFragmentIndex) {
            1 -> {
                mViewBinding.mainRg.check(R.id.special_rb)
                setFragmentWithAnimation(fragmentList[1], "B", false)
            }
            2 -> {
                mViewBinding.mainRg.check(R.id.my_rb)
                setFragmentWithAnimation(fragmentList[2], "E", false)
            }
            3 -> {
                mViewBinding.mainRg.check(R.id.search_rb)
                setFragmentWithAnimation(fragmentList[3], "C", false)
            }
            else -> {
                mViewBinding.mainRg.check(R.id.home_rb)
                setFragmentWithAnimation(fragmentList[0], "A", false)
                currentFragmentIndex = 0
            }
        }
        
        // Update lastFragment reference to match the current fragment after recreation
        // This ensures the refresh logic works correctly after theme/language changes
        lastFragment = fragmentList[currentFragmentIndex]
        
        // Reset bottom bar state after recreation to ensure auto fade-in works
        if (savedInstanceState != null) {
            resetBottomBarState()
        }
    }

    private fun setFragment(fragment: Fragment, tag: String?) {
        setFragmentWithAnimation(fragment, tag, true)
    }
    
    private fun setFragmentWithAnimation(fragment: Fragment, tag: String?, animate: Boolean) {
        Log.d("MainActivity", "setFragment called - fragment: ${fragment.javaClass.simpleName}, tag: $tag, lastFragment: ${lastFragment?.javaClass?.simpleName}")
        
        val transaction = mFragmentMgr!!.beginTransaction()
        
        // Determine animation direction based on fragment indices
        val targetIndex = fragmentList.indexOf(fragment)
        val currentIndex = if (lastFragment != null) fragmentList.indexOf(lastFragment!!) else -1
        
        // Add animations if requested and we have a valid current fragment
        if (animate && currentIndex >= 0 && targetIndex >= 0 && currentIndex != targetIndex) {
            val slideLeft = targetIndex > currentIndex
            
            if (slideLeft) {
                // Moving right (to higher index) - slide in from right, slide out to left
                transaction.setCustomAnimations(
                    R.anim.slide_in_from_right,
                    R.anim.slide_out_to_left
                )
            } else {
                // Moving left (to lower index) - slide in from left, slide out to right
                transaction.setCustomAnimations(
                    R.anim.slide_in_from_left,
                    R.anim.slide_out_to_right
                )
            }
        }
        
        // Hide current fragment if it exists and is added
        if (lastFragment != null && lastFragment!!.isAdded) {
            transaction.hide(lastFragment!!)
        }
        
        // Check if target fragment already exists
        val existingFragment = mFragmentMgr!!.findFragmentByTag(tag)
        if (existingFragment != null && existingFragment.isAdded) {
            // Fragment exists, just show it
            transaction.show(existingFragment)
            Log.d("MainActivity", "Showing existing fragment: ${existingFragment.javaClass.simpleName}")
        } else {
            // Fragment doesn't exist, add it
            transaction.add(R.id.main_container, fragment, tag)
            Log.d("MainActivity", "Adding new fragment: ${fragment.javaClass.simpleName}")
        }
        
        transaction.commitAllowingStateLoss()
        this.lastFragment = fragment
        
        // Update current fragment index for state persistence
        currentFragmentIndex = fragmentList.indexOf(fragment)
        Log.d("MainActivity", "Fragment transition completed - currentFragmentIndex: $currentFragmentIndex")
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

    private fun addEventListener() {
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