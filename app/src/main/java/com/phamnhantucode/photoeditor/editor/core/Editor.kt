package com.phamnhantucode.photoeditor.editor.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.view.View
import android.widget.ImageView
import com.phamnhantucode.photoeditor.core.model.ui.ImageFilter
import com.phamnhantucode.photoeditor.editor.core.text.TextEditorState
import com.phamnhantucode.photoeditor.views.EditorView
import com.phamnhantucode.photoeditor.views.StyleableTextView

interface Editor {
    fun addText(textViewState: TextEditorState)

    fun editText(textView: StyleableTextView, textViewState: TextEditorState)

    fun addSticker(bitmap: Bitmap)

    fun setOnEditorListener(onEditorListener: OnEditorListener)

    fun removeView(v: View)

    fun setFilter(filter: ImageFilter)

    fun setImageFilter(bitmap: Bitmap?)

    class Builder(
        val context: Context,
        val editorView: EditorView,
    ) {
        var textTypeface: Typeface? = null

        fun build(): Editor {
            return EditorImpl(this)
        }
    }

    companion object {
        const val TEXT_SIZE_DEFAULT = 20f
    }
}
