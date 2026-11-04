package com.sinjartv.rstv

import java.net.URI

internal data class RSTVChannel(
    val name: String,
    val tvgId: String?,
    val group: String?,
    val logoUrl: String?,
    val streamUrl: String,
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

    fun parse(content: String): M3uParseResult {
        val lines = content.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        if (lines.firstOrNull()?.startsWith("#EXTM3U", ignoreCase = true) != true) {
            return M3uParseResult(emptyList(), 0, 0, false)
        }

        data class PendingChannel(
            val name: String,
            val tvgId: String?,
            val group: String?,
            val logoUrl: String?
        )

        val channels = mutableListOf<RSTVChannel>()
        val seenUrls = mutableSetOf<String>()
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

            if (seenUrls.add(line)) {
                channels += RSTVChannel(
                    name = channel.name,
                    tvgId = channel.tvgId,
                    group = channel.group,
                    logoUrl = channel.logoUrl,
                    streamUrl = line
                )
            } else {
                invalidEntries++
            }
        }

        if (pending != null) invalidEntries++
        return M3uParseResult(channels, skippedUnsafeLinks, invalidEntries, true)
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
