package com.searcher.zonenews.ui.mainfrag

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
import com.searcher.zonenews.R
import com.searcher.zonenews.databinding.FragYourFeedBinding
import com.searcher.zonenews.entry.SearchListEntry
import com.searcher.zonenews.entry.TopicListEntry
import com.searcher.zonenews.model.PersonRecommendModel
import com.searcher.zonenews.model.TopicModel
import com.searcher.zonenews.model.MyModel
import com.searcher.zonenews.ui.newsdetail.SubscriptionBottomSheetFragment
import com.searcher.zonenews.ui.newsdetail.TagNewsBottomSheetFragment
import com.searcher.zonenews.utils.SystemDialogUtils


import com.searcher.zonenews.selfview.popup.SortPopupWindow
import com.searcher.zonenews.ui.MainActivity
import com.searcher.zonenews.ui.topicmodify.TopicSelectionActivity
import com.searcher.zonenews.utils.Constants
import com.searcher.zonenews.utils.ToastUtils
import com.searcher.zonenews.utils.Utils
import android.widget.LinearLayout
import com.scwang.smartrefresh.layout.api.RefreshLayout
import com.scwang.smartrefresh.layout.listener.OnRefreshLoadMoreListener
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider

/**
 * Fragment displaying the "Your Feed" page with personalized news recommendations
 */
class YourFeedFragment : Fragment() {
    
    private var _binding: FragYourFeedBinding? = null
    private val binding get() = _binding!!
    
    private val personRecommendModel: PersonRecommendModel by activityViewModels()
    private val topicModel: TopicModel by activityViewModels()
    private val myModel: MyModel by activityViewModels()
    
    private var isPro = false
    
    private var pageNo = 1
    private val pageSize = 10
    private var isRefresh = true
    private var isLastPage = false
    private var isLoadingMore = false
    
    private lateinit var newsAdapter: YourFeedAdapter
    
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
        personRecommendModel.queryRecommendList(pageNo, pageSize)
        
        // Load all topics for optimistic following logic
        topicModel.queryAllTopics()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh tags when fragment becomes visible to ensure active tags indicator is up to date
        topicModel.queryMyTopics()
        
