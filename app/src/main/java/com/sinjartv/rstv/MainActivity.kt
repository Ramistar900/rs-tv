package com.sinjartv.rstv

import com.royal.adaptive.LocalRoyalAdaptiveProfile
import com.royal.adaptive.RoyalAdaptiveHost

import com.sinjartv.rstv.royalsounds.RoyalUiSound
import com.sinjartv.rstv.royalsounds.RoyalUiSounds

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
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
import androidx.compose.material.icons.outlined.VideoLibrary
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    SOURCES("مصادر IPTV", Icons.Outlined.CloudDownload),
    MOVIES("أفلام ومسلسلات", Icons.Outlined.VideoLibrary),
    FAVORITES("المفضلة", Icons.Outlined.Favorite),
    GUIDE("دليل البرامج", Icons.Outlined.CalendarToday),
    SETTINGS("الإعدادات", Icons.Outlined.Settings)
}

private val primaryNavigationTabs = listOf(
    RSTVTab.HOME, RSTVTab.LIVE, RSTVTab.FAVORITES, RSTVTab.SOURCES
)

private enum class ClearAction(val title: String, val message: String) {
    PLAYLIST("حذف القائمة المحفوظة", "سيتم حذف قائمة القنوات المحلية من هذا الجهاز."),
    HISTORY("مسح سجل المشاهدة", "سيتم مسح سجل القنوات التي شاهدتها."),
    FAVORITES("مسح المفضلة", "سيتم إزالة جميع القنوات من قائمة المفضلة."),
    SOURCE_LIBRARY("مسح مكتبة مصادر IPTV", "سيتم حذف نسخ قوائم المصادر المجمّعة فقط، دون حذف قائمة المستخدم أو المفضلة."),
}

private data class DerivedChannelCollections(
    val adultStreamKeys: Set<String> = emptySet(),
    val channelByStreamUrl: Map<String, RSTVChannel> = emptyMap(),
    val regularChannels: List<RSTVChannel> = emptyList(),
    val regularSourceLibraryChannels: List<RSTVChannel> = emptyList(),
    val favoriteChannels: List<RSTVChannel> = emptyList(),
    val recentChannels: List<RSTVChannel> = emptyList(),
    val visibleChannels: List<RSTVChannel> = emptyList(),
    val visibleSourceLibraryChannels: List<RSTVChannel> = emptyList()
)

