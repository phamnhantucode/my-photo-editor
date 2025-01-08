package com.phamnhantucode.photoeditor.camera

import android.graphics.Bitmap
import com.zomato.photofilters.SampleFilters
import com.zomato.photofilters.imageprocessors.Filter

data class FilterCamera(
    val name: String,
    val bitmap: Bitmap,
    val type: FilterType
) {
    fun applyFilter(): Bitmap {
        return type.getFilter().processFilter(bitmap.copy(bitmap.config, true))
    }
}

enum class FilterType {
    NONE,
    BRIGHTNESS,
    CONTRAST,
    SATURATION,
    HUE,
    SHARPNESS,
    FILTER;

    fun getFilter(): Filter {
        val filter = Filter()
        return when (this) {
            NONE -> filter
            BRIGHTNESS -> SampleFilters.getStarLitFilter()
            CONTRAST -> SampleFilters.getBlueMessFilter()
            SATURATION -> SampleFilters.getLimeStutterFilter()
            HUE -> SampleFilters.getNightWhisperFilter()
            SHARPNESS -> SampleFilters.getAweStruckVibeFilter()
            FILTER -> SampleFilters.getAweStruckVibeFilter()
        }
    }

    fun applyFilter(bitmap: Bitmap): Bitmap {
        return getFilter().processFilter(bitmap.copy(bitmap.config, true))
    }
}
