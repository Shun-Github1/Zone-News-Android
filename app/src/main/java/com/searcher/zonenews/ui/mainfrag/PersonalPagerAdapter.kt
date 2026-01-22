package com.searcher.zonenews.ui.mainfrag

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Adapter for ViewPager2 to enable swipe between Your Feed and Recap subpages
 */
class PersonalPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    
    override fun getItemCount(): Int = 2
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> YourFeedFragment()
            1 -> RecapFragment()
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}


