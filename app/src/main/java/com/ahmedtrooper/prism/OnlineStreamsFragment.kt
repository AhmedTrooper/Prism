package com.ahmedtrooper.prism

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ahmedtrooper.prism.databinding.FragmentOnlineStreamsBinding
import java.util.Locale

class OnlineStreamsFragment : Fragment(R.layout.fragment_online_streams) {

    private var _binding: FragmentOnlineStreamsBinding? = null
    private val binding get() = _binding!!

    private var currentCategoryId: String? = null
    private var currentCategoryName: String = "Streams"
    private val categoryStack = mutableListOf<Pair<String?, String>>() // (id, name)
    private var searchQuery = ""

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (binding.searchBarLayout.isVisible) {
                closeSearch()
            } else if (categoryStack.isNotEmpty()) {
                popCategory()
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnlineStreamsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Utils.handleInsetsAsPadding(binding.root)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)

        OnlineStreamManager.load(requireContext())

        setupUI()
        refreshList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupUI() {
        binding.streamsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.streamsRecyclerView.setHasFixedSize(true)

        // Nav Back Button
        binding.navBackBtn.setOnClickListener {
            if (binding.searchBarLayout.isVisible) {
                closeSearch()
            } else if (categoryStack.isNotEmpty()) {
                popCategory()
            }
        }

        // Add Button (Top bar and FAB)
        binding.addBtn.setOnClickListener { showAddChoiceDialog() }
        binding.addFab.setOnClickListener { showAddChoiceDialog() }
        binding.emptyAddBtn.setOnClickListener { showAddChoiceDialog() }

        // Search Button
        binding.searchBtn.setOnClickListener { openSearch() }
        binding.closeSearchBtn.setOnClickListener { closeSearch() }
        binding.clearSearchTextBtn.setOnClickListener { binding.searchEditText.setText("") }

        binding.searchEditText.addTextChangedListener { text ->
            searchQuery = text?.toString()?.trim() ?: ""
            binding.clearSearchTextBtn.isVisible = searchQuery.isNotEmpty()
            refreshList()
        }
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
        refreshList()
    }

    private fun enterCategory(category: StreamCategory) {
        categoryStack.add(Pair(currentCategoryId, currentCategoryName))
        currentCategoryId = category.id
        currentCategoryName = category.name
        refreshList()
    }

    private fun popCategory() {
        if (categoryStack.isNotEmpty()) {
            val previous = categoryStack.removeAt(categoryStack.size - 1)
            currentCategoryId = previous.first
            currentCategoryName = previous.second
            refreshList()
        }
    }

    fun canGoBack(): Boolean = categoryStack.isNotEmpty()

    fun navigateUp() {
        popCategory()
    }

    private fun refreshList() {
        if (_binding == null) return

        // Update Top Bar
        binding.streamsTitleTxt.text = currentCategoryName
        binding.navBackBtn.isVisible = categoryStack.isNotEmpty()

        // Fetch categories & videos
        val allCategories = OnlineStreamManager.getCategories(currentCategoryId)
        val allVideos = if (currentCategoryId != null) {
            OnlineStreamManager.getVideos(currentCategoryId!!)
        } else {
            emptyList()
        }

        // Apply search query filter if active
        val q = searchQuery.lowercase(Locale.ROOT)
        val filteredCategories = if (q.isEmpty()) allCategories else allCategories.filter { it.name.lowercase(Locale.ROOT).contains(q) }
        val filteredVideos = if (q.isEmpty()) allVideos else allVideos.filter { it.title.lowercase(Locale.ROOT).contains(q) || it.url.lowercase(Locale.ROOT).contains(q) }

        val isEmpty = filteredCategories.isEmpty() && filteredVideos.isEmpty()
        binding.emptyView.isVisible = isEmpty
        binding.streamsRecyclerView.isVisible = !isEmpty

        if (!isEmpty) {
            binding.streamsRecyclerView.adapter = CombinedStreamsAdapter(
                categories = filteredCategories,
                videos = filteredVideos,
                onCategoryClick = { cat -> enterCategory(cat) },
                onCategoryMoreClick = { view, cat -> showCategoryMenu(view, cat) },
                onVideoClick = { vid -> playStream(vid.url) },
                onVideoMoreClick = { view, vid -> showVideoMenu(view, vid) }
            )
        }
    }

