package com.phamnhantucode.photoeditor.editor

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.phamnhantucode.photoeditor.MainActivity
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.album.AlbumActivity
import com.phamnhantucode.photoeditor.album.preview.PreviewImageActivity
import com.phamnhantucode.photoeditor.core.BitmapUtil
import com.phamnhantucode.photoeditor.core.PhotoEditorGallery
import com.phamnhantucode.photoeditor.core.model.ui.ImageFilter
import com.phamnhantucode.photoeditor.databinding.ActivityEditorBinding
import com.phamnhantucode.photoeditor.databinding.DialogLoadingBinding
import com.phamnhantucode.photoeditor.databinding.LayoutTextInputOverlayBinding
import com.phamnhantucode.photoeditor.editor.adapter.FilterAdapter
import com.phamnhantucode.photoeditor.editor.core.Editor
import com.phamnhantucode.photoeditor.editor.core.OnEditorListener
import com.phamnhantucode.photoeditor.editor.core.ViewType
import com.phamnhantucode.photoeditor.editor.core.text.TextEditorMode
import com.phamnhantucode.photoeditor.editor.core.text.TextEditorState
import com.phamnhantucode.photoeditor.editor.crop.CropActivity
import com.phamnhantucode.photoeditor.editor.draw.DrawActivity
import com.phamnhantucode.photoeditor.editor.fragment.StickerBottomSheetDialogFragment
import com.phamnhantucode.photoeditor.extension.getContrastTextColor
import com.phamnhantucode.photoeditor.views.StyleableTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class EditorActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditorBinding
    private val viewModel: EditorViewModel by viewModels()
    private lateinit var editor: Editor
    private var currentSelectedTextView: StyleableTextView? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var adapter: FilterAdapter
    private lateinit var loadingDialog: AlertDialog

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
        setupLoadingDialog()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.mainToolBox.setPadding(
                systemBars.left,
                resources.getDimensionPixelSize(R.dimen.padding_large) + systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            binding.bottomSheetFilter.setPadding(
                systemBars.left,
                0,
                systemBars.right,
                resources.getDimensionPixelSize(R.dimen.padding_large) + systemBars.bottom
            )
            insets
        }
    }

    private fun setupLoadingDialog() {
        val layout = DialogLoadingBinding.inflate(layoutInflater)
        loadingDialog = AlertDialog.Builder(this)
            .setView(layout.root)
            .setCancelable(false)
            .create()
        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent);
    }

    private fun observeViewModel() {
        viewModel.originBitmap.observe(this) { bitmap ->
            Glide.with(this)
                .load(bitmap)
                .into(binding.editor.source)
            editor.setImageFilter(bitmap)
        }
        viewModel.moreOptionsVisible.observe(this) { flag ->
            binding.menuMore.isVisible = flag
        }
        viewModel.drawBitmap.observe(this) { bitmap ->
            Glide.with(this)
                .load(bitmap)
                .into(binding.editor.overlay)
        }
        viewModel.textEditorState.observe(this) { state ->
            binding.textInputOverlay.updateTextDemoIcon(state.mode)
            binding.textInputOverlay.updateTextEditor(state)
            updateColorPickerButton(state.color)
            if (viewModel.isEditingText && state.text != binding.textInputOverlay.etTextInput.text.toString()) {
                binding.textInputOverlay.etTextInput.setText(state.text)
            }
        }
        viewModel.selectedFilter.observe(this) { filter ->
            editor.setFilter(filter)
            binding.seekBarFilter.isVisible = filter.filterType.isAdjustable

        }
    }

    private fun updateColorPickerButton(color: Int) {
        binding.textInputOverlay.colorPickerBtn.backgroundTintList = ColorStateList.valueOf(color)
        binding.textInputOverlay.colorPickerBtn.imageTintList =
            ColorStateList.valueOf(color.getContrastTextColor())
    }

    private fun LayoutTextInputOverlayBinding.updateTextEditor(state: TextEditorState) {
        etTextInput.textSize = state.size
        etTextInput.typeface = state.typeface
        when (state.mode) {
            TextEditorMode.FILL -> {
                etTextInput.setTextColor(state.color.getContrastTextColor())
                etTextInput.background = ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.bg_text_fill,
                    null
                )
                etTextInput.backgroundTintList = ColorStateList.valueOf(state.color)
            }

            TextEditorMode.STROKE -> {
                etTextInput.setTextColor(state.color)
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
                etTextInput.backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            }
        }
    }

    private fun LayoutTextInputOverlayBinding.updateTextDemoIcon(mode: TextEditorMode) {
        when (mode) {
            TextEditorMode.FILL -> {
                tvDemonstrateText.setTextColor(Color.BLACK)
                tvDemonstrateText.background = ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.bg_text_fill,
                    null
                )
                tvDemonstrateText.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            }

            TextEditorMode.STROKE -> {
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
        setupEditor()
        setupFilterBottomSheet()
        binding.root.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
            if (binding.menuMore.isVisible) {
                viewModel.toggleMoreOptions()
            }
        }
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
            showTextEditingOverlay()
        }
        binding.saveBtn.setOnClickListener {
            loadingDialog.show()
            lifecycleScope.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    val bitmap = Bitmap.createBitmap(
                        binding.editor.width,
                        binding.editor.height,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    binding.editor.draw(canvas)
                    val imageUri = if (intent.action == ACTION_EDIT_SAVED_IMAGE) {
                        PhotoEditorGallery.saveImage(this@EditorActivity, BitmapUtil.removeTransparency(bitmap), viewModel.originUri!!)
                    } else {
                        PhotoEditorGallery.saveImage(this@EditorActivity, BitmapUtil.removeTransparency(bitmap))
                    }
                    loadingDialog.dismiss()
                    startActivity(
                        Intent(this@EditorActivity, AlbumActivity::class.java).apply {
                            action = AlbumActivity.ACTION_IMAGE_PREVIEW
                            putExtra(PreviewImageActivity.EXTRA_IMAGE_URI, imageUri.toString())
                            finish()
                        }
                    )
                }
            }
        }
        binding.stickerBtn.setOnClickListener {
            val fragment = StickerBottomSheetDialogFragment {
                editor.addSticker(it)
            }
            fragment.show(supportFragmentManager, fragment.tag)
        }
        binding.textInputOverlay.apply {
            textTypefaceWheel.setOnFontSelectedListener {  typeface ->
                if (typeface != null) {
                    viewModel.setTextOverlayTypeface(typeface)
                }
            }
            verticalSeekbar.apply {
                currentValue = Editor.TEXT_SIZE_DEFAULT
                shouldShowCircle = false
                setOnValueChanged {
                    viewModel.setTextOverlaySize(it)
                }
            }
            cancelBtn.setOnClickListener {
                if (viewModel.isEditingText && currentSelectedTextView != null) {
                    editor.editText(currentSelectedTextView!!, currentSelectedTextView!!.textViewState!!)
                }
                binding.textInputOverlay.root.isVisible = false
                viewModel.clearTextOverlayState()
                etTextInput.text.clear()
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(binding.textInputOverlay.etTextInput.windowToken, 0)
                showTools()
            }
            saveBtn.setOnClickListener {
                binding.textInputOverlay.root.isVisible = false
                viewModel.setTextOverlayText(etTextInput.text.toString())
                if (viewModel.isEditingText && currentSelectedTextView != null) {
                    editor.editText(currentSelectedTextView!!, viewModel.textEditorState.value!!)
                } else {
                    editor.addText(viewModel.textEditorState.value!!)
                }
                viewModel.clearTextOverlayState()
                etTextInput.text.clear()
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

    private fun setupFilterBottomSheet() {
        binding.apply {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetFilter)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            adapter = FilterAdapter(
                viewModel.originBitmap.value!!,
                onFilterClickListener = { filter ->
                    viewModel.setSelectedFilter(filter)
                    seekBarFilter.progress =
                        filter.filterType.normalizeToValueSeekbar(filter.currentValue)
                }
            )
            val filters = ImageFilter.mockFilters()
            viewModel.setSelectedFilter(filters.first())
            adapter.submitList(filters)
            rvFilter.adapter = adapter
            bottomSheetBehavior.addBottomSheetCallback(object :
                BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        showTools()
                    }
                    if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                        hideTools()
                    }
                }
            })
            filterBtn.setOnClickListener {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                hideTools()
            }
            seekBarFilter.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    adapter.selectedFilter?.let { filter ->
                        val currentValue = filter.filterType.normalizeToValueFilter(progress)
                        viewModel.setSelectedFilter(filter.copy(currentValue = currentValue))
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                }

            })
        }
    }

    private fun showTextEditingOverlay() {
        binding.textInputOverlay.root.isVisible = true
        binding.textInputOverlay.etTextInput.requestFocus()

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(
            binding.textInputOverlay.etTextInput,
            InputMethodManager.SHOW_IMPLICIT
        )
        hideTools()
    }

    private fun setupEditor() {
        editor = Editor.Builder(this, binding.editor)
            .build()
        editor.setOnEditorListener(object : OnEditorListener {
            override fun onEditTextChangeListener(
                rootView: StyleableTextView,
                textEditorState: TextEditorState,
            ) {
                currentSelectedTextView = rootView
                viewModel.setTextEditorState(textEditorState, true)
                showTextEditingOverlay()
            }

            override fun onAddViewListener(viewType: ViewType, numberOfAddedViews: Int) {
            }

            override fun onRemoveViewListener(viewType: ViewType, numberOfAddedViews: Int) {
            }

            override fun onStartViewChangeListener(viewType: ViewType) {
            }

            override fun onStopViewChangeListener(viewType: ViewType) {
            }

            override fun onTouchSourceImage(event: MotionEvent) {
            }
        })
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
        const val ACTION_EDIT_SAVED_IMAGE: String = "com.phamnhantucode.photoeditor.editor.ACTION_EDIT_SAVED_IMAGE"
    }
}