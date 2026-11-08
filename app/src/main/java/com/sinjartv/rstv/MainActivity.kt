package com.sinjartv.rstv

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.sinjartv.rstv.StreamPlaybackDiagnostic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ScreenBlack = Color(0xFF08090C)
private val PanelBlack = Color(0xFF151515)
private val PanelRaised = Color(0xFF211D12)
private val AccentRed = Color(0xFFD6B45A)
private val SoftWhite = Color(0xFFF7F3E8)
private val MutedText = Color(0xFFAAA49A)
private val Silver = Color(0xFFE3D1A0)

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
    ADULT("+18 للكبار", Icons.Outlined.Lock),
    SETTINGS("الإعدادات", Icons.Outlined.Settings)
}

private val primaryNavigationTabs = listOf(
    RSTVTab.HOME, RSTVTab.LIVE, RSTVTab.FAVORITES, RSTVTab.SOURCES, RSTVTab.WORLD
)

private enum class AdultPinMode { SETUP, VERIFY }

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
    val activity = remember(context) {
        var current: Context? = context
        while (current != null && current !is Activity) {
            current = (current as? ContextWrapper)?.baseContext
        }
        current as? Activity
    }
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
    var isFullscreenPlayer by remember { mutableStateOf(false) }
    var adultAccessGranted by remember { mutableStateOf(false) }
    var adultSectionHidden by remember { mutableStateOf(AdultAccessStore.isSectionHidden(context)) }
    var adultPinMode by remember { mutableStateOf<AdultPinMode?>(null) }
    var adultPinEntry by remember { mutableStateOf("") }
    var adultPinConfirmation by remember { mutableStateOf("") }
    var adultPinError by remember { mutableStateOf("") }
    val player = remember(context) { ExoPlayer.Builder(context).build() }
    val exoPlayer = player

    fun requestAdultAccess() {
        adultPinEntry = ""
        adultPinConfirmation = ""
        adultPinError = ""
        adultPinMode = if (AdultAccessStore.hasPin(context)) AdultPinMode.VERIFY else AdultPinMode.SETUP
    }

    fun navigateToTab(tab: RSTVTab) {
        if (tab == RSTVTab.ADULT) {
            if (adultAccessGranted) selectedTab = RSTVTab.ADULT else requestAdultAccess()
            return
        }
        adultAccessGranted = false
        selectedTab = tab
    }

    fun hideOrShowAdultSection() {
        val hidden = !adultSectionHidden
        AdultAccessStore.setSectionHidden(context, hidden)
        adultSectionHidden = hidden
        if (hidden) {
            adultAccessGranted = false
            selectedTab = RSTVTab.HOME
        } else {
            adultAccessGranted = true
            selectedTab = RSTVTab.ADULT
        }
    }

    DisposableEffect(isFullscreenPlayer, activity) {
        val window = activity?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        if (isFullscreenPlayer) {
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }

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

    fun addWorldPlaylistsToLibrary() {
        val sources = RemotePlaylistSources.worldLibraryBundleIds.mapNotNull { id ->
            RemotePlaylistSources.playlists.firstOrNull { it.id == id }
        }
        if (sources.isEmpty()) {
            remotePlaylistStatus = "لا توجد مصادر عالمية مهيأة حالياً."
            return
        }

        remotePlaylistStatus = "جارٍ تحديث مكتبة القنوات العالمية من ${sources.size} قوائم عامة..."
        scope.launch {
            val imported = mutableListOf<Pair<String, Int>>()
            val failures = mutableListOf<String>()

            sources.forEach { source ->
                runCatching {
                    val content = withContext(Dispatchers.IO) {
                        RemotePlaylistDownloader.download(source)
                    }
                    val parsed = withContext(Dispatchers.Default) {
                        M3uPlaylistParser.parse(content)
                    }
                    require(parsed.isValidPlaylist) { "المصدر لم يُرجع قائمة M3U صالحة" }
                    require(parsed.channels.isNotEmpty()) { "لم نجد روابط HTTPS صالحة في القائمة" }

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
                    imported += source.name to parsed.channels.size
                }.onFailure { error ->
                    failures += "${source.name}: ${error.message ?: "تعذر الاستيراد"}"
                }
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

            if (imported.isNotEmpty()) {
                status = "تم تنزيل حزم القنوات العالمية وحفظها في المكتبة. افتح البث المباشر لعرضها وتشغيلها."
            }

            remotePlaylistStatus = buildString {
                if (imported.isNotEmpty()) {
                    append("تم استيراد ${imported.size} من ${sources.size} مصادر عالمية؛ المكتبة تضم ${mergedSources.size} قناة فريدة.")
                } else {
                    append("لم ينجح استيراد أي مصدر عالمي.")
                }
                if (failures.isNotEmpty()) {
                    append(" تعذّر: ")
                    append(failures.joinToString(" • "))
                }
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
        if (AdultContentPolicy.isAdult(channel)) {
            status = "هذه القناة مصنّفة +18 وموقوفة حتى التحقق من ترخيص مصدرها. لا تُشغّل من القوائم العامة."
            return
        }
        val activeQueue = (if (showingSourceLibrary) sourceLibraryChannels else channels)
            .filterNot(AdultContentPolicy::isAdult)
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
        if (AdultContentPolicy.isAdult(channel)) return
        LocalLibraryStore.toggleFavorite(context, channel.streamUrl)
        favouriteRevision++
    }

    val favoriteUrls = remember(favouriteRevision) {
        LocalLibraryStore.favoriteStreams(context)
    }
    val allKnownChannels = (channels + sourceLibraryChannels)
        .groupBy { it.streamUrl }
        .values
        .map { candidates ->
            candidates.firstOrNull(AdultContentPolicy::isAdult) ?: candidates.first()
        }
    val adultStreamUrls = allKnownChannels.filter(AdultContentPolicy::isAdult)
        .map { it.streamUrl }
        .toSet()
    val adultChannels = allKnownChannels.filter(AdultContentPolicy::isAdult)
    val regularChannels = channels.filterNot { it.streamUrl in adultStreamUrls }
    val regularSourceLibraryChannels = sourceLibraryChannels.filterNot { it.streamUrl in adultStreamUrls }
    val favoriteChannels = allKnownChannels.filterNot(AdultContentPolicy::isAdult)
        .filter { it.streamUrl in favoriteUrls }
    val recentChannels = LocalLibraryStore.history(context)
        .mapNotNull { url -> allKnownChannels.firstOrNull { it.streamUrl == url && !AdultContentPolicy.isAdult(it) } }
    val visibleChannels = regularChannels.filter { channel ->
        searchQuery.isBlank() ||
            channel.name.contains(searchQuery, ignoreCase = true) ||
            channel.group.orEmpty().contains(searchQuery, ignoreCase = true)
    }
    val visibleSourceLibraryChannels = regularSourceLibraryChannels.filter { channel ->
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

    adultPinMode?.let { mode ->
        AlertDialog(
            onDismissRequest = { adultPinMode = null },
            title = {
                Text(
                    if (mode == AdultPinMode.SETUP) "إنشاء رمز حماية +18"
                    else "إدخال رمز حماية +18"
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        if (mode == AdultPinMode.SETUP)
                            "أنشئ رمزاً من 4 إلى 8 أرقام. لن يُحفظ الرمز نفسه، بل بصمة مشتقة ومملّحة."
                        else
                            "أدخل رمز PIN لفتح قسم المحتوى المصنّف للكبار.",
                        color = MutedText,
                        fontSize = 12.sp
                    )
                    OutlinedTextField(
                        value = adultPinEntry,
                        onValueChange = { value ->
                            adultPinEntry = value.filter { it.isDigit() }.take(8)
                            adultPinError = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("رمز PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                    if (mode == AdultPinMode.SETUP) {
                        OutlinedTextField(
                            value = adultPinConfirmation,
                            onValueChange = { value ->
                                adultPinConfirmation = value.filter { it.isDigit() }.take(8)
                                adultPinError = ""
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("تأكيد الرمز") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                        )
                    }
                    if (adultPinError.isNotBlank()) {
                        Text(adultPinError, color = Color(0xFFFF8A80), fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (mode == AdultPinMode.SETUP) {
                        when {
                            adultPinEntry.length !in 4..8 ->
                                adultPinError = "اختر رمزاً من 4 إلى 8 أرقام."
                            adultPinEntry != adultPinConfirmation ->
                                adultPinError = "الرمزان غير متطابقين."
                            else -> {
                                AdultAccessStore.setPin(context, adultPinEntry)
                                adultAccessGranted = true
                                selectedTab = RSTVTab.ADULT
                                adultPinMode = null
                                adultPinEntry = ""
                                adultPinConfirmation = ""
                            }
                        }
                    } else if (AdultAccessStore.verifyPin(context, adultPinEntry)) {
                        adultAccessGranted = true
                        selectedTab = RSTVTab.ADULT
                        adultPinMode = null
                        adultPinEntry = ""
                    } else {
                        adultPinEntry = ""
                        adultPinError = "الرمز غير صحيح أو أن المحاولات موقوفة مؤقتاً."
                    }
                }) {
                    Text(if (mode == AdultPinMode.SETUP) "حفظ وفتح القسم" else "فتح القسم", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { adultPinMode = null }) { Text("إلغاء") }
            },
            containerColor = PanelRaised,
            titleContentColor = SoftWhite,
            textContentColor = MutedText
        )
    }

    BackHandler(enabled = selectedTab == RSTVTab.ADULT) {
        adultAccessGranted = false
        selectedTab = RSTVTab.HOME
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ScreenBlack,
        contentColor = SoftWhite
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wideLayout = maxWidth >= 840.dp
            if (isFullscreenPlayer && selectedChannel != null) {
                ScreenBody(
                    tab = selectedTab,
                    worldChannels = worldChannels,
                    adultChannels = adultChannels,
                    isAdultSectionHidden = adultSectionHidden,
                    onHideAdultSection = ::hideOrShowAdultSection,
                    onManageAdult = ::requestAdultAccess,
                    worldCatalogStatus = worldCatalogStatus,
                    remotePlaylistStatus = remotePlaylistStatus,
                    sourceLibraryStatus = sourceLibraryStatus,
                    sourceLibraryCount = sourceLibraryCount,
                    onAddSourceToLibrary = ::addRemotePlaylistToLibrary,
                    onShowSourceLibrary = ::showSourceLibrary,
                    onAddWorldSources = ::addWorldPlaylistsToLibrary,
                    onOpenOfficialUrl = { url -> runCatching { uriHandler.openUri(url) } },
                    channels = when {
                        selectedTab == RSTVTab.FAVORITES -> favoriteChannels
                        selectedTab == RSTVTab.LIVE && showingSourceLibrary -> visibleSourceLibraryChannels
                        else -> visibleChannels
                    },
                    recentChannels = recentChannels,
                    programmes = programmes.toList(),
                    selectedChannel = selectedChannel,
                    isFullscreenPlayer = isFullscreenPlayer,
                    onExitFullscreen = { isFullscreenPlayer = false },
                    onEnterFullscreen = { isFullscreenPlayer = true },
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
                    onNavigate = ::navigateToTab,
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
                        (if (adultSectionHidden) primaryNavigationTabs else primaryNavigationTabs + RSTVTab.ADULT).forEach { tab ->
                            NavigationRailItem(
                                selected = selectedTab == tab,
                                onClick = { navigateToTab(tab) },
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
                            channelCount = regularChannels.size
                        )
                        ScreenBody(
                            tab = selectedTab,
                            worldChannels = worldChannels,
                            adultChannels = adultChannels,
                            isAdultSectionHidden = adultSectionHidden,
                            onHideAdultSection = ::hideOrShowAdultSection,
                            onManageAdult = ::requestAdultAccess,
                            worldCatalogStatus = worldCatalogStatus,
                            remotePlaylistStatus = remotePlaylistStatus,
                            sourceLibraryStatus = sourceLibraryStatus,
                            sourceLibraryCount = sourceLibraryCount,
                            onAddSourceToLibrary = ::addRemotePlaylistToLibrary,
                            onShowSourceLibrary = ::showSourceLibrary,
                            onAddWorldSources = ::addWorldPlaylistsToLibrary,
                            onOpenOfficialUrl = { url -> runCatching { uriHandler.openUri(url) } },
                            channels = when {
                                selectedTab == RSTVTab.FAVORITES -> favoriteChannels
                                selectedTab == RSTVTab.LIVE && showingSourceLibrary -> visibleSourceLibraryChannels
                                else -> visibleChannels
                            },
                            recentChannels = recentChannels,
                            programmes = programmes.toList(),
                            selectedChannel = selectedChannel,
                            isFullscreenPlayer = isFullscreenPlayer,
                            onExitFullscreen = { isFullscreenPlayer = false },
                            onEnterFullscreen = { isFullscreenPlayer = true },
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
                            onNavigate = ::navigateToTab,
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
                            channelCount = regularChannels.size
                        )
                        ScreenBody(
                            tab = selectedTab,
                            worldChannels = worldChannels,
                            adultChannels = adultChannels,
                            isAdultSectionHidden = adultSectionHidden,
                            onHideAdultSection = ::hideOrShowAdultSection,
                            onManageAdult = ::requestAdultAccess,
                            worldCatalogStatus = worldCatalogStatus,
                            remotePlaylistStatus = remotePlaylistStatus,
                            sourceLibraryStatus = sourceLibraryStatus,
                            sourceLibraryCount = sourceLibraryCount,
                            onAddSourceToLibrary = ::addRemotePlaylistToLibrary,
                            onShowSourceLibrary = ::showSourceLibrary,
                            onAddWorldSources = ::addWorldPlaylistsToLibrary,
                            onOpenOfficialUrl = { url -> runCatching { uriHandler.openUri(url) } },
                            channels = when {
                                selectedTab == RSTVTab.FAVORITES -> favoriteChannels
                                selectedTab == RSTVTab.LIVE && showingSourceLibrary -> visibleSourceLibraryChannels
                                else -> visibleChannels
                            },
                            recentChannels = recentChannels,
                            programmes = programmes.toList(),
                            selectedChannel = selectedChannel,
                            isFullscreenPlayer = isFullscreenPlayer,
                            onExitFullscreen = { isFullscreenPlayer = false },
                            onEnterFullscreen = { isFullscreenPlayer = true },
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
                            onNavigate = ::navigateToTab,
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
                        (if (adultSectionHidden) primaryNavigationTabs else primaryNavigationTabs + RSTVTab.ADULT).forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { navigateToTab(tab) },
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
private fun PlayerWatermark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.sinjar_show_logo),
        contentDescription = "SINJAR SHOW",
        modifier = modifier.size(width = 88.dp, height = 34.dp),
        alpha = 0.48f
    )
}

@Composable
private fun BrandMark() {
    Image(
        painter = painterResource(id = R.drawable.sinjar_show_logo),
        contentDescription = "SINJAR SHOW",
        modifier = Modifier.size(width = 88.dp, height = 34.dp)
    )
}

@Composable
private fun TopHeader(
    onImportPlaylist: () -> Unit,
    channelCount: Int
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
    ) {
        val logoWidth = (maxWidth * 0.18f).coerceIn(68.dp, 116.dp)
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = logoWidth + 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
        Image(
            painter = painterResource(id = R.drawable.sinjar_show_logo),
            contentDescription = "SINJAR TV SHOW",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(logoWidth)
                .aspectRatio(176f / 68f)
        )
    }
}

@Composable
private fun ScreenBody(
    tab: RSTVTab,
    worldChannels: List<WorldChannelCandidate>,
    adultChannels: List<RSTVChannel>,
    isAdultSectionHidden: Boolean,
    onHideAdultSection: () -> Unit,
    onManageAdult: () -> Unit,
    worldCatalogStatus: String,
    remotePlaylistStatus: String,
    sourceLibraryStatus: String,
    sourceLibraryCount: Int,
    onAddSourceToLibrary: (RemotePlaylistSource) -> Unit,
    onShowSourceLibrary: () -> Unit,
    onAddWorldSources: () -> Unit,
    onOpenOfficialUrl: (String) -> Unit,
    channels: List<RSTVChannel>,
    recentChannels: List<RSTVChannel>,
    programmes: List<GuideProgramme>,
    selectedChannel: RSTVChannel?,
    isFullscreenPlayer: Boolean,
    onExitFullscreen: () -> Unit,
    onEnterFullscreen: () -> Unit,
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
            onAddToLibrary = onAddSourceToLibrary,
            onShowLibrary = onShowSourceLibrary,
            onOpenOfficialUrl = onOpenOfficialUrl,
            modifier = modifier
        )
        RSTVTab.WORLD -> WorldCatalogContent(
            candidates = worldChannels,
            catalogStatus = worldCatalogStatus,
            sourceLibraryStatus = sourceLibraryStatus,
            sourceLibraryCount = sourceLibraryCount,
            remotePlaylistStatus = remotePlaylistStatus,
            onAddWorldSources = onAddWorldSources,
            onShowSourceLibrary = onShowSourceLibrary,
            modifier = modifier
        )
        RSTVTab.YOUTUBE -> YouTubeLiveContent(modifier = modifier)
        RSTVTab.MOVIES -> MoviesLibraryContent(modifier = modifier)
        RSTVTab.ADULT -> AdultContentContent(
            channels = adultChannels,
            isSectionHidden = isAdultSectionHidden,
            onToggleHidden = onHideAdultSection,
            modifier = modifier
        )
        RSTVTab.LIVE -> LiveContent(
            channels = channels,
            selectedChannel = selectedChannel,
            isFullscreenPlayer = isFullscreenPlayer,
            onExitFullscreen = onExitFullscreen,
            onEnterFullscreen = onEnterFullscreen,
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
            onManageAdult = onManageAdult,
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
                    .border(1.dp, AccentRed.copy(alpha = 0.34f), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = PanelRaised),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(13.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(62.dp)
                                .background(Color(0xFF302714), RoundedCornerShape(18.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.sinjar_show_logo),
                                contentDescription = "SINJAR SHOW",
                                modifier = Modifier.size(width = 58.dp, height = 28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("RS TV", color = AccentRed, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Text("عالم الترفيه بين يديك", color = SoftWhite, fontSize = 23.sp, fontWeight = FontWeight.Black)
                            Text("تجربة مشاهدة عالمية بتصميم ملكي", color = MutedText, fontSize = 11.sp)
                        }
                    }
                    Text(
                        "قنوات مباشرة ومصادر عالمية ومكتبة ترفيهية، في واجهة واحدة سهلة الاستخدام.",
                        color = Silver,
                        fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { onNavigate(RSTVTab.LIVE) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = ScreenBlack),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(painterResource(R.drawable.royal_play), contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("ابدأ المشاهدة", fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = onImportPlaylist) {
                            Icon(painterResource(R.drawable.royal_playlist), contentDescription = null, tint = AccentRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("إضافة قائمة", color = SoftWhite)
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("القنوات المحمّلة", channelCount.toString(), Modifier.weight(1f))
                StatCard("آخر المشاهدات", recentChannels.size.toString(), Modifier.weight(1f))
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("اكتشف RS TV", color = SoftWhite, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("اختصاراتك", color = AccentRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureCard("البث المباشر", "شاهد قنواتك وقوائمك", "مشاهدة الآن", R.drawable.royal_smart_tv, Modifier.weight(1f)) {
                    onNavigate(RSTVTab.LIVE)
                }
                FeatureCard("القنوات العالمية", "$worldChannelCount قناة مرشّحة", "استكشف العالم", R.drawable.royal_apps_grid, Modifier.weight(1f)) {
                    onNavigate(RSTVTab.WORLD)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureCard("مصادر IPTV", "قوائم بث قابلة للتحديث", "إدارة المصادر", R.drawable.royal_playlist, Modifier.weight(1f)) {
                    onNavigate(RSTVTab.SOURCES)
                }
                FeatureCard("الأفلام والمسلسلات", "اكتشف مكتبة الترفيه", "فتح المكتبة", R.drawable.royal_media_video, Modifier.weight(1f)) {
                    onNavigate(RSTVTab.MOVIES)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureCard("YouTube المباشر", "اكتشاف البث الرسمي", "استكشاف", R.drawable.royal_play, Modifier.weight(1f)) {
                    onNavigate(RSTVTab.YOUTUBE)
                }
                FeatureCard("دليل البرامج", "مواعيد وبرامج القنوات", "عرض الدليل", R.drawable.royal_apps_grid, Modifier.weight(1f)) {
                    onNavigate(RSTVTab.GUIDE)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureCard("المفضلة", "قنواتك المحفوظة", "فتح المفضلة", R.drawable.royal_account, Modifier.weight(1f)) {
                    onNavigate(RSTVTab.FAVORITES)
                }
                FeatureCard("الإعدادات", "تخصيص وإدارة التطبيق", "فتح الإعدادات", R.drawable.royal_settings, Modifier.weight(1f)) {
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
    iconRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.heightIn(min = 132.dp)
            .border(1.dp, AccentRed.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier.size(42.dp)
                    .background(Color(0xFF302714), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = title,
                    tint = AccentRed,
                    modifier = Modifier.size(27.dp)
                )
            }
            Text(title, color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(description, color = MutedText, fontSize = 10.sp, maxLines = 2)
            Text(action, color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}
private val channelCategoryOrder = listOf(
    "القنوات العربية",
    "القنوات التركية",
    "القنوات الإخبارية",
    "القنوات الرياضية",
    "الأفلام والمسلسلات",
    "قنوات الأطفال",
    "القنوات الدينية",
    "القنوات العالمية"
)

private fun normalizedChannelText(value: String): String =
    value.lowercase(Locale.ROOT)
        .replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .trim()

private fun channelCategory(channel: RSTVChannel): String {
    val group = normalizedChannelText(channel.group.orEmpty())
    val name = normalizedChannelText(channel.name)
    val country = normalizedChannelText(channel.country.orEmpty())
    val metadata = "$group $name"
    fun hasAny(vararg terms: String) = terms.any { normalizedChannelText(it) in metadata }

    return when {
        hasAny("sports", "sport", "رياضة", "رياضي") -> "القنوات الرياضية"
        hasAny("news", "اخبار", "اخباري", "نشرات") -> "القنوات الإخبارية"
        hasAny("movie", "movies", "cinema", "film", "series", "drama", "افلام", "مسلسلات", "سينما", "دراما") ->
            "الأفلام والمسلسلات"
        hasAny("kids", "children", "cartoon", "animation", "اطفال", "كرتون", "رسوم متحركة") ->
            "قنوات الأطفال"
        hasAny("religion", "religious", "islam", "quran", "christian", "ديني", "اسلام", "قران", "مسيحي", "كنيسة") ->
            "القنوات الدينية"
        country in setOf("tr", "tur", "turkey", "turkiye", "تركيا") || "تركيا" in country ->
            "القنوات التركية"
        country in setOf("dz", "bh", "km", "dj", "eg", "iq", "jo", "kw", "lb", "ly", "mr", "ma", "om", "ps", "qa", "sa", "so", "sd", "sy", "tn", "ae", "ye") ||
            listOf("السعود", "سوريا", "مصر", "العراق", "الاردن", "فلسطين", "المغرب", "الجزائر", "تونس", "لبنان", "الامارات", "الكويت", "قطر", "عمان", "اليمن", "البحرين", "ليبيا", "السودان").any { it in country } ||
            hasAny("arabic", "العربية", "عربي") ->
            "القنوات العربية"
        else -> "القنوات العالمية"
    }
}

private fun orderedChannelGroups(channels: List<RSTVChannel>): List<Pair<String, List<RSTVChannel>>> {
    val grouped = channels.groupBy(::channelCategory)
    return channelCategoryOrder.mapNotNull { category ->
        grouped[category]
            ?.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            ?.takeIf { it.isNotEmpty() }
            ?.let { category to it }
    }
}

@Composable
private fun LiveContent(
    channels: List<RSTVChannel>,
    selectedChannel: RSTVChannel?,
    isFullscreenPlayer: Boolean,
    onExitFullscreen: () -> Unit,
    onEnterFullscreen: () -> Unit,
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
    var showChannelTable by remember { mutableStateOf(false) }
    val fullscreenFocusRequester = remember { FocusRequester() }
    val numberedChannels = remember(channels) {
        orderedChannelGroups(channels)
            .flatMap { it.second }
            .distinctBy { it.streamUrl }
            .mapIndexed { index, channel -> (index + 1) to channel }
    }
    val selectedChannelNumber = numberedChannels
        .firstOrNull { it.second.streamUrl == selectedChannel?.streamUrl }
        ?.first
    var showChannelInfo by remember { mutableStateOf(false) }

    LaunchedEffect(isFullscreenPlayer, selectedChannel?.streamUrl) {
        if (isFullscreenPlayer && selectedChannel != null) {
            showChannelInfo = true
            runCatching { fullscreenFocusRequester.requestFocus() }
            delay(4_500L)
            showChannelInfo = false
        } else {
            showChannelInfo = false
        }
    }

    BackHandler(enabled = showChannelTable || (isFullscreenPlayer && selectedChannel != null)) {
        if (showChannelTable) {
            showChannelTable = false
        } else {
            onExitFullscreen()
        }
    }

    if (isFullscreenPlayer && selectedChannel != null) {
        Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        this.player = player
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                        keepScreenOn = true
                    }
                },
                update = { view ->
                    view.player = player
                    view.useController = false
                    view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    view.keepScreenOn = true
                }
            )
            if (showChannelInfo && !showChannelTable) {
                ReceiverChannelInfoOverlay(
                    number = selectedChannelNumber,
                    channel = selectedChannel,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp)
                )
            }
            PlayerWatermark(Modifier.align(Alignment.BottomEnd).padding(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(fullscreenFocusRequester)
                    .onPreviewKeyEvent { event ->
                        if (
                            event.type == KeyEventType.KeyDown &&
                            (event.key == Key.DirectionCenter || event.key == Key.Enter)
                        ) {
                            showChannelTable = true
                            true
                        } else {
                            false
                        }
                    }
                    .clickable { showChannelTable = true }
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
                TextButton(onClick = { showChannelTable = true }) {
                    Text("${channels.size} قناة", color = MutedText, fontSize = 11.sp)
                }
            }
            if (selectedChannel != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            selectedChannel.name,
                            color = SoftWhite,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        Text("LIVE", color = AccentRed, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        TextButton(onClick = onEnterFullscreen) {
                            Text("ملء الشاشة", color = Silver, fontSize = 11.sp)
                        }
                    }
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        val playerWidth = minOf(maxWidth, 760.dp)
                        val playerHeight = (playerWidth * 9f / 16f).coerceAtMost(300.dp)
                        val fittedWidth = playerHeight * 16f / 9f
                        Box(
                            modifier = Modifier
                                .width(fittedWidth)
                                .height(playerHeight)
                                .background(Color.Black, RoundedCornerShape(16.dp))
                        ) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { viewContext ->
                                    PlayerView(viewContext).apply {
                                        this.player = player
                                        useController = false
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                        setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                                        keepScreenOn = true
                                    }
                                },
                                update = { view ->
                                    view.player = player
                                    view.useController = false
                                    view.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    view.keepScreenOn = true
                                }
                            )
                            PlayerWatermark(Modifier.align(Alignment.BottomEnd).padding(10.dp))
                        }
                    }
                }
            }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("ابحث عن قناة أو مجموعة") }
            )
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
                val groupedChannels = orderedChannelGroups(channels)
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
                        items(groupChannels, key = { it.streamUrl }) { channel ->
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
    if (showChannelTable) {
        FullscreenChannelSelectorDialog(
            channels = channels,
            activeChannel = selectedChannel,
            onDismiss = { showChannelTable = false },
            onPlay = { channel ->
                showChannelTable = false
                onPlay(channel)
            }
        )
    }

}


private val channelCountryLabels = mapOf(
    "US" to "الولايات المتحدة الأمريكية", "USA" to "الولايات المتحدة الأمريكية",
    "CA" to "كندا", "CN" to "الصين", "JP" to "اليابان",
    "KR" to "كوريا الجنوبية", "RU" to "روسيا", "BR" to "البرازيل",
    "AR" to "الأرجنتين", "CL" to "تشيلي", "CO" to "كولومبيا",
    "MX" to "المكسيك", "PE" to "بيرو", "UK" to "المملكة المتحدة",
    "GB" to "المملكة المتحدة", "DE" to "ألمانيا", "FR" to "فرنسا",
    "IT" to "إيطاليا", "ES" to "إسبانيا", "PT" to "البرتغال",
    "PL" to "بولندا", "NL" to "هولندا", "SE" to "السويد", "NO" to "النرويج",
    "TR" to "تركيا"
)

private fun channelCountryLabel(channel: RSTVChannel): String {
    val raw = channel.country?.trim().orEmpty()
    if (raw.isNotBlank()) {
        val normalized = raw.uppercase(Locale.ROOT)
        return channelCountryLabels[normalized] ?: raw
    }
    return "غير محددة"
}

@Composable
private fun ReceiverChannelInfoOverlay(
    number: Int?,
    channel: RSTVChannel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(0.74f).border(
            1.dp,
            AccentRed.copy(alpha = 0.58f),
            RoundedCornerShape(14.dp)
        ),
        colors = CardDefaults.cardColors(containerColor = Color(0xE9161B23)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(number?.toString() ?: "—", color = AccentRed, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("رقم القناة", color = MutedText, fontSize = 9.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(channel.name, color = SoftWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    "${channelCountryLabel(channel)}  •  ${channel.group?.takeIf(String::isNotBlank) ?: "بث مباشر"}",
                    color = Silver,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                if (channel.sourceNames.isNotEmpty()) {
                    Text(
                        "المصدر: ${channel.sourceNames.joinToString("، ")}",
                        color = MutedText,
                        fontSize = 9.sp,
                        maxLines = 1
                    )
                }
            }
            Text("LIVE", color = AccentRed, fontWeight = FontWeight.Black, fontSize = 12.sp)
        }
    }
}

@Composable
private fun FullscreenChannelSelectorDialog(
    channels: List<RSTVChannel>,
    activeChannel: RSTVChannel?,
    onDismiss: () -> Unit,
    onPlay: (RSTVChannel) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var previewChannel by remember(activeChannel?.streamUrl) { mutableStateOf(activeChannel) }
    val numberedChannels = remember(channels) {
        orderedChannelGroups(channels)
            .flatMap { it.second }
            .distinctBy { it.streamUrl }
            .mapIndexed { index, channel -> (index + 1) to channel }
    }
    val filteredChannels = numberedChannels.filter { (_, channel) ->
        query.isBlank() ||
            channel.name.contains(query, ignoreCase = true) ||
            channelCountryLabel(channel).contains(query, ignoreCase = true) ||
            channel.group.orEmpty().contains(query, ignoreCase = true)
    }
    val previewEntry = numberedChannels.firstOrNull {
        it.second.streamUrl == previewChannel?.streamUrl
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.62f))
                    .clickable(onClick = onDismiss)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(0.94f)
                        .fillMaxWidth(0.78f)
                        .background(Color(0xE9161B23), RoundedCornerShape(20.dp))
                        .border(1.dp, AccentRed.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                        .padding(14.dp)
                        .clickable { },
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("قائمة القنوات", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                            Text("${numberedChannels.size} قناة مرقّمة", color = MutedText, fontSize = 10.sp)
                        }
                        TextButton(onClick = onDismiss) {
                            Text("إغلاق", color = Silver)
                        }
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("ابحث عن قناة أو دولة أو تصنيف") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(filteredChannels, key = { it.second.streamUrl }) { (number, channel) ->
                            val isPreview = channel.streamUrl == previewChannel?.streamUrl
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { if (it.isFocused) previewChannel = channel }
                                    .clickable { previewChannel = channel }
                                    .background(
                                        if (isPreview) Color(0x663B3424) else Color.Transparent,
                                        RoundedCornerShape(9.dp)
                                    )
                                    .border(
                                        if (isPreview) 1.dp else 0.dp,
                                        if (isPreview) AccentRed.copy(alpha = 0.8f) else Color.Transparent,
                                        RoundedCornerShape(9.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Text(
                                    number.toString(),
                                    color = AccentRed,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.width(38.dp)
                                )
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(channel.name, color = SoftWhite, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    Text(
                                        channel.group?.takeIf(String::isNotBlank) ?: "قناة",
                                        color = MutedText,
                                        fontSize = 9.sp,
                                        maxLines = 1
                                    )
                                }
                                Text(channelCountryLabel(channel), color = Silver, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                    if (previewChannel != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().border(
                                1.dp,
                                AccentRed.copy(alpha = 0.6f),
                                RoundedCornerShape(14.dp)
                            ),
                            colors = CardDefaults.cardColors(containerColor = Color(0xF020242C)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(13.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("معاينة معلومات القناة", color = Silver, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                    Text("رقم ${previewEntry?.first ?: "—"}", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Text(previewChannel!!.name, color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
                                Text(
                                    "الدولة: ${channelCountryLabel(previewChannel!!)}  •  التصنيف: ${previewChannel!!.group?.takeIf(String::isNotBlank) ?: "غير محدد"}",
                                    color = Silver,
                                    fontSize = 11.sp,
                                    maxLines = 2
                                )
                                Text(
                                    "المصدر: ${previewChannel!!.sourceNames.takeIf { it.isNotEmpty() }?.joinToString("، ") ?: "القائمة المحلية"}",
                                    color = MutedText,
                                    fontSize = 9.sp,
                                    maxLines = 2
                                )
                                Button(
                                    onClick = { onPlay(previewChannel!!) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("تشغيل القناة", color = ScreenBlack, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Text("اختر قناة لعرض رقمها وبلدها ومعلوماتها قبل التشغيل.", color = MutedText, fontSize = 11.sp)
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
            "نزّل حزم القنوات العالمية وأضفها إلى مكتبتك. تشغيل القنوات يتم حصراً من صفحة البث المباشر.",
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
                    "هذه الصفحة لتنزيل حزم القنوات وحفظها فقط؛ لا يتم تشغيل الفيديو هنا ولا تتغير قائمتك المحلية. افتح «البث المباشر» ثم «مكتبة المصادر» لمشاهدة القنوات. تُقبل روابط HTTPS فقط، وقد تتوقف بعض القنوات أو تُقيّد جغرافياً.",
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
                    }
                }
            }
            item {
                Text(OfficialDestinationCategory.SPORTS.title, color = Silver, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            items(RemotePlaylistSources.officialDestinations.filter { it.category == OfficialDestinationCategory.SPORTS }, key = { it.id }) { destination ->
                OfficialDestinationCard(destination, onOpenOfficialUrl)
            }
            item {
                Text(OfficialDestinationCategory.MOVIES.title, color = Silver, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            items(RemotePlaylistSources.officialDestinations.filter { it.category == OfficialDestinationCategory.MOVIES }, key = { it.id }) { destination ->
                OfficialDestinationCard(destination, onOpenOfficialUrl)
            }
            item {
                Text(OfficialDestinationCategory.GENERAL.title, color = Silver, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            items(RemotePlaylistSources.officialDestinations.filter { it.category == OfficialDestinationCategory.GENERAL }, key = { it.id }) { destination ->
                OfficialDestinationCard(destination, onOpenOfficialUrl)
            }
        }
    }
}

@Composable
private fun OfficialDestinationCard(
    destination: OfficialLiveDestination,
    onOpenOfficialUrl: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(destination.name, color = SoftWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(destination.description, color = MutedText, fontSize = 10.sp)
            }
            TextButton(onClick = { onOpenOfficialUrl(destination.url) }) {
                Text("فتح رسمي", color = AccentRed, fontSize = 11.sp)
            }
        }
    }
}

private data class WorldFilterOption(val key: String, val label: String)

@Composable
private fun WorldCatalogContent(
    candidates: List<WorldChannelCandidate>,
    catalogStatus: String,
    sourceLibraryStatus: String,
    sourceLibraryCount: Int,
    remotePlaylistStatus: String,
    onAddWorldSources: () -> Unit,
    onShowSourceLibrary: () -> Unit,
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
        Text(
            "استورد قوائم القنوات العالمية إلى المشغّل الداخلي، ثم تصفّح القنوات وشغّلها من داخل التطبيق.",
            color = MutedText,
            fontSize = 11.sp
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PanelRaised),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("مكتبة البث العالمية", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "يجلب التطبيق قوائم عالمية وعربية وتركية من مصادر IPTV، ويحفظها في مكتبة منفصلة دون استبدال قائمتك المحلية.",
                    color = MutedText,
                    fontSize = 11.sp
                )
                Button(
                    onClick = onAddWorldSources,
                    enabled = !remotePlaylistStatus.startsWith("جارٍ تحديث مكتبة القنوات العالمية"),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                    shape = RoundedCornerShape(11.dp)
                ) {
                    Text(
                        if (remotePlaylistStatus.startsWith("جارٍ تحديث مكتبة القنوات العالمية")) {
                            "جارٍ تنزيل القوائم..."
                        } else {
                            "تحديث واستيراد القنوات العالمية"
                        },
                        color = ScreenBlack,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = onShowSourceLibrary,
                    enabled = sourceLibraryCount > 0,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PanelBlack,
                        contentColor = SoftWhite,
                        disabledContainerColor = PanelBlack.copy(alpha = 0.45f),
                        disabledContentColor = MutedText
                    ),
                    shape = RoundedCornerShape(11.dp)
                ) {
                    Text("فتح مكتبة القنوات المستوردة (${sourceLibraryCount} مصدر)")
                }
                Text(sourceLibraryStatus, color = Silver, fontSize = 10.sp)
                Text(remotePlaylistStatus, color = SoftWhite, fontSize = 10.sp)
                Text(
                    "تُزال القنوات المكررة. توفر الرابط لا يضمن استمرار البث أو حقوق إعادة نشر المحتوى.",
                    color = MutedText,
                    fontSize = 9.sp
                )
            }
        }
        Text("دليل الدول والقنوات الرسمية", color = Silver, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
            "هذا الدليل يعرض بيانات القناة وموقعها الرسمي؛ التشغيل داخل التطبيق يكون من قوائم IPTV المستوردة أعلاه.",
            color = MutedText,
            fontSize = 10.sp
        )
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
        Text(
            "${visibleCandidates.size} / ${candidates.size} محطة في الدليل",
            color = SoftWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
        Text(catalogStatus, color = MutedText, fontSize = 10.sp)
        if (visibleCandidates.isEmpty()) {
            EmptyState(
                title = if (candidates.isEmpty()) "الدليل العالمي غير متاح" else "لا توجد نتائج",
                description = if (candidates.isEmpty()) {
                    "يمكنك مع ذلك استيراد القوائم العالمية من بطاقة مكتبة البث أعلاه."
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
                        Text("موقع القناة", color = Silver, fontSize = 11.sp)
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
    var focused by remember(channel.streamUrl) { mutableStateOf(false) }
    val focusScale by animateFloatAsState(
        targetValue = if (focused) 1.035f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "channelCardFocusScale"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer {
                scaleX = focusScale
                scaleY = focusScale
            }
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) AccentRed else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
,
        colors = CardDefaults.cardColors(containerColor = if (active || focused) PanelRaised else PanelBlack),
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
private fun AdultContentContent(
    channels: List<RSTVChannel>,
    isSectionHidden: Boolean,
    onToggleHidden: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ageRatedChannels = channels.filter { AdultContentPolicy.classify(it) == AdultContentType.AGE_RATED }
    val explicitChannels = channels.filter { AdultContentPolicy.classify(it) == AdultContentType.EXPLICIT }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("+18 للكبار فقط", color = SoftWhite, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Text(
                "قسم منفصل لا يظهر في البحث أو المفضلة أو قوائم البث العادية. يلزم رمز PIN للدخول.",
                color = MutedText,
                fontSize = 12.sp
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PanelRaised),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("تنبيه الترخيص", color = Silver, fontWeight = FontWeight.Bold)
                    Text(
                        "لا توجد حالياً حزمة IPTV مجانية ثبت أنها تملك إذناً واضحاً لبث محتوى +18. القنوات المكتشفة في قوائم M3U تبقى محجوبة ولا تعمل حتى مراجعة حقوق مصدرها. لن نستخدم روابط مسروقة أو قوائم مجهولة.",
                        color = MutedText,
                        fontSize = 12.sp
                    )
                }
            }
        }
        item {
            Text("أفلام ومسلسلات بتصنيف عمري 18+", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("يُعتمد التصنيف فقط عند وجود بيانات عمرية صريحة مثل TV-MA أو NC-17 أو 18+.", color = MutedText, fontSize = 11.sp)
        }
        if (ageRatedChannels.isEmpty()) {
            item {
                EmptyState("لا توجد قنوات معتمدة بعد", "لم يتم العثور على قناة ذات تصنيف عمري 18+ من مصدر تمت مراجعته.")
            }
        } else {
            items(ageRatedChannels, key = { it.streamUrl }) { channel ->
                AdultCandidateCard(channel, "تصنيف عمري 18+")
            }
        }
        item {
            Text("قنوات ذات محتوى صريح للبالغين", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("هذه الفئة منفصلة تماماً عن الأفلام والمسلسلات المصنّفة عمرياً.", color = MutedText, fontSize = 11.sp)
        }
        if (explicitChannels.isEmpty()) {
            item {
                EmptyState("لا توجد مصادر قانونية معتمدة", "لن نضيف قناة صريحة إلا من مشغّل رسمي يثبت السماح بالبث المجاني.")
            }
        } else {
            items(explicitChannels, key = { it.streamUrl }) { channel ->
                AdultCandidateCard(channel, "محتوى صريح — محجوب للمراجعة")
            }
        }
        item {
            Button(
                onClick = onToggleHidden,
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = ScreenBlack),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isSectionHidden) "إظهار +18 في القائمة" else "إخفاء +18 من القائمة")
            }
        }
        item {
            Text(
                "إخفاء القسم يمنع ظهوره في شريط التنقل. يمكن إدارته مجدداً من بطاقة «إدارة قسم +18» في الرئيسية، بعد إدخال PIN.",
                color = MutedText,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun AdultCandidateCard(channel: RSTVChannel, label: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        shape = RoundedCornerShape(13.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(channel.name, color = SoftWhite, fontWeight = FontWeight.Bold)
            Text(label, color = Silver, fontSize = 11.sp)
            Text(
                "المصدر: " + (channel.group ?: "قائمة مستوردة") + " • حالة البث: محجوب حتى مراجعة الحقوق",
                color = MutedText,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun AdultContentContent(
    channels: List<RSTVChannel>,
    isSectionHidden: Boolean,
    onToggleHidden: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ageRatedChannels = channels.filter { AdultContentPolicy.classify(it) == AdultContentType.AGE_RATED }
    val explicitChannels = channels.filter { AdultContentPolicy.classify(it) == AdultContentType.EXPLICIT }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("+18 للكبار فقط", color = SoftWhite, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Text(
                "قسم منفصل لا يظهر في البحث أو المفضلة أو قوائم البث العادية. يلزم رمز PIN للدخول.",
                color = MutedText,
                fontSize = 12.sp
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PanelRaised),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("تنبيه الترخيص", color = Silver, fontWeight = FontWeight.Bold)
                    Text(
                        "لا توجد حالياً حزمة IPTV مجانية ثبت أنها تملك إذناً واضحاً لبث محتوى +18. القنوات المكتشفة في قوائم M3U تبقى محجوبة ولا تعمل حتى مراجعة حقوق مصدرها. لن نستخدم روابط مسروقة أو قوائم مجهولة.",
                        color = MutedText,
                        fontSize = 12.sp
                    )
                }
            }
        }
        item {
            Text("أفلام ومسلسلات بتصنيف عمري 18+", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("يُعتمد التصنيف فقط عند وجود بيانات عمرية صريحة مثل TV-MA أو NC-17 أو 18+.", color = MutedText, fontSize = 11.sp)
        }
        if (ageRatedChannels.isEmpty()) {
            item {
                EmptyState("لا توجد قنوات معتمدة بعد", "لم يتم العثور على قناة ذات تصنيف عمري 18+ من مصدر تمت مراجعته.")
            }
        } else {
            items(ageRatedChannels, key = { it.streamUrl }) { channel ->
                AdultCandidateCard(channel, "تصنيف عمري 18+")
            }
        }
        item {
            Text("قنوات ذات محتوى صريح للبالغين", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("هذه الفئة منفصلة تماماً عن الأفلام والمسلسلات المصنّفة عمرياً.", color = MutedText, fontSize = 11.sp)
        }
        if (explicitChannels.isEmpty()) {
            item {
                EmptyState("لا توجد مصادر قانونية معتمدة", "لن نضيف قناة صريحة إلا من مشغّل رسمي يثبت السماح بالبث المجاني.")
            }
        } else {
            items(explicitChannels, key = { it.streamUrl }) { channel ->
                AdultCandidateCard(channel, "محتوى صريح — محجوب للمراجعة")
            }
        }
        item {
            Button(
                onClick = onToggleHidden,
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed, contentColor = ScreenBlack),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (isSectionHidden) "إظهار +18 في القائمة" else "إخفاء +18 من القائمة")
            }
        }
        item {
            Text(
                "إخفاء القسم يمنع ظهوره في شريط التنقل. يمكن إدارته مجدداً من بطاقة «إدارة قسم +18» في الرئيسية، بعد إدخال PIN.",
                color = MutedText,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun AdultCandidateCard(channel: RSTVChannel, label: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PanelBlack),
        shape = RoundedCornerShape(13.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(channel.name, color = SoftWhite, fontWeight = FontWeight.Bold)
            Text(label, color = Silver, fontSize = 11.sp)
            Text(
                "المصدر: " + (channel.group ?: "قائمة مستوردة") + " • حالة البث: محجوب حتى مراجعة الحقوق",
                color = MutedText,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun SettingsContent(
    status: String,
    onImportPlaylist: () -> Unit,
    onImportGuide: () -> Unit,
    onClear: (ClearAction) -> Unit,
    onManageAdult: () -> Unit,
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
            SettingsAction("إدارة قسم +18", "حماية القسم برمز PIN وإظهار القسم أو إخفاؤه", onManageAdult)
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

