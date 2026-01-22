package com.searcher.zonenews.ui.mainfrag

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
// removed unused ViewGroup import to avoid ambiguity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.searcher.zonenews.R
import com.searcher.zonenews.base.BaseFragment
import com.searcher.zonenews.databinding.FragHomeBinding
import com.searcher.zonenews.ui.MainActivity
import com.searcher.zonenews.ui.mainfrag.homechild.HomeChildFrag
import com.searcher.zonenews.ui.notice.NoticeListActivity
import com.searcher.zonenews.utils.SharedPreferenceUtils
import com.google.android.material.tabs.TabLayoutMediator
import android.widget.TextView
import android.view.Gravity
import androidx.core.content.ContextCompat
import android.widget.FrameLayout
import android.view.ViewGroup

/**
 * @Description 主界面
 * @Author yulu
 * @CreateTime 2025年06月30日 08:50:12
 */

class HomeFrag : BaseFragment() {
    private lateinit var mViewBinding:FragHomeBinding
    private var fragmentList = ArrayList<Fragment>()
    private var titleGroup = arrayOf<String>()
    companion object{
        fun  getInstance():HomeFrag{
            return HomeFrag()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mViewBinding = FragHomeBinding.inflate(inflater)
        return mViewBinding.root
    }

    override fun initData() {
       titleGroup = arrayOf(getString(R.string.today), getString(R.string.hongkong),
            getString(
                R.string.china
            ))
        titleGroup.forEach {
            fragmentList.add(HomeChildFrag.getInstance(it))
        }
        mViewBinding.homeVp.adapter = MyAdapter(this)
        mViewBinding.homeVp.isUserInputEnabled = true
        // Preload all adjacent fragments to prevent lag when switching tabs
        mViewBinding.homeVp.offscreenPageLimit = 2
        
        // Set initial tab based on landing page preference (only on first load)
        setInitialTabFromPreference()
        
        // Setup MaterialToolbar menu item clicks
        setupToolbar()
        
        val tabLayoutMediator  =TabLayoutMediator(mViewBinding.tabLayout,mViewBinding.homeVp){ tab, position ->
            val container = FrameLayout(requireContext()).apply {
                // Use MATCH_PARENT width so container fills the tab space allocated by TabLayout
                // TabLayout with fixed mode will allocate equal widths, container should use all of it
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                foregroundGravity = Gravity.CENTER
                // Add horizontal and top padding to accommodate scaled text (1.1x scale means 10% larger)
                // No bottom padding to prevent pushing divider down - indicator is positioned at bottom via drawable
                val paddingDp = 6f
                val paddingPx = (paddingDp * resources.displayMetrics.density).toInt()
                setPadding(paddingPx, paddingPx, paddingPx, 0)
            }
            val baseText = TextView(requireContext()).apply {
                text = titleGroup[position]
                gravity = Gravity.CENTER
                textSize = 10f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSmall))
                typeface = android.graphics.Typeface.DEFAULT
                alpha = 0f
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
            val labelText = TextView(requireContext()).apply {
                text = titleGroup[position]
                gravity = Gravity.CENTER
                textSize = 10f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSmall))
                typeface = android.graphics.Typeface.DEFAULT
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
            container.addView(baseText)
            container.addView(labelText)
            container.tag = labelText
            tab.customView = container
        }
        tabLayoutMediator.attach()
        // Apply bold and size changes via listener with scale animation
        fun styleTab(tab: com.google.android.material.tabs.TabLayout.Tab?, selected: Boolean) {
            val tv = (tab?.customView?.tag) as? TextView ?: return
            if (selected) {
                tv.setTypeface(tv.typeface, android.graphics.Typeface.BOLD)
                tv.textSize = 16f
                tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextDeep))
                // Animate scale to 1.1x (reduced from 1.2x)
                animateTabScale(tv, 1.0f, 1.1f)
            } else {
                tv.setTypeface(null, android.graphics.Typeface.NORMAL)
                tv.textSize = 14f
                tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSmall))
                // Animate scale back to 1.0x
                animateTabScale(tv, 1.1f, 1.0f)
            }
        }

        mViewBinding.tabLayout.addOnTabSelectedListener(object: com.google.android.material.tabs.TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                styleTab(tab, true)
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                styleTab(tab, false)
            }

            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })

        // Initialize current styles without animation
        mViewBinding.tabLayout.post {
            val selected = mViewBinding.tabLayout.selectedTabPosition
            for (i in 0 until mViewBinding.tabLayout.tabCount) {
                val tab = mViewBinding.tabLayout.getTabAt(i)
                val tv = (tab?.customView?.tag) as? TextView ?: continue
                val isSelected = i == selected
                if (isSelected) {
                    tv.setTypeface(tv.typeface, android.graphics.Typeface.BOLD)
                    tv.textSize = 16f
                    tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextDeep))
                } else {
                    tv.setTypeface(null, android.graphics.Typeface.NORMAL)
                    tv.textSize = 14f
                    tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSmall))
                }
                // Set initial scale without animation for first load
                tv.scaleX = if (isSelected) 1.1f else 1.0f
                tv.scaleY = if (isSelected) 1.1f else 1.0f
            }
        }
    }

    /**
     * Helper function to animate tab text scale
     */
    private fun animateTabScale(textView: TextView, startScale: Float, endScale: Float) {
        val scaleX = ObjectAnimator.ofFloat(textView, "scaleX", startScale, endScale).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleY = ObjectAnimator.ofFloat(textView, "scaleY", startScale, endScale).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
        }
        AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            start()
        }
    }

    private fun setupToolbar() {
        val toolbar = mViewBinding.toolbar
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_notifications -> {
                    startActivity(Intent(mContext, NoticeListActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private inner class MyAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = titleGroup.size

        override fun createFragment(position: Int): Fragment {
            return fragmentList[position]
        }
    }

    /**
     * Set the current tab in the Home fragment (0 = Today, 1 = Hong Kong, 2 = China)
     */
    fun setCurrentTab(tabIndex: Int) {
        if (tabIndex in 0..2 && mViewBinding.homeVp.adapter != null) {
            mViewBinding.homeVp.setCurrentItem(tabIndex, false)
        }
    }
    
    /**
     * Set initial tab based on landing page preference (only called on first load)
     */
    private fun setInitialTabFromPreference() {
        // Only set initial tab if this is the first time the fragment is created
        // Check if we've already set the initial tab by checking if ViewPager has a current item
        if (mViewBinding.homeVp.currentItem == 0) {
            val landingPage = SharedPreferenceUtils.getString(mContext, "landing_page")
            val subTabIndex = when (landingPage) {
                "today" -> 0 // Today tab
                "hongKong" -> 1 // Hong Kong tab
                "china" -> 2 // China tab
                else -> 0 // Default to Today
            }
            // Use post to ensure ViewPager is ready
            mViewBinding.homeVp.post {
                if (subTabIndex != 0) {
                    mViewBinding.homeVp.setCurrentItem(subTabIndex, false)
                }
            }
        }
    }
    
    /**
     * Refresh only the currently visible child fragment
     */
    fun refreshData() {
        android.util.Log.d("HomeFrag", "Refreshing home fragment data")
        // Check if this fragment is properly attached before refreshing child fragments
        if (!isAdded || isDetached || activity == null) {
            android.util.Log.w("HomeFrag", "Fragment not properly attached, skipping refresh")
            return
        }
        
        // Update titleGroup with current localized strings
        titleGroup = arrayOf(getString(R.string.today), getString(R.string.hongkong),
            getString(R.string.china))
        
        // Only refresh the currently visible fragment to avoid performance issues
        // Refreshing all three fragments at once causes lag from 3 simultaneous API calls and adapter updates
        val currentPosition = mViewBinding.homeVp.currentItem
        val fragment = childFragmentManager.findFragmentByTag("f$currentPosition")
        if (fragment is HomeChildFrag && fragment.isAdded && !fragment.isDetached && fragment.activity != null) {
            // Update the fragment's arguments with the new localized string
            fragment.arguments?.putString("type", titleGroup[currentPosition])
            fragment.refreshData()
        }
    }
}
