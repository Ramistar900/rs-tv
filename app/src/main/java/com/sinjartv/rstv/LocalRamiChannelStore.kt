package com.sinjartv.rstv

import android.content.Context
import android.net.Uri
import org.json.JSONArray

/**
 * Device-local configuration for one of the ten independent RAMİ TV channels.
 * Branding images remain optional until the official channel assets are supplied.
 */
data class LocalRamiChannelConfig(
    val id: Int,
    val name: String,
    val logoUri: String? = null,
    val backgroundUri: String? = null,
    val primaryColor: Long = 0xFFE3D1A0,
    val videoUris: List<String> = emptyList(),
    val active: Boolean = false
) {
    val isReady: Boolean
        get() = active && videoUris.isNotEmpty()
}

object LocalRamiChannelStore {
    private const val LEGACY_PREFS = "rami_tv_one_library"
    private const val PREFIX = "rami_tv_channel_"

    fun loadAll(context: Context): List<LocalRamiChannelConfig> =
        (1..10).map { load(context, it) }

    fun load(context: Context, id: Int): LocalRamiChannelConfig {
        require(id in 1..10) { "RAMİ TV channel id must be between 1 and 10" }
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
            active = prefs.getBoolean("channel_active", false)
        )
    }

    fun save(context: Context, config: LocalRamiChannelConfig) {
        require(config.id in 1..10) { "RAMİ TV channel id must be between 1 and 10" }
        context.getSharedPreferences(PREFIX + config.id, Context.MODE_PRIVATE)
            .edit()
            .putString("name", config.name.ifBlank { "RAMİ TV ${config.id}" })
            .putString("logo_uri", config.logoUri)
            .putString("background_uri", config.backgroundUri)
            .putLong("primary_color", config.primaryColor)
            .putString("video_uris_ordered", JSONArray(config.videoUris).toString())
            .putStringSet("video_uris", config.videoUris.toSet())
            .putBoolean("channel_active", config.active)
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
