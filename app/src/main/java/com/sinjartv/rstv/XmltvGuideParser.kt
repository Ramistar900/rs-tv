package com.sinjartv.rstv

import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

internal data class GuideProgramme(
    val channelId: String,
    val title: String,
    val description: String?,
    val startMillis: Long,
    val endMillis: Long
)

internal object XmltvGuideParser {
    private const val MAX_PROGRAMMES = 20_000

    fun parse(content: String): List<GuideProgramme> {
        require(content.isNotBlank()) { "ملف دليل البرامج فارغ" }
        require(content.length <= 15_000_000) { "حجم دليل البرامج أكبر من الحد المسموح" }

        val factory = DocumentBuilderFactory.newInstance().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
        val document = factory.newDocumentBuilder().parse(
            ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))
        )
        val nodes = document.getElementsByTagName("programme")
        val result = ArrayList<GuideProgramme>(minOf(nodes.length, MAX_PROGRAMMES))

        for (index in 0 until minOf(nodes.length, MAX_PROGRAMMES)) {
            val element = nodes.item(index) as? Element ?: continue
            val channelId = element.getAttribute("channel").trim()
            val title = element.getElementsByTagName("title").item(0)?.textContent?.trim().orEmpty()
            val start = parseXmltvTime(element.getAttribute("start")) ?: continue
            val end = parseXmltvTime(element.getAttribute("stop")) ?: continue
            if (channelId.isBlank() || title.isBlank() || end <= start) continue
            val description = element.getElementsByTagName("desc").item(0)
                ?.textContent?.trim()?.takeIf(String::isNotBlank)
            result += GuideProgramme(channelId, title, description, start, end)
        }
        return result.sortedBy(GuideProgramme::startMillis)
    }

    private fun parseXmltvTime(raw: String): Long? {
        val value = raw.trim()
        if (value.length < 14) return null
        val pattern = if (value.length > 14) "yyyyMMddHHmmss Z" else "yyyyMMddHHmmss"
        val formatter = SimpleDateFormat(pattern, Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val position = ParsePosition(0)
        val parsed: Date = formatter.parse(value, position) ?: return null
        return parsed.time.takeIf { position.index == value.length }
    }
}
