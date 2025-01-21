package com.phamnhantucode.photoeditor.core.model.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.CameraEffect
import com.phamnhantucode.photoeditor.camera.effect.FilterMappingSurfaceEffect
import com.phamnhantucode.photoeditor.camera.effect.processor.FilterProcessor
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBoxBlurFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageExposureFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHazeFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHueFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageRGBDilationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSharpenFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSketchFilter
import java.util.Locale
import java.util.logging.Filter

data class ImageFilter(
    var name: String = "",
    var filterType: FilterType = FilterType.NONE,
    var currentValue: Float = 0f,
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

    fun setFilterValue(value: Float) {
        val maxMinValue = filterType.getMaxMinValue()
        currentValue = value.coerceIn(maxMinValue.first, maxMinValue.second)
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

    companion object {
        fun createFilter(filterType: FilterType): ImageFilter {
            return ImageFilter(
                name = filterType.name.lowercase()
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                filterType = filterType,
                currentValue = filterType.getDemoValue()
            )
        }

        fun createFilter(filterType: FilterType, value: Float): ImageFilter {
            return ImageFilter(
                name = filterType.name,
                filterType = filterType,
                currentValue = value
            )
        }

        fun createFilter(filterType: FilterType, valueSeekbar: Int): ImageFilter {
            return ImageFilter(
                name = filterType.name,
                filterType = filterType,
                currentValue = filterType.normalizeToValueFilter(valueSeekbar)
            )
        }

        fun mockFilters(): List<ImageFilter> {
            return listOf(
                createFilter(FilterType.NONE),
                createFilter(FilterType.BRIGHTNESS),
                createFilter(FilterType.CONTRAST),
                createFilter(FilterType.SATURATION),
                createFilter(FilterType.HUE),
                createFilter(FilterType.EXPOSURE),
                createFilter(FilterType.RGB_DILATION),
                createFilter(FilterType.SHARPEN),
                createFilter(FilterType.BOX_BLUR),
            )
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

    fun getMaxMinValue(): Pair<Float, Float> {
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