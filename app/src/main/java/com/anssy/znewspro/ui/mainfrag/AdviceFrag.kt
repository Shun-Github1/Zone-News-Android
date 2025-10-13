package com.anssy.znewspro.ui.mainfrag

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.view.Gravity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseFragment
import com.anssy.znewspro.databinding.FragAdviceBinding
import com.anssy.znewspro.model.PersonRecommendModel
import com.anssy.znewspro.model.TopicModel
import com.anssy.znewspro.ui.topicmodify.TopicSelectionActivity
import com.google.android.material.tabs.TabLayoutMediator

/**
 * @Description 推荐
 * @Author yulu
 * @CreateTime 2025年06月30日 09:27:40
 */

class AdviceFrag : BaseFragment() {
    private lateinit var mViewBinding: FragAdviceBinding
    
    // ViewModels shared with child fragments
    val personRecommendModel: PersonRecommendModel by viewModels()
    val topicModel: TopicModel by viewModels()

    // Modern activity result launcher
    val topicSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == TopicSelectionActivity.RESULT_TOPICS_UPDATED) {
            // Refresh the Your Feed fragment
            val yourFeedFragment = childFragmentManager.fragments.find { it is YourFeedFragment } as? YourFeedFragment
            yourFeedFragment?.refreshData()
        }
    }

    companion object {
        fun getInstance(): AdviceFrag {
            return AdviceFrag()
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mViewBinding = FragAdviceBinding.inflate(inflater)
        return mViewBinding.root
    }

    override fun initData() {
        setupViewPager()
        setupTabs()
    }

    private fun setupViewPager() {
        val adapter = PersonalPagerAdapter(this)
        mViewBinding.personalViewPager.adapter = adapter
    }
    
    private fun setupTabs() {
        val tabLayout = mViewBinding.personalTabLayout
        val tabTitles = listOf(getString(R.string.your_feed_title), getString(R.string.recap_title))
        
        // Link TabLayout with ViewPager2 and set up custom views
        TabLayoutMediator(tabLayout, mViewBinding.personalViewPager) { tab, position ->
            // Create custom view for each tab
            val container = FrameLayout(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            val labelText = TextView(requireContext()).apply {
                text = tabTitles[position]
                gravity = Gravity.CENTER
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSmall))
                typeface = android.graphics.Typeface.DEFAULT
            }
            container.addView(labelText)
            container.tag = labelText
            
            tab.customView = container
        }.attach()

        // Apply bold and size changes via listener (matching HomeFrag styling)
        fun styleTab(tab: com.google.android.material.tabs.TabLayout.Tab?, selected: Boolean) {
            val tv = (tab?.customView?.tag) as? TextView ?: return
            if (selected) {
                tv.setTypeface(tv.typeface, android.graphics.Typeface.BOLD)
                tv.textSize = 16f
                tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextDeep))
            } else {
                tv.setTypeface(null, android.graphics.Typeface.NORMAL)
                tv.textSize = 14f
                tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSmall))
            }
        }

        tabLayout.addOnTabSelectedListener(object: com.google.android.material.tabs.TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                styleTab(tab, true)
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                styleTab(tab, false)
            }
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })

        // Initialize current styles
        tabLayout.post {
            val selected = tabLayout.selectedTabPosition
            for (i in 0 until tabLayout.tabCount) {
                styleTab(tabLayout.getTabAt(i), i == selected)
            }
        }
    }

    /**
     * Refresh the fragment data (delegates to YourFeedFragment)
     */
    fun refreshData() {
        val yourFeedFragment = childFragmentManager.fragments.find { it is YourFeedFragment } as? YourFeedFragment
        yourFeedFragment?.refreshData()
    }
}