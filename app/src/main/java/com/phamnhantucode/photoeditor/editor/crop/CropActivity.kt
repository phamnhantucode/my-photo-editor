package com.phamnhantucode.photoeditor.editor.crop

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.databinding.ActivityCropBinding
import com.yalantis.ucrop.callback.BitmapCropCallback
import com.yalantis.ucrop.view.CropImageView
import com.yalantis.ucrop.view.TransformImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Exception

class CropActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCropBinding
    private val viewModel: CropViewModel by viewModels()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var toolboxTimeoutJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCropBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent.getStringExtra(EXTRA_IMAGE_URI)?.let {
            viewModel.setImageUri(Uri.parse(it))
        }

        setupUI()
        observeViewModel()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupUI() {
        binding.apply {
            ucropView.apply {
                cropImageView.setTransformImageListener(object : TransformImageView.TransformImageListener {
                    override fun onLoadComplete() {
                        binding.rulerScale.setValue(binding.ucropView.cropImageView.currentScale)
                        binding.rulerRotation.setValue(binding.ucropView.cropImageView.currentAngle)
                        binding.rulerRotation.setStep(1f)
                    }

                    override fun onLoadFailure(e: Exception) {
                    }

                    override fun onRotate(currentAngle: Float) {
                    }

                    override fun onScale(currentScale: Float) {
                    }
                })
            }

            cropBtn.setOnClickListener {
                cropImage()
            }
            backBtn.setOnClickListener {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }

            ratioBtn.setOnClickListener {
                resetToolboxTimeout()
                viewModel.setTool(CropViewModel.SelectedTool.RATIO)
            }

            rotateBtn.setOnClickListener {
                resetToolboxTimeout()
                viewModel.setTool(CropViewModel.SelectedTool.ROTATE)
            }

            scaleBtn.setOnClickListener {
                resetToolboxTimeout()
                viewModel.setTool(CropViewModel.SelectedTool.SCALE)
            }

            oneByOne.setOnClickListener {
                resetToolboxTimeout()
                viewModel.setRatio(CropViewModel.SelectedRatio.RATIO_1_1)
            }

            fourByThree.setOnClickListener {
                resetToolboxTimeout()
                viewModel.setRatio(CropViewModel.SelectedRatio.RATIO_4_3)
            }

            threeByTwo.setOnClickListener {
                resetToolboxTimeout()
                viewModel.setRatio(CropViewModel.SelectedRatio.RATIO_3_2)
            }

            sixTeenByNine.setOnClickListener {
                resetToolboxTimeout()
                viewModel.setRatio(CropViewModel.SelectedRatio.RATIO_16_9)
            }

            origin.setOnClickListener {
                resetToolboxTimeout()
                viewModel.setRatio(CropViewModel.SelectedRatio.ORIGIN)
            }

            cancelRotateBtn.setOnClickListener {
                resetToolboxTimeout()
                viewModel.setRotationAngle(0f)
                binding.ucropView.cropImageView.setImageToWrapCropBounds()
            }

            rotateLeftBtn.setOnClickListener {
                resetToolboxTimeout()
                viewModel.setRotationAngle(viewModel.rotationAngle.value?.minus(90f) ?: 0f)
                binding.ucropView.cropImageView.setImageToWrapCropBounds()
            }

            rulerRotation.setOnValueChanged {value ->
                resetToolboxTimeout()
                viewModel.setRotationAngle(value)
                binding.ucropView.cropImageView.setImageToWrapCropBounds()
            }

            rulerScale.setOnValueChanged {value ->
                resetToolboxTimeout()
                viewModel.setScale(value)
                if (value > binding.ucropView.cropImageView.currentScale) {
                    binding.ucropView.cropImageView.zoomInImage(value - binding.ucropView.cropImageView.currentScale)
                } else {
                    binding.ucropView.cropImageView.zoomOutImage(value - binding.ucropView.cropImageView.currentScale)
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.imageUri.observe(this) { uri ->
            binding.ucropView.cropImageView.setImageUri(uri, viewModel.tempUri)
        }
        viewModel.selectedTool.observe(this) { tool ->
            binding.apply {
                when (tool) {
                    CropViewModel.SelectedTool.RATIO -> {
                        ratioToolBox.isVisible = true
                        scaleToolBox.isVisible = false
                        rotateToolBox.isVisible = false
                        ratioBtn.imageTintList = getColorStateList(R.color.text_selected)
                        rotateBtn.imageTintList = getColorStateList(R.color.white)
                        scaleBtn.imageTintList = getColorStateList(R.color.white)
                    }

                    CropViewModel.SelectedTool.ROTATE -> {
                        rotateToolBox.isVisible = true
                        ratioToolBox.isVisible = false
                        scaleToolBox.isVisible = false
                        rotateBtn.imageTintList = getColorStateList(R.color.text_selected)
                        ratioBtn.imageTintList = getColorStateList(R.color.white)
                        scaleBtn.imageTintList = getColorStateList(R.color.white)
                    }

                    CropViewModel.SelectedTool.SCALE -> {
                        scaleToolBox.isVisible = true
                        ratioToolBox.isVisible = false
                        rotateToolBox.isVisible = false
                        scaleBtn.imageTintList = getColorStateList(R.color.text_selected)
                        rotateBtn.imageTintList = getColorStateList(R.color.white)
                        ratioBtn.imageTintList = getColorStateList(R.color.white)
                    }

                    CropViewModel.SelectedTool.NONE -> {
                        ratioToolBox.isVisible = false
                        scaleToolBox.isVisible = false
                        rotateToolBox.isVisible = false
                        scaleBtn.imageTintList = getColorStateList(R.color.white)
                        rotateBtn.imageTintList = getColorStateList(R.color.white)
                        ratioBtn.imageTintList = getColorStateList(R.color.white)
                    }

                    else -> {
                        ratioToolBox.isVisible = false
                        scaleToolBox.isVisible = false
                        rotateToolBox.isVisible = false
                    }
                }
            }
            viewModel.rotationAngle.observe(this) { angle ->
                binding.ucropView.cropImageView.postRotate(angle - binding.ucropView.cropImageView.currentAngle)
                binding.rulerRotation.setValue(angle)
            }
        }
        viewModel.selectedRatio.observe(this) {ratio ->
            binding.ucropView.cropImageView.setTargetAspectRatio(
                if (ratio == CropViewModel.SelectedRatio.ORIGIN) CropImageView.SOURCE_IMAGE_ASPECT_RATIO
                else ratio.getRatio()
            )
            binding.apply {
                when (ratio) {
                    CropViewModel.SelectedRatio.RATIO_1_1 -> {
                        oneByOne.setTextColor(getColor(R.color.text_selected))
                        fourByThree.setTextColor(getColor(R.color.white))
                        threeByTwo.setTextColor(getColor(R.color.white))
                        sixTeenByNine.setTextColor(getColor(R.color.white))
                        origin.setTextColor(getColor(R.color.white))
                    }
                    CropViewModel.SelectedRatio.RATIO_3_2 -> {
                        oneByOne.setTextColor(getColor(R.color.white))
                        fourByThree.setTextColor(getColor(R.color.white))
                        threeByTwo.setTextColor(getColor(R.color.text_selected))
                        sixTeenByNine.setTextColor(getColor(R.color.white))
                        origin.setTextColor(getColor(R.color.white))
                    }
                    CropViewModel.SelectedRatio.RATIO_4_3 -> {
                        oneByOne.setTextColor(getColor(R.color.white))
                        fourByThree.setTextColor(getColor(R.color.text_selected))
                        threeByTwo.setTextColor(getColor(R.color.white))
                        sixTeenByNine.setTextColor(getColor(R.color.white))
                        origin.setTextColor(getColor(R.color.white))
                    }
                    CropViewModel.SelectedRatio.RATIO_16_9 -> {
                        oneByOne.setTextColor(getColor(R.color.white))
                        fourByThree.setTextColor(getColor(R.color.white))
                        threeByTwo.setTextColor(getColor(R.color.white))
                        sixTeenByNine.setTextColor(getColor(R.color.text_selected))
                        origin.setTextColor(getColor(R.color.white))
                    }
                    CropViewModel.SelectedRatio.ORIGIN -> {
                        oneByOne.setTextColor(getColor(R.color.white))
                        fourByThree.setTextColor(getColor(R.color.white))
                        threeByTwo.setTextColor(getColor(R.color.white))
                        sixTeenByNine.setTextColor(getColor(R.color.white))
                        origin.setTextColor(getColor(R.color.text_selected))
                    }
                }
            }
        }
    }

    private fun cropImage() {
        binding.ucropView.cropImageView.cropAndSaveImage(
            Bitmap.CompressFormat.JPEG,
            100,
            object : BitmapCropCallback {
                override fun onBitmapCropped(
                    resultUri: Uri,
                    offsetX: Int,
                    offsetY: Int,
                    imageWidth: Int,
                    imageHeight: Int,
                ) {
                    setResult(Activity.RESULT_OK, Intent().apply {
                        putExtra(RESULT_CROPPED_URI, resultUri.toString())
                    })
                    finish()
                }

                override fun onCropFailure(t: Throwable) {
                    t.printStackTrace()
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
            }
        )
    }

    private fun resetToolboxTimeout() {
        toolboxTimeoutJob?.cancel()
        toolboxTimeoutJob = coroutineScope.launch {
            delay(10000)
            viewModel.setTool(CropViewModel.SelectedTool.NONE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        toolboxTimeoutJob?.cancel()
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        const val RESULT_CROPPED_URI = "result_cropped_uri"
    }
}