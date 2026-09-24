package com.ahmedtrooper.prism.preferences

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.ahmedtrooper.prism.BuildConfig
import com.ahmedtrooper.prism.PrismLib
import com.ahmedtrooper.prism.PrismLib.MpvLogLevel
import com.ahmedtrooper.prism.R
import com.ahmedtrooper.prism.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity(), PrismLib.LogObserver {
    private lateinit var binding: ActivityAboutBinding
    private var logs = ""
    private var mpvDestroyed = true

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

        logs = "Prism ${BuildConfig.VERSION_NAME} / ${BuildConfig.VERSION_CODE} (${BuildConfig.BUILD_TYPE})\n"

        // create mpv context to capture version info from log
        PrismLib.create(this)
        mpvDestroyed = false
        PrismLib.addLogObserver(this)
        PrismLib.init()
    }

    private fun updateLog() {
        runOnUiThread {
            binding.logs.text = logs
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!mpvDestroyed) {
            PrismLib.destroy()
            mpvDestroyed = true
        }
    }

    override fun logMessage(prefix: String, level: Int, text: String) {
        if (prefix != "cplayer")
            return
        if (level == MpvLogLevel.MPV_LOG_LEVEL_V)
            logs += text

        if (text.startsWith("List of enabled features:", true)) {
            // stop receiving log messages and populate text field
            PrismLib.removeLogObserver(this)
            updateLog()
        }
    }
}
