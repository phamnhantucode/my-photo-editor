package com.phamnhantucode.photoeditor.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView


class ListenableImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : androidx.appcompat.widget.AppCompatImageView(context, attrs) {

    private var onImageChangeListener: ((ImageView) -> Unit)? = null

    fun setOnImageChangeListener(listener: (ImageView) -> Unit) {
        onImageChangeListener = listener
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        onImageChangeListener?.invoke(this)
    }

    override fun setImageURI(uri: android.net.Uri?) {
        super.setImageURI(uri)
        onImageChangeListener?.invoke(this)
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        onImageChangeListener?.invoke(this)
    }

}