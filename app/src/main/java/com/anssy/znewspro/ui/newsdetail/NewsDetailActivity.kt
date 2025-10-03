package com.anssy.znewspro.ui.newsdetail

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.GestureDetector
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import android.view.animation.Transformation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.animation.PathInterpolatorCompat
import androidx.core.content.ContextCompat
import android.content.res.ColorStateList
import com.anssy.znewspro.utils.Constants
import com.anssy.znewspro.R
import com.anssy.znewspro.base.BaseActivity
import com.anssy.znewspro.databinding.ActivityNewsDetailBinding
import com.anssy.znewspro.entry.ArticleDetailEntry
import com.anssy.znewspro.model.NewsDetailModel

import com.anssy.znewspro.utils.ToastUtils
import com.anssy.znewspro.utils.HapticFeedbackHelper
import com.bumptech.glide.Glide

import com.google.android.material.card.MaterialCardView
import com.anssy.znewspro.utils.SystemDialogUtils
import com.anssy.znewspro.selfview.popup.PublisherArticlesSortPopupWindow
import com.anssy.znewspro.utils.SwipeGestureHelper
import kotlinx.coroutines.launch


/**
 * @Description 新闻详情
 * @Author yulu
 * @CreateTime 2025年07月01日 16:58:28
 */
class NewsDetailActivity : BaseActivity() {
    private val newsDetailModel:NewsDetailModel by viewModels ()
    private lateinit var mViewBinding: ActivityNewsDetailBinding

    
    // Floating bar control
    private var mBottomView: MaterialCardView? = null
    private var isBottomBarHidden: Boolean = false
    private val autoShowRunnable = Runnable { showBottomBar() }
    
    // Publisher articles sorting
    private var currentSortOption = PublisherArticlesSortPopupWindow.SortOption.PUBLISHER_NAME
    private var isAscending = true
    private var originalArticles: List<ArticleDetailEntry.DataDTO.ArticlesDTO>? = null
    
