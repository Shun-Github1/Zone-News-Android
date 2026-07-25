package com.searcher.zonenews.selfview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.widget.NestedScrollView
import com.searcher.zonenews.R

/**
 * TutorialOverlayView - A full-screen overlay that darkens everything except a highlighted area
 * and displays a message bubble above the highlighted area.
 * 
 * Features:
 * - Darkens all content except the focus area
 * - Displays a speech bubble with tutorial message
 * - Advances to next step on tap anywhere
 * - Auto-scrolls to bring highlighted elements into view
 * - Clean appear/disappear transitions
 */
class TutorialOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Paint for the dark overlay
    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#CC000000") // 80% black
        style = Paint.Style.FILL
    }

    // Paint for cutting out the highlight area
    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.FILL
    }

    // The highlighted area (excluded from darkening)
    private var highlightRect: RectF? = null
    private val highlightPadding = 4f // dp converted later (restored small padding)
    private val highlightCornerRadius = 12f // dp converted later (restored larger radius)

    // Tutorial steps
    private var steps: List<TutorialStep> = emptyList()
    private var currentStepIndex = 0
    
    // Callback when tutorial completes
    private var onTutorialComplete: (() -> Unit)? = null
    
    // Bubble view
    private var bubbleView: View? = null
    private var bubbleTextView: TextView? = null

    private val density = context.resources.displayMetrics.density
    
    init {
        // Enable hardware layer for xfermode to work
        setLayerType(LAYER_TYPE_HARDWARE, null)
        
        // Allow clicks to pass through to the handler
        setOnClickListener {
            advanceToNextStep()
        }
        
        // Create bubble view
        createBubbleView()
    }

    private fun createBubbleView() {
        // Inflate the bubble layout
        bubbleView = LayoutInflater.from(context).inflate(R.layout.tutorial_bubble, this, false)
        bubbleTextView = bubbleView?.findViewById(R.id.tutorial_bubble_text)
        
        // Hide the close button since we're using tap to continue
        bubbleView?.findViewById<View>(R.id.close_iv)?.visibility = View.GONE
        
        // Set width to match info popups (screen width - 40dp margins)
        val screenWidth = context.resources.displayMetrics.widthPixels
        val cardMargin = (20 * density).toInt() * 2 // 20dp margin on each side
        val bubbleWidth = screenWidth - cardMargin
        
        bubbleView?.layoutParams = LayoutParams(bubbleWidth, LayoutParams.WRAP_CONTENT)
        
        addView(bubbleView)
        bubbleView?.visibility = View.INVISIBLE
    }

    fun setTutorialSteps(steps: List<TutorialStep>) {
        this.steps = steps
        currentStepIndex = 0
    }

    fun setOnTutorialCompleteListener(listener: () -> Unit) {
        this.onTutorialComplete = listener
    }

    /**
     * Start the tutorial with the given steps
     * @param scrollView Optional NestedScrollView to auto-scroll
     * @param getTargetView Function that returns the target view for each step
     */
    fun start(
        scrollView: NestedScrollView? = null, 
        getTargetView: (TutorialStep) -> View?
    ) {
        if (steps.isEmpty()) {
            onTutorialComplete?.invoke()
            return
        }
        
        visibility = View.VISIBLE
        alpha = 0f
        animate().alpha(1f).setDuration(200).start()
        
        showStep(currentStepIndex, scrollView, getTargetView)
    }

    private fun showStep(
        index: Int, 
        scrollView: NestedScrollView?,
        getTargetView: (TutorialStep) -> View?
    ) {
        if (index >= steps.size) {
            complete()
            return
        }

        val step = steps[index]
        
        // Reset bubble visibility for fresh appearance
        bubbleView?.visibility = View.INVISIBLE
        bubbleView?.alpha = 0f
        
        // Get target view first to calculate proper scroll position
        val targetView = getTargetView(step)
        
        if (scrollView != null && targetView != null && step.hasHighlight) {
            scrollView.post {
                // Calculate scroll needed to position element as far up as possible
                val screenHeight = height
                val scrollViewLocation = IntArray(2)
                scrollView.getLocationOnScreen(scrollViewLocation)
                val scrollViewTop = scrollViewLocation[1]
                
                val targetLocationOnScreen = IntArray(2)
                targetView.getLocationOnScreen(targetLocationOnScreen)
                val currentTargetTop = targetLocationOnScreen[1]
                
                val targetHeight = targetView.height.toFloat()
                
                // Determine the desired top position on screen
                // For tall elements (>50% screen): top at 10% from screen top
                // For normal elements: top at 25% from screen top
                val desiredTopOnScreen = if (targetHeight > screenHeight * 0.5f) {
                    screenHeight * 0.10f
                } else {
                    screenHeight * 0.25f
                }
                
                // Calculate how much we need to scroll to achieve the desired position
                // Current position + scroll delta = desired position
                // scroll delta = desired position - current position
                val scrollDelta = currentTargetTop - desiredTopOnScreen
                val newScrollY = scrollView.scrollY + scrollDelta.toInt()
                
                // Clamp to valid scroll range
                val targetScrollY = newScrollY.coerceAtLeast(0)
                
                scrollView.smoothScrollTo(0, targetScrollY)
                
                // Wait for scroll to complete before showing highlight
                scrollView.postDelayed({
                    showHighlightAndBubble(step, scrollView, getTargetView)
                }, 350)
            }
        } else {
            showHighlightAndBubble(step, scrollView, getTargetView)
        }
    }

    private fun showHighlightAndBubble(
        step: TutorialStep,
        scrollView: NestedScrollView?,
        getTargetView: (TutorialStep) -> View?
    ) {
        val targetView = getTargetView(step)
        
        if (targetView != null && step.hasHighlight) {
            // Calculate the highlight rect in this view's coordinates
            val location = IntArray(2)
            targetView.getLocationOnScreen(location)
            
            val overlayLocation = IntArray(2)
            this.getLocationOnScreen(overlayLocation)
            
            val padding = highlightPadding * density
            highlightRect = RectF(
                (location[0] - overlayLocation[0]).toFloat() - padding,
                (location[1] - overlayLocation[1]).toFloat() - padding,
                (location[0] - overlayLocation[0] + targetView.width).toFloat() + padding,
                (location[1] - overlayLocation[1] + targetView.height).toFloat() + padding
            )
        } else {
            // No highlight for this step
            highlightRect = null
        }
        
        // Update bubble text and position
        bubbleTextView?.text = step.message
        
        // Position bubble below highlight or at a fixed position
        positionBubble(step)
        
        // Show bubble with fade-in animation
        bubbleView?.visibility = View.VISIBLE
        bubbleView?.animate()
            ?.alpha(1f)
            ?.setDuration(150)
            ?.start()
        
        // Redraw overlay
        invalidate()
    }

    private fun positionBubble(step: TutorialStep) {
        bubbleView?.let { bubble ->
            bubble.post {
                val bubbleWidth = bubble.width
                val bubbleHeight = bubble.height
                val screenHeight = height
                val screenCenterY = screenHeight / 2f
                
                // Center horizontally (bubble width is already set to match info popups)
                val margin = 20 * density // Same as card margin
                val x = margin
                
                if (highlightRect != null) {
                    val smallMargin = 8 * density
                    
                    // Calculate potential Y positions
                    // Option 1: Below element
                    val yBelow = highlightRect!!.bottom + smallMargin
                    
                    // Option 2: Above element
                    val yAbove = highlightRect!!.top - bubbleHeight - smallMargin
                    
                    // Calculate centers of potential bubble positions
                    val centerBelow = yBelow + bubbleHeight / 2f
                    val centerAbove = yAbove + bubbleHeight / 2f
                    
                    // Determine which is closer to screen center
                    val distBelow = kotlin.math.abs(centerBelow - screenCenterY)
                    val distAbove = kotlin.math.abs(centerAbove - screenCenterY)
                    
                    // Check validity (must fit on screen with some margin)
                    val topEdgeSafe = 16 * density // Minimum distance from top
                    val bottomEdgeSafe = screenHeight - (16 * density) // Maximum bottom Y
                    
                    val isBelowValid = (yBelow + bubbleHeight) <= bottomEdgeSafe
                    val isAboveValid = yAbove >= topEdgeSafe
                    
                    var finalY: Float
                    
                    if (isBelowValid && isAboveValid) {
                        // Both valid, pick the more centered one
                        finalY = if (distBelow <= distAbove) yBelow else yAbove
                    } else if (isBelowValid) {
                        finalY = yBelow
                    } else if (isAboveValid) {
                        finalY = yAbove
                    } else {
                        // Neither fits perfectly, pick the one that fits arguably "better" 
                        // or default to below and clamp
                        finalY = if (distBelow <= distAbove) yBelow else yAbove
                    }
                    
                    // Apply safety clamping regardless of choice
                    // Constraint: bubble should be no less than 20% screen height from bottom
                    val maxBubbleTop = screenHeight * 0.80f - bubbleHeight
                    
                    // Also clamp to top (16dp)
                    finalY = finalY.coerceIn(16 * density, maxBubbleTop)
                    
                    bubble.translationX = x
                    bubble.translationY = finalY
                } else {
                    // No highlight - center bubble vertically and horizontally
                    val y = (height - bubbleHeight) / 2f - 50 * density // Slightly above center
                    
                    bubble.translationX = x
                    bubble.translationY = y
                }
            }
        }
    }


    private fun advanceToNextStep() {
        currentStepIndex++
        
        // Hide current bubble instantly
        bubbleView?.animate()?.cancel()
        bubbleView?.visibility = View.INVISIBLE
        bubbleView?.alpha = 0f
        
        if (currentStepIndex >= steps.size) {
            complete()
        } else {
            // Invalidate to clear highlight immediately
            highlightRect = null
            invalidate()
            
            // Use post to give time for the view state to update
            post {
                // Re-show with the stored callbacks - this is a simplified flow
                // In actual usage, start() should be called with proper callbacks
            }
        }
    }
    
    /**
     * Advance to next step with access to the view lookup function
     */
    fun advanceWithCallback(
        scrollView: NestedScrollView?,
        getTargetView: (TutorialStep) -> View?
    ) {
        currentStepIndex++
        
        // Hide current bubble instantly
        bubbleView?.animate()?.cancel()
        bubbleView?.visibility = View.INVISIBLE
        bubbleView?.alpha = 0f
        highlightRect = null
        invalidate()
        
        if (currentStepIndex >= steps.size) {
            complete()
        } else {
            showStep(currentStepIndex, scrollView, getTargetView)
        }
    }

    private fun complete() {
        animate()
            .alpha(0f)
            .setDuration(200)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                    (parent as? ViewGroup)?.removeView(this@TutorialOverlayView)
                    onTutorialComplete?.invoke()
                }
            })
            .start()
    }

    override fun dispatchDraw(canvas: Canvas) {
        // Save layer for porter-duff operations
        val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        
        // Draw dark overlay
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
        
        // Cut out highlight area if present
        highlightRect?.let { rect ->
            val cornerRadius = highlightCornerRadius * density
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, clearPaint)
        }
        
        canvas.restoreToCount(saveCount)
        
        // Draw children (bubble) on top
        super.dispatchDraw(canvas)
    }

    /**
     * Data class representing a single tutorial step
     */
    data class TutorialStep(
        val id: String,                    // Unique identifier for this step
        val message: String,               // The localized message to show
        val hasHighlight: Boolean = true,  // Whether to highlight an area
        val scrollPosition: Int? = null,   // Scroll position (null = top)
        val targetViewId: Int? = null      // Optional view ID to highlight
    )
}
