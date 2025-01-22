package com.phamnhantucode.photoeditor.album

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.album.dialogs.DeleteOptionsDialog
import com.phamnhantucode.photoeditor.album.model.MyImage
import com.phamnhantucode.photoeditor.album.preview.PreviewImageActivity
import com.phamnhantucode.photoeditor.databinding.ActivityAlbumBinding
import kotlinx.coroutines.launch

class AlbumActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlbumBinding
    private lateinit var imagesLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var viewModel: AlbumViewModel
    private var imagesAdapter = ListImagesAdapter(
        onImageClick = ::onImageClicked,
        onImageLongClick = ::onImageLongClicked
    )

    override fun onRestart() {
        super.onRestart()
        viewModel.loadImages()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent.action == ACTION_IMAGE_PREVIEW) {
            val imageUri = intent.getStringExtra(EXTRA_IMAGE_URI)
            startActivity(Intent(this, PreviewImageActivity::class.java).apply {
                putExtra(PreviewImageActivity.EXTRA_IMAGE_URI, imageUri)
            })
        }
        binding = ActivityAlbumBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this)[AlbumViewModel::class.java]
        imagesLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            viewModel.loadImages()
        }
        setContentView(binding.root)
        setUpActionBar()
        setUpViewAction()
        setUpViewModelObserver()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setUpViewModelObserver() {
        viewModel.images.observe(this) {images ->
            imagesAdapter.images = images
            binding.tvSelectedLabel.text = resources.getString(R.string.selected, images.count { it.isSelected })
        }

        viewModel.isSelectedMode.observe(this) { isSelectedMode ->
            imagesAdapter.isSelectedMode = isSelectedMode
            binding.toolModeSelected.isVisible = isSelectedMode
        }
    }

    private fun setUpViewAction() {
        binding.apply {
            ivClose.setOnClickListener {
                (rvImages.adapter as ListImagesAdapter).apply {
                    isSelectedMode = false
                    viewModel.removeAllSelectedImages()
                }
            }
            ivDeleteBtn.setOnClickListener {
                DeleteOptionsDialog(
                    context = this@AlbumActivity,
                    message = resources.getString(R.string.delete_images, viewModel.images.value?.count { it.isSelected }),
                    onDelete = {
                        lifecycleScope.launch {
                            viewModel.deleteSelectedImages()
                        }
                    }
                ).show(supportFragmentManager, "DeleteOptionsDialog")
            }
        }
    }

    private fun setUpActionBar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setUpRecyclerView() {
        binding.apply {
            rvImages.apply {
                layoutManager = GridLayoutManager(this@AlbumActivity, 2)
                adapter = imagesAdapter
                progressBar.isVisible = false
            }
        }
    }

    private fun onImageLongClicked(image: MyImage) = binding.rvImages.apply {
        val imagesAdapter = adapter as ListImagesAdapter
        if (!imagesAdapter.isSelectedMode) {
            viewModel.toggleSelectMode()
        }
        viewModel.onSelectImage(image)
    }

    private fun onImageClicked(image: MyImage) = binding.rvImages.apply {
        val imagesAdapter = adapter as ListImagesAdapter
        if (imagesAdapter.isSelectedMode) {
            viewModel.onSelectImage(image)
        } else {
            val intent = Intent(
                this@AlbumActivity,
                PreviewImageActivity::class.java
            ).putExtra(PreviewImageActivity.EXTRA_IMAGE_URI, image.uri.toString())
            startActivity(intent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        setUpRecyclerView()
        super.onResume()
    }

    companion object {
        const val ACTION_IMAGE_PREVIEW: String = "com.phamnhantucode.photoeditor.album.ACTION_IMAGE_PREVIEW"
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
}