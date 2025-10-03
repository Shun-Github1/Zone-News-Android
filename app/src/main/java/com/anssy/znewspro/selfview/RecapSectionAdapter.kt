package com.anssy.znewspro.selfview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.anssy.znewspro.R

/**
 * Adapter for displaying recap sections in a RecyclerView.
 * Each section contains a header, optional date range, and numbered bullet points.
 */
class RecapSectionAdapter(
    private val sections: List<RecapSection>
) : RecyclerView.Adapter<RecapSectionAdapter.SectionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recap_section, parent, false)
        return SectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
        val section = sections[position]
        holder.bind(section, position == 0)
    }

    override fun getItemCount(): Int = sections.size

    class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerText: TextView = itemView.findViewById(R.id.recap_section_header)
        private val dateRangeText: TextView = itemView.findViewById(R.id.recap_section_date_range)
        private val bulletPointsContainer: LinearLayout = itemView.findViewById(R.id.recap_bullet_points_container)

        fun bind(section: RecapSection, isFirst: Boolean) {
            // Set header
            headerText.text = section.header
            
            // Set header color based on checkpoint
            val headerColor = when (section.checkpoint) {
                RecapCheckpoint.DAILY -> itemView.context.getColor(R.color.main_color)
                RecapCheckpoint.WEEKLY -> itemView.context.getColor(R.color.colorTextDeep)
                RecapCheckpoint.MONTHLY -> itemView.context.getColor(R.color.colorTextDeep)
            }
            headerText.setTextColor(headerColor)
            
            // Set date range
            if (section.dateRange != null && section.dateRange.isNotEmpty()) {
                dateRangeText.visibility = View.VISIBLE
                dateRangeText.text = section.dateRange
            } else {
                // For daily section (no date range), add consistent spacing
                dateRangeText.visibility = View.GONE
                // Add bottom margin to header to match the spacing of date range
                val layoutParams = headerText.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.bottomMargin = itemView.context.resources.getDimensionPixelSize(R.dimen.recap_header_bottom_margin)
                headerText.layoutParams = layoutParams
            }
            
            // Clear existing bullet points
            bulletPointsContainer.removeAllViews()
            
            // Add bullet points
            section.bulletPoints.forEachIndexed { index, bulletPoint ->
                val bulletView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_recap_bullet_point, bulletPointsContainer, false)
                
                val numberView = bulletView.findViewById<TextView>(R.id.bullet_number)
                val textView = bulletView.findViewById<TextView>(R.id.bullet_text)
                
                numberView.text = (index + 1).toString()
                textView.text = bulletPoint
                
                // Add staggered animation
                bulletView.alpha = 0f
                bulletView.animate()
                    .alpha(1f)
                    .setDuration(400)
                    .setStartDelay((index * 80).toLong())
                    .start()
                
                bulletPointsContainer.addView(bulletView)
            }
        }
    }
}

