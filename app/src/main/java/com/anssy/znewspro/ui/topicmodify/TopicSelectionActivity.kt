package com.anssy.znewspro.ui.topicmodify

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseActivity
import com.anssy.znewspro.databinding.ActivityTopicSelectionBinding
import com.anssy.znewspro.entry.TopicListEntry
import com.anssy.znewspro.model.TopicModel
import com.anssy.znewspro.utils.Constants
import com.anssy.znewspro.utils.ToastUtils
import com.anssy.znewspro.utils.SystemDialogUtils
import com.anssy.znewspro.utils.ThemeManager
import androidx.core.content.ContextCompat
import com.scwang.smartrefresh.layout.api.RefreshLayout
import com.scwang.smartrefresh.layout.listener.OnRefreshListener
import dagger.hilt.android.AndroidEntryPoint

/**
 * Topic Selection Activity - matches iOS design
 * Allows users to select/deselect topics for personalized recommendations
 * Includes search, tabbed sections (All, Sectors, Regions, Trending, Custom), and Clear All
 * Section headers appear above and outside their respective cards
 */
@AndroidEntryPoint
class TopicSelectionActivity : BaseActivity() {
    private val topicModel: TopicModel by viewModels()
    private lateinit var mViewBinding: ActivityTopicSelectionBinding
    
    // Topic lists - fetched from separate backend endpoints
    private var sectorsList = ArrayList<TopicListEntry.TopicDTO>()
    private var regionsList = ArrayList<TopicListEntry.TopicDTO>()
    private var trendingListAll = ArrayList<TopicListEntry.TopicDTO>() // All trending topics for "All" tab
    private var trendingListLimited = ArrayList<TopicListEntry.TopicDTO>() // 3-6 trending topics for "Trending" tab
    private var displayList = ArrayList<DisplayItem>()
    
    private var selectedTopics = ArrayList<String>() // Keep as tags for API calls
    private var initialSelectedTopics = ArrayList<String>() // Track initial state
    private lateinit var mAdapter: SectionAdapter
    private var isRefreshing = false
    
    // Current tab selection
    private var currentTab = Tab.ALL
    
    // Search query
    private var searchQuery = ""
    
    // Cache string resources to avoid repeated lookups
    private lateinit var sectionSectorsString: String
    private lateinit var sectionRegionsString: String
    private lateinit var sectionTrendingString: String
    
    enum class Tab {
        ALL, SECTORS, REGIONS, TRENDING, CUSTOM
    }
    
    // Display item can be a section (with header and topics) or just for layout
    sealed class DisplayItem {
        data class Section(
            val title: String,
            val topics: List<TopicListEntry.TopicDTO>
        ) : DisplayItem()
    }

    companion object {
        const val EXTRA_SELECTED_TOPICS = "selected_topics"
        const val RESULT_TOPICS_UPDATED = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding = ActivityTopicSelectionBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)
        applyStatusBarStyle()
        
        // Cache string resources
        sectionSectorsString = getString(R.string.section_sectors)
        sectionRegionsString = getString(R.string.section_regions)
        sectionTrendingString = getString(R.string.section_trending)
        
        // Get currently selected topics from intent
        selectedTopics = intent.getStringArrayListExtra(EXTRA_SELECTED_TOPICS) ?: ArrayList()
        
