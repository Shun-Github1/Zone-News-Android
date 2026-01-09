package com.anssy.znewspro.ui.newsdetail

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import com.anssy.znewspro.R
import androidx.core.content.ContextCompat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class SubjectivityScoreView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

	private val valueTv: TextView
	private val denomTv: TextView
	private val statusTv: TextView
    private val infoBtn: View

	init {
		orientation = VERTICAL
		LayoutInflater.from(context).inflate(R.layout.view_subjectivity_score, this, true)
		valueTv = findViewById(R.id.subjectivity_value)
		denomTv = findViewById(R.id.subjectivity_denom)
		statusTv = findViewById(R.id.subjectivity_status)
        infoBtn = findViewById(R.id.info_btn)
        infoBtn.setOnClickListener { showInfoPopover(it) }
	}

    fun setSubjectivity(value: Double) {
        val dfs = DecimalFormatSymbols(Locale.US)
        val df = DecimalFormat("#.##", dfs).apply { isGroupingUsed = false }
        val formatted = df.format(value)
		valueTv.text = formatted
		denomTv.text = "/1"
		val status = when {
			value > 0.66 -> context.getString(R.string.low_objectivity)
			value > 0.33 && value < 0.66 -> context.getString(R.string.medium_objectivity)
			value < 0.33 -> context.getString(R.string.high_objectivity)
			else -> context.getString(R.string.medium_objectivity)
		}
		statusTv.text = status
		applyStatusColors(value)
	}

	private fun applyStatusColors(value: Double) {
		val (textColor, bgColor) = when {
			value > 0.66 -> Pair(ContextCompat.getColor(context, R.color.subjectivity_high), ContextCompat.getColor(context, R.color.subjectivity_bg_high))
			value > 0.33 && value < 0.66 -> Pair(ContextCompat.getColor(context, R.color.subjectivity_medium), ContextCompat.getColor(context, R.color.subjectivity_bg_medium))
			value < 0.33 -> Pair(ContextCompat.getColor(context, R.color.subjectivity_low), ContextCompat.getColor(context, R.color.subjectivity_bg_low))
			else -> Pair(ContextCompat.getColor(context, R.color.subjectivity_medium), ContextCompat.getColor(context, R.color.subjectivity_bg_medium))
		}
		// Ensure text color has no opacity by setting alpha to fully opaque
		val opaqueTextColor = android.graphics.Color.argb(255, android.graphics.Color.red(textColor), android.graphics.Color.green(textColor), android.graphics.Color.blue(textColor))
		statusTv.setTextColor(opaqueTextColor)
		val bg = GradientDrawable()
		bg.cornerRadius = resources.displayMetrics.density * 4f
		bg.setColor(bgColor)
		statusTv.background = bg
	}

    private fun showInfoPopover(anchor: View) {
        val content = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(16f).toInt(), dp(12f).toInt(), dp(16f).toInt(), dp(12f).toInt())
            val fullText = context.getString(R.string.subjectivity_algorithm_description)
            val linkText = context.getString(R.string.our_webpage)
            val spannable = SpannableString(fullText)
            val start = fullText.indexOf(linkText)
            if (start >= 0) {
                val end = start + linkText.length
                spannable.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.example_website)))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val linkColor = ContextCompat.getColor(context, R.color.link_color)
                spannable.setSpan(ForegroundColorSpan(linkColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            val tv = TextView(context).apply {
                text = spannable
                setTextColor(ContextCompat.getColor(context, R.color.colorTextSmall))
                textSize = 14f
                movementMethod = LinkMovementMethod.getInstance()
            }
            addView(tv)
        }
        // Make the popup the same width as the card content (this view)
        val popupWidth = if (width > 0) width else LayoutParams.MATCH_PARENT
        val popup = android.widget.PopupWindow(content, popupWidth, LayoutParams.WRAP_CONTENT, true)
        popup.isOutsideTouchable = true
        popup.isFocusable = true
        popup.elevation = dp(4f)
        popup.setBackgroundDrawable(GradientDrawable().apply {
            cornerRadius = dp(12f)
            setColor(ContextCompat.getColor(context, R.color.profile_card_bg))
        })
        // Align popup's left edge with the card's left edge (negative offset to move left from anchor)
        // Include this view's left padding so we snap to the card boundary, not the inner content.
        val xoff = - (anchor.left + dp(16f).toInt())
        // Align the BOTTOM of the popup with the icon's UPPER edge
        content.measure(MeasureSpec.makeMeasureSpec(popupWidth, MeasureSpec.EXACTLY), MeasureSpec.UNSPECIFIED)
        val popupHeight = content.measuredHeight
        val yoff = -(popupHeight + anchor.height + dp(6f).toInt())
        popup.showAsDropDown(anchor, xoff, yoff)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}


