package com.phamnhantucode.photoeditor.core

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.phamnhantucode.photoeditor.R
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

    fun saveImage(context: Context, bitmap: Bitmap, saveToUri: Uri): Uri {
        val file = saveToUri.toFile()
        file.outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        return saveToUri
    }

    fun shareImage(context: Context, imageUri: Uri) {
        val sharedUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}${context.getString(R.string.file_provider_authority)}",
            imageUri.toFile()
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_STREAM, sharedUri)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_image)))
    }

    private const val IMAGE_GALLERY_PATH = "gallery"
}