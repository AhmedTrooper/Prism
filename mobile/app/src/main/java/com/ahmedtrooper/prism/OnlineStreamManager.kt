package com.ahmedtrooper.prism

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class StreamCategory(
    val id: String,
    val parentId: String?,
    var name: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class StreamVideo(
    val id: String,
    var categoryId: String,
    var title: String,
    var url: String,
    val createdAt: Long = System.currentTimeMillis()
)

object OnlineStreamManager {
    private const val FILE_NAME = "online_streams.json"

    private val categories = mutableListOf<StreamCategory>()
    private val videos = mutableListOf<StreamVideo>()
    private var isLoaded = false

    @Synchronized
    fun load(context: Context) {
        if (isLoaded) return
        categories.clear()
        videos.clear()

        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) {
            try {
                val jsonStr = file.readText()
                val root = JSONObject(jsonStr)

                val catArr = root.optJSONArray("categories") ?: JSONArray()
                for (i in 0 until catArr.length()) {
                    val obj = catArr.getJSONObject(i)
                    categories.add(
                        StreamCategory(
                            id = obj.getString("id"),
                            parentId = if (obj.has("parentId") && !obj.isNull("parentId")) obj.getString("parentId") else null,
                            name = obj.getString("name"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }

                val vidArr = root.optJSONArray("videos") ?: JSONArray()
                for (i in 0 until vidArr.length()) {
                    val obj = vidArr.getJSONObject(i)
                    videos.add(
                        StreamVideo(
                            id = obj.getString("id"),
                            categoryId = obj.getString("categoryId"),
                            title = obj.getString("title"),
                            url = obj.getString("url"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                isLoaded = true
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Seed with authentic starter categories & working HLS/m3u8/MP4 streams
        seedInitialStreams(context)
        isLoaded = true
    }

    private fun seedInitialStreams(context: Context) {
        val catLive = addCategoryInternal("IPTV & Live Channels", null)
        val catNews = addCategoryInternal("News Streams", catLive.id)
        val catDemos = addCategoryInternal("HLS Test Streams", catLive.id)

        val catWeb = addCategoryInternal("Web Series & Movies", null)
        val catShorts = addCategoryInternal("Blender Open Movies", catWeb.id)

        addVideoInternal("France 24 English (HLS)", "https://static.france24.com/live/F24_EN_LO_HLS/live_web.m3u8", catNews.id)
        addVideoInternal("Deutsche Welle News (HLS)", "https://dwamdstream102.akamaized.net/hls/live/2015525/dl_live_1_en/master.m3u8", catNews.id)

        addVideoInternal("Big Buck Bunny (Multi-bitrate HLS)", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", catDemos.id)
        addVideoInternal("Tears of Steel (4K HLS)", "https://demo.unified-streaming.com/kaltura/tears-of-steel/tears-of-steel.ism/.m3u8", catDemos.id)

        addVideoInternal("Sintel (1080p MP4)", "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4", catShorts.id)

        saveInternal(context)
    }

    @Synchronized
    private fun saveInternal(context: Context) {
        try {
            val root = JSONObject()
            val catArr = JSONArray()
            for (cat in categories) {
                val obj = JSONObject()
                obj.put("id", cat.id)
                obj.put("parentId", cat.parentId ?: JSONObject.NULL)
                obj.put("name", cat.name)
                obj.put("createdAt", cat.createdAt)
                catArr.put(obj)
            }
            root.put("categories", catArr)

            val vidArr = JSONArray()
            for (vid in videos) {
                val obj = JSONObject()
                obj.put("id", vid.id)
                obj.put("categoryId", vid.categoryId)
                obj.put("title", vid.title)
                obj.put("url", vid.url)
                obj.put("createdAt", vid.createdAt)
                vidArr.put(obj)
            }
            root.put("videos", vidArr)

            val file = File(context.filesDir, FILE_NAME)
            file.writeText(root.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun getCategories(parentId: String?): List<StreamCategory> {
        return categories.filter { it.parentId == parentId }.sortedBy { it.name.lowercase() }
    }

    @Synchronized
    fun getVideos(categoryId: String): List<StreamVideo> {
        return videos.filter { it.categoryId == categoryId }.sortedBy { it.title.lowercase() }
    }

    @Synchronized
    fun getCategory(id: String): StreamCategory? {
        return categories.find { it.id == id }
    }

    @Synchronized
    fun countItems(categoryId: String): Pair<Int, Int> {
        val folderCount = categories.count { it.parentId == categoryId }
        val videoCount = videos.count { it.categoryId == categoryId }
        return Pair(folderCount, videoCount)
    }

    @Synchronized
    fun addCategory(context: Context, name: String, parentId: String?): StreamCategory {
        load(context)
        val cat = addCategoryInternal(name, parentId)
        saveInternal(context)
        return cat
    }

    private fun addCategoryInternal(name: String, parentId: String?): StreamCategory {
        val cat = StreamCategory(
            id = UUID.randomUUID().toString(),
            parentId = parentId,
            name = name.trim().ifEmpty { "New Folder" }
        )
        categories.add(cat)
        return cat
    }

    @Synchronized
    fun renameCategory(context: Context, id: String, newName: String) {
        load(context)
        categories.find { it.id == id }?.name = newName.trim()
        saveInternal(context)
    }

    @Synchronized
    fun deleteCategory(context: Context, id: String) {
        load(context)
        // Find all descendants recursively
        val toDeleteCatIds = mutableSetOf<String>()
        collectDescendants(id, toDeleteCatIds)
        toDeleteCatIds.add(id)

        categories.removeAll { it.id in toDeleteCatIds }
        videos.removeAll { it.categoryId in toDeleteCatIds }
        saveInternal(context)
    }

    private fun collectDescendants(parentId: String, set: MutableSet<String>) {
        val children = categories.filter { it.parentId == parentId }
        for (child in children) {
            set.add(child.id)
            collectDescendants(child.id, set)
        }
    }

    @Synchronized
    fun addVideo(context: Context, title: String, url: String, categoryId: String): StreamVideo {
        load(context)
        val vid = addVideoInternal(title, url, categoryId)
        saveInternal(context)
        return vid
    }

    private fun addVideoInternal(title: String, url: String, categoryId: String): StreamVideo {
        val vid = StreamVideo(
            id = UUID.randomUUID().toString(),
            categoryId = categoryId,
            title = title.trim().ifEmpty { "Online Video" },
            url = url.trim()
        )
        videos.add(vid)
        return vid
    }

    @Synchronized
    fun editVideo(context: Context, id: String, newTitle: String, newUrl: String) {
        load(context)
        videos.find { it.id == id }?.let {
            it.title = newTitle.trim()
            it.url = newUrl.trim()
        }
        saveInternal(context)
    }

    @Synchronized
    fun deleteVideo(context: Context, id: String) {
        load(context)
        videos.removeAll { it.id == id }
        saveInternal(context)
    }

    fun detectFormatBadge(url: String): String {
        val lower = url.lowercase()
        return when {
            lower.contains(".m3u8") || lower.contains("hls") -> "HLS"
            lower.contains(".mpd") || lower.contains("dash") -> "DASH"
            lower.contains(".mp4") -> "MP4"
            lower.contains(".mkv") -> "MKV"
            lower.contains("rtsp://") -> "RTSP"
            else -> "STREAM"
        }
    }

    /**
     * Test-only helper: resets the in-memory singleton state so a test can
     * simulate a fresh app launch without leaking state across tests.
     */
    fun clearForTesting() {
        synchronized(this) {
            categories.clear()
            videos.clear()
            isLoaded = false
        }
    }
}
