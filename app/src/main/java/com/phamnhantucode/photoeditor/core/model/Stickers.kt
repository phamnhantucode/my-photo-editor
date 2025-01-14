package com.phamnhantucode.photoeditor.core.model

import com.google.gson.annotations.SerializedName

data class Stickers(
    @SerializedName("version") var version: Int = 0,
    @SerializedName("stickers") var stickers: ArrayList<Sticker> = arrayListOf(),
    )