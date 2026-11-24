package com.example.ui.tv

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.view.Choreographer
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

const val KEY_CONTROL_APP_ICON_SHAPE = "control_app_icon_shape"
const val KEY_CONTROL_AMBIENT_IDLE_MINUTES = "control_ambient_idle_minutes"
const val KEY_CONTROL_LIVE_WALLPAPER_RES = "control_live_wallpaper_res"
const val KEY_LIVE_WALLPAPER_PREVIEW_ENABLED = "live_wallpaper_preview_enabled"
const val KEY_CONTROL_USB_BOOT_AUTO_BACK = "control_usb_boot_auto_back"
const val KEY_WALLPAPER_MODE_TYPE = "wallpaper_mode_type" // "STATIC" or "LIVE"
const val KEY_ACTIVE_LIVE_WALLPAPER_URI = "active_live_wallpaper_uri"

private val liveWallpaperStaticFrameCache = ConcurrentHashMap<String, ImageBitmap>()

val AMBIENT_IDLE_MINUTE_OPTIONS = listOf(0, 1, 3, 5, 10, 15, 20, 25, 30)
val LIVE_WALLPAPER_RESOLUTIONS = listOf("360p", "480p", "720p", "1080p")

enum class LiveWallpaperResolution(val id: String, val label: String) {
    RES_360P("360p", "360p"),
    RES_480P("480p", "480p"),
    RES_720P("720p", "720p (Default)"),
    RES_1080P("1080p", "1080p");

    companion object {
        fun fromId(id: String?): LiveWallpaperResolution {
            if (id.isNullOrBlank()) return RES_720P
            return entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) } ?: RES_720P
        }
    }
}

data class LiveWallpaperItem(
    val id: String,
    val uri: String,
    val isBundled: Boolean
)

val BUNDLED_LIVE_WALLPAPER_FILES = listOf(
    "live_wp_01_cyber_nebula.mp4",
    "live_wp_02_aurora_waves.mp4",
    "live_wp_03_neon_horizon.mp4"
)

val BUNDLED_LIVE_WALLPAPERS: List<LiveWallpaperItem> = BUNDLED_LIVE_WALLPAPER_FILES.mapIndexed { idx, fileName ->
    LiveWallpaperItem(
        id = "bundled_live_${idx}_$fileName",
        uri = "asset:///live_wallpapers/$fileName",
        isBundled = true
    )
}

/**
 * All 19 App Icon Shapes selectable in the Control Panel for both Home and Apps tabs.
 * Note: `RECTANGLE` renders wide 16:9 Android TV banner cards (`loadBanner(pm)` with styled 16:9 fallback).
 */
enum class TvAppIconShape(val id: String, val displayName: String) {
    CIRCLE("Circle", "Circle"),
    RECTANGLE("Rectangle", "Rectangle"),
    LEAF("Leaf", "Leaf"),
    SQUARE("Square", "Square"),
    SQUIRCLE("Squircle", "Squircle"),
    ROUNDED_SQUARE("Rounded Square", "Rounded Square"),
    HEXAGON("Hexagon", "Hexagon"),
    OCTAGON("Octagon", "Octagon"),
    DIAMOND("Diamond", "Diamond"),
    PENTAGON("Pentagon", "Pentagon"),
    SHIELD("Shield", "Shield"),
    TEARDROP("Teardrop", "Teardrop"),
    PEBBLE("Pebble", "Pebble"),
    CLOVER("Clover", "Clover"),
    FLOWER("Flower", "Flower"),
    STARBURST("Starburst", "Starburst"),
    CAPSULE("Capsule", "Capsule"),
    CUT_CORNER("Cut Corner", "Cut Corner"),
    SCALLOPED_BADGE("Scalloped Badge", "Scalloped Badge");

    val label: String get() = displayName
    val isWideBanner: Boolean get() = this == RECTANGLE

