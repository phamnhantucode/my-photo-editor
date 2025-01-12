package com.phamnhantucode.photoeditor.editor.core

import android.content.Context
import android.graphics.Typeface
import android.widget.ImageView
import com.phamnhantucode.photoeditor.editor.core.text.TextEditorState
import com.phamnhantucode.photoeditor.extension.sp
import com.phamnhantucode.photoeditor.views.EditorView
import com.phamnhantucode.photoeditor.views.StyleableTextView

interface Editor {
    fun addText(textViewState: TextEditorState)

    fun editText(textView: StyleableTextView, textViewState: TextEditorState)

    fun setOnEditorListener(onEditorListener: OnEditorListener)

    class Builder(
        val context: Context,
        val editorView: EditorView
    ) {
        val imageView: ImageView = editorView.source

        var textTypeface: Typeface? = null

        var isTextPinchScalable = true

        fun setDefaultTextTypeface(textTypeface: Typeface?): Builder {
            this.textTypeface = textTypeface
            return this
        }

        fun setPinchTextScalable(isTextPinchScalable: Boolean): Builder {
            this.isTextPinchScalable = isTextPinchScalable
            return this
        }

        fun build(): Editor {
            return EditorImpl(this)
        }
    }

    companion object {
        const val TEXT_SIZE_DEFAULT = 20f
    }
}