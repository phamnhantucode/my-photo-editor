package com.phamnhantucode.photoeditor

import android.app.Application
import com.phamnhantucode.photoeditor.core.PhotoEditorFirebaseStorage

class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        PhotoEditorFirebaseStorage.getInstance().loadConfigFile(this)
    }
}