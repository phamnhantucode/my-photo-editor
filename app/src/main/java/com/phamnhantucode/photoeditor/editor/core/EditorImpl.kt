package com.phamnhantucode.photoeditor.editor.core

import android.view.LayoutInflater
import com.phamnhantucode.photoeditor.databinding.ItemTextEditorBinding
import com.phamnhantucode.photoeditor.editor.core.text.TextEditor
import com.phamnhantucode.photoeditor.editor.core.text.TextEditorState
import com.phamnhantucode.photoeditor.views.EditorView
import com.phamnhantucode.photoeditor.views.StyleableTextView

class EditorImpl(
    private val builder: Editor.Builder
): Editor {
    private val editorView: EditorView = builder.editorView
    private var onEditorListener: OnEditorListener? = null
    private val editorViewState = EditorViewState()
    private val graphicManager = GraphicManager(builder.editorView, editorViewState)

    override fun addText(textViewState: TextEditorState) {
        val multiTouchListener = getMultiTouchListener()
        val textEditor = TextEditor(
            editorView,
            multiTouchListener,
            builder.textTypeface,
            graphicManager,
            editorViewState,
            ItemTextEditorBinding.inflate(LayoutInflater.from(editorView.context))
        )
        textEditor.buildView(textViewState)
        addToEditor(textEditor)
    }

    private fun addToEditor(textEditor: TextEditor) {
        graphicManager.addView(textEditor)
        editorViewState.currentSelectedView = textEditor.rootView
    }

    private fun getMultiTouchListener(): MultiTouchListener {
        return MultiTouchListener(
            editorView,
            onEditorListener,
            editorViewState,
        )

    }

    override fun editText(textView: StyleableTextView, textViewState: TextEditorState) {
        textView.setStyleableTextViewState(textViewState)
        onEditorListener?.onEditTextChangeListener(editorView, textViewState)
    }

    override fun setOnEditorListener(onEditorListener: OnEditorListener) {
        this.onEditorListener = onEditorListener
    }
}