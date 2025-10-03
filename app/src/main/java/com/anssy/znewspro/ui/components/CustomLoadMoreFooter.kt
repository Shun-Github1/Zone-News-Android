package com.anssy.znewspro.ui.components

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
