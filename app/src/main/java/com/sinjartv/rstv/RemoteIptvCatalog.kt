package com.sinjartv.rstv

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

internal data class RemoteCatalogSourceDefinition(
    val source: RemotePlaylistSource,
    val autoSync: Boolean,
    val refreshIntervalMinutes: Int
)

internal data class RemoteIptvCatalogSnapshot(
    val revision: String,
    val refreshMinutes: Int,
    val sources: List<RemoteCatalogSourceDefinition>,
    val channels: List<RSTVChannel>
)

/**
 * Public, read-only catalog shared by RS TV and ROYAL+.
 * New sources/channels are published by editing catalog/remote-sources.json;
 * client APKs only refresh this document and do not need to be rebuilt.
 */
internal object RemoteIptvCatalog {
    const val CATALOG_URL =
        "https://raw.githubusercontent.com/Ramistar900/rs-tv/experiment/channel-position-resume/catalog/remote-sources.json"

    private const val PREFS = "rs_tv_remote_iptv_catalog"
    private const val CACHE_KEY = "catalog_json"
    private const val MAX_CATALOG_CHARS = 1_000_000

    fun loadOrCached(context: Context): RemoteIptvCatalogSnapshot {
        val freshJson = runCatching { downloadCatalog() }.getOrNull()
        if (freshJson != null) {
            val parsed = runCatching { parse(freshJson) }.getOrNull()
            if (parsed != null) {
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putString(CACHE_KEY, freshJson).apply()
                return parsed
            }
        }
        val cachedJson = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(CACHE_KEY, null)
        return cachedJson?.let { runCatching { parse(it) }.getOrNull() }
            ?: RemoteIptvCatalogSnapshot("empty", 15, emptyList(), emptyList())
    }

    private fun downloadCatalog(): String {
        val connection = (URL(CATALOG_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            useCaches = false
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cache-Control", "no-cache")
        }
        try {
            val status = connection.responseCode
            require(status in 200..299) { "Remote catalog HTTP $status" }
            val content = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            require(content.length <= MAX_CATALOG_CHARS) { "Remote catalog is too large" }
            return content
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(content: String): RemoteIptvCatalogSnapshot {
        val root = JSONObject(content)
        require(root.optInt("schemaVersion", 0) == 1) { "Unsupported remote catalog schema" }

        val sources = buildList {
            val array = root.optJSONArray("sources") ?: return@buildList
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                if (!item.optBoolean("enabled", true)) continue
                val id = item.optString("id").trim()
                val name = item.optString("name").trim()
                val playlistUrl = item.optString("playlistUrl").trim()
                val provider = item.optString("provider").trim()
                val uri = runCatching { URI(playlistUrl) }.getOrNull() ?: continue
                val host = uri.host?.lowercase()?.takeIf { it.isNotBlank() } ?: continue
                if (!id.matches(Regex("[a-z0-9][a-z0-9._-]{1,79}")) ||
                    name.isBlank() || provider.isBlank() ||
                    uri.scheme?.equals("https", ignoreCase = true) != true ||
                    uri.userInfo != null
                ) continue

                val declaredHosts = buildSet {
                    val hosts = item.optJSONArray("allowedHosts")
                    if (hosts != null) {
                        for (hostIndex in 0 until hosts.length()) {
                            val allowedHost = hosts.optString(hostIndex).trim().lowercase()
                            if (allowedHost.matches(Regex("[a-z0-9.-]{1,253}"))) add(allowedHost)
                        }
                    }
                    add(host)
                }
                val source = RemotePlaylistSource(
                    id = id,
                    name = name,
                    description = item.optString("description"),
                    playlistUrl = playlistUrl,
                    provider = provider,
                    attribution = item.optString("attribution").takeIf { it.isNotBlank() && it != "null" },
                    allowedHosts = declaredHosts
                )
                add(
                    RemoteCatalogSourceDefinition(
                        source = source,
                        autoSync = item.optBoolean("autoSync", false),
                        refreshIntervalMinutes = item.optInt("refreshIntervalMinutes", 360).coerceIn(60, 10_080)
                    )
                )
            }
        }.distinctBy { it.source.id }

        val channels = buildList {
            val array = root.optJSONArray("channels") ?: return@buildList
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                if (!item.optBoolean("enabled", true)) continue
                val name = item.optString("name").trim()
                val streamUrl = item.optString("streamUrl").trim()
                val uri = runCatching { URI(streamUrl) }.getOrNull() ?: continue
                if (name.isBlank() ||
                    uri.scheme?.equals("https", ignoreCase = true) != true ||
                    uri.host.isNullOrBlank() ||
                    uri.userInfo != null
                ) continue
                val provider = item.optString("provider", "الكتالوج المركزي")
                add(
                    RSTVChannel(
                        name = name,
                        tvgId = item.optString("tvgId").takeIf { it.isNotBlank() && it != "null" },
                        group = item.optString("group", "قنوات إضافية").takeIf { it.isNotBlank() },
                        logoUrl = item.optString("logoUrl").takeIf { it.startsWith("https://") },
                        country = item.optString("country").takeIf { it.isNotBlank() && it != "null" },
                        contentRating = item.optString("contentRating").takeIf { it.isNotBlank() && it != "null" },
                        streamUrl = streamUrl,
                        sourceIds = setOf("remote-catalog"),
                        sourceNames = setOf("الكتالوج المركزي"),
                        sourceProviders = setOf(provider)
                    )
                )
            }
        }.distinctBy { RemotePlaylistMerger.canonicalStreamKey(it.streamUrl, it.httpHeaders) }

        return RemoteIptvCatalogSnapshot(
            revision = root.optString("revision", root.optString("updatedAt", "unknown")),
            refreshMinutes = root.optInt("refreshMinutes", 15).coerceIn(5, 240),
            sources = sources,
            channels = channels
        )
    }
}
