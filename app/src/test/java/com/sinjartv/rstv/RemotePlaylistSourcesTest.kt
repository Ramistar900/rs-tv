package com.sinjartv.rstv

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class RemotePlaylistSourcesTest {
    @Test
    fun allRemotePlaylistSourcesUseHttpsAndApprovedHosts() {
        assertTrue(RemotePlaylistSources.playlists.isNotEmpty())
        RemotePlaylistSources.playlists.forEach { source ->
            val uri = URI(source.playlistUrl)
            assertTrue(source.id.isNotBlank())
            assertTrue(uri.scheme.equals("https", ignoreCase = true))
            assertTrue(uri.host.lowercase() in source.allowedHosts)
            assertFalse(uri.userInfo != null)
        }
    }

    @Test
    fun catalogueIncludesWorldwideArabicAndTurkishLists() {
        val ids = RemotePlaylistSources.playlists.map { it.id }.toSet()
        assertTrue("doubo-world" in ids)
        assertTrue("doubo-turkey" in ids)
        assertTrue("doubo-arabic" in ids)
        assertTrue("iptv-org-world" in ids)
        assertTrue("iptv-org-arab" in ids)
        assertTrue("iptv-org-turkey" in ids)
        assertTrue("m3upt-portuguese" in ids)
        assertTrue("iptv-org-country-index" in ids)
        assertTrue("iptv-org-language-index" in ids)
        assertTrue("iptv-org-category-index" in ids)
        assertTrue("iptv-org-region-index" in ids)
    }

    @Test
    fun groupedIndexesAreExplicitlySameProviderAndPortugueseListHasAttribution() {
        val sources = RemotePlaylistSources.playlists.associateBy { it.id }
        val indexes = listOf(
            "iptv-org-country-index",
            "iptv-org-language-index",
            "iptv-org-category-index",
            "iptv-org-region-index"
        )
        indexes.forEach { id ->
            assertTrue(sources.getValue(id).provider == "iptv-org/iptv")
        }
        val portuguese = sources.getValue("m3upt-portuguese")
        assertTrue(portuguese.playlistUrl.endsWith("/M3UPT.m3u"))
        assertTrue(!portuguese.attribution.isNullOrBlank())
    }

    @Test
    fun officialDestinationsUseHttps() {
        assertTrue(RemotePlaylistSources.officialDestinations.isNotEmpty())
        RemotePlaylistSources.officialDestinations.forEach { destination ->
            assertTrue(destination.url.startsWith("https://"))
        }
    }
}