    private fun showAddChoiceDialog() {
        val options = arrayOf("📁 Create New Folder / Category", "🎬 Add Stream URL (HLS / m3u8 / MP4)")
        AlertDialog.Builder(requireContext())
            .setTitle(if (currentCategoryId == null) "Add to Streams" else "Add to $currentCategoryName")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showCreateCategoryDialog()
                    1 -> showAddStreamDialog()
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showCreateCategoryDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Folder / Category name"
            setSingleLine()
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (currentCategoryId == null) "New Category" else "New Subcategory")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    OnlineStreamManager.addCategory(requireContext(), name, currentCategoryId)
                    refreshList()
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showAddStreamDialog() {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val titleInput = EditText(requireContext()).apply {
            hint = "Stream / Video Title"
            setSingleLine()
        }

        val urlInput = EditText(requireContext()).apply {
            hint = "URL (https://.../stream.m3u8)"
            setSingleLine()
        }

        layout.addView(titleInput)
        layout.addView(urlInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Add Online Stream")
            .setView(layout)
            .setPositiveButton("Add") { _, _ ->
                val title = titleInput.text.toString().trim()
                val url = urlInput.text.toString().trim()
                if (url.isNotEmpty()) {
                    // If at root without category, create or put in default category
                    val targetCatId = currentCategoryId ?: run {
                        val defaultCat = OnlineStreamManager.addCategory(requireContext(), "My Streams", null)
                        defaultCat.id
                    }
                    OnlineStreamManager.addVideo(requireContext(), title.ifEmpty { "Stream Video" }, url, targetCatId)
                    refreshList()
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showCategoryMenu(anchor: View, category: StreamCategory) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, "Rename")
        popup.menu.add(0, 2, 1, "Delete Folder")

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                1 -> {
                    showRenameCategoryDialog(category)
                    true
                }
                2 -> {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Delete ${category.name}?")
                        .setMessage("This will delete this category, all its subcategories, and streams.")
                        .setPositiveButton("Delete") { _, _ ->
                            OnlineStreamManager.deleteCategory(requireContext(), category.id)
                            refreshList()
                        }
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .show()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showRenameCategoryDialog(category: StreamCategory) {
        val input = EditText(requireContext()).apply {
            setText(category.name)
            setSelection(category.name.length)
            setSingleLine()
            setPadding(48, 32, 48, 32)
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Rename Category")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    OnlineStreamManager.renameCategory(requireContext(), category.id, newName)
                    refreshList()
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showVideoMenu(anchor: View, video: StreamVideo) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, "Play")
        popup.menu.add(0, 2, 1, "Copy URL")
        popup.menu.add(0, 3, 2, "Edit")
        popup.menu.add(0, 4, 3, "Delete")

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                1 -> {
                    playStream(video.url)
                    true
                }
                2 -> {
                    val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Stream URL", video.url))
                    Toast.makeText(requireContext(), "URL copied to clipboard", Toast.LENGTH_SHORT).show()
                    true
                }
                3 -> {
                    showEditVideoDialog(video)
                    true
                }
                4 -> {
                    OnlineStreamManager.deleteVideo(requireContext(), video.id)
                    refreshList()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showEditVideoDialog(video: StreamVideo) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val titleInput = EditText(requireContext()).apply {
            setText(video.title)
            setSingleLine()
        }

        val urlInput = EditText(requireContext()).apply {
            setText(video.url)
            setSingleLine()
        }

        layout.addView(titleInput)
        layout.addView(urlInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Stream")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newTitle = titleInput.text.toString().trim()
                val newUrl = urlInput.text.toString().trim()
                if (newUrl.isNotEmpty()) {
                    OnlineStreamManager.editVideo(requireContext(), video.id, newTitle.ifEmpty { "Stream" }, newUrl)
                    refreshList()
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun playStream(url: String) {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(url)
            putExtra("filepath", url)
        }
        startActivity(intent)
    }

    // =========================================================================
    // Combined Adapter for Categories and Online Videos
    // =========================================================================

    private class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val folderIcon: ImageView = view.findViewById(R.id.folderIcon)
        val folderNameTxt: TextView = view.findViewById(R.id.folderNameTxt)
        val videoCountTxt: TextView = view.findViewById(R.id.videoCountTxt)
        val newBadgeTxt: TextView = view.findViewById(R.id.newBadgeTxt)
    }

    private class StreamViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val streamBadgeTxt: TextView = view.findViewById(R.id.streamBadgeTxt)
        val streamTypeTagTxt: TextView = view.findViewById(R.id.streamTypeTagTxt)
        val streamTitleTxt: TextView = view.findViewById(R.id.streamTitleTxt)
        val streamUrlTxt: TextView = view.findViewById(R.id.streamUrlTxt)
        val streamMoreBtn: ImageButton = view.findViewById(R.id.streamMoreBtn)
    }

    private inner class CombinedStreamsAdapter(
        private val categories: List<StreamCategory>,
        private val videos: List<StreamVideo>,
        private val onCategoryClick: (StreamCategory) -> Unit,
        private val onCategoryMoreClick: (View, StreamCategory) -> Unit,
        private val onVideoClick: (StreamVideo) -> Unit,
        private val onVideoMoreClick: (View, StreamVideo) -> Unit
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_CATEGORY = 0
        private val TYPE_VIDEO = 1

        override fun getItemViewType(position: Int): Int {
            return if (position < categories.size) TYPE_CATEGORY else TYPE_VIDEO
        }

        override fun getItemCount(): Int = categories.size + videos.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return if (viewType == TYPE_CATEGORY) {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_media_folder, parent, false)
                CategoryViewHolder(v)
            } else {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_online_video, parent, false)
                StreamViewHolder(v)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (getItemViewType(position) == TYPE_CATEGORY) {
                val cat = categories[position]
                val h = holder as CategoryViewHolder
                h.folderNameTxt.text = cat.name
                val (subfolders, vids) = OnlineStreamManager.countItems(cat.id)
                val countText = when {
                    subfolders > 0 && vids > 0 -> "$subfolders folders, $vids streams"
                    subfolders > 0 -> if (subfolders == 1) "1 folder" else "$subfolders folders"
                    vids > 0 -> if (vids == 1) "1 stream" else "$vids streams"
                    else -> "Empty folder"
                }
                h.videoCountTxt.text = countText
                h.newBadgeTxt.isVisible = false
                h.folderIcon.setImageResource(R.drawable.ic_mx_folder_default)

                h.itemView.setOnClickListener { onCategoryClick(cat) }
                h.itemView.setOnLongClickListener {
                    onCategoryMoreClick(h.itemView, cat)
                    true
                }
            } else {
                val vid = videos[position - categories.size]
                val h = holder as StreamViewHolder
                h.streamTitleTxt.text = vid.title
                h.streamUrlTxt.text = vid.url
                val badge = OnlineStreamManager.detectFormatBadge(vid.url)
                h.streamBadgeTxt.text = badge

                h.itemView.setOnClickListener { onVideoClick(vid) }
                h.streamMoreBtn.setOnClickListener { v -> onVideoMoreClick(v, vid) }
            }
        }
    }
}
