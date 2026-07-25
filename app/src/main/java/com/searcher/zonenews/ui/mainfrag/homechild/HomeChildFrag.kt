package com.searcher.zonenews.ui.mainfrag.homechild

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
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.PathInterpolator
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.searcher.zonenews.utils.Constants
import com.searcher.zonenews.R
import com.searcher.zonenews.base.BaseFragment
import com.searcher.zonenews.databinding.FragChildHomeBinding
import com.searcher.zonenews.entry.HomeDataListEntry
import com.searcher.zonenews.model.HomeModel

import com.searcher.zonenews.ui.MainActivity
import com.searcher.zonenews.ui.newsdetail.NewsDetailActivity
import com.searcher.zonenews.utils.CalculateUtil
import com.searcher.zonenews.utils.ToastUtils
import com.searcher.zonenews.utils.Utils
import com.bumptech.glide.Glide
import com.scwang.smartrefresh.layout.api.RefreshLayout
import com.scwang.smartrefresh.layout.listener.OnRefreshLoadMoreListener
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader
import com.bumptech.glide.util.ViewPreloadSizeProvider
import java.text.SimpleDateFormat
import java.util.Locale
import com.searcher.zonenews.widget.WidgetDataProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


/**
 * @Description Today/HongKong/China
 * @Author yulu
 * @CreateTime 2025年06月30日 11:22:01
 */

class HomeChildFrag : BaseFragment() {
    private lateinit var mViewBinding: FragChildHomeBinding
    private val mHomeModel: HomeModel by viewModels()

    companion object {
        private const val TYPE_NEWS = "type"
        fun getInstance(tag: String): HomeChildFrag {
            val bundle = Bundle()
            bundle.putString(TYPE_NEWS, tag)
            val childFrag = HomeChildFrag()
            childFrag.arguments = bundle
            return childFrag
        }
    }

    private lateinit var mAdapter: NewsAdapter
    
