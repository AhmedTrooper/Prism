package `is`.xyz.mpv

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.text.format.Formatter
import android.util.Log
import android.util.LruCache
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import `is`.xyz.mpv.databinding.FragmentMainScreenBinding
import `is`.xyz.mpv.preferences.PreferenceActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
        var videoCount: Int = 0
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
        val bucketName: String
    )

    private val allVideos = mutableListOf<MediaVideo>()
    private val allFolders = mutableListOf<MediaFolder>()
    private var currentFolderId: Long? = null
    private var isFolderMode = true
    private var searchQuery = ""

    private val thumbnailCache = LruCache<Long, Bitmap>(80)

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (binding.searchBarLayout.isVisible) {
                closeSearch()
            } else if (currentFolderId != null) {
                exitFolder()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
            // Refresh to update resume progress bars
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
        super.onDestroyView()
        _binding = null
    }

    private fun setupUI() {
        binding.mediaRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Top App Bar buttons
        binding.searchBtn.setOnClickListener {
            binding.searchBarLayout.isVisible = true
            binding.searchEditText.requestFocus()
        }

        binding.closeSearchBtn.setOnClickListener {
            closeSearch()
        }

        binding.searchEditText.addTextChangedListener { text ->
            searchQuery = text?.toString()?.trim() ?: ""
            applyFilterAndDisplay()
        }

        binding.viewModeBtn.setOnClickListener {
            isFolderMode = !isFolderMode
            currentFolderId = null
            binding.viewModeBtn.setImageResource(
                if (isFolderMode) R.drawable.ic_folder_24dp else R.drawable.ic_video_file_24dp
            )
            applyFilterAndDisplay()
        }

        binding.urlBtn.setOnClickListener {
            val helper = Utils.OpenUrlDialog(requireContext())
            with (helper) {
                builder.setPositiveButton(R.string.dialog_ok) { _, _ -> playFile(helper.text) }
                builder.setNegativeButton(R.string.dialog_cancel) { dialog, _ -> dialog.cancel() }
                create().show()
            }
        }

        binding.openUrlEmptyBtn.setOnClickListener {
            binding.urlBtn.callOnClick()
        }

        binding.filepickerBtn.setOnClickListener {
            val i = Intent(context, FilePickerActivity::class.java)
            i.putExtra("skip", FilePickerActivity.FILE_PICKER)
            filePickerLauncher.launch(i)
        }

        binding.settingsBtn.setOnClickListener {
            startActivity(Intent(context, PreferenceActivity::class.java))
        }

        binding.folderBackBtn.setOnClickListener {
            exitFolder()
        }

        binding.grantPermissionBtn.setOnClickListener {
            requestStoragePermission()
        }
    }

    private fun closeSearch() {
        binding.searchBarLayout.isVisible = false
        binding.searchEditText.setText("")
        searchQuery = ""
        applyFilterAndDisplay()
    }

    private fun exitFolder() {
        currentFolderId = null
        binding.breadcrumbLayout.isVisible = false
        applyFilterAndDisplay()
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
            binding.emptyLayout.isVisible = false
        }
    }

    private fun requestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permissionLauncher.launch(permission)
    }

    private fun scanMediaLibrary() {
        lifecycleScope.launch(Dispatchers.IO) {
            val videos = mutableListOf<MediaVideo>()
            val foldersMap = mutableMapOf<Long, MediaFolder>()

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
                        val video = MediaVideo(id, name, path, contentUri, duration, size, width, height, date, bucketId, bucketName)
                        videos.add(video)

                        val folder = foldersMap.getOrPut(bucketId) {
                            val folderPath = if (path.contains("/")) path.substringBeforeLast("/") else path
                            MediaFolder(bucketId, bucketName, folderPath, 0)
                        }
                        folder.videoCount++
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error scanning media library", e)
            }

            withContext(Dispatchers.Main) {
                if (_binding == null) return@withContext
                allVideos.clear()
                allVideos.addAll(videos)

                allFolders.clear()
                allFolders.addAll(foldersMap.values)

                applyFilterAndDisplay()
            }
        }
    }

    private fun applyFilterAndDisplay() {
        if (_binding == null) return

        if (allVideos.isEmpty() && !binding.permissionCard.isVisible) {
            binding.emptyLayout.isVisible = true
            binding.mediaRecyclerView.isVisible = false
            return
        }

        binding.emptyLayout.isVisible = false
        binding.mediaRecyclerView.isVisible = true

        val query = searchQuery.lowercase(Locale.ROOT)

        if (isFolderMode && currentFolderId == null) {
            // Folder List Mode
            binding.breadcrumbLayout.isVisible = false
            val filteredFolders = if (query.isEmpty()) {
                allFolders
            } else {
                allFolders.filter { it.name.lowercase(Locale.ROOT).contains(query) }
            }

            binding.mediaRecyclerView.adapter = FolderAdapter(filteredFolders) { folder ->
                currentFolderId = folder.id
                binding.breadcrumbLayout.isVisible = true
                binding.currentFolderNameTxt.text = folder.name
                binding.currentFolderCountTxt.text = "${folder.videoCount} videos"
                applyFilterAndDisplay()
            }
        } else {
            // Video List Mode (either inside a folder or All Videos)
            val baseList = if (currentFolderId != null) {
                allVideos.filter { it.bucketId == currentFolderId }
            } else {
                binding.breadcrumbLayout.isVisible = false
                allVideos
            }

            val filteredVideos = if (query.isEmpty()) {
                baseList
            } else {
                baseList.filter { it.title.lowercase(Locale.ROOT).contains(query) }
            }

            binding.mediaRecyclerView.adapter = VideoAdapter(filteredVideos) { video ->
                playFile(video.path.ifEmpty { video.uri.toString() })
            }
        }
    }

    private fun playFile(filepath: String) {
        val i: Intent = if (filepath.startsWith("content://")) {
            Intent(Intent.ACTION_VIEW, Uri.parse(filepath))
        } else {
            Intent().apply { putExtra("filepath", filepath) }
        }
        i.setClass(requireContext(), MPVActivity::class.java)
        playerLauncher.launch(i)
    }

    // =========================================================================
    // Adapters
    // =========================================================================

    private class FolderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val folderNameTxt: TextView = view.findViewById(R.id.folderNameTxt)
        val folderPathTxt: TextView = view.findViewById(R.id.folderPathTxt)
        val videoCountTxt: TextView = view.findViewById(R.id.videoCountTxt)
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
            holder.folderPathTxt.text = item.path
            holder.videoCountTxt.text = "${item.videoCount} videos"
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = folders.size
    }

    private class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val videoThumbnail: ImageView = view.findViewById(R.id.videoThumbnail)
        val videoPlaceholderIcon: ImageView = view.findViewById(R.id.videoPlaceholderIcon)
        val videoDurationTxt: TextView = view.findViewById(R.id.videoDurationTxt)
        val videoProgressBar: ProgressBar = view.findViewById(R.id.videoProgressBar)
        val videoTitleTxt: TextView = view.findViewById(R.id.videoTitleTxt)
        val videoResolutionBadge: TextView = view.findViewById(R.id.videoResolutionBadge)
        val videoDetailsTxt: TextView = view.findViewById(R.id.videoDetailsTxt)
        val videoMoreBtn: ImageButton = view.findViewById(R.id.videoMoreBtn)
    }

    private inner class VideoAdapter(
        private val videos: List<MediaVideo>,
        private val onClick: (MediaVideo) -> Unit
    ) : RecyclerView.Adapter<VideoViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_media_video, parent, false)
            return VideoViewHolder(v)
        }

        override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
            val item = videos[position]

            holder.videoTitleTxt.text = item.title
            holder.videoDurationTxt.text = Utils.prettyTime((item.durationMs / 1000).toInt())

            val sizeStr = Formatter.formatFileSize(requireContext(), item.sizeBytes)
            holder.videoDetailsTxt.text = sizeStr

            // Resolution badge
            val resText = if (item.height >= 2160 || item.width >= 3840) {
                "4K"
            } else if (item.height >= 1080 || item.width >= 1920) {
                "1080p"
            } else if (item.height >= 720 || item.width >= 1280) {
                "720p"
            } else if (item.height >= 480 || item.width >= 854) {
                "480p"
            } else {
                "SD"
            }
            holder.videoResolutionBadge.text = resText

            // Thumbnail loading
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

                lifecycleScope.launch(Dispatchers.IO) {
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

            holder.videoMoreBtn.setOnClickListener {
                showVideoOptions(item)
            }
        }

        override fun getItemCount() = videos.size
    }

    private fun showVideoOptions(video: MediaVideo) {
        val options = arrayOf("Play", "Share", "Details")
        AlertDialog.Builder(requireContext())
            .setTitle(video.title)
            .setItems(options) { dialog, idx ->
                when (idx) {
                    0 -> playFile(video.path.ifEmpty { video.uri.toString() })
                    1 -> {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "video/*"
                            putExtra(Intent.EXTRA_STREAM, video.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, "Share Video"))
                    }
                    2 -> {
                        val details = """
                            Title: ${video.title}
                            Duration: ${Utils.prettyTime((video.durationMs / 1000).toInt())}
                            Resolution: ${video.width} x ${video.height}
                            Size: ${Formatter.formatFileSize(requireContext(), video.sizeBytes)}
                            Path: ${video.path}
                        """.trimIndent()
                        AlertDialog.Builder(requireContext())
                            .setTitle("Video Details")
                            .setMessage(details)
                            .setPositiveButton(R.string.dialog_ok, null)
                            .show()
                    }
                }
                dialog.dismiss()
            }
            .show()
    }

    companion object {
        private const val TAG = "mpv"
    }
}
