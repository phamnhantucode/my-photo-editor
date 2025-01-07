package com.phamnhantucode.photoeditor.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Scroller
import com.phamnhantucode.photoeditor.camera.FilterCamera
import com.phamnhantucode.photoeditor.camera.FilterType
import kotlin.math.max
import kotlin.math.min

class FilterView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var originPhoto: Bitmap? = null
    private var filteredBitmaps: MutableList<Bitmap> = mutableListOf()
    private var scrollX = 0
    private val scroller = Scroller(context)
    private var maxScrollX = 0
    private val selectedPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 10f
    }

    private var filterSelected = FilterType.NONE
    private var filterSelectedListener: ((FilterType) -> Unit)? = null

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val index= ((scrollX + e.x) / DEFAULT_SIZE).toInt()
            filterSelected = FilterType.entries.getOrNull(index) ?: FilterType.NONE
            filterSelectedListener?.invoke(filterSelected)
            return super.onSingleTapConfirmed(e)
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            scrollX = (scrollX + distanceX).toInt()
            scrollX = max(0, min(scrollX, maxScrollX))
            invalidate()
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            scroller.fling(
                scrollX,
                0,
                -velocityX.toInt(),
                0,
                0,
                maxScrollX,
                0,
                0
            )
            postInvalidateOnAnimation()
            return true
        }
    })

    init {
        isClickable = true
        isFocusable = true
    }

    fun setOriginalPhoto(bitmap: Bitmap) {
        originPhoto = bitmap
        updateFilteredBitmaps()
        invalidate()
    }

    private fun updateFilteredBitmaps() {
        filteredBitmaps.clear()
        originPhoto?.let { original ->
            FilterType.entries.forEach { filterType ->
                val filter = FilterCamera(
                    filterType.name,
                    original,
                    filterType
                )
                val filtered = filter.applyFilter()
                val scaledBitmap = Bitmap.createScaledBitmap(
                    filtered,
                    DEFAULT_SIZE,
                    DEFAULT_SIZE,
                    true
                )
                filteredBitmaps.add(scaledBitmap)
            }
        }
        updateMaxScroll()
    }

    private fun updateMaxScroll() {
        maxScrollX = max(0, (filteredBitmaps.size * DEFAULT_SIZE) - width)
    }

    fun setOnFilterSelectedListener(listener: (FilterType) -> Unit) {
        filterSelectedListener = listener
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, DEFAULT_SIZE)
        updateMaxScroll()
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollX = scroller.currX
            postInvalidateOnAnimation()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Handle touch events first through gesture detector
        val handled = gestureDetector.onTouchEvent(event)

        // Handle the UP and CANCEL events
        when (event.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                // Handle any cleanup if needed
                return true
            }
        }
        return handled
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val startIndex = scrollX / DEFAULT_SIZE
        val endIndex = min(
            ((scrollX + width) / DEFAULT_SIZE) + 1,
            filteredBitmaps.size
        )

        for (i in startIndex until endIndex) {
            val left = (i * DEFAULT_SIZE - scrollX).toFloat()
            filteredBitmaps.getOrNull(i)?.let { bitmap ->
                canvas.drawBitmap(bitmap, left, 0f, null)
                if (filterSelected == FilterType.entries[i]) {
                    canvas.drawRect(left, 0f, left + DEFAULT_SIZE, DEFAULT_SIZE.toFloat(), selectedPaint)
                }
            }
        }
    }

    override fun setOnClickListener(l: OnClickListener?) {

        super.setOnClickListener(l)
    }

    companion object {
        private const val TAG = "FilterView"
        private const val DEFAULT_SIZE = 250
    }
}