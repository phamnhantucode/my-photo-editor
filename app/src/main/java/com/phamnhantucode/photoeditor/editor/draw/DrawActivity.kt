package com.phamnhantucode.photoeditor.editor.draw

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toRect
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.github.dhaval2404.colorpicker.ColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.databinding.ActivityDrawBinding
import com.phamnhantucode.photoeditor.extension.doIfAboveApi
import com.phamnhantucode.photoeditor.extension.dp
import com.phamnhantucode.photoeditor.extension.getContrastTextColor
import com.phamnhantucode.photoeditor.views.DrawOverlay

class DrawActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDrawBinding
    private val viewModel: DrawViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDrawBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val imageUri = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (imageUri != null) {
            viewModel.setDrawBitmapBy(imageUri.toUri())
        }
        val drawOverlayUri = intent.getStringExtra(DRAW_OVERLAY_URI)
        if (drawOverlayUri != null) {
            val bitmap = BitmapFactory.decodeFile(Uri.parse(drawOverlayUri).path)
            binding.drawView.bitmapBottomLayer = bitmap
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
            ivImage.viewTreeObserver.addOnGlobalLayoutListener {
                val matrix: Matrix = ivImage.getImageMatrix()
                val rect = RectF(
                    0f, 0f,
                    ivImage.drawable.intrinsicWidth.toFloat(),
                    ivImage.drawable.intrinsicHeight.toFloat()
                )
                matrix.mapRect(rect)
                drawView.drawableArea = rect.toRect()
            }
            ivImage.setImageResource(R.drawable.sample_img)


            //drawView
            drawView.setOnDrawStateChangeListener { isDrawing ->
                toolbox.isVisible = !isDrawing
                verticalSeekbar.isVisible = !isDrawing
            }

            //verticalSeekBar
            verticalSeekbar.setOnValueChanged {
                drawView.paintStrokeWidth = it.dp
            }

            colorPickerBtn.setOnClickListener {
                ColorPickerDialog
                    .Builder(this@DrawActivity)
                    .setTitle("Pick Theme")
                    .setColorShape(ColorShape.SQAURE)
                    .setDefaultColor(viewModel.color.value!!)
                    .setColorListener { color, colorHex ->
                        viewModel.setPaintColor(color)
                    }
                    .show()
            }

            nextBtn.setOnClickListener {
                val bitmap = drawView.exportDrawing()
                val file = createTempFile()
                file.outputStream().use { os ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                }
                setResult(RESULT_OK, Intent().putExtra(DRAW_OVERLAY_URI, file.toUri().toString()))
                finish()
            }

            //toolbox
            ivPen.setOnClickListener {
                viewModel.setDrawMode(DrawOverlay.PaintType.PEN)
            }
            ivEraser.setOnClickListener {
                viewModel.setDrawMode(DrawOverlay.PaintType.ERASER)
            }
            ivPenNeon.setOnClickListener {
                viewModel.setDrawMode(DrawOverlay.PaintType.NEON)
            }
            ivBrush.setOnClickListener {
                viewModel.setDrawMode(DrawOverlay.PaintType.BRUSH)
            }
        }
    }

    private fun updateToolboxState() {
        binding.apply {

            ivPen.backgroundTintList = getColorStateList(android.R.color.transparent)
            ivEraser.backgroundTintList = getColorStateList(android.R.color.transparent)
            ivPenNeon.backgroundTintList = getColorStateList(android.R.color.transparent)
            ivBrush.backgroundTintList = getColorStateList(android.R.color.transparent)

            when (viewModel.drawMode.value) {
                DrawOverlay.PaintType.PEN -> {
                    ivPen.backgroundTintList = getColorStateList(R.color.text_selected)
                }

                DrawOverlay.PaintType.ERASER -> {
                    ivEraser.backgroundTintList = getColorStateList(R.color.text_selected)
                }

                DrawOverlay.PaintType.NEON -> {
                    ivPenNeon.backgroundTintList = getColorStateList(R.color.text_selected)
                }

                DrawOverlay.PaintType.BRUSH -> {
                    ivBrush.backgroundTintList = getColorStateList(R.color.text_selected)
                }

                else -> {

                }
            }

        }
    }

    private fun observeViewModel() {
        viewModel.drawMode.observe(this) {
            updateToolboxState()
            binding.drawView.paintType = it
        }
        viewModel.color.observe(this) {
            binding.drawView.paintColor = it
            binding.colorPickerBtn.backgroundTintList = ColorStateList.valueOf(it)
            binding.colorPickerBtn.imageTintList = ColorStateList.valueOf(it.getContrastTextColor())
            doIfAboveApi(Build.VERSION_CODES.P) {
                binding.colorPickerBtn.outlineSpotShadowColor = it
            }
        }
        viewModel.drawBitmap.observe(this) {
            binding.ivImage.setImageBitmap(it)
        }
    }

    companion object {
        const val DRAW_OVERLAY_URI = "drawOverlay"
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
}