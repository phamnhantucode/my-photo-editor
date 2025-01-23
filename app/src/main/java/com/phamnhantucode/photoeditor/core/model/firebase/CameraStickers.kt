package com.phamnhantucode.photoeditor.core.model.firebase

import android.graphics.Point
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import com.google.gson.annotations.SerializedName
import com.google.mediapipe.tasks.components.containers.Detection

data class CameraStickers(
    @SerializedName("version") var version: Int = 0,
    @SerializedName("stickers") var stickers: ArrayList<CameraSticker> = arrayListOf(),
)

data class CameraSticker(
    @SerializedName("id") var id: Int? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("partials") var partials: ArrayList<CameraStickerPartial> = arrayListOf(),
    var isDownloaded: Boolean = false,
)

data class CameraStickerPartial(
    @SerializedName("filename") var fileName: String? = null,
    @SerializedName("path") var path: String? = null,
    @SerializedName("position") var position: CameraStickerPosition? = null,
    var uri: Uri? = null,
)

enum class CameraStickerPosition {
    @SerializedName("TOP_OF_HEAD")
    TOP_OF_HEAD,

    @SerializedName("FOREHEAD")
    FOREHEAD,

    @SerializedName("CHEEK")
    CHEEK,

    @SerializedName("NOSE")
    NOSE,

    @SerializedName("MOUTH")
    MOUTH,

    @SerializedName("CHIN")
    CHIN,

    @SerializedName("EYE")
    EYE,

    @SerializedName("EAR")
    EAR;

    fun getDrawGuideBy(result: Detection): DrawGuide {
        val faceBox = result.boundingBox()
        val keyPoints = result.keypoints().get()
        val ear1Position = PointF(faceBox.left + keyPoints[4].x() * faceBox.width(), faceBox.top + keyPoints[4].y() * faceBox.height())
        val ear2Position = PointF(faceBox.left + keyPoints[5].x() * faceBox.width(), faceBox.top + keyPoints[5].y() * faceBox.height())
        val eye1Position = PointF(faceBox.left + keyPoints[0].x() * faceBox.width(), faceBox.top + keyPoints[0].y() * faceBox.height())
        val eye2Position = PointF(faceBox.left + keyPoints[1].x() * faceBox.width(), faceBox.top + keyPoints[1].y() * faceBox.height())
        val nosePosition = PointF(faceBox.left + keyPoints[2].x() * faceBox.width(), faceBox.top + keyPoints[2].y() * faceBox.height())
        val mouthPosition = PointF(faceBox.left + keyPoints[3].x() * faceBox.width(), faceBox.top + keyPoints[3].y() * faceBox.height())
        return when (this) {
            TOP_OF_HEAD -> {
                DrawGuide(
                    faceBox.left + faceBox.width() / 2,
                    faceBox.top - faceBox.height() / 2 - faceBox.height() / 8,
                    faceBox.width(),
                    faceBox.height()
                )
            }

            FOREHEAD -> {
                DrawGuide(
                    faceBox.left + faceBox.width() / 2,
                    eye1Position.y + (eye2Position.y - eye1Position.y) / 2,
                    faceBox.width(),
                    faceBox.height()
                )
            }

            CHEEK -> {
                DrawGuide(
                    nosePosition.x,
                    nosePosition.y  - faceBox.height() / 8,
                    faceBox.width(),
                    faceBox.height()
                )
            }

            NOSE -> {
                DrawGuide(
                    nosePosition.x,
                    nosePosition.y  - faceBox.height() / 8,
                    faceBox.width() / 5,
                    faceBox.height() / 5
                )
            }

            MOUTH -> {
                DrawGuide(
                    mouthPosition.x,
                    mouthPosition.y + faceBox.height() / 8,
                    faceBox.width() / 2,
                    faceBox.height() / 4
                )
            }

            CHIN -> {
                DrawGuide(
                    faceBox.left + faceBox.width() / 2,
                    faceBox.bottom  - faceBox.height() / 8,
                    faceBox.width() / 5,
                    faceBox.height() / 5
                )
            }

            EYE -> {
                DrawGuide(
                    eye1Position.x + (eye2Position.x - eye1Position.x) / 2,
                    eye1Position.y + (eye2Position.y - eye1Position.y) / 2  - faceBox.height() / 6,
                    faceBox.width(),
                    faceBox.height() / 2
                )
            }

            EAR -> {
                DrawGuide(
                    ear1Position.x + (ear2Position.x - ear1Position.x) / 2,
                    ear1Position.y + (ear2Position.y - ear1Position.y) / 2  - faceBox.height() / 8,
                    faceBox.width(),
                    faceBox.height()
                )
            }
        }
    }

    data class DrawGuide(
        val centerPointX: Float,
        val centerPointY: Float,
        val width: Float,
        val height: Float,
    ) {
        fun getRectF(): RectF {
            return RectF(
                centerPointX - width / 2,
                centerPointY - height / 2,
                centerPointX + width / 2,
                centerPointY + height / 2
            )
        }
    }
}
