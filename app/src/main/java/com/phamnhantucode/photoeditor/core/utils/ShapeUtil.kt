package com.phamnhantucode.photoeditor.core.utils

import android.graphics.Rect

object ShapeUtil {
    fun hasSignificantOverlap(rect1: Rect, rect2: Rect, threshold: Double = 0.5): Boolean {
        // Get intersection rectangle
        val intersection = Rect()
        if (!intersection.setIntersect(rect1, rect2)) {
            return false // No intersection at all
        }

        // Calculate areas
        val intersectionArea = intersection.width() * intersection.height()
        val rect1Area = rect1.width() * rect1.height()
        val rect2Area = rect2.width() * rect2.height()

        // Get the smaller rectangle's area
        val smallerArea = minOf(rect1Area, rect2Area)

        // Calculate overlap percentage relative to smaller rectangle
        val overlapPercentage = intersectionArea.toDouble() / smallerArea

        return overlapPercentage > threshold
    }
}

fun Rect.isCenterInside(rect: Rect): Boolean {
    // Calculate center point of first rectangle
    val centerX = this.left + (this.width() / 2)
    val centerY = this.top + (this.height() / 2)

    // Check if this point lies within the second rectangle
    return rect.contains(centerX, centerY)
}
