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
    private var playbackErrorListener: ((String) -> Unit)? = null
    var currentUrl: String? = null
        private set

    init {
        mediaPlayer.setEventListener { event ->
            if (event.type == MediaPlayer.Event.EncounteredError) {
                currentUrl = null
                playbackErrorListener?.invoke("تعذر تشغيل الفيديو بواسطة VLC")
            }
        }
    }

    fun setOnPlaybackErrorListener(listener: ((String) -> Unit)?) {
        playbackErrorListener = listener
    }

    fun attach(layout: VLCVideoLayout) {
        check(!released) { "LibVLC player has already been released" }
        if (attachedLayout === layout) return
        mediaPlayer.detachViews()
        mediaPlayer.attachViews(layout, null, false, false)
        attachedLayout = layout
    }

    fun detach(layout: VLCVideoLayout? = null) {
        if (released) return
        if (layout != null && attachedLayout !== layout) return
        mediaPlayer.detachViews()
        attachedLayout = null
    }

    fun play(url: String, startPositionMs: Long = 0L) {
        check(!released) { "LibVLC player has already been released" }
        require(url.startsWith("https://", ignoreCase = true)) {
            "Only HTTPS playback URLs are allowed"
        }
        if (currentUrl == url && mediaPlayer.isPlaying) {
            if (startPositionMs > 0L &&
                kotlin.math.abs(mediaPlayer.time - startPositionMs) > 30_000L
            ) {
                mediaPlayer.setTime(startPositionMs)
            }
            return
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
        currentUrl = url
        mediaPlayer.play()
        if (startPositionMs > 0L) {
            mediaPlayer.setTime(startPositionMs)
        }
    }

    fun pause() {
        if (!released) mediaPlayer.pause()
    }

    fun stop() {
        if (!released) {
            mediaPlayer.stop()
            currentUrl = null
        }
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
        playbackErrorListener = null
        runCatching { mediaPlayer.stop() }
        runCatching { mediaPlayer.detachViews() }
        attachedLayout = null
        currentUrl = null
        mediaPlayer.setEventListener(null)
        mediaPlayer.release()
        libVlc.release()
    }
}