    private var mNewsList = ArrayList<HomeDataListEntry.DataDTO.ArticlesDTO>()
    private var mBannerList = ArrayList<HomeDataListEntry.DataDTO.HeadlinesDTO>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        mViewBinding = FragChildHomeBinding.inflate(inflater)
        return mViewBinding.root
    }

    override fun onStart() {
        super.onStart()
        // Banner loop handled by Adapter now
        // Reset button refresh flag when fragment becomes visible
        isButtonRefresh = false
    }

    override fun onResume() {
        super.onResume()
        // Check if tutorial needs to be shown when fragment becomes visible
        // This handles cases where data was loaded while fragment was in background
        if (shouldShowBanner() && mNewsList.isNotEmpty()) {
            mViewBinding.homeRecycler.post {
                showTutorialIfNeeded()
            }
        }
    }

    private var pageNo = 1
    private val pageSize = 10
    private var mCurrentType = ""
    override fun initData() {
        mCurrentType = arguments?.getString(TYPE_NEWS).toString()
        initView()
        initModel()
    }

    private fun initView() {
        // Show Shimmer, Hide Content initially
        mViewBinding.root.findViewById<View>(R.id.shimmer_layout)?.visibility = View.VISIBLE
        mViewBinding.smartRefresh.visibility = View.INVISIBLE

        // Show banner only on Today tab
        val showBanner = shouldShowBanner()
        
        // Create single adapter with banner support
        mAdapter = NewsAdapter(
            mContext!!,
            mNewsList,
            showBanner,
            { article -> shareArticle(article) },
            { headline ->
                // Banner click handler
                val intent = Intent(mContext, NewsDetailActivity::class.java)
                intent.putExtra("id", headline.articleID)
                intent.putExtra("source_fragment", "home")
                startActivity(intent)
            }
        )
        mAdapter.setBannerVisible(showBanner)
        mAdapter.setBannerAdapter(NewsAdapter.BannerPagerAdapter())

        // Use standard LinearLayoutManager (no extra layout space needed now)
        val layoutManager = LinearLayoutManager(mContext, RecyclerView.VERTICAL, false)
        mViewBinding.homeRecycler.layoutManager = layoutManager
        
        // Set adapter directly (no ConcatAdapter)
        mViewBinding.homeRecycler.adapter = mAdapter
        
        // --- Batch Image Preloading Logic ---
        // Preload 7 items ahead to ensure images are ready before they scroll onto screen
        val sizeProvider = ViewPreloadSizeProvider<HomeDataListEntry.DataDTO.ArticlesDTO>()
        val preloader = RecyclerViewPreloader<HomeDataListEntry.DataDTO.ArticlesDTO>(
            Glide.with(this), mAdapter, sizeProvider, 7
        )
        mViewBinding.homeRecycler.addOnScrollListener(preloader)
        
        // Optimize RecyclerView for variable height items
        mViewBinding.homeRecycler.setItemViewCacheSize(20)
        mViewBinding.homeRecycler.setHasFixedSize(false)
        
        // Add RecyclerView scroll listener
        mViewBinding.homeRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                // Only process scroll events if this fragment is currently visible
                // This prevents off-screen ViewPager pages from interfering
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
                    if (!recyclerView.canScrollVertically(1) && !isLastPage && !refresh) {
                        android.util.Log.d("HomeChildFrag", "Loading more at bottom (scroll stopped)")
                        refresh = false
                        pageNo++
                        mHomeModel.getHomeDataList(mCurrentType, pageNo, pageSize)
                    }
                } else {
                    activity.cancelBottomBarAutoShow()
                }
            }
        })
        
        // Disable SmartRefreshLayout load-more completely to avoid jerk
        mViewBinding.smartRefresh.setEnableLoadMore(false)

        mViewBinding.smartRefresh.setOnRefreshListener { refreshLayout ->
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
                refresh = true
                pageNo = 1
                mHomeModel.getHomeDataList(mCurrentType, pageNo, pageSize)
            }
        }
    }

    private var refresh = true
    private var isBannerLoad = false
    private var isLastPage = false
    private var isLoadingMore = false
    
    // Animation timing
    private var refreshStartTime: Long = 0
    private val minimumRefreshDuration = 800L // 800ms minimum duration
    private var isButtonRefresh = false // Flag to prevent double API calls
    
    // Debounce timing to prevent rapid successive refreshes
    private var lastRefreshTime: Long = 0
    private val minimumTimeBetweenRefreshes = 1500L // 1.5 seconds minimum between refreshes

    @SuppressLint("NotifyDataSetChanged")
    private fun initModel() {
        // Reset button refresh flag on initial load to prevent animation on first data load
        isButtonRefresh = false
        mHomeModel.getHomeDataList(mCurrentType, pageNo, pageSize)
        mHomeModel.homeDataList.observe(viewLifecycleOwner) {
            if (it.code == Constants.SUCCESS_CODE) {
                if (refresh) {
                    // Hide Shimmer
                    val shimmerLayout = mViewBinding.root.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmer_layout)
                    shimmerLayout?.stopShimmer()
                    shimmerLayout?.visibility = View.GONE
                    mViewBinding.smartRefresh.visibility = View.VISIBLE
                    
                    // Sync with widgets if it's the Today feed and only for the first page
                    if (shouldShowBanner() && (pageNo == 1)) {
                        // Launch in coroutine to avoid blocking UI thread with MMKV writes/Broadcasts
                        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            // Only send the first 10 items to the widget as requested
                            val widgetItems = it.data.articles.take(10)
                            WidgetDataProvider.updateFromHomeData(requireContext(), widgetItems)
                        }
                    }

                    isBannerLoad = false
                    mNewsList.clear()
                    mBannerList.clear()
                    finishRefreshWithMinimumDuration(true)
                }
                isLastPage = it.data.articles.isEmpty()
                mNewsList.addAll(it.data.articles)
                val lastPosition = mNewsList.size
                if (!isBannerLoad && shouldShowBanner()) {
                    mBannerList.addAll(it.data.headlines)
                    mAdapter.updateBannerData(mBannerList)
                    isBannerLoad = true
                }
                
                // Reset refresh flag after processing data to enable load-more
                val wasRefresh = refresh
                refresh = false
                if (wasRefresh){
                    mAdapter.notifyDataSetChanged()
                    
                    // Trigger wave animation after data is refreshed
                    if (isButtonRefresh && isResumed && isVisible) {
                        mViewBinding.homeRecycler.post {
                            triggerWaveAnimation()
                        }
                    }
                    
                    // Show tutorial (TutorialManager handles once-only logic)
                    if (shouldShowBanner() && mNewsList.isNotEmpty()) {
                        mViewBinding.homeRecycler.post {
                            showTutorialIfNeeded()
                        }
                    }
                }else{
                    // Calculate insert index - account for banner if visible
                    val bannerOffset = if (shouldShowBanner()) 1 else 0
                    val startInsertIndex = (mNewsList.size - it.data.articles.size) + bannerOffset
                    mAdapter.notifyItemRangeInserted(startInsertIndex, it.data.articles.size)
                    
                    mViewBinding.smartRefresh.finishLoadMore()
                }

            } else {
                if (refresh) {
                    // Hide Shimmer on error too
                    val shimmerLayout = mViewBinding.root.findViewById<com.facebook.shimmer.ShimmerFrameLayout>(R.id.shimmer_layout)
                    shimmerLayout?.stopShimmer()
                    shimmerLayout?.visibility = View.GONE
                    mViewBinding.smartRefresh.visibility = View.VISIBLE
                    
                    finishRefreshWithMinimumDuration(false)
                } else {
                    mViewBinding.smartRefresh.finishLoadMore(false)
                }
                ToastUtils.showShortToast(mContext!!, it.msg)
            }

        }
    }


    override fun onStop() {
        super.onStop()
        // Banner loop handled by adapter logic
    }

    private fun shouldShowBanner(): Boolean {
        // Update mCurrentType to current localized string if it matches the "today" type
        // This handles language changes where the stored type might be in a different language
        val currentTodayString = getString(R.string.today)
        if (mCurrentType != currentTodayString) {
            // Check if this fragment was originally the "today" tab by comparing with stored type
            // This is a fallback for when language changes but the stored type is in old language
            val wasTodayTab = mCurrentType == getString(R.string.today) || 
                             mCurrentType == "Today" || 
                             mCurrentType == "今日" || 
                             mCurrentType == "今天"
            if (wasTodayTab) {
                mCurrentType = currentTodayString
            }
        }
        return mCurrentType == currentTodayString
    }

    /**
     * Refresh the fragment data
     */
    fun refreshData() {
        android.util.Log.d("HomeChildFrag", "Refreshing HomeChildFrag data for type: $mCurrentType")
        
        // Check if fragment is properly attached before accessing ViewModels
        if (!isAdded || isDetached || activity == null) {
            android.util.Log.w("HomeChildFrag", "Fragment not properly attached, skipping refresh")
            return
        }
        
        // Update mCurrentType from arguments in case it was updated due to language change
        val newType = arguments?.getString("type")
        if (newType != null && newType != mCurrentType) {
            mCurrentType = newType
            android.util.Log.d("HomeChildFrag", "Updated mCurrentType to: $mCurrentType")
        }

        // Show banner only on Today tab
        val showBanner = shouldShowBanner()
        mAdapter.setBannerVisible(showBanner)
        mAdapter.setIsTodayTab(showBanner)
        
        // Check debounce timing to prevent rapid successive refreshes
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRefresh = currentTime - lastRefreshTime
        if (timeSinceLastRefresh < minimumTimeBetweenRefreshes) {
            android.util.Log.d("HomeChildFrag", "Refresh blocked - too soon since last refresh (${timeSinceLastRefresh}ms < ${minimumTimeBetweenRefreshes}ms)")
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
        
        refresh = true
        pageNo = 1
        mHomeModel.getHomeDataList(mCurrentType, pageNo, pageSize)
    }
    
    /**
     * Trigger wave animation for news cards when reloaded via bottom bar
     * Each card animates down 20dp over 180ms (smooth easeOut), then back to 0px over 320ms (smooth easeInOut)
     * Stagger delay: cardIndex * 40ms for tighter wave effect
     */
    private fun triggerWaveAnimation() {
        val recyclerView = mViewBinding.homeRecycler
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
        }
    }

    /**
     * Share article functionality
     */
    private fun shareArticle(article: HomeDataListEntry.DataDTO.ArticlesDTO) {
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

    /**
     * Show welcome poster and tutorial if they haven't been shown yet
     */
    private fun showTutorialIfNeeded() {
        val context = mContext ?: return
        
        // ONLY show tutorial if fragment is actually visible and resumed
        // This prevents tutorials for background-preloaded fragments from appearing
        if (!isResumed || !isVisible) return
        
        var accountId = com.searcher.zonenews.utils.SharedPreferenceUtils.getString(context, "current_account_id")
        
        // If accountId is empty, wait briefly for profile data to load (defensive check)
        // This handles edge cases where fragment loads before account ID is set
        if (accountId.isEmpty()) {
            mViewBinding.homeRecycler.postDelayed({
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
                com.searcher.zonenews.utils.TutorialManager.TUTORIAL_HOME,
                accountId
            )) {
            return
        }
        
        // Need at least one item in the list
        if (mNewsList.isEmpty()) return
        
        // Create tutorial steps
        val steps = listOf(
            com.searcher.zonenews.selfview.TutorialOverlayView.TutorialStep(
                id = "home_welcome",
                message = getString(R.string.tutorial_home_step1_message),
                hasHighlight = false
            ),
            com.searcher.zonenews.selfview.TutorialOverlayView.TutorialStep(
                id = "home_news_row",
                message = getString(R.string.tutorial_home_step1),
                hasHighlight = true
            ),
            com.searcher.zonenews.selfview.TutorialOverlayView.TutorialStep(
                id = "home_sentiment",
                message = getString(R.string.tutorial_detail_sentiment_meter),
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
                com.searcher.zonenews.utils.TutorialManager.TUTORIAL_HOME,
                accountId
            )
            // Remove overlay from decorView when done
            decorView.removeView(overlay)
        }
        
        decorView.addView(overlay)
        
        // Get target view function
        val getTargetView: (com.searcher.zonenews.selfview.TutorialOverlayView.TutorialStep) -> View? = { step ->
            when (step.id) {
                "home_news_row" -> {
                    // Get the first news item (after banner if showing)
                    val firstItemPosition = if (shouldShowBanner()) 1 else 0
                    val layoutManager = mViewBinding.homeRecycler.layoutManager as? LinearLayoutManager
                    layoutManager?.findViewByPosition(firstItemPosition)
                }
                "home_sentiment" -> {
                    // Get the sentiment bar from the first news item
                    val firstItemPosition = if (shouldShowBanner()) 1 else 0
                    val layoutManager = mViewBinding.homeRecycler.layoutManager as? LinearLayoutManager
                    val firstItem = layoutManager?.findViewByPosition(firstItemPosition)
                    firstItem?.findViewById(R.id.progress_track)
                }
                else -> null
            }
        }
        
        // Set up click listener that advances with proper callback
        overlay.setOnClickListener {
            overlay.advanceWithCallback(null, getTargetView)
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
            mViewBinding.homeRecycler.post {
                showTutorialIfNeeded()
            }
        }
        welcomeFragment.show(parentFragmentManager, "WelcomePoster")
    }

}

