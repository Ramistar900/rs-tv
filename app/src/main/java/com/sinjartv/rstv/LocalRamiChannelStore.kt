package com.sinjartv.rstv

import android.content.Context
import android.net.Uri
import org.json.JSONArray

/**
 * Device-local configuration for one independently branded RAMİ TV channel.
 * Channel IDs are allocated sequentially and are not capped.
 */
data class LocalRamiChannelConfig(
    val id: Int,
    val name: String,
    val logoUri: String? = null,
    val backgroundUri: String? = null,
    val primaryColor: Long = 0xFFE3D1A0,
    val videoUris: List<String> = emptyList(),
    val active: Boolean = false,
    val tickerEnabled: Boolean = false,
    val tickerText: String = "",
    val clockEnabled: Boolean = false
) {
    val isReady: Boolean
        get() = active && videoUris.isNotEmpty()
}

object LocalRamiChannelStore {
    private const val LEGACY_PREFS = "rami_tv_one_library"
    private const val PREFIX = "rami_tv_channel_"
    private const val INDEX_PREFS = "rami_tv_channels_index"
    private const val IDS_KEY = "channel_ids"

    /**
     * The first install keeps the ten legacy slots visible. Existing installations
     * therefore retain their current channel numbers and preferences; new channels
     * are appended without an upper limit.
     */
    private fun channelIds(context: Context): List<Int> {
        val index = context.getSharedPreferences(INDEX_PREFS, Context.MODE_PRIVATE)
        val saved = index.getString("channel_ids_ordered", null)
        val ids = if (saved == null) {
            (1..10).toList()
        } else {
            runCatching {
                val array = JSONArray(saved)
                (0 until array.length()).mapNotNull { array.optInt(it).takeIf { id -> id > 0 } }
            }.getOrDefault((1..10).toList())
        }
        val normalized = (ids + (1..10)).distinct().sorted()
        if (saved == null || normalized != ids) {
            index.edit().putString("channel_ids_ordered", JSONArray(normalized).toString())
                .putInt("next_channel_id", (normalized.maxOrNull() ?: 0) + 1).apply()
        }
        return normalized
    }

    private fun registerChannel(context: Context, id: Int) {
        val index = context.getSharedPreferences(INDEX_PREFS, Context.MODE_PRIVATE)
        val ids = (channelIds(context) + id).distinct().sorted()
        index.edit().putString("channel_ids_ordered", JSONArray(ids).toString())
            .putInt("next_channel_id", (ids.maxOrNull() ?: 0) + 1).apply()
    }

    fun createChannel(context: Context): LocalRamiChannelConfig {
        val index = context.getSharedPreferences(INDEX_PREFS, Context.MODE_PRIVATE)
        val ids = channelIds(context)
        val id = maxOf(index.getInt("next_channel_id", 1), (ids.maxOrNull() ?: 0) + 1)
        val config = LocalRamiChannelConfig(id = id, name = "RAMİ TV $id")
        save(context, config)
        return config
    }

    fun loadAll(context: Context): List<LocalRamiChannelConfig> =
        channelIds(context).map { load(context, it) }

    fun load(context: Context, id: Int): LocalRamiChannelConfig {
        require(id > 0) { "RAMİ TV channel id must be positive" }
        val prefs = context.getSharedPreferences(PREFIX + id, Context.MODE_PRIVATE)

        if (id == 1 && !prefs.contains("migrated_from_legacy")) {
            migrateLegacyChannelOne(context, prefs)
        }

        val ordered = prefs.getString("video_uris_ordered", null)
        val videos = if (ordered != null) {
            runCatching {
                val array = JSONArray(ordered)
                (0 until array.length()).mapNotNull { index ->
                    array.optString(index).takeIf(String::isNotBlank)
                }
            }.getOrDefault(emptyList())
        } else {
            prefs.getStringSet("video_uris", emptySet()).orEmpty().toList()
        }

        return LocalRamiChannelConfig(
            id = id,
            name = prefs.getString("name", "RAMİ TV $id") ?: "RAMİ TV $id",
            logoUri = prefs.getString("logo_uri", null),
            backgroundUri = prefs.getString("background_uri", null),
            primaryColor = prefs.getLong("primary_color", 0xFFE3D1A0),
            videoUris = videos,
            active = prefs.getBoolean("channel_active", false),
            tickerEnabled = prefs.getBoolean("ticker_enabled", false),
            tickerText = prefs.getString("ticker_text", "") ?: "",
            clockEnabled = prefs.getBoolean("clock_enabled", false)
        )
    }

    fun save(context: Context, config: LocalRamiChannelConfig) {
        require(config.id > 0) { "RAMİ TV channel id must be positive" }
        registerChannel(context, config.id)
        context.getSharedPreferences(PREFIX + config.id, Context.MODE_PRIVATE)
            .edit()
            .putString("name", config.name.ifBlank { "RAMİ TV ${config.id}" })
            .putString("logo_uri", config.logoUri)
            .putString("background_uri", config.backgroundUri)
            .putLong("primary_color", config.primaryColor)
            .putString("video_uris_ordered", JSONArray(config.videoUris).toString())
            .putStringSet("video_uris", config.videoUris.toSet())
            .putBoolean("channel_active", config.active)
            .putBoolean("ticker_enabled", config.tickerEnabled)
            .putString("ticker_text", config.tickerText)
            .putBoolean("clock_enabled", config.clockEnabled)
            .putBoolean("migrated_from_legacy", true)
            .apply()
    }

    fun readyChannels(context: Context): List<LocalRamiChannelConfig> =
        loadAll(context).filter { channel ->
            channel.isReady && channel.videoUris.all { rawUri ->
                runCatching {
                    context.contentResolver.openAssetFileDescriptor(Uri.parse(rawUri), "r")?.use { true }
                        ?: false
                }.getOrDefault(false)
            }
        }

    private fun migrateLegacyChannelOne(
        context: Context,
        destination: android.content.SharedPreferences
    ) {
        val legacy = context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
        val ordered = legacy.getString("video_uris_ordered", null)
        val videos = if (ordered != null) ordered else {
            JSONArray(legacy.getStringSet("video_uris", emptySet()).orEmpty().sorted()).toString()
        }
        destination.edit()
            .putString("video_uris_ordered", videos)
            .putStringSet("video_uris", legacy.getStringSet("video_uris", emptySet()).orEmpty())
            .putBoolean("channel_active", legacy.getBoolean("channel_active", false))
            .putString("name", "RAMİ TV 1")
            .putBoolean("migrated_from_legacy", true)
            .apply()
    }
}
