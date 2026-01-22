package com.searcher.zonenews.ui.newsdetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.searcher.zonenews.R

class ScreenshotsCarouselAdapter(
    private val screenshotCount: Int = 4
) : RecyclerView.Adapter<ScreenshotsCarouselAdapter.ScreenshotViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScreenshotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_screenshot_carousel, parent, false)
        // Ensure the view fills the ViewPager2
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return ScreenshotViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScreenshotViewHolder, position: Int) {
        holder.bind(position + 1)
    }

    override fun getItemCount(): Int = screenshotCount

    class ScreenshotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerText: TextView = itemView.findViewById(R.id.screenshotHeaderText)
        private val placeholderText: TextView = itemView.findViewById(R.id.screenshotPlaceholderText)

        fun bind(screenshotNumber: Int) {
            val context = itemView.context
            val screenshotLabel = context.getString(R.string.subscription_screenshot)
            headerText.text = "$screenshotLabel $screenshotNumber"
            placeholderText.text = "$screenshotLabel $screenshotNumber"
        }
    }
}
