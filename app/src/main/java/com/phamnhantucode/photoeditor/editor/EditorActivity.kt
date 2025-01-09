package com.phamnhantucode.photoeditor.editor

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.phamnhantucode.photoeditor.MainActivity
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.databinding.ActivityEditorBinding
import com.phamnhantucode.photoeditor.editor.crop.CropActivity

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
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
    }

    companion object {
        const val EXTRA_IMAGE_URI: String = "extra_image_uri"
    }
}