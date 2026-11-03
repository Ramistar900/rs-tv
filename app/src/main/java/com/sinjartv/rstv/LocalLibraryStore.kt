package com.sinjartv.rstv

import android.content.Context
import java.io.File

internal object LocalLibraryStore {
    private const val PREFS = "rs_tv_library"
    private const val FAVORITES = "favorite_streams"
    private const val HISTORY = "recent_streams"
    private const val PLAYLIST_EXISTS = "playlist_exists"
    private const val GUIDE_EXISTS = "guide_exists"
    private const val MAX_FILE_CHARS = 15_000_000

    fun savePlaylist(context: Context, content: String) {
        require(content.length <= MAX_FILE_CHARS) { "حجم القائمة أكبر من الحد المسموح" }
        File(context.filesDir, "user-playlist.m3u8").writeText(content, Charsets.UTF_8)
        prefs(context).edit().putBoolean(PLAYLIST_EXISTS, true).apply()
    }

    fun loadPlaylist(context: Context): String? =
        File(context.filesDir, "user-playlist.m3u8")
            .takeIf { it.isFile && it.length() <= MAX_FILE_CHARS * 4L }
            ?.readText(Charsets.UTF_8)

    fun clearPlaylist(context: Context) {
        File(context.filesDir, "user-playlist.m3u8").delete()
        prefs(context).edit().putBoolean(PLAYLIST_EXISTS, false).apply()
    }

    fun saveGuide(context: Context, content: String) {
        require(content.length <= MAX_FILE_CHARS) { "حجم دليل البرامج أكبر من الحد المسموح" }
        File(context.filesDir, "user-guide.xml").writeText(content, Charsets.UTF_8)
        prefs(context).edit().putBoolean(GUIDE_EXISTS, true).apply()
    }

    fun loadGuide(context: Context): String? =
        File(context.filesDir, "user-guide.xml")
            .takeIf { it.isFile && it.length() <= MAX_FILE_CHARS * 4L }
            ?.readText(Charsets.UTF_8)

    fun isFavorite(context: Context, streamUrl: String): Boolean =
        streamUrl in prefs(context).getStringSet(FAVORITES, emptySet()).orEmpty()

    fun toggleFavorite(context: Context, streamUrl: String): Boolean {
        val current = prefs(context).getStringSet(FAVORITES, emptySet()).orEmpty().toMutableSet()
        val isNowFavorite = if (current.remove(streamUrl)) false else {
            current.add(streamUrl)
            true
        }
        prefs(context).edit().putStringSet(FAVORITES, current).apply()
        return isNowFavorite
    }

    fun favoriteStreams(context: Context): Set<String> =
        prefs(context).getStringSet(FAVORITES, emptySet()).orEmpty().toSet()

    fun addToHistory(context: Context, streamUrl: String) {
        val recent = history(context).filterNot { it == streamUrl }.toMutableList()
        recent.add(0, streamUrl)
        prefs(context).edit().putString(HISTORY, recent.take(30).joinToString("\n")).apply()
    }

    fun history(context: Context): List<String> =
        prefs(context).getString(HISTORY, "").orEmpty()
            .lineSequence().map(String::trim).filter(String::isNotBlank).take(30).toList()

    fun clearHistory(context: Context) {
        prefs(context).edit().remove(HISTORY).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
