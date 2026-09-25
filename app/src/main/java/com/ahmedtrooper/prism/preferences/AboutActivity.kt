package com.ahmedtrooper.prism.preferences

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.ahmedtrooper.prism.BuildConfig
import com.ahmedtrooper.prism.R
import com.ahmedtrooper.prism.databinding.ActivityAboutBinding

/**
 * Prism About screen. Pure presentation: shows the app name, version, a short
 * description, and the credit list. No mpv internals are exposed here.
 */
class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        if (preferences.getBoolean("material_you_theming", false))
            DynamicColors.applyToActivityIfAvailable(this)

        enableEdgeToEdge()
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.elevation = 0f
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.aboutVersion.text = getString(
            R.string.about_prism_version_format,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE,
            BuildConfig.BUILD_TYPE
        )
    }
}
