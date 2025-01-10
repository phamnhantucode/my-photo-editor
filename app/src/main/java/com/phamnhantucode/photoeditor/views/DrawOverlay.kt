package com.phamnhantucode.photoeditor.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import com.phamnhantucode.photoeditor.extension.dp
import com.phamnhantucode.photoeditor.extension.then

class DrawOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var bitmapBottomLayer: Bitmap? = null
        set(value) {
            field = value
            postInvalidate()
        }

    var drawableArea = Rect()
        set(value) = run {
            field = value
            invalidate()
        }

    var paintStrokeWidth: Float = 0f.dp
        set(value) {
            penPaint.strokeWidth = value
            erasePaint.strokeWidth = value
            neonPaint.strokeWidth = value * 1.75f
            brushPaint.strokeWidth = value
            field = value
        }

    var paintColor: Int = Color.BLACK
        set(value) {
            penPaint.color = value
            brushPaint.color = value
            neonPaint.color = (value and 0x7FFFFFFF)
            field = value
        }

    private val penPaint = Paint().apply {
        color = paintColor
        strokeWidth = paintStrokeWidth
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val erasePaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = paintStrokeWidth
        isAntiAlias = true
    }

    private val neonPaint = Paint().apply {
        color = paintColor
        strokeWidth = paintStrokeWidth
        maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    private val brushPaint = Paint().apply {
        color = paintColor
        strokeWidth = paintStrokeWidth
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        strokeJoin = Paint.Join.BEVEL
    }

    private var bitmap: Bitmap? = null
    private var canvasBm: Canvas? = null
    private var currentPath = Path()
    private var lastX = 0f
    private var lastY = 0f
    private var controlX = 0f
    private var controlY = 0f
    private var userCursorPoint = PointF()
    var paintType = PaintType.PEN
        set(value) = run {
            field = value
//            eraserRect.offsetTo(width / 2f, height / 2f)
            userCursorPoint.set(width / 2f, height / 2f)
            tempPointF.set(0f, 0f)
            invalidate()
        }

    //
//    private val eraserRadius = 20f.dp
//    private var eraserRect = RectF(0f, 0f, eraserRadius * 2, eraserRadius * 2)

    private var onDrawStateChangeListener: ((Boolean) -> Unit)? = null
    private var isDrawing = false
        set(value) {
            field = value
            onDrawStateChangeListener?.invoke(value)
        }
    private var tempPointF = PointF()
    private var tempRect = RectF()

    private val pathStack = mutableListOf<StoredPath>()
    private var currentStoredPath: StoredPath? = null

    private fun drawPathToBitmap(path: Path) {
        if (canvasBm != null) {

            canvasBm!!.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            drawBottomLayer()
            for (storedPath in pathStack) {
                canvasBm!!.drawPath(storedPath.path, storedPath.paint)
            }

            if (paintType == PaintType.NEON) {
                canvasBm!!.drawPath(path, penPaint)
            }
            if (isDrawing)
                canvasBm!!.drawPath(path, getPaintFromType(paintType))
        }
    }

    private fun drawBottomLayer() {
        bitmapBottomLayer?.let {
            canvasBm!!.drawBitmap(it, 0f, 0f, null)
        }
    }

    fun getPaintFromType(paintType: PaintType): Paint {
        return when (paintType) {
            PaintType.PEN -> penPaint
            PaintType.ERASER -> erasePaint
            PaintType.BRUSH -> brushPaint
            PaintType.NEON -> neonPaint
        }
    }

    private val canvasPadding = 16f.dp
    private val canvasRect = RectF()
    var imageBorderColor = Color.BLACK
        set(value) {
            field = value
            invalidate()
        }
    private var scaleFactor = 1f
    private val scaleListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor = detector.scaleFactor
            scaleFactor = 1f.coerceAtLeast(scaleFactor.coerceAtMost(3f))
            invalidate()
            return true
        }
    }
    private val scaleDetector = ScaleGestureDetector(context, scaleListener)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        canvasRect.set(
            canvasPadding,
            canvasPadding,
            w - canvasPadding,
            h - canvasPadding
        )

        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvasBm = Canvas(bitmap!!)
        drawBottomLayer()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.clipRect(drawableArea)
        bitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
        canvas.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        userCursorPoint.set(event?.x ?: 0f, event?.y ?: 0f)
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                isDrawing = true
                currentPath = Path()
                lastX = event.x - tempPointF.x
                lastY = event.y - tempPointF.y
                currentPath.moveTo(lastX, lastY)


                currentStoredPath = StoredPath(
                    path = Path(currentPath),
                    paintType = paintType,
                    paint = Paint(getPaintFromType(paintType)),
                    lastX = lastX,
                    lastY = lastY,
                    controlX = lastX,
                    controlY = lastY
                )
            }

            MotionEvent.ACTION_MOVE -> {
                val x = event.x - tempPointF.x
                val y = event.y - tempPointF.y

                controlX = (lastX + x) / 2
                controlY = (lastY + y) / 2

                currentPath.quadTo(lastX, lastY, controlX, controlY)
                currentStoredPath?.let { storedPath ->
                    storedPath.path.quadTo(lastX, lastY, controlX, controlY)
                    storedPath.lastX = lastX
                    storedPath.lastY = lastY
                    storedPath.controlX = controlX
                    storedPath.controlY = controlY
                }

                drawPathToBitmap(currentPath)

                lastX = x
                lastY = y
                postInvalidate()
            }

            MotionEvent.ACTION_UP -> {
                isDrawing = false
                currentPath.lineTo(event.x, event.y)
                currentStoredPath?.let { storedPath ->
                    storedPath.path.lineTo(event.x, event.y)
                    pathStack.add(storedPath)
                    if (paintType == PaintType.NEON) {
                        pathStack.add(storedPath.copy(
                            paint = Paint(penPaint),
                        ))
                    }
                }
                currentStoredPath = null

                drawPathToBitmap(currentPath)
                postInvalidate()
            }
        }
        return if (paintType == PaintType.ERASER && event != null) {
            scaleDetector.onTouchEvent(event)
        } else {
            true
        }
    }

    fun setOnDrawStateChangeListener(listener: (Boolean) -> Unit) {
        onDrawStateChangeListener = listener
    }

    fun exportDrawing(): Bitmap {
        val exportBitmap = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(exportBitmap)

        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        canvas.drawBitmap(bitmap!!, 0f, 0f, null)
        return exportBitmap
    }

    enum class PaintType {
        PEN, ERASER, BRUSH, NEON
    }


    private data class StoredPath(
        val path: Path,
        val paintType: PaintType = PaintType.PEN,
        val paint: Paint,
        var lastX: Float,
        var lastY: Float,
        var controlX: Float,
        var controlY: Float
    )
}