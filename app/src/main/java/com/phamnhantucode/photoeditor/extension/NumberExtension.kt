package com.phamnhantucode.photoeditor.extension

import android.content.res.Resources
import android.util.TypedValue

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density).toInt()

val Int.px: Int
    get() = this

val Int.sp: Float
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    )

val Float.dp: Float
    get() = this * Resources.getSystem().displayMetrics.density

val Float.px: Float
    get() = this

val Float.sp: Float
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this,
        Resources.getSystem().displayMetrics
    )

fun Float.positionFInRange(min: Float, max: Float): Int {
    return ((this - min) / (max - min) * 100).toInt()
}

fun Int.valueBasedOnPosition(min: Float, max: Float): Float {
    return (this / 100f) * (max - min) + min
}
