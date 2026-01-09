package com.anssy.znewspro.ui.newsdetail

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.GestureDetector
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.ViewGroup
import android.view.ViewConfiguration
import android.widget.FrameLayout
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.Interpolator
import android.view.animation.Transformation
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
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
import com.anssy.znewspro.entry.ViewHisEntry
import com.anssy.znewspro.model.MyModel
import com.anssy.znewspro.model.NewsDetailModel

import com.anssy.znewspro.utils.ToastUtils
import com.anssy.znewspro.utils.HapticFeedbackHelper
import com.anssy.znewspro.utils.Utils
import com.anssy.znewspro.utils.SharedPreferenceUtils
import com.bumptech.glide.Glide

import com.google.android.material.card.MaterialCardView
import com.anssy.znewspro.utils.SystemDialogUtils
import com.anssy.znewspro.selfview.popup.PublisherArticlesSortPopupWindow
import com.anssy.znewspro.selfview.popup.NewsDetailSettingsPopupWindow
import com.anssy.znewspro.utils.SwipeGestureHelper
import com.anssy.znewspro.utils.ThemeManager
import com.anssy.znewspro.utils.LanguageManager
import com.anssy.znewspro.utils.Language
import androidx.core.os.ConfigurationCompat
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderScriptBlur
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * @Description 新闻详情
 * @Author yulu
 * @CreateTime 2025年07月01日 16:58:28
 */
class NewsDetailActivity : BaseActivity() {
    private val newsDetailModel:NewsDetailModel by viewModels ()
    private val myModel: MyModel by viewModels()
    @Inject lateinit var languageManager: LanguageManager
    private lateinit var mViewBinding: ActivityNewsDetailBinding

    // Track saved state based on saved articles list from backend
    private var savedArticleIds: Set<String> = emptySet()
    private var isArticleSaved: Boolean = false
    
    // Floating bar control
    private var mBottomView: FrameLayout? = null
    private var isBottomBarHidden: Boolean = false
    private val autoShowRunnable = Runnable { showBottomBar() }
    private var isBottomBarInitialized: Boolean = false
    
    // Publisher articles sorting
    private var currentSortOption = PublisherArticlesSortPopupWindow.SortOption.PUBLISHER_NAME
    private var isAscending = true
    private var originalArticles: List<ArticleDetailEntry.DataDTO.ArticlesDTO>? = null
    
    // Gesture detection
    private lateinit var gestureDetector: GestureDetector
    // Timeline
    private var isProUser: Boolean = true
    private lateinit var timelineAdapter: TimelineAdapter
    data class TimelineArticle(
        val id: String,
        val title: String,
        val date: String,
        val articleID: String,
        val isCurrent: Boolean = false
    )
    
    // Card collapse/expand state
    private val cardCollapsedState = mutableMapOf<String, Boolean>()
    
    // Card order management
    private val cardIds = listOf("sentiment", "publisher", "subjectivity", "timeline")
    private val defaultCardOrder = listOf("sentiment", "publisher", "subjectivity", "timeline")
    
    // Summary language preference
    private var currentSummaryLanguage: String = Constants.LANGUAGE_ENGLISH_UK
    
    // Sentiment animation tracking
    private var sentimentValue: Double = 0.0
    private var hasSentimentCardAnimated: Boolean = false
    private var sentimentCardPreDrawListener: ViewTreeObserver.OnPreDrawListener? = null
    
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

        // Load saved summary language preference
        loadSummaryLanguagePreference()

        mBottomView = mViewBinding.newsDetailBottomCard
		setupToolbar()
        initModel()
        setupBottomBarBlur()
        setupFloatingBar()
        setupGestureDetection()
        setupTimelineStatic()
        mViewBinding.feedBackLayout.setOnClickListener {
                showFeedBackWindow()
        }
		mViewBinding.generateContextBtn.setOnClickListener {
			// Placeholder for future context generation
		}
        
