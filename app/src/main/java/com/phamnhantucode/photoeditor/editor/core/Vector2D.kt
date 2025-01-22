package com.phamnhantucode.photoeditor.editor.core

import android.graphics.PointF

class Vector2D: PointF() {

    private fun normalize() {
        val length = kotlin.math.sqrt((x * x + y * y).toDouble()).toFloat()
        x /= length
        y /= length
    }

    companion object {
        fun getAngle(vector1: Vector2D, vector2: Vector2D): Float {
            vector1.normalize()
            vector2.normalize()
            val degrees = 180.0 / kotlin.math.PI * (kotlin.math.atan2(
                vector2.y.toDouble(),
                vector2.x.toDouble()
            ) - kotlin.math.atan2(vector1.y.toDouble(), vector1.x.toDouble()))
            return degrees.toFloat()
        }
    }
}
