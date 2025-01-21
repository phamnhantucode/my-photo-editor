package com.phamnhantucode.photoeditor.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.camera.core.CameraSelector
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.camera.fragment.CameraStickerBottomSheetDialogFragment
import com.phamnhantucode.photoeditor.databinding.ActivityCameraBinding
import com.phamnhantucode.photoeditor.editor.EditorActivity
import com.phamnhantucode.photoeditor.extension.hideSystemBars
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class CameraActivity() : AppCompatActivity() {

    init {
        System.loadLibrary("NativeImageProcessor");
    }

    private lateinit var binding: ActivityCameraBinding
    private val viewModel: CameraViewModel by viewModels()
    private val stickerFragment = CameraStickerBottomSheetDialogFragment {
        viewModel.setFaceSticker(it)
    }

    private val scaleGestureDetector by lazy {
        ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                viewModel.setZoom(detector.scaleFactor)
                return true
            }
        })
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermissionAndStartCamera()
        setupUI()
        observeViewModel()
    }

    private fun checkPermissionAndStartCamera() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }

            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        viewModel.initializeCamera(this, binding.cameraView)
    }

    @OptIn(FlowPreview::class)
    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI() {
        AppCompatResources.getDrawable(this, R.drawable.filter_demo)
            ?.let { binding.filterView.setOriginalPhoto(it.toBitmap()) }
        binding.apply {
            captureBtn.setOnClickListener {
                viewModel.takePicture(binding.faceDetectOverlay)
            }
            switchCameraBtn.setOnClickListener {
                viewModel.flipCamera(
                    this@CameraActivity,
                    binding.cameraView
                )
            }

            zoom1x.setOnClickListener { viewModel.setZoom(1f) }
            zoom2x.setOnClickListener { viewModel.setZoom(2f) }
            zoom4x.setOnClickListener { viewModel.setZoom(4f) }

            filterBtn.setOnClickListener { viewModel.toggleImageFilters() }
            filterView.getFilterFlow().debounce(300).onEach {
                viewModel.applyFilter(it)
            }.launchIn(lifecycleScope)

            cameraView.setOnTouchListener { _, event ->
                scaleGestureDetector.onTouchEvent(event)
                true
            }

            filterSeekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    viewModel.selectedFilter.value?.let { filter ->
                        val value = filter.filterType.normalizeToValueFilter(seekBar?.progress ?: 0)
                        filter.currentValue = value
                        viewModel.applyFilter(filter)
                    }
                }
            })

            faceStickerBtn.setOnClickListener {
                try {
                    stickerFragment.show(supportFragmentManager, stickerFragment.tag)
                } catch (e: IllegalStateException) {
                    e.printStackTrace()
                }
            }
            cancelFaceStickerBtn.setOnClickListener {
                viewModel.setFaceSticker(null)
            }
        }
    }

    private fun observeViewModel() {
        viewModel.cameraState.observe(this) { state ->
            handleCameraState(state)
        }

        viewModel.photoUri.observe(this) { uri ->
            Toast.makeText(this, "Photo captured: $uri", Toast.LENGTH_SHORT).show()
        }

        viewModel.currentZoom.observe(this) { zoom ->
            updateZoomUI(zoom)
        }

        viewModel.isShowingImageFilters.observe(this) { isShowingFilters ->
            binding.filterView.isVisible = isShowingFilters
            binding.filterSeekbar.isVisible =
                isShowingFilters && viewModel.selectedFilter.value?.filterType?.isAdjustable == true
        }

        viewModel.photoUri.observe(this) { uri ->
            startActivity(Intent(this, EditorActivity::class.java).apply {
                putExtra(EditorActivity.EXTRA_IMAGE_URI, uri.toString())
                finish()
            })
        }
        viewModel.selectedFilter.observe(this) { filter ->
            binding.filterSeekbar.visibility =
                if (filter.filterType.isAdjustable) View.VISIBLE else View.INVISIBLE
            binding.filterSeekbar.progress =
                filter.filterType.normalizeToValueSeekbar(filter.currentValue)
        }
        viewModel.faceDetectorResult.observe(this) { bundle ->
            binding.faceDetectOverlay.setResults(
                bundle.results[0],
                bundle.inputImageHeight,
                bundle.inputImageWidth,
            )
        }
        viewModel.faceSticker.observe(this) { sticker ->

            binding.cancelFaceStickerBtn.isVisible = sticker != null
            if (sticker == null) {
                binding.faceStickerBtn.imageTintList =
                    ColorStateList.valueOf(getColor(R.color.white))
            } else {
                binding.faceStickerBtn.imageTintList =
                    ColorStateList.valueOf(getColor(R.color.text_selected))
            }
            binding.faceDetectOverlay.setFaceSticker(sticker)
        }
        viewModel.cameraSelector.observe(this) { selector ->
            binding.faceDetectOverlay.isFrontCamera = selector == CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }

    private fun handleCameraState(state: CameraViewModel.CameraState) {
        binding.progressbar.visibility = when (state) {
            CameraViewModel.CameraState.Processing -> View.VISIBLE
            else -> View.GONE
        }

        when (state) {
            is CameraViewModel.CameraState.Error -> {
                Toast.makeText(
                    this,
                    state.exception.message ?: "Camera error occurred",
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {}
        }
    }

    private fun updateZoomUI(zoom: Float) {
        binding.apply {
            val selectedColor = getColor(R.color.text_selected)
            val defaultColor = getColor(R.color.white)
            zoom1x.setTextColor(if (zoom in 1f..1.99f) selectedColor else defaultColor)
            zoom2x.setTextColor(if (zoom in 2f..3.99f) selectedColor else defaultColor)
            zoom4x.setTextColor(if (zoom >= 4f) selectedColor else defaultColor)
        }
    }

    companion object {
        private const val TAG = "CameraActivity"
    }
}