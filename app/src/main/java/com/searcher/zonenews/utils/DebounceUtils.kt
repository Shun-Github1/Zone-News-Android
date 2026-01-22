package com.searcher.zonenews.utils

import android.view.View

/**
 * Utility for handling debounce click events to prevent multiple rapid clicks.
 */
object DebounceUtils {
    
    /**
     * Extension function for View to set a debounce click listener.
     * @param delayMillis The minimum time interval between clicks in milliseconds. Default is 500ms.
     * @param onClick The action to perform on click.
     */
    fun View.setOnDebounceClickListener(delayMillis: Long = 500L, onClick: (View) -> Unit) {
        setOnClickListener(object : View.OnClickListener {
            private var lastClickTime: Long = 0

            override fun onClick(v: View) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime > delayMillis) {
                    lastClickTime = currentTime
                    onClick(v)
                }
            }
        })
    }
}
