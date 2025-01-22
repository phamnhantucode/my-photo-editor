package com.phamnhantucode.photoeditor.album.model

import android.net.Uri

data class MyImage(
    val uri: Uri,
    val isSelected: Boolean = false
)
