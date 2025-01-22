package com.phamnhantucode.photoeditor.editor.crop

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.io.File

class CropViewModel(application: Application) : AndroidViewModel(application) {
    private val _imageUri = MutableLiveData<Uri>()
    val imageUri: LiveData<Uri> = _imageUri
    val tempUri: Uri = Uri.fromFile(createTempFile())

    private val _selectedTool = MutableLiveData(SelectedTool.NONE)
    val selectedTool: LiveData<SelectedTool> = _selectedTool

    private val _selectedRatio = MutableLiveData(SelectedRatio.ORIGIN)
    val selectedRatio: LiveData<SelectedRatio> = _selectedRatio

    private val _rotationAngle = MutableLiveData(0f)
    val rotationAngle: LiveData<Float> = _rotationAngle

    private val _scale = MutableLiveData(1f)
    val scale: LiveData<Float> = _scale

    fun setImageUri(uri: Uri) {
        _imageUri.value = uri
    }

    private fun createTempFile(): File {
        return File.createTempFile(
            "temp_",
            ".jpg",
            getApplication<Application>().cacheDir
        )
    }

    fun setTool(tool: CropViewModel.SelectedTool) {
        _selectedTool.value = tool
    }

    fun setRatio(ratio: CropViewModel.SelectedRatio) {
        _selectedRatio.value = ratio

    }

    fun setRotationAngle(fl: Float) {
        _rotationAngle.value = fl
    }

    fun setScale(fl: Float) {
        _scale.value = fl
    }

    enum class SelectedTool {
        SCALE, ROTATE, RATIO, NONE
    }

    enum class SelectedRatio {
        RATIO_1_1, RATIO_4_3, ORIGIN, RATIO_3_2, RATIO_16_9;

        fun getRatio(): Float {
            return when (this) {
                RATIO_1_1 -> 1f
                RATIO_4_3 -> 4f / 3f
                RATIO_3_2 -> 3f / 2f
                RATIO_16_9 -> 16f / 9f
                else -> 0f
            }
        }
    }
}
