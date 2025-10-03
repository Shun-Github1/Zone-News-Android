package com.anssy.znewspro.selfview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import com.anssy.znewspro.R
import com.anssy.znewspro.utils.HapticFeedbackHelper
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
    private val itemSpacing = dpToPx(120f)
    private val itemSpacingSelected = dpToPx(120f) // Keep same spacing to prevent shifting
    
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
            val currentSpacing = itemSpacing // Use consistent spacing
            
            // Draw circle with equal spacing using padding
            val radius = if (isSelected) circleRadiusSelected else circleRadius
            val maxRadius = circleRadiusSelected // Use the larger radius for consistent spacing
            val padding = maxRadius - radius // Calculate padding to center smaller circles
            
            if (isSelected) {
                circlePaint.color = brandColor
                canvas.drawCircle(centerX, currentY, radius, circlePaint)
            }
            
            circleStrokePaint.color = if (isSelected) brandColor else textSecondaryColor
            circleStrokePaint.alpha = if (isSelected) 255 else 128
            canvas.drawCircle(centerX, currentY, radius, circleStrokePaint)
            
            // Draw label
            val label = checkpoint.getTitle(context)
            textPaint.color = if (isSelected) brandColor else textSecondaryColor
            textPaint.textSize = textSize * 1.05f // Always use selected state font size
            textPaint.isFakeBoldText = isSelected
            
            // Draw text below circle (positioned relative to the max circle area)
            val textY = currentY + maxRadius + dpToPx(20f)
            canvas.drawText(label, centerX, textY, textPaint)
            
            // Draw connecting line (for all items, including monthly)
            val lineStartY = textY + dpToPx(15f)
            val lineEndY = currentY + currentSpacing - maxRadius - dpToPx(15f) // Fixed end position relative to max circle
            val lineHeight = lineEndY - lineStartY
            
            // Background line
            canvas.drawLine(centerX, lineStartY, centerX, lineEndY, linePaint)
            
            // Progress line (for selected checkpoint)
            if (isSelected && scrollProgress > 0f) {
                val progressHeight = lineHeight * scrollProgress
                canvas.drawLine(centerX, lineStartY, centerX, lineStartY + progressHeight, lineProgressPaint)
            }
            
            currentY += currentSpacing
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
        val centerX = width / 2f
        val totalHeight = calculateTotalHeight()
        val startY = (height - totalHeight) / 2f
        
        var currentY = startY
        
        RecapCheckpoint.values().forEach { checkpoint ->
            val isSelected = checkpoint == selectedCheckpoint
            val currentSpacing = itemSpacing // Use consistent spacing
            val radius = if (isSelected) circleRadiusSelected else circleRadius
            val maxRadius = circleRadiusSelected // Use the larger radius for consistent spacing
            
            // Check if tap is within this checkpoint's region (circle + text area)
            val tapThreshold = dpToPx(40f) // Larger tap area
            if (y >= currentY - tapThreshold && y <= currentY + tapThreshold) {
                HapticFeedbackHelper.performNavigationHaptic(this)
                setSelectedCheckpoint(checkpoint)
                onCheckpointSelectedListener?.invoke(checkpoint)
                return
            }
            
            currentY += currentSpacing
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
        // Calculate height for all checkpoints plus the extra progress line under monthly
        return RecapCheckpoint.values().size * itemSpacing + dpToPx(60f) // Extra space for monthly progress line
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