    companion object {
        fun fromId(id: String?): TvAppIconShape {
            if (id.isNullOrBlank()) return CIRCLE
            val trimmed = id.trim()
            if (trimmed.startsWith("Rectangle", ignoreCase = true)) return RECTANGLE
            return entries.firstOrNull {
                it.id.equals(trimmed, ignoreCase = true) ||
                    it.displayName.equals(trimmed, ignoreCase = true)
            } ?: CIRCLE
        }
    }
}

/**
 * Returns the Compose [Shape] for any of the 19 [TvAppIconShape] options.
 */
fun resolveComposeShapeForAppIcon(shapeType: TvAppIconShape): Shape {
    return when (shapeType) {
        TvAppIconShape.CIRCLE -> CircleShape
        TvAppIconShape.RECTANGLE -> RoundedCornerShape(14.dp)
        TvAppIconShape.LEAF -> RoundedCornerShape(
            topStart = 28.dp,
            topEnd = 6.dp,
            bottomEnd = 28.dp,
            bottomStart = 6.dp
        )
        TvAppIconShape.SQUARE -> RoundedCornerShape(4.dp)
        TvAppIconShape.SQUIRCLE -> RoundedCornerShape(percent = 34)
        TvAppIconShape.ROUNDED_SQUARE -> RoundedCornerShape(18.dp)
        TvAppIconShape.HEXAGON -> RegularPolygonShape(sides = 6, rotationDeg = -90f)
        TvAppIconShape.OCTAGON -> RegularPolygonShape(sides = 8, rotationDeg = 22.5f)
        TvAppIconShape.DIAMOND -> RegularPolygonShape(sides = 4, rotationDeg = 0f)
        TvAppIconShape.PENTAGON -> RegularPolygonShape(sides = 5, rotationDeg = -90f)
        TvAppIconShape.SHIELD -> ShieldIconShape
        TvAppIconShape.TEARDROP -> RoundedCornerShape(
            topStartPercent = 50,
            topEndPercent = 50,
            bottomEndPercent = 12,
            bottomStartPercent = 50
        )
        TvAppIconShape.PEBBLE -> RoundedCornerShape(
            topStartPercent = 44,
            topEndPercent = 32,
            bottomEndPercent = 46,
            bottomStartPercent = 28
        )
        TvAppIconShape.CLOVER -> StarScallopShape(lobes = 4, innerRatio = 0.78f)
        TvAppIconShape.FLOWER -> StarScallopShape(lobes = 6, innerRatio = 0.80f)
        TvAppIconShape.STARBURST -> StarScallopShape(lobes = 8, innerRatio = 0.82f)
        TvAppIconShape.CAPSULE -> RoundedCornerShape(percent = 50)
        TvAppIconShape.CUT_CORNER -> CutCornerShape(percent = 24)
        TvAppIconShape.SCALLOPED_BADGE -> StarScallopShape(lobes = 12, innerRatio = 0.88f)
    }
}

private data class RegularPolygonShape(
    val sides: Int,
    val rotationDeg: Float = 0f
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = min(cx, cy) * 0.98f
        val startRad = rotationDeg * (PI.toFloat() / 180f)
        for (i in 0 until sides) {
            val angle = startRad + (2f * PI.toFloat() * i / sides)
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

private data object ShieldIconShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.03f)
            lineTo(w * 0.93f, h * 0.18f)
            lineTo(w * 0.88f, h * 0.60f)
            quadraticTo(w * 0.78f, h * 0.86f, w * 0.5f, h * 0.97f)
            quadraticTo(w * 0.22f, h * 0.86f, w * 0.12f, h * 0.60f)
            lineTo(w * 0.07f, h * 0.18f)
            close()
        }
        return Outline.Generic(path)
    }
}

