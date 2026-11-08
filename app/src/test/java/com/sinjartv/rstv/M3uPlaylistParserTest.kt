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
        assertEquals("news.id", result.channels.single().tvgId)
        assertEquals("News", result.channels.single().group)
        assertEquals("https://cdn.example/logo.png", result.channels.single().logoUrl)
        assertEquals("https://cdn.example/live/index.m3u8", result.channels.single().streamUrl)
    }

    @Test
    fun parsesExplicitContentRatingMetadata() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 tvg-rating="TV-MA",Mature Drama
            https://cdn.example/mature.m3u8
        """.trimIndent()

        val channel = M3uPlaylistParser.parse(playlist).channels.single()

        assertEquals("TV-MA", channel.contentRating)
        assertEquals(AdultContentType.AGE_RATED, AdultContentPolicy.classify(channel))
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
    fun skipsPlatformWebPagesThatAreNotDirectMediaStreams() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,YouTube page
            https://www.youtube.com/@channel/live
            #EXTINF:-1,Twitch page
            https://player.twitch.tv/?channel=example
            #EXTINF:-1,Direct HLS stream
            https://cdn.example/live/index.m3u8
        """.trimIndent()

        val result = M3uPlaylistParser.parse(playlist)

        assertEquals(1, result.channels.size)
        assertEquals("Direct HLS stream", result.channels.single().name)
        assertEquals(2, result.skippedUnsafeLinks)
    }

    @Test
    fun rejectsCredentialBearingStreamUrls() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,Credential URL
            https://user:password@cdn.example/live.m3u8
        """.trimIndent()

        val result = M3uPlaylistParser.parse(playlist)

        assertTrue(result.channels.isEmpty())
        assertEquals(1, result.skippedUnsafeLinks)
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
    fun duplicateStreamRetainsMostRestrictiveAdultClassification() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1 group-title="News",Regular entry
            https://cdn.example/shared.m3u8
            #EXTINF:-1 group-title="XXX",Restricted duplicate
            https://cdn.example/shared.m3u8
        """.trimIndent()

        val channel = M3uPlaylistParser.parse(playlist).channels.single()

        assertEquals("Restricted duplicate", channel.name)
        assertEquals(AdultContentType.EXPLICIT, AdultContentPolicy.classify(channel))
    }

    @Test
    fun rejectsFilesWithoutM3uHeader() {
        val result = M3uPlaylistParser.parse("https://cdn.example/live.m3u8")

        assertFalse(result.isValidPlaylist)
        assertTrue(result.channels.isEmpty())
    }

    @Test
    fun readsPerChannelVlcHttpHeaders() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,Channel One
            #EXTVLCOPT:http-user-agent=ExamplePlayer/1.0
            #EXTVLCOPT:http-referrer=https://provider.example/
            https://cdn.example/one.m3u8
            #EXTINF:-1,Channel Two
            https://cdn.example/two.m3u8
        """.trimIndent()

        val result = M3uPlaylistParser.parse(playlist)

        assertEquals("ExamplePlayer/1.0", result.channels[0].httpHeaders["User-Agent"])
        assertEquals("https://provider.example/", result.channels[0].httpHeaders["Referer"])
        assertTrue(result.channels[1].httpHeaders.isEmpty())
    }

    @Test
    fun readsSupportedHeadersFromExtHttpJsonAndRejectsUnsafeHeaders() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,JSON headers
            #EXTHTTP:{"User-Agent":"Player/2","Origin":"https://provider.example","Cookie":"session=secret"}
            https://cdn.example/live.m3u8
        """.trimIndent()

        val headers = M3uPlaylistParser.parse(playlist).channels.single().httpHeaders

        assertEquals("Player/2", headers["User-Agent"])
        assertEquals("https://provider.example", headers["Origin"])
        assertFalse(headers.containsKey("Cookie"))
    }

    @Test
    fun keepsSameStreamWhenDifferentHeadersAreRequired() {
        val playlist = """
            #EXTM3U
            #EXTINF:-1,First profile
            #EXTVLCOPT:http-user-agent=PlayerA
            https://cdn.example/live.m3u8
            #EXTINF:-1,Second profile
            #EXTVLCOPT:http-user-agent=PlayerB
            https://cdn.example/live.m3u8
        """.trimIndent()

        val result = M3uPlaylistParser.parse(playlist)

        assertEquals(2, result.channels.size)
        assertEquals("PlayerA", result.channels[0].httpHeaders["User-Agent"])
        assertEquals("PlayerB", result.channels[1].httpHeaders["User-Agent"])
    }

}
