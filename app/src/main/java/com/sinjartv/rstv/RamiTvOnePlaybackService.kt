package com.sinjartv.rstv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * Owns independent IPTV and local RAMİ playback engines.
 *
 * RAMİ keeps advancing while IPTV is displayed. Its audio is muted during IPTV playback
 * and restored when the user returns to a local RAMİ channel.
 */
@OptIn(UnstableApi::class)
class RamiTvOnePlaybackService : MediaSessionService() {
    private var iptvPlayer: ExoPlayer? = null
    private var localPlayer: ExoPlayer? = null
    private var iptvSession: MediaSession? = null
    private var localSession: MediaSession? = null
    private val positionHandler = Handler(Looper.getMainLooper())
    private val positionSaver = object : Runnable {
        override fun run() {
            saveCurrentLocalState()
            positionHandler.postDelayed(this, POSITION_SAVE_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()

        val iptv = ExoPlayer.Builder(this).build()
        val local = ExoPlayer.Builder(this).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 1f
        }
        iptvPlayer = iptv
        localPlayer = local

        iptvSession = createSession(
            player = iptv,
            sessionId = IPTV_SESSION_ID,
            allowedCommand = RamiPlaybackCommands.PLAY_IPTV
        ) { args -> playIptv(args) }

        localSession = createSession(
            player = local,
            sessionId = LOCAL_SESSION_ID,
            allowedCommand = RamiPlaybackCommands.PLAY_LOCAL
        ) { args -> playLocal(args) }

        addSession(iptvSession!!)
        addSession(localSession!!)
        positionHandler.postDelayed(positionSaver, POSITION_SAVE_INTERVAL_MS)
    }

    private fun createSession(
        player: ExoPlayer,
        sessionId: String,
        allowedCommand: SessionCommand,
        executeCommand: (Bundle) -> Unit
    ): MediaSession {
        return MediaSession.Builder(this, player)
            .setId(sessionId)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                        .buildUpon()
                        .add(allowedCommand)
                        .add(RamiPlaybackCommands.RESTORE_FOREGROUND_AUDIO)
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(commands)
                        .build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    return try {
                        when (customCommand.customAction) {
                            allowedCommand.customAction -> executeCommand(args)
                            RamiPlaybackCommands.RESTORE_FOREGROUND_AUDIO.customAction ->
                                restoreForegroundAudio()
                            RamiPlaybackCommands.MUTE_BACKGROUND_AUDIO.customAction ->
                                muteBackgroundAudio()
                            else -> return Futures.immediateFuture(
                                SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED)
                            )
                        }
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    } catch (_: Throwable) {
                        Futures.immediateFuture(
                            SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE)
                        )
                    }
                }
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return if (controllerInfo.connectionHints.getString(RamiPlaybackCommands.KEY_SESSION_ROLE) ==
            RamiPlaybackCommands.ROLE_LOCAL
        ) localSession else iptvSession
    }

    private fun playIptv(args: Bundle) {
        saveCurrentLocalState()
        val streamUrl = args.getString(KEY_STREAM_URL)
            ?.takeIf { it.startsWith("https://") }
            ?: throw IllegalArgumentException("Invalid IPTV stream URL")
        val headersBundle = args.getBundle(KEY_HEADERS) ?: Bundle.EMPTY
        val headers = buildMap {
            for (key in headersBundle.keySet()) {
                val value = headersBundle.getString(key)
                if (!key.isNullOrBlank() && value != null && value.isNotBlank()) put(key, value)
            }
        }
        val httpFactory = DefaultHttpDataSource.Factory().setDefaultRequestProperties(headers)
        val sourceFactory = DefaultMediaSourceFactory(DefaultDataSource.Factory(this, httpFactory))
        val mediaSource = sourceFactory.createMediaSource(MediaItem.fromUri(streamUrl))
        val playbackPlayer = iptvPlayer ?: throw IllegalStateException("IPTV player is unavailable")

        // Keep the local RAMİ timeline running, but never mix its sound with IPTV.
        localPlayer?.volume = 0f
        playbackPlayer.volume = 1f
        val liveMode = args.getBoolean(KEY_LIVE_MODE, true)
        val positionMs = args.getLong(KEY_POSITION_MS, 0L).coerceAtLeast(0L)

        playbackPlayer.repeatMode = Player.REPEAT_MODE_OFF
        if (liveMode) playbackPlayer.setMediaSource(mediaSource)
        else playbackPlayer.setMediaSource(mediaSource, positionMs)
        playbackPreferences().edit().putString(KEY_ACTIVE_TYPE, TYPE_IPTV).apply()
        playbackPlayer.prepare()
        playbackPlayer.play()
    }

    private fun playLocal(args: Bundle) {
        val channelId = args.getInt(KEY_CHANNEL_ID, -1)
        require(channelId in 1..10) { "Invalid local RAMİ TV channel" }
        val videoUris = args.getStringArrayList(KEY_VIDEO_URIS).orEmpty()
            .filter { it.startsWith("content://") || it.startsWith("file://") }
        require(videoUris.isNotEmpty()) { "The local channel has no valid videos" }

        val preferences = playbackPreferences()
        saveCurrentLocalState()
        val playbackPlayer = localPlayer ?: throw IllegalStateException("Local RAMİ player is unavailable")

        // Returning to the same channel only restores its picture and sound. Do not reload
        // the playlist: the independent player has already advanced while IPTV was on screen.
        val activeChannelId = preferences.getInt(KEY_ACTIVE_CHANNEL_ID, -1)
        val currentUris = (0 until playbackPlayer.mediaItemCount).mapNotNull { index ->
            playbackPlayer.getMediaItemAt(index).localConfiguration?.uri?.toString()
        }
        if (activeChannelId == channelId && currentUris == videoUris &&
            playbackPlayer.mediaItemCount == videoUris.size
        ) {
            iptvPlayer?.pause()
            iptvPlayer?.volume = 0f
            playbackPlayer.volume = 1f
            preferences.edit().putString(KEY_ACTIVE_TYPE, TYPE_LOCAL).apply()
            playbackPlayer.play()
            return
        }

        val startIndex = preferences.getInt(channelIndexKey(channelId), 0).coerceIn(0, videoUris.lastIndex)
        val startPosition = preferences.getLong(channelPositionKey(channelId), 0L).coerceAtLeast(0L)

        // IPTV is not the selected channel now. Keep its session independent but pause it.
        iptvPlayer?.pause()
        iptvPlayer?.volume = 0f
        playbackPlayer.volume = 1f
        playbackPlayer.repeatMode = Player.REPEAT_MODE_ALL
        playbackPlayer.setMediaItems(
            videoUris.map { MediaItem.fromUri(Uri.parse(it)) },
            startIndex,
            startPosition
        )
        preferences.edit()
            .putString(KEY_ACTIVE_TYPE, TYPE_LOCAL)
            .putInt(KEY_ACTIVE_CHANNEL_ID, channelId)
            .apply()
        playbackPlayer.prepare()
        playbackPlayer.play()
    }

    private fun muteBackgroundAudio() {
        // The silent background-continuation rule applies only to local RAMİ channels.
        // IPTV keeps its existing audio/background policy unchanged.
        if (playbackPreferences().getString(KEY_ACTIVE_TYPE, null) == TYPE_LOCAL) {
            localPlayer?.volume = 0f
        }
    }

    private fun restoreForegroundAudio() {
        // Restore only RAMİ audio. Do not alter IPTV volume or playback behaviour.
        if (playbackPreferences().getString(KEY_ACTIVE_TYPE, null) == TYPE_LOCAL) {
            localPlayer?.volume = 1f
            iptvPlayer?.volume = 0f
        }
    }

    private fun saveCurrentLocalState() {
        val preferences = playbackPreferences()
        val channelId = preferences.getInt(KEY_ACTIVE_CHANNEL_ID, -1)
        val playbackPlayer = localPlayer ?: return
        if (channelId !in 1..10 || playbackPlayer.mediaItemCount == 0) return
        preferences.edit()
            .putInt(channelIndexKey(channelId), playbackPlayer.currentMediaItemIndex.coerceAtLeast(0))
            .putLong(channelPositionKey(channelId), playbackPlayer.currentPosition.coerceAtLeast(0L))
            .apply()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val activeType = playbackPreferences().getString(KEY_ACTIVE_TYPE, null)

        if (activeType == TYPE_LOCAL) {
            // Only RAMİ channels get the new always-continue-silently behaviour.
            saveCurrentLocalState()
            localPlayer?.volume = 0f
            super.onTaskRemoved(rootIntent)
            return
        }

        // IPTV keeps its previous behaviour and the user's existing background setting.
        val keepInBackground = getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE)
            .getBoolean(KEY_KEEP_BACKGROUND, false)
        if (keepInBackground) {
            super.onTaskRemoved(rootIntent)
            return
        }

        saveCurrentLocalState()
        playbackPreferences().edit().putString(KEY_ACTIVE_TYPE, TYPE_STOPPED).apply()
        iptvPlayer?.pause()
        localPlayer?.pause()
        iptvPlayer?.stop()
        localPlayer?.stop()
        iptvPlayer?.clearMediaItems()
        localPlayer?.clearMediaItems()
        stopSelf()
    }

    override fun onDestroy() {
        positionHandler.removeCallbacks(positionSaver)
        saveCurrentLocalState()
        iptvSession?.release()
        localSession?.release()
        iptvSession = null
        localSession = null
        iptvPlayer?.release()
        localPlayer?.release()
        iptvPlayer = null
        localPlayer = null
        super.onDestroy()
    }

    private fun playbackPreferences() = getSharedPreferences(PLAYBACK_PREFS, MODE_PRIVATE)
    private fun channelIndexKey(channelId: Int) = "channel_${channelId}_media_index"
    private fun channelPositionKey(channelId: Int) = "channel_${channelId}_position_ms"

    companion object {
        const val SETTINGS_PREFS = "rs_tv_playback_settings"
        const val KEY_KEEP_BACKGROUND = "keep_playing_in_background"

        private const val PLAYBACK_PREFS = "rs_tv_playback_state"
        private const val KEY_ACTIVE_TYPE = "active_media_type"
        private const val KEY_ACTIVE_CHANNEL_ID = "active_local_channel_id"
        private const val KEY_STREAM_URL = "stream_url"
        private const val KEY_HEADERS = "headers"
        private const val KEY_LIVE_MODE = "live_mode"
        private const val KEY_POSITION_MS = "position_ms"
        private const val KEY_CHANNEL_ID = "channel_id"
        private const val KEY_VIDEO_URIS = "video_uris"
        private const val TYPE_LOCAL = "local"
        private const val TYPE_IPTV = "iptv"
        private const val TYPE_STOPPED = "stopped"
        private const val IPTV_SESSION_ID = "rs-tv-iptv"
        private const val LOCAL_SESSION_ID = "rs-tv-local-rami"
        private const val POSITION_SAVE_INTERVAL_MS = 5_000L
    }
}
