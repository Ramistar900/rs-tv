package com.sinjartv.rstv

import java.net.URI

internal data class RemoteSourceSnapshot(
    val id: String,
    val name: String,
    val provider: String,
    val attribution: String?,
    val content: String
)

internal object RemotePlaylistMerger {
    private val countryBySourceId = mapOf(
        "doubo-turkey" to "تركيا",
        "iptv-org-us" to "الولايات المتحدة الأمريكية",
        "iptv-org-ca" to "كندا",
        "iptv-org-cn" to "الصين",
        "iptv-org-jp" to "اليابان",
        "iptv-org-kr" to "كوريا الجنوبية",
        "iptv-org-ru" to "روسيا",
        "iptv-org-br" to "البرازيل",
        "iptv-org-ar" to "الأرجنتين",
        "iptv-org-cl" to "تشيلي",
        "iptv-org-co" to "كولومبيا",
        "iptv-org-mx" to "المكسيك",
        "iptv-org-pe" to "بيرو",
        "iptv-org-uk" to "المملكة المتحدة",
        "iptv-org-de" to "ألمانيا",
        "iptv-org-fr" to "فرنسا",
        "iptv-org-it" to "إيطاليا",
        "iptv-org-es" to "إسبانيا",
        "iptv-org-pt" to "البرتغال",
        "iptv-org-pl" to "بولندا",
        "iptv-org-nl" to "هولندا",
        "iptv-org-se" to "السويد",
        "iptv-org-no" to "النرويج"
    )

    fun merge(snapshots: List<RemoteSourceSnapshot>): List<RSTVChannel> {
        val merged = linkedMapOf<String, RSTVChannel>()
        snapshots.forEach { snapshot ->
            val parsed = M3uPlaylistParser.parse(snapshot.content)
            if (!parsed.isValidPlaylist) return@forEach

            parsed.channels.forEach { channel ->
                val key = canonicalStreamKey(channel.streamUrl, channel.httpHeaders)
                val incoming = channel.copy(
                    country = channel.country?.takeIf(String::isNotBlank) ?: countryBySourceId[snapshot.id] ?: countryFromPackageId(snapshot.id),
                    sourceIds = setOf(snapshot.id),
                    sourceNames = setOf(snapshot.name),
                    sourceProviders = setOf(snapshot.provider)
                )
                val existing = merged[key]
                if (existing == null) {
                    merged[key] = incoming
                } else {
                    val restrictiveMetadata = when {
                        AdultContentPolicy.isAdult(existing) -> existing
                        AdultContentPolicy.isAdult(incoming) -> incoming
                        else -> null
                    }
                    merged[key] = existing.copy(
                        name = restrictiveMetadata?.name ?: betterValue(existing.name, incoming.name),
                        tvgId = existing.tvgId ?: incoming.tvgId,
                        group = restrictiveMetadata?.group ?: existing.group ?: incoming.group,
                        logoUrl = existing.logoUrl ?: incoming.logoUrl,
                        country = existing.country?.takeIf(String::isNotBlank) ?: incoming.country,
                        contentRating = restrictiveMetadata?.contentRating ?: existing.contentRating ?: incoming.contentRating,
                        sourceIds = existing.sourceIds + incoming.sourceIds,
                        sourceNames = existing.sourceNames + incoming.sourceNames,
                        sourceProviders = existing.sourceProviders + incoming.sourceProviders
                    )
                }
            }
        }
        return merged.values.toList()
    }

    private fun countryFromPackageId(sourceId: String): String? {
        val code = when {
            sourceId.startsWith("iptv-org-") -> sourceId.removePrefix("iptv-org-").uppercase()
            sourceId.startsWith("doubo-country-") -> sourceId.removePrefix("doubo-country-").uppercase()
            else -> return null
        }
        if (!code.matches(Regex("[A-Z]{2}"))) return null
        return RemotePlaylistSources.countryNameForPackageCode(code)
    }

    fun canonicalStreamKey(streamUrl: String, httpHeaders: Map<String, String> = emptyMap()): String {
        val uri = runCatching { URI(streamUrl) }.getOrNull() ?: return streamUrl
        val scheme = uri.scheme?.lowercase() ?: return streamUrl
        val host = uri.host?.lowercase() ?: return streamUrl
        val port = if (uri.port == -1 || uri.port == 443) "" else ":" + uri.port
        val path = uri.rawPath.orEmpty().ifEmpty { "/" }
        val query = uri.rawQuery?.let { "?" + it }.orEmpty()
        val headerProfile = httpHeaders.toSortedMap().entries.joinToString("&") { "${it.key}=${it.value}" }
        val canonicalUrl = "$scheme://$host$port$path$query"
        return if (headerProfile.isEmpty()) canonicalUrl else "$canonicalUrl|headers:$headerProfile"
    }

    private fun betterValue(first: String, second: String): String =
        if (first.isBlank() || first == "قناة بدون اسم") second else first
}
