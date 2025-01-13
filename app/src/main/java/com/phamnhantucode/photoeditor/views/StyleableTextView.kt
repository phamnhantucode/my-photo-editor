package com.phamnhantucode.photoeditor.views

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.editor.core.text.TextEditorMode
import com.phamnhantucode.photoeditor.editor.core.text.TextEditorState
import com.phamnhantucode.photoeditor.extension.getContrastTextColor

class StyleableTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : androidx.appcompat.widget.AppCompatTextView(context, attrs) {

    var textViewState: TextEditorState? = null

    fun setStyleableTextViewState(textViewState: TextEditorState) {
        isVisible = true
        this.textViewState = textViewState
        setText(textViewState.text)
        textSize = textViewState.size
        when (textViewState.mode) {
            TextEditorMode.FILL -> {
                setTextColor(textViewState.color.getContrastTextColor())
                background = ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.bg_text_fill,
                    null
                )
                backgroundTintList = ColorStateList.valueOf(textViewState.color)
            }
            TextEditorMode.STROKE -> {
                setTextColor(textViewState.color)
                (ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.bg_text_demo_stroke,
                    null
                ) as? GradientDrawable)?.apply {
                    mutate()
                    setStroke(
                        resources.getDimensionPixelSize(R.dimen.stroke_width_2dp),
                        textViewState.color
                    )
                    background = this
                }
            }
            else ->{
                setTextColor(textViewState.color)
                setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }
}