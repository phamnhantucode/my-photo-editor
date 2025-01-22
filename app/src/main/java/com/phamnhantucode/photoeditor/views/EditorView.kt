package com.phamnhantucode.photoeditor.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.graphics.drawable.toBitmap
import com.phamnhantucode.photoeditor.core.model.ui.ImageFilter
import com.phamnhantucode.photoeditor.databinding.LayoutDeleteGraphicBinding
import com.phamnhantucode.photoeditor.extension.doIfAboveApi
import jp.co.cyberagent.android.gpuimage.GPUImage

class EditorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : RelativeLayout(context, attrs) {
    private val imgSource = ImageView(context)
    val source: ImageView
        get() = imgSource

    private val drawOverlay: ImageView = ImageView(context)
    val overlay: ImageView
        get() = drawOverlay

    private var glSurfaceView = GLSurfaceView(context)

    private val deleteViewBinding = LayoutDeleteGraphicBinding.inflate(LayoutInflater.from(context))
    val deleteView: View
        get() = deleteViewBinding.root

    private var currentFilter: ImageFilter? = null
    private var originBitmap: Bitmap? = null

    init {
        val sourceParams = setupImageSource()
        addView(imgSource, sourceParams)

        val overlayParams = LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
        )
        addView(drawOverlay, overlayParams)
        val deleteParams = LayoutParams(
            LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
        )
        addView(deleteView, deleteParams)

        imgSource.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            gpuImage = GPUImage(v.context)
            gpuImage.setImage(originBitmap)
            removeView(glSurfaceView)
            glSurfaceView = GLSurfaceView(v.context)
            val params = LayoutParams(
                (right - left), (bottom - top)
            )
            params.addRule(CENTER_IN_PARENT, TRUE)
            addView(glSurfaceView, params)
            gpuImage.setGLSurfaceView(glSurfaceView)
            gpuImage.requestRender()
            drawOverlay.bringToFront()
            deleteView.bringToFront()
        }
    }

    private fun setupImageSource(): LayoutParams {
        imgSource.id = sourceId
        imgSource.adjustViewBounds = true
        imgSource.scaleType = ImageView.ScaleType.FIT_CENTER

        val widthParam = LayoutParams.MATCH_PARENT

        val params = LayoutParams(
            widthParam, LayoutParams.WRAP_CONTENT
        )

        params.addRule(CENTER_IN_PARENT, TRUE)
        return params
    }

    fun getDeleteViewDeletableArea(): Rect {
        val location = IntArray(2)
        deleteViewBinding.deleteGraphicBtn.getLocationOnScreen(location)
        return Rect(
            location[0],
            location[1],
            location[0] + deleteViewBinding.deleteGraphicBtn.width,
            location[1] + deleteViewBinding.deleteGraphicBtn.height
        )
    }

    fun scaleDeleteArea(scale: Float = 1.25f) {
        deleteViewBinding.deleteGraphicBtn.animate().scaleX(scale).scaleY(scale).setDuration(200)
            .start()
        // vibrate()
        if (scale > 1.0f) {
            val vibrate = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            doIfAboveApi(Build.VERSION_CODES.O) {
                doIfAboveApi(Build.VERSION_CODES.Q) {
                    vibrate.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.EFFECT_TICK))
                } ?: vibrate.vibrate(
                    VibrationEffect.createOneShot(
                        100,
                        VibrationEffect.DEFAULT_AMPLITUDE
                    )
                )
            } ?: vibrate.vibrate(100)
        }
    }

    private var gpuImage = GPUImage(context)
    fun setFilter(filter: ImageFilter) {
        if (filter.filterType == currentFilter?.filterType) {
            currentFilter?.updateValue(filter.currentValue)
        } else {
            currentFilter = filter
        }
        gpuImage.setFilter(currentFilter?.guiFilter)
//        imgSource.setImageBitmap(gpuImage.bitmapWithFilterApplied)
    }

    fun setImageNeedFilter(bitmap: Bitmap?) {
        originBitmap = bitmap
    }

    private fun applyFilter() {
        val gpuImage = GPUImage(context)
        gpuImage.setImage(imgSource.drawable.toBitmap())
        gpuImage.setFilter(currentFilter?.getFilter())
        imgSource.setImageBitmap(gpuImage.bitmapWithFilterApplied)
    }

    fun getDraw(canvas: Canvas) {
        glSurfaceView.visibility = View.GONE
        applyFilter()
        super.draw(canvas)
        glSurfaceView.visibility = View.VISIBLE
    }

    companion object {
        private const val TAG = "EditorView"
        const val sourceId = 1
    }
}
