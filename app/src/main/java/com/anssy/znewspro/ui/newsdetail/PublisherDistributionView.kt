package com.anssy.znewspro.ui.newsdetail

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.anssy.znewspro.R
import com.bumptech.glide.Glide

class PublisherDistributionView @JvmOverloads constructor(
	context: Context,
	attrs: AttributeSet? = null,
	defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

	data class Icon(
		val size: Double,
		val rx: Double,
		val ry: Double,
		val logo: String
	)

	data class Data(
		val centricPercent: Double,
		val centricIcons: List<Icon>,
		val progressiveIcons: List<Icon>
	)

	private var data: Data? = null
	private val cornerRadius: Float = dp(8f)
	private val leftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.FILL
		color = ContextCompat.getColor(context, R.color.colorTextSmall) // #999999
	}
	private val rightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.FILL
		color = ContextCompat.getColor(context, R.color.c_color) // close to #9AEDDD container
	}
	private val separatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.FILL
		color = ContextCompat.getColor(context, R.color.white)
	}

	fun setData(d: Data) {
		data = d
		requestLayout()
		invalidate()
		buildIcons()
	}

	override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
		super.onSizeChanged(w, h, oldw, oldh)
		outlineProvider = object : android.view.ViewOutlineProvider() {
			override fun getOutline(view: View, outline: Outline) {
				outline.setRoundRect(0, 0, w, h, cornerRadius)
			}
		}
		clipToOutline = true
	}

	private fun buildIcons() {
		removeAllViews()
		val d = data ?: return
		val all = ArrayList<Pair<Icon, Boolean>>()
		all.addAll(d.centricIcons.map { it to false })
		all.addAll(d.progressiveIcons.map { it to true })
		all.forEach { (icon, isProg) ->
			val iv = ImageView(context)
			iv.scaleType = ImageView.ScaleType.CENTER_CROP
			Glide.with(iv).load(icon.logo).placeholder(R.drawable.ease_default_image).error(R.drawable.ease_default_image).into(iv)
			iv.clipToOutline = true
			iv.tag = Pair(icon, isProg)
			iv.contentDescription = "publisher"
			addView(iv)
			val lp = LayoutParams(0, 0)
			iv.layoutParams = lp
		}
	}

	override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
		super.onLayout(changed, l, t, r, b)
		positionIcons()
	}

	private fun positionIcons() {
		val d = data ?: return
		val width = width.toFloat()
		val height = height.toFloat()
		val separationX = (width * d.centricPercent).toFloat()
		for (i in 0 until childCount) {
			val iv = getChildAt(i) as ImageView
			@Suppress("UNCHECKED_CAST")
			val pair = iv.tag as Pair<Icon, Boolean>
			val icon = pair.first
			val isProg = pair.second
			val minSize = dp(16f)
			val maxSize = dp(50f)
			val sizePx = (minSize + (maxSize - minSize) * icon.size).toInt()
			val leftBound = (if (isProg) separationX else 0f) + (sizePx/2f)
			val rightBound = (if (isProg) width else separationX) - (sizePx/2f)
			val x = leftBound + (rightBound - leftBound) * icon.rx.toFloat()
			val upperBound = height - sizePx/2f
			val lowerBound = 0f + sizePx	/2f
			val y = height - (lowerBound + (upperBound - lowerBound) * icon.ry.toFloat())
			iv.layout((x - sizePx / 2f).toInt(), (y - sizePx / 2f).toInt(), (x + sizePx / 2f).toInt(), (y + sizePx / 2f).toInt())
			iv.outlineProvider = CircleOutlineProvider()
		}
	}

	override fun dispatchDraw(canvas: Canvas) {
		// draw background rectangles and separator behind children
		val d = data
		if (d != null) {
			val width = width.toFloat()
			val height = height.toFloat()
			val separationX = (width * d.centricPercent).toFloat()
			canvas.drawRect(0f, 0f, separationX, height, leftPaint)
			canvas.drawRect(separationX, 0f, width, height, rightPaint)
			canvas.drawRect(separationX - dp(1f), 0f, separationX + dp(1f), height, separatorPaint)
		}
		super.dispatchDraw(canvas)
	}

	private fun dp(v: Float): Float = v * resources.displayMetrics.density
}

internal class CircleOutlineProvider : android.view.ViewOutlineProvider() {
	override fun getOutline(view: View, outline: android.graphics.Outline) {
		outline.setOval(0, 0, view.width, view.height)
	}
}
