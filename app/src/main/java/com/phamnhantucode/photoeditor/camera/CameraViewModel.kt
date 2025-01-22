package com.phamnhantucode.photoeditor.camera

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.phamnhantucode.photoeditor.camera.effect.FilterMappingSurfaceEffect
import com.phamnhantucode.photoeditor.camera.helper.FaceDetectorHelper
import com.phamnhantucode.photoeditor.core.model.firebase.CameraSticker
import com.phamnhantucode.photoeditor.core.model.ui.ImageFilter
import com.phamnhantucode.photoeditor.views.CameraFaceDetectOverlayView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraViewModel(
    private val application: Application,
) : AndroidViewModel(application) {
    private var cameraController: LifecycleCameraController? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val _cameraState = MutableLiveData<CameraState>()
    val cameraState: LiveData<CameraState> = _cameraState

    private val _photoUri = MutableLiveData<Uri>()
    val photoUri: LiveData<Uri> = _photoUri

    private val _cameraSelector = MutableLiveData(CameraSelector.DEFAULT_BACK_CAMERA)
    val cameraSelector: LiveData<CameraSelector> = _cameraSelector

    private val _currentZoom = MutableLiveData(1f)
    val currentZoom: LiveData<Float> = _currentZoom

    private val _isShowingImageFilters = MutableLiveData(false)
    val isShowingImageFilters: LiveData<Boolean> = _isShowingImageFilters

    private val _selectedFilter = MutableLiveData<ImageFilter>()
    val selectedFilter: LiveData<ImageFilter> = _selectedFilter

    private val _faceSticker = MutableLiveData<CameraSticker?>(null)
    val faceSticker: LiveData<CameraSticker?> = _faceSticker

    private var _bitmapPreview = MutableLiveData<Bitmap>()

    private var cameraEffect: CameraEffect? = null

    private val _faceDetectorResult = MutableLiveData<FaceDetectorHelper.ResultBundle>()
    val faceDetectorResult: LiveData<FaceDetectorHelper.ResultBundle> = _faceDetectorResult

    private lateinit var faceDetectorHelper: FaceDetectorHelper

    @OptIn(ExperimentalGetImage::class)
    fun initializeCamera(context: Context, surfaceView: PreviewView) {
        try {
            cameraController = LifecycleCameraController(context)
            cameraController?.bindToLifecycle(context as androidx.lifecycle.LifecycleOwner)
            cameraController?.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            surfaceView.controller = cameraController
            faceDetectorHelper = FaceDetectorHelper(
                context = application,
                faceDetectorListener = object : FaceDetectorHelper.DetectorListener {
                    override fun onError(error: String, errorCode: Int) {

                    }

                    override fun onResults(resultBundle: FaceDetectorHelper.ResultBundle) {
                        _faceDetectorResult.postValue(resultBundle)
                    }

                })
            cameraController?.setImageAnalysisAnalyzer(cameraExecutor) { imageProxy ->
                if (cameraState.value != CameraState.Processing) {
                    faceDetectorHelper.detectLivestreamFrame(imageProxy)
                }
                imageProxy.close()
            }
            _cameraState.postValue(CameraState.Success)
        } catch (e: Exception) {
            _cameraState.postValue(CameraState.Error(e))
        }
    }


    fun flipCamera() {
        viewModelScope.launch {
            _cameraState.value = CameraState.Processing
            try {
                cameraController?.cameraSelector =
                    if (cameraController?.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    } else {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    }
                _cameraSelector.postValue(cameraController?.cameraSelector)
                _cameraState.value = CameraState.Success
            } catch (e: Exception) {
                _cameraState.value = CameraState.Error(e)
            }
        }
    }

    fun takePicture(faceDetectOverlay: CameraFaceDetectOverlayView) {
        if (cameraState.value != CameraState.Processing) {
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
                            val rotatedBitmap = bitmap?.let {
                                rotateBitmapIfNeeded(
                                    it,
                                    photoFile.absolutePath,
                                    cameraSelector.value
                                )
                            }?.copy(Bitmap.Config.ARGB_8888, true)

                            val filteredBitmap =
                                faceDetectOverlay.drawOnBitmap(rotatedBitmap!!).let {
                                    selectedFilter.value?.applyFilter(
                                        application,
                                        it
                                    )
                                }

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
    }

    private fun rotateBitmapIfNeeded(
        bitmap: Bitmap,
        photoPath: String,
        cameraSelector: CameraSelector?,
    ): Bitmap {
        val exif = ExifInterface(photoPath)
        val orientation =
            exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        if (cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA) {
            matrix.postRotate(-90f)
        }

        when (orientation) {
            ExifInterface.ORIENTATION_NORMAL -> return bitmap
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }

            else -> return bitmap
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
            faceDetectorHelper.clearFaceDetector()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applyFilter(filter: ImageFilter) {
        viewModelScope.launch(Dispatchers.IO) {
            val effects = mutableSetOf<CameraEffect>()
            filter.toCameraEffect().let { effects.add(it) }
            cameraEffect = effects.first()
            withContext(Dispatchers.Main) {
                cameraController?.setEffects(effects)
            }
            _selectedFilter.postValue(filter)
        }
    }

    fun setFaceSticker(sticker: CameraSticker?) {
        _faceSticker.postValue(sticker)
    }

    sealed class CameraState {
        data object Success : CameraState()
        data object Processing : CameraState()
        data class Error(val exception: Throwable) : CameraState()
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val MAX_ZOOM = 4f
        private const val MIN_ZOOM = 1f
    }
}
