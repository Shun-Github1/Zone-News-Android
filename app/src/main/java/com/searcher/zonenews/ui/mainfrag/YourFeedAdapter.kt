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
import com.bumptech.glide.Glide
import kotlin.math.abs

class YourFeedAdapter(
    private val context: android.content.Context,
    private val newsList: MutableList<SearchListEntry.DataDTO.ArticlesDTO>,
    private val onShareArticle: (SearchListEntry.DataDTO.ArticlesDTO) -> Unit,
    private val headerCallback: HeaderCallback? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Header callback interface
    interface HeaderCallback {
        fun onSortClick(anchor: View)
        fun onManageTagsClick()
        fun onTagClick(tag: TopicListEntry.TopicDTO)
    }
    
    // Header state
    private var currentSort: SortPopupWindow.SortOption = SortPopupWindow.SortOption.LATEST
    private var activeTags: List<TopicListEntry.TopicDTO> = emptyList()
    
    fun updateSort(sort: SortPopupWindow.SortOption) {
        currentSort = sort
        notifyItemChanged(0)
    }
    
    fun updateTags(tags: List<TopicListEntry.TopicDTO>) {
        activeTags = tags
        notifyItemChanged(0)
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
        private val aiIcon: ImageView = itemView.findViewById(R.id.ai_icon)
        private val recommendedText: TextView = itemView.findViewById(R.id.recommended_text)

        @SuppressLint("SetTextI18n")
        fun bind(article: SearchListEntry.DataDTO.ArticlesDTO) {
            // IMMEDIATELY reset sentiment bar state to prevent flash from recycled views
            // Use View.GONE instead of INVISIBLE to prevent any layout space allocation
            // and set width to 1px (not 0) to avoid MATCH_CONSTRAINT behavior
            val lpReset = highlightView.layoutParams as ConstraintLayout.LayoutParams
            highlightView.clearAnimation()
            highlightView.visibility = View.GONE
            lpReset.width = 1  // Use 1px instead of 0 to avoid ConstraintLayout MATCH_CONSTRAINT behavior
            lpReset.marginStart = 0
            highlightView.layoutParams = lpReset
            
            placeTv.text = article.region ?: ""
            placeTv.text = article.region ?: ""
            
            // Localize "General" topic
            if (article.sector != null && article.sector.equals("General", ignoreCase = true)) {
                tagTv.text = context.getString(R.string.topic_general)
            } else {
                tagTv.text = article.sector ?: ""
            }
            
            aiIcon.visibility = View.VISIBLE
            recommendedText.visibility = View.VISIBLE
            
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
            
            Glide.with(context)
                .load(article.pictureURL)
                .placeholder(R.drawable.ic_image_not_supported_24)
                .error(R.drawable.ic_image_not_supported_24)
                .into(newsIv)

            trackView.post {
                val totalWidth = trackView.width
                val half = totalWidth / 2
                val score = sentimentScore
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

            // Format date using backend date string directly
            timeTv.text = Utils.formatBackendDate(context, article.date)

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
        private val recommendedHintRow: View = itemView.findViewById(R.id.recommended_hint_row)

        @SuppressLint("SetTextI18n")
        fun bind(article: SearchListEntry.DataDTO.ArticlesDTO) {
            // IMMEDIATELY reset sentiment bar state to prevent flash from recycled views
            // Use View.GONE instead of INVISIBLE to prevent any layout space allocation
            // and set width to 1px (not 0) to avoid MATCH_CONSTRAINT behavior
            val lpReset = highlightView.layoutParams as ConstraintLayout.LayoutParams
            highlightView.clearAnimation()
            highlightView.visibility = View.GONE
            lpReset.width = 1  // Use 1px instead of 0 to avoid ConstraintLayout MATCH_CONSTRAINT behavior
            lpReset.marginStart = 0
            highlightView.layoutParams = lpReset
            
            placeTv.text = article.region ?: ""
            val sentimentScore = article.metrics?.sentiment ?: 0.0
            val sentimentText = context.getString(CalculateUtil.getSentimentLabelResId(sentimentScore))
            
            // Set sentiment text without "Sentiment:" prefix
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
                tagTv.text = article.sector ?: ""
            }
            titleTv.text = article.title
            Glide.with(context).load(article.pictureURL).error(R.drawable.ic_image_not_supported_24)
                .into(featuredImage)

            // Format date using backend date string directly
            timeTv.text = Utils.formatBackendDate(context, article.date)
            
            countTv.text = context.getString(R.string.reports_count, article.nSources)
            
            // Set up sentiment progress bar with animation
            trackView.post {
                val totalWidth = trackView.width
                val half = totalWidth / 2
                val score = CalculateUtil.round(sentimentScore, 2)
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

        init {
            sortChip.setOnClickListener {
                headerCallback?.onSortClick(sortChip)
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
            // Always add manage chevron at the end
            addManageChip(ctx)
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
            selectTagsText.setTextColor(ctx.getColor(R.color.colorTextMiddle))
            selectTagsIcon.setImageResource(R.drawable.shoppingmode_24)
            selectTagsIcon.setColorFilter(ctx.getColor(R.color.colorTextMiddle))

            (selectTagsChip.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                lp.bottomMargin = 0
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
                R.layout.item_tag_chip,
                activeTagsChipsContainer,
                false
            )
            val tagIcon = chipView.findViewById<ImageView>(R.id.tag_icon)
            val tagText = chipView.findViewById<TextView>(R.id.tag_text)

            if (tag.tag != null && tag.tag.equals("General", ignoreCase = true)) {
                tagText.text = ctx.getString(R.string.topic_general)
            } else {
                tagText.text = tag.displayName
            }
            tagIcon.setImageResource(getTagIcon(tag.tag))

            (chipView.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                lp.bottomMargin = 0
                lp.gravity = android.view.Gravity.CENTER_VERTICAL
                chipView.layoutParams = lp
            }

            chipView.setOnClickListener {
                headerCallback?.onManageTagsClick()
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