private fun deriveChannelCollections(
    userChannels: List<RSTVChannel>,
    sourceChannels: List<RSTVChannel>,
    favoriteUrls: Set<String>,
    historyUrls: List<String>,
    searchQuery: String
): DerivedChannelCollections {
    val knownByKey = linkedMapOf<String, RSTVChannel>()
    (userChannels + sourceChannels).forEach { channel ->
        val key = RemotePlaylistMerger.canonicalStreamKey(channel.streamUrl, channel.httpHeaders)
        val existing = knownByKey[key]
        if (existing == null || (!AdultContentPolicy.isAdult(existing) && AdultContentPolicy.isAdult(channel))) {
            knownByKey[key] = channel
        }
    }

    val adultKeys = knownByKey.filterValues(AdultContentPolicy::isAdult).keys
    val regularUser = userChannels.filter { channel ->
        RemotePlaylistMerger.canonicalStreamKey(channel.streamUrl, channel.httpHeaders) !in adultKeys
    }
    val regularSources = sourceChannels.filter { channel ->
        RemotePlaylistMerger.canonicalStreamKey(channel.streamUrl, channel.httpHeaders) !in adultKeys
    }
    val knownByStreamUrl = knownByKey.values.associateBy { it.streamUrl }
    val favorites = knownByKey.values.filter { !AdultContentPolicy.isAdult(it) && it.streamUrl in favoriteUrls }
    val recent = historyUrls.mapNotNull { knownByStreamUrl[it] }
        .filterNot(AdultContentPolicy::isAdult)
        .distinctBy { it.streamUrl }
    val query = searchQuery.trim()

    return DerivedChannelCollections(
        adultStreamKeys = adultKeys,
        channelByStreamUrl = knownByStreamUrl,
        regularChannels = regularUser,
        regularSourceLibraryChannels = regularSources,
        favoriteChannels = favorites,
        recentChannels = recent,
        visibleChannels = regularUser.filter { channel ->
            query.isBlank() ||
                channel.name.contains(query, ignoreCase = true) ||
                channel.group.orEmpty().contains(query, ignoreCase = true)
        },
        visibleSourceLibraryChannels = regularSources.filter { channel ->
            query.isBlank() ||
                channel.name.contains(query, ignoreCase = true) ||
                channel.group.orEmpty().contains(query, ignoreCase = true)
        }
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RoyalUiSounds.initialize(applicationContext, setOf(
            RoyalUiSound.NAV_BACK,
            RoyalUiSound.NAV_SELECT,
            RoyalUiSound.ACTION_ERROR,
            RoyalUiSound.NAV_OPEN_MENU,
            RoyalUiSound.ACTION_WARNING,
            RoyalUiSound.TV_CHANNEL_CHANGE,
            RoyalUiSound.ACTION_REMOVE,
            RoyalUiSound.ACTION_FAVORITE,
            RoyalUiSound.ACTION_DELETE,
            RoyalUiSound.NAV_TV_FOCUS,
            RoyalUiSound.NAV_FOCUS_MOVE,
            RoyalUiSound.ACTION_DOWNLOAD,
            RoyalUiSound.MEDIA_FULLSCREEN_EXIT,
            RoyalUiSound.MEDIA_FULLSCREEN_ENTER,
            RoyalUiSound.ACTION_ADD
        ))
        setContent {
            RoyalAdaptiveHost {
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

    override fun onDestroy() {
        RoyalUiSounds.release()
        super.onDestroy()
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (event.action == android.view.KeyEvent.ACTION_DOWN && event.repeatCount == 0 &&
            event.keyCode == android.view.KeyEvent.KEYCODE_BACK
        ) {
            RoyalUiSounds.play(RoyalUiSound.NAV_BACK)
        }
        return super.dispatchKeyEvent(event)
    }
}

@Composable
private fun RSTVHome() {
    val context = LocalContext.current
    val adaptiveProfile = LocalRoyalAdaptiveProfile.current
    // Shared detection uses Android TV capabilities and the current window size.
    val isTelevision = adaptiveProfile.isTelevision
    val isTablet = adaptiveProfile.isTablet
    val activity = remember(context) {
        var current: Context? = context
        while (current != null && current !is Activity) {
            current = (current as? ContextWrapper)?.baseContext
        }
        current as? Activity
    }
    val scope = rememberCoroutineScope()
    val libraryWriteMutex = remember { Mutex() }
    val channels = remember { mutableStateListOf<RSTVChannel>() }
    val sourceLibraryChannels = remember { mutableStateListOf<RSTVChannel>() }
    var sourceLibraryStatus by remember { mutableStateOf("لم تتم إضافة مصادر بعد") }
    var sourceLibraryCount by remember { mutableIntStateOf(0) }
    var showingSourceLibrary by remember { mutableStateOf(false) }
    var countryPackageSources by remember { mutableStateOf(RemotePlaylistSources.countryPackages(emptyList()) + RemotePlaylistSources.douboCountryPackages()) }
    var remotePlaylistStatus by remember { mutableStateOf("اختر قائمة من المصادر لتنزيل قنواتها إلى التطبيق.") }
    val programmes = remember { mutableStateListOf<GuideProgramme>() }
    var selectedChannel by remember { mutableStateOf<RSTVChannel?>(null) }
    var selectedTab by remember { mutableStateOf(RSTVTab.HOME) }
    val backgroundPrefs = remember(context) { context.getSharedPreferences("sinjar_ui", Context.MODE_PRIVATE) }
    var selectedBackground by remember {
        mutableIntStateOf(backgroundPrefs.getInt("background_res", R.drawable.sinjar_bg_royal_waves))
    }
    var searchQuery by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("مرحباً بك في RS TV") }
    var skippedUnsafeLinks by remember { mutableIntStateOf(0) }
    var favouriteRevision by remember { mutableIntStateOf(0) }
    var libraryRevision by remember { mutableIntStateOf(0) }
    var historyRevision by remember { mutableIntStateOf(0) }
    var derivedCollections by remember { mutableStateOf(DerivedChannelCollections()) }
    var favoriteUrls by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(libraryRevision, favouriteRevision, historyRevision, searchQuery) {
        val userSnapshot = channels.toList()
        val sourceSnapshot = sourceLibraryChannels.toList()
        val (favoriteSnapshot, historySnapshot) = withContext(Dispatchers.IO) {
            LocalLibraryStore.favoriteStreams(context) to LocalLibraryStore.history(context)
        }
        favoriteUrls = favoriteSnapshot
        derivedCollections = withContext(Dispatchers.Default) {
            deriveChannelCollections(
                userChannels = userSnapshot,
                sourceChannels = sourceSnapshot,
                favoriteUrls = favoriteSnapshot,
                historyUrls = historySnapshot,
                searchQuery = searchQuery
            )
        }
    }
    var pendingClear by remember { mutableStateOf<ClearAction?>(null) }
    var isFullscreenPlayer by remember { mutableStateOf(false) }
    val player = remember(context) { ExoPlayer.Builder(context).build() }
    val exoPlayer = player

    fun navigateToTab(tab: RSTVTab) {
        if (selectedTab != tab) {
            RoyalUiSounds.play(context, RoyalUiSound.NAV_SELECT)
        }
        selectedTab = tab
    }

    fun isRestrictedAdultChannel(channel: RSTVChannel): Boolean {
        val targetKey = RemotePlaylistMerger.canonicalStreamKey(channel.streamUrl, channel.httpHeaders)
        return AdultContentPolicy.isAdult(channel) || targetKey in derivedCollections.adultStreamKeys
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
                val currentChannel = currentUrl?.let { derivedCollections.channelByStreamUrl[it] }
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
                RoyalUiSounds.play(context, RoyalUiSound.ACTION_ERROR)
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
            val parsed = withContext(Dispatchers.Default) {
                M3uPlaylistParser.parse(saved)
            }
            if (parsed.isValidPlaylist) {
                channels.clear()
                channels.addAll(parsed.channels)
                libraryRevision++
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
            libraryRevision++
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
                WorldChannelCatalogParser.parseCountryCodes(catalogJson)
            }
        }.onSuccess { countryCodes ->
            countryPackageSources = RemotePlaylistSources.countryPackages(countryCodes) + RemotePlaylistSources.douboCountryPackages()
        }.onFailure {
            countryPackageSources = RemotePlaylistSources.countryPackages(emptyList()) + RemotePlaylistSources.douboCountryPackages()
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
                    libraryRevision++
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

    fun addRemotePlaylistPackageToLibrary(playlistPackage: RemotePlaylistPackage) {
        remotePlaylistStatus = "جارٍ جمع باقة ${playlistPackage.name} من ${playlistPackage.sources.size} مصادر..."
        scope.launch {
            val downloadedSources = mutableListOf<String>()
            val failedSources = mutableListOf<String>()
            var importedChannelCount = 0

            playlistPackage.sources.distinctBy { it.playlistUrl }.forEach { source ->
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
                    parsed.channels.size
                }.onSuccess { count ->
                    importedChannelCount += count
                    downloadedSources += source.provider
                }.onFailure { error ->
                    failedSources += "${source.provider}: ${error.message ?: "تعذر التنزيل"}"
                }
            }

            if (downloadedSources.isEmpty()) {
                remotePlaylistStatus = "تعذر تنزيل باقة ${playlistPackage.name}: ${failedSources.joinToString("؛ ")}"
                return@launch
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
                libraryRevision++
                sourceLibraryCount = savedSources.size
                sourceLibraryStatus = "المكتبة تضم ${savedSources.size} مصادر محفوظة و${mergedSources.size} قناة فريدة بعد إزالة التكرار"
                remotePlaylistStatus = buildString {
                    append("تم جمع باقة ${playlistPackage.name}: ${downloadedSources.size} من ${playlistPackage.sources.size} مصادر، و${importedChannelCount} مدخلاً قبل إزالة التكرار. ")
                    append("أصبحت المكتبة تضم ${mergedSources.size} قناة فريدة.")
                    if (failedSources.isNotEmpty()) append(" تعذر بعض المصادر: ${failedSources.joinToString("؛ ")}")
                    append(" قائمتك المحلية لم تتغير.")
                }
            }.onFailure { error ->
                remotePlaylistStatus = "تم تنزيل بعض مصادر ${playlistPackage.name} لكن تعذر تحديث المكتبة: ${error.message ?: "خطأ غير معروف"}"
            }
        }
    }

    fun showSourceLibrary() {
        RoyalUiSounds.play(context, RoyalUiSound.NAV_OPEN_MENU)
        showingSourceLibrary = true
        selectedChannel = null
        searchQuery = ""
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        status = "تعرض الآن مكتبة المصادر المجمّعة. قائمتك المحلية محفوظة دون تغيير."
        selectedTab = RSTVTab.LIVE
    }


    fun playChannel(channel: RSTVChannel) {
        if (isRestrictedAdultChannel(channel)) {
            RoyalUiSounds.play(context, RoyalUiSound.ACTION_WARNING)
            status = "هذه القناة مصنّفة +18 وموقوفة حتى التحقق من ترخيص مصدرها. لا تُشغّل من القوائم العامة."
            return
        }
        RoyalUiSounds.play(context, RoyalUiSound.TV_CHANNEL_CHANGE)
        selectedChannel = channel
        selectedTab = RSTVTab.LIVE
        status = "جارٍ تجهيز قناة ${channel.name}..."
        scope.launch {
            libraryWriteMutex.withLock {
                withContext(Dispatchers.IO) {
                    LocalLibraryStore.addToHistory(context, channel.streamUrl)
                }
            }
            historyRevision++
        }
        val httpFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(channel.httpHeaders)
        val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
        val mediaSource = DefaultMediaSourceFactory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(channel.streamUrl))
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun toggleFavourite(channel: RSTVChannel) {
        if (isRestrictedAdultChannel(channel)) return
        RoyalUiSounds.play(
            context,
            if (channel.streamUrl in favoriteUrls) RoyalUiSound.ACTION_REMOVE
            else RoyalUiSound.ACTION_FAVORITE
        )
        scope.launch {
            libraryWriteMutex.withLock {
                withContext(Dispatchers.IO) {
                    LocalLibraryStore.toggleFavorite(context, channel.streamUrl)
                }
            }
            favouriteRevision++
        }
    }

    val regularChannels = derivedCollections.regularChannels
    val regularSourceLibraryChannels = derivedCollections.regularSourceLibraryChannels
    val favoriteChannels = derivedCollections.favoriteChannels
    val recentChannels = derivedCollections.recentChannels
    val visibleChannels = derivedCollections.visibleChannels
    val visibleSourceLibraryChannels = derivedCollections.visibleSourceLibraryChannels

    pendingClear?.let { action ->
        AlertDialog(
            onDismissRequest = { pendingClear = null },
            title = { Text(action.title) },
            text = { Text(action.message) },
            confirmButton = {
                TextButton(onClick = {
                    RoyalUiSounds.play(context, RoyalUiSound.ACTION_DELETE)
                    pendingClear = null
                    scope.launch {
                        runCatching {
                            libraryWriteMutex.withLock {
                                withContext(Dispatchers.IO) {
                                    when (action) {
                                        ClearAction.PLAYLIST -> LocalLibraryStore.clearPlaylist(context)
                                        ClearAction.HISTORY -> LocalLibraryStore.clearHistory(context)
                                        ClearAction.FAVORITES -> LocalLibraryStore.clearFavorites(context)
                                        ClearAction.SOURCE_LIBRARY -> LocalLibraryStore.clearRemoteSources(context)
                                    }
                                }
                            }
                        }.onSuccess {
                            when (action) {
                                ClearAction.PLAYLIST -> {
                                    channels.clear()
                                    libraryRevision++
                                    exoPlayer.stop()
                                    exoPlayer.clearMediaItems()
                                    selectedChannel = null
                                    status = "تم حذف القائمة المحلية"
                                }
                                ClearAction.HISTORY -> {
                                    historyRevision++
                                    status = "تم مسح سجل المشاهدة"
                                }
                                ClearAction.FAVORITES -> {
                                    favouriteRevision++
                                    status = "تم مسح المفضلة"
                                }
                                ClearAction.SOURCE_LIBRARY -> {
                                    sourceLibraryChannels.clear()
                                    libraryRevision++
                                    sourceLibraryCount = 0
                                    sourceLibraryStatus = "تم مسح مكتبة المصادر"
                                    status = "تم حذف مكتبة مصادر IPTV دون المساس بقائمة المستخدم"
                                }
                            }
                        }.onFailure { error ->
                            status = "تعذر إكمال الحذف: ${error.message ?: "خطأ غير معروف"}"
                        }
                    }
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
        color = Color.Transparent,
        contentColor = SoftWhite
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    event.key in setOf(Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight)
                ) {
                    RoyalUiSounds.play(
                        context,
                        if (isTelevision) RoyalUiSound.NAV_TV_FOCUS else RoyalUiSound.NAV_FOCUS_MOVE
                    )
                }
                false
            }
        ) {
            Image(
                painter = painterResource(id = selectedBackground),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.68f)))
            val wideLayout = adaptiveProfile.prefersWideLayout || maxWidth >= 840.dp
            if (isFullscreenPlayer && selectedChannel != null) {
                ScreenBody(
                    tab = selectedTab,
                    backgroundRes = selectedBackground,
                    onSelectBackground = { resourceId ->
                        selectedBackground = resourceId
                        backgroundPrefs.edit().putInt("background_res", resourceId).apply()
                    },
                    playlistSources = RemotePlaylistSources.playlists + countryPackageSources,
                    remotePlaylistStatus = remotePlaylistStatus,
                    sourceLibraryStatus = sourceLibraryStatus,
                    sourceLibraryCount = sourceLibraryCount,
                    sourceLibraryChannelCount = sourceLibraryChannels.size,
                    onAddSourceToLibrary = { playlistPackage ->
                        RoyalUiSounds.play(context, RoyalUiSound.ACTION_DOWNLOAD)
                        addRemotePlaylistPackageToLibrary(playlistPackage)
                    },
                    onShowSourceLibrary = ::showSourceLibrary,
                    channels = when {
                        selectedTab == RSTVTab.FAVORITES -> favoriteChannels
                        selectedTab == RSTVTab.LIVE && showingSourceLibrary -> visibleSourceLibraryChannels
                        else -> visibleChannels
                    },
                    recentChannels = recentChannels,
                    programmes = programmes.toList(),
                    selectedChannel = selectedChannel,
                    isFullscreenPlayer = isFullscreenPlayer,
                    onExitFullscreen = {
                        RoyalUiSounds.play(context, RoyalUiSound.MEDIA_FULLSCREEN_EXIT)
                        isFullscreenPlayer = false
                    },
                    onEnterFullscreen = {
                        RoyalUiSounds.play(context, RoyalUiSound.MEDIA_FULLSCREEN_ENTER)
                        isFullscreenPlayer = true
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
                        RoyalUiSounds.play(context, RoyalUiSound.NAV_BACK)
                        showingSourceLibrary = false
                        selectedChannel = null
                        searchQuery = ""
                        status = "تمت العودة إلى قائمة المستخدم المحلية"
                    },
                    onNavigate = ::navigateToTab,
                    onImportPlaylist = {
                        RoyalUiSounds.play(context, RoyalUiSound.ACTION_ADD)
                        playlistPicker.launch(arrayOf("*/*"))
                    },
                    onImportGuide = {
                        RoyalUiSounds.play(context, RoyalUiSound.ACTION_DOWNLOAD)
                        guidePicker.launch(arrayOf("*/*"))
                    },
                    onClear = { pendingClear = it },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (wideLayout) {
                Row(modifier = Modifier.fillMaxSize()) {
                    NavigationRail(
                        modifier = Modifier.width(
                            adaptiveProfile.iconSize(84.dp).coerceIn(84.dp, 120.dp)
                        ),
                        containerColor = if (isTelevision) Color(0xF20A0E18) else PanelBlack,
                        contentColor = SoftWhite,
                        header = {
                            Column(
                                modifier = Modifier.padding(vertical = adaptiveProfile.sectionSpacing),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                BrandMark()
                                Text(
                                    "RS TV",
                                    color = SoftWhite,
                                    fontWeight = FontWeight.Black,
                                    fontSize = if (isTelevision) 18.sp else 14.sp
                                )
                            }
                        }
                    ) {
                        primaryNavigationTabs.forEach { tab ->
                            NavigationRailItem(
                                selected = selectedTab == tab,
                                onClick = { navigateToTab(tab) },
                                icon = {
                                    Icon(
                                        tab.icon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(adaptiveProfile.iconSize(22.dp).coerceIn(22.dp, 36.dp))
                                    )
                                },
                                label = {
                                    Text(
                                        tab.title,
                                        fontSize = when {
                                            isTelevision -> 14.sp
                                            isTablet -> 12.sp
                                            else -> 10.sp
                                        }
                                    )
                                },
                                alwaysShowLabel = true
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(
                            horizontal = adaptiveProfile.pageHorizontalPadding,
                            vertical = adaptiveProfile.pageVerticalPadding
                        )
                    ) {
                        TopHeader(
                            onImportPlaylist = {
                                RoyalUiSounds.play(context, RoyalUiSound.ACTION_ADD)
                                playlistPicker.launch(arrayOf("*/*"))
                            },
                            channelCount = regularChannels.size
                        )
                        ScreenBody(
                            tab = selectedTab,
                            backgroundRes = selectedBackground,
                            onSelectBackground = { resourceId ->
                                selectedBackground = resourceId
                                backgroundPrefs.edit().putInt("background_res", resourceId).apply()
                            },
                            playlistSources = RemotePlaylistSources.playlists + countryPackageSources,
                            remotePlaylistStatus = remotePlaylistStatus,
                            sourceLibraryStatus = sourceLibraryStatus,
                            sourceLibraryCount = sourceLibraryCount,
                            sourceLibraryChannelCount = sourceLibraryChannels.size,
                            onAddSourceToLibrary = { playlistPackage ->
                        RoyalUiSounds.play(context, RoyalUiSound.ACTION_DOWNLOAD)
                        addRemotePlaylistPackageToLibrary(playlistPackage)
                    },
                            onShowSourceLibrary = ::showSourceLibrary,
                            channels = when {
                                selectedTab == RSTVTab.FAVORITES -> favoriteChannels
                                selectedTab == RSTVTab.LIVE && showingSourceLibrary -> visibleSourceLibraryChannels
                                else -> visibleChannels
                            },
                            recentChannels = recentChannels,
                            programmes = programmes.toList(),
                            selectedChannel = selectedChannel,
                            isFullscreenPlayer = isFullscreenPlayer,
                            onExitFullscreen = {
                                RoyalUiSounds.play(context, RoyalUiSound.MEDIA_FULLSCREEN_EXIT)
                                isFullscreenPlayer = false
                            },
                            onEnterFullscreen = {
                                RoyalUiSounds.play(context, RoyalUiSound.MEDIA_FULLSCREEN_ENTER)
                                isFullscreenPlayer = true
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
                                RoyalUiSounds.play(context, RoyalUiSound.NAV_BACK)
                                showingSourceLibrary = false
                                selectedChannel = null
                                searchQuery = ""
                                status = "تمت العودة إلى قائمة المستخدم المحلية"
                            },
                            onNavigate = ::navigateToTab,
                            onImportPlaylist = {
                                RoyalUiSounds.play(context, RoyalUiSound.ACTION_ADD)
                                playlistPicker.launch(arrayOf("*/*"))
                            },
                            onImportGuide = {
                                RoyalUiSounds.play(context, RoyalUiSound.ACTION_DOWNLOAD)
                                guidePicker.launch(arrayOf("*/*"))
                            },
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
                            onImportPlaylist = {
                                RoyalUiSounds.play(context, RoyalUiSound.ACTION_ADD)
                                playlistPicker.launch(arrayOf("*/*"))
                            },
                            channelCount = regularChannels.size
                        )
                        ScreenBody(
                            tab = selectedTab,
                            backgroundRes = selectedBackground,
                            onSelectBackground = { resourceId ->
                                selectedBackground = resourceId
                                backgroundPrefs.edit().putInt("background_res", resourceId).apply()
                            },
                            playlistSources = RemotePlaylistSources.playlists + countryPackageSources,
                            remotePlaylistStatus = remotePlaylistStatus,
                            sourceLibraryStatus = sourceLibraryStatus,
                            sourceLibraryCount = sourceLibraryCount,
                            sourceLibraryChannelCount = sourceLibraryChannels.size,
                            onAddSourceToLibrary = { playlistPackage ->
                        RoyalUiSounds.play(context, RoyalUiSound.ACTION_DOWNLOAD)
                        addRemotePlaylistPackageToLibrary(playlistPackage)
                    },
                            onShowSourceLibrary = ::showSourceLibrary,
                            channels = when {
                                selectedTab == RSTVTab.FAVORITES -> favoriteChannels
                                selectedTab == RSTVTab.LIVE && showingSourceLibrary -> visibleSourceLibraryChannels
                                else -> visibleChannels
                            },
                            recentChannels = recentChannels,
                            programmes = programmes.toList(),
                            selectedChannel = selectedChannel,
                            isFullscreenPlayer = isFullscreenPlayer,
                            onExitFullscreen = {
                                RoyalUiSounds.play(context, RoyalUiSound.MEDIA_FULLSCREEN_EXIT)
                                isFullscreenPlayer = false
                            },
                            onEnterFullscreen = {
                                RoyalUiSounds.play(context, RoyalUiSound.MEDIA_FULLSCREEN_ENTER)
                                isFullscreenPlayer = true
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
                                RoyalUiSounds.play(context, RoyalUiSound.NAV_BACK)
                                showingSourceLibrary = false
                                selectedChannel = null
                                searchQuery = ""
                                status = "تمت العودة إلى قائمة المستخدم المحلية"
                            },
                            onNavigate = ::navigateToTab,
                            onImportPlaylist = {
                                RoyalUiSounds.play(context, RoyalUiSound.ACTION_ADD)
                                playlistPicker.launch(arrayOf("*/*"))
                            },
                            onImportGuide = {
                                RoyalUiSounds.play(context, RoyalUiSound.ACTION_DOWNLOAD)
                                guidePicker.launch(arrayOf("*/*"))
                            },
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
    val adaptiveProfile = LocalRoyalAdaptiveProfile.current
    Image(
        painter = painterResource(id = R.drawable.sinjar_show_logo),
        contentDescription = "SINJAR SHOW",
        modifier = Modifier.size(
            width = adaptiveProfile.iconSize(88.dp).coerceIn(88.dp, 122.dp),
            height = adaptiveProfile.iconSize(34.dp).coerceIn(34.dp, 48.dp)
        )
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
    backgroundRes: Int,
    onSelectBackground: (Int) -> Unit,
    playlistSources: List<RemotePlaylistSource>,
    remotePlaylistStatus: String,
    sourceLibraryStatus: String,
    sourceLibraryCount: Int,
    sourceLibraryChannelCount: Int,
    onAddSourceToLibrary: (RemotePlaylistPackage) -> Unit,
    onShowSourceLibrary: () -> Unit,
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
    val context = LocalContext.current
    when (tab) {
        RSTVTab.HOME -> HomeContent(
            channelCount = channels.size,
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
            sourceLibraryChannelCount = sourceLibraryChannelCount,
            sources = playlistSources,
            onAddToLibrary = onAddSourceToLibrary,
            onShowLibrary = onShowSourceLibrary,
            modifier = modifier
        )
        RSTVTab.MOVIES -> MoviesLibraryContent(modifier = modifier)
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
            backgroundRes = backgroundRes,
            onSelectBackground = onSelectBackground,
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
                        "قنوات مباشرة وباقات IPTV حسب الدول والقارات ومكتبة ترفيهية، في واجهة واحدة سهلة الاستخدام.",
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
                FeatureCard("البث المباشر", "شاهد القنوات المحمّلة", "مشاهدة الآن", R.drawable.royal_smart_tv, Modifier.weight(1f)) {
                    onNavigate(RSTVTab.LIVE)
                }
                FeatureCard("مصادر IPTV", "باقات حسب القارات والدول", "إدارة الباقات", R.drawable.royal_playlist, Modifier.weight(1f)) {
                    onNavigate(RSTVTab.SOURCES)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureCard("الأفلام والمسلسلات", "اكتشف مكتبة الترفيه", "فتح المكتبة", R.drawable.royal_media_video, Modifier.weight(1f)) {
                    onNavigate(RSTVTab.MOVIES)
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
    "قنوات متنوعة"
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
        else -> "قنوات متنوعة"
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
    val context = LocalContext.current
    var showChannelTable by remember { mutableStateOf(false) }
    val fullscreenFocusRequester = remember { FocusRequester() }
    var orderedGroups by remember { mutableStateOf(emptyList<Pair<String, List<RSTVChannel>>>()) }
    var numberedChannels by remember { mutableStateOf(emptyList<Pair<Int, RSTVChannel>>()) }
    var numberByStreamUrl by remember { mutableStateOf(emptyMap<String, Int>()) }
    LaunchedEffect(channels.size, channels.firstOrNull()?.streamUrl, channels.lastOrNull()?.streamUrl) {
        val channelSnapshot = channels.toList()
        val result = withContext(Dispatchers.Default) {
            val groups = orderedChannelGroups(channelSnapshot)
            val numbered = groups
                .flatMap { it.second }
                .distinctBy { it.streamUrl }
                .mapIndexed { index, channel -> (index + 1) to channel }
            Triple(groups, numbered, numbered.associate { it.second.streamUrl to it.first })
        }
        orderedGroups = result.first
        numberedChannels = result.second
        numberByStreamUrl = result.third
    }
    val selectedChannelNumber = numberByStreamUrl[selectedChannel?.streamUrl]
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
            RoyalUiSounds.play(context, RoyalUiSound.NAV_BACK)
            showChannelTable = false
        } else {
            RoyalUiSounds.play(context, RoyalUiSound.MEDIA_FULLSCREEN_EXIT)
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
                    .clickable {
                        RoyalUiSounds.play(context, RoyalUiSound.NAV_OPEN_MENU)
                        showChannelTable = true
                    }
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
                TextButton(onClick = {
                    RoyalUiSounds.play(context, RoyalUiSound.NAV_OPEN_MENU)
                    showChannelTable = true
                }) {
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
                val groupedChannels = orderedGroups
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
    var numberedChannels by remember { mutableStateOf(emptyList<Pair<Int, RSTVChannel>>()) }
    var numberByStreamUrl by remember { mutableStateOf(emptyMap<String, Pair<Int, RSTVChannel>>()) }
    var filteredChannels by remember { mutableStateOf(emptyList<Pair<Int, RSTVChannel>>()) }
    LaunchedEffect(channels.size, channels.firstOrNull()?.streamUrl, channels.lastOrNull()?.streamUrl) {
        val channelSnapshot = channels.toList()
        val result = withContext(Dispatchers.Default) {
            val numbered = orderedChannelGroups(channelSnapshot)
                .flatMap { it.second }
                .distinctBy { it.streamUrl }
                .mapIndexed { index, channel -> (index + 1) to channel }
            numbered to numbered.associateBy { it.second.streamUrl }
        }
        numberedChannels = result.first
        numberByStreamUrl = result.second
    }
    LaunchedEffect(numberedChannels, query) {
        val channelSnapshot = numberedChannels
        filteredChannels = withContext(Dispatchers.Default) {
            channelSnapshot.filter { (_, channel) ->
                query.isBlank() ||
                    channel.name.contains(query, ignoreCase = true) ||
                    channelCountryLabel(channel).contains(query, ignoreCase = true) ||
                    channel.group.orEmpty().contains(query, ignoreCase = true)
            }
        }
    }
    val previewEntry = numberByStreamUrl[previewChannel?.streamUrl]

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
    sourceLibraryChannelCount: Int,
    sources: List<RemotePlaylistSource>,
    onAddToLibrary: (RemotePlaylistPackage) -> Unit,
    onShowLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val groupOrder = listOf(
        "الدول العربية", "باقات عربية مجمعة", "باقات تركيا", "الدول الأوروبية", "أمريكا الشمالية",
        "أمريكا الجنوبية واللاتينية", "شرق آسيا", "آسيا وبقية العالم",
        "الدول الإفريقية", "أوقيانوسيا", "أوروبا", "أمريكا الجنوبية",
        "قنوات الأفلام", "قنوات المسلسلات", "قنوات الرياضة",
        "فهارس IPTV الإضافية"
    )
    val packages = remember(sources) { RemotePlaylistSources.displayPackages(sources) }
    val groupedPackages = remember(packages) {
        packages.groupBy { it.packageGroup }
            .toList()
            .sortedWith(compareBy(
                { groupOrder.indexOf(it.first).let { index -> if (index < 0) Int.MAX_VALUE else index } },
                { it.first }
            ))
    }
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("باقات ومصادر IPTV", color = SoftWhite, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Text(
            "كل القنوات تُكتشف عبر قوائم IPTV بصيغة M3U. اختر القارة أو الدولة؛ سوريا ولبنان والعراق وبقية الدول العربية مفصولة كلٌّ على حدة.",
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
                Text(
                    "الباقات المتاحة: ${packages.size} • مصادر محفوظة: $sourceLibraryCount",
                    color = SoftWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    "القنوات الفريدة المحمّلة: $sourceLibraryChannelCount",
                    color = Silver,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Button(
                    onClick = onShowLibrary,
                    enabled = sourceLibraryCount > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) {
                    Text("عرض مكتبة القنوات المحمّلة ($sourceLibraryChannelCount)")
                }
                Text(
                    "هذه الصفحة لتنزيل حزم IPTV فقط؛ لا تشغّل الفيديو هنا ولا تستبدل قائمتك المحلية. افتح «البث المباشر» لمشاهدة القنوات. تُزال روابط البث المتطابقة عند دمج المصادر. وجود رابط HTTPS في فهرس عام لا يثبت وحده ترخيص القناة أو استمرار عملها.",
                    color = MutedText,
                    fontSize = 10.sp
                )
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groupedPackages.forEach { (group, groupEntries) ->
                item(key = "package-group-$group") {
                    Text(group, color = Silver, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                items(groupEntries, key = { it.id }) { playlistPackage ->
                    val providers = playlistPackage.sources.map { it.provider }.distinct()
                    val attributions = playlistPackage.sources.mapNotNull { it.attribution }.distinct()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PanelBlack),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(13.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(playlistPackage.name, color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(playlistPackage.description, color = MutedText, fontSize = 11.sp)
                            Text(
                                "المصادر: ${providers.joinToString(" + ")} • عدد القوائم: ${playlistPackage.sources.size}",
                                color = Silver,
                                fontSize = 10.sp
                            )
                            attributions.forEach { attribution ->
                                Text(attribution, color = MutedText, fontSize = 9.sp)
                            }
                            Button(
                                onClick = { onAddToLibrary(playlistPackage) },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("تنزيل الباقة الموحّدة إلى المكتبة")
                            }
                        }
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
private fun SettingsContent(
    status: String,
    backgroundRes: Int,
    onSelectBackground: (Int) -> Unit,
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
            Text("خلفية التطبيق", color = SoftWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("اختر الخلفية التي تظهر خلف القوائم. يتم حفظ اختيارك على الجهاز.", color = MutedText, fontSize = 11.sp)
        }
        item {
            SettingsAction("الموجات الملكية الزرقاء والذهبية", if (backgroundRes == R.drawable.sinjar_bg_royal_waves) "الخلفية الحالية" else "تطبيق الخلفية", { onSelectBackground(R.drawable.sinjar_bg_royal_waves) })
        }
        item {
            SettingsAction("المدينة السينمائية الليلية", if (backgroundRes == R.drawable.sinjar_bg_night_city) "الخلفية الحالية" else "تطبيق الخلفية", { onSelectBackground(R.drawable.sinjar_bg_night_city) })
        }
        item {
            SettingsAction("الجبال والبحيرة", if (backgroundRes == R.drawable.sinjar_bg_mountain_lake) "الخلفية الحالية" else "تطبيق الخلفية", { onSelectBackground(R.drawable.sinjar_bg_mountain_lake) })
        }
        item {
            SettingsAction("الطبيعة الذهبية السينمائية", if (backgroundRes == R.drawable.sinjar_bg_golden_nature) "الخلفية الحالية" else "تطبيق الخلفية", { onSelectBackground(R.drawable.sinjar_bg_golden_nature) })
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

