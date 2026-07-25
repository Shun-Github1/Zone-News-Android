package com.searcher.zonenews.ui.mainfrag

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import android.view.Gravity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.searcher.zonenews.R
import com.searcher.zonenews.base.BaseFragment
import com.searcher.zonenews.databinding.FragAdviceBinding
import com.searcher.zonenews.model.PersonRecommendModel
import com.searcher.zonenews.model.TopicModel
import com.searcher.zonenews.model.MyModel
import com.searcher.zonenews.ui.topicmodify.TopicSelectionActivity
import com.searcher.zonenews.utils.SystemDialogUtils
import com.searcher.zonenews.ui.activity.LevityFeedActivity
import com.searcher.zonenews.ui.newsdetail.SubscriptionBottomSheetFragment
import android.content.Intent
import com.searcher.zonenews.utils.Constants
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
    val myModel: MyModel by viewModels()
    
    private var isPro = false

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
        setupLevityButton()
        observeData()
    }
    
    private fun setupLevityButton() {
        val levityCard = mViewBinding.toolbar.findViewById<View>(R.id.levity_card)
        levityCard.setOnClickListener {
            if (isPro) {
                 SystemDialogUtils.showAlertDialog(
                    requireContext(),
                    getString(R.string.levity_enter_dialog_title),
                    getString(R.string.levity_enter_dialog_message),
                    getString(R.string.levity_enter_dialog_confirm),
                    getString(R.string.dialog_button_cancel),
                    onPositiveClick = {
                        startActivity(Intent(requireContext(), LevityFeedActivity::class.java))
                    }
                )
            } else {
                SubscriptionBottomSheetFragment.newInstance(isPro).show(parentFragmentManager, "SubscriptionBottomSheetFragment")
            }
        }
    }
    
    private fun observeData() {
        myModel.myEntry.observe(viewLifecycleOwner) { response ->
            if (response != null && response.code == Constants.SUCCESS_CODE) {
                isPro = response.data?.isPro == true
                updateLevityButtonIcon()
            }
        }
        // Force refresh to get data
        myModel.queryMyFormation()
    }

    private fun updateLevityButtonIcon() {
        val iconView = mViewBinding.toolbar.findViewById<android.widget.ImageView>(R.id.levity_icon)
        if (isPro) {
            iconView.setImageResource(R.drawable.ic_wb_sunny_24)
        } else {
            iconView.setImageResource(R.drawable.lock_24px)
        }
    }

    private fun setupViewPager() {
        val adapter = PersonalPagerAdapter(this)
        mViewBinding.personalViewPager.adapter = adapter
        // Preload all adjacent fragments to prevent lag when switching tabs
        mViewBinding.personalViewPager.offscreenPageLimit = 1
        // Disable swiping to Recap page as it is coming soon
        mViewBinding.personalViewPager.isUserInputEnabled = false
    }
    
    private fun setupTabs() {
        val tabLayout = mViewBinding.personalTabLayout
        val tabTitles = listOf(getString(R.string.your_feed_title), getString(R.string.recap_title))
        
        // Link TabLayout with ViewPager2 and set up custom views
        TabLayoutMediator(tabLayout, mViewBinding.personalViewPager) { tab, position ->
            // Create custom view for each tab
            val container = FrameLayout(requireContext()).apply {
                // Use MATCH_PARENT width so container fills the tab space allocated by TabLayout
                // TabLayout with fixed mode will allocate equal widths, container should use all of it
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                // Add horizontal and top padding to accommodate scaled text (1.1x scale means 10% larger)
                // No bottom padding to prevent pushing divider down - indicator is positioned at bottom via drawable
                val paddingDp = 6f
                val paddingPx = (paddingDp * resources.displayMetrics.density).toInt()
                setPadding(paddingPx, paddingPx, paddingPx, 0)
            }
            val labelText = TextView(requireContext()).apply {
                text = tabTitles[position]
                gravity = Gravity.CENTER
                textSize = 14f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSmall))
                typeface = android.graphics.Typeface.DEFAULT
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
            container.addView(labelText)
            container.tag = labelText
            
            tab.customView = container
        }.attach()

        // Apply bold and size changes via listener (matching HomeFrag styling) with scale animation
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

        tabLayout.addOnTabSelectedListener(object: com.google.android.material.tabs.TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                // Intercept Recap tab (position 1) and show coming soon message
                if (tab.position == 1) {
                    // Prevent navigation to RecapFragment by resetting to position 0
                    mViewBinding.personalViewPager.setCurrentItem(0, false)
                    // Reset tab selection back to position 0 (Your Feed) after a short delay
                    // to ensure TabLayoutMediator has processed the selection
                    tabLayout.post {
                        val yourFeedTab = tabLayout.getTabAt(0)
                        yourFeedTab?.select()
                    }
                    // Show coming soon message
                    com.searcher.zonenews.utils.ToastUtils.showShortToast(requireContext(), getString(R.string.recap_coming_soon))
                    return
                }
                styleTab(tab, true)
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                // Don't unstyle if this is the Recap tab being unselected due to our reset
                if (tab.position != 1) {
                    styleTab(tab, false)
                }
            }
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                // If Recap tab is reselected, show coming soon message again
                if (tab.position == 1) {
                    com.searcher.zonenews.utils.ToastUtils.showShortToast(requireContext(), getString(R.string.recap_coming_soon))
                }
            }
        })

        // Initialize current styles without animation
        tabLayout.post {
            val selected = tabLayout.selectedTabPosition
            for (i in 0 until tabLayout.tabCount) {
                val tab = tabLayout.getTabAt(i)
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

    /**
     * Refresh the fragment data (delegates to YourFeedFragment)
     */
    fun refreshData() {
        val yourFeedFragment = childFragmentManager.fragments.find { it is YourFeedFragment } as? YourFeedFragment
        yourFeedFragment?.refreshData()
    }
}
