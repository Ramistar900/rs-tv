package com.sinjartv.rstv

import android.content.Context
import android.net.Uri
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.util.VLCVideoLayout

/**
 * Owns the embedded VideoLAN engine for streams that need broader codec/container support.
 *
 * Call attach() from the video view lifecycle and close() when the owning screen is disposed.
 * The supplied URL must already be a short-lived, authorized playback URL; never pass Google
 * refresh tokens or other long-lived credentials to the player.
 */
internal class EmbeddedVlcPlayer(context: Context) : AutoCloseable {
    private val libVlc = LibVLC(
        context.applicationContext,
        arrayListOf(
            "--no-video-title-show",
            "--network-caching=2000"
        )
    )
    private val mediaPlayer = MediaPlayer(libVlc)
    private var attachedLayout: VLCVideoLayout? = null
    private var released = false

    fun attach(layout: VLCVideoLayout) {
        check(!released) { "LibVLC player has already been released" }
        if (attachedLayout === layout) return
        mediaPlayer.detachViews()
        mediaPlayer.attachViews(layout, null, false, false)
        attachedLayout = layout
    }

    fun play(url: String, startPositionMs: Long = 0L) {
        check(!released) { "LibVLC player has already been released" }
        require(url.startsWith("https://", ignoreCase = true)) {
            "Only HTTPS playback URLs are allowed"
        }
        val media = Media(libVlc, Uri.parse(url))
        try {
            media.setHWDecoderEnabled(true, false)
            media.addOption(":network-caching=2000")
            media.addOption(":http-user-agent=SINJAR-TV Android TV")
            mediaPlayer.setMedia(media)
        } finally {
            media.release()
        }
        mediaPlayer.play()
        if (startPositionMs > 0L) {
            mediaPlayer.setTime(startPositionMs)
        }
    }

    fun pause() {
        if (!released) mediaPlayer.pause()
    }

    fun stop() {
        if (!released) mediaPlayer.stop()
    }

    fun seekTo(positionMs: Long) {
        if (!released) mediaPlayer.setTime(positionMs.coerceAtLeast(0L))
    }

    fun isPlaying(): Boolean = !released && mediaPlayer.isPlaying

    fun currentPositionMs(): Long = if (released) 0L else mediaPlayer.time.coerceAtLeast(0L)

    fun durationMs(): Long = if (released) 0L else mediaPlayer.length.coerceAtLeast(0L)

    override fun close() {
        if (released) return
        released = true
        runCatching { mediaPlayer.stop() }
        runCatching { mediaPlayer.detachViews() }
        attachedLayout = null
        mediaPlayer.release()
        libVlc.release()
    }
}
