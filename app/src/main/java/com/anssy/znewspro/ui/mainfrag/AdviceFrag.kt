package com.anssy.znewspro.ui.mainfrag

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.view.Gravity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anssy.znewspro.utils.Constants
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseFragment
import com.anssy.znewspro.databinding.FragAdviceBinding
import com.anssy.znewspro.entry.SearchListEntry
import com.anssy.znewspro.entry.TopicListEntry
import com.anssy.znewspro.model.PersonRecommendModel
import com.anssy.znewspro.model.TopicModel
import com.anssy.znewspro.utils.SharedPreferenceUtils
import com.anssy.znewspro.ui.MainActivity
import com.anssy.znewspro.ui.newsdetail.NewsDetailActivity
import com.anssy.znewspro.selfview.popup.SortPopupWindow
import com.anssy.znewspro.ui.topicmodify.TopicSelectionActivity
import com.anssy.znewspro.utils.CalculateUtil
import com.anssy.znewspro.utils.ToastUtils
import com.anssy.znewspro.utils.Utils
import com.bumptech.glide.Glide
import com.scwang.smartrefresh.layout.api.RefreshLayout
import com.scwang.smartrefresh.layout.listener.OnRefreshLoadMoreListener
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * @Description 推荐
 * @Author yulu
 * @CreateTime 2025年06月30日 09:27:40
 */

class AdviceFrag : BaseFragment() {
    private lateinit var mViewBinding: FragAdviceBinding
    private var pageNo = 1
    private val pageSize = 10

