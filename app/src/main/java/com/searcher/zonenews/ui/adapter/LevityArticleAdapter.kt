package com.searcher.zonenews.ui.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.searcher.zonenews.R
import com.searcher.zonenews.entry.HomeDataListEntry
import com.searcher.zonenews.widget.CatMascotView

class LevityArticleAdapter(
    private val context: Context,
    private val articles: MutableList<HomeDataListEntry.DataDTO.ArticlesDTO>,
    private val onArticleClick: (HomeDataListEntry.DataDTO.ArticlesDTO) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_LOADING = 1
    }

    private var isLoading = false

    fun updateList(newArticles: List<HomeDataListEntry.DataDTO.ArticlesDTO>) {
        articles.clear()
        articles.addAll(newArticles)
        notifyDataSetChanged()
    }

    fun appendList(newArticles: List<HomeDataListEntry.DataDTO.ArticlesDTO>) {
        val start = articles.size
        articles.addAll(newArticles)
        notifyItemRangeInserted(start, newArticles.size)
    }

    fun setLoading(loading: Boolean) {
        if (isLoading == loading) return
        isLoading = loading
        if (isLoading) {
            notifyItemInserted(articles.size)
        } else {
            notifyItemRemoved(articles.size)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < articles.size) VIEW_TYPE_ITEM else VIEW_TYPE_LOADING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_ITEM) {
            val view = LayoutInflater.from(context).inflate(R.layout.item_levity_article, parent, false)
            ItemViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item_loading_footer, parent, false)
            LoadingViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ItemViewHolder) {
            holder.bind(articles[position])
        }
    }

    override fun getItemCount(): Int = articles.size + (if (isLoading) 1 else 0)

    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTv: TextView = itemView.findViewById(R.id.news_title_tv)
        private val newsIv: ImageView = itemView.findViewById(R.id.news_iv)
        private val mascotView: CatMascotView = itemView.findViewById(R.id.mascot_view)
        private val sentimentTv: TextView = itemView.findViewById(R.id.sentiment_tv)
        private val regionTv: TextView = itemView.findViewById(R.id.region_tv)

        fun bind(article: HomeDataListEntry.DataDTO.ArticlesDTO) {
            titleTv.text = article.getTitle()
            regionTv.text = "${article.getRegion()} | ${article.getSector()}"
            
            val sentiment = article.getMetrics()?.getSentiment() ?: 0.5
            mascotView.setSentiment(sentiment)

            sentimentTv.text = context.getString(when {
                sentiment > 0.38 -> R.string.sentiment_uplifting
                sentiment > 0.33 -> R.string.levity_sentiment_positive
                else -> R.string.sentiment_hopeful
            })
            
            Glide.with(context)
                .load(article.getPictureURL())
                .placeholder(R.drawable.ic_image_not_supported_24)
                .error(R.drawable.ic_image_not_supported_24)
                .into(newsIv)
                
            itemView.setOnClickListener {
                onArticleClick(article)
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
                
                shareArticle(context, article)
                true
            }
        }

        private fun shareArticle(context: Context, article: HomeDataListEntry.DataDTO.ArticlesDTO) {
            val shareText = buildString {
                append(article.getTitle())
                append("\n\n")
                append("https://zonenews.io/article/${article.getArticleID()}")
            }
            
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = "text/plain"
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.app_name)))
        }
    }
}
