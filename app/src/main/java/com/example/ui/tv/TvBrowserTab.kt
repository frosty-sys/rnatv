package com.example.ui.tv

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.res.Configuration
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.CircularProgressIndicator
import com.example.WebContainerActivity
import com.example.data.model.RnaApp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VioletSecondary
import com.example.ui.wizard.RnaLogoSelectorCard
import com.example.ui.wizard.bitmapToPngDataUri
import com.example.ui.wizard.buildFaviconUrlForWebsite
import com.example.ui.wizard.resolveWebsiteLogoToPersistentDataUri
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

import android.os.Environment
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebResourceResponse
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.window.DialogProperties
import com.example.RnaFileExplorerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class TvBrowserBookmark(
    val title: String,
    val url: String
)

private val AD_BLOCK_HOST_KEYWORDS = listOf(
    "doubleclick.net",
    "googlesyndication.com",
    "googleadservices.com",
    "adservice.google.",
    "googletagmanager.com",
    "google-analytics.com",
    "adnxs.com",
    "taboola.com",
    "outbrain.com",
    "popads.net",
    "popcash.net",
    "criteo.com",
    "scorecardresearch.com",
    "rubiconproject.com",
    "pubmatic.com",
    "openx.net",
    "adsrvr.org",
    "moatads.com",
    "amazon-adsystem.com",
    "quantserve.com",
    "hotjar.com",
    "clarity.ms",
    "sentry.io",
    "newrelic.com"
)

private fun isAdUrl(url: String): Boolean {
    val lower = url.lowercase()
    return AD_BLOCK_HOST_KEYWORDS.any { lower.contains(it) }
}

private data class NativeAppRedirectTarget(
    val appLabel: String,
    val hostPatterns: List<String>,
    val schemePrefixes: List<String>,
    val candidatePackages: List<String>,
    val fallbackUrl: String
)

private val KNOWN_NATIVE_APP_TARGETS = listOf(
    NativeAppRedirectTarget(
        appLabel = "YouTube",
        hostPatterns = listOf("youtube.com", "youtu.be"),
        schemePrefixes = listOf("vnd.youtube", "youtube://"),
        candidatePackages = listOf(
            "com.google.android.youtube.tv",
            "com.google.android.youtube",
            "com.liskovsoft.smarttubetv.beta",
            "com.liskovsoft.smarttubetv",
            "com.google.android.youtube.tvkids"
        ),
        fallbackUrl = "https://www.youtube.com"
    ),
    NativeAppRedirectTarget(
        appLabel = "Netflix",
        hostPatterns = listOf("netflix.com"),
        schemePrefixes = listOf("nflx://", "netflix://"),
        candidatePackages = listOf(
            "com.netflix.ninja",
            "com.netflix.mediaclient"
        ),
        fallbackUrl = "https://www.netflix.com"
    ),
    NativeAppRedirectTarget(
        appLabel = "JioHotstar",
        hostPatterns = listOf("hotstar.com", "jiohotstar.com", "jiocinema.com"),
        schemePrefixes = listOf("hotstar://", "jiohotstar://", "jiocinema://"),
        candidatePackages = listOf(
            "in.startv.hotstar",
            "in.startv.hotstar.dplus",
            "in.startv.hotstar.dplus.tv",
            "com.jio.media.ondemand",
            "com.jio.media.stb.ondemand"
        ),
        fallbackUrl = "https://www.hotstar.com"
    ),
    NativeAppRedirectTarget(
        appLabel = "Prime Video",
        hostPatterns = listOf("primevideo.com", "amazon.com/gp/video"),
        schemePrefixes = listOf("primevideo://", "aiv://"),
        candidatePackages = listOf(
            "com.amazon.amazonvideo.livingroom",
            "com.amazon.avod.thirdpartyclient"
        ),
        fallbackUrl = "https://www.primevideo.com"
    ),
    NativeAppRedirectTarget(
        appLabel = "Disney+",
        hostPatterns = listOf("disneyplus.com"),
        schemePrefixes = listOf("disneyplus://"),
        candidatePackages = listOf(
            "com.disney.disneyplus"
        ),
        fallbackUrl = "https://www.disneyplus.com"
    ),
    NativeAppRedirectTarget(
        appLabel = "Spotify",
        hostPatterns = listOf("spotify.com"),
        schemePrefixes = listOf("spotify://"),
        candidatePackages = listOf(
            "com.spotify.tv.android",
            "com.spotify.music"
        ),
        fallbackUrl = "https://open.spotify.com"
    ),
    NativeAppRedirectTarget(
        appLabel = "Twitch",
        hostPatterns = listOf("twitch.tv"),
        schemePrefixes = listOf("twitch://"),
        candidatePackages = listOf(
            "tv.twitch.android.viewer",
            "tv.twitch.android.app"
        ),
        fallbackUrl = "https://www.twitch.tv"
    ),
    NativeAppRedirectTarget(
        appLabel = "Apple TV",
        hostPatterns = listOf("tv.apple.com"),
        schemePrefixes = listOf("appletv://"),
        candidatePackages = listOf(
            "com.apple.atve.androidtv.appletv",
            "com.apple.atve.sony.appletv"
        ),
        fallbackUrl = "https://tv.apple.com"
    ),
    NativeAppRedirectTarget(
        appLabel = "Max",
        hostPatterns = listOf("max.com", "hbomax.com"),
        schemePrefixes = listOf("hbomax://", "max://"),
        candidatePackages = listOf(
            "com.wbd.stream",
            "com.hbo.hbonow"
        ),
        fallbackUrl = "https://www.max.com"
    ),
    NativeAppRedirectTarget(
        appLabel = "Hulu",
        hostPatterns = listOf("hulu.com"),
        schemePrefixes = listOf("hulu://"),
        candidatePackages = listOf(
            "com.hulu.livingroomplus",
            "com.hulu.plus"
        ),
        fallbackUrl = "https://www.hulu.com"
    ),
    NativeAppRedirectTarget(
        appLabel = "Crunchyroll",
        hostPatterns = listOf("crunchyroll.com"),
        schemePrefixes = listOf("crunchyroll://"),
        candidatePackages = listOf(
            "com.crunchyroll.crunchyroid"
        ),
        fallbackUrl = "https://www.crunchyroll.com"
    ),
    NativeAppRedirectTarget(
        appLabel = "SonyLIV",
        hostPatterns = listOf("sonyliv.com"),
        schemePrefixes = listOf("sonyliv://"),
        candidatePackages = listOf(
            "com.sonyliv"
        ),
        fallbackUrl = "https://www.sonyliv.com"
    ),
    NativeAppRedirectTarget(
        appLabel = "ZEE5",
        hostPatterns = listOf("zee5.com"),
        schemePrefixes = listOf("zee5://"),
        candidatePackages = listOf(
            "com.graymatrix.did"
        ),
        fallbackUrl = "https://www.zee5.com"
    )
)

private fun detectNativeAppForUrl(url: String): NativeAppRedirectTarget? {
    val lower = url.lowercase().trim()
    if (lower.isBlank()) return null
    for (target in KNOWN_NATIVE_APP_TARGETS) {
        if (target.schemePrefixes.any { lower.startsWith(it) }) return target
        if (lower.startsWith("intent://") && target.hostPatterns.any { lower.contains(it) }) return target
        if (target.hostPatterns.any { lower.contains(it) }) return target
    }
    return null
}

private fun launchNativeTargetApp(context: Context, targetUrl: String, fallbackAppLabel: String): Boolean {
    val pm = context.packageManager
    try {
        if (targetUrl.startsWith("intent://", ignoreCase = true)) {
            val parsedIntent = Intent.parseUri(targetUrl, Intent.URI_INTENT_SCHEME).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (parsedIntent.resolveActivity(pm) != null) {
                context.startActivity(parsedIntent)
                return true
            }
        }
    } catch (_: Exception) {
    }

    val detected = detectNativeAppForUrl(targetUrl)
    if (detected != null) {
        val normalizedUri = if (targetUrl.startsWith("intent://", ignoreCase = true)) {
            Uri.parse(detected.fallbackUrl)
        } else {
            Uri.parse(targetUrl)
        }
        for (pkg in detected.candidatePackages) {
            try {
                val viewIntent = Intent(Intent.ACTION_VIEW, normalizedUri).apply {
                    setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (viewIntent.resolveActivity(pm) != null) {
                    context.startActivity(viewIntent)
                    return true
                }
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                    ?: pm.getLeanbackLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.data = normalizedUri
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return true
                }
            } catch (_: Exception) {
            }
        }
    }

    return try {
        val genericView = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(genericView)
        true
    } catch (_: Exception) {
        val label = detected?.appLabel ?: fallbackAppLabel
        Toast.makeText(context, "$label app is not installed on this TV", Toast.LENGTH_SHORT).show()
        false
    }
}

