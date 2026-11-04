package com.sinjartv.rstv

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.sinjartv.rstv.StreamPlaybackDiagnostic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ScreenBlack = Color(0xFF08090C)
private val PanelBlack = Color(0xFF17191F)
private val PanelRaised = Color(0xFF20232B)
private val AccentRed = Color(0xFFD71920)
private val SoftWhite = Color(0xFFF4F4F4)
private val MutedText = Color(0xFFADB2BD)
private val Silver = Color(0xFFB7BEC9)

private enum class RSTVTab(
    val title: String,
    val icon: ImageVector
) {
    HOME("الرئيسية", Icons.Outlined.Home),
    LIVE("البث المباشر", Icons.Outlined.Tv),
    WORLD("العالمية", Icons.Outlined.Public),
    SOURCES("مصادر IPTV", Icons.Outlined.CloudDownload),
    YOUTUBE("YouTube", Icons.Outlined.PlayCircle),
    MOVIES("أفلام ومسلسلات", Icons.Outlined.VideoLibrary),
    FAVORITES("المفضلة", Icons.Outlined.Favorite),
    GUIDE("دليل البرامج", Icons.Outlined.CalendarToday),
    SETTINGS("الإعدادات", Icons.Outlined.Settings)
}

private val primaryNavigationTabs = listOf(
    RSTVTab.HOME, RSTVTab.LIVE, RSTVTab.FAVORITES, RSTVTab.SOURCES, RSTVTab.WORLD
)

private enum class ClearAction(val title: String, val message: String) {
    PLAYLIST("حذف القائمة المحفوظة", "سيتم حذف قائمة القنوات المحلية من هذا الجهاز."),
    HISTORY("مسح سجل المشاهدة", "سيتم مسح سجل القنوات التي شاهدتها."),
    FAVORITES("مسح المفضلة", "سيتم إزالة جميع القنوات من قائمة المفضلة."),
    SOURCE_LIBRARY("مسح مكتبة مصادر IPTV", "سيتم حذف نسخ قوائم المصادر المجمّعة فقط، دون حذف قائمة المستخدم أو المفضلة.")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = AccentRed,
                    secondary = Silver,
                    background = ScreenBlack,
                    surface = PanelBlack,
                    surfaceVariant = PanelRaised,
                    onPrimary = SoftWhite,
                    onBackground = SoftWhite,
                    onSurface = SoftWhite,
                    onSurfaceVariant = MutedText
                )
            ) {
                RSTVHome()
            }
        }
    }
}

