package com.ahmedtrooper.prism

import android.content.Context
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File

/**
 * Tests for the OnlineStreamManager singleton. Uses a TemporaryFolder-backed
 * mock Context so the JSON persistence layer exercises the real file I/O path.
 *
 * Note: OnlineStreamManager is an `object` (singleton) with mutable state.
 * We clear it between tests to avoid cross-test pollution.
 */
class OnlineStreamManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockContext: Context
    private lateinit var streamsFile: File

    @Before
    fun setUp() {
        mockContext = mock(Context::class.java)
        streamsFile = File(tempFolder.root, "online_streams.json")
        `when`(mockContext.filesDir).thenReturn(tempFolder.root)
        // Reset internal state by deleting any state from a previous test
        // (no public API for this; we just clear the file so load() reseeds)
        OnlineStreamManager.clearForTesting()
    }

    @After
    fun tearDown() {
        OnlineStreamManager.clearForTesting()
    }

    @Test
    fun loadSeedsInitialStreamsWhenFileMissing() {
        OnlineStreamManager.load(mockContext)

        val rootCats = OnlineStreamManager.getCategories(null)
        assertTrue("Should seed at least one root category", rootCats.isNotEmpty())

        // At least one of the seeded categories should be IPTV & Live Channels
        assertTrue(
            "Seeded root categories must include 'IPTV & Live Channels'",
            rootCats.any { it.name == "IPTV & Live Channels" }
        )
        // Persisted to disk after seeding
        assertTrue("Seed must write the JSON file to disk", streamsFile.exists())
    }

    @Test
    fun loadReusesExistingFileWithoutReseeding() {
        // First load seeds and writes file
        OnlineStreamManager.load(mockContext)
        val firstLoadRootCats = OnlineStreamManager.getCategories(null)
        val firstLoadCount = firstLoadRootCats.size
        assertTrue(firstLoadCount > 0)

        // Capture current state
        val firstSeedCatNames = firstLoadRootCats.map { it.name }.toSet()

        // Second load should NOT re-seed — the file already exists.
        // It should parse the existing JSON back into memory.
        OnlineStreamManager.load(mockContext)
        val secondLoadRootCats = OnlineStreamManager.getCategories(null)
        val secondLoadNames = secondLoadRootCats.map { it.name }.toSet()

        assertEquals("Re-loading should preserve seeded category names",
            firstSeedCatNames, secondLoadNames)
    }

    @Test
    fun addCategoryCreatesEntryWithUniqueId() {
        OnlineStreamManager.load(mockContext)
        val initialRootCount = OnlineStreamManager.getCategories(null).size

        val cat1 = OnlineStreamManager.addCategory(mockContext, "Sports", null)
        val cat2 = OnlineStreamManager.addCategory(mockContext, "Music", null)

        assertNotEquals("Each category gets a unique ID", cat1.id, cat2.id)
        assertEquals("Sports", cat1.name)
        assertEquals("Music", cat2.name)
        assertNull("Root category has no parent", cat1.parentId)

        val after = OnlineStreamManager.getCategories(null)
        assertEquals(initialRootCount + 2, after.size)
        assertTrue(after.any { it.id == cat1.id })
        assertTrue(after.any { it.id == cat2.id })
    }

    @Test
    fun addCategoryWithParentAssignsParentId() {
        OnlineStreamManager.load(mockContext)
        val parent = OnlineStreamManager.addCategory(mockContext, "Entertainment", null)
        val child = OnlineStreamManager.addCategory(mockContext, "Comedy", parent.id)

        assertEquals(parent.id, child.parentId)

        val childrenOfParent = OnlineStreamManager.getCategories(parent.id)
        assertEquals(1, childrenOfParent.size)
        assertEquals(child.id, childrenOfParent[0].id)
    }

    @Test
    fun getVideosForCategoryReturnsOnlyMatching() {
        OnlineStreamManager.load(mockContext)
        val cat = OnlineStreamManager.addCategory(mockContext, "Test", null)

        OnlineStreamManager.addVideo(mockContext, "Video A", "https://x/a.m3u8", cat.id)
        OnlineStreamManager.addVideo(mockContext, "Video B", "https://x/b.mp4", cat.id)

        val otherCat = OnlineStreamManager.addCategory(mockContext, "Other", null)
        OnlineStreamManager.addVideo(mockContext, "Video C", "https://x/c.mp4", otherCat.id)

        val videos = OnlineStreamManager.getVideos(cat.id)
        assertEquals(2, videos.size)
        assertTrue(videos.any { it.title == "Video A" })
        assertTrue(videos.any { it.title == "Video B" })
        assertFalse("Should not include videos from another category",
            videos.any { it.title == "Video C" })
    }

    @Test
    fun renameCategoryUpdatesName() {
        OnlineStreamManager.load(mockContext)
        val cat = OnlineStreamManager.addCategory(mockContext, "Old Name", null)

        OnlineStreamManager.renameCategory(mockContext, cat.id, "New Name")

        val fetched = OnlineStreamManager.getCategory(cat.id)
        assertNotNull(fetched)
        assertEquals("New Name", fetched!!.name)
    }

    @Test
    fun deleteCategoryRemovesIt() {
        OnlineStreamManager.load(mockContext)
        val cat = OnlineStreamManager.addCategory(mockContext, "Doomed", null)
        assertNotNull(OnlineStreamManager.getCategory(cat.id))

        OnlineStreamManager.deleteCategory(mockContext, cat.id)

        assertNull("Category should be gone after delete",
            OnlineStreamManager.getCategory(cat.id))
    }

    @Test
    fun deleteCategoryRemovesDescendantsAndVideos() {
        OnlineStreamManager.load(mockContext)
        val root = OnlineStreamManager.addCategory(mockContext, "Root", null)
        val child = OnlineStreamManager.addCategory(mockContext, "Child", root.id)
        val grand = OnlineStreamManager.addCategory(mockContext, "Grandchild", child.id)
        val otherRoot = OnlineStreamManager.addCategory(mockContext, "Unrelated", null)

        OnlineStreamManager.addVideo(mockContext, "V1", "https://x/v1.mp4", root.id)
        OnlineStreamManager.addVideo(mockContext, "V2", "https://x/v2.mp4", child.id)
        OnlineStreamManager.addVideo(mockContext, "V3", "https://x/v3.mp4", otherRoot.id)

        OnlineStreamManager.deleteCategory(mockContext, root.id)

        // Root and all descendants removed
        assertNull(OnlineStreamManager.getCategory(root.id))
        assertNull(OnlineStreamManager.getCategory(child.id))
        assertNull(OnlineStreamManager.getCategory(grand.id))
        // Unrelated root still present
        assertNotNull(OnlineStreamManager.getCategory(otherRoot.id))
        // Videos in deleted tree removed
        assertTrue("V1 should be deleted", OnlineStreamManager.getVideos(root.id).isEmpty())
        assertTrue("V2 should be deleted", OnlineStreamManager.getVideos(child.id).isEmpty())
        // Unrelated video still present
        assertEquals(1, OnlineStreamManager.getVideos(otherRoot.id).size)
    }

    @Test
    fun editVideoChangesTitleAndUrl() {
        OnlineStreamManager.load(mockContext)
        val cat = OnlineStreamManager.addCategory(mockContext, "Cat", null)
        val vid = OnlineStreamManager.addVideo(mockContext, "Old", "https://x/old", cat.id)

        OnlineStreamManager.editVideo(mockContext, vid.id, "New Title", "https://x/new.m3u8")

        val videos = OnlineStreamManager.getVideos(cat.id)
        assertEquals(1, videos.size)
        assertEquals("New Title", videos[0].title)
        assertEquals("https://x/new.m3u8", videos[0].url)
    }

    @Test
    fun deleteVideoRemovesOnlyThatVideo() {
        OnlineStreamManager.load(mockContext)
        val cat = OnlineStreamManager.addCategory(mockContext, "Cat", null)
        val v1 = OnlineStreamManager.addVideo(mockContext, "V1", "https://x/1", cat.id)
        val v2 = OnlineStreamManager.addVideo(mockContext, "V2", "https://x/2", cat.id)

        OnlineStreamManager.deleteVideo(mockContext, v1.id)

        val remaining = OnlineStreamManager.getVideos(cat.id)
        assertEquals(1, remaining.size)
        assertEquals(v2.id, remaining[0].id)
    }

    @Test
    fun countItemsReturnsCorrectTuple() {
        OnlineStreamManager.load(mockContext)
        val cat = OnlineStreamManager.addCategory(mockContext, "Hub", null)
        OnlineStreamManager.addCategory(mockContext, "Sub 1", cat.id)
        OnlineStreamManager.addCategory(mockContext, "Sub 2", cat.id)
        OnlineStreamManager.addVideo(mockContext, "V", "https://x/v", cat.id)
        OnlineStreamManager.addVideo(mockContext, "V2", "https://x/v2", cat.id)

        val (folders, videos) = OnlineStreamManager.countItems(cat.id)
        assertEquals(2, folders)
        assertEquals(2, videos)
    }

    @Test
    fun detectFormatBadgeMapsKnownExtensions() {
        assertEquals("HLS", OnlineStreamManager.detectFormatBadge("https://x/stream.m3u8"))
        assertEquals("HLS", OnlineStreamManager.detectFormatBadge("https://x/HLS/path"))
        assertEquals("DASH", OnlineStreamManager.detectFormatBadge("https://x/manifest.mpd"))
        assertEquals("DASH", OnlineStreamManager.detectFormatBadge("https://x/dash/manifest"))
        assertEquals("MP4", OnlineStreamManager.detectFormatBadge("https://x/video.mp4"))
        assertEquals("MKV", OnlineStreamManager.detectFormatBadge("https://x/video.mkv"))
        assertEquals("RTSP", OnlineStreamManager.detectFormatBadge("rtsp://camera/stream"))
        assertEquals("STREAM", OnlineStreamManager.detectFormatBadge("https://x/something"))
    }

    @Test
    fun persistenceSurvivesReread() {
        // Write some data
        OnlineStreamManager.load(mockContext)
        val cat = OnlineStreamManager.addCategory(mockContext, "Persisted", null)
        OnlineStreamManager.addVideo(mockContext, "Stream", "https://x/s.m3u8", cat.id)

        // Simulate fresh app launch: clear in-memory state but keep the file
        OnlineStreamManager.clearForTesting()
        OnlineStreamManager.load(mockContext)

        // Data should be back from disk
        val restored = OnlineStreamManager.getCategory(cat.id)
        assertNotNull("Category must survive a re-read", restored)
        assertEquals("Persisted", restored!!.name)
        val restoredVideos = OnlineStreamManager.getVideos(cat.id)
        assertEquals(1, restoredVideos.size)
        assertEquals("Stream", restoredVideos[0].title)
        assertEquals("https://x/s.m3u8", restoredVideos[0].url)
    }
}
