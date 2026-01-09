package com.anssy.znewspro.ui.mainfrag

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anssy.znewspro.R
import com.anssy.znewspro.databinding.FragYourFeedBinding
import com.anssy.znewspro.entry.SearchListEntry
import com.anssy.znewspro.entry.TopicListEntry
import com.anssy.znewspro.model.PersonRecommendModel
import com.anssy.znewspro.model.TopicModel
import com.anssy.znewspro.selfview.NewNestedScrollView
import com.anssy.znewspro.selfview.popup.SortPopupWindow
import com.anssy.znewspro.ui.MainActivity
import com.anssy.znewspro.ui.topicmodify.TopicSelectionActivity
import com.anssy.znewspro.utils.Constants
import com.anssy.znewspro.utils.ToastUtils
import com.anssy.znewspro.utils.Utils
import android.widget.LinearLayout
import com.scwang.smartrefresh.layout.api.RefreshLayout
import com.scwang.smartrefresh.layout.listener.OnRefreshLoadMoreListener

/**
 * Fragment displaying the "Your Feed" page with personalized news recommendations
 */
class YourFeedFragment : Fragment() {
    
    private var _binding: FragYourFeedBinding? = null
    private val binding get() = _binding!!
    
    private val personRecommendModel: PersonRecommendModel by activityViewModels()
    private val topicModel: TopicModel by activityViewModels()
    
    private var pageNo = 1
    private val pageSize = 10
    private var isRefresh = true
    private var isLastPage = false
    private var isLoadingMore = false
    