@Composable
private fun RSTVHome() {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val scope = rememberCoroutineScope()
    val channels = remember { mutableStateListOf<RSTVChannel>() }
    val sourceLibraryChannels = remember { mutableStateListOf<RSTVChannel>() }
    var sourceLibraryStatus by remember { mutableStateOf("لم تتم إضافة مصادر بعد") }
    var sourceLibraryCount by remember { mutableIntStateOf(0) }
    var showingSourceLibrary by remember { mutableStateOf(false) }
    var worldChannels by remember { mutableStateOf<List<WorldChannelCandidate>>(emptyList()) }
    var worldCatalogStatus by remember { mutableStateOf("جارٍ تحميل الكتالوج العالمي...") }
    var remotePlaylistStatus by remember { mutableStateOf("اختر قائمة من المصادر لتنزيل قنواتها إلى التطبيق.") }
    val uriHandler = LocalUriHandler.current
    val programmes = remember { mutableStateListOf<GuideProgramme>() }
    var selectedChannel by remember { mutableStateOf<RSTVChannel?>(null) }
    var selectedTab by remember { mutableStateOf(RSTVTab.HOME) }
    var searchQuery by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("مرحباً بك في RS TV") }
    var skippedUnsafeLinks by remember { mutableIntStateOf(0) }
    var favouriteRevision by remember { mutableIntStateOf(0) }
    var pendingClear by remember { mutableStateOf<ClearAction?>(null) }
    val player = remember(context) { ExoPlayer.Builder(context).build() }
    val exoPlayer = player

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val currentUrl = mediaItem?.localConfiguration?.uri?.toString()
                val currentChannel = (channels + sourceLibraryChannels).firstOrNull { it.streamUrl == currentUrl }
                if (currentChannel != null) {
                    selectedChannel = currentChannel
                    status = "القناة الحالية: ${currentChannel.name}"
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                status = when (playbackState) {
                    Player.STATE_BUFFERING -> "جارٍ تحميل بث القناة..."
                    Player.STATE_READY -> "القناة جاهزة للتشغيل"
                    Player.STATE_ENDED -> "انتهى البث"
                    else -> status
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                status = "تعذر تشغيل القناة: ${error.errorCodeName}"
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            LocalLibraryStore.loadPlaylist(context)
        }?.let { saved ->
            val parsed = M3uPlaylistParser.parse(saved)
            if (parsed.isValidPlaylist) {
                channels.clear()
                channels.addAll(parsed.channels)
                skippedUnsafeLinks = parsed.skippedUnsafeLinks
                status = "تمت استعادة ${parsed.channels.size} قناة محفوظة"
            }
        }
        runCatching {
            val savedSources = withContext(Dispatchers.IO) {
                LocalLibraryStore.loadRemoteSources(context)
            }
            val mergedSources = withContext(Dispatchers.Default) {
                RemotePlaylistMerger.merge(savedSources)
            }
            sourceLibraryChannels.clear()
            sourceLibraryChannels.addAll(mergedSources)
            sourceLibraryCount = savedSources.size
            sourceLibraryStatus = "تمت استعادة ${savedSources.size} قوائم مصدر و${mergedSources.size} قناة بعد إزالة التكرار"
        }.onFailure {
            sourceLibraryStatus = "تعذر استعادة مكتبة المصادر: ${it.message ?: "خطأ غير معروف"}"
        }
        val savedGuide = withContext(Dispatchers.IO) {
            LocalLibraryStore.loadGuide(context)
        }
        if (!savedGuide.isNullOrBlank()) {
            runCatching { withContext(Dispatchers.Default) { XmltvGuideParser.parse(savedGuide) } }
                .onSuccess { programmes.addAll(it) }
                .onFailure { status = "تعذر استعادة دليل البرامج المحفوظ" }
        }
        runCatching {
            val catalogJson = withContext(Dispatchers.IO) {
                context.assets.open("candidates.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
            withContext(Dispatchers.Default) {
                WorldChannelCatalogParser.parse(catalogJson)
            }
        }.onSuccess { imported ->
            worldChannels = imported
            worldCatalogStatus = "تم تحميل ${imported.size} قناة مرشّحة من الكتالوج المضمّن"
        }.onFailure {
            worldCatalogStatus = "تعذر تحميل الكتالوج العالمي: ${it.message ?: "ملف غير متاح"}"
        }
    }

    val playlistPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            status = "جارٍ استيراد قائمة القنوات..."
            scope.launch {
                runCatching {
                    val text = withContext(Dispatchers.IO) {
                        readBoundedDocument(context, uri)
                    }
                    val parsed = withContext(Dispatchers.Default) {
                        M3uPlaylistParser.parse(text)
                    }
                    require(parsed.isValidPlaylist) { "الملف المحدد ليس قائمة M3U صالحة" }
                    withContext(Dispatchers.IO) { LocalLibraryStore.savePlaylist(context, text) }
                    exoPlayer.stop()
                    exoPlayer.clearMediaItems()
                    selectedChannel = null
                    channels.clear()
                    channels.addAll(parsed.channels)
                    skippedUnsafeLinks = parsed.skippedUnsafeLinks
                    status = if (parsed.channels.isEmpty()) {
                        "القائمة صحيحة، لكنها لا تحتوي روابط HTTPS صالحة للتشغيل"
                    } else {
                        "تم استيراد ${parsed.channels.size} قناة وحفظ القائمة على الجهاز"
                    }
                    selectedTab = RSTVTab.LIVE
                }.onFailure {
                    status = "فشل استيراد القائمة: ${it.message ?: "خطأ غير معروف"}"
                }
            }
        }
    }

    val guidePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            status = "جارٍ استيراد دليل البرامج..."
            scope.launch {
                runCatching {
                    val text = withContext(Dispatchers.IO) {
                        readBoundedDocument(context, uri)
                    }
                    val parsed = withContext(Dispatchers.Default) {
                        XmltvGuideParser.parse(text)
                    }
                    withContext(Dispatchers.IO) { LocalLibraryStore.saveGuide(context, text) }
                    programmes.clear()
                    programmes.addAll(parsed)
                    status = "تم تحميل ${parsed.size} برنامج من دليل XMLTV"
                    selectedTab = RSTVTab.GUIDE
                }.onFailure {
                    status = "تعذر استيراد دليل البرامج: ${it.message ?: "ملف غير صالح"}"
                }
            }
        }
    }

    fun loadRemotePlaylist(source: RemotePlaylistSource) {
        remotePlaylistStatus = "جارٍ جلب ${source.name}..."
        scope.launch {
            runCatching {
                val content = withContext(Dispatchers.IO) {
                    RemotePlaylistDownloader.download(source)
                }
                val parsed = withContext(Dispatchers.Default) {
                    M3uPlaylistParser.parse(content)
                }
                require(parsed.isValidPlaylist) { "المصدر لم يُرجع قائمة M3U صالحة" }
                require(parsed.channels.isNotEmpty()) {
                    "لم نجد روابط HTTPS صالحة في هذه القائمة"
                }
                withContext(Dispatchers.IO) {
                    LocalLibraryStore.savePlaylist(context, content)
                }
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                selectedChannel = null
                channels.clear()
                channels.addAll(parsed.channels)
                skippedUnsafeLinks = parsed.skippedUnsafeLinks
                val resultMessage = "تم تحميل ${parsed.channels.size} قناة من ${source.name}. تم استبدال القائمة المحلية السابقة."
                status = resultMessage
                remotePlaylistStatus = resultMessage
                selectedTab = RSTVTab.LIVE
            }.onFailure { error ->
                remotePlaylistStatus = "تعذر تحميل ${source.name}: ${error.message ?: "خطأ غير معروف"}"
            }
        }
    }

    fun addRemotePlaylistToLibrary(source: RemotePlaylistSource) {
        remotePlaylistStatus = "جارٍ إضافة ${source.name} إلى مكتبة المصادر..."
        scope.launch {
            runCatching {
                val content = withContext(Dispatchers.IO) {
                    RemotePlaylistDownloader.download(source)
                }
                val parsed = withContext(Dispatchers.Default) {
                    M3uPlaylistParser.parse(content)
                }
                require(parsed.isValidPlaylist) { "المصدر لم يُرجع قائمة M3U صالحة" }
                require(parsed.channels.isNotEmpty()) { "لم نجد روابط HTTPS صالحة في هذه القائمة" }

                val snapshot = RemoteSourceSnapshot(
                    id = source.id,
                    name = source.name,
                    provider = source.provider,
                    attribution = source.attribution,
                    content = content
                )
                withContext(Dispatchers.IO) {
                    LocalLibraryStore.saveRemoteSource(context, snapshot)
                }
                val savedSources = withContext(Dispatchers.IO) {
                    LocalLibraryStore.loadRemoteSources(context)
                }
                val mergedSources = withContext(Dispatchers.Default) {
                    RemotePlaylistMerger.merge(savedSources)
                }
                sourceLibraryChannels.clear()
                sourceLibraryChannels.addAll(mergedSources)
                sourceLibraryCount = savedSources.size
                sourceLibraryStatus = "المكتبة تضم ${savedSources.size} مصادر و${mergedSources.size} قناة فريدة"
                remotePlaylistStatus = "تمت إضافة ${parsed.channels.size} قناة من ${source.name}. قائمتك الحالية لم تتغير."
            }.onFailure { error ->
                remotePlaylistStatus = "تعذرت إضافة ${source.name}: ${error.message ?: "خطأ غير معروف"}"
            }
        }
    }

    fun showSourceLibrary() {
        showingSourceLibrary = true
        selectedChannel = null
        searchQuery = ""
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        status = "تعرض الآن مكتبة المصادر المجمّعة. قائمتك المحلية محفوظة دون تغيير."
        selectedTab = RSTVTab.LIVE
    }


    fun playChannel(channel: RSTVChannel) {
        val activeQueue = (if (showingSourceLibrary) sourceLibraryChannels else channels).toList()
        val queue = if (activeQueue.any { it.streamUrl == channel.streamUrl }) activeQueue else listOf(channel) + activeQueue
        val startIndex = queue.indexOfFirst { it.streamUrl == channel.streamUrl }.coerceAtLeast(0)
        selectedChannel = channel
        selectedTab = RSTVTab.LIVE
        status = "جارٍ تجهيز قناة ${channel.name}..."
        LocalLibraryStore.addToHistory(context, channel.streamUrl)
        val mediaSources = queue.map { item ->
            val httpFactory = DefaultHttpDataSource.Factory()
                .setDefaultRequestProperties(item.httpHeaders)
            val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
            DefaultMediaSourceFactory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(item.streamUrl))
        }
        exoPlayer.setMediaSources(mediaSources, startIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun toggleFavourite(channel: RSTVChannel) {
        LocalLibraryStore.toggleFavorite(context, channel.streamUrl)
        favouriteRevision++
    }

    val favoriteUrls = remember(favouriteRevision) {
        LocalLibraryStore.favoriteStreams(context)
    }
    val allKnownChannels = (channels + sourceLibraryChannels).distinctBy { it.streamUrl }
    val favoriteChannels = allKnownChannels.filter { it.streamUrl in favoriteUrls }
    val recentChannels = LocalLibraryStore.history(context)
        .mapNotNull { url -> allKnownChannels.firstOrNull { it.streamUrl == url } }
    val visibleChannels = channels.filter { channel ->
        searchQuery.isBlank() ||
            channel.name.contains(searchQuery, ignoreCase = true) ||
            channel.group.orEmpty().contains(searchQuery, ignoreCase = true)
    }
    val visibleSourceLibraryChannels = sourceLibraryChannels.filter { channel ->
        searchQuery.isBlank() ||
            channel.name.contains(searchQuery, ignoreCase = true) ||
            channel.group.orEmpty().contains(searchQuery, ignoreCase = true)
    }

    pendingClear?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingClear = null },
            title = { Text(action.title) },
            text = { Text(action.message) },
            confirmButton = {
                TextButton(onClick = {
                    when (action) {
                        ClearAction.PLAYLIST -> {
                            LocalLibraryStore.clearPlaylist(context)
                            channels.clear()
                            exoPlayer.stop()
                            exoPlayer.clearMediaItems()
                            selectedChannel = null
                            status = "تم حذف القائمة المحلية"
                        }
                        ClearAction.HISTORY -> {
                            LocalLibraryStore.clearHistory(context)
                            status = "تم مسح سجل المشاهدة"
                        }
                        ClearAction.FAVORITES -> {
                            LocalLibraryStore.clearFavorites(context)
                            favouriteRevision++
                            status = "تم مسح المفضلة"
                        }
                        ClearAction.SOURCE_LIBRARY -> {
                            LocalLibraryStore.clearRemoteSources(context)
                            sourceLibraryChannels.clear()
                            sourceLibraryCount = 0
                            sourceLibraryStatus = "تم مسح مكتبة المصادر"
                            status = "تم حذف مكتبة مصادر IPTV دون المساس بقائمة المستخدم"
                        }
                    }
                    pendingClear = null
                }) { Text("تأكيد", color = AccentRed) }
            },
            dismissButton = {
                TextButton(onClick = { pendingClear = null }) { Text("إلغاء") }
            },
            containerColor = PanelRaised,
            titleContentColor = SoftWhite,
            textContentColor = MutedText
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScreenBlack,
        contentColor = SoftWhite
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wideLayout = maxWidth >= 840.dp
            if (isLandscape && selectedChannel != null) {
                ScreenBody(
                    tab = selectedTab,
                    worldChannels = worldChannels,
                    worldCatalogStatus = worldCatalogStatus,
                    remotePlaylistStatus = remotePlaylistStatus,
                    sourceLibraryStatus = sourceLibraryStatus,
                    sourceLibraryCount = sourceLibraryCount,
                    onLoadRemotePlaylist = ::loadRemotePlaylist,
                    onAddSourceToLibrary = ::addRemotePlaylistToLibrary,
                    onShowSourceLibrary = ::showSourceLibrary,
                    onOpenOfficialUrl = { url -> runCatching { uriHandler.openUri(url) } },
                    channels = when {
                        selectedTab == RSTVTab.FAVORITES -> favoriteChannels
                        selectedTab == RSTVTab.LIVE && showingSourceLibrary -> visibleSourceLibraryChannels
                        else -> visibleChannels
                    },
                    recentChannels = recentChannels,
                    programmes = programmes.toList(),
                    selectedChannel = selectedChannel,
                    isLandscape = isLandscape,
                    onExitFullscreen = {
                        exoPlayer.pause()
                        selectedChannel = null
                    },
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onPlay = ::playChannel,
                    onToggleFavorite = ::toggleFavourite,
                    isFavorite = { it.streamUrl in favoriteUrls },
                    player = exoPlayer,
                    status = status,
                    skippedUnsafeLinks = skippedUnsafeLinks,
                    showingSourceLibrary = showingSourceLibrary,
                    onShowUserPlaylist = {
                        showingSourceLibrary = false
                        selectedChannel = null
                        searchQuery = ""
                        status = "تمت العودة إلى قائمة المستخدم المحلية"
                    },
                    onNavigate = { selectedTab = it },
                    onImportPlaylist = { playlistPicker.launch(arrayOf("*/*")) },
                    onImportGuide = { guidePicker.launch(arrayOf("*/*")) },
                    onClear = { pendingClear = it },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (wideLayout) {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(
                        containerColor = PanelBlack,
                        contentColor = SoftWhite,
                        header = {
                            Column(
                                modifier = Modifier.padding(vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                BrandMark()
                                Text("RS TV", color = SoftWhite, fontWeight = FontWeight.Black)
                            }
                        }
                    ) {
                        primaryNavigationTabs.forEach { tab ->
                            NavigationRailItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.title) },
                                label = { Text(tab.title, fontSize = 10.sp) },
                                alwaysShowLabel = true
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 18.dp)
                    ) {
                        TopHeader(
                            onImportPlaylist = { playlistPicker.launch(arrayOf("*/*")) },
                            channelCount = channels.size
                        )
                        ScreenBody(
                            tab = selectedTab,
                            worldChannels = worldChannels,
                            worldCatalogStatus = worldCatalogStatus,
                            remotePlaylistStatus = remotePlaylistStatus,
                            sourceLibraryStatus = sourceLibraryStatus,
                            sourceLibraryCount = sourceLibraryCount,
                            onLoadRemotePlaylist = ::loadRemotePlaylist,
                            onAddSourceToLibrary = ::addRemotePlaylistToLibrary,
                            onShowSourceLibrary = ::showSourceLibrary,
                            onOpenOfficialUrl = { url -> runCatching { uriHandler.openUri(url) } },
                            channels = when {
                                selectedTab == RSTVTab.FAVORITES -> favoriteChannels
                                selectedTab == RSTVTab.LIVE && showingSourceLibrary -> visibleSourceLibraryChannels
                                else -> visibleChannels
                            },
                            recentChannels = recentChannels,
                            programmes = programmes.toList(),
                            selectedChannel = selectedChannel,
                            isLandscape = isLandscape,
                            onExitFullscreen = {
                                exoPlayer.pause()
                                selectedChannel = null
                            },
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            onPlay = ::playChannel,
                            onToggleFavorite = ::toggleFavourite,
                            isFavorite = { it.streamUrl in favoriteUrls },
                            player = exoPlayer,
                            status = status,
                            skippedUnsafeLinks = skippedUnsafeLinks,
                            showingSourceLibrary = showingSourceLibrary,
                            onShowUserPlaylist = {
                                showingSourceLibrary = false
                                selectedChannel = null
                                searchQuery = ""
                                status = "تمت العودة إلى قائمة المستخدم المحلية"
                            },
                            onNavigate = { selectedTab = it },
                            onImportPlaylist = { playlistPicker.launch(arrayOf("*/*")) },
                            onImportGuide = { guidePicker.launch(arrayOf("*/*")) },
                            onClear = { pendingClear = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        TopHeader(
                            onImportPlaylist = { playlistPicker.launch(arrayOf("*/*")) },
                            channelCount = channels.size
                        )
                        ScreenBody(
                            tab = selectedTab,
                            worldChannels = worldChannels,
                            worldCatalogStatus = worldCatalogStatus,
                            remotePlaylistStatus = remotePlaylistStatus,
                            sourceLibraryStatus = sourceLibraryStatus,
                            sourceLibraryCount = sourceLibraryCount,
                            onLoadRemotePlaylist = ::loadRemotePlaylist,
                            onAddSourceToLibrary = ::addRemotePlaylistToLibrary,
                            onShowSourceLibrary = ::showSourceLibrary,
                            onOpenOfficialUrl = { url -> runCatching { uriHandler.openUri(url) } },
                            channels = when {
                                selectedTab == RSTVTab.FAVORITES -> favoriteChannels
                                selectedTab == RSTVTab.LIVE && showingSourceLibrary -> visibleSourceLibraryChannels
                                else -> visibleChannels
                            },
                            recentChannels = recentChannels,
                            programmes = programmes.toList(),
                            selectedChannel = selectedChannel,
                            isLandscape = isLandscape,
                            onExitFullscreen = {
                                exoPlayer.pause()
                                selectedChannel = null
                            },
                            searchQuery = searchQuery,
                            onSearchChange = { searchQuery = it },
                            onPlay = ::playChannel,
                            onToggleFavorite = ::toggleFavourite,
                            isFavorite = { it.streamUrl in favoriteUrls },
                            player = exoPlayer,
                            status = status,
                            skippedUnsafeLinks = skippedUnsafeLinks,
                            showingSourceLibrary = showingSourceLibrary,
                            onShowUserPlaylist = {
                                showingSourceLibrary = false
                                selectedChannel = null
                                searchQuery = ""
                                status = "تمت العودة إلى قائمة المستخدم المحلية"
                            },
                            onNavigate = { selectedTab = it },
                            onImportPlaylist = { playlistPicker.launch(arrayOf("*/*")) },
                            onImportGuide = { guidePicker.launch(arrayOf("*/*")) },
                            onClear = { pendingClear = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    NavigationBar(
                        containerColor = PanelBlack,
                        contentColor = SoftWhite
                    ) {
                        primaryNavigationTabs.forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.title) },
                                label = { Text(tab.title, fontSize = 9.sp) },
                                alwaysShowLabel = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandMark() {
    Image(
        painter = painterResource(id = R.drawable.ic_rs_tv),
        contentDescription = "شعار RS TV",
        modifier = Modifier.size(46.dp)
    )
}

@Composable
private fun TopHeader(
    onImportPlaylist: () -> Unit,
    channelCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BrandMark()
        Column(modifier = Modifier.weight(1f)) {
            Text("RS TV", color = SoftWhite, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Text("SINJAR TV SHOW", color = MutedText, fontSize = 9.sp, letterSpacing = 2.sp)
        }
        Text(
            text = "$channelCount قناة",
            color = Silver,
            fontSize = 11.sp
        )
        Button(
            onClick = onImportPlaylist,
            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("استيراد")
        }
    }
}

@Composable
private fun ScreenBody(
    tab: RSTVTab,
    worldChannels: List<WorldChannelCandidate>,
    worldCatalogStatus: String,
    remotePlaylistStatus: String,
    sourceLibraryStatus: String,
    sourceLibraryCount: Int,
    onLoadRemotePlaylist: (RemotePlaylistSource) -> Unit,
    onAddSourceToLibrary: (RemotePlaylistSource) -> Unit,
    onShowSourceLibrary: () -> Unit,
    onOpenOfficialUrl: (String) -> Unit,
    channels: List<RSTVChannel>,
    recentChannels: List<RSTVChannel>,
    programmes: List<GuideProgramme>,
    selectedChannel: RSTVChannel?,
    isLandscape: Boolean,
    onExitFullscreen: () -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onPlay: (RSTVChannel) -> Unit,
    onToggleFavorite: (RSTVChannel) -> Unit,
    isFavorite: (RSTVChannel) -> Boolean,
    player: ExoPlayer,
    status: String,
    skippedUnsafeLinks: Int,
    showingSourceLibrary: Boolean,
    onShowUserPlaylist: () -> Unit,
    onNavigate: (RSTVTab) -> Unit,
    onImportPlaylist: () -> Unit,
    onImportGuide: () -> Unit,
    onClear: (ClearAction) -> Unit,
    modifier: Modifier = Modifier
) {
    when (tab) {
        RSTVTab.HOME -> HomeContent(
            channelCount = channels.size,
            worldChannelCount = worldChannels.size,
            recentChannels = recentChannels,
            onPlay = onPlay,
            onNavigate = onNavigate,
            onImportPlaylist = onImportPlaylist,
            modifier = modifier
        )
        RSTVTab.SOURCES -> OnlineSourcesContent(
            status = remotePlaylistStatus,
            sourceLibraryStatus = sourceLibraryStatus,
            sourceLibraryCount = sourceLibraryCount,
            onLoadPlaylist = onLoadRemotePlaylist,
            onAddToLibrary = onAddSourceToLibrary,
            onShowLibrary = onShowSourceLibrary,
            onOpenOfficialUrl = onOpenOfficialUrl,
            modifier = modifier
        )
        RSTVTab.WORLD -> WorldCatalogContent(
            candidates = worldChannels,
            catalogStatus = worldCatalogStatus,
            modifier = modifier
        )
        RSTVTab.YOUTUBE -> YouTubeLiveContent(modifier = modifier)
        RSTVTab.MOVIES -> MoviesLibraryContent(modifier = modifier)
        RSTVTab.LIVE -> LiveContent(
            channels = channels,
            selectedChannel = selectedChannel,
            isLandscape = isLandscape,
            onExitFullscreen = onExitFullscreen,
            searchQuery = searchQuery,
            onSearchChange = onSearchChange,
            onPlay = onPlay,
            onToggleFavorite = onToggleFavorite,
            isFavorite = isFavorite,
            player = player,
            status = status,
            skippedUnsafeLinks = skippedUnsafeLinks,
            showingSourceLibrary = showingSourceLibrary,
            onShowUserPlaylist = onShowUserPlaylist,
            modifier = modifier
        )
        RSTVTab.FAVORITES -> ChannelLibraryContent(
            title = "القنوات المفضلة",
            subtitle = "قنواتك المحفوظة على هذا الجهاز",
            channels = channels,
            onPlay = onPlay,
            onToggleFavorite = onToggleFavorite,
            isFavorite = isFavorite,
            modifier = modifier
        )
        RSTVTab.GUIDE -> GuideContent(
            programmes = programmes,
            selectedChannel = selectedChannel,
            onImportGuide = onImportGuide,
            modifier = modifier
        )
        RSTVTab.SETTINGS -> SettingsContent(
            status = status,
            onImportPlaylist = onImportPlaylist,
            onImportGuide = onImportGuide,
            onClear = onClear,
            modifier = modifier
        )
    }
}

@Composable
private fun HomeContent(
    channelCount: Int,
    worldChannelCount: Int,
    recentChannels: List<RSTVChannel>,
    onPlay: (RSTVChannel) -> Unit,
    onNavigate: (RSTVTab) -> Unit,
    onImportPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PanelRaised),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("عالم التلفزيون بين يديك", color = SoftWhite, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text(
                        "قوائم البث المباشر، دليل البرامج، ومصادر قنوات الإنترنت في مكان واحد.",
                        color = MutedText,
                        fontSize = 14.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onImportPlaylist,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                        ) { Text("إضافة قائمة قنوات") }
                        TextButton(onClick = { onNavigate(RSTVTab.LIVE) }) {
                            Text("البث المباشر", color = SoftWhite)
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("القنوات", channelCount.toString(), Modifier.weight(1f))
                StatCard("المشاهدة الأخيرة", recentChannels.size.toString(), Modifier.weight(1f))
            }
        }
        item {
            Text("استكشف RS TV", color = SoftWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureCard("البث المباشر", "قنواتك المستوردة", "فتح", Modifier.weight(1f)) {
                    onNavigate(RSTVTab.LIVE)
                }
                FeatureCard("دليل البرامج", "ملفات XMLTV", "فتح", Modifier.weight(1f)) {
                    onNavigate(RSTVTab.GUIDE)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureCard("القنوات العالمية", "${worldChannelCount} قناة مرشّحة من مصادر دولية", "استعراض", Modifier.weight(1f)) {
                    onNavigate(RSTVTab.WORLD)
                }
                FeatureCard("YouTube المباشر", "اكتشاف البث عبر YouTube الرسمي", "استكشاف", Modifier.weight(1f)) {
                    onNavigate(RSTVTab.YOUTUBE)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureCard("مصادر IPTV", "قوائم عالمية وعربية وتركية قابلة للتحديث", "تحميل القنوات", Modifier.weight(1f)) {
                    onNavigate(RSTVTab.SOURCES)
                }
                FeatureCard("الأفلام والمسلسلات", "كتالوج معلوماتي منفصل عن حقوق المشاهدة", "فتح المكتبة", Modifier.weight(1f)) {
                    onNavigate(RSTVTab.MOVIES)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureCard("المفضلة", "قنواتك المختارة", "فتح", Modifier.weight(1f)) {
                    onNavigate(RSTVTab.FAVORITES)
                }
                FeatureCard("الإعدادات", "إدارة القوائم ودليل البرامج", "فتح", Modifier.weight(1f)) {
                    onNavigate(RSTVTab.SETTINGS)
                }
            }
        }
        if (recentChannels.isNotEmpty()) {
            item {
                Text("تابع المشاهدة", color = SoftWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            items(recentChannels.take(8), key = { it.streamUrl }) { channel ->
                CompactChannelCard(channel = channel, onClick = { onPlay(channel) })
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, color = AccentRed, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(label, color = MutedText, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    description: String,
    action: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.heightIn(min = 112.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(description, color = MutedText, fontSize = 11.sp)
            Text(action, color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun LiveContent(
    channels: List<RSTVChannel>,
    selectedChannel: RSTVChannel?,
    isLandscape: Boolean,
    onExitFullscreen: () -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onPlay: (RSTVChannel) -> Unit,
    onToggleFavorite: (RSTVChannel) -> Unit,
    isFavorite: (RSTVChannel) -> Boolean,
    player: ExoPlayer,
    status: String,
    skippedUnsafeLinks: Int,
    showingSourceLibrary: Boolean,
    onShowUserPlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = isLandscape && selectedChannel != null) {
        onExitFullscreen()
    }

    if (isLandscape && selectedChannel != null) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        this.player = player
                        useController = true
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        keepScreenOn = true
                    }
                },
                update = { view ->
                    view.player = player
                    view.keepScreenOn = true
                }
            )
            TextButton(
                onClick = onExitFullscreen,
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.65f),
                    contentColor = SoftWhite
                )
            ) { Text("رجوع للقنوات") }
            Text(
                selectedChannel.name,
                color = SoftWhite,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).padding(14.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    } else {
        Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BrandMark()
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (showingSourceLibrary) "مكتبة مصادر IPTV المجمّعة" else "البث المباشر",
                        color = SoftWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (showingSourceLibrary) {
                        TextButton(onClick = onShowUserPlaylist) {
                            Text("العودة إلى قائمتي المحلية", color = AccentRed)
                        }
                    }
                }
                Text("${channels.size} قناة", color = MutedText, fontSize = 11.sp)
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("ابحث عن قناة أو مجموعة") }
            )
            if (selectedChannel != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PanelBlack),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                selectedChannel.name,
                                color = SoftWhite,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text("LIVE", color = AccentRed, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val playerHeight = (maxHeight * 0.44f).coerceIn(170.dp, 340.dp)
                            AndroidView(
                                modifier = Modifier.fillMaxWidth().height(playerHeight),
                                factory = { viewContext ->
                                    PlayerView(viewContext).apply {
                                        this.player = player
                                        useController = true
                                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                                        keepScreenOn = true
                                    }
                                },
                                update = { view ->
                                    view.player = player
                                    view.keepScreenOn = true
                                }
                            )
                        }
                        Text(status, color = MutedText, fontSize = 11.sp)
                        StreamPlaybackDiagnostic(selectedChannel.streamUrl, selectedChannel.httpHeaders)
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("القنوات المتاحة: ${channels.size}", color = MutedText, fontSize = 12.sp, modifier = Modifier.weight(1f))
                if (skippedUnsafeLinks > 0) {
                    Text("تجاهلنا $skippedUnsafeLinks رابطاً غير متوافق أو غير آمن", color = AccentRed, fontSize = 10.sp)
                }
            }
            if (channels.isEmpty()) {
                if (searchQuery.isBlank()) {
                    EmptyState(
                        title = "لا توجد قنوات بعد",
                        description = "استورد قائمة M3U أو M3U8 من جهازك لعرض قنواتك هنا."
                    )
                } else {
                    EmptyState("لا توجد نتائج", "جرّب اسماً أو تصنيفاً آخر.")
                }
            } else {
                val groupedChannels = channels
                    .groupBy { it.group?.takeIf(String::isNotBlank) ?: "غير مصنّفة" }
                    .toSortedMap(String.CASE_INSENSITIVE_ORDER)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedChannels.forEach { (groupName, groupChannels) ->
                        item(key = "group-$groupName") {
                            Text(
                                "$groupName  •  ${groupChannels.size}",
                                color = Silver,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }
                        items(groupChannels.sortedBy { it.name.lowercase(Locale.ROOT) }, key = { it.streamUrl }) { channel ->
                            ChannelCard(
                                channel = channel,
                                active = selectedChannel?.streamUrl == channel.streamUrl,
                                favorite = isFavorite(channel),
                                onPlay = { onPlay(channel) },
                                onToggleFavorite = { onToggleFavorite(channel) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineSourcesContent(
    status: String,
    sourceLibraryStatus: String,
    sourceLibraryCount: Int,
    onLoadPlaylist: (RemotePlaylistSource) -> Unit,
    onAddToLibrary: (RemotePlaylistSource) -> Unit,
    onShowLibrary: () -> Unit,
    onOpenOfficialUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("مصادر القنوات عبر الإنترنت", color = SoftWhite, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text(
            "قوائم عالمية وعربية وتركية ومصادر بث رسمية. اختر مصدراً لتنزيل قائمته وتشغيل القنوات في RS TV.",
            color = MutedText,
            fontSize = 11.sp
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PanelRaised),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(status, color = SoftWhite, fontSize = 11.sp)
                Text(sourceLibraryStatus, color = Silver, fontSize = 10.sp)
                Button(
                    onClick = onShowLibrary,
                    enabled = sourceLibraryCount > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("عرض المكتبة المجمّعة (${sourceLibraryCount} مصدر)")
                }
                Text(
                    "إضافة مصدر إلى المكتبة لا تستبدل قائمتك الحالية. التشغيل المنفرد فقط يستبدل القائمة النشطة. تُقبل روابط HTTPS فقط؛ وقد تتوقف القنوات أو تُقيّد جغرافياً، والإتاحة العامة لا تثبت حقوق إعادة البث.",
                    color = MutedText,
                    fontSize = 10.sp
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("قوائم IPTV القابلة للتحديث", color = Silver, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            items(RemotePlaylistSources.playlists, key = { it.id }) { source ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PanelBlack),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(source.name, color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(source.description, color = MutedText, fontSize = 11.sp)
                        Text("المصدر: ${source.provider}", color = Silver, fontSize = 10.sp)
                        source.attribution?.let {
                            Text(it, color = MutedText, fontSize = 9.sp)
                        }
                        Button(
                            onClick = { onAddToLibrary(source) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("إضافة إلى المكتبة المجمّعة")
                        }
                        TextButton(onClick = { onLoadPlaylist(source) }) {
                            Text("تشغيل منفرد واستبدال القائمة النشطة", color = Silver, fontSize = 11.sp)
                        }
                    }
                }
            }
            item {
                Text("البث من المواقع الرسمية", color = Silver, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            items(RemotePlaylistSources.officialDestinations, key = { it.id }) { destination ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PanelBlack),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(destination.name, color = SoftWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(destination.description, color = MutedText, fontSize = 10.sp)
                        }
                        TextButton(onClick = { onOpenOfficialUrl(destination.url) }) {
                            Text("فتح رسمي", color = AccentRed, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

private data class WorldFilterOption(val key: String, val label: String)

@Composable
private fun WorldCatalogContent(
    candidates: List<WorldChannelCandidate>,
    catalogStatus: String,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf("ALL") }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var selectedLanguage by remember { mutableStateOf("ALL") }

    val countries = remember(candidates) {
        listOf(WorldFilterOption("ALL", "كل الدول")) +
            candidates.distinctBy { it.countryCode }
                .sortedBy { it.countryName }
                .map { WorldFilterOption(it.countryCode, "${it.countryFlag} ${it.countryName}") }
    }
    val categories = remember(candidates) {
        val labels = linkedMapOf<String, String>()
        candidates.forEach { channel ->
            channel.categoryIds.forEachIndexed { index, id ->
                labels.putIfAbsent(id, channel.categoryLabels.getOrNull(index) ?: id)
            }
        }
        listOf(WorldFilterOption("ALL", "كل التصنيفات")) +
            labels.map { (id, label) -> WorldFilterOption(id, label) }.sortedBy { it.label }
    }
    val languages = remember(candidates) {
        val labels = linkedMapOf<String, String>()
        candidates.forEach { channel ->
            channel.languageSearchValues.forEachIndexed { index, code ->
                labels.putIfAbsent(code, channel.languageLabels.getOrNull(index) ?: code)
            }
        }
        listOf(WorldFilterOption("ALL", "كل اللغات")) +
            labels.map { (code, label) -> WorldFilterOption(code, label) }.sortedBy { it.label }
    }
    val visibleCandidates = candidates.filter { channel ->
        val matchesQuery = query.isBlank() ||
            channel.name.contains(query, ignoreCase = true) ||
            channel.alternativeNames.any { it.contains(query, ignoreCase = true) } ||
            channel.countryName.contains(query, ignoreCase = true)
        val matchesCountry = selectedCountry == "ALL" || channel.countryCode == selectedCountry
        val matchesCategory = selectedCategory == "ALL" || selectedCategory in channel.categoryIds
        val matchesLanguage = selectedLanguage == "ALL" || selectedLanguage in channel.languageSearchValues
        matchesQuery && matchesCountry && matchesCategory && matchesLanguage
    }
    val uriHandler = LocalUriHandler.current

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("القنوات العالمية", color = SoftWhite, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text("تصفح قنوات الإنترنت حسب الدولة واللغة والتصنيف", color = MutedText, fontSize = 11.sp)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("ابحث عن قناة أو دولة") }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            WorldFilterDropdown(
                title = "الدولة",
                selectedKey = selectedCountry,
                options = countries,
                onSelected = { selectedCountry = it },
                modifier = Modifier.weight(1f)
            )
            WorldFilterDropdown(
                title = "التصنيف",
                selectedKey = selectedCategory,
                options = categories,
                onSelected = { selectedCategory = it },
                modifier = Modifier.weight(1f)
            )
        }
        WorldFilterDropdown(
            title = "اللغة",
            selectedKey = selectedLanguage,
            options = languages,
            onSelected = { selectedLanguage = it },
            modifier = Modifier.fillMaxWidth()
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PanelRaised),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${visibleCandidates.size} / ${candidates.size} قناة مرشّحة", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(catalogStatus, color = MutedText, fontSize = 10.sp)
                Text(
                    "هذه بيانات اكتشاف أولية. لا يوجد تشغيل مباشر قبل مراجعة الحقوق والتحقق من المصدر.",
                    color = AccentRed,
                    fontSize = 10.sp
                )
            }
        }
        if (visibleCandidates.isEmpty()) {
            EmptyState(
                title = if (candidates.isEmpty()) "الكتالوج العالمي غير متاح" else "لا توجد نتائج",
                description = if (candidates.isEmpty()) {
                    "تأكد من تضمين ملف القنوات العالمي في نسخة التطبيق."
                } else {
                    "غيّر الدولة أو اللغة أو التصنيف أو عبارة البحث."
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visibleCandidates, key = { it.id }) { candidate ->
                    WorldChannelCard(candidate) {
                        candidate.websiteUrl?.let { url ->
                            runCatching { uriHandler.openUri(url) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorldFilterDropdown(
    title: String,
    selectedKey: String,
    options: List<WorldFilterOption>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.key == selectedKey }?.label ?: title
    Box(modifier) {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("$title: $selectedLabel", color = SoftWhite, maxLines = 1, fontSize = 11.sp)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 340.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, fontSize = 12.sp) },
                    onClick = {
                        onSelected(option.key)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun WorldChannelCard(
    channel: WorldChannelCandidate,
    onOpenOfficialWebsite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(channel.name, color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "${channel.countryFlag} ${channel.countryName} • ${channel.countryCode}",
                        color = MutedText,
                        fontSize = 10.sp
                    )
                }
                Text("مرشّحة", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            if (channel.languageLabels.isNotEmpty()) {
                Text("اللغات: ${channel.languageLabels.joinToString("، ")}", color = Silver, fontSize = 10.sp)
            }
            if (channel.categoryLabels.isNotEmpty()) {
                Text("التصنيفات: ${channel.categoryLabels.joinToString(" • ")}", color = MutedText, fontSize = 10.sp)
            }
            Text(
                if (channel.candidateStreamCount > 0) {
                    "${channel.candidateStreamCount} رابط بث مبدئي • غير معتمد للتشغيل"
                } else {
                    "لا يوجد رابط بث ضمن بيانات المصدر"
                },
                color = MutedText,
                fontSize = 10.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "لم تُراجع حقوق البث بعد",
                    color = AccentRed,
                    fontSize = 10.sp,
                    modifier = Modifier.weight(1f)
                )
                if (channel.websiteUrl != null) {
                    TextButton(onClick = onOpenOfficialWebsite) {
                        Text("الموقع الرسمي", color = Silver, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelLibraryContent(
    title: String,
    subtitle: String,
    channels: List<RSTVChannel>,
    onPlay: (RSTVChannel) -> Unit,
    onToggleFavorite: (RSTVChannel) -> Unit,
    isFavorite: (RSTVChannel) -> Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = SoftWhite, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MutedText, fontSize = 12.sp)
        if (channels.isEmpty()) {
            EmptyState("القائمة فارغة", "أضف قنوات إلى المفضلة من صفحة البث المباشر.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(channels, key = { it.streamUrl }) { channel ->
                    ChannelCard(
                        channel = channel,
                        active = false,
                        favorite = isFavorite(channel),
                        onPlay = { onPlay(channel) },
                        onToggleFavorite = { onToggleFavorite(channel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelCard(
    channel: RSTVChannel,
    active: Boolean,
    favorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (active) PanelRaised else PanelBlack),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier.size(34.dp).background(AccentRed.copy(alpha = 0.16f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Tv, contentDescription = null, tint = AccentRed)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(channel.name, color = SoftWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(channel.group ?: "بدون تصنيف", color = MutedText, fontSize = 10.sp)
                if (channel.sourceNames.isNotEmpty()) {
                    Text(
                        "المصادر: ${channel.sourceNames.joinToString("، ")}",
                        color = Silver,
                        fontSize = 9.sp,
                        maxLines = 2
                    )
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    Icons.Outlined.Favorite,
                    contentDescription = if (favorite) "إزالة من المفضلة" else "إضافة إلى المفضلة",
                    tint = if (favorite) AccentRed else MutedText
                )
            }
            TextButton(onClick = onPlay) {
                Text(if (active) "يعمل" else "تشغيل", color = AccentRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CompactChannelCard(channel: RSTVChannel, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Tv, contentDescription = null, tint = AccentRed)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(channel.name, color = SoftWhite, fontWeight = FontWeight.SemiBold)
                Text(channel.group ?: "قناة", color = MutedText, fontSize = 10.sp)
            }
            Text("متابعة", color = AccentRed, fontSize = 11.sp)
        }
    }
}

@Composable
private fun GuideContent(
    programmes: List<GuideProgramme>,
    selectedChannel: RSTVChannel?,
    onImportGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val matching = if (selectedChannel?.tvgId.isNullOrBlank()) {
        programmes
    } else {
        programmes.filter { it.channelId == selectedChannel?.tvgId }
    }
    val currentAndUpcoming = matching.filter { it.endMillis > System.currentTimeMillis() }
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("دليل البرامج", color = SoftWhite, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("XMLTV • الآن وما سيأتي لاحقاً", color = MutedText, fontSize = 11.sp)
            }
            Button(
                onClick = onImportGuide,
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
            ) { Text("استيراد XMLTV") }
        }
        if (programmes.isEmpty()) {
            EmptyState("لا يوجد دليل برامج بعد", "استورد ملف XMLTV من مزودك. اربط البرامج بالقنوات باستخدام tvg-id.")
        } else if (matching.isEmpty()) {
            EmptyState(
                "لا توجد برامج مرتبطة بهذه القناة",
                "تأكد من تطابق tvg-id في قائمة M3U مع channel id في ملف XMLTV."
            )
        } else if (currentAndUpcoming.isEmpty()) {
            EmptyState("لا توجد برامج حالية أو قادمة", "ملف XMLTV المحمّل لا يحتوي جدولاً مستقبلياً لهذه القناة.")
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentAndUpcoming, key = { "${it.channelId}-${it.startMillis}-${it.title}" }) { programme ->
                    ProgrammeCard(programme)
                }
            }
        }
    }
}

@Composable
private fun ProgrammeCard(programme: GuideProgramme) {
    val formatter = remember { SimpleDateFormat("dd/MM  HH:mm", Locale.getDefault()) }
    val start = formatter.format(Date(programme.startMillis))
    val end = formatter.format(Date(programme.endMillis))
    val now = System.currentTimeMillis()
    val programmeState = when {
        now in programme.startMillis until programme.endMillis -> "يعرض الآن"
        programme.startMillis > now -> "لاحقاً"
        else -> "انتهى"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        shape = RoundedCornerShape(13.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(programme.title, color = SoftWhite, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(programmeState, color = if (programmeState == "يعرض الآن") AccentRed else Silver, fontSize = 10.sp)
            }
            Text("$start  —  $end  •  ${programme.channelId}", color = MutedText, fontSize = 11.sp)
            programme.description?.let {
                Text(it, color = MutedText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SettingsContent(
    status: String,
    onImportPlaylist: () -> Unit,
    onImportGuide: () -> Unit,
    onClear: (ClearAction) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("الإعدادات", color = SoftWhite, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text("إدارة مكتبتك المحلية وتجربة التشغيل", color = MutedText, fontSize = 12.sp)
        }
        item {
            SettingsAction("استيراد / استبدال قائمة M3U", "يتم حفظ القائمة داخل مساحة التطبيق الخاصة", onImportPlaylist)
        }
        item {
            SettingsAction("استيراد دليل XMLTV", "ربط جدول البرامج بمعرّفات القنوات", onImportGuide)
        }
        item {
            SettingsAction("حذف القائمة المحفوظة", "لا يؤثر على ملفاتك الأصلية", { onClear(ClearAction.PLAYLIST) })
        }
        item {
            SettingsAction("مسح سجل المشاهدة", "حذف سجل آخر القنوات التي شغّلتها", { onClear(ClearAction.HISTORY) })
        }
        item {
            SettingsAction("مسح المفضلة", "إزالة القنوات المحفوظة كمفضلة", { onClear(ClearAction.FAVORITES) })
        }
        item {
            SettingsAction("مسح مكتبة مصادر IPTV", "حذف النسخ المجمّعة فقط مع إبقاء قائمة المستخدم والمفضلة", { onClear(ClearAction.SOURCE_LIBRARY) })
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PanelBlack),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("حالة التطبيق", color = SoftWhite, fontWeight = FontWeight.Bold)
                    Text(status, color = MutedText, fontSize = 11.sp)
                    Text("أمان الروابط: HTTPS فقط", color = Silver, fontSize = 11.sp)
                    Text("التخزين: محلي على هذا الجهاز", color = Silver, fontSize = 11.sp)
                }
            }
        }
        item {
            Text("RS TV • Sinjar TV Show", color = MutedText, fontSize = 11.sp)
        }
    }
}

@Composable
private fun SettingsAction(title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        shape = RoundedCornerShape(13.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = SoftWhite, fontWeight = FontWeight.SemiBold)
                Text(description, color = MutedText, fontSize = 10.sp)
            }
            Text("›", color = AccentRed, fontSize = 24.sp)
        }
    }
}

@Composable
private fun EmptyState(title: String, description: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.Tv, contentDescription = null, tint = AccentRed, modifier = Modifier.size(38.dp))
            Text(title, color = SoftWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(description, color = MutedText, fontSize = 12.sp)
        }
    }
}

private fun readBoundedDocument(context: Context, uri: Uri): String {
    val maxBytes = 15 * 1024 * 1024
    val output = ByteArrayOutputStream()
    val input = context.contentResolver.openInputStream(uri)
        ?: throw IllegalStateException("تعذر فتح الملف المحدد")
    input.use { stream ->
        val buffer = ByteArray(8 * 1024)
        var total = 0
        while (true) {
            val count = stream.read(buffer)
            if (count < 0) break
            total += count
            require(total <= maxBytes) { "حجم الملف أكبر من 15 ميغابايت" }
            output.write(buffer, 0, count)
        }
    }
    return output.toByteArray().toString(Charsets.UTF_8)
}

@Composable
private fun YouTubeLiveContent(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    var query by remember { mutableStateOf("قنوات مباشرة") }
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("البث المباشر عبر YouTube", color = SoftWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "ابحث وافتح نتائج البث من منصة YouTube الرسمية. لا يشغّل RS TV فيديوهات YouTube عبر روابط غير رسمية ولا يتجاوز قيود المنصة.",
            color = MutedText,
            fontSize = 13.sp
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("اسم القناة أو موضوع البث") }
        )
        Button(
            onClick = {
                val encoded = Uri.encode(query.trim().ifBlank { "قنوات مباشرة" })
                uriHandler.openUri("https://www.youtube.com/results?search_query=$encoded")
            },
            enabled = query.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
        ) {
            Text("فتح البحث الرسمي في YouTube")
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PanelBlack),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("تكامل API داخل التطبيق: غير مفعّل بعد", color = SoftWhite, fontWeight = FontWeight.Bold)
                Text(
                    "لإضافة نتائج مباشرة داخل RS TV نحتاج مفتاح YouTube Data API محفوظاً كسرّ في بيئة البناء/الخادم، ثم مراجعة شروط API وطريقة التشغيل الرسمية. لم نضع مفتاحاً داخل التطبيق.",
                    color = MutedText,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun MoviesLibraryContent(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("الأفلام والمسلسلات", color = SoftWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "قسم مستقل لبيانات الأفلام والمسلسلات: العناوين، الملخصات، التصنيفات، الملصقات، وسنوات الإصدار.",
            color = MutedText,
            fontSize = 13.sp
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PanelBlack),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("مصدر البيانات الوصفية: TMDB", color = SoftWhite, fontWeight = FontWeight.Bold)
                Text(
                    "لم يتم ربط API بعد. يتطلب ذلك إعداد TMDB_API_KEY بطريقة آمنة وإضافة إشعار الإسناد المطلوب. بيانات TMDB لا توفر ملفات مشاهدة ولا تمنح حقوق بث الأفلام أو الحلقات.",
                    color = MutedText,
                    fontSize = 12.sp
                )
                TextButton(onClick = {
                    uriHandler.openUri("https://www.themoviedb.org/")
                }) {
                    Text("زيارة TMDB", color = AccentRed)
                }
            }
        }
        Text(
            "حالة المكتبة: بانتظار إعداد مصدر البيانات ومراجعة روابط المشاهدة القانونية لكل مزوّد على حدة.",
            color = Silver,
            fontSize = 12.sp
        )
    }
}

