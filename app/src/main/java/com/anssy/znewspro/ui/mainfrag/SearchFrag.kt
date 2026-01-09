package com.anssy.znewspro.ui.mainfrag

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anssy.znewspro.utils.Constants
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseFragment
import com.anssy.znewspro.databinding.FragSearchBinding
import com.anssy.znewspro.entry.SearchListEntry
import com.anssy.znewspro.entry.TopicListEntry
import com.anssy.znewspro.model.SearchModel
import com.anssy.znewspro.model.TopicModel
import com.anssy.znewspro.ui.MainActivity
import com.anssy.znewspro.ui.newsdetail.NewsDetailActivity
import com.anssy.znewspro.utils.CalculateUtil
import com.anssy.znewspro.utils.ToastUtils
import com.anssy.znewspro.utils.Utils
import com.bumptech.glide.Glide
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import com.scwang.smartrefresh.layout.api.RefreshLayout
import com.scwang.smartrefresh.layout.listener.OnRefreshLoadMoreListener
import com.scwang.smartrefresh.layout.listener.OnRefreshListener
import com.google.android.flexbox.FlexboxLayout
import com.hjq.shape.view.ShapeButton
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * @Description 搜寻
 * @Author yulu
 * @CreateTime 2025年06月30日 09:28:48
 */

class SearchFrag : BaseFragment() {
    private val searchModel:SearchModel by viewModels()
    private val topicModel:TopicModel by viewModels()
    private lateinit var mViewBinding:FragSearchBinding
    
    // Search state
    private var isSearchMode = false
    private var hasTypedFirstCharacter = false
    
    // Trending topics from backend - will be populated dynamically
    private var trendingTopics = ArrayList<TopicListEntry.TopicDTO>()
    private lateinit var mTrendingAdapter: CommonAdapter<SearchListEntry.DataDTO.ArticlesDTO>
    private var mTrendingList = ArrayList<SearchListEntry.DataDTO.ArticlesDTO>()
    
    companion object{
        fun  getInstance():SearchFrag{
            return SearchFrag()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mViewBinding = FragSearchBinding.inflate(layoutInflater)
        return mViewBinding.root
    }
    
    // Search results
    private var mProgressWidth = 0
    private lateinit var mAdapter: CommonAdapter<SearchListEntry.DataDTO.ArticlesDTO>
    private var mNewsList = ArrayList<SearchListEntry.DataDTO.ArticlesDTO>()
    
    // Animation timing
    private var refreshStartTime: Long = 0
    private val minimumRefreshDuration = 800L // 800ms minimum duration
    private var isButtonRefresh = false // Flag to prevent double API calls
    
    // Debounce timing to prevent rapid successive refreshes
    private var lastRefreshTime: Long = 0
    private val minimumTimeBetweenRefreshes = 1500L // 1.5 seconds minimum between refreshes
    
    // Auto-search debouncing
    private val searchHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val searchDebounceDelay = 500L // 500ms delay before triggering search
    private var isProgrammaticTextChange = false // Flag to skip auto-search for programmatic text changes

    override fun initData() {
        mProgressWidth = resources.displayMetrics.widthPixels - Utils.dpToPx(36f, resources)
        
        // Set up search functionality
        setupSearchBar()
        
        // Set up trending searches (discover content)
        setupTrendingSearches()
        
        // Setup coming soon overlay for trending topics
        setupComingSoonOverlay()
        
        // Initialize data
        initModel()
        loadDiscoverContent()
        loadTrendingTopics()
    }
    
    private fun setupSearchBar() {
        mViewBinding.searchEt.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Cancel any pending auto-search and perform immediate search
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                performSearch()
            }
            false
        }
        
