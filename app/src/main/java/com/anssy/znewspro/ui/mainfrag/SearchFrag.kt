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

    override fun initData() {
        mProgressWidth = resources.displayMetrics.widthPixels - Utils.dpToPx(36f, resources)
        
        // Set up search functionality
        setupSearchBar()
        
        // Set up trending searches (discover content)
        setupTrendingSearches()
        
        // Initialize data
        initModel()
        loadDiscoverContent()
        loadTrendingTopics()
    }
    
    private fun setupSearchBar() {
        mViewBinding.searchEt.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
            }
            false
        }
        
        // Handle focus changes for search bar styling
        mViewBinding.searchEt.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Change search bar edge to brand primary color when focused
                updateSearchBarFocusState(true)
                // When user taps search, show search UI
                if (!isSearchMode) {
                    enterSearchMode()
                }
            } else {
                // Reset search bar edge color when not focused
                updateSearchBarFocusState(false)
            }
        }
        
        // Initialize search bar to default state
        updateSearchBarFocusState(false)
        
        // Handle text changes for width animation
        mViewBinding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrEmpty()
                
                // Only trigger animation on the first character typed
                if (hasText && !hasTypedFirstCharacter) {
                    hasTypedFirstCharacter = true
                    animateSearchBarWidth(true) // Make it narrower
                } else if (!hasText && hasTypedFirstCharacter) {
                    hasTypedFirstCharacter = false
                    animateSearchBarWidth(false) // Restore original width
                }
            }
            
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        
        // Setup cancel button click listener
        mViewBinding.cancelButton.setOnClickListener {
            // Clear the search text and exit search mode
            mViewBinding.searchEt.setText(getString(R.string.empty_search_text))
            exitSearchMode()
        }
        
        // Setup search button click listener
        mViewBinding.searchButton.setOnClickListener {
            // Perform search
            performSearch()
        }
    }
    
    /**
     * Update search bar focus state (change edge color)
     */
    private fun updateSearchBarFocusState(hasFocus: Boolean) {
        val searchLayout = mViewBinding.searchLayout
        if (hasFocus) {
            // Remove background and add brand primary border when focused
            searchLayout.shapeDrawableBuilder
                .setSolidColor(android.R.color.transparent)
                .setStrokeColor(ContextCompat.getColor(requireContext(), R.color.brand_primary))
                .setStrokeSize(Utils.dpToPx(2f, resources))
                .intoBackground()
        } else {
            // Maintain theme-aware background and remove edge when not focused
            val backgroundColor = ContextCompat.getColor(requireContext(), R.color.colorSurfaceVariant)
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
            // Hide cancel button and search button first, then restore width
            hideCancelButton()
            hideSearchButton()
            
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
     * Handle back button press when in search mode
     */
    fun onBackPressed(): Boolean {
        if (isSearchMode) {
            exitSearchMode()
            return true // Consumed the back press
        }
        return false // Let the system handle it
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
                    // Perform search with this topic
                    mViewBinding.searchEt.setText(topic.displayName)
                    performSearch()
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
                Glide.with(mContext).load(t.pictureURL).error(R.drawable.ease_default_image)
                    .into(newsIv)
            }
        }
        
        mViewBinding.trendingRecycler.adapter = mTrendingAdapter
        
        // Set up search results RecyclerView
        mViewBinding.homeRecycler.layoutManager = LinearLayoutManager(mContext, RecyclerView.VERTICAL, false)
        
        mAdapter = object :
            CommonAdapter<SearchListEntry.DataDTO.ArticlesDTO>(mContext, R.layout.item_home_recycler, mNewsList) {
            @SuppressLint("SetTextI18n")
            override fun convert(holder: ViewHolder?, t: SearchListEntry.DataDTO.ArticlesDTO, position: Int) {
                val placeTv: TextView = holder!!.getView(R.id.place_tv)
                val tagTv: TextView = holder.getView(R.id.tag_tv)
                val titleTv: TextView = holder.getView(R.id.news_title_tv)
                val newsIv: ImageView = holder.getView(R.id.news_iv)
                val trackView: View = holder.getView(R.id.progress_track)
                val highlightView: View = holder.getView(R.id.progress_highlight)
                val timeTv: TextView = holder.getView(R.id.news_time_tv)
                val countTv: TextView = holder.getView(R.id.news_count_tv)
                countTv.text = getString(R.string.reports_count, t.nSources)
                val transScoreTv: TextView = holder.getView(R.id.trans_score_tv)
                holder.convertView.setOnClickListener {
                    val intent = Intent(mContext, NewsDetailActivity::class.java)
                    intent.putExtra(getString(R.string.id_key),t.articleID)
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
                placeTv.text = t.region
                val sentimentText = getString(CalculateUtil.getSentimentLabelResId(t.metrics.sentiment))
                val sentimentScore = t.metrics.sentiment
                
                // Set sentiment text without "Sentiment:" prefix
                if (sentimentScore > 0.1 || sentimentScore < -0.1) {
                    // Apply colorization for significant positive/negative sentiment
                    val spannableString = SpannableString(sentimentText)
                    val colorResId = resources.getIdentifier(CalculateUtil.getSentimentColorName(sentimentScore), "color", context?.packageName)
                    val sentimentColor = ContextCompat.getColor(mContext, colorResId)
                    spannableString.setSpan(ForegroundColorSpan(sentimentColor), 0, sentimentText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    transScoreTv.text = spannableString
                } else {
                    // No colorization for neutral sentiment
                    transScoreTv.text = sentimentText
                }
                tagTv.text = t.sector
                titleTv.text = t.title
                Glide.with(mContext).load(t.pictureURL).error(R.drawable.ease_default_image)
                    .into(newsIv)

                try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val parse = dateFormat.parse(t.date)
                    timeTv.text = Utils.getMultilingualSpaceTime(mContext!!, parse!!.time)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                // progress highlight from zero
                trackView.post(Runnable {
                    val totalWidth = trackView.width
                    val half = totalWidth / 2
                    val score = CalculateUtil.round(t.metrics.sentiment, 2)
                    val distance = (kotlin.math.abs(score) * half).toInt()
                    val lp = highlightView.layoutParams as ConstraintLayout.LayoutParams
                    if (distance <= 0) {
                        highlightView.visibility = View.INVISIBLE
                        lp.width = 1
                        lp.marginStart = half
                    } else {
                        highlightView.visibility = View.VISIBLE
                        lp.width = distance
                        lp.marginStart = if (score > 0) half else (half - distance)
                    }
                    highlightView.layoutParams = lp
                    highlightView.setBackgroundResource(
                        if (score > 0) R.drawable.bg_progress_positive else R.drawable.bg_progress_negative
                    )
                })
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
        mViewBinding.contentSmartRefresh.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
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

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                // Discover content doesn't support load more, just finish
                refreshLayout.finishLoadMore()
            }
        })
    }
    
    private fun enterSearchMode() {
        isSearchMode = true
        mViewBinding.contentSmartRefresh.visibility = View.GONE
        mViewBinding.smartRefresh.visibility = View.VISIBLE
    }
    
    private fun exitSearchMode() {
        isSearchMode = false
        mViewBinding.contentSmartRefresh.visibility = View.VISIBLE
        mViewBinding.smartRefresh.visibility = View.GONE
        mViewBinding.searchEt.clearFocus()
        mViewBinding.searchEt.setText(getString(R.string.empty_search_text))
        
        // Reset search bar state
        hasTypedFirstCharacter = false
        updateSearchBarFocusState(false)
        hideCancelButton()
        hideSearchButton()
        hideSearchResultsReminder()
        animateSearchBarWidth(false)
    }
    
    private fun performSearch() {
        val query = mViewBinding.searchEt.text.toString().trim()
        if (query.isEmpty()) {
            searchModel.querySearchList()
            hideSearchResultsReminder()
        } else {
            searchModel.queryListByTitle(query)
            showSearchResultsReminder(query, 0) // Start with 0 results, will be updated when actual results come in
        }
        enterSearchMode()
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