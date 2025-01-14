package com.phamnhantucode.photoeditor.editor

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.phamnhantucode.photoeditor.core.model.ui.ImageFilter
import com.phamnhantucode.photoeditor.editor.core.Editor
import com.phamnhantucode.photoeditor.editor.core.text.TextEditorMode
import com.phamnhantucode.photoeditor.editor.core.text.TextEditorState
import java.io.File
import java.io.FileOutputStream

class EditorViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val _originBitmap = MutableLiveData<Bitmap>()
    val originBitmap: LiveData<Bitmap> = _originBitmap

    private val _drawBitmap = MutableLiveData<Bitmap>()
    val drawBitmap: LiveData<Bitmap> = _drawBitmap

    private val _textEditorState = MutableLiveData(
        TextEditorState(
            text = "",
            mode = TextEditorMode.FILL,
            color = Color.WHITE,
            size = Editor.TEXT_SIZE_DEFAULT,
        )
    )
    val textEditorState: LiveData<TextEditorState> = _textEditorState
    private val _isEditingText = MutableLiveData(false)
    var isEditingText: Boolean = false

    var originUri: Uri? = null
    var drawUri: Uri? = null

    private val _moreOptionsVisible = MutableLiveData(false)
    val moreOptionsVisible: LiveData<Boolean> = _moreOptionsVisible

    private val _selectedFilter = MutableLiveData<ImageFilter>()
    val selectedFilter: LiveData<ImageFilter> = _selectedFilter

    fun setOriginBitmapBy(photoUri: Uri) {
        _originBitmap.value = BitmapFactory.decodeFile(photoUri.path)
        originUri = photoUri
    }

    fun setOriginBitmapBy(bitmap: Bitmap) {
        _originBitmap.value = bitmap
        originUri = Uri.fromFile(
            kotlin.io.createTempFile().apply {
                FileOutputStream(this).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
            }
        )
    }

    private fun createTempFile() = File.createTempFile(
        "temp_",
        ".jpg",
        getApplication<Application>().cacheDir
    )

    fun toggleMoreOptions() {
        _moreOptionsVisible.value = !_moreOptionsVisible.value!!
    }

    fun setDrawBitmapBy(parse: Uri?) {
        _drawBitmap.value = BitmapFactory.decodeFile(parse?.path)
        drawUri = parse
    }

    fun setToNextTextMode() {
        _textEditorState.value = _textEditorState.value?.copy(
            mode = when (_textEditorState.value?.mode) {
                TextEditorMode.FILL -> TextEditorMode.STROKE
                TextEditorMode.STROKE -> TextEditorMode.NONE
                else -> TextEditorMode.FILL
            }
        )
    }

    fun setTextOverlaySize(value: Float) {
        _textEditorState.value = _textEditorState.value?.copy(size = value)
    }

    fun setTextOverlayColor(color: Int) {
        _textEditorState.value = _textEditorState.value?.copy(color = color)
    }

    fun setTextOverlayText(text: String) {
        _textEditorState.value = _textEditorState.value?.copy(text = text)
    }

    fun setSelectedFilter(filter: ImageFilter) {
        _selectedFilter.value = filter
    }

    fun clearTextOverlayState() {
        this.isEditingText = false
        _textEditorState.value = _textEditorState.value?.copy(
            text = "",
            mode = TextEditorMode.FILL,
            color = Color.WHITE,
            size = Editor.TEXT_SIZE_DEFAULT,
        )
    }

    fun setTextEditorState(textEditorState: TextEditorState, isEditingText: Boolean = false) {
        this.isEditingText = isEditingText
        _textEditorState.value = textEditorState
    }
}