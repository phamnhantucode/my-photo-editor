package com.phamnhantucode.photoeditor.editor

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.phamnhantucode.photoeditor.MainActivity
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.databinding.ActivityEditorBinding
import com.phamnhantucode.photoeditor.databinding.LayoutTextInputOverlayBinding
import com.phamnhantucode.photoeditor.editor.crop.CropActivity
import com.phamnhantucode.photoeditor.editor.draw.DrawActivity
import com.phamnhantucode.photoeditor.extension.getContrastTextColor


class EditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditorBinding
    private val viewModel: EditorViewModel by viewModels()

    private val cropActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra(CropActivity.RESULT_CROPPED_URI)?.let { uri ->
                viewModel.setOriginBitmapBy(Uri.parse(uri))
            }
        }
    }

    private val drawActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringExtra(DrawActivity.DRAW_OVERLAY_URI)?.let { uri ->
                viewModel.setDrawBitmapBy(Uri.parse(uri))
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        intent.getStringExtra(EXTRA_IMAGE_URI)?.let {
            viewModel.setOriginBitmapBy(Uri.parse(it))
        } ?: run {
            viewModel.setOriginBitmapBy(
                BitmapFactory.decodeResource(
                    resources,
                    R.drawable.sample_img
                )
            )
        }

        setupUI()
        observeViewModel()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.mainToolBox.setPadding(
                systemBars.left,
                resources.getDimensionPixelSize(R.dimen.padding_large) + systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }
    }

    private fun observeViewModel() {
        viewModel.originBitmap.observe(this) { bitmap ->
            Glide.with(this)
                .load(bitmap)
                .into(binding.image)
        }
        viewModel.moreOptionsVisible.observe(this) { flag ->
            binding.menuMore.isVisible = flag
        }
        viewModel.drawBitmap.observe(this) { bitmap ->
            Glide.with(this)
                .load(bitmap)
                .into(binding.drawOverlay)
        }
        viewModel.textEditorState.observe(this) { state ->
            binding.textInputOverlay.updateTextDemoIcon(state.mode)
            binding.textInputOverlay.updateTextEditor(state)
            updateColorPickerButton(state.color)
        }
    }

    private fun updateColorPickerButton(color: Int){
        binding.textInputOverlay.colorPickerBtn.backgroundTintList = ColorStateList.valueOf(color)
        binding.textInputOverlay.colorPickerBtn.imageTintList = ColorStateList.valueOf(color.getContrastTextColor())
    }

    private fun LayoutTextInputOverlayBinding.updateTextEditor(state: EditorViewModel.TextEditorState) {
        etTextInput.textSize = state?.size ?: 0f
        when (state.mode) {
            EditorViewModel.TextEditorMode.FILL -> {
                etTextInput.setTextColor(state.color.getContrastTextColor())
                etTextInput.background = ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.bg_text_fill,
                    null
                )
                etTextInput.backgroundTintList = ColorStateList.valueOf(state.color)
            }

            EditorViewModel.TextEditorMode.STROKE -> {
                etTextInput.setTextColor(Color.WHITE)
                etTextInput.backgroundTintList = null
                (ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.bg_text_demo_stroke,
                    null
                ) as? GradientDrawable)?.apply {
                    mutate()
                    setStroke(
                        resources.getDimensionPixelSize(R.dimen.stroke_width_2dp),
                        state.color
                    )
                    etTextInput.background = this
                }
            }
            else -> {
                etTextInput.setTextColor(state.color)
                etTextInput.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    private fun LayoutTextInputOverlayBinding.updateTextDemoIcon(mode: EditorViewModel.TextEditorMode) {
        when (mode) {
            EditorViewModel.TextEditorMode.FILL -> {
                tvDemonstrateText.setTextColor(Color.BLACK)
                tvDemonstrateText.background = ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.bg_text_fill,
                    null
                )
                tvDemonstrateText.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            }

            EditorViewModel.TextEditorMode.STROKE -> {
                tvDemonstrateText.setTextColor(Color.WHITE)
                tvDemonstrateText.background = ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.bg_text_demo_stroke,
                    null
                )
            }

            else -> {
                tvDemonstrateText.setTextColor(Color.WHITE)
                tvDemonstrateText.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    private fun setupUI() {
        binding.cropBtn.setOnClickListener {
            viewModel.originUri?.let { uri ->
                cropActivityLauncher.launch(
                    Intent(this, CropActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                        putExtra(CropActivity.EXTRA_IMAGE_URI, uri.toString())
                    }
                )
            }
        }
        binding.backBtn.setOnClickListener {
            finish()
            startActivity(Intent(this, MainActivity::class.java))
        }
        binding.moreBtn.setOnClickListener {
            viewModel.toggleMoreOptions()
        }
        binding.drawBtn.setOnClickListener {
            drawActivityLauncher.launch(
                Intent(this, DrawActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    putExtra(DrawActivity.EXTRA_IMAGE_URI, viewModel.originUri.toString())
                    if (viewModel.drawBitmap.value != null) {
                        putExtra(DrawActivity.DRAW_OVERLAY_URI, viewModel.drawUri.toString())
                    }
                }
            )
        }
        binding.textBtn.setOnClickListener {
            binding.textInputOverlay.root.isVisible = true
            binding.textInputOverlay.etTextInput.requestFocus()

            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(
                binding.textInputOverlay.etTextInput,
                InputMethodManager.SHOW_IMPLICIT
            )
            hideTools()
        }
        binding.textInputOverlay.apply {
            verticalSeekbar.apply {
                shouldShowCircle = false
                setOnValueChanged {
                    viewModel.setTextOverlaySize(it)
                }
            }
            saveBtn.setOnClickListener {
                binding.textInputOverlay.root.isVisible = false

                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.textInputOverlay.etTextInput.windowToken, 0)
                showTools()
            }
            tvDemonstrateText.setOnClickListener {
                viewModel.setToNextTextMode()
            }
            colorPickerBtn.setOnClickListener {
                ColorPickerDialog.Builder(this@EditorActivity)
                    .setDefaultColor(viewModel.textEditorState.value?.color ?: Color.BLACK)
                    .setColorListener { color, _ ->
                        viewModel.setTextOverlayColor(color)
                    }
                    .show()
            }
        }

    }

    private fun showTools() {
        binding.apply {
            mainToolBox.isVisible = true
        }
    }

    private fun hideTools() {
        binding.apply {
            mainToolBox.isVisible = false
        }
    }

    companion object {
        const val EXTRA_IMAGE_URI: String = "extra_image_uri"
    }
}