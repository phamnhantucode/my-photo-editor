package com.phamnhantucode.photoeditor.editor.draw

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.phamnhantucode.photoeditor.views.DrawOverlay

class DrawViewModel(application: Application): AndroidViewModel(application) {

    private val _drawBitmap = MutableLiveData<Bitmap>()
    val drawBitmap: LiveData<Bitmap> = _drawBitmap

    private val _drawMode = MutableLiveData(DrawOverlay.PaintType.PEN)
    val drawMode:LiveData<DrawOverlay.PaintType> = _drawMode

    private val _color = MutableLiveData(Color.WHITE)
    val color:LiveData<Int> = _color

    fun setDrawMode(mode: DrawOverlay.PaintType) {
        _drawMode.value = mode
    }

    fun setPaintColor(color: Int) {
        _color.value = color
    }

    fun setDrawBitmapBy(toUri: Uri) {
        _drawBitmap.value = Bitmap.createBitmap(BitmapFactory.decodeFile(toUri.path))
    }
}