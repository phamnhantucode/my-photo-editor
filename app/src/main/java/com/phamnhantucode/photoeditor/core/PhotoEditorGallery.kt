package com.phamnhantucode.photoeditor.core

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import java.io.File

object PhotoEditorGallery {

    suspend fun saveImage(context: Context, bitmap: Bitmap): Uri {
        val imageName = System.currentTimeMillis().toString() + ".png"
        val file = File("${context.filesDir}/$IMAGE_GALLERY_PATH", imageName)
        file.parentFile?.mkdirs()
        file.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return Uri.fromFile(file)
    }

    fun getImageUris(context: Context): List<Uri> {
        val imageDir = File(context.filesDir, IMAGE_GALLERY_PATH)
        return imageDir.listFiles()?.map { it.toUri() } ?: emptyList()
    }

    fun deleteImages(context: Context, imageUris: List<Uri>) {
        imageUris.forEach {
            val file = it.path?.let { it1 -> File(it1) }
            file?.delete()
        }
    }

    private const val IMAGE_GALLERY_PATH = "gallery"
}