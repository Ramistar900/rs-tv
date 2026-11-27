package com.sinjartv.rstv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
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
 * Owns the single RS TV playback engine for IPTV and all local RAMİ TV channels.
 * Compose screens are controllers only; they do not create a competing playback engine.
 */
@OptIn(UnstableApi::class)
class RamiTvOnePlaybackService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private var scheduledPlaybackChannelId: Int? = null
    private var scheduledResumeIndex: Int = 0
    private val scheduleHandler = Handler(Looper.getMainLooper())
    private val scheduleTicker = object : Runnable {
        override fun run() {
            runScheduleCheck()
            scheduleHandler.postDelayed(this, SCHEDULE_CHECK_INTERVAL_MS)
        }
    }
    private val positionHandler = Handler(Looper.getMainLooper())
    private val positionSaver = object : Runnable {
        override fun run() {
            saveCurrentLocalState()
            positionHandler.postDelayed(this, POSITION_SAVE_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        val playbackPlayer = ExoPlayer.Builder(this).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }
        player = playbackPlayer
        playbackPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && scheduledPlaybackChannelId != null) {
                    resumeNormalLocalPlaylist()
                }
            }
        })
        mediaSession = MediaSession.Builder(this, playbackPlayer)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                        .buildUpon()
                        .add(RamiPlaybackCommands.PLAY_IPTV)
                        .add(RamiPlaybackCommands.PLAY_LOCAL)
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
                            RamiPlaybackCommands.PLAY_IPTV.customAction -> playIptv(args)
                            RamiPlaybackCommands.PLAY_LOCAL.customAction -> playLocal(args)
                            else -> return Futures.immediateFuture(
                                SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED)
                            )
                        }
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    } catch (_: Throwable) {
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE))
                    }
                }
            })
            .build()
        positionHandler.postDelayed(positionSaver, POSITION_SAVE_INTERVAL_MS)
        scheduleHandler.postDelayed(scheduleTicker, SCHEDULE_CHECK_INTERVAL_MS)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    private fun playIptv(args: Bundle) {
        saveCurrentLocalState()
        scheduledPlaybackChannelId = null
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
        val playbackPlayer = player ?: throw IllegalStateException("Playback player is unavailable")
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
        val wasScheduledPlayback = scheduledPlaybackChannelId != null
        scheduledPlaybackChannelId = null
        val channelId = args.getInt(KEY_CHANNEL_ID, -1)
        require(channelId > 0) { "Invalid local RAMİ TV channel" }
        val videoUris = args.getStringArrayList(KEY_VIDEO_URIS).orEmpty()
            .filter { it.startsWith("content://") || it.startsWith("file://") }
        require(videoUris.isNotEmpty()) { "The local channel has no valid videos" }

        val preferences = playbackPreferences()
        if (!wasScheduledPlayback && preferences.getString(KEY_ACTIVE_TYPE, null) == TYPE_LOCAL) {
            saveCurrentLocalState()
        }

        val startIndex = preferences.getInt(channelIndexKey(channelId), 0).coerceIn(0, videoUris.lastIndex)
        val startPosition = preferences.getLong(channelPositionKey(channelId), 0L).coerceAtLeast(0L)
        val playbackPlayer = player ?: throw IllegalStateException("Playback player is unavailable")

        playbackPlayer.repeatMode = Player.REPEAT_MODE_ALL
        playbackPlayer.setMediaItems(videoUris.map { MediaItem.fromUri(Uri.parse(it)) }, startIndex, startPosition)
        preferences.edit()
            .putString(KEY_ACTIVE_TYPE, TYPE_LOCAL)
            .putInt(KEY_ACTIVE_CHANNEL_ID, channelId)
            .apply()
        playbackPlayer.prepare()
        playbackPlayer.play()
    }

    private fun saveCurrentLocalState() {
        val preferences = playbackPreferences()
        if (preferences.getString(KEY_ACTIVE_TYPE, null) != TYPE_LOCAL) return
        val channelId = preferences.getInt(KEY_ACTIVE_CHANNEL_ID, -1)
        val playbackPlayer = player ?: return
        if (channelId <= 0 || playbackPlayer.mediaItemCount == 0 || scheduledPlaybackChannelId != null) return
        preferences.edit()
            .putInt(channelIndexKey(channelId), playbackPlayer.currentMediaItemIndex.coerceAtLeast(0))
            .putLong(channelPositionKey(channelId), playbackPlayer.currentPosition.coerceAtLeast(0L))
            .apply()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val keepInBackground = getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE)
            .getBoolean(KEY_KEEP_BACKGROUND, false)
        if (keepInBackground) {
            super.onTaskRemoved(rootIntent)
            return
        }
        saveCurrentLocalState()
        playbackPreferences().edit().putString(KEY_ACTIVE_TYPE, TYPE_STOPPED).apply()
        player?.pause()
        player?.stop()
        player?.clearMediaItems()
        stopSelf()
    }

    override fun onDestroy() {
        positionHandler.removeCallbacks(positionSaver)
        scheduleHandler.removeCallbacks(scheduleTicker)
        saveCurrentLocalState()
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun runScheduleCheck() {
        val preferences = playbackPreferences()
        if (preferences.getString(KEY_ACTIVE_TYPE, null) != TYPE_LOCAL) return
        if (scheduledPlaybackChannelId != null) return
        val channelId = preferences.getInt(KEY_ACTIVE_CHANNEL_ID, -1)
        if (channelId <= 0) return
        val now = Calendar.getInstance()
        val schedules = RamiProgrammeScheduleStore.load(this, channelId).filter { it.enabled }
        val due = schedules.firstOrNull { entry ->
            val start = mostRecentStart(now, entry)
            start != null && now.timeInMillis - start.timeInMillis in 0 until entry.durationMs
        } ?: return
        val start = mostRecentStart(now, due) ?: return
        val elapsed = (now.timeInMillis - start.timeInMillis).coerceAtLeast(0L)
        val marker = "schedule_started_" + due.id
        val dateKey = SimpleDateFormat("yyyyMMdd", Locale.ROOT).format(start.time)
        val schedulePrefs = getSharedPreferences(SCHEDULE_STATE_PREFS, MODE_PRIVATE)
        if (schedulePrefs.getString(marker, null) == dateKey) return

        val playbackPlayer = player ?: return
        scheduledResumeIndex = (playbackPlayer.currentMediaItemIndex + 1).coerceAtLeast(0)
        saveCurrentLocalState()
        scheduledPlaybackChannelId = channelId
        schedulePrefs.edit().putString(marker, dateKey).apply()
        playbackPlayer.repeatMode = Player.REPEAT_MODE_OFF
        playbackPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(due.videoUri)), elapsed)
        playbackPlayer.prepare()
        playbackPlayer.play()
    }

    private fun mostRecentStart(now: Calendar, entry: RamiProgrammeSchedule): Calendar? {
        for (daysBack in 0..7) {
            val candidate = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -daysBack)
                set(Calendar.HOUR_OF_DAY, entry.startMinute / 60)
                set(Calendar.MINUTE, entry.startMinute % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val weekday = candidate.get(Calendar.DAY_OF_WEEK)
            if (weekday in entry.weekdays && candidate.timeInMillis <= now.timeInMillis) return candidate
        }
        return null
    }

    private fun resumeNormalLocalPlaylist() {
        val channelId = scheduledPlaybackChannelId ?: return
        val channel = LocalRamiChannelStore.load(this, channelId)
        if (channel.videoUris.isEmpty()) {
            scheduledPlaybackChannelId = null
            playbackPreferences().edit().putString(KEY_ACTIVE_TYPE, TYPE_STOPPED).apply()
            return
        }
        val playbackPlayer = player ?: return
        val nextIndex = if (scheduledResumeIndex >= channel.videoUris.size) 0 else scheduledResumeIndex
        scheduledPlaybackChannelId = null
        playbackPlayer.repeatMode = Player.REPEAT_MODE_ALL
        playbackPlayer.setMediaItems(
            channel.videoUris.map { MediaItem.fromUri(Uri.parse(it)) },
            nextIndex,
            0L
        )
        playbackPreferences().edit()
            .putString(KEY_ACTIVE_TYPE, TYPE_LOCAL)
            .putInt(KEY_ACTIVE_CHANNEL_ID, channelId)
            .apply()
        playbackPlayer.prepare()
        playbackPlayer.play()
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
        private const val POSITION_SAVE_INTERVAL_MS = 5_000L
        private const val SCHEDULE_CHECK_INTERVAL_MS = 1_000L
        private const val SCHEDULE_STATE_PREFS = "rami_programme_schedule_state"
    }
}