private data class StarScallopShape(
    val lobes: Int,
    val innerRatio: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = min(cx, cy) * 0.98f
        val innerR = outerR * innerRatio
        val totalPoints = lobes * 2
        for (i in 0 until totalPoints) {
            val r = if (i % 2 == 0) outerR else innerR
            val angle = (PI.toFloat() * 2f * i / totalPoints) - (PI.toFloat() / 2f)
            val x = cx + r * cos(angle)
            val y = cy + r * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * Extracts the bundled sample MP4 live wallpapers from `assets/live_wallpapers/` into
 * `context.filesDir/live_wallpapers/` on first access so `MediaPlayer` can play them directly.
 */
suspend fun ensureBundledLiveWallpapersExtracted(context: Context): List<File> = withContext(Dispatchers.IO) {
    val dir = File(context.filesDir, "live_wallpapers").apply { mkdirs() }
    for (assetName in BUNDLED_LIVE_WALLPAPER_FILES) {
        val target = File(dir, assetName)
        if (!target.exists() || target.length() < 1024L) {
            runCatching {
                context.assets.open("live_wallpapers/$assetName").use { input ->
                    FileOutputStream(target).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
    dir.listFiles()
        ?.filter { it.isFile && it.extension.lowercase() in setOf("mp4", "webm", "mkv", "mov") }
        ?.sortedBy { it.name }
        ?: emptyList()
}

/**
 * Copies a user-selected video file or content URI into `context.filesDir/live_wallpapers/`
 * so it persists in the Live Wallpapers grid with a Delete button (`!isBundled`).
 */
suspend fun copyUserLiveWallpaperToInternalDir(context: Context, rawUriOrPath: String): File? = withContext(Dispatchers.IO) {
    if (rawUriOrPath.isBlank()) return@withContext null
    val dir = File(context.filesDir, "live_wallpapers").apply { mkdirs() }
    try {
        if (rawUriOrPath.startsWith("content://")) {
            val uri = android.net.Uri.parse(rawUriOrPath)
            val safeName = "user_live_${System.currentTimeMillis()}.mp4"
            val dest = File(dir, safeName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            if (dest.exists() && dest.length() > 0L) return@withContext dest
        } else {
            val cleanPath = rawUriOrPath.removePrefix("file://").trim()
            val src = File(cleanPath)
            if (src.exists() && src.isFile) {
                if (src.parentFile?.absolutePath == dir.absolutePath) {
                    return@withContext src
                }
                val ext = src.extension.ifBlank { "mp4" }
                val base = src.nameWithoutExtension.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(28)
                val destName = "user_live_${System.currentTimeMillis()}_${base}.$ext"
                val dest = File(dir, destName)
                src.inputStream().use { input ->
                    FileOutputStream(dest).use { output ->
                        input.copyTo(output)
                    }
                }
                if (dest.exists() && dest.length() > 0L) return@withContext dest
            }
        }
    } catch (_: Exception) {
    }
    null
}

/**
 * Resolves a live wallpaper identifier (either `asset:///live_wallpapers/live_wp_...`, a filename, or an absolute file path)
 * into a readable local [File].
 */
fun resolveLiveWallpaperFile(context: Context, rawUriOrName: String): File? {
    if (rawUriOrName.isBlank()) return null
    val dir = File(context.filesDir, "live_wallpapers").apply { mkdirs() }
    val cleanPath = rawUriOrName
        .removePrefix("asset:///live_wallpapers/")
        .removePrefix("file://")
        .trim()
    val directFile = File(cleanPath)
    if (directFile.exists() && directFile.isFile) return directFile
    val inLiveDir = File(dir, directFile.name)
    if (inLiveDir.exists() && inLiveDir.isFile) return inLiveDir
    // If it's a bundled asset not yet copied, copy synchronously on fallback
    if (directFile.name in BUNDLED_LIVE_WALLPAPER_FILES) {
        runCatching {
            context.assets.open("live_wallpapers/${directFile.name}").use { input ->
                FileOutputStream(inLiveDir).use { output ->
                    input.copyTo(output)
                }
            }
        }
        if (inLiveDir.exists()) return inLiveDir
    }
    return null
}

fun resolutionDimensionsForTag(resTag: String): Pair<Int, Int> {
    return when (resTag.lowercase()) {
        "360p" -> 640 to 360
        "480p" -> 854 to 480
        "1080p" -> 1920 to 1080
        else -> 1280 to 720 // 720p default
    }
}

private class ManagedWallpaperMediaPlayer(
    val player: MediaPlayer,
    var surface: Surface? = null,
    @Volatile var prepared: Boolean = false,
    @Volatile var released: Boolean = false
) {
    fun releaseSafely() {
        if (released) return
        released = true
        prepared = false
        runCatching {
            if (player.isPlaying) {
                player.stop()
            }
        }
        runCatching { player.setSurface(null) }
        runCatching { player.release() }
        runCatching { surface?.release() }
        surface = null
    }
}

/**
 * Full-Screen Live Video Wallpaper Background Engine:
 * - Plays at the selected resolution (`720p` by default, configurable in Control Panel: `360p`, `480p`, `720p`, `1080p`).
 * - Strictly pauses playback whenever `shouldPlay` is false (i.e. whenever anything other than Home tab, Apps tab, or 4K Ambient Mode is open).
 * - Automatically resumes playback on `ON_RESUME` when returning from File Explorer, RNA AI, or any activity/overlay.
 * - Uses `key(fileToPlay.absolutePath)` so replacing one downloaded live wallpaper with another recreates the Surface & MediaPlayer cleanly.
 * - Strictly gates `start()`, `pause()`, and `stop()` on `prepared && !released` so `MediaPlayer` never enters state `4`/`0` error `(-38, 0)`.
 */
@Composable
fun TvLiveWallpaperBackground(
    liveWallpaperUri: String,
    configuredResolution: String,
    shouldPlay: Boolean,
    onAutoResolutionStepDown: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var resolvedFile by remember(liveWallpaperUri) { mutableStateOf<File?>(null) }
    var effectiveRes by remember(configuredResolution) { mutableStateOf(configuredResolution) }
    var autoPausedBySevereLag by remember(liveWallpaperUri, configuredResolution) { mutableStateOf(false) }
    var managedPlayerRef by remember(liveWallpaperUri) { mutableStateOf<ManagedWallpaperMediaPlayer?>(null) }
    var isPlayerPrepared by remember(liveWallpaperUri) { mutableStateOf(false) }
    var textureViewRef by remember(liveWallpaperUri) { mutableStateOf<TextureView?>(null) }

    val latestShouldPlay by androidx.compose.runtime.rememberUpdatedState(shouldPlay)
    val latestAutoPaused by androidx.compose.runtime.rememberUpdatedState(autoPausedBySevereLag)

    LaunchedEffect(liveWallpaperUri) {
        ensureBundledLiveWallpapersExtracted(context)
        resolvedFile = withContext(Dispatchers.IO) {
            resolveLiveWallpaperFile(context, liveWallpaperUri)
        }
    }

    // Update fixed buffer resolution whenever effectiveRes changes
    LaunchedEffect(effectiveRes, textureViewRef) {
        val tv = textureViewRef ?: return@LaunchedEffect
        val (w, h) = resolutionDimensionsForTag(effectiveRes)
        runCatching {
            tv.surfaceTexture?.setDefaultBufferSize(w, h)
        }
    }

    // Strict playback gating: only play/pause when prepared && !released
    LaunchedEffect(shouldPlay, autoPausedBySevereLag, managedPlayerRef, isPlayerPrepared) {
        val holder = managedPlayerRef ?: return@LaunchedEffect
        if (holder.released || !holder.prepared || !isPlayerPrepared) return@LaunchedEffect
        runCatching {
            if (shouldPlay && !autoPausedBySevereLag) {
                if (!holder.player.isPlaying) {
                    holder.player.start()
                }
            } else {
                if (holder.player.isPlaying) {
                    holder.player.pause()
                }
            }
        }
    }

    // Resume playback when returning to MainActivity from File Explorer, RNA AI, or system dialogs
    DisposableEffect(lifecycleOwner, managedPlayerRef) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            val holder = managedPlayerRef ?: return@LifecycleEventObserver
            if (holder.released || !holder.prepared) return@LifecycleEventObserver
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    if (latestShouldPlay && !latestAutoPaused) {
                        runCatching {
                            if (!holder.player.isPlaying) {
                                holder.player.start()
                            }
                        }
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    runCatching {
                        if (holder.player.isPlaying) {
                            holder.player.pause()
                        }
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Choreographer frame-time lag detector while live video is playing
    DisposableEffect(shouldPlay, effectiveRes, autoPausedBySevereLag) {
        if (!shouldPlay || autoPausedBySevereLag) {
            return@DisposableEffect onDispose {}
        }
        val choreographer = Choreographer.getInstance()
        var lastFrameNanos = 0L
        var consecutiveJankFrames = 0
        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (lastFrameNanos != 0L) {
                    val deltaMs = (frameTimeNanos - lastFrameNanos) / 1_000_000L
                    if (deltaMs > 85L) {
                        consecutiveJankFrames++
                        if (consecutiveJankFrames >= 14) {
                            consecutiveJankFrames = 0
                            when (effectiveRes.lowercase()) {
                                "1080p" -> {
                                    effectiveRes = "720p"
                                    onAutoResolutionStepDown("720p")
                                }
                                "720p" -> {
                                    effectiveRes = "480p"
                                    onAutoResolutionStepDown("480p")
                                }
                                "480p" -> {
                                    effectiveRes = "360p"
                                    onAutoResolutionStepDown("360p")
                                }
                                else -> {
                                    autoPausedBySevereLag = true
                                }
                            }
                        }
                    } else if (deltaMs < 36L) {
                        consecutiveJankFrames = (consecutiveJankFrames - 1).coerceAtLeast(0)
                    }
                }
                lastFrameNanos = frameTimeNanos
                if (shouldPlay && !autoPausedBySevereLag) {
                    choreographer.postFrameCallback(this)
                }
            }
        }
        choreographer.postFrameCallback(callback)
        onDispose {
            runCatching { choreographer.removeFrameCallback(callback) }
        }
    }

    val fileToPlay = resolvedFile ?: return

    DisposableEffect(fileToPlay.absolutePath) {
        onDispose {
            managedPlayerRef?.releaseSafely()
            managedPlayerRef = null
            isPlayerPrepared = false
        }
    }

    androidx.compose.runtime.key(fileToPlay.absolutePath) {
        AndroidView(
            modifier = modifier
                .fillMaxSize()
                .testTag("tv_live_wallpaper_video_surface"),
            factory = { ctx ->
                TextureView(ctx).apply {
                    textureViewRef = this
                    isOpaque = true
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                            val (bufW, bufH) = resolutionDimensionsForTag(effectiveRes)
                            runCatching { surfaceTexture.setDefaultBufferSize(bufW, bufH) }
                            managedPlayerRef?.releaseSafely()
                            managedPlayerRef = null
                            isPlayerPrepared = false

                            val surf = Surface(surfaceTexture)
                            val mp = MediaPlayer()
                            val holder = ManagedWallpaperMediaPlayer(player = mp, surface = surf)
                            managedPlayerRef = holder

                            runCatching {
                                mp.setDataSource(fileToPlay.absolutePath)
                                mp.setSurface(surf)
                                mp.isLooping = true
                                mp.setVolume(0f, 0f)
                                mp.setOnErrorListener { _, _, _ ->
                                    holder.prepared = false
                                    isPlayerPrepared = false
                                    true
                                }
                                mp.setOnPreparedListener { preparedPlayer ->
                                    if (holder.released) return@setOnPreparedListener
                                    holder.prepared = true
                                    isPlayerPrepared = true
                                    if (latestShouldPlay && !latestAutoPaused) {
                                        runCatching {
                                            if (!preparedPlayer.isPlaying) {
                                                preparedPlayer.start()
                                            }
                                        }
                                    }
                                }
                                mp.prepareAsync()
                            }.onFailure {
                                holder.releaseSafely()
                                if (managedPlayerRef === holder) {
                                    managedPlayerRef = null
                                    isPlayerPrepared = false
                                }
                            }
                        }

                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                            val (bufW, bufH) = resolutionDimensionsForTag(effectiveRes)
                            runCatching { surface.setDefaultBufferSize(bufW, bufH) }
                        }

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            managedPlayerRef?.releaseSafely()
                            managedPlayerRef = null
                            isPlayerPrepared = false
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                    }
                }
            },
            update = { view ->
                textureViewRef = view
                val holder = managedPlayerRef
                if (holder != null && !holder.released && holder.prepared) {
                    runCatching {
                        if (shouldPlay && !autoPausedBySevereLag && !holder.player.isPlaying) {
                            holder.player.start()
                        } else if ((!shouldPlay || autoPausedBySevereLag) && holder.player.isPlaying) {
                            holder.player.pause()
                        }
                    }
                }
            }
        )
    }
}

/**
 * 3-per-row Live Wallpaper Preview Card:
 * - By default (`playPreview = false`), renders a static thumbnail frame with a Play badge without
 *   playing video streams in the preview grid.
 * - When `playPreview = true` (enabled in Control Panel), plays the live video preview at 360p (`640x360`).
 * - Has NO subtext underneath the card (per user specification).
 */
@Composable
fun TvLiveWallpaperPreviewCard(
    videoFile: File,
    isSelected: Boolean,
    playPreview: Boolean = false,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var previewHolder by remember { mutableStateOf<ManagedWallpaperMediaPlayer?>(null) }
    var staticThumbnail by remember(videoFile.absolutePath) {
        mutableStateOf(liveWallpaperStaticFrameCache[videoFile.absolutePath])
    }

    LaunchedEffect(videoFile.absolutePath, playPreview) {
        if (!playPreview) {
            previewHolder?.releaseSafely()
            previewHolder = null
            if (staticThumbnail == null) {
                val extracted = withContext(Dispatchers.IO) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(videoFile.absolutePath)
                        val rawBmp = retriever.getFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            ?: retriever.getFrameAtTime(0L)
                        rawBmp?.asImageBitmap()
                    } catch (_: Exception) {
                        null
                    } finally {
                        runCatching { retriever.release() }
                    }
                }
                if (extracted != null) {
                    liveWallpaperStaticFrameCache[videoFile.absolutePath] = extracted
                    staticThumbnail = extracted
                }
            }
        }
    }

    DisposableEffect(videoFile.absolutePath, playPreview) {
        onDispose {
            previewHolder?.releaseSafely()
            previewHolder = null
        }
    }

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1f,
        label = "live_wp_card_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF121624))
            .border(
                width = when {
                    isFocused -> 3.dp
                    isSelected -> 2.5.dp
                    else -> 1.dp
                },
                color = when {
                    isFocused -> Color.White
                    isSelected -> CyanPrimary
                    else -> Color(0x33FFFFFF)
                },
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onSelect)
            .testTag("live_wallpaper_card_${videoFile.name}")
    ) {
        if (playPreview) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    TextureView(ctx).apply {
                        isOpaque = true
                        isFocusable = false
                        isFocusableInTouchMode = false
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                // 360p preview resolution (640x360)
                                runCatching { st.setDefaultBufferSize(640, 360) }
                                previewHolder?.releaseSafely()
                                previewHolder = null

                                val surf = Surface(st)
                                val mp = MediaPlayer()
                                val holder = ManagedWallpaperMediaPlayer(player = mp, surface = surf)
                                previewHolder = holder

                                runCatching {
                                    mp.setDataSource(videoFile.absolutePath)
                                    mp.setSurface(surf)
                                    mp.isLooping = true
                                    mp.setVolume(0f, 0f)
                                    mp.setOnErrorListener { _, _, _ ->
                                        holder.prepared = false
                                        true
                                    }
                                    mp.setOnPreparedListener { preparedPlayer ->
                                        if (holder.released) return@setOnPreparedListener
                                        holder.prepared = true
                                        runCatching {
                                            if (!preparedPlayer.isPlaying) {
                                                preparedPlayer.start()
                                            }
                                        }
                                    }
                                    mp.prepareAsync()
                                }.onFailure {
                                    holder.releaseSafely()
                                    if (previewHolder === holder) {
                                        previewHolder = null
                                    }
                                }
                            }

                            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                                runCatching { st.setDefaultBufferSize(640, 360) }
                            }

                            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                previewHolder?.releaseSafely()
                                previewHolder = null
                                return true
                            }

                            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                        }
                    }
                }
            )
        } else {
            val thumb = staticThumbnail
            if (thumb != null) {
                Image(
                    bitmap = thumb,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x44090D16))
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isFocused) Color.White else Color(0xCC161B2C),
                    border = BorderStroke(1.5.dp, if (isFocused) CyanPrimary else Color(0x55FFFFFF)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Live Wallpaper",
                            tint = if (isFocused) Color(0xFF090D16) else CyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        if (isSelected) {
            Surface(
                shape = CircleShape,
                color = EmeraldSuccess,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Active Live Wallpaper",
                        tint = Color(0xFF07130E),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TvLiveWallpaperBackground(
    videoUri: String,
    shouldPlay: Boolean,
    preferredResolution: LiveWallpaperResolution,
    performanceMode: Boolean = false,
    onAutoResolutionStepDown: (LiveWallpaperResolution) -> Unit,
    modifier: Modifier = Modifier
) {
    TvLiveWallpaperBackground(
        liveWallpaperUri = videoUri,
        configuredResolution = if (performanceMode) "360p" else preferredResolution.id,
        shouldPlay = shouldPlay && !performanceMode,
        onAutoResolutionStepDown = { steppedId ->
            onAutoResolutionStepDown(LiveWallpaperResolution.fromId(steppedId))
        },
        modifier = modifier
    )
}

@Composable
fun TvLiveWallpaperPreviewCard(
    item: LiveWallpaperItem,
    isSelected: Boolean,
    playPreview: Boolean = false,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var resolvedFile by remember(item.uri) {
        mutableStateOf(resolveLiveWallpaperFile(context, item.uri))
    }

    LaunchedEffect(item.uri) {
        if (resolvedFile == null) {
            ensureBundledLiveWallpapersExtracted(context)
            resolvedFile = withContext(Dispatchers.IO) {
                resolveLiveWallpaperFile(context, item.uri)
            }
        }
    }

    val file = resolvedFile
    if (file != null) {
        TvLiveWallpaperPreviewCard(
            videoFile = file,
            isSelected = isSelected,
            playPreview = playPreview,
            onSelect = onSelect,
            modifier = modifier
        )
    } else {
        val placeholderInteraction = remember { MutableInteractionSource() }
        val placeholderFocused by placeholderInteraction.collectIsFocusedAsState()
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF121624))
                .border(
                    width = if (placeholderFocused) 3.dp else if (isSelected) 2.5.dp else 1.dp,
                    color = if (placeholderFocused) Color.White else if (isSelected) CyanPrimary else Color(0x33FFFFFF),
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable(interactionSource = placeholderInteraction, indication = null, onClick = onSelect)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Loading Live Wallpaper",
                tint = if (placeholderFocused) Color.White else CyanPrimary.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

