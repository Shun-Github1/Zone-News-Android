package com.anssy.znewspro.ui.newsdetail

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.anssy.znewspro.R
import kotlin.math.abs

class SentimentMeterView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1f)
        strokeCap = Paint.Cap.BUTT
        color = ContextCompat.getColor(context, R.color.divider_color)
    }

	private val positivePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.STROKE
		strokeWidth = dp(12f)
		strokeCap = Paint.Cap.BUTT
		color = ContextCompat.getColor(context, R.color.score_positive)
	}

	private val negativePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.STROKE
		strokeWidth = dp(12f)
		strokeCap = Paint.Cap.BUTT
		color = ContextCompat.getColor(context, R.color.score_negative)
	}

	private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.FILL
		textSize = dp(12f)
		color = ContextCompat.getColor(context, R.color.colorTextSmall)
	}

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        textSize = dp(44f)
        color = ContextCompat.getColor(context, R.color.colorTextMiddle)
    }

	private var targetSentiment: Double = 0.0
	private var animatedSentiment: Float = 0f

	fun setSentiment(value: Double) {
		val clamped = when {
			value > 1.0 -> 1.0
			value < -1.0 -> -1.0
			else -> value
		}
		val start = animatedSentiment
		targetSentiment = clamped
		ValueAnimator.ofFloat(start, clamped.toFloat()).apply {
			duration = 600
			addUpdateListener {
				animatedSentiment = it.animatedValue as Float
				invalidate()
			}
			start()
		}
	}

	override fun onDraw(canvas: Canvas) {
		super.onDraw(canvas)
		val w = width.toFloat()
		val h = height.toFloat()
		val centerX = w / 2f
        val centerY = h * 0.82f
        val radius = (minOf(w, h) * 0.68f) - dp(6f)
		val left = centerX - radius
		val top = centerY - radius
		val right = centerX + radius
		val bottom = centerY + radius

		// Transparent track with only light gray edges (inner and outer), no rounded caps
		val halfBand = positivePaint.strokeWidth / 2f
		// Outer edge
		canvas.drawArc(left - halfBand, top - halfBand, right + halfBand, bottom + halfBand, 180f, 180f, false, edgePaint)
        // Inner edge
        canvas.drawArc(left + halfBand, top + halfBand, right - halfBand, bottom - halfBand, 180f, 180f, false, edgePaint)
        // Zero divider at top center bridging inner and outer edges
        canvas.drawLine(centerX, top - halfBand, centerX, top + halfBand, edgePaint)
		// Close the band at the base with short straight edges only spanning the band thickness
		val yBase = centerY
		// Left base connector
		canvas.drawLine(centerX - (radius + halfBand), yBase, centerX - (radius - halfBand), yBase, edgePaint)
		// Right base connector
		canvas.drawLine(centerX + (radius - halfBand), yBase, centerX + (radius + halfBand), yBase, edgePaint)

		// Sentiment arc with rounded caps from top center to left/right according to value magnitude
		val sentiment = animatedSentiment.toDouble()
		if (sentiment != 0.0) {
			val sweep = (abs(sentiment).toFloat() * 90f)
			val startAngle = 270f
			if (sentiment > 0) {
				canvas.drawArc(left, top, right, bottom, startAngle, sweep, false, positivePaint)
			} else {
				canvas.drawArc(left, top, right, bottom, startAngle, -sweep, false, negativePaint)
			}
		}

		// Labels aligned with horizontal edges of the semicircular arc (left/right ends)
		drawCenteredText(canvas, "0", centerX, centerY - radius - dp(18f), labelPaint)
        drawCenteredText(canvas, "-1", left, centerY + dp(12f), labelPaint)
        drawCenteredText(canvas, "+1", right, centerY + dp(12f), labelPaint)

		// Current value: color follows highlight (green for positive, red for negative; neutral gray for zero)
		val valueStr = (if (sentiment > 0) "+" else if (sentiment < 0) "" else "") + String.format("%.2f", sentiment)
		valuePaint.color = when {
			sentiment > 0 -> positivePaint.color
			sentiment < 0 -> negativePaint.color
			else -> ContextCompat.getColor(context, R.color.colorTextMiddle)
		}
		drawCenteredText(canvas, valueStr, centerX, centerY - dp(10f), valuePaint)
	}

	private fun drawCenteredText(canvas: Canvas, text: String, cx: Float, cy: Float, paint: Paint) {
		val bounds = Rect()
		paint.getTextBounds(text, 0, text.length, bounds)
		canvas.drawText(text, cx - bounds.exactCenterX(), cy - bounds.exactCenterY(), paint)
	}

	private fun dp(v: Float): Float = v * resources.displayMetrics.density
}


