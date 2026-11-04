package com.sinjartv.rstv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemotePlaylistMergerTest {
    @Test
    fun mergesEquivalentUrlsAndRetainsSourceProvenance() {
        val snapshots = listOf(
            RemoteSourceSnapshot(
                id = "one",
                name = "Source One",
                provider = "Provider One",
                attribution = null,
                content = """
                    #EXTM3U
                    #EXTINF:-1 tvg-id="news.id",News
                    https://CDN.example:443/live.m3u8#first
                """.trimIndent()
            ),
            RemoteSourceSnapshot(
                id = "two",
                name = "Source Two",
                provider = "Provider Two",
                attribution = "Attribution",
                content = """
                    #EXTM3U
                    #EXTINF:-1 tvg-logo="https://cdn.example/logo.png" group-title="News",News HD
                    https://cdn.example/live.m3u8#second
                """.trimIndent()
            )
        )

        val merged = RemotePlaylistMerger.merge(snapshots)

        assertEquals(1, merged.size)
        assertEquals(setOf("one", "two"), merged.single().sourceIds)
        assertEquals(setOf("Source One", "Source Two"), merged.single().sourceNames)
        assertEquals(setOf("Provider One", "Provider Two"), merged.single().sourceProviders)
        assertEquals("news.id", merged.single().tvgId)
        assertEquals("News", merged.single().group)
        assertEquals("https://cdn.example/logo.png", merged.single().logoUrl)
    }

    @Test
    fun keepsDifferentQueryTokensAsSeparateStreams() {
        val snapshots = listOf(
            RemoteSourceSnapshot(
                id = "one",
                name = "Source One",
                provider = "Provider One",
                attribution = null,
                content = """
                    #EXTM3U
                    #EXTINF:-1,Token One
                    https://cdn.example/live.m3u8?token=one
                    #EXTINF:-1,Token Two
                    https://cdn.example/live.m3u8?token=two
                """.trimIndent()
            )
        )

        assertEquals(2, RemotePlaylistMerger.merge(snapshots).size)
    }

    @Test
    fun stripsFragmentButPreservesQueryWhenBuildingCanonicalKey() {
        assertEquals(
            "https://cdn.example:8443/live.m3u8?token=abc",
            RemotePlaylistMerger.canonicalStreamKey(
                "HTTPS://CDN.EXAMPLE:8443/live.m3u8?token=abc#player"
            )
        )
        assertTrue(
            RemotePlaylistMerger.canonicalStreamKey("https://cdn.example/live.m3u8?token=a") !=
                RemotePlaylistMerger.canonicalStreamKey("https://cdn.example/live.m3u8?token=b")
        )
    }
}
