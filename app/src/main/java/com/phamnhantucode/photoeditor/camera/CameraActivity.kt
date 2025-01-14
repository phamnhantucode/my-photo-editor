package com.phamnhantucode.photoeditor.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.databinding.ActivityCameraBinding
import com.phamnhantucode.photoeditor.editor.EditorActivity
import com.phamnhantucode.photoeditor.extension.hideSystemBars

class CameraActivity() : AppCompatActivity() {

    init {
        System.loadLibrary("NativeImageProcessor");
    }

    private lateinit var binding: ActivityCameraBinding
    private val viewModel: CameraViewModel by viewModels()

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

    @SuppressLint("ClickableViewAccessibility")
    private fun setupUI() {
        binding.apply {
            captureBtn.setOnClickListener { viewModel.takePicture() }
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
            filterView.setOnFilterSelectedListener { filter ->
                viewModel.applyFilter(filter)
            }

            cameraView.setOnTouchListener { _, event ->
                scaleGestureDetector.onTouchEvent(event)
                true
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
        viewModel.bitmapPreview.observe(this) { bitmap ->
            binding.filterView.setOriginalPhoto(bitmap)
        }
        viewModel.isShowingImageFilters.observe(this) { isShowingFilters ->
            binding.filterView.isVisible = isShowingFilters
        }
        viewModel.photoUri.observe(this) { uri ->
            startActivity(Intent(this, EditorActivity::class.java).apply {
                putExtra(EditorActivity.EXTRA_IMAGE_URI, uri.toString())
                finish()
            })
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