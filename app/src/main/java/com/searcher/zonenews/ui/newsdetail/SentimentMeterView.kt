package com.searcher.zonenews.ui.newsdetail

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import com.searcher.zonenews.R
import kotlin.math.abs

class SentimentMeterView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(12f)
        strokeCap = Paint.Cap.BUTT
        color = ContextCompat.getColor(context, R.color.sentiment_track_bg)
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

    private val neutralPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(12f)
        strokeCap = Paint.Cap.BUTT
        color = ContextCompat.getColor(context, R.color.neutral_gray)
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
	private var hasAnimated: Boolean = false

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

	fun setSentimentWithAnimationFromZero(value: Double) {
		if (hasAnimated) {
			// Already animated, just set normally
			setSentiment(value)
			return
		}
		val clamped = when {
			value > 1.0 -> 1.0
			value < -1.0 -> -1.0
			else -> value
		}
		targetSentiment = clamped
		animatedSentiment = 0f
		hasAnimated = true
		ValueAnimator.ofFloat(0f, clamped.toFloat()).apply {
			duration = 800
			interpolator = AccelerateDecelerateInterpolator()
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
        // Adjust centerY to leave more space at the bottom for the new labels
        val centerY = h * 0.70f
        val radius = (minOf(w, h * 0.85f) * 0.68f) - dp(6f)
		val left = centerX - radius
		val top = centerY - radius
		val right = centerX + radius
		val bottom = centerY + radius

		// Draw the background track (solid grey)
		canvas.drawArc(left, top, right, bottom, 180f, 180f, false, trackPaint)

        // Zero divider at top center (optional, but might be good to keep for reference? User said "remove frame", implies cleaner look. Let's remove the divider too for now/make it simple solid track)

		// Sentiment arc with rounded caps from top center to left/right according to value magnitude
		val sentiment = animatedSentiment.toDouble()
		if (sentiment != 0.0) {
			val sweep = (abs(sentiment).toFloat() * 90f)
			val startAngle = 270f

            
            val paint = when {
                sentiment > 0.10001 -> positivePaint
                sentiment < -0.10001 -> negativePaint
                else -> neutralPaint
            }
            
			if (sentiment > 0) {
				canvas.drawArc(left, top, right, bottom, startAngle, sweep, false, paint)
			} else {
				canvas.drawArc(left, top, right, bottom, startAngle, -sweep, false, paint)
			}
		}

		// Labels aligned with horizontal edges of the semicircular arc (left/right ends)
		drawCenteredText(canvas, "0", centerX, centerY - radius - dp(18f), labelPaint)
        drawCenteredText(canvas, "-1", left, centerY + dp(12f), labelPaint)
        drawCenteredText(canvas, "+1", right, centerY + dp(12f), labelPaint)

        // Add descriptive labels under the -1 and +1 values (localized)
        val negativeLabel = resources.getString(R.string.sentiment_negative)
        val positiveLabel = resources.getString(R.string.sentiment_positive)
        drawCenteredText(canvas, negativeLabel, left, centerY + dp(28f), labelPaint)
        drawCenteredText(canvas, positiveLabel, right, centerY + dp(28f), labelPaint)

		// Current value: color follows highlight (green for positive, red for negative; neutral gray for zero/neutral)
		val valueStr = (if (sentiment > 0) "+" else if (sentiment < 0) "" else "") + String.format("%.2f", sentiment)
		valuePaint.color = when {
			sentiment > 0.10001 -> positivePaint.color
			sentiment < -0.10001 -> negativePaint.color
			else -> neutralPaint.color
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


