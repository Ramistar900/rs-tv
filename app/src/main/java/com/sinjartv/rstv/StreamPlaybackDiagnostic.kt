package com.sinjartv.rstv

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.delay

private const val DiagnosticTimeoutMs = 20_000L

private fun describePlaybackError(error: PlaybackException): String {
    val causes = generateSequence(error.cause) { it.cause }.take(6).toList()
    val httpError = causes.filterIsInstance<HttpDataSource.InvalidResponseCodeException>().firstOrNull()
    val causeTypes = causes.map { it.javaClass.simpleName }.filter { it.isNotBlank() }.distinct()

    return buildString {
        append("فشل الاختبار: ${error.errorCodeName} (${error.errorCode})")
        if (httpError != null) {
            append("\\nحالة الخادم: HTTP ${httpError.responseCode}")
        }
        if (causeTypes.isNotEmpty()) {
            append("\\nتفاصيل السبب: ${causeTypes.joinToString(" ← ")}")
        }
    }
}

@Composable
internal fun StreamPlaybackDiagnostic(streamUrl: String) {
    val context = LocalContext.current
    val diagnosticPlayer = remember(context, streamUrl) {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
        }
    }
    var isTesting by remember(streamUrl) { mutableStateOf(false) }
    var result by remember(streamUrl) {
        mutableStateOf("اختبار اختياري صامت: يتحقق من جاهزية البث وظهور أول إطار فيديو.")
    }
    var firstFrameRendered by remember(streamUrl) { mutableStateOf(false) }

    DisposableEffect(diagnosticPlayer) {
        val playerListener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY && isTesting) {
                    result = "اتصل المصدر وأصبح المشغّل جاهزاً؛ نتحقق من ظهور الصورة..."
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isTesting = false
                result = describePlaybackError(error)
                diagnosticPlayer.pause()
            }
        }
        val analyticsListener = object : AnalyticsListener {
            override fun onRenderedFirstFrame(
                eventTime: AnalyticsListener.EventTime,
                output: Any,
                renderTimeMs: Long
            ) {
                if (isTesting) {
                    firstFrameRendered = true
                    isTesting = false
                    result = "نجح الاختبار: ظهر أول إطار فيديو. بقيت القناة صامتة أثناء الفحص."
                    diagnosticPlayer.pause()
                }
            }
        }
        diagnosticPlayer.addListener(playerListener)
        diagnosticPlayer.addAnalyticsListener(analyticsListener)
        onDispose {
            diagnosticPlayer.removeListener(playerListener)
            diagnosticPlayer.removeAnalyticsListener(analyticsListener)
            diagnosticPlayer.release()
        }
    }

    LaunchedEffect(isTesting, streamUrl) {
        if (isTesting) {
            delay(DiagnosticTimeoutMs)
            if (isTesting) {
                isTesting = false
                result = if (diagnosticPlayer.playbackState == Player.STATE_READY) {
                    "وصل البث وأصبح المشغّل جاهزاً، لكن لم يظهر إطار فيديو خلال 20 ثانية."
                } else {
                    "انتهت مهلة الاختبار بعد 20 ثانية دون جاهزية المشغّل."
                }
                diagnosticPlayer.pause()
            }
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    enabled = !isTesting,
                    onClick = {
                        firstFrameRendered = false
                        result = "جارٍ اختبار القناة بصمت..."
                        isTesting = true
                        diagnosticPlayer.stop()
                        diagnosticPlayer.clearMediaItems()
                        diagnosticPlayer.volume = 0f
                        diagnosticPlayer.setMediaItem(MediaItem.fromUri(streamUrl))
                        diagnosticPlayer.prepare()
                        diagnosticPlayer.playWhenReady = true
                    }
                ) {
                    Text(if (isTesting) "جارٍ الفحص..." else "اختبار تشغيل صامت")
                }
                if (isTesting) {
                    Text("  20 ثانية كحد أقصى", fontSize = 10.sp)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AndroidView(
                    modifier = Modifier.width(112.dp).height(63.dp),
                    factory = { viewContext ->
                        PlayerView(viewContext).apply {
                            player = diagnosticPlayer
                            useController = false
                            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                        }
                    },
                    update = { it.player = diagnosticPlayer }
                )
                Text(result, fontSize = 11.sp, modifier = Modifier.weight(1f))
            }
        }
    }
}
