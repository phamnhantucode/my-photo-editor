package com.phamnhantucode.photoeditor.editor.core

import android.widget.RelativeLayout
import com.phamnhantucode.photoeditor.views.EditorView

class GraphicManager(
    val editorView: EditorView,
    val editorViewState: EditorViewState
){
    var onEditorListener: OnEditorListener? = null

    fun addView(graphic: Graphic) {
        val view = graphic.rootView
        val params = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE)
        editorView.addView(view, params)
        editorViewState.addAddedView(view)

        if (editorViewState.redoStackCount > 0) {
            editorViewState.clearRedoViews()
        }

        onEditorListener?.onAddViewListener(
            graphic.viewType,
            editorViewState.addedViewsCount
        )
    }

    fun removeView(graphic: Graphic) {
        val view = graphic.rootView
        if (editorViewState.containsAddedView(view)) {
            editorView.removeView(view)
            editorViewState.removeAddedView(view)
            editorViewState.pushRedoView(view)
            onEditorListener?.onRemoveViewListener(
                graphic.viewType,
                editorViewState.addedViewsCount
            )
        }
    }

    fun updateView(view: RelativeLayout) {
        editorView.updateViewLayout(view, view.layoutParams)
        editorViewState.replaceAddedView(view)
    }

    fun undoView(): Boolean {
        if (editorViewState.addedViewsCount > 0) {
            return true
        }
        return false
    }
}