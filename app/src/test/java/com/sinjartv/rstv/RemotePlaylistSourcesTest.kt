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
        assertTrue("doubo-sports" in ids)
        assertTrue("iptv-org-world" in ids)
        assertTrue("iptv-org-arab" in ids)
        assertTrue("iptv-org-turkey" in ids)
        assertTrue("iptv-org-sports" in ids)
        assertTrue("iptv-org-movies" in ids)
        assertTrue("iptv-org-series" in ids)
        assertTrue("m3upt-portuguese" in ids)
        assertTrue("romaxa-world-auto-checked" in ids)
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
        val romaxa = sources.getValue("romaxa-world-auto-checked")
        assertTrue(romaxa.playlistUrl == "https://raw.githubusercontent.com/Romaxa55/world_ip_tv/master/output/index.m3u")
        assertTrue(romaxa.provider == "Romaxa55/world_ip_tv")
        val portuguese = sources.getValue("m3upt-portuguese")
        assertTrue(portuguese.playlistUrl.endsWith("/M3UPT.m3u"))
        assertTrue(!portuguese.attribution.isNullOrBlank())
    }


    @Test
    fun officialSportsAndFilmDestinationsAreCategorizedSeparately() {
        val destinations = RemotePlaylistSources.officialDestinations.associateBy { it.id }
        assertTrue(destinations.getValue("fifa-plus").category == OfficialDestinationCategory.SPORTS)
        assertTrue(destinations.getValue("eurovision-sport").category == OfficialDestinationCategory.SPORTS)
        assertTrue(destinations.getValue("red-bull-tv").category == OfficialDestinationCategory.SPORTS)
        assertTrue(destinations.getValue("trt-spor").category == OfficialDestinationCategory.SPORTS)
        assertTrue(destinations.getValue("rakuten-tv-free").category == OfficialDestinationCategory.MOVIES)
        assertTrue(destinations.getValue("pluto-tv").category == OfficialDestinationCategory.MOVIES)
        assertTrue(destinations.getValue("internet-archive-movies").category == OfficialDestinationCategory.MOVIES)
        assertTrue(destinations.values.any { it.category == OfficialDestinationCategory.GENERAL })
    }

    @Test
    fun officialDestinationsUseHttps() {
        assertTrue(RemotePlaylistSources.officialDestinations.isNotEmpty())
        RemotePlaylistSources.officialDestinations.forEach { destination ->
            assertTrue(destination.url.startsWith("https://"))
        }
    }
}
