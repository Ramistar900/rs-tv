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
    fun adultClassificationWinsWhenSourcesDisagreeAboutSameStream() {
        val regular = RemoteSourceSnapshot(
            id = "regular",
            name = "Regular source",
            provider = "Provider",
            attribution = null,
            content = """
                #EXTM3U
                #EXTINF:-1 group-title="Movies",Movie Channel
                https://cdn.example/shared.m3u8
            """.trimIndent()
        )
        val adult = RemoteSourceSnapshot(
            id = "adult",
            name = "Adult source",
            provider = "Provider",
            attribution = null,
            content = """
                #EXTM3U
                #EXTINF:-1 group-title="XXX",Restricted Channel
                https://cdn.example/shared.m3u8
            """.trimIndent()
        )

        val merged = RemotePlaylistMerger.merge(listOf(regular, adult))

        assertEquals(1, merged.size)
        assertEquals(AdultContentType.EXPLICIT, AdultContentPolicy.classify(merged.single()))
        assertEquals("Restricted Channel", merged.single().name)
    }

    @Test
    fun infersCountryFromCountryPackageWhenPlaylistEntryHasNoCountryTag() {
        val snapshot = RemoteSourceSnapshot(
            id = "iptv-org-jp",
            name = "IPTV-org — اليابان",
            provider = "iptv-org/iptv",
            attribution = null,
            content = """
                #EXTM3U
                #EXTINF:-1 group-title="News",Example Japan News
                https://jp.example/live.m3u8
            """.trimIndent()
        )

        val merged = RemotePlaylistMerger.merge(listOf(snapshot))

        assertEquals(1, merged.size)
        assertEquals("اليابان", merged.single().country)
    }

    @Test
    fun infersArabicCountryFromDynamicCountryPackageId() {
        val snapshot = RemoteSourceSnapshot(
            id = "iptv-org-sy",
            name = "IPTV-org — سوريا",
            provider = "iptv-org/iptv",
            attribution = null,
            content = """
                #EXTM3U
                #EXTINF:-1 group-title="News",Example Syria News
                https://sy.example/live.m3u8
            """.trimIndent()
        )

        val merged = RemotePlaylistMerger.merge(listOf(snapshot))

        assertEquals(1, merged.size)
        assertEquals("سوريا", merged.single().country)
    }

    @Test
    fun infersArabicCountryFromDouboCountryPackageId() {
        val snapshot = RemoteSourceSnapshot(
            id = "doubo-country-sy",
            name = "Doubo — سوريا",
            provider = "Doubo Public Channel Directory",
            attribution = "CC BY 4.0",
            content = """
                #EXTM3U
                #EXTINF:-1 group-title="News",Example Syria News
                https://sy.example/live.m3u8
            """.trimIndent()
        )

        val merged = RemotePlaylistMerger.merge(listOf(snapshot))

        assertEquals(1, merged.size)
        assertEquals("سوريا", merged.single().country)
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
    fun keepsDifferentHeaderProfilesAsSeparateSources() {
        val first = RemoteSourceSnapshot(
            id = "one", name = "One", provider = "Provider", attribution = null,
            content = """
                #EXTM3U
                #EXTINF:-1,First
                #EXTVLCOPT:http-user-agent=PlayerA
                https://cdn.example/live.m3u8
            """.trimIndent()
        )
        val second = RemoteSourceSnapshot(
            id = "two", name = "Two", provider = "Provider", attribution = null,
            content = """
                #EXTM3U
                #EXTINF:-1,Second
                #EXTVLCOPT:http-user-agent=PlayerB
                https://cdn.example/live.m3u8
            """.trimIndent()
        )

        val merged = RemotePlaylistMerger.merge(listOf(first, second))

        assertEquals(2, merged.size)
        assertEquals("PlayerA", merged[0].httpHeaders["User-Agent"])
        assertEquals("PlayerB", merged[1].httpHeaders["User-Agent"])
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
