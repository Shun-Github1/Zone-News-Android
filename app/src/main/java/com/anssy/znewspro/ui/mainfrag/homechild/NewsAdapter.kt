package com.anssy.znewspro.ui.mainfrag.homechild

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.anssy.znewspro.R
import com.anssy.znewspro.entry.HomeDataListEntry
import com.anssy.znewspro.ui.newsdetail.NewsDetailActivity
import com.anssy.znewspro.utils.CalculateUtil
import com.anssy.znewspro.utils.Utils
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Locale

class NewsAdapter(
    private val context: android.content.Context,
    private val newsList: MutableList<HomeDataListEntry.DataDTO.ArticlesDTO>,
    private val onShareArticle: (HomeDataListEntry.DataDTO.ArticlesDTO) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_NORMAL = 0
        private const val VIEW_TYPE_BIG = 1
        private const val BIG_CARD_INTERVAL = 6 // Show big card every 6 cards
        private const val BIG_CARD_OFFSET = 5 // Show big card at position 5 (6th card: 0,1,2,3,4,5)
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override fun getItemViewType(position: Int): Int {
        // Show big card at positions 5, 11, 17, 23, etc. (6th card in each cycle)
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
                    .inflate(R.layout.item_home_recycler, parent, false)
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
        fun bind(article: HomeDataListEntry.DataDTO.ArticlesDTO) {
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
            
            tagTv.text = article.sector
            titleTv.text = article.title
            Glide.with(context).load(article.pictureURL).error(R.drawable.ease_default_image)
                .into(newsIv)

            try {
                val parse = dateFormat.parse(article.date)
                timeTv.text = Utils.getMultilingualSpaceTime(context, parse!!.time)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            countTv.text = context.getString(R.string.reports_count, article.nSources)
            
            // Set up sentiment progress bar
            trackView.post {
                val totalWidth = trackView.width
                val half = totalWidth / 2
                val score = CalculateUtil.round(article.metrics.sentiment, 2)
                val distance = (kotlin.math.abs(score) * half).toInt()
                val lp = highlightView.layoutParams as ConstraintLayout.LayoutParams
                if (distance <= 0) {
                    // Avoid width==0 which equals MATCH_CONSTRAINT in ConstraintLayout
                    highlightView.visibility = View.INVISIBLE
                    lp.width = 1
                    lp.marginStart = half
                } else {
                    highlightView.visibility = View.VISIBLE
                    lp.width = distance
                    lp.marginStart = if (score > 0) half else (half - distance)
                }
                highlightView.layoutParams = lp
                highlightView.setBackgroundResource(
                    if (score > 0) R.drawable.bg_progress_positive else R.drawable.bg_progress_negative
                )
            }

            // Set click listeners
            itemView.setOnClickListener {
                val intent = Intent(context, NewsDetailActivity::class.java)
                intent.putExtra("id", article.articleID)
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
        fun bind(article: HomeDataListEntry.DataDTO.ArticlesDTO) {
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
            
            tagTv.text = article.sector
            titleTv.text = article.title
            Glide.with(context).load(article.pictureURL).error(R.drawable.ease_default_image)
                .into(featuredImage)

            try {
                val parse = dateFormat.parse(article.date)
                timeTv.text = Utils.getMultilingualSpaceTime(context, parse!!.time)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            countTv.text = context.getString(R.string.reports_count, article.nSources)
            
            // Set up sentiment progress bar
            trackView.post {
                val totalWidth = trackView.width
                val half = totalWidth / 2
                val score = CalculateUtil.round(article.metrics.sentiment, 2)
                val distance = (kotlin.math.abs(score) * half).toInt()
                val lp = highlightView.layoutParams as ConstraintLayout.LayoutParams
                if (distance <= 0) {
                    // Avoid width==0 which equals MATCH_CONSTRAINT in ConstraintLayout
                    highlightView.visibility = View.INVISIBLE
                    lp.width = 1
                    lp.marginStart = half
                } else {
                    highlightView.visibility = View.VISIBLE
                    lp.width = distance
                    lp.marginStart = if (score > 0) half else (half - distance)
                }
                highlightView.layoutParams = lp
                highlightView.setBackgroundResource(
                    if (score > 0) R.drawable.bg_progress_positive else R.drawable.bg_progress_negative
                )
            }

            // Show recommended hint for big cards (optional)
            recommendedHintRow.visibility = View.GONE

            // Set click listeners
            itemView.setOnClickListener {
                val intent = Intent(context, NewsDetailActivity::class.java)
                intent.putExtra("id", article.articleID)
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
