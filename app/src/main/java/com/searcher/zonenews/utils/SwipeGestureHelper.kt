package com.searcher.zonenews.utils

import android.view.MotionEvent
import android.view.View

/**
 * Helper class to detect right swipe gestures
 */
class SwipeGestureHelper(private val onSwipeRight: () -> Unit) : View.OnTouchListener {
    private var startX = 0f
    private var startY = 0f
    private val SWIPE_THRESHOLD = 100

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
            }
            MotionEvent.ACTION_UP -> {
                val endX = event.x
                val endY = event.y
                val diffX = endX - startX
                val diffY = endY - startY

                if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > SWIPE_THRESHOLD) {
                    if (diffX > 0) { // Right swipe
                        onSwipeRight.invoke()
                        return true
                    }
                }
            }
        }
        return false
    }
}
