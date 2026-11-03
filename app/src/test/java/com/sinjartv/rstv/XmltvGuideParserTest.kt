package com.sinjartv.rstv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class XmltvGuideParserTest {
    @Test
    fun parsesProgrammesAndSortsByStartTime() {
        val xml = """
            <tv>
              <programme start="20261103130000 +0000" stop="20261103140000 +0000" channel="news">
                <title>Evening News</title><desc>Daily headlines</desc>
              </programme>
              <programme start="20261103120000 +0000" stop="20261103130000 +0000" channel="news">
                <title>World Report</title>
              </programme>
            </tv>
        """.trimIndent()

        val result = XmltvGuideParser.parse(xml)

        assertEquals(2, result.size)
        assertEquals("World Report", result.first().title)
        assertEquals("news", result.first().channelId)
        assertEquals("Daily headlines", result.last().description)
        assertTrue(result.first().endMillis > result.first().startMillis)
    }

    @Test
    fun skipsInvalidProgrammeRows() {
        val xml = """
            <tv>
              <programme start="bad" stop="also-bad" channel="x"><title>Bad</title></programme>
              <programme start="20261103120000 +0000" stop="20261103110000 +0000" channel="x"><title>Backwards</title></programme>
              <programme start="20261103120000 +0000" stop="20261103130000 +0000" channel="x"><title>Good</title></programme>
            </tv>
        """.trimIndent()

        val result = XmltvGuideParser.parse(xml)

        assertEquals(1, result.size)
        assertEquals("Good", result.single().title)
    }

    @Test(expected = Exception::class)
    fun rejectsDocumentTypeDeclarations() {
        XmltvGuideParser.parse(
            """<!DOCTYPE tv [<!ENTITY x SYSTEM "file:///etc/passwd">]><tv><programme channel="x" start="20261103120000" stop="20261103130000"><title>&x;</title></programme></tv>"""
        )
    }
}
