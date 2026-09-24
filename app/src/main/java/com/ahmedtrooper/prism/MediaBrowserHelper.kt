package com.ahmedtrooper.prism

import java.util.Locale

object MediaResumeHelper {
    fun buildResumeKey(path: String, sizeBytes: Long, dateModified: Long): String {
        val safe = path.hashCode().toString(36) + "_" + sizeBytes + "_" + dateModified
        return "resume_v2_$safe"
    }

    fun progressPercent(lastPosMs: Long, durationMs: Long): Int {
        if (durationMs <= 0L || lastPosMs <= 0L) return 0
        return ((lastPosMs * 100L) / durationMs).toInt().coerceIn(0, 100)
    }

    fun isWatched(durationMs: Long, lastPosMs: Long, watchedFlag: Boolean): Boolean {
        if (watchedFlag) return true
        // Threshold: watched if resume beyond 5s (legacy heuristic)
        return lastPosMs > 5000L
    }
}

object MediaBrowserHelper {
    fun resolutionBadge(width: Int, height: Int): String = when {
        height >= 2160 || width >= 3840 -> "4K"
        height >= 1080 || width >= 1920 -> "1080p"
        height >= 720 || width >= 1280 -> "720p"
        height >= 480 || width >= 854 -> "480p"
        else -> "SD"
    }

    fun filterFolders(
        folders: List<MainScreenFragment.MediaFolder>,
        query: String
    ): List<MainScreenFragment.MediaFolder> {
        if (query.isEmpty()) return folders
        val q = query.lowercase(Locale.ROOT)
        return folders.filter { it.name.lowercase(Locale.ROOT).contains(q) }
    }

    fun filterVideos(
        videos: List<MainScreenFragment.MediaVideo>,
        query: String
    ): List<MainScreenFragment.MediaVideo> {
        if (query.isEmpty()) return videos
        val q = query.lowercase(Locale.ROOT)
        return videos.filter { it.title.lowercase(Locale.ROOT).contains(q) }
    }
}
