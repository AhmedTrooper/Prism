package com.ahmedtrooper.prism

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import androidx.preference.PreferenceManager
import android.provider.MediaStore
import android.text.format.Formatter
import android.util.Log
import android.util.LruCache
import android.util.Size
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ahmedtrooper.prism.databinding.FragmentMainScreenBinding
import com.ahmedtrooper.prism.preferences.PreferenceActivity
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainScreenFragment : Fragment(R.layout.fragment_main_screen) {
    private var _binding: FragmentMainScreenBinding? = null
    private val binding get() = _binding!!

    private lateinit var documentTreeOpener: ActivityResultLauncher<Uri?>
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var playerLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    data class MediaFolder(
        val id: Long,
        val name: String,
        val path: String,
        var videoCount: Int = 0,
        var hasNew: Boolean = false,
        var newCount: Int = 0
    )

    data class MediaVideo(
        val id: Long,
        val title: String,
        val path: String,
        val uri: Uri,
        val durationMs: Long,
        val sizeBytes: Long,
        val width: Int,
        val height: Int,
        val dateModified: Long,
        val bucketId: Long,
        val bucketName: String,
        var playbackProgress: Int = 0, // 0 - 100
        var isNew: Boolean = true,
        val formattedDuration: String = "",
        val formattedDetailsList: String = "",
        val formattedDetailsGrid: String = "",
        val resolutionBadge: String = ""
    )

    private val allVideos = mutableListOf<MediaVideo>()
    private val allFolders = mutableListOf<MediaFolder>()
    private var currentFolderId: Long? = null
    private var currentFolderName: String = ""

    private var isFolderView = true
    private var isGridMode = false
    private var searchQuery = ""

    private val thumbnailCache: LruCache<Long, Bitmap> by lazy {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = (maxMemory / 8).coerceIn(1024 * 16, 1024 * 64)
        object : LruCache<Long, Bitmap>(cacheSize) {
            override fun sizeOf(key: Long, bitmap: Bitmap): Int {
                return (bitmap.byteCount / 1024).coerceAtLeast(1)
            }
        }
    }
    private lateinit var sharedPrefs: SharedPreferences
    private var pulseAnimator: AnimatorSet? = null

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (binding.searchBarLayout.isVisible) {
                closeSearch()
            } else if (currentFolderId != null) {
                exitFolder()
            } else if (!isFolderView) {
                isFolderView = true
                applyFilterAndDisplay()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                binding.permissionCard.isVisible = false
                scanMediaLibrary()
            } else {
                binding.permissionCard.isVisible = true
            }
        }

        documentTreeOpener = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
            it?.let { root ->
                requireContext().contentResolver.takePersistableUriPermission(
                    root, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val i = Intent(context, FilePickerActivity::class.java)
                i.putExtra("skip", FilePickerActivity.DOC_PICKER)
                i.putExtra("root", root.toString())
                filePickerLauncher.launch(i)
            }
        }

        filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            it.data?.getStringExtra("path")?.let { path -> playFile(path) }
        }

        playerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            Log.v(TAG, "returned from player ($it)")
            scanMediaLibrary()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.handleInsetsAsPadding(binding.root)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)

        setupUI()
        checkPermissionAndLoad()
    }

    override fun onDestroyView() {
        stopPulseAnimation()
        super.onDestroyView()
        _binding = null
    }

    private fun startPulseAnimation() {
        pulseAnimator?.cancel()
        if (_binding == null) return

        val scaleX = ObjectAnimator.ofFloat(binding.loadingLogo, View.SCALE_X, 1.0f, 1.12f, 1.0f).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleY = ObjectAnimator.ofFloat(binding.loadingLogo, View.SCALE_Y, 1.0f, 1.12f, 1.0f).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val alpha = ObjectAnimator.ofFloat(binding.loadingLogo, View.ALPHA, 0.75f, 1.0f, 0.75f).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
        }
        pulseAnimator = AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            start()
        }
    }

    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        if (_binding != null) {
            binding.loadingLogo.scaleX = 1.0f
            binding.loadingLogo.scaleY = 1.0f
            binding.loadingLogo.alpha = 1.0f
        }
    }

    private fun showLoading(message: String? = null) {
        if (_binding == null) return
        binding.mediaRecyclerView.isVisible = false
        binding.emptyView.isVisible = false
        binding.loadingLayout.isVisible = true
        if (message != null) {
            binding.loadingText.text = message
            binding.loadingText.isVisible = true
        } else {
            binding.loadingText.isVisible = false
        }
        startPulseAnimation()
    }

    private fun hideLoading() {
        if (_binding == null) return
        stopPulseAnimation()
        binding.loadingLayout.isVisible = false
        binding.mediaRecyclerView.isVisible = true
    }

    private fun setupUI() {
        // Fix ANR "No adapter attached" - set empty placeholder immediately
        binding.mediaRecyclerView.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(p: ViewGroup, v: Int) = object : RecyclerView.ViewHolder(View(p.context)) {}
            override fun onBindViewHolder(h: RecyclerView.ViewHolder, p: Int) {}
            override fun getItemCount() = 0
        }
        binding.mediaRecyclerView.setHasFixedSize(true)
        binding.mediaRecyclerView.setItemViewCacheSize(20)
        updateLayoutManager()

        // Navigation Back Button
        binding.navBackBtn.setOnClickListener {
            if (binding.searchBarLayout.isVisible) {
                closeSearch()
            } else if (currentFolderId != null) {
                exitFolder()
            } else if (!isFolderView) {
                isFolderView = true
                applyFilterAndDisplay()
            }
        }

        // Search Action Button
        binding.searchBtn.setOnClickListener {
            openSearch()
        }

        // Close Search Button
        binding.closeSearchBtn.setOnClickListener {
            closeSearch()
        }

        // Clear Search Text Button
        binding.clearSearchTextBtn.setOnClickListener {
            binding.searchEditText.setText("")
        }

        // Search Input Watcher
        binding.searchEditText.addTextChangedListener { text ->
            searchQuery = text?.toString()?.trim() ?: ""
            binding.clearSearchTextBtn.isVisible = searchQuery.isNotEmpty()
            applyFilterAndDisplay()
        }

        // Folder All Button (Toggle between Folders mode and All Videos mode)
        binding.folderAllBtn.setOnClickListener {
            if (currentFolderId != null) {
                currentFolderId = null
                currentFolderName = ""
                isFolderView = false
                applyFilterAndDisplay()
            } else if (isFolderView) {
                isFolderView = false
                applyFilterAndDisplay()
            } else {
                isFolderView = true
                applyFilterAndDisplay()
            }
        }

        // View Mode Switch (Grid vs List Layout)
        binding.viewModeBtn.setOnClickListener {
            isGridMode = !isGridMode
            updateLayoutManager()
            applyFilterAndDisplay()
        }

        // Refresh Media Library
        binding.refreshBtn.setOnClickListener {
            scanMediaLibrary()
        }

        // Overflow 3-Dots Menu
        binding.moreMenuBtn.setOnClickListener { view ->
            showOverflowMenu(view)
        }

        // Permission Card Button
        binding.grantPermissionBtn.setOnClickListener {
            requestStoragePermission()
        }
    }

    private fun updateLayoutManager() {
        if (isFolderView && currentFolderId == null) {
            binding.mediaRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        } else {
            if (isGridMode) {
                // 4dp grid: 8dp recycler padding + 8dp card padding needs ItemDecoration
                val span = if (Utils.isXLargeTablet(requireContext())) 3 else 2
                binding.mediaRecyclerView.layoutManager = GridLayoutManager(requireContext(), span)
            } else {
                binding.mediaRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            }
        }
        // Ensure clipToPadding false already set via XML still holds after layout change
        binding.mediaRecyclerView.clipToPadding = false
    }

    private fun openSearch() {
        binding.normalBarLayout.isVisible = false
        binding.searchBarLayout.isVisible = true
        binding.searchEditText.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(binding.searchEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun closeSearch() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(binding.searchEditText.windowToken, 0)
        binding.searchBarLayout.isVisible = false
        binding.normalBarLayout.isVisible = true
        binding.searchEditText.setText("")
        searchQuery = ""
        applyFilterAndDisplay()
    }

    private fun exitFolder() {
        currentFolderId = null
        currentFolderName = ""
        applyFilterAndDisplay()
    }

    private fun showOverflowMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 5, 0, "Refresh Library")
        if (currentFolderId != null || !isFolderView) {
            popup.menu.add(0, 4, 1, if (isGridMode) "Switch to List View" else "Switch to Grid View")
        }
        popup.menu.add(0, 1, 2, "Network Stream")
        popup.menu.add(0, 2, 3, "Open File")
        popup.menu.add(0, 3, 4, "Settings")

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                5 -> {
                    scanMediaLibrary()
                    true
                }
                1 -> {
                    val helper = Utils.OpenUrlDialog(requireContext())
                    with(helper) {
                        builder.setPositiveButton(R.string.dialog_ok) { _, _ -> playFile(helper.text) }
                        builder.setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
                        create().show()
                    }
                    true
                }
                2 -> {
                    val i = Intent(context, FilePickerActivity::class.java)
                    i.putExtra("skip", FilePickerActivity.FILE_PICKER)
                    filePickerLauncher.launch(i)
                    true
                }
                3 -> {
                    startActivity(Intent(context, PreferenceActivity::class.java))
                    true
                }
                4 -> {
                    isGridMode = !isGridMode
                    updateLayoutManager()
                    applyFilterAndDisplay()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun checkPermissionAndLoad() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            binding.permissionCard.isVisible = false
            scanMediaLibrary()
        } else {
            binding.permissionCard.isVisible = true
            binding.emptyView.isVisible = false
        }
    }

    private fun requestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        if (shouldShowRequestPermissionRationale(permission)) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Storage Access Required")
                .setMessage("Prism needs video access to show your media library. Please allow permission.")
                .setPositiveButton(R.string.dialog_ok) { _, _ -> permissionLauncher.launch(permission) }
                .setNegativeButton(R.string.dialog_cancel, null)
                .show()
        } else {
            permissionLauncher.launch(permission)
        }
    }

    /**
     * Public entry point for other UI surfaces (e.g. MeFragment's quick action)
     * to request a fresh library scan. Forwards to the private worker.
     */
    fun userRequestedRescan() {
        scanMediaLibrary()
    }

    private fun scanMediaLibrary() {
        showLoading(getString(R.string.loading_scanning))
        val appContext = context?.applicationContext
        lifecycleScope.launch(Dispatchers.IO) {
            val videos = mutableListOf<MediaVideo>()
            val foldersMap = mutableMapOf<Long, MediaFolder>()
            val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

            val projection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.BUCKET_ID,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME
            )

            val sortOrder = "${MediaStore.Video.Media.DATE_MODIFIED} DESC"
            val uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

            try {
                requireContext().contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                    val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                    val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                    val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                    val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
                    val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val name = cursor.getString(nameCol) ?: "Video"
                        val path = cursor.getString(dataCol) ?: ""
                        val duration = cursor.getLong(durationCol)
                        val size = cursor.getLong(sizeCol)
                        val width = cursor.getInt(widthCol)
                        val height = cursor.getInt(heightCol)
                        val date = cursor.getLong(dateCol)
                        val bucketId = cursor.getLong(bucketIdCol)
                        val bucketName = cursor.getString(bucketNameCol) ?: "Internal Storage"

                        val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                        // Robust resume key: path hash + size + date prevents collisions
                        val resumeKeyV2 = MediaResumeHelper.buildResumeKey(path, size, date)
                        val resumeKeyLegacyPath = "resume_path_${path.hashCode()}"
                        val resumeKeyLegacyUri = "resume_path_${contentUri.toString().hashCode()}"
                        val lastPos = sharedPrefs.getLong(
                            resumeKeyV2,
                            sharedPrefs.getLong(resumeKeyLegacyPath, sharedPrefs.getLong(resumeKeyLegacyUri, 0L))
                        )
                        val isWatched = MediaResumeHelper.isWatched(
                            duration, lastPos, sharedPrefs.getBoolean("watched_$id", false)
                        )
                        val progress = MediaResumeHelper.progressPercent(lastPos, duration)

                        val resBadge = MediaBrowserHelper.resolutionBadge(width, height)
                        val formattedDur = Utils.prettyTime((duration / 1000).toInt())
                        val sizeStr = if (appContext != null) Formatter.formatFileSize(appContext, size) else "${size / (1024 * 1024)} MB"
                        val dateStr = dateFormat.format(Date(date * 1000L))
                        val detailsList = "• $sizeStr • $dateStr"
                        val detailsGrid = "$resBadge • $sizeStr"

                        val video = MediaVideo(
                            id, name, path, contentUri, duration, size, width, height, date,
                            bucketId, bucketName, progress, !isWatched,
                            formattedDuration = formattedDur,
                            formattedDetailsList = detailsList,
                            formattedDetailsGrid = detailsGrid,
                            resolutionBadge = resBadge
                        )
                        videos.add(video)

                        val folder = foldersMap.getOrPut(bucketId) {
                            val folderPath = if (path.contains("/")) path.substringBeforeLast("/") else path
                            MediaFolder(bucketId, bucketName, folderPath, 0, false, 0)
                        }
                        folder.videoCount++
                        if (video.isNew) {
                            folder.hasNew = true
                            folder.newCount++
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning media library", e)
            }

            withContext(Dispatchers.Main) {
                if (_binding == null) return@withContext
                hideLoading()

                allVideos.clear()
                allVideos.addAll(videos)

                allFolders.clear()
                allFolders.addAll(foldersMap.values)

                applyFilterAndDisplay()
            }
        }
    }

    private fun enterFolder(folder: MediaFolder) {
        currentFolderId = folder.id
        currentFolderName = folder.name
        applyFilterAndDisplay()
        binding.mediaRecyclerView.scrollToPosition(0)
    }

    private fun applyFilterAndDisplay() {
        if (_binding == null) return

        val query = searchQuery.trim().lowercase(Locale.ROOT)

        if (allVideos.isEmpty() && !binding.permissionCard.isVisible) {
            binding.emptyView.isVisible = true
            binding.emptyMessageTxt.text = getString(R.string.empty_no_videos)
            binding.mediaRecyclerView.isVisible = false
            updateTopBarUI()
            return
        }

        updateLayoutManager()

        if (isFolderView && currentFolderId == null) {
            // Folders List View Mode
            updateTopBarUI()
            val filteredFolders = if (query.isEmpty()) {
                allFolders
            } else {
                MediaBrowserHelper.filterFolders(allFolders, query)
            }

            if (filteredFolders.isEmpty()) {
                binding.emptyView.isVisible = true
                binding.emptyMessageTxt.text = getString(R.string.empty_no_videos)
                binding.mediaRecyclerView.isVisible = false
            } else {
                binding.emptyView.isVisible = false
                binding.mediaRecyclerView.isVisible = true
                binding.mediaRecyclerView.adapter = FolderAdapter(filteredFolders) { folder ->
                    enterFolder(folder)
                }
            }
        } else {
            // Videos View Mode (inside folder or all videos)
            val baseList = if (currentFolderId != null) {
                allVideos.filter { it.bucketId == currentFolderId }
            } else {
                allVideos
            }

            updateTopBarUI(baseList.size)

            val filteredVideos = MediaBrowserHelper.filterVideos(baseList, query)

            if (filteredVideos.isEmpty()) {
                binding.emptyView.isVisible = true
                binding.emptyMessageTxt.text = if (currentFolderId != null) {
                    getString(R.string.empty_folder)
                } else {
                    getString(R.string.empty_no_videos)
                }
                binding.mediaRecyclerView.isVisible = false
            } else {
                binding.emptyView.isVisible = false
                binding.mediaRecyclerView.isVisible = true
                binding.mediaRecyclerView.adapter = VideoAdapter(filteredVideos, isGridMode) { video ->
                    markVideoWatched(video)
                    playFile(video.path.ifEmpty { video.uri.toString() })
                }
            }
        }
    }

    private fun updateTopBarUI(itemCount: Int = 0) {
        binding.folderCountSubtitleTxt.isVisible = false
        binding.folderCountSubtitleTxt.text = ""

        if (currentFolderId != null) {
            // Inside Folder View - MX Player does NOT show number of videos in top bar!
            binding.navBackBtn.isVisible = true
            binding.appTitleTxt.text = currentFolderName
            binding.folderAllBtn.setImageResource(R.drawable.ic_folder_all_24dp)
        } else if (!isFolderView) {
            // All Videos View
            binding.navBackBtn.isVisible = true
            binding.appTitleTxt.text = "All Videos"
            binding.folderAllBtn.setImageResource(R.drawable.ic_folder_24dp)
        } else {
            // Root Folders View
            binding.navBackBtn.isVisible = false
            binding.appTitleTxt.text = "Folders"
            binding.folderAllBtn.setImageResource(R.drawable.ic_folder_all_24dp)
        }

        binding.viewModeBtn.setImageResource(
            if (isGridMode) R.drawable.ic_view_list_24dp else R.drawable.ic_mx_layout_switcher
        )
    }

    private fun markVideoWatched(video: MediaVideo) {
        video.isNew = false
        sharedPrefs.edit().putBoolean("watched_${video.id}", true).apply()
    }

    private fun playFile(filepath: String, fromBeginning: Boolean = false) {
        val i: Intent = if (filepath.startsWith("content://")) {
            Intent(Intent.ACTION_VIEW, Uri.parse(filepath))
        } else {
            Intent().apply { putExtra("filepath", filepath) }
        }
        if (fromBeginning) {
            i.putExtra("from_beginning", true)
        }
        i.setClass(requireContext(), PlayerActivity::class.java)
        playerLauncher.launch(i)
    }

    // =========================================================================
    // Adapters
    // =========================================================================

    private class FolderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val folderIcon: ImageView = view.findViewById(R.id.folderIcon)
        val folderNameTxt: TextView = view.findViewById(R.id.folderNameTxt)
        val videoCountTxt: TextView = view.findViewById(R.id.videoCountTxt)
        val newBadgeTxt: TextView = view.findViewById(R.id.newBadgeTxt)
    }

    private inner class FolderAdapter(
        private val folders: List<MediaFolder>,
        private val onClick: (MediaFolder) -> Unit
    ) : RecyclerView.Adapter<FolderViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_media_folder, parent, false)
            return FolderViewHolder(v)
        }

        override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
            val item = folders[position]
            holder.folderNameTxt.text = item.name
            holder.videoCountTxt.text = if (item.videoCount == 1) "1 video" else "${item.videoCount} videos"
            holder.newBadgeTxt.isVisible = item.hasNew
            holder.newBadgeTxt.text = if (item.newCount > 0) item.newCount.toString() else "1"

            // Choose authentic MX Player folder icon based on folder name
            val lower = item.name.lowercase(Locale.ROOT)
            val iconRes = when {
                lower.contains("camera") || lower.contains("dcim") -> R.drawable.ic_mx_folder_camera
                lower.contains("screenshot") -> R.drawable.ic_mx_folder_screenshots
                lower.contains("download") -> R.drawable.ic_mx_folder_download
                else -> R.drawable.ic_mx_folder_default
            }
            holder.folderIcon.setImageResource(iconRes)

            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = folders.size
    }

    private class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val videoThumbnail: ImageView = view.findViewById(R.id.videoThumbnail)
        val videoPlaceholderIcon: ImageView = view.findViewById(R.id.videoPlaceholderIcon)
        val videoDurationTxt: TextView = view.findViewById(R.id.videoDurationTxt)
        val videoNewBadge: TextView = view.findViewById(R.id.videoNewBadge)
        val videoProgressBar: ProgressBar = view.findViewById(R.id.videoProgressBar)
        val videoTitleTxt: TextView = view.findViewById(R.id.videoTitleTxt)
        val videoResolutionBadge: TextView? = view.findViewById(R.id.videoResolutionBadge)
        val videoDetailsTxt: TextView = view.findViewById(R.id.videoDetailsTxt)
        val videoMoreBtn: ImageButton = view.findViewById(R.id.videoMoreBtn)
        var thumbnailJob: Job? = null
    }

    private inner class VideoAdapter(
        private val videos: List<MediaVideo>,
        private val isGrid: Boolean,
        private val onClick: (MediaVideo) -> Unit
    ) : RecyclerView.Adapter<VideoViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
            val layoutId = if (isGrid) R.layout.item_media_video_grid else R.layout.item_media_video
            val v = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
            return VideoViewHolder(v)
        }

        override fun onViewRecycled(holder: VideoViewHolder) {
            super.onViewRecycled(holder)
            holder.thumbnailJob?.cancel()
            holder.thumbnailJob = null
        }

        override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
            val item = videos[position]
            holder.thumbnailJob?.cancel()
            holder.thumbnailJob = null

            holder.videoTitleTxt.text = item.title
            holder.videoDurationTxt.text = item.formattedDuration.ifEmpty {
                Utils.prettyTime((item.durationMs / 1000).toInt())
            }
            holder.videoNewBadge.isVisible = item.isNew

            // Resume progress bar
            if (item.playbackProgress > 5 && item.playbackProgress < 95) {
                holder.videoProgressBar.isVisible = true
                holder.videoProgressBar.progress = item.playbackProgress
            } else {
                holder.videoProgressBar.isVisible = false
            }

            // Specs and Details (instant pre-formatted strings, zero allocations)
            val resText = item.resolutionBadge.ifEmpty { MediaBrowserHelper.resolutionBadge(item.width, item.height) }

            if (holder.videoResolutionBadge != null) {
                holder.videoResolutionBadge.text = resText
                holder.videoDetailsTxt.text = item.formattedDetailsList.ifEmpty {
                    val sizeStr = Formatter.formatFileSize(holder.itemView.context, item.sizeBytes)
                    val dateStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(item.dateModified * 1000L))
                    "• $sizeStr • $dateStr"
                }
            } else {
                holder.videoDetailsTxt.text = item.formattedDetailsGrid.ifEmpty {
                    val sizeStr = Formatter.formatFileSize(holder.itemView.context, item.sizeBytes)
                    "$resText • $sizeStr"
                }
            }

            // Thumbnail Loading via LruCache with job cancellation on recycle
            holder.videoThumbnail.tag = item.id
            val cachedBitmap = thumbnailCache.get(item.id)
            if (cachedBitmap != null) {
                holder.videoThumbnail.setImageBitmap(cachedBitmap)
                holder.videoThumbnail.isVisible = true
                holder.videoPlaceholderIcon.isVisible = false
            } else {
                holder.videoThumbnail.setImageDrawable(null)
                holder.videoThumbnail.isVisible = false
                holder.videoPlaceholderIcon.isVisible = true

                holder.thumbnailJob = lifecycleScope.launch(Dispatchers.IO) {
                    val bitmap: Bitmap? = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val signal = CancellationSignal()
                            requireContext().contentResolver.loadThumbnail(
                                item.uri, Size(320, 180), signal
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            MediaStore.Video.Thumbnails.getThumbnail(
                                requireContext().contentResolver,
                                item.id,
                                MediaStore.Video.Thumbnails.MINI_KIND,
                                null
                            )
                        }
                    } catch (e: Exception) {
                        null
                    }

                    if (bitmap != null) {
                        thumbnailCache.put(item.id, bitmap)
                        withContext(Dispatchers.Main) {
                            if (holder.videoThumbnail.tag == item.id) {
                                holder.videoThumbnail.setImageBitmap(bitmap)
                                holder.videoThumbnail.isVisible = true
                                holder.videoPlaceholderIcon.isVisible = false
                            }
                        }
                    }
                }
            }

            holder.itemView.setOnClickListener { onClick(item) }

            holder.videoMoreBtn.setOnClickListener { v ->
                showVideoItemMenu(v, item)
            }
        }

        override fun getItemCount() = videos.size
    }

    private fun showVideoItemMenu(anchor: View, video: MediaVideo) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, "Play")
        popup.menu.add(0, 2, 1, "Play from beginning")
        popup.menu.add(0, 3, 2, "Share")
        popup.menu.add(0, 4, 3, "Properties")

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                1 -> {
                    markVideoWatched(video)
                    playFile(video.path.ifEmpty { video.uri.toString() })
                    true
                }
                2 -> {
                    markVideoWatched(video)
                    playFile(video.path.ifEmpty { video.uri.toString() }, fromBeginning = true)
                    true
                }
                3 -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "video/*"
                        putExtra(Intent.EXTRA_STREAM, video.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share Video"))
                    true
                }
                4 -> {
                    val dateFormatted = SimpleDateFormat("MMMM dd, yyyy, h:mm a", Locale.getDefault())
                        .format(Date(video.dateModified * 1000L))
                    val sizeFormatted = Formatter.formatFileSize(requireContext(), video.sizeBytes)
                    val ext = if (video.path.contains(".")) video.path.substringAfterLast(".").uppercase(Locale.ROOT) else "Video"
                    val resStr = if (video.width > 0 && video.height > 0) "${video.width} x ${video.height}" else "Unknown"

                    val details = """
                        File
                        File: ${video.title}
                        Location: ${video.path}
                        Size: $sizeFormatted (${String.format(Locale.getDefault(), "%,d", video.sizeBytes)} bytes)
                        Date: $dateFormatted

                        Media
                        Format: $ext
                        Resolution: $resStr
                        Length: ${Utils.prettyTime((video.durationMs / 1000).toInt())}
                    """.trimIndent()

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(video.title)
                        .setMessage(details)
                        .setPositiveButton("Okay", null)
                        .show()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    companion object {
        private const val TAG = "Prism"
    }
}
