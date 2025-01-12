package com.phamnhantucode.photoeditor.editor.core

import android.content.Context
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

    fun buildGestureController(
        editorView: EditorView,
        viewState: EditorViewState,
    ): MultiTouchListener.OnGestureControl {
        return object : MultiTouchListener.OnGestureControl {
            override fun onClick() {
                toggleSelection()
                viewState.currentSelectedView = rootView
            }

            override fun onLongClick() {
                updateView()
            }

        }
    }

    abstract fun setupView()
    abstract fun updateView()
}