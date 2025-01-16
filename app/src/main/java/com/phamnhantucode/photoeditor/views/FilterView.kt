package com.phamnhantucode.photoeditor.views

import android.annotation.SuppressLint
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
import com.phamnhantucode.photoeditor.core.model.ui.ImageFilter
import jp.co.cyberagent.android.gpuimage.GPUImage
import kotlin.math.max
import kotlin.math.min

class FilterView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var originPhoto: Bitmap? = null
    private var filteredBitmaps: MutableList<Bitmap> = mutableListOf()
    private var filters: List<ImageFilter> = ImageFilter.mockCameraFilters()
    private var scrollX = 0
    private val scroller = Scroller(context)
    private var maxScrollX = 0
    private val spacing = 10f
    private val borderWidth = 4f
    private val selectedPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
    }

    private var selectedFilter = ImageFilter()
    private var filterSelectedListener: ((ImageFilter) -> Unit)? = null
    private var gpuImage: GPUImage? = null

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val index = ((scrollX + e.x) / DEFAULT_SIZE).toInt()
            selectedFilter = filters.getOrNull(index) ?: ImageFilter()
            filterSelectedListener?.invoke(selectedFilter)
            invalidate()
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
        gpuImage = GPUImage(context)
    }

    fun setOriginalPhoto(bitmap: Bitmap) {
        originPhoto = bitmap
        gpuImage?.setImage(bitmap)
        updateFilteredBitmaps()
        invalidate()
    }

    private fun updateFilteredBitmaps() {
        filteredBitmaps.clear()
        originPhoto?.let { original ->
            filters.forEach { filter ->
                gpuImage?.let { gpu ->
                    gpu.setFilter(filter.getFilter())
                    val filtered = gpu.bitmapWithFilterApplied
                    val scaledBitmap = Bitmap.createScaledBitmap(
                        filtered,
                        DEFAULT_SIZE,
                        DEFAULT_SIZE,
                        true
                    )
                    filteredBitmaps.add(scaledBitmap)
                }
            }
        }
        updateMaxScroll()
    }

    private fun updateMaxScroll() {
        maxScrollX = max(0, ((filteredBitmaps.size * (DEFAULT_SIZE + spacing.toInt())) -  width) / 2)
    }

    fun setOnFilterSelectedListener(listener: (ImageFilter) -> Unit) {
        filterSelectedListener = listener
        if (filters.isNotEmpty()) {
            listener(filters.first())
            selectedFilter = filters.first()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(width, DEFAULT_SIZE + borderWidth.toInt())
        updateMaxScroll()
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollX = scroller.currX
            postInvalidateOnAnimation()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handled = gestureDetector.onTouchEvent(event)

        when (event.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                return true
            }
        }
        return handled
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val startIndex = scrollX / (DEFAULT_SIZE + spacing.toInt())
        val endIndex = min(
            ((scrollX + width) / (DEFAULT_SIZE + spacing.toInt())) + 1,
            filteredBitmaps.size
        )

        for (i in startIndex until endIndex) {
            val left = (i * (DEFAULT_SIZE + spacing) - scrollX).toFloat()
            filteredBitmaps.getOrNull(i)?.let { bitmap ->
                canvas.drawBitmap(bitmap, left, 0f, null)
                if (selectedFilter == filters[i]) {
                    canvas.drawRect(left, 0f, left + DEFAULT_SIZE, DEFAULT_SIZE.toFloat(), selectedPaint)
                }
            }
        }
    }

    companion object {
        private const val TAG = "FilterView"
        private const val DEFAULT_SIZE = 250
    }
}