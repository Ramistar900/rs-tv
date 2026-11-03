package com.sinjartv.rstv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ScreenBlack = Color(0xFF08090C)
private val PanelBlack = Color(0xFF17191F)
private val AccentRed = Color(0xFFD71920)
private val SoftWhite = Color(0xFFF4F4F4)
private val MutedText = Color(0xFFADB2BD)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                RSTVHome()
            }
        }
    }
}

@Composable
private fun RSTVHome() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val channels = remember { mutableStateListOf<RSTVChannel>() }
    var selectedChannel by remember { mutableStateOf<RSTVChannel?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("اختر ملف M3U أو M3U8 للبدء") }
    var skippedUnsafeLinks by remember { mutableIntStateOf(0) }

    val player = remember(context) { ExoPlayer.Builder(context).build() }
    val exoPlayer = player

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> status = "جارٍ تحميل بث القناة..."
                    Player.STATE_READY -> status = "القناة جاهزة للتشغيل"
                    Player.STATE_ENDED -> status = "انتهى البث"
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                status = "تعذر تشغيل القناة (${error.errorCodeName})"
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    val playlistPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            status = "جارٍ قراءة ملف القائمة..."
            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)
                            ?.bufferedReader(Charsets.UTF_8)
                            ?.use { reader -> M3uPlaylistParser.parse(reader.readText()) }
                            ?: throw IllegalStateException("تعذر فتح الملف المحدد")
                    }

                    if (!result.isValidPlaylist) {
                        status = "الملف المحدد ليس قائمة M3U صالحة"
                    } else {
                        exoPlayer.stop()
                        exoPlayer.clearMediaItems()
                        selectedChannel = null
                        channels.clear()
                        channels.addAll(result.channels)
                        skippedUnsafeLinks = result.skippedUnsafeLinks
                        status = if (result.channels.isEmpty()) {
                            "القائمة مقروءة، لكن لا توجد روابط HTTPS صالحة للتشغيل"
                        } else {
                            "تم تحميل ${result.channels.size} قناة"
                        }
                    }
                } catch (error: Exception) {
                    status = "تعذرت قراءة القائمة: ${error.localizedMessage ?: "خطأ غير معروف"}"
                }
            }
        }
    }

    val visibleChannels = channels.filter { channel ->
        searchQuery.isBlank() ||
            channel.name.contains(searchQuery, ignoreCase = true) ||
            channel.group.orEmpty().contains(searchQuery, ignoreCase = true)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScreenBlack,
        contentColor = SoftWhite
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RS", color = AccentRed, fontSize = 34.sp, fontWeight = FontWeight.Black)
                Text(" TV", color = SoftWhite, fontSize = 34.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "SINJAR TV SHOW",
                    color = MutedText,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { playlistPicker.launch(arrayOf("*/*")) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("استيراد قائمة M3U")
                }
                Text(
                    text = "القنوات: ${channels.size}",
                    color = MutedText,
                    fontSize = 13.sp
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("ابحث عن قناة أو مجموعة") }
            )

            selectedChannel?.let { channel ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PanelBlack),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "يتم تشغيل: ${channel.name}",
                            color = SoftWhite,
                            fontWeight = FontWeight.Bold
                        )
                        AndroidView(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .aspectRatio(16f / 9f)
                                .background(Color.Black),
                            factory = { viewContext ->
                                PlayerView(viewContext).apply {
                                    player = exoPlayer
                                    useController = true
                                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                                }
                            },
                            update = { view -> view.player = exoPlayer }
                        )
                        Text(status, color = MutedText, fontSize = 12.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status,
                    color = MutedText,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                if (skippedUnsafeLinks > 0) {
                    Text(
                        text = "تم تجاهل $skippedUnsafeLinks رابط غير آمن",
                        color = AccentRed,
                        fontSize = 11.sp
                    )
                }
            }

            if (channels.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "مكتبة القنوات فارغة",
                            color = SoftWhite,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "استورد قائمة M3U/M3U8 من جهازك لعرض قنواتك.",
                            color = MutedText,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            "تُقبل روابط HTTPS فقط في هذه المرحلة.",
                            color = MutedText,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 5.dp)
                        )
                    }
                }
            } else if (visibleChannels.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد قنوات تطابق البحث", color = MutedText)
                }
            } else {
                Text(
                    text = "قائمة القنوات",
                    color = SoftWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleChannels, key = { it.streamUrl }) { channel ->
                        Card(
                            onClick = {
                                selectedChannel = channel
                                status = "جارٍ تجهيز القناة..."
                                exoPlayer.setMediaItem(MediaItem.fromUri(channel.streamUrl))
                                exoPlayer.prepare()
                                exoPlayer.playWhenReady = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = PanelBlack),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(AccentRed, RoundedCornerShape(50))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = channel.name,
                                        color = SoftWhite,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    )
                                    channel.group?.let { group ->
                                        Text(text = group, color = MutedText, fontSize = 11.sp)
                                    }
                                }
                                Text(
                                    text = if (selectedChannel?.streamUrl == channel.streamUrl) "يعمل" else "تشغيل",
                                    color = AccentRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
