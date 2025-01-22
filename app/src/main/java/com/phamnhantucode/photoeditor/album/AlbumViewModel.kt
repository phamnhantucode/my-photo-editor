package com.phamnhantucode.photoeditor.album

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.phamnhantucode.photoeditor.album.model.MyImage
import com.phamnhantucode.photoeditor.core.helper.PhotoEditorGallery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumViewModel(
    private val application: Application,
) : AndroidViewModel(application) {

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _images = MutableLiveData<List<MyImage>>()
    val images = _images as LiveData<List<MyImage>>

    private val _isSelectedMode = MutableLiveData(false)
    val isSelectedMode = _isSelectedMode as LiveData<Boolean>

    init {
        loadImages()
    }

    fun toggleSelectMode() {
        _isSelectedMode.value = !_isSelectedMode.value!!
    }

    fun loadImages() {
        _images.postValue(emptyList())
        viewModelScope.launch {
            _images.value = loadImagesFromStorage()
        }
    }

    fun onSelectImage(image: MyImage) {
        val images = _images.value.orEmpty().map {
            if (it.uri == image.uri) {
                it.copy(isSelected = !it.isSelected)
            } else {
                it
            }
        }
        _images.value = images
    }

    fun removeAllSelectedImages() {
        val images = _images.value.orEmpty().map {
            it.copy(isSelected = false)
        }
        _images.value = images
        _isSelectedMode.value = false
    }

    private suspend fun loadImagesFromStorage() =
        withContext(Dispatchers.IO) {
            val imageUri = PhotoEditorGallery.getImageUris(application)
            imageUri.map {
                MyImage(it)
            }
        }

    suspend fun deleteSelectedImages() {
        withContext(Dispatchers.IO) {
            val selectedImages = _images.value.orEmpty().filter { it.isSelected }
            PhotoEditorGallery.deleteImages(selectedImages.map { it.uri })
            loadImages()
        }
        _isSelectedMode.value = false
    }
}
