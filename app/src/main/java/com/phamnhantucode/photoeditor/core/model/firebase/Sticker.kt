package com.phamnhantucode.photoeditor.core.model.firebase

import com.google.gson.annotations.SerializedName

data class Sticker(
    @SerializedName("id") var id: Int? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("fileName") var fileName: String? = null,
    @SerializedName("path") var path: String? = null,
    @SerializedName("url") var url: String? = null,
    var isDownloaded: Boolean = false
)
