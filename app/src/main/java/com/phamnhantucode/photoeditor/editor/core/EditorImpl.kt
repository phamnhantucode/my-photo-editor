package com.phamnhantucode.photoeditor.editor.core

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import com.phamnhantucode.photoeditor.core.model.ui.ImageFilter
import com.phamnhantucode.photoeditor.databinding.ItemStickerEditorBinding
import com.phamnhantucode.photoeditor.databinding.ItemTextEditorBinding
import com.phamnhantucode.photoeditor.editor.core.sticker.StickerEditor
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

    private fun addToEditor(graphic: Graphic) {
        graphicManager.addView(graphic)
        editorViewState.currentSelectedView = graphic.rootView
    }

    private fun getMultiTouchListener(): MultiTouchListener {
        return MultiTouchListener(
            editorView,
            onEditorListener,
            editorViewState,
            this,
        )
    }

    override fun editText(textView: StyleableTextView, textViewState: TextEditorState) {
        textView.setStyleableTextViewState(textViewState)
    }

    override fun addSticker(bitmap: Bitmap) {
        val multiTouchListener = getMultiTouchListener()
        val sticker =  StickerEditor(
            editorView,
            multiTouchListener,
            builder.textTypeface,
            graphicManager,
            editorViewState,
            ItemStickerEditorBinding.inflate(LayoutInflater.from(editorView.context))
        )
        sticker.buildView(bitmap)
        addToEditor(sticker)
    }

    override fun setOnEditorListener(onEditorListener: OnEditorListener) {
        this.onEditorListener = onEditorListener
        graphicManager.onEditorListener = onEditorListener
    }

    override fun removeView(v: View) {
        graphicManager.removeView(v)
    }

    override fun setFilter(filter: ImageFilter?) {
        editorView.setFilter(filter)
    }

    override fun setImageFilter(bitmap: Bitmap?) {
        editorView.setImageNeedFilter(bitmap)
    }
}