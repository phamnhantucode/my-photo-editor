package com.phamnhantucode.photoeditor.editor.core

import android.graphics.Rect
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import com.phamnhantucode.photoeditor.views.EditorView
import kotlin.math.max
import kotlin.math.min

class MultiTouchListener(
    private val editorView: EditorView,
    private val onEditorListener: OnEditorListener?,
    private val viewState: EditorViewState
): OnTouchListener {
    private var onGestureControl: OnGestureControl? = null
    private val gestureListener = GestureDetector(editorView.context, GestureListener())
    private val isRotateEnabled = true
    private val isTranslateEnabled = true
    private val isScaleEnabled = true
    private val minimumScale = 0.5f
    private val maximumScale = 10.0f
    private var activePointerId = INVALID_POINTER_ID
    private var prevX = 0f
    private var prevY = 0f
    private var prevRawX = 0f
    private var prevRawY = 0f
    private val scaleGestureDetector = ScaleGestureDetector(ScaleListener())
    private val location = IntArray(2)

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(v, event)
        gestureListener.onTouchEvent(event)
        if (!isTranslateEnabled) {
            return true
        }

        val action = event.action
        val x = event.rawX.toInt()
        val y = event.rawY.toInt()

        when (action and event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                prevX = event.x
                prevY = event.y
                prevRawX = event.rawX
                prevRawY = event.rawY
                activePointerId = event.getPointerId(0)
                v.bringToFront()
                viewState.currentSelectedView = v
                fireEditorSDKListener(v, true)
            }
            MotionEvent.ACTION_MOVE -> {

                if (v === viewState.currentSelectedView) {
                    val pointerIndexMove = event.findPointerIndex(activePointerId)
                    if (pointerIndexMove != -1) {
                        val currX = event.getX(pointerIndexMove)
                        val currY = event.getY(pointerIndexMove)
                        if (!scaleGestureDetector.isInProgress) {
                            adjustTranslation(v, currX - prevX, currY - prevY)
                        }
                    }
                }
            }
            MotionEvent.ACTION_CANCEL -> activePointerId = INVALID_POINTER_ID
            MotionEvent.ACTION_UP -> {
                activePointerId = INVALID_POINTER_ID
                fireEditorSDKListener(v, false)
                if (!isInViewBounds(editorView.source, x, y)) {
                    viewState.currentSelectedView = null
                    v.animate().translationY(0f).translationY(0f)
                }

            }
        }

        return true
    }

    private fun fireEditorSDKListener(view: View, isStart: Boolean) {
        val viewTag = view.tag
        if (onEditorListener != null && viewTag != null && viewTag is ViewType) {
            if (isStart) {
                onEditorListener.onStartViewChangeListener(viewTag)
            } else {
                onEditorListener.onStopViewChangeListener(viewTag)
            }
        }
    }

    private fun isInViewBounds(view: View, x: Int, y: Int): Boolean {
        view.getLocationOnScreen(location)
        val viewRect = Rect(
            location[0],
            location[1],
            location[0] + view.width,
            location[1] + view.height
        )
        return viewRect.contains(x, y)
    }

    interface OnGestureControl {
        fun onClick()
        fun onLongClick()
    }

    fun setOnGestureControl(onGestureControl: OnGestureControl) {
        this.onGestureControl = onGestureControl
    }

    private inner class TransformInfo {
        var deltaX = 0f
        var deltaY = 0f
        var deltaScale = 0f
        var deltaAngle = 0f
        var pivotX = 0f
        var pivotY = 0f
        var minimumScale = 0f
        var maximumScale = 0f
    }


    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            onGestureControl?.onClick()
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            super.onLongPress(e)
            onGestureControl?.onLongClick()
        }
    }

    private inner class ScaleListener :ScaleGestureDetector.SimpleOnScaleGestureListener() {
        private var pivotX = 0f
        private var pivotY = 0f
        private var prevSpanVector = Vector2D()

        override fun onScaleBegin(view: View, detector: ScaleGestureDetector): Boolean {
            pivotX = detector.getFocusX()
            pivotY = detector.getFocusY()
            prevSpanVector.apply {
                x = detector.getCurrentSpanVector().x
                y = detector.getCurrentSpanVector().y
            }
            return true
        }

        override fun onScale(view: View, detector: ScaleGestureDetector): Boolean {
            val info = TransformInfo()
            info.deltaScale = if (isScaleEnabled) detector.getScaleFactor() else 1.0f
            info.deltaAngle = if (isRotateEnabled) Vector2D.getAngle(
                prevSpanVector,
                detector.getCurrentSpanVector()
            ) else 0.0f
            info.deltaX = if (isTranslateEnabled) detector.getFocusX() - pivotX else 0.0f
            info.deltaY = if (isTranslateEnabled) detector.getFocusY() - pivotY else 0.0f
            info.pivotX = pivotX
            info.pivotY = pivotY
            info.minimumScale = minimumScale
            info.maximumScale = maximumScale
            move(view, info)
            return true
        }

    }

    companion object {
        private const val INVALID_POINTER_ID = -1

        private fun adjustAngle(degrees: Float): Float {
            return when {
                degrees > 180.0f -> {
                    degrees - 360.0f
                }
                degrees < -180.0f -> {
                    degrees + 360.0f
                }
                else -> degrees
            }
        }

        private fun move(view: View, info: TransformInfo) {
            computeRenderOffset(view, info.pivotX, info.pivotY)
            adjustTranslation(view, info.deltaX, info.deltaY)
            var scale = view.scaleX * info.deltaScale
            scale = max(info.minimumScale, min(info.maximumScale, scale))
            view.scaleX = scale
            view.scaleY = scale
            val rotation = adjustAngle(view.rotation + info.deltaAngle)
            Log.e("Rotation", "Rotation: $rotation")
            view.rotation = rotation
        }

        private fun adjustTranslation(view: View, deltaX: Float, deltaY: Float) {
            val deltaVector = floatArrayOf(deltaX, deltaY)
            view.matrix.mapVectors(deltaVector)
            view.translationX += deltaVector[0]
            view.translationY += deltaVector[1]
        }

        private fun computeRenderOffset(view: View, pivotX: Float, pivotY: Float) {
            if (view.pivotX == pivotX && view.pivotY == pivotY) {
                return
            }
            val prevPoint = floatArrayOf(0.0f, 0.0f)
            view.matrix.mapPoints(prevPoint)
            view.pivotX = pivotX
            view.pivotY = pivotY
            val currPoint = floatArrayOf(0.0f, 0.0f)
            view.matrix.mapPoints(currPoint)
            val offsetX = currPoint[0] - prevPoint[0]
            val offsetY = currPoint[1] - prevPoint[1]
            view.translationX -= offsetX
            view.translationY -= offsetY
        }
    }
}