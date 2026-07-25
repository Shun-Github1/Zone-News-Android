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

class QuotesAdapter(
    private val quotes: List<QuoteEntry>,
    private val onItemClick: (QuoteEntry) -> Unit
) : RecyclerView.Adapter<QuotesAdapter.QuoteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_quote_cardlet, parent, false)
        return QuoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        val actualPosition = position % quotes.size
        holder.bind(quotes[actualPosition], onItemClick)
    }

    override fun getItemCount(): Int = if (quotes.isNotEmpty()) Int.MAX_VALUE else 0

    class QuoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivAvatarContainer: View = itemView.findViewById(R.id.iv_avatar_container)
        private val ivAvatarIcon: ImageView = itemView.findViewById(R.id.iv_avatar_icon)
        private val tvName: TextView = itemView.findViewById(R.id.tv_name)
        // private val tvRole: TextView = itemView.findViewById(R.id.tv_role) // Removed in layout
        private val tvQuote: TextView = itemView.findViewById(R.id.tv_quote_body)

        fun bind(quote: QuoteEntry, onClick: (QuoteEntry) -> Unit) {
            var displayName = quote.name
            var displayRole = quote.role

            // Handle edge case: Person with no name but has a role
            if (displayName.isNullOrEmpty() && !displayRole.isNullOrEmpty()) {
                displayName = displayRole
                displayRole = null
            }
            
            val roleText = if (displayRole.isNullOrEmpty()) "" else ", $displayRole"
            tvName.text = "${displayName ?: ""}$roleText"

            val quoteOpen = itemView.context.getString(R.string.quote_open)
            val quoteClose = itemView.context.getString(R.string.quote_close)
            tvQuote.text = "$quoteOpen${quote.text ?: ""}$quoteClose"

            // Entity Type Logic (1/organisation = Institution, 0/person/other = Person)
            val isInstitution = quote.entityType == "1" || quote.entityType.equals("organisation", ignoreCase = true)
            val iconRes = if (isInstitution) R.drawable.ic_account_balance_24 else R.drawable.ic_profile_24_unselected
            ivAvatarIcon.setImageResource(iconRes)

            // Category Color Logic
            val colorHex = getCategoryColor(quote.category)
            val color = Color.parseColor(colorHex)
            val drawable = ivAvatarContainer.background as GradientDrawable

            if (isInstitution) {
                // For institutions: Icon gets the color, background is transparent
                // Size: 18dp (container) - 0 padding = 18dp icon
                ivAvatarIcon.setPadding(0, 0, 0, 0)
                ivAvatarIcon.setColorFilter(color)
                drawable.setColor(Color.TRANSPARENT)
            } else {
                // For people: Icon is white (default), background gets the color
                // Size: 18dp (container) - 1dp padding each side = 16dp icon
                val padding = dpToPx(itemView.context, 1)
                ivAvatarIcon.setPadding(padding, padding, padding, padding)
                ivAvatarIcon.setColorFilter(Color.WHITE)
                drawable.setColor(color)
            }

            itemView.setOnClickListener { onClick(quote) }
        }

        private fun dpToPx(context: Context, dp: Int): Int {
            return (dp * context.resources.displayMetrics.density).toInt()
        }

        private fun getCategoryColor(category: String?): String {
            // Default to 'Other' color if null or unknown
            if (category == null) return "#f0fff5"
            
            // Try to map known string categories to existing colors
            // If the backend sends "1", "2" etc as strings, we try to parse them
            // If it sends "policy", "political" etc, we map them
            
            // First try to parse as Int (backward compatibility / if numeric strings used)
            try {
                val categoryId = category.toInt()
                return getCategoryColorById(categoryId)
            } catch (e: NumberFormatException) {
                // Not a number, treat as a string key
                return when (category.lowercase()) {
                    "policy", "political" -> "#1e1a3d" // Political
                    "administrative" -> "#3d4048"
                    "legal" -> "#0d2d52"
                    "emergency" -> "#5c1528"
                    "military" -> "#1a3828"
                    "medical", "health" -> "#4a2518"
                    "corporate", "business" -> "#353028"
                    "finance", "financial" -> "#1a3d3d"
                    "education" -> "#3d1f3d"
                    "academic" -> "#4a4020"
                    "entertainment" -> "#2d1a4a"
                    "sports", "sport" -> "#6b5032"
                    "charity" -> "#2a3d35"
                    "media" -> "#6b2820"
                    "other" -> "#5a5a5a"
                    else -> "#5a5a5a"
                }
            }
        }
        
        private fun getCategoryColorById(categoryId: Int): String {
            return when (categoryId) {
                1 -> "#1e1a3d" // Political
                2 -> "#3d4048" // Administrative
                3 -> "#0d2d52" // Legal
                4 -> "#5c1528" // Emergency Services
                5 -> "#1a3828" // Military
                6 -> "#4a2518" // Medical
                7 -> "#353028" // Corporate
                8 -> "#1a3d3d" // Finance
                9 -> "#3d1f3d" // Education
                10 -> "#4a4020" // Academic
                11 -> "#2d1a4a" // Entertainment
                12 -> "#6b5032" // Sports
                13 -> "#2a3d35" // Charity
                14 -> "#6b2820" // Media
                15 -> "#5a5a5a" // Other
                else -> "#5a5a5a"
            }
        }
    }
}
