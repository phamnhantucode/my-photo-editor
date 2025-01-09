package com.phamnhantucode.photoeditor.extension

import android.content.res.Resources

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Int.px: Int
    get() = (this / Resources.getSystem().displayMetrics.density).toInt()

val Float.dp: Float
    get() = this * Resources.getSystem().displayMetrics.density

val Float.px: Float
    get() = this / Resources.getSystem().displayMetrics.density

fun Float.positionFInRange(min: Float, max: Float): Int {
    return ((this - min) / (max - min) * 100).toInt()
}

fun Int.valueBasedOnPosition(min: Float, max: Float): Float {
    return (this / 100f) * (max - min) + min
}