        // Setup settings button
        mViewBinding.settingsButtonLayout.setOnClickListener {
            showSettingsPopup(it)
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
        
        // Load collapsed states before setting up controls
        loadCardCollapsedStates()
        // Setup card collapse/expand and drag functionality
        setupCardControls()
        // Initialize and apply card order
        applyCardOrder()
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
					// Use local isArticleSaved state which is determined from saved articles list
					if (isArticleSaved) {
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
        // Initialize save button as unsaved (default state before we know the actual state)
        initSaveButtonAsUnsaved()
        
        // Fetch saved articles list to determine actual saved state
        myModel.queryMyCollect()
        
        // Observe saved articles list from backend
        myModel.myCollectEntry.observe(this) { response ->
            if (response != null && response.code == Constants.SUCCESS_CODE && response.data?.articles != null) {
                // Build set of saved article IDs
                savedArticleIds = response.data.articles.mapNotNull { it.articleID }.toSet()
                // Check if current article is in the saved list
                isArticleSaved = savedArticleIds.contains(articleId)
                // Update button state based on actual saved status
                updateSaveButtonState(isArticleSaved)
            }
            // If the request fails or returns empty, keep button as unsaved (default)
        }
        
        newsDetailModel.queryNewsDetail(articleId, currentSummaryLanguage)
        newsDetailModel.newsDetailEntry.observe(this) {
            if (it.code == Constants.SUCCESS_CODE) {
                completeView(it.data)
                addHis()
                // After loading, refresh timeline current item title/date
                centerCurrentTimelineItem(it.data)
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
                    isArticleSaved = true
                    updateSaveButtonState(true)
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
                    isArticleSaved = false
                    updateSaveButtonState(false)
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
    
    /**
     * Initialize save button as unsaved (default state)
     */
    private fun initSaveButtonAsUnsaved() {
        val saveItem = mViewBinding.topAppBar.menu.findItem(R.id.action_save)
        saveItem.setIcon(R.drawable.ic_bookmark_outline_24)
        saveItem.iconTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorTextDeep))
    }
    
    /**
     * Update save button state based on whether article is saved
     */
    private fun updateSaveButtonState(isSaved: Boolean) {
        val saveItem = mViewBinding.topAppBar.menu.findItem(R.id.action_save)
        if (isSaved) {
            saveItem.setIcon(R.drawable.ic_bookmark_filled_24)
            saveItem.iconTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.brand_primary))
        } else {
            saveItem.setIcon(R.drawable.ic_bookmark_outline_24)
            saveItem.iconTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.colorTextDeep))
        }
    }
    private var mArticleDetailEntry: ArticleDetailEntry.DataDTO?=null
    @SuppressLint("NotifyDataSetChanged")
    private fun completeView(articleDetailEntry: ArticleDetailEntry.DataDTO){
        mArticleDetailEntry = articleDetailEntry
        Glide.with(mContext!!).load(articleDetailEntry.pictureURL).error(R.drawable.ic_image_not_supported_24)
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
                mViewBinding.newsSynopsisTv.visibility = View.VISIBLE
                android.util.Log.d("NewsDetail", "Synopsis displayed")
            } else {
                mViewBinding.newsSynopsisTv.visibility = View.GONE
                android.util.Log.d("NewsDetail", "Synopsis hidden - empty or null")
            }
            
            // Handle implications section
            if (!description.implications.isNullOrEmpty()) {
                mViewBinding.newsImplicationsTv.text = description.implications
                mViewBinding.newsImplicationsTv.visibility = View.VISIBLE
                android.util.Log.d("NewsDetail", "Implications displayed")
            } else {
                mViewBinding.newsImplicationsTv.visibility = View.GONE
                android.util.Log.d("NewsDetail", "Implications hidden - empty or null")
            }
        } ?: run {
            // Fallback for null description
            android.util.Log.d("NewsDetail", "Description is null")
            mViewBinding.newsSynopsisTv.visibility = View.GONE
            mViewBinding.newsImplicationsTv.visibility = View.GONE
        }
		// Note: Save button state is now determined by the saved articles list fetched in initModel()
		// The button state is managed by isArticleSaved and updateSaveButtonState()
		
		// Sentiment meter - store value for animation when card becomes visible
		sentimentValue = articleDetailEntry.metrics.sentiment
		hasSentimentCardAnimated = false
		// Initialize sentiment meter to 0, will animate when card becomes visible
		mViewBinding.sentimentMeter.setSentiment(0.0)
        startSentimentCardVisibilityWatcher()
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

    private fun setupTimelineStatic() {
        // Info button
        findViewById<ImageView>(R.id.timeline_info_btn)?.setOnClickListener { anchor ->
            val content = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(16f).toInt(), dp(12f).toInt(), dp(16f).toInt(), dp(12f).toInt())
                addView(TextView(this@NewsDetailActivity).apply {
                    text = getString(R.string.timeline_info_description)
                    setTextColor(ContextCompat.getColor(this@NewsDetailActivity, R.color.colorTextSmall))
                    textSize = 14f
                })
            }
            showTimelineInfoPopup(anchor, content)
        }

        val list = findViewById<RecyclerView>(R.id.timeline_list)
        val layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        list.layoutManager = layoutManager
        list.clipToPadding = false
        // Add side padding so middle item can center; half screen minus item half width (100dp)
        val screenWidth = resources.displayMetrics.widthPixels
        val side = (screenWidth / 2f - dp(100f)).toInt()
        list.setPadding(side, 0, side, 0)

        timelineAdapter = TimelineAdapter(isProUser) { article ->
            if (!isProUser) {
                ToastUtils.showShortToast(this, getString(R.string.open_in_browser))
                return@TimelineAdapter
            }
            // Navigate to related article if not current
            if (!article.isCurrent) {
                // Open web page or perform navigation per app behavior
                ToastUtils.showShortToast(this, getString(R.string.open_in_browser))
            }
        }
        list.adapter = timelineAdapter

        // Mock timeline data: 3 before, current, 3 after
        val mock = buildMockTimeline()
        timelineAdapter.submitList(mock)

        // Scroll to current center
        list.post {
            val currentIndex = mock.indexOfFirst { it.isCurrent }.let { if (it < 0) 3 else it }
            (list.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(currentIndex, 0)
        }
    }

    private fun buildMockTimeline(): List<TimelineArticle> {
        val current = mArticleDetailEntry
        val base = listOf(
            TimelineArticle("timeline-1", getString(R.string.timeline_mock_article1_title), "2024-01-10 09:00:00", "timeline-1"),
            TimelineArticle("timeline-2", getString(R.string.timeline_mock_article2_title), "2024-01-12 14:30:00", "timeline-2"),
            TimelineArticle("timeline-3", getString(R.string.timeline_mock_article3_title), "2024-01-14 11:15:00", "timeline-3"),
            TimelineArticle(current?.articleID ?: articleId, current?.title ?: (getString(R.string.details)), current?.date ?: "2024-01-15 10:00:00", current?.articleID ?: articleId, true),
            TimelineArticle("timeline-5", getString(R.string.timeline_mock_article5_title), "2024-01-16 16:45:00", "timeline-5"),
            TimelineArticle("timeline-6", getString(R.string.timeline_mock_article6_title), "2024-01-18 10:20:00", "timeline-6"),
            TimelineArticle("timeline-7", getString(R.string.timeline_mock_article7_title), "2024-01-20 13:00:00", "timeline-7")
        )
        return base
    }

    private fun centerCurrentTimelineItem(data: ArticleDetailEntry.DataDTO) {
        val list = findViewById<RecyclerView>(R.id.timeline_list)
        val currentIndex = 3
        (list.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(currentIndex, 0)
        // Update center item data
        val updated = buildMockTimeline().toMutableList()
        updated[3] = TimelineArticle(data.articleID, data.title ?: getString(R.string.details), data.date ?: "", data.articleID, true)
        timelineAdapter.submitList(updated)
    }

    private class TimelineAdapter(
        private val isProUser: Boolean,
        private val onItemClick: (TimelineArticle) -> Unit
    ) : RecyclerView.Adapter<TimelineAdapter.VH>() {
        private val items = mutableListOf<TimelineArticle>()

        fun submitList(list: List<TimelineArticle>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_timeline_article, parent, false)
            return VH(v)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.bind(item, isProUser, onItemClick)
        }

        class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val title: TextView = itemView.findViewById(R.id.timeline_item_title)
            private val date: TextView = itemView.findViewById(R.id.timeline_item_date)
            private val circle: View = itemView.findViewById(R.id.timeline_item_circle)
            private val circleBg: View = itemView.findViewById(R.id.timeline_item_circle_bg)

            fun bind(item: TimelineArticle, isPro: Boolean, onClick: (TimelineArticle) -> Unit) {
                title.text = item.title
                title.alpha = if (isPro) 1f else 0.35f
                // Lock icon overlay for current non-pro could be added via drawable, keep minimal per spec

                // Dot styling
                val lpBg = circleBg.layoutParams
                lpBg.width = if (item.isCurrent) dp(itemView, 20f) else dp(itemView, 16f)
                lpBg.height = if (item.isCurrent) dp(itemView, 20f) else dp(itemView, 16f)
                circleBg.layoutParams = lpBg
                val lp = circle.layoutParams
                lp.width = if (item.isCurrent) dp(itemView, 14f) else dp(itemView, 10f)
                lp.height = if (item.isCurrent) dp(itemView, 14f) else dp(itemView, 10f)
                circle.layoutParams = lp
                circle.setBackgroundResource(if (item.isCurrent) R.drawable.timeline_circle else R.drawable.timeline_circle_gray)

                // Position the larger circle (current article) slightly higher to align centers
                val circleContainer = circle.parent as FrameLayout
                val containerLp = circleContainer.layoutParams as LinearLayout.LayoutParams
                containerLp.topMargin = if (item.isCurrent) dp(itemView, -3f) else dp(itemView, 0f)
                circleContainer.layoutParams = containerLp

                // Use relative time format (e.g., "2 days ago")
                date.text = Utils.formatBackendDate(itemView.context, item.date)

                itemView.setOnClickListener { onClick(item) }
            }

            private fun dp(view: View, v: Float): Int = (v * view.resources.displayMetrics.density).toInt()
        }
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
                
                Glide.with(this@NewsDetailActivity).load(t.publisherIcon).error(R.drawable.ic_image_not_supported_24).into(iconIv)
                nameTv.text = if (t.publisherName.isNullOrEmpty()) getString(R.string.about) else t.publisherName
                
                // Setup publisher bias tag - only show if report patterns is enabled
                val reportPatternsEnabled = com.anssy.znewspro.utils.SharedPreferenceUtils.getBoolean(this@NewsDetailActivity, "report_patterns_enabled")
                if (reportPatternsEnabled && t.publisherStance != null && !t.publisherStance.tag.isNullOrEmpty()) {
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
                        openArticleLink(link, t.publisherIcon, t.publisherName)
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

    /**
     * Open article link based on user preference (in-app browser or external browser)
     */
    private fun openArticleLink(url: String, publisherIcon: String?, publisherName: String?) {
        val articleOpeningMethod = SharedPreferenceUtils.getString(this, "article_opening_method")
        
        if (articleOpeningMethod == "external") {
            // Open in external browser or app
            try {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                startActivity(intent)
            } catch (e: Exception) {
                ToastUtils.showShortToast(this, getString(R.string.open_in_browser))
            }
        } else {
            // Default: Open in in-app browser (WebActivity)
            val intent = Intent(this, com.anssy.znewspro.ui.web.WebActivity::class.java)
            intent.putExtra(getString(R.string.url_key), url)
            intent.putExtra(getString(R.string.type_key), getString(R.string.news_type))
            intent.putExtra(getString(R.string.publisher_icon_key), publisherIcon)
            intent.putExtra(getString(R.string.publisher_name_key), publisherName)
            startActivity(intent)
        }
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
     * Setup blur effect for bottom navigation bar to achieve liquid glass appearance
     */
    private fun setupBottomBarBlur() {
        val blurView = mViewBinding.newsDetailBottomBlurView ?: return
        val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val glassOverlay = findViewById<View>(R.id.newsDetailBottomGlassOverlay)
        val cardView = mBottomView
        
        // Ensure FrameLayout is transparent
        cardView?.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        
        // Setup fully rounded corners outline for clipping and elevation for shadow (pill-shaped)
        cardView?.post {
            val cornerRadiusPx = (28 * resources.displayMetrics.density).toInt()
            cardView?.outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: android.view.View, outline: android.graphics.Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx.toFloat())
                }
            }
            cardView?.clipToOutline = true
            
            // Add subtle elevation for shadow effect (2dp for slight shadow)
            cardView?.elevation = 2 * resources.displayMetrics.density
        }
        
        // Set rounded background drawable on BlurView for proper clipping
        blurView.setBackgroundResource(R.drawable.bottom_nav_rounded_background)
        
        // Configure blur view to blur content behind the navigation bar
        blurView.setupWith(rootView, RenderScriptBlur(this))
            .setBlurRadius(20f) // Blur radius for frosted glass effect
            .setBlurAutoUpdate(true) // Automatically update blur when content changes
        
        // Set translucent background drawable on the overlay view based on theme
        // Using drawable instead of solid color to ensure rounded corners work properly
        val glassOverlayDrawable = if (ThemeManager.isDarkModeActive(this)) {
            ContextCompat.getDrawable(this, R.drawable.bottom_nav_glass_overlay_dark)
        } else {
            ContextCompat.getDrawable(this, R.drawable.bottom_nav_glass_overlay_light)
        }
        
        // Apply translucent background drawable to overlay view for liquid glass effect
        glassOverlay?.background = glassOverlayDrawable
    }
    
    /**
     * Setup floating bar functionality
     */
    private fun setupFloatingBar() {
        // Clear any initial selection to prevent immediate navigation
        mViewBinding.newsDetailRg.clearCheck()
        
        // Note: Navigation is handled by individual click listeners below
        // This allows clicking the already-selected button to act as a back button
        
        // Mark as initialized and set initial checked state after layout settles
        mViewBinding.newsDetailRg.post {
            // Check which page the user came from (default to home)
            val sourceFragment = intent.getStringExtra("source_fragment") ?: "home"
            val radioButtonId = when (sourceFragment) {
                "special" -> R.id.news_detail_special_rb
                "my" -> R.id.news_detail_my_rb
                "search" -> R.id.news_detail_search_rb
                else -> R.id.news_detail_home_rb
            }
            
            // Set the appropriate radio button as checked (won't trigger navigation yet)
            mViewBinding.newsDetailRg.check(radioButtonId)
            
            // Update tab backgrounds for the selected state
            updateTabBackgrounds()
            
            // Now mark as initialized - future changes will trigger navigation
            isBottomBarInitialized = true
        }
        
        // Add click listeners to handle navigation
        // Home button always goes to home, other buttons go to their respective pages
        // Use fade animations for consistent navigation
        mViewBinding.newsDetailRg.findViewById<RadioButton>(R.id.news_detail_home_rb).setOnClickListener { view ->
            if (!isBottomBarInitialized) return@setOnClickListener
            HapticFeedbackHelper.performNavigationHaptic(view)
            val intent = Intent(this, com.anssy.znewspro.ui.MainActivity::class.java)
            intent.putExtra("fragment", "home")
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }
        
        mViewBinding.newsDetailRg.findViewById<RadioButton>(R.id.news_detail_special_rb).setOnClickListener { view ->
            if (!isBottomBarInitialized) return@setOnClickListener
            HapticFeedbackHelper.performNavigationHaptic(view)
            val intent = Intent(this, com.anssy.znewspro.ui.MainActivity::class.java)
            intent.putExtra("fragment", "special")
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }
        
        mViewBinding.newsDetailRg.findViewById<RadioButton>(R.id.news_detail_my_rb).setOnClickListener { view ->
            if (!isBottomBarInitialized) return@setOnClickListener
            HapticFeedbackHelper.performNavigationHaptic(view)
            val intent = Intent(this, com.anssy.znewspro.ui.MainActivity::class.java)
            intent.putExtra("fragment", "my")
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
        }
        
        mViewBinding.newsDetailRg.findViewById<RadioButton>(R.id.news_detail_search_rb).setOnClickListener { view ->
            if (!isBottomBarInitialized) return@setOnClickListener
            HapticFeedbackHelper.performNavigationHaptic(view)
            val intent = Intent(this, com.anssy.znewspro.ui.MainActivity::class.java)
            intent.putExtra("fragment", "search")
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finish()
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
     * Start a one-time watcher that triggers the sentiment animation as soon as the card is visible.
     * Uses pre-draw callbacks so it works both when initially on-screen and when scrolled into view.
     */
    private fun startSentimentCardVisibilityWatcher() {
        val sentimentCard = mViewBinding.sentimentCard
        val observer = sentimentCard.viewTreeObserver

        // Clear any previous listener before adding a new one
        sentimentCardPreDrawListener?.let { existing ->
            observer.takeIf { it.isAlive }?.removeOnPreDrawListener(existing)
        }

        sentimentCardPreDrawListener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (hasSentimentCardAnimated) {
                    sentimentCard.viewTreeObserver.takeIf { it.isAlive }?.removeOnPreDrawListener(this)
                    sentimentCardPreDrawListener = null
                    return true
                }

                if (isSentimentCardVisible()) {
                    animateSentimentCard()
                    sentimentCard.viewTreeObserver.takeIf { it.isAlive }?.removeOnPreDrawListener(this)
                    sentimentCardPreDrawListener = null
                }
                return true
            }
        }

        observer.takeIf { it.isAlive }?.addOnPreDrawListener(sentimentCardPreDrawListener)
    }

    private fun isSentimentCardVisible(): Boolean {
        val sentimentCard = mViewBinding.sentimentCard
        if (!sentimentCard.isShown) return false
        val visibleRect = Rect()
        return sentimentCard.getGlobalVisibleRect(visibleRect)
    }

    private fun animateSentimentCard() {
        if (hasSentimentCardAnimated) return
        hasSentimentCardAnimated = true
        mViewBinding.sentimentMeter.setSentimentWithAnimationFromZero(sentimentValue)
    }
    
    /**
     * Update RadioButton backgrounds to show selected tab highlight
     */
    private fun updateTabBackgrounds() {
        val isDarkMode = ThemeManager.isDarkModeActive(this)
        val selectedDrawable = if (isDarkMode) {
            ContextCompat.getDrawable(this, R.drawable.bottom_nav_tab_selected_dark)
        } else {
            ContextCompat.getDrawable(this, R.drawable.bottom_nav_tab_selected_light)
        }
        
        val radioGroup = mViewBinding.newsDetailRg
        val radioButtons = listOf(
            radioGroup.findViewById<RadioButton>(R.id.news_detail_home_rb),
            radioGroup.findViewById<RadioButton>(R.id.news_detail_special_rb),
            radioGroup.findViewById<RadioButton>(R.id.news_detail_my_rb),
            radioGroup.findViewById<RadioButton>(R.id.news_detail_search_rb)
        )
        
        radioButtons.forEach { radioButton ->
            radioButton?.background = if (radioButton?.isChecked == true) {
                selectedDrawable
            } else {
                null // Transparent background for unselected tabs
            }
        }
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
     * Show settings popup
     */
    private fun showSettingsPopup(anchor: View) {
        val popup = NewsDetailSettingsPopupWindow(this, object : NewsDetailSettingsPopupWindow.Callback {
            override fun onOptionSelected(option: NewsDetailSettingsPopupWindow.SettingOption) {
                when (option) {
                    NewsDetailSettingsPopupWindow.SettingOption.ENGLISH -> {
                        currentSummaryLanguage = Constants.LANGUAGE_ENGLISH_UK
                        saveSummaryLanguagePreference(currentSummaryLanguage)
                        // Reload article with new language
                        newsDetailModel.queryNewsDetail(articleId, currentSummaryLanguage)
                    }
                    NewsDetailSettingsPopupWindow.SettingOption.TRADITIONAL_CHINESE -> {
                        currentSummaryLanguage = Constants.LANGUAGE_TRADITIONAL_CHINESE
                        saveSummaryLanguagePreference(currentSummaryLanguage)
                        // Reload article with new language
                        newsDetailModel.queryNewsDetail(articleId, currentSummaryLanguage)
                    }
                    NewsDetailSettingsPopupWindow.SettingOption.SIMPLIFIED_CHINESE -> {
                        currentSummaryLanguage = Constants.LANGUAGE_SIMPLIFIED_CHINESE
                        saveSummaryLanguagePreference(currentSummaryLanguage)
                        // Reload article with new language
                        newsDetailModel.queryNewsDetail(articleId, currentSummaryLanguage)
                    }
                    else -> {
                        // STRAIGHTFORWARD and NUANCED options are placeholders for future functionality
                    }
                }
            }
        })
        // Set current language selection
        val currentLanguageOption = when (currentSummaryLanguage) {
            Constants.LANGUAGE_ENGLISH_UK -> NewsDetailSettingsPopupWindow.SettingOption.ENGLISH
            Constants.LANGUAGE_TRADITIONAL_CHINESE -> NewsDetailSettingsPopupWindow.SettingOption.TRADITIONAL_CHINESE
            Constants.LANGUAGE_SIMPLIFIED_CHINESE -> NewsDetailSettingsPopupWindow.SettingOption.SIMPLIFIED_CHINESE
            else -> NewsDetailSettingsPopupWindow.SettingOption.ENGLISH
        }
        popup.setCurrentLanguage(currentLanguageOption)
        popup.showPopupWindow(anchor)
    }
    
    /**
     * Load saved summary language preference
     * If user has set a preference, use it. Otherwise, detect and match the current app language.
     */
    private fun loadSummaryLanguagePreference() {
        val savedLanguage = com.anssy.znewspro.utils.SharedPreferenceUtils.getString(this, "news_detail_summary_language")
        currentSummaryLanguage = if (savedLanguage.isNotEmpty()) {
            // User has set a preference, use it
            savedLanguage
        } else {
            // No preference saved, detect and match current app language
            // Use LanguageManager if injected, otherwise detect directly
            if (::languageManager.isInitialized) {
                languageManager.getCurrentLanguageCode()
            } else {
                // Fallback: detect language directly using the same logic as LanguageManager
                getCurrentLanguageCodeDirectly()
            }
        }
    }
    
    /**
     * Get current language code directly without LanguageManager injection
     * Uses the same logic as LanguageManager.getCurrentLanguageCode()
     */
    private fun getCurrentLanguageCodeDirectly(): String {
        val locale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            ConfigurationCompat.getLocales(resources.configuration)[0]
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale
        }

        // Use full language tag if possible (e.g. zh-HK, zh-TW, en-US)
        val localeTag = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            locale?.toLanguageTag() ?: "en"
        } else {
            val language = locale?.language ?: "en"
            val country = try { locale?.country } catch (_: Throwable) { null }
            if (!country.isNullOrEmpty()) "$language-$country" else language
        }
        return Language.getBackendCode(localeTag)
    }
    
    /**
     * Save summary language preference
     */
    private fun saveSummaryLanguagePreference(language: String) {
        com.anssy.znewspro.utils.SharedPreferenceUtils.saveString(this, "news_detail_summary_language", language)
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
     * Show timeline info popup with alignment matching other cards
     */
    private fun showTimelineInfoPopup(anchor: View, content: LinearLayout) {
        // Calculate popup width to match card content area (screen width minus card margins)
        val screenWidth = resources.displayMetrics.widthPixels
        val cardMargin = dp(20f).toInt() * 2 // 12dp margin on each side
        val popupWidth = screenWidth - cardMargin
        
        val popup = android.widget.PopupWindow(content, popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        popup.isOutsideTouchable = true
        popup.isFocusable = true
        popup.elevation = dp(4f)
        popup.setBackgroundDrawable(android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(12f)
            setColor(androidx.core.content.ContextCompat.getColor(this@NewsDetailActivity, R.color.profile_card_bg))
        })
        
        // Align popup to match card content area (12dp from screen edge)
        val xoff = dp(20f).toInt() - anchor.left
        
        // Align the BOTTOM of the popup with the icon's UPPER edge
        content.measure(View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY), View.MeasureSpec.UNSPECIFIED)
        val popupHeight = content.measuredHeight
        val yoff = -(popupHeight + anchor.height + dp(6f).toInt())
        
        popup.showAsDropDown(anchor, xoff, yoff)
    }
    
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
                    
                    Glide.with(this@NewsDetailActivity).load(t.publisherIcon).error(R.drawable.ic_image_not_supported_24).into(iconIv)
                    nameTv.text = if (t.publisherName.isNullOrEmpty()) getString(R.string.about) else t.publisherName
                    
                    // Setup publisher bias tag - only show if report patterns is enabled
                    val reportPatternsEnabled = com.anssy.znewspro.utils.SharedPreferenceUtils.getBoolean(this@NewsDetailActivity, "report_patterns_enabled")
                    if (reportPatternsEnabled && t.publisherStance != null && !t.publisherStance.tag.isNullOrEmpty()) {
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
                            openArticleLink(link, t.publisherIcon, t.publisherName)
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
    
    private fun setupCardControls() {
        // Setup chevron buttons for collapse/expand
        setupChevronButton("sentiment", R.id.sentiment_chevron_btn, R.id.sentiment_content_container)
        setupChevronButton("publisher", R.id.publisher_chevron_btn, R.id.publisher_content_container)
        setupSubjectivityChevronButton()
        setupChevronButton("timeline", R.id.timeline_chevron_btn, R.id.timeline_content_container)
        
        // Setup menu buttons for drag and drop
        setupMenuButton("sentiment", R.id.sentiment_menu_btn, R.id.sentiment_card)
        setupMenuButton("publisher", R.id.publisher_menu_btn, R.id.publisher_card)
        setupSubjectivityMenuButton()
        setupMenuButton("timeline", R.id.timeline_menu_btn, R.id.timeline_container)
    }
    
    
    private fun applyCardOrderToViews(order: List<String>) {
        val scrollView = findViewById<androidx.core.widget.NestedScrollView>(R.id.newsDetailScrollView)
        val cardsContainer = scrollView?.getChildAt(0) as? LinearLayout
        if (cardsContainer == null) return
        
        val cardViews = mapOf(
            "sentiment" to findViewById<View>(R.id.sentiment_card),
            "publisher" to findViewById<View>(R.id.publisher_card),
            "subjectivity" to findViewById<View>(R.id.subjectivity_card),
            "timeline" to findViewById<View>(R.id.timeline_container)
        )
        
        // Find current positions
        val currentIndices = mutableMapOf<String, Int>()
        for (i in 0 until cardsContainer.childCount) {
            val child = cardsContainer.getChildAt(i)
            cardViews.forEach { (id, view) ->
                if (view == child) {
                    currentIndices[id] = i
                }
            }
        }
        
        if (currentIndices.isNotEmpty()) {
            val firstCardIndex = currentIndices.values.minOrNull() ?: 0
            val viewsToReorder = mutableMapOf<String, View>()
            cardViews.forEach { (id, view) ->
                if (currentIndices.containsKey(id)) {
                    viewsToReorder[id] = view
                }
            }
            
            // Remove all card views in reverse order
            currentIndices.toList().sortedByDescending { it.second }.forEach { (_, index) ->
                cardsContainer.removeViewAt(index)
            }
            
            // Add back in new order
            order.forEachIndexed { orderIndex, cardId ->
                val view = viewsToReorder[cardId]
                if (view != null) {
                    cardsContainer.addView(view, firstCardIndex + orderIndex)
                }
            }
        }
    }
    
    private fun setupSubjectivityChevronButton() {
        val subjectivityView = findViewById<SubjectivityScoreView>(R.id.subjectivity_score)
        val chevronBtn = subjectivityView?.findViewById<ImageView>(R.id.chevron_btn)
        val contentContainer = subjectivityView?.findViewById<LinearLayout>(R.id.content_container)
        
        if (chevronBtn != null && contentContainer != null) {
            val isCollapsed = cardCollapsedState["subjectivity"] ?: false
            updateChevronIcon(chevronBtn, isCollapsed)
            contentContainer.visibility = if (isCollapsed) View.GONE else View.VISIBLE
            
            chevronBtn.setOnClickListener {
                val currentlyCollapsed = cardCollapsedState["subjectivity"] ?: false
                val newCollapsedState = !currentlyCollapsed
                cardCollapsedState["subjectivity"] = newCollapsedState
                saveCardCollapsedState("subjectivity", newCollapsedState)
                
                if (newCollapsedState) {
                    collapseView(contentContainer)
                } else {
                    expandView(contentContainer)
                }
                updateChevronIcon(chevronBtn, newCollapsedState)
            }
        }
    }
    
    private fun setupSubjectivityMenuButton() {
        val subjectivityView = findViewById<SubjectivityScoreView>(R.id.subjectivity_score)
        val menuBtn = subjectivityView?.findViewById<ImageView>(R.id.menu_btn)
        val cardView = findViewById<View>(R.id.subjectivity_card)
        
        if (menuBtn != null && cardView != null) {
            var isDragging = false
            
            menuBtn.setOnLongClickListener { view ->
                isDragging = true
                menuBtn.setColorFilter(ContextCompat.getColor(this, R.color.brand_primary))
                
                // Make card fully transparent to prevent judder - drag shadow provides visual feedback
                cardView.alpha = 0f
                
                val shadowBuilder = createTopRightDragShadow(cardView, menuBtn)
                val item = android.content.ClipData.Item("subjectivity")
                val dragData = android.content.ClipData("subjectivity", arrayOf("text/plain"), item)
                cardView.startDragAndDrop(dragData, shadowBuilder, cardView, 0)
                
                true
            }
            
            cardView.setOnDragListener(createDragListener("subjectivity", menuBtn) { isDragging = false })
        }
    }
    
    private fun setupChevronButton(cardId: String, chevronBtnId: Int, contentContainerId: Int) {
        val chevronBtn = findViewById<ImageView>(chevronBtnId)
        val contentContainer = findViewById<View>(contentContainerId)
        
        if (chevronBtn != null && contentContainer != null) {
            // Initialize state - all cards expanded by default
            val isCollapsed = cardCollapsedState[cardId] ?: false
            updateChevronIcon(chevronBtn, isCollapsed)
            contentContainer.visibility = if (isCollapsed) View.GONE else View.VISIBLE
            
            chevronBtn.setOnClickListener {
                val currentlyCollapsed = cardCollapsedState[cardId] ?: false
                val newCollapsedState = !currentlyCollapsed
                cardCollapsedState[cardId] = newCollapsedState
                saveCardCollapsedState(cardId, newCollapsedState)
                
                if (newCollapsedState) {
                    collapseView(contentContainer)
                } else {
                    expandView(contentContainer)
                }
                updateChevronIcon(chevronBtn, newCollapsedState)
            }
        }
    }
    
    private fun setupMenuButton(cardId: String, menuBtnId: Int, cardViewId: Int) {
        val menuBtn = findViewById<ImageView>(menuBtnId)
        val cardView = findViewById<View>(cardViewId)
        
        if (menuBtn != null && cardView != null) {
            var isDragging = false
            var dragStartY = 0f
            
            menuBtn.setOnLongClickListener { view ->
                isDragging = true
                menuBtn.setColorFilter(ContextCompat.getColor(this, R.color.brand_primary))
                
                // Make card fully transparent to prevent judder - drag shadow provides visual feedback
                cardView.alpha = 0f
                
                val shadowBuilder = createTopRightDragShadow(cardView, menuBtn)
                val item = android.content.ClipData.Item(cardId)
                val dragData = android.content.ClipData(cardId, arrayOf("text/plain"), item)
                cardView.startDragAndDrop(dragData, shadowBuilder, cardView, 0)
                
                true
            }
            
            cardView.setOnDragListener(createDragListener(cardId, menuBtn) { isDragging = false })
        }
    }
    
    private fun createTopRightDragShadow(cardView: View, menuBtn: ImageView): View.DragShadowBuilder {
        return object : View.DragShadowBuilder(cardView) {
            override fun onProvideShadowMetrics(outShadowSize: android.graphics.Point, outShadowTouchPoint: android.graphics.Point) {
                // Get shadow size from the card
                val shadowWidth = cardView.width
                val shadowHeight = cardView.height
                outShadowSize.set(shadowWidth, shadowHeight)
                
                // Get the menu button's position relative to the card using getLocationInWindow
                // This gives us more accurate positioning within the activity's window
                val menuBtnLocation = IntArray(2)
                menuBtn.getLocationInWindow(menuBtnLocation)
                val cardLocation = IntArray(2)
                cardView.getLocationInWindow(cardLocation)
                
                // Calculate the menu button's center point relative to the card's top-left corner
                // This will align the touch point with the center of the menu button where the user's finger is
                val menuBtnCenterX = menuBtnLocation[0] + (menuBtn.width / 2) - cardLocation[0]
                val menuBtnCenterY = menuBtnLocation[1] + (menuBtn.height / 2) - cardLocation[1]
                
                // Set touch point to the menu button's center
                // This ensures the button appears exactly where the user's finger is when they long press
                outShadowTouchPoint.set(menuBtnCenterX, menuBtnCenterY)
            }
        }
    }
    
    private fun createDragListener(cardId: String, menuBtn: ImageView, onDragEnd: () -> Unit): View.OnDragListener {
        return View.OnDragListener { view, event ->
            when (event.action) {
                android.view.DragEvent.ACTION_DRAG_STARTED -> {
                    val draggedCardId = event.clipDescription?.label?.toString()
                    if (draggedCardId == cardId) {
                        // Card is already hidden to prevent judder
                        // Don't provide haptic here - system long press already did
                        true
                    } else {
                        // Other cards can accept drops
                        true
                    }
                }
                android.view.DragEvent.ACTION_DRAG_LOCATION -> {
                    val draggedCardId = event.clipDescription?.label?.toString()
                    if (draggedCardId != null && draggedCardId != cardId) {
                        // Determine if hovering over top or bottom half
                        val y = event.y
                        val cardHeight = view.height
                        val isTopHalf = y < cardHeight / 2f
                        
                        // Visual feedback: slightly different alpha for top vs bottom
                        view.alpha = if (isTopHalf) 0.8f else 0.7f
                    }
                    true
                }
                android.view.DragEvent.ACTION_DRAG_ENTERED -> {
                    val draggedCardId = event.clipDescription?.label?.toString()
                    if (draggedCardId != null && draggedCardId != cardId) {
                        // Highlight drop target - default to top half
                        view.alpha = 0.8f
                    }
                    true
                }
                android.view.DragEvent.ACTION_DRAG_EXITED -> {
                    val draggedCardId = event.clipDescription?.label?.toString()
                    if (draggedCardId != null && draggedCardId != cardId) {
                        view.alpha = 1.0f
                    }
                    true
                }
                android.view.DragEvent.ACTION_DROP -> {
                    val draggedCardId = event.clipData?.getItemAt(0)?.text?.toString()
                    if (draggedCardId != null && draggedCardId != cardId) {
                        // Determine if dropped on top or bottom half
                        val y = event.y
                        val cardHeight = view.height
                        val isTopHalf = y < cardHeight / 2f
                        
                        // Reorder based on which half was targeted
                        reorderCards(draggedCardId, cardId, isTopHalf)
                    }
                    view.alpha = 1.0f
                    menuBtn.clearColorFilter()
                    onDragEnd()
                    true
                }
                android.view.DragEvent.ACTION_DRAG_ENDED -> {
                    val draggedCardId = event.clipDescription?.label?.toString()
                    if (draggedCardId == cardId) {
                        // Restore card alpha after drag ends
                        view.alpha = 1.0f
                    } else {
                        view.alpha = 1.0f
                    }
                    menuBtn.clearColorFilter()
                    onDragEnd()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun reorderCards(draggedCardId: String, targetCardId: String, insertBefore: Boolean = true) {
        val cardOrder = loadCardOrder()
        val draggedIndex = cardOrder.indexOf(draggedCardId)
        val targetIndex = cardOrder.indexOf(targetCardId)
        
        if (draggedIndex == -1 || targetIndex == -1 || draggedIndex == targetIndex) return
        
        // Remove dragged card from order
        cardOrder.removeAt(draggedIndex)
        
        // Calculate insertion index based on whether inserting before or after target
        val newTargetIndex = if (insertBefore) {
            // Insert before target card
            if (targetIndex > draggedIndex) targetIndex - 1 else targetIndex
        } else {
            // Insert after target card
            if (targetIndex > draggedIndex) targetIndex else targetIndex + 1
        }
        
        // Ensure index is within bounds
        val insertIndex = newTargetIndex.coerceIn(0, cardOrder.size)
        cardOrder.add(insertIndex, draggedCardId)
        
        applyCardOrderToViews(cardOrder)
        saveCardOrder(cardOrder)
    }
    
    private fun updateChevronIcon(chevronBtn: ImageView, isCollapsed: Boolean) {
        val iconRes = if (isCollapsed) R.drawable.ic_chevron_up_24 else R.drawable.ic_chevron_down_24
        chevronBtn.setImageResource(iconRes)
        chevronBtn.setColorFilter(ContextCompat.getColor(this, R.color.colorTextMiddle))
    }
    
    private fun collapseView(view: View) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec((view.parent as View).width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val initialHeight = view.measuredHeight
        
        val animator = android.animation.ValueAnimator.ofInt(initialHeight, 0)
        animator.duration = 300
        animator.interpolator = easeInOutQuart
        animator.addUpdateListener { animation ->
            val height = animation.animatedValue as Int
            view.layoutParams.height = height
            view.requestLayout()
        }
        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                view.visibility = View.GONE
                view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        })
        animator.start()
    }
    
    private fun expandView(view: View) {
        view.visibility = View.VISIBLE
        view.measure(
            View.MeasureSpec.makeMeasureSpec((view.parent as View).width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val targetHeight = view.measuredHeight
        
        view.layoutParams.height = 0
        val animator = android.animation.ValueAnimator.ofInt(0, targetHeight)
        animator.duration = 300
        animator.interpolator = easeInOutQuart
        animator.addUpdateListener { animation ->
            val height = animation.animatedValue as Int
            view.layoutParams.height = height
            view.requestLayout()
        }
        animator.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                view.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            }
        })
        animator.start()
    }
    
    override fun onDestroy() {
        sentimentCardPreDrawListener?.let {
            mViewBinding.sentimentCard.viewTreeObserver.takeIf { observer -> observer.isAlive }?.removeOnPreDrawListener(it)
        }
        sentimentCardPreDrawListener = null
        super.onDestroy()
    }
    
    private fun loadCardOrder(): MutableList<String> {
        val savedOrder = com.anssy.znewspro.utils.SharedPreferenceUtils.getString(this, "news_detail_card_order")
        return if (savedOrder.isNotEmpty()) {
            savedOrder.split(",").toMutableList()
        } else {
            defaultCardOrder.toMutableList()
        }
    }
    
    private fun saveCardOrder(order: List<String>) {
        com.anssy.znewspro.utils.SharedPreferenceUtils.saveString(this, "news_detail_card_order", order.joinToString(","))
    }
    
    private fun loadCardCollapsedStates() {
        cardIds.forEach { cardId ->
            val isCollapsed = com.anssy.znewspro.utils.SharedPreferenceUtils.getBoolean(this, "news_detail_card_collapsed_$cardId")
            cardCollapsedState[cardId] = isCollapsed
        }
    }
    
    private fun saveCardCollapsedState(cardId: String, isCollapsed: Boolean) {
        com.anssy.znewspro.utils.SharedPreferenceUtils.saveBoolean(this, "news_detail_card_collapsed_$cardId", isCollapsed)
    }
    
    private fun applyCardOrder() {
        val savedOrder = com.anssy.znewspro.utils.SharedPreferenceUtils.getString(this, "news_detail_card_order")
        val order = if (savedOrder.isNotEmpty()) {
            savedOrder.split(",")
        } else {
            saveCardOrder(defaultCardOrder)
            defaultCardOrder
        }
        
        applyCardOrderToViews(order)
    }

}