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
    }

    @Test
    fun officialDestinationsUseHttps() {
        assertTrue(RemotePlaylistSources.officialDestinations.isNotEmpty())
        RemotePlaylistSources.officialDestinations.forEach { destination ->
            assertTrue(destination.url.startsWith("https://"))
        }
    }
}
