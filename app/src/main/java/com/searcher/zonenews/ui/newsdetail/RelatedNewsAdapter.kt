package com.searcher.zonenews.ui.newsdetail

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.searcher.zonenews.R
import com.searcher.zonenews.entry.ArticleDetailEntry

/**
 * Adapter for displaying related news articles in a horizontal RecyclerView.
 */
class RelatedNewsAdapter(
    private val context: Context,
    private val articles: List<ArticleDetailEntry.DataDTO.RelatedArticlesDTO>,
    private val onArticleClick: (ArticleDetailEntry.DataDTO.RelatedArticlesDTO) -> Unit
) : RecyclerView.Adapter<RelatedNewsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val thumbnail: ImageView = itemView.findViewById(R.id.related_news_item_thumbnail)
        val title: TextView = itemView.findViewById(R.id.related_news_item_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_related_news, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article = articles[position]
        
        // Set title
        holder.title.text = article.title
        
        // Load thumbnail if available
        val pictureUrl = article.pictureURL
        if (!pictureUrl.isNullOrEmpty()) {
            holder.thumbnail.visibility = View.VISIBLE
            Glide.with(context)
                .load(pictureUrl)
                .apply(
                    RequestOptions()
                        .transform(RoundedCorners(dpToPx(8)))
                        .placeholder(R.drawable.ic_image_not_supported_24)
                        .error(R.drawable.ic_image_not_supported_24)
                )
                .into(holder.thumbnail)
        } else {
            holder.thumbnail.visibility = View.GONE
        }
        
        // Set click listener
        holder.itemView.setOnClickListener {
            onArticleClick(article)
        }
    }

    override fun getItemCount(): Int = articles.size

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
