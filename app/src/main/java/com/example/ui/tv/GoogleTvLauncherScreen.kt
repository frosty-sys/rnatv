package com.example.ui.tv

import android.Manifest
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.tv.TvContract
import android.media.tv.TvInputInfo
import android.media.tv.TvInputManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsInputComposite
import androidx.compose.material.icons.filled.SettingsInputHdmi
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.RnaFileExplorerActivity
import com.example.RnaHomeInterceptorService
import com.example.RnaTvInputMethodService
import com.example.data.TvAppCustomEntity
import com.example.data.model.RnaApp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VioletSecondary
import com.example.ui.wizard.decodeDataUriToImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt

const val KEY_SPLASH_SCREEN_ENABLED = "tv_splash_screen_enabled"
const val KEY_YOUTUBE_SEARCH_BAR_ENABLED = "show_youtube_search_bar_on_home"
const val RNA_FILE_EXPLORER_PACKAGE_NAME = "com.rna.fileexplorer"
const val RNA_FILE_EXPLORER_UNIQUE_KEY = "builtin_rna_file_explorer"

object ControlPanelReturnState {
    @Volatile
    var returnToFunctionsFromStudio: Boolean = false
}

/**
 * Crash-proof FocusRequester extension that never throws IllegalStateException
 * when a target node is not yet composed or scrolled off-screen.
 */
internal fun FocusRequester.safeRequestFocus(): Boolean {
    return try {
        requestFocus()
        true
    } catch (_: Throwable) {
        false
    }
}

/**
 * Pre-decoded Native App Model & Singleton RAM Cache so all Home Screen and All Apps
 * icons and 16:9 Android TV banners remain permanently in memory and render on Frame 1.
 */
internal data class LoadedNativeApp(
    val packageName: String,
    val label: String,
    val drawable: Drawable?,
    val bitmap: ImageBitmap?,
    val systemLastTimeUsed: Long,
    val bannerDrawable: Drawable? = null,
    val bannerBitmap: ImageBitmap? = null
)

internal object TvAppRamCache {
    @Volatile
    var loadedNativeApps: List<LoadedNativeApp> = emptyList()
    val iconBitmaps = ConcurrentHashMap<String, ImageBitmap>()
    val bannerBitmaps = ConcurrentHashMap<String, ImageBitmap>()
}

/**
 * Lightweight disk cache for Favorite Apps metadata & PNG icons so Favorite Apps
 * hydrate synchronously on Frame 1 (just as fast as built-in RNA AI & File Explorer)
 * before Android's full PackageManager scan finishes.
 */
internal object FavoriteAppsDiskCache {
    private const val PREFS_NAME = "rna_fav_apps_fast_cache_v1"
    private const val KEY_META_JSON = "fav_apps_meta_json"

    private fun iconDir(context: Context): File =
        File(context.filesDir, "fav_icons_cache").apply { if (!exists()) mkdirs() }

    private fun safeFileName(pkg: String): String =
        pkg.replace(Regex("[^a-zA-Z0-9._-]"), "_")

    fun hydrateSynchronously(context: Context): List<LoadedNativeApp> {
        if (TvAppRamCache.loadedNativeApps.isNotEmpty()) return TvAppRamCache.loadedNativeApps
        return runCatching {
            val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_META_JSON, null) ?: return emptyList()
            val arr = JSONArray(raw)
            val dir = iconDir(context)
            val result = mutableListOf<LoadedNativeApp>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val pkg = obj.optString("pkg", "").trim()
                val label = obj.optString("label", "").trim()
                if (pkg.isEmpty() || label.isEmpty()) continue
                val lastUsed = obj.optLong("lastUsed", 0L)
                val safeName = safeFileName(pkg)

                val iconBmp = TvAppRamCache.iconBitmaps[pkg] ?: run {
                    val f = File(dir, "$safeName.png")
                    if (f.exists() && f.length() > 64L) {
                        android.graphics.BitmapFactory.decodeFile(f.absolutePath)
                            ?.asImageBitmap()
                            ?.also { TvAppRamCache.iconBitmaps[pkg] = it }
                    } else null
                }
                val bannerBmp = TvAppRamCache.bannerBitmaps[pkg] ?: run {
                    val bf = File(dir, "${safeName}_banner.png")
                    if (bf.exists() && bf.length() > 64L) {
                        android.graphics.BitmapFactory.decodeFile(bf.absolutePath)
                            ?.asImageBitmap()
                            ?.also { TvAppRamCache.bannerBitmaps[pkg] = it }
                    } else null
                }
                result.add(
                    LoadedNativeApp(
                        packageName = pkg,
                        label = label,
                        drawable = null,
                        bitmap = iconBmp,
                        systemLastTimeUsed = lastUsed,
                        bannerDrawable = null,
                        bannerBitmap = bannerBmp
                    )
                )
            }
            if (result.isNotEmpty()) {
                TvAppRamCache.loadedNativeApps = result
            }
            result
        }.getOrDefault(emptyList())
    }

    fun persistFavoritesSnapshot(
        context: Context,
        nativeApps: List<LoadedNativeApp>,
        favoritePackages: Set<String>
    ) {
        runCatching {
            val targets = nativeApps.filterIndexed { index, app ->
                app.packageName in favoritePackages || (favoritePackages.isEmpty() && index < 10)
            }.take(24)
            val dir = iconDir(context)
            val arr = JSONArray()
            targets.forEach { app ->
                arr.put(
                    JSONObject().apply {
                        put("pkg", app.packageName)
                        put("label", app.label)
                        put("lastUsed", app.systemLastTimeUsed)
                    }
                )
                val safeName = safeFileName(app.packageName)
                val iconFile = File(dir, "$safeName.png")
                if (!iconFile.exists() || iconFile.length() < 64L) {
                    app.bitmap?.let { imgBmp ->
                        runCatching {
                            val androidBmp = imgBmp.asAndroidBitmap()
                            FileOutputStream(iconFile).use { fos ->
                                androidBmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 92, fos)
                            }
                        }
                    }
                }
                val bannerFile = File(dir, "${safeName}_banner.png")
                if (!bannerFile.exists() || bannerFile.length() < 64L) {
                    app.bannerBitmap?.let { bannerImg ->
                        runCatching {
                            val androidBmp = bannerImg.asAndroidBitmap()
                            FileOutputStream(bannerFile).use { fos ->
                                androidBmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 88, fos)
                            }
                        }
                    }
                }
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_META_JSON, arr.toString())
                .apply()
        }
    }
}

/**
 * Renders an Android [Drawable] (including [android.graphics.drawable.AdaptiveIconDrawable])
 * as a full-bleed square [ImageBitmap] without the system circular/squircle adaptive icon mask,
 * so any of the 19 custom app icon shapes (Star, Hexagon, Diamond, Heart, Shield, etc.)
 * clips the icon cleanly edge-to-edge without a circle border inside the shape.
 */
internal fun renderFullBleedAppIconBitmap(drawable: Drawable, sizePx: Int = 144): ImageBitmap {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
        drawable is android.graphics.drawable.AdaptiveIconDrawable
    ) {
        val bmp = android.graphics.Bitmap.createBitmap(
            sizePx,
            sizePx,
            android.graphics.Bitmap.Config.ARGB_8888
        )
        val canvas = android.graphics.Canvas(bmp)
        val inset = (sizePx * 0.18f).toInt()
        drawable.background?.let { bg ->
            bg.setBounds(-inset, -inset, sizePx + inset, sizePx + inset)
            bg.draw(canvas)
        }
        drawable.foreground?.let { fg ->
            fg.setBounds(-inset, -inset, sizePx + inset, sizePx + inset)
            fg.draw(canvas)
        }
        return bmp.asImageBitmap()
    }
    return drawable.toBitmap(sizePx, sizePx).asImageBitmap()
}

internal object TvPerformanceMode {
    @Volatile
    var isEnabled: Boolean = false
}

/**
 * Hardware D-Pad + Menu Button + Mouse/Touch Press-and-Hold modifier that works reliably
 * even when Android Accessibility Services are active.
 *
 * - Holding OK / Enter / Pointer for [holdDurationMs] fires [onLongClick] immediately while
 *   still holding and suppresses [onClick] when the button is released.
 * - Pressing the Menu / Info / X / Y button on a TV remote or controller immediately triggers [onLongClick].
 */
internal fun Modifier.tvPressAndHoldClickable(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    holdDurationMs: Long = 360L
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    var isKeyDown by remember { mutableStateOf(false) }
    var longPressTriggered by remember { mutableStateOf(false) }
    var holdJob by remember { mutableStateOf<Job?>(null) }

    this
        .onPreviewKeyEvent { keyEvent ->
            val native = keyEvent.nativeKeyEvent
            val isMenuKey = native.keyCode == KeyEvent.KEYCODE_MENU ||
                native.keyCode == KeyEvent.KEYCODE_BUTTON_X ||
                native.keyCode == KeyEvent.KEYCODE_BUTTON_Y ||
                native.keyCode == KeyEvent.KEYCODE_INFO ||
                native.keyCode == KeyEvent.KEYCODE_BOOKMARK
            if (isMenuKey) {
                if (native.action == KeyEvent.ACTION_DOWN) {
                    currentOnLongClick()
                }
                return@onPreviewKeyEvent true
            }

            val isOkKey = native.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                native.keyCode == KeyEvent.KEYCODE_ENTER ||
                native.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
                native.keyCode == KeyEvent.KEYCODE_BUTTON_A
            if (!isOkKey) {
                false
            } else {
                when (native.action) {
                    KeyEvent.ACTION_DOWN -> {
                        if (!isKeyDown) {
                            isKeyDown = true
                            longPressTriggered = false
                            holdJob?.cancel()
                            holdJob = scope.launch {
                                delay(holdDurationMs)
                                if (isKeyDown && !longPressTriggered) {
                                    longPressTriggered = true
                                    currentOnLongClick()
                                }
                            }
                        } else if (!longPressTriggered && (native.eventTime - native.downTime) >= holdDurationMs) {
                            longPressTriggered = true
                            holdJob?.cancel()
                            currentOnLongClick()
                        }
                        true
                    }
                    KeyEvent.ACTION_UP -> {
                        val wasLong = longPressTriggered
                        isKeyDown = false
                        longPressTriggered = false
                        holdJob?.cancel()
                        if (!wasLong) {
                            currentOnClick()
                        }
                        true
                    }
                    else -> false
                }
            }
        }
        .pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                var pointerLongTriggered = false
                val pJob = scope.launch {
                    delay(holdDurationMs)
                    pointerLongTriggered = true
                    currentOnLongClick()
                }
                val up = waitForUpOrCancellation()
                pJob.cancel()
                if (up != null && !pointerLongTriggered) {
                    currentOnClick()
                }
            }
        }
        .focusable()
}

data class UnifiedLauncherApp(
    val uniqueKey: String,
    val title: String,
    val packageName: String,
    val isRnaWebApp: Boolean,
    val rnaApp: RnaApp? = null,
    val customLogoUri: String? = null,
    val nativeIcon: Drawable? = null,
    val cachedBitmap: ImageBitmap? = null,
    val nativeBanner: Drawable? = null,
    val cachedBannerBitmap: ImageBitmap? = null,
    val isFavorite: Boolean = false,
    val favoriteOrder: Int = 100,
    val allAppsOrder: Int = 100,
    val lastUsedTimestamp: Long = 0L
)

/**
 * All 43 user-uploaded 4K wallpapers bundled in APK assets for instant 60fps rendering
 * (includes wp_01 default + the 6 newly added wallpapers wp_38..wp_43 + wp_02..wp_37).
 */
val PRESET_WALLPAPER_ASSET_URIS: List<String> = (
    listOf(1) + (38..43) + (2..37)
).map { index ->
    String.format(Locale.US, "file:///android_asset/wallpapers/wp_%02d.jpg", index)
}

