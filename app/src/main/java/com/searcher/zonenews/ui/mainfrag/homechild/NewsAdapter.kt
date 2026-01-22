package com.searcher.zonenews.ui.mainfrag.homechild

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.searcher.zonenews.R
import com.searcher.zonenews.entry.HomeDataListEntry
import com.searcher.zonenews.ui.newsdetail.NewsDetailActivity
import com.searcher.zonenews.utils.CalculateUtil
import com.searcher.zonenews.utils.Utils
import com.bumptech.glide.Glide
import com.zhpan.bannerview.BannerViewPager
import com.zhpan.bannerview.BaseBannerAdapter
import com.zhpan.bannerview.BaseViewHolder
import kotlin.math.abs

class NewsAdapter(
    private val context: android.content.Context,
    private val newsList: MutableList<HomeDataListEntry.DataDTO.ArticlesDTO>,
    var isTodayTab: Boolean,
    private val onShareArticle: (HomeDataListEntry.DataDTO.ArticlesDTO) -> Unit,
    private val onBannerClick: ((HomeDataListEntry.DataDTO.HeadlinesDTO) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Cache density to avoid repeated calculations
    private val density = context.resources.displayMetrics.density
    private val defaultPadding = (8 * density).toInt()
    private val reducedPadding = (0 * density).toInt()
    
    // Banner-related state
    private var showBanner = false
    private var bannerData: MutableList<HomeDataListEntry.DataDTO.HeadlinesDTO> = mutableListOf()
    private var bannerAdapter: BaseBannerAdapter<HomeDataListEntry.DataDTO.HeadlinesDTO, BannerNetViewHolder>? = null

    companion object {
        private const val VIEW_TYPE_BANNER = -1
        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_BIG = 1
        private const val VIEW_TYPE_FOOTER = 2
        private const val BIG_CARD_INTERVAL = 5 // Show big card every 5 cards (4 normal + 1 big)
        private const val BIG_CARD_OFFSET = 4 // Show big card at position 4 (5th card: 0,1,2,3,4)
    }
    
    // Banner control methods
    fun setBannerVisible(visible: Boolean) {
        showBanner = visible
    }
    
    fun setBannerAdapter(adapter: BaseBannerAdapter<HomeDataListEntry.DataDTO.HeadlinesDTO, BannerNetViewHolder>) {
        this.bannerAdapter = adapter
    }
    
    fun updateBannerData(data: List<HomeDataListEntry.DataDTO.HeadlinesDTO>) {
        bannerData.clear()
        bannerData.addAll(data)
        if (showBanner) {
            notifyItemChanged(0)
        }
    }
    
    private fun hasBanner(): Boolean = showBanner
    
    private fun getArticlePosition(adapterPosition: Int): Int {
        return if (hasBanner()) adapterPosition - 1 else adapterPosition
    }

    override fun getItemViewType(position: Int): Int {
        // Banner is always at position 0 when visible
        if (hasBanner() && position == 0) {
            return VIEW_TYPE_BANNER
        }
        
        // Footer is always at the last position
        val footerPosition = newsList.size + (if (hasBanner()) 1 else 0)
        if (position == footerPosition) {
            return VIEW_TYPE_FOOTER
        }
        
        // Adjust position for article view types
        val articlePosition = getArticlePosition(position)
        
        // Show big card at positions 4, 9, 14, 19, etc. (one every 4 normal cards)
        return if ((articlePosition - BIG_CARD_OFFSET) % BIG_CARD_INTERVAL == 0 && articlePosition >= BIG_CARD_OFFSET) VIEW_TYPE_BIG else VIEW_TYPE_NORMAL
    }

    fun setIsTodayTab(isToday: Boolean) {
        this.isTodayTab = isToday
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_BANNER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_home_banner, parent, false)
                BannerViewHolder(view)
            }
            VIEW_TYPE_BIG -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_big_news_card, parent, false)
                BigCardViewHolder(view)
            }
            VIEW_TYPE_FOOTER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_loading_footer, parent, false)
                FooterViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_home_recycler, parent, false)
                NormalCardViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is BannerViewHolder -> holder.bind()
            is FooterViewHolder -> { /* Footer is static, no binding needed */ }
            else -> {
                val articlePosition = getArticlePosition(position)
                if (articlePosition >= 0 && articlePosition < newsList.size) {
                    val article = newsList[articlePosition]
                    when (holder) {
                        is NormalCardViewHolder -> holder.bind(article, articlePosition)
                        is BigCardViewHolder -> holder.bind(article, articlePosition)
                    }
                }
            }
        }
    }

    // +1 for footer, +1 for banner if visible
    override fun getItemCount(): Int = newsList.size + 1 + if (hasBanner()) 1 else 0
    
    // Handle lifecycle for the banner loop
    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (holder is BannerViewHolder) {
            holder.startLoop()
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder is BannerViewHolder) {
            holder.stopLoop()
        }
    }

    /**
     * Animate sentiment bar from startWidth to endWidth over 720ms with LinearInterpolator
     */
    private fun animateSentimentBar(
        highlightView: View,
        halfWidth: Int,
        score: Double,
        startWidth: Float,
        endWidth: Float
    ) {
        // Cancel any previous animations
        highlightView.clearAnimation()
        
        val lp = highlightView.layoutParams as ConstraintLayout.LayoutParams
        
        // Clamp score between -1.0 and 1.0
        val clampedScore = when {
            score > 1.0 -> 1.0
            score < -1.0 -> -1.0
            else -> score
        }
        
        // View should be GONE with width=1px from previous step
        // Change from GONE to INVISIBLE, keeping width at 1px initially
        highlightView.visibility = View.INVISIBLE
        lp.width = startWidth.toInt()  // Start from 1px (or whatever startWidth is)
        lp.marginStart = halfWidth
        highlightView.layoutParams = lp
        
        // Post to ensure layout is complete before starting animation
        highlightView.post {
            // Make visible and start animation from 1px to target width
            highlightView.visibility = View.VISIBLE
            
            // Animate using ValueAnimator over 720ms with LinearInterpolator (provides smooth counting effect)
            ValueAnimator.ofFloat(startWidth, endWidth).apply {
                duration = 720L
                interpolator = LinearInterpolator()
                addUpdateListener { animator ->
                    val animatedValue = animator.animatedValue as Float
                    val animatedDistance = animatedValue.toInt()
                    
                    if (animatedDistance > 0) {
                        lp.width = animatedDistance
                        // For positive: marginStart = half (extends right from center)
                        // For negative: marginStart = half - distance (extends left from center)
                        lp.marginStart = if (clampedScore > 0) {
                            halfWidth
                        } else {
                            halfWidth - animatedDistance
                        }
                        highlightView.layoutParams = lp
                    }
                }
                start()
            }
        }
    }

    inner class NormalCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val placeTv: TextView = itemView.findViewById(R.id.place_tv)
        private val tagTv: TextView = itemView.findViewById(R.id.tag_tv)
        private val titleTv: TextView = itemView.findViewById(R.id.news_title_tv)
        private val newsIv: ImageView = itemView.findViewById(R.id.news_iv)
        private val trackView: View = itemView.findViewById(R.id.progress_track)
        private val highlightView: View = itemView.findViewById(R.id.progress_highlight)
        private val timeTv: TextView = itemView.findViewById(R.id.news_time_tv)
        private val countTv: TextView = itemView.findViewById(R.id.news_count_tv)
        private val transScoreTv: TextView = itemView.findViewById(R.id.trans_score_tv)

        @SuppressLint("SetTextI18n")
        fun bind(article: HomeDataListEntry.DataDTO.ArticlesDTO, position: Int) {
            // Adjust top padding for first item on Today tab (reduce by 8dp)
            if (isTodayTab && position == 0) {
                itemView.setPadding(
                    defaultPadding,
                    reducedPadding,
                    defaultPadding,
                    defaultPadding
                )
            } else {
                itemView.setPadding(
                    defaultPadding,
                    defaultPadding,
                    defaultPadding,
                    defaultPadding
                )
            }
            // IMMEDIATELY reset sentiment bar to prevent flash from recycled views
            // Use View.GONE instead of INVISIBLE to prevent any layout space allocation
            // and set width to 1px (not 0) to avoid MATCH_CONSTRAINT behavior
            highlightView.clearAnimation()
            highlightView.visibility = View.GONE
            val lpReset = highlightView.layoutParams as ConstraintLayout.LayoutParams
            lpReset.width = 1  // Use 1px instead of 0 to avoid ConstraintLayout MATCH_CONSTRAINT behavior
            lpReset.marginStart = 0
            highlightView.layoutParams = lpReset
            
            placeTv.text = article.region
            val sentimentText = context.getString(CalculateUtil.getSentimentLabelResId(article.metrics.sentiment))
            val sentimentScore = article.metrics.sentiment
            
            // Set sentiment text without "Sentiment:" prefix
            if (sentimentScore > 0.1 || sentimentScore < -0.1) {
                // Apply colorization for significant positive/negative sentiment
                val spannableString = SpannableString(sentimentText)
                val colorResId = context.resources.getIdentifier(CalculateUtil.getSentimentColorName(sentimentScore), "color", context.packageName)
                val sentimentColor = ContextCompat.getColor(context, colorResId)
                spannableString.setSpan(ForegroundColorSpan(sentimentColor), 0, sentimentText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                transScoreTv.text = spannableString
            } else {
                // No colorization for neutral sentiment
                transScoreTv.text = sentimentText
            }
            
            // Localize "General" topic
            if (article.sector != null && article.sector.equals("General", ignoreCase = true)) {
                tagTv.text = context.getString(R.string.topic_general)
            } else {
                tagTv.text = article.sector
            }
            titleTv.text = article.title
            Glide.with(context).load(article.pictureURL)
                .placeholder(R.drawable.ic_image_not_supported_24)
                .error(R.drawable.ic_image_not_supported_24)
                .into(newsIv)

            // Format date using backend date string directly
            timeTv.text = Utils.formatBackendDate(context, article.date)
            
            countTv.text = context.getString(R.string.reports_count, article.nSources)
            
            // Set up sentiment progress bar with animation
            trackView.post {
                val totalWidth = trackView.width
                val half = totalWidth / 2
                val score = sentimentScore  // Use score directly like YourFeedAdapter
                val targetDistance = (abs(score) * half).toInt()
                val lp = highlightView.layoutParams as ConstraintLayout.LayoutParams
                
                if (targetDistance <= 0) {
                    // Keep view GONE for zero distance
                    highlightView.visibility = View.GONE
                    lp.width = 1
                    lp.marginStart = half
                    highlightView.layoutParams = lp
                } else {
                    // Ensure view is GONE and width is 1px before setting up animation
                    highlightView.clearAnimation()
                    highlightView.visibility = View.GONE
                    lp.width = 1  // Use 1px to avoid MATCH_CONSTRAINT, will animate from here
                    lp.marginStart = half
                    highlightView.setBackgroundResource(
                        if (score > 0.1) R.drawable.bg_progress_positive 
                        else if (score < -0.1) R.drawable.bg_progress_negative
                        else R.drawable.bg_progress_neutral
                    )
                    highlightView.layoutParams = lp
                    
                    // Wait one frame to ensure GONE state is applied
                    highlightView.post {
                        animateSentimentBar(highlightView, half, score, 1f, targetDistance.toFloat())
                    }
                }
            }

            // Set click listeners
            itemView.setOnClickListener {
                val intent = Intent(context, NewsDetailActivity::class.java)
                intent.putExtra("id", article.articleID)
                intent.putExtra("source_fragment", "home")
                context.startActivity(intent)
            }

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
                
                onShareArticle(article)
                true
            }
        }
    }

    inner class BigCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val placeTv: TextView = itemView.findViewById(R.id.place_tv)
        private val tagTv: TextView = itemView.findViewById(R.id.tag_tv)
        private val titleTv: TextView = itemView.findViewById(R.id.news_title_tv)
        private val featuredImage: ImageView = itemView.findViewById(R.id.featured_image)
        private val trackView: View = itemView.findViewById(R.id.progress_track)
        private val highlightView: View = itemView.findViewById(R.id.progress_highlight)
        private val timeTv: TextView = itemView.findViewById(R.id.news_time_tv)
        private val countTv: TextView = itemView.findViewById(R.id.news_count_tv)
        private val transScoreTv: TextView = itemView.findViewById(R.id.trans_score_tv)
        private val recommendedHintRow: View = itemView.findViewById(R.id.recommended_hint_row)

        @SuppressLint("SetTextI18n")
        fun bind(article: HomeDataListEntry.DataDTO.ArticlesDTO, position: Int) {
            // Adjust top padding for first item on Today tab
            if (isTodayTab && position == 0) {
                itemView.setPadding(
                    defaultPadding,
                    reducedPadding,
                    defaultPadding,
                    defaultPadding
                )
            } else {
                itemView.setPadding(
                    defaultPadding,
                    defaultPadding,
                    defaultPadding,
                    defaultPadding
                )
            }
            // IMMEDIATELY reset sentiment bar state to prevent flash from recycled views
            // Use View.GONE instead of INVISIBLE to prevent any layout space allocation
            // and set width to 1px (not 0) to avoid MATCH_CONSTRAINT behavior
            val lpReset = highlightView.layoutParams as ConstraintLayout.LayoutParams
            highlightView.clearAnimation()
            highlightView.visibility = View.GONE
            lpReset.width = 1  // Use 1px instead of 0 to avoid ConstraintLayout MATCH_CONSTRAINT behavior
            lpReset.marginStart = 0
            highlightView.layoutParams = lpReset
            
            placeTv.text = article.region
            val sentimentText = context.getString(CalculateUtil.getSentimentLabelResId(article.metrics.sentiment))
            val sentimentScore = article.metrics.sentiment
            
            // Set sentiment text without "Sentiment:" prefix
            if (sentimentScore > 0.1 || sentimentScore < -0.1) {
                // Apply colorization for significant positive/negative sentiment
                val spannableString = SpannableString(sentimentText)
                val colorResId = context.resources.getIdentifier(CalculateUtil.getSentimentColorName(sentimentScore), "color", context.packageName)
                val sentimentColor = ContextCompat.getColor(context, colorResId)
                spannableString.setSpan(ForegroundColorSpan(sentimentColor), 0, sentimentText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                transScoreTv.text = spannableString
            } else {
                // No colorization for neutral sentiment
                transScoreTv.text = sentimentText
            }
            
            // Localize "General" topic
            if (article.sector != null && article.sector.equals("General", ignoreCase = true)) {
                tagTv.text = context.getString(R.string.topic_general)
            } else {
                tagTv.text = article.sector
            }
            titleTv.text = article.title
            Glide.with(context).load(article.pictureURL)
                .placeholder(R.drawable.ic_image_not_supported_24)
                .error(R.drawable.ic_image_not_supported_24)
                .into(featuredImage)

            // Format date using backend date string directly
            timeTv.text = Utils.formatBackendDate(context, article.date)
            
            countTv.text = context.getString(R.string.reports_count, article.nSources)
            
            // Set up sentiment progress bar with animation
            trackView.post {
                val totalWidth = trackView.width
                val half = totalWidth / 2
                val score = sentimentScore  // Use score directly like YourFeedAdapter
                val targetDistance = (abs(score) * half).toInt()
                val lp = highlightView.layoutParams as ConstraintLayout.LayoutParams
                
                if (targetDistance <= 0) {
                    // Keep view GONE for zero distance
                    highlightView.visibility = View.GONE
                    lp.width = 1
                    lp.marginStart = half
                    highlightView.layoutParams = lp
                } else {
                    // Ensure view is GONE and width is 1px before setting up animation
                    highlightView.clearAnimation()
                    highlightView.visibility = View.GONE
                    lp.width = 1  // Use 1px to avoid MATCH_CONSTRAINT, will animate from here
                    lp.marginStart = half
                    highlightView.setBackgroundResource(
                        if (score > 0.1) R.drawable.bg_progress_positive 
                        else if (score < -0.1) R.drawable.bg_progress_negative
                        else R.drawable.bg_progress_neutral
                    )
                    highlightView.layoutParams = lp
                    
                    // Wait one frame to ensure GONE state is applied
                    highlightView.post {
                        animateSentimentBar(highlightView, half, score, 1f, targetDistance.toFloat())
                    }
                }
            }

            // Show recommended hint for big cards (optional)
            recommendedHintRow.visibility = View.GONE

            // Set click listeners
            itemView.setOnClickListener {
                val intent = Intent(context, NewsDetailActivity::class.java)
                intent.putExtra("id", article.articleID)
                intent.putExtra("source_fragment", "home")
                context.startActivity(intent)
            }

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
                
                onShareArticle(article)
                true
            }
        }
    }
    
    // Footer ViewHolder for loading indicator
    inner class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            val progressBar: android.widget.ProgressBar = itemView.findViewById(R.id.loading_progress)
            try {
                // Use CircularProgressDrawable for standard multi-colored Android spinner
                val drawable = androidx.swiperefreshlayout.widget.CircularProgressDrawable(itemView.context)
                drawable.setStyle(androidx.swiperefreshlayout.widget.CircularProgressDrawable.DEFAULT)
                
                // Calculate dimensions in pixels based on density
                val density = itemView.context.resources.displayMetrics.density
                drawable.strokeWidth = 3f * density // 3dp stroke
                drawable.centerRadius = 12f * density // 12dp radius (24dp diam) -> Fits in 36dp
                
                drawable.setColorSchemeColors(
                    android.graphics.Color.parseColor("#4285F4"), // Blue
                    android.graphics.Color.parseColor("#DB4437"), // Red
                    android.graphics.Color.parseColor("#F4B400"), // Yellow
                    android.graphics.Color.parseColor("#0F9D58")  // Green
                )
                drawable.start()
                progressBar.indeterminateDrawable = drawable
            } catch (e: Exception) {
                // Fallback to default if library missing
                e.printStackTrace()
            }
        }
    }
    
    // Banner ViewHolder
    inner class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bannerView: BannerViewPager<HomeDataListEntry.DataDTO.HeadlinesDTO, BannerNetViewHolder> = 
            itemView.findViewById(R.id.homeBanner)

        fun bind() {
            // Only set up and create the banner when there's data to display
            if (bannerData.isEmpty()) {
                return
            }
            
            bannerAdapter?.let { adapter ->
                if (bannerView.adapter == null) {
                    bannerView.adapter = adapter
                }
            }
            
            onBannerClick?.let { clickHandler ->
                bannerView.setOnPageClickListener { position ->
                    if (position < bannerData.size) {
                        clickHandler(bannerData[position])
                    }
                }
            }
            
            bannerView.create()
            bannerView.refreshData(bannerData)
        }

        fun startLoop() {
            bannerView.startLoop()
        }

        fun stopLoop() {
            bannerView.stopLoop()
        }
    }
    
    // Banner page ViewHolder for BannerViewPager
    class BannerNetViewHolder(itemView: View) :
        BaseViewHolder<HomeDataListEntry.DataDTO.HeadlinesDTO>(itemView) {
        private val mBannerIv: ImageView = itemView.findViewById(R.id.banner_image)
        private val mTitleTv: TextView = itemView.findViewById(R.id.banner_title_tv)
        private val mTransTv: TextView = itemView.findViewById(R.id.banner_desc_tv)
        
        override fun bindData(
            data: HomeDataListEntry.DataDTO.HeadlinesDTO,
            position: Int,
            pageSize: Int
        ) {
            Glide.with(itemView.context).load(data.pictureURL)
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
                
                shareHeadline(itemView.context, data)
                true
            }
        }

        private fun shareHeadline(context: android.content.Context, headline: HomeDataListEntry.DataDTO.HeadlinesDTO) {
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
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.app_name)))
        }
    }
    
    // Banner adapter for BannerViewPager
    class BannerPagerAdapter :
        BaseBannerAdapter<HomeDataListEntry.DataDTO.HeadlinesDTO, BannerNetViewHolder>() {
        override fun onBind(
            holder: BannerNetViewHolder,
            data: HomeDataListEntry.DataDTO.HeadlinesDTO,
            position: Int,
            pageSize: Int
        ) {
            holder.bindData(data, position, pageSize)
        }

        override fun createViewHolder(itemView: View, viewType: Int): BannerNetViewHolder {
            return BannerNetViewHolder(itemView)
        }

        override fun getLayoutId(viewType: Int): Int {
            return R.layout.item_banner
        }
    }
}