    private lateinit var mAdapter: YourFeedAdapter
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
        _binding = FragYourFeedBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSortAndTags()
        observeData()
        loadData()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh tags when fragment becomes visible to ensure active tags indicator is up to date
        topicModel.queryMyTopics()
    }
    
    private fun setupRecyclerView() {
        binding.homeRecycler.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        mAdapter = YourFeedAdapter(requireContext(), mNewsList) { article ->
            shareArticle(article)
        }
        binding.homeRecycler.adapter = mAdapter
        
        // Add RecyclerView scroll listener
        android.util.Log.d("YourFeedFragment", "Adding RecyclerView scroll listener")
        binding.homeRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                android.util.Log.d("YourFeedFragment", "RecyclerView onScrolled: dx=$dx, dy=$dy")
                val activity = requireActivity() as MainActivity
                if (dy > 0) {
                    android.util.Log.d("YourFeedFragment", "RecyclerView scrolling down - hiding bottom bar")
                    activity.hideBottomBar()
                } else if (dy < 0) {
                    android.util.Log.d("YourFeedFragment", "RecyclerView scrolling up - showing bottom bar")
                    activity.showBottomBar()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                android.util.Log.d("YourFeedFragment", "RecyclerView onScrollStateChanged: $newState")
                val activity = requireActivity() as MainActivity
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    android.util.Log.d("YourFeedFragment", "RecyclerView IDLE - scheduling auto-show")
                    activity.scheduleBottomBarAutoShow()
                } else {
                    android.util.Log.d("YourFeedFragment", "RecyclerView not IDLE - canceling auto-show")
                    activity.cancelBottomBarAutoShow()
                }
            }
        })
        
        // Add NestedScrollView scroll listener as backup
        android.util.Log.d("YourFeedFragment", "Adding NestedScrollView scroll listener")
        binding.nestedScrollView.addScrollChangeListener(object :
            NewNestedScrollView.AddScrollChangeListener {
            override fun onScrollChange(
                scrollX: Int,
                scrollY: Int,
                oldScrollX: Int,
                oldScrollY: Int
            ) {
                android.util.Log.d("YourFeedFragment", "NestedScrollView onScrollChange: scrollY=$scrollY, oldScrollY=$oldScrollY, isLoadingMore=$isLoadingMore")
                val activity = requireActivity() as MainActivity
                
                // Detect suspicious scroll changes that might be load-more rebound
                // The rebound typically shows scrollY < oldScrollY with a large difference
                val scrollDifference = oldScrollY - scrollY
                val isSuspiciousRebound = scrollDifference > 100 && scrollY < oldScrollY
                
                // Only handle scroll direction changes if we're not loading more and not a suspicious rebound
                if (!isLoadingMore && !isSuspiciousRebound) {
                    if (scrollY > oldScrollY) {
                        android.util.Log.d("YourFeedFragment", "NestedScrollView scrolling down - hiding bottom bar")
                        activity.hideBottomBar()
                    } else if (scrollY < oldScrollY) {
                        android.util.Log.d("YourFeedFragment", "NestedScrollView scrolling up - showing bottom bar")
                        activity.showBottomBar()
                    }
                } else {
                    if (isSuspiciousRebound) {
                        android.util.Log.d("YourFeedFragment", "NestedScrollView suspicious rebound detected (diff=$scrollDifference) - ignoring scroll change")
                    } else {
                        android.util.Log.d("YourFeedFragment", "NestedScrollView scroll change during load-more - ignoring")
                    }
                }
            }

            override fun onScrollState(state: NewNestedScrollView.ScrollState?) {
                android.util.Log.d("YourFeedFragment", "NestedScrollView onScrollState: $state")
                val activity = requireActivity() as MainActivity
                when (state) {
                    NewNestedScrollView.ScrollState.IDLE -> {
                        android.util.Log.d("YourFeedFragment", "NestedScrollView IDLE - scheduling auto-show")
                        activity.scheduleBottomBarAutoShow()
                    }
                    NewNestedScrollView.ScrollState.DRAG, NewNestedScrollView.ScrollState.SCROLLING -> {
                        android.util.Log.d("YourFeedFragment", "NestedScrollView DRAG/SCROLLING - canceling auto-show")
                        activity.cancelBottomBarAutoShow()
                    }
                    else -> {}
                }
            }
        })
        
        binding.smartRefresh.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                // Only proceed if this is a pull-to-refresh, not a button-triggered refresh
                if (!isButtonRefresh) {
                    // Check debounce timing for pull-to-refresh as well
                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastRefresh = currentTime - lastRefreshTime
                    if (timeSinceLastRefresh < minimumTimeBetweenRefreshes) {
                        android.util.Log.d("YourFeedFragment", "Pull-to-refresh blocked - too soon since last refresh (${timeSinceLastRefresh}ms < ${minimumTimeBetweenRefreshes}ms)")
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
                if (isLastPage) {
                    refreshLayout.finishLoadMore(true)
                    return
                }
                android.util.Log.d("YourFeedFragment", "Starting load-more - setting isLoadingMore=true")
                isLoadingMore = true
                isRefresh = false
                pageNo++
                personRecommendModel.queryRecommendList(pageNo, pageSize)
            }
        })
    }
    
    private fun setupSortAndTags() {
        updateSortIndicator()
        updateActiveTags()
        
        binding.sortChip.setOnClickListener { showSortPopup() }
    }
    
    @SuppressLint("NotifyDataSetChanged")
    private fun observeData() {
        topicModel.myTopicsEntry.observe(viewLifecycleOwner) { response ->
            if (response != null && response.code == Constants.SUCCESS_CODE) {
                userSelectedTopics.clear()
                response.data?.topics?.let { topics ->
                    userSelectedTopics.addAll(topics)
                }
                updateActiveTags()
            }
        }
        
        topicModel.topicListEntry.observe(viewLifecycleOwner) { response ->
            if (response != null && response.code == Constants.SUCCESS_CODE) {
                allTopics.clear()
                response.data?.topics?.let { topics ->
                    allTopics.addAll(topics)
                }
            }
        }
        
        topicModel.commonResponseEntry.observe(viewLifecycleOwner) { response ->
            if (response != null && response.code == Constants.SUCCESS_CODE) {
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
                        binding.smartRefresh.finishLoadMore(true)
                        android.util.Log.d("YourFeedFragment", "Load-more completed - setting isLoadingMore=false")
                        isLoadingMore = false
                    }
                    isLastPage = it.data.articles.isEmpty()
                    mNewsList.addAll(it.data.articles)
                    applyCurrentSort()
                    val lastPosition = mNewsList.size
                    if (isRefresh){
                        mAdapter.notifyDataSetChanged()
                        // Trigger wave animation after data is refreshed (only for button refresh, not pull-to-refresh or tab switch)
                        // Check if fragment is currently visible to prevent animation when switching tabs
                        if (isButtonRefresh && isResumed && isVisible) {
                            binding.homeRecycler.post {
                                triggerWaveAnimation()
                            }
                        }
                    }else{
                        mAdapter.notifyItemRangeInserted(lastPosition,it.data.articles.size)
                    }
                } else {
                    if (isRefresh) {
                        finishRefreshWithMinimumDuration(false)
                    } else {
                        binding.smartRefresh.finishLoadMore(false)
                        android.util.Log.d("YourFeedFragment", "Load-more failed - setting isLoadingMore=false")
                        isLoadingMore = false
                    }
                    if (it.code == 1000) {
                        ToastUtils.showShortToast(requireContext(), getString(R.string.server_error_message))
                    } else {
                        ToastUtils.showShortToast(requireContext(), it.msg)
                    }
                }
            }
        }
    }
    
    private fun loadData() {
        topicModel.queryMyTopics()
        topicModel.queryAllTopics()
        personRecommendModel.queryRecommendList(pageNo, pageSize)
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
        popup.showPopupWindow(binding.sortChip)
    }
    
    private fun updateSortIndicator() {
        val sortValue = when (currentSort) {
            SortPopupWindow.SortOption.LATEST -> getString(R.string.latest_sort_option)
            SortPopupWindow.SortOption.POPULAR -> getString(R.string.popular_sort_option)
            SortPopupWindow.SortOption.RELEVANT -> "Relevant"
        }
        binding.sortValueTv.text = sortValue
        
        val sortIcon = when (currentSort) {
            SortPopupWindow.SortOption.LATEST -> R.drawable.ic_clock_24
            SortPopupWindow.SortOption.POPULAR -> R.drawable.ic_trending_24
            SortPopupWindow.SortOption.RELEVANT -> R.drawable.ic_star_24
        }
        binding.sortInfoIcon.setImageResource(sortIcon)
    }
    
    private fun updateActiveTags() {
        binding.activeTagsChipsContainer.removeAllViews()
        
        val activeTags = userSelectedTopics
        
        if (activeTags.isEmpty()) {
            // "Select tags to follow" chip
            val selectTagsChip = LayoutInflater.from(requireContext()).inflate(
                R.layout.item_tag_chip,
                binding.activeTagsChipsContainer,
                false
            )
            val selectTagsIcon = selectTagsChip.findViewById<ImageView>(R.id.tag_icon)
            val selectTagsText = selectTagsChip.findViewById<TextView>(R.id.tag_text)
            
            selectTagsText.text = getString(R.string.select_tags_to_follow)
            selectTagsText.setTextColor(requireContext().getColor(R.color.colorTextMiddle))
            selectTagsIcon.setImageResource(R.drawable.shoppingmode_24)
            selectTagsIcon.setColorFilter(requireContext().getColor(R.color.colorTextMiddle))
            
            // Adjust layout params to remove bottom margin for horizontal scroll and ensure vertical centering
            (selectTagsChip.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                lp.bottomMargin = 0
                lp.gravity = android.view.Gravity.CENTER_VERTICAL
                selectTagsChip.layoutParams = lp
            }
            
            selectTagsChip.setOnClickListener {
                showTopicSelection()
            }
            
            binding.activeTagsChipsContainer.addView(selectTagsChip)
            
            // Manage chip with chevron
            val manageView = LayoutInflater.from(requireContext()).inflate(
                R.layout.item_tag_chip,
                binding.activeTagsChipsContainer,
                false
            )
            val manageIcon = manageView.findViewById<ImageView>(R.id.tag_icon)
            val manageText = manageView.findViewById<TextView>(R.id.tag_text)
            manageIcon.setImageResource(R.drawable.ic_chevron_right_24)
            manageIcon.setColorFilter(requireContext().getColor(R.color.colorTextMiddle))
            manageText.visibility = View.GONE
            (manageIcon.layoutParams as? android.widget.LinearLayout.LayoutParams)?.let { lp ->
                lp.marginEnd = 0
                manageIcon.layoutParams = lp
            }
            // Smaller pointer cardlet with same rounding as regular tags, less padding
            manageView.background = requireContext().getDrawable(R.drawable.tag_chip_background)
            val padV = Utils.dpToPx(2f, resources) // Reduced vertical padding for smaller size
            val padH = Utils.dpToPx(4f, resources) // Reduced horizontal padding for smaller size
            manageView.setPadding(padH, padV, padH, padV)
            // Adjust layout params to remove bottom margin for horizontal scroll and ensure vertical centering
            (manageView.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                lp.bottomMargin = 0
                lp.gravity = android.view.Gravity.CENTER_VERTICAL
                manageView.layoutParams = lp
            }
            manageView.setOnClickListener { showTopicSelection() }
            binding.activeTagsChipsContainer.addView(manageView)
        } else {
            activeTags.forEach { tag ->
                val chipView = LayoutInflater.from(requireContext()).inflate(
                    R.layout.item_tag_chip,
                    binding.activeTagsChipsContainer,
                    false
                )
                val tagIcon = chipView.findViewById<ImageView>(R.id.tag_icon)
                val tagText = chipView.findViewById<TextView>(R.id.tag_text)
                
                tagText.text = tag.displayName
                tagIcon.setImageResource(getTagIcon(tag.tag))
                
                // Adjust layout params to remove bottom margin for horizontal scroll and ensure vertical centering
                (chipView.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                    lp.bottomMargin = 0
                    lp.gravity = android.view.Gravity.CENTER_VERTICAL
                    chipView.layoutParams = lp
                }
                
                // Clicking on any tag cardlet should open the menu
                chipView.setOnClickListener {
                    showTopicSelection()
                }
                
                binding.activeTagsChipsContainer.addView(chipView)
            }
            
            val manageView = LayoutInflater.from(requireContext()).inflate(
                R.layout.item_tag_chip,
                binding.activeTagsChipsContainer,
                false
            )
            val manageIcon = manageView.findViewById<ImageView>(R.id.tag_icon)
            val manageText = manageView.findViewById<TextView>(R.id.tag_text)
            manageIcon.setImageResource(R.drawable.ic_chevron_right_24)
            manageIcon.setColorFilter(requireContext().getColor(R.color.colorTextMiddle))
            manageText.visibility = View.GONE
            (manageIcon.layoutParams as? android.widget.LinearLayout.LayoutParams)?.let { lp ->
                lp.marginEnd = 0
                manageIcon.layoutParams = lp
            }
            // Smaller pointer cardlet with same rounding as regular tags, less padding
            manageView.background = requireContext().getDrawable(R.drawable.tag_chip_background)
            val padV = Utils.dpToPx(2f, resources) // Reduced vertical padding for smaller size
            val padH = Utils.dpToPx(4f, resources) // Reduced horizontal padding for smaller size
            manageView.setPadding(padH, padV, padH, padV)
            // Adjust layout params to remove bottom margin for horizontal scroll and ensure vertical centering
            (manageView.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                lp.bottomMargin = 0
                lp.gravity = android.view.Gravity.CENTER_VERTICAL
                manageView.layoutParams = lp
            }
            manageView.setOnClickListener { showTopicSelection() }
            // Add chevron cardlet last to ensure it's always rightmost
            binding.activeTagsChipsContainer.addView(manageView)
        }
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
            else -> R.drawable.shoppingmode_24 // Default icon changed to shoppingmode
        }
    }
    
    private fun removeActiveTag(tag: String) {
        topicModel.editTopic(Constants.TYPE_TOPIC_DELETE, tag)
        
        isRefresh = true
        pageNo = 1
        personRecommendModel.queryRecommendList(pageNo, pageSize)
    }
    
    private fun showTopicSelection() {
        val intent = Intent(requireContext(), TopicSelectionActivity::class.java)
        val selectedTopicTags = userSelectedTopics.map { it.tag }
        intent.putStringArrayListExtra(TopicSelectionActivity.EXTRA_SELECTED_TOPICS, ArrayList(selectedTopicTags))
        (parentFragment as? AdviceFrag)?.topicSelectionLauncher?.launch(intent)
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
    
    private fun finishRefreshWithMinimumDuration(success: Boolean) {
        val elapsed = System.currentTimeMillis() - refreshStartTime
        val remainingTime = minimumRefreshDuration - elapsed
        
        if (remainingTime > 0) {
            Handler(Looper.getMainLooper()).postDelayed({
                binding.smartRefresh.finishRefresh(success)
                isButtonRefresh = false
            }, remainingTime)
        } else {
            binding.smartRefresh.finishRefresh(success)
            isButtonRefresh = false
        }
    }
    
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
    
    fun refreshData() {
        if (!isAdded || isDetached || activity == null) {
            return
        }
        
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRefresh = currentTime - lastRefreshTime
        if (timeSinceLastRefresh < minimumTimeBetweenRefreshes) {
            return
        }
        
        lastRefreshTime = currentTime
        refreshStartTime = System.currentTimeMillis()
        isButtonRefresh = true
        binding.smartRefresh.autoRefresh()
        
        binding.homeRecycler.post {
            binding.homeRecycler.scrollToPosition(0)
        }
        
        isRefresh = true
        pageNo = 1
        personRecommendModel.queryRecommendList(pageNo, pageSize)
        // Also refresh tags when refreshing data
        topicModel.queryMyTopics()
    }
    
    /**
     * Trigger wave animation for news cards when reloaded via bottom bar
     * Each card animates down 20dp over 180ms (smooth easeOut), then back to 0px over 320ms (smooth easeInOut)
     * Stagger delay: cardIndex * 40ms for tighter wave effect
     */
    private fun triggerWaveAnimation() {
        val recyclerView = binding.homeRecycler
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        
        // Convert 20dp to pixels
        val density = resources.displayMetrics.density
        val offsetPx = 20f * density
        
        // Smooth interpolators for more natural motion
        // FastOutSlowIn curve: (0.4f, 0f, 0.2f, 1f) - smooth deceleration
        val smoothEaseOut = PathInterpolator(0.4f, 0f, 0.2f, 1f)
        // Standard easeInOut curve: (0.4f, 0f, 0.6f, 1f) - smooth acceleration and deceleration
        val smoothEaseInOut = PathInterpolator(0.4f, 0f, 0.6f, 1f)
        
        // Reset all card offsets to 0 before animating
        for (i in 0 until layoutManager.childCount) {
            val child = layoutManager.getChildAt(i)
            child?.translationY = 0f
        }
        
        // Animate each visible card with stagger delay
        for (i in 0 until layoutManager.childCount) {
            val child = layoutManager.getChildAt(i) ?: continue
            val cardIndex = i
            val delay = cardIndex * 40L // 40ms stagger per card for tighter wave
            
            // Stage 1: Animate down by 20dp over 180ms with smooth easeOut
            val stage1 = ObjectAnimator.ofFloat(child, "translationY", 0f, offsetPx).apply {
                duration = 180
                interpolator = smoothEaseOut
            }
            
            // Stage 2: Animate back to 0px over 320ms with smooth easeInOut
            val stage2 = ObjectAnimator.ofFloat(child, "translationY", offsetPx, 0f).apply {
                duration = 320
                interpolator = smoothEaseInOut
                startDelay = 180 // Start after stage 1 completes
            }
            
            // Combine both stages
            AnimatorSet().apply {
                playSequentially(stage1, stage2)
                startDelay = delay
                start()
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