        // Check if tutorial needs to be shown when fragment becomes visible
        if (mNewsList.isNotEmpty()) {
            binding.homeRecycler.post {
                showTutorialIfNeeded()
            }
        }
    }
    
    private fun setupRecyclerView() {
        // Use standard LinearLayoutManager (no extra layout space needed now)
        val layoutManager = LinearLayoutManager(requireContext(), RecyclerView.VERTICAL, false)
        binding.homeRecycler.layoutManager = layoutManager
        
        // Create single adapter with header support
        newsAdapter = YourFeedAdapter(
            requireContext(),
            mNewsList,
            { article -> shareArticle(article) },
            object : YourFeedAdapter.HeaderCallback {
                override fun onSortClick(anchor: View) {
                    showSortPopup(anchor)
                }

                override fun onManageTagsClick() {
                    showTopicSelection()
                }

                override fun onTagClick(tag: TopicListEntry.TopicDTO) {
                    showTagNews(tag)
                }

                override fun onTagUnselected(tag: TopicListEntry.TopicDTO) {
                    removeActiveTag(tag.tag)
                }

                override fun onFollowTag(tag: String) {
                    // Use shared optimistic logic to ensure all components stay in sync
                    topicModel.updateMyTopicsOptimistically(Constants.TYPE_TOPIC_ADD, tag)
                    
                    topicModel.editTopic(Constants.TYPE_TOPIC_ADD, tag)
                    
                    // No longer refreshing feed content here to avoid disrupting the user's scroll position.
                    // The new tag will be reflected in the "Active Tags" row via observation, 
                    // and its content will appear naturally on the next manual refresh or reload.
                }
            }
        )
        
        // Set adapter directly (no ConcatAdapter)
        binding.homeRecycler.adapter = newsAdapter
        
        // --- Batch Image Preloading Logic ---
        // Preload 7 items ahead forpersonalized feed
        val sizeProvider = ViewPreloadSizeProvider<SearchListEntry.DataDTO.ArticlesDTO>()
        val preloader = RecyclerViewPreloader<SearchListEntry.DataDTO.ArticlesDTO>(
            Glide.with(this), newsAdapter, sizeProvider, 7
        )
        binding.homeRecycler.addOnScrollListener(preloader)
        
        // Optimize RecyclerView for variable height items
        binding.homeRecycler.setItemViewCacheSize(20)
        binding.homeRecycler.setHasFixedSize(false)
        
        // Add RecyclerView scroll listener
        binding.homeRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                // Only process scroll events if this fragment is currently visible
                if (!isResumed || !isVisible) return
                
                val activity = requireActivity() as MainActivity
                if (dy > 0) {
                    activity.hideBottomBar()
                } else if (dy < 0) {
                    activity.showBottomBar()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                
                // Only process state changes if this fragment is currently visible
                if (!isResumed || !isVisible) return
                
                val activity = requireActivity() as MainActivity
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    activity.scheduleBottomBarAutoShow()
                    
                    // Load more when scroll stops at bottom
                    if (!recyclerView.canScrollVertically(1) && !isLastPage && !isRefresh) {
                        android.util.Log.d("YourFeedFragment", "Loading more at bottom (scroll stopped)")
                        isRefresh = false
                        pageNo++
                        personRecommendModel.queryRecommendList(pageNo, pageSize)
                    }
                } else {
                    activity.cancelBottomBarAutoShow()
                }
            }
        })
        
        // Disable SmartRefreshLayout load-more completely to avoid jerk
        binding.smartRefresh.setEnableLoadMore(false)
        
        binding.smartRefresh.setOnRefreshListener { refreshLayout ->
            // Only proceed if this is a pull-to-refresh, not a button-triggered refresh
            if (!isButtonRefresh) {
                // Check debounce timing for pull-to-refresh as well
                val currentTime = System.currentTimeMillis()
                val timeSinceLastRefresh = currentTime - lastRefreshTime
                if (timeSinceLastRefresh < minimumTimeBetweenRefreshes) {
                    refreshLayout.finishRefresh()
                    return@setOnRefreshListener
                }
                
                // Update last refresh time and proceed
                lastRefreshTime = currentTime
                refreshStartTime = System.currentTimeMillis()
                isRefresh = true
                pageNo = 1
                personRecommendModel.queryRecommendList(pageNo, pageSize)
            }
        }
    }
    
    private fun setupSortAndTags() {
        newsAdapter.updateSort(currentSort)
        newsAdapter.updateTags(userSelectedTopics)
    }
    
    @SuppressLint("NotifyDataSetChanged")
    private fun observeData() {
        myModel.myEntry.observe(viewLifecycleOwner) { response ->
            if (response != null && response.code == Constants.SUCCESS_CODE) {
                isPro = response.data?.isPro == true
            }
        }
        
        topicModel.myTopicsEntry.observe(viewLifecycleOwner) { response ->
            if (response != null && response.code == Constants.SUCCESS_CODE) {
                userSelectedTopics.clear()
                response.data?.topics?.let { topics ->
                    userSelectedTopics.addAll(topics)
                }
                newsAdapter.updateTags(userSelectedTopics)
                // Force RecyclerView to recalculate scroll extent after tags update
                binding.homeRecycler.post {
                    binding.homeRecycler.invalidateItemDecorations()
                }
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
                // Success is already handled optimistically or via myTopicsEntry/recommendList refresh
            }
        }
        
        personRecommendModel.recommendListEntry.observe(viewLifecycleOwner) {
            if (it != null) {
                if (it.code == Constants.SUCCESS_CODE) {
                    if (isRefresh) {
                        // Hide Shimmer, show content
                        val shimmerLayout = binding.root.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmer_view_container)
                        shimmerLayout?.stopShimmer()
                        binding.root.findViewById<View>(R.id.shimmer_layout)?.visibility = View.GONE
                        binding.smartRefresh.visibility = View.VISIBLE
                        
                        mNewsList.clear()
                        finishRefreshWithMinimumDuration(true)
                    }
                    isLastPage = it.data.articles.isEmpty()
                    mNewsList.addAll(it.data.articles)
                    
                    // Reset refresh flag after processing data to enable load-more
                    val wasRefresh = isRefresh
                    isRefresh = false
                    
                    if (wasRefresh) {
                        newsAdapter.notifyDataSetChanged()
                        
                        if (isButtonRefresh && isResumed && isVisible) {
                            binding.homeRecycler.post {
                                triggerWaveAnimation()
                            }
                        }
                        
                        // Show tutorial (TutorialManager handles once-only logic)
                        if (mNewsList.isNotEmpty()) {
                            binding.homeRecycler.post {
                                showTutorialIfNeeded()
                            }
                        }
                    } else {
                        // Calculate insert index - account for header at position 0
                        val startInsertIndex = (mNewsList.size - it.data.articles.size) + 1
                        newsAdapter.notifyItemRangeInserted(startInsertIndex, it.data.articles.size)
                        
                        binding.smartRefresh.finishLoadMore()
                    }
                } else {
                    if (isRefresh) {
                        // Hide Shimmer on error too
                        val shimmerLayout = binding.root.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmer_view_container)
                        shimmerLayout?.stopShimmer()
                        binding.root.findViewById<View>(R.id.shimmer_layout)?.visibility = View.GONE
                        binding.smartRefresh.visibility = View.VISIBLE
                        
                        finishRefreshWithMinimumDuration(false)
                    } else {
                        binding.smartRefresh.finishLoadMore(false)
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
    
    // Helper method for shareArticle if it was inline before, but it's separate method
    
    // Remove obsolete methods
    private fun showSortPopup(anchor: View) {
        val popup = SortPopupWindow(requireContext(), object : SortPopupWindow.Callback {
            override fun onSortSelected(option: SortPopupWindow.SortOption) {
                currentSort = option
                newsAdapter.updateSort(currentSort)
                applyCurrentSort()
                newsAdapter.notifyDataSetChanged()
            }
        })
        popup.setCurrentSort(currentSort)
        popup.showPopupWindow(anchor)
    }
    
    // Obsolete tag methods removed
    
    private fun removeActiveTag(tag: String) {
        // Use shared optimistic logic to ensure all components stay in sync
        topicModel.updateMyTopicsOptimistically(Constants.TYPE_TOPIC_DELETE, tag)

        // API call to update server
        topicModel.editTopic(Constants.TYPE_TOPIC_DELETE, tag)
        
        // No longer refreshing feed content here to avoid disrupting the user's scroll position.
        // The tag will disappear from the "Active Tags" row via observation, but the current 
        // feed articles will remain until the user manually refreshes or reloads.
    }
    
    private fun showTagNews(topic: TopicListEntry.TopicDTO) {
        val displayName = if (topic.tag.equals("General", ignoreCase = true)) {
            getString(R.string.topic_general)
        } else {
            topic.displayName ?: topic.tag
        }
        val fragment = TagNewsBottomSheetFragment.newInstance(
            tagName = displayName,
            tagApiId = topic.tag,
            isFollowing = true,
            sourceFragment = "your_feed"
        )
        fragment.show(childFragmentManager, "TagNewsBottomSheet")
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
        newsAdapter.notifyDataSetChanged()
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
            append("\n\n")
            append("https://zonenews.io/article/${article.articleID}")
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
    
    /**
     * Show tutorial overlay if it hasn't been shown yet
     */
    /**
     * Show tutorial overlay if it hasn't been shown yet
     */
    private fun showTutorialIfNeeded() {
        val context = requireContext()
        
        // ONLY show tutorial if fragment is actually visible and resumed
        // This prevents tutorials for background-preloaded fragments from appearing
        if (!isResumed || !isVisible) return
        
        var accountId = com.searcher.zonenews.utils.SharedPreferenceUtils.getString(context, "current_account_id")
        
        // If accountId is empty, wait briefly for profile data to load (defensive check)
        // This handles edge cases where fragment loads before account ID is set
        if (accountId.isEmpty()) {
            // Post a delayed check to retry after a short period
            binding.homeRecycler.postDelayed({
                if (isResumed && isVisible && mNewsList.isNotEmpty()) {
                    showTutorialIfNeeded()
                }
            }, 500)
            return
        }
        
        // Check if welcome poster has been shown - if not, show it first
        // If tutorials were just reset, skip the welcome poster override
        val skipPosterOverride = com.searcher.zonenews.utils.SharedPreferenceUtils.getBoolean(context, "skip_welcome_poster_once")
        if (skipPosterOverride) {
            com.searcher.zonenews.utils.SharedPreferenceUtils.saveBooleanCommit(context, "skip_welcome_poster_once", false)
        }
        
        if (!skipPosterOverride && !com.searcher.zonenews.utils.TutorialManager.hasWelcomePosterBeenShown(context, accountId)) {
            showWelcomePoster(accountId)
            return
        }
        
        // Check if tutorial has already been shown
        if (com.searcher.zonenews.utils.TutorialManager.hasTutorialBeenShown(
                context, 
                com.searcher.zonenews.utils.TutorialManager.TUTORIAL_YOUR_FEED,
                accountId
            )) {
            return
        }
        
        // Create tutorial steps
        val steps = listOf(
            com.searcher.zonenews.selfview.TutorialOverlayView.TutorialStep(
                id = "personal_welcome",
                message = getString(R.string.tutorial_personal_step1_message),
                hasHighlight = false
            ),
            com.searcher.zonenews.selfview.TutorialOverlayView.TutorialStep(
                id = "personal_active_tags",
                message = getString(R.string.tutorial_personal_active_tags),
                hasHighlight = true
            ),
            com.searcher.zonenews.selfview.TutorialOverlayView.TutorialStep(
                id = "personal_news_row",
                message = getString(R.string.tutorial_home_step1),
                hasHighlight = true
            ),
            com.searcher.zonenews.selfview.TutorialOverlayView.TutorialStep(
                id = "personal_levity",
                message = getString(R.string.tutorial_home_levity_button),
                hasHighlight = true
            )
        )
        
        // Create overlay and add to root view (DecorView to cover status bar/toolbar)
        val decorView = requireActivity().window.decorView as ViewGroup
        val overlay = com.searcher.zonenews.selfview.TutorialOverlayView(context)
        overlay.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        
        overlay.setTutorialSteps(steps)
        overlay.setOnTutorialCompleteListener {
            com.searcher.zonenews.utils.TutorialManager.markTutorialAsShown(
                context,
                com.searcher.zonenews.utils.TutorialManager.TUTORIAL_YOUR_FEED,
                accountId
            )
            // Remove overlay from decorView when done
            decorView.removeView(overlay)
        }
        
        decorView.addView(overlay)
        
        // Get target view function
        val getTargetView: (com.searcher.zonenews.selfview.TutorialOverlayView.TutorialStep) -> View? = { step ->
            when (step.id) {
                "personal_active_tags" -> {
                    // Get the header view from the adapter (position 0)
                    val layoutManager = binding.homeRecycler.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager
                    val headerView = layoutManager?.findViewByPosition(0)
                    headerView?.findViewById(R.id.active_tags_container)
                }
                "personal_news_row" -> {
                    // Get the first news item (position 1, since 0 is header)
                    val layoutManager = binding.homeRecycler.layoutManager as? androidx.recyclerview.widget.LinearLayoutManager
                    layoutManager?.findViewByPosition(1)
                }
                "personal_levity" -> {
                    // Get levity button from parent fragment
                    parentFragment?.view?.findViewById(R.id.levity_card)
                }
                else -> null
            }
        }
        
        // Set up click listener that advances with proper callback
        overlay.setOnClickListener {
            overlay.advanceWithCallback(null, getTargetView) // NestedScrollView is null for RecyclerView based pages
        }
        
        // Start the tutorial
        overlay.start(null, getTargetView)
    }

    /**
     * Show welcome poster bottom sheet before the tutorial
     */
    private fun showWelcomePoster(accountId: String) {
        val welcomeFragment = com.searcher.zonenews.ui.mainfrag.WelcomePosterBottomSheetFragment.newInstance {
            // After welcome poster is dismissed, show the tutorial
            binding.homeRecycler.post {
                showTutorialIfNeeded()
            }
        }
        welcomeFragment.show(parentFragmentManager, "WelcomePoster")
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


