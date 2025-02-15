package com.phamnhantucode.photoeditor.core.model.firebase

import com.google.gson.annotations.SerializedName

data class DataCenter(
    @SerializedName("stickers") var stickers: Stickers? = null,
    @SerializedName("camera_stickers") var cameraStickers: CameraStickers? = null,
    )
