package com.searcher.zonenews.selfview.popup

import android.content.Context
import android.view.View
import com.searcher.zonenews.R
import razerdp.basepopup.BasePopupWindow
import razerdp.util.animation.AnimationHelper

class NewsDetailMorePopupWindow(
    context: Context,
    private val onFeedbackClick: () -> Unit,
    private val onEditModeClick: () -> Unit
) : BasePopupWindow(context) {

    init {
        setContentView(R.layout.news_detail_more_popup)
        setBlurBackgroundEnable(false)
        initView()
    }

    private fun initView() {
        val content = contentView
        
        content.findViewById<View>(R.id.menu_feedback).setOnClickListener {
            onFeedbackClick()
            dismiss()
        }

        content.findViewById<View>(R.id.menu_edit_mode).setOnClickListener {
            onEditModeClick()
            dismiss()
        }
    }

    override fun onCreateShowAnimation(): android.view.animation.Animation? {
        return AnimationHelper.asAnimation()
            .withAlpha(razerdp.util.animation.AlphaConfig.IN)
            .toShow()
    }

    override fun onCreateDismissAnimation(): android.view.animation.Animation? {
        return AnimationHelper.asAnimation()
            .withAlpha(razerdp.util.animation.AlphaConfig.OUT)
            .toDismiss()
    }

    override fun showPopupWindow(anchorView: View?) {
        if (anchorView == null) return
        
        val anchorHeight = anchorView.height
        val anchorWidth = anchorView.width
        
        // Measure popup to get its width
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWidth = contentView.measuredWidth
        
        // Align popup's right edge with anchor's right edge
        // BasePopupWindow default aligns left edges
        setOffsetX(anchorWidth - popupWidth)
        
        // Align popup's top edge with anchor's top edge
        // BasePopupWindow default aligns popup top with anchor bottom
        setOffsetY(-anchorHeight)
        
        super.showPopupWindow(anchorView)
    }
}
