package com.searcher.zonenews.ui.newsdetail

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.searcher.zonenews.R
import com.searcher.zonenews.entry.ArticleDetailEntry

/**
 * Custom view for displaying related news articles grouped by topics.
 */
class RelatedNewsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val sectionsContainer: LinearLayout
    val contentContainer: LinearLayout  // Exposed for collapse/expand functionality
    val chevronBtn: View  // Exposed for collapse/expand control
    val menuBtn: View  // Exposed for reorder control
    
    // Callback for when a related article is clicked
    var onArticleClickListener: ((String) -> Unit)? = null

    // Callback for when a topic header is clicked
    var onTopicClickListener: ((String, String) -> Unit)? = null
    
    // Callback to control swipe-to-go-back gesture
    var onScrollStateChangedListener: ((Boolean) -> Unit)? = null
    
    // Callback for when a follow button is clicked
    var onFollowAction: ((String, String, Boolean) -> Unit)? = null

    // List of currently followed tag IDs/names to sync checkbox state
    var followedTags: List<String> = emptyList()
        set(value) {
            field = value
            refreshTags()
        }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_related_news, this, true)
        sectionsContainer = findViewById(R.id.related_news_sections_container)
        contentContainer = sectionsContainer  // Content container is the sections container
        chevronBtn = findViewById(R.id.chevron_btn)
        menuBtn = findViewById(R.id.menu_btn)
    }

    /**
     * Refreshes only the tag chips to update their followed state.
     */
    private fun refreshTags() {
        for (i in 0 until sectionsContainer.childCount) {
            val child = sectionsContainer.getChildAt(i)
            if (child is ViewGroup && child.tag != null) {
                val tagId = child.tag as String
                updateTagChipState(child, tagId)
            }
        }
    }

    private fun updateTagChipState(chipView: View, tagId: String) {
        val checkBtn = chipView.findViewById<LinearLayout>(R.id.tag_check_btn) ?: return
        val checkImg = checkBtn.getChildAt(0) as? ImageView ?: return
        
        val isFollowed = followedTags.any { it.equals(tagId, ignoreCase = true) }
        
        if (isFollowed) {
            checkImg.setImageResource(R.drawable.ic_check)
        } else {
            checkImg.setImageResource(R.drawable.ic_add)
        }
        checkBtn.isEnabled = true
        checkImg.alpha = 1f
    }

    /**
     * Sets the related news data and builds the UI.
     * 
     * @param topics List of related topics to use as section headers
     * @param articles List of related articles to display
     */
    fun setRelatedData(
        topics: List<ArticleDetailEntry.DataDTO.RelatedTopicDTO>?,
        articles: List<ArticleDetailEntry.DataDTO.RelatedArticlesDTO>?
    ) {
        sectionsContainer.removeAllViews()
        
        if (articles.isNullOrEmpty()) {
            visibility = View.GONE
            return
        }
        
        visibility = View.VISIBLE
        requestLayout()
        invalidate()
        
        // 1. Collect all potential grouping keys from all articles
        // A key is Pair(Type, Value), e.g., ("Tag", "Politics"), ("Region", "China"), ("Sector", "Economy")
        val allGroups = mutableMapOf<String, MutableList<ArticleDetailEntry.DataDTO.RelatedArticlesDTO>>()
        val groupTypes = mutableMapOf<String, Int>() // Store priority: 3=Dynamic Tag, 2=Region, 1=Sector

        articles.forEach { article ->
            // Dynamic Topics (Priority 3)
            // Access the 'topics' list instead of 'tags'
            article.topics?.forEach { topic ->
                val tag = topic.tag
                if (!tag.isNullOrEmpty()) {
                    allGroups.getOrPut(tag) { mutableListOf() }.add(article)
                    groupTypes[tag] = 3
                }
            }
            
            // Regions (Priority 2)
            article.region?.let { region ->
                if (region.isNotEmpty()) {
                    allGroups.getOrPut(region) { mutableListOf() }.add(article)
                    if (!groupTypes.containsKey(region) || groupTypes[region]!! < 2) {
                        groupTypes[region] = 2
                    }
                }
            }
            
            // Sectors (Priority 1)
            article.sector?.let { sector ->
                if (sector.isNotEmpty()) {
                    allGroups.getOrPut(sector) { mutableListOf() }.add(article)
                    if (!groupTypes.containsKey(sector) || groupTypes[sector]!! < 1) {
                        groupTypes[sector] = 1
                    }
                }
            }
        }

        // 2. Sort groups by Size (Primary) and Priority (Secondary)
        val sortedGroups = allGroups.entries
            .filter { it.value.isNotEmpty() }
            .sortedWith(compareByDescending<Map.Entry<String, MutableList<ArticleDetailEntry.DataDTO.RelatedArticlesDTO>>> { it.value.size }
                .thenByDescending { groupTypes[it.key] ?: 0 })

        // 3. Display the groups (max 2)
        val usedArticles = mutableSetOf<String>()
        var displayedSections = 0
        
        for ((header, groupArticles) in sortedGroups) {
            // Stop if we have shown 2 sections
            if (displayedSections >= 2) break
            
            val uniqueArticles = groupArticles.filter { it.articleID !in usedArticles }
            
            // Only add section if it contributes at least one new article
            if (uniqueArticles.isNotEmpty()) {
                // Determine display name
                var displayName = topics?.find { it.tag.equals(header, ignoreCase = true) }?.displayName
                
                if (displayName == null) {
                    val articleWithTopic = groupArticles.firstOrNull { art -> 
                        art.topics?.any { it.tag.equals(header, ignoreCase = true) } == true
                    }
                    displayName = articleWithTopic?.topics?.find { it.tag.equals(header, ignoreCase = true) }?.displayName
                }
                
                if (displayName == null) {
                    displayName = header
                }
                
                addSection(displayName, header, uniqueArticles)
                usedArticles.addAll(uniqueArticles.mapNotNull { it.articleID })
                displayedSections++
            }
        }

        // 4. Fallback
        if (displayedSections == 0 && articles.isNotEmpty()) {
             addSection(null, null, articles)
        }
        
        // Final visibility check - if in rearrange mode, always show
        if (visibility == View.GONE && isRearrangeMode) {
            visibility = View.VISIBLE
        }
    }

    private var isRearrangeMode = false

    /**
     * Sets whether the view is in rearrange mode.
     * In rearrange mode, the view is always visible even if there are no related articles.
     */
    fun setRearrangeMode(isRearrange: Boolean) {
        if (isRearrangeMode == isRearrange) return
        isRearrangeMode = isRearrange
        
        if (isRearrangeMode) {
            // Force visibility on
            visibility = View.VISIBLE
            // If sections container is empty, we might want to show a placeholder or just the header is enough
            // The header is part of the layout (R.layout.view_related_news), so it will show.
        } else {
            // Re-evaluate visibility based on data
            // If sectionsContainer is empty, hide
            if (sectionsContainer.childCount == 0) {
                visibility = View.GONE
            }
        }
    }

    private fun addSection(sectionHeader: String?, tagId: String?, articles: List<ArticleDetailEntry.DataDTO.RelatedArticlesDTO>) {
        // Add section header if provided
        if (!sectionHeader.isNullOrEmpty()) {
            val chipView = LayoutInflater.from(context).inflate(
                R.layout.item_followed_tag,
                sectionsContainer,
                false
            )
            
            // Store tagId in view's tag for updates
            chipView.tag = tagId
            
            val tagText = chipView.findViewById<TextView>(R.id.tag_text)
            val checkBtn = chipView.findViewById<LinearLayout>(R.id.tag_check_btn)
            val checkImg = checkBtn.getChildAt(0) as ImageView

            // Set display name
            tagText.text = if (sectionHeader.equals("General", ignoreCase = true)) {
                context.getString(R.string.topic_general)
            } else {
                sectionHeader
            }
            
            // Adjust padding to keep text start position same as before
            // Previous header used setPadding(0, dpToPx(8), 0, dpToPx(4))
            // item_followed_tag has paddingStart="8dp", so we reset it.
            tagText.setPadding(0, tagText.paddingTop, tagText.paddingEnd, tagText.paddingBottom)
            
            // Set checkbox state
            val isFollowed = followedTags.any { it.equals(tagId, ignoreCase = true) }
            if (isFollowed) {
                checkImg.setImageResource(R.drawable.ic_check)
            } else {
                checkImg.setImageResource(R.drawable.ic_add)
            }
            
            // Handle follow click
            checkBtn.setOnClickListener {
                if (tagId == null) return@setOnClickListener
                val currentFollowed = followedTags.any { it.equals(tagId, ignoreCase = true) }
                
                checkBtn.isEnabled = false
                checkImg.animate().alpha(0f).setDuration(200).withEndAction {
                    onFollowAction?.invoke(tagId, sectionHeader, !currentFollowed)
                }.start()
            }
            
            // Handle tag text click
            tagText.setOnClickListener {
                if (tagId != null) {
                    onTopicClickListener?.invoke(tagId, sectionHeader)
                }
            }

            sectionsContainer.addView(chipView)
        }
        
        // Create horizontal RecyclerView for articles
        val recyclerView = RecyclerView(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(4)
                bottomMargin = dpToPx(8)
            }
            
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            
            // Set a minimum height to ensure stable measurement during animation
            // Image (56dp) + Padding (24dp) = 80dp
            minimumHeight = dpToPx(80)

            // Add scroll listener to disable swipe-to-go-back during scroll
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    val isScrolling = newState != RecyclerView.SCROLL_STATE_IDLE
                    onScrollStateChangedListener?.invoke(isScrolling)
                }
            })
            
            clipToPadding = false
            setPadding(0, 0, dpToPx(8), 0)
        }
        
        // Set adapter
        recyclerView.adapter = RelatedNewsAdapter(context, articles) { article ->
            article.articleID?.let { articleId ->
                onArticleClickListener?.invoke(articleId)
            }
        }
        
        sectionsContainer.addView(recyclerView)
    }

    /**
     * Forces a refresh of the layout and adapters.
     * Useful when the view becomes visible after being collapsed.
     */
    fun refresh() {
        requestLayout()
        invalidate()
        for (i in 0 until sectionsContainer.childCount) {
            val child = sectionsContainer.getChildAt(i)
            if (child is RecyclerView) {
                child.adapter?.notifyDataSetChanged()
                child.requestLayout()
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}
