package com.sinjartv.rstv

import android.content.Context
import java.io.File
import org.json.JSONObject

internal object LocalLibraryStore {
    private const val PREFS = "rs_tv_library"
    private const val FAVORITES = "favorite_streams"
    private const val HISTORY = "recent_streams"
    private const val PLAYLIST_EXISTS = "playlist_exists"
    private const val GUIDE_EXISTS = "guide_exists"
    private const val MAX_FILE_CHARS = 15_000_000
    private const val REMOTE_SOURCE_DIRECTORY = "remote-source-library"
    private const val PRIVATE_PACKAGE_DIRECTORY = "private-content-package-library"
    private const val MAX_REMOTE_SOURCE_CHARS = 12 * 1024 * 1024
    private const val MAX_REMOTE_LIBRARY_BYTES = 64L * 1024 * 1024
    private const val MAX_PRIVATE_PACKAGE_LIBRARY_BYTES = 32L * 1024 * 1024

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

    fun saveRemoteSource(context: Context, snapshot: RemoteSourceSnapshot) {
        require(snapshot.id.matches(Regex("[a-z0-9][a-z0-9-]*"))) { "معرّف المصدر غير صالح" }
        require(snapshot.content.length <= MAX_REMOTE_SOURCE_CHARS) { "قائمة المصدر أكبر من الحد المسموح" }

        val directory = File(context.filesDir, REMOTE_SOURCE_DIRECTORY)
        require(directory.exists() || directory.mkdirs()) { "تعذر إنشاء مجلد مكتبة المصادر" }
        val playlistFile = File(directory, "${snapshot.id}.m3u8")
        val metadataFile = File(directory, "${snapshot.id}.json")
        val existingBytes = directory.listFiles()
            .orEmpty()
            .filter { it.extension == "m3u8" }
            .sumOf { it.length() }
        val previousBytes = playlistFile.takeIf { it.isFile }?.length() ?: 0L
        val newBytes = snapshot.content.toByteArray(Charsets.UTF_8).size.toLong()
        require(existingBytes - previousBytes + newBytes <= MAX_REMOTE_LIBRARY_BYTES) {
            "مكتبة مصادر IPTV وصلت إلى حد التخزين المسموح"
        }

        val playlistTemp = File(directory, "${snapshot.id}.m3u8.tmp")
        playlistTemp.writeText(snapshot.content, Charsets.UTF_8)
        if (playlistFile.exists() && !playlistFile.delete()) {
            playlistTemp.delete()
            throw IllegalStateException("تعذر تحديث نسخة المصدر القديمة")
        }
        check(playlistTemp.renameTo(playlistFile)) { "تعذر حفظ قائمة المصدر" }

        val metadata = JSONObject()
            .put("id", snapshot.id)
            .put("name", snapshot.name)
            .put("provider", snapshot.provider)
            .put("attribution", snapshot.attribution ?: JSONObject.NULL)
        val metadataTemp = File(directory, "${snapshot.id}.json.tmp")
        metadataTemp.writeText(metadata.toString(), Charsets.UTF_8)
        if (metadataFile.exists() && !metadataFile.delete()) {
            metadataTemp.delete()
            throw IllegalStateException("تعذر تحديث بيانات المصدر")
        }
        check(metadataTemp.renameTo(metadataFile)) { "تعذر حفظ بيانات المصدر" }
    }

    fun loadRemoteSources(context: Context): List<RemoteSourceSnapshot> {
        val directory = File(context.filesDir, REMOTE_SOURCE_DIRECTORY)
        return directory.listFiles()
            .orEmpty()
            .filter { it.extension == "m3u8" && it.isFile && it.length() <= MAX_REMOTE_SOURCE_CHARS * 4L }
            .sortedBy { it.name }
            .mapNotNull { playlistFile ->
                runCatching {
                    val id = playlistFile.nameWithoutExtension
                    val metadataFile = File(directory, "${id}.json")
                    val metadata = if (metadataFile.isFile) {
                        JSONObject(metadataFile.readText(Charsets.UTF_8))
                    } else {
                        JSONObject()
                    }
                    RemoteSourceSnapshot(
                        id = id,
                        name = metadata.optString("name").takeIf(String::isNotBlank) ?: id,
                        provider = metadata.optString("provider").takeIf(String::isNotBlank) ?: id,
                        attribution = metadata.optString("attribution").takeIf { it.isNotBlank() && it != "null" },
                        content = playlistFile.readText(Charsets.UTF_8)
                    )
                }.getOrNull()
            }
    }

    fun clearRemoteSources(context: Context) {
        File(context.filesDir, REMOTE_SOURCE_DIRECTORY).deleteRecursively()
    }

