package com.searcher.zonenews.ui.components

import android.content.Context
import android.util.AttributeSet
import com.scwang.smartrefresh.layout.footer.ClassicsFooter

/**
 * Custom footer that removes the "Load Success" message but keeps "Load Failed" message
 */
class CustomLoadMoreFooter @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ClassicsFooter(context, attrs, defStyleAttr) {

    init {
        // Hide the title text
        setTextSizeTitle(0f)
        // Hide the arrow by setting size to 0 and resource to transparent
        setDrawableArrowSize(0f)
        setArrowResource(android.R.color.transparent)
        // Remove margin to ensure the loading circle is strictly centered
        setDrawableMarginRight(0f)
        
        // Ensure spinner style is consistent
        setSpinnerStyle(com.scwang.smartrefresh.layout.constant.SpinnerStyle.Translate)
        
        // Force hide any TextViews to ensure strict centering of the spinner
        // Force hide any TextViews to ensure strict centering of the spinner
        post {
            for (i in 0 until childCount) {
                val view = getChildAt(i)
                if (view is android.widget.TextView) {
                    view.visibility = android.view.View.GONE
                    view.text = ""
                } else if (view is android.widget.ImageView) {
                    // Force the spinner (and arrow if present) to be strictly centered
                    val params = view.layoutParams as? android.widget.RelativeLayout.LayoutParams
                    params?.let {
                        it.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT)
                        it.removeRule(android.widget.RelativeLayout.LEFT_OF)
                        it.removeRule(android.widget.RelativeLayout.RIGHT_OF)
                        it.removeRule(android.widget.RelativeLayout.START_OF)
                        it.removeRule(android.widget.RelativeLayout.END_OF)
                        it.marginEnd = 0
                        it.rightMargin = 0
                        view.layoutParams = it
                    }
                }
            }
        }
    }

    override fun onFinish(refreshLayout: com.scwang.smartrefresh.layout.api.RefreshLayout, success: Boolean): Int {
        // Only show message for failed loads, not successful ones
        return if (success) {
            // Return 0 to not show any message for success
            0
        } else {
            // Show the default failed message
            super.onFinish(refreshLayout, success)
        }
    }
}
