package com.sinjartv.rstv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URI

class RemotePlaylistSourcesTest {
    @Test
    fun allRemotePlaylistSourcesUseHttpsAndApprovedHosts() {
        assertTrue(RemotePlaylistSources.playlists.isNotEmpty())
        (RemotePlaylistSources.playlists + RemotePlaylistSources.countryPackages(listOf("SY", "LB", "IQ", "US", "CA")) + RemotePlaylistSources.douboCountryPackages()).forEach { source ->
            val uri = URI(source.playlistUrl)
            assertTrue(source.id.isNotBlank())
            assertTrue(uri.scheme.equals("https", ignoreCase = true))
            assertTrue(uri.host.lowercase() in source.allowedHosts)
            assertFalse(uri.userInfo != null)
        }
    }

    @Test
    fun douboListsAllowOnlyKnownJsDelivrRedirectHosts() {
        val douboSources = RemotePlaylistSources.playlists.filter { it.id.startsWith("doubo-") } + RemotePlaylistSources.douboCountryPackages()
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
    fun catalogueIncludesIptvGlobalRegionalMovieAndSeriesPlaylists() {
        val ids = RemotePlaylistSources.playlists.map { it.id }.toSet()
        listOf(
            "doubo-world", "doubo-arabic", "doubo-turkey", "doubo-sports",
            "iptv-org-world", "iptv-org-arab", "iptv-org-turkey", "iptv-org-sports",
            "iptv-org-movies", "iptv-org-series", "iptv-org-europe",
            "iptv-org-western-europe", "iptv-org-east-asia",
            "iptv-org-north-america", "iptv-org-south-america",
            "iptv-org-latin-america", "iptv-org-us", "iptv-org-ca",
            "iptv-org-cn", "iptv-org-jp", "iptv-org-kr", "iptv-org-ru"
        ).forEach { assertTrue("Missing IPTV package: $it", it in ids) }
    }

    @Test
    fun countryPackagesSeparateArabCountriesAndCoverMajorRegions() {
        val packages = (RemotePlaylistSources.playlists + RemotePlaylistSources.countryPackages(
            listOf("SY", "LB", "IQ", "US", "CA", "DE", "FR", "BR", "JP", "ZA")
        )).associateBy { it.id }

        assertTrue("iptv-org-sy" in packages)
        assertTrue("iptv-org-lb" in packages)
        assertTrue("iptv-org-iq" in packages)
        assertEquals("IPTV-org — سوريا", packages.getValue("iptv-org-sy").name)
        assertEquals("IPTV-org — لبنان", packages.getValue("iptv-org-lb").name)
        assertEquals("IPTV-org — العراق", packages.getValue("iptv-org-iq").name)
        assertEquals("الدول العربية", packages.getValue("iptv-org-sy").packageGroup)
        assertEquals("أمريكا الشمالية", packages.getValue("iptv-org-us").packageGroup)
        assertEquals("أمريكا الشمالية", packages.getValue("iptv-org-ca").packageGroup)
        assertEquals("الدول الأوروبية", packages.getValue("iptv-org-de").packageGroup)
        assertEquals("شرق آسيا", packages.getValue("iptv-org-jp").packageGroup)
        assertEquals("أفريقيا", packages.getValue("iptv-org-za").packageGroup)
        assertEquals("باقات عربية مجمعة", RemotePlaylistSources.packageGroupFor("iptv-org-arab"))
        assertEquals("باقات عربية مجمعة", RemotePlaylistSources.packageGroupFor("doubo-arabic"))
        assertEquals("باقات تركيا", RemotePlaylistSources.packageGroupFor("doubo-turkey"))
    }

    @Test
    fun generatedCountryPackagesAreUniqueAndUseOfficialIptvOrgCountryPaths() {
        val packages = RemotePlaylistSources.countryPackages(
            listOf("SY", "LB", "IQ", "SY", "US", "CA", "DE", "JP", "BR")
        )
        assertEquals(packages.size, packages.map { it.id }.distinct().size)
        assertEquals(packages.size, packages.map { it.playlistUrl }.distinct().size)
        packages.forEach { source ->
            assertEquals("iptv-org/iptv", source.provider)
            assertTrue(source.playlistUrl.startsWith("https://iptv-org.github.io/iptv/countries/"))
            assertTrue(source.playlistUrl.endsWith(".m3u"))
        }
    }

    @Test
    fun douboCountryPackagesCoverAll128PublishedCountriesWithoutDuplicates() {
        val packages = RemotePlaylistSources.douboCountryPackages()
        assertEquals(128, packages.size)
        assertEquals(128, packages.map { it.id }.distinct().size)
        assertEquals(128, packages.map { it.playlistUrl }.distinct().size)

        val byId = packages.associateBy { it.id }
        listOf("sy", "lb", "iq", "us", "ca", "de", "fr", "gb", "tr").forEach { code ->
            assertTrue("Missing Doubo country: $code", "doubo-country-$code" in byId)
        }
        assertEquals("الدول العربية", byId.getValue("doubo-country-sy").packageGroup)
        assertEquals("الدول العربية", byId.getValue("doubo-country-lb").packageGroup)
        assertEquals("الدول العربية", byId.getValue("doubo-country-iq").packageGroup)
        assertEquals("أمريكا الشمالية", byId.getValue("doubo-country-us").packageGroup)
        assertEquals("أمريكا الشمالية", byId.getValue("doubo-country-ca").packageGroup)
        assertEquals("باقات تركيا", byId.getValue("doubo-country-tr").packageGroup)
        packages.forEach { source ->
            assertEquals("Doubo Public Channel Directory", source.provider)
            assertTrue(source.playlistUrl.startsWith("https://cdn.jsdelivr.net/gh/streamline-iptv/iptv-playlists@dist/countries/"))
            assertTrue(source.playlistUrl.endsWith(".m3u"))
            assertTrue("cdn.jsdelivr.net" in source.allowedHosts)
            assertFalse(source.attribution.isNullOrBlank())
        }
    }

    @Test
    fun groupedIndexesAreExplicitlySameProviderAndPortugueseListHasAttribution() {
        val sources = RemotePlaylistSources.playlists.associateBy { it.id }
        listOf(
            "iptv-org-country-index",
            "iptv-org-language-index",
            "iptv-org-category-index",
            "iptv-org-region-index"
        ).forEach { id ->
            assertTrue(sources.getValue(id).provider == "iptv-org/iptv")
        }
        val romaxa = sources.getValue("romaxa-world-auto-checked")
        assertTrue(romaxa.playlistUrl == "https://raw.githubusercontent.com/Romaxa55/world_ip_tv/master/output/index.m3u")
        assertTrue(romaxa.provider == "Romaxa55/world_ip_tv")
        val portuguese = sources.getValue("m3upt-portuguese")
        assertTrue(portuguese.playlistUrl.endsWith("/M3UPT.m3u"))
        assertFalse(portuguese.attribution.isNullOrBlank())
    }
}