@Composable
fun GoogleTvLauncherScreen(
    rnaApps: List<RnaApp>,
    tvCustomizations: List<TvAppCustomEntity>,
    showRecentAppsRow: Boolean = false,
    onToggleRecentAppsRow: (Boolean) -> Unit = {},
    singleRowHomeApps: Boolean = true,
    onToggleSingleRowHomeApps: (Boolean) -> Unit = {},
    recentAppTimestamps: Map<String, Long> = emptyMap(),
    onRecordAppLaunched: (packageName: String, rnaAppId: String?) -> Unit = { _, _ -> },
    onRemoveRecentApp: (packageName: String) -> Unit = {},
    onClearRecentApps: () -> Unit = {},
    onLaunchRnaApp: (RnaApp) -> Unit,
    onLaunchDirectUrl: (String, String) -> Unit,
    onInstallRnaUri: (Uri) -> Unit = {},
    onUpdateRnaApp: (RnaApp) -> Unit,
    onUninstallRnaApp: (RnaApp) -> Unit = {},
    onExportRnaApp: (RnaApp) -> Unit = {},
    onSaveTvCustomization: (TvAppCustomEntity) -> Unit,
    onOpenRnaStudio: (initialTab: Int) -> Unit,
    onOpenCreateWizard: () -> Unit,
    onConvertWebsiteToRnaApp: (RnaApp) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE) }

    var tvTab by remember { mutableIntStateOf(0) } // 0: Home, 1: Apps, 2: Browser
    var browserTargetUrl by remember { mutableStateOf("https://www.google.com") }
    var isBrowserFullScreen by remember { mutableStateOf(false) }
    var isBrowserTopBarAutoHidden by remember { mutableStateOf(true) }

    // TV Quick Actions Pro-style Remote Button Remapper, Triggers & Macros, and quick overlays
    var showKeyRemapperScreen by remember { mutableStateOf(false) }
    var showTriggersAndMacrosScreen by remember { mutableStateOf(false) }
    var showRecentAppsOverlayInLauncher by remember { mutableStateOf(false) }
    var showQuickAppsTaskbarInLauncher by remember { mutableStateOf(false) }
    var openRnaAtBootEnabled by remember {
        mutableStateOf(prefs.getBoolean(KEY_OPEN_RNA_AT_BOOT, false))
    }
    var showYoutubeSearchBar by remember {
        mutableStateOf(prefs.getBoolean(KEY_YOUTUBE_SEARCH_BAR_ENABLED, false))
    }
    var youtubeSearchQuery by remember { mutableStateOf("") }
    var showYoutubeHomeKeyboard by remember { mutableStateOf(false) }
    var isYoutubeInlineEditing by remember { mutableStateOf(false) }
    val youtubeSearchFocusRequester = remember { FocusRequester() }
    val youtubeSearchSubmitBtnFocusRequester = remember { FocusRequester() }
    val homeKeyboardController = LocalSoftwareKeyboardController.current

    // Control Panel Tab Memory & Back-to-Functions Return Tracking
    var controlPanelTabToOpen by remember { mutableStateOf(ControlPanelTab.HOME_LAYOUT) }
    var controlPanelLastTab by remember { mutableStateOf(ControlPanelTab.HOME_LAYOUT) }
    var returnToControlPanelTabOnClose by remember { mutableStateOf<ControlPanelTab?>(null) }

    // Per-tab isolated FocusRequesters so cross-tab FocusRequester collisions never happen
    val homeTabFocusRequester = remember { FocusRequester() }
    val homeFirstItemFocusRequester = remember { FocusRequester() }
    val appsFirstItemFocusRequester = remember { FocusRequester() }
    val browserFirstToolbarFocusRequester = remember { FocusRequester() }

    val activeDownFocusRequester = when (tvTab) {
        0 -> homeFirstItemFocusRequester
        1 -> appsFirstItemFocusRequester
        else -> browserFirstToolbarFocusRequester
    }

    var currentTime by remember {
        mutableStateOf(SimpleDateFormat("h:mm", Locale.getDefault()).format(Date()))
    }
    var currentDateStr by remember {
        mutableStateOf(SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()))
    }

    // Multi-User TV Profiles State
    var profiles by remember { mutableStateOf(loadTvProfiles(prefs)) }
    var activeProfileId by remember {
        val askOnBoot = prefs.getBoolean("ask_who_is_watching_on_boot", false)
        val initialId = if (!askOnBoot) {
            val mainProf = profiles.firstOrNull {
                it.id == "default" || it.id == "main" || it.name.equals("Main", ignoreCase = true)
            } ?: profiles.first()
            setActiveTvProfileId(prefs, mainProf.id)
            mainProf.id
        } else {
            getActiveTvProfileId(prefs)
        }
        mutableStateOf(initialId)
    }
    val activeProfile = remember(profiles, activeProfileId) {
        profiles.find { it.id == activeProfileId } ?: profiles.first()
    }
    var showProfileDialog by remember { mutableStateOf(false) }
    var openProfileDialogInCreateMode by remember { mutableStateOf(false) }
    var showDevicesDrawer by remember { mutableStateOf(false) }

    // Active wallpaper URI per profile (defaults to the first uploaded 4K preset wallpaper wp_01.jpg)
    var activeWallpaperUri by remember(activeProfile.id) {
        mutableStateOf(
            activeProfile.wallpaperUri.ifBlank {
                prefs.getString("wallpaper_uri", null) ?: PRESET_WALLPAPER_ASSET_URIS.first()
            }
        )
    }
    var showWallpaperAppliedBanner by remember { mutableStateOf(false) }
    val wallpaperScaleAnim = remember { Animatable(1f) }

    var isAccessibilityEnabled by remember {
        mutableStateOf(RnaHomeInterceptorService.isAccessibilityServiceEnabled(context))
    }
    var showAccessibilityBanner by remember { mutableStateOf(!isAccessibilityEnabled) }

    // Dialog, Context Menu, Move Mode & Custom Input Selector States
    var showSearchUtilityDialog by remember { mutableStateOf(false) }
    var showTvPowerHubDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showAddFavoritesDialog by remember { mutableStateOf(false) }
    var showAmbientScreensaver by remember { mutableStateOf(false) }
    var showHdmiInputSelectorDialog by remember { mutableStateOf(false) }
    var showUpdatesDrawer by remember { mutableStateOf(false) }
    var showVoiceAssistantOverlay by remember { mutableStateOf(false) }
    var initialWakeWordCommand by remember { mutableStateOf<String?>(null) }

    // RNAAI Full-Screen App & Control Panel Toggle State
    var rnaAiAppEnabled by remember {
        mutableStateOf(prefs.getBoolean(KEY_RNAAI_ENABLED_IN_CONTROL_PANEL, true))
    }
    var showRnaAiFullScreenApp by remember { mutableStateOf(false) }
    var rnaAiInitialSection by remember { mutableIntStateOf(0) }

    // EdVanced 1.1 Pro Voice Assistant & Performance Mode Settings ("Replace Google Assistant" & "Performance Mode" disabled by default)
    var replaceGoogleAssistantEnabled by remember {
        mutableStateOf(prefs.getBoolean(RnaHomeInterceptorService.KEY_REPLACE_GOOGLE_ASSISTANT, false))
    }
    var performanceModeEnabled by remember {
        mutableStateOf(
            prefs.getBoolean("performance_mode_enabled", false) ||
                prefs.getBoolean("control_performance_mode", false)
        ).also {
            TvPerformanceMode.isEnabled = it.value
        }
    }
    var browserFastPerformanceMode by remember {
        mutableStateOf(prefs.getBoolean("browser_fast_performance_mode", true))
    }
    var splashScreenEnabled by remember {
        mutableStateOf(
            prefs.getBoolean(KEY_SPLASH_SCREEN_ENABLED, true) &&
                prefs.getBoolean("splash_screen_enabled", true)
        )
    }
    var aiTtsEnabled by remember {
        mutableStateOf(prefs.getBoolean("ai_tts_enabled", true))
    }

    // New Wallpaper (Static & Live Video), 19 App Icon Shapes, Idle Timer, Key Links & File Explorer States
    var wallpaperModeType by remember(activeProfile.id) {
        mutableStateOf(
            activeProfile.wallpaperModeType.ifBlank {
                prefs.getString("wallpaper_mode_type", "STATIC") ?: "STATIC"
            }
        )
    }
    var activeLiveWallpaperUri by remember(activeProfile.id) {
        mutableStateOf(
            if (activeProfile.wallpaperModeType.equals("LIVE", ignoreCase = true) && activeProfile.liveWallpaperUri.isNotBlank()) {
                activeProfile.liveWallpaperUri
            } else {
                prefs.getString("active_live_wallpaper_uri", BUNDLED_LIVE_WALLPAPERS.first().uri)
                    ?: BUNDLED_LIVE_WALLPAPERS.first().uri
            }
        )
    }
    var liveWallpaperResolution by remember {
        mutableStateOf(
            LiveWallpaperResolution.fromId(prefs.getString("control_live_wallpaper_res", "720p"))
        )
    }
    var liveWallpaperPreviewEnabled by remember {
        mutableStateOf(
            prefs.getBoolean(
                KEY_LIVE_WALLPAPER_PREVIEW_ENABLED,
                prefs.getBoolean("control_live_wallpaper_preview_enabled", false)
            )
        )
    }
    var selectedAppIconShape by remember(activeProfile.id) {
        mutableStateOf(
            TvAppIconShape.fromId(
                activeProfile.appIconShape.ifBlank {
                    prefs.getString("control_app_icon_shape", TvAppIconShape.CIRCLE.id)
                }
            )
        )
    }
    var ambientIdleMinutes by remember(activeProfile.id) {
        mutableIntStateOf(
            if (prefs.contains("control_ambient_idle_minutes")) {
                activeProfile.ambientIdleMinutes
            } else {
                5
            }
        )
    }
    var usbBootAutoBackEnabled by remember {
        mutableStateOf(prefs.getBoolean("control_usb_boot_auto_back", false))
    }
    var askWhoIsWatchingOnBoot by remember {
        mutableStateOf(prefs.getBoolean("ask_who_is_watching_on_boot", false))
    }
    var showWhoIsWatchingBootOverlay by remember {
        mutableStateOf(prefs.getBoolean("ask_who_is_watching_on_boot", false))
    }
    var showFirstRunSetupWizard by remember {
        mutableStateOf(!prefs.getBoolean("tv_first_run_setup_completed_v1", false))
    }
    var showKeyLinksEditor by remember { mutableStateOf(false) }
    var fileExplorerPickerMode by remember { mutableStateOf<FileExplorerPickerMode?>(null) }
    var lastUserInteractionRealtimeMs by remember {
        mutableStateOf(android.os.SystemClock.elapsedRealtime())
    }

    // Track whether the RNA Home Launcher Activity is actively resumed in the foreground
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var isLauncherActivityResumed by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    isLauncherActivityResumed = true
                    lastUserInteractionRealtimeMs = android.os.SystemClock.elapsedRealtime()
                }
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    isLauncherActivityResumed = false
                    showAmbientScreensaver = false
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var selectedTvInputSource by remember {
        mutableStateOf(prefs.getString("selected_tv_input_source", "HDMI 1") ?: "HDMI 1")
    }

    var contextMenuApp by remember { mutableStateOf<UnifiedLauncherApp?>(null) }
    var appDetailsTarget by remember { mutableStateOf<UnifiedLauncherApp?>(null) }
    var customizingApp by remember { mutableStateOf<UnifiedLauncherApp?>(null) }
    var movingAppKey by remember { mutableStateOf<String?>(null) }

    var nightWarmthIntensity by remember {
        mutableFloatStateOf(prefs.getFloat("night_warmth_intensity", 0f))
    }
    var sleepTimerRemainingSeconds by remember { mutableIntStateOf(0) }

    fun updateActiveProfileFields(transform: (TvUserProfile) -> TvUserProfile) {
        val updated = profiles.map { if (it.id == activeProfile.id) transform(it) else it }
        profiles = updated
        saveTvProfiles(prefs, updated)
    }

    fun switchActiveUserProfile(chosen: TvUserProfile) {
        activeProfileId = chosen.id
        setActiveTvProfileId(prefs, chosen.id)
        if (chosen.wallpaperModeType.equals("LIVE", ignoreCase = true) && chosen.liveWallpaperUri.isNotBlank()) {
            wallpaperModeType = "LIVE"
            activeLiveWallpaperUri = chosen.liveWallpaperUri
            prefs.edit()
                .putString("wallpaper_mode_type", "LIVE")
                .putString("active_live_wallpaper_uri", chosen.liveWallpaperUri)
                .commit()
        } else {
            wallpaperModeType = "STATIC"
            val staticWp = chosen.wallpaperUri.ifBlank { PRESET_WALLPAPER_ASSET_URIS.first() }
            activeWallpaperUri = staticWp
            prefs.edit()
                .putString("wallpaper_mode_type", "STATIC")
                .putString("wallpaper_uri", staticWp)
                .putString("selected_tv_wallpaper", staticWp)
                .commit()
        }
        selectedAppIconShape = TvAppIconShape.fromId(chosen.appIconShape)
        ambientIdleMinutes = chosen.ambientIdleMinutes
        onToggleSingleRowHomeApps(chosen.singleRowHomeApps)
        onToggleRecentAppsRow(chosen.showRecentAppsRow)
        prefs.edit()
            .putString("control_app_icon_shape", selectedAppIconShape.id)
            .putInt("control_ambient_idle_minutes", ambientIdleMinutes)
            .commit()
    }

    fun applyWallpaperWithAnimation(newUri: String, targetProfileName: String? = null) {
        val lowerUri = newUri.lowercase(Locale.ROOT)
        val isLive = lowerUri.endsWith(".mp4") ||
            lowerUri.endsWith(".mkv") ||
            lowerUri.endsWith(".mov") ||
            lowerUri.endsWith(".webm") ||
            lowerUri.endsWith(".m4v") ||
            lowerUri.endsWith(".3gp") ||
            lowerUri.endsWith(".ts") ||
            lowerUri.endsWith(".avi") ||
            newUri.startsWith("asset:///live_wallpapers/", ignoreCase = true) ||
            newUri.startsWith("live_wp_", ignoreCase = true)
        val rawNormalized = when {
            isLive && !newUri.contains("://") && !newUri.startsWith("/") -> "asset:///live_wallpapers/$newUri"
            !isLive && !newUri.contains("://") && !newUri.startsWith("/") -> "file:///android_asset/wallpapers/$newUri"
            newUri.startsWith("/") -> "file://$newUri"
            else -> newUri
        }
        val normalizedUri: String = rawNormalized
        if (isLive) {
            activeLiveWallpaperUri = normalizedUri
            wallpaperModeType = "LIVE"
            prefs.edit()
                .putString("active_live_wallpaper_uri", normalizedUri)
                .putString("wallpaper_mode_type", "LIVE")
                .commit()
        } else {
            activeWallpaperUri = normalizedUri
            wallpaperModeType = "STATIC"
            prefs.edit()
                .putString("wallpaper_uri", normalizedUri)
                .putString("selected_tv_wallpaper", normalizedUri)
                .putString("wallpaper_mode_type", "STATIC")
                .commit()
        }
        val targetProfId = if (!targetProfileName.isNullOrBlank()) {
            profiles.firstOrNull { it.name.equals(targetProfileName, ignoreCase = true) }?.id ?: activeProfile.id
        } else {
            activeProfile.id
        }
        val updatedProfiles = profiles.map { prof ->
            if (prof.id == targetProfId) {
                if (isLive) {
                    prof.copy(
                        wallpaperModeType = "LIVE",
                        liveWallpaperUri = normalizedUri
                    )
                } else {
                    prof.copy(
                        wallpaperUri = normalizedUri,
                        wallpaperModeType = "STATIC"
                    )
                }
            } else {
                prof
            }
        }
        profiles = updatedProfiles
        saveTvProfiles(prefs, updatedProfiles)
        scope.launch {
            if (isLive &&
                !rawNormalized.startsWith("asset:///") &&
                !BUNDLED_LIVE_WALLPAPER_FILES.any { rawNormalized.endsWith(it, ignoreCase = true) }
            ) {
                val copiedFile = copyUserLiveWallpaperToInternalDir(context, rawNormalized)
                if (copiedFile != null) {
                    val persistedUri = "file://${copiedFile.absolutePath}"
                    activeLiveWallpaperUri = persistedUri
                    prefs.edit().putString("active_live_wallpaper_uri", persistedUri).commit()
                    val persistedProfiles = profiles.map { prof ->
                        if (prof.id == targetProfId) prof.copy(liveWallpaperUri = persistedUri) else prof
                    }
                    profiles = persistedProfiles
                    saveTvProfiles(prefs, persistedProfiles)
                }
            }
            showWallpaperAppliedBanner = true
            if (performanceModeEnabled || isLive) {
                wallpaperScaleAnim.snapTo(1.0f)
            } else {
                wallpaperScaleAnim.snapTo(1.05f)
                wallpaperScaleAnim.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                )
            }
            delay(1600L)
            showWallpaperAppliedBanner = false
        }
    }

    val isAgentActive by RnaAutonomousAgentController.isAgentModeActive.collectAsState()
    val isAnyModalOrSubScreenActive = showSearchUtilityDialog ||
        showTvPowerHubDialog ||
        showWallpaperDialog ||
        showAddFavoritesDialog ||
        showHdmiInputSelectorDialog ||
        showUpdatesDrawer ||
        showDevicesDrawer ||
        showVoiceAssistantOverlay ||
        showRnaAiFullScreenApp ||
        showKeyRemapperScreen ||
        showTriggersAndMacrosScreen ||
        showKeyLinksEditor ||
        showProfileDialog ||
        showFirstRunSetupWizard ||
        showWhoIsWatchingBootOverlay ||
        fileExplorerPickerMode != null ||
        contextMenuApp != null ||
        appDetailsTarget != null ||
        customizingApp != null

    // 4K Ambient Mode Auto-Idle Timer (default 5 minutes; only counts when RNA Home/Apps tab is actively open and resumed)
    LaunchedEffect(
        ambientIdleMinutes,
        showAmbientScreensaver,
        isLauncherActivityResumed,
        tvTab,
        isAnyModalOrSubScreenActive,
        isAgentActive
    ) {
        if (ambientIdleMinutes <= 0 ||
            showAmbientScreensaver ||
            !isLauncherActivityResumed ||
            (tvTab != 0 && tvTab != 1) ||
            isAnyModalOrSubScreenActive ||
            isAgentActive
        ) {
            return@LaunchedEffect
        }
        lastUserInteractionRealtimeMs = android.os.SystemClock.elapsedRealtime()
        while (ambientIdleMinutes > 0 &&
            !showAmbientScreensaver &&
            isLauncherActivityResumed &&
            (tvTab == 0 || tvTab == 1) &&
            !isAnyModalOrSubScreenActive &&
            !isAgentActive
        ) {
            delay(3_000L)
            val idleElapsedMs = android.os.SystemClock.elapsedRealtime() - lastUserInteractionRealtimeMs
            if (idleElapsedMs >= ambientIdleMinutes * 60_000L) {
                showAmbientScreensaver = true
            }
        }
    }

    fun reopenControlPanelIfNeeded() {
        val tabToReturn = returnToControlPanelTabOnClose
        if (tabToReturn != null) {
            returnToControlPanelTabOnClose = null
            controlPanelTabToOpen = tabToReturn
            showTvPowerHubDialog = true
        }
    }

    fun submitHomeYoutubeSearch(rawQuery: String = youtubeSearchQuery) {
        val cleaned = rawQuery.trim()
        showYoutubeHomeKeyboard = false
        isYoutubeInlineEditing = false
        homeKeyboardController?.hide()
        launchYoutubeSearch(
            context = context,
            rawQuery = cleaned,
            onFallbackOpenInBrowser = { ytUrl ->
                browserTargetUrl = ytUrl
                isBrowserTopBarAutoHidden = true
                tvTab = 2
            }
        )
        if (cleaned.isNotEmpty()) {
            youtubeSearchQuery = ""
        }
    }

    // Never let Back button exit to the stock Android TV home screen!
    BackHandler(enabled = true) {
        lastUserInteractionRealtimeMs = android.os.SystemClock.elapsedRealtime()
        when {
            showYoutubeHomeKeyboard || isYoutubeInlineEditing -> {
                showYoutubeHomeKeyboard = false
                isYoutubeInlineEditing = false
                homeKeyboardController?.hide()
                youtubeSearchFocusRequester.safeRequestFocus()
            }
            fileExplorerPickerMode != null -> fileExplorerPickerMode = null
            showKeyLinksEditor -> {
                showKeyLinksEditor = false
                reopenControlPanelIfNeeded()
            }
            showTriggersAndMacrosScreen -> {
                showTriggersAndMacrosScreen = false
                reopenControlPanelIfNeeded()
            }
            showKeyRemapperScreen -> {
                showKeyRemapperScreen = false
                reopenControlPanelIfNeeded()
            }
            showRecentAppsOverlayInLauncher -> showRecentAppsOverlayInLauncher = false
            showQuickAppsTaskbarInLauncher -> showQuickAppsTaskbarInLauncher = false
            showRnaAiFullScreenApp -> {
                showRnaAiFullScreenApp = false
                reopenControlPanelIfNeeded()
            }
            showDevicesDrawer -> {
                showDevicesDrawer = false
                reopenControlPanelIfNeeded()
            }
            showUpdatesDrawer -> {
                showUpdatesDrawer = false
                reopenControlPanelIfNeeded()
            }
            movingAppKey != null -> {
                movingAppKey = null
                Toast.makeText(context, "App position saved", Toast.LENGTH_SHORT).show()
            }
            showAmbientScreensaver -> {
                showAmbientScreensaver = false
                reopenControlPanelIfNeeded()
            }
            tvTab != 0 -> {
                isBrowserFullScreen = false
                isBrowserTopBarAutoHidden = false
                tvTab = 0
            }
            // When already on Home (tvTab == 0), consume Back so it stays on RNA. TV!
        }
    }

    // Low-frequency clock updater (updates every 15s so the launcher does NOT recompose every 1 second)
    LaunchedEffect(Unit) {
        if (ControlPanelReturnState.returnToFunctionsFromStudio) {
            ControlPanelReturnState.returnToFunctionsFromStudio = false
            controlPanelTabToOpen = ControlPanelTab.FUNCTIONS
            controlPanelLastTab = ControlPanelTab.FUNCTIONS
            showTvPowerHubDialog = true
        }
        RnaAutonomousAgentController.applyCustomizationSettings(loadRnaAiSettings(prefs))
        val pendingBrowserUrl = prefs.getString("pending_external_browser_url", null)
        if (!pendingBrowserUrl.isNullOrBlank()) {
            prefs.edit().remove("pending_external_browser_url").apply()
            browserTargetUrl = pendingBrowserUrl
            isBrowserTopBarAutoHidden = true
            tvTab = 2
        }
        val timeFormat = SimpleDateFormat("h:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        while (true) {
            val now = Date()
            val nextTime = timeFormat.format(now)
            val nextDate = dateFormat.format(now)
            if (nextTime != currentTime) currentTime = nextTime
            if (nextDate != currentDateStr) currentDateStr = nextDate
            isAccessibilityEnabled = RnaHomeInterceptorService.isAccessibilityServiceEnabled(context)
            val extUrl = prefs.getString("pending_external_browser_url", null)
            if (!extUrl.isNullOrBlank()) {
                prefs.edit().remove("pending_external_browser_url").apply()
                browserTargetUrl = extUrl
                isBrowserTopBarAutoHidden = true
                tvTab = 2
            }
            delay(15_000L)
        }
    }

    // Sleep Timer Countdown Engine
    LaunchedEffect(sleepTimerRemainingSeconds > 0) {
        while (sleepTimerRemainingSeconds > 0) {
            delay(1_000L)
            sleepTimerRemainingSeconds--
            if (sleepTimerRemainingSeconds == 0) {
                nightWarmthIntensity = 0.72f
                showAmbientScreensaver = true
                try {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
                    audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE))
                } catch (_: Exception) {
                }
                Toast.makeText(context, "RNA. TV Sleep Timer completed — Media paused & screen dimmed", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Pre-warm singleton TTS engine at launcher startup so first voice response has zero delay
    LaunchedEffect(Unit) {
        RnaTtsController.preWarm(context)
    }

    // Initialize nativeApps synchronously from RAM or Disk Cache so Favorite Apps render on Frame 1
    var nativeApps by remember {
        mutableStateOf(
            TvAppRamCache.loadedNativeApps.ifEmpty {
                FavoriteAppsDiskCache.hydrateSynchronously(context)
            }
        )
    }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val usageMap = try {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                val now = System.currentTimeMillis()
                usm?.queryAndAggregateUsageStats(now - 7L * 24 * 3600 * 1000L, now)
                    ?.mapValues { it.value.lastTimeUsed }
                    ?: emptyMap()
            } catch (_: Exception) {
                emptyMap()
            }

            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val leanbackIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            }
            val resolved = (pm.queryIntentActivities(mainIntent, 0) + pm.queryIntentActivities(leanbackIntent, 0))
                .distinctBy { it.activityInfo.packageName }
                .filter {
                    it.activityInfo.packageName != context.packageName
                }

            val loaded = resolved.mapNotNull { info ->
                try {
                    val pkg = info.activityInfo.packageName
                    val label = info.loadLabel(pm).toString()
                    val icon = info.loadIcon(pm)
                    val cachedBmp = TvAppRamCache.iconBitmaps[pkg] ?: try {
                        renderFullBleedAppIconBitmap(icon, 144).also { decoded ->
                            TvAppRamCache.iconBitmaps[pkg] = decoded
                        }
                    } catch (_: Exception) {
                        null
                    }
                    val bannerDrawable = runCatching {
                        info.activityInfo?.loadBanner(pm)
                            ?: info.activityInfo?.applicationInfo?.loadBanner(pm)
                    }.getOrNull()
                    val cachedBannerBmp = if (bannerDrawable != null) {
                        TvAppRamCache.bannerBitmaps[pkg] ?: runCatching {
                            bannerDrawable.toBitmap(320, 180).asImageBitmap().also { decoded ->
                                TvAppRamCache.bannerBitmaps[pkg] = decoded
                            }
                        }.getOrNull()
                    } else {
                        null
                    }
                    val lastUsed = usageMap[pkg] ?: 0L
                    LoadedNativeApp(pkg, label, icon, cachedBmp, lastUsed, bannerDrawable, cachedBannerBmp)
                } catch (_: Exception) {
                    null
                }
            }
            TvAppRamCache.loadedNativeApps = loaded
            nativeApps = loaded

            val favPkgs = tvCustomizations.filter { it.isFavorite }.map { it.packageName }.toSet()
            FavoriteAppsDiskCache.persistFavoritesSnapshot(context, loaded, favPkgs)
        }
    }

    // Keep disk cache synced whenever user adds/removes Favorite Apps
    LaunchedEffect(tvCustomizations, nativeApps.size) {
        if (nativeApps.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                val favPkgs = tvCustomizations.filter { it.isFavorite }.map { it.packageName }.toSet()
                FavoriteAppsDiskCache.persistFavoritesSnapshot(context, nativeApps, favPkgs)
            }
        }
    }

    // Pre-cache RNA custom/base64 logos in TvAppRamCache as well
    LaunchedEffect(rnaApps, tvCustomizations) {
        withContext(Dispatchers.Default) {
            rnaApps.forEach { rna ->
                val uri = rna.bannerBase64 ?: rna.iconBase64
                if (!uri.isNullOrBlank() && !TvAppRamCache.iconBitmaps.containsKey(uri)) {
                    decodeDataUriToImageBitmap(uri)?.let { bmp ->
                        TvAppRamCache.iconBitmaps[uri] = bmp
                    }
                }
            }
            tvCustomizations.forEach { custom ->
                val uri = custom.customLogoUri
                if (!uri.isNullOrBlank() && !TvAppRamCache.iconBitmaps.containsKey(uri)) {
                    decodeDataUriToImageBitmap(uri)?.let { bmp ->
                        TvAppRamCache.iconBitmaps[uri] = bmp
                    }
                }
            }
        }
    }

    // Merge RNAAI + RNA apps + Native installed apps into a unified list with customizations & lastUsedTimestamp
    val unifiedApps = remember(rnaApps, nativeApps, tvCustomizations, recentAppTimestamps, rnaAiAppEnabled) {
        val customMap = tvCustomizations.associateBy { it.packageName }
        val list = mutableListOf<UnifiedLauncherApp>()

        if (rnaAiAppEnabled) {
            val customRnaAi = customMap[RNAAI_PACKAGE_NAME]
            val recordedRnaAiTs = recentAppTimestamps[RNAAI_PACKAGE_NAME] ?: 0L
            val customRnaAiLogo = customRnaAi?.customLogoUri
            val rawCustomTitle = customRnaAi?.customTitle?.takeIf { it.isNotBlank() }
            val resolvedRnaAiTitle = if (rawCustomTitle == null || rawCustomTitle == "RNAAI") "RNA AI" else rawCustomTitle
            list.add(
                UnifiedLauncherApp(
                    uniqueKey = RNAAI_UNIQUE_KEY,
                    title = resolvedRnaAiTitle,
                    packageName = RNAAI_PACKAGE_NAME,
                    isRnaWebApp = false,
                    rnaApp = null,
                    customLogoUri = customRnaAiLogo,
                    nativeIcon = null,
                    cachedBitmap = customRnaAiLogo?.let { TvAppRamCache.iconBitmaps[it] },
                    isFavorite = customRnaAi?.isFavorite ?: true,
                    favoriteOrder = customRnaAi?.favoriteOrder ?: -100,
                    allAppsOrder = customRnaAi?.allAppsOrder ?: -100,
                    lastUsedTimestamp = recordedRnaAiTs
                )
            )
        }

        // Built-in standalone Dark-Mode File Explorer app (accessible in RNA Launcher and system app drawer)
        val customFileExp = customMap[RNA_FILE_EXPLORER_PACKAGE_NAME]
        val recordedFileExpTs = recentAppTimestamps[RNA_FILE_EXPLORER_PACKAGE_NAME] ?: 0L
        val customFileExpLogo = customFileExp?.customLogoUri
        list.add(
            UnifiedLauncherApp(
                uniqueKey = RNA_FILE_EXPLORER_UNIQUE_KEY,
                title = customFileExp?.customTitle?.takeIf { it.isNotBlank() && it != "RNA File Explorer" } ?: "File Explorer",
                packageName = RNA_FILE_EXPLORER_PACKAGE_NAME,
                isRnaWebApp = false,
                rnaApp = null,
                customLogoUri = customFileExpLogo,
                nativeIcon = null,
                cachedBitmap = customFileExpLogo?.let { TvAppRamCache.iconBitmaps[it] },
                isFavorite = customFileExp?.isFavorite ?: true,
                favoriteOrder = customFileExp?.favoriteOrder ?: -90,
                allAppsOrder = customFileExp?.allAppsOrder ?: -90,
                lastUsedTimestamp = recordedFileExpTs
            )
        )

        rnaApps.filter { it.isInstalled }.forEachIndexed { idx, rna ->
            val custom = customMap[rna.packageName]
            val recordedTs = recentAppTimestamps[rna.packageName] ?: rna.lastLaunchedAt
            val logoUri = custom?.customLogoUri ?: rna.bannerBase64 ?: rna.iconBase64
            list.add(
                UnifiedLauncherApp(
                    uniqueKey = "rna_${rna.id}",
                    title = custom?.customTitle ?: rna.title,
                    packageName = rna.packageName,
                    isRnaWebApp = true,
                    rnaApp = rna,
                    customLogoUri = logoUri,
                    nativeIcon = null,
                    cachedBitmap = logoUri?.let { TvAppRamCache.iconBitmaps[it] },
                    isFavorite = custom?.isFavorite ?: rna.isTvFavorite,
                    favoriteOrder = custom?.favoriteOrder ?: rna.favoriteOrder.takeIf { it > 0 } ?: (1 + idx),
                    allAppsOrder = custom?.allAppsOrder ?: rna.allAppsOrder.takeIf { it > 0 } ?: (1 + idx),
                    lastUsedTimestamp = recordedTs
                )
            )
        }

        nativeApps.forEachIndexed { index, item ->
            val custom = customMap[item.packageName]
            val recordedTs = recentAppTimestamps[item.packageName] ?: item.systemLastTimeUsed
            val customUri = custom?.customLogoUri
            list.add(
                UnifiedLauncherApp(
                    uniqueKey = "pkg_${item.packageName}",
                    title = custom?.customTitle ?: item.label,
                    packageName = item.packageName,
                    isRnaWebApp = false,
                    rnaApp = null,
                    customLogoUri = customUri,
                    nativeIcon = item.drawable,
                    cachedBitmap = (customUri?.let { TvAppRamCache.iconBitmaps[it] })
                        ?: item.bitmap
                        ?: TvAppRamCache.iconBitmaps[item.packageName],
                    nativeBanner = item.bannerDrawable,
                    cachedBannerBitmap = item.bannerBitmap ?: TvAppRamCache.bannerBitmaps[item.packageName],
                    isFavorite = custom?.isFavorite ?: (index < 8),
                    favoriteOrder = custom?.favoriteOrder ?: (50 + index),
                    allAppsOrder = custom?.allAppsOrder ?: (50 + index),
                    lastUsedTimestamp = recordedTs
                )
            )
        }
        list
    }

    val favoriteApps = remember(unifiedApps) {
        unifiedApps.filter { it.isFavorite }.sortedBy { it.favoriteOrder }
    }

    val allAppsSorted = remember(unifiedApps) {
        unifiedApps.sortedBy { it.allAppsOrder }
    }

    // Recent Apps list: combines recently launched RNA web apps and normal apps, sorted by most recent usage
    val recentApps = remember(unifiedApps, recentAppTimestamps) {
        val explicitlyUsed = unifiedApps
            .filter { it.lastUsedTimestamp > 0L }
            .sortedByDescending { it.lastUsedTimestamp }
        if (explicitlyUsed.isNotEmpty()) {
            explicitlyUsed.take(10)
        } else {
            unifiedApps.take(5)
        }
    }

    // Helper to move an app within either favoriteApps (Home tab) or allAppsSorted (Apps tab)
    fun moveAppInList(appKey: String, delta: Int, inFavoritesList: Boolean) {
        val currentList = if (inFavoritesList) favoriteApps else allAppsSorted
        val currentIndex = currentList.indexOfFirst { it.uniqueKey == appKey }
        if (currentIndex < 0) return
        val targetIndex = (currentIndex + delta).coerceIn(0, currentList.size - 1)
        if (targetIndex == currentIndex) return

        val reordered = currentList.toMutableList()
        val movedItem = reordered.removeAt(currentIndex)
        reordered.add(targetIndex, movedItem)

        reordered.forEachIndexed { index, item ->
            saveAppOrdering(
                item = item,
                newFavOrder = if (inFavoritesList) index else item.favoriteOrder,
                newAllOrder = if (!inFavoritesList) index else item.allAppsOrder,
                onUpdateRnaApp = onUpdateRnaApp,
                onSaveTvCustomization = onSaveTvCustomization
            )
        }
    }

    // Helper to toggle Favorite status for any app
    fun toggleAppFavoriteStatus(app: UnifiedLauncherApp, makeFavorite: Boolean) {
        val nextFavOrder = if (makeFavorite) {
            (favoriteApps.maxOfOrNull { it.favoriteOrder } ?: 0) + 1
        } else {
            app.favoriteOrder
        }
        if (app.isRnaWebApp && app.rnaApp != null) {
            onUpdateRnaApp(
                app.rnaApp.copy(
                    isTvFavorite = makeFavorite,
                    favoriteOrder = nextFavOrder
                )
            )
        }
        onSaveTvCustomization(
            TvAppCustomEntity(
                packageName = app.packageName,
                customTitle = app.title,
                customLogoUri = app.customLogoUri,
                isFavorite = makeFavorite,
                favoriteOrder = nextFavOrder,
                allAppsOrder = app.allAppsOrder
            )
        )
    }

    // Helper to uninstall an RNA web app or launch the Android package uninstaller for a native app
    fun uninstallUnifiedApp(app: UnifiedLauncherApp) {
        if (app.isRnaWebApp && app.rnaApp != null) {
            onUninstallRnaApp(app.rnaApp)
            Toast.makeText(context, "Uninstalled ${app.title}", Toast.LENGTH_SHORT).show()
        } else {
            try {
                val deleteIntent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(deleteIntent)
            } catch (_: Exception) {
                try {
                    val detailsIntent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${app.packageName}")
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(detailsIntent)
                } catch (_: Exception) {
                    Toast.makeText(context, "Cannot uninstall system package ${app.title}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Zero-permission Android Photo Picker for custom user photos (copied to permanent internal storage)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    pickedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            val durableUri = TvProfilePersistenceStore.persistCustomProfileMediaUri(
                context = context,
                sourceUri = pickedUri,
                prefix = "wallpaper"
            )
            applyWallpaperWithAnimation(durableUri)
        }
    }

    fun handleLaunchUnifiedApp(app: UnifiedLauncherApp) {
        if (app.packageName == RNAAI_PACKAGE_NAME) {
            onRecordAppLaunched(app.packageName, null)
            try {
                val intent = Intent(context, com.example.RnaAiActivity::class.java).apply {
                    putExtra(com.example.RnaAiActivity.EXTRA_INITIAL_TAB, 0)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                rnaAiInitialSection = 0
                showRnaAiFullScreenApp = true
            }
            return
        }
        if (app.packageName == RNA_FILE_EXPLORER_PACKAGE_NAME) {
            onRecordAppLaunched(app.packageName, null)
            try {
                val intent = Intent(context, RnaFileExplorerActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                fileExplorerPickerMode = FileExplorerPickerMode.STANDALONE_FULL
            }
            return
        }
        onRecordAppLaunched(app.packageName, app.rnaApp?.id)
        launchUnifiedApp(context, app, onLaunchRnaApp)
    }

    // Wire KeyRemapperBridge so pressing the remapped RNA Home button (or hardware Home key)
    // while inside RNA (e.g. on Apps tab, Browser tab, or any dialog) immediately returns to the Home tab.
    DisposableEffect(Unit) {
        val callback: () -> Unit = {
            isBrowserFullScreen = false
            isBrowserTopBarAutoHidden = false
            tvTab = 0
            returnToControlPanelTabOnClose = null
            showYoutubeHomeKeyboard = false
            isYoutubeInlineEditing = false
            homeKeyboardController?.hide()
            showTvPowerHubDialog = false
            showKeyRemapperScreen = false
            showTriggersAndMacrosScreen = false
            showKeyLinksEditor = false
            fileExplorerPickerMode = null
            showWallpaperDialog = false
            showDevicesDrawer = false
            showAddFavoritesDialog = false
            showSearchUtilityDialog = false
            showHdmiInputSelectorDialog = false
            showProfileDialog = false
            showUpdatesDrawer = false
            showVoiceAssistantOverlay = false
            showRnaAiFullScreenApp = false
            showRecentAppsOverlayInLauncher = false
            showQuickAppsTaskbarInLauncher = false
            contextMenuApp = null
            appDetailsTarget = null
            customizingApp = null
            movingAppKey = null
            try {
                homeTabFocusRequester.requestFocus()
            } catch (_: Exception) {}
        }
        KeyRemapperBridge.onNavigateToHomeTabInMainActivity = callback
        onDispose {
            if (KeyRemapperBridge.onNavigateToHomeTabInMainActivity === callback) {
                KeyRemapperBridge.onNavigateToHomeTabInMainActivity = null
            }
        }
    }

    // Strict playback gating: Live video ONLY plays on Home tab (0), Apps tab (1), or 4K Ambient Mode
    // when no dialogs, drawers, AI overlays, settings, search, or file explorer are open!
    val isAnyDialogOrOverlayOpen = showTvPowerHubDialog ||
        showWallpaperDialog ||
        showSearchUtilityDialog ||
        showYoutubeHomeKeyboard ||
        isYoutubeInlineEditing ||
        showVoiceAssistantOverlay ||
        showRnaAiFullScreenApp ||
        showKeyRemapperScreen ||
        showTriggersAndMacrosScreen ||
        showKeyLinksEditor ||
        fileExplorerPickerMode != null ||
        showProfileDialog ||
        showAddFavoritesDialog ||
        showDevicesDrawer ||
        showUpdatesDrawer ||
        showHdmiInputSelectorDialog ||
        showRecentAppsOverlayInLauncher ||
        showQuickAppsTaskbarInLauncher ||
        contextMenuApp != null ||
        customizingApp != null ||
        appDetailsTarget != null

    val shouldPlayLiveWallpaperOnLauncher = wallpaperModeType == "LIVE" &&
        activeLiveWallpaperUri.isNotBlank() &&
        !showAmbientScreensaver &&
        (tvTab == 0 || tvTab == 1) &&
        !isAnyDialogOrOverlayOpen

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1017))
            .onPreviewKeyEvent {
                lastUserInteractionRealtimeMs = android.os.SystemClock.elapsedRealtime()
                false
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    lastUserInteractionRealtimeMs = android.os.SystemClock.elapsedRealtime()
                }
            }
            .testTag("google_tv_launcher_root")
    ) {
        // 1. Full-Screen Static OR Live Video Wallpaper Backdrop
        if (wallpaperModeType == "LIVE" && activeLiveWallpaperUri.isNotBlank()) {
            Box(modifier = Modifier.fillMaxSize()) {
                TvLiveWallpaperBackground(
                    videoUri = activeLiveWallpaperUri,
                    shouldPlay = shouldPlayLiveWallpaperOnLauncher,
                    preferredResolution = liveWallpaperResolution,
                    performanceMode = performanceModeEnabled,
                    onAutoResolutionStepDown = { steppedRes ->
                        liveWallpaperResolution = steppedRes
                        prefs.edit().putString("control_live_wallpaper_res", steppedRes.id).apply()
                    },
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color(0x880B0C12),
                                0.18f to Color(0x180B0C12),
                                0.46f to Color(0x300B0C12),
                                0.68f to Color(0xBE0E1018),
                                1.0f to Color(0xF50E1018)
                            )
                        )
                )
            }
        } else {
            AnimatedContent(
                targetState = activeWallpaperUri,
                transitionSpec = {
                    fadeIn(animationSpec = tween(420, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing))
                },
                label = "RnaTvWallpaperCrossfade"
            ) { targetUri ->
                val wallpaperRequest = remember(targetUri) {
                    ImageRequest.Builder(context)
                        .data(targetUri)
                        .size(1920, 1080)
                        .crossfade(false)
                        .build()
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = wallpaperRequest,
                        contentDescription = "RNA. TV Wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = wallpaperScaleAnim.value
                                scaleY = wallpaperScaleAnim.value
                            }
                    )

                    // Subtle top scrim for top navigation bar legibility & smooth bottom gradient under the apps area
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color(0x880B0C12),
                                    0.18f to Color(0x180B0C12),
                                    0.46f to Color(0x300B0C12),
                                    0.68f to Color(0xBE0E1018),
                                    1.0f to Color(0xF50E1018)
                                )
                            )
                    )
                }
            }
        }

        // 2. Main Foreground Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar (Visible on Home & Apps; in Browser tab it shows for 5 seconds and auto-hides, reappearing when moving Up)
            val isTopBarVisible = !(tvTab == 2 && (isBrowserFullScreen || isBrowserTopBarAutoHidden))
            AnimatedVisibility(
                visible = isTopBarVisible,
                enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(180)),
                exit = shrinkVertically(animationSpec = tween(240)) + fadeOut(animationSpec = tween(180))
            ) {
                GoogleTvTopBar(
                    activeProfile = activeProfile,
                    onProfileClick = { showProfileDialog = true },
                    selectedTab = tvTab,
                    onSelectTab = { nextTab ->
                        movingAppKey = null
                        isBrowserFullScreen = false
                        isBrowserTopBarAutoHidden = (nextTab == 2)
                        tvTab = nextTab
                    },
                    homeTabFocusRequester = homeTabFocusRequester,
                    downContentFocusRequester = activeDownFocusRequester,
                    currentTime = currentTime,
                    sleepTimerRemainingSeconds = sleepTimerRemainingSeconds,
                    onSearchClick = {
                        showSearchUtilityDialog = true
                    },
                    onAiAssistantClick = {
                        returnToControlPanelTabOnClose = null
                        initialWakeWordCommand = null
                        showVoiceAssistantOverlay = true
                    },
                    onUpdatesClick = {
                        returnToControlPanelTabOnClose = null
                        showUpdatesDrawer = true
                    },
                    onDevicesClick = {
                        returnToControlPanelTabOnClose = null
                        showDevicesDrawer = true
                    },
                    onPowerHubClick = {
                        // Controls button opens RNA. Settings
                        returnToControlPanelTabOnClose = null
                        controlPanelTabToOpen = ControlPanelTab.HOME_LAYOUT
                        controlPanelLastTab = ControlPanelTab.HOME_LAYOUT
                        showTvPowerHubDialog = true
                    },
                    onHdmiClick = {
                        // Opens our custom RNA. TV Input Selector (HDMI 1, HDMI 2, HDMI 3, AV, TV)
                        showHdmiInputSelectorDialog = true
                    },
                    onWallpaperClick = {
                        returnToControlPanelTabOnClose = null
                        showWallpaperDialog = true
                    },
                    onSettingsClick = {
                        // Settings gear button opens normal Android system settings
                        launchNormalAndroidSettings(context)
                    }
                )
            }

            // Interactive Android TV "Move" Mode Banner when an app is selected for live D-Pad moving
            AnimatedVisibility(
                visible = movingAppKey != null,
                enter = fadeIn(tween(180)) + expandVertically(tween(200)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(180))
            ) {
                val movingAppTitle = unifiedApps.find { it.uniqueKey == movingAppKey }?.title ?: "App"
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xEE181C2C),
                    border = BorderStroke(1.5.dp, AmberAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 38.dp, vertical = 4.dp)
                        .testTag("tv_move_mode_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenWith,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Moving \"$movingAppTitle\" — Use D-Pad ◀ ▶ ▲ ▼ to move, press OK or Back to lock position",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                        Button(
                            onClick = { movingAppKey = null },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberAccent,
                                contentColor = Color(0xFF12131A)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Optional Accessibility Service Prompt Banner for TV Home Button Interception (Full D-Pad Navigable + Step-by-Step TV Instructions)
            var showAccessibilityInstructionsDialog by remember { mutableStateOf(false) }
            if (showAccessibilityBanner && !isAccessibilityEnabled && !(tvTab == 2 && isBrowserFullScreen)) {
                var enableBtnFocused by remember { mutableStateOf(false) }
                var dismissBtnFocused by remember { mutableStateOf(false) }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xEE141929),
                    border = BorderStroke(
                        width = if (enableBtnFocused || dismissBtnFocused) 2.dp else 1.2.dp,
                        color = if (enableBtnFocused || dismissBtnFocused) CyanPrimary else CyanPrimary.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp, vertical = 4.dp)
                        .testTag("tv_accessibility_notification_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CyanPrimary.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.5f)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = CyanPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Enable RNA. TV Accessibility Service (Default Launcher & Home Key)",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Settings → Device Preferences → Accessibility → RNA. TV → Enable (keeps Home & stock launcher locked to RNA. TV)",
                                    color = Color(0xFFB8C4E0),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (dismissBtnFocused) Color.White else Color(0xFF20273D),
                                border = BorderStroke(
                                    width = if (dismissBtnFocused) 2.dp else 1.dp,
                                    color = if (dismissBtnFocused) CyanPrimary else Color(0x44FFFFFF)
                                ),
                                modifier = Modifier
                                    .onFocusChanged { dismissBtnFocused = it.isFocused }
                                    .focusable()
                                    .onKeyEvent { ev ->
                                        if (ev.type == KeyEventType.KeyDown &&
                                            (ev.key == Key.DirectionCenter || ev.key == Key.Enter || ev.key == Key.NumPadEnter)
                                        ) {
                                            showAccessibilityBanner = false
                                            true
                                        } else false
                                    }
                                    .clickable { showAccessibilityBanner = false }
                                    .testTag("tv_accessibility_banner_dismiss_btn")
                            ) {
                                Text(
                                    text = "Dismiss",
                                    color = if (dismissBtnFocused) Color(0xFF0A0E17) else Color(0xFFD6DEFF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (enableBtnFocused) Color.White else CyanPrimary,
                                border = BorderStroke(
                                    width = if (enableBtnFocused) 2.5.dp else 1.dp,
                                    color = if (enableBtnFocused) EmeraldSuccess else CyanPrimary
                                ),
                                modifier = Modifier
                                    .onFocusChanged { enableBtnFocused = it.isFocused }
                                    .focusable()
                                    .onKeyEvent { ev ->
                                        if (ev.type == KeyEventType.KeyDown &&
                                            (ev.key == Key.DirectionCenter || ev.key == Key.Enter || ev.key == Key.NumPadEnter)
                                        ) {
                                            showAccessibilityInstructionsDialog = true
                                            true
                                        } else false
                                    }
                                    .clickable { showAccessibilityInstructionsDialog = true }
                                    .testTag("tv_accessibility_banner_enable_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = Color(0xFF0A0E17),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "How to Enable",
                                        color = Color(0xFF0A0E17),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showAccessibilityInstructionsDialog) {
                val openSettingsFocusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    delay(60L)
                    runCatching { openSettingsFocusRequester.requestFocus() }
                }
                Dialog(onDismissRequest = { showAccessibilityInstructionsDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = Color(0xFF121728),
                        border = BorderStroke(2.dp, CyanPrimary),
                        modifier = Modifier.width(580.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(26.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = CyanPrimary.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, CyanPrimary),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Security, contentDescription = null, tint = CyanPrimary)
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Enable RNA. TV Accessibility Service",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 19.sp
                                    )
                                    Text(
                                        text = "Follow these quick steps on your Android TV remote:",
                                        color = CyanPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            val steps = listOf(
                                "1. Click 'Open TV Settings' below (or press the Settings gear button on your remote).",
                                "2. Navigate to Device Preferences (or System) → Accessibility.",
                                "3. Scroll down to Downloaded / Installed Services and select RNA. TV.",
                                "4. Turn Enable ON and select OK to confirm."
                            )
                            steps.forEach { stepText ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF192138),
                                    border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = stepText,
                                        color = Color(0xFFE5ECFF),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                var closeFocused by remember { mutableStateOf(false) }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (closeFocused) Color.White else Color(0xFF20273D),
                                    border = BorderStroke(1.5.dp, if (closeFocused) CyanPrimary else Color(0x44FFFFFF)),
                                    modifier = Modifier
                                        .onFocusChanged { closeFocused = it.isFocused }
                                        .focusable()
                                        .onKeyEvent { ev ->
                                            if (ev.type == KeyEventType.KeyDown &&
                                                (ev.key == Key.DirectionCenter || ev.key == Key.Enter || ev.key == Key.NumPadEnter)
                                            ) {
                                                showAccessibilityInstructionsDialog = false
                                                true
                                            } else false
                                        }
                                        .clickable { showAccessibilityInstructionsDialog = false }
                                ) {
                                    Text(
                                        text = "Later",
                                        color = if (closeFocused) Color(0xFF0A0E17) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                var openFocused by remember { mutableStateOf(false) }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (openFocused) Color.White else CyanPrimary,
                                    border = BorderStroke(2.dp, if (openFocused) EmeraldSuccess else CyanPrimary),
                                    modifier = Modifier
                                        .focusRequester(openSettingsFocusRequester)
                                        .onFocusChanged { openFocused = it.isFocused }
                                        .focusable()
                                        .onKeyEvent { ev ->
                                            if (ev.type == KeyEventType.KeyDown &&
                                                (ev.key == Key.DirectionCenter || ev.key == Key.Enter || ev.key == Key.NumPadEnter)
                                            ) {
                                                showAccessibilityInstructionsDialog = false
                                                try {
                                                    context.startActivity(
                                                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }
                                                    )
                                                } catch (_: Exception) {
                                                    runCatching {
                                                        context.startActivity(
                                                            Intent(Settings.ACTION_SETTINGS).apply {
                                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            }
                                                        )
                                                    }
                                                }
                                                true
                                            } else false
                                        }
                                        .clickable {
                                            showAccessibilityInstructionsDialog = false
                                            try {
                                                context.startActivity(
                                                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                )
                                            } catch (_: Exception) {
                                                runCatching {
                                                    context.startActivity(
                                                        Intent(Settings.ACTION_SETTINGS).apply {
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                ) {
                                    Text(
                                        text = "Open TV Settings Now",
                                        color = Color(0xFF0A0E17),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Main Tab Body (0: Home, 1: Apps, 2: Browser) — Direct composition without cross-tab FocusRequester collisions
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (tvTab) {
                    0 -> GoogleTvHomeTab(
                        showRecentAppsRow = showRecentAppsRow,
                        singleRowHomeApps = singleRowHomeApps,
                        showYoutubeSearchBar = showYoutubeSearchBar,
                        youtubeSearchQuery = youtubeSearchQuery,
                        onYoutubeSearchQueryChange = { youtubeSearchQuery = it },
                        isYoutubeKeyboardOpen = showYoutubeHomeKeyboard || isYoutubeInlineEditing,
                        isYoutubeInlineEditing = isYoutubeInlineEditing,
                        onYoutubeInlineEditingChange = { editing -> isYoutubeInlineEditing = editing },
                        onOpenYoutubeRnaKeyboard = { showYoutubeHomeKeyboard = true },
                        onSubmitYoutubeSearch = { submitHomeYoutubeSearch(it) },
                        youtubeSearchFocusRequester = youtubeSearchFocusRequester,
                        youtubeSearchSubmitBtnFocusRequester = youtubeSearchSubmitBtnFocusRequester,
                        appIconShape = selectedAppIconShape,
                        recentApps = recentApps,
                        favoriteApps = favoriteApps,
                        topBarFocusRequester = homeTabFocusRequester,
                        firstContentFocusRequester = homeFirstItemFocusRequester,
                        movingAppKey = movingAppKey,
                        onMoveApp = { appKey, delta ->
                            moveAppInList(appKey = appKey, delta = delta, inFavoritesList = true)
                        },
                        onFinishMovingApp = {
                            movingAppKey = null
                            Toast.makeText(context, "App position saved", Toast.LENGTH_SHORT).show()
                        },
                        onLaunchApp = { app ->
                            handleLaunchUnifiedApp(app)
                        },
                        onLongPressRecentApp = { app ->
                            contextMenuApp = app
                        },
                        onLongPressApp = { app ->
                            contextMenuApp = app
                        },
                        onAddExistingAppsClick = { showAddFavoritesDialog = true },
                        onCreateRnaAppClick = { onOpenCreateWizard() },
                        onOpenRnaManagerClick = { onOpenRnaStudio(0) }
                    )
                    1 -> GoogleTvAppsTab(
                        allApps = allAppsSorted,
                        appIconShape = selectedAppIconShape,
                        topBarFocusRequester = homeTabFocusRequester,
                        firstContentFocusRequester = appsFirstItemFocusRequester,
                        movingAppKey = movingAppKey,
                        onMoveApp = { appKey, delta ->
                            moveAppInList(appKey = appKey, delta = delta, inFavoritesList = false)
                        },
                        onFinishMovingApp = {
                            movingAppKey = null
                            Toast.makeText(context, "App position saved", Toast.LENGTH_SHORT).show()
                        },
                        onLaunchApp = { app ->
                            handleLaunchUnifiedApp(app)
                        },
                        onLongPressApp = { app ->
                            contextMenuApp = app
                        },
                        onAddExistingAppsClick = { showAddFavoritesDialog = true },
                        onCreateRnaAppClick = { onOpenCreateWizard() },
                        onOpenRnaManagerClick = { onOpenRnaStudio(0) }
                    )
                    else -> TvBrowserTab(
                        initialUrl = browserTargetUrl,
                        activeProfileId = activeProfile.id,
                        isolateProfileWebData = activeProfile.isolateBrowserData,
                        isFullScreen = isBrowserFullScreen,
                        onToggleFullScreen = { isBrowserFullScreen = it },
                        isTopBarAutoHidden = isBrowserTopBarAutoHidden,
                        onTopBarAutoHideChange = { hidden -> isBrowserTopBarAutoHidden = hidden },
                        launcherTopBarFocusRequester = homeTabFocusRequester,
                        firstToolbarFocusRequester = browserFirstToolbarFocusRequester,
                        onExitToHomeTab = {
                            isBrowserFullScreen = false
                            isBrowserTopBarAutoHidden = false
                            tvTab = 0
                        },
                        onConvertWebsiteToRnaApp = { convertedApp ->
                            onConvertWebsiteToRnaApp(convertedApp)
                        }
                    )
                }
            }
        }

        // Aesthetic Floating Confirmation Pill when Wallpaper is Set
        AnimatedVisibility(
            visible = showWallpaperAppliedBanner,
            enter = fadeIn(tween(260)) + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut(tween(320)) + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 84.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xEE181B26),
                border = BorderStroke(1.5.dp, CyanPrimary),
                shadowElevation = 18.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = CyanPrimary,
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF12131A),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Text(
                        text = "RNA. TV Wallpaper Applied",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // 3. Night Eye-Care / Blue-Light Warmth Filter Overlay
        if (nightWarmthIntensity > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFF8C2B).copy(alpha = (nightWarmthIntensity * 0.42f).coerceIn(0f, 0.45f)))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (nightWarmthIntensity * 0.45f).coerceIn(0f, 0.50f)))
            )
        }

        // 4. 4K Ambient Screensaver Mode Overlay (Supports both Static and Live Video Wallpapers!)
        if (showAmbientScreensaver) {
            AmbientScreensaverOverlay(
                activeWallpaperUri = activeWallpaperUri,
                wallpaperModeType = wallpaperModeType,
                activeLiveWallpaperUri = activeLiveWallpaperUri,
                liveWallpaperResolution = liveWallpaperResolution,
                performanceMode = performanceModeEnabled,
                currentTime = currentTime,
                currentDate = currentDateStr,
                onExit = {
                    lastUserInteractionRealtimeMs = android.os.SystemClock.elapsedRealtime()
                    showAmbientScreensaver = false
                    reopenControlPanelIfNeeded()
                }
            )
        }

        // ── Modals & Dialogs ──

        // A. Local + Files + Web Search Utility Dialog (Routes web links directly into the integrated TV Browser Tab!)
        if (showSearchUtilityDialog) {
            TvSearchUtilityDialog(
                allApps = allAppsSorted,
                onLaunchApp = { app ->
                    handleLaunchUnifiedApp(app)
                },
                onInstallRnaFile = { uri ->
                    onInstallRnaUri(uri)
                },
                onOpenWebUrlInBrowser = { url, _ ->
                    showSearchUtilityDialog = false
                    browserTargetUrl = url
                    isBrowserTopBarAutoHidden = true
                    tvTab = 2
                },
                onDismiss = { showSearchUtilityDialog = false }
            )
        }

        // A1. Custom RNA. TV Input Selector Dialog (HDMI 1, HDMI 2, HDMI 3, AV, TV)
        if (showHdmiInputSelectorDialog) {
            RnaTvInputSelectorDialog(
                selectedInput = selectedTvInputSource,
                onSelectInput = { chosenInput ->
                    selectedTvInputSource = chosenInput
                    prefs.edit().putString("selected_tv_input_source", chosenInput).apply()
                    showHdmiInputSelectorDialog = false
                    launchTvHardwareInput(context, chosenInput)
                },
                onDismiss = { showHdmiInputSelectorDialog = false }
            )
        }

        // A2. Multi-User Profile Switcher & Editor Dialog
        if (showProfileDialog) {
            TvProfileSwitcherDialog(
                profiles = profiles,
                activeProfile = activeProfile,
                initialCreateNew = openProfileDialogInCreateMode,
                onSelectProfile = { chosen ->
                    openProfileDialogInCreateMode = false
                    switchActiveUserProfile(chosen)
                    Toast.makeText(context, "Switched to ${chosen.name}", Toast.LENGTH_SHORT).show()
                },
                onSaveProfile = { savedProfile, isNew ->
                    openProfileDialogInCreateMode = false
                    val enrichedProfile = if (isNew) {
                        savedProfile.copy(
                            wallpaperModeType = "STATIC",
                            liveWallpaperUri = "",
                            appIconShape = TvAppIconShape.CIRCLE.id,
                            singleRowHomeApps = false,
                            showRecentAppsRow = true,
                            ambientIdleMinutes = 5
                        )
                    } else {
                        savedProfile
                    }
                    val updatedList = if (isNew) {
                        profiles + enrichedProfile
                    } else {
                        profiles.map { if (it.id == enrichedProfile.id) enrichedProfile else it }
                    }
                    profiles = updatedList
                    saveTvProfiles(prefs, updatedList)
                    switchActiveUserProfile(enrichedProfile)
                },
                onDeleteProfile = { toDelete ->
                    if (profiles.size > 1) {
                        val remaining = profiles.filterNot { it.id == toDelete.id }
                        profiles = remaining
                        saveTvProfiles(prefs, remaining)
                        if (activeProfileId == toDelete.id) {
                            switchActiveUserProfile(remaining.first())
                        }
                    }
                },
                onDismiss = {
                    openProfileDialogInCreateMode = false
                    showProfileDialog = false
                }
            )
        }

        // B. Control Panel Dialog (Opened via the Controls button; bigger, multi-section layout)
        if (showTvPowerHubDialog) {
            TvPowerHubDialog(
                context = context,
                initialTab = controlPanelTabToOpen,
                onTabChanged = { tab -> controlPanelLastTab = tab },
                showRecentAppsRow = showRecentAppsRow,
                onToggleRecentAppsRow = { enabled ->
                    onToggleRecentAppsRow(enabled)
                    updateActiveProfileFields { it.copy(showRecentAppsRow = enabled) }
                },
                singleRowHomeApps = singleRowHomeApps,
                onToggleSingleRowHomeApps = { enabled ->
                    onToggleSingleRowHomeApps(enabled)
                    updateActiveProfileFields { it.copy(singleRowHomeApps = enabled) }
                },
                showYoutubeSearchBar = showYoutubeSearchBar,
                onToggleYoutubeSearchBar = { enabled ->
                    showYoutubeSearchBar = enabled
                    if (!enabled) {
                        showYoutubeHomeKeyboard = false
                        isYoutubeInlineEditing = false
                    }
                    prefs.edit().putBoolean(KEY_YOUTUBE_SEARCH_BAR_ENABLED, enabled).commit()
                },
                onClearRecentApps = onClearRecentApps,
                selectedAppIconShape = selectedAppIconShape,
                onSelectAppIconShape = { nextShape ->
                    selectedAppIconShape = nextShape
                    prefs.edit().putString("control_app_icon_shape", nextShape.id).commit()
                    updateActiveProfileFields { it.copy(appIconShape = nextShape.id) }
                },
                liveWallpaperResolution = liveWallpaperResolution,
                onSelectLiveWallpaperResolution = { nextRes ->
                    liveWallpaperResolution = nextRes
                    prefs.edit().putString("control_live_wallpaper_res", nextRes.id).commit()
                },
                liveWallpaperPreviewEnabled = liveWallpaperPreviewEnabled,
                onToggleLiveWallpaperPreview = { enabled ->
                    liveWallpaperPreviewEnabled = enabled
                    prefs.edit()
                        .putBoolean(KEY_LIVE_WALLPAPER_PREVIEW_ENABLED, enabled)
                        .putBoolean("control_live_wallpaper_preview_enabled", enabled)
                        .commit()
                },
                ambientIdleMinutes = ambientIdleMinutes,
                onSelectAmbientIdleMinutes = { mins ->
                    ambientIdleMinutes = mins
                    lastUserInteractionRealtimeMs = android.os.SystemClock.elapsedRealtime()
                    prefs.edit().putInt("control_ambient_idle_minutes", mins).commit()
                    updateActiveProfileFields { it.copy(ambientIdleMinutes = mins) }
                },
                askWhoIsWatchingOnBoot = askWhoIsWatchingOnBoot,
                onToggleAskWhoIsWatchingOnBoot = { enabled ->
                    askWhoIsWatchingOnBoot = enabled
                    prefs.edit().putBoolean("ask_who_is_watching_on_boot", enabled).commit()
                },
                usbBootAutoBackEnabled = usbBootAutoBackEnabled,
                onToggleUsbBootAutoBack = { enabled ->
                    usbBootAutoBackEnabled = enabled
                    prefs.edit().putBoolean("control_usb_boot_auto_back", enabled).commit()
                },
                openRnaAtBootEnabled = openRnaAtBootEnabled,
                onToggleOpenRnaAtBoot = { enabled ->
                    openRnaAtBootEnabled = enabled
                    prefs.edit().putBoolean(KEY_OPEN_RNA_AT_BOOT, enabled).commit()
                },
                performanceModeEnabled = performanceModeEnabled,
                onTogglePerformanceMode = { enabled ->
                    performanceModeEnabled = enabled
                    TvPerformanceMode.isEnabled = enabled
                    prefs.edit()
                        .putBoolean("performance_mode_enabled", enabled)
                        .putBoolean("control_performance_mode", enabled)
                        .commit()
                },
                browserFastPerformanceMode = browserFastPerformanceMode,
                onToggleBrowserFastPerformanceMode = { enabled ->
                    browserFastPerformanceMode = enabled
                    prefs.edit().putBoolean("browser_fast_performance_mode", enabled).commit()
                    context.getSharedPreferences(com.example.WebContainerActivity.PREFS_WEB_CONTROLS, Context.MODE_PRIVATE)
                        .edit()
                        .putBoolean("browser_fast_performance_mode", enabled)
                        .commit()
                },
                splashScreenEnabled = splashScreenEnabled,
                onToggleSplashScreen = { enabled ->
                    splashScreenEnabled = enabled
                    prefs.edit()
                        .putBoolean(KEY_SPLASH_SCREEN_ENABLED, enabled)
                        .putBoolean("splash_screen_enabled", enabled)
                        .commit()
                },
                replaceGoogleAssistantEnabled = replaceGoogleAssistantEnabled,
                onToggleReplaceGoogleAssistant = { enabled ->
                    replaceGoogleAssistantEnabled = enabled
                    prefs.edit().putBoolean(RnaHomeInterceptorService.KEY_REPLACE_GOOGLE_ASSISTANT, enabled).commit()
                },
                aiTtsEnabled = aiTtsEnabled,
                onToggleAiTts = { enabled ->
                    aiTtsEnabled = enabled
                    prefs.edit().putBoolean("ai_tts_enabled", enabled).commit()
                },
                rnaAiAppEnabled = rnaAiAppEnabled,
                onToggleRnaAiAppEnabled = { enabled ->
                    rnaAiAppEnabled = enabled
                    prefs.edit().putBoolean(KEY_RNAAI_ENABLED_IN_CONTROL_PANEL, enabled).commit()
                },
                onOpenRnaAiSettings = {
                    val returnTab = controlPanelLastTab
                    try {
                        val intent = Intent(context, com.example.RnaAiActivity::class.java).apply {
                            putExtra(com.example.RnaAiActivity.EXTRA_INITIAL_TAB, 2)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        // Keep Control Panel open on the active tab so pressing Back from RnaAiActivity lands right back here
                        controlPanelTabToOpen = returnTab
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        returnToControlPanelTabOnClose = returnTab
                        showTvPowerHubDialog = false
                        rnaAiInitialSection = 2
                        showRnaAiFullScreenApp = true
                    }
                },
                onOpenKeyRemapper = {
                    returnToControlPanelTabOnClose = controlPanelLastTab
                    showTvPowerHubDialog = false
                    showKeyRemapperScreen = true
                },
                onOpenTriggersAndMacros = {
                    returnToControlPanelTabOnClose = controlPanelLastTab
                    showTvPowerHubDialog = false
                    showTriggersAndMacrosScreen = true
                },
                onOpenKeyLinksEditor = {
                    returnToControlPanelTabOnClose = controlPanelLastTab
                    showTvPowerHubDialog = false
                    showKeyLinksEditor = true
                },
                onOpenVoiceAssistant = {
                    returnToControlPanelTabOnClose = controlPanelLastTab
                    showTvPowerHubDialog = false
                    initialWakeWordCommand = null
                    showVoiceAssistantOverlay = true
                },
                onOpenUpdatesDrawer = {
                    returnToControlPanelTabOnClose = controlPanelLastTab
                    showTvPowerHubDialog = false
                    showUpdatesDrawer = true
                },
                onOpenWallpapers = {
                    returnToControlPanelTabOnClose = controlPanelLastTab
                    showTvPowerHubDialog = false
                    showWallpaperDialog = true
                },
                onOpenDevices = {
                    returnToControlPanelTabOnClose = controlPanelLastTab
                    showTvPowerHubDialog = false
                    showDevicesDrawer = true
                },
                onOpenRnaStudioSettings = {
                    ControlPanelReturnState.returnToFunctionsFromStudio = true
                    showTvPowerHubDialog = false
                    onOpenRnaStudio(3) // Opens More -> Settings tab
                },
                sleepTimerRemainingSeconds = sleepTimerRemainingSeconds,
                onSetSleepTimerMinutes = { minutes ->
                    sleepTimerRemainingSeconds = minutes * 60
                },
                nightWarmthIntensity = nightWarmthIntensity,
                onChangeNightWarmth = { value ->
                    nightWarmthIntensity = value
                    prefs.edit().putFloat("night_warmth_intensity", value).apply()
                },
                onLaunchAmbientMode = {
                    returnToControlPanelTabOnClose = controlPanelLastTab
                    showTvPowerHubDialog = false
                    showAmbientScreensaver = true
                },
                onDismiss = {
                    returnToControlPanelTabOnClose = null
                    showTvPowerHubDialog = false
                }
            )
        }

        // C1. App Context Menu Dialog (Triggered by Long-Press or Menu Button on an App Icon)
        contextMenuApp?.let { targetApp ->
            AppContextMenuDialog(
                app = targetApp,
                onDismiss = { contextMenuApp = null },
                onStartMoveMode = {
                    contextMenuApp = null
                    movingAppKey = targetApp.uniqueKey
                },
                onToggleFavorite = {
                    val nextFav = !targetApp.isFavorite
                    toggleAppFavoriteStatus(targetApp, nextFav)
                    contextMenuApp = null
                    Toast.makeText(
                        context,
                        if (nextFav) "Added ${targetApp.title} to Favorites" else "Removed ${targetApp.title} from Favorites",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onViewAppDetails = {
                    contextMenuApp = null
                    appDetailsTarget = targetApp
                },
                onExportApp = {
                    contextMenuApp = null
                    if (targetApp.isRnaWebApp && targetApp.rnaApp != null) {
                        onExportRnaApp(targetApp.rnaApp)
                    }
                },
                onUninstallApp = {
                    contextMenuApp = null
                    // Open App Info instead of directly launching the package uninstaller dialog!
                    when {
                        targetApp.packageName == RNAAI_PACKAGE_NAME ||
                            targetApp.packageName == RNA_FILE_EXPLORER_PACKAGE_NAME -> {
                            appDetailsTarget = targetApp
                        }
                        targetApp.isRnaWebApp -> {
                            appDetailsTarget = targetApp
                        }
                        else -> {
                            try {
                                val detailsIntent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.parse("package:${targetApp.packageName}")
                                ).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(detailsIntent)
                            } catch (_: Exception) {
                                appDetailsTarget = targetApp
                            }
                        }
                    }
                },
                onCustomizeApp = {
                    contextMenuApp = null
                    customizingApp = targetApp
                }
            )
        }

        // C2. View App Details Dialog
        appDetailsTarget?.let { targetApp ->
            AppDetailsDialog(
                app = targetApp,
                onDismiss = { appDetailsTarget = null },
                onLaunchApp = {
                    appDetailsTarget = null
                    handleLaunchUnifiedApp(targetApp)
                },
                onExportApp = {
                    if (targetApp.isRnaWebApp && targetApp.rnaApp != null) {
                        onExportRnaApp(targetApp.rnaApp)
                    }
                },
                onUninstallApp = {
                    appDetailsTarget = null
                    uninstallUnifiedApp(targetApp)
                },
                onOpenSystemAppInfo = {
                    appDetailsTarget = null
                    try {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${targetApp.packageName}")
                        ).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        launchNormalAndroidSettings(context)
                    }
                }
            )
        }

        // C3. Customize App Logo / Name Modal (Includes "Revert to Original Logo")
        customizingApp?.let { targetApp ->
            val currentList = if (tvTab == 0) favoriteApps else allAppsSorted
            val currentIndex = currentList.indexOfFirst { it.uniqueKey == targetApp.uniqueKey }

            CustomizeAndRearrangeAppDialog(
                app = targetApp,
                canMoveLeft = currentIndex > 0,
                canMoveRight = currentIndex >= 0 && currentIndex < currentList.size - 1,
                onDismiss = { customizingApp = null },
                onMoveDirection = { delta ->
                    moveAppInList(
                        appKey = targetApp.uniqueKey,
                        delta = delta,
                        inFavoritesList = (tvTab == 0)
                    )
                },
                onRevertToOriginalLogo = {
                    if (targetApp.isRnaWebApp && targetApp.rnaApp != null) {
                        val defaultFavicon = if (targetApp.rnaApp.sourceType == "URL") {
                            com.example.ui.wizard.buildFaviconUrlForWebsite(targetApp.rnaApp.sourceUriOrContent)
                        } else null
                        onUpdateRnaApp(
                            targetApp.rnaApp.copy(
                                iconBase64 = defaultFavicon,
                                bannerBase64 = defaultFavicon
                            )
                        )
                    }
                    onSaveTvCustomization(
                        TvAppCustomEntity(
                            packageName = targetApp.packageName,
                            customTitle = targetApp.title,
                            customLogoUri = null,
                            isFavorite = targetApp.isFavorite,
                            favoriteOrder = targetApp.favoriteOrder,
                            allAppsOrder = targetApp.allAppsOrder
                        )
                    )
                    Toast.makeText(context, "Reverted ${targetApp.title} to original logo", Toast.LENGTH_SHORT).show()
                    customizingApp = null
                },
                onSaveCustomNameAndLogo = { newTitle, newLogoUri, isFav ->
                    if (!newLogoUri.isNullOrBlank()) {
                        decodeDataUriToImageBitmap(newLogoUri)?.let { bmp ->
                            TvAppRamCache.iconBitmaps[newLogoUri] = bmp
                        }
                    }
                    if (targetApp.isRnaWebApp && targetApp.rnaApp != null) {
                        onUpdateRnaApp(
                            targetApp.rnaApp.copy(
                                title = newTitle,
                                iconBase64 = newLogoUri,
                                bannerBase64 = newLogoUri,
                                isTvFavorite = isFav
                            )
                        )
                    }
                    onSaveTvCustomization(
                        TvAppCustomEntity(
                            packageName = targetApp.packageName,
                            customTitle = newTitle,
                            customLogoUri = newLogoUri,
                            isFavorite = isFav,
                            favoriteOrder = targetApp.favoriteOrder,
                            allAppsOrder = targetApp.allAppsOrder
                        )
                    )
                    customizingApp = null
                },
                onOpenInRnaStudio = {
                    customizingApp = null
                    onOpenRnaStudio(2)
                }
            )
        }

        // D. Add Existing Apps to Home Favorites Dialog (with real App Icons & sequential ordering)
        if (showAddFavoritesDialog) {
            AddExistingAppsToHomeDialog(
                allApps = allAppsSorted,
                onToggleFavorite = { app, isFav ->
                    toggleAppFavoriteStatus(app, isFav)
                },
                onCreateNewRnaApp = {
                    showAddFavoritesDialog = false
                    onOpenCreateWizard()
                },
                onOpenRnaStudio = {
                    showAddFavoritesDialog = false
                    onOpenRnaStudio(0)
                },
                onDismiss = { showAddFavoritesDialog = false }
            )
        }

        // E. Wallpapers Dialog ("Wallpapers" with Static & Live Pill Buttons, optional 360p Live Previews, File Explorer Picker & Browser Search)
        if (showWallpaperDialog) {
            WallpaperCustomizerDialog(
                activeWallpaperUri = activeWallpaperUri,
                activeLiveWallpaperUri = activeLiveWallpaperUri,
                wallpaperModeType = wallpaperModeType,
                liveWallpaperPreviewEnabled = liveWallpaperPreviewEnabled,
                onSelectWallpaper = { selectedUri ->
                    returnToControlPanelTabOnClose = null
                    applyWallpaperWithAnimation(selectedUri)
                    showWallpaperDialog = false
                },
                onSelectLiveWallpaper = { selectedLiveUri ->
                    returnToControlPanelTabOnClose = null
                    applyWallpaperWithAnimation(selectedLiveUri)
                    showWallpaperDialog = false
                },
                onOpenFileExplorerForStatic = {
                    showWallpaperDialog = false
                    fileExplorerPickerMode = FileExplorerPickerMode.IMAGES_FOR_WALLPAPER
                },
                onOpenFileExplorerForLive = {
                    showWallpaperDialog = false
                    fileExplorerPickerMode = FileExplorerPickerMode.VIDEOS_FOR_LIVE_WALLPAPER
                },
                onSearchStaticWallpapersInBrowser = {
                    returnToControlPanelTabOnClose = null
                    showWallpaperDialog = false
                    browserTargetUrl = "https://www.google.com/search?tbm=isch&q=4K+uhd+tv+wallpapers"
                    isBrowserTopBarAutoHidden = true
                    tvTab = 2
                },
                onDownloadLiveWallpapersInBrowser = {
                    returnToControlPanelTabOnClose = null
                    showWallpaperDialog = false
                    browserTargetUrl = "https://www.google.com/search?q=free+4k+live+wallpaper+loop+mp4+download"
                    isBrowserTopBarAutoHidden = true
                    tvTab = 2
                },
                onDismiss = {
                    showWallpaperDialog = false
                    reopenControlPanelIfNeeded()
                }
            )
        }

        // E2. Full-Screen Focus-Trapping RNA File Explorer Dialog (Custom Static Image Wallpaper, Live Video Wallpaper, or Standalone)
        fileExplorerPickerMode?.let { mode ->
            Dialog(
                onDismissRequest = { fileExplorerPickerMode = null },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
                RnaFileExplorerScreen(
                    pickerMode = mode,
                    onSelectWallpaperImage = { chosenImgUri ->
                        returnToControlPanelTabOnClose = null
                        fileExplorerPickerMode = null
                        applyWallpaperWithAnimation(chosenImgUri)
                    },
                    onSelectLiveWallpaperVideo = { chosenVidUri ->
                        returnToControlPanelTabOnClose = null
                        fileExplorerPickerMode = null
                        applyWallpaperWithAnimation(chosenVidUri)
                    },
                    onClose = { fileExplorerPickerMode = null }
                )
            }
        }

        // E3. Full-Screen Key Links Editor Overlay (BUTTON_1..BUTTON_16 + Xbox Keycodes + Custom Keycode Editor)
        if (showKeyLinksEditor) {
            TvKeyLinksEditorOverlay(
                visible = true,
                onDismiss = {
                    showKeyLinksEditor = false
                    reopenControlPanelIfNeeded()
                }
            )
        }

        // F. Wire Autonomous Agent Controller & RnaAiToolsEngine to current Launcher Activity, Apps, Browser & Home navigation
        LaunchedEffect(allAppsSorted, unifiedApps) {
            RnaAiToolsEngine.onApplyWallpaperCallback = { wpNameOrUri, targetProfName ->
                applyWallpaperWithAnimation(wpNameOrUri, targetProfName)
            }
            RnaAiToolsEngine.onSwitchProfileByNameCallback = { profName ->
                val matchedProf = profiles.firstOrNull { it.name.equals(profName, ignoreCase = true) }
                if (matchedProf != null) {
                    switchActiveUserProfile(matchedProf)
                    Toast.makeText(context, "Switched to profile ${matchedProf.name}", Toast.LENGTH_SHORT).show()
                }
            }
            RnaAiToolsEngine.onApplyControlPanelSettingCallback = { key, value ->
                val boolVal = value.lowercase(Locale.US) in setOf("true", "on", "1", "yes", "enabled", "enable")
                when (key) {
                    "icon_shape", "app_icon_shape" -> {
                        val shape = TvAppIconShape.fromId(value)
                        selectedAppIconShape = shape
                        updateActiveProfileFields { it.copy(appIconShape = shape.id) }
                    }
                    "ambient_idle", "ambient_idle_minutes" -> {
                        val mins = value.filter { it.isDigit() }.toIntOrNull() ?: 5
                        ambientIdleMinutes = mins
                        lastUserInteractionRealtimeMs = android.os.SystemClock.elapsedRealtime()
                        updateActiveProfileFields { it.copy(ambientIdleMinutes = mins) }
                    }
                    "live_res", "live_wallpaper_res" -> {
                        val norm = if (value.endsWith("p", ignoreCase = true)) value else "${value}p"
                        liveWallpaperResolution = LiveWallpaperResolution.fromId(norm)
                    }
                    "live_wallpaper_preview", "live_preview" -> {
                        liveWallpaperPreviewEnabled = boolVal
                        prefs.edit().putBoolean(KEY_LIVE_WALLPAPER_PREVIEW_ENABLED, boolVal).commit()
                    }
                    "performance_mode", "perf_mode" -> {
                        performanceModeEnabled = boolVal
                        TvPerformanceMode.isEnabled = boolVal
                    }
                    "splash_screen", "splash" -> {
                        splashScreenEnabled = boolVal
                    }
                    "usb_boot_auto_back" -> {
                        usbBootAutoBackEnabled = boolVal
                    }
                    "rna_ai", "rna_ai_enabled" -> {
                        rnaAiAppEnabled = boolVal
                    }
                }
            }
            RnaAiToolsEngine.onLaunchAppCallback = { targetPkgOrName ->
                val matched = unifiedApps.firstOrNull {
                    it.packageName.equals(targetPkgOrName, ignoreCase = true) ||
                        it.title.equals(targetPkgOrName, ignoreCase = true)
                }
                if (matched != null) {
                    handleLaunchUnifiedApp(matched)
                } else {
                    val pm = context.packageManager
                    val intent = pm.getLeanbackLaunchIntentForPackage(targetPkgOrName)
                        ?: pm.getLaunchIntentForPackage(targetPkgOrName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(intent) }
                    }
                }
            }

            RnaAutonomousAgentController.activeActivityRef = java.lang.ref.WeakReference(context as? android.app.Activity)
            RnaAutonomousAgentController.installedAppsSnapshot = allAppsSorted
            RnaAutonomousAgentController.onNavigateHomeCallback = {
                showRnaAiFullScreenApp = false
                isBrowserFullScreen = false
                isBrowserTopBarAutoHidden = false
                tvTab = 0
                runCatching {
                    val homeIntent = Intent(context, com.example.MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra(com.example.MainActivity.EXTRA_OPEN_TV_LAUNCHER, true)
                    }
                    context.startActivity(homeIntent)
                }
            }
            RnaAutonomousAgentController.onOpenUrlInBrowserCallback = { targetUrl ->
                showRnaAiFullScreenApp = false
                browserTargetUrl = targetUrl
                isBrowserTopBarAutoHidden = true
                tvTab = 2
                prefs.edit().putString("pending_external_browser_url", targetUrl).apply()
                runCatching {
                    val browserIntent = Intent(context, com.example.MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        putExtra(com.example.MainActivity.EXTRA_OPEN_TV_LAUNCHER, true)
                        putExtra(com.example.MainActivity.EXTRA_OPEN_TV_BROWSER_URL, targetUrl)
                    }
                    context.startActivity(browserIntent)
                }
            }
            RnaAutonomousAgentController.onLaunchAppCallback = { targetPkgOrName ->
                val trimmedTarget = targetPkgOrName.trim()
                if (RnaAutonomousAgentController.isExternalBrowserNameOrPackage(trimmedTarget)) {
                    showRnaAiFullScreenApp = false
                    browserTargetUrl = "https://www.google.com"
                    isBrowserTopBarAutoHidden = true
                    tvTab = 2
                } else if (trimmedTarget.equals("rnaai", ignoreCase = true) || trimmedTarget.equals("rna ai", ignoreCase = true) || trimmedTarget.equals(RNAAI_PACKAGE_NAME, ignoreCase = true)) {
                    try {
                        val intent = Intent(context, com.example.RnaAiActivity::class.java).apply {
                            putExtra(com.example.RnaAiActivity.EXTRA_INITIAL_TAB, 0)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        rnaAiInitialSection = 0
                        showRnaAiFullScreenApp = true
                    }
                } else {
                    val matchedApp = unifiedApps.firstOrNull {
                        it.packageName.equals(trimmedTarget, ignoreCase = true)
                    } ?: unifiedApps.firstOrNull {
                        it.title.equals(trimmedTarget, ignoreCase = true) ||
                            it.title.contains(trimmedTarget, ignoreCase = true) ||
                            trimmedTarget.contains(it.title, ignoreCase = true)
                    }
                    if (matchedApp != null && RnaAutonomousAgentController.isExternalBrowserNameOrPackage(matchedApp.packageName)) {
                        showRnaAiFullScreenApp = false
                        browserTargetUrl = "https://www.google.com"
                        isBrowserTopBarAutoHidden = true
                        tvTab = 2
                    } else if (matchedApp != null) {
                        showRnaAiFullScreenApp = false
                        handleLaunchUnifiedApp(matchedApp)
                    } else {
                        val pm = context.packageManager
                        val launchIntent = pm.getLeanbackLaunchIntentForPackage(trimmedTarget)
                            ?: pm.getLaunchIntentForPackage(trimmedTarget)
                        if (launchIntent != null) {
                            showRnaAiFullScreenApp = false
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                            // Keep the Agent glow & HUD visible on top of external apps!
                            RnaAutonomousAgentController.onSyncSystemOverlayCallback?.invoke()
                            if (RnaAutonomousAgentController.isAgentModeActive.value &&
                                !RnaHomeInterceptorService.isAccessibilityServiceEnabled(context)
                            ) {
                                scope.launch {
                                    delay(400L)
                                    val overlayIntent = Intent(context, com.example.RnaAssistantOverlayActivity::class.java).apply {
                                        addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                                Intent.FLAG_ACTIVITY_NO_ANIMATION
                                        )
                                    }
                                    runCatching { context.startActivity(overlayIntent) }
                                }
                            }
                        } else {
                            showRnaAiFullScreenApp = false
                            browserTargetUrl = "https://www.google.com/search?q=${Uri.encode(trimmedTarget)}"
                            isBrowserTopBarAutoHidden = true
                            tvTab = 2
                        }
                    }
                }
            }

            // Wire RNA Controller Network Bridge (Phone Remote + Dual Xbox Wireless Controller Server)
            RnaControllerNetworkBridge.ensureTvControllerServerStarted(context)
            RnaControllerNetworkBridge.updateExportedTvAppsFromUnifiedList(allAppsSorted)
            RnaControllerNetworkBridge.onRemoteNavigateHome = {
                showRnaAiFullScreenApp = false
                showDevicesDrawer = false
                showUpdatesDrawer = false
                isBrowserFullScreen = false
                isBrowserTopBarAutoHidden = false
                tvTab = 0
            }
            RnaControllerNetworkBridge.onRemoteOpenRecentApps = {
                showRecentAppsOverlayInLauncher = !showRecentAppsOverlayInLauncher
            }
            RnaControllerNetworkBridge.onRemoteOpenTaskbar = {
                showQuickAppsTaskbarInLauncher = !showQuickAppsTaskbarInLauncher
            }
            RnaControllerNetworkBridge.onRemoteOpenAiBar = {
                initialWakeWordCommand = null
                showVoiceAssistantOverlay = true
            }
            RnaControllerNetworkBridge.onRemoteLaunchAppByPackage = { targetPkg ->
                val matched = unifiedApps.firstOrNull {
                    it.packageName.equals(targetPkg, ignoreCase = true)
                }
                if (matched != null) {
                    handleLaunchUnifiedApp(matched)
                } else {
                    val pm = context.packageManager
                    val launchIntent = pm.getLeanbackLaunchIntentForPackage(targetPkg)
                        ?: pm.getLaunchIntentForPackage(targetPkg)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launchIntent)
                    }
                }
            }
        }

        // F2. Full-Screen RNA AI TV Application (fallback in-process overlay if Activity launch fails)
        if (showRnaAiFullScreenApp) {
            RnaAiTvAppScreen(
                prefs = prefs,
                allApps = allAppsSorted,
                initialTab = rnaAiInitialSection,
                onLaunchAppByPackageOrName = { targetPkgOrName ->
                    val trimmedTarget = targetPkgOrName.trim()
                    if (RnaAutonomousAgentController.isExternalBrowserNameOrPackage(trimmedTarget)) {
                        showRnaAiFullScreenApp = false
                        browserTargetUrl = "https://www.google.com"
                        isBrowserTopBarAutoHidden = true
                        tvTab = 2
                    } else {
                        val matchedApp = unifiedApps.firstOrNull {
                            it.packageName.equals(trimmedTarget, ignoreCase = true)
                        } ?: unifiedApps.firstOrNull {
                            it.title.equals(trimmedTarget, ignoreCase = true) ||
                                it.title.contains(trimmedTarget, ignoreCase = true) ||
                                trimmedTarget.contains(it.title, ignoreCase = true)
                        }
                        if (matchedApp != null && RnaAutonomousAgentController.isExternalBrowserNameOrPackage(matchedApp.packageName)) {
                            showRnaAiFullScreenApp = false
                            browserTargetUrl = "https://www.google.com"
                            isBrowserTopBarAutoHidden = true
                            tvTab = 2
                        } else if (matchedApp != null) {
                            showRnaAiFullScreenApp = false
                            handleLaunchUnifiedApp(matchedApp)
                        } else {
                            val pm = context.packageManager
                            val launchIntent = pm.getLeanbackLaunchIntentForPackage(trimmedTarget)
                                ?: pm.getLaunchIntentForPackage(trimmedTarget)
                            if (launchIntent != null) {
                                showRnaAiFullScreenApp = false
                                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(launchIntent)
                            } else {
                                showRnaAiFullScreenApp = false
                                browserTargetUrl = "https://www.google.com/search?q=${Uri.encode(trimmedTarget)}"
                                isBrowserTopBarAutoHidden = true
                                tvTab = 2
                            }
                        }
                    }
                },
                onOpenUrlInRnaBrowser = { targetUrl ->
                    showRnaAiFullScreenApp = false
                    browserTargetUrl = targetUrl
                    isBrowserTopBarAutoHidden = true
                    tvTab = 2
                },
                onLaunchLiveVoiceAssistant = {
                    initialWakeWordCommand = null
                    showVoiceAssistantOverlay = true
                },
                onStartAutonomousAgentWithGoal = { taskQuery ->
                    showRnaAiFullScreenApp = false
                    RnaAutonomousAgentController.enterAgentMode()
                    RnaAutonomousAgentController.submitUserInstruction(taskQuery)
                },
                onExitApp = {
                    showRnaAiFullScreenApp = false
                    reopenControlPanelIfNeeded()
                }
            )
        }

        // F3. Full-Screen TV Remote Button Remapper (TV Quick Actions Pro style)
        if (showKeyRemapperScreen) {
            TvKeyRemapperScreen(
                prefs = prefs,
                allApps = allAppsSorted,
                onTestExecuteAction = { binding ->
                    returnToControlPanelTabOnClose = null
                    when (binding.actionType) {
                        RemappedKeyActionType.RNA_HOME -> {
                            showKeyRemapperScreen = false
                            tvTab = 0
                        }
                        RemappedKeyActionType.RECENT_APPS_DRAWER -> {
                            showKeyRemapperScreen = false
                            showRecentAppsOverlayInLauncher = true
                        }
                        RemappedKeyActionType.QUICK_APPS_TASKBAR -> {
                            showKeyRemapperScreen = false
                            showQuickAppsTaskbarInLauncher = true
                        }
                        RemappedKeyActionType.OPEN_APP -> {
                            val found = unifiedApps.firstOrNull { it.packageName == binding.targetPackage }
                            if (found != null) {
                                showKeyRemapperScreen = false
                                handleLaunchUnifiedApp(found)
                            }
                        }
                        RemappedKeyActionType.LIVE_VOICE_AI -> {
                            showKeyRemapperScreen = false
                            showVoiceAssistantOverlay = true
                        }
                        RemappedKeyActionType.AUTONOMOUS_AGENT -> {
                            showKeyRemapperScreen = false
                            RnaAutonomousAgentController.enterAgentMode()
                        }
                        RemappedKeyActionType.RNA_AI_APP -> {
                            showKeyRemapperScreen = false
                            try {
                                val intent = Intent(context, com.example.RnaAiActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                showRnaAiFullScreenApp = true
                            }
                        }
                        RemappedKeyActionType.RNA_BROWSER -> {
                            showKeyRemapperScreen = false
                            isBrowserTopBarAutoHidden = true
                            tvTab = 2
                        }
                        else -> {
                            Toast.makeText(context, "Tested action: ${binding.actionType.title}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                onDismiss = {
                    showKeyRemapperScreen = false
                    reopenControlPanelIfNeeded()
                }
            )
        }

        // F3b. Full-Screen Triggers & Macros Automation Studio
        if (showTriggersAndMacrosScreen) {
            TvTriggersAndMacrosScreen(
                prefs = prefs,
                allApps = allAppsSorted,
                onDismiss = {
                    showTriggersAndMacrosScreen = false
                    reopenControlPanelIfNeeded()
                }
            )
        }

        // F4. Recent Apps Drawer Overlay (when triggered from within Launcher)
        if (showRecentAppsOverlayInLauncher) {
            TvRecentAppsDrawerOverlay(
                allApps = allAppsSorted,
                onLaunchApp = { app ->
                    showRecentAppsOverlayInLauncher = false
                    handleLaunchUnifiedApp(app)
                },
                onDismiss = { showRecentAppsOverlayInLauncher = false }
            )
        }

        // F5. Small All-Apps Dark Taskbar Overlay (when triggered from within Launcher)
        if (showQuickAppsTaskbarInLauncher) {
            TvQuickAppsTaskbarOverlay(
                allApps = allAppsSorted,
                onLaunchApp = { app ->
                    showQuickAppsTaskbarInLauncher = false
                    handleLaunchUnifiedApp(app)
                },
                onDismiss = { showQuickAppsTaskbarInLauncher = false }
            )
        }

        // G. EdVanced 1.1 Pro Voice Assistant Overlay (Opened via top-bar AI button or replacing Google Assistant)
        TvVoiceAssistantOverlay(
            visible = showVoiceAssistantOverlay,
            initialWakeQuery = initialWakeWordCommand,
            allApps = allAppsSorted,
            ttsEnabled = aiTtsEnabled,
            onToggleTts = { enabled ->
                aiTtsEnabled = enabled
                prefs.edit().putBoolean("ai_tts_enabled", enabled).apply()
            },
            onLaunchAppByPackageOrName = { targetPkgOrName ->
                val trimmedTarget = targetPkgOrName.trim()
                if (RnaAutonomousAgentController.isExternalBrowserNameOrPackage(trimmedTarget)) {
                    showRnaAiFullScreenApp = false
                    browserTargetUrl = "https://www.google.com"
                    isBrowserTopBarAutoHidden = true
                    tvTab = 2
                } else if (trimmedTarget.equals("rnaai", ignoreCase = true) || trimmedTarget.equals("rna ai", ignoreCase = true) || trimmedTarget.equals(RNAAI_PACKAGE_NAME, ignoreCase = true)) {
                    try {
                        val intent = Intent(context, com.example.RnaAiActivity::class.java).apply {
                            putExtra(com.example.RnaAiActivity.EXTRA_INITIAL_TAB, 0)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        rnaAiInitialSection = 0
                        showRnaAiFullScreenApp = true
                    }
                } else {
                    val matchedApp = unifiedApps.firstOrNull {
                        it.packageName.equals(trimmedTarget, ignoreCase = true)
                    } ?: unifiedApps.firstOrNull {
                        it.title.equals(trimmedTarget, ignoreCase = true) ||
                            it.title.contains(trimmedTarget, ignoreCase = true) ||
                            trimmedTarget.contains(it.title, ignoreCase = true)
                    }
                    if (matchedApp != null && RnaAutonomousAgentController.isExternalBrowserNameOrPackage(matchedApp.packageName)) {
                        showRnaAiFullScreenApp = false
                        browserTargetUrl = "https://www.google.com"
                        isBrowserTopBarAutoHidden = true
                        tvTab = 2
                    } else if (matchedApp != null) {
                        handleLaunchUnifiedApp(matchedApp)
                    } else {
                        val pm = context.packageManager
                        val launchIntent = pm.getLeanbackLaunchIntentForPackage(trimmedTarget)
                            ?: pm.getLaunchIntentForPackage(trimmedTarget)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                        } else {
                            Toast.makeText(
                                context,
                                "App \"$trimmedTarget\" is not installed — opening in RNA Browser",
                                Toast.LENGTH_SHORT
                            ).show()
                            browserTargetUrl = "https://www.google.com/search?q=${Uri.encode(trimmedTarget)}"
                            isBrowserTopBarAutoHidden = true
                            tvTab = 2
                        }
                    }
                }
            },
            onOpenUrlInBrowser = { targetUrl ->
                showRnaAiFullScreenApp = false
                browserTargetUrl = targetUrl
                isBrowserTopBarAutoHidden = true
                tvTab = 2
            },
            onDismiss = {
                showVoiceAssistantOverlay = false
                initialWakeWordCommand = null
                reopenControlPanelIfNeeded()
            },
            onEnterAgentMode = {
                returnToControlPanelTabOnClose = null
                showVoiceAssistantOverlay = false
                initialWakeWordCommand = null
            }
        )

        // H. Android TV Left-Side Updates & More Apps Drawer
        TvUpdatesSideDrawer(
            visible = showUpdatesDrawer,
            onDismiss = {
                showUpdatesDrawer = false
                reopenControlPanelIfNeeded()
            }
        )

        // H2. Android TV Left-Side Devices & RNA Controller Drawer (Wi-Fi, Bluetooth, 4-Digit Phone Remote & Dual Xbox Gamepad)
        TvDevicesSideDrawer(
            visible = showDevicesDrawer,
            onDismiss = {
                showDevicesDrawer = false
                reopenControlPanelIfNeeded()
            }
        )

        // H3. In-Home YouTube Search Bar RNA TV Keyboard Overlay (types directly into the Home screen YouTube search bar)
        if (tvTab == 0 && showYoutubeSearchBar && showYoutubeHomeKeyboard) {
            RnaTvKeyboardOverlay(
                visible = true,
                currentText = youtubeSearchQuery,
                fieldHint = "Search YouTube...",
                showTextPreviewBar = true,
                onTextChange = { updated -> youtubeSearchQuery = updated },
                onCommitText = { chunk -> youtubeSearchQuery += chunk },
                onBackspace = {
                    if (youtubeSearchQuery.isNotEmpty()) {
                        youtubeSearchQuery = youtubeSearchQuery.dropLast(1)
                    }
                },
                onSubmit = {
                    submitHomeYoutubeSearch(youtubeSearchQuery)
                },
                onDismiss = {
                    showYoutubeHomeKeyboard = false
                    if (youtubeSearchQuery.isNotBlank()) {
                        youtubeSearchSubmitBtnFocusRequester.safeRequestFocus()
                    } else {
                        youtubeSearchFocusRequester.safeRequestFocus()
                    }
                }
            )
        }

        // I. Persistent Autonomous Agent Glow & Bottom-Center [X] [Pause] [Mic] [Keyboard] HUD Overlay
        RnaAgentGlowAndHudOverlay(
            performanceMode = performanceModeEnabled
        )

        // J. "Who is watching?" Startup / Boot Profile Selector Overlay (Uses the unified Who is Watching UI with small Edit button underneath)
        if (!showFirstRunSetupWizard && showWhoIsWatchingBootOverlay) {
            TvProfileSwitcherDialog(
                profiles = profiles,
                activeProfile = activeProfile,
                initialCreateNew = false,
                onSelectProfile = { chosen ->
                    switchActiveUserProfile(chosen)
                    showWhoIsWatchingBootOverlay = false
                },
                onSaveProfile = { savedProfile, isNew ->
                    val enrichedProfile = if (isNew) {
                        savedProfile.copy(
                            wallpaperModeType = "STATIC",
                            liveWallpaperUri = "",
                            appIconShape = TvAppIconShape.CIRCLE.id,
                            singleRowHomeApps = false,
                            showRecentAppsRow = true,
                            ambientIdleMinutes = 5
                        )
                    } else {
                        savedProfile
                    }
                    val updatedList = if (isNew) {
                        profiles + enrichedProfile
                    } else {
                        profiles.map { if (it.id == enrichedProfile.id) enrichedProfile else it }
                    }
                    profiles = updatedList
                    saveTvProfiles(prefs, updatedList)
                    switchActiveUserProfile(enrichedProfile)
                    showWhoIsWatchingBootOverlay = false
                },
                onDeleteProfile = { toDelete ->
                    if (profiles.size > 1) {
                        val remaining = profiles.filterNot { it.id == toDelete.id }
                        profiles = remaining
                        saveTvProfiles(prefs, remaining)
                        if (activeProfileId == toDelete.id) {
                            switchActiveUserProfile(remaining.first())
                        }
                    }
                },
                onDismiss = {
                    showWhoIsWatchingBootOverlay = false
                }
            )
        }

        // K. First-Install Onboarding & Setup Wizard
        if (showFirstRunSetupWizard) {
            TvFirstRunSetupWizardOverlay(
                initialProfile = activeProfile,
                selectedAppIconShape = selectedAppIconShape,
                singleRowHomeApps = singleRowHomeApps,
                showRecentAppsRow = showRecentAppsRow,
                splashScreenEnabled = splashScreenEnabled,
                ambientIdleMinutes = ambientIdleMinutes,
                initialReplaceGoogleAssistant = replaceGoogleAssistantEnabled,
                onCompleteSetup = { updatedProfile, chosenShape, singleRow, showRecent, splashOn, ambientMins, askWhoIsWatching, replaceAssistant ->
                    val updatedList = profiles.map { if (it.id == updatedProfile.id) updatedProfile else it }
                    profiles = updatedList
                    saveTvProfiles(prefs, updatedList)
                    switchActiveUserProfile(updatedProfile)
                    selectedAppIconShape = chosenShape
                    onToggleSingleRowHomeApps(singleRow)
                    onToggleRecentAppsRow(showRecent)
                    splashScreenEnabled = splashOn
                    ambientIdleMinutes = ambientMins
                    askWhoIsWatchingOnBoot = askWhoIsWatching
                    replaceGoogleAssistantEnabled = replaceAssistant
                    isAccessibilityEnabled = RnaHomeInterceptorService.isAccessibilityServiceEnabled(context)
                    showAccessibilityBanner = !isAccessibilityEnabled
                    prefs.edit()
                        .putBoolean("tv_first_run_setup_completed_v1", true)
                        .putString("control_app_icon_shape", chosenShape.id)
                        .putBoolean(KEY_SPLASH_SCREEN_ENABLED, splashOn)
                        .putBoolean("splash_screen_enabled", splashOn)
                        .putInt("control_ambient_idle_minutes", ambientMins)
                        .putBoolean("ask_who_is_watching_on_boot", askWhoIsWatching)
                        .putBoolean(RnaHomeInterceptorService.KEY_REPLACE_GOOGLE_ASSISTANT, replaceAssistant)
                        .commit()
                    showFirstRunSetupWizard = false
                }
            )
        }
    }
}

/**
 * Directly opens the normal Android system settings app when the top-bar Settings gear icon is pressed.
 */
private fun launchNormalAndroidSettings(context: Context) {
    val candidateIntents = listOf(
        Intent(Settings.ACTION_SETTINGS),
        Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)
    )
    for (candidate in candidateIntents) {
        try {
            candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(candidate)
            return
        } catch (_: Exception) {
        }
    }
    Toast.makeText(context, "Opening Android System Settings", Toast.LENGTH_SHORT).show()
}

private fun saveAppOrdering(
    item: UnifiedLauncherApp,
    newFavOrder: Int,
    newAllOrder: Int,
    onUpdateRnaApp: (RnaApp) -> Unit,
    onSaveTvCustomization: (TvAppCustomEntity) -> Unit
) {
    if (item.isRnaWebApp && item.rnaApp != null) {
        onUpdateRnaApp(
            item.rnaApp.copy(
                favoriteOrder = newFavOrder,
                allAppsOrder = newAllOrder
            )
        )
    }
    onSaveTvCustomization(
        TvAppCustomEntity(
            packageName = item.packageName,
            customTitle = item.title,
            customLogoUri = item.customLogoUri,
            isFavorite = item.isFavorite,
            favoriteOrder = newFavOrder,
            allAppsOrder = newAllOrder
        )
    )
}

private fun launchUnifiedApp(
    context: Context,
    app: UnifiedLauncherApp,
    onLaunchRnaApp: (RnaApp) -> Unit
) {
    if (app.isRnaWebApp && app.rnaApp != null) {
        onLaunchRnaApp(app.rnaApp)
    } else {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(app.packageName)
            ?: pm.getLeanbackLaunchIntentForPackage(app.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Cannot launch ${app.title}", Toast.LENGTH_SHORT).show()
        }
    }
}

internal fun formatRelativeUsageTime(timestampMillis: Long): String {
    if (timestampMillis <= 0L) return "Ready to resume"
    val diffSec = ((System.currentTimeMillis() - timestampMillis) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60L -> "Just now"
        diffSec < 3600L -> "${diffSec / 60L}m ago"
        diffSec < 86400L -> "${diffSec / 3600L}h ago"
        else -> "${diffSec / 86400L}d ago"
    }
}

@Composable
private fun GoogleTvTopBar(
    activeProfile: TvUserProfile,
    onProfileClick: () -> Unit,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    homeTabFocusRequester: FocusRequester,
    downContentFocusRequester: FocusRequester,
    currentTime: String,
    sleepTimerRemainingSeconds: Int,
    onSearchClick: () -> Unit,
    onAiAssistantClick: () -> Unit,
    onUpdatesClick: () -> Unit,
    onDevicesClick: () -> Unit,
    onPowerHubClick: () -> Unit,
    onHdmiClick: () -> Unit,
    onWallpaperClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val profileFocus = remember { FocusRequester() }
    val searchFocus = remember { FocusRequester() }
    val aiVoiceFocus = remember { FocusRequester() }
    val appsTabFocus = remember { FocusRequester() }
    val browserTabFocus = remember { FocusRequester() }
    val devicesFocus = remember { FocusRequester() }
    val controlsFocus = remember { FocusRequester() }
    val hdmiFocus = remember { FocusRequester() }
    val wallpaperFocus = remember { FocusRequester() }
    val settingsFocus = remember { FocusRequester() }

    fun Modifier.topBarSafeDownRedirect(): Modifier = this.onPreviewKeyEvent { event ->
        if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
            event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_DOWN
        ) {
            downContentFocusRequester.safeRequestFocus()
        } else {
            false
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Google TV Multi-User Profile Pill (100% offline-capable avatar)
            var profileFocused by remember { mutableStateOf(false) }
            val profileScale by animateFloatAsState(
                targetValue = if (profileFocused && !TvPerformanceMode.isEnabled) 1.08f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "ProfileBtnScale"
            )
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = if (profileFocused) Color.White else Color(0x88181B26),
                border = BorderStroke(1.5.dp, if (profileFocused) CyanPrimary else Color(0x44FFFFFF)),
                modifier = Modifier
                    .scale(profileScale)
                    .focusRequester(profileFocus)
                    .focusProperties {
                        right = searchFocus
                    }
                    .topBarSafeDownRedirect()
                    .onFocusChanged { profileFocused = it.isFocused }
                    .clickable { onProfileClick() }
                    .testTag("tv_profile_switcher_button")
            ) {
                Row(
                    modifier = Modifier.padding(start = 5.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TvProfileAvatarImage(
                        avatarUri = activeProfile.avatarUri,
                        contentDescription = activeProfile.name,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                    )
                    Text(
                        text = activeProfile.name.take(10),
                        color = if (profileFocused) Color(0xFF12131A) else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }

            // Capsule 1: Search + EdVanced 1.1 Pro AI Voice Assistant + Home + Apps + Browser
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color(0x77181B26),
                border = BorderStroke(1.dp, Color(0x33FFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    var searchFocused by remember { mutableStateOf(false) }
                    val searchScale by animateFloatAsState(
                        targetValue = if (searchFocused && !TvPerformanceMode.isEnabled) 1.1f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "SearchBtnScale"
                    )
                    Surface(
                        shape = CircleShape,
                        color = if (searchFocused) Color(0xFFF2F3FA) else Color.Transparent,
                        modifier = Modifier
                            .size(40.dp)
                            .scale(searchScale)
                            .focusRequester(searchFocus)
                            .focusProperties {
                                left = profileFocus
                                right = aiVoiceFocus
                            }
                            .topBarSafeDownRedirect()
                            .onFocusChanged { searchFocused = it.isFocused }
                            .clickable { onSearchClick() }
                            .testTag("tv_search_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Apps, Files & Web",
                                tint = if (searchFocused) Color(0xFF12131A) else Color(0xFFE4E1E9),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // AI Voice Assistant Button right near Search ("Powered by EdVanced 1.1 Pro")
                    var aiFocused by remember { mutableStateOf(false) }
                    val aiScale by animateFloatAsState(
                        targetValue = if (aiFocused && !TvPerformanceMode.isEnabled) 1.12f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "AiVoiceBtnScale"
                    )
                    Surface(
                        shape = CircleShape,
                        color = if (aiFocused) Color(0xFFF2F3FA) else CyanPrimary.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, if (aiFocused) Color.White else CyanPrimary.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .size(40.dp)
                            .scale(aiScale)
                            .focusRequester(aiVoiceFocus)
                            .focusProperties {
                                left = searchFocus
                                right = homeTabFocusRequester
                            }
                            .topBarSafeDownRedirect()
                            .onFocusChanged { aiFocused = it.isFocused }
                            .clickable { onAiAssistantClick() }
                            .testTag("tv_ai_assistant_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "AI Voice Assistant (Powered by EdVanced 1.1 Pro)",
                                tint = if (aiFocused) Color(0xFF12131A) else CyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    TvTopTabPill(
                        title = "Home",
                        selected = selectedTab == 0,
                        onClick = { onSelectTab(0) },
                        testTag = "tv_tab_home",
                        modifier = Modifier
                            .focusRequester(homeTabFocusRequester)
                            .focusProperties {
                                left = aiVoiceFocus
                                right = appsTabFocus
                            }
                            .topBarSafeDownRedirect()
                    )

                    TvTopTabPill(
                        title = "Apps",
                        selected = selectedTab == 1,
                        onClick = { onSelectTab(1) },
                        testTag = "tv_tab_apps",
                        modifier = Modifier
                            .focusRequester(appsTabFocus)
                            .focusProperties {
                                left = homeTabFocusRequester
                                right = browserTabFocus
                            }
                            .topBarSafeDownRedirect()
                    )

                    TvTopTabPill(
                        title = "Browser",
                        selected = selectedTab == 2,
                        onClick = { onSelectTab(2) },
                        testTag = "tv_tab_browser",
                        modifier = Modifier
                            .focusRequester(browserTabFocus)
                            .focusProperties {
                                left = appsTabFocus
                                right = devicesFocus
                            }
                            .topBarSafeDownRedirect()
                    )
                }
            }

            // Capsule 2: Devices (Wi-Fi, Bluetooth, RNA Controller), RNA. Settings Controls, Custom TV Input Selector, Wallpapers, Android System Settings
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color(0x77181B26),
                border = BorderStroke(1.dp, Color(0x33FFFFFF))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TvTopIconButton(
                        icon = Icons.Default.Devices,
                        contentDescription = "Devices (Wi-Fi, Bluetooth & RNA Controller)",
                        onClick = onDevicesClick,
                        testTag = "tv_devices_top_button",
                        modifier = Modifier
                            .focusRequester(devicesFocus)
                            .focusProperties {
                                left = browserTabFocus
                                right = controlsFocus
                            }
                            .topBarSafeDownRedirect()
                    )
                    TvTopIconButton(
                        icon = Icons.Default.Tune,
                        contentDescription = "RNA. Settings & Control Center",
                        onClick = onPowerHubClick,
                        testTag = "tv_power_hub_button",
                        modifier = Modifier
                            .focusRequester(controlsFocus)
                            .focusProperties {
                                left = devicesFocus
                                right = hdmiFocus
                            }
                            .topBarSafeDownRedirect()
                    )
                    TvTopIconButton(
                        icon = Icons.Default.SettingsInputHdmi,
                        contentDescription = "TV Input Source Selector (HDMI 1-3, AV, TV)",
                        onClick = onHdmiClick,
                        testTag = "tv_hdmi_button",
                        modifier = Modifier
                            .focusRequester(hdmiFocus)
                            .focusProperties {
                                left = controlsFocus
                                right = wallpaperFocus
                            }
                            .topBarSafeDownRedirect()
                    )
                    TvTopIconButton(
                        icon = Icons.Default.Wallpaper,
                        contentDescription = "Preset & Custom Wallpapers",
                        onClick = onWallpaperClick,
                        testTag = "tv_wallpaper_button",
                        modifier = Modifier
                            .focusRequester(wallpaperFocus)
                            .focusProperties {
                                left = hdmiFocus
                                right = settingsFocus
                            }
                            .topBarSafeDownRedirect()
                    )
                    TvTopIconButton(
                        icon = Icons.Default.Settings,
                        contentDescription = "Android System Settings",
                        onClick = onSettingsClick,
                        testTag = "tv_settings_button",
                        modifier = Modifier
                            .focusRequester(settingsFocus)
                            .focusProperties {
                                left = wallpaperFocus
                            }
                            .topBarSafeDownRedirect()
                    )
                }
            }
        }

        // Right: Active Sleep Timer Pill + "9:02  |  RNA. TV"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (sleepTimerRemainingSeconds > 0) {
                val mins = sleepTimerRemainingSeconds / 60
                val secs = sleepTimerRemainingSeconds % 60
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF282A37),
                    border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Sleep Timer",
                            tint = CyanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", mins, secs),
                            color = CyanPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0x66141620),
                border = BorderStroke(1.dp, Color(0x22FFFFFF))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("tv_brand_header")
                ) {
                    Text(
                        text = currentTime.ifBlank { "9:02" },
                        color = Color(0xFFD6D5E2),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TvTopTabPill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused && !TvPerformanceMode.isEnabled) 1.06f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "TabScale"
    )
    val isHighlighted = selected || isFocused
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = when {
            isFocused -> Color.White
            selected -> Color(0xFFE8EAFA)
            else -> Color.Transparent
        },
        modifier = modifier
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Text(
            text = title,
            color = if (isHighlighted) Color(0xFF12131A) else Color(0xFFE4E1E9),
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun TvTopIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused && !TvPerformanceMode.isEnabled) 1.12f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "TopIconScale"
    )
    Surface(
        shape = CircleShape,
        color = if (isFocused) Color(0xFFF2F3FA) else Color.Transparent,
        modifier = modifier
            .size(38.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (isFocused) Color(0xFF12131A) else Color(0xFFE4E1E9),
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

/**
 * Clean, High-Performance RNA. TV Home Tab:
 * - Upper/middle screen showcases the user's selected 4K static or live wallpaper cleanly.
 * - Optional 'Recent Apps' row (disabled by default; toggled ON/OFF from Control Panel) with smooth D-Pad Up navigation back to the Top Bar.
 * - "Favorites" dock supporting Single-Row layout (default) and 2-Row Grid layout, plus all 19 App Icon Shapes (including Wide 16:9 TV Banners).
 * - Supports live Android TV "Move" Mode with smooth spring item placement animations.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GoogleTvHomeTab(
    showRecentAppsRow: Boolean,
    singleRowHomeApps: Boolean,
    showYoutubeSearchBar: Boolean = false,
    youtubeSearchQuery: String = "",
    onYoutubeSearchQueryChange: (String) -> Unit = {},
    isYoutubeKeyboardOpen: Boolean = false,
    isYoutubeInlineEditing: Boolean = false,
    onYoutubeInlineEditingChange: (Boolean) -> Unit = {},
    onOpenYoutubeRnaKeyboard: () -> Unit = {},
    onSubmitYoutubeSearch: (String) -> Unit = {},
    youtubeSearchFocusRequester: FocusRequester = remember { FocusRequester() },
    youtubeSearchSubmitBtnFocusRequester: FocusRequester = remember { FocusRequester() },
    appIconShape: TvAppIconShape,
    recentApps: List<UnifiedLauncherApp>,
    favoriteApps: List<UnifiedLauncherApp>,
    topBarFocusRequester: FocusRequester,
    firstContentFocusRequester: FocusRequester,
    movingAppKey: String?,
    onMoveApp: (appKey: String, delta: Int) -> Unit,
    onFinishMovingApp: () -> Unit,
    onLaunchApp: (UnifiedLauncherApp) -> Unit,
    onLongPressRecentApp: (UnifiedLauncherApp) -> Unit,
    onLongPressApp: (UnifiedLauncherApp) -> Unit,
    onAddExistingAppsClick: () -> Unit,
    onCreateRnaAppClick: () -> Unit,
    onOpenRnaManagerClick: () -> Unit
) {
    val recentVisible = showRecentAppsRow && recentApps.isNotEmpty()
    val firstRecentItemFocusRequester = remember { FocusRequester() }
    val firstFavAppFocusRequester = remember { FocusRequester() }
    val homeListState = rememberLazyListState()
    val singleRowListState = rememberLazyListState()
    val twoRowGridState = rememberLazyGridState()
    val isWideBanner = appIconShape.isWideBanner
    val youtubeKbActive = showYoutubeSearchBar && isYoutubeKeyboardOpen
    val animatedBottomPadding by animateDpAsState(
        targetValue = if (youtubeKbActive) 256.dp else 22.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "home_youtube_kb_lift"
    )

    LaunchedEffect(youtubeKbActive) {
        if (youtubeKbActive) {
            runCatching { homeListState.animateScrollToItem(0) }
        }
    }

    var hadInitialScrollAnchor by remember { mutableStateOf(favoriteApps.isNotEmpty()) }
    LaunchedEffect(favoriteApps.size) {
        if (favoriteApps.isNotEmpty() && !hadInitialScrollAnchor) {
            hadInitialScrollAnchor = true
            runCatching { singleRowListState.scrollToItem(0) }
            runCatching { twoRowGridState.scrollToItem(0) }
        }
    }

    LazyColumn(
        state = homeListState,
        modifier = Modifier
            .fillMaxSize()
            .testTag("tv_home_list"),
        contentPadding = PaddingValues(
            top = if (showYoutubeSearchBar) 14.dp else 96.dp,
            bottom = animatedBottomPadding
        ),
        verticalArrangement = if (youtubeKbActive) Arrangement.Top else Arrangement.Bottom
    ) {
        // 0. Optional YouTube Search Bar Above Apps (Toggleable in Control Panel -> Home & Layout)
        if (showYoutubeSearchBar) {
            item(key = "section_youtube_search_bar") {
                val downFromYoutubeBar = if (recentVisible) {
                    firstRecentItemFocusRequester
                } else {
                    firstFavAppFocusRequester
                }
                HomeYoutubeSearchBar(
                    query = youtubeSearchQuery,
                    onQueryChange = onYoutubeSearchQueryChange,
                    isKeyboardOpen = isYoutubeKeyboardOpen,
                    isInlineEditing = isYoutubeInlineEditing,
                    onInlineEditingChange = onYoutubeInlineEditingChange,
                    onOpenRnaKeyboard = onOpenYoutubeRnaKeyboard,
                    onSubmitSearch = onSubmitYoutubeSearch,
                    searchFocusRequester = youtubeSearchFocusRequester,
                    firstContentFocusRequester = firstContentFocusRequester,
                    submitBtnFocusRequester = youtubeSearchSubmitBtnFocusRequester,
                    upFocusRequester = topBarFocusRequester,
                    downFocusRequester = downFromYoutubeBar
                )
            }
        }

        // 1. 'Recent Apps' Row (Disabled by default; can be turned ON or OFF from Control Panel)
        if (recentVisible) {
            item(key = "section_recent_apps") {
                val upTargetFromRecent = if (showYoutubeSearchBar) {
                    youtubeSearchFocusRequester
                } else {
                    topBarFocusRequester
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 38.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Recent Apps",
                                color = Color(0xFFF2F3FA),
                                fontSize = 19.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 38.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("tv_recent_apps_row")
                    ) {
                        itemsIndexed(
                            items = recentApps,
                            key = { _, app -> "recent_${app.uniqueKey}" }
                        ) { index, app ->
                            val itemMod = Modifier
                                .then(
                                    if (index == 0) {
                                        if (!showYoutubeSearchBar) {
                                            Modifier
                                                .focusRequester(firstContentFocusRequester)
                                                .focusRequester(firstRecentItemFocusRequester)
                                        } else {
                                            Modifier.focusRequester(firstRecentItemFocusRequester)
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                                .onPreviewKeyEvent { event ->
                                    if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                        when (event.nativeKeyEvent.keyCode) {
                                            KeyEvent.KEYCODE_DPAD_UP -> upTargetFromRecent.safeRequestFocus()
                                            KeyEvent.KEYCODE_DPAD_DOWN -> firstFavAppFocusRequester.safeRequestFocus()
                                            else -> false
                                        }
                                    } else {
                                        false
                                    }
                                }
                            Box(modifier = Modifier.animateItem()) {
                                RecentLauncherAppCard(
                                    app = app,
                                    onClick = { onLaunchApp(app) },
                                    onLongClick = { onLongPressRecentApp(app) },
                                    modifier = itemMod
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. "Favorites" Section Header
        item(key = "section_your_apps") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 38.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "Favorites",
                    color = Color(0xFFF2F3FA),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val upTargetFromTopAppsRow = when {
                recentVisible -> firstRecentItemFocusRequester
                showYoutubeSearchBar -> youtubeSearchFocusRequester
                else -> topBarFocusRequester
            }

            // 3. Single-Row (default) or 2-Row Horizontal Apps Dock
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                if (singleRowHomeApps) {
                    LazyRow(
                        state = singleRowListState,
                        contentPadding = PaddingValues(horizontal = 38.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (isWideBanner) 18.dp else 22.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isWideBanner) 120.dp else 114.dp)
                            .testTag("tv_favorites_row")
                    ) {
                        itemsIndexed(
                            items = favoriteApps,
                            key = { _, app -> app.uniqueKey }
                        ) { index, app ->
                            val isMovingThis = movingAppKey == app.uniqueKey
                            val iconModifier = Modifier
                                .then(
                                    if (index == 0) {
                                        if (!recentVisible && !showYoutubeSearchBar) {
                                            Modifier
                                                .focusRequester(firstContentFocusRequester)
                                                .focusRequester(firstFavAppFocusRequester)
                                        } else {
                                            Modifier.focusRequester(firstFavAppFocusRequester)
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                                .onPreviewKeyEvent { event ->
                                    if (!isMovingThis &&
                                        event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                                        event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP
                                    ) {
                                        upTargetFromTopAppsRow.safeRequestFocus()
                                    } else {
                                        false
                                    }
                                }
                            Box(modifier = Modifier.animateItem()) {
                                CircularGoogleTvAppIcon(
                                    app = app,
                                    appIconShape = appIconShape,
                                    isMoving = isMovingThis,
                                    onMoveLeft = { onMoveApp(app.uniqueKey, -1) },
                                    onMoveRight = { onMoveApp(app.uniqueKey, 1) },
                                    onMoveUp = { onMoveApp(app.uniqueKey, -1) },
                                    onMoveDown = { onMoveApp(app.uniqueKey, 1) },
                                    onFinishMove = onFinishMovingApp,
                                    onClick = { onLaunchApp(app) },
                                    onLongClick = { onLongPressApp(app) },
                                    modifier = iconModifier
                                )
                            }
                        }

                        item(key = "action_add_apps") {
                            val attachFirstFocus = favoriteApps.isEmpty() && !recentVisible && !showYoutubeSearchBar
                            Box(modifier = Modifier.animateItem()) {
                                CircularActionIcon(
                                    title = "Add Apps",
                                    icon = Icons.Default.Add,
                                    accentColor = CyanPrimary,
                                    appIconShape = appIconShape,
                                    onClick = onAddExistingAppsClick,
                                    testTag = "tv_home_end_add_apps",
                                    modifier = Modifier
                                        .then(
                                            when {
                                                attachFirstFocus -> Modifier
                                                    .focusRequester(firstContentFocusRequester)
                                                    .focusRequester(firstFavAppFocusRequester)
                                                favoriteApps.isEmpty() -> Modifier.focusRequester(firstFavAppFocusRequester)
                                                else -> Modifier
                                            }
                                        )
                                        .onPreviewKeyEvent { event ->
                                            if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                                                event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP
                                            ) {
                                                upTargetFromTopAppsRow.safeRequestFocus()
                                            } else {
                                                false
                                            }
                                        }
                                )
                            }
                        }

                        item(key = "action_create_rna") {
                            Box(modifier = Modifier.animateItem()) {
                                CircularActionIcon(
                                    title = "Create App",
                                    icon = Icons.Default.Widgets,
                                    accentColor = VioletSecondary,
                                    appIconShape = appIconShape,
                                    onClick = onCreateRnaAppClick,
                                    testTag = "tv_home_end_create_rna",
                                    modifier = Modifier.onPreviewKeyEvent { event ->
                                        if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                                            event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP
                                        ) {
                                            upTargetFromTopAppsRow.safeRequestFocus()
                                        } else {
                                            false
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    LazyHorizontalGrid(
                        state = twoRowGridState,
                        rows = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 38.dp),
                        horizontalArrangement = Arrangement.spacedBy(if (isWideBanner) 18.dp else 22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isWideBanner) 236.dp else 222.dp)
                            .testTag("tv_favorites_row")
                    ) {
                        itemsIndexed(
                            items = favoriteApps,
                            key = { _, app -> app.uniqueKey }
                        ) { index, app ->
                            val isTopRowInGrid = (index % 2 == 0)
                            val isMovingThis = movingAppKey == app.uniqueKey
                            val iconModifier = Modifier
                                .then(
                                    if (index == 0) {
                                        if (!recentVisible && !showYoutubeSearchBar) {
                                            Modifier
                                                .focusRequester(firstContentFocusRequester)
                                                .focusRequester(firstFavAppFocusRequester)
                                        } else {
                                            Modifier.focusRequester(firstFavAppFocusRequester)
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                                .then(
                                    if (isTopRowInGrid) {
                                        Modifier.onPreviewKeyEvent { event ->
                                            if (!isMovingThis &&
                                                event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                                                event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP
                                            ) {
                                                upTargetFromTopAppsRow.safeRequestFocus()
                                            } else {
                                                false
                                            }
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                            Box(modifier = Modifier.animateItem()) {
                                CircularGoogleTvAppIcon(
                                    app = app,
                                    appIconShape = appIconShape,
                                    isMoving = isMovingThis,
                                    onMoveLeft = { onMoveApp(app.uniqueKey, -2) },
                                    onMoveRight = { onMoveApp(app.uniqueKey, 2) },
                                    onMoveUp = { onMoveApp(app.uniqueKey, -1) },
                                    onMoveDown = { onMoveApp(app.uniqueKey, 1) },
                                    onFinishMove = onFinishMovingApp,
                                    onClick = { onLaunchApp(app) },
                                    onLongClick = { onLongPressApp(app) },
                                    modifier = iconModifier
                                )
                            }
                        }

                        // Action items at the end of the 2-row favorites grid
                        item(key = "action_add_apps") {
                            val attachFirstFocus = favoriteApps.isEmpty() && !recentVisible && !showYoutubeSearchBar
                            val isTopRowAddApps = (favoriteApps.size % 2 == 0)
                            Box(modifier = Modifier.animateItem()) {
                                CircularActionIcon(
                                    title = "Add Apps",
                                    icon = Icons.Default.Add,
                                    accentColor = CyanPrimary,
                                    appIconShape = appIconShape,
                                    onClick = onAddExistingAppsClick,
                                    testTag = "tv_home_end_add_apps",
                                    modifier = Modifier
                                        .then(
                                            when {
                                                attachFirstFocus -> Modifier
                                                    .focusRequester(firstContentFocusRequester)
                                                    .focusRequester(firstFavAppFocusRequester)
                                                favoriteApps.isEmpty() -> Modifier.focusRequester(firstFavAppFocusRequester)
                                                else -> Modifier
                                            }
                                        )
                                        .then(
                                            if (isTopRowAddApps) {
                                                Modifier.onPreviewKeyEvent { event ->
                                                    if (event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                                                        event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP
                                                    ) {
                                                        upTargetFromTopAppsRow.safeRequestFocus()
                                                    } else {
                                                        false
                                                    }
                                                }
                                            } else {
                                                Modifier
                                            }
                                        )
                                )
                            }
                        }

                        item(key = "action_create_rna") {
                            Box(modifier = Modifier.animateItem()) {
                                CircularActionIcon(
                                    title = "Create App",
                                    icon = Icons.Default.Widgets,
                                    accentColor = VioletSecondary,
                                    appIconShape = appIconShape,
                                    onClick = onCreateRnaAppClick,
                                    testTag = "tv_home_end_create_rna"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sleek glassmorphic YouTube Search Bar rendered directly above apps on the Home screen.
 * Move D-Pad UP from apps -> highlight bar -> press OK to open the keyboard right in Home ->
 * press Enter/Search or click the Search icon on the bar to search directly in the YouTube app.
 */
@Composable
private fun HomeYoutubeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isKeyboardOpen: Boolean,
    isInlineEditing: Boolean,
    onInlineEditingChange: (Boolean) -> Unit,
    onOpenRnaKeyboard: () -> Unit,
    onSubmitSearch: (String) -> Unit,
    searchFocusRequester: FocusRequester,
    firstContentFocusRequester: FocusRequester,
    submitBtnFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester
) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val inlineFieldFocusRequester = remember { FocusRequester() }
    val clearBtnFocusRequester = remember { FocusRequester() }
    val micBtnFocusRequester = remember { FocusRequester() }

    var barFocused by remember { mutableStateOf(false) }
    var searchBtnFocused by remember { mutableStateOf(false) }
    var clearBtnFocused by remember { mutableStateOf(false) }
    var micBtnFocused by remember { mutableStateOf(false) }

    var isVoiceListening by remember { mutableStateOf(false) }
    var voicePartialText by remember { mutableStateOf("") }
    var speechRecognizerRef by remember { mutableStateOf<SpeechRecognizer?>(null) }

    val latestOnQueryChange by rememberUpdatedState(onQueryChange)
    val latestOnSubmitSearch by rememberUpdatedState(onSubmitSearch)

    val anyFocused = barFocused || searchBtnFocused || clearBtnFocused || micBtnFocused || isKeyboardOpen || isVoiceListening
    val ytRed = Color(0xFFFF1F1F)
    val ytDeepRed = Color(0xFFD90429)

    val barScale by animateFloatAsState(
        targetValue = if (anyFocused && !TvPerformanceMode.isEnabled) 1.01f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "home_youtube_bar_scale"
    )

    fun stopYoutubeVoiceCapture() {
        isVoiceListening = false
        voicePartialText = ""
        runCatching {
            speechRecognizerRef?.stopListening()
            speechRecognizerRef?.cancel()
            speechRecognizerRef?.destroy()
        }
        speechRecognizerRef = null
    }

    val fallbackVoiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isVoiceListening = false
        voicePartialText = ""
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
        if (!spoken.isNullOrBlank()) {
            latestOnQueryChange(spoken)
            latestOnSubmitSearch(spoken)
        }
    }

    fun startYoutubeVoiceCapture() {
        if (isVoiceListening) {
            stopYoutubeVoiceCapture()
            return
        }
        onInlineEditingChange(false)
        keyboardController?.hide()

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                stopYoutubeVoiceCapture()
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizerRef = recognizer
                isVoiceListening = true
                voicePartialText = ""

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isVoiceListening = true
                    }
                    override fun onBeginningOfSpeech() {
                        isVoiceListening = true
                    }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isVoiceListening = false
                    }
                    override fun onError(error: Int) {
                        val fallbackText = voicePartialText.trim()
                        stopYoutubeVoiceCapture()
                        if (fallbackText.isNotBlank()) {
                            latestOnQueryChange(fallbackText)
                            latestOnSubmitSearch(fallbackText)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val part = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            .orEmpty()
                        if (part.isNotBlank()) {
                            voicePartialText = part
                            latestOnQueryChange(part)
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        val spoken = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?: voicePartialText.trim()
                        stopYoutubeVoiceCapture()
                        if (spoken.isNotBlank()) {
                            latestOnQueryChange(spoken)
                            latestOnSubmitSearch(spoken)
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                recognizer.startListening(intent)
                return
            } catch (_: Exception) {
                stopYoutubeVoiceCapture()
            }
        }

        try {
            val fallbackIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Search YouTube")
            }
            isVoiceListening = true
            fallbackVoiceLauncher.launch(fallbackIntent)
        } catch (_: Exception) {
            isVoiceListening = false
            Toast.makeText(context, "Voice search unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startYoutubeVoiceCapture()
        } else {
            Toast.makeText(context, "Microphone permission required", Toast.LENGTH_SHORT).show()
        }
    }

    fun triggerYoutubeVoiceSearch() {
        val hasAudioPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasAudioPerm) {
            startYoutubeVoiceCapture()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopYoutubeVoiceCapture()
        }
    }

    LaunchedEffect(isInlineEditing) {
        if (isInlineEditing) {
            stopYoutubeVoiceCapture()
            delay(50L)
            openDeviceDefaultKeyboard(
                context = context,
                view = view,
                focusRequester = inlineFieldFocusRequester,
                keyboardController = keyboardController
            )
        }
    }

    fun activateKeyboardInHome() {
        stopYoutubeVoiceCapture()
        if (isRnaKeyboardDefaultIme(context)) {
            onInlineEditingChange(false)
            onOpenRnaKeyboard()
        } else {
            onInlineEditingChange(true)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 38.dp, end = 38.dp, bottom = 14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color.Transparent,
            border = BorderStroke(
                width = if (anyFocused) 2.dp else 1.2.dp,
                brush = if (isVoiceListening) {
                    Brush.horizontalGradient(listOf(Color(0xFF38BDF8), ytRed, Color.White))
                } else if (anyFocused) {
                    Brush.horizontalGradient(listOf(ytRed, Color(0xFFFF7A7A), Color.White))
                } else {
                    Brush.horizontalGradient(listOf(ytRed.copy(alpha = 0.55f), Color(0x38FFFFFF)))
                }
            ),
            shadowElevation = if (anyFocused) 18.dp else 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 620.dp)
                .height(52.dp)
                .graphicsLayer {
                    scaleX = barScale
                    scaleY = barScale
                }
                .testTag("home_youtube_search_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = if (anyFocused) {
                                listOf(
                                    Color(0xF224131E),
                                    Color(0xF0171A2B),
                                    Color(0xF0141827)
                                )
                            } else {
                                listOf(
                                    Color(0xD919131C),
                                    Color(0xD6121623),
                                    Color(0xD4101420)
                                )
                            }
                        )
                    )
                    .padding(start = 9.dp, end = 7.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Iconic YouTube Crimson Play Pill Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Transparent,
                    shadowElevation = if (anyFocused) 6.dp else 2.dp,
                    modifier = Modifier.size(width = 38.dp, height = 27.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(listOf(ytRed, ytDeepRed))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "YouTube",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Interactive Query Input Area (Click/OK activates keyboard directly on Home screen)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (isInlineEditing) {
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            cursorBrush = SolidColor(ytRed),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { onSubmitSearch(query) },
                                onDone = { onSubmitSearch(query) }
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (query.isEmpty()) {
                                        Text(
                                            text = "Search YouTube...",
                                            color = Color(0x99CBD5E1),
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(inlineFieldFocusRequester)
                                .onFocusChanged { barFocused = it.isFocused }
                                .onPreviewKeyEvent { ev ->
                                    val native = ev.nativeKeyEvent
                                    when (native.keyCode) {
                                        KeyEvent.KEYCODE_BACK -> {
                                            if (native.action == KeyEvent.ACTION_UP) {
                                                onInlineEditingChange(false)
                                                keyboardController?.hide()
                                                searchFocusRequester.safeRequestFocus()
                                            }
                                            true
                                        }
                                        KeyEvent.KEYCODE_DPAD_UP -> {
                                            if (native.action == KeyEvent.ACTION_DOWN) {
                                                onInlineEditingChange(false)
                                                keyboardController?.hide()
                                                upFocusRequester.safeRequestFocus()
                                            } else true
                                        }
                                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                                            if (native.action == KeyEvent.ACTION_DOWN) {
                                                onInlineEditingChange(false)
                                                keyboardController?.hide()
                                                downFocusRequester.safeRequestFocus()
                                            } else true
                                        }
                                        KeyEvent.KEYCODE_ENTER,
                                        KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                            if (native.action == KeyEvent.ACTION_UP && query.isNotBlank()) {
                                                onSubmitSearch(query)
                                                true
                                            } else false
                                        }
                                        else -> false
                                    }
                                }
                                .testTag("home_youtube_search_inline_field")
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                                .focusRequester(firstContentFocusRequester)
                                .focusRequester(searchFocusRequester)
                                .onFocusChanged { barFocused = it.isFocused }
                                .onPreviewKeyEvent { ev ->
                                    val native = ev.nativeKeyEvent
                                    val isOk = native.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                                        native.keyCode == KeyEvent.KEYCODE_ENTER ||
                                        native.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
                                        native.keyCode == KeyEvent.KEYCODE_BUTTON_A
                                    if (isOk) {
                                        if (native.action == KeyEvent.ACTION_UP) {
                                            activateKeyboardInHome()
                                        }
                                        true
                                    } else if (native.action == KeyEvent.ACTION_DOWN) {
                                        when (native.keyCode) {
                                            KeyEvent.KEYCODE_DPAD_UP -> upFocusRequester.safeRequestFocus()
                                            KeyEvent.KEYCODE_DPAD_DOWN -> downFocusRequester.safeRequestFocus()
                                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                if (query.isNotEmpty()) {
                                                    clearBtnFocusRequester.safeRequestFocus()
                                                } else {
                                                    micBtnFocusRequester.safeRequestFocus()
                                                }
                                            }
                                            else -> false
                                        }
                                    } else {
                                        false
                                    }
                                }
                                .clickable { activateKeyboardInHome() }
                                .padding(horizontal = 4.dp)
                                .testTag("home_youtube_search_input_trigger"),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val displayLabel = when {
                                isVoiceListening && voicePartialText.isNotBlank() -> voicePartialText
                                isVoiceListening -> "Listening..."
                                query.isNotBlank() -> query
                                else -> "Search YouTube..."
                            }
                            Text(
                                text = displayLabel,
                                color = when {
                                    isVoiceListening -> Color(0xFF38BDF8)
                                    query.isNotBlank() -> Color.White
                                    barFocused -> Color(0xFFE2E8F0)
                                    else -> Color(0xA6C6D0E4)
                                },
                                fontSize = 15.sp,
                                fontWeight = if (query.isBlank() && !isVoiceListening) FontWeight.Medium else FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Clear Button when text is entered
                if (query.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = if (clearBtnFocused) Color.White else Color(0x28FFFFFF),
                        border = BorderStroke(
                            width = if (clearBtnFocused) 2.dp else 1.dp,
                            color = if (clearBtnFocused) ytRed else Color(0x33FFFFFF)
                        ),
                        modifier = Modifier
                            .size(34.dp)
                            .focusRequester(clearBtnFocusRequester)
                            .onFocusChanged { clearBtnFocused = it.isFocused }
                            .onPreviewKeyEvent { ev ->
                                if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                    when (ev.nativeKeyEvent.keyCode) {
                                        KeyEvent.KEYCODE_DPAD_UP -> upFocusRequester.safeRequestFocus()
                                        KeyEvent.KEYCODE_DPAD_DOWN -> downFocusRequester.safeRequestFocus()
                                        KeyEvent.KEYCODE_DPAD_LEFT -> searchFocusRequester.safeRequestFocus()
                                        KeyEvent.KEYCODE_DPAD_RIGHT -> micBtnFocusRequester.safeRequestFocus()
                                        else -> false
                                    }
                                } else false
                            }
                            .clickable {
                                stopYoutubeVoiceCapture()
                                onQueryChange("")
                                searchFocusRequester.safeRequestFocus()
                            }
                            .testTag("home_youtube_search_clear_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear YouTube search",
                                tint = if (clearBtnFocused) Color(0xFF0B0E17) else Color(0xFFE2E8F0),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Voice Search Mic Button (Speaks directly into YouTube search)
                Surface(
                    shape = CircleShape,
                    color = when {
                        micBtnFocused -> Color.White
                        isVoiceListening -> Color(0xFF38BDF8)
                        else -> Color(0x2AFFFFFF)
                    },
                    border = BorderStroke(
                        width = if (micBtnFocused || isVoiceListening) 2.dp else 1.dp,
                        color = when {
                            micBtnFocused -> ytRed
                            isVoiceListening -> Color.White
                            else -> Color(0x33FFFFFF)
                        }
                    ),
                    modifier = Modifier
                        .size(38.dp)
                        .focusRequester(micBtnFocusRequester)
                        .onFocusChanged { micBtnFocused = it.isFocused }
                        .onPreviewKeyEvent { ev ->
                            if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                when (ev.nativeKeyEvent.keyCode) {
                                    KeyEvent.KEYCODE_DPAD_UP -> upFocusRequester.safeRequestFocus()
                                    KeyEvent.KEYCODE_DPAD_DOWN -> downFocusRequester.safeRequestFocus()
                                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                                        if (query.isNotEmpty()) {
                                            clearBtnFocusRequester.safeRequestFocus()
                                        } else {
                                            searchFocusRequester.safeRequestFocus()
                                        }
                                    }
                                    KeyEvent.KEYCODE_DPAD_RIGHT -> submitBtnFocusRequester.safeRequestFocus()
                                    else -> false
                                }
                            } else false
                        }
                        .clickable { triggerYoutubeVoiceSearch() }
                        .testTag("home_youtube_search_voice_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isVoiceListening) Icons.Default.GraphicEq else Icons.Default.Mic,
                            contentDescription = "Voice search YouTube",
                            tint = when {
                                micBtnFocused -> ytDeepRed
                                isVoiceListening -> Color(0xFF0B0E17)
                                else -> Color.White
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Trailing Circular Search Action Button (Searches directly in YouTube App)
                Surface(
                    shape = CircleShape,
                    color = when {
                        searchBtnFocused -> Color.White
                        query.isNotBlank() || anyFocused -> ytRed
                        else -> Color(0x2AFFFFFF)
                    },
                    border = BorderStroke(
                        width = if (searchBtnFocused) 2.dp else 1.dp,
                        color = when {
                            searchBtnFocused -> ytRed
                            query.isNotBlank() || anyFocused -> Color(0x88FFFFFF)
                            else -> Color(0x33FFFFFF)
                        }
                    ),
                    modifier = Modifier
                        .size(38.dp)
                        .focusRequester(submitBtnFocusRequester)
                        .onFocusChanged { searchBtnFocused = it.isFocused }
                        .onPreviewKeyEvent { ev ->
                            if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                                when (ev.nativeKeyEvent.keyCode) {
                                    KeyEvent.KEYCODE_DPAD_UP -> upFocusRequester.safeRequestFocus()
                                    KeyEvent.KEYCODE_DPAD_DOWN -> downFocusRequester.safeRequestFocus()
                                    KeyEvent.KEYCODE_DPAD_LEFT -> micBtnFocusRequester.safeRequestFocus()
                                    else -> false
                                }
                            } else false
                        }
                        .clickable {
                            stopYoutubeVoiceCapture()
                            onSubmitSearch(query)
                        }
                        .testTag("home_youtube_search_submit_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search in YouTube",
                            tint = if (searchBtnFocused) ytDeepRed else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun launchYoutubeSearch(
    context: Context,
    rawQuery: String,
    onFallbackOpenInBrowser: (String) -> Unit
) {
    val query = rawQuery.trim()
    val encoded = Uri.encode(query)
    val ytUrl = if (query.isEmpty()) {
        "https://www.youtube.com"
    } else {
        "https://www.youtube.com/results?search_query=$encoded"
    }
    val pm = context.packageManager
    val ytPackages = listOf(
        "com.google.android.youtube.tv",
        "com.google.android.youtube",
        "com.liskovsoft.smarttubetv.beta",
        "com.teamsmart.videomanager.tv"
    )

    if (query.isEmpty()) {
        for (pkg in ytPackages) {
            val launch = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (runCatching { context.startActivity(launch) }.isSuccess) return
            }
        }
        onFallbackOpenInBrowser(ytUrl)
        return
    }

    for (pkg in ytPackages) {
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(ytUrl)).apply {
            setPackage(pkg)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (runCatching { context.startActivity(viewIntent) }.isSuccess) return

        val searchIntent = Intent(Intent.ACTION_SEARCH).apply {
            setPackage(pkg)
            putExtra(android.app.SearchManager.QUERY, query)
            putExtra("query", query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (runCatching { context.startActivity(searchIntent) }.isSuccess) return
    }

    Toast.makeText(context, "YouTube app not found — opening in RNA. Browser", Toast.LENGTH_SHORT).show()
    onFallbackOpenInBrowser(ytUrl)
}

/**
 * Compact Quick-Resume Card for the 'Recent Apps' row on the main launcher screen.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentLauncherAppCard(
    app: UnifiedLauncherApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused && !TvPerformanceMode.isEnabled) 1.07f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "RecentAppCardScale"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isFocused) Color(0xEE262B40) else Color(0xCC181B26),
        border = BorderStroke(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = if (isFocused) Color.White else Color(0x38FFFFFF)
        ),
        shadowElevation = if (isFocused) 14.dp else 6.dp,
        modifier = modifier
            .width(214.dp)
            .height(64.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .tvPressAndHoldClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("recent_app_card_${app.uniqueKey}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF222534),
                border = BorderStroke(1.dp, if (app.isRnaWebApp) CyanPrimary else Color(0x44FFFFFF)),
                modifier = Modifier.size(44.dp)
            ) {
                AppAvatarContent(app = app)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = app.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (app.isRnaWebApp) "RNA. Web" else "App",
                        color = if (app.isRnaWebApp) CyanPrimary else VioletSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• ${formatRelativeUsageTime(app.lastUsedTimestamp)}",
                        color = Color(0xFFB5B6C6),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = if (isFocused) CyanPrimary else Color(0x33FFFFFF),
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume ${app.title}",
                        tint = if (isFocused) Color(0xFF12131A) else Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GoogleTvAppsTab(
    allApps: List<UnifiedLauncherApp>,
    appIconShape: TvAppIconShape,
    topBarFocusRequester: FocusRequester,
    firstContentFocusRequester: FocusRequester,
    movingAppKey: String?,
    onMoveApp: (appKey: String, delta: Int) -> Unit,
    onFinishMovingApp: () -> Unit,
    onLaunchApp: (UnifiedLauncherApp) -> Unit,
    onLongPressApp: (UnifiedLauncherApp) -> Unit,
    onAddExistingAppsClick: () -> Unit,
    onCreateRnaAppClick: () -> Unit,
    onOpenRnaManagerClick: () -> Unit
) {
    // 5 wide 16:9 TV banners per row when Rectangle shape is selected; 7 apps per row for standard shapes
    val columnsPerRow = if (appIconShape.isWideBanner) 5 else 7
    val rows = remember(allApps, columnsPerRow) { allApps.chunked(columnsPerRow) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("tv_all_apps_list"),
            contentPadding = PaddingValues(horizontal = 38.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = "All Apps",
                    color = Color(0xFFF2F3FA),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            itemsIndexed(rows) { rowIndex, rowApps ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (slotIndex in 0 until columnsPerRow) {
                        val app = rowApps.getOrNull(slotIndex)
                        if (app != null) {
                            val isMovingThis = movingAppKey == app.uniqueKey
                            val mod = Modifier
                                .then(
                                    if (rowIndex == 0 && slotIndex == 0) {
                                        Modifier.focusRequester(firstContentFocusRequester)
                                    } else {
                                        Modifier
                                    }
                                )
                                .then(
                                    if (rowIndex == 0) {
                                        Modifier.onPreviewKeyEvent { event ->
                                            if (!isMovingThis &&
                                                event.nativeKeyEvent.action == KeyEvent.ACTION_DOWN &&
                                                event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP
                                            ) {
                                                topBarFocusRequester.safeRequestFocus()
                                            } else {
                                                false
                                            }
                                        }
                                    } else {
                                        Modifier
                                    }
                                )
                            CircularGoogleTvAppIcon(
                                app = app,
                                appIconShape = appIconShape,
                                isMoving = isMovingThis,
                                onMoveLeft = { onMoveApp(app.uniqueKey, -1) },
                                onMoveRight = { onMoveApp(app.uniqueKey, 1) },
                                onMoveUp = { onMoveApp(app.uniqueKey, -columnsPerRow) },
                                onMoveDown = { onMoveApp(app.uniqueKey, columnsPerRow) },
                                onFinishMove = onFinishMovingApp,
                                onClick = { onLaunchApp(app) },
                                onLongClick = { onLongPressApp(app) },
                                modifier = mod
                            )
                        } else {
                            Spacer(modifier = Modifier.width(if (appIconShape.isWideBanner) 156.dp else 88.dp))
                        }
                    }
                }
            }

            item {
                val attachFirstFocus = allApps.isEmpty()
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0x28FFFFFF))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "App Management & RNA. Container Builder",
                    color = CyanPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Button(
                        onClick = onAddExistingAppsClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF282A37),
                            contentColor = Color(0xFFE4E1E9)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .weight(1f)
                            .then(if (attachFirstFocus) Modifier.focusRequester(firstContentFocusRequester) else Modifier)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = CyanPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Existing Apps to Home", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onCreateRnaAppClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = Color(0xFF1B2654)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Widgets, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create New RNA. App", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CircularGoogleTvAppIcon(
    app: UnifiedLauncherApp,
    appIconShape: TvAppIconShape = TvAppIconShape.CIRCLE,
    isMoving: Boolean = false,
    onMoveLeft: () -> Unit = {},
    onMoveRight: () -> Unit = {},
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    onFinishMove: () -> Unit = {},
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val itemFocusRequester = remember { FocusRequester() }
    val isWideBanner = appIconShape.isWideBanner
    val resolvedShape = remember(appIconShape) { resolveComposeShapeForAppIcon(appIconShape) }

    // Keep focus locked onto the app while it is being moved across positions!
    LaunchedEffect(isMoving) {
        if (isMoving) {
            itemFocusRequester.safeRequestFocus()
        }
    }

    val scale by animateFloatAsState(
        targetValue = when {
            TvPerformanceMode.isEnabled -> 1.0f
            isMoving -> 1.15f
            isFocused -> if (isWideBanner) 1.08f else 1.13f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "CircularIconScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(if (isWideBanner) 156.dp else 88.dp)
            .focusRequester(itemFocusRequester)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .scale(scale)
            .onPreviewKeyEvent { event ->
                if (!isMoving) {
                    false
                } else {
                    val native = event.nativeKeyEvent
                    if (native.action == KeyEvent.ACTION_DOWN) {
                        when (native.keyCode) {
                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                onMoveLeft()
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                onMoveRight()
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_UP -> {
                                onMoveUp()
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                onMoveDown()
                                true
                            }
                            KeyEvent.KEYCODE_DPAD_CENTER,
                            KeyEvent.KEYCODE_ENTER,
                            KeyEvent.KEYCODE_NUMPAD_ENTER,
                            KeyEvent.KEYCODE_BUTTON_A,
                            KeyEvent.KEYCODE_BACK,
                            KeyEvent.KEYCODE_ESCAPE -> {
                                onFinishMove()
                                true
                            }
                            else -> true
                        }
                    } else {
                        true
                    }
                }
            }
            .tvPressAndHoldClickable(
                onClick = {
                    if (isMoving) onFinishMove() else onClick()
                },
                onLongClick = {
                    if (isMoving) onFinishMove() else onLongClick()
                }
            )
            .testTag("circular_app_icon_${app.uniqueKey}")
    ) {
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.runtime.key(appIconShape.id) {
                Surface(
                    shape = resolvedShape,
                    color = Color(0xFF222534),
                    border = BorderStroke(
                        width = when {
                            isMoving -> 3.5.dp
                            isFocused -> 3.dp
                            else -> 1.dp
                        },
                        color = when {
                            isMoving -> AmberAccent
                            isFocused -> Color.White
                            else -> Color(0x38FFFFFF)
                        }
                    ),
                    shadowElevation = when {
                        isMoving -> 22.dp
                        isFocused && !TvPerformanceMode.isEnabled -> 18.dp
                        else -> 6.dp
                    },
                    modifier = if (isWideBanner) {
                        Modifier.size(width = 148.dp, height = 83.dp)
                    } else {
                        Modifier.size(72.dp)
                    }
                ) {
                    AppAvatarContent(
                        app = app,
                        appIconShape = appIconShape
                    )
                }
            }

            // Directional Move Overlay Pill when in Android TV Move Mode
            if (isMoving) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = AmberAccent,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                ) {
                    Text(
                        text = "◀ MOVE ▶",
                        color = Color(0xFF12131A),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isMoving) "◀ ${app.title} ▶" else app.title,
            color = when {
                isMoving -> AmberAccent
                isFocused -> Color.White
                else -> Color(0xFFE2E2EC)
            },
            fontSize = 11.5.sp,
            fontWeight = if (isFocused || isMoving) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AppAvatarContent(
    app: UnifiedLauncherApp,
    appIconShape: TvAppIconShape = TvAppIconShape.CIRCLE
) {
    val clipShape = remember(appIconShape) { resolveComposeShapeForAppIcon(appIconShape) }
    val isWideBanner = appIconShape.isWideBanner

    // If Wide 16:9 TV Banner (Rectangle shape) is active and no custom logo override is set,
    // render the app's native Android TV Banner (`loadBanner(pm)`) first!
    if (isWideBanner && app.customLogoUri.isNullOrBlank()) {
        val bannerBitmap = remember(app.packageName, app.cachedBannerBitmap) {
            app.cachedBannerBitmap
                ?: TvAppRamCache.bannerBitmaps[app.packageName]
                ?: app.nativeBanner?.let { bannerDrawable ->
                    try {
                        bannerDrawable.toBitmap(320, 180).asImageBitmap().also { decoded ->
                            TvAppRamCache.bannerBitmaps[app.packageName] = decoded
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
        }
        if (bannerBitmap != null) {
            Image(
                bitmap = bannerBitmap,
                contentDescription = app.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(clipShape)
            )
            return
        }

        // Styled 16:9 Android TV Banner layout for apps without a dedicated TV banner drawable
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .clip(clipShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF141929), Color(0xFF232B45))
                    )
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    AppSquareOrCircleIconContent(app = app, clipShape = RoundedCornerShape(12.dp))
                }
                Text(
                    text = app.title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        }
        return
    }

    AppSquareOrCircleIconContent(app = app, clipShape = clipShape)
}

@Composable
private fun AppSquareOrCircleIconContent(
    app: UnifiedLauncherApp,
    clipShape: androidx.compose.ui.graphics.Shape
) {
    // 1. Check in-memory RAM cache first for zero-latency Frame 1 rendering
    val ramCachedBitmap = remember(app.uniqueKey, app.customLogoUri, app.cachedBitmap) {
        app.cachedBitmap
            ?: app.customLogoUri?.let { TvAppRamCache.iconBitmaps[it] }
            ?: TvAppRamCache.iconBitmaps[app.packageName]
            ?: app.customLogoUri?.let { uri ->
                decodeDataUriToImageBitmap(uri)?.also { decoded ->
                    TvAppRamCache.iconBitmaps[uri] = decoded
                }
            }
    }

    when {
        ramCachedBitmap != null -> {
            Image(
                bitmap = ramCachedBitmap,
                contentDescription = app.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(clipShape)
            )
        }
        !app.customLogoUri.isNullOrBlank() -> {
            AsyncImage(
                model = app.customLogoUri,
                contentDescription = app.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(clipShape)
            )
        }
        app.packageName == RNA_FILE_EXPLORER_PACKAGE_NAME -> {
            Image(
                painter = painterResource(id = R.drawable.ic_file_explorer_logo),
                contentDescription = "RNA File Explorer",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(clipShape)
            )
        }
        app.packageName == RNAAI_PACKAGE_NAME -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF0D1322), Color(0xFF172542))
                        )
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground_img),
                    contentDescription = "RNA AI",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(clipShape)
                )
            }
        }
        app.nativeIcon != null -> {
            val bmp = remember(app.packageName) {
                TvAppRamCache.iconBitmaps[app.packageName] ?: try {
                    renderFullBleedAppIconBitmap(app.nativeIcon, 144).also { decoded ->
                        TvAppRamCache.iconBitmaps[app.packageName] = decoded
                    }
                } catch (_: Exception) {
                    null
                }
            }
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = app.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(clipShape)
                )
            }
        }
        else -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF282D46), Color(0xFF362B48))
                        )
                    )
            ) {
                Text(
                    text = app.title.take(2).uppercase(),
                    color = CyanPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
private fun CircularActionIcon(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    appIconShape: TvAppIconShape = TvAppIconShape.CIRCLE,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val isWideBanner = appIconShape.isWideBanner
    val resolvedShape = remember(appIconShape) { resolveComposeShapeForAppIcon(appIconShape) }

    val scale by animateFloatAsState(
        targetValue = when {
            TvPerformanceMode.isEnabled -> 1.0f
            isFocused -> if (isWideBanner) 1.08f else 1.13f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ActionIconScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(if (isWideBanner) 156.dp else 88.dp)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .scale(scale)
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Surface(
            shape = resolvedShape,
            color = if (isFocused) accentColor else Color(0xCC222534),
            border = BorderStroke(
                width = if (isFocused) 2.5.dp else 1.5.dp,
                color = if (isFocused) Color.White else accentColor.copy(alpha = 0.6f)
            ),
            shadowElevation = if (isFocused && !TvPerformanceMode.isEnabled) 14.dp else 4.dp,
            modifier = if (isWideBanner) {
                Modifier.size(width = 148.dp, height = 83.dp)
            } else {
                Modifier.size(72.dp)
            }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isFocused) Color(0xFF12131A) else accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            color = if (isFocused) Color.White else Color(0xFFE2E2EC),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

/**
 * Wallpapers Gallery Dialog with Static & Live Pill Buttons:
 * - Header is simply "Wallpapers" with no subtext underneath.
 * - Under "Wallpapers", two small pill-shaped buttons switch between "Static" and "Live".
 * - Static: 3-per-row 4K static wallpapers + "Choose Custom Image" (opens RNA File Explorer in Images category) + "Search More Wallpapers" (opens TV Browser) + "Load More Wallpapers (+5)".
 * - Live: 3-per-row 360p live video preview cards (no subtext under live wallpapers) + user-uploaded videos + "Upload / Pick Video" (opens RNA File Explorer in Video category) + "Download Live Wallpapers" (opens TV Browser; no server load-more button).
 */
@Composable
private fun WallpaperCustomizerDialog(
    activeWallpaperUri: String,
    activeLiveWallpaperUri: String,
    wallpaperModeType: String,
    liveWallpaperPreviewEnabled: Boolean = false,
    onSelectWallpaper: (String) -> Unit,
    onSelectLiveWallpaper: (String) -> Unit,
    onOpenFileExplorerForStatic: () -> Unit,
    onOpenFileExplorerForLive: () -> Unit,
    onSearchStaticWallpapersInBrowser: () -> Unit,
    onDownloadLiveWallpapersInBrowser: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE) }
    var animatingSelectionUri by remember { mutableStateOf<String?>(null) }
    var selectedTabMode by remember {
        mutableStateOf(if (wallpaperModeType.equals("LIVE", ignoreCase = true)) "LIVE" else "STATIC")
    }

    val downloadedDir = remember {
        File(context.filesDir, "downloaded_wallpapers").apply { mkdirs() }
    }
    val userLiveWallpapersDir = remember {
        File(context.filesDir, "live_wallpapers").apply { mkdirs() }
    }

    var downloadedFiles by remember {
        mutableStateOf(
            downloadedDir.listFiles()
                ?.filter { it.isFile && it.length() > 512L }
                ?.sortedBy { it.name }
                ?: emptyList()
        )
    }

    var userLiveVideoFiles by remember {
        mutableStateOf(
            userLiveWallpapersDir.listFiles()
                ?.filter { it.isFile && it.length() > 512L && it.name !in BUNDLED_LIVE_WALLPAPER_FILES }
                ?.sortedBy { it.name }
                ?: emptyList()
        )
    }

    var nextWallpaperBatchStart by remember {
        mutableIntStateOf(prefs.getInt("next_wallpaper_batch_start", 1))
    }
    var isDownloadingBatch by remember { mutableStateOf(false) }
    var loadMoreStatusMessage by remember { mutableStateOf<String?>(null) }
    var noMoreAvailable by remember { mutableStateOf(false) }

    val allStaticWallpapers = remember(downloadedFiles) {
        val dlUris = downloadedFiles.map { "file://${it.absolutePath}" }
        PRESET_WALLPAPER_ASSET_URIS + dlUris
    }

    val allLiveWallpapers = remember(userLiveVideoFiles) {
        val userItems = userLiveVideoFiles.mapIndexed { idx, file ->
            LiveWallpaperItem(
                id = "user_live_${idx}_${file.name}",
                uri = "file://${file.absolutePath}",
                isBundled = false
            )
        }
        BUNDLED_LIVE_WALLPAPERS + userItems
    }

    fun deleteDownloadedWallpaper(uri: String) {
        val path = uri.removePrefix("file://")
        val targetFile = File(path)
        if (targetFile.exists()) {
            targetFile.delete()
        }
        downloadedFiles = downloadedDir.listFiles()
            ?.filter { it.isFile && it.length() > 512L }
            ?.sortedBy { it.name }
            ?: emptyList()
        Toast.makeText(context, "Deleted downloaded wallpaper", Toast.LENGTH_SHORT).show()
    }

    fun deleteUserLiveWallpaper(uri: String) {
        val path = uri.removePrefix("file://")
        val targetFile = File(path)
        if (targetFile.exists()) {
            targetFile.delete()
        }
        userLiveVideoFiles = userLiveWallpapersDir.listFiles()
            ?.filter { it.isFile && it.length() > 512L && it.name !in BUNDLED_LIVE_WALLPAPER_FILES }
            ?.sortedBy { it.name }
            ?: emptyList()
        Toast.makeText(context, "Deleted live wallpaper video", Toast.LENGTH_SHORT).show()
    }

    fun loadMoreOnlineWallpapers() {
        if (isDownloadingBatch) return
        isDownloadingBatch = true
        loadMoreStatusMessage = null
        val startIdx = nextWallpaperBatchStart
        val endIdx = startIdx + 4

        scope.launch {
            var downloadedInThisBatch = 0
            withContext(Dispatchers.IO) {
                for (i in startIdx..endIdx) {
                    val destFile = File(downloadedDir, String.format(Locale.US, "wp_online_%03d.png", i))
                    if (destFile.exists() && destFile.length() > 1024L) {
                        downloadedInThisBatch++
                        continue
                    }
                    val candidateUrls = listOf(
                        "https://edvanced.ct.ws/wallpapers/$i.png",
                        "https://EdVanced.ct.ws/wallpapers/$i.png",
                        "https://edvanced.ct.ws/wallpapers/$i.jpg"
                    )
                    var saved = false
                    for (urlStr in candidateUrls) {
                        if (saved) break
                        try {
                            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                                requestMethod = "GET"
                                instanceFollowRedirects = true
                                connectTimeout = 10_000
                                readTimeout = 20_000
                                setRequestProperty("User-Agent", "RNA-TV-Wallpaper-Loader/2.5")
                            }
                            val code = conn.responseCode
                            val cType = (conn.contentType ?: "").lowercase()
                            if (code in 200..299 && !cType.contains("text/html")) {
                                val tempFile = File(downloadedDir, "tmp_$i.png")
                                conn.inputStream.use { input ->
                                    FileOutputStream(tempFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                if (tempFile.length() > 1024L) {
                                    tempFile.renameTo(destFile)
                                    saved = true
                                    downloadedInThisBatch++
                                } else {
                                    tempFile.delete()
                                }
                            }
                            conn.disconnect()
                        } catch (_: Exception) {
                        }
                    }
                }
            }

            downloadedFiles = downloadedDir.listFiles()
                ?.filter { it.isFile && it.length() > 512L }
                ?.sortedBy { it.name }
                ?: emptyList()

            isDownloadingBatch = false
            if (downloadedInThisBatch > 0) {
                nextWallpaperBatchStart = endIdx + 1
                prefs.edit().putInt("next_wallpaper_batch_start", nextWallpaperBatchStart).apply()
                noMoreAvailable = false
                loadMoreStatusMessage = "Downloaded $downloadedInThisBatch new wallpaper(s)"
            } else {
                noMoreAvailable = true
                loadMoreStatusMessage = "No more available"
            }
        }
    }

    val staticPillFocus = remember { FocusRequester() }
    val livePillFocus = remember { FocusRequester() }
    val firstStaticCardFocus = remember { FocusRequester() }
    val firstLiveCardFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(80L)
        if (selectedTabMode == "LIVE") {
            if (!firstLiveCardFocus.safeRequestFocus()) {
                livePillFocus.safeRequestFocus()
            }
        } else {
            if (!firstStaticCardFocus.safeRequestFocus()) {
                staticPillFocus.safeRequestFocus()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF2141620)),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .fillMaxHeight(0.88f)
                .testTag("wallpaper_customizer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 22.dp)
            ) {
                // Header Row: "Wallpapers" (no subtext) + Small Pill Buttons "Static" and "Live" underneath
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = CyanPrimary.copy(alpha = 0.18f),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Wallpaper,
                                        contentDescription = null,
                                        tint = CyanPrimary,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Wallpapers",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        // Two small pill-shaped buttons: Static and Live (with explicit D-Pad Down into grid)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var staticPillFocused by remember { mutableStateOf(false) }
                            val isStaticActive = selectedTabMode == "STATIC"
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = when {
                                    staticPillFocused -> Color.White
                                    isStaticActive -> CyanPrimary
                                    else -> Color(0xFF222534)
                                },
                                border = BorderStroke(
                                    width = if (staticPillFocused) 2.5.dp else 1.dp,
                                    color = if (staticPillFocused || isStaticActive) Color.White else Color(0x38FFFFFF)
                                ),
                                modifier = Modifier
                                    .focusRequester(staticPillFocus)
                                    .onFocusChanged { staticPillFocused = it.isFocused }
                                    .onPreviewKeyEvent { ev ->
                                        if (ev.type == KeyEventType.KeyDown && ev.key == Key.DirectionDown) {
                                            if (selectedTabMode == "LIVE") firstLiveCardFocus.safeRequestFocus()
                                            else firstStaticCardFocus.safeRequestFocus()
                                        } else false
                                    }
                                    .clickable {
                                        selectedTabMode = "STATIC"
                                        scope.launch {
                                            delay(60L)
                                            firstStaticCardFocus.safeRequestFocus()
                                        }
                                    }
                                    .testTag("wallpapers_static_pill_btn")
                            ) {
                                Text(
                                    text = "Static",
                                    color = if (staticPillFocused || isStaticActive) Color(0xFF12131A) else Color(0xFFE4E1E9),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                                )
                            }

                            var livePillFocused by remember { mutableStateOf(false) }
                            val isLiveActive = selectedTabMode == "LIVE"
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = when {
                                    livePillFocused -> Color.White
                                    isLiveActive -> CyanPrimary
                                    else -> Color(0xFF222534)
                                },
                                border = BorderStroke(
                                    width = if (livePillFocused) 2.5.dp else 1.dp,
                                    color = if (livePillFocused || isLiveActive) Color.White else Color(0x38FFFFFF)
                                ),
                                modifier = Modifier
                                    .focusRequester(livePillFocus)
                                    .onFocusChanged { livePillFocused = it.isFocused }
                                    .onPreviewKeyEvent { ev ->
                                        if (ev.type == KeyEventType.KeyDown && ev.key == Key.DirectionDown) {
                                            if (selectedTabMode == "LIVE") firstLiveCardFocus.safeRequestFocus()
                                            else firstStaticCardFocus.safeRequestFocus()
                                        } else false
                                    }
                                    .clickable {
                                        selectedTabMode = "LIVE"
                                        scope.launch {
                                            delay(80L)
                                            firstLiveCardFocus.safeRequestFocus()
                                        }
                                    }
                                    .testTag("wallpapers_live_pill_btn")
                            ) {
                                Text(
                                    text = "Live",
                                    color = if (livePillFocused || isLiveActive) Color(0xFF12131A) else Color(0xFFE4E1E9),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (selectedTabMode == "STATIC") {
                            var searchStaticFocused by remember { mutableStateOf(false) }
                            OutlinedButton(
                                onClick = onSearchStaticWallpapersInBrowser,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (searchStaticFocused) Color.White else Color.Transparent,
                                    contentColor = if (searchStaticFocused) Color(0xFF12131A) else Color.White
                                ),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(if (searchStaticFocused) 2.5.dp else 1.dp, if (searchStaticFocused) Color.White else CyanPrimary),
                                modifier = Modifier
                                    .onFocusChanged { searchStaticFocused = it.isFocused }
                                    .testTag("search_more_static_wallpapers_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = if (searchStaticFocused) Color(0xFF12131A) else CyanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Search More Wallpapers",
                                    color = if (searchStaticFocused) Color(0xFF12131A) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            var pickPhotoFocused by remember { mutableStateOf(false) }
                            Button(
                                onClick = onOpenFileExplorerForStatic,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (pickPhotoFocused) Color.White else CyanPrimary,
                                    contentColor = Color(0xFF12131A)
                                ),
                                border = if (pickPhotoFocused) BorderStroke(2.5.dp, CyanPrimary) else null,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .onFocusChanged { pickPhotoFocused = it.isFocused }
                                    .testTag("pick_custom_wallpaper_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose Custom Photo", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            var dlHeaderFocused by remember { mutableStateOf(false) }
                            OutlinedButton(
                                onClick = onDownloadLiveWallpapersInBrowser,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (dlHeaderFocused) Color.White else Color.Transparent,
                                    contentColor = if (dlHeaderFocused) Color(0xFF12131A) else Color.White
                                ),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(if (dlHeaderFocused) 2.5.dp else 1.dp, if (dlHeaderFocused) Color.White else CyanPrimary),
                                modifier = Modifier
                                    .onFocusChanged { dlHeaderFocused = it.isFocused }
                                    .testTag("download_live_wallpapers_browser_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = if (dlHeaderFocused) Color(0xFF12131A) else CyanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Download Live Wallpapers",
                                    color = if (dlHeaderFocused) Color(0xFF12131A) else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            var uploadLiveHeaderFocused by remember { mutableStateOf(false) }
                            Button(
                                onClick = onOpenFileExplorerForLive,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uploadLiveHeaderFocused) Color.White else CyanPrimary,
                                    contentColor = Color(0xFF12131A)
                                ),
                                border = if (uploadLiveHeaderFocused) BorderStroke(2.5.dp, CyanPrimary) else null,
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier
                                    .onFocusChanged { uploadLiveHeaderFocused = it.isFocused }
                                    .testTag("upload_custom_live_wallpaper_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upload / Pick Video", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        var closeWpFocused by remember { mutableStateOf(false) }
                        Surface(
                            shape = CircleShape,
                            color = if (closeWpFocused) Color.White else Color(0xFF23273A),
                            border = BorderStroke(if (closeWpFocused) 2.dp else 1.dp, if (closeWpFocused) CyanPrimary else Color(0x33FFFFFF)),
                            modifier = Modifier
                                .size(38.dp)
                                .onFocusChanged { closeWpFocused = it.isFocused }
                                .clickable(onClick = onDismiss)
                                .testTag("close_wallpapers_dialog_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Wallpapers",
                                    tint = if (closeWpFocused) Color(0xFF12131A) else Color(0xFFE4E1E9),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTabMode == "STATIC") {
                    // 3 Static Wallpapers Per Row Vertical Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("preset_wallpapers_grid"),
                        contentPadding = PaddingValues(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(
                            items = allStaticWallpapers,
                            key = { _, uri -> uri }
                        ) { index, wallpaperUri ->
                            val isSelected = wallpaperModeType != "LIVE" && activeWallpaperUri == wallpaperUri
                            val isTriggered = animatingSelectionUri == wallpaperUri
                            val isDownloadedWallpaper = !wallpaperUri.startsWith("file:///android_asset/")
                            var isFocused by remember { mutableStateOf(false) }

                            val scale by animateFloatAsState(
                                targetValue = when {
                                    isTriggered -> 0.94f
                                    isFocused -> 1.05f
                                    else -> 1.0f
                                },
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "WallpaperItemScale"
                            )

                            val thumbRequest = remember(wallpaperUri) {
                                ImageRequest.Builder(context)
                                    .data(wallpaperUri)
                                    .size(640, 360)
                                    .crossfade(false)
                                    .build()
                            }

                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E202B)),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (isFocused || isSelected) 16.dp else 4.dp
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .then(
                                        if (index == 0) Modifier.focusRequester(firstStaticCardFocus)
                                        else Modifier
                                    )
                                    .scale(scale)
                                    .onFocusChanged { isFocused = it.isFocused }
                                    .border(
                                        width = when {
                                            isFocused -> 3.dp
                                            isSelected -> 2.5.dp
                                            else -> 1.dp
                                        },
                                        color = when {
                                            isFocused -> Color.White
                                            isSelected -> CyanPrimary
                                            else -> Color(0x26FFFFFF)
                                        },
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .clickable {
                                        animatingSelectionUri = wallpaperUri
                                        scope.launch {
                                            delay(120L)
                                            onSelectWallpaper(wallpaperUri)
                                        }
                                    }
                                    .testTag("preset_wallpaper_card_$index")
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = thumbRequest,
                                        contentDescription = "Wallpaper ${index + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    if (isDownloadedWallpaper) {
                                        var deleteFocused by remember { mutableStateOf(false) }
                                        Surface(
                                            shape = CircleShape,
                                            color = if (deleteFocused) CoralAccent else Color(0xCC181B26),
                                            border = BorderStroke(1.dp, if (deleteFocused) Color.White else CoralAccent.copy(alpha = 0.7f)),
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(10.dp)
                                                .size(32.dp)
                                                .onFocusChanged { deleteFocused = it.isFocused }
                                                .clickable {
                                                    deleteDownloadedWallpaper(wallpaperUri)
                                                }
                                                .testTag("delete_downloaded_wallpaper_$index")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete Downloaded Wallpaper",
                                                    tint = if (deleteFocused) Color(0xFF12131A) else CoralAccent,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = isSelected || isTriggered,
                                        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                                        exit = scaleOut() + fadeOut(),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = CyanPrimary,
                                            shadowElevation = 8.dp,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected Wallpaper",
                                                    tint = Color(0xFF12131A),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Full-Width Footer Row for Static Wallpapers: Load More (+5) + Search More Wallpapers in Browser
                        item(span = { GridItemSpan(3) }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    var loadMoreFocused by remember { mutableStateOf(false) }
                                    Button(
                                        onClick = { loadMoreOnlineWallpapers() },
                                        enabled = !isDownloadingBatch,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (loadMoreFocused) Color.White else Color(0xFF25293C),
                                            contentColor = if (loadMoreFocused) Color(0xFF12131A) else CyanPrimary
                                        ),
                                        border = BorderStroke(
                                            width = if (loadMoreFocused) 2.dp else 1.5.dp,
                                            color = if (loadMoreFocused) Color.White else CyanPrimary
                                        ),
                                        shape = RoundedCornerShape(22.dp),
                                        modifier = Modifier
                                            .height(50.dp)
                                            .onFocusChanged { loadMoreFocused = it.isFocused }
                                            .testTag("wallpapers_load_more_button")
                                    ) {
                                        if (isDownloadingBatch) {
                                            CircularProgressIndicator(
                                                color = CyanPrimary,
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Downloading Wallpapers $nextWallpaperBatchStart–${nextWallpaperBatchStart + 4}...",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.CloudDownload,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (noMoreAvailable) "No more available" else "Load More Wallpapers (+5)",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }

                                    var searchWebFocused by remember { mutableStateOf(false) }
                                    OutlinedButton(
                                        onClick = onSearchStaticWallpapersInBrowser,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (searchWebFocused) Color.White else Color(0xFF1D2130),
                                            contentColor = if (searchWebFocused) Color(0xFF12131A) else Color.White
                                        ),
                                        border = BorderStroke(1.5.dp, if (searchWebFocused) Color.White else VioletSecondary),
                                        shape = RoundedCornerShape(22.dp),
                                        modifier = Modifier
                                            .height(50.dp)
                                            .onFocusChanged { searchWebFocused = it.isFocused }
                                    ) {
                                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Search More 4K Wallpapers in Browser",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                if (!loadMoreStatusMessage.isNullOrBlank()) {
                                    Text(
                                        text = loadMoreStatusMessage ?: "",
                                        color = if (noMoreAvailable) AmberAccent else EmeraldSuccess,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.testTag("wallpapers_load_more_status")
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // 3 Live Wallpapers Per Row Vertical Grid (Previews OFF by default unless enabled in Control Panel)
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("live_wallpapers_grid"),
                        contentPadding = PaddingValues(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(
                            items = allLiveWallpapers,
                            key = { _, item -> item.id }
                        ) { index, liveItem ->
                            val isSelected = wallpaperModeType == "LIVE" && activeLiveWallpaperUri == liveItem.uri
                            Box(modifier = Modifier.fillMaxWidth()) {
                                TvLiveWallpaperPreviewCard(
                                    item = liveItem,
                                    isSelected = isSelected,
                                    playPreview = liveWallpaperPreviewEnabled,
                                    onSelect = {
                                        onSelectLiveWallpaper(liveItem.uri)
                                    },
                                    modifier = Modifier
                                        .then(
                                            if (index == 0) Modifier.focusRequester(firstLiveCardFocus)
                                            else Modifier
                                        )
                                        .testTag("live_wallpaper_card_$index")
                                )

                                if (!liveItem.isBundled) {
                                    var delFocused by remember { mutableStateOf(false) }
                                    Surface(
                                        shape = CircleShape,
                                        color = if (delFocused) CoralAccent else Color(0xCC181B26),
                                        border = BorderStroke(1.5.dp, if (delFocused) Color.White else CoralAccent),
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(10.dp)
                                            .size(32.dp)
                                            .onFocusChanged { delFocused = it.isFocused }
                                            .clickable { deleteUserLiveWallpaper(liveItem.uri) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete Live Video",
                                                tint = if (delFocused) Color(0xFF12131A) else CoralAccent,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Footer Row in Live Tab: Upload / Pick Video from File Explorer + Download Live Wallpapers in Browser
                        item(span = { GridItemSpan(3) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                var pickVideoFocused by remember { mutableStateOf(false) }
                                Button(
                                    onClick = onOpenFileExplorerForLive,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (pickVideoFocused) Color.White else CyanPrimary,
                                        contentColor = Color(0xFF12131A)
                                    ),
                                    border = if (pickVideoFocused) BorderStroke(2.5.dp, CyanPrimary) else null,
                                    shape = RoundedCornerShape(22.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .onFocusChanged { pickVideoFocused = it.isFocused }
                                ) {
                                    Icon(imageVector = Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Upload / Pick Video from Storage", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                }

                                var dlLiveFocused by remember { mutableStateOf(false) }
                                OutlinedButton(
                                    onClick = onDownloadLiveWallpapersInBrowser,
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (dlLiveFocused) Color.White else Color(0xFF1D2130),
                                        contentColor = if (dlLiveFocused) Color(0xFF12131A) else Color.White
                                    ),
                                    border = BorderStroke(if (dlLiveFocused) 2.5.dp else 1.5.dp, if (dlLiveFocused) Color.White else CyanPrimary),
                                    shape = RoundedCornerShape(22.dp),
                                    modifier = Modifier
                                        .height(48.dp)
                                        .onFocusChanged { dlLiveFocused = it.isFocused }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        tint = if (dlLiveFocused) Color(0xFF12131A) else CyanPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Download Live Wallpapers in Browser",
                                        color = if (dlLiveFocused) Color(0xFF12131A) else Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Expanded, Full-Featured RNA. TV Control Panel Dialog:
 * - Includes:
 *   1. Single-Row vs 2-Row Home Apps toggle + 'Recent Apps' Row ON/OFF toggle + Startup Wallpaper Splash Screen toggle
 *   2. 19 App Icon Shapes Selector (including Wide 16:9 TV Banners for Rectangle)
 *   3. Live Wallpaper Playback Resolution Selector (360p, 480p, 720p Default, 1080p)
 *   4. Auto 4K Ambient Mode Idle Timer (Off, 1, 3, 5, 10, 15, 20, 25, 30 min)
 *   5. Remapper, Key Links Editor (BUTTON_1..16 + Xbox Keycodes), and RNA TV Keyboard IME setup
 *   6. Optional Device Admin Access & Optional Auto-Back on USB Boot Detection (< 1 min)
 *   7. Night Eye-Care Warmth Filter, Sleep Timer, and Live 4K Network Speed Test.
 */
private enum class ControlPanelTab(
    val title: String,
    val badge: String,
    val icon: ImageVector,
    val accent: Color
) {
    HOME_LAYOUT("Home & Layout", "Grid & Boot", Icons.Default.GridView, CyanPrimary),
    ICON_SHAPES("Icon Shapes", "19 TV Masks", Icons.Default.Category, VioletSecondary),
    WALLPAPERS_AMBIENT("Wallpapers & Ambient", "Live & 4K", Icons.Default.Wallpaper, EmeraldSuccess),
    AI_SYSTEM("System & AI", "Performance & AI", Icons.Default.AutoAwesome, AmberAccent),
    FUNCTIONS("Functions", "Menus & Tools", Icons.Default.Apps, CoralAccent)
}

@Composable
private fun TvPowerHubDialog(
    context: Context,
    initialTab: ControlPanelTab = ControlPanelTab.HOME_LAYOUT,
    onTabChanged: (ControlPanelTab) -> Unit = {},
    showRecentAppsRow: Boolean,
    onToggleRecentAppsRow: (Boolean) -> Unit,
    singleRowHomeApps: Boolean,
    onToggleSingleRowHomeApps: (Boolean) -> Unit,
    showYoutubeSearchBar: Boolean = false,
    onToggleYoutubeSearchBar: (Boolean) -> Unit = {},
    onClearRecentApps: () -> Unit,
    performanceModeEnabled: Boolean,
    onTogglePerformanceMode: (Boolean) -> Unit,
    browserFastPerformanceMode: Boolean = true,
    onToggleBrowserFastPerformanceMode: (Boolean) -> Unit = {},
    splashScreenEnabled: Boolean,
    onToggleSplashScreen: (Boolean) -> Unit,
    selectedAppIconShape: TvAppIconShape,
    onSelectAppIconShape: (TvAppIconShape) -> Unit,
    liveWallpaperResolution: LiveWallpaperResolution,
    onSelectLiveWallpaperResolution: (LiveWallpaperResolution) -> Unit,
    liveWallpaperPreviewEnabled: Boolean = false,
    onToggleLiveWallpaperPreview: (Boolean) -> Unit = {},
    ambientIdleMinutes: Int,
    onSelectAmbientIdleMinutes: (Int) -> Unit,
    askWhoIsWatchingOnBoot: Boolean = false,
    onToggleAskWhoIsWatchingOnBoot: (Boolean) -> Unit = {},
    usbBootAutoBackEnabled: Boolean,
    onToggleUsbBootAutoBack: (Boolean) -> Unit,
    openRnaAtBootEnabled: Boolean = false,
    onToggleOpenRnaAtBoot: (Boolean) -> Unit = {},
    replaceGoogleAssistantEnabled: Boolean,
    onToggleReplaceGoogleAssistant: (Boolean) -> Unit,
    aiTtsEnabled: Boolean,
    onToggleAiTts: (Boolean) -> Unit,
    rnaAiAppEnabled: Boolean,
    onToggleRnaAiAppEnabled: (Boolean) -> Unit,
    onOpenRnaAiSettings: () -> Unit,
    onOpenKeyRemapper: () -> Unit,
    onOpenTriggersAndMacros: () -> Unit = {},
    onOpenKeyLinksEditor: () -> Unit,
    onOpenVoiceAssistant: () -> Unit,
    onOpenUpdatesDrawer: () -> Unit,
    onOpenWallpapers: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenRnaStudioSettings: () -> Unit,
    sleepTimerRemainingSeconds: Int,
    onSetSleepTimerMinutes: (Int) -> Unit,
    nightWarmthIntensity: Float,
    onChangeNightWarmth: (Float) -> Unit,
    onLaunchAmbientMode: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var imeControllerBridgeEnabled by remember {
        mutableStateOf(RnaKeyLinksRegistry.isControllerOverKeyboardImeEnabled(context))
    }

    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val initialTabFocusRequester = remember { FocusRequester() }
    LaunchedEffect(initialTab) {
        onTabChanged(initialTab)
        if (initialTab != ControlPanelTab.HOME_LAYOUT) {
            delay(70L)
            runCatching { initialTabFocusRequester.requestFocus() }
        }
    }
    var isTestingNetwork by remember { mutableStateOf(false) }
    var downloadMbps by remember { mutableStateOf<Double?>(null) }
    var latencyMs by remember { mutableStateOf<Long?>(null) }
    var localIpAddress by remember { mutableStateOf("Detecting...") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            localIpAddress = getDeviceLocalIpAddress()
        }
    }

    fun runRealSpeedTest() {
        if (isTestingNetwork) return
        isTestingNetwork = true
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val pingStart = System.nanoTime()
                    val pingConn = (URL("https://www.google.com/generate_204").openConnection() as HttpURLConnection).apply {
                        connectTimeout = 4000
                        readTimeout = 4000
                    }
                    pingConn.responseCode
                    pingConn.disconnect()
                    val pingElapsedMs = ((System.nanoTime() - pingStart) / 1_000_000L).coerceAtLeast(1L)
                    latencyMs = pingElapsedMs

                    val dlUrl = URL("https://images.unsplash.com/photo-1534447677768-be436bb09401?w=2400&q=90")
                    val dlStart = System.nanoTime()
                    val dlConn = (dlUrl.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 6000
                        readTimeout = 6000
                        useCaches = false
                    }
                    var totalBytes = 0L
                    dlConn.inputStream.use { input ->
                        val buffer = ByteArray(16 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            totalBytes += read
                        }
                    }
                    dlConn.disconnect()
                    val elapsedSec = ((System.nanoTime() - dlStart) / 1_000_000_000.0).coerceAtLeast(0.05)
                    val bits = totalBytes * 8.0
                    downloadMbps = (bits / elapsedSec) / 1_000_000.0
                } catch (_: Exception) {
                    latencyMs = -1L
                } finally {
                    isTestingNetwork = false
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            selectedTab.accent.copy(alpha = 0.22f),
                            Color(0xF50B0E17),
                            Color(0xFC07090F)
                        ),
                        radius = 1500f
                    )
                )
                .testTag("tv_rna_settings_dialog"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 36.dp, vertical = 26.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // LEFT SIDEBAR (Matches Edit Profile Dialog Architecture + X Close Button in Sidebar)
                Card(
                    shape = RoundedCornerShape(30.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEB111624)),
                    border = BorderStroke(1.5.dp, selectedTab.accent.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .width(292.dp)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Top Identity Card in Sidebar + Compact Top 'X' Close Button right next to "Control Panel"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            selectedTab.accent.copy(alpha = 0.24f),
                                            VioletSecondary.copy(alpha = 0.14f)
                                        )
                                    )
                                )
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = selectedTab.accent.copy(alpha = 0.25f),
                                border = BorderStroke(1.5.dp, selectedTab.accent),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Control Panel",
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.5.sp,
                                    maxLines = 1
                                )
                                Text(
                                    text = selectedTab.title,
                                    color = selectedTab.accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }

                            // Compact focusable 'X' button right beside Control Panel title
                            var closeFocused by remember { mutableStateOf(false) }
                            val closeScale by animateFloatAsState(
                                targetValue = if (closeFocused) 1.12f else 1f,
                                label = "cp_top_close_scale"
                            )
                            Surface(
                                shape = CircleShape,
                                color = if (closeFocused) CoralAccent else Color(0xFF221A28),
                                border = BorderStroke(
                                    width = if (closeFocused) 2.dp else 1.2.dp,
                                    color = if (closeFocused) Color.White else CoralAccent.copy(alpha = 0.7f)
                                ),
                                modifier = Modifier
                                    .size(36.dp)
                                    .graphicsLayer {
                                        scaleX = closeScale
                                        scaleY = closeScale
                                    }
                                    .onFocusChanged { closeFocused = it.isFocused }
                                    .clickable { onDismiss() }
                                    .testTag("power_hub_close_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Control Panel",
                                        tint = if (closeFocused) Color(0xFF0B0E17) else CoralAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Sidebar Category Tabs (Roomy & Uncluttered without bottom Close button)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ControlPanelTab.entries.forEach { tab ->
                                var tabFocused by remember { mutableStateOf(false) }
                                val isSelected = selectedTab == tab
                                val scale by animateFloatAsState(
                                    targetValue = if (tabFocused) 1.04f else 1f,
                                    label = "cp_tab_scale_${tab.name}"
                                )
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = when {
                                        tabFocused -> tab.accent
                                        isSelected -> tab.accent.copy(alpha = 0.22f)
                                        else -> Color(0xFF171D2E)
                                    },
                                    border = BorderStroke(
                                        width = if (tabFocused || isSelected) 2.dp else 1.dp,
                                        color = when {
                                            tabFocused -> Color.White
                                            isSelected -> tab.accent
                                            else -> Color(0x26FFFFFF)
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .then(
                                            if (tab == initialTab) Modifier.focusRequester(initialTabFocusRequester)
                                            else Modifier
                                        )
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        .onFocusChanged { tabFocused = it.isFocused }
                                        .clickable {
                                            selectedTab = tab
                                            onTabChanged(tab)
                                        }
                                        .testTag("control_panel_tab_${tab.name.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(11.dp),
                                            color = if (tabFocused) Color(0x280B0E17) else tab.accent.copy(alpha = 0.20f),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = tab.icon,
                                                    contentDescription = null,
                                                    tint = if (tabFocused) Color(0xFF0B0E17) else tab.accent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tab.title,
                                                color = if (tabFocused) Color(0xFF0B0E17) else Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = tab.badge,
                                                color = if (tabFocused) Color(0xCC0B0E17) else Color(0xFF9AA3BC),
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 10.sp,
                                                maxLines = 1
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = if (tabFocused) Color(0xFF0B0E17) else tab.accent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // RIGHT CONTENT AREA (Material 3 Expressive Tonal Spot Panel)
                Card(
                    shape = RoundedCornerShape(30.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xEB111624)),
                    border = BorderStroke(1.5.dp, Color(0x28FFFFFF)),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 28.dp, vertical = 24.dp)
                    ) {
                        // Content Header Banner
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = selectedTab.accent.copy(alpha = 0.20f),
                                    border = BorderStroke(1.5.dp, selectedTab.accent),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = selectedTab.icon,
                                            contentDescription = null,
                                            tint = selectedTab.accent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = selectedTab.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 24.sp
                                    )
                                    Text(
                                        text = selectedTab.badge,
                                        color = selectedTab.accent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Animated Tab Body
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(150))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            label = "control_panel_tab_transition"
                        ) { activeTab ->
                            when (activeTab) {
                                ControlPanelTab.HOME_LAYOUT -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        contentPadding = PaddingValues(bottom = 16.dp)
                                    ) {
                                        item {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                ExpressiveToggleTile(
                                                    title = "App-Rows",
                                                    badge = if (singleRowHomeApps) "1 Row Ribbon" else "2 Row Grid",
                                                    icon = Icons.Default.ViewCarousel,
                                                    checked = singleRowHomeApps,
                                                    accentColor = CyanPrimary,
                                                    onCheckedChange = onToggleSingleRowHomeApps,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("toggle_single_row_home_apps")
                                                )
                                                ExpressiveToggleTile(
                                                    title = "Recent Apps Row",
                                                    badge = if (showRecentAppsRow) "Visible" else "Hidden",
                                                    icon = Icons.Default.History,
                                                    checked = showRecentAppsRow,
                                                    accentColor = VioletSecondary,
                                                    onCheckedChange = onToggleRecentAppsRow,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("toggle_recent_apps_row")
                                                )
                                            }
                                        }

                                        item {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                ExpressiveToggleTile(
                                                    title = "Profiles",
                                                    badge = if (askWhoIsWatchingOnBoot) "Ask on Startup" else "Auto Main Profile",
                                                    icon = Icons.Default.AccountCircle,
                                                    checked = askWhoIsWatchingOnBoot,
                                                    accentColor = CyanPrimary,
                                                    onCheckedChange = onToggleAskWhoIsWatchingOnBoot,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("toggle_who_is_watching_boot")
                                                )
                                                ExpressiveToggleTile(
                                                    title = "Startup Splash Screen",
                                                    badge = if (splashScreenEnabled) "Active" else "Instant Skip",
                                                    icon = Icons.Default.AutoAwesomeMotion,
                                                    checked = splashScreenEnabled,
                                                    accentColor = AmberAccent,
                                                    onCheckedChange = onToggleSplashScreen,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("toggle_splash_screen")
                                                )
                                            }
                                        }

                                        item {
                                            ExpressiveToggleTile(
                                                title = "YouTube",
                                                badge = if (showYoutubeSearchBar) {
                                                    "Search Bar Above Apps on Home"
                                                } else {
                                                    "Hidden"
                                                },
                                                icon = Icons.Default.PlayCircleOutline,
                                                checked = showYoutubeSearchBar,
                                                accentColor = Color(0xFFFF3B3B),
                                                onCheckedChange = onToggleYoutubeSearchBar,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("toggle_youtube_search_bar")
                                            )
                                        }

                                        item {
                                            var isDefaultLauncherActive by remember {
                                                val p = context.getSharedPreferences(
                                                    com.example.RnaHomeInterceptorService.PREFS_NAME,
                                                    Context.MODE_PRIVATE
                                                )
                                                mutableStateOf(
                                                    p.getBoolean(
                                                        com.example.RnaHomeInterceptorService.KEY_INTERCEPT_HOME,
                                                        true
                                                    )
                                                )
                                            }
                                            ExpressiveToggleTile(
                                                title = "Set RNA. TV as Default Launcher",
                                                badge = if (isDefaultLauncherActive) {
                                                    "Active • Auto-Intercepts Stock Android TV Launcher"
                                                } else {
                                                    "Disabled"
                                                },
                                                icon = Icons.Default.Tv,
                                                checked = isDefaultLauncherActive,
                                                accentColor = CyanPrimary,
                                                onCheckedChange = { next ->
                                                    isDefaultLauncherActive = next
                                                    context.getSharedPreferences(
                                                        com.example.RnaHomeInterceptorService.PREFS_NAME,
                                                        Context.MODE_PRIVATE
                                                    ).edit()
                                                        .putBoolean(
                                                            com.example.RnaHomeInterceptorService.KEY_INTERCEPT_HOME,
                                                            next
                                                        )
                                                        .apply()
                                                    if (next && !com.example.RnaHomeInterceptorService.isAccessibilityServiceEnabled(context)) {
                                                        Toast.makeText(
                                                            context,
                                                            "Enable RNA. TV in Settings > Device Preferences > Accessibility so RNA. TV automatically intercepts the stock launcher",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("toggle_set_rna_default_launcher")
                                            )
                                        }

                                        item {
                                            ExpressiveToggleTile(
                                                title = "Open RNA TV at Boot",
                                                badge = if (openRnaAtBootEnabled) "Enabled" else "Off",
                                                icon = Icons.Default.Launch,
                                                checked = openRnaAtBootEnabled,
                                                accentColor = AmberAccent,
                                                onCheckedChange = onToggleOpenRnaAtBoot,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("toggle_open_rna_at_boot")
                                            )
                                        }

                                        item {
                                            ExpressiveToggleTile(
                                                title = "Auto-Back on USB Boot Detection (< 1 Min)",
                                                badge = "Dismisses system USB popups right after TV boot",
                                                icon = Icons.Default.Usb,
                                                checked = usbBootAutoBackEnabled,
                                                accentColor = EmeraldSuccess,
                                                onCheckedChange = onToggleUsbBootAutoBack,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("toggle_usb_boot_auto_back")
                                            )
                                        }
                                    }
                                }

                                ControlPanelTab.ICON_SHAPES -> {
                                    // TV-First Visual Icon Shape Studio Grid
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Active Shape Showcase Banner (fixed preview container + weighted text so Rectangle never stretches or wraps pill)
                                        Surface(
                                            shape = RoundedCornerShape(24.dp),
                                            color = Color(0xFF171E31),
                                            border = BorderStroke(1.5.dp, VioletSecondary.copy(alpha = 0.55f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                                ) {
                                                    val activeShapePreview = remember(selectedAppIconShape) {
                                                        resolveComposeShapeForAppIcon(selectedAppIconShape)
                                                    }
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.size(width = 66.dp, height = 48.dp)
                                                    ) {
                                                        Surface(
                                                            shape = activeShapePreview,
                                                            color = VioletSecondary.copy(alpha = 0.28f),
                                                            border = BorderStroke(2.dp, CyanPrimary),
                                                            modifier = if (selectedAppIconShape.isWideBanner) {
                                                                Modifier.size(width = 64.dp, height = 40.dp)
                                                            } else {
                                                                Modifier.size(46.dp)
                                                            }
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxSize()
                                                                    .background(
                                                                        Brush.linearGradient(
                                                                            listOf(
                                                                                CyanPrimary.copy(alpha = 0.45f),
                                                                                VioletSecondary.copy(alpha = 0.45f)
                                                                            )
                                                                        )
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Apps,
                                                                    contentDescription = null,
                                                                    tint = Color.White,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "Current Profile Shape: ${selectedAppIconShape.label}",
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 16.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "Saved automatically for the current user profile",
                                                            color = CyanPrimary,
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 11.5.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = VioletSecondary.copy(alpha = 0.22f),
                                                    border = BorderStroke(1.dp, VioletSecondary)
                                                ) {
                                                    Text(
                                                        text = "${TvAppIconShape.entries.size} Shapes",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 12.sp,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // 4-Column TV Visual Shape Grid
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(4),
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxWidth()
                                                .testTag("tv_settings_app_icon_shapes_row"),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            contentPadding = PaddingValues(bottom = 12.dp)
                                        ) {
                                            itemsIndexed(
                                                items = TvAppIconShape.entries,
                                                key = { _, shapeOpt -> shapeOpt.id }
                                            ) { _, shapeOpt ->
                                                val isSel = selectedAppIconShape == shapeOpt
                                                var shapeFocused by remember { mutableStateOf(false) }
                                                val tileScale by animateFloatAsState(
                                                    targetValue = if (shapeFocused) 1.05f else 1f,
                                                    label = "shape_tile_scale_${shapeOpt.id}"
                                                )
                                                val previewShape = remember(shapeOpt) {
                                                    resolveComposeShapeForAppIcon(shapeOpt)
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(22.dp),
                                                    color = when {
                                                        shapeFocused -> VioletSecondary
                                                        isSel -> VioletSecondary.copy(alpha = 0.24f)
                                                        else -> Color(0xFF171D2E)
                                                    },
                                                    border = BorderStroke(
                                                        width = if (shapeFocused || isSel) 2.dp else 1.dp,
                                                        color = when {
                                                            shapeFocused -> Color.White
                                                            isSel -> CyanPrimary
                                                            else -> Color(0x28FFFFFF)
                                                        }
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(112.dp)
                                                        .graphicsLayer {
                                                            scaleX = tileScale
                                                            scaleY = tileScale
                                                        }
                                                        .onFocusChanged { shapeFocused = it.isFocused }
                                                        .focusable()
                                                        .onKeyEvent { event ->
                                                            if (event.type == KeyEventType.KeyDown &&
                                                                (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                                            ) {
                                                                onSelectAppIconShape(shapeOpt)
                                                                true
                                                            } else false
                                                        }
                                                        .clickable { onSelectAppIconShape(shapeOpt) }
                                                        .testTag("icon_shape_chip_${shapeOpt.id}")
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(12.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Surface(
                                                            shape = previewShape,
                                                            color = if (shapeFocused) {
                                                                Color(0xFF0B0E17)
                                                            } else if (isSel) {
                                                                CyanPrimary.copy(alpha = 0.30f)
                                                            } else {
                                                                Color(0xFF252E48)
                                                            },
                                                            border = BorderStroke(
                                                                1.5.dp,
                                                                if (shapeFocused) Color.White else if (isSel) CyanPrimary else VioletSecondary.copy(alpha = 0.7f)
                                                            ),
                                                            modifier = if (shapeOpt.isWideBanner) {
                                                                Modifier.size(width = 68.dp, height = 38.dp)
                                                            } else {
                                                                Modifier.size(44.dp)
                                                            }
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                if (isSel) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Check,
                                                                        contentDescription = null,
                                                                        tint = if (shapeFocused) CyanPrimary else Color.White,
                                                                        modifier = Modifier.size(18.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                            text = shapeOpt.label,
                                                            color = if (shapeFocused) Color(0xFF0B0E17) else Color.White,
                                                            fontWeight = if (isSel || shapeFocused) FontWeight.Black else FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            maxLines = 1
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                ControlPanelTab.WALLPAPERS_AMBIENT -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        contentPadding = PaddingValues(bottom = 16.dp)
                                    ) {
                                        // Live Wallpaper Previews Toggle (OFF by default per user instructions)
                                        item {
                                            ExpressiveToggleTile(
                                                title = "Play Live Wallpaper Previews in Picker",
                                                badge = if (liveWallpaperPreviewEnabled) "Live Video Previews ON" else "Static Thumbnails (Default OFF)",
                                                icon = Icons.Default.PlayCircleOutline,
                                                checked = liveWallpaperPreviewEnabled,
                                                accentColor = EmeraldSuccess,
                                                onCheckedChange = onToggleLiveWallpaperPreview,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("toggle_live_wallpaper_preview")
                                            )
                                        }

                                        // Live Wallpaper Playback Resolution
                                        item {
                                            Surface(
                                                shape = RoundedCornerShape(24.dp),
                                                color = Color(0xFF171D2E),
                                                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.4f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(18.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.HighQuality,
                                                                contentDescription = null,
                                                                tint = EmeraldSuccess,
                                                                modifier = Modifier.size(22.dp)
                                                            )
                                                            Text(
                                                                text = "Live Wallpaper Resolution",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 16.sp
                                                            )
                                                        }
                                                        Text(
                                                            text = liveWallpaperResolution.label,
                                                            color = EmeraldSuccess,
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 13.sp
                                                        )
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        LiveWallpaperResolution.entries.forEach { resOpt ->
                                                            val isResSel = liveWallpaperResolution == resOpt
                                                            var resFocused by remember { mutableStateOf(false) }
                                                            Surface(
                                                                shape = RoundedCornerShape(16.dp),
                                                                color = when {
                                                                    resFocused -> EmeraldSuccess
                                                                    isResSel -> EmeraldSuccess.copy(alpha = 0.24f)
                                                                    else -> Color(0xFF1E263C)
                                                                },
                                                                border = BorderStroke(
                                                                    width = if (resFocused || isResSel) 2.dp else 1.dp,
                                                                    color = if (resFocused) Color.White else if (isResSel) EmeraldSuccess else Color(0x28FFFFFF)
                                                                ),
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .height(48.dp)
                                                                    .onFocusChanged { resFocused = it.isFocused }
                                                                    .focusable()
                                                                    .onKeyEvent { event ->
                                                                        if (event.type == KeyEventType.KeyDown &&
                                                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                                                        ) {
                                                                            onSelectLiveWallpaperResolution(resOpt)
                                                                            true
                                                                        } else false
                                                                    }
                                                                    .clickable { onSelectLiveWallpaperResolution(resOpt) }
                                                                    .testTag("live_res_chip_${resOpt.id}")
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Text(
                                                                        text = resOpt.label,
                                                                        color = if (resFocused) Color(0xFF0B0E17) else Color.White,
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        fontSize = 12.sp
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // 4K Ambient Idle Timer
                                        item {
                                            Surface(
                                                shape = RoundedCornerShape(24.dp),
                                                color = Color(0xFF171D2E),
                                                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.4f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(18.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Tv,
                                                                contentDescription = null,
                                                                tint = CyanPrimary,
                                                                modifier = Modifier.size(22.dp)
                                                            )
                                                            Text(
                                                                text = "4K Ambient Idle Timer",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 16.sp
                                                            )
                                                        }
                                                        Text(
                                                            text = if (ambientIdleMinutes == 0) "Off" else "${ambientIdleMinutes}m",
                                                            color = CyanPrimary,
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 13.sp
                                                        )
                                                    }

                                                    val idleOptions = listOf(0, 1, 3, 5, 10, 15, 20, 25, 30)
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .horizontalScroll(rememberScrollState()),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        idleOptions.forEach { mins ->
                                                            val isSel = ambientIdleMinutes == mins
                                                            var chipFocused by remember { mutableStateOf(false) }
                                                            Surface(
                                                                shape = RoundedCornerShape(14.dp),
                                                                color = when {
                                                                    chipFocused -> CyanPrimary
                                                                    isSel -> CyanPrimary.copy(alpha = 0.25f)
                                                                    else -> Color(0xFF1E263C)
                                                                },
                                                                border = BorderStroke(
                                                                    width = if (chipFocused || isSel) 2.dp else 1.dp,
                                                                    color = if (chipFocused) Color.White else if (isSel) CyanPrimary else Color(0x28FFFFFF)
                                                                ),
                                                                modifier = Modifier
                                                                    .height(42.dp)
                                                                    .onFocusChanged { chipFocused = it.isFocused }
                                                                    .focusable()
                                                                    .onKeyEvent { event ->
                                                                        if (event.type == KeyEventType.KeyDown &&
                                                                            (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                                                        ) {
                                                                            onSelectAmbientIdleMinutes(mins)
                                                                            true
                                                                        } else false
                                                                    }
                                                                    .clickable { onSelectAmbientIdleMinutes(mins) }
                                                                    .testTag("ambient_idle_chip_$mins")
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier.padding(horizontal = 16.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(
                                                                        text = if (mins == 0) "Off" else "${mins}m",
                                                                        color = if (chipFocused) Color(0xFF0B0E17) else Color.White,
                                                                        fontWeight = FontWeight.ExtraBold,
                                                                        fontSize = 12.sp
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Night Eye-Care Warmth & Sleep Timer Row
                                        item {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                // Eye-Care Warmth Card
                                                Surface(
                                                    shape = RoundedCornerShape(24.dp),
                                                    color = Color(0xFF171D2E),
                                                    border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.4f)),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(18.dp),
                                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "Night Warmth",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 15.sp
                                                            )
                                                            Text(
                                                                text = if (nightWarmthIntensity <= 0.01f) "Off" else "${(nightWarmthIntensity * 100).toInt()}%",
                                                                color = AmberAccent,
                                                                fontWeight = FontWeight.Black,
                                                                fontSize = 13.sp
                                                            )
                                                        }
                                                        val warmthPresets = listOf(0f to "Off", 0.25f to "25%", 0.5f to "50%", 0.75f to "75%")
                                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            warmthPresets.forEach { (level, label) ->
                                                                val isSel = kotlin.math.abs(nightWarmthIntensity - level) < 0.08f
                                                                var wFocused by remember { mutableStateOf(false) }
                                                                Surface(
                                                                    shape = RoundedCornerShape(12.dp),
                                                                    color = when {
                                                                        wFocused -> AmberAccent
                                                                        isSel -> AmberAccent.copy(alpha = 0.25f)
                                                                        else -> Color(0xFF1E263C)
                                                                    },
                                                                    border = BorderStroke(
                                                                        1.dp,
                                                                        if (wFocused) Color.White else if (isSel) AmberAccent else Color(0x28FFFFFF)
                                                                    ),
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .height(38.dp)
                                                                        .onFocusChanged { wFocused = it.isFocused }
                                                                        .focusable()
                                                                        .onKeyEvent { event ->
                                                                            if (event.type == KeyEventType.KeyDown &&
                                                                                (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                                                            ) {
                                                                                onChangeNightWarmth(level)
                                                                                true
                                                                            } else false
                                                                        }
                                                                        .clickable { onChangeNightWarmth(level) }
                                                                ) {
                                                                    Box(contentAlignment = Alignment.Center) {
                                                                        Text(
                                                                            text = label,
                                                                            color = if (wFocused) Color(0xFF0B0E17) else Color.White,
                                                                            fontWeight = FontWeight.Bold,
                                                                            fontSize = 11.sp
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                // Sleep Timer Card
                                                Surface(
                                                    shape = RoundedCornerShape(24.dp),
                                                    color = Color(0xFF171D2E),
                                                    border = BorderStroke(1.dp, VioletSecondary.copy(alpha = 0.4f)),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(18.dp),
                                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "Sleep Timer",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 15.sp
                                                            )
                                                            Text(
                                                                text = if (sleepTimerRemainingSeconds > 0) "${sleepTimerRemainingSeconds / 60}m left" else "Off",
                                                                color = VioletSecondary,
                                                                fontWeight = FontWeight.Black,
                                                                fontSize = 13.sp
                                                            )
                                                        }
                                                        val sleepPresets = listOf(0 to "Off", 15 to "15m", 30 to "30m", 60 to "60m")
                                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            sleepPresets.forEach { (mins, label) ->
                                                                val activeMins = sleepTimerRemainingSeconds / 60
                                                                val isSel = (mins == 0 && sleepTimerRemainingSeconds == 0) ||
                                                                    (mins > 0 && activeMins in (mins - 5)..mins)
                                                                var sFocused by remember { mutableStateOf(false) }
                                                                Surface(
                                                                    shape = RoundedCornerShape(12.dp),
                                                                    color = when {
                                                                        sFocused -> VioletSecondary
                                                                        isSel -> VioletSecondary.copy(alpha = 0.25f)
                                                                        else -> Color(0xFF1E263C)
                                                                    },
                                                                    border = BorderStroke(
                                                                        1.dp,
                                                                        if (sFocused) Color.White else if (isSel) VioletSecondary else Color(0x28FFFFFF)
                                                                    ),
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .height(38.dp)
                                                                        .onFocusChanged { sFocused = it.isFocused }
                                                                        .focusable()
                                                                        .onKeyEvent { event ->
                                                                            if (event.type == KeyEventType.KeyDown &&
                                                                                (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
                                                                            ) {
                                                                                onSetSleepTimerMinutes(mins)
                                                                                true
                                                                            } else false
                                                                        }
                                                                        .clickable { onSetSleepTimerMinutes(mins) }
                                                                ) {
                                                                    Box(contentAlignment = Alignment.Center) {
                                                                        Text(
                                                                            text = label,
                                                                            color = if (sFocused) Color(0xFF0B0E17) else Color.White,
                                                                            fontWeight = FontWeight.Bold,
                                                                            fontSize = 11.sp
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                ControlPanelTab.AI_SYSTEM -> {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        contentPadding = PaddingValues(bottom = 16.dp)
                                    ) {
                                        // ── SECTION 1: SYSTEM SETTINGS (FIRST) ──
                                        item {
                                            Text(
                                                text = "SYSTEM & PERFORMANCE",
                                                color = EmeraldSuccess,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 1.sp
                                            )
                                        }

                                        item {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                ExpressiveToggleTile(
                                                    title = "Performance Mode",
                                                    badge = if (performanceModeEnabled) "Zero-Lag Mode ON" else "Full Visual FX",
                                                    icon = Icons.Default.Speed,
                                                    checked = performanceModeEnabled,
                                                    accentColor = EmeraldSuccess,
                                                    onCheckedChange = onTogglePerformanceMode,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("toggle_performance_mode")
                                                )
                                                ExpressiveToggleTile(
                                                    title = "Browser Fast Mode",
                                                    badge = if (browserFastPerformanceMode) "Hardware Turbo" else "Standard",
                                                    icon = Icons.Default.FlashOn,
                                                    checked = browserFastPerformanceMode,
                                                    accentColor = CyanPrimary,
                                                    onCheckedChange = onToggleBrowserFastPerformanceMode,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("toggle_browser_fast_mode")
                                                )
                                            }
                                        }

                                        // Optional Controller over Keyboard IME Toggle in Control Panel
                                        item {
                                            ExpressiveToggleTile(
                                                title = "Use RNA Keyboard IME for Remote & Xbox Buttons",
                                                badge = if (imeControllerBridgeEnabled) "Optional IME Bridge ON" else "Optional (Default OFF)",
                                                icon = Icons.Default.Keyboard,
                                                checked = imeControllerBridgeEnabled,
                                                accentColor = CyanPrimary,
                                                onCheckedChange = { next ->
                                                    imeControllerBridgeEnabled = next
                                                    RnaKeyLinksRegistry.setControllerOverKeyboardImeEnabled(context, next)
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("tv_settings_ime_controller_bridge_row")
                                            )
                                        }

                                        // Live Network Speed Test Card + Local IP Underneath
                                        item {
                                            Surface(
                                                shape = RoundedCornerShape(24.dp),
                                                color = Color(0xFF171D2E),
                                                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.4f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(18.dp),
                                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column {
                                                            Text(
                                                                text = "4K Streaming Diagnostics",
                                                                color = Color.White,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 16.sp
                                                            )
                                                            Text(
                                                                text = when {
                                                                    isTestingNetwork -> "Testing bandwidth & latency..."
                                                                    downloadMbps != null -> String.format(
                                                                        Locale.US,
                                                                        "%.1f Mbps  •  %d ms Ping",
                                                                        downloadMbps,
                                                                        latencyMs ?: 0L
                                                                    )
                                                                    else -> "Ready to test internet speed"
                                                                },
                                                                color = CyanPrimary,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 13.sp
                                                            )
                                                        }

                                                        var speedFocused by remember { mutableStateOf(false) }
                                                        Surface(
                                                            shape = RoundedCornerShape(16.dp),
                                                            color = if (speedFocused) CyanPrimary else CyanPrimary.copy(alpha = 0.22f),
                                                            border = BorderStroke(1.5.dp, if (speedFocused) Color.White else CyanPrimary),
                                                            modifier = Modifier
                                                                .height(44.dp)
                                                                .onFocusChanged { speedFocused = it.isFocused }
                                                                .clickable { runRealSpeedTest() }
                                                                .testTag("control_panel_run_speed_test_btn")
                                                        ) {
                                                            Box(
                                                                modifier = Modifier.padding(horizontal = 18.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = if (isTestingNetwork) "Testing..." else "Run Speed Test",
                                                                    color = if (speedFocused) Color(0xFF0B0E17) else Color.White,
                                                                    fontWeight = FontWeight.Black,
                                                                    fontSize = 13.sp
                                                                )
                                                            }
                                                        }
                                                    }

                                                    HorizontalDivider(color = Color(0x1FFFFFFF))

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.testTag("control_panel_local_ip_row")
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(EmeraldSuccess)
                                                        )
                                                        Text(
                                                            text = "IP Address: $localIpAddress",
                                                            color = Color(0xFFD6DEFF),
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.5.sp
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // ── SECTION 2: AI SETTINGS (UNDERNEATH SYSTEM SETTINGS) ──
                                        item {
                                            HorizontalDivider(
                                                color = Color(0x22FFFFFF),
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                            Text(
                                                text = "AI ASSISTANT & MODELS",
                                                color = AmberAccent,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 1.sp
                                            )
                                        }

                                        item {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                ExpressiveToggleTile(
                                                    title = "RNA. AI App",
                                                    badge = if (rnaAiAppEnabled) "Enabled" else "Hidden",
                                                    icon = Icons.Default.AutoAwesome,
                                                    checked = rnaAiAppEnabled,
                                                    accentColor = CyanPrimary,
                                                    onCheckedChange = onToggleRnaAiAppEnabled,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("toggle_rna_ai_app")
                                                )
                                                ExpressiveToggleTile(
                                                    title = "AI Voice Readout (TTS)",
                                                    badge = if (aiTtsEnabled) "Speaks Replies" else "Muted",
                                                    icon = Icons.Default.RecordVoiceOver,
                                                    checked = aiTtsEnabled,
                                                    accentColor = VioletSecondary,
                                                    onCheckedChange = onToggleAiTts,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .testTag("toggle_ai_tts")
                                                )
                                            }
                                        }

                                        // Prominent Full-Width Voice Key Tile (Expanded into the space freed by removing Device Admin)
                                        item {
                                            var voiceKeyFocused by remember { mutableStateOf(false) }
                                            val voiceKeyScale by animateFloatAsState(
                                                targetValue = if (voiceKeyFocused) 1.025f else 1f,
                                                label = "voice_key_featured_scale"
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(26.dp),
                                                color = when {
                                                    voiceKeyFocused -> AmberAccent
                                                    replaceGoogleAssistantEnabled -> AmberAccent.copy(alpha = 0.22f)
                                                    else -> Color(0xFF171D2E)
                                                },
                                                border = BorderStroke(
                                                    width = if (voiceKeyFocused || replaceGoogleAssistantEnabled) 2.dp else 1.dp,
                                                    color = when {
                                                        voiceKeyFocused -> Color.White
                                                        replaceGoogleAssistantEnabled -> AmberAccent
                                                        else -> Color(0x30FFFFFF)
                                                    }
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(104.dp)
                                                    .graphicsLayer {
                                                        scaleX = voiceKeyScale
                                                        scaleY = voiceKeyScale
                                                    }
                                                    .onFocusChanged { voiceKeyFocused = it.isFocused }
                                                    .clickable {
                                                        val nextState = !replaceGoogleAssistantEnabled
                                                        onToggleReplaceGoogleAssistant(nextState)
                                                        if (nextState && !RnaHomeInterceptorService.isAccessibilityServiceEnabled(context)) {
                                                            Toast.makeText(
                                                                context,
                                                                "Enable RNA. TV Accessibility Service so Google Assistant can be replaced over any app",
                                                                Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                    }
                                                    .testTag("toggle_replace_google_assistant")
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 22.dp, vertical = 16.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(18.dp),
                                                        color = if (voiceKeyFocused) Color(0x280B0E17) else AmberAccent.copy(alpha = 0.24f),
                                                        border = BorderStroke(
                                                            1.5.dp,
                                                            if (voiceKeyFocused) Color(0x550B0E17) else AmberAccent
                                                        ),
                                                        modifier = Modifier.size(56.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Default.Mic,
                                                                contentDescription = null,
                                                                tint = if (voiceKeyFocused) Color(0xFF0B0E17) else AmberAccent,
                                                                modifier = Modifier.size(28.dp)
                                                            )
                                                        }
                                                    }
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "Use EdVanced AI instead of Google Assistant",
                                                            color = if (voiceKeyFocused) Color(0xFF0B0E17) else Color.White,
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 16.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = if (replaceGoogleAssistantEnabled) {
                                                                "Active • TV Voice Key & Google Assistant open EdVanced AI across all apps"
                                                            } else {
                                                                "System Default • Enable to intercept the remote Voice Key with EdVanced AI"
                                                            },
                                                            color = if (voiceKeyFocused) {
                                                                Color(0xCC0B0E17)
                                                            } else if (replaceGoogleAssistantEnabled) {
                                                                AmberAccent
                                                            } else {
                                                                Color(0xFFAAB4CF)
                                                            },
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 12.sp,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    Switch(
                                                        checked = replaceGoogleAssistantEnabled,
                                                        onCheckedChange = null,
                                                        colors = SwitchDefaults.colors(
                                                            checkedThumbColor = if (voiceKeyFocused) Color.White else Color(0xFF0B0E17),
                                                            checkedTrackColor = if (voiceKeyFocused) Color(0xFF0B0E17) else AmberAccent,
                                                            uncheckedThumbColor = Color(0xFF9AA3BC),
                                                            uncheckedTrackColor = Color(0xFF232B42)
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        item {
                                            ControlPanelAiRouterSection(context = context)
                                        }
                                    }
                                }

                                ControlPanelTab.FUNCTIONS -> {
                                    // Dedicated Functions Tab for Action & Menu Buttons (Material 3 Expressive Tonal Spot)
                                    val isRnaImeDefault = remember {
                                        com.example.RnaTvInputMethodService.isDefaultSystemIme(context)
                                    }
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(3),
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        contentPadding = PaddingValues(bottom = 16.dp)
                                    ) {
                                        item {
                                            ExpressiveFunctionCard(
                                                title = "Triggers & Macros",
                                                badge = "Boot, Screen & App Rules",
                                                icon = Icons.Default.FlashOn,
                                                accentColor = AmberAccent,
                                                onClick = onOpenTriggersAndMacros,
                                                modifier = Modifier.testTag("btn_open_triggers_macros_from_hub")
                                            )
                                        }
                                        item {
                                            ExpressiveFunctionCard(
                                                title = "Updates & More Apps",
                                                badge = "Store & OTA",
                                                icon = Icons.Default.SystemUpdateAlt,
                                                accentColor = EmeraldSuccess,
                                                onClick = onOpenUpdatesDrawer,
                                                modifier = Modifier.testTag("power_hub_updates_button")
                                            )
                                        }
                                        item {
                                            ExpressiveFunctionCard(
                                                title = "Button Remapper",
                                                badge = "Multi-Press Remote Keys",
                                                icon = Icons.Default.SettingsRemote,
                                                accentColor = CyanPrimary,
                                                onClick = onOpenKeyRemapper,
                                                modifier = Modifier.testTag("btn_open_remapper_from_hub")
                                            )
                                        }
                                        item {
                                            ExpressiveFunctionCard(
                                                title = "Key Link Editor",
                                                badge = "Gamepad & Xbox Links",
                                                icon = Icons.Default.SportsEsports,
                                                accentColor = VioletSecondary,
                                                onClick = onOpenKeyLinksEditor,
                                                modifier = Modifier.testTag("btn_open_key_links_from_hub")
                                            )
                                        }
                                        item {
                                            ExpressiveFunctionCard(
                                                title = "RNA TV Keyboard",
                                                badge = if (isRnaImeDefault) "Default IME Active" else "Setup & IME Picker",
                                                icon = Icons.Default.Keyboard,
                                                accentColor = AmberAccent,
                                                onClick = {
                                                    com.example.RnaTvKeyboardBridge.openStandaloneSettings(context)
                                                },
                                                modifier = Modifier.testTag("btn_select_rna_ime")
                                            )
                                        }
                                        item {
                                            ExpressiveFunctionCard(
                                                title = "Wallpapers Studio",
                                                badge = "4K & Live Video",
                                                icon = Icons.Default.Wallpaper,
                                                accentColor = CyanPrimary,
                                                onClick = onOpenWallpapers,
                                                modifier = Modifier.testTag("power_hub_wallpapers_button")
                                            )
                                        }
                                        item {
                                            ExpressiveFunctionCard(
                                                title = "4K Ambient Mode",
                                                badge = "Launch Screensaver",
                                                icon = Icons.Default.Tv,
                                                accentColor = VioletSecondary,
                                                onClick = onLaunchAmbientMode,
                                                modifier = Modifier.testTag("power_hub_ambient_button")
                                            )
                                        }
                                        item {
                                            ExpressiveFunctionCard(
                                                title = "Devices & Remote",
                                                badge = "Bluetooth & Pairing",
                                                icon = Icons.Default.Devices,
                                                accentColor = EmeraldSuccess,
                                                onClick = onOpenDevices,
                                                modifier = Modifier.testTag("power_hub_devices_button")
                                            )
                                        }
                                        item {
                                            ExpressiveFunctionCard(
                                                title = "EdVanced Voice AI",
                                                badge = "1.1 Pro Assistant",
                                                icon = Icons.Default.GraphicEq,
                                                accentColor = AmberAccent,
                                                onClick = onOpenVoiceAssistant,
                                                modifier = Modifier.testTag("btn_open_voice_ai_from_hub")
                                            )
                                        }
                                        item {
                                            ExpressiveFunctionCard(
                                                title = "RNA AI & Agent Glow",
                                                badge = "Overlay & Colors",
                                                icon = Icons.Default.AutoAwesome,
                                                accentColor = CyanPrimary,
                                                onClick = onOpenRnaAiSettings,
                                                modifier = Modifier.testTag("btn_open_rna_ai_settings_from_hub")
                                            )
                                        }
                                        item {
                                            ExpressiveFunctionCard(
                                                title = "RNA. Studio Settings",
                                                badge = "Rows & Customization",
                                                icon = Icons.Default.Edit,
                                                accentColor = VioletSecondary,
                                                onClick = onOpenRnaStudioSettings,
                                                modifier = Modifier.testTag("power_hub_studio_button")
                                            )
                                        }
                                        item {
                                            ExpressiveFunctionCard(
                                                title = "Android Settings",
                                                badge = "System Preferences",
                                                icon = Icons.Default.Settings,
                                                accentColor = CyanPrimary,
                                                onClick = { launchNormalAndroidSettings(context) },
                                                modifier = Modifier.testTag("power_hub_android_settings_button")
                                            )
                                        }
                                        item {
                                            ExpressiveFunctionCard(
                                                title = "Clear Recent Apps",
                                                badge = "Reset History",
                                                icon = Icons.Default.DeleteSweep,
                                                accentColor = CoralAccent,
                                                onClick = onClearRecentApps,
                                                modifier = Modifier.testTag("btn_clear_recent_apps")
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressiveToggleTile(
    title: String,
    badge: String,
    icon: ImageVector,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.03f else 1f,
        label = "expressive_toggle_scale"
    )
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = when {
            focused -> accentColor
            checked -> accentColor.copy(alpha = 0.18f)
            else -> Color(0xFF171D2E)
        },
        border = BorderStroke(
            width = if (focused || checked) 2.dp else 1.dp,
            color = when {
                focused -> Color.White
                checked -> accentColor
                else -> Color(0x28FFFFFF)
            }
        ),
        modifier = modifier
            .height(84.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { focused = it.isFocused }
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = if (focused) Color(0x280B0E17) else accentColor.copy(alpha = 0.22f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (focused) Color(0xFF0B0E17) else accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = if (focused) Color(0xFF0B0E17) else Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                    Text(
                        text = badge,
                        color = if (focused) Color(0xCC0B0E17) else if (checked) accentColor else Color(0xFF9AA3BC),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = if (focused) Color.White else Color(0xFF0B0E17),
                    checkedTrackColor = if (focused) Color(0xFF0B0E17) else accentColor,
                    uncheckedThumbColor = Color(0xFF9AA3BC),
                    uncheckedTrackColor = Color(0xFF232B42)
                )
            )
        }
    }
}

@Composable
private fun ExpressiveFunctionCard(
    title: String,
    badge: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.04f else 1f,
        label = "expressive_fn_scale"
    )
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (focused) accentColor else Color(0xFF171D2E),
        border = BorderStroke(
            width = if (focused) 2.5.dp else 1.5.dp,
            color = if (focused) Color.White else accentColor.copy(alpha = 0.45f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(114.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (focused) Color(0x280B0E17) else accentColor.copy(alpha = 0.22f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (focused) Color(0xFF0B0E17) else accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = if (focused) Color(0x280B0E17) else Color(0xFF222A42),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ArrowOutward,
                            contentDescription = null,
                            tint = if (focused) Color(0xFF0B0E17) else accentColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = title,
                    color = if (focused) Color(0xFF0B0E17) else Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = badge,
                    color = if (focused) Color(0xCC0B0E17) else accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

private fun getDeviceLocalIpAddress(): String {
    return try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val intf = interfaces.nextElement()
            val addrs = intf.inetAddresses
            while (addrs.hasMoreElements()) {
                val addr = addrs.nextElement()
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    return addr.hostAddress ?: "192.168.1.100"
                }
            }
        }
        "Connected"
    } catch (_: Exception) {
        "Connected"
    }
}

@Composable
private fun AmbientScreensaverOverlay(
    activeWallpaperUri: String,
    wallpaperModeType: String = "STATIC",
    activeLiveWallpaperUri: String = "",
    liveWallpaperResolution: LiveWallpaperResolution = LiveWallpaperResolution.RES_720P,
    performanceMode: Boolean = false,
    currentTime: String,
    currentDate: String,
    onExit: () -> Unit
) {
    val chosenWallpaper = remember(activeWallpaperUri) {
        activeWallpaperUri.ifBlank { PRESET_WALLPAPER_ASSET_URIS.first() }
    }
    val openedAtMs = remember { android.os.SystemClock.elapsedRealtime() }
    val ambientFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(60L)
        ambientFocusRequester.safeRequestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(ambientFocusRequester)
            .focusable()
            .onPreviewKeyEvent { ev ->
                val elapsed = android.os.SystemClock.elapsedRealtime() - openedAtMs
                if (elapsed < 450L) {
                    true
                } else if (ev.type == KeyEventType.KeyDown) {
                    onExit()
                    true
                } else {
                    true
                }
            }
            .clickable {
                if (android.os.SystemClock.elapsedRealtime() - openedAtMs >= 450L) {
                    onExit()
                }
            }
    ) {
        if (wallpaperModeType == "LIVE" && activeLiveWallpaperUri.isNotBlank()) {
            TvLiveWallpaperBackground(
                videoUri = activeLiveWallpaperUri,
                shouldPlay = true,
                preferredResolution = liveWallpaperResolution,
                performanceMode = performanceMode,
                onAutoResolutionStepDown = {},
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = chosenWallpaper,
                contentDescription = "4K Ambient Backdrop",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x28000000), Color(0xCC0A0B10))
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(56.dp)
        ) {
            Text(
                text = currentTime.ifBlank { "9:02" },
                color = Color.White,
                fontSize = 76.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1.5).sp
            )
            Text(
                text = "$currentDate  •  RNA. TV 4K Ambient Mode",
                color = Color(0xFFD6D5E2),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Current Active Wallpaper  •  Press any button or tap anywhere to resume RNA. TV",
                color = Color(0xFF90909E),
                fontSize = 12.sp
            )
        }
    }
}

private fun launchTvHardwareInput(context: Context, chosenInput: String) {
    val tvInputManager = context.getSystemService(Context.TV_INPUT_SERVICE) as? TvInputManager
    val allInputs: List<TvInputInfo> = runCatching { tvInputManager?.tvInputList ?: emptyList() }.getOrDefault(emptyList())

    fun tryStartIntent(intent: Intent): Boolean {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    // 1. Try matching a real TvInputInfo from Android's TvInputManager
    val matchedInput: TvInputInfo? = when {
        chosenInput.startsWith("HDMI", ignoreCase = true) -> {
            val portNum = chosenInput.removePrefix("HDMI").trim().toIntOrNull() ?: 1
            val hdmiInputs = allInputs.filter { info ->
                info.type == TvInputInfo.TYPE_HDMI ||
                    info.id.contains("hdmi", ignoreCase = true) ||
                    info.loadLabel(context)?.toString()?.contains("hdmi", ignoreCase = true) == true
            }.sortedBy { it.id }

            hdmiInputs.firstOrNull { info ->
                val label = info.loadLabel(context)?.toString().orEmpty()
                val customLabel = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    info.loadCustomLabel(context)?.toString().orEmpty()
                } else {
                    ""
                }
                val id = info.id
                label.contains("HDMI $portNum", ignoreCase = true) ||
                    label.contains("HDMI$portNum", ignoreCase = true) ||
                    customLabel.contains("HDMI $portNum", ignoreCase = true) ||
                    id.contains("hdmi$portNum", ignoreCase = true) ||
                    id.contains("hdmi_$portNum", ignoreCase = true) ||
                    id.endsWith("/HW$portNum", ignoreCase = true) ||
                    id.endsWith("_$portNum") ||
                    id.endsWith("$portNum")
            } ?: hdmiInputs.getOrNull(portNum - 1)
        }

        chosenInput.equals("AV", ignoreCase = true) -> {
            allInputs.firstOrNull { info ->
                info.type == TvInputInfo.TYPE_COMPOSITE ||
                    info.type == TvInputInfo.TYPE_COMPONENT ||
                    info.type == TvInputInfo.TYPE_SVIDEO ||
                    info.type == TvInputInfo.TYPE_SCART ||
                    info.id.contains("av", ignoreCase = true) ||
                    info.id.contains("composite", ignoreCase = true) ||
                    info.id.contains("cvbs", ignoreCase = true) ||
                    info.loadLabel(context)?.toString()?.contains("AV", ignoreCase = true) == true ||
                    info.loadLabel(context)?.toString()?.contains("Composite", ignoreCase = true) == true
            }
        }

        chosenInput.equals("TV", ignoreCase = true) -> {
            allInputs.firstOrNull { info ->
                info.type == TvInputInfo.TYPE_TUNER ||
                    info.id.contains("tuner", ignoreCase = true) ||
                    info.id.contains("dtv", ignoreCase = true) ||
                    info.id.contains("atv", ignoreCase = true) ||
                    info.loadLabel(context)?.toString()?.contains("TV", ignoreCase = true) == true ||
                    info.loadLabel(context)?.toString()?.contains("Channels", ignoreCase = true) == true
            }
        }

        else -> null
    }

    if (matchedInput != null) {
        val uri = if (matchedInput.isPassthroughInput) {
            TvContract.buildChannelUriForPassthroughInput(matchedInput.id)
        } else {
            TvContract.buildChannelsUriForInput(matchedInput.id)
        }
        if (tryStartIntent(Intent(Intent.ACTION_VIEW, uri))) return
        if (tryStartIntent(Intent("com.android.tv.action.VIEW_INPUT", uri).apply {
                putExtra("input_id", matchedInput.id)
            })
        ) return
    }

    // 2. If "TV" (Live Tuner / Channels) was chosen, try opening Live TV / Channels URI directly
    if (chosenInput.equals("TV", ignoreCase = true)) {
        if (tryStartIntent(Intent(Intent.ACTION_VIEW, TvContract.Channels.CONTENT_URI))) return
        val liveTvPackages = listOf(
            "com.google.android.tv",
            "com.android.tv",
            "com.sony.dtv.tvx",
            "com.tcl.tv",
            "com.hisense.tv",
            "com.xiaomi.mitv.tvplayer",
            "org.dtvkit.inputsource"
        )
        for (pkg in liveTvPackages) {
            val launch = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launch != null && tryStartIntent(launch)) return
        }
    }

    // 3. Try common OEM passthrough URIs for HDMI 1..3 and AV (Amlogic / MediaTek / Realtek / Google TV / Sony / TCL)
    val candidateInputIds = when {
        chosenInput.startsWith("HDMI", ignoreCase = true) -> {
            val portNum = chosenInput.removePrefix("HDMI").trim().toIntOrNull() ?: 1
            val amlogicHw = portNum + 4 // Amlogic typically maps HDMI1..3 to HW5..HW7
            listOf(
                "com.droidlogic.tvinput/.services.Hdmi${portNum}InputService/HW$amlogicHw",
                "com.droidlogic.tvinput/.services.Hdmi${portNum}InputService/HW$portNum",
                "com.mediatek.tvinput/.hdmi.HDMIInputService/HW$portNum",
                "com.realtek.tv.tvinput/.hdmi.HdmiInputService/HW$portNum",
                "com.google.android.tv.btservices/.HdmiInputService/HW$portNum",
                "com.tcl.tvinput/.hdmi.HDMI${portNum}Service",
                "com.sony.dtv.tvinput/.hdmi.Hdmi${portNum}Service"
            )
        }
        chosenInput.equals("AV", ignoreCase = true) -> {
            listOf(
                "com.droidlogic.tvinput/.services.Av1InputService/HW1",
                "com.droidlogic.tvinput/.services.AvInputService/HW1",
                "com.mediatek.tvinput/.composite.CompositeInputService/HW1",
                "com.realtek.tv.tvinput/.av.AvInputService/HW1"
            )
        }
        else -> emptyList()
    }

    for (candidateId in candidateInputIds) {
        val passthroughUri = TvContract.buildChannelUriForPassthroughInput(candidateId)
        val viewIntent = Intent(Intent.ACTION_VIEW, passthroughUri)
        if (context.packageManager.resolveActivity(viewIntent, 0) != null) {
            if (tryStartIntent(viewIntent)) return
        }
    }

    // 4. Try OEM input switch intents with port parameters
    val switchActions = listOf(
        "com.android.tv.action.SWITCH_INPUT",
        "android.media.tv.action.SWITCH_INPUT",
        "android.intent.action.TV_INPUT_BUTTON",
        TvInputManager.ACTION_SETUP_INPUTS
    )
    for (action in switchActions) {
        val intent = Intent(action).apply {
            putExtra("input_label", chosenInput)
            putExtra("source", chosenInput)
            if (chosenInput.startsWith("HDMI", ignoreCase = true)) {
                val portNum = chosenInput.removePrefix("HDMI").trim().toIntOrNull() ?: 1
                putExtra("hdmi_port", portNum)
            }
        }
        if (tryStartIntent(intent)) return
    }

    // 5. Fallback if running on a device/emulator without hardware TIF inputs registered
    Toast.makeText(context, "Opening $chosenInput...", Toast.LENGTH_SHORT).show()
}

@Composable
private fun RnaTvInputSelectorDialog(
    selectedInput: String,
    onSelectInput: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val inputOptions = remember {
        listOf(
            "HDMI 1" to Icons.Default.Tv,
            "HDMI 2" to Icons.Default.Tv,
            "HDMI 3" to Icons.Default.Tv,
            "AV" to Icons.Default.Settings,
            "TV" to Icons.Default.Tv
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C26)),
            border = BorderStroke(1.5.dp, Color(0x38FFFFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("rna_input_selector_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TV Inputs",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF282B3A),
                        border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = selectedInput,
                            color = CyanPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                inputOptions.forEach { (inputName, icon) ->
                    var isFocused by remember { mutableStateOf(false) }
                    val isSelected = selectedInput == inputName
                    val scale by animateFloatAsState(
                        targetValue = if (isFocused) 1.03f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "InputOptionScale"
                    )

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = when {
                            isFocused -> Color(0xFFE8EAED)
                            isSelected -> Color(0xFF293048)
                            else -> Color(0xFF222532)
                        },
                        border = BorderStroke(
                            width = if (isFocused) 2.5.dp else 1.dp,
                            color = when {
                                isFocused -> CyanPrimary
                                isSelected -> CyanPrimary.copy(alpha = 0.7f)
                                else -> Color(0x26FFFFFF)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .onFocusChanged { isFocused = it.isFocused }
                            .clickable { onSelectInput(inputName) }
                            .focusable()
                            .testTag("input_option_${inputName.lowercase().replace(" ", "_")}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isFocused) Color(0xFF1A1C26) else Color(0xFF2E3244),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = inputName,
                                            tint = if (isFocused) CyanPrimary else Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = inputName,
                                    color = if (isFocused) Color(0xFF111318) else Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (isSelected) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isFocused) Color(0xFF111318) else CyanPrimary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        color = CyanPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("input_selector_close_btn")
                    ) {
                        Text("Close", color = Color(0xFFD6D5E2))
                    }
                }
            }
        }
    }
}

@Composable
private fun AppContextMenuDialog(
    app: UnifiedLauncherApp,
    onDismiss: () -> Unit,
    onStartMoveMode: () -> Unit,
    onToggleFavorite: () -> Unit,
    onViewAppDetails: () -> Unit,
    onExportApp: () -> Unit,
    onUninstallApp: () -> Unit,
    onCustomizeApp: () -> Unit
) {
    var ignoredFirstOk by remember { mutableStateOf(false) }
    val firstActionFocusRequester = remember { FocusRequester() }
    val menuScrollState = rememberScrollState()

    fun guardFirstOk(action: () -> Unit) {
        if (!ignoredFirstOk) {
            ignoredFirstOk = true
            return
        }
        action()
    }

    LaunchedEffect(Unit) {
        delay(60L)
        runCatching { firstActionFocusRequester.requestFocus() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C26)),
            border = BorderStroke(1.5.dp, Color(0x38FFFFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .onPreviewKeyEvent { event ->
                    val native = event.nativeKeyEvent
                    val isOkKey = native.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                        native.keyCode == KeyEvent.KEYCODE_ENTER ||
                        native.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
                        native.keyCode == KeyEvent.KEYCODE_BUTTON_A
                    if (isOkKey && !ignoredFirstOk) {
                        if (native.action == KeyEvent.ACTION_UP) {
                            ignoredFirstOk = true
                        }
                        true
                    } else {
                        false
                    }
                }
                .testTag("app_context_menu_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF252836),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                        modifier = Modifier.size(48.dp)
                    ) {
                        AppAvatarContent(app = app)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.title,
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (app.isRnaWebApp) "RNA. Web Container" else app.packageName,
                            color = Color(0xFF9E9FAE),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(menuScrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.focusRequester(firstActionFocusRequester)) {
                        ContextMenuActionRow(
                            title = "Move",
                            subtitle = "Slide app across the dock with D-Pad",
                            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            accentColor = Color(0xFFFFD54F),
                            testTag = "context_action_move",
                            onClick = { guardFirstOk(onStartMoveMode) }
                        )
                    }

                    ContextMenuActionRow(
                        title = if (app.isFavorite) "Remove from Favorites" else "Add to Favorites",
                        subtitle = if (app.isFavorite) "Unpin from Home screen" else "Pin to Home screen",
                        icon = if (app.isFavorite) Icons.Default.Check else Icons.Default.Add,
                        accentColor = CyanPrimary,
                        testTag = "context_action_favorite",
                        onClick = { guardFirstOk(onToggleFavorite) }
                    )

                    ContextMenuActionRow(
                        title = "View App Details",
                        subtitle = "Package info & permissions",
                        icon = Icons.Default.Info,
                        accentColor = Color(0xFF80DEEA),
                        testTag = "context_action_details",
                        onClick = { guardFirstOk(onViewAppDetails) }
                    )

                    if (app.isRnaWebApp) {
                        ContextMenuActionRow(
                            title = "Export .rna Package",
                            subtitle = "Save .rna bundle to Downloads",
                            icon = Icons.Default.IosShare,
                            accentColor = EmeraldSuccess,
                            testTag = "context_action_export",
                            onClick = { guardFirstOk(onExportApp) }
                        )
                    }

                    ContextMenuActionRow(
                        title = "Uninstall",
                        subtitle = if (app.isRnaWebApp) "Remove container" else "Uninstall Android package",
                        icon = Icons.Default.Close,
                        accentColor = Color(0xFFFF8A80),
                        testTag = "context_action_uninstall",
                        onClick = { guardFirstOk(onUninstallApp) }
                    )

                    ContextMenuActionRow(
                        title = "Customize Logo & Name",
                        subtitle = "Change icon artwork or custom label",
                        icon = Icons.Default.Edit,
                        accentColor = VioletSecondary,
                        testTag = "context_action_customize",
                        onClick = { guardFirstOk(onCustomizeApp) }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("context_menu_cancel_button")
                    ) {
                        Text("Close", color = Color(0xFFD6D5E2))
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextMenuActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ContextMenuActionScale"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isFocused) Color(0xFFE8EAED) else Color(0xFF232634),
        border = BorderStroke(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = if (isFocused) accentColor else Color(0x24FFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isFocused) Color(0xFF1A1C26) else accentColor.copy(alpha = 0.16f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isFocused) Color(0xFF111318) else Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    color = if (isFocused) Color(0xFF454754) else Color(0xFF9E9FAE),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AppDetailsDialog(
    app: UnifiedLauncherApp,
    onDismiss: () -> Unit,
    onLaunchApp: () -> Unit,
    onExportApp: () -> Unit = {},
    onUninstallApp: () -> Unit,
    onOpenSystemAppInfo: () -> Unit
) {
    val context = LocalContext.current
    val versionInfo = remember(app) {
        if (app.isRnaWebApp) {
            val srcType = app.rnaApp?.sourceType ?: "WEB"
            val tvMode = if (app.rnaApp?.isTvEnabled == true) "D-Pad + Virtual Mouse Enabled" else "Standard Controls"
            "RNA. Container ($srcType) • $tvMode"
        } else {
            runCatching {
                val pInfo = context.packageManager.getPackageInfo(app.packageName, 0)
                "Version ${pInfo.versionName ?: "1.0"}"
            }.getOrDefault("Native Android Package")
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C26)),
            border = BorderStroke(1.5.dp, Color(0x38FFFFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("app_details_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF252836),
                        border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                        modifier = Modifier.size(60.dp)
                    ) {
                        AppAvatarContent(app = app)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = versionInfo,
                            color = CyanPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF232634),
                    border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Package / Identifier: ${app.packageName}",
                            color = Color(0xFFE4E1E9),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Application Type: ${if (app.isRnaWebApp) "RNA. Hybrid Web Application" else "Installed Android TV / System App"}",
                            color = Color(0xFFB5B6C6),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Home Screen Status: ${if (app.isFavorite) "Pinned to Favorites" else "In All Apps Drawer"}",
                            color = Color(0xFFB5B6C6),
                            fontSize = 12.sp
                        )
                        if (app.isRnaWebApp && app.rnaApp != null) {
                            Text(
                                text = "Source: ${app.rnaApp.sourceUriOrContent.take(80)}",
                                color = Color(0xFF9E9FAE),
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onLaunchApp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = Color(0xFF111318)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("details_open_app_button")
                    ) {
                        Text("Open App", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onUninstallApp,
                        border = BorderStroke(1.dp, Color(0xFFFF8A80)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("details_uninstall_button")
                    ) {
                        Text("Uninstall", color = Color(0xFFFF8A80), fontWeight = FontWeight.Bold)
                    }
                }

                if (app.isRnaWebApp) {
                    OutlinedButton(
                        onClick = onExportApp,
                        border = BorderStroke(1.dp, EmeraldSuccess),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("details_export_button")
                    ) {
                        Icon(imageVector = Icons.Default.IosShare, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export .rna Package to Downloads", color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                    }
                }

                if (!app.isRnaWebApp) {
                    OutlinedButton(
                        onClick = onOpenSystemAppInfo,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("details_system_info_button")
                    ) {
                        Text("Open Android System App Settings", color = Color(0xFFD6D5E2), fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Close", color = Color(0xFFD6D5E2))
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomizeAndRearrangeAppDialog(
    app: UnifiedLauncherApp,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onDismiss: () -> Unit,
    onMoveDirection: (Int) -> Unit,
    onSaveCustomNameAndLogo: (String, String?, Boolean) -> Unit,
    onRevertToOriginalLogo: () -> Unit = {},
    onOpenInRnaStudio: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val customNameFieldFocus = remember { FocusRequester() }
    var customName by remember(app) { mutableStateOf(app.title) }
    var customLogo by remember(app) { mutableStateOf(app.customLogoUri) }
    var isFav by remember(app) { mutableStateOf(app.isFavorite) }
    var showNameKeyboard by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { customLogo = it.toString() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        EnableSystemKeyboardInDialog()
        val dialogView = LocalView.current
        val dialogKeyboardController = LocalSoftwareKeyboardController.current

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E202B)),
                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .heightIn(max = 560.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Customize & Rearrange App",
                        color = Color(0xFFF2F3FA),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // D-Pad Focusable Custom App Name Row + Keyboard Trigger
                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            singleLine = true,
                            label = { Text("Custom App Name") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = Color(0xFF454652),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF2B3044),
                                unfocusedContainerColor = Color(0xFF141620)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(customNameFieldFocus)
                                .tvKeyboardFieldTrigger(
                                    context = context,
                                    view = dialogView,
                                    focusRequester = customNameFieldFocus,
                                    keyboardController = dialogKeyboardController,
                                    onOpenRnaKeyboard = { showNameKeyboard = true }
                                )
                                .testTag("custom_app_name_dpad_button")
                        )

                        OutlinedButton(
                            onClick = {
                                customLogo = null
                                onRevertToOriginalLogo()
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, AmberAccent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("revert_original_app_logo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                tint = AmberAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Revert to Original Logo",
                                color = AmberAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        com.example.ui.wizard.RnaLogoSelectorCard(
                            headerTitle = "Custom App Logo",
                            headerSubtitle = "Preset Vectors, Monogram, Favicon, or Custom Photo",
                            currentLogoUri = customLogo,
                            websiteUrlForFavicon = app.rnaApp?.takeIf { it.sourceType == "URL" }?.sourceUriOrContent,
                            defaultTitleForAlphabet = customName.take(2).uppercase(),
                            onSelectLogoUri = { customLogo = it },
                            onPickFromGallery = {
                                logoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onMoveDirection(-1) },
                                enabled = canMoveLeft,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = Color.White)
                                Text("Move Earlier", color = Color.White, fontSize = 12.sp)
                            }
                            OutlinedButton(
                                onClick = { onMoveDirection(1) },
                                enabled = canMoveRight,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Move Later", color = Color.White, fontSize = 12.sp)
                                Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.White)
                            }
                        }

                        var favFocused by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (favFocused) Color(0xFF2B3044) else Color(0xFF141620),
                            border = BorderStroke(1.dp, if (favFocused) CyanPrimary else Color(0x28FFFFFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { favFocused = it.isFocused }
                                .clickable { isFav = !isFav }
                                .focusable()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Show on Home 'Favorites' Dock", color = Color(0xFFE4E1E9), fontSize = 13.sp)
                                Checkbox(
                                    checked = isFav,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(checkedColor = CyanPrimary)
                                )
                            }
                        }

                        if (app.isRnaWebApp) {
                            OutlinedButton(
                                onClick = onOpenInRnaStudio,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Edit Container in RNA. Studio", color = VioletSecondary, fontSize = 12.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("customize_app_cancel_button")
                        ) {
                            Text("Cancel", color = Color(0xFFC6C5D0))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { onSaveCustomNameAndLogo(customName.ifBlank { app.title }, customLogo, isFav) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanPrimary,
                                contentColor = Color(0xFF1B2654)
                            ),
                            modifier = Modifier.testTag("customize_app_save_button")
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (showNameKeyboard && isRnaKeyboardDefaultIme(context)) {
                RnaTvKeyboardOverlay(
                    visible = true,
                    currentText = customName,
                    onTextChange = { customName = it },
                    onCommitText = { customName += it },
                    onBackspace = {
                        if (customName.isNotEmpty()) customName = customName.dropLast(1)
                    },
                    onSubmit = { showNameKeyboard = false },
                    onDismiss = { showNameKeyboard = false }
                )
            }
        }
    }
}

@Composable
private fun AddExistingAppsToHomeDialog(
    allApps: List<UnifiedLauncherApp>,
    onToggleFavorite: (UnifiedLauncherApp, Boolean) -> Unit,
    onCreateNewRnaApp: () -> Unit,
    onOpenRnaStudio: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E202B)),
            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Manage Home Favorites",
                    color = Color(0xFFF2F3FA),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select which installed apps or RNA. web apps appear on your Home screen.",
                    color = Color(0xFF90909E),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCreateNewRnaApp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = Color(0xFF1B2654)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+ Create RNA. App", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onOpenRnaStudio,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Open RNA. Studio", color = Color(0xFFE4E1E9), fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allApps, key = { it.uniqueKey }) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (app.isFavorite) Color(0xFF2A2E42) else Color(0xFF252734))
                                .clickable { onToggleFavorite(app, !app.isFavorite) }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF1E202B),
                                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    AppAvatarContent(app = app)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = app.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (app.isRnaWebApp) "RNA. Web App • ${app.packageName}" else app.packageName,
                                        color = Color(0xFF90909E),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Checkbox(
                                checked = app.isFavorite,
                                onCheckedChange = { onToggleFavorite(app, it) },
                                colors = CheckboxDefaults.colors(checkedColor = CyanPrimary)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = Color(0xFF1B2654)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TvWhoIsWatchingBootOverlay(
    profiles: List<TvUserProfile>,
    activeProfile: TvUserProfile,
    onSelectProfile: (TvUserProfile) -> Unit,
    onAddProfile: () -> Unit
) {
    val firstProfileFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(80L)
        firstProfileFocus.safeRequestFocus()
    }

    Dialog(
        onDismissRequest = { onSelectProfile(activeProfile) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF151B2E), Color(0xFF070910))
                    )
                )
                .testTag("tv_who_is_watching_boot_overlay")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(36.dp)
            ) {
                Text(
                    text = "Who is watching?",
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(36.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(26.dp),
                    contentPadding = PaddingValues(horizontal = 32.dp)
                ) {
                    itemsIndexed(profiles, key = { _, p -> p.id }) { index, prof ->
                        var isFocused by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(
                            targetValue = if (isFocused) 1.12f else 1.0f,
                            label = "BootProfileScale"
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .then(if (index == 0) Modifier.focusRequester(firstProfileFocus) else Modifier)
                                .scale(scale)
                                .onFocusChanged { isFocused = it.isFocused }
                                .clickable { onSelectProfile(prof) }
                                .focusable()
                                .testTag("boot_profile_${prof.id}")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF1E2235),
                                border = BorderStroke(
                                    width = if (isFocused) 4.dp else 2.dp,
                                    color = if (isFocused) Color.White else Color(0x44FFFFFF)
                                ),
                                shadowElevation = if (isFocused) 20.dp else 6.dp,
                                modifier = Modifier.size(112.dp)
                            ) {
                                TvProfileAvatarImage(
                                    avatarUri = prof.avatarUri,
                                    contentDescription = prof.name,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = prof.name,
                                color = if (isFocused) Color.White else Color(0xFFC6C7D4),
                                fontSize = 18.sp,
                                fontWeight = if (isFocused) FontWeight.ExtraBold else FontWeight.SemiBold
                            )
                        }
                    }

                    item {
                        var addFocused by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(
                            targetValue = if (addFocused) 1.12f else 1.0f,
                            label = "BootAddProfileScale"
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .scale(scale)
                                .onFocusChanged { addFocused = it.isFocused }
                                .clickable { onAddProfile() }
                                .focusable()
                                .testTag("boot_add_profile_btn")
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF1E2235),
                                border = BorderStroke(
                                    width = if (addFocused) 4.dp else 2.dp,
                                    color = if (addFocused) CyanPrimary else Color(0x44FFFFFF)
                                ),
                                modifier = Modifier.size(112.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Profile",
                                        tint = if (addFocused) CyanPrimary else Color.White,
                                        modifier = Modifier.size(42.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Add Profile",
                                color = if (addFocused) CyanPrimary else Color(0xFFC6C7D4),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TvFirstRunSetupWizardOverlay(
    initialProfile: TvUserProfile,
    selectedAppIconShape: TvAppIconShape,
    singleRowHomeApps: Boolean,
    showRecentAppsRow: Boolean,
    splashScreenEnabled: Boolean,
    ambientIdleMinutes: Int,
    initialReplaceGoogleAssistant: Boolean = false,
    onCompleteSetup: (
        updatedProfile: TvUserProfile,
        chosenShape: TvAppIconShape,
        singleRow: Boolean,
        showRecent: Boolean,
        splashOn: Boolean,
        ambientMins: Int,
        askWhoIsWatching: Boolean,
        replaceGoogleAssistant: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val wizardPrefs = remember {
        context.getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
    }

    var step by remember { mutableIntStateOf(0) } // 0: Profile, 1: Wallpaper, 2: 4K Ambient Mode, 3: Layout & Icon Shapes, 4: System Defaults
    var profileName by remember { mutableStateOf(initialProfile.name.ifBlank { "Main" }) }
    var selectedAvatarUri by remember {
        mutableStateOf(initialProfile.avatarUri.ifBlank { PRESET_PROFILE_AVATARS.first().presetUri })
    }
    var chosenWallpaperMode by remember { mutableStateOf("STATIC") }
    var chosenStaticWallpaper by remember { mutableStateOf(PRESET_WALLPAPER_ASSET_URIS.first()) }
    var chosenLiveWallpaper by remember { mutableStateOf(BUNDLED_LIVE_WALLPAPERS.first().uri) }
    var chosenAmbientMins by remember { mutableIntStateOf(if (ambientIdleMinutes > 0) ambientIdleMinutes else 5) }
    var chosenShape by remember { mutableStateOf(selectedAppIconShape) }
    var chosenSingleRow by remember { mutableStateOf(singleRowHomeApps) }
    var chosenShowRecent by remember { mutableStateOf(showRecentAppsRow) }
    var chosenSplash by remember { mutableStateOf(splashScreenEnabled) }
    var chosenAskWhoWatching by remember { mutableStateOf(false) }
    var chosenRnaKeyboardDefault by remember { mutableStateOf(isRnaKeyboardDefaultIme(context)) }
    var chosenReplaceGoogleAssistant by remember { mutableStateOf(initialReplaceGoogleAssistant) }
    var isAccessibilityReady by remember {
        mutableStateOf(RnaHomeInterceptorService.isAccessibilityServiceEnabled(context))
    }
    var isRnaImeActiveNow by remember {
        mutableStateOf(RnaTvInputMethodService.isDefaultSystemIme(context))
    }
    var showNameKeyboard by remember { mutableStateOf(false) }

    val step0Focus = remember { FocusRequester() }
    val step1Focus = remember { FocusRequester() }
    val step2Focus = remember { FocusRequester() }
    val step3Focus = remember { FocusRequester() }
    val step4Focus = remember { FocusRequester() }
    val nameFieldFocusRequester = remember { FocusRequester() }
    val primaryBtnFocus = remember { FocusRequester() }

    LaunchedEffect(step) {
        delay(90L)
        when (step) {
            0 -> step0Focus.safeRequestFocus()
            1 -> step1Focus.safeRequestFocus()
            2 -> step2Focus.safeRequestFocus()
            3 -> step3Focus.safeRequestFocus()
            else -> step4Focus.safeRequestFocus()
        }
    }

    // Live-poll Accessibility & Keyboard status while on Step 5 (System Defaults) so returning from Settings updates immediately
    LaunchedEffect(step) {
        if (step == 4) {
            wizardPrefs.edit().putBoolean(RnaHomeInterceptorService.KEY_INTERCEPT_HOME, true).apply()
            while (step == 4) {
                isAccessibilityReady = RnaHomeInterceptorService.isAccessibilityServiceEnabled(context)
                isRnaImeActiveNow = RnaTvInputMethodService.isDefaultSystemIme(context)
                if (isRnaImeActiveNow) {
                    chosenRnaKeyboardDefault = true
                }
                delay(1200L)
            }
        }
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        EnableSystemKeyboardInDialog()
        val dialogView = LocalView.current
        val dialogKeyboardController = LocalSoftwareKeyboardController.current

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF161F38), Color(0xFF070A14))
                    )
                )
                .testTag("tv_first_run_setup_wizard")
        ) {
            Card(
                shape = RoundedCornerShape(30.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xF5151928)),
                border = BorderStroke(1.5.dp, CyanPrimary.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .fillMaxHeight(0.88f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header + Animated Step Progress Pills
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (step) {
                                    0 -> "Welcome to RNA. TV • Setup Your Profile"
                                    1 -> "Choose Your Wallpaper (Static or Live)"
                                    2 -> "4K Ambient Mode Timer"
                                    3 -> "Home Layout & App Icon Shapes"
                                    else -> "System Defaults • Launcher, Assistant & Keyboard"
                                },
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = CyanPrimary.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, CyanPrimary)
                            ) {
                                Text(
                                    text = "Step ${step + 1} of 5",
                                    color = CyanPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            (0..4).forEach { idx ->
                                val isActive = idx <= step
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (isActive) CyanPrimary else Color(0x33FFFFFF))
                                )
                            }
                        }
                    }

                    // Body Content with Smooth Step Transitions
                    AnimatedContent(
                        targetState = step,
                        label = "SetupWizardStepContent",
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) { currentStep ->
                        when (currentStep) {
                            0 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF1E2235),
                                            border = BorderStroke(2.5.dp, CyanPrimary),
                                            modifier = Modifier.size(76.dp)
                                        ) {
                                            TvProfileAvatarImage(
                                                avatarUri = selectedAvatarUri,
                                                contentDescription = profileName,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        var editNameBtnFocused by remember { mutableStateOf(false) }
                                        val editNameScale by animateFloatAsState(
                                            targetValue = if (editNameBtnFocused) 1.04f else 1f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            label = "SetupEditNameBtnScale"
                                        )

                                        OutlinedTextField(
                                            value = profileName,
                                            onValueChange = { profileName = it.take(16) },
                                            singleLine = true,
                                            label = { Text("Profile Name") },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color.White,
                                                unfocusedBorderColor = CyanPrimary,
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White,
                                                cursorColor = CyanPrimary,
                                                focusedLabelColor = CyanPrimary,
                                                unfocusedLabelColor = Color(0xFFB5B8C8)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .scale(editNameScale)
                                                .focusRequester(nameFieldFocusRequester)
                                                .onFocusChanged { editNameBtnFocused = it.isFocused }
                                                .tvKeyboardFieldTrigger(
                                                    context = context,
                                                    view = dialogView,
                                                    focusRequester = nameFieldFocusRequester,
                                                    keyboardController = dialogKeyboardController,
                                                    onOpenRnaKeyboard = { showNameKeyboard = true }
                                                )
                                        )

                                        var typeBtnFocused by remember { mutableStateOf(false) }
                                        val typeBtnScale by animateFloatAsState(
                                            targetValue = if (typeBtnFocused) 1.06f else 1f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            label = "SetupTypeBtnScale"
                                        )
                                        Button(
                                            onClick = {
                                                if (isRnaKeyboardDefaultIme(context)) {
                                                    showNameKeyboard = true
                                                } else {
                                                    openDeviceDefaultKeyboard(
                                                        context = context,
                                                        view = dialogView,
                                                        focusRequester = nameFieldFocusRequester,
                                                        keyboardController = dialogKeyboardController
                                                    )
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (typeBtnFocused) Color.White else CyanPrimary,
                                                contentColor = Color(0xFF090D16)
                                            ),
                                            shape = RoundedCornerShape(14.dp),
                                            border = BorderStroke(if (typeBtnFocused) 2.5.dp else 1.dp, Color.White),
                                            modifier = Modifier
                                                .scale(typeBtnScale)
                                                .focusRequester(step0Focus)
                                                .onFocusChanged { typeBtnFocused = it.isFocused }
                                                .focusable()
                                        ) {
                                            Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Open Keyboard", fontWeight = FontWeight.ExtraBold)
                                        }
                                    }

                                    Text(
                                        text = "Select Avatar (Navigate with D-Pad & Press OK)",
                                        color = Color(0xFFD5D7E6),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp, horizontal = 4.dp)
                                    ) {
                                        items(PRESET_PROFILE_AVATARS, key = { it.id }) { preset ->
                                            val isSelected = selectedAvatarUri == preset.presetUri
                                            var avatarFocused by remember { mutableStateOf(false) }
                                            val avatarScale by animateFloatAsState(
                                                targetValue = if (avatarFocused) 1.16f else if (isSelected) 1.05f else 1f,
                                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                label = "SetupAvatarScale"
                                            )
                                            Surface(
                                                shape = CircleShape,
                                                color = if (avatarFocused) Color.White else Color(0xFF222638),
                                                border = BorderStroke(
                                                    width = if (avatarFocused) 3.5.dp else if (isSelected) 2.5.dp else 1.dp,
                                                    color = when {
                                                        avatarFocused -> Color.White
                                                        isSelected -> CyanPrimary
                                                        else -> Color(0x33FFFFFF)
                                                    }
                                                ),
                                                shadowElevation = if (avatarFocused) 14.dp else 2.dp,
                                                modifier = Modifier
                                                    .size(62.dp)
                                                    .scale(avatarScale)
                                                    .onFocusChanged { avatarFocused = it.isFocused }
                                                    .clickable { selectedAvatarUri = preset.presetUri }
                                            ) {
                                                TvProfileAvatarImage(
                                                    avatarUri = preset.presetUri,
                                                    contentDescription = preset.label,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }

                                    var askWhoRowFocused by remember { mutableStateOf(false) }
                                    val askWhoScale by animateFloatAsState(
                                        targetValue = if (askWhoRowFocused) 1.02f else 1f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        label = "SetupAskWhoScale"
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (askWhoRowFocused) Color(0xFF2A314A) else Color(0xFF1E2235),
                                        border = BorderStroke(
                                            width = if (askWhoRowFocused) 2.5.dp else 1.dp,
                                            color = if (askWhoRowFocused) Color.White else Color(0x33FFFFFF)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .scale(askWhoScale)
                                            .onFocusChanged { askWhoRowFocused = it.isFocused }
                                            .clickable { chosenAskWhoWatching = !chosenAskWhoWatching }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 18.dp, vertical = 14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Ask 'Who is Watching?' on Startup",
                                                color = Color.White,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Switch(
                                                checked = chosenAskWhoWatching,
                                                onCheckedChange = null,
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color(0xFF12131A),
                                                    checkedTrackColor = CyanPrimary
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            1 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        var staticBtnFocused by remember { mutableStateOf(false) }
                                        val staticScale by animateFloatAsState(
                                            targetValue = if (staticBtnFocused) 1.08f else 1f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            label = "SetupStaticPillScale"
                                        )
                                        Button(
                                            onClick = { chosenWallpaperMode = "STATIC" },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = when {
                                                    staticBtnFocused -> Color.White
                                                    chosenWallpaperMode == "STATIC" -> CyanPrimary
                                                    else -> Color(0xFF222638)
                                                },
                                                contentColor = if (staticBtnFocused || chosenWallpaperMode == "STATIC") Color(0xFF090D16) else Color.White
                                            ),
                                            border = BorderStroke(
                                                width = if (staticBtnFocused) 2.5.dp else 1.dp,
                                                color = if (staticBtnFocused) CyanPrimary else Color(0x44FFFFFF)
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier
                                                .scale(staticScale)
                                                .focusRequester(step1Focus)
                                                .onFocusChanged { staticBtnFocused = it.isFocused }
                                                .focusable()
                                        ) {
                                            Text("Static", fontWeight = FontWeight.ExtraBold)
                                        }

                                        var liveBtnFocused by remember { mutableStateOf(false) }
                                        val liveScale by animateFloatAsState(
                                            targetValue = if (liveBtnFocused) 1.08f else 1f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            label = "SetupLivePillScale"
                                        )
                                        Button(
                                            onClick = { chosenWallpaperMode = "LIVE" },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = when {
                                                    liveBtnFocused -> Color.White
                                                    chosenWallpaperMode == "LIVE" -> CyanPrimary
                                                    else -> Color(0xFF222638)
                                                },
                                                contentColor = if (liveBtnFocused || chosenWallpaperMode == "LIVE") Color(0xFF090D16) else Color.White
                                            ),
                                            border = BorderStroke(
                                                width = if (liveBtnFocused) 2.5.dp else 1.dp,
                                                color = if (liveBtnFocused) CyanPrimary else Color(0x44FFFFFF)
                                            ),
                                            shape = RoundedCornerShape(20.dp),
                                            modifier = Modifier
                                                .scale(liveScale)
                                                .onFocusChanged { liveBtnFocused = it.isFocused }
                                                .focusable()
                                        ) {
                                            Text("Live", fontWeight = FontWeight.ExtraBold)
                                        }
                                    }

                                    if (chosenWallpaperMode == "STATIC") {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                                        ) {
                                            items(PRESET_WALLPAPER_ASSET_URIS.take(10), key = { it }) { uri ->
                                                val isSel = chosenStaticWallpaper == uri
                                                var wpFocused by remember { mutableStateOf(false) }
                                                val wpScale by animateFloatAsState(
                                                    targetValue = if (wpFocused) 1.08f else 1f,
                                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                    label = "SetupStaticWpScale"
                                                )
                                                Card(
                                                    shape = RoundedCornerShape(16.dp),
                                                    border = BorderStroke(
                                                        width = if (wpFocused) 3.5.dp else if (isSel) 2.5.dp else 1.dp,
                                                        color = when {
                                                            wpFocused -> Color.White
                                                            isSel -> CyanPrimary
                                                            else -> Color(0x33FFFFFF)
                                                        }
                                                    ),
                                                    modifier = Modifier
                                                        .size(width = 215.dp, height = 124.dp)
                                                        .scale(wpScale)
                                                        .onFocusChanged { wpFocused = it.isFocused }
                                                        .clickable { chosenStaticWallpaper = uri }
                                                        .focusable()
                                                ) {
                                                    Box(modifier = Modifier.fillMaxSize()) {
                                                        AsyncImage(
                                                            model = uri,
                                                            contentDescription = null,
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                        if (isSel) {
                                                            Surface(
                                                                shape = CircleShape,
                                                                color = CyanPrimary,
                                                                modifier = Modifier
                                                                    .align(Alignment.TopEnd)
                                                                    .padding(8.dp)
                                                                    .size(24.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFF090D16),
                                                                    modifier = Modifier.padding(4.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                                        ) {
                                            items(BUNDLED_LIVE_WALLPAPERS, key = { it.id }) { liveItem ->
                                                val isSel = chosenLiveWallpaper == liveItem.uri
                                                var liveCardFocused by remember { mutableStateOf(false) }
                                                val liveCardScale by animateFloatAsState(
                                                    targetValue = if (liveCardFocused) 1.08f else 1f,
                                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                    label = "SetupLiveWpScale"
                                                )
                                                Card(
                                                    shape = RoundedCornerShape(16.dp),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (liveCardFocused) Color(0xFF28304A) else Color(0xFF1E2235)
                                                    ),
                                                    border = BorderStroke(
                                                        width = if (liveCardFocused) 3.5.dp else if (isSel) 2.5.dp else 1.dp,
                                                        color = when {
                                                            liveCardFocused -> Color.White
                                                            isSel -> CyanPrimary
                                                            else -> Color(0x33FFFFFF)
                                                        }
                                                    ),
                                                    modifier = Modifier
                                                        .size(width = 215.dp, height = 124.dp)
                                                        .scale(liveCardScale)
                                                        .onFocusChanged { liveCardFocused = it.isFocused }
                                                        .clickable { chosenLiveWallpaper = liveItem.uri }
                                                        .focusable()
                                                ) {
                                                    Box(
                                                        contentAlignment = Alignment.Center,
                                                        modifier = Modifier.fillMaxSize()
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.PlayArrow,
                                                            contentDescription = null,
                                                            tint = if (isSel || liveCardFocused) CyanPrimary else Color.White,
                                                            modifier = Modifier.size(38.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            2 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        text = "Select Inactivity Timeout (Default: 5m)",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp)
                                    ) {
                                        itemsIndexed(AMBIENT_IDLE_MINUTE_OPTIONS) { idx, mins ->
                                            val isSel = chosenAmbientMins == mins
                                            var minFocused by remember { mutableStateOf(false) }
                                            val minScale by animateFloatAsState(
                                                targetValue = if (minFocused) 1.1f else 1f,
                                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                label = "SetupAmbientMinScale"
                                            )
                                            Button(
                                                onClick = { chosenAmbientMins = mins },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = when {
                                                        minFocused -> Color.White
                                                        isSel -> CyanPrimary
                                                        else -> Color(0xFF222638)
                                                    },
                                                    contentColor = if (minFocused || isSel) Color(0xFF090D16) else Color.White
                                                ),
                                                border = BorderStroke(
                                                    width = if (minFocused) 2.5.dp else 1.dp,
                                                    color = if (minFocused) CyanPrimary else Color(0x44FFFFFF)
                                                ),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier
                                                    .scale(minScale)
                                                    .then(if (idx == 0) Modifier.focusRequester(step2Focus) else Modifier)
                                                    .onFocusChanged { minFocused = it.isFocused }
                                                    .focusable()
                                            ) {
                                                Text(
                                                    text = if (mins == 0) "Off" else "${mins}m",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            3 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Text(
                                        text = "App Icon Shape (${chosenShape.label})",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)
                                    ) {
                                        itemsIndexed(TvAppIconShape.entries) { idx, shapeOpt ->
                                            val isSel = chosenShape == shapeOpt
                                            var shapeFocused by remember { mutableStateOf(false) }
                                            val shapeScale by animateFloatAsState(
                                                targetValue = if (shapeFocused) 1.08f else 1f,
                                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                label = "SetupShapeBtnScale"
                                            )
                                            Button(
                                                onClick = { chosenShape = shapeOpt },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = when {
                                                        shapeFocused -> Color.White
                                                        isSel -> CyanPrimary
                                                        else -> Color(0xFF222638)
                                                    },
                                                    contentColor = if (shapeFocused || isSel) Color(0xFF090D16) else Color.White
                                                ),
                                                border = BorderStroke(
                                                    width = if (shapeFocused) 2.5.dp else 1.dp,
                                                    color = if (shapeFocused) CyanPrimary else Color(0x44FFFFFF)
                                                ),
                                                shape = RoundedCornerShape(14.dp),
                                                modifier = Modifier
                                                    .scale(shapeScale)
                                                    .then(if (idx == 0) Modifier.focusRequester(step3Focus) else Modifier)
                                                    .onFocusChanged { shapeFocused = it.isFocused }
                                            ) {
                                                Text(shapeOpt.label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        var singleRowCardFocused by remember { mutableStateOf(false) }
                                        val singleRowScale by animateFloatAsState(
                                            targetValue = if (singleRowCardFocused) 1.04f else 1f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            label = "SetupSingleRowScale"
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (singleRowCardFocused) Color(0xFF2A314A) else Color(0xFF1E2235),
                                            border = BorderStroke(
                                                width = if (singleRowCardFocused) 2.5.dp else 1.dp,
                                                color = if (singleRowCardFocused) Color.White else Color(0x33FFFFFF)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .scale(singleRowScale)
                                                .onFocusChanged { singleRowCardFocused = it.isFocused }
                                                .clickable { chosenSingleRow = !chosenSingleRow }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("App-Rows", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Switch(checked = chosenSingleRow, onCheckedChange = null)
                                            }
                                        }

                                        var recentCardFocused by remember { mutableStateOf(false) }
                                        val recentScale by animateFloatAsState(
                                            targetValue = if (recentCardFocused) 1.04f else 1f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            label = "SetupRecentScale"
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (recentCardFocused) Color(0xFF2A314A) else Color(0xFF1E2235),
                                            border = BorderStroke(
                                                width = if (recentCardFocused) 2.5.dp else 1.dp,
                                                color = if (recentCardFocused) Color.White else Color(0x33FFFFFF)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .scale(recentScale)
                                                .onFocusChanged { recentCardFocused = it.isFocused }
                                                .clickable { chosenShowRecent = !chosenShowRecent }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Show Recent Apps Row", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Switch(checked = chosenShowRecent, onCheckedChange = null)
                                            }
                                        }

                                        var splashCardFocused by remember { mutableStateOf(false) }
                                        val splashScale by animateFloatAsState(
                                            targetValue = if (splashCardFocused) 1.04f else 1f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            label = "SetupSplashScale"
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (splashCardFocused) Color(0xFF2A314A) else Color(0xFF1E2235),
                                            border = BorderStroke(
                                                width = if (splashCardFocused) 2.5.dp else 1.dp,
                                                color = if (splashCardFocused) Color.White else Color(0x33FFFFFF)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .scale(splashScale)
                                                .onFocusChanged { splashCardFocused = it.isFocused }
                                                .clickable { chosenSplash = !chosenSplash }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("Startup Splash Screen", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Switch(checked = chosenSplash, onCheckedChange = null)
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                // Step 5 of 5: Dedicated System Defaults Page (Launcher, RNA Assistant, RNA Keyboard)
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("tv_setup_wizard_step_system_defaults"),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // CARD 1: Set RNA. TV Launcher as Default (with TV Accessibility instructions + Open Settings)
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = Color(0xFF171F34),
                                        border = BorderStroke(
                                            width = 1.5.dp,
                                            color = if (isAccessibilityReady) EmeraldSuccess else CyanPrimary.copy(alpha = 0.65f)
                                        ),
                                        modifier = Modifier
                                            .weight(1.15f)
                                            .fillMaxHeight()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(20.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(14.dp),
                                                        color = CyanPrimary.copy(alpha = 0.2f),
                                                        border = BorderStroke(1.dp, CyanPrimary),
                                                        modifier = Modifier.size(44.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Default.Tv,
                                                                contentDescription = null,
                                                                tint = CyanPrimary,
                                                                modifier = Modifier.size(22.dp)
                                                            )
                                                        }
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = if (isAccessibilityReady) EmeraldSuccess.copy(alpha = 0.22f) else AmberAccent.copy(alpha = 0.2f),
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (isAccessibilityReady) EmeraldSuccess else AmberAccent
                                                        )
                                                    ) {
                                                        Text(
                                                            text = if (isAccessibilityReady) "ACTIVE" else "REQUIRED",
                                                            color = if (isAccessibilityReady) EmeraldSuccess else AmberAccent,
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Black,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "Default TV Launcher",
                                                    color = Color.White,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = "Android TV restricts opening Accessibility directly. Follow these steps after opening Settings:",
                                                    color = Color(0xFFB5C0DC),
                                                    fontSize = 12.sp,
                                                    lineHeight = 16.sp
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = Color(0xFF101626),
                                                    border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.35f)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(12.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "1. Device Preferences",
                                                            color = CyanPrimary,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "2. Accessibility → Services",
                                                            color = Color.White,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "3. Enable RNA. TV & Press Back",
                                                            color = EmeraldSuccess,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.ExtraBold
                                                        )
                                                    }
                                                }
                                            }

                                            var openSettingsFocused by remember { mutableStateOf(false) }
                                            val openSettingsScale by animateFloatAsState(
                                                targetValue = if (openSettingsFocused) 1.05f else 1f,
                                                label = "WizardOpenAccSettingsScale"
                                            )
                                            Button(
                                                onClick = {
                                                    wizardPrefs.edit()
                                                        .putBoolean(RnaHomeInterceptorService.KEY_INTERCEPT_HOME, true)
                                                        .apply()
                                                    try {
                                                        context.startActivity(
                                                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            }
                                                        )
                                                    } catch (_: Exception) {
                                                        launchNormalAndroidSettings(context)
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (openSettingsFocused) Color.White else CyanPrimary,
                                                    contentColor = Color(0xFF090D16)
                                                ),
                                                shape = RoundedCornerShape(14.dp),
                                                border = BorderStroke(
                                                    if (openSettingsFocused) 2.5.dp else 1.dp,
                                                    if (openSettingsFocused) CyanPrimary else Color.White.copy(alpha = 0.4f)
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(46.dp)
                                                    .scale(openSettingsScale)
                                                    .focusRequester(step4Focus)
                                                    .onFocusChanged { openSettingsFocused = it.isFocused }
                                                    .testTag("wizard_open_accessibility_settings_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Settings,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (isAccessibilityReady) "Settings (Enabled)" else "Open Settings",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }

                                    // CARD 2: Set RNA Assistant Default over Google Assistant
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = Color(0xFF171F34),
                                        border = BorderStroke(
                                            width = 1.5.dp,
                                            color = if (chosenReplaceGoogleAssistant) AmberAccent else Color(0x30FFFFFF)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(20.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(14.dp),
                                                        color = AmberAccent.copy(alpha = 0.2f),
                                                        border = BorderStroke(1.dp, AmberAccent),
                                                        modifier = Modifier.size(44.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Default.Mic,
                                                                contentDescription = null,
                                                                tint = AmberAccent,
                                                                modifier = Modifier.size(22.dp)
                                                            )
                                                        }
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = if (chosenReplaceGoogleAssistant) AmberAccent.copy(alpha = 0.22f) else Color(0xFF232B42),
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (chosenReplaceGoogleAssistant) AmberAccent else Color(0x33FFFFFF)
                                                        )
                                                    ) {
                                                        Text(
                                                            text = if (chosenReplaceGoogleAssistant) "RNA AI" else "GOOGLE",
                                                            color = if (chosenReplaceGoogleAssistant) AmberAccent else Color(0xFFAAB4CF),
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Black,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "RNA Assistant Default",
                                                    color = Color.White,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = "Replace Google Assistant with EdVanced AI when pressing the remote Voice Key across your TV.",
                                                    color = Color(0xFFB5C0DC),
                                                    fontSize = 12.sp,
                                                    lineHeight = 17.sp
                                                )
                                            }

                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                var assistToggleFocused by remember { mutableStateOf(false) }
                                                val assistToggleScale by animateFloatAsState(
                                                    targetValue = if (assistToggleFocused) 1.04f else 1f,
                                                    label = "WizardAssistToggleScale"
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(14.dp),
                                                    color = when {
                                                        assistToggleFocused -> AmberAccent
                                                        chosenReplaceGoogleAssistant -> AmberAccent.copy(alpha = 0.22f)
                                                        else -> Color(0xFF222A40)
                                                    },
                                                    border = BorderStroke(
                                                        width = if (assistToggleFocused) 2.5.dp else 1.2.dp,
                                                        color = if (assistToggleFocused) Color.White else AmberAccent.copy(alpha = 0.7f)
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(48.dp)
                                                        .scale(assistToggleScale)
                                                        .onFocusChanged { assistToggleFocused = it.isFocused }
                                                        .clickable {
                                                            val next = !chosenReplaceGoogleAssistant
                                                            chosenReplaceGoogleAssistant = next
                                                            wizardPrefs.edit()
                                                                .putBoolean(RnaHomeInterceptorService.KEY_REPLACE_GOOGLE_ASSISTANT, next)
                                                                .commit()
                                                        }
                                                        .testTag("wizard_toggle_rna_assistant_default")
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .padding(horizontal = 14.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = if (chosenReplaceGoogleAssistant) "RNA AI Active" else "Use RNA Assistant",
                                                            color = if (assistToggleFocused) Color(0xFF090D16) else Color.White,
                                                            fontWeight = FontWeight.Black,
                                                            fontSize = 12.5.sp
                                                        )
                                                        Switch(
                                                            checked = chosenReplaceGoogleAssistant,
                                                            onCheckedChange = null,
                                                            colors = SwitchDefaults.colors(
                                                                checkedThumbColor = if (assistToggleFocused) Color.White else Color(0xFF090D16),
                                                                checkedTrackColor = if (assistToggleFocused) Color(0xFF090D16) else AmberAccent
                                                            )
                                                        )
                                                    }
                                                }

                                                var assistSysFocused by remember { mutableStateOf(false) }
                                                OutlinedButton(
                                                    onClick = {
                                                        try {
                                                            context.startActivity(
                                                                Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
                                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                }
                                                            )
                                                        } catch (_: Exception) {
                                                            launchNormalAndroidSettings(context)
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(14.dp),
                                                    border = BorderStroke(
                                                        if (assistSysFocused) 2.dp else 1.dp,
                                                        if (assistSysFocused) Color.White else Color(0x44FFFFFF)
                                                    ),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = if (assistSysFocused) Color.White else Color(0xFF1F263B),
                                                        contentColor = if (assistSysFocused) Color(0xFF090D16) else Color.White
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(42.dp)
                                                        .onFocusChanged { assistSysFocused = it.isFocused }
                                                        .testTag("wizard_open_assistant_settings_btn")
                                                ) {
                                                    Text(
                                                        text = "Assistant Settings",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // CARD 3: Set RNA Keyboard over System Keyboard
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = Color(0xFF171F34),
                                        border = BorderStroke(
                                            width = 1.5.dp,
                                            color = if (isRnaImeActiveNow || chosenRnaKeyboardDefault) VioletSecondary else Color(0x30FFFFFF)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(20.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(14.dp),
                                                        color = VioletSecondary.copy(alpha = 0.22f),
                                                        border = BorderStroke(1.dp, VioletSecondary),
                                                        modifier = Modifier.size(44.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Default.Keyboard,
                                                                contentDescription = null,
                                                                tint = VioletSecondary,
                                                                modifier = Modifier.size(22.dp)
                                                            )
                                                        }
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = if (isRnaImeActiveNow) EmeraldSuccess.copy(alpha = 0.22f) else VioletSecondary.copy(alpha = 0.2f),
                                                        border = BorderStroke(
                                                            1.dp,
                                                            if (isRnaImeActiveNow) EmeraldSuccess else VioletSecondary
                                                        )
                                                    ) {
                                                        Text(
                                                            text = if (isRnaImeActiveNow) "ACTIVE" else "OPTIONAL",
                                                            color = if (isRnaImeActiveNow) EmeraldSuccess else VioletSecondary,
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Black,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = "RNA TV Keyboard",
                                                    color = Color.White,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                                Text(
                                                    text = "Use RNA Keyboard over the stock TV keyboard for faster D-Pad typing, voice dictation, and remote phone input.",
                                                    color = Color(0xFFB5C0DC),
                                                    fontSize = 12.sp,
                                                    lineHeight = 17.sp
                                                )
                                            }

                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                var selectImeFocused by remember { mutableStateOf(false) }
                                                val selectImeScale by animateFloatAsState(
                                                    targetValue = if (selectImeFocused) 1.04f else 1f,
                                                    label = "WizardSelectImeScale"
                                                )
                                                Button(
                                                    onClick = {
                                                        chosenRnaKeyboardDefault = true
                                                        setRnaKeyboardPreferredDefault(context, true)
                                                        try {
                                                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
                                                            imm?.showInputMethodPicker()
                                                        } catch (_: Exception) {
                                                            Toast.makeText(
                                                                context,
                                                                "Enable RNA Keyboard in Keyboard Settings first",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (selectImeFocused) Color.White else VioletSecondary,
                                                        contentColor = if (selectImeFocused) Color(0xFF090D16) else Color.White
                                                    ),
                                                    shape = RoundedCornerShape(14.dp),
                                                    border = BorderStroke(
                                                        if (selectImeFocused) 2.5.dp else 1.dp,
                                                        if (selectImeFocused) VioletSecondary else Color.White.copy(alpha = 0.4f)
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(46.dp)
                                                        .scale(selectImeScale)
                                                        .onFocusChanged { selectImeFocused = it.isFocused }
                                                        .testTag("wizard_select_rna_keyboard_btn")
                                                ) {
                                                    Text(
                                                        text = if (isRnaImeActiveNow) "RNA Keyboard Active" else "Select RNA Keyboard",
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 12.5.sp
                                                    )
                                                }

                                                var kbSettingsFocused by remember { mutableStateOf(false) }
                                                OutlinedButton(
                                                    onClick = {
                                                        try {
                                                            context.startActivity(
                                                                Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                                }
                                                            )
                                                        } catch (_: Exception) {
                                                            launchNormalAndroidSettings(context)
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(14.dp),
                                                    border = BorderStroke(
                                                        if (kbSettingsFocused) 2.dp else 1.dp,
                                                        if (kbSettingsFocused) Color.White else Color(0x44FFFFFF)
                                                    ),
                                                    colors = ButtonDefaults.outlinedButtonColors(
                                                        containerColor = if (kbSettingsFocused) Color.White else Color(0xFF1F263B),
                                                        contentColor = if (kbSettingsFocused) Color(0xFF090D16) else Color.White
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(42.dp)
                                                        .onFocusChanged { kbSettingsFocused = it.isFocused }
                                                        .testTag("wizard_open_keyboard_settings_btn")
                                                ) {
                                                    Text(
                                                        text = "Keyboard Settings",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Navigation Row with High-Contrast D-Pad Focus Highlights
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (step > 0) {
                            var backBtnFocused by remember { mutableStateOf(false) }
                            val backScale by animateFloatAsState(
                                targetValue = if (backBtnFocused) 1.06f else 1f,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "SetupBackBtnScale"
                            )
                            OutlinedButton(
                                onClick = { step-- },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    width = if (backBtnFocused) 2.5.dp else 1.dp,
                                    color = if (backBtnFocused) CyanPrimary else Color.White
                                ),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (backBtnFocused) Color.White else Color(0xFF222638),
                                    contentColor = if (backBtnFocused) Color(0xFF090D16) else Color.White
                                ),
                                modifier = Modifier
                                    .scale(backScale)
                                    .onFocusChanged { backBtnFocused = it.isFocused }
                                    .testTag("tv_setup_wizard_back_btn")
                            ) {
                                Text(
                                    text = "Back",
                                    color = if (backBtnFocused) Color(0xFF090D16) else Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        var nextBtnFocused by remember { mutableStateOf(false) }
                        val nextScale by animateFloatAsState(
                            targetValue = if (nextBtnFocused) 1.07f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "SetupNextBtnScale"
                        )
                        Button(
                            onClick = {
                                if (step < 4) {
                                    step++
                                } else {
                                    val finalProf = initialProfile.copy(
                                        name = profileName.trim().ifBlank { "Main" },
                                        avatarUri = selectedAvatarUri,
                                        wallpaperUri = chosenStaticWallpaper,
                                        wallpaperModeType = chosenWallpaperMode,
                                        liveWallpaperUri = if (chosenWallpaperMode == "LIVE") chosenLiveWallpaper else "",
                                        appIconShape = chosenShape.id,
                                        singleRowHomeApps = chosenSingleRow,
                                        showRecentAppsRow = chosenShowRecent,
                                        ambientIdleMinutes = chosenAmbientMins
                                    )
                                    onCompleteSetup(
                                        finalProf,
                                        chosenShape,
                                        chosenSingleRow,
                                        chosenShowRecent,
                                        chosenSplash,
                                        chosenAmbientMins,
                                        chosenAskWhoWatching,
                                        chosenReplaceGoogleAssistant
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (nextBtnFocused) Color.White else CyanPrimary,
                                contentColor = Color(0xFF090D16)
                            ),
                            border = BorderStroke(
                                width = if (nextBtnFocused) 3.dp else 1.dp,
                                color = if (nextBtnFocused) CyanPrimary else Color.White.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .scale(nextScale)
                                .focusRequester(primaryBtnFocus)
                                .onFocusChanged { nextBtnFocused = it.isFocused }
                                .testTag("tv_setup_wizard_next_btn")
                        ) {
                            Text(
                                text = if (step < 4) "Next" else "Finish Setup",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (isRnaKeyboardDefaultIme(context)) {
                RnaTvKeyboardOverlay(
                    visible = showNameKeyboard,
                    currentText = profileName,
                    fieldHint = "Profile Name",
                    showTextPreviewBar = true,
                    onTextChange = { profileName = it },
                    onCommitChar = { profileName += it },
                    onBackspace = {
                        if (profileName.isNotEmpty()) {
                            profileName = profileName.dropLast(1)
                        }
                    },
                    onSearchOrSubmit = { showNameKeyboard = false },
                    onDismiss = { showNameKeyboard = false }
                )
            }
        }
    }
}

