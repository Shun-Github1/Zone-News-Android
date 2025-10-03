package com.anssy.znewspro.ui.mainfrag

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
// removed unused ViewGroup import to avoid ambiguity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseFragment
import com.anssy.znewspro.databinding.FragHomeBinding
import com.anssy.znewspro.ui.MainActivity
import com.anssy.znewspro.ui.mainfrag.homechild.HomeChildFrag
import com.anssy.znewspro.ui.notice.NoticeListActivity
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
        
        // Setup MaterialToolbar menu item clicks
        setupToolbar()
        
        val tabLayoutMediator  =TabLayoutMediator(mViewBinding.tabLayout,mViewBinding.homeVp){ tab, position ->
            val container = FrameLayout(requireContext()).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                foregroundGravity = Gravity.CENTER
            }
            val baseText = TextView(requireContext()).apply {
                text = titleGroup[position]
                gravity = Gravity.CENTER
                textSize = 10f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSmall))
                typeface = android.graphics.Typeface.DEFAULT
                alpha = 0f
            }
            val labelText = TextView(requireContext()).apply {
                text = titleGroup[position]
                gravity = Gravity.CENTER
                textSize = 10f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.colorTextSmall))
                typeface = android.graphics.Typeface.DEFAULT
            }
            container.addView(baseText)
            container.addView(labelText)
            container.tag = labelText
            tab.customView = container
        }
        tabLayoutMediator.attach()
        // Apply bold and size changes via listener
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

        mViewBinding.tabLayout.addOnTabSelectedListener(object: com.google.android.material.tabs.TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                styleTab(tab, true)
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab) {
                styleTab(tab, false)
            }

            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab) {}
        })

        // Initialize current styles
        mViewBinding.tabLayout.post {
            val selected = mViewBinding.tabLayout.selectedTabPosition
            for (i in 0 until mViewBinding.tabLayout.tabCount) {
                styleTab(mViewBinding.tabLayout.getTabAt(i), i == selected)
            }
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
     * Refresh all child fragments
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
        
        // Get the actual fragments from the ViewPager adapter instead of using fragmentList
        // The FragmentStateAdapter manages its own fragment instances
        val adapter = mViewBinding.homeVp.adapter as? MyAdapter
        if (adapter != null) {
            // Refresh all child fragments by getting them from the adapter
            for (i in 0 until adapter.itemCount) {
                val fragment = childFragmentManager.findFragmentByTag("f$i")
                if (fragment is HomeChildFrag && fragment.isAdded && !fragment.isDetached && fragment.activity != null) {
                    // Update the fragment's arguments with the new localized string
                    fragment.arguments?.putString("type", titleGroup[i])
                    fragment.refreshData()
                }
            }
        } else {
            // Fallback to the old method if adapter is not available
            fragmentList.forEachIndexed { index, fragment ->
                if (fragment is HomeChildFrag && fragment.isAdded && !fragment.isDetached && fragment.activity != null) {
                    // Update the fragment's arguments with the new localized string
                    fragment.arguments?.putString("type", titleGroup[index])
                    fragment.refreshData()
                }
            }
        }
    }
}