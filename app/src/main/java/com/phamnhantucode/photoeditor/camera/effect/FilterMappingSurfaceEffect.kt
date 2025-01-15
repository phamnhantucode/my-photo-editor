package com.phamnhantucode.photoeditor.camera.effect

import android.annotation.SuppressLint
import androidx.camera.core.CameraEffect
import com.phamnhantucode.photoeditor.camera.effect.processor.FilterProcessor

@SuppressLint("RestrictedApi")
class FilterMappingSurfaceEffect(
    target: Int = PREVIEW,
    private val processor: FilterProcessor = FilterProcessor(),
) : CameraEffect(
    target,
    TRANSFORMATION_CAMERA_AND_SURFACE_ROTATION,
    processor.glExecutor,
    processor,
    {}
) {
    fun release() {
        processor.release()
    }
}