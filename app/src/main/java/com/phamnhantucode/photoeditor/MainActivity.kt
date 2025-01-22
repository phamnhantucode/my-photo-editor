package com.phamnhantucode.photoeditor

import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewOutlineProvider
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.phamnhantucode.photoeditor.album.AlbumActivity
import com.phamnhantucode.photoeditor.camera.CameraActivity
import com.phamnhantucode.photoeditor.databinding.ActivityMainBinding
import com.phamnhantucode.photoeditor.editor.EditorActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private var accelerator: Sensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handleIntent()
        setupClickListeners()
        setupBlurView()
        setupSloganAnimation()
        setupEdgeInsets()
        setupSensorListener()
    }

    private fun setupSensorListener() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerator = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    private fun setupClickListeners() {
        binding.btnAdd.setOnClickListener {
            startActivity(Intent(this, CameraActivity::class.java))
        }

        binding.btnAlbum.setOnClickListener {
            startActivity(Intent(this, AlbumActivity::class.java))
        }
    }

    private fun setupBlurView() {
        binding.blurView.apply {
            setupWith(binding.root)
                .setFrameClearDrawable(getDrawable(R.drawable.bg_main_2))
                .setBlurRadius(8f)

            outlineProvider = ViewOutlineProvider.BACKGROUND
            clipToOutline = true
        }
    }

    private fun setupSloganAnimation() {
        lifecycleScope.launch {
            while (true) {
                binding.sloganTv.animateText(slogans.random())
                kotlinx.coroutines.delay(TEXT_ANIMATION_DURATION)
            }
        }
    }

    private fun handleIntent() {
        if (intent.action == Intent.ACTION_SEND) {
            val uri = intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri ?: intent.data
            if (uri != null) {
                startActivity(Intent(this, EditorActivity::class.java).apply {
                    putExtra(EditorActivity.EXTRA_IMAGE_URI, uri.toString())
                })
            } else {
                finish()
            }
        }
    }

    private fun setupEdgeInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.content.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
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

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerator, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        val x = event?.values?.get(0) ?: 0f
        val y = event?.values?.get(1) ?: 0f
        val z = event?.values?.get(2) ?: 0f

        binding.ivBubble.updateVelocity(x, y)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.e("Sensor", "Accuracy: $accuracy")
    }
}
