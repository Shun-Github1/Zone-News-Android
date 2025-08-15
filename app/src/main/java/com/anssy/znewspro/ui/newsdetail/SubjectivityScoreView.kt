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
		orientation = HORIZONTAL
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
			value > 0.66 -> "Low objectivity"
			value > 0.33 && value < 0.66 -> "Medium objectivity"
			value < 0.33 -> "High objectivity"
			else -> "Medium objectivity"
		}
		statusTv.text = status
		applyStatusColors(value)
	}

	private fun applyStatusColors(value: Double) {
		val (textColor, bgColor) = when {
			value > 0.66 -> Pair(Color.parseColor("#879693"), Color.parseColor("#2E3D3A"))
			value > 0.33 && value < 0.66 -> Pair(Color.parseColor("#9AEDDD"), Color.parseColor("#3D776C"))
			value < 0.33 -> Pair(Color.parseColor("#239B98"), Color.parseColor("#9AEDDD"))
			else -> Pair(Color.parseColor("#9AEDDD"), Color.parseColor("#3D776C"))
		}
		statusTv.setTextColor(textColor)
		val bg = GradientDrawable()
		bg.cornerRadius = resources.displayMetrics.density * 4f
		bg.setColor(bgColor)
		statusTv.background = bg
	}

    private fun showInfoPopover(anchor: View) {
        val content = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(dp(16f).toInt(), dp(12f).toInt(), dp(16f).toInt(), dp(12f).toInt())
            val fullText = "Determined by a stolen algorithm from a…\nVisit our webpage for detailed information."
            val linkText = "our webpage"
            val spannable = SpannableString(fullText)
            val start = fullText.indexOf(linkText)
            if (start >= 0) {
                val end = start + linkText.length
                spannable.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val linkColor = Color.parseColor("#219dff")
                spannable.setSpan(ForegroundColorSpan(linkColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            val tv = TextView(context).apply {
                text = spannable
                setTextColor(Color.parseColor("#333333"))
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
        popup.elevation = dp(12f)
        popup.setBackgroundDrawable(android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = dp(12f)
            setColor(Color.WHITE)
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