        setupBackPressedHandler()
        initView()
        initModel()
    }

    private fun initView() {
        // Set up close button
        mViewBinding.closeButton.setOnClickListener { 
            setResult(RESULT_TOPICS_UPDATED)
            finish()
        }
        
        // Set up clear all button
        mViewBinding.clearAllButton.setOnClickListener {
            showClearAllConfirmation()
        }
        
        // Set up search bar
        setupSearchBar()
        
        // Set up tab toggle
        setupTabToggle()
        
        // Set up sliding indicator
        setupSlidingIndicator()

        // Set up RecyclerView with section adapter
        mViewBinding.topicsRecycler.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        mAdapter = SectionAdapter()
        mViewBinding.topicsRecycler.adapter = mAdapter
        
        // Make gradient overlays non-clickable so touches pass through
        mViewBinding.topGradientFade.isClickable = false
        mViewBinding.topGradientFade.isFocusable = false
        mViewBinding.bottomGradientFade.isClickable = false
        mViewBinding.bottomGradientFade.isFocusable = false
        
        // Set up pull-to-refresh
        mViewBinding.smartRefresh.setEnableLoadMore(false)
        mViewBinding.smartRefresh.setOnRefreshListener(object : OnRefreshListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                isRefreshing = true
                loadAllData()
            }
        })
    }
    
    private fun setupSearchBar() {
        mViewBinding.searchEt.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s?.toString()?.trim() ?: ""
                mViewBinding.clearSearchButton.isVisible = searchQuery.isNotEmpty()
                updateDisplayList()
            }
        })
        
        mViewBinding.searchEt.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                updateDisplayList()
                true
            } else {
                false
            }
        }
        
        mViewBinding.clearSearchButton.setOnClickListener {
            mViewBinding.searchEt.setText("")
            searchQuery = ""
            updateDisplayList()
        }
    }
    
    private fun setupTabToggle() {
        mViewBinding.tabToggleGroup.setOnCheckedChangeListener { _, checkedId ->
            currentTab = when (checkedId) {
                R.id.tab_all -> Tab.ALL
                R.id.tab_sectors -> Tab.SECTORS
                R.id.tab_regions -> Tab.REGIONS
                R.id.tab_trending -> Tab.TRENDING
                R.id.tab_custom -> Tab.CUSTOM
                else -> Tab.ALL
            }
            
            // Update indicator position with animation
            updateIndicatorPosition(animate = true)
            
            // Show/hide coming soon overlay for custom tab
            if (currentTab == Tab.CUSTOM) {
                mViewBinding.smartRefresh.visibility = View.GONE
                mViewBinding.comingSoonOverlay.visibility = View.VISIBLE
            } else {
                mViewBinding.smartRefresh.visibility = View.VISIBLE
                mViewBinding.comingSoonOverlay.visibility = View.GONE
                updateDisplayList()
            }
        }
    }
    
    /**
     * Setup the sliding indicator for topic toggle
     */
    private fun setupSlidingIndicator() {
        val indicator = mViewBinding.topicToggleIndicator ?: return
        val isDarkMode = ThemeManager.isDarkModeActive(this)
        val selectedDrawable = if (isDarkMode) {
            ContextCompat.getDrawable(this, R.drawable.topic_toggle_selected_dark)
        } else {
            ContextCompat.getDrawable(this, R.drawable.topic_toggle_selected_light)
        }
        
        indicator.background = selectedDrawable
        
        // Wait for layout to be measured, then position indicator
        mViewBinding.tabToggleGroup.post {
            updateIndicatorPosition(animate = false)
        }
    }
    
    /**
     * Update the position of the sliding indicator to match the selected tab
     */
    private fun updateIndicatorPosition(animate: Boolean = true) {
        val indicator = mViewBinding.topicToggleIndicator ?: return
        val radioGroup = mViewBinding.tabToggleGroup
        
        // Find the checked RadioButton
        val checkedId = radioGroup.checkedRadioButtonId
        val checkedButton = radioGroup.findViewById<android.widget.RadioButton>(checkedId) ?: return
        
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

    private fun initModel() {
        // Load all data
        loadAllData()
        
        // Observe user's selected topics
        topicModel.myTopicsEntry.observe(this) { response ->
            if (response != null) {
                if (response.code == Constants.SUCCESS_CODE) {
                    selectedTopics.clear()
                    initialSelectedTopics.clear()
                    response.data?.topics?.let { topics ->
                        val tags = topics.map { it.tag }
                        selectedTopics.addAll(tags)
                        initialSelectedTopics.addAll(tags)
                    }
                    updateClearAllButtonVisibility()
                    updateDisplayList()
                }
            }
        }
        
        // Observe sectors response - uses "sectors" field from /profile/sectors
        // Display exactly as received from backend - no sorting or filtering
        topicModel.sectorsEntry.observe(this) { response ->
            if (response != null && response.code == Constants.SUCCESS_CODE) {
                sectorsList.clear()
                response.data?.sectors?.let { sectors ->
                    // Add ALL sectors in exact order from backend - no modifications
                    sectorsList.addAll(sectors)
                }
                updateDisplayList()
                finishRefresh(true)
            } else {
                finishRefresh(false)
            }
        }
        
        // Observe regions response - uses /profile/listtopics?type=regions
        // This is separate from /profile/publisher-region - specifically for topic selection menu
        // Display exactly as received from backend - no sorting or filtering
        topicModel.regionsEntry.observe(this) { response ->
            if (response != null && response.code == Constants.SUCCESS_CODE) {
                regionsList.clear()
                // Use only "regions" field - no fallback
                response.data?.regions?.let { regionList ->
                    // Add ALL regions in exact order from backend - no modifications
                    regionsList.addAll(regionList)
                }
                updateDisplayList()
            }
        }
        
        // Observe trending topics response for "All" tab - uses /profile/listtopics (all topics, no limits)
        // This is separate from /feed/trending-topics which is used only for "Trending" tab (3-6 topics)
        // Display exactly as received from backend - no sorting or filtering
        topicModel.trendingTopicsEntry.observe(this) { response ->
            if (response != null && response.code == Constants.SUCCESS_CODE) {
                trendingListAll.clear()
                // Use only topics field - fetches ALL topics without limits (like regions endpoint)
                response.data?.topics?.let { topics ->
                    // Add ALL topics for "All" tab - no modifications, no limits
                    trendingListAll.addAll(topics)
                }
                updateDisplayList()
            }
        }
        
        // Observe limited trending topics response - uses /feed/trending-topics (returns 3-6)
        // Display exactly as received from backend - no sorting or filtering
        topicModel.trendingTopicsLimitedEntry.observe(this) { response ->
            if (response != null && response.code == Constants.SUCCESS_CODE) {
                trendingListLimited.clear()
                response.data?.topics?.let { topics ->
                    // Add 3-6 trending topics for "Trending" tab - no modifications
                    trendingListLimited.addAll(topics)
                }
                updateDisplayList()
            }
        }
        
        // Observe topic edit responses
        topicModel.commonResponseEntry.observe(this) { response ->
            if (response != null) {
                val topicTag = response.msg
                if (topicTag != null) {
                    if (response.code == Constants.SUCCESS_CODE) {
                        // Update initial state to match current state on success
                        if (selectedTopics.contains(topicTag)) {
                            if (!initialSelectedTopics.contains(topicTag)) {
                                initialSelectedTopics.add(topicTag)
                            }
                        } else {
                            initialSelectedTopics.remove(topicTag)
                        }
                        updateClearAllButtonVisibility()
                    } else {
                        // Revert the change if server update failed
                        val wasSelected = initialSelectedTopics.contains(topicTag)
                        if (wasSelected && !selectedTopics.contains(topicTag)) {
                            selectedTopics.add(topicTag)
                            updateDisplayList()
                        } else if (!wasSelected && selectedTopics.contains(topicTag)) {
                            selectedTopics.remove(topicTag)
                            updateDisplayList()
                        }
                        
                        // Always show standard server error message instead of topic tag
                        ToastUtils.showShortToast(this, getString(R.string.server_error_message))
                    }
                }
            }
        }
    }
    
    private fun loadAllData() {
        topicModel.queryMyTopics()
        topicModel.querySectors()  // Fetches from /profile/sectors (returns "sectors" field)
        topicModel.queryRegions()  // Fetches from /profile/listtopics?type=regions (returns "regions" field with all regions, no limits)
        topicModel.getTrendingTopics()  // Fetches from /profile/listtopics (returns ALL topics for "All" tab, no limits - separate from Trending tab)
        topicModel.getTrendingTopicsLimited()  // Fetches from /feed/trending-topics (returns 3-6 trending topics for "Trending" tab only)
    }
    
    private fun finishRefresh(success: Boolean) {
        if (isRefreshing) {
            mViewBinding.smartRefresh.finishRefresh(success)
            isRefreshing = false
        }
    }
    
    private fun updateDisplayList() {
        val newDisplayList = ArrayList<DisplayItem>()
        
        // Filter only by user search query - no other filtering or sorting
        // All topics displayed in exact order from backend
        val filteredSectors = filterTopics(sectorsList)
        val filteredRegions = filterTopics(regionsList)
        
        when (currentTab) {
            Tab.ALL -> {
                // Show all sections - use ALL trending topics for "All" tab
                if (filteredSectors.isNotEmpty()) {
                    newDisplayList.add(DisplayItem.Section(sectionSectorsString, filteredSectors))
                }
                if (filteredRegions.isNotEmpty()) {
                    newDisplayList.add(DisplayItem.Section(sectionRegionsString, filteredRegions))
                }
                val filteredTrendingAll = filterTopics(trendingListAll)
                if (filteredTrendingAll.isNotEmpty()) {
                    newDisplayList.add(DisplayItem.Section(sectionTrendingString, filteredTrendingAll))
                }
            }
            Tab.SECTORS -> {
                if (filteredSectors.isNotEmpty()) {
                    newDisplayList.add(DisplayItem.Section(sectionSectorsString, filteredSectors))
                }
            }
            Tab.REGIONS -> {
                if (filteredRegions.isNotEmpty()) {
                    newDisplayList.add(DisplayItem.Section(sectionRegionsString, filteredRegions))
                }
            }
            Tab.TRENDING -> {
                // Use limited trending topics (3-6) for "Trending" tab
                val filteredTrendingLimited = filterTopics(trendingListLimited)
                if (filteredTrendingLimited.isNotEmpty()) {
                    newDisplayList.add(DisplayItem.Section(sectionTrendingString, filteredTrendingLimited))
                }
            }
            Tab.CUSTOM -> {
                // Handled by coming soon overlay
            }
        }
        
        displayList.clear()
        displayList.addAll(newDisplayList)
        mAdapter.notifyDataSetChanged()
    }
    
    /**
     * Filter topics only by user search query.
     * No sorting or other filtering - displays backend data as-is.
     * When search is empty, returns all topics in original backend order.
     */
    private fun filterTopics(topics: List<TopicListEntry.TopicDTO>): List<TopicListEntry.TopicDTO> {
        // If no search query, return all topics in exact backend order
        if (searchQuery.isEmpty()) return topics
        // Only filter by search query - maintain backend order
        return topics.filter { topic ->
            topic.displayName.contains(searchQuery, ignoreCase = true) ||
            topic.tag.contains(searchQuery, ignoreCase = true)
        }
    }
    
    private fun updateClearAllButtonVisibility() {
        mViewBinding.clearAllButton.isVisible = selectedTopics.isNotEmpty()
    }
    
    private fun showClearAllConfirmation() {
        SystemDialogUtils.showAlertDialog(
            this,
            getString(R.string.clear_all_topics_confirmation_title),
            getString(R.string.clear_all_topics_confirmation_message),
            getString(R.string.clear_all_topics_confirmation_confirm),
            getString(R.string.dialog_button_cancel),
            isDestructive = true,
            onPositiveClick = {
                clearAllTopics()
            }
        )
    }
    
    private fun clearAllTopics() {
        if (selectedTopics.isEmpty()) return
        
        // Create a copy of selected topics to delete
        val topicsToDelete = selectedTopics.toList()
        
        // Clear UI immediately
        selectedTopics.clear()
        updateDisplayList()
        updateClearAllButtonVisibility()
        
        // Delete all topics from backend
        topicsToDelete.forEach { topicTag ->
            topicModel.editTopic(Constants.TYPE_TOPIC_DELETE, topicTag)
        }
        
        ToastUtils.showShortToast(this, getString(R.string.cleared_all_topics))
    }

    private fun clearSectionTopics(topics: List<TopicListEntry.TopicDTO>) {
        // Get topics in this section that are currently selected
        val topicsToClear = topics.map { it.tag }.filter { selectedTopics.contains(it) }
        
        if (topicsToClear.isEmpty()) return
        
        // Clear UI immediately
        topicsToClear.forEach { topicTag ->
            selectedTopics.remove(topicTag)
        }
        updateDisplayList()
        updateClearAllButtonVisibility()
        
        // Delete topics from backend
        topicsToClear.forEach { topicTag ->
            topicModel.editTopic(Constants.TYPE_TOPIC_DELETE, topicTag)
        }
    }
    
    private fun toggleTopic(topic: TopicListEntry.TopicDTO) {
        val action: String
        if (selectedTopics.contains(topic.tag)) {
            selectedTopics.remove(topic.tag)
            action = Constants.TYPE_TOPIC_DELETE
        } else {
            selectedTopics.add(topic.tag)
            action = Constants.TYPE_TOPIC_ADD
        }
        
        // Send change immediately to backend
        topicModel.editTopic(action, topic.tag)
        
        // Update UI immediately
        updateDisplayList()
        updateClearAllButtonVisibility()
    }

    private fun setupBackPressedHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                setResult(RESULT_TOPICS_UPDATED)
                finish()
            }
        })
    }

    private fun getTopicIcon(topicTag: String): Int {
        return when (topicTag.lowercase()) {
            "conflict" -> R.drawable.ic_security_24
            "culture" -> R.drawable.ic_palette_24
            "diplomacy" -> R.drawable.ic_public_24
            "economics" -> R.drawable.ic_trending_up_24
            "entertainment" -> R.drawable.ic_live_tv_24
            "politics" -> R.drawable.ic_account_balance_24
            "science" -> R.drawable.ic_science_24
            "sports" -> R.drawable.ic_sports_soccer_24
            "technology" -> R.drawable.ic_memory_24
            "military" -> R.drawable.ic_security_24
            "current-affairs" -> R.drawable.ic_trending_24
            // Region icons (regions are dynamically fetched from backend)
            "hk" -> R.drawable.ic_flag_24
            "china" -> R.drawable.ic_flag_24
            "uk" -> R.drawable.ic_flag_24
            "usa" -> R.drawable.ic_flag_24
            "asia-others" -> R.drawable.ic_public_24
            "europe-others" -> R.drawable.ic_public_24
            else -> R.drawable.shoppingmode_24
        }
    }
    
    /**
     * Adapter for displaying sections with headers outside and above cards
     */
    inner class SectionAdapter : RecyclerView.Adapter<SectionAdapter.SectionViewHolder>() {
        
        inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val headerContainer: RelativeLayout = itemView.findViewById(R.id.section_header_container)
            val headerTv: TextView = itemView.findViewById(R.id.section_header_tv)
            val clearButton: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.section_clear_button)
            val cardContainer: LinearLayout = itemView.findViewById(R.id.card_container)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_topic_section, parent, false)
            return SectionViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
            val item = displayList[position]
            
            when (item) {
                is DisplayItem.Section -> {
                    // Show header container above the card
                    holder.headerContainer.visibility = View.VISIBLE
                    holder.headerTv.text = item.title
                    
                    // Show clear button only for trending topics section and only if there are selected topics
                    val isTrendingSection = item.title == sectionTrendingString
                    val hasSelectedTopicsInSection = item.topics.any { selectedTopics.contains(it.tag) }
                    holder.clearButton.visibility = if (isTrendingSection && hasSelectedTopicsInSection) View.VISIBLE else View.GONE
                    
                    // Set up clear button click listener for trending topics
                    if (isTrendingSection) {
                        holder.clearButton.setOnClickListener {
                            clearSectionTopics(item.topics)
                        }
                    }
                    
                    // Show card container
                    holder.cardContainer.visibility = View.VISIBLE
                    
                    // Only rebuild views if the number of topics changed or if this is a fresh bind
                    val currentChildCount = holder.cardContainer.childCount
                    val expectedChildCount = item.topics.size
                    
                    if (currentChildCount != expectedChildCount) {
                        // Rebuild all views if count changed
                        holder.cardContainer.removeAllViews()
                        buildTopicViews(holder.cardContainer, item.topics)
                    } else {
                        // Update existing views - much faster than rebuilding
                        updateTopicViews(holder.cardContainer, item.topics)
                    }
                }
            }
        }
        
        private fun buildTopicViews(container: LinearLayout, topics: List<TopicListEntry.TopicDTO>) {
            topics.forEachIndexed { index, topic ->
                val topicView = LayoutInflater.from(container.context)
                    .inflate(R.layout.item_topic_row, container, false)
                
                // Store topic tag in view for change detection
                topicView.tag = topic.tag
                
                val topicRow = topicView.findViewById<LinearLayout>(R.id.topic_row)
                val topicTv = topicView.findViewById<TextView>(R.id.topic_tv)
                val topicIcon = topicView.findViewById<ImageView>(R.id.topic_icon)
                val checkmarkIv = topicView.findViewById<ImageView>(R.id.checkmark_iv)
                val divider = topicView.findViewById<View>(R.id.divider)
                
                topicTv.text = topic.displayName
                
                // Set topic icon
                val iconRes = getTopicIcon(topic.tag)
                topicIcon.setImageResource(iconRes)
                topicIcon.setColorFilter(getColor(R.color.colorTextMiddle))
                topicIcon.tag = iconRes
                
                // Hide divider for last item
                divider.visibility = if (index == topics.size - 1) View.GONE else View.VISIBLE
                
                // Set selection state
                val isSelected = selectedTopics.contains(topic.tag)
                val checkmarkRes = if (isSelected) R.drawable.ic_check_circle_filled else R.drawable.ic_circle_outline
                checkmarkIv.setImageResource(checkmarkRes)
                checkmarkIv.setColorFilter(
                    if (isSelected) getColor(R.color.global_color) 
                    else getColor(R.color.colorTextHint)
                )
                checkmarkIv.tag = checkmarkRes
                
                // Set click listener
                topicRow.setOnClickListener {
                    toggleTopic(topic)
                }
                
                container.addView(topicView)
            }
        }
        
        private fun updateTopicViews(container: LinearLayout, topics: List<TopicListEntry.TopicDTO>) {
            topics.forEachIndexed { index, topic ->
                val topicView = container.getChildAt(index) ?: run {
                    // If view doesn't exist, rebuild all
                    container.removeAllViews()
                    buildTopicViews(container, topics)
                    return
                }
                
                val topicTv = topicView.findViewById<TextView>(R.id.topic_tv)
                val topicIcon = topicView.findViewById<ImageView>(R.id.topic_icon)
                val checkmarkIv = topicView.findViewById<ImageView>(R.id.checkmark_iv)
                val divider = topicView.findViewById<View>(R.id.divider)
                
                // Check if topic data changed by comparing stored tag
                val storedTag = topicView.tag as? String
                val topicChanged = storedTag != topic.tag
                
                if (topicChanged) {
                    // Topic changed, update all fields
                    topicView.tag = topic.tag
                    topicTv.text = topic.displayName
                    
                    val iconRes = getTopicIcon(topic.tag)
                    topicIcon.setImageResource(iconRes)
                    topicIcon.setColorFilter(getColor(R.color.colorTextMiddle))
                    topicIcon.tag = iconRes
                }
                
                // Hide divider for last item
                divider.visibility = if (index == topics.size - 1) View.GONE else View.VISIBLE
                
                // Update selection state (this can change even if topic didn't)
                val isSelected = selectedTopics.contains(topic.tag)
                val checkmarkRes = if (isSelected) R.drawable.ic_check_circle_filled else R.drawable.ic_circle_outline
                val currentCheckmarkRes = checkmarkIv.tag as? Int
                if (currentCheckmarkRes != checkmarkRes) {
                    checkmarkIv.setImageResource(checkmarkRes)
                    checkmarkIv.setColorFilter(
                        if (isSelected) getColor(R.color.global_color) 
                        else getColor(R.color.colorTextHint)
                    )
                    checkmarkIv.tag = checkmarkRes
                }
                
                // Update click listener with current topic
                val topicRow = topicView.findViewById<LinearLayout>(R.id.topic_row)
                topicRow.setOnClickListener {
                    toggleTopic(topic)
                }
            }
        }
        
        override fun getItemCount(): Int = displayList.size
    }
}
