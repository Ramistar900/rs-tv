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
    fun merge(snapshots: List<RemoteSourceSnapshot>): List<RSTVChannel> {
        val merged = linkedMapOf<String, RSTVChannel>()
        snapshots.forEach { snapshot ->
            val parsed = M3uPlaylistParser.parse(snapshot.content)
            if (!parsed.isValidPlaylist) return@forEach

            parsed.channels.forEach { channel ->
                val key = canonicalStreamKey(channel.streamUrl)
                val incoming = channel.copy(
                    sourceIds = setOf(snapshot.id),
                    sourceNames = setOf(snapshot.name),
                    sourceProviders = setOf(snapshot.provider)
                )
                val existing = merged[key]
                if (existing == null) {
                    merged[key] = incoming
                } else {
                    merged[key] = existing.copy(
                        name = betterValue(existing.name, incoming.name),
                        tvgId = existing.tvgId ?: incoming.tvgId,
                        group = existing.group ?: incoming.group,
                        logoUrl = existing.logoUrl ?: incoming.logoUrl,
                        sourceIds = existing.sourceIds + incoming.sourceIds,
                        sourceNames = existing.sourceNames + incoming.sourceNames,
                        sourceProviders = existing.sourceProviders + incoming.sourceProviders
                    )
                }
            }
        }
        return merged.values.toList()
    }

    fun canonicalStreamKey(streamUrl: String): String {
        val uri = runCatching { URI(streamUrl) }.getOrNull() ?: return streamUrl
        val scheme = uri.scheme?.lowercase() ?: return streamUrl
        val host = uri.host?.lowercase() ?: return streamUrl
        val port = if (uri.port == -1 || uri.port == 443) "" else ":" + uri.port
        val path = uri.rawPath.orEmpty().ifEmpty { "/" }
        val query = uri.rawQuery?.let { "?" + it }.orEmpty()
        return "$scheme://$host$port$path$query"
    }

    private fun betterValue(first: String, second: String): String =
        if (first.isBlank() || first == "قناة بدون اسم") second else first
}
