package com.phamnhantucode.photoeditor.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.core.model.firebase.CameraSticker
import com.phamnhantucode.photoeditor.core.model.firebase.CameraStickerPartial
import com.phamnhantucode.photoeditor.core.model.firebase.CameraStickerPosition
import com.phamnhantucode.photoeditor.extension.then

class CameraFaceDetectOverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: FaceDetectorResult? = null
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var scaleFactor: FloatArray = FloatArray(2)

    private var bounds = Rect()
    private var faceRects = listOf<RectF>()

    private var isFrontCamera = true

    val cameraSticker = CameraSticker(
        partials = arrayListOf(
            CameraStickerPartial(
                position = CameraStickerPosition.TOP_OF_HEAD
            )
        )
    )

    var boxDrawGuideRect: RectF? = null

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
            for (faceRect in faceRects) {
                canvas.drawRect(faceRect, boxPaint)
            }
            val sticker =
                AppCompatResources.getDrawable(context, R.drawable.same_face_sticker)!!.toBitmap()
            boxDrawGuideRect?.let { it1 ->
                canvas.drawBitmap(
                    sticker, Rect(
                        0, 0, sticker.width, sticker.height
                    ), it1, null
                )
            }
        }
    }

    fun setResults(
        detectionResults: FaceDetectorResult,
        imageHeight: Int,
        imageWidth: Int,
    ) {
        results = detectionResults

        val matrix = Matrix()

        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight
        val scaleFitCenter = maxOf(scaleX, scaleY)

        val newWidth = imageWidth * scaleFitCenter
        val newHeight = imageHeight * scaleFitCenter

        val translateX = (width - newWidth) / 2
        val translateY = (height - newHeight) / 2

        matrix.postScale(scaleFitCenter, scaleFitCenter)
        matrix.postTranslate(translateX, translateY)

        if (isFrontCamera) {
            matrix.postScale(-1f, 1f, width / 2f, height / 2f)
        }

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
            val rect = cameraSticker.partials.first().position?.getDrawGuideBy(
                detectionResults.detections().first()
            )?.getRectF()
            (rect != null).then {
                matrix.mapRect(rect)
                boxDrawGuideRect = rect
            }
        }


        invalidate()
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}