    // Private-content packages are stored separately so unclassified entries never leak
    // into the ordinary live-channel library.
    fun savePrivatePackage(context: Context, snapshot: RemoteSourceSnapshot) {
        saveSourceSnapshot(
            context = context,
            snapshot = snapshot,
            directoryName = PRIVATE_PACKAGE_DIRECTORY,
            maxLibraryBytes = MAX_PRIVATE_PACKAGE_LIBRARY_BYTES,
            limitMessage = "مكتبة الباقات الخاصة وصلت إلى حد التخزين المسموح"
        )
    }

    fun loadPrivatePackages(context: Context): List<RemoteSourceSnapshot> =
        loadSourceSnapshots(context, PRIVATE_PACKAGE_DIRECTORY)

    fun clearPrivatePackages(context: Context) {
        File(context.filesDir, PRIVATE_PACKAGE_DIRECTORY).deleteRecursively()
    }

    private fun saveSourceSnapshot(
        context: Context,
        snapshot: RemoteSourceSnapshot,
        directoryName: String,
        maxLibraryBytes: Long,
        limitMessage: String
    ) {
        require(snapshot.id.matches(Regex("[a-z0-9][a-z0-9-]*"))) { "معرّف المصدر غير صالح" }
        require(snapshot.content.length <= MAX_REMOTE_SOURCE_CHARS) { "قائمة المصدر أكبر من الحد المسموح" }

        val directory = File(context.filesDir, directoryName)
        require(directory.exists() || directory.mkdirs()) { "تعذر إنشاء مجلد مكتبة الباقات" }
        val playlistFile = File(directory, "${snapshot.id}.m3u8")
        val metadataFile = File(directory, "${snapshot.id}.json")
        val existingBytes = directory.listFiles()
            .orEmpty()
            .filter { it.extension == "m3u8" }
            .sumOf { it.length() }
        val previousBytes = playlistFile.takeIf { it.isFile }?.length() ?: 0L
        val newBytes = snapshot.content.toByteArray(Charsets.UTF_8).size.toLong()
        require(existingBytes - previousBytes + newBytes <= maxLibraryBytes) { limitMessage }

        val playlistTemp = File(directory, "${snapshot.id}.m3u8.tmp")
        playlistTemp.writeText(snapshot.content, Charsets.UTF_8)
        if (playlistFile.exists() && !playlistFile.delete()) {
            playlistTemp.delete()
            throw IllegalStateException("تعذر تحديث نسخة الباقة القديمة")
        }
        check(playlistTemp.renameTo(playlistFile)) { "تعذر حفظ قائمة الباقة" }

        val metadata = JSONObject()
            .put("id", snapshot.id)
            .put("name", snapshot.name)
            .put("provider", snapshot.provider)
            .put("attribution", snapshot.attribution ?: JSONObject.NULL)
        val metadataTemp = File(directory, "${snapshot.id}.json.tmp")
        metadataTemp.writeText(metadata.toString(), Charsets.UTF_8)
        if (metadataFile.exists() && !metadataFile.delete()) {
            metadataTemp.delete()
            throw IllegalStateException("تعذر تحديث بيانات الباقة")
        }
        check(metadataTemp.renameTo(metadataFile)) { "تعذر حفظ بيانات الباقة" }
    }

    private fun loadSourceSnapshots(context: Context, directoryName: String): List<RemoteSourceSnapshot> {
        val directory = File(context.filesDir, directoryName)
        return directory.listFiles()
            .orEmpty()
            .filter { it.extension == "m3u8" && it.isFile && it.length() <= MAX_REMOTE_SOURCE_CHARS * 4L }
            .sortedBy { it.name }
            .mapNotNull { playlistFile ->
                runCatching {
                    val id = playlistFile.nameWithoutExtension
                    val metadataFile = File(directory, "${id}.json")
                    val metadata = if (metadataFile.isFile) {
                        JSONObject(metadataFile.readText(Charsets.UTF_8))
                    } else {
                        JSONObject()
                    }
                    RemoteSourceSnapshot(
                        id = id,
                        name = metadata.optString("name").takeIf(String::isNotBlank) ?: id,
                        provider = metadata.optString("provider").takeIf(String::isNotBlank) ?: id,
                        attribution = metadata.optString("attribution").takeIf { it.isNotBlank() && it != "null" },
                        content = playlistFile.readText(Charsets.UTF_8)
                    )
                }.getOrNull()
            }
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

    fun clearFavorites(context: Context) {
        prefs(context).edit().remove(FAVORITES).apply()
    }

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
