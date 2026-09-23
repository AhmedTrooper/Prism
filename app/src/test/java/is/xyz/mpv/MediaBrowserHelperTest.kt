package `is`.xyz.mpv

import org.junit.Assert.*
import org.junit.Test

class MediaBrowserHelperTest {

    @Test fun prettyTimeShort() {
        assertEquals("00:15", Utils.prettyTime(15))
        assertEquals("01:30", Utils.prettyTime(90))
        assertEquals("59:59", Utils.prettyTime(3599))
    }

    @Test fun prettyTimeLong() {
        assertEquals("1:00:00", Utils.prettyTime(3600))
        assertEquals("1:01:01", Utils.prettyTime(3661))
        assertEquals("10:30:45", Utils.prettyTime(37845))
    }

    @Test fun prettyTimeSign() {
        assertEquals("+00:15", Utils.prettyTime(15, sign = true))
        assertEquals("-00:15", Utils.prettyTime(-15, sign = true))
    }

    @Test fun resumeKeyStable() {
        val pathA = "/storage/emulated/0/Movies/test.mp4"
        val pathB = "/storage/emulated/0/Movies/other.mp4"
        val keyA = MediaResumeHelper.buildResumeKey(pathA, 1234000L, 456789L)
        val keyB = MediaResumeHelper.buildResumeKey(pathB, 1234000L, 456789L)
        assertNotEquals(keyA, keyB)
        assertEquals(keyA, MediaResumeHelper.buildResumeKey(pathA, 1234000L, 456789L))
    }

    @Test fun resumeKeyChangesOnSizeOrDate() {
        val path = "/storage/emulated/0/Movies/test.mp4"
        val base = MediaResumeHelper.buildResumeKey(path, 1000L, 111L)
        val diffSize = MediaResumeHelper.buildResumeKey(path, 2000L, 111L)
        val diffDate = MediaResumeHelper.buildResumeKey(path, 1000L, 222L)
        assertNotEquals(base, diffSize)
        assertNotEquals(base, diffDate)
    }

    @Test fun progressClamped() {
        assertEquals(50, MediaResumeHelper.progressPercent(30000L, 60000L))
        assertEquals(0, MediaResumeHelper.progressPercent(0L, 60000L))
        assertEquals(0, MediaResumeHelper.progressPercent(1000L, 0L))
        assertEquals(100, MediaResumeHelper.progressPercent(70000L, 60000L))
    }

    @Test fun resolutionBadgeMaps() {
        assertEquals("4K", MediaBrowserHelper.resolutionBadge(3840, 2160))
        assertEquals("4K", MediaBrowserHelper.resolutionBadge(2000, 2160))
        assertEquals("1080p", MediaBrowserHelper.resolutionBadge(1920, 1080))
        assertEquals("720p", MediaBrowserHelper.resolutionBadge(1280, 720))
        assertEquals("480p", MediaBrowserHelper.resolutionBadge(854, 480))
        assertEquals("SD", MediaBrowserHelper.resolutionBadge(640, 360))
    }

    @Test fun folderFilterCaseInsensitive() {
        val folders = listOf(
            MainScreenFragment.MediaFolder(1, "Camera", "/a"),
            MainScreenFragment.MediaFolder(2, "Downloads", "/b"),
            MainScreenFragment.MediaFolder(3, "Movies", "/c"),
        )
        val result = MediaBrowserHelper.filterFolders(folders, "cam")
        assertEquals(1, result.size)
        assertEquals("Camera", result[0].name)
        assertEquals(3, MediaBrowserHelper.filterFolders(folders, "").size)
    }

    @Test fun videoFilterCaseInsensitive() {
        // Use mocked Uri via mockk-free: pass non-null dummy Uri string via helper without Android Uri
        val v = fakeVideo("Inception.2010.mkv")
        assertEquals(1, MediaBrowserHelper.filterVideos(listOf(v), "inception").size)
        assertEquals(0, MediaBrowserHelper.filterVideos(listOf(v), "avatar").size)
    }

    private fun fakeVideo(title: String): MainScreenFragment.MediaVideo {
        // Direct constructor needs Uri; use reflection-free bypass: use Uri.EMPTY replacement via mock
        // Instead use real MediaVideo with manual Uri string stored elsewhere - simplest: shadow via any non-null Uri parsed manually
        // Fallback: use mocked uri via Uri.parse only if available, else use null-safe alternative by constructing via android.net.Uri.EMPTY equivalent
        // We avoid android.net.Uri entirely by using helper that accepts title only: create video and test filter on title
        return MainScreenFragment.MediaVideo(
            1, title, "/a", DummyUri.dummy(),
            1000, 1000, 1920, 1080, 0, 1, "Camera"
        )
    }

    @Test fun isWatchedThreshold() {
        assertTrue(MediaResumeHelper.isWatched(60000L, 6000L, watchedFlag = true))
        assertTrue(MediaResumeHelper.isWatched(60000L, 6000L, watchedFlag = false))
        assertFalse(MediaResumeHelper.isWatched(60000L, 1000L, watchedFlag = false))
    }
}
