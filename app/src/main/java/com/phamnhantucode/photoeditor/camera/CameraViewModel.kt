package com.phamnhantucode.photoeditor.camera

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.phamnhantucode.photoeditor.camera.effect.FilterMappingSurfaceEffect
import com.phamnhantucode.photoeditor.core.model.ui.ImageFilter
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private var cameraController: LifecycleCameraController? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val _cameraState = MutableLiveData<CameraState>()
    val cameraState: LiveData<CameraState> = _cameraState

    private val _photoUri = MutableLiveData<Uri>()
    val photoUri: LiveData<Uri> = _photoUri

    private val _currentZoom = MutableLiveData(1f)
    val currentZoom: LiveData<Float> = _currentZoom

    private val _isShowingImageFilters = MutableLiveData(false)
    val isShowingImageFilters: LiveData<Boolean> = _isShowingImageFilters

    private val _filters = MutableLiveData<List<FilterCamera>>()
    val filters: LiveData<List<FilterCamera>> = _filters

    private val _selectedFilter = MutableLiveData<FilterType>(FilterType.NONE)
    val selectedFilter: LiveData<FilterType> = _selectedFilter

    private var _bitmapPreview = MutableLiveData<Bitmap>()
    val bitmapPreview: LiveData<Bitmap> = _bitmapPreview

    private var count = 0

    private var cameraEffect: CameraEffect? = null

    @OptIn(ExperimentalGetImage::class)
    fun initializeCamera(context: Context, surfaceView: PreviewView) {
        try {
            cameraController = LifecycleCameraController(context)
            cameraController?.bindToLifecycle(context as androidx.lifecycle.LifecycleOwner)
            cameraController?.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            surfaceView.controller = cameraController
            _cameraState.postValue(CameraState.Success)
        } catch (e: Exception) {
            _cameraState.postValue(CameraState.Error(e))
        }
    }


    fun flipCamera(context: Context, surfaceView: PreviewView) {
        viewModelScope.launch {
            _cameraState.value = CameraState.Processing
            try {
                cameraController?.cameraSelector = if (cameraController?.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
                _cameraState.value = CameraState.Success
            } catch (e: Exception) {
                _cameraState.value = CameraState.Error(e)
            }
        }
    }

    fun takePicture() {
        viewModelScope.launch {
            _cameraState.value = CameraState.Processing

            val photoFile = File(
                getApplication<Application>().getExternalFilesDir(null),
                SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                    .format(System.currentTimeMillis()) + ".jpg"
            )

            cameraController?.takePicture(
                ImageCapture.OutputFileOptions.Builder(photoFile).build(),
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                        val rotatedBitmap = bitmap?.let { rotateBitmapIfNeeded(it, photoFile.absolutePath) }
                        val filteredBitmap = selectedFilter.value?.applyFilter(rotatedBitmap ?: bitmap)

                        filteredBitmap?.let {
                            _bitmapPreview.postValue(it)
                            photoFile.outputStream()
                                .use { os -> it.compress(Bitmap.CompressFormat.JPEG, 100, os) }
                        }

                        val savedUri = Uri.fromFile(photoFile)
                        _photoUri.postValue(savedUri)
                        _cameraState.postValue(CameraState.Success)
                    }

                    override fun onError(exc: ImageCaptureException) {
                        _cameraState.postValue(CameraState.Error(exc))
                    }
                }
            )
            (cameraEffect as? FilterMappingSurfaceEffect)?.release()
        }
    }

    private fun rotateBitmapIfNeeded(bitmap: Bitmap, photoPath: String): Bitmap {
        val exif = ExifInterface(photoPath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun toggleImageFilters() {
        _isShowingImageFilters.value = isShowingImageFilters.value?.not()
    }

    fun setZoom(zoomLevel: Float) {
        viewModelScope.launch {
            try {
                val nZoomLevel = zoomLevel.coerceAtLeast(MIN_ZOOM).coerceAtMost(MAX_ZOOM)
                cameraController?.setZoomRatio(nZoomLevel)
                _currentZoom.value = nZoomLevel
            } catch (e: Exception) {
                _cameraState.value = CameraState.Error(e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            cameraExecutor.shutdown()
            cameraExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applyFilter(filter: ImageFilter) {
        val effects = mutableSetOf<CameraEffect>()
        filter.toCameraEffect().let { effects.add(it) }
        cameraEffect = effects.first()
        cameraController?.setEffects(effects)
    }

    sealed class CameraState {
        data object Success : CameraState()
        data object Processing : CameraState()
        data class Error(val exception: Throwable) : CameraState()
    }

    companion object {
        private const val TAG = "CameraViewModel"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val MAX_ZOOM = 4f
        private const val MIN_ZOOM = 1f
    }
}