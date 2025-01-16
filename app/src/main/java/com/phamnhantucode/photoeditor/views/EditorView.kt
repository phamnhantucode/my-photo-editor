package com.phamnhantucode.photoeditor.views

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EditorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : RelativeLayout(context, attrs) {
    private val imgSource = ImageView(context)
    val source: ImageView
        get() = imgSource

    private val drawOverlay: ImageView = ImageView(context)
    val overlay: ImageView
        get() = drawOverlay

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
        deleteViewBinding.deleteGraphicBtn.animate().scaleX(scale).scaleY(scale).setDuration(200).start()
        // vibrate()
        if (scale > 1.0f) {
            val vibrate = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            doIfAboveApi(Build.VERSION_CODES.O) {
                doIfAboveApi(Build.VERSION_CODES.Q) {
                    vibrate.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.EFFECT_TICK))
                } ?: vibrate.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } ?: vibrate.vibrate(100)
        }
    }

    fun setFilter(filter: ImageFilter?) {
        val gpuImage = GPUImage(context)
        gpuImage.setImage(originBitmap)
        gpuImage.setFilter(filter?.getFilter())
        imgSource.setImageBitmap(gpuImage.bitmapWithFilterApplied)
        currentFilter = filter
    }

    fun setImageNeedFilter(bitmap: Bitmap?) {
        originBitmap = bitmap
//        gpuImage = GPUImage(context)
//        gpuImage.setImage(bitmap)
//        removeView(glFilterOverlay)
//        glFilterOverlay = GLSurfaceView(context)
//        val width = imgSource.width
//        val height = imgSource.height
//        val layoutParams = if (width > 0 && height > 0) {
//            LayoutParams(width, height)
//        } else {
//            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
//        }.apply {
//            addRule(CENTER_IN_PARENT, TRUE)
//        }
//        addView(glFilterOverlay, layoutParams)
//        drawOverlay.bringToFront()
//        deleteView.bringToFront()
//        gpuImage.setGLSurfaceView(glFilterOverlay)
//        requestLayout()
    }

    suspend fun applyFilter() = withContext(Dispatchers.IO) {
        val gpuImage = GPUImage(context)
        gpuImage.setImage(imgSource.drawable.toBitmap())
        gpuImage.setFilter(currentFilter?.getFilter())
        withContext(Dispatchers.Main) {
            imgSource.setImageBitmap(gpuImage.bitmapWithFilterApplied)
        }
    }

    companion object {
        private const val TAG = "EditorView"
        const val sourceId = 1
    }
}