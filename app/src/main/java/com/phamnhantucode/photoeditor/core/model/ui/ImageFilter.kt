package com.phamnhantucode.photoeditor.core.model.ui

import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageExposureFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHueFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageRGBDilationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import java.util.Locale
import java.util.logging.Filter

data class ImageFilter(
    var name: String? = null,
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
            else -> GPUImageFilter()
        }
    }

    fun setFilterValue(value: Float) {
        val maxMinValue = filterType.getMaxMinValue()
        currentValue = value.coerceIn(maxMinValue.first, maxMinValue.second)
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
    ;

    val isAdjustable: Boolean
        get() = this != NONE

    fun getMaxMinValue(): Pair<Float, Float> {
        return when (this) {
            BRIGHTNESS -> -1f to 1f
            CONTRAST -> 0.0f to 4.0f
            SATURATION -> 0f to 2f
            HUE -> -180f to 180f
            EXPOSURE -> -4f to 4f
            RGB_DILATION -> 1f to 4f
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