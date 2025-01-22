package com.phamnhantucode.photoeditor.core.model.firebase

import com.google.gson.annotations.SerializedName

data class Version(
    @SerializedName("sticker") var stickerVersion: Int = 0,
    @SerializedName("camera_sticker") var cameraStickerVersion: Int = 0
)
