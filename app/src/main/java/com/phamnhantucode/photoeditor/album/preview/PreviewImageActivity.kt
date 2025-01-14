package com.phamnhantucode.photoeditor.album.preview

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.databinding.ActivityPreviewImageBinding

class PreviewImageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPreviewImageBinding
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPreviewImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val imageUri = Uri.parse(intent.getStringExtra(EXTRA_IMAGE_URI))
        binding.imageView.setImageURI(imageUri)
        binding.imageView.isRotateEnabled = false

        binding.imageView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                binding.toolbar.isVisible = false
            } else if (event.action == MotionEvent.ACTION_UP) {
                binding.toolbar.isVisible = true
            }
            v.onTouchEvent(event)

        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
}