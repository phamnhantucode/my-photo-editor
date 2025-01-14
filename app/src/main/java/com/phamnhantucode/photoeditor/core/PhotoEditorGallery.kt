package com.phamnhantucode.photoeditor.core

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File

object PhotoEditorGallery {

    suspend fun saveImage(context: Context, bitmap: Bitmap): Uri {
        val imageName = System.currentTimeMillis().toString() + ".png"
        val file = File("${context.filesDir}$IMAGE_GALLERY_PATH", imageName)
        file.parentFile?.mkdirs()
        file.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return Uri.fromFile(file)
    }

    private const val IMAGE_GALLERY_PATH = "gallery/"
}