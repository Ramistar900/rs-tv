package com.sinjartv.rstv

import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand

/** Commands that keep IPTV and local RAMİ TV playback on one service-owned player. */
@OptIn(UnstableApi::class)
internal object RamiPlaybackCommands {
    private const val ACTION_PLAY_IPTV = "com.sinjartv.rstv.action.PLAY_IPTV"
    private const val ACTION_PLAY_LOCAL = "com.sinjartv.rstv.action.PLAY_LOCAL"

    val PLAY_IPTV = SessionCommand(ACTION_PLAY_IPTV, Bundle.EMPTY)
    val PLAY_LOCAL = SessionCommand(ACTION_PLAY_LOCAL, Bundle.EMPTY)

    fun playIptv(
        controller: MediaController?,
        streamUrl: String,
        headers: Map<String, String> = emptyMap(),
        liveMode: Boolean = true,
        positionMs: Long = 0L
    ): Boolean {
        if (controller == null || !streamUrl.startsWith("https://")) return false
        val headerBundle = Bundle().apply {
            headers.forEach { (key, value) -> putString(key, value) }
        }
        val args = Bundle().apply {
            putString("stream_url", streamUrl)
            putBundle("headers", headerBundle)
            putBoolean("live_mode", liveMode)
            putLong("position_ms", positionMs.coerceAtLeast(0L))
        }
        controller.sendCustomCommand(PLAY_IPTV, args)
        return true
    }

    fun playLocal(controller: MediaController?, channelId: Int, videoUris: List<String>): Boolean {
        if (controller == null || channelId <= 0 || videoUris.isEmpty()) return false
        val args = Bundle().apply {
            putInt("channel_id", channelId)
            putStringArrayList("video_uris", ArrayList(videoUris))
        }
        controller.sendCustomCommand(PLAY_LOCAL, args)
        return true
    }
}
