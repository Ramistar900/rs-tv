package com.sinjartv.rstv

import org.json.JSONObject
import java.net.URI

internal data class RSTVChannel(
    val name: String,
    val tvgId: String?,
    val group: String?,
    val logoUrl: String?,
    val streamUrl: String,
    val httpHeaders: Map<String, String> = emptyMap(),
    val sourceIds: Set<String> = emptySet(),
    val sourceNames: Set<String> = emptySet(),
    val sourceProviders: Set<String> = emptySet()
)

internal data class M3uParseResult(
    val channels: List<RSTVChannel>,
    val skippedUnsafeLinks: Int,
    val invalidEntries: Int,
    val isValidPlaylist: Boolean
)

internal object M3uPlaylistParser {
    private val attributePattern =
        Regex("""([A-Za-z0-9_-]+)[ ]*=[ ]*("([^"]*)"|'([^']*)'|([^ ,]+))""")
    private val pageOnlyPlatformHosts = setOf(
        "youtube.com", "youtu.be", "googlevideo.com",
        "twitch.tv", "ttvnw.net",
        "dailymotion.com", "facebook.com", "fb.watch", "fbcdn.net",
        "instagram.com", "tiktok.com", "rumble.com"
    )
    private val supportedHeaderNames = mapOf(
        "user-agent" to "User-Agent",
        "http-user-agent" to "User-Agent",
        "referer" to "Referer",
        "referrer" to "Referer",
        "http-referrer" to "Referer",
        "origin" to "Origin",
        "accept" to "Accept"
    )

    fun parse(content: String): M3uParseResult {
        val lines = content.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        if (lines.firstOrNull()?.startsWith("#EXTM3U", ignoreCase = true) != true) {
            return M3uParseResult(emptyList(), 0, 0, false)
        }

        data class PendingChannel(
            val name: String,
            val tvgId: String?,
            val group: String?,
            val logoUrl: String?,
            val httpHeaders: MutableMap<String, String> = linkedMapOf()
        )

        val channels = mutableListOf<RSTVChannel>()
        val seenEntries = mutableSetOf<String>()
        var pending: PendingChannel? = null
        var skippedUnsafeLinks = 0
        var invalidEntries = 0

        for (line in lines.drop(1)) {
            if (line.startsWith("#EXTINF:", ignoreCase = true)) {
                if (pending != null) invalidEntries++
                val info = line.substringAfter(':')
                val separator = findTitleSeparator(info)
                val metadata = if (separator >= 0) info.substring(0, separator) else info
                val displayName = if (separator >= 0) info.substring(separator + 1).trim() else ""
                val attributes = attributePattern.findAll(metadata).associate { match ->
                    val value = match.groups[3]?.value
                        ?: match.groups[4]?.value
                        ?: match.groups[5]?.value.orEmpty()
                    match.groupValues[1].lowercase() to value
                }

                val name = attributes["tvg-name"]?.takeIf(String::isNotBlank)
                    ?: displayName.takeIf(String::isNotBlank)
                    ?: "قناة بدون اسم"

                pending = PendingChannel(
                    name = name,
                    tvgId = attributes["tvg-id"]?.takeIf(String::isNotBlank),
                    group = attributes["group-title"]?.takeIf(String::isNotBlank),
                    logoUrl = attributes["tvg-logo"]?.takeIf(String::isNotBlank)
                )
                continue
            }

            if (line.startsWith("#EXTVLCOPT:", ignoreCase = true)) {
                pending?.let { channel -> parseVlcOption(line.substringAfter(':'))?.let { (key, value) ->
                    channel.httpHeaders[key] = value
                } }
                continue
            }

            if (line.startsWith("#EXTHTTP:", ignoreCase = true)) {
                pending?.let { channel -> parseHttpJson(line.substringAfter(':')).forEach { (key, value) ->
                    channel.httpHeaders[key] = value
                } }
                continue
            }

            if (line.startsWith('#')) continue

            val channel = pending ?: continue
            pending = null

            val streamUri = runCatching { URI(line) }.getOrNull()
            val host = streamUri?.host?.lowercase().orEmpty()
            val isPageOnlyPlatform = pageOnlyPlatformHosts.any { blockedHost ->
                host == blockedHost || host.endsWith(".$blockedHost")
            }
            val isSecureHttps = streamUri != null &&
                streamUri.scheme.equals("https", ignoreCase = true) &&
                host.isNotBlank() &&
                streamUri.userInfo == null &&
                !isPageOnlyPlatform

            if (!isSecureHttps) {
                skippedUnsafeLinks++
                continue
            }

            val headers = channel.httpHeaders.toMap()
            val entryKey = line + headers.toSortedMap().entries.joinToString("|") { "${it.key}=${it.value}" }
            if (seenEntries.add(entryKey)) {
                channels += RSTVChannel(
                    name = channel.name,
                    tvgId = channel.tvgId,
                    group = channel.group,
                    logoUrl = channel.logoUrl,
                    streamUrl = line,
                    httpHeaders = headers
                )
            } else {
                invalidEntries++
            }
        }

        if (pending != null) invalidEntries++
        return M3uParseResult(channels, skippedUnsafeLinks, invalidEntries, true)
    }

    private fun parseVlcOption(option: String): Pair<String, String>? {
        val separator = option.indexOf('=')
        if (separator <= 0) return null
        val rawName = option.substring(0, separator).trim().lowercase()
        val rawValue = option.substring(separator + 1).trim().trim('"', '\'')
        return safeHeader(rawName, rawValue)
    }

    private fun parseHttpJson(json: String): Map<String, String> {
        return runCatching {
            val objectValue = JSONObject(json)
            buildMap {
                val keys = objectValue.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val value = objectValue.optString(key).trim()
                    safeHeader(key, value)?.let { (header, safeValue) -> put(header, safeValue) }
                }
            }
        }.getOrDefault(emptyMap())
    }

    private fun safeHeader(rawName: String, rawValue: String): Pair<String, String>? {
        val headerName = supportedHeaderNames[rawName.trim().lowercase()] ?: return null
        val value = rawValue.trim()
        if (value.isBlank() || value.length > 1024 || '\r' in value || '\n' in value) return null
        return headerName to value
    }

    private fun findTitleSeparator(value: String): Int {
        var quote: Char? = null
        value.forEachIndexed { index, character ->
            if (quote != null && character == quote) {
                quote = null
            } else if (quote == null && (character == '"' || character == '\'')) {
                quote = character
            } else if (quote == null && character == ',') {
                return index
            }
        }
        return -1
    }
}
