package com.sinjartv.rstv

import android.content.Intent
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Owns RAMİ TV 1 playback independently from the Compose screen/activity lifecycle.
 * IPTV playback continues to use its existing player in MainActivity.
 */
class RamiTvOnePlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val playbackPlayer = ExoPlayer.Builder(this).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }
        player = playbackPlayer
        mediaSession = MediaSession.Builder(this, playbackPlayer).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_LIST -> {
                val uris = intent.getStringArrayListExtra(EXTRA_VIDEO_URIS).orEmpty()
                if (uris.isNotEmpty()) {
                    val items = uris.map { uri ->
                        MediaItem.Builder()
                            .setUri(uri)
                            .setMediaMetadata(
                                MediaMetadata.Builder()
                                    .setTitle("RAMİ TV 1")
                                    .build()
                            )
                            .build()
                    }
                    player?.apply {
                        setMediaItems(items)
                        repeatMode = Player.REPEAT_MODE_ALL
                        prepare()
                        play()
                    }
                }
            }
            ACTION_STOP -> {
                player?.pause()
                player?.stop()
                player?.clearMediaItems()
                stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        player?.run {
            release()
            player = null
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY_LIST = "com.sinjartv.rstv.ramitvone.PLAY_LIST"
        const val ACTION_STOP = "com.sinjartv.rstv.ramitvone.STOP"
        const val EXTRA_VIDEO_URIS = "com.sinjartv.rstv.ramitvone.VIDEO_URIS"
    }
}
