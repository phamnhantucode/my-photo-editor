package com.phamnhantucode.photoeditor.editor.core

import android.content.Context
import android.graphics.Rect
import androidx.viewbinding.ViewBinding
import com.phamnhantucode.photoeditor.views.EditorView
import com.phamnhantucode.photoeditor.views.GraphicView

abstract class Graphic(
    val context: Context,
    private val layoutBinding: ViewBinding,
    val viewType: ViewType,
    private val graphicManager: GraphicManager,
) {
    var rootView: GraphicView = layoutBinding.root as GraphicView

    init {
        setupRemoveView(rootView)
    }

    private fun setupRemoveView(rootView: GraphicView) {
        rootView.tag = viewType
        rootView.setOnCloseListener {
            graphicManager.removeView(this)
        }
    }

    fun toggleSelection() {
        rootView.toggleSelection()
    }

    open fun buildGestureController(
        editorView: EditorView,
        viewState: EditorViewState,
    ): MultiTouchListener.OnGestureControl {
        return object : MultiTouchListener.OnGestureControl {
            override fun onClick() {
                viewState.currentSelectedView = rootView
            }

            override fun onLongClick() {
                updateView()
            }

        }
    }

    fun getRect(): Rect {
        val location = IntArray(2)
        rootView.getLocationOnScreen(location)
        return Rect(
            location[0],
            location[1],
            location[0] + rootView.width,
            location[1] + rootView.height
        )
    }

    abstract fun setupView()
    abstract fun updateView()
}