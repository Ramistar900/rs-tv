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
    fun douboListsAllowOnlyKnownJsDelivrRedirectHosts() {
        val douboSources = RemotePlaylistSources.playlists.filter { it.id.startsWith("doubo-") }
        assertTrue(douboSources.isNotEmpty())
        douboSources.forEach { source ->
            assertTrue("cdn.jsdelivr.net" in source.allowedHosts)
            assertTrue("fastly.jsdelivr.net" in source.allowedHosts)
            assertTrue("gcore.jsdelivr.net" in source.allowedHosts)
            assertTrue("testingcf.jsdelivr.net" in source.allowedHosts)
            assertFalse("untrusted.example" in source.allowedHosts)
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
        assertTrue("iptv-org-animation" in ids)
        assertTrue("iptv-org-auto" in ids)
        assertTrue("iptv-org-business" in ids)
        assertTrue("iptv-org-classic" in ids)
        assertTrue("iptv-org-comedy" in ids)
        assertTrue("iptv-org-cooking" in ids)
        assertTrue("iptv-org-culture" in ids)
        assertTrue("iptv-org-documentary" in ids)
        assertTrue("iptv-org-education" in ids)
        assertTrue("iptv-org-entertainment" in ids)
        assertTrue("iptv-org-family" in ids)
        assertTrue("iptv-org-general" in ids)
        assertTrue("iptv-org-kids" in ids)
        assertTrue("iptv-org-legislative" in ids)
        assertTrue("iptv-org-lifestyle" in ids)
        assertTrue("iptv-org-music" in ids)
        assertTrue("iptv-org-news" in ids)
        assertTrue("iptv-org-outdoor" in ids)
        assertTrue("iptv-org-public" in ids)
        assertTrue("iptv-org-relax" in ids)
        assertTrue("iptv-org-religious" in ids)
        assertTrue("iptv-org-science" in ids)
        assertTrue("iptv-org-travel" in ids)
        assertTrue("iptv-org-weather" in ids)
        assertTrue("m3upt-portuguese" in ids)
        assertTrue("romaxa-world-auto-checked" in ids)
        assertTrue("iptv-org-country-index" in ids)
        assertTrue("iptv-org-language-index" in ids)
        assertTrue("iptv-org-category-index" in ids)
        assertTrue("iptv-org-region-index" in ids)
    }

    @Test
    fun worldQuickImportBundleUsesKnownWorldwideArabicAndTurkishSources() {
        val knownIds = RemotePlaylistSources.playlists.map { it.id }.toSet()
        assertTrue(RemotePlaylistSources.worldLibraryBundleIds.isNotEmpty())
        assertTrue(RemotePlaylistSources.worldLibraryBundleIds.size == RemotePlaylistSources.worldLibraryBundleIds.distinct().size)
        RemotePlaylistSources.worldLibraryBundleIds.forEach { id ->
            assertTrue("Unknown world bundle source: $id", id in knownIds)
        }
    }

    @Test
    fun featuredRegionalAndCountryPackagesCoverRequestedMarkets() {
        val ids = RemotePlaylistSources.playlists.map { it.id }.toSet()
        val featured = setOf(
            "iptv-org-europe",
            "iptv-org-western-europe",
            "iptv-org-east-asia",
            "iptv-org-north-america",
            "iptv-org-south-america",
            "iptv-org-latin-america",
            "iptv-org-us",
            "iptv-org-ca",
            "iptv-org-cn",
            "iptv-org-jp",
            "iptv-org-kr",
            "iptv-org-ru",
            "iptv-org-br",
            "iptv-org-ar",
            "iptv-org-cl",
            "iptv-org-co",
            "iptv-org-mx",
            "iptv-org-pe",
            "iptv-org-uk",
            "iptv-org-de",
            "iptv-org-fr",
            "iptv-org-it",
            "iptv-org-es",
            "iptv-org-pt",
            "iptv-org-pl",
            "iptv-org-nl",
            "iptv-org-se",
            "iptv-org-no"
        )

        assertTrue(featured.all { it in ids })
        assertTrue(featured.all { it in RemotePlaylistSources.worldLibraryBundleIds })
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
