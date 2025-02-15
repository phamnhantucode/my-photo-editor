package com.phamnhantucode.photoeditor.album.preview

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.palette.graphics.Palette
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.core.helper.PhotoEditorGallery
import com.phamnhantucode.photoeditor.databinding.ActivityPreviewImageBinding
import com.phamnhantucode.photoeditor.editor.EditorActivity
import com.phamnhantucode.photoeditor.extension.getContrastTextColor

class PreviewImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPreviewImageBinding

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPreviewImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val imageUri = Uri.parse(intent.getStringExtra(EXTRA_IMAGE_URI))
        binding.apply {
            imageView.apply {
                setImageURI(imageUri)
                isRotateEnabled = false
                setOnTouchListener { v, event ->
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        hideTools()
                    } else if (event.action == MotionEvent.ACTION_UP) {
                        showTools()
                    }
                    v.onTouchEvent(event)
                }
            }
            toolbar.apply {
                setDominantColor()
                setNavigationOnClickListener {
                    finish()
                }
                shareBtn.setOnClickListener {
                    PhotoEditorGallery.shareImage(this@PreviewImageActivity, imageUri)
                }
                editBtn.setOnClickListener {
                    startActivity(
                        Intent(
                            this@PreviewImageActivity,
                            EditorActivity::class.java
                        ).apply {
                            action = EditorActivity.ACTION_EDIT_SAVED_IMAGE
                            putExtra(EditorActivity.EXTRA_IMAGE_URI, imageUri.toString())
                        })
                }
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }
    }

    private fun hideTools() {
        binding.toolbar.animate()
            .alpha(0f)
            .translationY(-binding.toolbar.height.toFloat())
            .setDuration(300).start()
    }

    private fun showTools() {
        binding.toolbar.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300).start()
    }

    private fun setDominantColor() {
        Palette.from(binding.imageView.drawable.toBitmap()).generate { palette ->
            val color = palette?.lightMutedSwatch?.rgb ?: Color.BLACK
            val tintColor = color.getContrastTextColor()
            binding.apply {
                root.setBackgroundColor(color)
                toolbar.setNavigationIconTint(tintColor)
                shareBtn.imageTintList = ColorStateList.valueOf(tintColor)
                editBtn.imageTintList = ColorStateList.valueOf(tintColor)
            }
        }
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
}
