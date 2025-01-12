package com.phamnhantucode.photoeditor.views

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.RelativeLayout

class EditorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : RelativeLayout(context, attrs) {
    private val imgSource = ImageView(context)
    val source: ImageView
        get() = imgSource

    init {
        val sourceParams = setupImageSource(attrs)
        addView(imgSource, sourceParams)
    }

    private fun setupImageSource(attrs: AttributeSet?): LayoutParams {
        imgSource.id = sourceId
        imgSource.adjustViewBounds = true
        imgSource.scaleType = ImageView.ScaleType.CENTER_INSIDE

        val widthParam = LayoutParams.MATCH_PARENT

        val params = LayoutParams(
            widthParam, LayoutParams.WRAP_CONTENT
        )

        params.addRule(CENTER_IN_PARENT, TRUE)
        return params
    }

    companion object {
        private const val TAG = "EditorView"
        const val sourceId = 1
    }
}