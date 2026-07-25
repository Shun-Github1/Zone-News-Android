package com.searcher.zonenews.ui.newsdetail

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.searcher.zonenews.R
import com.searcher.zonenews.entry.QuoteEntry

class QuotesBottomSheetAdapter(
    private val quotes: List<QuoteEntry>,
    private val onItemClick: (QuoteEntry) -> Unit
) : RecyclerView.Adapter<QuotesBottomSheetAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_quote_bottom_sheet, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(quotes[position], onItemClick)
    }

    override fun getItemCount(): Int = quotes.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatarContainer: View = itemView.findViewById(R.id.iv_avatar_container_bs)
        private val ivAvatarIcon: ImageView = itemView.findViewById(R.id.iv_avatar_icon_bs)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name_bs)
        private val tvRole: TextView = itemView.findViewById(R.id.tv_role_bs)
        private val tvQuote: TextView = itemView.findViewById(R.id.tv_quote_full_bs)
        private val tvContext: TextView = itemView.findViewById(R.id.tv_context_bs)
        private val tvCategoryBadge: TextView = itemView.findViewById(R.id.tv_category_badge_bs)

        fun bind(quote: QuoteEntry, onClick: (QuoteEntry) -> Unit) {
            var displayName = quote.name
            var displayRole = quote.role

            // Handle edge case: Person with no name but has a role
            // Treat same as institution with name and no role -> Show role in name field
            if (displayName.isNullOrEmpty() && !displayRole.isNullOrEmpty()) {
                displayName = displayRole
                displayRole = null
            }

            tvName.text = displayName ?: ""
            
            if (displayRole.isNullOrEmpty()) {
                tvRole.visibility = View.GONE
            } else {
                tvRole.visibility = View.VISIBLE
                tvRole.text = displayRole
            }
            val quoteOpen = itemView.context.getString(R.string.quote_open)
            val quoteClose = itemView.context.getString(R.string.quote_close)
            tvQuote.text = "$quoteOpen${quote.text ?: ""}$quoteClose"
            tvContext.text = quote.background ?: ""

            // Entity Type Logic (1/organisation = Institution, 0/person/other = Person)
            val isInstitution = quote.entityType == "1" || quote.entityType.equals("organisation", ignoreCase = true)
            val iconRes = if (isInstitution) R.drawable.ic_account_balance_24 else R.drawable.ic_profile_24_unselected
            ivAvatarIcon.setImageResource(iconRes)

            // Category Logic
            val (categoryName, colorHex) = getCategoryInfo(quote.category)
            val categoryColor = Color.parseColor(colorHex)
            
            // Avatar Styling Logic
            val drawable = ivAvatarContainer.background as GradientDrawable
            if (isInstitution) {
                // Institutional: No circle background, colored icon
                drawable.setColor(Color.TRANSPARENT)
                ivAvatarIcon.setColorFilter(categoryColor)
            } else {
                // Person: Circle background, white icon
                drawable.setColor(categoryColor)
                ivAvatarIcon.setColorFilter(Color.WHITE)
            }
            
            // Badge
            tvCategoryBadge.text = categoryName
            val badgeDrawable = tvCategoryBadge.background as GradientDrawable
            badgeDrawable.setColor(Color.parseColor(colorHex))

            itemView.setOnClickListener { onClick(quote) }
        }

        private fun getCategoryInfo(category: String?): Pair<String, String> {
            val context = itemView.context
            if (category == null) return context.getString(R.string.quote_category_other) to "#f0fff5"
            
            try {
                val id = category.toInt()
                return getCategoryInfoById(id)
            } catch (e: NumberFormatException) {
                return when (category.lowercase()) {
                    "policy", "political" -> context.getString(R.string.quote_category_political) to "#1e1a3d"
                    "administrative" -> context.getString(R.string.quote_category_administrative) to "#3d4048"
                    "legal" -> context.getString(R.string.quote_category_legal) to "#0d2d52"
                    "emergency" -> context.getString(R.string.quote_category_emergency) to "#5c1528"
                    "military" -> context.getString(R.string.quote_category_military) to "#1a3828"
                    "medical", "health" -> context.getString(R.string.quote_category_medical) to "#4a2518"
                    "corporate", "business" -> context.getString(R.string.quote_category_corporate) to "#353028"
                    "finance", "financial" -> context.getString(R.string.quote_category_finance) to "#1a3d3d"
                    "education" -> context.getString(R.string.quote_category_education) to "#3d1f3d"
                    "academic" -> context.getString(R.string.quote_category_academic) to "#4a4020"
                    "entertainment" -> context.getString(R.string.quote_category_entertainment) to "#2d1a4a"
                    "sports", "sport" -> context.getString(R.string.quote_category_sports) to "#6b5032"
                    "charity" -> context.getString(R.string.quote_category_charity) to "#2a3d35"
                    "media" -> context.getString(R.string.quote_category_media) to "#6b2820"
                    "other" -> context.getString(R.string.quote_category_other) to "#5a5a5a"
                    else -> context.getString(R.string.quote_category_other) to "#5a5a5a"
                }
            }
        }

        private fun getCategoryInfoById(categoryId: Int): Pair<String, String> {
            val context = itemView.context
            return when (categoryId) {
                1 -> context.getString(R.string.quote_category_political) to "#1e1a3d"
                2 -> context.getString(R.string.quote_category_administrative) to "#3d4048"
                3 -> context.getString(R.string.quote_category_legal) to "#0d2d52"
                4 -> context.getString(R.string.quote_category_emergency) to "#5c1528"
                5 -> context.getString(R.string.quote_category_military) to "#1a3828"
                6 -> context.getString(R.string.quote_category_medical) to "#4a2518"
                7 -> context.getString(R.string.quote_category_corporate) to "#353028"
                8 -> context.getString(R.string.quote_category_finance) to "#1a3d3d"
                9 -> context.getString(R.string.quote_category_education) to "#3d1f3d"
                10 -> context.getString(R.string.quote_category_academic) to "#4a4020"
                11 -> context.getString(R.string.quote_category_entertainment) to "#2d1a4a"
                12 -> context.getString(R.string.quote_category_sports) to "#6b5032"
                13 -> context.getString(R.string.quote_category_charity) to "#2a3d35"
                14 -> context.getString(R.string.quote_category_media) to "#6b2820"
                15 -> context.getString(R.string.quote_category_other) to "#5a5a5a"
                else -> context.getString(R.string.quote_category_other) to "#5a5a5a"
            }
        }
    }
}
