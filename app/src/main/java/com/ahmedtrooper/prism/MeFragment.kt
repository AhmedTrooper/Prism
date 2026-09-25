package com.ahmedtrooper.prism

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.ahmedtrooper.prism.databinding.FragmentMeBinding
import com.ahmedtrooper.prism.preferences.AboutActivity
import com.ahmedtrooper.prism.preferences.PreferenceActivity

class MeFragment : Fragment(R.layout.fragment_me) {

    private var _binding: FragmentMeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.handleInsetsAsPadding(binding.root)

        setupActions()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupActions() {
        // Quick Actions
        binding.cardNetworkStream.setOnClickListener {
            val helper = Utils.OpenUrlDialog(requireContext())
            with(helper) {
                builder.setPositiveButton(R.string.dialog_ok) { _, _ -> playFile(helper.text) }
                builder.setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
                create().show()
            }
        }

        binding.cardOpenFile.setOnClickListener {
            val i = Intent(requireContext(), FilePickerActivity::class.java)
            i.putExtra("skip", FilePickerActivity.FILE_PICKER)
            startActivity(i)
        }

        binding.cardRescan.setOnClickListener {
            Toast.makeText(requireContext(), "Rescanning media library...", Toast.LENGTH_SHORT).show()
            // Notify MainActivity or trigger library rescan
            (activity as? MainActivity)?.rescanLibrary()
        }

        // Theme Dialog
        binding.rowAppTheme.setOnClickListener {
            val themes = arrayOf("System Default (Adaptive)", "Light Theme", "Dark Theme")
            val currentMode = AppCompatDelegate.getDefaultNightMode()
            val selected = when (currentMode) {
                AppCompatDelegate.MODE_NIGHT_NO -> 1
                AppCompatDelegate.MODE_NIGHT_YES -> 2
                else -> 0
            }

            AlertDialog.Builder(requireContext())
                .setTitle("Select App Theme")
                .setSingleChoiceItems(themes, selected) { dialog, which ->
                    val mode = when (which) {
                        1 -> AppCompatDelegate.MODE_NIGHT_NO
                        2 -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                    AppCompatDelegate.setDefaultNightMode(mode)
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .edit().putInt("theme_mode", mode).apply()
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.dialog_cancel, null)
                .show()
        }

        // Settings
        binding.rowSettings.setOnClickListener {
            startActivity(Intent(requireContext(), PreferenceActivity::class.java))
        }

        // Legal & Licenses
        binding.rowLegal.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Legal & Licenses")
                .setMessage(
                    "Prism Android Video Player\n\n" +
                    "• Core Engine: Powered by libmpv and FFmpeg (distributed under GPLv3)\n" +
                    "• Application UI: Apache License 2.0\n" +
                    "• Upstream Android Components: MIT License\n\n" +
                    "All original rights and upstream licenses preserved."
                )
                .setPositiveButton("Close", null)
                .show()
        }

        // About Prism
        binding.rowAbout.setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }
    }

    private fun playFile(filepath: String) {
        val i = Intent(requireContext(), PlayerActivity::class.java).apply {
            if (filepath.startsWith("content://") || filepath.startsWith("http://") || filepath.startsWith("https://") || filepath.startsWith("rtsp://")) {
                action = Intent.ACTION_VIEW
                data = Uri.parse(filepath)
            }
            putExtra("filepath", filepath)
        }
        startActivity(i)
    }
}
