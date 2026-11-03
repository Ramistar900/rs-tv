package com.sinjartv.rstv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class M3uPlaylistParserTest {

    @Test
    fun parsesHttpsChannelAndMetadata() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 tvg-id="news.id" tvg-name="News HD" tvg-logo="https://cdn.example/logo.png" group-title="News",News Channel
            https://cdn.example/live/index.m3u8
        """.trimIndent()

        val result = M3uPlaylistParser.parse(playlist)

        assertTrue(result.isValidPlaylist)
        assertEquals(1, result.channels.size)
        assertEquals("News HD", result.channels.single().name)
        assertEquals("News", result.channels.single().group)
        assertEquals("https://cdn.example/logo.png", result.channels.single().logoUrl)
        assertEquals("https://cdn.example/live/index.m3u8", result.channels.single().streamUrl)
    }

    @Test
    fun rejectsNonHttpsStreamUrls() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,Insecure HTTP
            http://example.com/live.m3u8
            #EXTINF:-1,Local file
            file:///storage/emulated/0/live.m3u8
        """.trimIndent()

        val result = M3uPlaylistParser.parse(playlist)

        assertTrue(result.isValidPlaylist)
        assertTrue(result.channels.isEmpty())
        assertEquals(2, result.skippedUnsafeLinks)
    }

    @Test
    fun removesDuplicateStreamUrls() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,First name
            https://cdn.example/live.m3u8
            #EXTINF:-1,Second name
            https://cdn.example/live.m3u8
        """.trimIndent()

        val result = M3uPlaylistParser.parse(playlist)

        assertEquals(1, result.channels.size)
        assertEquals(1, result.invalidEntries)
    }

    @Test
    fun rejectsFilesWithoutM3uHeader() {
        val result = M3uPlaylistParser.parse("https://cdn.example/live.m3u8")

        assertFalse(result.isValidPlaylist)
        assertTrue(result.channels.isEmpty())
    }
}