    // Modern activity result launcher
    private val topicSelectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == TopicSelectionActivity.RESULT_TOPICS_UPDATED) {
            // Update active tags display
            updateActiveTags()
            // Refresh recommendations when topics are updated
            isRefresh = true
            pageNo = 1
            personRecommendModel.queryRecommendList(pageNo, pageSize)
        }
    }

    companion object {
        fun getInstance(): AdviceFrag {
            return AdviceFrag()
        }
    }

    private val personRecommendModel: PersonRecommendModel by viewModels()
    private val topicModel: TopicModel by viewModels()
    private var mProgressWidth = 0
    private lateinit var mAdapter: CommonAdapter<SearchListEntry.DataDTO.ArticlesDTO>
    private var mNewsList = ArrayList<SearchListEntry.DataDTO.ArticlesDTO>()
    private var currentSort: SortPopupWindow.SortOption = SortPopupWindow.SortOption.LATEST
    private var allTopics = ArrayList<TopicListEntry.TopicDTO>()
    private var userSelectedTopics = ArrayList<TopicListEntry.TopicDTO>()
    
    // Animation timing
    private var refreshStartTime: Long = 0
    private val minimumRefreshDuration = 800L // 800ms minimum duration
    private var isButtonRefresh = false // Flag to prevent double API calls
    
    // Debounce timing to prevent rapid successive refreshes
    private var lastRefreshTime: Long = 0
    private val minimumTimeBetweenRefreshes = 1500L // 1.5 seconds minimum between refreshes
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mViewBinding = FragAdviceBinding.inflate(inflater)
        return mViewBinding.root
    }

    private var isRefresh = true

    override fun initData() {
        mProgressWidth = resources.displayMetrics.widthPixels - Utils.dpToPx(36f, resources)
        mViewBinding.searchEt.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                Log.e(getString(R.string.xxx_log_tag), getString(R.string.search_log))

            }
            false
        }

        mViewBinding.homeRecycler.layoutManager = LinearLayoutManager(mContext, RecyclerView.VERTICAL, false)
        mAdapter = object : CommonAdapter<SearchListEntry.DataDTO.ArticlesDTO>(mContext, R.layout.item_recommended_news, mNewsList) {
            override fun convert(holder: ViewHolder, t: SearchListEntry.DataDTO.ArticlesDTO, position: Int) {
                val placeTv: TextView = holder.getView(R.id.place_tv)
                val tagTv: TextView = holder.getView(R.id.tag_tv)
                val titleTv: TextView = holder.getView(R.id.news_title_tv)
                val newsIv: ImageView = holder.getView(R.id.news_iv)
                val trackView: View = holder.getView(R.id.progress_track)
                val highlightView: View = holder.getView(R.id.progress_highlight)
                val timeTv: TextView = holder.getView(R.id.news_time_tv)
                val countTv: TextView = holder.getView(R.id.news_count_tv)
                val transScoreTv: TextView = holder.getView(R.id.trans_score_tv)
                val aiIcon: ImageView = holder.getView(R.id.ai_icon)
                val recommendedText: TextView = holder.getView(R.id.recommended_text)
                
                // Set place and tag
                placeTv.text = t.region ?: ""
                tagTv.text = t.sector ?: ""
                
                // Set AI icon and recommended text (these are always visible in recommended page)
                aiIcon.visibility = View.VISIBLE
                recommendedText.visibility = View.VISIBLE
                
                // Set title
                titleTv.text = t.title
                
                // Set count
                countTv.text = getString(R.string.reports_count, t.nSources)
                
                // Set sentiment score
                val subjectivity = t.metrics?.subjectivity ?: 0.0
                val sentimentText = getString(CalculateUtil.getSentimentLabelResId(subjectivity))
                
                if (subjectivity > 0.1 || subjectivity < -0.1) {
                    // Apply colorization for significant positive/negative sentiment
                    val spannableString = SpannableString(sentimentText)
                    val colorResId = resources.getIdentifier(CalculateUtil.getSentimentColorName(subjectivity), "color", context?.packageName)
                    val sentimentColor = ContextCompat.getColor(mContext, colorResId)
                    spannableString.setSpan(ForegroundColorSpan(sentimentColor), 0, sentimentText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    transScoreTv.text = spannableString
                } else {
                    // No colorization for neutral sentiment
                    transScoreTv.text = sentimentText
                }
                
                // Load image with Glide
                Glide.with(mContext!!)
                    .load(t.pictureURL)
                    .placeholder(R.drawable.ease_default_image)
                    .error(R.drawable.ease_default_image)
                    .into(newsIv)

                // Set up progress bar for subjectivity score
                trackView.post {
                    val totalWidth = trackView.width
                    val half = totalWidth / 2
                    val score = subjectivity
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
                }

                // Set time
                try {
                    val dateFormat = SimpleDateFormat(getString(R.string.date_format_pattern), Locale.getDefault())
                    val parse = dateFormat.parse(t.date)
                    timeTv.text = Utils.getMultilingualSpaceTime(mContext!!, parse!!.time)
                } catch (e: Exception) {
                    e.printStackTrace()
                    timeTv.text = t.date
                }

                holder.itemView.setOnClickListener {
                    val intent = Intent(mContext, NewsDetailActivity::class.java)
                    intent.putExtra("id", t.articleID)
                    startActivity(intent)
                }
                
                // Add long press listener for sharing with shrink animation
                holder.itemView.setOnLongClickListener {
                    // Animate shrink effect
                    holder.itemView.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(150)
                        .withEndAction {
                            // Restore original size after animation
                            holder.itemView.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(300)
                                .start()
                        }
                        .start()
                    
                    shareArticle(t)
                    true
                }
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
        
        // Setup personal subpage switch (Your Feed | Recap)
        setupTabs()
        
        // Initialize sort indicator and active tags
        updateSortIndicator()
        updateActiveTags()
        
        initModel()
        personRecommendModel.queryRecommendList(pageNo, pageSize)
        mViewBinding.smartRefresh.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                // Only proceed if this is a pull-to-refresh, not a button-triggered refresh
                if (!isButtonRefresh) {
                    // Check debounce timing for pull-to-refresh as well
                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastRefresh = currentTime - lastRefreshTime
                    if (timeSinceLastRefresh < minimumTimeBetweenRefreshes) {
                        Log.d(getString(R.string.advice_frag_log_tag), getString(R.string.pull_refresh_blocked, timeSinceLastRefresh, minimumTimeBetweenRefreshes))
                        refreshLayout.finishRefresh()
                        return
                    }
                    
                    // Update last refresh time and proceed
                    lastRefreshTime = currentTime
                    refreshStartTime = System.currentTimeMillis()
                    isRefresh = true
                    pageNo = 1
                    personRecommendModel.queryRecommendList(pageNo, pageSize)
                }
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                isRefresh = false
                pageNo++
                personRecommendModel.queryRecommendList(pageNo, pageSize)
            }

        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initModel() {
        // Load user's selected topics first
        topicModel.queryMyTopics()
        topicModel.myTopicsEntry.observe(viewLifecycleOwner) { response ->
            if (response != null && response.code == Constants.SUCCESS_CODE) {
                userSelectedTopics.clear()
                response.data?.topics?.let { topics ->
                    userSelectedTopics.addAll(topics)
                }
                updateActiveTags()
            }
        }
        
        // Load all available topics for reference
        topicModel.queryAllTopics()
        topicModel.topicListEntry.observe(viewLifecycleOwner) { response ->
            if (response != null && response.code == Constants.SUCCESS_CODE) {
                allTopics.clear()
                response.data?.topics?.let { topics ->
                    allTopics.addAll(topics)
                }
            }
        }
        
        // Observe topic edit responses
        topicModel.commonResponseEntry.observe(viewLifecycleOwner) { response ->
            if (response != null && response.code == Constants.SUCCESS_CODE) {
                // Topic was successfully removed, update the display
                updateActiveTags()
            }
        }
        
        personRecommendModel.recommendListEntry.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.code == Constants.SUCCESS_CODE) {
                    if (isRefresh) {
                        mNewsList.clear()
                        finishRefreshWithMinimumDuration(true)
                    } else {
                        mViewBinding.smartRefresh.finishLoadMore(true)
                    }
                    mNewsList.addAll(it.data.articles)
                    applyCurrentSort()
                    mAdapter.notifyDataSetChanged()
                } else {
                    if (isRefresh) {
                        finishRefreshWithMinimumDuration(false)
                    } else {
                        mViewBinding.smartRefresh.finishLoadMore(false)
                    }
                    if (it.code == 1000) {
                        ToastUtils.showShortToast(mContext!!, getString(R.string.server_error_message))
                    } else {
                        ToastUtils.showShortToast(mContext!!, it.msg)
                    }
                }
            }
        }
    }

    private fun showSortPopup() {
        val popup = SortPopupWindow(requireContext(), object : SortPopupWindow.Callback {
            override fun onSortSelected(option: SortPopupWindow.SortOption) {
                currentSort = option
                updateSortIndicator()
                applyCurrentSort()
                mAdapter.notifyDataSetChanged()
            }
        })
        popup.setCurrentSort(currentSort)
        popup.showPopupWindow(mViewBinding.sortChip)
    }

    private fun showTopicSelection() {
        val intent = Intent(mContext, TopicSelectionActivity::class.java)
        // Pass currently selected topics from API
        val selectedTopicTags = userSelectedTopics.map { it.tag }
        intent.putStringArrayListExtra(TopicSelectionActivity.EXTRA_SELECTED_TOPICS, ArrayList(selectedTopicTags))
        topicSelectionLauncher.launch(intent)
    }

    private fun setupTabs() {
        val tabLayout = mViewBinding.personalTabLayout
        val tabTitles = listOf(getString(R.string.your_feed_title), getString(R.string.recap_title))
        
        // Create custom views for each tab (matching HomeFrag approach)
        repeat(tabTitles.size) { position ->
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
            
            tabLayout.addTab(tabLayout.newTab().setCustomView(container))
        }

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
                
                if (tab.position == 0) {
                    // Your Feed visible
                    mViewBinding.sortIndicatorContainer.visibility = View.VISIBLE
                    mViewBinding.activeTagsContainer.visibility = View.VISIBLE
                    mViewBinding.smartRefresh.visibility = View.VISIBLE
                    mViewBinding.recapContainer.visibility = View.GONE
                } else {
                    // Recap visible
                    ensureRecapContainer()
                    mViewBinding.sortIndicatorContainer.visibility = View.GONE
                    mViewBinding.activeTagsContainer.visibility = View.GONE
                    mViewBinding.smartRefresh.visibility = View.GONE
                    mViewBinding.recapContainer.visibility = View.VISIBLE
                }
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

    private fun ensureRecapContainer() {
        // Always replace existing content to avoid stale/old layout remaining
        mViewBinding.recapContainer.removeAllViews()
        
        // Create and add the new unified RecapView
        val recapView = com.anssy.znewspro.selfview.RecapView(requireContext())
        mViewBinding.recapContainer.addView(recapView)
    }

    private fun updateSortIndicator() {
        val sortValue = when (currentSort) {
            SortPopupWindow.SortOption.LATEST -> getString(R.string.latest_sort_option)
            SortPopupWindow.SortOption.POPULAR -> getString(R.string.popular_sort_option)
            SortPopupWindow.SortOption.RELEVANT -> "Relevant"
        }
        mViewBinding.sortValueTv.text = sortValue
        
        // Update the icon based on current sort option
        val sortIcon = when (currentSort) {
            SortPopupWindow.SortOption.LATEST -> R.drawable.ic_clock_24
            SortPopupWindow.SortOption.POPULAR -> R.drawable.ic_trending_24
            SortPopupWindow.SortOption.RELEVANT -> R.drawable.ic_star_24
        }
        mViewBinding.sortInfoIcon.setImageResource(sortIcon)
        // Open sort menu when tapping the sort chip
        mViewBinding.sortChip.setOnClickListener { showSortPopup() }
    }

    private fun updateActiveTags() {
        // Clear existing tag chips
        mViewBinding.activeTagsChipsContainer.removeAllViews()
        
        // Get active tags from API
        val activeTags = getActiveTags()
        
        if (activeTags.isEmpty()) {
            // Show "None" message when no active tags
            val noneText = TextView(mContext)
            noneText.text = "None"
            noneText.setTextColor(mContext!!.getColor(R.color.colorTextDeep))
            noneText.textSize = 16f
            noneText.typeface = mContext!!.resources.getFont(R.font.inter_regular)
            noneText.setTypeface(noneText.typeface, android.graphics.Typeface.BOLD)
            
            mViewBinding.activeTagsChipsContainer.addView(noneText)
        } else {
            // Add tag chips for each active tag
            activeTags.forEach { tag ->
                val chipView = LayoutInflater.from(mContext).inflate(
                    R.layout.item_tag_chip,
                    mViewBinding.activeTagsChipsContainer,
                    false
                )
                val tagIcon = chipView.findViewById<ImageView>(R.id.tag_icon)
                val tagText = chipView.findViewById<TextView>(R.id.tag_text)
                
                // Set tag text
                tagText.text = tag.displayName
                
                // Set tag icon based on tag type
                tagIcon.setImageResource(getTagIcon(tag.tag))
                
                // Add click listener to remove tag
                chipView.setOnClickListener {
                    removeActiveTag(tag.tag)
                }
                
                mViewBinding.activeTagsChipsContainer.addView(chipView)
            }
            // Add small tag with chevron to open topic selection
            val manageView = LayoutInflater.from(mContext).inflate(
                R.layout.item_tag_chip,
                mViewBinding.activeTagsChipsContainer,
                false
            )
            val manageIcon = manageView.findViewById<ImageView>(R.id.tag_icon)
            val manageText = manageView.findViewById<TextView>(R.id.tag_text)
            manageIcon.setImageResource(R.drawable.ic_chevron_right_24)
            // Hide text so the chip shows only the chevron icon
            manageText.visibility = View.GONE
            // Remove extra space to the right of the chevron
            (manageIcon.layoutParams as? android.widget.LinearLayout.LayoutParams)?.let { lp ->
                lp.marginEnd = 0
                manageIcon.layoutParams = lp
            }
            // Tighten chip horizontal padding for icon-only chip
            val padV = com.anssy.znewspro.utils.Utils.dpToPx(4f, resources)
            val padH = com.anssy.znewspro.utils.Utils.dpToPx(8f, resources)
            manageView.setPadding(padH, padV, padH, padV)
            manageView.setOnClickListener { showTopicSelection() }
            mViewBinding.activeTagsChipsContainer.addView(manageView)
        }
    }

    private fun getActiveTags(): List<TopicListEntry.TopicDTO> {
        // Return user's selected topics from API
        return userSelectedTopics
    }


    private fun getTagIcon(tag: String): Int {
        return when (tag.lowercase()) {
            "conflict" -> R.drawable.ic_security_24
            "culture" -> R.drawable.ic_palette_24
            "diplomacy" -> R.drawable.ic_public_24
            "economics" -> R.drawable.ic_trending_up_24
            "entertainment" -> R.drawable.ic_live_tv_24
            "politics" -> R.drawable.ic_account_balance_24
            "science" -> R.drawable.ic_science_24
            "sports" -> R.drawable.ic_sports_soccer_24
            "technology" -> R.drawable.ic_memory_24
            else -> R.drawable.ic_security_24 // Default icon
        }
    }

    private fun removeActiveTag(tag: String) {
        // Remove tag using API call
        topicModel.editTopic(Constants.TYPE_TOPIC_DELETE, tag)
        
        // Refresh recommendations
        isRefresh = true
        pageNo = 1
        personRecommendModel.queryRecommendList(pageNo, pageSize)
    }


    private fun applyCurrentSort() {
        when (currentSort) {
            SortPopupWindow.SortOption.LATEST -> mNewsList.sortByDescending { it.date }
            SortPopupWindow.SortOption.POPULAR -> mNewsList.sortByDescending { it.nSources }
            SortPopupWindow.SortOption.RELEVANT -> {
                mNewsList.sortByDescending { article ->
                    val sentiment = article.metrics?.sentiment ?: 0.0
                    val subjectivity = article.metrics?.subjectivity ?: 0.0
                    kotlin.math.abs(sentiment) + kotlin.math.abs(subjectivity)
                }
            }
        }
    }

    /**
     * Refresh the fragment data
     */
    fun refreshData() {
        Log.d(getString(R.string.advice_frag_log_tag), getString(R.string.refreshing_advice_fragment))
        
        // Check if fragment is properly attached before accessing ViewModels
        if (!isAdded || isDetached || activity == null) {
            Log.w(getString(R.string.advice_frag_log_tag), getString(R.string.fragment_not_attached))
            return
        }
        
        // Check debounce timing to prevent rapid successive refreshes
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRefresh = currentTime - lastRefreshTime
        if (timeSinceLastRefresh < minimumTimeBetweenRefreshes) {
            Log.d(getString(R.string.advice_frag_log_tag), getString(R.string.refresh_blocked, timeSinceLastRefresh, minimumTimeBetweenRefreshes))
            return
        }
        
        // Update last refresh time and proceed
        lastRefreshTime = currentTime
        
        // Start the refresh animation and set button refresh flag
        refreshStartTime = System.currentTimeMillis()
        isButtonRefresh = true
        mViewBinding.smartRefresh.autoRefresh()
        
        // Scroll to top before refreshing (instant for better performance)
        mViewBinding.homeRecycler.post {
            mViewBinding.homeRecycler.scrollToPosition(0)
        }
        
        isRefresh = true
        pageNo = 1
        personRecommendModel.queryRecommendList(pageNo, pageSize)
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
                mViewBinding.smartRefresh.finishRefresh(success)
                // Reset button refresh flag when animation completes
                isButtonRefresh = false
            }, remainingTime)
        } else {
            // Already exceeded minimum duration, finish immediately
            mViewBinding.smartRefresh.finishRefresh(success)
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