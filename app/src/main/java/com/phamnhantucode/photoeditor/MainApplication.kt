package com.phamnhantucode.photoeditor

import android.app.Application
import com.google.firebase.FirebaseApp
import com.phamnhantucode.photoeditor.core.model.PhotoEditorFirebaseStorage

class MainApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        PhotoEditorFirebaseStorage.getInstance().loadConfigFile(this)
    }
}