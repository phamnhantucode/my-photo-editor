package com.phamnhantucode.photoeditor.editor.core.text

import android.annotation.SuppressLint
import android.graphics.Typeface
import androidx.core.view.isVisible
import com.phamnhantucode.photoeditor.databinding.ItemTextEditorBinding
import com.phamnhantucode.photoeditor.editor.core.EditorViewState
import com.phamnhantucode.photoeditor.editor.core.Graphic
import com.phamnhantucode.photoeditor.editor.core.GraphicManager
import com.phamnhantucode.photoeditor.editor.core.MultiTouchListener
import com.phamnhantucode.photoeditor.editor.core.ViewType
import com.phamnhantucode.photoeditor.views.EditorView

class TextEditor(
    private val editorView: EditorView,
    private val multiTouchListener: MultiTouchListener,
    private val defaultTextTypeface: Typeface?,
    private val graphicManager: GraphicManager,
    private val editorViewState: EditorViewState,
    private val binding: ItemTextEditorBinding,
): Graphic(
    editorView.context,
    binding,
    ViewType.TEXT,
    graphicManager,
) {

    init {
        setupGesture()
        setupView()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGesture() {
        val onGestureControl = buildGestureController(editorView, editorViewState)
        multiTouchListener.setOnGestureControl(onGestureControl)
        val rootView = rootView
        rootView.setOnTouchListener(multiTouchListener)
    }

    override fun setupView() {
        binding.tvEditorText.apply {
            gravity = android.view.Gravity.CENTER
            typeface = defaultTextTypeface
        }
    }

    override fun updateView() {
        binding.tvEditorText.isVisible = false
        binding.tvEditorText.textViewState?.let {
            graphicManager.onEditorListener?.onEditTextChangeListener(binding.tvEditorText,
                it
            )
        }
    }

    fun buildView(textViewState: TextEditorState) {
        binding.tvEditorText.setStyleableTextViewState(textViewState)
    }

    override fun buildGestureController(
        editorView: EditorView,
        viewState: EditorViewState
    ): MultiTouchListener.OnGestureControl {
        return object : MultiTouchListener.OnGestureControl {
            override fun onClick() {
                updateView()
            }

            override fun onLongClick() {
                updateView()
            }
        }
    }
}
