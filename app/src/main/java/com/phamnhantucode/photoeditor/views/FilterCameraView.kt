package com.phamnhantucode.photoeditor.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.widget.Scroller
import com.phamnhantucode.photoeditor.core.model.ui.ImageFilter
import com.phamnhantucode.photoeditor.extension.dp
import jp.co.cyberagent.android.gpuimage.GPUImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.io.ByteArrayOutputStream


class FilterCameraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private data class FilterItem(
        val name: String,
        val filter: ImageFilter,
        var bounds: RectF = RectF(),
        var centerX: Float = 0f,
        var filteredBitmap: Bitmap? = null,
        var clipPath: Path = Path(),
    )

    private var filterSelectedListener: ((ImageFilter) -> Unit)? = null
    private var filters: List<ImageFilter> = ImageFilter.mockCameraFilters()
    private val filterItems = filters.map { FilterItem(it.name, it) }
    private var filterFlow: MutableStateFlow<ImageFilter>? = MutableStateFlow(filterItems[0].filter)

    private val space = 8f.dp
    private var originPhoto: Bitmap? = null
    private var gpuImage: GPUImage? = null

    private val selectedRectPaintStrokeWidth = 2f.dp
    private val selectedRectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = selectedRectPaintStrokeWidth
    }

    private val centerRect = RectF()

    private var totalWidth = 0f
    private var scrollX = 0f
    private var selectedIndex = 0
    private var startPadding = 0f
    private var endPadding = 0f
    private var cornerRadius = 8f.dp

    init {
        isClickable = true
        isFocusable = true
        gpuImage = GPUImage(context)
    }

    private val scroller = Scroller(context)
    private val gestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
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
                scrollX.toInt(),
                0,
                -velocityX.toInt(),
                0,
                0,
                (totalWidth - (width - (startPadding + endPadding))).toInt(),
                0,
                0
            )

            val finalX = scroller.finalX.toFloat()
            selectedIndex =
                ((finalX + width / 2f - startPadding) / (ITEM_DEFAULT_WIDTH + space)).toInt()
                    .coerceIn(0, filterItems.size - 1)

            postDelayed({
                fitSelectedItemCenter()
            }, scroller.duration.toLong())

            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            val index = ((scrollX + e.x - startPadding) / (ITEM_DEFAULT_WIDTH + space)).toInt()
            selectedIndex = index
            fitSelectedItemCenter()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            return super.onSingleTapUp(e)
        }
    })

    fun setOriginalPhoto(bitmap: Bitmap) {

        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            ITEM_DEFAULT_WIDTH.toInt(), ITEM_DEFAULT_HEIGHT.toInt(), true
        )

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)

        val compressedData = outputStream.toByteArray()

        originPhoto = BitmapFactory.decodeByteArray(compressedData, 0, compressedData.size)

        gpuImage?.setImage(originPhoto)
        updateFilteredBitmaps()
        invalidate()
    }

    private fun updateFilteredBitmaps() {
        originPhoto?.let { original ->
            filterItems.forEach { item ->
                gpuImage?.let { gpu ->
                    gpu.setFilter(item.filter.getFilter())
                    val filtered = gpu.bitmapWithFilterApplied
                    item.filteredBitmap = Bitmap.createScaledBitmap(
                        filtered,
                        ITEM_DEFAULT_WIDTH.toInt(),
                        ITEM_DEFAULT_HEIGHT.toInt(),
                        true
                    )
                }
            }
        }
    }

    private fun calculateImageDimensions() {
        var currentX = 0f
        filterItems.forEach { item ->
            item.bounds.set(
                currentX,
                0f,
                currentX + ITEM_DEFAULT_WIDTH,
                ITEM_DEFAULT_HEIGHT
            )
            item.centerX = item.bounds.centerX()
            item.clipPath.apply {
                addRoundRect(item.bounds, cornerRadius, cornerRadius, Path.Direction.CCW)
            }
            currentX += item.bounds.width() + space
        }
        totalWidth = currentX

    }

    private fun updateSelectedItem() {
        val centerX = width / 2f + scrollX - startPadding
        var minDistance = Float.MAX_VALUE
        var newSelectedIndex = selectedIndex

        filterItems.forEachIndexed { index, item ->
            val distance = Math.abs(centerX - item.centerX)
            if (distance < minDistance) {
                minDistance = distance
                newSelectedIndex = index
            }
        }

        if (newSelectedIndex != selectedIndex) {
            selectedIndex = newSelectedIndex
            filterFlow?.update {
                filterItems[selectedIndex].filter
            }
        }
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollX = scroller.currX.toFloat()
            updateSelectedItem()
            invalidate()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                fitSelectedItemCenter()
            }
        }
        return true
    }

    private fun fitSelectedItemCenter() {
        val targetScrollX = (filterItems[selectedIndex].centerX - width / 2f + startPadding)
            .coerceIn(0f, maxOf(0f, totalWidth - (width - (startPadding + endPadding))))

        scroller.startScroll(
            scrollX.toInt(),
            0,
            (targetScrollX - scrollX).toInt(),
            0,
            300 // duration in milliseconds
        )
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.translate(-scrollX + startPadding, height / 2f - ITEM_DEFAULT_HEIGHT / 2)

        filterItems.forEach { item ->
            val bounds = item.bounds
            if (item.filteredBitmap != null) {
                canvas.save()
                canvas.clipPath(item.clipPath)
                canvas.drawBitmap(item.filteredBitmap!!, bounds.left, bounds.top, null)
                canvas.restore()
            }
        }
        centerRect.offsetTo(
            scrollX + width / 2f - ITEM_DEFAULT_WIDTH / 2 - startPadding,
            0f
        )
        canvas.drawRoundRect(centerRect, cornerRadius, cornerRadius, selectedRectPaint)

        canvas.restore()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = ITEM_DEFAULT_HEIGHT + paddingTop + paddingBottom
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = resolveSize(desiredHeight.toInt(), heightMeasureSpec)

        startPadding = width / 2f - ITEM_DEFAULT_WIDTH / 2
        endPadding = width / 2f - ITEM_DEFAULT_WIDTH / 2 - space

        centerRect.set(
            width / 2f - ITEM_DEFAULT_WIDTH / 2 - startPadding,
            0f,
            width / 2f + ITEM_DEFAULT_WIDTH / 2 - startPadding,
            ITEM_DEFAULT_HEIGHT
        )
        setMeasuredDimension(width, height)
        calculateImageDimensions()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            scrollX = 0f
            updateSelectedItem()
            invalidate()
        }
    }

    fun setOnFilterSelectedListener(listener: (ImageFilter) -> Unit) {
        filterSelectedListener = listener
    }

    fun getFilterFlow(): Flow<ImageFilter> {
        if (filterFlow == null) {
            filterFlow = MutableStateFlow(filterItems[selectedIndex].filter)
        }
        return filterFlow!!
    }

    fun setFilter(filter: String) {
        val index = filters.indexOfFirst { it.name == filter }
        if (index != -1) {
            selectedIndex = index
            scrollX = 0f
            invalidate()
        }
    }

    companion object {
        const val ITEM_DEFAULT_WIDTH = 100f
        const val ITEM_DEFAULT_HEIGHT = 100f
    }
}
