package com.phamnhantucode.photoeditor.editor.core

import android.view.View
import java.util.Stack

class EditorViewState(
) {
    var currentSelectedView: View? = null
    private val addedViews: MutableList<View> = ArrayList()
    private val redoViews: Stack<View> = Stack()

    val addedViewsCount: Int
        get() = addedViews.size

    val redoStackCount: Int
        get() = redoViews.size

    fun clearCurrentSelectedView() {
        currentSelectedView = null
    }

    fun getAddedView(index: Int): View {
        return addedViews[index]
    }

    fun clearAddedViews() {
        addedViews.clear()
    }

    fun addAddedView(view: View) {
        addedViews.add(view)
    }

    fun removeAddedView(view: View) {
        addedViews.remove(view)
    }

    fun removeAddedView(index: Int): View {
        return addedViews.removeAt(index)
    }

    fun containsAddedView(view: View): Boolean {
        return addedViews.contains(view)
    }

    fun replaceAddedView(view: View): Boolean {
        val i = addedViews.indexOf(view)
        if (i > -1) {
            addedViews[i] = view
            return true
        }
        return false
    }

    fun clearRedoViews() {
        redoViews.clear()
    }

    fun pushRedoView(view: View) {
        redoViews.push(view)
    }

    fun popRedoView(): View {
        return redoViews.pop()
    }

    fun getRedoView(index: Int): View {
        return redoViews[index]
    }
}