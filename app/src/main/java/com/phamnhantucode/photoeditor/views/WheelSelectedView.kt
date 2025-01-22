package com.phamnhantucode.photoeditor.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Scroller
import androidx.core.content.res.ResourcesCompat
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.extension.dp
import com.phamnhantucode.photoeditor.extension.sp
import kotlin.math.abs

class WheelSelectedView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private data class FontItem(
        val name: String,
        val typeface: Typeface?,
        var bounds: RectF = RectF(),
        var textWidth: Float = 0f,
        var textHeight: Float = 0f,
        var centerX: Float = 0f,
    )

    private var fontSelectedListener: ((Typeface?) -> Unit)? = null

    private val fontItems = listOf(
        FontItem("Nunito", ResourcesCompat.getFont(context, R.font.nunito)),
        FontItem("Poppins", ResourcesCompat.getFont(context, R.font.poppins)),
        FontItem("Roboto", ResourcesCompat.getFont(context, R.font.roboto)),
        FontItem("Rouge", ResourcesCompat.getFont(context, R.font.rouge)),
        FontItem("Lavishly", ResourcesCompat.getFont(context, R.font.lavishly)),
        FontItem("Monsieur", ResourcesCompat.getFont(context, R.font.monsieur)),
        FontItem("Playwrite", ResourcesCompat.getFont(context, R.font.playwrite)),
    )

    private val textBounds = Rect()
    private val padding = 10f.dp
    private val cornerRadius = 8f.dp
    private val itemSpacing = 20f.dp
    private val viewTextSize = 16f.sp
    private val elementPadding = 8f.dp

    private val selectedRectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val unselectedRectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE.adjustAlpha(0.5f)
        style = Paint.Style.FILL
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = viewTextSize
        textAlign = Paint.Align.LEFT
    }

    private var totalWidth = 0f
    private var maxTextHeight = 0f
    private var scrollX = 0f
    private var selectedIndex = 0
    private var startPadding = 0f
    private var endPadding = 0f

    private val scroller = Scroller(context)
    private val gestureDetector = GestureDetector(context, GestureListener())

    init {
        calculateTextDimensions()
    }

    private fun Int.adjustAlpha(factor: Float): Int {
        val alpha = (Color.alpha(this) * factor).toInt()
        val red = Color.red(this)
        val green = Color.green(this)
        val blue = Color.blue(this)
        return Color.argb(alpha, red, green, blue)
    }

    private fun calculateTextDimensions() {
        var currentX = 0f

        fontItems.forEach { item ->
            textPaint.typeface = item.typeface
            textPaint.getTextBounds(item.name, 0, item.name.length, textBounds)
            item.textWidth = textPaint.measureText(item.name)
            item.textHeight = textBounds.height().toFloat()
            maxTextHeight = maxOf(maxTextHeight, item.textHeight)
        }

        fontItems.forEachIndexed { index, item ->
            item.bounds.set(
                currentX - padding,
                -viewTextSize / 2 - padding,
                currentX + item.textWidth + padding,
                viewTextSize / 2 + padding
            )
            item.centerX = currentX + item.textWidth / 2
            currentX += item.textWidth + itemSpacing + if (index == fontItems.size) 0f else elementPadding
        }

        totalWidth = currentX - itemSpacing
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (maxTextHeight + 2 * padding).toInt()
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = resolveSize(desiredHeight, heightMeasureSpec)

        startPadding = width / 2f - fontItems.first().textWidth / 2
        endPadding = width / 2f - fontItems.last().textWidth / 2 - padding

        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            scrollX = 0f
            updateSelectedItem()
            invalidate()
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(-scrollX + startPadding, height / 2f)

        fontItems.forEachIndexed { index, item ->
            val distanceFromCenter = abs(width / 2 + scrollX - startPadding - item.centerX)
            val isSelected = index == selectedIndex

            canvas.drawRoundRect(
                item.bounds,
                cornerRadius,
                cornerRadius,
                if (isSelected) selectedRectPaint else unselectedRectPaint
            )

            textPaint.typeface = item.typeface
            canvas.drawText(
                item.name,
                item.bounds.left + padding,
                (item.textHeight / 4),
                textPaint
            )
        }

        canvas.restore()
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float,
        ): Boolean {
            scrollX += distanceX
            scrollX =
                scrollX.coerceIn(0f, maxOf(0f, totalWidth - (width - (startPadding + endPadding))))
            updateSelectedItem()
            invalidate()
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float,
        ): Boolean {
            scroller.fling(
                scrollX.toInt(), 0,
                -velocityX.toInt(), 0,
                0, maxOf(0, (totalWidth - (width - (startPadding + endPadding))).toInt()),
                0, 0
            )
            return true
        }
    }

    private fun updateSelectedItem() {
        val centerX = width / 2 + scrollX - startPadding
        var minDistance = Float.MAX_VALUE
        var newSelectedIndex = selectedIndex

        fontItems.forEachIndexed { index, item ->
            val distance = abs(centerX - item.centerX)
            if (distance < minDistance) {
                minDistance = distance
                newSelectedIndex = index
            }
        }

        if (newSelectedIndex != selectedIndex) {
            selectedIndex = newSelectedIndex
            fontSelectedListener?.invoke(
                fontItems[selectedIndex].typeface
            )
        }
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollX = scroller.currX.toFloat()
            updateSelectedItem()
            invalidate()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true
    }

    fun setSelectedFont(fontName: String) {
        val index = fontItems.indexOfFirst { it.name == fontName }
        if (index != -1) {
            selectedIndex = index
            scrollX = (fontItems[index].centerX - width / 2 + startPadding)
                .coerceIn(0f, maxOf(0f, totalWidth - (width - (startPadding + endPadding))))
            invalidate()
        }
    }

    fun setOnFontSelectedListener(listener: (Typeface?) -> Unit) {
        fontSelectedListener = listener
        listener.invoke(fontItems[selectedIndex].typeface)
    }
}
