package com.phamnhantucode.photoeditor.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.core.model.firebase.CameraSticker
import kotlin.math.max
import kotlin.math.min

class CameraFaceDetectOverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: FaceDetectorResult? = null
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var faceRects = listOf<RectF>()

    var isFrontCamera = true

    private var cameraSticker: CameraSticker? = null

    private var boxDrawGuideRect = listOf<RectF>()


    init {
        initPaints()
    }

    fun clear() {
        results = null
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.text_selected)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        results?.let {
            boxDrawGuideRect.forEach { rect ->
                val sticker =
                    cameraSticker?.partials?.get(boxDrawGuideRect.indexOf(rect))?.uri?.let {
                        BitmapFactory.decodeFile(it.path)
                    }
                if (sticker != null) {
                    canvas.drawBitmap(
                        sticker, Rect(
                            0, 0, sticker.width, sticker.height
                        ), rect, null
                    )
                }
            }
        }
    }

    fun setResults(
        detectionResults: FaceDetectorResult,
        imageHeight: Int,
        imageWidth: Int,
    ) {
        results = detectionResults

        val matrix = getScaleMatrix(imageWidth, imageHeight)

        faceRects = detectionResults.detections().map { detection ->
            val boundingBox = detection.boundingBox()
            val rect = RectF(
                boundingBox.left,
                boundingBox.top,
                boundingBox.right,
                boundingBox.bottom
            )

            matrix.mapRect(rect)
            rect
        }

        if (detectionResults.detections().isNotEmpty()) {
//            val rect = cameraSticker?.partials.first().position?.getDrawGuideBy(
//                detectionResults.detections().first()
//            )?.getRectF()
//            (rect != null).then {
//                matrix.mapRect(rect)
//                boxDrawGuideRect = rect
//            }

            boxDrawGuideRect = cameraSticker?.partials?.mapNotNull { partial ->
                val rect = partial.position?.getDrawGuideBy(detectionResults.detections().first())
                    ?.getRectF()
                rect?.let {
                    matrix.mapRect(it)
                }
                rect
            } ?: emptyList()
        }
        invalidate()
    }

    private fun getScaleMatrix(imageWidth: Int, imageHeight: Int): Matrix {
        val matrix = Matrix()

        val maxWidth = max(width, imageWidth)
        val maxHeight = max(height, imageHeight)
        val minWidth = min(width, imageWidth)
        val minHeight = min(height, imageHeight)

        val scaleX = maxWidth / minWidth.toFloat()
        val scaleY = maxHeight / minHeight.toFloat()
        val scaleFitCenter = maxOf(scaleX, scaleY)

        val newWidth = minWidth * scaleFitCenter
        val newHeight = minHeight * scaleFitCenter

        val translateX = (maxWidth - newWidth) / 2
        val translateY = (maxHeight - newHeight) / 2

        matrix.postScale(scaleFitCenter, scaleFitCenter)
        matrix.postTranslate(translateX, translateY)

        if (isFrontCamera) {
            matrix.postScale(-1f, 1f, maxWidth / 2f, maxHeight / 2f)
        }

        return matrix
    }

    fun setFaceSticker(sticker: CameraSticker?) {
        cameraSticker = sticker
    }

    fun getDrawBitmap(): Bitmap {
        val scaledBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(scaledBitmap)
        draw(canvas)
        return scaledBitmap
    }

    fun drawOnBitmap(bitmap: Bitmap): Bitmap {
        val matrix = getScaleMatrix(bitmap.width, bitmap.height)
        boxDrawGuideRect = boxDrawGuideRect.map { rect ->
            val newRect = RectF(rect)
            matrix.mapRect(newRect)
            newRect
        }
        val canvas = Canvas(bitmap)
        draw(canvas)
        return bitmap
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}