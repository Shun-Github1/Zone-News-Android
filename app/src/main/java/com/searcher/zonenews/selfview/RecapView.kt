package com.searcher.zonenews.selfview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.searcher.zonenews.R
import com.searcher.zonenews.databinding.ViewRecapBinding

/**
 * Unified Recap View that displays all three recap sections (Daily, Weekly, Monthly)
 * in a single scrollable view with an interactive progress tracker on the right.
 * 
 * Matches the iOS PersonalRecapView implementation.
 */
class RecapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: ViewRecapBinding
    private val progressTracker: RecapProgressTracker
    private var selectedCheckpoint = RecapCheckpoint.DAILY
    private var isUserScrolling = false
    private var isProgrammaticScroll = false
    
    // Section positions for scroll tracking
    private val sectionPositions = mutableMapOf<RecapCheckpoint, Int>()
    
    init {
        binding = ViewRecapBinding.inflate(LayoutInflater.from(context), this, true)
        progressTracker = binding.recapProgressTracker
        
        setupRecyclerView()
        setupProgressTracker()
        setupScrollListener()
    }
    
    private fun setupRecyclerView() {
        val sections = listOf(
            RecapSection(
                checkpoint = RecapCheckpoint.DAILY,
                header = context.getString(R.string.recap_24h_header),
                bulletPoints = listOf(
                    context.getString(R.string.recap_daily_item_1),
                    context.getString(R.string.recap_daily_item_2),
                    context.getString(R.string.recap_daily_item_3),
                    context.getString(R.string.recap_daily_item_4),
                    context.getString(R.string.recap_daily_item_5)
                ),
                dateRange = null
            ),
            RecapSection(
                checkpoint = RecapCheckpoint.WEEKLY,
                header = context.getString(R.string.recap_weekly_header),
                bulletPoints = listOf(
                    context.getString(R.string.recap_weekly_item_1),
                    context.getString(R.string.recap_weekly_item_2),
                    context.getString(R.string.recap_weekly_item_3),
                    context.getString(R.string.recap_weekly_item_4),
                    context.getString(R.string.recap_weekly_item_5)
                ),
                dateRange = context.getString(R.string.recap_weekly_date_range)
            ),
            RecapSection(
                checkpoint = RecapCheckpoint.MONTHLY,
                header = context.getString(R.string.recap_monthly_header),
                bulletPoints = listOf(
                    context.getString(R.string.recap_monthly_item_1),
                    context.getString(R.string.recap_monthly_item_2),
                    context.getString(R.string.recap_monthly_item_3),
                    context.getString(R.string.recap_monthly_item_4),
                    context.getString(R.string.recap_monthly_item_5)
                ),
                dateRange = context.getString(R.string.recap_monthly_date_range)
            )
        )
        
        val adapter = RecapSectionAdapter(sections)
        binding.recapRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.recapRecyclerView.adapter = adapter
        
        // Calculate section positions after layout
        binding.recapRecyclerView.post {
            calculateSectionPositions()
        }
    }
    
    private fun setupProgressTracker() {
        progressTracker.setSelectedCheckpoint(selectedCheckpoint)
        progressTracker.setOnCheckpointSelectedListener { checkpoint ->
            isUserScrolling = true
            isProgrammaticScroll = true
            selectedCheckpoint = checkpoint
            scrollToSection(checkpoint)
            
            // Reset flags after scroll completes
            binding.recapRecyclerView.postDelayed({
                isUserScrolling = false
                isProgrammaticScroll = false
            }, 500)
        }
    }
    
    private fun setupScrollListener() {
        binding.recapRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                if (!isUserScrolling && !isProgrammaticScroll) {
                    updateSelectedCheckpointBasedOnScroll()
                }
                
                // Update progress based on scroll position
                updateProgressIndicator()
                
                // Hide/show bottom bar based on scroll direction
                val activity = (context as? android.app.Activity)
                if (activity is com.searcher.zonenews.ui.MainActivity) {
                    if (dy > 0) {
                        activity.hideBottomBar()
                    } else if (dy < 0) {
                        activity.showBottomBar()
                    }
                }
            }
            
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                
                val activity = (context as? android.app.Activity)
                if (activity is com.searcher.zonenews.ui.MainActivity) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        activity.scheduleBottomBarAutoShow()
                    } else {
                        activity.cancelBottomBarAutoShow()
                    }
                }
            }
        })
    }
    
    private fun calculateSectionPositions() {
        val layoutManager = binding.recapRecyclerView.layoutManager as? LinearLayoutManager
        layoutManager?.let {
            // Daily is at position 0
            sectionPositions[RecapCheckpoint.DAILY] = 0
            // Weekly is at position 1 
            sectionPositions[RecapCheckpoint.WEEKLY] = 1
            // Monthly is at position 2
            sectionPositions[RecapCheckpoint.MONTHLY] = 2
        }
    }
    
    private fun scrollToSection(checkpoint: RecapCheckpoint) {
        val position = sectionPositions[checkpoint] ?: return
        val layoutManager = binding.recapRecyclerView.layoutManager as? LinearLayoutManager
        
        // Stop any ongoing scroll
        binding.recapRecyclerView.stopScroll()
        
        layoutManager?.scrollToPositionWithOffset(position, 0)
    }
    
    private fun updateSelectedCheckpointBasedOnScroll() {
        val layoutManager = binding.recapRecyclerView.layoutManager as? LinearLayoutManager
        layoutManager?.let {
            val firstVisiblePosition = it.findFirstVisibleItemPosition()
            it.findViewByPosition(firstVisiblePosition)
            val secondVisibleView = it.findViewByPosition(firstVisiblePosition + 1)
            
            val newCheckpoint = when (firstVisiblePosition) {
                0 -> {
                    // Switch to weekly only when weekly section is fully visible
                    if (secondVisibleView != null) {
                        val weeklyViewTop = secondVisibleView.top
                        if (weeklyViewTop <= 0) {
                            RecapCheckpoint.WEEKLY
                        } else {
                            RecapCheckpoint.DAILY
                        }
                    } else {
                        RecapCheckpoint.DAILY
                    }
                }
                1 -> {
                    // Switch to monthly only when monthly section is fully visible
                    if (secondVisibleView != null) {
                        val monthlyViewTop = secondVisibleView.top
                        if (monthlyViewTop <= 0) {
                            RecapCheckpoint.MONTHLY
                        } else {
                            RecapCheckpoint.WEEKLY
                        }
                    } else {
                        RecapCheckpoint.WEEKLY
                    }
                }
                2 -> RecapCheckpoint.MONTHLY
                else -> selectedCheckpoint
            }
            
            if (newCheckpoint != selectedCheckpoint) {
                selectedCheckpoint = newCheckpoint
                progressTracker.setSelectedCheckpoint(newCheckpoint)
            }
        }
    }
    
    private fun updateProgressIndicator() {
        val layoutManager = binding.recapRecyclerView.layoutManager as? LinearLayoutManager
        layoutManager?.let {
            val firstVisiblePosition = it.findFirstVisibleItemPosition()
            val firstVisibleView = it.findViewByPosition(firstVisiblePosition)
            
            if (firstVisibleView != null) {
                val visibleHeight = firstVisibleView.bottom.toFloat()
                val totalHeight = firstVisibleView.height.toFloat()
                
                if (totalHeight > 0) {
                    val scrollProgress = 1f - (visibleHeight / totalHeight)
                    progressTracker.updateProgress(selectedCheckpoint, scrollProgress.coerceIn(0f, 1f))
                }
            }
        }
    }
    
    /**
     * Scroll to top of the recap view
     */
    fun scrollToTop() {
        selectedCheckpoint = RecapCheckpoint.DAILY
        progressTracker.setSelectedCheckpoint(selectedCheckpoint)
        binding.recapRecyclerView.smoothScrollToPosition(0)
    }
}

/**
 * Enum representing the three recap checkpoints
 */
enum class RecapCheckpoint {
    DAILY,
    WEEKLY,
    MONTHLY;
    
    fun getTitle(context: Context): String {
        return when (this) {
            DAILY -> context.getString(R.string.recap_daily)
            WEEKLY -> context.getString(R.string.recap_weekly)
            MONTHLY -> context.getString(R.string.recap_monthly)
        }
    }
}

/**
 * Data class representing a recap section
 */
data class RecapSection(
    val checkpoint: RecapCheckpoint,
    val header: String,
    val bulletPoints: List<String>,
    val dateRange: String?
)

