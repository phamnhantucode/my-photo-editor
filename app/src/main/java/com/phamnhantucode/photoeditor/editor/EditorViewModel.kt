package com.phamnhantucode.photoeditor.editor

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.io.File
import java.io.FileOutputStream

class EditorViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _originBitmap = MutableLiveData<Bitmap>()
    val originBitmap: LiveData<Bitmap> = _originBitmap
    var originUri: Uri? = null

    fun setOriginBitmapBy(photoUri: Uri) {
        _originBitmap.value = BitmapFactory.decodeFile(photoUri.path)
        originUri = photoUri
    }

    fun setOriginBitmapBy(bitmap: Bitmap) {
        _originBitmap.value = bitmap
        originUri = Uri.fromFile(
            kotlin.io.createTempFile().apply {
                FileOutputStream(this).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            }
        )
    }

    private fun createTempFile() = File.createTempFile(
        "temp_",
        ".jpg",
        getApplication<Application>().cacheDir
    )
}