package com.phamnhantucode.photoeditor.views

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Point
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.extension.dp
import com.phamnhantucode.photoeditor.extension.then

@SuppressLint("Recycle")
class VerticalSeekBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {


    var shouldShowCircle: Boolean = true
    var onValueChangeListener = { _: Float -> }

    var minValue = 0f
    var maxValue = 100f


    var currentValue = 0f
        set(value) {
            field = value
            onValueChangeListener(value)
            invalidate()
        }

    private val triangleSegment = Path()
    private val triangleSegmentTopWidth = 16f.dp
    private val triangleBotWidth = 4f.dp

    private val trianglePaintingRect = Rect()
    private val trianglePaintingRectPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private var currentHeight = 0f

    private val triangleFullRect = Rect()
        get() = field.apply {
            set(
                trianglePaintingRect.left,
                0,
                trianglePaintingRect.right,
                height
            )
        }


    //circle demonstrate
    val circleRadiusTransformer: (Float) -> Float = { it }
    private val circleDemonstratePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(12f, 0f, 0f, Color.BLACK)
    }

    private val centerPoint = Point()
    private var isFromUser = false

    private val triangleSegmentBorderPaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private var expandProgress = 0f
    private val expandAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 250
        interpolator = DecelerateInterpolator()
        addUpdateListener {
            expandProgress = it.animatedValue as Float
            invalidate()
        }
    }

    private var startPadding = 8f.dp

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        val typeArrays = context.obtainStyledAttributes(attrs, R.styleable.VerticalSeekBar)
        maxValue = typeArrays.getFloat(R.styleable.VerticalSeekBar_max, maxValue)
        minValue = typeArrays.getFloat(R.styleable.VerticalSeekBar_min, minValue)
        currentValue = typeArrays.getFloat(R.styleable.VerticalSeekBar_progress, currentValue)
        currentValue = 20f
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerPoint.set(w / 2, h / 2)

        triangleSegment.apply {
            moveTo(0f, 0f)
            lineTo(triangleSegmentTopWidth, 0f)
            lineTo(triangleSegmentTopWidth / 2f + triangleBotWidth / 2, h.toFloat())
            lineTo(triangleSegmentTopWidth / 2f - triangleBotWidth / 2, h.toFloat())
            close()
        }

        trianglePaintingRect.set(
            0,
            0,
            triangleSegmentTopWidth.toInt(),
            h
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.translate(startPadding, 0f)
        drawTriangleSegment(canvas)
        drawCircleDemonstrate(canvas)
        canvas.restore()
    }

    private fun drawCircleDemonstrate(canvas: Canvas) {
        shouldShowCircle.then {
            isFromUser.then {
                canvas.drawCircle(
                    centerPoint.x.toFloat(),
                    centerPoint.y.toFloat(),
                    circleRadiusTransformer(currentValue.dp / 2),
                    circleDemonstratePaint
                )
            }
        }
        canvas.drawCircle(
            (triangleSegmentTopWidth * expandProgress).coerceAtLeast(MIN_WIDTH) / 2,
            height - currentHeight,
            6f.dp * (1 - expandProgress),
            circleDemonstratePaint
        )
    }

    private fun drawTriangleSegment(canvas: Canvas) {
        recalculateTriangleRectHeight()
        canvas.save()
        canvas.clipPath(triangleSegment)
        canvas.drawPath(triangleSegment, triangleSegmentBorderPaint)
        canvas.drawRect(Rect(
            trianglePaintingRect.left,
            0,
            trianglePaintingRect.right,
            height
        ), trianglePaintingRectPaint.apply {
            color = resources.getColor(R.color.white_overlay, null)
        })
        canvas.drawRect(trianglePaintingRect, trianglePaintingRectPaint.apply {
            color = resources.getColor(R.color.white, null)
        })

        canvas.restore()
    }

    private fun recalculateTriangleRectHeight() {
        currentHeight = height * (currentValue - minValue) / (maxValue - minValue)
        triangleSegment.apply {
            reset()
            moveTo(0f, 0f)
            lineTo((triangleSegmentTopWidth * expandProgress).coerceAtLeast(MIN_WIDTH), 0f)
            lineTo(
                (triangleSegmentTopWidth * expandProgress / 2f + triangleBotWidth * expandProgress / 2).coerceAtLeast(
                    MIN_WIDTH
                ), height.toFloat()
            )
            lineTo(
                triangleSegmentTopWidth * expandProgress / 2f - triangleBotWidth * expandProgress / 2,
                height.toFloat()
            )
            close()
        }
        trianglePaintingRect.set(
            trianglePaintingRect.left,
            height - currentHeight.toInt(),
            trianglePaintingRect.right,
            trianglePaintingRect.bottom
        )
    }

    fun setOnValueChanged(listener: (Float) -> Unit) {
        onValueChangeListener = listener
        listener(currentValue)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val eventPoint = Point(event?.x?.toInt() ?: 0, event?.y?.toInt() ?: 0)
        if (triangleFullRect.contains(eventPoint.x, eventPoint.y)) {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    expandAnimator.cancel()
                    expandAnimator.setFloatValues(expandProgress, 1f)
                    expandAnimator.start()
                    isFromUser = true
                    currentValue = maxValue - (maxValue - minValue) * eventPoint.y / height
                    invalidate()
                }
            }
        }
        when (event?.action) {
            MotionEvent.ACTION_MOVE -> {
                currentValue = maxValue - (maxValue - minValue) * eventPoint.y / height
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                expandAnimator.cancel()
                expandAnimator.setFloatValues(expandProgress, 0f)
                expandAnimator.start()
                isFromUser = false
                invalidate()
            }
        }
        return isFromUser
    }

    companion object {
        val MIN_WIDTH = 2f.dp
    }
}