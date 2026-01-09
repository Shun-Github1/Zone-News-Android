package com.anssy.znewspro.ui.mainfrag

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
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.anssy.znewspro.R
import com.anssy.znewspro.entry.SearchListEntry
import com.anssy.znewspro.ui.newsdetail.NewsDetailActivity
import com.anssy.znewspro.utils.CalculateUtil
import com.anssy.znewspro.utils.Utils
import com.bumptech.glide.Glide
import kotlin.math.abs

class YourFeedAdapter(
    private val context: android.content.Context,
    private val newsList: MutableList<SearchListEntry.DataDTO.ArticlesDTO>,
    private val onShareArticle: (SearchListEntry.DataDTO.ArticlesDTO) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_BIG = 1
        private const val BIG_CARD_INTERVAL = 5 // Show big card every 5 cards (4 normal + 1 big)
        private const val BIG_CARD_OFFSET = 4 // Show big card at position 4 (5th card: 0,1,2,3,4)
    }

    override fun getItemViewType(position: Int): Int {
        // Show big card at positions 4, 9, 14, 19, etc. (one every 4 normal cards)
        return if ((position - BIG_CARD_OFFSET) % BIG_CARD_INTERVAL == 0 && position >= BIG_CARD_OFFSET) VIEW_TYPE_BIG else VIEW_TYPE_NORMAL
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_BIG -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_big_news_card, parent, false)
                BigCardViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_recommended_news, parent, false)
                NormalCardViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val article = newsList[position]
        
        when (holder) {
            is NormalCardViewHolder -> holder.bind(article)
            is BigCardViewHolder -> holder.bind(article)
        }
    }

    override fun getItemCount(): Int = newsList.size

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
            tagTv.text = article.sector ?: ""
            
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
                        if (score > 0) R.drawable.bg_progress_positive else R.drawable.bg_progress_negative
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
            
            tagTv.text = article.sector ?: ""
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
                        if (score > 0) R.drawable.bg_progress_positive else R.drawable.bg_progress_negative
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
}

