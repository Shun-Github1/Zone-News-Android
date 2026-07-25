package com.searcher.zonenews.ui.mainfrag

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.searcher.zonenews.R
import com.searcher.zonenews.entry.SearchListEntry
import com.searcher.zonenews.entry.TopicListEntry
import com.searcher.zonenews.selfview.popup.SortPopupWindow
import com.searcher.zonenews.ui.newsdetail.NewsDetailActivity
import com.searcher.zonenews.utils.CalculateUtil
import com.searcher.zonenews.utils.Utils
import com.searcher.zonenews.utils.ImageCacheManager
import com.bumptech.glide.Glide
import com.bumptech.glide.ListPreloader
import com.bumptech.glide.RequestBuilder
import kotlin.math.abs

class YourFeedAdapter(
    private val context: android.content.Context,
    private val newsList: MutableList<SearchListEntry.DataDTO.ArticlesDTO>,
    private val onShareArticle: (SearchListEntry.DataDTO.ArticlesDTO) -> Unit,
    private val headerCallback: HeaderCallback? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), ListPreloader.PreloadModelProvider<SearchListEntry.DataDTO.ArticlesDTO> {

    override fun getPreloadItems(position: Int): List<SearchListEntry.DataDTO.ArticlesDTO> {
        val articlePosition = getArticlePosition(position)
        return if (articlePosition >= 0 && articlePosition < newsList.size) {
            listOf(newsList[articlePosition])
        } else {
            emptyList()
        }
    }

    override fun getPreloadRequestBuilder(item: SearchListEntry.DataDTO.ArticlesDTO): RequestBuilder<*>? {
        return Glide.with(context).asBitmap().load(item.pictureURL)
    }

    // Header callback interface
    interface HeaderCallback {
        fun onSortClick(anchor: View)
        fun onManageTagsClick()
        fun onTagClick(tag: TopicListEntry.TopicDTO)
        fun onTagUnselected(tag: TopicListEntry.TopicDTO)
        fun onFollowTag(tag: String) // Added for following tags from cards
    }
    
    // Header state
    private var currentSort: SortPopupWindow.SortOption = SortPopupWindow.SortOption.LATEST
    private var activeTags: List<TopicListEntry.TopicDTO> = emptyList()
    
    fun updateSort(sort: SortPopupWindow.SortOption) {
        currentSort = sort
        notifyItemChanged(0)
    }
    
    @SuppressLint("NotifyDataSetChanged")
    fun updateTags(tags: List<TopicListEntry.TopicDTO>) {
        this.activeTags = tags
        notifyDataSetChanged()
    }
    
    private fun hasHeader(): Boolean = headerCallback != null
    
    private fun getArticlePosition(adapterPosition: Int): Int {
        return if (hasHeader()) adapterPosition - 1 else adapterPosition
    }

    companion object {
        private const val VIEW_TYPE_HEADER = -1
        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_BIG = 1
        private const val VIEW_TYPE_FOOTER = 2
        private const val BIG_CARD_INTERVAL = 5 // Show big card every 5 cards (4 normal + 1 big)
        private const val BIG_CARD_OFFSET = 4 // Show big card at position 4 (5th card: 0,1,2,3,4)
    }

    override fun getItemViewType(position: Int): Int {
        // Header is always at position 0 when present
        if (hasHeader() && position == 0) {
            return VIEW_TYPE_HEADER
        }
        
        // Footer is always at the last position
        val footerPosition = newsList.size + (if (hasHeader()) 1 else 0)
        if (position == footerPosition) {
            return VIEW_TYPE_FOOTER
        }
        
        // Adjust position for article view types
        val articlePosition = getArticlePosition(position)
        
        // Show big card at positions 4, 9, 14, 19, etc. (one every 4 normal cards)
        return if ((articlePosition - BIG_CARD_OFFSET) % BIG_CARD_INTERVAL == 0 && articlePosition >= BIG_CARD_OFFSET) VIEW_TYPE_BIG else VIEW_TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_your_feed_header, parent, false)
                HeaderViewHolder(view)
            }
            VIEW_TYPE_BIG -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_big_news_card_your_feed, parent, false)
                BigCardViewHolder(view)
            }
            VIEW_TYPE_FOOTER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_loading_footer, parent, false)
                FooterViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_recommended_news, parent, false)
                NormalCardViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> holder.bind(currentSort, activeTags)
            is FooterViewHolder -> { /* Footer is static, no binding needed */ }
            else -> {
                val articlePosition = getArticlePosition(position)
                if (articlePosition >= 0 && articlePosition < newsList.size) {
                    val article = newsList[articlePosition]
                    when (holder) {
                        is NormalCardViewHolder -> holder.bind(article)
                        is BigCardViewHolder -> holder.bind(article)
                    }
                }
            }
        }
    }

    // +1 for footer, +1 for header if present
    override fun getItemCount(): Int = newsList.size + 1 + if (hasHeader()) 1 else 0

    fun updateList(newList: List<SearchListEntry.DataDTO.ArticlesDTO>) {
        newsList.clear()
        newsList.addAll(newList)
        notifyDataSetChanged()
    }

    /**
     * Animate sentiment bar using GPU-accelerated transformations (Scale/Translation)
     * This avoids expensive layout passes and ensures buttery-smooth scrolling.
     */
    private fun animateSentimentBar(
        highlightView: View,
        halfWidth: Int,
        score: Double
    ) {
        // Cancel any previous animations and reset state
        highlightView.animate().cancel()
        
        // Clamp score between -1.0 and 1.0
        val clampedScore = when {
            score > 1.0 -> 1.0
            score < -1.0 -> -1.0
            else -> score
        }
        
        val absScore = kotlin.math.abs(clampedScore).toFloat()
        
        // Setup initial state for GPU transformation
        highlightView.visibility = View.VISIBLE
        highlightView.alpha = 0f // Start invisible for a smooth fade-in
        
        // Setup pivot and translation based on sentiment direction
        if (clampedScore >= 0) {
            highlightView.pivotX = 0f // Center point is left edge of view
            highlightView.translationX = 0f // Stay in right half
        } else {
            highlightView.pivotX = halfWidth.toFloat() // Center point is right edge of view
            highlightView.translationX = -halfWidth.toFloat() // Move right-half view to left-half position
        }

        // Animate Scale and Alpha simultaneously (GPU handles this, zero UI lag)
        highlightView.scaleX = 0f // Start from zero width
        highlightView.animate()
            .scaleX(absScore)
            .alpha(1f)
            .setDuration(450) // Slightly faster for snappier feel
            .setInterpolator(LinearInterpolator())
            .start()
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
        private val cardTagsContainer: LinearLayout = itemView.findViewById(R.id.card_tags_container)
        private var currentArticleId: String? = null

        @SuppressLint("SetTextI18n")
        fun bind(article: SearchListEntry.DataDTO.ArticlesDTO) {
            placeTv.text = article.region ?: ""
            
            // Localize "General" topic
            if (article.sector != null && article.sector.equals("General", ignoreCase = true)) {
                tagTv.text = context.getString(R.string.topic_general)
            } else {
                tagTv.text = article.sector ?: ""
            }
            
            titleTv.text = article.title
            countTv.text = context.getString(R.string.reports_count, article.nSources)
            
            val sentimentScore = article.metrics?.sentiment ?: 0.0
            val sentimentText = context.getString(CalculateUtil.getSentimentLabelResId(sentimentScore))
            
            if (sentimentScore > 0.1 || sentimentScore < -0.1) {
                val spannableString = SpannableString(sentimentText)
                val colorResId = context.resources.getIdentifier(CalculateUtil.getSentimentColorName(sentimentScore), "color", context.packageName)
                val sentimentColor = ContextCompat.getColor(context, colorResId)
                spannableString.setSpan(ForegroundColorSpan(sentimentColor), 0, sentimentText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                transScoreTv.text = spannableString
            } else {
                transScoreTv.text = sentimentText
            }
            
            // Use manual persistent cache for instant appearance
            val cachedBitmap = ImageCacheManager.get(article.pictureURL)
            if (cachedBitmap != null) {
                newsIv.setImageBitmap(cachedBitmap)
            }
            
            Glide.with(context).asBitmap().load(article.pictureURL)
                .placeholder(R.drawable.ic_image_not_supported_24)
                .error(R.drawable.ic_image_not_supported_24)
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.Bitmap> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.Bitmap>?,
                        isFirstResource: Boolean
                    ): Boolean = false
                    
                    override fun onResourceReady(
                        resource: android.graphics.Bitmap?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.Bitmap>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        resource?.let { ImageCacheManager.put(article.pictureURL, it) }
                        return false
                    }
                })
                .into(newsIv)

            // Reset and animate sentiment bar only if the article has changed
            if (currentArticleId != article.articleID) {
                currentArticleId = article.articleID
                
                highlightView.animate().cancel()
                highlightView.visibility = View.INVISIBLE
                highlightView.scaleX = 0f
                highlightView.alpha = 0f
                val lpReset = highlightView.layoutParams as ConstraintLayout.LayoutParams
                lpReset.width = 0
                highlightView.layoutParams = lpReset

                // Set up sentiment progress bar with optimized GPU animation
                trackView.post {
                    val totalWidth = trackView.width
                    val half = totalWidth / 2
                    val score = sentimentScore
                    
                    // Set the highlightView's layout width to exactly half (the max it can be)
                    val lp = highlightView.layoutParams as ConstraintLayout.LayoutParams
                    lp.width = half
                    lp.marginStart = half
                    highlightView.layoutParams = lp
                    
                    if (kotlin.math.abs(score) > 0.01) {
                        highlightView.setBackgroundResource(
                            if (score > 0.1) R.drawable.bg_progress_positive 
                            else if (score < -0.1) R.drawable.bg_progress_negative
                            else R.drawable.bg_progress_neutral
                        )
                        animateSentimentBar(highlightView, half, score)
                    } else {
                        highlightView.visibility = View.INVISIBLE
                    }
                }
            }

            // Format date using backend date string directly
            timeTv.text = Utils.formatBackendDate(context, article.date)

            // Handle Tags
            populateCardTags(cardTagsContainer, article.sector, article.region, article.topics)

            itemView.setOnClickListener {
                val intent = Intent(context, NewsDetailActivity::class.java)
                intent.putExtra("id", article.articleID)
                intent.putExtra("source_fragment", "special")
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
        private val cardTagsContainer: LinearLayout = itemView.findViewById(R.id.card_tags_container)
        private var currentArticleId: String? = null

        @SuppressLint("SetTextI18n")
        fun bind(article: SearchListEntry.DataDTO.ArticlesDTO) {
            placeTv.text = article.region ?: ""
            val sentimentScore = article.metrics?.sentiment ?: 0.0
            val sentimentText = context.getString(CalculateUtil.getSentimentLabelResId(sentimentScore))
            
            if (sentimentScore > 0.1 || sentimentScore < -0.1) {
                val spannableString = SpannableString(sentimentText)
                val colorResId = context.resources.getIdentifier(CalculateUtil.getSentimentColorName(sentimentScore), "color", context.packageName)
                val sentimentColor = ContextCompat.getColor(context, colorResId)
                spannableString.setSpan(ForegroundColorSpan(sentimentColor), 0, sentimentText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                transScoreTv.text = spannableString
            } else {
                transScoreTv.text = sentimentText
            }
            
            // Localize "General" topic
            if (article.sector != null && article.sector.equals("General", ignoreCase = true)) {
                tagTv.text = context.getString(R.string.topic_general)
            } else {
                tagTv.text = article.sector ?: ""
            }
            titleTv.text = article.title
            
            // Use manual persistent cache for instant appearance
            val cachedBitmap = ImageCacheManager.get(article.pictureURL)
            if (cachedBitmap != null) {
                featuredImage.setImageBitmap(cachedBitmap)
            }
            
            Glide.with(context).asBitmap().load(article.pictureURL)
                .placeholder(R.drawable.ic_image_not_supported_24)
                .error(R.drawable.ic_image_not_supported_24)
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.Bitmap> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.Bitmap>?,
                        isFirstResource: Boolean
                    ): Boolean = false
                    
                    override fun onResourceReady(
                        resource: android.graphics.Bitmap?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.Bitmap>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        resource?.let { ImageCacheManager.put(article.pictureURL, it) }
                        return false
                    }
                })
                .into(featuredImage)

            // Format date using backend date string directly
            timeTv.text = Utils.formatBackendDate(context, article.date)
            
            countTv.text = context.getString(R.string.reports_count, article.nSources)

            // Reset and animate sentiment bar only if the article has changed
            if (currentArticleId != article.articleID) {
                currentArticleId = article.articleID

                highlightView.animate().cancel()
                highlightView.visibility = View.INVISIBLE
                highlightView.scaleX = 0f
                highlightView.alpha = 0f
                val lpReset = highlightView.layoutParams as ConstraintLayout.LayoutParams
                lpReset.width = 0
                highlightView.layoutParams = lpReset

                // Set up sentiment progress bar with optimized GPU animation
                trackView.post {
                    val totalWidth = trackView.width
                    val half = totalWidth / 2
                    val score = CalculateUtil.round(sentimentScore, 2)
                    
                    // Set the highlightView's layout width to exactly half (the max it can be)
                    val lp = highlightView.layoutParams as ConstraintLayout.LayoutParams
                    lp.width = half
                    lp.marginStart = half
                    highlightView.layoutParams = lp
                    
                    if (kotlin.math.abs(score) > 0.01) {
                        highlightView.setBackgroundResource(
                            if (score > 0.1) R.drawable.bg_progress_positive 
                            else if (score < -0.1) R.drawable.bg_progress_negative
                            else R.drawable.bg_progress_neutral
                        )
                        animateSentimentBar(highlightView, half, score)
                    } else {
                        highlightView.visibility = View.INVISIBLE
                    }
                }
            }

            // Handle Tags
            populateCardTags(cardTagsContainer, article.sector, article.region, article.topics)

            // Set click listeners
            itemView.setOnClickListener {
                val intent = Intent(context, NewsDetailActivity::class.java)
                intent.putExtra("id", article.articleID)
                intent.putExtra("source_fragment", "special")
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

    private fun populateCardTags(container: LinearLayout, sector: String?, region: String?, topics: List<SearchListEntry.DataDTO.ArticlesDTO.TopicsDTO>?) {
        container.removeAllViews()
        
        // 1. Gather all unique tags with their priorities
        // Priority: 3=Trending Topic, 2=Region, 1=Sector
        data class TagCandidate(val id: String, val name: String, val priority: Int)
        val candidates = mutableMapOf<String, TagCandidate>()

        // Trending Topics (Priority 3)
        topics?.forEach { topic ->
            if (!topic.tag.isNullOrEmpty()) {
                candidates[topic.tag] = TagCandidate(topic.tag, topic.displayName ?: topic.tag, 3)
            }
        }

        // Regions (Priority 2)
        if (!region.isNullOrEmpty()) {
            if (!candidates.containsKey(region)) {
                candidates[region] = TagCandidate(region, region, 2)
            }
        }

        // Sectors (Priority 1)
        if (!sector.isNullOrEmpty()) {
            if (!candidates.containsKey(sector)) {
                candidates[sector] = TagCandidate(sector, sector, 1)
            }
        }

        // 2. Sort by Priority (Descending) and take top 2
        val sortedTags = candidates.values.sortedByDescending { it.priority }.take(2)

        sortedTags.forEach { candidate ->
            val tagId = candidate.id
            val displayName = candidate.name
            
            val isFollowed = activeTags.any { it.tag.equals(tagId, ignoreCase = true) || it.displayName.equals(displayName, ignoreCase = true) }
            
            val chipView = LayoutInflater.from(context).inflate(
                R.layout.item_followed_tag,
                container,
                false
            )
            val tagText = chipView.findViewById<TextView>(R.id.tag_text)
            val checkBtn = chipView.findViewById<LinearLayout>(R.id.tag_check_btn)
            val checkImg = checkBtn.getChildAt(0) as ImageView

            if (displayName.equals("General", ignoreCase = true)) {
                tagText.text = context.getString(R.string.topic_general)
            } else {
                tagText.text = displayName
            }

            if (isFollowed) {
                checkImg.setImageResource(R.drawable.ic_check)
                checkBtn.setOnClickListener {
                    val topic = activeTags.find { it.tag.equals(tagId, ignoreCase = true) || it.displayName.equals(displayName, ignoreCase = true) }
                    topic?.let { 
                        checkBtn.isEnabled = false
                        checkImg.animate().alpha(0f).setDuration(200).withEndAction {
                            headerCallback?.onTagUnselected(it)
                        }.start()
                    }
                }
            } else {
                checkImg.setImageResource(R.drawable.ic_add)
                checkBtn.setOnClickListener {
                    checkBtn.isEnabled = false
                    checkImg.animate().alpha(0f).setDuration(200).withEndAction {
                        headerCallback?.onFollowTag(tagId)
                    }.start()
                }
            }

            // Tag text opens the Tag News popup
            tagText.setOnClickListener {
                val topic = activeTags.find { it.tag.equals(tagId, ignoreCase = true) || it.displayName.equals(displayName, ignoreCase = true) }
                if (topic != null) {
                    headerCallback?.onTagClick(topic)
                } else {
                    // Create a dummy topic DTO to show news for non-followed tags
                    val dummyTopic = TopicListEntry.TopicDTO()
                    dummyTopic.tag = tagId
                    dummyTopic.displayName = displayName
                    headerCallback?.onTagClick(dummyTopic)
                }
            }

            container.addView(chipView)
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
    
    // Header ViewHolder
    inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val sortChip: LinearLayout = itemView.findViewById(R.id.sort_chip)
        private val sortValueTv: TextView = itemView.findViewById(R.id.sort_value_tv)
        private val sortInfoIcon: ImageView = itemView.findViewById(R.id.sort_info_icon)
        private val activeTagsChipsContainer: LinearLayout = itemView.findViewById(R.id.active_tags_chips_container)
        private val manageTagsBtn: LinearLayout = itemView.findViewById(R.id.manage_tags_btn)

        init {
            sortChip.setOnClickListener {
                headerCallback?.onSortClick(sortChip)
            }
            manageTagsBtn.setOnClickListener {
                headerCallback?.onManageTagsClick()
            }
        }

        fun bind(sort: SortPopupWindow.SortOption, tags: List<TopicListEntry.TopicDTO>) {
            // Update Sort
            val ctx = itemView.context
            val sortValue = when (sort) {
                SortPopupWindow.SortOption.LATEST -> ctx.getString(R.string.latest_sort_option)
                SortPopupWindow.SortOption.POPULAR -> ctx.getString(R.string.popular_sort_option)
                SortPopupWindow.SortOption.RELEVANT -> "Relevant"
            }
            sortValueTv.text = sortValue

            val sortIcon = when (sort) {
                SortPopupWindow.SortOption.LATEST -> R.drawable.ic_clock_24
                SortPopupWindow.SortOption.POPULAR -> R.drawable.ic_trending_24
                SortPopupWindow.SortOption.RELEVANT -> R.drawable.ic_star_24
            }
            sortInfoIcon.setImageResource(sortIcon)

            // Update Tags
            updateActiveTags(ctx, tags)
        }

        private fun updateActiveTags(ctx: android.content.Context, tags: List<TopicListEntry.TopicDTO>) {
            activeTagsChipsContainer.removeAllViews()

            if (tags.isEmpty()) {
                // "Select tags to follow" chip
                addSelectTagsChip(ctx)
            } else {
                // Add tag chips
                tags.forEach { tag ->
                    addTagChip(ctx, tag)
                }
            }
            // Managed by static button now
            // addManageChip(ctx)
        }
        
        private fun addSelectTagsChip(ctx: android.content.Context) {
            val selectTagsChip = LayoutInflater.from(ctx).inflate(
                R.layout.item_tag_chip,
                activeTagsChipsContainer,
                false
            )
            val selectTagsIcon = selectTagsChip.findViewById<ImageView>(R.id.tag_icon)
            val selectTagsText = selectTagsChip.findViewById<TextView>(R.id.tag_text)

            selectTagsText.text = ctx.getString(R.string.select_tags_to_follow)
            selectTagsText.setTextColor(ctx.getColor(R.color.colorTextDeep))
            selectTagsIcon.setImageResource(R.drawable.shoppingmode_24)
            selectTagsIcon.setColorFilter(ctx.getColor(R.color.colorTextDeep))
            
            // Remove background and padding to look like plain text
            selectTagsChip.background = null
            
            // Adjust padding to align with other tags
            // item_followed_tag has Root(2dp) + Text(6dp) = 8dp vertical padding
            val padV = Utils.dpToPx(8f, ctx.resources)
            val padStart = Utils.dpToPx(8f, ctx.resources)
            val padEnd = Utils.dpToPx(0f, ctx.resources)
            selectTagsChip.setPadding(padStart, padV, padEnd, padV)

            (selectTagsChip.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                lp.bottomMargin = Utils.dpToPx(4f, ctx.resources)
                lp.rightMargin = Utils.dpToPx(4f, ctx.resources)
                lp.gravity = android.view.Gravity.CENTER_VERTICAL
                selectTagsChip.layoutParams = lp
            }

            selectTagsChip.setOnClickListener {
                headerCallback?.onManageTagsClick()
            }

            activeTagsChipsContainer.addView(selectTagsChip)
        }
        
        private fun addTagChip(ctx: android.content.Context, tag: TopicListEntry.TopicDTO) {
            val chipView = LayoutInflater.from(ctx).inflate(
                R.layout.item_followed_tag,
                activeTagsChipsContainer,
                false
            )
            val tagText = chipView.findViewById<TextView>(R.id.tag_text)
            val checkBtn = chipView.findViewById<LinearLayout>(R.id.tag_check_btn)

            if (tag.tag != null && tag.tag.equals("General", ignoreCase = true)) {
                tagText.text = ctx.getString(R.string.topic_general)
            } else {
                tagText.text = tag.displayName
            }
            
            // Remove previous layout/margin setting code as margins are handled in xml now 
            // and gravity is handled by container

            checkBtn.setOnClickListener {
                checkBtn.isEnabled = false // Prevent double clicks
                // Animate fade out of the icon
                val icon = checkBtn.getChildAt(0)
                icon.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction {
                        headerCallback?.onTagUnselected(tag)
                    }
                    .start()
            }
            // Tag text opens the Tag News popup
             tagText.setOnClickListener {
                headerCallback?.onTagClick(tag)
            }

            activeTagsChipsContainer.addView(chipView)
        }
        
        private fun addManageChip(ctx: android.content.Context) {
            val manageView = LayoutInflater.from(ctx).inflate(
                R.layout.item_tag_chip,
                activeTagsChipsContainer,
                false
            )
            val manageIcon = manageView.findViewById<ImageView>(R.id.tag_icon)
            val manageText = manageView.findViewById<TextView>(R.id.tag_text)
            manageIcon.setImageResource(R.drawable.ic_chevron_right_24)
            manageIcon.setColorFilter(ctx.getColor(R.color.colorTextMiddle))
            manageText.visibility = View.GONE
            (manageIcon.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                lp.marginEnd = 0
                manageIcon.layoutParams = lp
            }
            manageView.background = ctx.getDrawable(R.drawable.tag_chip_background)
            val padV = Utils.dpToPx(2f, ctx.resources)
            val padH = Utils.dpToPx(4f, ctx.resources)
            manageView.setPadding(padH, padV, padH, padV)
            (manageView.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                lp.bottomMargin = 0
                lp.gravity = android.view.Gravity.CENTER_VERTICAL
                manageView.layoutParams = lp
            }
            manageView.setOnClickListener { headerCallback?.onManageTagsClick() }
            activeTagsChipsContainer.addView(manageView)
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
                else -> R.drawable.shoppingmode_24
            }
        }
    }
}