    // Gesture detection
    private lateinit var gestureDetector: GestureDetector
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewBinding = ActivityNewsDetailBinding.inflate(layoutInflater)
        setContentView(mViewBinding.root)
        applyStatusBarStyle()
        initView()
    }


    private var articleId = ""
    private fun initView() {
        val incomingId = intent.getStringExtra("id")
        articleId = if (!incomingId.isNullOrBlank() && incomingId != getString(R.string.null_string)) incomingId else ""

        mBottomView = mViewBinding.newsDetailBottomCard
		setupToolbar()
        initModel()
        setupFloatingBar()
        setupGestureDetection()
        mViewBinding.feedBackLayout.setOnClickListener {
                showFeedBackWindow()
        }
		mViewBinding.generateContextBtn.setOnClickListener {
			// Placeholder for future context generation
		}
        
        // Setup info button click listeners
        mViewBinding.sentimentInfoBtn.setOnClickListener { 
            showSentimentInfoPopup(it)
        }
        mViewBinding.publisherInfoBtn.setOnClickListener { 
            showPublisherInfoPopup(it)
        }
        
        // Setup publisher articles sort buttons
        findViewById<LinearLayout>(R.id.sort_option_button).setOnClickListener {
            showPublisherArticlesSortPopup(it)
        }
        
        findViewById<LinearLayout>(R.id.sort_direction_button).setOnClickListener {
            // Toggle sort direction
            isAscending = !isAscending
            updateSortIndicator()
            applyCurrentSort()
        }
	}

	private fun setupToolbar() {
		val toolbar = mViewBinding.topAppBar
		toolbar.setNavigationOnClickListener { finish() }
		toolbar.setOnMenuItemClickListener { item: MenuItem ->
			when (item.itemId) {
				R.id.action_share -> {
					if (mArticleDetailEntry == null) return@setOnMenuItemClickListener true
            val link = when {
                !mArticleDetailEntry!!.shareURL.isNullOrEmpty() -> mArticleDetailEntry!!.shareURL
                !mArticleDetailEntry!!.articleURL.isNullOrEmpty() -> mArticleDetailEntry!!.articleURL
                else -> mArticleDetailEntry!!.pictureURL
            }
            shareLink(link)
					addHis()
					true
				}
				R.id.action_save -> {
					if (mArticleDetailEntry == null) return@setOnMenuItemClickListener true
					if (mArticleDetailEntry!!.liked) {
						newsDetailModel.deleteCollect(articleId)
					} else {
						newsDetailModel.collectHis(articleId)
					}
					true
				}
				else -> false
			}
        }
    }

    /**
     * 分享链接
     */

    private fun shareLink(link:String) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = getString(R.string.text_plain)
        shareIntent.putExtra(Intent.EXTRA_TEXT, link)
        startActivity(Intent.createChooser(shareIntent, getString(R.string.app_name)))
    }
    /**
     *获取数据
     */
    private fun initModel(){
        newsDetailModel.queryNewsDetail(articleId)
        newsDetailModel.newsDetailEntry.observe(this) {
            if (it.code == Constants.SUCCESS_CODE) {
                completeView(it.data)
                addHis()
            } else {
                ToastUtils.showShortToast(mContext!!, it.msg)
            }
        }
        newsDetailModel.feeBackResponseEntry.observe(this){
            if (it!=null){
                if (it.code== Constants.SUCCESS_CODE){
                    SystemDialogUtils.showSuccessMessage(this, getString(R.string.success_message))
                }else{
                    if (it.code==1000){
                        SystemDialogUtils.showErrorMessage(this, getString(R.string.server_error_message))
                    }else{
                        SystemDialogUtils.showErrorMessage(this, it.msg)
                    }
                }
            }
        }
        newsDetailModel.addHisEntry.observe(this){
            if (it!=null){
                if (it.code== Constants.SUCCESS_CODE){
                    Log.e(getString(R.string.xxx_log_tag), getString(R.string.add_success_log))
                }else{
                    Log.e(getString(R.string.xxx_log_tag), getString(R.string.add_failed_log))
                }
            }
        }
        newsDetailModel.collectEntry.observe(this){
            if (it!=null){
                if (it.code== Constants.SUCCESS_CODE){
                    ToastUtils.showShortToast(mContext!!,getString(R.string.collect_success_toast))
                    mArticleDetailEntry!!.liked = true
					val saveItem = mViewBinding.topAppBar.menu.findItem(R.id.action_save)
					// Article is now saved - show filled icon
					saveItem.setIcon(R.drawable.ic_bookmark_filled_24)
					saveItem.iconTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brand_primary))
                }else{
                    if (it.code==1000){
                        SystemDialogUtils.showErrorMessage(this, getString(R.string.server_error_message))
                    }else{
                        SystemDialogUtils.showErrorMessage(this, it.msg)
                    }
                }
            }
        }
        newsDetailModel.deleteCollectEntry.observe(this){
            if (it!=null){
                if (it.code== Constants.SUCCESS_CODE){
                    ToastUtils.showShortToast(mContext!!,getString(R.string.uncollect_success_toast))
                    mArticleDetailEntry!!.liked = false
					val saveItem = mViewBinding.topAppBar.menu.findItem(R.id.action_save)
					// Article is now unsaved - show outline icon
					saveItem.setIcon(R.drawable.ic_bookmark_outline_24)
					saveItem.iconTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorTextDeep))
                }else{
                    if (it.code==1000){
                        SystemDialogUtils.showErrorMessage(this, getString(R.string.server_error_message))
                    }else{
                        SystemDialogUtils.showErrorMessage(this, it.msg)
                    }
                }
            }
        }

    }
    private var mArticleDetailEntry: ArticleDetailEntry.DataDTO?=null
    @SuppressLint("NotifyDataSetChanged")
    private fun completeView(articleDetailEntry: ArticleDetailEntry.DataDTO){
        mArticleDetailEntry = articleDetailEntry
        Glide.with(mContext!!).load(articleDetailEntry.pictureURL).error(R.drawable.ease_default_image)
            .into(mViewBinding.newsIv)
        mViewBinding.newsTitleTv.text = articleDetailEntry.title
        
        // Handle new description structure
        articleDetailEntry.description?.let { description ->
            // Debug logging
            android.util.Log.d("NewsDetail", "Description object: ${articleDetailEntry.description}")
            android.util.Log.d("NewsDetail", "Synopsis: '${description.synopsis}'")
            android.util.Log.d("NewsDetail", "Implications: '${description.implications}'")
            
            // Handle synopsis section
            if (!description.synopsis.isNullOrEmpty()) {
                mViewBinding.newsSynopsisTv.text = description.synopsis
                mViewBinding.synopsisContainer.visibility = View.VISIBLE
                android.util.Log.d("NewsDetail", "Synopsis displayed")
            } else {
                mViewBinding.synopsisContainer.visibility = View.GONE
                android.util.Log.d("NewsDetail", "Synopsis hidden - empty or null")
            }
            
            // Handle implications section
            if (!description.implications.isNullOrEmpty()) {
                mViewBinding.newsImplicationsTv.text = description.implications
                mViewBinding.implicationsContainer.visibility = View.VISIBLE
                android.util.Log.d("NewsDetail", "Implications displayed")
            } else {
                mViewBinding.implicationsContainer.visibility = View.GONE
                android.util.Log.d("NewsDetail", "Implications hidden - empty or null")
            }
        } ?: run {
            // Fallback for null description
            android.util.Log.d("NewsDetail", "Description is null")
            mViewBinding.synopsisContainer.visibility = View.GONE
            mViewBinding.implicationsContainer.visibility = View.GONE
        }
		// Update toolbar save icon state
		val saveItem = mViewBinding.topAppBar.menu.findItem(R.id.action_save)
		if (articleDetailEntry.liked){
			// Article is saved - show filled icon
			saveItem.setIcon(R.drawable.ic_bookmark_filled_24)
			saveItem.iconTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brand_primary))
		}else{
			// Article is not saved - show outline icon
			saveItem.setIcon(R.drawable.ic_bookmark_outline_24)
			saveItem.iconTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorTextDeep))
		}
		// Sentiment meter
		mViewBinding.sentimentMeter.setSentiment(articleDetailEntry.metrics.sentiment)
		// Subjectivity score
		findViewById<SubjectivityScoreView>(R.id.subjectivity_score).setSubjectivity(articleDetailEntry.metrics.subjectivity)
		// Publisher distribution
		mViewBinding.publisherDistribution.setData(
			PublisherDistributionView.Data(
				centricPercent = articleDetailEntry.coverage.percentage.centric,
				centricIcons = articleDetailEntry.coverage.icons.centric.map { icon ->
					PublisherDistributionView.Icon(
						size = icon.size,
						rx = icon.rx,
						ry = icon.ry,
						logo = icon.logo
					)
				},
				progressiveIcons = articleDetailEntry.coverage.icons.progressive.map { icon ->
					PublisherDistributionView.Icon(
						size = icon.size,
						rx = icon.rx,
						ry = icon.ry,
						logo = icon.logo
					)
				}
			)
		)

        setupPublisherArticles(articleDetailEntry)
	}

    private fun setupPublisherArticles(data: ArticleDetailEntry.DataDTO) {
        val listRv = findViewById<RecyclerView>(R.id.publisher_articles_list)
        listRv.layoutManager = object : LinearLayoutManager(this, RecyclerView.VERTICAL, false) {
            override fun canScrollVertically(): Boolean = false
        }
        // Store original articles for sorting
        originalArticles = data.articles
        // Apply initial sorting
        val items = applyInitialSort(data.articles)
        // Update sort indicator to show initial state
        updateSortIndicator()
        listRv.adapter = object : com.zhy.adapter.recyclerview.CommonAdapter<ArticleDetailEntry.DataDTO.ArticlesDTO>(
            this, R.layout.item_publisher_article, items
        ) {
            override fun convert(holder: com.zhy.adapter.recyclerview.base.ViewHolder, t: ArticleDetailEntry.DataDTO.ArticlesDTO, position: Int) {
                val iconIv = holder.getView<ImageView>(R.id.article_publisher_icon)
                val nameTv = holder.getView<TextView>(R.id.article_publisher_name)
                val biasTv = holder.getView<TextView>(R.id.article_publisher_bias)
                val titleTv = holder.getView<TextView>(R.id.article_title)
                
                Glide.with(this@NewsDetailActivity).load(t.publisherIcon).error(R.drawable.ease_default_image).into(iconIv)
                nameTv.text = if (t.publisherName.isNullOrEmpty()) getString(R.string.about) else t.publisherName
                
                // Setup publisher bias tag
                if (t.publisherStance != null && !t.publisherStance.tag.isNullOrEmpty()) {
                    biasTv.visibility = View.VISIBLE
                    biasTv.text = getPublisherBiasText(t.publisherStance.tag)
                    biasTv.setTextColor(getPublisherBiasTextColor(t.publisherStance.tag))
                    biasTv.setBackgroundResource(getPublisherBiasBackground(t.publisherStance.tag))
                } else {
                    biasTv.visibility = View.GONE
                }
                
                val title = t.title
                titleTv.text = when {
                    !title.isNullOrEmpty() -> title
                    !t.description.isNullOrEmpty() -> t.description
                    else -> getString(R.string.details)
                }
                holder.convertView.setOnClickListener {
                    val linkRaw = when {
                        !t.articleURL.isNullOrEmpty() -> t.articleURL
                        !mArticleDetailEntry?.articleURL.isNullOrEmpty() -> mArticleDetailEntry?.articleURL
                        !mArticleDetailEntry?.shareURL.isNullOrEmpty() -> mArticleDetailEntry?.shareURL
                        else -> ""
                    }
                    val link = ensureHttpUrl(linkRaw ?: "")
                    if (link.isNotEmpty()) {
                        val intent = Intent(this@NewsDetailActivity, com.anssy.znewspro.ui.web.WebActivity::class.java)
                        intent.putExtra(getString(R.string.url_key), link)
                        intent.putExtra(getString(R.string.type_key), getString(R.string.news_type))
                        intent.putExtra(getString(R.string.publisher_icon_key), t.publisherIcon)
                        intent.putExtra(getString(R.string.publisher_name_key), t.publisherName)
                        startActivity(intent)
                    } else {
                        ToastUtils.showShortToast(this@NewsDetailActivity, getString(R.string.open_in_browser))
                    }
                }
            }
        }
        listRv.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: android.graphics.Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                super.getItemOffsets(outRect, view, parent, state)
                val pos = parent.getChildAdapterPosition(view)
                // Add top divider for all but the first, left aligned with text by adding left margin of 52dp (40 icon + 12 spacing)
                if (pos > 0) {
                    outRect.top = 1
                }
            }

            override fun onDraw(c: android.graphics.Canvas, parent: RecyclerView, state: RecyclerView.State) {
                val paint = android.graphics.Paint().apply { color = getColor(R.color.line_color); strokeWidth = resources.displayMetrics.density }
                val left = parent.paddingLeft + (resources.displayMetrics.density * (40 + 12)).toInt()
                val right = parent.width - parent.paddingRight
                for (i in 0 until parent.childCount) {
                    val child = parent.getChildAt(i)
                    val pos = parent.getChildAdapterPosition(child)
                    if (pos > 0) {
                        val y = child.top.toFloat()
                        c.drawLine(left.toFloat(), y, right.toFloat(), y, paint)
                    }
                }
            }
        })
    }

    private fun ensureHttpUrl(url: String): String {
        if (url.isEmpty()) return url
        val trimmed = url.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "${getString(R.string.http_prefix)}$trimmed"
    }
    
    /**
     * Get the localized text for publisher bias using tag-based system
     */
    private fun getPublisherBiasText(stanceTag: String): String {
        return when (stanceTag.trim().lowercase()) {
            "c" -> getString(R.string.conservative)
            "p" -> getString(R.string.liberal)
            // Legacy support for full names
            "conservative", "centric" -> getString(R.string.conservative)
            "progressive" -> getString(R.string.liberal)
            else -> stanceTag
        }
    }
    
    /**
     * Get the text color for publisher bias tag
     */
    private fun getPublisherBiasTextColor(stanceTag: String): Int {
        return when (stanceTag.trim().lowercase()) {
            "c" -> getColor(R.color.publisher_bias_conservative_text)
            "p" -> getColor(R.color.publisher_bias_progressive_text)
            // Legacy support for full names
            "conservative", "centric" -> getColor(R.color.publisher_bias_conservative_text)
            "progressive" -> getColor(R.color.publisher_bias_progressive_text)
            else -> getColor(R.color.colorTextMiddle)
        }
    }
    
    /**
     * Get the background resource for publisher bias tag
     */
    private fun getPublisherBiasBackground(stanceTag: String): Int {
        return when (stanceTag.trim().lowercase()) {
            "c" -> R.drawable.publisher_bias_tag_background
            "p" -> R.drawable.publisher_bias_tag_progressive_background
            // Legacy support for full names
            "conservative", "centric" -> R.drawable.publisher_bias_tag_background
            "progressive" -> R.drawable.publisher_bias_tag_progressive_background
            else -> R.drawable.publisher_bias_tag_background
        }
    }
    
    /**
     * Setup floating bar functionality
     */
    private fun setupFloatingBar() {
        // Setup radio button click listeners
        mViewBinding.newsDetailRg.setOnCheckedChangeListener { _, checkedId ->
            // Provide haptic feedback for bottom bar navigation
            HapticFeedbackHelper.performNavigationHaptic(mViewBinding.newsDetailRg)
            
            when (checkedId) {
                R.id.news_detail_home_rb -> {
                    // Navigate to home
                    finish()
                }
                R.id.news_detail_special_rb -> {
                    // Navigate to person recommend
                    val intent = Intent(this, com.anssy.znewspro.ui.MainActivity::class.java)
                    intent.putExtra("fragment", "special")
                    startActivity(intent)
                    finish()
                }
                R.id.news_detail_my_rb -> {
                    // Navigate to profile
                    val intent = Intent(this, com.anssy.znewspro.ui.MainActivity::class.java)
                    intent.putExtra("fragment", "my")
                    startActivity(intent)
                    finish()
                }
                R.id.news_detail_search_rb -> {
                    // Navigate to search
                    val intent = Intent(this, com.anssy.znewspro.ui.MainActivity::class.java)
                    intent.putExtra("fragment", "search")
                    startActivity(intent)
                    finish()
                }
            }
        }
        
        // Setup scroll listener for the NestedScrollView
        mViewBinding.newsDetailScrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            if (scrollY > oldScrollY) {
                // Scrolling down - hide the bar
                hideBottomBar()
            } else if (scrollY < oldScrollY) {
                // Scrolling up - show the bar
                showBottomBar()
            }
            
            // Check if user has reached the bottom of the page
            val scrollView = mViewBinding.newsDetailScrollView
            val childView = scrollView.getChildAt(0)
            if (childView != null) {
                val scrollViewHeight = scrollView.height
                val childHeight = childView.height
                val maxScrollY = childHeight - scrollViewHeight
                
                // If we're at or near the bottom (within 50dp), show the bar
                if (scrollY >= maxScrollY - 50) {
                    showBottomBar()
                }
            }
        }
        
        // Touch listener will be set up in setupSwipeGesture() to avoid conflicts
        
        // Schedule initial auto-show of bottom bar
        scheduleBottomBarAutoShow()
    }
    
    /**
     * Hide the floating bottom bar
     */
    fun hideBottomBar() {
        val bar = mBottomView ?: return
        if (isBottomBarHidden) return
        bar.clearAnimation()
        cancelBottomBarAutoShow()
        val params = bar.layoutParams
        val bottomMargin = if (params is ViewGroup.MarginLayoutParams) params.bottomMargin else 0
        val translationDistance = (bar.height + bottomMargin).toFloat()
        bar.animate().cancel()
        bar.animate()
            .translationY(translationDistance)
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                // Schedule auto-show after animation completes
                scheduleBottomBarAutoShow()
            }
            .start()
        isBottomBarHidden = true
    }
    
    /**
     * Show the floating bottom bar
     */
    fun showBottomBar() {
        val bar = mBottomView ?: return
        if (!isBottomBarHidden) return
        bar.clearAnimation()
        bar.animate().cancel()
        bar.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(200)
            .start()
        isBottomBarHidden = false
    }
    
    /**
     * Schedule auto-show of bottom bar
     */
    fun scheduleBottomBarAutoShow(delayMs: Long = 2500) {
        val bar = mBottomView ?: return
        bar.removeCallbacks(autoShowRunnable)
        bar.postDelayed(autoShowRunnable, delayMs)
    }
    
    /**
     * Cancel auto-show of bottom bar
     */
    fun cancelBottomBarAutoShow() {
        mBottomView?.removeCallbacks(autoShowRunnable)
    }

    /**
     * 反馈弹窗
     */
    private fun showFeedBackWindow(){
        val feedbackBottomSheet = FeedbackBottomSheetFragment.newInstance { feedbackContent ->
            // Async submission like iOS Task block
            try {
                addFeedBack(feedbackContent)
                true // Return success
            } catch (e: Exception) {
                false // Return failure
            }
        }
        feedbackBottomSheet.show(supportFragmentManager, getString(R.string.feedback_bottom_sheet))
    }


    /**
     * 添加反馈
     */
    private fun addFeedBack(content:String){
        newsDetailModel.addFeedBack(articleId,content)
    }

    /**
     * 添加历史
     */
    private fun addHis(){
        newsDetailModel.addNewsHis(articleId)
    }

   private val easeInOutQuart: Interpolator = PathInterpolatorCompat.create(0.77f, 0f, 0.175f, 1f)

    /**
     * 展开动画
     */
    private fun expand(view: View, which: Int) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(
                (view.parent as View).width,
                View.MeasureSpec.EXACTLY
            ), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val targetHeight = view.measuredHeight
        view.visibility = View.VISIBLE
		// Legacy expand arrows removed in new design
        val animation: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                view.layoutParams.height = if (interpolatedTime == 1f)
                    ViewGroup.LayoutParams.WRAP_CONTENT

                else (targetHeight * interpolatedTime).toInt()
                view.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
        animation.interpolator = this.easeInOutQuart
        animation.duration = computeDurationFromHeight(view).toLong()
        view.startAnimation(animation)
    }

    private fun collapse(view: View, which: Int) {
        val initialHeight = view.measuredHeight
        val a: Animation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation?) {
                if (interpolatedTime == 1.0f) {
					// Legacy collapse arrows removed in new design
                    view.visibility = View.GONE
                    return
                }
                val layoutParams = view.layoutParams
                layoutParams.height =
                    initialHeight - (((initialHeight.toFloat()) * interpolatedTime).toInt())
                view.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
        a.interpolator = this.easeInOutQuart
        a.duration = computeDurationFromHeight(view).toLong()
        view.startAnimation(a)
    }

    private fun computeDurationFromHeight(view: View): Int {
        return ((view.measuredHeight.toFloat()) / view.context.resources.displayMetrics.density).toInt()
    }

	// Legacy lists removed

    /**
     * Show sentiment analysis info popup
     */
    private fun showSentimentInfoPopup(anchor: View) {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f).toInt(), dp(12f).toInt(), dp(16f).toInt(), dp(12f).toInt())
            val fullText = getString(R.string.sentiment_analysis_description)
            val linkText = getString(R.string.our_webpage)
            val spannable = android.text.SpannableString(fullText)
            val start = fullText.indexOf(linkText)
            if (start >= 0) {
                val end = start + linkText.length
                spannable.setSpan(object : android.text.style.ClickableSpan() {
                    override fun onClick(widget: View) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(getString(R.string.example_website)))
                            startActivity(intent)
                        } catch (_: Exception) {}
                    }
                }, start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val linkColor = androidx.core.content.ContextCompat.getColor(this@NewsDetailActivity, R.color.link_color)
                spannable.setSpan(android.text.style.ForegroundColorSpan(linkColor), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            val tv = TextView(this@NewsDetailActivity).apply {
                text = spannable
                setTextColor(androidx.core.content.ContextCompat.getColor(this@NewsDetailActivity, R.color.colorTextSmall))
                textSize = 14f
                movementMethod = android.text.method.LinkMovementMethod.getInstance()
            }
            addView(tv)
        }
        showInfoPopup(anchor, content)
    }

    /**
     * Show publisher distribution info popup
     */
    private fun showPublisherInfoPopup(anchor: View) {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f).toInt(), dp(12f).toInt(), dp(16f).toInt(), dp(12f).toInt())
            val fullText = getString(R.string.publisher_distribution_description)
            val linkText = getString(R.string.our_webpage)
            val spannable = android.text.SpannableString(fullText)
            val start = fullText.indexOf(linkText)
            if (start >= 0) {
                val end = start + linkText.length
                spannable.setSpan(object : android.text.style.ClickableSpan() {
                    override fun onClick(widget: View) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(getString(R.string.example_website)))
                            startActivity(intent)
                        } catch (_: Exception) {}
                    }
                }, start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val linkColor = androidx.core.content.ContextCompat.getColor(this@NewsDetailActivity, R.color.link_color)
                spannable.setSpan(android.text.style.ForegroundColorSpan(linkColor), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            val tv = TextView(this@NewsDetailActivity).apply {
                text = spannable
                setTextColor(androidx.core.content.ContextCompat.getColor(this@NewsDetailActivity, R.color.colorTextSmall))
                textSize = 14f
                movementMethod = android.text.method.LinkMovementMethod.getInstance()
            }
            addView(tv)
        }
        showInfoPopup(anchor, content)
    }

    /**
     * Common method to show info popup
     */
    private fun showInfoPopup(anchor: View, content: LinearLayout) {
        // Use the same width calculation as the original subjectivity score popup
        // The info button is inside a horizontal LinearLayout, which is inside a vertical LinearLayout with padding
        // We need to get the width of the vertical LinearLayout (the card content container) to match the original popup width
        val horizontalContainer = anchor.parent as? LinearLayout
        val verticalContainer = horizontalContainer?.parent as? LinearLayout
        val popupWidth = if (verticalContainer != null && verticalContainer.width > 0) verticalContainer.width else ViewGroup.LayoutParams.MATCH_PARENT
        val popup = android.widget.PopupWindow(content, popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popup.isOutsideTouchable = true
        popup.isFocusable = true
        popup.elevation = dp(4f)
        popup.setBackgroundDrawable(android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(12f)
            setColor(androidx.core.content.ContextCompat.getColor(this@NewsDetailActivity, R.color.profile_card_bg))
        })
        // Align popup's left edge with the card's left edge (same logic as original)
        val xoff = -(anchor.left + dp(16f).toInt())
        // Align the BOTTOM of the popup with the icon's UPPER edge
        content.measure(View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED)
        val popupHeight = content.measuredHeight
        val yoff = -(popupHeight + anchor.height + dp(6f).toInt())
        popup.showAsDropDown(anchor, xoff, yoff)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
    
    /**
     * Setup gesture detection for back navigation and scroll stop detection
     */
    private fun setupGestureDetection() {
        // Initialize gesture detector with proper thresholds
        val viewConfiguration = ViewConfiguration.get(this)
        val touchSlop = viewConfiguration.scaledTouchSlop
        val minimumVelocity = viewConfiguration.scaledMinimumFlingVelocity
        val maximumVelocity = viewConfiguration.scaledMaximumFlingVelocity
        
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                val absDiffX = Math.abs(diffX)
                val absDiffY = Math.abs(diffY)
                val absVelocityX = Math.abs(velocityX)
                val absVelocityY = Math.abs(velocityY)
                
                // Only trigger swipe if:
                // 1. Horizontal movement is significantly greater than vertical (3:1 ratio)
                // 2. Horizontal velocity is greater than vertical velocity (2:1 ratio)
                // 3. Horizontal movement exceeds touch slop threshold
                // 4. Velocity is within reasonable bounds (not too slow, not too fast)
                // 5. It's a right swipe (positive X direction)
                if (absDiffX > absDiffY * 3 && // Horizontal movement is 3x vertical
                    absVelocityX > absVelocityY * 2 && // Horizontal velocity is 2x vertical
                    absDiffX > touchSlop * 2 && // Movement exceeds 2x touch slop
                    absVelocityX > minimumVelocity && // Not too slow
                    absVelocityX < maximumVelocity && // Not too fast
                    diffX > 0) { // Right swipe
                    
                    finish()
                    return true
                }
                return false
            }
            
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                // This is called during scrolling - we don't need to do anything special here
                // The scroll view will handle the actual scrolling
                return false
            }
        })
        
        // Set up touch listener that combines gesture detection with scroll stop detection
        mViewBinding.newsDetailScrollView.setOnTouchListener { _, event ->
            // Let gesture detector handle the touch event first
            val handled = gestureDetector.onTouchEvent(event)
            
            // Handle scroll stop detection
            when (event.action) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isBottomBarHidden) {
                        scheduleBottomBarAutoShow()
                    }
                }
            }
            
            // Don't consume the event unless it was a valid swipe
            handled
        }
    }
    
    private fun showPublisherArticlesSortPopup(anchor: View) {
        val popup = PublisherArticlesSortPopupWindow(this, object : PublisherArticlesSortPopupWindow.Callback {
            override fun onSortSelected(option: PublisherArticlesSortPopupWindow.SortOption) {
                currentSortOption = option
                updateSortIndicator()
                applyCurrentSort()
            }
        })
        popup.setCurrentSort(currentSortOption, isAscending)
        popup.showPopupWindow(anchor)
    }
    
    private fun updateSortIndicator() {
        val sortOptionText = findViewById<TextView>(R.id.sort_option_text)
        val sortOptionIcon = findViewById<ImageView>(R.id.sort_option_icon)
        val sortIndicatorArrowUp = findViewById<ImageView>(R.id.sort_indicator_arrow_up)
        val sortIndicatorArrowDown = findViewById<ImageView>(R.id.sort_indicator_arrow_down)
        
        val (sortText, sortIcon) = when (currentSortOption) {
            PublisherArticlesSortPopupWindow.SortOption.PUBLISHER_NAME -> 
                Pair(getString(R.string.publisher_name), R.drawable.ic_building_2_24)
            PublisherArticlesSortPopupWindow.SortOption.MEDIA_SIGNIFICANCE -> 
                Pair(getString(R.string.media_significance), R.drawable.ic_media_significance_24)
            PublisherArticlesSortPopupWindow.SortOption.PUBLISHER_BIAS -> 
                Pair(getString(R.string.publisher_bias), R.drawable.ic_publisher_bias_24)
            PublisherArticlesSortPopupWindow.SortOption.PUBLISHER_REGION -> 
                Pair(getString(R.string.publisher_region), R.drawable.ic_publisher_region_24)
            PublisherArticlesSortPopupWindow.SortOption.ARTICLE_TITLE -> 
                Pair(getString(R.string.article_title), R.drawable.ic_article_title_24)
        }
        
        sortOptionText.text = sortText
        sortOptionIcon.setImageResource(sortIcon)
        
        // Update arrow colors based on sort order
        // When ascending: up arrow is primary color, down arrow is secondary
        // When descending: up arrow is secondary color, down arrow is primary
        val primaryColor = ContextCompat.getColor(this, R.color.colorTextDeep)
        val secondaryColor = ContextCompat.getColor(this, R.color.colorTextSmall)
        
        if (isAscending) {
            sortIndicatorArrowUp.setColorFilter(primaryColor)
            sortIndicatorArrowDown.setColorFilter(secondaryColor)
        } else {
            sortIndicatorArrowUp.setColorFilter(secondaryColor)
            sortIndicatorArrowDown.setColorFilter(primaryColor)
        }
    }
    
    private fun applyCurrentSort() {
        val listRv = findViewById<RecyclerView>(R.id.publisher_articles_list)
        
        if (originalArticles != null) {
            val sortedArticles = when (currentSortOption) {
                PublisherArticlesSortPopupWindow.SortOption.PUBLISHER_NAME -> {
                    originalArticles!!.sortedWith(compareBy<ArticleDetailEntry.DataDTO.ArticlesDTO> { 
                        it.publisherName ?: "" 
                    }.let { if (isAscending) it else it.reversed() })
                }
                PublisherArticlesSortPopupWindow.SortOption.MEDIA_SIGNIFICANCE -> {
                    originalArticles!!.sortedWith(compareBy<ArticleDetailEntry.DataDTO.ArticlesDTO> { 
                        it.mediaSignificance ?: 0 
                    }.let { if (isAscending) it else it.reversed() })
                }
                PublisherArticlesSortPopupWindow.SortOption.PUBLISHER_BIAS -> {
                    originalArticles!!.sortedWith(compareBy<ArticleDetailEntry.DataDTO.ArticlesDTO> { 
                        it.publisherStance?.tag ?: "" 
                    }.let { if (isAscending) it else it.reversed() })
                }
                PublisherArticlesSortPopupWindow.SortOption.PUBLISHER_REGION -> {
                    originalArticles!!.sortedWith(compareBy<ArticleDetailEntry.DataDTO.ArticlesDTO> { 
                        it.publisherRegion ?: "" 
                    }.let { if (isAscending) it else it.reversed() })
                }
                PublisherArticlesSortPopupWindow.SortOption.ARTICLE_TITLE -> {
                    originalArticles!!.sortedWith(compareBy<ArticleDetailEntry.DataDTO.ArticlesDTO> { 
                        it.title ?: "" 
                    }.let { if (isAscending) it else it.reversed() })
                }
            }
            
            // Create new adapter with sorted data
            listRv.adapter = object : com.zhy.adapter.recyclerview.CommonAdapter<ArticleDetailEntry.DataDTO.ArticlesDTO>(
                this, R.layout.item_publisher_article, sortedArticles
            ) {
                override fun convert(holder: com.zhy.adapter.recyclerview.base.ViewHolder, t: ArticleDetailEntry.DataDTO.ArticlesDTO, position: Int) {
                    val iconIv = holder.getView<ImageView>(R.id.article_publisher_icon)
                    val nameTv = holder.getView<TextView>(R.id.article_publisher_name)
                    val biasTv = holder.getView<TextView>(R.id.article_publisher_bias)
                    val titleTv = holder.getView<TextView>(R.id.article_title)
                    
                    Glide.with(this@NewsDetailActivity).load(t.publisherIcon).error(R.drawable.ease_default_image).into(iconIv)
                    nameTv.text = if (t.publisherName.isNullOrEmpty()) getString(R.string.about) else t.publisherName
                    
                    // Setup publisher bias tag
                    if (t.publisherStance != null && !t.publisherStance.tag.isNullOrEmpty()) {
                        biasTv.visibility = View.VISIBLE
                        biasTv.text = getPublisherBiasText(t.publisherStance.tag)
                        biasTv.setTextColor(getPublisherBiasTextColor(t.publisherStance.tag))
                        biasTv.setBackgroundResource(getPublisherBiasBackground(t.publisherStance.tag))
                    } else {
                        biasTv.visibility = View.GONE
                    }
                    
                    val title = t.title
                    titleTv.text = when {
                        !title.isNullOrEmpty() -> title
                        !t.description.isNullOrEmpty() -> t.description
                        else -> getString(R.string.details)
                    }
                    holder.convertView.setOnClickListener {
                        val linkRaw = when {
                            !t.articleURL.isNullOrEmpty() -> t.articleURL
                            !mArticleDetailEntry?.articleURL.isNullOrEmpty() -> mArticleDetailEntry?.articleURL
                            !mArticleDetailEntry?.shareURL.isNullOrEmpty() -> mArticleDetailEntry?.shareURL
                            else -> ""
                        }
                        val link = ensureHttpUrl(linkRaw ?: "")
                        if (link.isNotEmpty()) {
                            val intent = Intent(this@NewsDetailActivity, com.anssy.znewspro.ui.web.WebActivity::class.java)
                            intent.putExtra(getString(R.string.url_key), link)
                            intent.putExtra(getString(R.string.type_key), getString(R.string.news_type))
                            intent.putExtra(getString(R.string.publisher_icon_key), t.publisherIcon)
                            intent.putExtra(getString(R.string.publisher_name_key), t.publisherName)
                            startActivity(intent)
                        }
                    }
                }
            }
        }
    }
    
    private fun applyInitialSort(articles: List<ArticleDetailEntry.DataDTO.ArticlesDTO>): List<ArticleDetailEntry.DataDTO.ArticlesDTO> {
        return when (currentSortOption) {
            PublisherArticlesSortPopupWindow.SortOption.PUBLISHER_NAME -> {
                articles.sortedWith(compareBy<ArticleDetailEntry.DataDTO.ArticlesDTO> { 
                    it.publisherName ?: "" 
                }.let { if (isAscending) it else it.reversed() })
            }
            PublisherArticlesSortPopupWindow.SortOption.MEDIA_SIGNIFICANCE -> {
                articles.sortedWith(compareBy<ArticleDetailEntry.DataDTO.ArticlesDTO> { 
                    it.mediaSignificance ?: 0 
                }.let { if (isAscending) it else it.reversed() })
            }
            PublisherArticlesSortPopupWindow.SortOption.PUBLISHER_BIAS -> {
                articles.sortedWith(compareBy<ArticleDetailEntry.DataDTO.ArticlesDTO> { 
                    it.publisherStance?.tag ?: "" 
                }.let { if (isAscending) it else it.reversed() })
            }
            PublisherArticlesSortPopupWindow.SortOption.PUBLISHER_REGION -> {
                articles.sortedWith(compareBy<ArticleDetailEntry.DataDTO.ArticlesDTO> { 
                    it.publisherRegion ?: "" 
                }.let { if (isAscending) it else it.reversed() })
            }
            PublisherArticlesSortPopupWindow.SortOption.ARTICLE_TITLE -> {
                articles.sortedWith(compareBy<ArticleDetailEntry.DataDTO.ArticlesDTO> { 
                    it.title ?: "" 
                }.let { if (isAscending) it else it.reversed() })
            }
        }
    }

}