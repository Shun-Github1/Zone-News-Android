package com.anssy.znewspro.ui.mainfrag.homechild

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
import com.anssy.znewspro.utils.Constants
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseFragment
import com.anssy.znewspro.databinding.FragChildHomeBinding
import com.anssy.znewspro.entry.HomeDataListEntry
import com.anssy.znewspro.model.HomeModel
import com.anssy.znewspro.selfview.NewNestedScrollView
import com.anssy.znewspro.ui.MainActivity
import com.anssy.znewspro.ui.newsdetail.NewsDetailActivity
import com.anssy.znewspro.utils.CalculateUtil
import com.anssy.znewspro.utils.ToastUtils
import com.anssy.znewspro.utils.Utils
import com.bumptech.glide.Glide
import com.scwang.smartrefresh.layout.api.RefreshLayout
import com.scwang.smartrefresh.layout.listener.OnRefreshLoadMoreListener
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import com.zhy.adapter.recyclerview.CommonAdapter
import com.zhy.adapter.recyclerview.base.ViewHolder
import java.text.SimpleDateFormat
import java.util.Locale


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
        if (mViewBinding.homeBanner.visibility == View.VISIBLE) {
            mViewBinding.homeBanner.startLoop()
        }
        // Reset button refresh flag when fragment becomes visible
        // This prevents wave animation from triggering when switching tabs
        isButtonRefresh = false
    }

    private lateinit var mBannerAdapter: HomeAdapter
    private var pageNo = 1
    private val pageSize = 10
    private var mCurrentType = ""
    override fun initData() {
        mCurrentType = arguments?.getString(TYPE_NEWS).toString()
        initView()
        initModel()
    }

    private fun initView() {
        mBannerAdapter = HomeAdapter()
        mViewBinding.homeBanner.setAdapter(mBannerAdapter)
        // Show banner only on Today tab
        val showBanner = shouldShowBanner()
        mViewBinding.homeBanner.visibility = if (showBanner) View.VISIBLE else View.GONE
        mViewBinding.homeBanner.setOnPageClickListener {
            val headlinesDTO = mBannerList.get(it)
            val intent = Intent(mContext, NewsDetailActivity::class.java)
            intent.putExtra("id", headlinesDTO.articleID)
            intent.putExtra("source_fragment", "home")
            startActivity(intent)
        }
        if (showBanner) {
            mViewBinding.homeBanner.create()
        }

        mViewBinding.homeRecycler.layoutManager = LinearLayoutManager(mContext, RecyclerView.VERTICAL, false)
        
        // Disable NestedScrollView scrolling to let RecyclerView handle it
        mViewBinding.scrollView.isNestedScrollingEnabled = false
        
        mAdapter = NewsAdapter(mContext!!, mNewsList) { article ->
            shareArticle(article)
        }
        mViewBinding.homeRecycler.adapter = mAdapter
        
        // Add RecyclerView scroll listener
        android.util.Log.d("HomeChildFrag", "Adding RecyclerView scroll listener")
        mViewBinding.homeRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                android.util.Log.d("HomeChildFrag", "RecyclerView onScrolled: dx=$dx, dy=$dy")
                val activity = requireActivity() as MainActivity
                if (dy > 0) {
                    android.util.Log.d("HomeChildFrag", "RecyclerView scrolling down - hiding bottom bar")
                    activity.hideBottomBar()
                } else if (dy < 0) {
                    android.util.Log.d("HomeChildFrag", "RecyclerView scrolling up - showing bottom bar")
                    activity.showBottomBar()
                }
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                android.util.Log.d("HomeChildFrag", "RecyclerView onScrollStateChanged: $newState")
                val activity = requireActivity() as MainActivity
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    android.util.Log.d("HomeChildFrag", "RecyclerView IDLE - scheduling auto-show")
                    activity.scheduleBottomBarAutoShow()
                } else {
                    android.util.Log.d("HomeChildFrag", "RecyclerView not IDLE - canceling auto-show")
                    activity.cancelBottomBarAutoShow()
                }
            }
        })
        
        // Add NestedScrollView scroll listener as backup
        android.util.Log.d("HomeChildFrag", "Adding NestedScrollView scroll listener")
        mViewBinding.scrollView.addScrollChangeListener(object :
            NewNestedScrollView.AddScrollChangeListener {
            override fun onScrollChange(
                scrollX: Int,
                scrollY: Int,
                oldScrollX: Int,
                oldScrollY: Int
            ) {
                android.util.Log.d("HomeChildFrag", "NestedScrollView onScrollChange: scrollY=$scrollY, oldScrollY=$oldScrollY, isLoadingMore=$isLoadingMore")
                val activity = requireActivity() as MainActivity
                
                // Detect suspicious scroll changes that might be load-more rebound
                // The rebound typically shows scrollY < oldScrollY with a large difference
                val scrollDifference = oldScrollY - scrollY
                val isSuspiciousRebound = scrollDifference > 100 && scrollY < oldScrollY
                
                // Only handle scroll direction changes if we're not loading more and not a suspicious rebound
                if (!isLoadingMore && !isSuspiciousRebound) {
                    if (scrollY > oldScrollY) {
                        android.util.Log.d("HomeChildFrag", "NestedScrollView scrolling down - hiding bottom bar")
                        activity.hideBottomBar()
                    } else if (scrollY < oldScrollY) {
                        android.util.Log.d("HomeChildFrag", "NestedScrollView scrolling up - showing bottom bar")
                        activity.showBottomBar()
                    }
                } else {
                    if (isSuspiciousRebound) {
                        android.util.Log.d("HomeChildFrag", "NestedScrollView suspicious rebound detected (diff=$scrollDifference) - ignoring scroll change")
                    } else {
                        android.util.Log.d("HomeChildFrag", "NestedScrollView scroll change during load-more - ignoring")
                    }
                }
            }

            override fun onScrollState(state: NewNestedScrollView.ScrollState?) {
                android.util.Log.d("HomeChildFrag", "NestedScrollView onScrollState: $state")
                val activity = requireActivity() as MainActivity
                when (state) {
                    NewNestedScrollView.ScrollState.IDLE -> {
                        android.util.Log.d("HomeChildFrag", "NestedScrollView IDLE - scheduling auto-show")
                        activity.scheduleBottomBarAutoShow()
                    }
                    NewNestedScrollView.ScrollState.DRAG, NewNestedScrollView.ScrollState.SCROLLING -> {
                        android.util.Log.d("HomeChildFrag", "NestedScrollView DRAG/SCROLLING - canceling auto-show")
                        activity.cancelBottomBarAutoShow()
                    }
                    else -> {}
                }
            }
        })
        mViewBinding.smartRefresh.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                // Only proceed if this is a pull-to-refresh, not a button-triggered refresh
                if (!isButtonRefresh) {
                    // Check debounce timing for pull-to-refresh as well
                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastRefresh = currentTime - lastRefreshTime
                    if (timeSinceLastRefresh < minimumTimeBetweenRefreshes) {
                        android.util.Log.d("HomeChildFrag", "Pull-to-refresh blocked - too soon since last refresh (${timeSinceLastRefresh}ms < ${minimumTimeBetweenRefreshes}ms)")
                        refreshLayout.finishRefresh()
                        return
                    }
                    
                    // Update last refresh time and proceed
                    lastRefreshTime = currentTime
                    refreshStartTime = System.currentTimeMillis()
                    refresh = true
                    pageNo = 1
                    mHomeModel.getHomeDataList(mCurrentType, pageNo, pageSize)
                }
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                if (isLastPage) {
                    refreshLayout.finishLoadMore(true)
                    return
                }
                android.util.Log.d("HomeChildFrag", "Starting load-more - setting isLoadingMore=true")
                isLoadingMore = true
                refresh = false
                pageNo++
                mHomeModel.getHomeDataList(mCurrentType, pageNo, pageSize)
            }

        })
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
                    isBannerLoad = false
                    mNewsList.clear()
                    mBannerList.clear()
                    finishRefreshWithMinimumDuration(true)
                } else {
                    mViewBinding.smartRefresh.finishLoadMore(true)
                    android.util.Log.d("HomeChildFrag", "Load-more completed - setting isLoadingMore=false")
                    isLoadingMore = false
                }
                isLastPage = it.data.articles.isEmpty()
                mNewsList.addAll(it.data.articles)
                val lastPosition = mNewsList.size
                if (!isBannerLoad && shouldShowBanner()) {
                    mBannerList.addAll(it.data.headlines)
                    mViewBinding.homeBanner.refreshData(mBannerList)
                    isBannerLoad = true
                }
                if (refresh){
                    mAdapter.notifyDataSetChanged()
                    // Trigger wave animation after data is refreshed (only for button refresh, not pull-to-refresh or tab switch)
                    // Check if fragment is currently visible to prevent animation when switching tabs
                    if (isButtonRefresh && isResumed && isVisible) {
                        mViewBinding.homeRecycler.post {
                            triggerWaveAnimation()
                        }
                    }
                }else{
                    mAdapter.notifyItemRangeInserted(lastPosition,it.data.articles.size)
                }

            } else {
                if (refresh) {
                    finishRefreshWithMinimumDuration(false)
                } else {
                    mViewBinding.smartRefresh.finishLoadMore(false)
                    android.util.Log.d("HomeChildFrag", "Load-more failed - setting isLoadingMore=false")
                    isLoadingMore = false
                }
                ToastUtils.showShortToast(mContext!!, it.msg)
            }

        }
    }


    override fun onStop() {
        super.onStop()
        if (mViewBinding.homeBanner.visibility == View.VISIBLE) {
            mViewBinding.homeBanner.stopLoop()
        }
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
        mViewBinding.scrollView.post {
            mViewBinding.scrollView.scrollTo(0, 0)
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
            isButtonRefresh = false
        }
    }

    inner class NetViewHolder(itemView: View) :
        BaseViewHolder<HomeDataListEntry.DataDTO.HeadlinesDTO>(itemView) {
        private val mBannerIv: ImageView = itemView.findViewById(R.id.banner_image)
        private val mTitleTv: TextView = itemView.findViewById(R.id.banner_title_tv)
        private val mTransTv: TextView = itemView.findViewById(R.id.banner_desc_tv)
        override fun bindData(
            data: HomeDataListEntry.DataDTO.HeadlinesDTO,
            position: Int,
            pageSize: Int
        ) {
            Glide.with(mContext!!).load(data.pictureURL)
                .centerCrop().error(R.drawable.ic_image_not_supported_24).into(mBannerIv)
            mTitleTv.text = data.title
            mTransTv.text = data.description
            
            // Add long press listener for sharing with shrink animation
            itemView.setOnLongClickListener {
                // Animate shrink effect
                itemView.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(150)
                    .withEndAction {
                        // Restore original size after animation
                        itemView.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(300)
                            .start()
                    }
                    .start()
                
                shareHeadline(data)
                true
            }
        }
    }

    /**
     * Share article functionality
     */
    private fun shareArticle(article: HomeDataListEntry.DataDTO.ArticlesDTO) {
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

    /**
     * Share headline functionality
     */
    private fun shareHeadline(headline: HomeDataListEntry.DataDTO.HeadlinesDTO) {
        val shareText = buildString {
            append(headline.title)
            if (!headline.articleURL.isNullOrEmpty()) {
                append("\n\n")
                append(headline.articleURL)
            }
        }
        
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
        startActivity(Intent.createChooser(shareIntent, getString(R.string.app_name)))
    }

    /**
     * banner 适配器
     */
    inner class HomeAdapter :
        BaseBannerAdapter<HomeDataListEntry.DataDTO.HeadlinesDTO, NetViewHolder>() {
        override fun onBind(
            holder: NetViewHolder,
            data: HomeDataListEntry.DataDTO.HeadlinesDTO,
            position: Int,
            pageSize: Int
        ) {
            holder.bindData(data, position, pageSize)
        }

        override fun createViewHolder(itemView: View, viewType: Int): NetViewHolder {
            return NetViewHolder(itemView)
        }

        override fun getLayoutId(viewType: Int): Int {
            return R.layout.item_banner
        }
    }

}