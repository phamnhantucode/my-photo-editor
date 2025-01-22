package com.phamnhantucode.photoeditor.views

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CaptureButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private var scale = 1f
    private var isAnimating = false
    private var lastClickTime = 0L
    private val minClickInterval = 1000L // Prevent clicks within 1 second

    private val scaleAnimator = ValueAnimator.ofFloat(1f, 0.8f, 1f).apply {
        duration = 500
        addUpdateListener { animation ->
            scale = animation.animatedValue as Float
            invalidate()
        }
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                isAnimating = false
            }
        })
    }

    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < minClickInterval) return@setOnClickListener
            lastClickTime = currentTime

            if (!isAnimating) {
                isAnimating = true
                scaleAnimator.start()
                l?.onClick(this)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.drawCircle(width / 2f, height / 2f, width * scale / 2f, paint.apply {
            style = Paint.Style.FILL
        })
        canvas.drawCircle(width / 2f, height / 2f, width / 2f - 10f / 2, paint.apply {
            strokeWidth = 10f
            style = Paint.Style.STROKE
        })

        canvas.restore()
    }
}
