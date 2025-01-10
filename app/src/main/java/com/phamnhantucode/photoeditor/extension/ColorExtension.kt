package com.phamnhantucode.photoeditor.extension

import android.graphics.Color

fun Int.getContrastTextColor(): Int {
    // Extract RGB components and convert to relative luminance
    val red = Color.red(this) / 255.0
    val green = Color.green(this) / 255.0
    val blue = Color.blue(this) / 255.0

    // Convert RGB to sRGB
    val sRed = if (red <= 0.03928) red / 12.92 else Math.pow((red + 0.055) / 1.055, 2.4)
    val sGreen = if (green <= 0.03928) green / 12.92 else Math.pow((green + 0.055) / 1.055, 2.4)
    val sBlue = if (blue <= 0.03928) blue / 12.92 else Math.pow((blue + 0.055) / 1.055, 2.4)

    // Calculate relative luminance
    val luminance = 0.2126 * sRed + 0.7152 * sGreen + 0.0722 * sBlue

    // Return white for dark backgrounds, black for light backgrounds
    // Using 0.5 as the threshold, but you can adjust this value
    return if (luminance > 0.5) Color.BLACK else Color.WHITE
}