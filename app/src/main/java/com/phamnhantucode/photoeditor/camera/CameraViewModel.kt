package com.phamnhantucode.photoeditor.camera

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import toBitmap
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var lensFacing = CameraSelector.LENS_FACING_BACK

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

    private lateinit var imageAnalysis: ImageAnalysis

    private var count = 0

    @OptIn(ExperimentalGetImage::class)
    fun initializeCamera(context: Context, surfaceView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(context, surfaceView)
            } catch (e: Exception) {
                _cameraState.postValue(CameraState.Error(e))
            }
        }, ContextCompat.getMainExecutor(context))

        imageAnalysis = ImageAnalysis.Builder()
            .build()
            .also {
                it.setAnalyzer(cameraExecutor) { image ->
                    viewModelScope.launch(Dispatchers.IO) {
                        if (count % 3 == 0) {
                            val bitmap = image.toBitmap()
                            _bitmapPreview.postValue(bitmap!!)
                        }
                        count++
                        image.close()
                    }
                }
            }
    }

    private fun bindCameraUseCases(context: Context, surfaceView: PreviewView) {
        viewModelScope.launch {
            try {
                val cameraProvider =
                    cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

                withTimeout(UNBIND_TIMEOUT) {
                    cameraProvider.unbindAll()
                }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(surfaceView.surfaceProvider)
                    }

                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                try {
                    withTimeout(BIND_TIMEOUT) {
                        camera = cameraProvider.bindToLifecycle(
                            context as androidx.lifecycle.LifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture,
                            imageAnalysis
                        )
                    }
                    _cameraState.postValue(CameraState.Success)
                } catch (e: Exception) {
                    when (e) {
                        is CancellationException -> throw e
                        else -> _cameraState.postValue(CameraState.Error(e))
                    }
                }
            } catch (e: Exception) {
                _cameraState.postValue(CameraState.Error(e))
            }
        }
    }

    fun flipCamera(context: Context, surfaceView: PreviewView) {
        viewModelScope.launch {
            _cameraState.value = CameraState.Processing
            try {
                camera?.cameraControl?.cancelFocusAndMetering()
                cameraProvider?.unbindAll()

                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }

                bindCameraUseCases(context, surfaceView)
            } catch (e: Exception) {
                _cameraState.value = CameraState.Error(e)
            }
        }
    }

    fun takePicture() {
        viewModelScope.launch {
            _cameraState.value = CameraState.Processing
            val imageCapture = imageCapture ?: return@launch

            val photoFile = File(
                getApplication<Application>().getExternalFilesDir(null),
                SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                    .format(System.currentTimeMillis()) + ".jpg"
            )

            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val savedUri = Uri.fromFile(photoFile)
                        _photoUri.postValue(savedUri)
                        _cameraState.postValue(CameraState.Success)
                    }

                    override fun onError(exc: ImageCaptureException) {
                        _cameraState.postValue(CameraState.Error(exc))
                    }
                }
            )
        }
    }

    fun toggleImageFilters() {
        _isShowingImageFilters.value = isShowingImageFilters.value?.not()
    }

    fun setZoom(zoomLevel: Float) {
        viewModelScope.launch {
            try {
                val nZoomLevel = zoomLevel.coerceAtLeast(MIN_ZOOM).coerceAtMost(MAX_ZOOM)
                camera?.cameraControl?.setZoomRatio(nZoomLevel)
                _currentZoom.value = nZoomLevel
            } catch (e: Exception) {
                _cameraState.value = CameraState.Error(e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
            cameraExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun applyFilter(filter: FilterType) {
        val bitmap = _bitmapPreview.value ?: return
        val filterCamera = FilterCamera(
            filter.name,
            bitmap,
            filter
        )
        _selectedFilter.value = filterCamera.type

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
        private const val BIND_TIMEOUT = 5000L
        private const val UNBIND_TIMEOUT = 1000L
        private const val TIME_STEP = 100L
    }
}