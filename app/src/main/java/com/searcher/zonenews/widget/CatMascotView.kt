package com.searcher.zonenews.widget

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.random.Random

class CatMascotView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var sentiment: Double = 0.5
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sparklePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Animation states
    private var breathScale = 1.0f
    private var tailAngle = 0f
    private var earWiggleAngle = 0f
    private var sparkleState = 0f

    private val breathAnimator = ValueAnimator.ofFloat(0.98f, 1.02f).apply {
        duration = 2000
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = LinearInterpolator()
        addUpdateListener {
            breathScale = it.animatedValue as Float
            invalidate()
        }
    }

    private val tailAnimator = ValueAnimator.ofFloat(-15f, 5f).apply {
        duration = 1500
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = LinearInterpolator()
        addUpdateListener {
            tailAngle = it.animatedValue as Float
            invalidate()
        }
    }

    private val earAnimator = ValueAnimator.ofFloat(-3f, 3f).apply {
        duration = 180
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        interpolator = LinearInterpolator()
        addUpdateListener {
            earWiggleAngle = it.animatedValue as Float
            invalidate()
        }
    }

    private val sparkleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 1000
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            sparkleState = it.animatedValue as Float
            if (sentiment > 0.38) invalidate()
        }
    }

    init {
        // Setup paints
        strokePaint.color = Color.BLACK
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeCap = Paint.Cap.ROUND
        strokePaint.strokeJoin = Paint.Join.ROUND

        fillPaint.color = Color.WHITE
        fillPaint.style = Paint.Style.FILL

        accentPaint.color = Color.parseColor("#F4C29F") // Tan/Orange
        accentPaint.style = Paint.Style.FILL

        sparklePaint.color = Color.parseColor("#EFB315") // Darker Gold for visibility
        sparklePaint.style = Paint.Style.FILL

        breathAnimator.start()
        tailAnimator.start()
        earAnimator.start()
        sparkleAnimator.start()
    }

    fun setSentiment(value: Double) {
        this.sentiment = value.coerceIn(0.0, 1.0)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        breathAnimator.cancel()
        tailAnimator.cancel()
        earAnimator.cancel()
        sparkleAnimator.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2
        val cy = h / 2
        
        val minDim = w.coerceAtMost(h)
        val dynamicStrokeWidth = (minDim * 0.035f).coerceAtLeast(2f)
        strokePaint.strokeWidth = dynamicStrokeWidth
        
        val baseRadius = (minDim / 2) * 0.7f * breathScale

        // Draw Sequence
        if (sentiment > 0.38) {
            drawSparkles(canvas, cx, cy, baseRadius)
        }
        drawTail(canvas, cx, cy, baseRadius)
        drawEars(canvas, cx, cy, baseRadius)
        drawBodyHead(canvas, cx, cy, baseRadius)
        drawFace(canvas, cx, cy, baseRadius, dynamicStrokeWidth)
        drawPaws(canvas, cx, cy, baseRadius)
    }

    private fun drawSparkles(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val radius = r * 1.5f
        val count = 5
        for (i in 0 until count) {
            val angle = (Math.PI * 2 / count * i) + (sparkleState * Math.PI)
            val dist = radius + (sin(sparkleState * Math.PI * 2 + i).toFloat() * r * 0.1f)
            val sx = cx + cos(angle).toFloat() * dist
            val sy = cy + sin(angle).toFloat() * dist
            val size = r * 0.15f * (0.5f + 0.5f * sin(sparkleState * Math.PI * 2 + i).toFloat())
            
            drawStar(canvas, sx, sy, size)
        }
    }

    private fun drawStar(canvas: Canvas, x: Float, y: Float, size: Float) {
        path.reset()
        val points = 5
        val innerRatio = 0.4f
        var currentAngle = -Math.PI / 2
        val angleStep = Math.PI / points

        path.moveTo(
            (x + cos(currentAngle) * size).toFloat(),
            (y + sin(currentAngle) * size).toFloat()
        )

        for (i in 0 until points) {
            currentAngle += angleStep
            path.lineTo(
                (x + cos(currentAngle) * size * innerRatio).toFloat(),
                (y + sin(currentAngle) * size * innerRatio).toFloat()
            )
            currentAngle += angleStep
            path.lineTo(
                (x + cos(currentAngle) * size).toFloat(),
                (y + sin(currentAngle) * size).toFloat()
            )
        }
        path.close()
        canvas.drawPath(path, sparklePaint)
    }

    private fun drawTail(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        canvas.save()
        canvas.rotate(tailAngle, cx + r * 0.5f, cy + r * 0.5f)
        
        path.reset()
        val tailRootX = cx + r * 0.6f
        val tailRootY = cy + r * 0.5f
        
        val tailRect = RectF(tailRootX, tailRootY - r * 0.2f, tailRootX + r * 0.6f, tailRootY + r * 0.2f)
        path.addRoundRect(tailRect, r * 0.2f, r * 0.2f, Path.Direction.CW)
        
        canvas.drawPath(path, accentPaint)
        canvas.drawPath(path, strokePaint)
        canvas.restore()
    }

    private fun drawEars(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        // Left Ear
        canvas.save()
        canvas.translate(cx - r * 0.5f, cy - r * 0.6f)
        // Add wiggle to the base rotation
        canvas.rotate(-15f + earWiggleAngle) 
        drawSingleEar(canvas, r)
        canvas.restore()

        // Right Ear
        canvas.save()
        canvas.translate(cx + r * 0.5f, cy - r * 0.6f)
        canvas.rotate(15f - earWiggleAngle)
        canvas.scale(-1f, 1f)
        drawSingleEar(canvas, r)
        canvas.restore()
    }

    private fun drawSingleEar(canvas: Canvas, r: Float) {
        path.reset()
        val w = r * 0.5f
        val h = r * 0.6f
        
        path.moveTo(-w/2, h/2)
        path.quadTo(-w/2, -h/2, 0f, -h/2)
        path.quadTo(w/2, -h/2, w/2, h/2)
        path.close()

        canvas.drawPath(path, accentPaint)
        canvas.drawPath(path, strokePaint)
    }

    private fun drawBodyHead(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        path.reset()
        
        val headW = r * 1.8f
        val headH = r * 1.5f
        
        val left = cx - headW / 2
        val top = cy - headH / 2
        val right = cx + headW / 2
        val bottom = cy + headH / 2
        
        val rect = RectF(left, top, right, bottom)
        val corner = r * 0.6f
        
        path.addRoundRect(rect, corner, corner, Path.Direction.CW)
        
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)
    }

    private fun drawFace(canvas: Canvas, cx: Float, cy: Float, r: Float, baseStrokeWidth: Float) {
        // Cheeks
        val cheekY = cy + r * 0.15f
        val cheekXDist = r * 0.6f
        val cheekSize = r * 0.18f
        
        canvas.drawCircle(cx - cheekXDist, cheekY, cheekSize, accentPaint)
        canvas.drawCircle(cx + cheekXDist, cheekY, cheekSize, accentPaint)

        // Eyes
        val eyeY = cy 
        val eyeXDist = r * 0.25f
        val eyeSize = r * 0.08f
        
        when {
            sentiment > 0.38 -> {
                // Uplifting: Star Eyes
                drawStar(canvas, cx - eyeXDist, eyeY, eyeSize * 1.6f)
                drawStar(canvas, cx + eyeXDist, eyeY, eyeSize * 1.6f)
                // Draw outline for definition 
                val savedStroke = strokePaint.strokeWidth
                strokePaint.strokeWidth = baseStrokeWidth * 0.3f
                // We're just drawing filled stars above using sparklePaint, which is now darker gold.
            }
            sentiment > 0.33 -> {
                // Positive: Large shiny eyes
                drawShinyEye(canvas, cx - eyeXDist, eyeY, eyeSize * 1.3f)
                drawShinyEye(canvas, cx + eyeXDist, eyeY, eyeSize * 1.3f)
            }
            else -> {
                // Hopeful: Closed curved eyes (^_^), small smile
                drawClosedEye(canvas, cx - eyeXDist, eyeY, eyeSize * 1.2f, baseStrokeWidth)
                drawClosedEye(canvas, cx + eyeXDist, eyeY, eyeSize * 1.2f, baseStrokeWidth)
            }
        }

        // Mouth
        val mouthY = cy + r * 0.1f
        val mouthSize = r * 0.05f

        path.reset()
        strokePaint.strokeWidth = baseStrokeWidth * 0.65f 
        
        when {
            sentiment > 0.38 -> {
                // Uplifting: Open mouth (inverted rounded triangle or D shape)
                 val mSize = r * 0.08f
                 val mTop = mouthY + mSize * 0.5f
                 val rect = RectF(cx - mSize, mTop, cx + mSize, mTop + mSize * 1.5f)
                 path.addArc(rect, 0f, 180f)
                 path.close()
                 canvas.drawPath(path, fillPaint) // Black fill? Usually pink/red inside.
                 // Let's just outline for consistency or fill black/dark grey
                 val mPaint = Paint(fillPaint)
                 mPaint.color = Color.parseColor("#444444")
                 canvas.drawPath(path, mPaint)
            }
            sentiment > 0.33 -> {
                // Positive: W mouth
                path.moveTo(cx - mouthSize * 1.5f, mouthY)
                path.quadTo(cx - mouthSize * 0.75f, mouthY + mouthSize, cx, mouthY)
                path.quadTo(cx + mouthSize * 0.75f, mouthY + mouthSize, cx + mouthSize * 1.5f, mouthY)
                canvas.drawPath(path, strokePaint)
            }
            else -> {
                // Hopeful: Small smile (simple U)
                path.moveTo(cx - mouthSize, mouthY + mouthSize * 0.5f)
                path.quadTo(cx, mouthY + mouthSize * 2f, cx + mouthSize, mouthY + mouthSize * 0.5f)
                canvas.drawPath(path, strokePaint)
            }
        }
        strokePaint.strokeWidth = baseStrokeWidth // Reset
    }
    
    private fun drawShinyEye(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        fillPaint.color = Color.BLACK
        canvas.drawCircle(cx, cy, size, fillPaint)
        // Shine
        fillPaint.color = Color.WHITE
        canvas.drawCircle(cx + size * 0.3f, cy - size * 0.3f, size * 0.3f, fillPaint)
        canvas.drawCircle(cx - size * 0.2f, cy + size * 0.4f, size * 0.15f, fillPaint)
        fillPaint.color = Color.WHITE // Reset check
    }

    private fun drawClosedEye(canvas: Canvas, cx: Float, cy: Float, size: Float, strokeW: Float) {
        val savedStroke = strokePaint.strokeWidth
        strokePaint.strokeWidth = strokeW * 0.8f
        path.reset()
        // Curved up ^ shape
        path.moveTo(cx - size, cy)
        path.quadTo(cx, cy - size, cx + size, cy)
        canvas.drawPath(path, strokePaint)
        strokePaint.strokeWidth = savedStroke
    }
    
    private fun drawPaws(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        val pawY = cy + r * 0.75f 
        val pawXDist = r * 0.25f
        val pawW = r * 0.15f
        val pawH = r * 0.15f
        
        drawSinglePaw(canvas, cx - pawXDist, pawY, pawW, pawH)
        drawSinglePaw(canvas, cx + pawXDist, pawY, pawW, pawH)
    }

    private fun drawSinglePaw(canvas: Canvas, x: Float, y: Float, w: Float, h: Float) {
        path.reset()
        fillPaint.color = Color.WHITE
        val rect = RectF(x - w, y - h, x + w, y + h)
        path.addRoundRect(rect, w, h, Path.Direction.CW)
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)
    }
}
