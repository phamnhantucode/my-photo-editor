package com.phamnhantucode.photoeditor.extension

import android.graphics.Rect
import android.view.View

fun View.getVisibleRect(): Rect {
    val location = IntArray(2)
    getLocationOnScreen(location)
    return Rect(
        location[0],
        location[1],
        location[0] + width,
        location[1] + height
    )
}