        // Handle focus changes for search bar styling
        mViewBinding.searchEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Change search bar edge to brand primary color when focused
                updateSearchBarFocusState(true)
                // Don't enter search mode just on focus - wait for user to type
            } else {
                // Reset search bar edge color when not focused
                updateSearchBarFocusState(false)
            }
        }
        
        // Initialize search bar to default state
        updateSearchBarFocusState(false)
        
        // Handle text changes for width animation, search mode, and auto-search
        mViewBinding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrEmpty()
                val query = s?.toString()?.trim() ?: ""
                
                // Cancel any pending search requests
                searchRunnable?.let { searchHandler.removeCallbacks(it) }
                
                // Skip auto-search if this is a programmatic text change (we'll handle search separately)
                if (isProgrammaticTextChange) {
                    isProgrammaticTextChange = false
                    // Still handle UI state changes
                    if (hasText && !isSearchMode) {
                        hasTypedFirstCharacter = true
                        enterSearchMode()
                        animateSearchBarWidth(true)
                    } else if (!hasText && isSearchMode) {
                        hasTypedFirstCharacter = false
                        exitSearchMode()
                        animateSearchBarWidth(false)
                    }
                    return
                }
                
                // Enter search mode when user types something, exit when text is cleared
                if (hasText && !isSearchMode) {
                    // User started typing - enter search mode and hide trending sections
                    hasTypedFirstCharacter = true
                    enterSearchMode()
                    animateSearchBarWidth(true) // Make it narrower
                    // Trigger search immediately for first character
                    triggerAutoSearch(query)
                } else if (!hasText && isSearchMode) {
                    // User cleared text - exit search mode and show trending sections
                    hasTypedFirstCharacter = false
                    exitSearchMode()
                    animateSearchBarWidth(false) // Restore original width
                } else if (hasText && !hasTypedFirstCharacter) {
                    // Edge case: text exists but flag not set
                    hasTypedFirstCharacter = true
                    if (!isSearchMode) {
                        enterSearchMode()
                    }
                    animateSearchBarWidth(true)
                    // Trigger search immediately
                    triggerAutoSearch(query)
                } else if (!hasText && hasTypedFirstCharacter) {
                    // Edge case: no text but flag is set
                    hasTypedFirstCharacter = false
                    if (isSearchMode) {
                        exitSearchMode()
                    }
                    animateSearchBarWidth(false)
                } else if (hasText && isSearchMode) {
                    // User is continuing to type - debounce the search
                    triggerAutoSearch(query)
                }
            }
            
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        
        // Setup cancel button click listener
        mViewBinding.cancelButton.setOnClickListener {
            // Clear the search text - this will trigger text watcher to exit search mode
            mViewBinding.searchEt.setText("")
            mViewBinding.searchEt.clearFocus()
        }
        
        // Setup search button click listener
        mViewBinding.searchButton.setOnClickListener {
            // Cancel any pending auto-search and perform immediate search
            searchRunnable?.let { searchHandler.removeCallbacks(it) }
            performSearch()
        }
    }
    
    /**
     * Update search bar focus state (change edge color while keeping background visible)
     */
    private fun updateSearchBarFocusState(hasFocus: Boolean) {
        val searchLayout = mViewBinding.searchLayout
        val backgroundColor = ContextCompat.getColor(requireContext(), R.color.colorSurfaceVariant)
        
        if (hasFocus) {
            // Keep background visible and add brand primary border when focused
            searchLayout.shapeDrawableBuilder
                .setSolidColor(backgroundColor)
                .setStrokeColor(ContextCompat.getColor(requireContext(), R.color.brand_primary))
                .setStrokeSize(Utils.dpToPx(2f, resources))
                .intoBackground()
        } else {
            // Maintain theme-aware background and remove edge when not focused
            searchLayout.shapeDrawableBuilder
                .setSolidColor(backgroundColor)
                .setStrokeSize(0)
                .intoBackground()
        }
    }
    
    /**
     * Animate search bar width (25% narrower when typing by moving right edge left)
     */
    private fun animateSearchBarWidth(makeNarrower: Boolean) {
        val searchLayout = mViewBinding.searchLayout
        val layoutParams = searchLayout.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        
        if (makeNarrower) {
            // Make it 25% narrower by increasing right margin (moving right edge left)
            val originalRightMargin = Utils.dpToPx(12f, resources) // Original margin from layout
            val currentWidth = searchLayout.width
            val widthReduction = (currentWidth * 0.25f).toInt() // 25% of current width
            val newRightMargin = originalRightMargin + widthReduction
            
            // Animate the right margin change
            val animator = android.animation.ValueAnimator.ofInt(originalRightMargin, newRightMargin)
            animator.duration = 300
            animator.addUpdateListener { animation ->
                val animatedMargin = animation.animatedValue as Int
                layoutParams.rightMargin = animatedMargin
                searchLayout.layoutParams = layoutParams
            }
            animator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    // Fade in the cancel button and search button when animation completes
                    showCancelButton()
                    showSearchButton()
                }
            })
            animator.start()
        } else {
            // Hide cancel button and search button quickly (fade out) before restoring width
            // This prevents the search button from appearing to narrow as the search bar expands
            hideCancelButton()
            
            // Hide search button immediately (set visibility to GONE) to prevent narrowing visual effect
            // Then fade out quickly for smooth transition
            val searchButton = mViewBinding.searchButton
            if (searchButton.visibility == View.VISIBLE) {
                // Set visibility to GONE immediately so it doesn't participate in layout
                searchButton.visibility = View.GONE
                // Reset alpha for next time it's shown
                searchButton.alpha = 1f
            }
            
            // Restore original width by resetting right margin
            val currentRightMargin = layoutParams.rightMargin
            val originalRightMargin = Utils.dpToPx(12f, resources) // Original margin from layout
            
            // Animate back to original right margin
            val animator = android.animation.ValueAnimator.ofInt(currentRightMargin, originalRightMargin)
            animator.duration = 300
            animator.addUpdateListener { animation ->
                val animatedMargin = animation.animatedValue as Int
                layoutParams.rightMargin = animatedMargin
                searchLayout.layoutParams = layoutParams
            }
            animator.start()
        }
    }
    
    /**
     * Show cancel button with fade in animation
     */
    private fun showCancelButton() {
        val cancelButton = mViewBinding.cancelButton
        cancelButton.visibility = View.VISIBLE
        cancelButton.alpha = 0f
        cancelButton.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }
    
    /**
     * Hide cancel button with fade out animation
     */
    private fun hideCancelButton() {
        val cancelButton = mViewBinding.cancelButton
        if (cancelButton.visibility == View.VISIBLE) {
            cancelButton.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    cancelButton.visibility = View.GONE
                }
                .start()
        }
    }
    
    /**
     * Show search button with fade in animation
     */
    private fun showSearchButton() {
        val searchButton = mViewBinding.searchButton
        searchButton.visibility = View.VISIBLE
        searchButton.alpha = 0f
        searchButton.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }
    
    /**
     * Hide search button with fade out animation
     */
    private fun hideSearchButton() {
        val searchButton = mViewBinding.searchButton
        if (searchButton.visibility == View.VISIBLE) {
            searchButton.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    searchButton.visibility = View.GONE
                }
                .start()
        }
    }
    
    /**
     * Hide search button quickly with fast fade out animation
     * Used when search bar is expanding to prevent narrowing visual effect
     */
    private fun hideSearchButtonQuickly() {
        val searchButton = mViewBinding.searchButton
        if (searchButton.visibility == View.VISIBLE) {
            searchButton.animate()
                .alpha(0f)
                .setDuration(100) // Quick fade out
                .withEndAction {
                    searchButton.visibility = View.GONE
                }
                .start()
        }
    }
    
    /**
     * Show search results reminder with query and result count
     */
    private fun showSearchResultsReminder(query: String, resultCount: Int) {
        val reminderText = mViewBinding.searchResultsReminder
        reminderText.text = getString(R.string.found_results_format, resultCount, query)
        reminderText.visibility = View.VISIBLE
    }
    
    /**
     * Hide search results reminder
     */
    private fun hideSearchResultsReminder() {
        val reminderText = mViewBinding.searchResultsReminder
        reminderText.visibility = View.GONE
    }
    
    /**
     * Handle back button press when in search mode or when search bar is focused
     */
    fun onBackPressed(): Boolean {
        // Check if search bar is focused (selected state)
        if (mViewBinding.searchEt.isFocused) {
            val hasText = mViewBinding.searchEt.text.toString().trim().isNotEmpty()
            if (hasText) {
                // Clear the text and focus - this will trigger text watcher to handle state changes
                mViewBinding.searchEt.setText("")
                mViewBinding.searchEt.clearFocus()
                return true // Consumed the back press
            } else {
                // No text, just clear focus
                mViewBinding.searchEt.clearFocus()
                return true // Consumed the back press
            }
        }
        
        // Handle search mode (when not focused but in search mode)
        if (isSearchMode) {
            exitSearchMode()
            return true // Consumed the back press
        }
        
        return false // Let the system handle it
    }
    
    private fun setupComingSoonOverlay() {
        val overlay = mViewBinding.comingSoonOverlay
        val blurView = mViewBinding.comingSoonBlurView
        val icon = mViewBinding.comingSoonIcon
        
        // Start pulse animation on the star icon
        val pulseAnimation = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_animation)
        icon.startAnimation(pulseAnimation)
        
        // Ensure overlay blocks all interactions
        overlay.isClickable = true
        overlay.isFocusable = true
        overlay.setOnClickListener {
            // Block all clicks - do nothing
        }
        
        // Setup frosted glass effect using semi-transparent overlay
        // This avoids hardware acceleration issues that RenderScriptBlur causes
        overlay.post {
            // Use semi-transparent overlay for frosted glass effect
            // The translucent background with opacity provides the frosted glass look
            // Without actual blur processing, this won't affect hardware acceleration
            blurView.setBackgroundResource(R.drawable.coming_soon_overlay)
            blurView.visibility = View.VISIBLE
        }
    }
    
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        // No cleanup needed - RenderEffect doesn't affect hardware acceleration globally
        // For older versions, we're using a simple overlay which doesn't need cleanup
    }
    
    private fun setupTrendingTopics() {
        val flexboxLayout = mViewBinding.trendingTopicsContainer
        
        // Clear existing views
        flexboxLayout.removeAllViews()
        
        // Limit to 6 topics (3 per row, 2 rows max)
        val topicsToShow: List<TopicListEntry.TopicDTO> = trendingTopics.take(6)
        
        topicsToShow.forEach { topic ->
            val button = ShapeButton(requireContext()).apply {
                text = topic.displayName
                textSize = 14f
                setTextColor(resources.getColor(R.color.colorTextDeep, null))
                setPadding(
                    Utils.dpToPx(16f, resources),
                    Utils.dpToPx(8f, resources),
                    Utils.dpToPx(16f, resources),
                    Utils.dpToPx(8f, resources)
                )
                
                // Remove background, make less round, keep border
                val strokeColor = resources.getColor(R.color.main_color, null)
                
                shapeDrawableBuilder
                    .setSolidColor(android.R.color.transparent)
                    .setRadius(Utils.dpToPx(8f, resources).toFloat())
                    .setStrokeColor(strokeColor)
                    .setStrokeSize(1)
                    .intoBackground()
                
                setOnClickListener {
                    // Cancel any pending auto-search
                    searchRunnable?.let { searchHandler.removeCallbacks(it) }
                    // Set flag to skip auto-search in text watcher
                    isProgrammaticTextChange = true
                    // Set text and perform immediate search
                    mViewBinding.searchEt.setText(topic.displayName)
                    // Perform immediate search without debounce
                    performSearch(topic.displayName)
                }
            }
            
            val layoutParams = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, Utils.dpToPx(8f, resources), Utils.dpToPx(8f, resources))
            }
            button.layoutParams = layoutParams
            flexboxLayout.addView(button)
        }
    }
    
    private fun setupTrendingSearches() {
        // Set up trending searches RecyclerView
        mViewBinding.trendingRecycler.layoutManager = LinearLayoutManager(mContext, RecyclerView.VERTICAL, false)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        
        mTrendingAdapter = object :
            CommonAdapter<SearchListEntry.DataDTO.ArticlesDTO>(mContext, R.layout.item_trending_search, mTrendingList) {
            @SuppressLint("SetTextI18n")
            override fun convert(holder: ViewHolder?, t: SearchListEntry.DataDTO.ArticlesDTO, position: Int) {
                val rankingTv: TextView = holder!!.getView(R.id.ranking_tv)
                val titleTv: TextView = holder.getView(R.id.news_title_tv)
                val newsIv: ImageView = holder.getView(R.id.news_iv)
                
                // Set ranking number
                rankingTv.text = (position + 1).toString()
                
                holder.convertView.setOnClickListener {
                    val intent = Intent(mContext, NewsDetailActivity::class.java)
                    intent.putExtra(getString(R.string.id_key), t.articleID)
                    startActivity(intent)
                }
                
                // Add long press listener for sharing with shrink animation
                holder.convertView.setOnLongClickListener {
                    // Animate shrink effect
                    holder.convertView.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(150)
                        .withEndAction {
                            // Restore original size after animation
                            holder.convertView.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(300)
                                .start()
                        }
                        .start()
                    
                    shareArticle(t)
                    true
                }
                
                titleTv.text = t.title
                Glide.with(mContext).load(t.pictureURL).error(R.drawable.ic_image_not_supported_24)
                    .into(newsIv)
            }
        }
        
        mViewBinding.trendingRecycler.adapter = mTrendingAdapter
        
        // Set up search results RecyclerView
        mViewBinding.homeRecycler.layoutManager = LinearLayoutManager(mContext, RecyclerView.VERTICAL, false)
        
        mAdapter = object :
            CommonAdapter<SearchListEntry.DataDTO.ArticlesDTO>(mContext, R.layout.item_search_result, mNewsList) {
            @SuppressLint("SetTextI18n")
            override fun convert(holder: ViewHolder?, t: SearchListEntry.DataDTO.ArticlesDTO, position: Int) {
                val titleTv: TextView = holder!!.getView(R.id.news_title_tv)
                val newsIv: ImageView = holder.getView(R.id.news_iv)
                
                holder.convertView.setOnClickListener {
                    val intent = Intent(mContext, NewsDetailActivity::class.java)
                    intent.putExtra(getString(R.string.id_key), t.articleID)
                    startActivity(intent)
                }
                
                // Add long press listener for sharing with shrink animation
                holder.convertView.setOnLongClickListener {
                    // Animate shrink effect
                    holder.convertView.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(150)
                        .withEndAction {
                            // Restore original size after animation
                            holder.convertView.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(300)
                                .start()
                        }
                        .start()
                    
                    shareArticle(t)
                    true
                }
                
                titleTv.text = t.title
                Glide.with(mContext).load(t.pictureURL).error(R.drawable.ic_image_not_supported_24)
                    .into(newsIv)
            }
        }
        mViewBinding.homeRecycler.adapter = mAdapter
        mViewBinding.homeRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val activity = requireActivity() as MainActivity
                if (dy > 0) {
                    activity.hideBottomBar()
                } else if (dy < 0) {
                    activity.showBottomBar()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val activity = requireActivity() as MainActivity
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    activity.scheduleBottomBarAutoShow()
                } else {
                    activity.cancelBottomBarAutoShow()
                }
            }
        })
        // Remove settings icon from discover page (following iOS design)
        // mViewBinding.settingIv.setOnClickListener {  (requireActivity() as MainActivity).showSettingPop(it) }
        
        // Setup SmartRefreshLayout for pull-to-refresh functionality (search results)
        mViewBinding.smartRefresh.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                // Only proceed if this is a pull-to-refresh, not a button-triggered refresh
                if (!isButtonRefresh) {
                    // Check debounce timing for pull-to-refresh as well
                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastRefresh = currentTime - lastRefreshTime
                    if (timeSinceLastRefresh < minimumTimeBetweenRefreshes) {
                        android.util.Log.d(getString(R.string.search_frag_log_tag), getString(R.string.pull_refresh_blocked_search, timeSinceLastRefresh, minimumTimeBetweenRefreshes))
                        refreshLayout.finishRefresh()
                        return
                    }
                    
                    // Update last refresh time and proceed
                    lastRefreshTime = currentTime
                    refreshStartTime = System.currentTimeMillis()
                    mNewsList.clear()
                    mAdapter.notifyDataSetChanged()
                    searchModel.querySearchList()
                }
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                // Search doesn't support load more, just finish
                refreshLayout.finishLoadMore()
            }
        })
        
        // Setup SmartRefreshLayout for pull-to-refresh functionality (discover content)
        // Disable load more as discover content doesn't support pagination
        mViewBinding.contentSmartRefresh.setEnableLoadMore(false)
        mViewBinding.contentSmartRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                // Only proceed if this is a pull-to-refresh, not a button-triggered refresh
                if (!isButtonRefresh) {
                    // Check debounce timing for pull-to-refresh as well
                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastRefresh = currentTime - lastRefreshTime
                    if (timeSinceLastRefresh < minimumTimeBetweenRefreshes) {
                        android.util.Log.d(getString(R.string.search_frag_log_tag), getString(R.string.pull_refresh_blocked_search, timeSinceLastRefresh, minimumTimeBetweenRefreshes))
                        refreshLayout.finishRefresh()
                        return
                    }
                    
                    // Update last refresh time and proceed
                    lastRefreshTime = currentTime
                    refreshStartTime = System.currentTimeMillis()
                    
                    // Refresh discover content
                    loadDiscoverContent()
                    loadTrendingTopics()
                }
            }
        })
    }
    
    private fun enterSearchMode() {
        isSearchMode = true
        mViewBinding.contentSmartRefresh.visibility = View.GONE
        mViewBinding.smartRefresh.visibility = View.VISIBLE
        // Initially show loading indicator (will be hidden when results arrive)
        mViewBinding.searchResultsProgress.visibility = View.VISIBLE
        mViewBinding.homeRecycler.visibility = View.GONE
    }
    
    private fun exitSearchMode() {
        isSearchMode = false
        mViewBinding.contentSmartRefresh.visibility = View.VISIBLE
        mViewBinding.smartRefresh.visibility = View.GONE
        
        // Reset search bar state
        hasTypedFirstCharacter = false
        
        // Update focus state based on current focus
        // If focused, keep the focused styling (background + border)
        // If not focused, use unfocused styling (background only)
        updateSearchBarFocusState(mViewBinding.searchEt.isFocused)
        
        hideCancelButton()
        hideSearchButton()
        hideSearchResultsReminder()
        animateSearchBarWidth(false)
    }
    
    /**
     * Trigger auto-search with debouncing
     */
    private fun triggerAutoSearch(query: String) {
        // Cancel any pending search
        searchRunnable?.let { searchHandler.removeCallbacks(it) }
        
        // Create new search runnable
        searchRunnable = Runnable {
            performSearch(query)
        }
        
        // Post with debounce delay
        searchHandler.postDelayed(searchRunnable!!, searchDebounceDelay)
    }
    
    /**
     * Perform search with given query
     */
    private fun performSearch(query: String? = null) {
        val searchQuery = query ?: mViewBinding.searchEt.text.toString().trim()
        
        // Show loading indicator and hide RecyclerView (this will also enter search mode if needed)
        showSearchLoading()
        
        // Hide results reminder until we have actual results (prevents flashing "0 results")
        hideSearchResultsReminder()
        
        if (searchQuery.isEmpty()) {
            searchModel.querySearchList()
        } else {
            searchModel.queryListByTitle(searchQuery)
        }
    }
    
    /**
     * Show loading indicator in place of search results
     */
    private fun showSearchLoading() {
        // Ensure we're in search mode (will be set by enterSearchMode if not already)
        if (!isSearchMode) {
            enterSearchMode()
        }
        mViewBinding.searchResultsProgress.visibility = View.VISIBLE
        mViewBinding.homeRecycler.visibility = View.GONE
    }
    
    /**
     * Hide loading indicator and show search results
     */
    private fun hideSearchLoading() {
        mViewBinding.searchResultsProgress.visibility = View.GONE
        mViewBinding.homeRecycler.visibility = View.VISIBLE
    }
    
    private fun loadDiscoverContent() {
        mViewBinding.trendingProgress.visibility = View.VISIBLE
        searchModel.querySearchList()
    }

    private fun loadTrendingTopics() {
        topicModel.getTrendingTopics()
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun initModel(){
        searchModel.searchListEntry.observe(viewLifecycleOwner){
            if (it!=null){
                if (it.code== Constants.SUCCESS_CODE){
                    if (isSearchMode) {
                        // Handle search results
                        mNewsList.clear()
                        mNewsList.addAll(it.data.articles)
                        mAdapter.notifyDataSetChanged()
                        
                        // Hide loading indicator and show results
                        hideSearchLoading()
                        
                        // Update search results reminder with actual count
                        val currentQuery = mViewBinding.searchEt.text.toString().trim()
                        if (currentQuery.isNotEmpty()) {
                            showSearchResultsReminder(currentQuery, it.data.articles.size)
                        }
                        
                        // Finish refresh animation with minimum duration
                        finishRefreshWithMinimumDuration(true)
                    } else {
                        // Handle discover content (trending searches)
                        mTrendingList.clear()
                        mTrendingList.addAll(it.data.articles)
                        mTrendingAdapter.notifyDataSetChanged()
                        mViewBinding.trendingProgress.visibility = View.GONE
                        // Finish refresh animation for discover content
                        finishRefreshWithMinimumDuration(true)
                    }
                }else{
                    if (it.code==1000){
                        ToastUtils.showShortToast(mContext!!, getString(R.string.server_error_message))
                    }else{
                        ToastUtils.showShortToast(mContext!!,it.msg)
                    }
                    
                    if (isSearchMode) {
                        // Hide loading indicator even on error (show empty state or previous results)
                        hideSearchLoading()
                        // Finish refresh animation with error and minimum duration
                        finishRefreshWithMinimumDuration(false)
                    } else {
                        mViewBinding.trendingProgress.visibility = View.GONE
                        // Finish refresh animation for discover content with error
                        finishRefreshWithMinimumDuration(false)
                    }
                }
            }
        }
        
        // Observe trending topics from backend
        topicModel.topicListEntry.observe(viewLifecycleOwner) { topicResponse ->
            if (topicResponse != null && topicResponse.code == Constants.SUCCESS_CODE) {
                trendingTopics.clear()
                trendingTopics.addAll(topicResponse.data.topics)
                setupTrendingTopics()
            } else if (topicResponse != null && topicResponse.code != Constants.SUCCESS_CODE) {
                // Handle error - could show a toast or fallback to default topics
                android.util.Log.w(getString(R.string.search_frag_log_tag), getString(R.string.failed_load_trending, topicResponse.msg))
            }
        }
    }

    /**
     * Refresh the fragment data
     */
    fun refreshData() {
        android.util.Log.d(getString(R.string.search_frag_log_tag), getString(R.string.refreshing_search_fragment))
        
        // Check if fragment is properly attached before accessing ViewModels
        if (!isAdded || isDetached || activity == null) {
            android.util.Log.w(getString(R.string.search_frag_log_tag), getString(R.string.fragment_not_attached_search))
            return
        }
        
        // Check debounce timing to prevent rapid successive refreshes
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRefresh = currentTime - lastRefreshTime
        if (timeSinceLastRefresh < minimumTimeBetweenRefreshes) {
            android.util.Log.d(getString(R.string.search_frag_log_tag), getString(R.string.refresh_blocked_search, timeSinceLastRefresh, minimumTimeBetweenRefreshes))
            return
        }
        
        // Update last refresh time and proceed
        lastRefreshTime = currentTime
        
        // If user clicks discover tab while in search mode, return to discover mode
        if (isSearchMode) {
            android.util.Log.d(getString(R.string.search_frag_log_tag), getString(R.string.exiting_search_mode))
            exitSearchMode()
            
            // Start the refresh animation for discover content
            refreshStartTime = System.currentTimeMillis()
            isButtonRefresh = true
            mViewBinding.contentSmartRefresh.autoRefresh()
            
            // Scroll to top of discover content
            mViewBinding.contentScrollView.post {
                mViewBinding.contentScrollView.scrollTo(0, 0)
            }
            
            loadDiscoverContent()
            loadTrendingTopics()
        } else {
            // Already in discover mode, just refresh the discover content
            android.util.Log.d(getString(R.string.search_frag_log_tag), getString(R.string.refreshing_discover_content))
            
            // Start the refresh animation for discover content
            refreshStartTime = System.currentTimeMillis()
            isButtonRefresh = true
            mViewBinding.contentSmartRefresh.autoRefresh()
            
            // Scroll to top of discover content
            mViewBinding.contentScrollView.post {
                mViewBinding.contentScrollView.scrollTo(0, 0)
            }
            
            loadDiscoverContent()
            loadTrendingTopics()
        }
    }
    
    /**
     * Finish refresh animation with minimum duration to prevent abrupt ending
     */
    private fun finishRefreshWithMinimumDuration(success: Boolean) {
        val elapsed = System.currentTimeMillis() - refreshStartTime
        val remainingTime = minimumRefreshDuration - elapsed
        
        if (remainingTime > 0) {
            // Delay the finish to meet minimum duration
            Handler(Looper.getMainLooper()).postDelayed({
                if (isSearchMode) {
                    mViewBinding.smartRefresh.finishRefresh(success)
                } else {
                    mViewBinding.contentSmartRefresh.finishRefresh(success)
                }
                // Reset button refresh flag when animation completes
                isButtonRefresh = false
            }, remainingTime)
        } else {
            // Already exceeded minimum duration, finish immediately
            if (isSearchMode) {
                mViewBinding.smartRefresh.finishRefresh(success)
            } else {
                mViewBinding.contentSmartRefresh.finishRefresh(success)
            }
            // Reset button refresh flag when animation completes
            isButtonRefresh = false
        }
    }
    
    /**
     * Share article functionality
     */
    private fun shareArticle(article: SearchListEntry.DataDTO.ArticlesDTO) {
        val shareText = buildString {
            append(article.title)
            if (!article.articleURL.isNullOrEmpty()) {
                append("\n\n")
                append(article.articleURL)
            }
        }
        
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
        startActivity(Intent.createChooser(shareIntent, getString(R.string.app_name)))
    }
}