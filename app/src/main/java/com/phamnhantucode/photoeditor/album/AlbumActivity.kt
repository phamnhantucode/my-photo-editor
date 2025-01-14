package com.phamnhantucode.photoeditor.album

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.phamnhantucode.photoeditor.R
import com.phamnhantucode.photoeditor.album.preview.PreviewImageActivity
import com.phamnhantucode.photoeditor.databinding.ActivityAlbumBinding

class AlbumActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAlbumBinding
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
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    companion object {
        const val ACTION_IMAGE_PREVIEW = "action_image_preview"
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
}