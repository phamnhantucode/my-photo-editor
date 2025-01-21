package com.phamnhantucode.photoeditor.editor.core.sticker

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Typeface
import com.phamnhantucode.photoeditor.databinding.ItemStickerEditorBinding
import com.phamnhantucode.photoeditor.editor.core.EditorViewState
import com.phamnhantucode.photoeditor.editor.core.Graphic
import com.phamnhantucode.photoeditor.editor.core.GraphicManager
import com.phamnhantucode.photoeditor.editor.core.MultiTouchListener
import com.phamnhantucode.photoeditor.editor.core.ViewType
import com.phamnhantucode.photoeditor.views.EditorView

class StickerEditor(
    private val editorView: EditorView,
    private val multiTouchListener: MultiTouchListener,
    private val graphicManager: GraphicManager,
    private val editorViewState: EditorViewState,
    private val binding: ItemStickerEditorBinding,
): Graphic(
    editorView.context,
    binding,
    ViewType.STICKER,
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

    }

    override fun updateView() {
    }

    fun buildView(bitmap: Bitmap) {
        binding.ivSticker.setImageBitmap(bitmap)
    }
}