private fun loadProfileBookmarks(prefs: android.content.SharedPreferences, profileKey: String): List<TvBrowserBookmark> {
    val raw = prefs.getString("bookmarks_$profileKey", null)
    if (raw.isNullOrBlank()) {
        return listOf(
            TvBrowserBookmark("Google", "https://www.google.com"),
            TvBrowserBookmark("YouTube", "https://www.youtube.com"),
            TvBrowserBookmark("Wikipedia", "https://www.wikipedia.org"),
            TvBrowserBookmark("Poki Games", "https://poki.com"),
            TvBrowserBookmark("Reddit", "https://www.reddit.com"),
            TvBrowserBookmark("GitHub", "https://github.com")
        )
    }
    return try {
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { i ->
            val obj = arr.optJSONObject(i) ?: return@mapNotNull null
            TvBrowserBookmark(
                title = obj.optString("title", "Site"),
                url = obj.optString("url", "https://www.google.com")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun saveProfileBookmarks(
    prefs: android.content.SharedPreferences,
    profileKey: String,
    bookmarks: List<TvBrowserBookmark>
) {
    val arr = JSONArray()
    bookmarks.forEach { b ->
        arr.put(
            JSONObject().apply {
                put("title", b.title)
                put("url", b.url)
            }
        )
    }
    prefs.edit().putString("bookmarks_$profileKey", arr.toString()).apply()
}

@Composable
private fun TvFocusableIconBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: Color = Color.White,
    onFocused: () -> Unit = {},
    onUpToTopBar: (() -> Boolean)? = null,
    onDownToWeb: (() -> Boolean)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.12f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "BrowserIconScale"
    )
    Surface(
        shape = CircleShape,
        color = if (focused) Color.White else Color(0xFF222534),
        border = BorderStroke(1.dp, if (focused) CyanPrimary else Color(0x28FFFFFF)),
        modifier = modifier
            .size(34.dp)
            .scale(scale)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onPreviewKeyEvent { ev ->
                if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (ev.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> onUpToTopBar?.invoke() ?: false
                        KeyEvent.KEYCODE_DPAD_DOWN -> onDownToWeb?.invoke() ?: false
                        else -> false
                    }
                } else {
                    false
                }
            }
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (focused) Color(0xFF12131A) else tint,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TvBrowserTab(
    initialUrl: String,
    activeProfileId: String,
    isolateProfileWebData: Boolean,
    isFullScreen: Boolean,
    onToggleFullScreen: (Boolean) -> Unit,
    isTopBarAutoHidden: Boolean = false,
    onTopBarAutoHideChange: (Boolean) -> Unit = {},
    launcherTopBarFocusRequester: FocusRequester? = null,
    firstToolbarFocusRequester: FocusRequester? = null,
    onExitToHomeTab: () -> Unit,
    onConvertWebsiteToRnaApp: (RnaApp) -> Unit,
    onSetDownloadedFileAsWallpaper: ((File, Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controlPrefs = remember {
        context.getSharedPreferences(WebContainerActivity.PREFS_WEB_CONTROLS, Context.MODE_PRIVATE)
    }
    val bookmarkNamespace = if (isolateProfileWebData) "profile_$activeProfileId" else "shared_default"

    var bookmarks by remember(bookmarkNamespace) {
        mutableStateOf(loadProfileBookmarks(controlPrefs, bookmarkNamespace))
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    // Render all top navigation & taskbar buttons immediately on Frame 1, then mount WebView
    var isWebViewMounted by remember { mutableStateOf(false) }
    // Out-of-Memory protection state: when RAM is critically low or WebView renderer dies,
    // we kill the webpage cleanly to prevent RNA. TV from crashing and show a recovery screen.
    var outOfMemoryKilledUrl by remember { mutableStateOf<String?>(null) }
    var outOfMemoryReason by remember { mutableStateOf("Device ran out of memory") }
    var webViewReloadEpoch by remember { mutableIntStateOf(0) }

    var currentUrl by remember(initialUrl) {
        mutableStateOf(initialUrl.ifBlank { "https://www.google.com" })
    }

    fun killWebpageToProtectApp(reason: String) {
        if (outOfMemoryKilledUrl != null) return
        val failedUrl = currentUrl
        val wv = webViewRef
        webViewRef = null
        RnaControllerNetworkBridge.activeWebViewRef = null
        isWebViewMounted = false
        outOfMemoryReason = reason
        outOfMemoryKilledUrl = failedUrl
        runCatching {
            wv?.stopLoading()
            wv?.loadUrl("about:blank")
            wv?.clearHistory()
            wv?.clearCache(true)
            (wv?.parent as? ViewGroup)?.removeView(wv)
            wv?.destroy()
        }
        runCatching { Runtime.getRuntime().gc() }
    }

    // Listen to Android system low-memory / trim-memory callbacks while Browser is open
    androidx.compose.runtime.DisposableEffect(context) {
        val memCallback = object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) {}
            override fun onLowMemory() {
                scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                    killWebpageToProtectApp("Device ran out of memory — webpage closed to keep RNA. TV running.")
                }
            }
            override fun onTrimMemory(level: Int) {
                if (level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
                    level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE
                ) {
                    scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                        killWebpageToProtectApp("Device ran out of memory — webpage closed to protect RNA. TV.")
                    }
                }
            }
        }
        runCatching { context.applicationContext.registerComponentCallbacks(memCallback) }
        onDispose {
            runCatching { context.applicationContext.unregisterComponentCallbacks(memCallback) }
        }
    }

    // Proactive low-RAM watchdog every 5s while a webpage is mounted
    LaunchedEffect(isWebViewMounted, outOfMemoryKilledUrl) {
        if (!isWebViewMounted || outOfMemoryKilledUrl != null) return@LaunchedEffect
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return@LaunchedEffect
        val memInfo = ActivityManager.MemoryInfo()
        while (isWebViewMounted && outOfMemoryKilledUrl == null) {
            delay(5000L)
            am.getMemoryInfo(memInfo)
            val availMb = memInfo.availMem / (1024L * 1024L)
            if (memInfo.lowMemory || (memInfo.totalMem > 0L && availMb < 75L)) {
                killWebpageToProtectApp("Device ran out of memory (${availMb} MB free) — webpage closed to prevent RNA. TV from crashing.")
                break
            }
        }
    }
    var currentTitle by remember { mutableStateOf("RNA. TV Browser") }
    var pageProgress by remember { mutableIntStateOf(0) }
    var isForcedDark by remember { mutableStateOf(false) }
    var adBlockEnabled by remember {
        mutableStateOf(controlPrefs.getBoolean("browser_adblock_enabled", true))
    }
    var browserFastPerformanceMode by remember {
        mutableStateOf(
            controlPrefs.getBoolean(
                WebContainerActivity.KEY_BROWSER_FAST_PERFORMANCE_MODE,
                context.getSharedPreferences("rna_tv_launcher_prefs", Context.MODE_PRIVATE)
                    .getBoolean("browser_fast_performance_mode", true)
            )
        )
    }
    var blockedAdsCount by remember { mutableIntStateOf(0) }
    var pageZoomPercent by remember {
        mutableIntStateOf(controlPrefs.getInt("browser_zoom_percent", 110))
    }

    // Default Input Mode in Browser Settings ("DPAD" or "MOUSE")
    var defaultBaseMode by remember {
        val saved = controlPrefs.getString(WebContainerActivity.KEY_BROWSER_DEFAULT_MODE, "DPAD")
        mutableIntStateOf(if (saved == "MOUSE") WebContainerActivity.MODE_MOUSE else WebContainerActivity.MODE_DPAD)
    }
    var inputMode by remember { mutableIntStateOf(defaultBaseMode) }
    var mouseSpeed by remember {
        mutableFloatStateOf(controlPrefs.getFloat(WebContainerActivity.KEY_BROWSER_MOUSE_SPEED, 1.2f))
    }
    var cursorDesign by remember {
        mutableStateOf(RnaCursorDesign.fromId(controlPrefs.getString(WebContainerActivity.KEY_CURSOR_DESIGN, null)))
    }
    var scrollWheelDesign by remember {
        mutableStateOf(RnaScrollWheelDesign.fromId(controlPrefs.getString(WebContainerActivity.KEY_SCROLL_DESIGN, null)))
    }

    var containerWidthPx by remember { mutableFloatStateOf(1280f) }
    var containerHeightPx by remember { mutableFloatStateOf(720f) }
    var topChromeHeightPx by remember { mutableIntStateOf(0) }
    var cursorX by remember { mutableFloatStateOf(640f) }
    var cursorY by remember { mutableFloatStateOf(360f) }
    var isCursorPressed by remember { mutableStateOf(false) }

    var modeBannerVisible by remember { mutableStateOf(true) }
    var bannerJob by remember { mutableStateOf<Job?>(null) }

    var showUrlInputDialog by remember { mutableStateOf(false) }
    var showBrowserSettingsDialog by remember { mutableStateOf(false) }
    var showConvertToRnaDialog by remember { mutableStateOf(false) }
    var convertSnapshotUrl by remember { mutableStateOf("") }
    var convertSnapshotTitle by remember { mutableStateOf("") }
    var convertSnapshotDomIcon by remember { mutableStateOf<String?>(null) }
    var currentPageFaviconBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showBrowserDownloadsDialog by remember { mutableStateOf(false) }
    var browserDownloadsList by remember { mutableStateOf<List<File>>(emptyList()) }
    var activeDownloadStatus by remember { mutableStateOf<String?>(null) }
    var activeBrowserVideoFile by remember { mutableStateOf<File?>(null) }
    var activeBrowserAudioFile by remember { mutableStateOf<File?>(null) }
    var activeBrowserImageFile by remember { mutableStateOf<File?>(null) }
    var autoSwitchedFromMouseToTopBar by remember { mutableStateOf(false) }

    fun getBrowserDownloadsDir(): File {
        val publicDl = runCatching {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.takeIf {
                it.exists() || it.mkdirs()
            }
        }.getOrNull()
        val internalDl = File(context.filesDir, "browser_downloads").apply { mkdirs() }
        return publicDl?.takeIf { it.canWrite() } ?: internalDl
    }

    fun refreshBrowserDownloads() {
        val dirs = listOfNotNull(
            File(context.filesDir, "browser_downloads").takeIf { it.exists() },
            runCatching { Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) }.getOrNull()?.takeIf { it.exists() }
        ).distinctBy { it.absolutePath }
        val allFiles = dirs.flatMap { d ->
            d.listFiles()?.filter { it.isFile }?.toList() ?: emptyList()
        }.distinctBy { it.name to it.length() }
            .sortedByDescending { it.lastModified() }
        browserDownloadsList = allFiles
    }

    fun startBrowserFileDownload(url: String, suggestedName: String? = null, userAgent: String? = null, mimeType: String? = null) {
        scope.launch {
            val cleanName = (suggestedName?.takeIf { it.isNotBlank() }
                ?: URLUtil.guessFileName(url, null, mimeType)
                ?: "download_${System.currentTimeMillis()}").replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            activeDownloadStatus = "Downloading $cleanName..."
            Toast.makeText(context, "Downloading $cleanName...", Toast.LENGTH_SHORT).show()
            val downloadedFile = withContext(Dispatchers.IO) {
                try {
                    val internalDir = File(context.filesDir, "browser_downloads").apply { mkdirs() }
                    val destFile = File(internalDir, cleanName)
                    val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 15000
                        readTimeout = 30000
                        instanceFollowRedirects = true
                        if (!userAgent.isNullOrBlank()) setRequestProperty("User-Agent", userAgent)
                        val cookie = CookieManager.getInstance().getCookie(url)
                        if (!cookie.isNullOrBlank()) setRequestProperty("Cookie", cookie)
                    }
                    conn.inputStream.use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    // Also mirror into public Downloads if writable so system & File Explorer Downloads see it
                    runCatching {
                        val pubDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        if (pubDir != null && (pubDir.exists() || pubDir.mkdirs()) && pubDir.canWrite()) {
                            val pubTarget = File(pubDir, cleanName)
                            if (pubTarget.absolutePath != destFile.absolutePath) {
                                destFile.copyTo(pubTarget, overwrite = true)
                            }
                        }
                    }
                    destFile
                } catch (_: Exception) {
                    null
                }
            }
            refreshBrowserDownloads()
            if (downloadedFile != null && downloadedFile.exists()) {
                activeDownloadStatus = "Downloaded: ${downloadedFile.name}"
                Toast.makeText(context, "Downloaded ${downloadedFile.name}", Toast.LENGTH_SHORT).show()
            } else {
                activeDownloadStatus = "Download failed for $cleanName"
                Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Web Page Text Field Keyboard Popup State (opens whenever any <input> or <textarea> is clicked by the user)
    var showWebTextFieldInputModal by remember { mutableStateOf(false) }
    var activeWebFieldText by remember { mutableStateOf("") }
    var activeWebFieldLabel by remember { mutableStateOf("Type in Webpage Text Field") }
    var agentTargetingUrlBar by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val isAgentModeActive by RnaAutonomousAgentController.isAgentModeActive.collectAsState()
    val isAgentPaused by RnaAutonomousAgentController.isPaused.collectAsState()

    fun shouldSuppressWebInputPopup(): Boolean {
        return RnaAutonomousAgentController.isAgentActivelyControlling() ||
            SystemClock.elapsedRealtime() < BrowserAgentBridge.suppressUserInputPopupUntilMs
    }

    LaunchedEffect(isAgentModeActive, isAgentPaused) {
        if (isAgentModeActive && !isAgentPaused) {
            showWebTextFieldInputModal = false
            keyboardController?.hide()
        }
    }

    // Native App Redirect Prompt State (YouTube, Netflix, JioHotstar, Prime Video, Disney+, Spotify, etc.)
    var pendingAppRedirectUrl by remember { mutableStateOf<String?>(null) }
    var pendingAppRedirectLabel by remember { mutableStateOf("YouTube") }
    var dismissedAppPromptsForSession by remember { mutableStateOf(setOf<String>()) }

    val internalAddressFocusRequester = remember { FocusRequester() }
    val addressBarFocusRequester = firstToolbarFocusRequester ?: internalAddressFocusRequester
    val firstBookmarkFocusRequester = remember { FocusRequester() }
    val webCanvasFocusRequester = remember { FocusRequester() }

    var isOkKeyDown by remember { mutableStateOf(false) }
    var okKeyDownTime by remember { mutableStateOf(0L) }
    var isLongPressTriggered by remember { mutableStateOf(false) }
    var holdOkJob by remember { mutableStateOf<Job?>(null) }
    var activeDirectionKey by remember { mutableIntStateOf(0) }
    var movementJob by remember { mutableStateOf<Job?>(null) }

    // 5-Second Auto-Hide State for the Top Bar (including the Home button & navigation bar)
    var topBarActivityTick by remember { mutableIntStateOf(0) }

    fun centerMouseCursor() {
        cursorX = (containerWidthPx / 2f).coerceAtLeast(120f)
        cursorY = (containerHeightPx / 2f).coerceAtLeast(120f)
    }

    fun notifyTopBarActivity() {
        topBarActivityTick++
        if (isTopBarAutoHidden) {
            onTopBarAutoHideChange(false)
        }
    }

    fun revealTopBarAndFocus(fromMouseTopEdge: Boolean = false) {
        topBarActivityTick++
        if (fromMouseTopEdge && inputMode == WebContainerActivity.MODE_MOUSE) {
            autoSwitchedFromMouseToTopBar = true
            activeDirectionKey = 0
            movementJob?.cancel()
            // Move cursor slightly down from top edge so when returning to Mouse mode it never re-triggers top bar immediately
            cursorY = (containerHeightPx * 0.25f).coerceAtLeast(96f)
        }
        onTopBarAutoHideChange(false)
        scope.launch {
            delay(45L)
            runCatching { addressBarFocusRequester.requestFocus() }
        }
    }

    fun focusWebCanvasSafely(): Boolean {
        if (autoSwitchedFromMouseToTopBar) {
            autoSwitchedFromMouseToTopBar = false
            inputMode = WebContainerActivity.MODE_MOUSE
            if (cursorY < 80f) {
                cursorY = (containerHeightPx * 0.28f).coerceAtLeast(100f)
            }
        } else if (inputMode == WebContainerActivity.MODE_MOUSE && cursorY < 48f) {
            cursorY = (containerHeightPx * 0.28f).coerceAtLeast(100f)
        }
        return try {
            webCanvasFocusRequester.requestFocus()
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun focusLauncherTopBarSafely(): Boolean {
        notifyTopBarActivity()
        val req = launcherTopBarFocusRequester ?: return false
        return try {
            req.requestFocus()
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun focusDownFromToolbarRowSafely(): Boolean {
        if (autoSwitchedFromMouseToTopBar) {
            return focusWebCanvasSafely()
        }
        return if (bookmarks.isNotEmpty()) {
            try {
                firstBookmarkFocusRequester.requestFocus()
                true
            } catch (_: Throwable) {
                focusWebCanvasSafely()
            }
        } else {
            focusWebCanvasSafely()
        }
    }

    // Start immediately in Full-Screen mode (top bar hidden) on launch without waiting 5 seconds;
    // the top bar & tabs only pop up when the user moves Up to the top edge!
    LaunchedEffect(Unit) {
        refreshBrowserDownloads()
        onTopBarAutoHideChange(true)
        delay(16L)
        isWebViewMounted = true
        delay(40L)
        if (inputMode == WebContainerActivity.MODE_MOUSE) {
            centerMouseCursor()
        }
        focusWebCanvasSafely()
    }

    // Automatically hide the Top Bar (including Home button) after 5 seconds of inactivity in Browser tab
    LaunchedEffect(
        isTopBarAutoHidden,
        topBarActivityTick,
        isFullScreen,
        showUrlInputDialog,
        showBrowserSettingsDialog,
        showConvertToRnaDialog,
        showBrowserDownloadsDialog,
        showWebTextFieldInputModal
    ) {
        if (!isTopBarAutoHidden &&
            !isFullScreen &&
            !showUrlInputDialog &&
            !showBrowserSettingsDialog &&
            !showConvertToRnaDialog &&
            !showBrowserDownloadsDialog &&
            !showWebTextFieldInputModal
        ) {
            delay(5_000L)
            onTopBarAutoHideChange(true)
            delay(30L)
            focusWebCanvasSafely()
        }
    }

    // Apply Dark Mode CSS only when isForcedDark changes (never on every recomposition)
    LaunchedEffect(isForcedDark, webViewRef) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (isForcedDark) {
            wv.loadUrl(WebContainerActivity.CSS_FORCE_DARK)
        } else {
            wv.loadUrl(WebContainerActivity.CSS_REMOVE_DARK)
        }
    }

    fun triggerModeBanner() {
        modeBannerVisible = true
        bannerJob?.cancel()
        bannerJob = scope.launch {
            delay(2800L)
            modeBannerVisible = false
        }
    }

    fun performHoldOkModeAdvance() {
        val prevMode = inputMode
        val nextMode = advanceInputModeOnHoldOk(inputMode, defaultBaseMode)
        if (nextMode != WebContainerActivity.MODE_DPAD) {
            webViewRef?.evaluateJavascript("if(window.__rnaClearFocus) window.__rnaClearFocus();", null)
        }
        if (prevMode != WebContainerActivity.MODE_MOUSE && nextMode == WebContainerActivity.MODE_MOUSE) {
            centerMouseCursor()
        }
        inputMode = nextMode
        triggerModeBanner()
    }

    LaunchedEffect(initialUrl) {
        if (initialUrl.isNotBlank() && initialUrl != currentUrl) {
            currentUrl = initialUrl
            webViewRef?.loadUrl(initialUrl)
        }
        val checkUrl = initialUrl.ifBlank { currentUrl }
        val detectedTarget = detectNativeAppForUrl(checkUrl)
        if (detectedTarget != null && detectedTarget.appLabel !in dismissedAppPromptsForSession) {
            pendingAppRedirectLabel = detectedTarget.appLabel
            pendingAppRedirectUrl = checkUrl
        }
        triggerModeBanner()
    }

    fun scanWebPageSiteTools(wv: WebView?) {
        val targetWv = wv ?: webViewRef ?: return
        val jsScanner = """
            (function() {
                try {
                    var nodes = document.querySelectorAll('input, textarea, select, button, a[href], [role="button"], [type="submit"], [onclick]');
                    var out = [];
                    var used = {};
                    function slugify(str) {
                        return (str || '')
                            .toLowerCase()
                            .replace(/[^a-z0-9]+/g, '-')
                            .replace(/^-+|-+$/g, '')
                            .substring(0, 36);
                    }
                    for (var i = 0; i < nodes.length && out.length < 75; i++) {
                        var el = nodes[i];
                        var rect = el.getBoundingClientRect();
                        if (rect.width < 8 || rect.height < 8) continue;
                        var style = window.getComputedStyle(el);
                        if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') continue;
                        var tag = (el.tagName || '').toLowerCase();
                        var type = (el.getAttribute('type') || '').toLowerCase();
                        if (type === 'hidden') continue;
                        var rawLabel = el.getAttribute('aria-label') ||
                            el.getAttribute('placeholder') ||
                            el.getAttribute('name') ||
                            el.getAttribute('id') ||
                            el.getAttribute('title') ||
                            (el.innerText || el.value || '').trim();
                        rawLabel = (rawLabel || '').replace(/\s+/g, ' ').trim().substring(0, 60);
                        var suffix = (tag === 'input' || tag === 'textarea' || tag === 'select') ? '-field' : '-button';
                        var baseSlug = slugify(rawLabel || (tag + '-' + (type || 'item')));
                        if (!baseSlug) baseSlug = tag + '-' + i;
                        if (baseSlug.indexOf('-field') === -1 && baseSlug.indexOf('-button') === -1 && baseSlug.indexOf('-link') === -1) {
                            baseSlug = baseSlug + (tag === 'a' ? '-link' : suffix);
                        }
                        var finalSlug = baseSlug;
                        var counter = 2;
                        while (used[finalSlug]) {
                            finalSlug = baseSlug + '-' + counter;
                            counter++;
                        }
                        used[finalSlug] = true;
                        el.setAttribute('data-rna-site-tool', finalSlug);
                        out.push({
                            slug: finalSlug,
                            tag: tag,
                            type: type,
                            label: rawLabel || finalSlug
                        });
                    }
                    if (!window.__rnaInputClickHooked) {
                        window.__rnaInputClickHooked = true;
                        window.__rnaIsTextInput = function(el) {
                            if (!el) return false;
                            var tag = (el.tagName || '').toUpperCase();
                            if (tag === 'TEXTAREA' || el.isContentEditable) return true;
                            if (tag === 'INPUT') {
                                var t = (el.getAttribute('type') || 'text').toLowerCase();
                                return ['button','submit','reset','checkbox','radio','file','image','range','color','hidden'].indexOf(t) === -1;
                            }
                            return false;
                        };
                        document.addEventListener('click', function(ev) {
                            var target = ev.target;
                            if (window.__rnaIsTextInput && window.__rnaIsTextInput(target)) {
                                window.__rnaLastTargetedElement = target;
                                if (window.__rnaSuppressInputPopupUntil && Date.now() < window.__rnaSuppressInputPopupUntil) {
                                    return;
                                }
                                var curVal = target.value !== undefined ? target.value : (target.innerText || '');
                                var hint = target.getAttribute('placeholder') || target.getAttribute('aria-label') || target.getAttribute('name') || 'Web Text Field';
                                if (window.RnaBrowserInputBridge && window.RnaBrowserInputBridge.onWebInputClicked) {
                                    window.RnaBrowserInputBridge.onWebInputClicked(curVal + '', hint + '');
                                }
                            }
                        }, true);
                    }
                    return JSON.stringify(out);
                } catch(e) {
                    return "[]";
                }
            })();
        """.trimIndent()

        targetWv.evaluateJavascript(jsScanner) { rawJson ->
            try {
                if (rawJson.isNullOrBlank() || rawJson == "null") return@evaluateJavascript
                val unescaped = if (rawJson.startsWith("\"") && rawJson.endsWith("\"")) {
                    org.json.JSONTokener(rawJson).nextValue().toString()
                } else {
                    rawJson
                }
                val arr = JSONArray(unescaped)
                val parsed = mutableListOf<SiteToolElement>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val slug = obj.optString("slug", "").trim()
                    if (slug.isNotEmpty()) {
                        parsed.add(
                            SiteToolElement(
                                slugId = slug,
                                tag = obj.optString("tag", ""),
                                inputType = obj.optString("type", ""),
                                label = obj.optString("label", slug)
                            )
                        )
                    }
                }
                BrowserAgentBridge.updateSiteTools(parsed)
            } catch (_: Exception) {
            }
        }
    }

    // Keep BrowserAgentBridge live state synchronized with TvBrowserTab
    LaunchedEffect(currentUrl, currentTitle, inputMode, cursorX, cursorY, mouseSpeed) {
        BrowserAgentBridge.isBrowserVisible = true
        BrowserAgentBridge.currentUrl = currentUrl
        BrowserAgentBridge.currentTitle = currentTitle
        BrowserAgentBridge.currentInputMode = when (inputMode) {
            WebContainerActivity.MODE_MOUSE -> "MOUSE"
            WebContainerActivity.MODE_SCROLL -> "SCROLL"
            else -> "DPAD"
        }
        BrowserAgentBridge.cursorX = cursorX
        BrowserAgentBridge.cursorY = cursorY
        BrowserAgentBridge.mouseSpeedMultiplier = mouseSpeed
    }

    // Aggressively release WebView resources when leaving the Browser tab
    DisposableEffect(bookmarkNamespace) {
        BrowserAgentBridge.isBrowserVisible = true
        BrowserAgentBridge.onExecuteBrowserTool = { toolName ->
            when (toolName.trim().lowercase()) {
                "home" -> {
                    onTopBarAutoHideChange(false)
                    onExitToHomeTab()
                    true
                }
                "previous-page", "back", "prev-page" -> {
                    if (webViewRef?.canGoBack() == true) {
                        webViewRef?.goBack()
                        true
                    } else false
                }
                "next-page", "forward" -> {
                    if (webViewRef?.canGoForward() == true) {
                        webViewRef?.goForward()
                        true
                    } else false
                }
                "reload", "refresh" -> {
                    webViewRef?.reload()
                    true
                }
                "search-bar", "address-bar", "url-bar" -> {
                    BrowserAgentBridge.suppressUserInputPopupUntilMs = SystemClock.elapsedRealtime() + 2000L
                    agentTargetingUrlBar = true
                    showUrlInputDialog = false
                    showWebTextFieldInputModal = false
                    keyboardController?.hide()
                    true
                }
                "scroll-down" -> {
                    webViewRef?.scrollBy(0, 360)
                    webViewRef?.evaluateJavascript("window.scrollBy(0, 360);", null)
                    scope.launch { delay(300L); scanWebPageSiteTools(webViewRef) }
                    true
                }
                "scroll-up" -> {
                    webViewRef?.scrollBy(0, -360)
                    webViewRef?.evaluateJavascript("window.scrollBy(0, -360);", null)
                    scope.launch { delay(300L); scanWebPageSiteTools(webViewRef) }
                    true
                }
                "scroll-left" -> {
                    webViewRef?.scrollBy(-280, 0)
                    true
                }
                "scroll-right" -> {
                    webViewRef?.scrollBy(280, 0)
                    true
                }
                "mode-dpad", "dpad" -> {
                    inputMode = WebContainerActivity.MODE_DPAD
                    triggerModeBanner()
                    true
                }
                "mode-mouse", "mouse" -> {
                    if (inputMode != WebContainerActivity.MODE_MOUSE) {
                        centerMouseCursor()
                    }
                    inputMode = WebContainerActivity.MODE_MOUSE
                    triggerModeBanner()
                    true
                }
                "mode-scroll", "scroll" -> {
                    inputMode = WebContainerActivity.MODE_SCROLL
                    triggerModeBanner()
                    true
                }
                "zoom-in" -> {
                    pageZoomPercent = (pageZoomPercent + 15).coerceAtMost(200)
                    true
                }
                "zoom-out" -> {
                    pageZoomPercent = (pageZoomPercent - 15).coerceAtLeast(50)
                    true
                }
                "top-bar" -> {
                    revealTopBarAndFocus()
                    true
                }
                else -> false
            }
        }

        BrowserAgentBridge.onExecuteSiteTool = { rawSlug ->
            val slug = rawSlug.trim().replace("'", "")
            val wv = webViewRef
            BrowserAgentBridge.suppressUserInputPopupUntilMs = SystemClock.elapsedRealtime() + 2000L
            showWebTextFieldInputModal = false
            showUrlInputDialog = false
            agentTargetingUrlBar = false
            keyboardController?.hide()
            if (wv != null && slug.isNotEmpty()) {
                val clickJs = """
                    (function() {
                        window.__rnaSuppressInputPopupUntil = Date.now() + 2000;
                        var slug = '$slug';
                        var el = document.querySelector('[data-rna-site-tool="' + slug + '"]');
                        if (!el) {
                            var all = document.querySelectorAll('[data-rna-site-tool], input, textarea, button, a, [role="button"]');
                            var norm = slug.toLowerCase().replace(/-(field|button|link)$/, '');
                            for (var i = 0; i < all.length; i++) {
                                var cand = all[i];
                                var attr = (cand.getAttribute('data-rna-site-tool') || '').toLowerCase();
                                var txt = ((cand.getAttribute('aria-label') || cand.getAttribute('placeholder') || cand.getAttribute('name') || cand.getAttribute('id') || cand.innerText || '') + '').toLowerCase();
                                if (attr.indexOf(norm) !== -1 || txt.indexOf(norm.replace(/-/g, ' ')) !== -1) {
                                    el = cand;
                                    break;
                                }
                            }
                        }
                        if (el) {
                            el.scrollIntoView({behavior: 'smooth', block: 'center'});
                            window.__rnaLastTargetedElement = el;
                            el.focus();
                            el.click();
                            return true;
                        }
                        return false;
                    })();
                """.trimIndent()
                wv.evaluateJavascript(clickJs, null)
                scope.launch {
                    delay(550L)
                    scanWebPageSiteTools(wv)
                }
                true
            } else {
                false
            }
        }

        BrowserAgentBridge.onExecuteTypeInBrowser = { textToType ->
            val wv = webViewRef
            BrowserAgentBridge.suppressUserInputPopupUntilMs = SystemClock.elapsedRealtime() + 2000L
            showWebTextFieldInputModal = false
            keyboardController?.hide()
            if (showUrlInputDialog || agentTargetingUrlBar) {
                showUrlInputDialog = false
                agentTargetingUrlBar = false
                val target = if (textToType.startsWith("http://") || textToType.startsWith("https://")) {
                    textToType
                } else if (textToType.contains(".") && !textToType.contains(" ")) {
                    "https://$textToType"
                } else {
                    "https://www.google.com/search?q=${Uri.encode(textToType)}"
                }
                currentUrl = target
                wv?.loadUrl(target)
                true
            } else if (wv != null) {
                val escaped = JSONObject.quote(textToType)
                val typeJs = """
                    (function() {
                        window.__rnaSuppressInputPopupUntil = Date.now() + 2000;
                        var text = $escaped;
                        var el = window.__rnaLastTargetedElement || document.activeElement;
                        if (!el || (el.tagName !== 'INPUT' && el.tagName !== 'TEXTAREA' && !el.isContentEditable)) {
                            el = document.querySelector('input[type="search"], input[type="text"], input:not([type="hidden"]):not([type="submit"]):not([type="button"]), textarea, [contenteditable="true"]');
                        }
                        if (el) {
                            el.focus();
                            if (el.isContentEditable) {
                                el.innerText = text;
                            } else {
                                var proto = el.tagName === 'TEXTAREA' ? window.HTMLTextAreaElement.prototype : window.HTMLInputElement.prototype;
                                var nativeSetter = Object.getOwnPropertyDescriptor(proto, 'value');
                                if (nativeSetter && nativeSetter.set) {
                                    nativeSetter.set.call(el, text);
                                } else {
                                    el.value = text;
                                }
                            }
                            el.dispatchEvent(new Event('input', { bubbles: true }));
                            el.dispatchEvent(new Event('change', { bubbles: true }));
                            return true;
                        }
                        return false;
                    })();
                """.trimIndent()
                wv.evaluateJavascript(typeJs, null)
                scope.launch {
                    delay(400L)
                    scanWebPageSiteTools(wv)
                }
                true
            } else {
                false
            }
        }

        BrowserAgentBridge.onExecuteBrowserKeyOrHold = { normalizedCmd ->
            val wv = webViewRef
            BrowserAgentBridge.suppressUserInputPopupUntilMs = SystemClock.elapsedRealtime() + 1800L
            val isHold = normalizedCmd.contains("[hold]")
            val isDouble = normalizedCmd.contains("[double]")
            val base = normalizedCmd
                .replace("[hold]", "")
                .replace("[double]", "")
                .replace("d-pad.ok.left", "d-pad.left")

            if (base.startsWith("mouse.move[") && base.endsWith("]")) {
                val coords = base.removePrefix("mouse.move[").removeSuffix("]").split(",")
                val dx = coords.getOrNull(0)?.toFloatOrNull() ?: 0f
                val dy = coords.getOrNull(1)?.toFloatOrNull() ?: 0f
                inputMode = WebContainerActivity.MODE_MOUSE
                cursorX = (cursorX + dx).coerceIn(6f, containerWidthPx - 6f)
                cursorY = (cursorY + dy).coerceIn(6f, containerHeightPx - 6f)
                true
            } else if (base == "d-pad.ok" || base == "ok") {
                if (isHold) {
                    performHoldOkModeAdvance()
                } else {
                    val clickOnce = {
                        showWebTextFieldInputModal = false
                        when (inputMode) {
                            WebContainerActivity.MODE_DPAD -> {
                                wv?.evaluateJavascript("window.__rnaSuppressInputPopupUntil = Date.now() + 1800; if(window.__rnaActivateFocused) window.__rnaActivateFocused();", null)
                            }
                            WebContainerActivity.MODE_MOUSE -> {
                                wv?.let { targetWv ->
                                    targetWv.evaluateJavascript("window.__rnaSuppressInputPopupUntil = Date.now() + 1800;", null)
                                    isCursorPressed = true
                                    val downTime = SystemClock.uptimeMillis()
                                    val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, cursorX, cursorY, 0)
                                    val upEvent = MotionEvent.obtain(downTime, downTime + 45, MotionEvent.ACTION_UP, cursorX, cursorY, 0)
                                    targetWv.dispatchTouchEvent(downEvent)
                                    targetWv.postDelayed({
                                        targetWv.dispatchTouchEvent(upEvent)
                                        downEvent.recycle()
                                        upEvent.recycle()
                                        isCursorPressed = false
                                    }, 50)
                                }
                            }
                            WebContainerActivity.MODE_SCROLL -> {
                                inputMode = WebContainerActivity.MODE_MOUSE
                                triggerModeBanner()
                            }
                        }
                    }
                    clickOnce()
                    if (isDouble) {
                        scope.launch {
                            delay(90L)
                            clickOnce()
                        }
                    }
                }
                true
            } else if (base in listOf("d-pad.up", "d-pad.down", "d-pad.left", "d-pad.right", "up", "down", "left", "right")) {
                val repeats = if (isDouble) 2 else if (isHold) 5 else 1
                val dir = when {
                    base.endsWith("up") -> "UP"
                    base.endsWith("down") -> "DOWN"
                    base.endsWith("left") -> "LEFT"
                    else -> "RIGHT"
                }
                repeat(repeats) {
                    when (inputMode) {
                        WebContainerActivity.MODE_DPAD -> {
                            wv?.evaluateJavascript("if(window.__rnaMoveDpad) window.__rnaMoveDpad('$dir');", null)
                        }
                        WebContainerActivity.MODE_MOUSE -> {
                            val step = 28f * mouseSpeed
                            when (dir) {
                                "UP" -> cursorY = (cursorY - step).coerceAtLeast(6f)
                                "DOWN" -> cursorY = (cursorY + step).coerceAtMost(containerHeightPx - 6f)
                                "LEFT" -> cursorX = (cursorX - step).coerceAtLeast(6f)
                                "RIGHT" -> cursorX = (cursorX + step).coerceAtMost(containerWidthPx - 6f)
                            }
                        }
                        WebContainerActivity.MODE_SCROLL -> {
                            val scrollStep = (140 * mouseSpeed).toInt()
                            when (dir) {
                                "UP" -> wv?.scrollBy(0, -scrollStep)
                                "DOWN" -> wv?.scrollBy(0, scrollStep)
                                "LEFT" -> wv?.scrollBy(-scrollStep, 0)
                                "RIGHT" -> wv?.scrollBy(scrollStep, 0)
                            }
                        }
                    }
                }
                true
            } else {
                false
            }
        }

        BrowserAgentBridge.onCaptureWebViewBitmap = {
            val wv = webViewRef
            if (wv != null && wv.width > 0 && wv.height > 0) {
                runCatching {
                    val bmp = android.graphics.Bitmap.createBitmap(wv.width, wv.height, android.graphics.Bitmap.Config.ARGB_8888)
                    val c = android.graphics.Canvas(bmp)
                    wv.draw(c)
                    bmp
                }.getOrNull()
            } else null
        }
        BrowserAgentBridge.onRequestFocusWebCanvas = {
            focusWebCanvasSafely()
        }

        if (isolateProfileWebData) {
            CookieManager.getInstance().flush()
        }
        onDispose {
            BrowserAgentBridge.clearBridge()
            holdOkJob?.cancel()
            movementJob?.cancel()
            bannerJob?.cancel()
            webViewRef?.let { wv ->
                try {
                    wv.stopLoading()
                    wv.onPause()
                    (wv.parent as? ViewGroup)?.removeView(wv)
                    wv.removeAllViews()
                    wv.destroy()
                } catch (_: Exception) {
                }
            }
            webViewRef = null
        }
    }

    // Back button behavior:
    // 1. If in Full-Screen mode -> exit full screen first
    // 2. If in Scroll or non-base mode -> step back one stage
    // 3. If WebView canGoBack() -> navigate back
    // 4. Otherwise -> return to Home tab
    BackHandler(enabled = true) {
        if (isFullScreen) {
            onToggleFullScreen(false)
            onTopBarAutoHideChange(false)
            return@BackHandler
        }
        val prevMode = stepBackInputModeOnBack(inputMode, defaultBaseMode)
        if (prevMode != null) {
            inputMode = prevMode
            triggerModeBanner()
            return@BackHandler
        }
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else {
            onTopBarAutoHideChange(false)
            onExitToHomeTab()
        }
    }

    fun checkElementAndOpenKeyboardIfInput(wv: WebView, jsTargetExpr: String) {
        if (shouldSuppressWebInputPopup()) return
        val checkJs = """
            (function() {
                var el = $jsTargetExpr;
                if (!el) return '';
                var tag = (el.tagName || '').toUpperCase();
                var isInput = (tag === 'TEXTAREA' || el.isContentEditable);
                if (tag === 'INPUT') {
                    var t = (el.getAttribute('type') || 'text').toLowerCase();
                    isInput = ['button','submit','reset','checkbox','radio','file','image','range','color','hidden'].indexOf(t) === -1;
                }
                if (isInput) {
                    window.__rnaLastTargetedElement = el;
                    el.focus();
                    var curVal = el.value !== undefined ? el.value : (el.innerText || '');
                    var hint = el.getAttribute('placeholder') || el.getAttribute('aria-label') || el.getAttribute('name') || 'Web Text Field';
                    if (window.RnaBrowserInputBridge && window.RnaBrowserInputBridge.onWebInputClicked) {
                        window.RnaBrowserInputBridge.onWebInputClicked(curVal + '', hint + '');
                    }
                    return 'INPUT';
                }
                return 'OTHER';
            })();
        """.trimIndent()
        wv.evaluateJavascript(checkJs, null)
    }

    fun simulateBrowserClick(x: Float, y: Float) {
        val wv = webViewRef ?: return
        isCursorPressed = true
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0)
        val upEvent = MotionEvent.obtain(downTime, eventTime + 45, MotionEvent.ACTION_UP, x, y, 0)
        wv.dispatchTouchEvent(downEvent)
        wv.postDelayed({
            wv.dispatchTouchEvent(upEvent)
            downEvent.recycle()
            upEvent.recycle()
            isCursorPressed = false
            val density = wv.resources.displayMetrics.density.coerceAtLeast(1f)
            val cssX = (x / density).toInt()
            val cssY = (y / density).toInt()
            checkElementAndOpenKeyboardIfInput(wv, "document.elementFromPoint($cssX, $cssY)")
        }, 50)
    }

    fun startContinuousCursorOrScroll(keyCode: Int) {
        movementJob?.cancel()
        movementJob = scope.launch {
            var ticks = 0
            val speed = mouseSpeed.coerceIn(0.5f, 3.0f)
            val edgeMarginY = containerHeightPx * 0.12f
            val edgeMarginX = containerWidthPx * 0.08f
            while (isActive && activeDirectionKey == keyCode) {
                ticks++
                val accel = (0.85f + (ticks.coerceAtMost(22) * 0.08f)) * speed
                if (inputMode == WebContainerActivity.MODE_MOUSE) {
                    val delta = 8.5f * accel
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            cursorX = (cursorX - delta).coerceAtLeast(6f)
                            if (cursorX < edgeMarginX) webViewRef?.scrollBy((-10 * speed).toInt(), 0)
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            cursorX = (cursorX + delta).coerceAtMost(containerWidthPx - 6f)
                            if (cursorX > containerWidthPx - edgeMarginX) webViewRef?.scrollBy((10 * speed).toInt(), 0)
                        }
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            cursorY = (cursorY - delta).coerceAtLeast(6f)
                            if (cursorY < edgeMarginY) webViewRef?.scrollBy(0, (-14 * accel).toInt())
                            // Moving virtual mouse cursor to the very top edge reveals the Top Bar & switches to D-Pad control on the top bar!
                            if (cursorY <= 20f && !isFullScreen) {
                                revealTopBarAndFocus(fromMouseTopEdge = true)
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            cursorY = (cursorY + delta).coerceAtMost(containerHeightPx - 6f)
                            if (cursorY > containerHeightPx - edgeMarginY) webViewRef?.scrollBy(0, (14 * accel).toInt())
                            if (cursorY >= containerHeightPx - 16f &&
                                RnaAutonomousAgentController.isAgentModeActive.value &&
                                RnaAutonomousAgentController.isPaused.value
                            ) {
                                activeDirectionKey = 0
                                RnaAutonomousAgentController.onReturnFocusToAgentBarCallback?.invoke()
                            }
                        }
                    }
                } else if (inputMode == WebContainerActivity.MODE_SCROLL) {
                    val scrollStep = (26f * accel).toInt()
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_UP -> {
                            webViewRef?.scrollBy(0, -scrollStep)
                            webViewRef?.evaluateJavascript("window.scrollBy(0, -$scrollStep);", null)
                            if ((webViewRef?.scrollY ?: 0) <= 0 && isTopBarAutoHidden && !isFullScreen) {
                                revealTopBarAndFocus()
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            val wv = webViewRef
                            wv?.scrollBy(0, scrollStep)
                            wv?.evaluateJavascript("window.scrollBy(0, $scrollStep);", null)
                            if (wv != null && !wv.canScrollVertically(1) &&
                                RnaAutonomousAgentController.isAgentModeActive.value &&
                                RnaAutonomousAgentController.isPaused.value
                            ) {
                                activeDirectionKey = 0
                                RnaAutonomousAgentController.onReturnFocusToAgentBarCallback?.invoke()
                            }
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            webViewRef?.scrollBy(-scrollStep, 0)
                            webViewRef?.evaluateJavascript("window.scrollBy(-$scrollStep, 0);", null)
                        }
                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            webViewRef?.scrollBy(scrollStep, 0)
                            webViewRef?.evaluateJavascript("window.scrollBy($scrollStep, 0);", null)
                        }
                    }
                }
                delay(16L)
            }
        }
    }

    val isBookmarked = remember(currentUrl, bookmarks) {
        bookmarks.any { it.url.equals(currentUrl, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1017))
            .testTag("tv_browser_tab_root")
    ) {
        // 1. Browser Top Control Bar & Bookmarks Strip
        // Renders immediately on Frame 1; auto-hides after 5s; reveals smoothly when user moves Up!
        AnimatedVisibility(visible = !isFullScreen && !isTopBarAutoHidden) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF151822))
                    .onGloballyPositioned { topChromeHeightPx = it.size.height }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Home button -> back to Home Tab
                    var homeFocused by remember { mutableStateOf(false) }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (homeFocused) Color.White else CyanPrimary,
                        border = BorderStroke(1.5.dp, if (homeFocused) Color.White else CyanPrimary),
                        modifier = Modifier
                            .onFocusChanged {
                                homeFocused = it.isFocused
                                if (it.isFocused) notifyTopBarActivity()
                            }
                            .onPreviewKeyEvent { ev ->
                                if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                    when (ev.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_UP -> focusLauncherTopBarSafely()
                                        KeyEvent.KEYCODE_DPAD_DOWN -> focusDownFromToolbarRowSafely()
                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            }
                            .clickable {
                                onTopBarAutoHideChange(false)
                                onExitToHomeTab()
                            }
                            .testTag("tv_browser_home_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                tint = Color(0xFF12131A),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Home",
                                color = Color(0xFF12131A),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    TvFocusableIconBtn(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onFocused = { notifyTopBarActivity() },
                        onUpToTopBar = { focusLauncherTopBarSafely() },
                        onDownToWeb = { focusDownFromToolbarRowSafely() },
                        onClick = {
                            notifyTopBarActivity()
                            if (webViewRef?.canGoBack() == true) webViewRef?.goBack()
                        }
                    )

                    TvFocusableIconBtn(
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        onFocused = { notifyTopBarActivity() },
                        onUpToTopBar = { focusLauncherTopBarSafely() },
                        onDownToWeb = { focusDownFromToolbarRowSafely() },
                        onClick = {
                            notifyTopBarActivity()
                            if (webViewRef?.canGoForward() == true) webViewRef?.goForward()
                        }
                    )

                    TvFocusableIconBtn(
                        icon = Icons.Default.Refresh,
                        contentDescription = "Reload",
                        onFocused = { notifyTopBarActivity() },
                        onUpToTopBar = { focusLauncherTopBarSafely() },
                        onDownToWeb = { focusDownFromToolbarRowSafely() },
                        onClick = {
                            notifyTopBarActivity()
                            webViewRef?.reload()
                        }
                    )

                    // Address / Search Pill
                    var addrFocused by remember { mutableStateOf(false) }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (addrFocused) Color(0xFF2E344B) else Color(0xFF222534),
                        border = BorderStroke(
                            width = if (addrFocused) 2.dp else 1.dp,
                            color = if (addrFocused) CyanPrimary else Color(0x33FFFFFF)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(addressBarFocusRequester)
                            .onFocusChanged {
                                addrFocused = it.isFocused
                                if (it.isFocused) notifyTopBarActivity()
                            }
                            .onPreviewKeyEvent { ev ->
                                if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                    when (ev.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_UP -> focusLauncherTopBarSafely()
                                        KeyEvent.KEYCODE_DPAD_DOWN -> focusDownFromToolbarRowSafely()
                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            }
                            .clickable {
                                notifyTopBarActivity()
                                showUrlInputDialog = true
                            }
                            .testTag("tv_browser_address_bar")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentTitle,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentUrl,
                                    color = Color(0xFF90909E),
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Quick Page Scroll Up / Down Buttons
                    TvFocusableIconBtn(
                        icon = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Page Up",
                        tint = CyanPrimary,
                        onFocused = { notifyTopBarActivity() },
                        onUpToTopBar = { focusLauncherTopBarSafely() },
                        onDownToWeb = { focusDownFromToolbarRowSafely() },
                        onClick = {
                            notifyTopBarActivity()
                            webViewRef?.scrollBy(0, -420)
                            webViewRef?.evaluateJavascript("window.scrollBy({top: -420, behavior: 'smooth'});", null)
                        }
                    )
                    TvFocusableIconBtn(
                        icon = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Page Down",
                        tint = CyanPrimary,
                        onFocused = { notifyTopBarActivity() },
                        onUpToTopBar = { focusLauncherTopBarSafely() },
                        onDownToWeb = { focusDownFromToolbarRowSafely() },
                        onClick = {
                            notifyTopBarActivity()
                            webViewRef?.scrollBy(0, 420)
                            webViewRef?.evaluateJavascript("window.scrollBy({top: 420, behavior: 'smooth'});", null)
                        }
                    )

                    // Page Zoom Controls (- / +)
                    TvFocusableIconBtn(
                        icon = Icons.Default.ZoomOut,
                        contentDescription = "Zoom Out",
                        onFocused = { notifyTopBarActivity() },
                        onUpToTopBar = { focusLauncherTopBarSafely() },
                        onDownToWeb = { focusDownFromToolbarRowSafely() },
                        onClick = {
                            notifyTopBarActivity()
                            pageZoomPercent = (pageZoomPercent - 15).coerceIn(70, 220)
                            webViewRef?.settings?.textZoom = pageZoomPercent
                            controlPrefs.edit().putInt("browser_zoom_percent", pageZoomPercent).apply()
                        }
                    )
                    TvFocusableIconBtn(
                        icon = Icons.Default.ZoomIn,
                        contentDescription = "Zoom In",
                        onFocused = { notifyTopBarActivity() },
                        onUpToTopBar = { focusLauncherTopBarSafely() },
                        onDownToWeb = { focusDownFromToolbarRowSafely() },
                        onClick = {
                            notifyTopBarActivity()
                            pageZoomPercent = (pageZoomPercent + 15).coerceIn(70, 220)
                            webViewRef?.settings?.textZoom = pageZoomPercent
                            controlPrefs.edit().putInt("browser_zoom_percent", pageZoomPercent).apply()
                        }
                    )

                    // Ad & Popup Shield Toggle
                    TvFocusableIconBtn(
                        icon = Icons.Default.Shield,
                        contentDescription = "Ad & Popup Shield",
                        tint = if (adBlockEnabled) EmeraldSuccess else Color(0xFF90909E),
                        onFocused = { notifyTopBarActivity() },
                        onUpToTopBar = { focusLauncherTopBarSafely() },
                        onDownToWeb = { focusDownFromToolbarRowSafely() },
                        onClick = {
                            notifyTopBarActivity()
                            adBlockEnabled = !adBlockEnabled
                            controlPrefs.edit().putBoolean("browser_adblock_enabled", adBlockEnabled).apply()
                            Toast.makeText(
                                context,
                                if (adBlockEnabled) "RNA. Ad & Popup Shield ON ($blockedAdsCount blocked)" else "Ad Shield OFF",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )

                    // Bookmark Toggle Button
                    TvFocusableIconBtn(
                        icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark Website",
                        tint = if (isBookmarked) AmberAccent else Color.White,
                        onFocused = { notifyTopBarActivity() },
                        onUpToTopBar = { focusLauncherTopBarSafely() },
                        onDownToWeb = { focusDownFromToolbarRowSafely() },
                        onClick = {
                            notifyTopBarActivity()
                            val updated = if (isBookmarked) {
                                bookmarks.filterNot { it.url.equals(currentUrl, ignoreCase = true) }
                            } else {
                                bookmarks + TvBrowserBookmark(
                                    title = currentTitle.take(22).ifBlank { "Bookmark" },
                                    url = currentUrl
                                )
                            }
                            bookmarks = updated
                            saveProfileBookmarks(controlPrefs, bookmarkNamespace, updated)
                            Toast.makeText(
                                context,
                                if (isBookmarked) "Removed bookmark" else "Bookmarked ${currentTitle.take(20)}",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.testTag("tv_browser_bookmark_btn")
                    )

                    // Convert Current Website to RNA. App Button!
                    var convertFocused by remember { mutableStateOf(false) }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (convertFocused) EmeraldSuccess else EmeraldSuccess.copy(alpha = 0.2f),
                        border = BorderStroke(1.5.dp, if (convertFocused) Color.White else EmeraldSuccess),
                        modifier = Modifier
                            .onFocusChanged {
                                convertFocused = it.isFocused
                                if (it.isFocused) notifyTopBarActivity()
                            }
                            .onPreviewKeyEvent { ev ->
                                if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                    when (ev.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_UP -> focusLauncherTopBarSafely()
                                        KeyEvent.KEYCODE_DPAD_DOWN -> focusDownFromToolbarRowSafely()
                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            }
                            .clickable {
                                notifyTopBarActivity()
                                val liveUrl = webViewRef?.url
                                    ?.takeIf { it.isNotBlank() && !it.startsWith("about:") }
                                    ?: currentUrl
                                val liveTitle = webViewRef?.title
                                    ?.takeIf { it.isNotBlank() && !it.equals("about:blank", ignoreCase = true) }
                                    ?: currentTitle
                                convertSnapshotUrl = liveUrl
                                convertSnapshotTitle = liveTitle
                                convertSnapshotDomIcon = null
                                // Probe live page DOM for high-res apple-touch-icon / icon link asynchronously
                                webViewRef?.evaluateJavascript(
                                    """
                                    (function() {
                                      try {
                                        var selectors = [
                                          'link[rel="apple-touch-icon"]',
                                          'link[rel="apple-touch-icon-precomposed"]',
                                          'link[rel="icon"][sizes*="192"]',
                                          'link[rel="icon"][sizes*="180"]',
                                          'link[rel="icon"][sizes*="128"]',
                                          'link[rel="shortcut icon"]',
                                          'link[rel="icon"]'
                                        ];
                                        for (var i = 0; i < selectors.length; i++) {
                                          var el = document.querySelector(selectors[i]);
                                          if (el && el.href && el.href.indexOf('http') === 0) return el.href;
                                        }
                                        return '';
                                      } catch (e) { return ''; }
                                    })();
                                    """.trimIndent()
                                ) { raw ->
                                    val cleaned = raw?.trim()?.removeSurrounding("\"")?.replace("\\/", "/")?.trim()
                                    if (!cleaned.isNullOrBlank() && (cleaned.startsWith("http://") || cleaned.startsWith("https://"))) {
                                        convertSnapshotDomIcon = cleaned
                                    }
                                }
                                showConvertToRnaDialog = true
                            }
                            .testTag("tv_browser_convert_to_rna_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.InstallMobile,
                                contentDescription = "Convert to RNA. App",
                                tint = if (convertFocused) Color(0xFF12131A) else EmeraldSuccess,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Convert to App",
                                color = if (convertFocused) Color(0xFF12131A) else EmeraldSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Input Mode Pill (D-Pad / Mouse / Scroll)
                    var modePillFocused by remember { mutableStateOf(false) }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            modePillFocused -> Color.White
                            inputMode == WebContainerActivity.MODE_MOUSE -> Color(0xFF303F9F)
                            inputMode == WebContainerActivity.MODE_SCROLL -> Color(0xFF4A2574)
                            else -> Color(0xFF282A37)
                        },
                        border = BorderStroke(1.5.dp, if (modePillFocused) CyanPrimary else CyanPrimary.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .onFocusChanged {
                                modePillFocused = it.isFocused
                                if (it.isFocused) notifyTopBarActivity()
                            }
                            .onPreviewKeyEvent { ev ->
                                if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                    when (ev.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_UP -> focusLauncherTopBarSafely()
                                        KeyEvent.KEYCODE_DPAD_DOWN -> focusDownFromToolbarRowSafely()
                                        else -> false
                                    }
                                } else {
                                    false
                                }
                            }
                            .clickable {
                                notifyTopBarActivity()
                                performHoldOkModeAdvance()
                                focusWebCanvasSafely()
                            }
                            .testTag("tv_browser_mode_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = when (inputMode) {
                                    WebContainerActivity.MODE_MOUSE -> Icons.Default.Mouse
                                    WebContainerActivity.MODE_SCROLL -> Icons.Default.SwapVert
                                    else -> Icons.Default.Tv
                                },
                                contentDescription = null,
                                tint = if (modePillFocused) Color(0xFF12131A) else CyanPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = when (inputMode) {
                                    WebContainerActivity.MODE_MOUSE -> "Mouse"
                                    WebContainerActivity.MODE_SCROLL -> "Scroll"
                                    else -> "D-Pad"
                                },
                                color = if (modePillFocused) Color(0xFF12131A) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Full-Screen Toggle Button
                    TvFocusableIconBtn(
                        icon = Icons.Default.Fullscreen,
                        contentDescription = "Full Screen",
                        onFocused = { notifyTopBarActivity() },
                        onUpToTopBar = { focusLauncherTopBarSafely() },
                        onDownToWeb = { focusDownFromToolbarRowSafely() },
                        onClick = { onToggleFullScreen(true) },
                        modifier = Modifier.testTag("tv_browser_fullscreen_btn")
                    )

                    // Browser Downloads Full-Screen Tab Button
                    TvFocusableIconBtn(
                        icon = Icons.Default.Download,
                        contentDescription = "Browser Downloads",
                        tint = EmeraldSuccess,
                        onFocused = { notifyTopBarActivity() },
                        onUpToTopBar = { focusLauncherTopBarSafely() },
                        onDownToWeb = { focusDownFromToolbarRowSafely() },
                        onClick = {
                            notifyTopBarActivity()
                            refreshBrowserDownloads()
                            showBrowserDownloadsDialog = true
                        },
                        modifier = Modifier.testTag("tv_browser_downloads_btn")
                    )

                    // Browser Controls & Designs Settings Button
                    TvFocusableIconBtn(
                        icon = Icons.Default.Settings,
                        contentDescription = "Browser Settings",
                        tint = CyanPrimary,
                        onFocused = { notifyTopBarActivity() },
                        onUpToTopBar = { focusLauncherTopBarSafely() },
                        onDownToWeb = { focusDownFromToolbarRowSafely() },
                        onClick = {
                            notifyTopBarActivity()
                            showBrowserSettingsDialog = true
                        },
                        modifier = Modifier.testTag("tv_browser_settings_btn")
                    )
                }

                // Bookmarks Quick Bar
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tv_browser_bookmarks_row")
                ) {
                    val distinctBookmarks = bookmarks.distinctBy { it.url }
                    items(distinctBookmarks.size, key = { idx -> distinctBookmarks[idx].url }) { idx ->
                        val bm = distinctBookmarks[idx]
                        val active = currentUrl.startsWith(bm.url, ignoreCase = true)
                        var bmFocused by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                bmFocused -> Color.White
                                active -> CyanPrimary.copy(alpha = 0.22f)
                                else -> Color(0xFF222534)
                            },
                            border = BorderStroke(
                                width = if (bmFocused) 1.5.dp else 1.dp,
                                color = if (bmFocused || active) CyanPrimary else Color(0x28FFFFFF)
                            ),
                            modifier = Modifier
                                .then(
                                    if (idx == 0) Modifier.focusRequester(firstBookmarkFocusRequester) else Modifier
                                )
                                .onFocusChanged {
                                    bmFocused = it.isFocused
                                    if (it.isFocused) notifyTopBarActivity()
                                }
                                .onPreviewKeyEvent { ev ->
                                    if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                        when (ev.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_DPAD_UP -> {
                                                runCatching { addressBarFocusRequester.requestFocus() }.isSuccess
                                            }
                                            KeyEvent.KEYCODE_DPAD_DOWN -> focusWebCanvasSafely()
                                            else -> false
                                        }
                                    } else {
                                        false
                                    }
                                }
                                .clickable {
                                    notifyTopBarActivity()
                                    currentUrl = bm.url
                                    webViewRef?.loadUrl(bm.url)
                                    focusWebCanvasSafely()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val favUrl = remember(bm.url) { buildFaviconUrlForWebsite(bm.url) }
                                if (favUrl != null) {
                                    AsyncImage(
                                        model = favUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(13.dp)
                                            .clip(CircleShape)
                                    )
                                }
                                Text(
                                    text = bm.title,
                                    color = if (bmFocused) Color(0xFF12131A) else Color(0xFFE4E1E9),
                                    fontSize = 10.5.sp,
                                    fontWeight = if (bmFocused) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Main WebView Canvas + 3-Stage D-Pad / Mouse / Scroll Overlay
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onGloballyPositioned { coords ->
                    containerWidthPx = coords.size.width.toFloat().coerceAtLeast(320f)
                    containerHeightPx = coords.size.height.toFloat().coerceAtLeast(240f)
                }
                .focusRequester(webCanvasFocusRequester)
                .focusable()
                .onPreviewKeyEvent { keyEvent ->
                    val native = keyEvent.nativeKeyEvent
                    val keyCode = native.keyCode
                    when (native.action) {
                        KeyEvent.ACTION_DOWN -> {
                            when (keyCode) {
                                KeyEvent.KEYCODE_DPAD_CENTER,
                                KeyEvent.KEYCODE_ENTER,
                                KeyEvent.KEYCODE_NUMPAD_ENTER,
                                KeyEvent.KEYCODE_BUTTON_A -> {
                                    if (!isOkKeyDown) {
                                        isOkKeyDown = true
                                        okKeyDownTime = SystemClock.uptimeMillis()
                                        isLongPressTriggered = false
                                        holdOkJob?.cancel()
                                        holdOkJob = scope.launch {
                                            delay(420L)
                                            if (isOkKeyDown && !isLongPressTriggered) {
                                                isLongPressTriggered = true
                                                performHoldOkModeAdvance()
                                            }
                                        }
                                    } else if (!isLongPressTriggered && native.eventTime - okKeyDownTime >= 420L) {
                                        isLongPressTriggered = true
                                        holdOkJob?.cancel()
                                        performHoldOkModeAdvance()
                                    }
                                    true
                                }
                                KeyEvent.KEYCODE_DPAD_UP,
                                KeyEvent.KEYCODE_DPAD_DOWN,
                                KeyEvent.KEYCODE_DPAD_LEFT,
                                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                    if (inputMode == WebContainerActivity.MODE_DPAD) {
                                        if (native.repeatCount == 0 || native.repeatCount % 2 == 0) {
                                            val dir = when (keyCode) {
                                                KeyEvent.KEYCODE_DPAD_UP -> "UP"
                                                KeyEvent.KEYCODE_DPAD_DOWN -> "DOWN"
                                                KeyEvent.KEYCODE_DPAD_LEFT -> "LEFT"
                                                else -> "RIGHT"
                                            }
                                            val wv = webViewRef
                                            if (wv == null) {
                                                if (keyCode == KeyEvent.KEYCODE_DPAD_UP && !isFullScreen) {
                                                    revealTopBarAndFocus()
                                                }
                                            } else {
                                                wv.evaluateJavascript(
                                                    "if(window.__rnaMoveDpad) window.__rnaMoveDpad('$dir'); else false;"
                                                ) { result ->
                                                    val notMoved = (result == "false" || result == "null" || result == null)
                                                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP &&
                                                        notMoved &&
                                                        (wv.scrollY <= 24) &&
                                                        !isFullScreen
                                                    ) {
                                                        revealTopBarAndFocus()
                                                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN &&
                                                        notMoved &&
                                                        RnaAutonomousAgentController.isAgentModeActive.value &&
                                                        RnaAutonomousAgentController.isPaused.value
                                                    ) {
                                                        RnaAutonomousAgentController.onReturnFocusToAgentBarCallback?.invoke()
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        if (keyCode == KeyEvent.KEYCODE_DPAD_UP &&
                                            isTopBarAutoHidden &&
                                            !isFullScreen &&
                                            (inputMode == WebContainerActivity.MODE_SCROLL && (webViewRef?.scrollY ?: 0) <= 10)
                                        ) {
                                            revealTopBarAndFocus()
                                            return@onPreviewKeyEvent true
                                        }
                                        if (activeDirectionKey != keyCode) {
                                            activeDirectionKey = keyCode
                                            startContinuousCursorOrScroll(keyCode)
                                        }
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                        KeyEvent.ACTION_UP -> {
                            if (keyCode == activeDirectionKey) {
                                activeDirectionKey = 0
                                movementJob?.cancel()
                                true
                            } else if (
                                keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                keyCode == KeyEvent.KEYCODE_ENTER ||
                                keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
                                keyCode == KeyEvent.KEYCODE_BUTTON_A
                            ) {
                                val wasLong = isLongPressTriggered
                                isOkKeyDown = false
                                isLongPressTriggered = false
                                holdOkJob?.cancel()
                                if (!wasLong) {
                                    when (inputMode) {
                                        WebContainerActivity.MODE_DPAD -> {
                                            webViewRef?.let { wv ->
                                                wv.evaluateJavascript(
                                                    "if(window.__rnaActivateFocused) window.__rnaActivateFocused(); else 'NONE';"
                                                ) { res ->
                                                    if (res?.contains("INPUT") == true) {
                                                        checkElementAndOpenKeyboardIfInput(wv, "(window.__rnaFocusedEl || document.activeElement)")
                                                    }
                                                }
                                            }
                                        }
                                        WebContainerActivity.MODE_MOUSE -> {
                                            simulateBrowserClick(cursorX, cursorY)
                                        }
                                        WebContainerActivity.MODE_SCROLL -> {
                                            inputMode = WebContainerActivity.MODE_MOUSE
                                            triggerModeBanner()
                                        }
                                    }
                                }
                                true
                            } else {
                                false
                            }
                        }
                        else -> false
                    }
                }
        ) {
            if (outOfMemoryKilledUrl != null) {
                val reloadBtnFocus = remember { FocusRequester() }
                var reloadFocused by remember { mutableStateOf(false) }
                var homeFocused by remember { mutableStateOf(false) }
                LaunchedEffect(outOfMemoryKilledUrl) {
                    delay(80L)
                    runCatching { reloadBtnFocus.requestFocus() }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Color(0xFF1A1F34), Color(0xFF0A0D16))
                            )
                        )
                        .testTag("tv_browser_oom_recovery_overlay"),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = Color(0xF5141929),
                        border = BorderStroke(2.dp, Color(0xFFF59E0B)),
                        shadowElevation = 24.dp,
                        modifier = Modifier
                            .width(620.dp)
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0x33F59E0B),
                                border = BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Device Ran Out of Memory",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp
                            )
                            Text(
                                text = outOfMemoryReason,
                                color = Color(0xFFCBD5E1),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = outOfMemoryKilledUrl.orEmpty(),
                                color = CyanPrimary,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        val targetUrl = outOfMemoryKilledUrl ?: currentUrl
                                        outOfMemoryKilledUrl = null
                                        currentUrl = targetUrl
                                        webViewReloadEpoch += 1
                                        isWebViewMounted = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (reloadFocused) Color.White else CyanPrimary,
                                        contentColor = Color(0xFF090D16)
                                    ),
                                    border = BorderStroke(if (reloadFocused) 2.5.dp else 1.dp, Color.White),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .focusRequester(reloadBtnFocus)
                                        .onFocusChanged { reloadFocused = it.isFocused }
                                        .testTag("btn_browser_oom_reload")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Reload Webpage", fontWeight = FontWeight.Black)
                                }
                                OutlinedButton(
                                    onClick = {
                                        outOfMemoryKilledUrl = null
                                        onExitToHomeTab()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (homeFocused) Color.White else Color(0xFF1F263B),
                                        contentColor = if (homeFocused) Color(0xFF090D16) else Color.White
                                    ),
                                    border = BorderStroke(if (homeFocused) 2.5.dp else 1.dp, if (homeFocused) CyanPrimary else Color(0x44FFFFFF)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .onFocusChanged { homeFocused = it.isFocused }
                                        .testTag("btn_browser_oom_home")
                                ) {
                                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Return Home", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            } else if (isWebViewMounted) {
                androidx.compose.runtime.key(webViewReloadEpoch) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewRef = this
                                RnaControllerNetworkBridge.activeWebViewRef = java.lang.ref.WeakReference(this)
                                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                    setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true)
                                }
                                // Keep Compose's webCanvasFocusRequester in control of D-Pad events so native WebView focus never crashes Compose focus search
                                isFocusable = false
                                isFocusableInTouchMode = false
                                addJavascriptInterface(
                                    object {
                                        @JavascriptInterface
                                        fun onWebInputClicked(currentVal: String?, placeholder: String?) {
                                            if (shouldSuppressWebInputPopup()) return
                                            scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                                if (shouldSuppressWebInputPopup()) return@launch
                                                activeWebFieldText = currentVal.orEmpty()
                                                activeWebFieldLabel = placeholder?.takeIf { it.isNotBlank() } ?: "Webpage Text Field"
                                                showWebTextFieldInputModal = true
                                            }
                                        }
                                    },
                                    "RnaBrowserInputBridge"
                                )
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    allowFileAccess = true
                                    allowContentAccess = true
                                    mediaPlaybackRequiresUserGesture = browserFastPerformanceMode
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    setSupportZoom(true)
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    textZoom = pageZoomPercent
                                    setSupportMultipleWindows(false)
                                    javaScriptCanOpenWindowsAutomatically = !adBlockEnabled
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 RNA/2.5"
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: RenderProcessGoneDetail?
                                    ): Boolean {
                                        val crashedMsg = if (detail?.didCrash() == true) {
                                            "Webpage crashed or ran out of memory — closed safely to keep RNA. TV running."
                                        } else {
                                            "Device ran out of memory — webpage closed safely to keep RNA. TV running."
                                        }
                                        scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                            killWebpageToProtectApp(crashedMsg)
                                        }
                                        return true
                                    }

                                    override fun shouldInterceptRequest(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): WebResourceResponse? {
                                        val reqUrl = request?.url?.toString() ?: return super.shouldInterceptRequest(view, request)
                                        if ((adBlockEnabled || browserFastPerformanceMode) && isAdUrl(reqUrl)) {
                                            blockedAdsCount++
                                            return WebResourceResponse(
                                                "text/plain",
                                                "utf-8",
                                                ByteArrayInputStream(ByteArray(0))
                                            )
                                        }
                                        return super.shouldInterceptRequest(view, request)
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        if (!url.isNullOrBlank()) {
                                            currentUrl = url
                                            val detected = detectNativeAppForUrl(url)
                                            if (detected != null &&
                                                detected.appLabel !in dismissedAppPromptsForSession &&
                                                pendingAppRedirectUrl == null
                                            ) {
                                                pendingAppRedirectLabel = detected.appLabel
                                                pendingAppRedirectUrl = url
                                            }
                                        }
                                        if (!view?.title.isNullOrBlank()) currentTitle = view?.title ?: currentTitle
                                        view?.loadUrl(WebContainerActivity.JS_INIT_SPATIAL_NAV)
                                        if (browserFastPerformanceMode) {
                                            view?.loadUrl(WebContainerActivity.JS_FAST_BROWSER_OPTIMIZER)
                                        }
                                        view?.evaluateJavascript(RnaControllerNetworkBridge.buildXboxGamepadSpoofJs(), null)
                                        if (isForcedDark) {
                                            view?.loadUrl(WebContainerActivity.CSS_FORCE_DARK)
                                        }
                                        scanWebPageSiteTools(view)
                                    }

                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val target = request?.url?.toString() ?: return false
                                        if (adBlockEnabled && isAdUrl(target)) {
                                            blockedAdsCount++
                                            return true
                                        }
                                        val lower = target.lowercase()
                                        val detected = detectNativeAppForUrl(target)
                                        if (detected != null &&
                                            (detected.schemePrefixes.any { lower.startsWith(it) } || lower.startsWith("intent://"))
                                        ) {
                                            pendingAppRedirectLabel = detected.appLabel
                                            pendingAppRedirectUrl = target
                                            return true
                                        }
                                        if (lower.startsWith("intent://") || lower.startsWith("market://")) {
                                            pendingAppRedirectLabel = "App"
                                            pendingAppRedirectUrl = target
                                            return true
                                        }
                                        if (detected != null && detected.appLabel !in dismissedAppPromptsForSession) {
                                            pendingAppRedirectLabel = detected.appLabel
                                            pendingAppRedirectUrl = target
                                        }
                                        currentUrl = target
                                        return false
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        super.onProgressChanged(view, newProgress)
                                        pageProgress = newProgress
                                    }

                                    override fun onReceivedTitle(view: WebView?, title: String?) {
                                        super.onReceivedTitle(view, title)
                                        if (!title.isNullOrBlank()) currentTitle = title
                                    }

                                    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                                        super.onReceivedIcon(view, icon)
                                        if (icon != null && !icon.isRecycled && icon.width >= 16 && icon.height >= 16) {
                                            currentPageFaviconBitmap = icon.copy(Bitmap.Config.ARGB_8888, false)
                                        }
                                    }
                                }

                                setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                                    val guessed = URLUtil.guessFileName(url, contentDisposition, mimetype)
                                    startBrowserFileDownload(
                                        url = url,
                                        suggestedName = guessed,
                                        userAgent = userAgent,
                                        mimeType = mimetype
                                    )
                                })

                                loadUrl(currentUrl)
                            }
                        },
                        update = { view ->
                            view.settings.textZoom = pageZoomPercent
                            view.settings.javaScriptCanOpenWindowsAutomatically = !adBlockEnabled
                        }
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0E1017)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Initializing RNA. TV Browser...",
                        color = Color(0xFF9E9FAF),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (pageProgress in 1..99) {
                LinearProgressIndicator(
                    progress = { pageProgress / 100f },
                    color = CyanPrimary,
                    trackColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter)
                )
            }

            // 3-Stage Mouse & Scroll Wheel Overlay
            RnaWebInputOverlay(
                inputMode = inputMode,
                defaultBaseMode = defaultBaseMode,
                cursorX = cursorX,
                cursorY = cursorY,
                isCursorPressed = isCursorPressed,
                cursorDesign = cursorDesign,
                scrollWheelDesign = scrollWheelDesign,
                mouseSpeed = mouseSpeed,
                modeBannerVisible = modeBannerVisible,
                activeScrollDirection = activeDirectionKey,
                onScrollStepClick = { dx, dy ->
                    webViewRef?.scrollBy(dx, dy)
                    webViewRef?.evaluateJavascript("window.scrollBy({left: $dx, top: $dy, behavior: 'smooth'});", null)
                }
            )

            // Subtle Top-Edge Reveal Pill when Top Bar is Auto-Hidden (Clicking or moving Up reveals Top Bar)
            if (isTopBarAutoHidden && !isFullScreen) {
                Surface(
                    shape = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp),
                    color = Color(0xAA151822),
                    border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .clickable { revealTopBarAndFocus() }
                        .testTag("tv_browser_reveal_topbar_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Show Top Bar",
                            tint = CyanPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Move Up to Show Top Bar & Home",
                            color = Color(0xFFE4E1E9),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Floating Exit Full-Screen Button when Full-Screen is active
            if (isFullScreen) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xCC161822),
                    border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .clickable { onToggleFullScreen(false) }
                        .testTag("tv_browser_exit_fullscreen_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloseFullscreen,
                            contentDescription = "Exit Full Screen",
                            tint = CyanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Exit Full Screen",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Modal Pop-Up Dialog asking if the user wants to open the Native TV App (YouTube, Netflix, JioHotstar, Prime Video, etc.)
            if (pendingAppRedirectUrl != null) {
                val targetRedirect = pendingAppRedirectUrl ?: currentUrl
                val openAppBtnFocusRequester = remember { FocusRequester() }
                var openAppFocused by remember { mutableStateOf(false) }
                var stayBrowserFocused by remember { mutableStateOf(false) }

                LaunchedEffect(pendingAppRedirectUrl) {
                    delay(50L)
                    runCatching { openAppBtnFocusRequester.requestFocus() }
                }

                Dialog(
                    onDismissRequest = {
                        dismissedAppPromptsForSession = dismissedAppPromptsForSession + pendingAppRedirectLabel
                        pendingAppRedirectUrl = null
                    }
                ) {
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = Color(0xF8151926),
                        border = BorderStroke(2.dp, CyanPrimary),
                        shadowElevation = 24.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tv_browser_open_youtube_app_prompt")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = CyanPrimary.copy(alpha = 0.2f),
                                    border = BorderStroke(1.5.dp, CyanPrimary),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Tv,
                                            contentDescription = null,
                                            tint = CyanPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Do you want to open the $pendingAppRedirectLabel app?",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 17.sp
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Press OK to launch the native $pendingAppRedirectLabel TV app, or Cancel to continue in RNA. Browser.",
                                        color = Color(0xFFB4B9CC),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        dismissedAppPromptsForSession = dismissedAppPromptsForSession + pendingAppRedirectLabel
                                        pendingAppRedirectUrl = null
                                        focusWebCanvasSafely()
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (stayBrowserFocused) Color.White else Color(0xFF222738),
                                        contentColor = if (stayBrowserFocused) Color(0xFF12131A) else Color.White
                                    ),
                                    border = BorderStroke(
                                        width = if (stayBrowserFocused) 2.5.dp else 1.dp,
                                        color = if (stayBrowserFocused) CyanPrimary else Color(0x44FFFFFF)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .onFocusChanged { stayBrowserFocused = it.isFocused }
                                        .testTag("tv_browser_stay_in_browser_btn")
                                ) {
                                    Text(
                                        text = "Cancel (Stay in Browser)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                }

                                Button(
                                    onClick = {
                                        val urlToOpen = targetRedirect
                                        val labelToOpen = pendingAppRedirectLabel
                                        pendingAppRedirectUrl = null
                                        launchNativeTargetApp(context, urlToOpen, labelToOpen)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (openAppFocused) Color.White else CyanPrimary,
                                        contentColor = Color(0xFF12131A)
                                    ),
                                    border = BorderStroke(
                                        width = if (openAppFocused) 2.5.dp else 1.5.dp,
                                        color = if (openAppFocused) Color.White else CyanPrimary
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .focusRequester(openAppBtnFocusRequester)
                                        .onFocusChanged { openAppFocused = it.isFocused }
                                        .testTag("tv_browser_confirm_open_app_btn")
                                ) {
                                    Text(
                                        text = "OK (Open $pendingAppRedirectLabel App)",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog 0: Webpage Text Field Keyboard Input Popup (Bottom-center TV Keyboard with Clipboard + Normal Voice + AI Voice)
    if (showWebTextFieldInputModal) {
        fun syncWebFieldText(newVal: String, submitForm: Boolean) {
            val wv = webViewRef ?: return
            val escaped = newVal
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
            val injectJs = """
                (function() {
                    var el = window.__rnaLastTargetedElement || window.__rnaFocusedEl || document.activeElement;
                    if (!el) return false;
                    el.focus();
                    var proto = Object.getPrototypeOf(el);
                    var desc = proto ? Object.getOwnPropertyDescriptor(proto, 'value') : null;
                    if (desc && desc.set) {
                        desc.set.call(el, '$escaped');
                    } else if (el.value !== undefined) {
                        el.value = '$escaped';
                    } else {
                        el.innerText = '$escaped';
                    }
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                    if ($submitForm) {
                        el.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true }));
                        el.dispatchEvent(new KeyboardEvent('keypress', { key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true }));
                        el.dispatchEvent(new KeyboardEvent('keyup', { key: 'Enter', code: 'Enter', keyCode: 13, which: 13, bubbles: true }));
                        if (el.form) {
                            var subBtn = el.form.querySelector('button[type="submit"], input[type="submit"], button:not([type="button"])');
                            if (subBtn) subBtn.click();
                            else el.form.submit();
                        }
                    }
                    return true;
                })();
            """.trimIndent()
            wv.evaluateJavascript(injectJs, null)
        }

        val webFieldFocusRequester = remember { FocusRequester() }
        val rnaDefaultIme = isRnaKeyboardDefaultIme(context)

        Dialog(
            onDismissRequest = {
                keyboardController?.hide()
                showWebTextFieldInputModal = false
                focusWebCanvasSafely()
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            EnableSystemKeyboardInDialog()
            val dialogView = LocalView.current
            val dialogKeyboardController = LocalSoftwareKeyboardController.current

            LaunchedEffect(showWebTextFieldInputModal, rnaDefaultIme) {
                if (showWebTextFieldInputModal && !rnaDefaultIme) {
                    delay(80L)
                    openDeviceDefaultKeyboard(
                        context = context,
                        view = dialogView,
                        focusRequester = webFieldFocusRequester,
                        keyboardController = dialogKeyboardController
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.TopCenter
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151925)),
                    border = BorderStroke(1.dp, CyanPrimary),
                    modifier = Modifier
                        .padding(top = 36.dp)
                        .width(560.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = activeWebFieldLabel.take(48).ifBlank { "Type in Web Field" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        OutlinedTextField(
                            value = activeWebFieldText,
                            onValueChange = { updated ->
                                activeWebFieldText = updated
                                syncWebFieldText(updated, submitForm = false)
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = Color(0x55FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(webFieldFocusRequester)
                                .tvKeyboardFieldTrigger(
                                    context = context,
                                    view = dialogView,
                                    focusRequester = webFieldFocusRequester,
                                    keyboardController = dialogKeyboardController,
                                    onOpenRnaKeyboard = {}
                                )
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(onClick = {
                                dialogKeyboardController?.hide()
                                showWebTextFieldInputModal = false
                                focusWebCanvasSafely()
                            }) {
                                Text("Cancel", color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    syncWebFieldText(activeWebFieldText, submitForm = true)
                                    dialogKeyboardController?.hide()
                                    showWebTextFieldInputModal = false
                                    focusWebCanvasSafely()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanPrimary,
                                    contentColor = Color(0xFF12131A)
                                )
                            ) {
                                Text("Submit", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (rnaDefaultIme) {
                    RnaTvKeyboardOverlay(
                        visible = true,
                        currentText = activeWebFieldText,
                        fieldHint = activeWebFieldLabel.take(48),
                        showTextPreviewBar = true,
                        onTextChange = { updated ->
                            activeWebFieldText = updated
                            syncWebFieldText(updated, submitForm = false)
                        },
                        onCommitChar = { ch ->
                            activeWebFieldText += ch
                            syncWebFieldText(activeWebFieldText, submitForm = false)
                        },
                        onBackspace = {
                            if (activeWebFieldText.isNotEmpty()) {
                                activeWebFieldText = activeWebFieldText.dropLast(1)
                                syncWebFieldText(activeWebFieldText, submitForm = false)
                            }
                        },
                        onSearchOrSubmit = {
                            syncWebFieldText(activeWebFieldText, submitForm = true)
                            dialogKeyboardController?.hide()
                            showWebTextFieldInputModal = false
                            focusWebCanvasSafely()
                        },
                        onDismiss = {
                            dialogKeyboardController?.hide()
                            showWebTextFieldInputModal = false
                            focusWebCanvasSafely()
                        }
                    )
                }
            }
        }
    }

    // Dialog 1: URL / Search Input with Default System Keyboard (or Bottom-Center RNA TV Keyboard if default IME)
    if (showUrlInputDialog) {
        var typed by remember(currentUrl) { mutableStateOf(currentUrl) }
        val urlFieldFocusRequester = remember { FocusRequester() }
        val rnaDefaultIme = isRnaKeyboardDefaultIme(context)

        Dialog(
            onDismissRequest = { showUrlInputDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            EnableSystemKeyboardInDialog()
            val dialogView = LocalView.current
            val dialogKeyboardController = LocalSoftwareKeyboardController.current

            LaunchedEffect(showUrlInputDialog, rnaDefaultIme) {
                if (showUrlInputDialog && !rnaDefaultIme) {
                    delay(80L)
                    openDeviceDefaultKeyboard(
                        context = context,
                        view = dialogView,
                        focusRequester = urlFieldFocusRequester,
                        keyboardController = dialogKeyboardController
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                // Top quick-destination bar above keyboard
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151925)),
                    border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp, start = 36.dp, end = 36.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Enter Website URL or Search Query",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("https://", "www.", ".com", ".org", "Clear").forEach { token ->
                                    var chipFocused by remember { mutableStateOf(false) }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (chipFocused) Color.White else Color(0xFF282A37),
                                        border = BorderStroke(1.dp, if (chipFocused) CyanPrimary else Color(0x33FFFFFF)),
                                        modifier = Modifier
                                            .onFocusChanged { chipFocused = it.isFocused }
                                            .clickable {
                                                if (token == "Clear") typed = "" else typed += token
                                            }
                                            .focusable()
                                    ) {
                                        Text(
                                            text = token,
                                            color = if (chipFocused) Color(0xFF12131A) else CyanPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = typed,
                                onValueChange = { typed = it },
                                singleLine = true,
                                placeholder = { Text("Search Google or type URL...") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = CyanPrimary,
                                    unfocusedBorderColor = Color(0x55FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(urlFieldFocusRequester)
                                    .tvKeyboardFieldTrigger(
                                        context = context,
                                        view = dialogView,
                                        focusRequester = urlFieldFocusRequester,
                                        keyboardController = dialogKeyboardController,
                                        onOpenRnaKeyboard = {}
                                    )
                            )
                            Button(
                                onClick = {
                                    val t = typed.trim()
                                    if (t.isNotEmpty()) {
                                        val resolved = when {
                                            t.startsWith("http://") || t.startsWith("https://") -> t
                                            t.contains(".") && !t.contains(" ") -> "https://$t"
                                            else -> "https://www.google.com/search?q=${Uri.encode(t)}"
                                        }
                                        currentUrl = resolved
                                        webViewRef?.loadUrl(resolved)
                                    }
                                    showUrlInputDialog = false
                                    focusWebCanvasSafely()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanPrimary,
                                    contentColor = Color(0xFF12131A)
                                )
                            ) {
                                Text("Go", fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val quickSites = listOf(
                                "Google" to "https://www.google.com",
                                "YouTube" to "https://www.youtube.com",
                                "DuckDuckGo" to "https://duckduckgo.com",
                                "Wikipedia" to "https://www.wikipedia.org",
                                "Reddit" to "https://www.reddit.com",
                                "Twitch" to "https://www.twitch.tv",
                                "GitHub" to "https://github.com",
                                "4K Wallpapers" to "https://www.google.com/search?tbm=isch&q=4k+uhd+tv+wallpaper"
                            )
                            items(quickSites) { (label, siteUrl) ->
                                var siteFocused by remember { mutableStateOf(false) }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (siteFocused) CyanPrimary else Color(0xFF252838),
                                    modifier = Modifier
                                        .onFocusChanged { siteFocused = it.isFocused }
                                        .clickable {
                                            currentUrl = siteUrl
                                            webViewRef?.loadUrl(siteUrl)
                                            showUrlInputDialog = false
                                            focusWebCanvasSafely()
                                        }
                                        .focusable()
                                ) {
                                    Text(
                                        text = label,
                                        color = if (siteFocused) Color(0xFF12131A) else Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (rnaDefaultIme) {
                    RnaTvKeyboardOverlay(
                        visible = true,
                        currentText = typed,
                        fieldHint = "Search Google or type URL...",
                        showTextPreviewBar = true,
                        onTextChange = { typed = it },
                        onCommitChar = { typed += it },
                        onBackspace = { if (typed.isNotEmpty()) typed = typed.dropLast(1) },
                        onSearchOrSubmit = {
                            val t = typed.trim()
                            if (t.isNotEmpty()) {
                                val resolved = when {
                                    t.startsWith("http://") || t.startsWith("https://") -> t
                                    t.contains(".") && !t.contains(" ") -> "https://$t"
                                    else -> "https://www.google.com/search?q=${Uri.encode(t)}"
                                }
                                currentUrl = resolved
                                webViewRef?.loadUrl(resolved)
                            }
                            showUrlInputDialog = false
                            focusWebCanvasSafely()
                        },
                        onDismiss = {
                            showUrlInputDialog = false
                            focusWebCanvasSafely()
                        }
                    )
                }
            }
        }
    }

    // Dialog 2: Full-Screen Browser Settings (Renamed to "Browser Settings", subtext removed)
    if (showBrowserSettingsDialog) {
        Dialog(
            onDismissRequest = { showBrowserSettingsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0D1018))
                    .padding(horizontal = 40.dp, vertical = 28.dp)
                    .testTag("tv_browser_settings_fullscreen")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Browser Settings",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    showBrowserSettingsDialog = false
                                    refreshBrowserDownloads()
                                    showBrowserDownloadsDialog = true
                                }
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Downloads", color = Color.White)
                            }
                            Button(
                                onClick = {
                                    showBrowserSettingsDialog = false
                                    focusWebCanvasSafely()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanPrimary,
                                    contentColor = Color(0xFF12131A)
                                )
                            ) {
                                Text("Done", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                defaultBaseMode = WebContainerActivity.MODE_DPAD
                                inputMode = WebContainerActivity.MODE_DPAD
                                controlPrefs.edit().putString(WebContainerActivity.KEY_BROWSER_DEFAULT_MODE, "DPAD").apply()
                                triggerModeBanner()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (defaultBaseMode == WebContainerActivity.MODE_DPAD) CyanPrimary else Color(0xFF282A37),
                                contentColor = if (defaultBaseMode == WebContainerActivity.MODE_DPAD) Color(0xFF12131A) else Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("D-Pad Default (Hold OK -> Mouse)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                defaultBaseMode = WebContainerActivity.MODE_MOUSE
                                inputMode = WebContainerActivity.MODE_MOUSE
                                centerMouseCursor()
                                controlPrefs.edit().putString(WebContainerActivity.KEY_BROWSER_DEFAULT_MODE, "MOUSE").apply()
                                triggerModeBanner()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (defaultBaseMode == WebContainerActivity.MODE_MOUSE) CyanPrimary else Color(0xFF282A37),
                                contentColor = if (defaultBaseMode == WebContainerActivity.MODE_MOUSE) Color(0xFF12131A) else Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Mouse Default (Hold OK -> D-Pad)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mouse Cursor Speed:", color = Color(0xFFE4E1E9), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("${String.format("%.1f", mouseSpeed)}x", color = CyanPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Slider(
                        value = mouseSpeed,
                        onValueChange = {
                            mouseSpeed = it
                            controlPrefs.edit().putFloat(WebContainerActivity.KEY_BROWSER_MOUSE_SPEED, it).apply()
                        },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(thumbColor = CyanPrimary, activeTrackColor = CyanPrimary)
                    )

                    Text("Mouse Cursor Design:", color = Color(0xFFB5B6C6), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RnaCursorDesign.entries.forEach { design ->
                            val selected = cursorDesign == design
                            var itemFocused by remember { mutableStateOf(false) }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = when {
                                    itemFocused -> CyanPrimary
                                    selected -> CyanPrimary.copy(alpha = 0.22f)
                                    else -> Color(0xFF282A37)
                                },
                                border = BorderStroke(1.5.dp, if (selected || itemFocused) CyanPrimary else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { itemFocused = it.isFocused }
                                    .clickable {
                                        cursorDesign = design
                                        controlPrefs.edit().putString(WebContainerActivity.KEY_CURSOR_DESIGN, design.id).apply()
                                    }
                            ) {
                                Text(
                                    text = design.label,
                                    color = if (itemFocused) Color(0xFF12131A) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp)
                                )
                            }
                        }
                    }

                    Text("Scroll Wheel Design:", color = Color(0xFFB5B6C6), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RnaScrollWheelDesign.entries.forEach { design ->
                            val selected = scrollWheelDesign == design
                            var itemFocused by remember { mutableStateOf(false) }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = when {
                                    itemFocused -> VioletSecondary
                                    selected -> VioletSecondary.copy(alpha = 0.22f)
                                    else -> Color(0xFF282A37)
                                },
                                border = BorderStroke(1.5.dp, if (selected || itemFocused) VioletSecondary else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { itemFocused = it.isFocused }
                                    .clickable {
                                        scrollWheelDesign = design
                                        controlPrefs.edit().putString(WebContainerActivity.KEY_SCROLL_DESIGN, design.id).apply()
                                    }
                            ) {
                                Text(
                                    text = design.label,
                                    color = if (itemFocused) Color(0xFF12131A) else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(onClick = { isForcedDark = !isForcedDark }) {
                            Icon(
                                imageVector = if (isForcedDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = VioletSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isForcedDark) "Force Dark: ON" else "Force Dark: OFF", color = Color.White, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                adBlockEnabled = !adBlockEnabled
                                controlPrefs.edit().putBoolean("browser_adblock_enabled", adBlockEnabled).apply()
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (adBlockEnabled) EmeraldSuccess else Color(0xFF90909E),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (adBlockEnabled) "Ad Shield: ON ($blockedAdsCount)" else "Ad Shield: OFF",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog 3: Full-Screen Convert Current Website to Installed RNA. App
    if (showConvertToRnaDialog) {
        val initialUrlSnap = remember {
            convertSnapshotUrl.ifBlank { currentUrl }.ifBlank { "https://www.google.com" }
        }
        val initialTitleSnap = remember {
            val raw = convertSnapshotTitle.ifBlank { currentTitle }.trim()
            if (raw.isBlank() || raw.equals("RNA. TV Browser", ignoreCase = true)) {
                runCatching {
                    Uri.parse(initialUrlSnap).host?.removePrefix("www.")?.substringBefore(".")
                        ?.replaceFirstChar { it.uppercase() }
                }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Web App"
            } else {
                raw.take(30)
            }
        }
        var appName by remember { mutableStateOf(initialTitleSnap) }
        var appUrl by remember { mutableStateOf(initialUrlSnap) }
        var userPickedCustomLogo by remember { mutableStateOf(false) }
        var selectedLogo by remember {
            mutableStateOf<String?>(
                convertSnapshotDomIcon
                    ?: buildFaviconUrlForWebsite(initialUrlSnap)
                    ?: currentPageFaviconBitmap?.let { bitmapToPngDataUri(it, 256) }
            )
        }
        var chosenInputMode by remember {
            mutableStateOf(if (defaultBaseMode == WebContainerActivity.MODE_MOUSE) "MOUSE" else "DPAD")
        }
        var chosenForceDark by remember { mutableStateOf(isForcedDark) }
        var pinToHomeFavorites by remember { mutableStateOf(true) }
        var isPackagingApp by remember { mutableStateOf(false) }
        var activeRnaKeyboardField by remember { mutableStateOf<String?>(null) } // "NAME" or "URL"

        val installBtnFocus = remember { FocusRequester() }
        val appNameFieldFocus = remember { FocusRequester() }
        val appUrlFieldFocus = remember { FocusRequester() }

        val logoPhotoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            uri?.let {
                userPickedCustomLogo = true
                selectedLogo = it.toString()
            }
        }

        // If high-res DOM icon arrives right after opening and user hasn't manually changed logo, adopt it
        LaunchedEffect(convertSnapshotDomIcon) {
            val domIcon = convertSnapshotDomIcon
            if (!userPickedCustomLogo && !domIcon.isNullOrBlank()) {
                selectedLogo = domIcon
            }
        }

        LaunchedEffect(Unit) {
            delay(80L)
            runCatching { installBtnFocus.requestFocus() }
        }

        Dialog(
            onDismissRequest = {
                if (!isPackagingApp) showConvertToRnaDialog = false
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            EnableSystemKeyboardInDialog()
            val dialogView = LocalView.current
            val dialogKeyboardController = LocalSoftwareKeyboardController.current

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0E121B))
                    .testTag("tv_browser_convert_fullscreen")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 42.dp, vertical = 26.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                            Text(
                                text = "Convert Website to RNA. App",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 25.sp
                            )
                            Text(
                                text = "Packages this website as a standalone TV app with offline-saved icon and custom controls.",
                                color = Color(0xFF9E9FAF),
                                fontSize = 12.5.sp
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var cancelFocused by remember { mutableStateOf(false) }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (cancelFocused) Color(0xFF334155) else Color(0xFF1A1F2E),
                                border = BorderStroke(
                                    width = if (cancelFocused) 2.5.dp else 1.dp,
                                    color = if (cancelFocused) Color.White else Color(0x44FFFFFF)
                                ),
                                modifier = Modifier
                                    .onFocusChanged { cancelFocused = it.isFocused }
                                    .clickable(enabled = !isPackagingApp) {
                                        showConvertToRnaDialog = false
                                    }
                                    .testTag("tv_browser_convert_cancel_btn")
                            ) {
                                Text(
                                    text = "Cancel",
                                    color = if (cancelFocused) Color.White else Color(0xFFC6C5D0),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp)
                                )
                            }

                            var installFocused by remember { mutableStateOf(false) }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = when {
                                    isPackagingApp -> EmeraldSuccess.copy(alpha = 0.6f)
                                    installFocused -> Color.White
                                    else -> EmeraldSuccess
                                },
                                border = BorderStroke(
                                    width = if (installFocused) 2.5.dp else 1.dp,
                                    color = if (installFocused) CyanPrimary else EmeraldSuccess
                                ),
                                modifier = Modifier
                                    .focusRequester(installBtnFocus)
                                    .onFocusChanged { installFocused = it.isFocused }
                                    .clickable(enabled = !isPackagingApp) {
                                        val cleanUrlRaw = appUrl.trim()
                                        if (cleanUrlRaw.isBlank()) {
                                            Toast.makeText(context, "Please enter a valid website URL", Toast.LENGTH_SHORT).show()
                                            return@clickable
                                        }
                                        val normalizedUrl = if (
                                            cleanUrlRaw.startsWith("http://", ignoreCase = true) ||
                                            cleanUrlRaw.startsWith("https://", ignoreCase = true)
                                        ) {
                                            cleanUrlRaw
                                        } else {
                                            "https://$cleanUrlRaw"
                                        }
                                        val finalTitle = appName.trim().ifBlank { "Web App" }
                                        isPackagingApp = true
                                        scope.launch {
                                            val persistentIconUri = resolveWebsiteLogoToPersistentDataUri(
                                                context = context,
                                                candidateUri = selectedLogo,
                                                websiteUrl = normalizedUrl,
                                                fallbackTitle = finalTitle,
                                                capturedWebViewFavicon = currentPageFaviconBitmap
                                            )
                                            val appId = UUID.randomUUID().toString()
                                            val slug = finalTitle.lowercase()
                                                .replace("[^a-z0-9]".toRegex(), "")
                                                .take(24)
                                                .ifBlank { "webapp" }
                                            val convertedApp = RnaApp(
                                                id = appId,
                                                packageName = "com.rna.$slug.${appId.take(4)}",
                                                title = finalTitle,
                                                version = "1.0.0",
                                                sourceType = "URL",
                                                sourceUriOrContent = normalizedUrl,
                                                iconBase64 = persistentIconUri,
                                                bannerBase64 = persistentIconUri,
                                                isTvEnabled = true,
                                                isTvFavorite = pinToHomeFavorites,
                                                defaultInputMode = chosenInputMode,
                                                mouseSpeed = mouseSpeed,
                                                forceDarkMode = chosenForceDark,
                                                showLoadingBar = true,
                                                loadingBarColor = "#BAC3FF"
                                            )
                                            isPackagingApp = false
                                            showConvertToRnaDialog = false
                                            onConvertWebsiteToRnaApp(convertedApp)
                                        }
                                    }
                                    .testTag("tv_browser_convert_install_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (isPackagingApp) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF0E2419),
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Packaging...",
                                            color = Color(0xFF0E2419),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.5.sp
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.InstallMobile,
                                            contentDescription = null,
                                            tint = Color(0xFF0E2419),
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Text(
                                            text = "Install RNA. App",
                                            color = Color(0xFF0E2419),
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Name & Website URL Row (with TV keyboard support)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = appName,
                            onValueChange = { appName = it },
                            label = { Text("App Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldSuccess,
                                unfocusedBorderColor = Color(0xFF454653),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF1A2030),
                                unfocusedContainerColor = Color(0xFF141824)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(appNameFieldFocus)
                                .tvKeyboardFieldTrigger(
                                    context = context,
                                    view = dialogView,
                                    focusRequester = appNameFieldFocus,
                                    keyboardController = dialogKeyboardController,
                                    onOpenRnaKeyboard = { activeRnaKeyboardField = "NAME" }
                                )
                                .testTag("tv_browser_convert_name_input")
                        )

                        OutlinedTextField(
                            value = appUrl,
                            onValueChange = { appUrl = it },
                            label = { Text("Website URL") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = Color(0xFF454653),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF1A2030),
                                unfocusedContainerColor = Color(0xFF141824)
                            ),
                            modifier = Modifier
                                .weight(1.35f)
                                .focusRequester(appUrlFieldFocus)
                                .tvKeyboardFieldTrigger(
                                    context = context,
                                    view = dialogView,
                                    focusRequester = appUrlFieldFocus,
                                    keyboardController = dialogKeyboardController,
                                    onOpenRnaKeyboard = { activeRnaKeyboardField = "URL" }
                                )
                                .testTag("tv_browser_convert_url_input")
                        )
                    }

                    // App Behavior Options Row (D-Pad / Mouse, Force Dark Mode, Pin to Home)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        var inputModeFocused by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (inputModeFocused) CyanPrimary else Color(0xFF1B2030),
                            border = BorderStroke(
                                width = if (inputModeFocused) 2.dp else 1.dp,
                                color = if (inputModeFocused) Color.White else CyanPrimary.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { inputModeFocused = it.isFocused }
                                .clickable {
                                    chosenInputMode = if (chosenInputMode == "MOUSE") "DPAD" else "MOUSE"
                                }
                                .testTag("tv_browser_convert_input_mode_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (chosenInputMode == "MOUSE") Icons.Default.Mouse else Icons.Default.Tv,
                                    contentDescription = null,
                                    tint = if (inputModeFocused) Color(0xFF10182B) else CyanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Controls: ${if (chosenInputMode == "MOUSE") "Mouse" else "D-Pad"}",
                                    color = if (inputModeFocused) Color(0xFF10182B) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        var darkFocused by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = when {
                                darkFocused -> VioletSecondary
                                chosenForceDark -> VioletSecondary.copy(alpha = 0.25f)
                                else -> Color(0xFF1B2030)
                            },
                            border = BorderStroke(
                                width = if (darkFocused) 2.dp else 1.dp,
                                color = when {
                                    darkFocused -> Color.White
                                    chosenForceDark -> VioletSecondary
                                    else -> Color(0x33FFFFFF)
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { darkFocused = it.isFocused }
                                .clickable { chosenForceDark = !chosenForceDark }
                                .testTag("tv_browser_convert_dark_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (chosenForceDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = if (darkFocused) Color(0xFF1F1038) else VioletSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Force Dark: ${if (chosenForceDark) "ON" else "OFF"}",
                                    color = if (darkFocused) Color(0xFF1F1038) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        var pinFocused by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = when {
                                pinFocused -> EmeraldSuccess
                                pinToHomeFavorites -> EmeraldSuccess.copy(alpha = 0.22f)
                                else -> Color(0xFF1B2030)
                            },
                            border = BorderStroke(
                                width = if (pinFocused) 2.dp else 1.dp,
                                color = when {
                                    pinFocused -> Color.White
                                    pinToHomeFavorites -> EmeraldSuccess
                                    else -> Color(0x33FFFFFF)
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { pinFocused = it.isFocused }
                                .clickable { pinToHomeFavorites = !pinToHomeFavorites }
                                .testTag("tv_browser_convert_pin_home_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = if (pinFocused) Color(0xFF0E2419) else EmeraldSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pin to Home: ${if (pinToHomeFavorites) "ON" else "OFF"}",
                                    color = if (pinFocused) Color(0xFF0E2419) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    RnaLogoSelectorCard(
                        headerTitle = "App Icon (Auto-Fetched or Custom)",
                        headerSubtitle = "Saved offline inside the app container • Pick Site Icon, Upload, Preset, or Alphabet",
                        currentLogoUri = selectedLogo,
                        websiteUrlForFavicon = appUrl,
                        defaultTitleForAlphabet = appName,
                        onSelectLogoUri = { uri ->
                            userPickedCustomLogo = true
                            selectedLogo = uri
                        },
                        onPickFromGallery = {
                            runCatching {
                                logoPhotoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }.onFailure {
                                Toast.makeText(context, "Image picker unavailable on this TV — use Site Icon, Preset, or Alphabet Logo", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }

                // Built-In RNA TV Keyboard Overlay when user triggers keyboard on App Name or Website URL
                if (activeRnaKeyboardField != null) {
                    val editingName = activeRnaKeyboardField == "NAME"
                    RnaTvKeyboardOverlay(
                        visible = true,
                        currentText = if (editingName) appName else appUrl,
                        fieldHint = if (editingName) "Enter App Name..." else "Enter Website URL...",
                        showTextPreviewBar = true,
                        onTextChange = { updated ->
                            if (editingName) appName = updated else appUrl = updated
                        },
                        onCommitChar = { ch ->
                            if (editingName) appName += ch else appUrl += ch
                        },
                        onBackspace = {
                            if (editingName) {
                                if (appName.isNotEmpty()) appName = appName.dropLast(1)
                            } else {
                                if (appUrl.isNotEmpty()) appUrl = appUrl.dropLast(1)
                            }
                        },
                        onSearchOrSubmit = { activeRnaKeyboardField = null },
                        onDismiss = { activeRnaKeyboardField = null }
                    )
                }
            }
        }
    }

    // Dialog 4: Full-Screen Browser Downloads Manager + Built-In Video / Audio / Image Player
    if (showBrowserDownloadsDialog) {
        var customDownloadUrl by remember(currentUrl) { mutableStateOf("") }
        Dialog(
            onDismissRequest = {
                if (activeBrowserVideoFile != null) activeBrowserVideoFile = null
                else if (activeBrowserAudioFile != null) activeBrowserAudioFile = null
                else if (activeBrowserImageFile != null) activeBrowserImageFile = null
                else showBrowserDownloadsDialog = false
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0E17))
                    .testTag("tv_browser_downloads_fullscreen")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 36.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Browser Downloads",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 26.sp
                            )
                            if (!activeDownloadStatus.isNullOrBlank()) {
                                Text(
                                    text = activeDownloadStatus!!,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 13.sp
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    // Scan current webpage for direct image/video/media or download current URL
                                    val wv = webViewRef
                                    if (wv != null) {
                                        wv.evaluateJavascript(
                                            """
                                            (function() {
                                                var v = document.querySelector('video[src], video source[src]');
                                                if (v && v.src) return v.src;
                                                var img = document.querySelector('img[src*="http"]');
                                                if (img && img.src && img.naturalWidth >= 400) return img.src;
                                                return window.location.href;
                                            })();
                                            """.trimIndent()
                                        ) { raw ->
                                            val clean = raw?.trim()?.removeSurrounding("\"") ?: currentUrl
                                            val target = if (clean.startsWith("http")) clean else currentUrl
                                            startBrowserFileDownload(target)
                                        }
                                    } else {
                                        startBrowserFileDownload(currentUrl)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Download Page Media", color = Color.White)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        context.startActivity(
                                            Intent(context, RnaFileExplorerActivity::class.java).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                        )
                                    } catch (_: Exception) {}
                                }
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open RNA File Explorer", color = Color.White)
                            }

                            Button(
                                onClick = {
                                    showBrowserDownloadsDialog = false
                                    focusWebCanvasSafely()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanPrimary,
                                    contentColor = Color(0xFF12131A)
                                )
                            ) {
                                Text("Close", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Direct URL Download Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customDownloadUrl,
                            onValueChange = { customDownloadUrl = it },
                            placeholder = { Text("Paste direct file/video/wallpaper URL to download...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val target = customDownloadUrl.trim().ifBlank { currentUrl }
                                if (target.startsWith("http")) {
                                    startBrowserFileDownload(target)
                                    customDownloadUrl = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldSuccess,
                                contentColor = Color(0xFF0A1E14)
                            )
                        ) {
                            Text("Download URL", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (browserDownloadsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No downloaded files yet. Click any download link in the Browser or press 'Download Page Media' above.",
                                color = Color(0xFF94A3B8),
                                fontSize = 15.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(browserDownloadsList, key = { it.absolutePath }) { file ->
                                val ext = file.extension.lowercase()
                                val isVid = ext in setOf("mp4", "mkv", "webm", "mov", "m2ts", "mts", "avi", "ts", "m4v", "3gp")
                                val isAud = ext in setOf("mp3", "wav", "flac", "ogg", "m4a", "aac", "opus")
                                val isImg = ext in setOf("jpg", "jpeg", "png", "webp", "bmp", "gif")
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF151C2C),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 18.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = when {
                                                    isVid -> Icons.Default.Movie
                                                    isAud -> Icons.Default.LibraryMusic
                                                    isImg -> Icons.Default.Image
                                                    else -> Icons.Default.Download
                                                },
                                                contentDescription = null,
                                                tint = when {
                                                    isVid -> Color(0xFFF43F5E)
                                                    isAud -> Color(0xFFA855F7)
                                                    isImg -> Color(0xFF10B981)
                                                    else -> CyanPrimary
                                                },
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = file.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${file.length() / 1024} KB • ${file.parentFile?.name ?: "Downloads"}",
                                                    color = Color(0xFF94A3B8),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (isVid || isAud || isImg) {
                                                Button(
                                                    onClick = {
                                                        when {
                                                            isVid -> activeBrowserVideoFile = file
                                                            isAud -> activeBrowserAudioFile = file
                                                            isImg -> activeBrowserImageFile = file
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = CyanPrimary,
                                                        contentColor = Color(0xFF12131A)
                                                    )
                                                ) {
                                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (isImg) "View" else "Play", fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            if ((isImg || isVid) && onSetDownloadedFileAsWallpaper != null) {
                                                OutlinedButton(
                                                    onClick = {
                                                        onSetDownloadedFileAsWallpaper(file, isVid)
                                                        Toast.makeText(
                                                            context,
                                                            if (isVid) "Set ${file.name} as Live Wallpaper!" else "Set ${file.name} as Static Wallpaper!",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Wallpaper, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (isVid) "Set Live Wallpaper" else "Set Wallpaper", color = Color.White)
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    runCatching { file.delete() }
                                                    refreshBrowserDownloads()
                                                }
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // In-Browser Media Playback Overlays for Downloaded Video / Audio / Image
                activeBrowserVideoFile?.let { vFile ->
                    BuiltInVideoPlayerOverlay(
                        file = vFile,
                        onClose = { activeBrowserVideoFile = null }
                    )
                }
                activeBrowserAudioFile?.let { aFile ->
                    BuiltInAudioPlayerOverlay(
                        file = aFile,
                        onClose = { activeBrowserAudioFile = null }
                    )
                }
                activeBrowserImageFile?.let { iFile ->
                    BuiltInImageViewerOverlay(
                        file = iFile,
                        onClose = { activeBrowserImageFile = null }
                    )
                }
            }
        }
    }
}
