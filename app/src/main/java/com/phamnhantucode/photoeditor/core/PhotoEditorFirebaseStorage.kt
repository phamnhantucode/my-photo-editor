package com.phamnhantucode.photoeditor.core

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.google.gson.Gson
import com.phamnhantucode.photoeditor.core.model.Version
import com.phamnhantucode.photoeditor.core.model.firebase.DataCenter
import com.phamnhantucode.photoeditor.core.model.firebase.Sticker
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class PhotoEditorFirebaseStorage {
    private val storageRef = Firebase.storage.reference
    private val gson = Gson()
    private var dataCenter = DataCenter()


    private fun downloadConfigFile(context: Context) {
        val configRef = storageRef.child(CONFIG_FILE_NAME)
        configRef.getBytes(Long.MAX_VALUE).addOnSuccessListener {
            val config = String(it)
            saveConfigToInternalStorage(context, config)
        }.addOnFailureListener {
            println("Error downloading config file")
        }
    }

    fun loadConfigFile(context: Context) {
        val configFile = File(context.filesDir, CONFIG_FILE_INTERNAL_PATH)

        if (!configFile.exists()) {
            downloadConfigFile(context)
        } else {
            try {
                FileInputStream(configFile).use { inputStream ->
                    val configContent = inputStream.bufferedReader().use { it.readText() }
                    dataCenter = gson.fromJson(configContent, DataCenter::class.java)
                }
                shouldUpdateConfigFile(context)
            } catch (e: Exception) {
                println("Error loading config file: ${e.message}")
            }
        }
    }

    private fun shouldUpdateConfigFile(context: Context) {
        val versionRef = storageRef.child(VERSION_FILE_NAME)
        versionRef.getBytes(Long.MAX_VALUE).addOnSuccessListener {
            val version = gson.fromJson(String(it), Version::class.java)
            if (dataCenter.stickers?.version != version.stickerVersion) {
                downloadConfigFile(context)
            }
        }.addOnFailureListener {
            println("Error downloading config file")
        }
    }

    private fun saveConfigToInternalStorage(context: Context, config: String) {
        try {
            val configFile = File(context.filesDir, CONFIG_FILE_INTERNAL_PATH)
            FileOutputStream(configFile).use { outputStream ->
                outputStream.write(config.toByteArray())
            }
            loadConfigFile(context)
        } catch (e: Exception) {
            println("Error saving config file: ${e.message}")
        }
    }

    fun getStickers(searchKey: String = ""): ArrayList<Sticker> {
        return dataCenter.stickers?.stickers?.filter {
            it.name!!.contains(searchKey, ignoreCase = true)
        } as ArrayList<Sticker>
    }

    fun getFileUrl(context: Context, filePath: String, onSuccessCallback: (uri: Uri) -> Unit) {
        val file = File(context.filesDir, filePath)
        if (file.exists()) {
            dataCenter.stickers?.stickers?.find { it.path == filePath }?.isDownloaded = true
            onSuccessCallback(Uri.fromFile(file))
        } else {
            storageRef.child(filePath).downloadUrl.addOnSuccessListener { uri ->
                dataCenter.stickers?.stickers?.find { it.path == filePath }?.url = uri.toString()
                onSuccessCallback(uri)
            }.addOnFailureListener {
                println("Error getting file url")
            }
        }
    }

    fun downloadFile(path: String, file: File, onSuccessCallback: () -> Unit) {
        storageRef.child(path).getFile(file).addOnSuccessListener {
            onSuccessCallback()
        }.addOnFailureListener {
            println("Error downloading file")
        }
    }

    fun downloadFile(context: Context, path: String, onSuccessCallback: (Uri) -> Unit) {
        val file = File(context.filesDir, path)
        file.parentFile?.mkdirs()
        storageRef.child(path).getFile(file).addOnSuccessListener {
            onSuccessCallback(Uri.fromFile(file))
            dataCenter.stickers?.stickers?.find { it.path == path }?.isDownloaded = true
        }.addOnFailureListener {
            println("Error downloading file")
        }
    }

    companion object {
        const val CONFIG_FILE_NAME = "config.json"
        const val VERSION_FILE_NAME = "version.json"
        const val CONFIG_FILE_INTERNAL_PATH = "config.json"

        @SuppressLint("StaticFieldLeak")
        private var instance: PhotoEditorFirebaseStorage? = null

        fun getInstance(): PhotoEditorFirebaseStorage {
            return instance ?: synchronized(this) {
                instance ?: PhotoEditorFirebaseStorage().also { instance = it }
            }
        }
    }
}