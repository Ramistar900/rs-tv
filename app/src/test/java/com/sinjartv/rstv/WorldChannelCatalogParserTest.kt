package com.sinjartv.rstv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldChannelCatalogParserTest {
    @Test
    fun parsesWorldwideCandidateMetadataWithoutExposingStreamUrls() {
        val json = """
            {
              "schema_version": 2,
              "records": [
                {
                  "id": "sample.tr",
                  "name": "Sample Türkiye",
                  "alternative_names": ["Sample TV"],
                  "country": {"code": "TR", "name": "Türkiye", "flag": "🇹🇷"},
                  "languages": [{"code": "tur", "name": "Turkish"}],
                  "categories": ["news"],
                  "category_names": ["News"],
                  "website": "https://example.org/live",
                  "streams": [
                    {"url": "https://stream.example.org/live.m3u8"}
                  ],
                  "status": "candidate",
                  "rights_review_status": "not_reviewed"
                }
              ]
            }
        """.trimIndent()

        val parsed = WorldChannelCatalogParser.parse(json)

        assertEquals(1, parsed.size)
        assertEquals("sample.tr", parsed.single().id)
        assertEquals("Türkiye", parsed.single().countryName)
        assertEquals("🇹🇷", parsed.single().countryFlag)
        assertEquals(listOf("Turkish"), parsed.single().languageLabels)
        assertEquals(listOf("news"), parsed.single().categoryIds)
        assertEquals(1, parsed.single().candidateStreamCount)
        assertEquals("https://example.org/live", parsed.single().websiteUrl)
        assertFalse(parsed.single().toString().contains("stream.example.org"))
    }

    @Test
    fun keepsUnknownCountryNeutralAndRejectsUnsafeWebsiteUrl() {
        val json = """
            {
              "records": [{
                "id": "global.unknown",
                "name": "Global Channel",
                "country": {"code": "ZZ", "name": "غير محدد", "flag": "🌐"},
                "website": "http://example.org",
                "streams": [],
                "status": "candidate",
                "rights_review_status": "not_reviewed"
              }]
            }
        """.trimIndent()

        val channel = WorldChannelCatalogParser.parse(json).single()

        assertEquals("ZZ", channel.countryCode)
        assertEquals("🌐", channel.countryFlag)
        assertEquals(null, channel.websiteUrl)
    }

    @Test
    fun hidesRecordsThatHaveNotPassedCandidateReviewState() {
        val json = """
            {
              "records": [
                {
                  "id": "approved.channel",
                  "name": "Approved",
                  "status": "approved",
                  "rights_review_status": "reviewed"
                },
                {
                  "id": "reviewed.channel",
                  "name": "Reviewed",
                  "status": "candidate",
                  "rights_review_status": "reviewed"
                }
              ]
            }
        """.trimIndent()

        assertTrue(WorldChannelCatalogParser.parse(json).isEmpty())
    }

    @Test
    fun removesDuplicateChannelIds() {
        val json = """
            {
              "records": [
                {"id":"same.id","name":"First","status":"candidate","rights_review_status":"not_reviewed"},
                {"id":"same.id","name":"Second","status":"candidate","rights_review_status":"not_reviewed"}
              ]
            }
        """.trimIndent()

        val parsed = WorldChannelCatalogParser.parse(json)

        assertEquals(1, parsed.size)
        assertEquals("First", parsed.single().name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsCatalogWithoutRecords() {
        WorldChannelCatalogParser.parse("""{"schema_version":2}""")
    }
}
