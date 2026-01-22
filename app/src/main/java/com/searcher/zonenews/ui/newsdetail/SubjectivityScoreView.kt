package com.searcher.zonenews.ui.newsdetail

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
import com.searcher.zonenews.R
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
			value <= 0.423 -> context.getString(R.string.low_subjectivity)
			value <= 0.575 -> context.getString(R.string.medium_subjectivity)
			else -> context.getString(R.string.high_subjectivity)
		}
		statusTv.text = status
		applyStatusColors(value)
	}

	private fun applyStatusColors(value: Double) {
		val (textColor, bgColor) = when {
			value <= 0.423 -> Pair(ContextCompat.getColor(context, R.color.subjectivity_low), ContextCompat.getColor(context, R.color.subjectivity_bg_low))
			value <= 0.575 -> Pair(ContextCompat.getColor(context, R.color.subjectivity_medium), ContextCompat.getColor(context, R.color.subjectivity_bg_medium))
			else -> Pair(ContextCompat.getColor(context, R.color.subjectivity_high), ContextCompat.getColor(context, R.color.subjectivity_bg_high))
		}
		// Ensure text color has no opacity by setting alpha to fully opaque
		val opaqueTextColor = Color.argb(255, Color.red(textColor), Color.green(textColor), Color.blue(textColor))
		statusTv.setTextColor(opaqueTextColor)
		val bg = GradientDrawable()
		bg.cornerRadius = resources.displayMetrics.density * 4f
		bg.setColor(bgColor)
		statusTv.background = bg
	}

    private fun showInfoPopover(anchor: View) {
        val content = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(dp(16f).toInt(), dp(20f).toInt(), dp(16f).toInt(), dp(20f).toInt())
            
            val rawText = context.getString(R.string.subjectivity_algorithm_description)
            val parts = rawText.split("\n")
            val mainBody = parts.getOrElse(0) { "" }
            val linkLine = parts.getOrElse(1) { "" }

            addView(TextView(context).apply {
                text = mainBody
                setTextColor(ContextCompat.getColor(context, R.color.colorTextDeep))
                textSize = 16f
            })

            if (linkLine.isNotEmpty()) {
                val linkTextView = TextView(context).apply {
                    layoutParams = LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(12f).toInt()
                    }

                    val linkText = context.getString(R.string.our_webpage)
                    val spannable = SpannableString(linkLine)
                    val start = linkLine.indexOf(linkText)
                    if (start >= 0) {
                        val end = start + linkText.length
                        spannable.setSpan(object : ClickableSpan() {
                            override fun onClick(widget: View) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.example_website)))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }

                            override fun updateDrawState(ds: android.text.TextPaint) {
                                super.updateDrawState(ds)
                                ds.color = ContextCompat.getColor(context, R.color.brand_primary)
                            }
                        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }

                    text = spannable
                    setTextColor(ContextCompat.getColor(context, R.color.colorTextSmall))
                    textSize = 14f
                    movementMethod = LinkMovementMethod.getInstance()
                    highlightColor = Color.TRANSPARENT
                }
                addView(linkTextView)
            }
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


