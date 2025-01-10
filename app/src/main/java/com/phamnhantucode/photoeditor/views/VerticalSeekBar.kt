package com.phamnhantucode.photoeditor.views

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
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.extension.dp
import com.phamnhantucode.photoeditor.extension.px
import com.phamnhantucode.photoeditor.extension.then

class VerticalSeekBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {


    var onValueChangeListener = { _: Float -> }

    var minValue = 0f
    var maxValue = 100f

    var defaultHeight = 200.px

    var currentValue = 0f
        set(value) {
            field = value
            onValueChangeListener(value)
            invalidate()
        }

    private val triangleSegment = Path()
    val triangleSegmentTopWidth = 16f.dp
    val triangleBotWidth = 4f.dp

    val trianglePaintingRect = Rect()
    val trianglePaintingRectPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    val triangleFullRect = Rect()
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
    val circleDemonstratePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(12f, 0f, 0f, Color.BLACK);
    }

    val centerPoint = Point()
    var isFromUser = false

    private val triangleSegmentBorderPaint = Paint().apply {
        isAntiAlias = true
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }


    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        currentValue = 20f
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
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
        drawTriangleSegment(canvas)
        drawCircleDemonstrate(canvas)

    }

    private fun drawCircleDemonstrate(canvas: Canvas) {
        isFromUser.then {
            canvas.drawCircle(
                centerPoint.x.toFloat(),
                centerPoint.y.toFloat(),
                circleRadiusTransformer(currentValue.dp / 2),
                circleDemonstratePaint
            )
        }
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
            color = resources.getColor(R.color.white_overlay)
        })
        canvas.drawRect(trianglePaintingRect, trianglePaintingRectPaint.apply {
            color = resources.getColor(R.color.white)
        })

        canvas.restore()
    }

    private fun recalculateTriangleRectHeight() {
        val currentHeight = height * (currentValue - minValue) / (maxValue - minValue)
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

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val eventPoint = Point(event?.x?.toInt() ?: 0, event?.y?.toInt() ?: 0)
        if (triangleFullRect.contains(eventPoint.x, eventPoint.y)) {
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
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
                isFromUser = false
                invalidate()
            }
        }
        return isFromUser
    }
}