package com.phamnhantucode.photoeditor.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.Scroller
import kotlin.math.abs
import kotlin.math.roundToInt

class RulerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val mScroller: Scroller = Scroller(context)
    private val mVelocityTracker: VelocityTracker = VelocityTracker.obtain()
    private val mMinVelocity = ViewConfiguration.get(getContext()).scaledMinimumFlingVelocity

    private val mLinePaint = Paint()
    private val mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var perWidth: Float = 20f
    private var mMove = 0f
    private var mLastX = 0

    // Line styling
    private var centerLineWidth = 6f
    private var sideLineWidth =6f
    private var centerLineHeight = 60f
    private var sideLineHeight = 40f

    // Value handling
    private var currentValue = 0f
    private var step = 1f

    // Colors
    private var sideLineColor: Int = Color.parseColor("#bcbcbc")
    private var centerLineColor: Int = Color.parseColor("#f24b16")

    // Callback
    private var onValueChanged: ((Float) -> Unit)? = null

    // Value transformer
    private var valueTransformer: ((Float) -> String) = { it.toString() }

    init {
        mTextPaint.apply {
            color = centerLineColor
            textSize = 40f
            textAlign = Paint.Align.CENTER
        }

        mLinePaint.strokeCap = Paint.Cap.ROUND
    }

    fun setValueTransformer(transformer: (Float) -> String) {
        valueTransformer = transformer
        invalidate()
    }

    fun setStep(value: Float) {
        step = value
        invalidate()
    }

    fun setValue(value: Float) {
        currentValue = value
        invalidate()
    }

    fun setOnValueChanged(listener: (Float) -> Unit) {
        onValueChanged = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawRuler(canvas)
        drawCenterValue(canvas)
    }

    private fun drawRuler(canvas: Canvas) {
        val centerX = width / 2f
        val bottomY = height.toFloat()

        // Draw center line
        mLinePaint.apply {
            color = centerLineColor
            strokeWidth = centerLineWidth
        }
        canvas.drawLine(
            centerX,
            bottomY - centerLineHeight,
            centerX,
            bottomY,
            mLinePaint
        )

        // Draw side lines


        for (i in 1..10) {
            val xOffset = i * perWidth
            var startY = bottomY - sideLineHeight
            if (i < 3) {
                mLinePaint.apply {
                    color = centerLineColor
                    strokeWidth = centerLineWidth
                }
                startY = bottomY - centerLineHeight + i * 10
            } else {
                mLinePaint.apply {
                    color = sideLineColor
                    strokeWidth = sideLineWidth
                }
                startY = bottomY - sideLineHeight
            }
            // Right side
            canvas.drawLine(
                centerX + xOffset - mMove,
                startY,
                centerX + xOffset - mMove,
                bottomY,
                mLinePaint
            )

            // Left side
            canvas.drawLine(
                centerX - xOffset - mMove,
                startY,
                centerX - xOffset - mMove,
                bottomY,
                mLinePaint
            )
        }
    }

    private fun drawCenterValue(canvas: Canvas) {
        val centerX = width / 2f
        val textY = height - centerLineHeight - 20f
        canvas.drawText(
            valueTransformer(currentValue),
            centerX,
            textY,
            mTextPaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        val xPosition = event.x.toInt()

        mVelocityTracker.addMovement(event)
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mScroller.forceFinished(true)
                mLastX = xPosition
                mMove = 0f
            }
            MotionEvent.ACTION_MOVE -> {
                mMove += (mLastX - xPosition)
                updateValueFromMove()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handleFling(event)
                return false
            }
        }
        mLastX = xPosition
        return true
    }

    private fun updateValueFromMove() {
        val movement = mMove / perWidth
        if (abs(movement) >= 1) {
            val steps = movement.roundToInt()
            currentValue += steps * step
            mMove -= steps * perWidth
            onValueChanged?.invoke(currentValue)
            invalidate()
        }
    }

    private fun handleFling(event: MotionEvent) {
        mVelocityTracker.computeCurrentVelocity(500)
        val xVelocity = mVelocityTracker.xVelocity

        if (abs(xVelocity) > mMinVelocity) {
            mScroller.fling(
                0, 0, xVelocity.toInt(), 0,
                Int.MIN_VALUE, Int.MAX_VALUE, 0, 0
            )
        }
        updateValueFromMove()
        mMove = 0f
        postInvalidate()
    }

    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            val xPosition = mScroller.currX
            mMove += mLastX - xPosition
            updateValueFromMove()
            mLastX = xPosition
            postInvalidate()
        }
    }
}