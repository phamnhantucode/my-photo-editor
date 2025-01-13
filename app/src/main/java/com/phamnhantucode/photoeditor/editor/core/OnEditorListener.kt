package com.phamnhantucode.photoeditor.editor.core

import android.view.MotionEvent
import android.view.View
import com.phamnhantucode.photoeditor.editor.core.text.TextEditorState
import com.phamnhantucode.photoeditor.views.StyleableTextView

interface OnEditorListener {

    fun onEditTextChangeListener(rootView: StyleableTextView, textEditorState: TextEditorState)

    fun onAddViewListener(viewType: ViewType, numberOfAddedViews: Int)

    fun onRemoveViewListener(viewType: ViewType, numberOfAddedViews: Int)

    fun onStartViewChangeListener(viewType: ViewType)

    fun onStopViewChangeListener(viewType: ViewType)

    fun onTouchSourceImage(event: MotionEvent)
}