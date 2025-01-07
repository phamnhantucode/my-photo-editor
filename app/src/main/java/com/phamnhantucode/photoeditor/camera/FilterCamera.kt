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
        return getFilter().processFilter(bitmap.copy(bitmap.config, true))
    }

    private fun getFilter(): Filter {
        val filter = Filter()
        return when (this.type) {
            FilterType.NONE -> filter
            FilterType.BRIGHTNESS -> SampleFilters.getStarLitFilter()
            FilterType.CONTRAST -> SampleFilters.getBlueMessFilter()
            FilterType.SATURATION -> SampleFilters.getLimeStutterFilter()
            FilterType.HUE -> SampleFilters.getNightWhisperFilter()
            FilterType.SHARPNESS -> SampleFilters.getAweStruckVibeFilter()
            FilterType.FILTER -> SampleFilters.getAweStruckVibeFilter()
        }
    }
}

enum class FilterType {
    NONE,
    BRIGHTNESS,
    CONTRAST,
    SATURATION,
    HUE,
    SHARPNESS,
    FILTER
}
