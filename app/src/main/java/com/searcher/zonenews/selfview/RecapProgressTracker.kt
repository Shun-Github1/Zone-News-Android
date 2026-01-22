package com.searcher.zonenews.selfview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.searcher.zonenews.R
import com.searcher.zonenews.utils.HapticFeedbackHelper
import kotlin.math.abs

/**
 * Progress tracker component for the Recap view.
 * Displays three checkpoints (Daily, Weekly, Monthly) with circles and labels.
 * Allows tapping or dragging to navigate between sections.
 * Shows progress indicator for the current section.
 * 
 * Matches the iOS RecapProgressTracker implementation.
 */
class RecapProgressTracker @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var selectedCheckpoint = RecapCheckpoint.DAILY
    private var scrollProgress = 0f
    private var onCheckpointSelectedListener: ((RecapCheckpoint) -> Unit)? = null
    
    // Dimensions
    private val circleRadius = dpToPx(6f)
    private val circleRadiusSelected = dpToPx(8f)
    private val lineWidth = dpToPx(1.5f)
    private val lineWidthProgress = dpToPx(2f)
    private val textSize = dpToPx(10f)
    
    // Fixed vertical spacing
    private val textBelowCircleSpacing = dpToPx(20f)
    private val lineBelowTextSpacing = dpToPx(15f)
    private val lineLength = dpToPx(60f)
    private val checkpointSpacing = dpToPx(35f) // Space between line end and next circle
    
    // Colors
    private val brandColor = ContextCompat.getColor(context, R.color.main_color)
    private val textSecondaryColor = ContextCompat.getColor(context, R.color.colorTextSmall)
    private val textPrimaryColor = ContextCompat.getColor(context, R.color.colorTextDeep)
    private val lineColor = ContextCompat.getColorStateList(context, R.color.colorTextSmall)?.defaultColor ?: textSecondaryColor
    
    // Paint objects
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val circleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = lineWidth
    }
    
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = lineWidth
        color = textSecondaryColor
    }
    
    private val lineProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = lineWidthProgress
        color = brandColor
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = this@RecapProgressTracker.textSize
        textAlign = Paint.Align.CENTER
    }
    
    // Touch handling
    private var downY = 0f
    private var lastHapticCheckpoint: RecapCheckpoint? = null
    
    // Animation
    private var animator: ValueAnimator? = null
    
    init {
        setWillNotDraw(false)
    }
    
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = dpToPx(80f).toInt()
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val centerX = width / 2f
        val totalHeight = calculateTotalHeight()
        val startY = (height - totalHeight) / 2f
        
        var currentY = startY
        
        RecapCheckpoint.values().forEachIndexed { index, checkpoint ->
            val isSelected = checkpoint == selectedCheckpoint
            
            // 1. Draw circle
            val radius = if (isSelected) circleRadiusSelected else circleRadius
            
            if (isSelected) {
                circlePaint.color = brandColor
                canvas.drawCircle(centerX, currentY, radius, circlePaint)
            }
            
            circleStrokePaint.color = if (isSelected) brandColor else textSecondaryColor
            circleStrokePaint.alpha = if (isSelected) 255 else 128
            canvas.drawCircle(centerX, currentY, radius, circleStrokePaint)
            
            // Move down by circle radius + spacing
            currentY += radius + textBelowCircleSpacing
            
            // 2. Draw label text
            val label = checkpoint.getTitle(context)
            textPaint.color = if (isSelected) brandColor else textSecondaryColor
            textPaint.textSize = textSize * 1.05f
            textPaint.isFakeBoldText = isSelected
            canvas.drawText(label, centerX, currentY, textPaint)
            
            // Move down by text spacing
            currentY += lineBelowTextSpacing
            
            // 3. Draw connecting line (identical for all checkpoints including monthly)
            val lineStartY = currentY
            val lineEndY = currentY + lineLength
            
            // Background line
            canvas.drawLine(centerX, lineStartY, centerX, lineEndY, linePaint)
            
            // Progress line (for selected checkpoint)
            if (isSelected && scrollProgress > 0f) {
                val progressHeight = lineLength * scrollProgress
                canvas.drawLine(centerX, lineStartY, centerX, lineStartY + progressHeight, lineProgressPaint)
            }
            
            // Move down by line length + checkpoint spacing for next checkpoint
            if (index < RecapCheckpoint.values().size - 1) {
                currentY += lineLength + checkpointSpacing - radius
            }
        }
    }
    
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downY = event.y
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaY = downY - event.y
                if (abs(deltaY) > dpToPx(10f)) {
                    handleDrag(deltaY)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val deltaY = downY - event.y
                if (abs(deltaY) < dpToPx(10f)) {
                    // This was a tap, not a drag
                    handleTap(event.y)
                } else {
                    // Finalize drag
                    finalizeDrag(deltaY)
                }
                lastHapticCheckpoint = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }
    
    private fun handleTap(y: Float) {
        val totalHeight = calculateTotalHeight()
        val startY = (height - totalHeight) / 2f
        
        var currentY = startY
        
        RecapCheckpoint.values().forEachIndexed { index, checkpoint ->
            val isSelected = checkpoint == selectedCheckpoint
            val radius = if (isSelected) circleRadiusSelected else circleRadius
            
            // Touch area covers only circle and text (not the connecting line)
            val regionStart = currentY - radius - dpToPx(5f) // Small padding above circle
            val regionEnd = currentY + radius + textBelowCircleSpacing + dpToPx(5f) // Small padding below text
            
            if (y >= regionStart && y <= regionEnd) {
                HapticFeedbackHelper.performNavigationHaptic(this)
                setSelectedCheckpoint(checkpoint)
                onCheckpointSelectedListener?.invoke(checkpoint)
                return
            }
            
            // Move currentY to next checkpoint circle center
            currentY += radius + textBelowCircleSpacing + lineBelowTextSpacing + lineLength + checkpointSpacing - radius
        }
    }
    
    private fun handleDrag(deltaY: Float) {
        val totalHeight = calculateTotalHeight()
        val progress = (deltaY / totalHeight).coerceIn(-0.1f, 1.1f)
        
        val newCheckpoint = when {
            progress < 0.33f -> RecapCheckpoint.DAILY
            progress < 0.66f -> RecapCheckpoint.WEEKLY
            else -> RecapCheckpoint.MONTHLY
        }
        
        if (newCheckpoint != selectedCheckpoint && newCheckpoint != lastHapticCheckpoint) {
            HapticFeedbackHelper.performNavigationHaptic(this)
            lastHapticCheckpoint = newCheckpoint
            setSelectedCheckpoint(newCheckpoint)
        }
    }
    
    private fun finalizeDrag(deltaY: Float) {
        val totalHeight = calculateTotalHeight()
        val progress = (deltaY / totalHeight).coerceIn(0f, 1f)
        
        val finalCheckpoint = when {
            progress < 0.33f -> RecapCheckpoint.DAILY
            progress < 0.66f -> RecapCheckpoint.WEEKLY
            else -> RecapCheckpoint.MONTHLY
        }
        
        if (finalCheckpoint != selectedCheckpoint) {
            HapticFeedbackHelper.performNavigationHaptic(this)
            setSelectedCheckpoint(finalCheckpoint)
            onCheckpointSelectedListener?.invoke(finalCheckpoint)
        }
    }
    
    private fun calculateTotalHeight(): Float {
        // Calculate total height based on vertical hierarchy:
        // For each checkpoint: radius + textBelowCircleSpacing + text height + lineBelowTextSpacing + lineLength + checkpointSpacing
        // Simplified: (radius + textBelowCircleSpacing + lineBelowTextSpacing + lineLength + checkpointSpacing) * 3 checkpoints
        val checkpointCount = RecapCheckpoint.values().size
        val singleCheckpointHeight = circleRadiusSelected + textBelowCircleSpacing + lineBelowTextSpacing + lineLength + checkpointSpacing
        return singleCheckpointHeight * checkpointCount + dpToPx(30f) // Extra space for final progress line
    }
    
    fun setSelectedCheckpoint(checkpoint: RecapCheckpoint) {
        if (selectedCheckpoint != checkpoint) {
            selectedCheckpoint = checkpoint
            animateToCheckpoint()
        }
    }
    
    fun updateProgress(checkpoint: RecapCheckpoint, progress: Float) {
        if (checkpoint == selectedCheckpoint && progress != scrollProgress) {
            scrollProgress = progress
            invalidate()
        }
    }
    
    fun setOnCheckpointSelectedListener(listener: (RecapCheckpoint) -> Unit) {
        onCheckpointSelectedListener = listener
    }
    
    private fun animateToCheckpoint() {
        animator?.cancel()
        invalidate()
    }
    
    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
}
