package com.github.milanmaji.sample

import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.milanmaji.sample.databinding.ActivityMainBinding
import com.github.milanmaji.triangleseekbar.TriangleSeekbar
import java.util.Random

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        init()
    }

    private fun init() {
        with(binding) {
            triangleSeekbar.setProgressListener(
                object : TriangleSeekbar.ProgressListener {
                    override fun onProgressChange(progress: Float) {
                        triangleSeekbarStaircase.setProgress(progress)
                        tvCurrentProgress.text = getString(R.string.current_progress_is, progress)
                    }

                }
            )

            triangleSeekbarStaircase.setProgressListener(
                object : TriangleSeekbar.ProgressListener {
                    override fun onProgressChange(progress: Float) {
                        triangleSeekbar.setProgress(progress)
                    }

                }
            )



            btnMakeSeekBar.setOnClickListener {
                val rnd = Random()
                val color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))

                triangleSeekbar.setSeekBarColor(color)
            }


            btnMakeProgress.setOnClickListener {
                val rnd = Random()
                val color = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))

                triangleSeekbar.seekbarLoadingColor = color
            }

            btnShowProgress.setOnClickListener {
                triangleSeekbar.isProgressVisible = !triangleSeekbar.isProgressVisible
                if (triangleSeekbar.isProgressVisible) {
                    btnShowProgress.text = getString(R.string.hide_progress_text_on_it)
                } else {
                    btnShowProgress.text = getString(R.string.show_progress_text_on_it)
                }
            }
        }
    }
}