package com.phamnhantucode.photoeditor.core.model.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.CameraEffect
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.camera.effect.FilterMappingSurfaceEffect
import com.phamnhantucode.photoeditor.camera.effect.processor.FilterProcessor
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBoxBlurFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageExposureFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHueFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageRGBDilationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSharpenFilter
import java.util.Locale

data class ImageFilter(
    var name: String = "",
    var filterType: FilterType = FilterType.NONE,
    var guiFilter: GPUImageFilter = GPUImageFilter(),
    var currentValue: Float = 0f,
    var demoBitmap: Bitmap? = null,
) {
    fun getFilter(): GPUImageFilter {
        return when (filterType) {
            FilterType.BRIGHTNESS -> GPUImageBrightnessFilter().apply { setBrightness(currentValue) }
            FilterType.CONTRAST -> GPUImageContrastFilter().apply { setContrast(currentValue) }
            FilterType.SATURATION -> GPUImageSaturationFilter().apply { setSaturation(currentValue) }
            FilterType.HUE -> GPUImageHueFilter().apply { setHue(currentValue) }
            FilterType.EXPOSURE -> GPUImageExposureFilter().apply { setExposure(currentValue) }
            FilterType.RGB_DILATION -> GPUImageRGBDilationFilter(currentValue.toInt())
            FilterType.SHARPEN -> GPUImageSharpenFilter().apply { setSharpness(currentValue) }
            FilterType.BOX_BLUR -> GPUImageBoxBlurFilter().apply { setBlurSize(currentValue) }
            else -> GPUImageFilter()
        }
    }

    fun toCameraEffect(): CameraEffect {
        return FilterMappingSurfaceEffect(
            processor = FilterProcessor(
                value = currentValue,
                filterType = filterType
            )
        )
    }

    fun applyFilter(context: Context, bitmap: Bitmap): Bitmap {
        val gpuImage = GPUImage(context)
        gpuImage.setImage(bitmap)
        gpuImage.setFilter(getFilter())
        return gpuImage.bitmapWithFilterApplied
    }

    fun updateValue(currentValue: Float) {
        this.currentValue = currentValue
        when (filterType) {
            FilterType.BRIGHTNESS -> (guiFilter as GPUImageBrightnessFilter).setBrightness(
                currentValue
            )

            FilterType.CONTRAST -> (guiFilter as GPUImageContrastFilter).setContrast(currentValue)
            FilterType.SATURATION -> (guiFilter as GPUImageSaturationFilter).setSaturation(
                currentValue
            )

            FilterType.HUE -> (guiFilter as GPUImageHueFilter).setHue(currentValue)
            FilterType.EXPOSURE -> (guiFilter as GPUImageExposureFilter).setExposure(currentValue)
            FilterType.SHARPEN -> (guiFilter as GPUImageSharpenFilter).setSharpness(currentValue)
            FilterType.BOX_BLUR -> (guiFilter as GPUImageBoxBlurFilter).setBlurSize(currentValue)
            else -> guiFilter
        }
    }

    companion object {
        private fun createFilter(filterType: FilterType): ImageFilter {
            return ImageFilter(
                name = filterType.name.lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                filterType = filterType,
                currentValue = filterType.getDemoValue(),
            ).apply {
                guiFilter = getFilter()
            }
        }

        fun mockFilters(context: Context? = null): List<ImageFilter> {
            val gpuImage = GPUImage(context)
            gpuImage.setImage(
                BitmapFactory.decodeResource(
                    context?.resources,
                    R.drawable.filter_demo
                )
            )
            val list = listOf(
                createFilter(FilterType.NONE),
                createFilter(FilterType.BRIGHTNESS),
                createFilter(FilterType.CONTRAST),
                createFilter(FilterType.SATURATION),
                createFilter(FilterType.HUE),
                createFilter(FilterType.EXPOSURE),
                createFilter(FilterType.RGB_DILATION),
                createFilter(FilterType.SHARPEN),
                createFilter(FilterType.BOX_BLUR),
            ).onEach {
                if (context != null) {
                    gpuImage.setFilter(it.getFilter())
                    it.demoBitmap = gpuImage.bitmapWithFilterApplied
                }
            }
            return list
        }

        fun mockCameraFilters(): List<ImageFilter> {
            return listOf(
                createFilter(FilterType.NONE),
                createFilter(FilterType.BRIGHTNESS),
                createFilter(FilterType.CONTRAST),
                createFilter(FilterType.SATURATION),
                createFilter(FilterType.HUE),
                createFilter(FilterType.EXPOSURE),
                createFilter(FilterType.SHARPEN),
                createFilter(FilterType.BOX_BLUR),
            )
        }
    }
}

enum class FilterType {
    NONE,
    BRIGHTNESS,
    CONTRAST,
    SATURATION,
    HUE,
    EXPOSURE,
    RGB_DILATION,
    SHARPEN,
    BOX_BLUR,
    ;

    val isAdjustable: Boolean
        get() = this != NONE

    private fun getMaxMinValue(): Pair<Float, Float> {
        return when (this) {
            BRIGHTNESS -> -1f to 1f
            CONTRAST -> 0.0f to 4.0f
            SATURATION -> 0f to 2f
            HUE -> -180f to 180f
            EXPOSURE -> -3f to 3f
            RGB_DILATION -> 1f to 4f
            SHARPEN -> -4f to 4f
            BOX_BLUR -> 1f to 2f
            else -> 0f to 0f
        }
    }

    private fun getNormalValue(): Float {
        return when (this) {
            BRIGHTNESS -> 0f
            CONTRAST -> 1f
            SATURATION -> 1f
            HUE -> 90f
            EXPOSURE -> 0f
            RGB_DILATION -> 1f
            SHARPEN -> 0f
            BOX_BLUR -> 1f
            else -> 0f
        }
    }

    fun normalizeToValueSeekbar(value: Float): Int {
        val maxMinValue = getMaxMinValue()
        return ((value - maxMinValue.first) / (maxMinValue.second - maxMinValue.first) * 100).toInt()
    }

    fun normalizeToValueFilter(valueSeekbar: Int): Float {
        val maxMinValue = getMaxMinValue()
        return valueSeekbar * (maxMinValue.second - maxMinValue.first) / 100 + maxMinValue.first
    }

    fun getDemoValue(): Float {
        return (getMaxMinValue().second - getNormalValue()) / 4 + getNormalValue()
    }
}
