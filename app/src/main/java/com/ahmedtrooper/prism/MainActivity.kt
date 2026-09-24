package com.ahmedtrooper.prism

import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Switch from window splash theme to the app's action theme
        setTheme(R.style.FilePickerTheme)
        super.onCreate(savedInstanceState)

        supportActionBar?.setTitle(R.string.app_name)

        val splashOverlay = findViewById<View>(R.id.splash_overlay)
        val splashLogo = findViewById<View>(R.id.splash_logo)

        if (savedInstanceState == null) {
            with (supportFragmentManager.beginTransaction()) {
                setReorderingAllowed(true)
                add(R.id.fragment_container_view, MainScreenFragment())
                commit()
            }

            // Animate splash elements on initial launch
            splashLogo?.apply {
                scaleX = 0.85f
                scaleY = 0.85f
                alpha = 0f
                animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(500)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }

            // Smoothly fade out the splash overlay into the media library
            splashOverlay?.apply {
                postDelayed({
                    if (!isFinishing && !isDestroyed) {
                        animate()
                            .alpha(0f)
                            .setDuration(400)
                            .withEndAction { visibility = View.GONE }
                            .start()
                    }
                }, 1000)

                // Quick tap to dismiss immediately
                setOnClickListener {
                    animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction { visibility = View.GONE }
                        .start()
                }
            }
        } else {
            // Configuration change / rotation: skip splash immediately
            splashOverlay?.visibility = View.GONE
        }
    }
}
