package com.phamnhantucode.photoeditor.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.phamnhantucode.photoeditor.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

class BubbleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val bubbleBitmap = AppCompatResources.getDrawable(context, R.drawable.bubble)?.toBitmap()
        ?.let { Bitmap.createScaledBitmap(it, 200, 200, false) }

    private var currentX = 0f
    private var currentY = 0f
    private var velocityX = 0f
    private var velocityY = 0f

    private var isStickToWall = false
    private var bouncingWallDirection: Direction? = null
    private var wallStickDuration = 0L
    private val minTimeToStickBall = 2000L

    private val friction = 0.98f
    private val sensitivity = 0.5f
    private val maxVelocity = 30f
    private val bounceEnergyLoss = 0.8f

    private val job = CoroutineScope(Dispatchers.Main).launch {
        while (true) {
            move()
            delay(16)
            invalidate()
        }
    }

    init {
        reset()
    }

    fun reset() {
        post {
            currentX = (width - (bubbleBitmap?.width ?: 0)) / 2f
            currentY = (height - (bubbleBitmap?.height ?: 0)) / 2f
            velocityX = 0f
            velocityY = 0f
            isStickToWall = false
            bouncingWallDirection = null
            wallStickDuration = 0L
        }
    }

    private fun move() {
        if (isStickToWall) {
            return
        }

        velocityX *= friction
        velocityY *= friction

        currentX += velocityX
        currentY += velocityY

        val bubbleWidth = bubbleBitmap?.width ?: 0
        val bubbleHeight = bubbleBitmap?.height ?: 0

        when {
            currentX < 0 -> handleWallCollision(Direction.LEFT, bubbleWidth)
            currentX + bubbleWidth > width -> handleWallCollision(Direction.RIGHT, bubbleWidth)
        }

        when {
            currentY < 0 -> handleWallCollision(Direction.UP, bubbleHeight)
            currentY + bubbleHeight > height -> handleWallCollision(Direction.DOWN, bubbleHeight)
        }

        Log.d("BubbleView", "velocityX: $velocityX, velocityY: $velocityY")
    }

    private fun handleWallCollision(direction: Direction, size: Int) {
        when (direction) {
            Direction.LEFT -> {
                currentX = 0f
                velocityX = abs(velocityX) * bounceEnergyLoss
            }
            Direction.RIGHT -> {
                currentX = (width - size).toFloat()
                velocityX = -abs(velocityX) * bounceEnergyLoss
            }
            Direction.UP -> {
                currentY = 0f
                velocityY = abs(velocityY) * bounceEnergyLoss
            }
            Direction.DOWN -> {
                currentY = (height - size).toFloat()
                velocityY = -abs(velocityY) * bounceEnergyLoss
            }
        }

        if (bouncingWallDirection == direction) {
            wallStickDuration += 16
            if (wallStickDuration >= minTimeToStickBall) {
                isStickToWall = true
                velocityX = 0f
                velocityY = 0f
            }
        } else {
            bouncingWallDirection = direction
            wallStickDuration = 0L
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bubbleBitmap?.let {
            canvas.drawBitmap(it, currentX, currentY, null)
            canvas.drawBitmap(it, width-currentX - bubbleBitmap.width, height-currentY - bubbleBitmap.height, null)

        }
    }

    fun updateVelocity(sensorX: Float, sensorY: Float) {
        if (isStickToWall) {
            when (bouncingWallDirection) {
                Direction.LEFT -> if (-sensorX > 0) unstickFromWall()
                Direction.RIGHT -> if (-sensorX < 0) unstickFromWall()
                Direction.UP -> if (sensorY > 0) unstickFromWall()
                Direction.DOWN -> if (sensorY < 0) unstickFromWall()
                null -> unstickFromWall()
            }
        }

        if (!isStickToWall) {
            velocityX += -sensorX * sensitivity
            velocityY += sensorY * sensitivity
        }

        velocityX = velocityX.coerceIn(-maxVelocity, maxVelocity)
        velocityY = velocityY.coerceIn(-maxVelocity, maxVelocity)
    }

    private fun unstickFromWall() {
        isStickToWall = false
        bouncingWallDirection = null
        wallStickDuration = 0L
    }

    fun cancel() {
        job.cancel()
    }

    enum class Direction {
        LEFT, RIGHT, UP, DOWN
    }
}
