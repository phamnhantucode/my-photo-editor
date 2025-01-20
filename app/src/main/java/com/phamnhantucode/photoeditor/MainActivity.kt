package com.phamnhantucode.photoeditor

import com.phamnhantucode.photoeditor.camera.CameraActivity
import android.content.Intent
import android.os.Bundle
import android.view.ViewOutlineProvider
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.phamnhantucode.photoeditor.album.AlbumActivity
import com.phamnhantucode.photoeditor.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnAdd.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }
        binding.btnAlbum.setOnClickListener {
            startActivity(Intent(this, AlbumActivity::class.java))
        }
        binding.blurView.apply {
            setupWith(binding.root)
                .setFrameClearDrawable(getDrawable(R.drawable.bg_main_2))
                .setBlurRadius(8f)

            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
        }

        lifecycleScope.launch {
            while (true) {
                binding.sloganTv.animateText(slogans.random())
                kotlinx.coroutines.delay(TEXT_ANIMATION_DURATION)
            }
        }
        setupEdgeInsets()
    }

    private fun setupEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.content.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    companion object {
        const val TEXT_ANIMATION_DURATION = 10000L
        val slogans = arrayOf(
            "Where everyday moments transform into extraordinary memories - your creative journey begins here",
            "Turn ordinary photos into visual stories. Edit, enhance, and express yourself with limitless creativity",
            "Discover the art of photo perfection. Every filter, every edit, every moment crafted by you",
            "Your memories deserve more than just storage - they deserve to be masterpieces. Start creating magic"
        )
    }
}