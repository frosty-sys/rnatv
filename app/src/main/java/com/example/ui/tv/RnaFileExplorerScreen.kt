package com.example.ui.tv

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.TimedText
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.os.storage.StorageManager
import android.provider.Settings
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import com.example.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class FileExplorerPickerMode {
    NONE,
    STANDALONE_FULL,
    IMAGES_FOR_WALLPAPER,
    VIDEOS_FOR_LIVE_WALLPAPER
}

enum class FileExplorerLayoutMode(val label: String) {
    GRID_8("Grid View"),
    LIST("List View")
}

data class StorageVolumeTile(
    val id: String,
    val title: String,
    val subtitle: String,
    val path: File?,
    val categoryKey: String?,
    val icon: ImageVector,
    val accentColor: Color,
    val usedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val isStorageBar: Boolean = false
)

data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

data class MediaTrackOption(
    val index: Int,
    val label: String,
    val language: String,
    val isEmbeddedPlayerTrack: Boolean = true,
    val extractorTrackIndex: Int? = null,
    val subtitleOrdinal: Int = 0
)

data class ExplorerInstalledAppItem(
    val packageName: String,
    val label: String,
    val versionName: String,
    val sourceApkPath: String,
    val sizeBytes: Long,
    val iconBitmap: ImageBitmap?
)

private object ExplorerThumbnailCache {
    val bitmaps = ConcurrentHashMap<String, ImageBitmap>()
}

private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif", "heic")
private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "mov", "webm", "avi", "m2ts", "ts", "mts", "m4v", "3gp")
private val AUDIO_EXTENSIONS = setOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "opus")
private val DOC_EXTENSIONS = setOf("pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "md", "json", "xml", "csv", "html")
private val SUBTITLE_EXTENSIONS = setOf("srt", "vtt", "ass", "ssa")

fun isImageFile(file: File): Boolean = file.extension.lowercase(Locale.ROOT) in IMAGE_EXTENSIONS
fun isVideoFile(file: File): Boolean = file.extension.lowercase(Locale.ROOT) in VIDEO_EXTENSIONS
fun isAudioFile(file: File): Boolean = file.extension.lowercase(Locale.ROOT) in AUDIO_EXTENSIONS
fun isDocFile(file: File): Boolean = file.extension.lowercase(Locale.ROOT) in DOC_EXTENSIONS
fun isApkFile(file: File): Boolean = file.extension.equals("apk", ignoreCase = true)
fun isRnaFile(file: File): Boolean = file.extension.equals("rna", ignoreCase = true)
fun isSubtitleFile(file: File): Boolean = file.extension.lowercase(Locale.ROOT) in SUBTITLE_EXTENSIONS

fun hasFullStorageAccess(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

fun launchAllFilesAccessSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) {
        }
        try {
            val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            return
        } catch (_: Exception) {
        }
    }
    try {
        val appDetails = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(appDetails)
    } catch (_: Exception) {
    }
}

fun formatBytesSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}

fun buildFileExplorerDashboardTiles(context: Context): List<StorageVolumeTile> {
    val tiles = mutableListOf<StorageVolumeTile>()

    // 1. Main Storage
    val mainDir = Environment.getExternalStorageDirectory() ?: context.filesDir
    val (mainUsed, mainTotal) = getDirectoryStorageStats(mainDir)
    tiles.add(
        StorageVolumeTile(
            id = "main_storage",
            title = "Main Storage",
            subtitle = "${formatBytesSize(mainUsed)} / ${formatBytesSize(mainTotal)}",
            path = mainDir,
            categoryKey = null,
            icon = Icons.Default.Storage,
            accentColor = Color(0xFF38BDF8),
            usedBytes = mainUsed,
            totalBytes = mainTotal,
            isStorageBar = true
        )
    )

    // 2. All Available USB Drives
    val usbVolumes = detectMountedUsbVolumes(context, mainDir)
    usbVolumes.forEachIndexed { idx, (usbName, usbDir) ->
        val (uUsed, uTotal) = getDirectoryStorageStats(usbDir)
        tiles.add(
            StorageVolumeTile(
                id = "usb_${idx}_${usbDir.absolutePath}",
                title = usbName,
                subtitle = "${formatBytesSize(uUsed)} / ${formatBytesSize(uTotal)}",
                path = usbDir,
                categoryKey = null,
                icon = Icons.Default.Usb,
                accentColor = Color(0xFF10B981),
                usedBytes = uUsed,
                totalBytes = uTotal,
                isStorageBar = true
            )
        )
    }

    // 3. Downloads
    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        ?: File(mainDir, "Download")
    tiles.add(
        StorageVolumeTile(
            id = "cat_downloads",
            title = "Downloads",
            subtitle = "",
            path = downloadsDir,
            categoryKey = "DOWNLOADS",
            icon = Icons.Default.Download,
            accentColor = Color(0xFF6366F1)
        )
    )

    // 4. Images
    tiles.add(
        StorageVolumeTile(
            id = "cat_images",
            title = "Images",
            subtitle = "",
            path = null,
            categoryKey = "IMAGES",
            icon = Icons.Default.Image,
            accentColor = Color(0xFFEC4899)
        )
    )

    // 5. Audio
    tiles.add(
        StorageVolumeTile(
            id = "cat_audio",
            title = "Audio",
            subtitle = "",
            path = null,
            categoryKey = "AUDIO",
            icon = Icons.Default.AudioFile,
            accentColor = Color(0xFFF59E0B)
        )
    )

    // 6. Video
    tiles.add(
        StorageVolumeTile(
            id = "cat_video",
            title = "Video",
            subtitle = "",
            path = null,
            categoryKey = "VIDEO",
            icon = Icons.Default.Movie,
            accentColor = Color(0xFFA855F7)
        )
    )

    // 7. Documents
    tiles.add(
        StorageVolumeTile(
            id = "cat_docs",
            title = "Documents",
            subtitle = "",
            path = null,
            categoryKey = "DOCUMENTS",
            icon = Icons.Default.Description,
            accentColor = Color(0xFF14B8A6)
        )
    )

    // 8. Apps (In-App Installed Apps & APKs Manager)
    tiles.add(
        StorageVolumeTile(
            id = "cat_apps",
            title = "Apps",
            subtitle = "",
            path = null,
            categoryKey = "SYSTEM_APPS",
            icon = Icons.Default.Apps,
            accentColor = Color(0xFFEF4444)
        )
    )

    return tiles
}

private fun getDirectoryStorageStats(dir: File): Pair<Long, Long> {
    return try {
        val stat = StatFs(dir.absolutePath)
        val total = stat.blockCountLong * stat.blockSizeLong
        val avail = stat.availableBlocksLong * stat.blockSizeLong
        val used = (total - avail).coerceAtLeast(0L)
        Pair(used, total.coerceAtLeast(1L))
    } catch (_: Exception) {
        Pair(0L, 1L)
    }
}

private fun detectMountedUsbVolumes(context: Context, mainDir: File): List<Pair<String, File>> {
    val result = mutableListOf<Pair<String, File>>()
    val seenPaths = mutableSetOf(mainDir.canonicalPathOrAbsolute())

    try {
        val sm = context.getSystemService(Context.STORAGE_SERVICE) as? StorageManager
        if (sm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            for (vol in sm.storageVolumes) {
                if (vol.isRemovable) {
                    val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        vol.directory
                    } else {
                        try {
                            val m = vol.javaClass.getMethod("getPathFile")
                            m.invoke(vol) as? File
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (dir != null && dir.exists()) {
                        val canonical = dir.canonicalPathOrAbsolute()
                        if (seenPaths.add(canonical)) {
                            val desc = vol.getDescription(context)?.takeIf { it.isNotBlank() }
                                ?: "USB Drive (${dir.name})"
                            result.add(desc to dir)
                        }
                    }
                }
            }
        }
    } catch (_: Exception) {}

    try {
        val storageRoot = File("/storage")
        storageRoot.listFiles()?.forEach { child ->
            if (child.isDirectory && child.canRead() &&
                child.name != "emulated" && child.name != "self"
            ) {
                val canonical = child.canonicalPathOrAbsolute()
                if (seenPaths.add(canonical)) {
                    result.add("USB (${child.name})" to child)
                }
            }
        }
    } catch (_: Exception) {}

    return result
}

private fun File.canonicalPathOrAbsolute(): String = try {
    canonicalPath
} catch (_: Exception) {
    absolutePath
}

suspend fun scanFilesRecursive(
    context: Context,
    filterPredicate: (File) -> Boolean,
    maxResults: Int = 500
): List<File> = withContext(Dispatchers.IO) {
    val results = mutableListOf<File>()
    val visited = mutableSetOf<String>()
    val roots = mutableListOf<File>()

    Environment.getExternalStorageDirectory()?.let { roots.add(it) }
    listOf(
        Environment.DIRECTORY_DOWNLOADS,
        Environment.DIRECTORY_MOVIES,
        Environment.DIRECTORY_MUSIC,
        Environment.DIRECTORY_PICTURES,
        Environment.DIRECTORY_DCIM,
        Environment.DIRECTORY_DOCUMENTS
    ).forEach { pubType ->
        runCatching {
            Environment.getExternalStoragePublicDirectory(pubType)?.let { roots.add(it) }
        }
    }
    context.filesDir?.let { roots.add(it) }
    context.getExternalFilesDir(null)?.let { roots.add(it) }
    detectMountedUsbVolumes(context, Environment.getExternalStorageDirectory() ?: context.filesDir)
        .forEach { roots.add(it.second) }

    fun walk(dir: File, depth: Int) {
        if (depth > 7 || results.size >= maxResults) return
        val path = dir.canonicalPathOrAbsolute()
        if (!visited.add(path)) return
        val children = try {
            dir.listFiles()
        } catch (_: Exception) {
            null
        } ?: return

        for (child in children) {
            if (results.size >= maxResults) break
            if (child.name.startsWith(".")) continue
            if (child.isDirectory) {
                if (child.name != "Android") {
                    walk(child, depth + 1)
                }
            } else if (filterPredicate(child)) {
                results.add(child)
            }
        }
    }

    for (root in roots) {
        if (root.exists()) walk(root, 0)
    }
    results.distinctBy { it.absolutePath }.sortedByDescending { it.lastModified() }
}

private fun resolveFileContentUri(context: Context, file: File): Uri {
    return try {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (_: Exception) {
        Uri.fromFile(file)
    }
}

private fun resolveMimeTypeForFile(file: File): String {
    val ext = file.extension.lowercase(Locale.ROOT)
    if (ext == "apk") return "application/vnd.android.package-archive"
    if (ext == "rna") return "application/octet-stream"
    if (ext == "mkv") return "video/x-matroska"
    val mapped = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    return mapped ?: "*/*"
}

private fun launchOpenWithChooser(context: Context, file: File) {
    try {
        val uri = resolveFileContentUri(context, file)
        val mime = resolveMimeTypeForFile(file)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(viewIntent, "Open ${file.name} with").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    } catch (e: Exception) {
        Toast.makeText(context, "No app available to open ${file.name}", Toast.LENGTH_SHORT).show()
    }
}

private fun tryOpenFileWithSupportedApp(context: Context, file: File): Boolean {
    return try {
        val uri = resolveFileContentUri(context, file)
        val mime = resolveMimeTypeForFile(file)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val resolved = context.packageManager.queryIntentActivities(viewIntent, 0)
            .filter { it.activityInfo?.packageName != context.packageName }
        if (resolved.isNotEmpty()) {
            val chooser = Intent.createChooser(viewIntent, "Open ${file.name}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            true
        } else {
            false
        }
    } catch (_: Exception) {
        false
    }
}

private fun canInstallUnknownPackages(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.packageManager.canRequestPackageInstalls()
    } else {
        true
    }
}

private fun openUnknownSourcesPermissionSettings(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return
        } catch (_: Exception) {}
    }
    try {
        val fallback = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(fallback)
    } catch (_: Exception) {}
}

private fun launchPackageInstallerForApk(context: Context, apkFile: File) {
    try {
        val apkUri = resolveFileContentUri(context, apkFile)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not launch Package Installer: ${e.message}", Toast.LENGTH_LONG).show()
    }
}

private fun launchRnaPackageInstaller(context: Context, rnaFile: File) {
    try {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.fromFile(rnaFile)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open .rna installer: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * M3 Expressive Dark-Mode File Explorer & Media Suite ("File Explorer").
 */
@Composable
fun RnaFileExplorerScreen(
    initialPath: String? = null,
    initialCategory: String? = null,
    pickerMode: FileExplorerPickerMode = FileExplorerPickerMode.NONE,
    onFilePicked: ((File) -> Unit)? = null,
    onSelectWallpaperImage: ((String) -> Unit)? = null,
    onSelectLiveWallpaperVideo: ((String) -> Unit)? = null,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var layoutMode by remember { mutableStateOf(FileExplorerLayoutMode.LIST) }
    var hasStoragePerm by remember { mutableStateOf(hasFullStorageAccess(context)) }
    val backBtnFocusRequester = remember { FocusRequester() }
    val firstItemFocusRequester = remember { FocusRequester() }
    val isMediaPickerMode = pickerMode == FileExplorerPickerMode.IMAGES_FOR_WALLPAPER ||
        pickerMode == FileExplorerPickerMode.VIDEOS_FOR_LIVE_WALLPAPER
    val defaultPickerCategory = when (pickerMode) {
        FileExplorerPickerMode.IMAGES_FOR_WALLPAPER -> "IMAGES"
        FileExplorerPickerMode.VIDEOS_FOR_LIVE_WALLPAPER -> "VIDEO"
        else -> null
    }

    val legacyStoragePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasStoragePerm = hasFullStorageAccess(context)
    }

    var currentDirectory by remember {
        mutableStateOf<File?>(initialPath?.let { File(it) }?.takeIf { it.exists() && it.isDirectory })
    }
    var activeCategory by remember {
        mutableStateOf<String?>(
            when (pickerMode) {
                FileExplorerPickerMode.IMAGES_FOR_WALLPAPER -> "IMAGES"
                FileExplorerPickerMode.VIDEOS_FOR_LIVE_WALLPAPER -> "VIDEO"
                FileExplorerPickerMode.NONE, FileExplorerPickerMode.STANDALONE_FULL -> initialCategory
            }
        )
    }

    // Apps category sub-pill state: 0 = Installed Apps ("Apps"), 1 = APK Files ("APK")
    var appsCategorySubTab by remember { mutableIntStateOf(0) }
    var installedAppsList by remember { mutableStateOf<List<ExplorerInstalledAppItem>>(emptyList()) }
    var selectedInstalledAppForAction by remember { mutableStateOf<ExplorerInstalledAppItem?>(null) }

    // Pending APK waiting for "Install Unknown Apps" permission
    var pendingApkInstallFile by remember { mutableStateOf<File?>(null) }
    var showApkPermissionDialog by remember { mutableStateOf(false) }

    var directoryFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoadingFiles by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val nowGranted = hasFullStorageAccess(context)
                if (nowGranted != hasStoragePerm) {
                    hasStoragePerm = nowGranted
                }
                val waitingApk = pendingApkInstallFile
                if (waitingApk != null && canInstallUnknownPackages(context)) {
                    showApkPermissionDialog = false
                    pendingApkInstallFile = null
                    launchPackageInstallerForApk(context, waitingApk)
                }
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasStoragePerm && Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            legacyStoragePermLauncher.launch(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    // Clipboard for Copy / Move operations
    var clipboardFile by remember { mutableStateOf<File?>(null) }
    var clipboardIsMove by remember { mutableStateOf(false) }

    // Long-press context menu state
    var contextMenuTargetFile by remember { mutableStateOf<File?>(null) }
    var renameTargetFile by remember { mutableStateOf<File?>(null) }
    var renameInputText by remember { mutableStateOf("") }
    var showRenameKeyboard by remember { mutableStateOf(false) }
    var propertiesTargetFile by remember { mutableStateOf<File?>(null) }

    // Built-in Media Viewers
    var activeImageViewerFile by remember { mutableStateOf<File?>(null) }
    var activeAudioPlayerFile by remember { mutableStateOf<File?>(null) }
    var activeVideoPlayerFile by remember { mutableStateOf<File?>(null) }

    // Load files or installed apps when directory or category changes
    LaunchedEffect(currentDirectory, activeCategory, appsCategorySubTab, refreshTrigger, hasStoragePerm) {
        if (currentDirectory != null) {
            isLoadingFiles = true
            val dir = currentDirectory!!
            val list = withContext(Dispatchers.IO) {
                try {
                    val direct = dir.listFiles()
                        ?.filter { !it.name.startsWith(".") }
                        ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) })
                        .orEmpty()
                    if (direct.isNotEmpty()) {
                        direct
                    } else if (dir.absolutePath == Environment.getExternalStorageDirectory()?.absolutePath) {
                        listOf("Download", "Movies", "Music", "Pictures", "DCIM", "Documents", "Android")
                            .map { File(dir, it) }
                            .filter { it.exists() }
                    } else {
                        emptyList()
                    }
                } catch (_: Exception) {
                    emptyList()
                }
            }
            directoryFiles = list
            isLoadingFiles = false
        } else if (activeCategory != null) {
            isLoadingFiles = true
            if (activeCategory == "SYSTEM_APPS") {
                if (appsCategorySubTab == 0) {
                    val loadedApps = withContext(Dispatchers.IO) {
                        val pm = context.packageManager
                        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                            addCategory(Intent.CATEGORY_LAUNCHER)
                        }
                        val leanbackIntent = Intent(Intent.ACTION_MAIN, null).apply {
                            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                        }
                        val resolved = (pm.queryIntentActivities(mainIntent, 0) + pm.queryIntentActivities(leanbackIntent, 0))
                            .distinctBy { it.activityInfo.packageName }
                        resolved.mapNotNull { info ->
                            try {
                                val pkg = info.activityInfo.packageName
                                val appInfo = info.activityInfo.applicationInfo
                                val label = info.loadLabel(pm).toString()
                                val pInfo = runCatching { pm.getPackageInfo(pkg, 0) }.getOrNull()
                                val ver = pInfo?.versionName ?: "1.0"
                                val apkPath = appInfo.sourceDir.orEmpty()
                                val size = apkPath.takeIf { it.isNotBlank() }?.let { File(it).length() } ?: 0L
                                val bmp = ExplorerThumbnailCache.bitmaps["app_$pkg"] ?: runCatching {
                                    info.loadIcon(pm).toBitmap(96, 96).asImageBitmap().also {
                                        ExplorerThumbnailCache.bitmaps["app_$pkg"] = it
                                    }
                                }.getOrNull()
                                ExplorerInstalledAppItem(
                                    packageName = pkg,
                                    label = label,
                                    versionName = ver,
                                    sourceApkPath = apkPath,
                                    sizeBytes = size,
                                    iconBitmap = bmp
                                )
                            } catch (_: Exception) {
                                null
                            }
                        }.sortedBy { it.label.lowercase(Locale.ROOT) }
                    }
                    installedAppsList = loadedApps
                    directoryFiles = emptyList()
                } else {
                    val apkFiles = scanFilesRecursive(context, ::isApkFile)
                    directoryFiles = apkFiles
                }
                isLoadingFiles = false
            } else {
                val list = when (activeCategory) {
                    "DOWNLOADS" -> {
                        val dl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val appDl = File(context.filesDir, "browser_downloads").apply { mkdirs() }
                        val scannedDl = scanFilesRecursive(context, filterPredicate = { f ->
                            f.parentFile?.name?.equals("Download", ignoreCase = true) == true ||
                                f.parentFile?.name?.equals("Downloads", ignoreCase = true) == true
                        })
                        withContext(Dispatchers.IO) {
                            val a = dl?.listFiles()?.toList().orEmpty()
                            val b = appDl.listFiles()?.toList().orEmpty()
                            (a + b + scannedDl).distinctBy { it.absolutePath }.sortedByDescending { it.lastModified() }
                        }
                    }
                    "IMAGES" -> {
                        val scanned = scanFilesRecursive(context, ::isImageFile)
                        val bundledPreviewDir = File(context.filesDir, "sample_wallpapers").apply { mkdirs() }
                        withContext(Dispatchers.IO) {
                            if ((bundledPreviewDir.listFiles()?.size ?: 0) < 8) {
                                try {
                                    context.assets.list("wallpapers")?.take(8)?.forEach { name ->
                                        val target = File(bundledPreviewDir, name)
                                        if (!target.exists()) {
                                            context.assets.open("wallpapers/$name").use { input ->
                                                FileOutputStream(target).use { output -> input.copyTo(output) }
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                            val samples = bundledPreviewDir.listFiles()?.filter(::isImageFile).orEmpty()
                            (scanned + samples).distinctBy { it.absolutePath }
                        }
                    }
                    "AUDIO" -> scanFilesRecursive(context, ::isAudioFile)
                    "VIDEO" -> {
                        val scanned = scanFilesRecursive(context, ::isVideoFile)
                        val bundledLiveDir = File(context.filesDir, "live_wallpapers").apply { mkdirs() }
                        withContext(Dispatchers.IO) {
                            try {
                                context.assets.list("live_wallpapers")?.forEach { name ->
                                    val target = File(bundledLiveDir, name)
                                    if (!target.exists()) {
                                        context.assets.open("live_wallpapers/$name").use { input ->
                                            FileOutputStream(target).use { output -> input.copyTo(output) }
                                        }
                                    }
                                }
                            } catch (_: Exception) {}
                            val samples = bundledLiveDir.listFiles()?.filter(::isVideoFile).orEmpty()
                            (scanned + samples).distinctBy { it.absolutePath }
                        }
                    }
                    "DOCUMENTS" -> scanFilesRecursive(context, ::isDocFile)
                    else -> emptyList()
                }
                directoryFiles = list
                isLoadingFiles = false
            }
        }
    }

    fun handleFileActivate(file: File) {
        if (file.isDirectory) {
            currentDirectory = file
            activeCategory = null
            return
        }
        if (pickerMode == FileExplorerPickerMode.IMAGES_FOR_WALLPAPER && onSelectWallpaperImage != null && isImageFile(file)) {
            onSelectWallpaperImage("file://${file.absolutePath}")
            return
        }
        if (pickerMode == FileExplorerPickerMode.VIDEOS_FOR_LIVE_WALLPAPER && onSelectLiveWallpaperVideo != null && isVideoFile(file)) {
            onSelectLiveWallpaperVideo("file://${file.absolutePath}")
            return
        }
        if (pickerMode != FileExplorerPickerMode.NONE && pickerMode != FileExplorerPickerMode.STANDALONE_FULL && onFilePicked != null) {
            onFilePicked(file)
            return
        }
        when {
            isRnaFile(file) -> launchRnaPackageInstaller(context, file)
            isApkFile(file) -> {
                if (canInstallUnknownPackages(context)) {
                    launchPackageInstallerForApk(context, file)
                } else {
                    pendingApkInstallFile = file
                    showApkPermissionDialog = true
                }
            }
            isVideoFile(file) -> activeVideoPlayerFile = file
            isAudioFile(file) -> activeAudioPlayerFile = file
            isImageFile(file) -> activeImageViewerFile = file
            else -> {
                val opened = tryOpenFileWithSupportedApp(context, file)
                if (!opened) {
                    propertiesTargetFile = file
                }
            }
        }
    }

    LaunchedEffect(initialPath) {
        if (!initialPath.isNullOrBlank()) {
            val f = File(initialPath)
            if (f.exists() && f.isFile) {
                currentDirectory = f.parentFile
                handleFileActivate(f)
            }
        }
    }

    // Guarantee immediate D-Pad focus when File Explorer opens (critical for Live/Static Wallpaper File Picker)
    LaunchedEffect(Unit) {
        delay(60L)
        runCatching { backBtnFocusRequester.requestFocus() }
    }

    // Automatically focus the first selectable file / tile once loading completes
    LaunchedEffect(isLoadingFiles, currentDirectory, activeCategory, appsCategorySubTab, directoryFiles.size, installedAppsList.size) {
        if (!isLoadingFiles) {
            delay(80L)
            val hasContent = when {
                currentDirectory == null && activeCategory == null -> true
                currentDirectory == null && activeCategory == "SYSTEM_APPS" && appsCategorySubTab == 0 -> installedAppsList.isNotEmpty()
                else -> directoryFiles.isNotEmpty()
            }
            if (hasContent) {
                if (!runCatching { firstItemFocusRequester.requestFocus() }.isSuccess) {
                    runCatching { backBtnFocusRequester.requestFocus() }
                }
            } else {
                runCatching { backBtnFocusRequester.requestFocus() }
            }
        }
    }

    BackHandler {
        when {
            activeVideoPlayerFile != null -> activeVideoPlayerFile = null
            activeAudioPlayerFile != null -> activeAudioPlayerFile = null
            activeImageViewerFile != null -> activeImageViewerFile = null
            showApkPermissionDialog -> showApkPermissionDialog = false
            selectedInstalledAppForAction != null -> selectedInstalledAppForAction = null
            contextMenuTargetFile != null -> contextMenuTargetFile = null
            renameTargetFile != null -> {
                renameTargetFile = null
                showRenameKeyboard = false
            }
            propertiesTargetFile != null -> propertiesTargetFile = null
            currentDirectory != null -> {
                val parent = currentDirectory?.parentFile
                val mainRoot = Environment.getExternalStorageDirectory()
                if (parent != null && parent.canRead() && currentDirectory != mainRoot) {
                    currentDirectory = parent
                } else if (isMediaPickerMode && defaultPickerCategory != null) {
                    currentDirectory = null
                    activeCategory = defaultPickerCategory
                } else {
                    currentDirectory = null
                }
            }
            activeCategory != null && !isMediaPickerMode -> {
                activeCategory = null
            }
            else -> onClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF060913), Color(0xFF0C1224), Color(0xFF070A14))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 20.dp)
        ) {
            // M3 Expressive Top Header Surface
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF12182B),
                border = BorderStroke(1.dp, Color(0x26FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ExplorerHeaderButton(
                            label = if (currentDirectory == null && (activeCategory == null || isMediaPickerMode)) "Exit" else "Back",
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            testTag = "explorer_back_btn",
                            modifier = Modifier.focusRequester(backBtnFocusRequester),
                            onClick = {
                                if (currentDirectory != null) {
                                    val parent = currentDirectory?.parentFile
                                    val mainRoot = Environment.getExternalStorageDirectory()
                                    if (parent != null && parent.canRead() && currentDirectory != mainRoot) {
                                        currentDirectory = parent
                                    } else if (isMediaPickerMode && defaultPickerCategory != null) {
                                        currentDirectory = null
                                        activeCategory = defaultPickerCategory
                                    } else {
                                        currentDirectory = null
                                    }
                                } else if (activeCategory != null && !isMediaPickerMode) {
                                    activeCategory = null
                                } else {
                                    onClose()
                                }
                            }
                        )

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF38BDF8).copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = when {
                                    pickerMode == FileExplorerPickerMode.IMAGES_FOR_WALLPAPER -> "File Explorer • Static Wallpaper"
                                    pickerMode == FileExplorerPickerMode.VIDEOS_FOR_LIVE_WALLPAPER -> "File Explorer • Live Wallpaper"
                                    currentDirectory != null -> currentDirectory?.name?.ifBlank { "Storage" } ?: "Storage"
                                    activeCategory == "SYSTEM_APPS" -> "File Explorer • Apps & APKs"
                                    activeCategory != null -> "File Explorer • ${activeCategory?.lowercase()?.replaceFirstChar { it.uppercase() }}"
                                    else -> "File Explorer"
                                },
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (currentDirectory != null) {
                                Text(
                                    text = currentDirectory?.absolutePath ?: "",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else if (pickerMode == FileExplorerPickerMode.VIDEOS_FOR_LIVE_WALLPAPER) {
                                Text(
                                    text = "Press OK on any video to apply it as your Live Wallpaper",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else if (pickerMode == FileExplorerPickerMode.IMAGES_FOR_WALLPAPER) {
                                Text(
                                    text = "Press OK on any image to apply it as your Wallpaper",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Right Controls: Grant All Files Access, Clipboard Paste + Cancel 'X', Browse Folders, Layout Mode
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!hasStoragePerm) {
                            ExplorerHeaderButton(
                                label = "Grant Access",
                                icon = Icons.Default.Security,
                                accentColor = Color(0xFFF59E0B),
                                testTag = "explorer_grant_storage_btn",
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        launchAllFilesAccessSettings(context)
                                    } else {
                                        legacyStoragePermLauncher.launch(
                                            arrayOf(
                                                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                            )
                                        )
                                    }
                                }
                            )
                        }

                        if (clipboardFile != null) {
                            if (currentDirectory != null) {
                                ExplorerHeaderButton(
                                    label = if (clipboardIsMove) "Move Here (${clipboardFile?.name?.take(12)})" else "Paste Here (${clipboardFile?.name?.take(12)})",
                                    icon = Icons.Default.ContentPaste,
                                    accentColor = Color(0xFF10B981),
                                    testTag = "explorer_paste_btn",
                                    onClick = {
                                        val src = clipboardFile ?: return@ExplorerHeaderButton
                                        val dstDir = currentDirectory ?: return@ExplorerHeaderButton
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val dst = File(dstDir, src.name)
                                                src.copyTo(dst, overwrite = true)
                                                if (clipboardIsMove) {
                                                    src.delete()
                                                }
                                                withContext(Dispatchers.Main) {
                                                    clipboardFile = null
                                                    refreshTrigger++
                                                    Toast.makeText(context, "Saved to ${dst.name}", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Paste failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF1E293B),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "${if (clipboardIsMove) "Moving" else "Copied"}: ${clipboardFile?.name?.take(14)}",
                                        color = Color(0xFF10B981),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                                    )
                                }
                            }

                            // Cancel Copy / Move 'X' Button
                            ExplorerHeaderButton(
                                label = "X",
                                icon = Icons.Default.Close,
                                accentColor = Color(0xFFEF4444),
                                testTag = "explorer_cancel_clipboard_btn",
                                onClick = {
                                    clipboardFile = null
                                    Toast.makeText(context, "Canceled copy/move", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        if (isMediaPickerMode) {
                            val inFolders = currentDirectory != null
                            ExplorerHeaderButton(
                                label = if (inFolders) {
                                    if (pickerMode == FileExplorerPickerMode.VIDEOS_FOR_LIVE_WALLPAPER) "All Videos" else "All Photos"
                                } else {
                                    "Folders"
                                },
                                icon = if (inFolders) Icons.Default.VideoLibrary else Icons.Default.FolderOpen,
                                accentColor = Color(0xFF10B981),
                                testTag = "explorer_browse_folders_btn",
                                onClick = {
                                    if (inFolders) {
                                        currentDirectory = null
                                        activeCategory = defaultPickerCategory
                                    } else {
                                        activeCategory = null
                                        currentDirectory = Environment.getExternalStorageDirectory() ?: context.filesDir
                                    }
                                }
                            )
                        }

                        ExplorerHeaderButton(
                            label = if (layoutMode == FileExplorerLayoutMode.GRID_8) "Grid View" else "List View",
                            icon = if (layoutMode == FileExplorerLayoutMode.GRID_8) Icons.Default.GridView else Icons.AutoMirrored.Filled.ViewList,
                            testTag = "explorer_layout_dropdown_btn",
                            onClick = {
                                layoutMode = if (layoutMode == FileExplorerLayoutMode.LIST) {
                                    FileExplorerLayoutMode.GRID_8
                                } else {
                                    FileExplorerLayoutMode.LIST
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // When inside Apps category, render the two top M3 Expressive Pill Buttons: "Apps" and "APK"
            if (currentDirectory == null && activeCategory == "SYSTEM_APPS") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExplorerPillTabButton(
                        title = "Apps (${installedAppsList.size})",
                        icon = Icons.Default.Apps,
                        selected = appsCategorySubTab == 0,
                        testTag = "explorer_apps_pill_tab",
                        onClick = { appsCategorySubTab = 0 }
                    )
                    ExplorerPillTabButton(
                        title = "APK",
                        icon = Icons.Default.Android,
                        selected = appsCategorySubTab == 1,
                        testTag = "explorer_apk_pill_tab",
                        onClick = { appsCategorySubTab = 1 }
                    )
                }
            }

            // Main Content Area
            val dashboardTiles = remember(refreshTrigger) { buildFileExplorerDashboardTiles(context) }

            if (currentDirectory == null && activeCategory == null) {
                if (layoutMode == FileExplorerLayoutMode.GRID_8) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(8),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(dashboardTiles, key = { _, it -> it.id }) { idx, tile ->
                            DashboardTileCard(
                                tile = tile,
                                isGrid = true,
                                modifier = if (idx == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier,
                                onClick = {
                                    if (tile.path != null && tile.categoryKey == null) {
                                        currentDirectory = tile.path
                                    } else {
                                        activeCategory = tile.categoryKey
                                    }
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(dashboardTiles, key = { _, it -> it.id }) { idx, tile ->
                            DashboardTileCard(
                                tile = tile,
                                isGrid = false,
                                modifier = if (idx == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier,
                                onClick = {
                                    if (tile.path != null && tile.categoryKey == null) {
                                        currentDirectory = tile.path
                                    } else {
                                        activeCategory = tile.categoryKey
                                    }
                                }
                            )
                        }
                    }
                }
            } else if (currentDirectory == null && activeCategory == "SYSTEM_APPS" && appsCategorySubTab == 0) {
                // Installed Apps ("Apps" Pill)
                if (isLoadingFiles) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF38BDF8))
                    }
                } else if (layoutMode == FileExplorerLayoutMode.GRID_8) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(8),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(installedAppsList, key = { _, it -> it.packageName }) { idx, appItem ->
                            InstalledAppExplorerCard(
                                app = appItem,
                                isGrid = true,
                                modifier = if (idx == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier,
                                onClick = { selectedInstalledAppForAction = appItem }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(installedAppsList, key = { _, it -> it.packageName }) { idx, appItem ->
                            InstalledAppExplorerCard(
                                app = appItem,
                                isGrid = false,
                                modifier = if (idx == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier,
                                onClick = { selectedInstalledAppForAction = appItem }
                            )
                        }
                    }
                }
            } else {
                // Directory or Category File View (including Live Wallpaper Video Picker & "APK" Pill)
                if (isLoadingFiles) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(color = Color(0xFF38BDF8))
                            Text(
                                text = if (pickerMode == FileExplorerPickerMode.VIDEOS_FOR_LIVE_WALLPAPER) {
                                    "Scanning videos on device..."
                                } else {
                                    "Loading files..."
                                },
                                color = Color(0xFF94A3B8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else if (directoryFiles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOff,
                                contentDescription = "Empty",
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = if (pickerMode == FileExplorerPickerMode.VIDEOS_FOR_LIVE_WALLPAPER && currentDirectory == null) {
                                    "No video files found automatically"
                                } else {
                                    "No files found"
                                },
                                color = Color.White.copy(alpha = 0.78f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (isMediaPickerMode && currentDirectory == null) {
                                ExplorerHeaderButton(
                                    label = "Browse Storage Folders",
                                    icon = Icons.Default.FolderOpen,
                                    accentColor = Color(0xFF10B981),
                                    testTag = "explorer_empty_browse_folders_btn",
                                    onClick = {
                                        activeCategory = null
                                        currentDirectory = Environment.getExternalStorageDirectory() ?: context.filesDir
                                    }
                                )
                            }
                        }
                    }
                } else if (layoutMode == FileExplorerLayoutMode.GRID_8) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(8),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(directoryFiles, key = { _, it -> it.absolutePath }) { idx, file ->
                            ExplorerFileCard(
                                file = file,
                                isGrid = true,
                                enableLongPressMenu = !isMediaPickerMode,
                                modifier = if (idx == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier,
                                onClick = { handleFileActivate(file) },
                                onLongPress = { contextMenuTargetFile = file }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(directoryFiles, key = { _, it -> it.absolutePath }) { idx, file ->
                            ExplorerFileCard(
                                file = file,
                                isGrid = false,
                                enableLongPressMenu = !isMediaPickerMode,
                                modifier = if (idx == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier,
                                onClick = { handleFileActivate(file) },
                                onLongPress = { contextMenuTargetFile = file }
                            )
                        }
                    }
                }
            }
        }

        // Installed App Action Dialog ("App Info" & "Extract App")
        if (selectedInstalledAppForAction != null) {
            val targetApp = selectedInstalledAppForAction!!
            val firstBtnFocusRequester = remember { FocusRequester() }
            LaunchedEffect(targetApp.packageName) {
                delay(50L)
                runCatching { firstBtnFocusRequester.requestFocus() }
            }
            Dialog(onDismissRequest = { selectedInstalledAppForAction = null }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF111827),
                    border = BorderStroke(1.5.dp, Color(0x38FFFFFF)),
                    modifier = Modifier.width(440.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (targetApp.iconBitmap != null) {
                                Image(
                                    bitmap = targetApp.iconBitmap,
                                    contentDescription = targetApp.label,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Android,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = targetApp.label,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "v${targetApp.versionName} • ${formatBytesSize(targetApp.sizeBytes)}",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(modifier = Modifier.focusRequester(firstBtnFocusRequester)) {
                            ExplorerMenuOptionButton(
                                label = "App Info",
                                icon = Icons.Default.Info
                            ) {
                                selectedInstalledAppForAction = null
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${targetApp.packageName}")
                                    ).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                        }

                        ExplorerMenuOptionButton(
                            label = "Extract App (.APK to Downloads)",
                            icon = Icons.Default.Download
                        ) {
                            selectedInstalledAppForAction = null
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val srcFile = File(targetApp.sourceApkPath)
                                    val dlDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                        ?: File(context.filesDir, "browser_downloads").apply { mkdirs() }
                                    if (!dlDir.exists()) dlDir.mkdirs()
                                    val safeName = targetApp.label.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                                    val outApk = File(dlDir, "${safeName}_v${targetApp.versionName}.apk")
                                    srcFile.copyTo(outApk, overwrite = true)
                                    withContext(Dispatchers.Main) {
                                        refreshTrigger++
                                        Toast.makeText(
                                            context,
                                            "Extracted to Downloads/${outApk.name}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "Extract failed: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        }

                        ExplorerMenuOptionButton(
                            label = "Cancel",
                            icon = Icons.Default.Close
                        ) {
                            selectedInstalledAppForAction = null
                        }
                    }
                }
            }
        }

        // APK Install Permission Instruction Dialog
        if (showApkPermissionDialog && pendingApkInstallFile != null) {
            val targetApk = pendingApkInstallFile!!
            val grantBtnFocus = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                delay(50L)
                runCatching { grantBtnFocus.requestFocus() }
            }
            Dialog(onDismissRequest = { showApkPermissionDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF111827),
                    border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                    modifier = Modifier.width(500.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = "Allow Installing Apps",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = "To install ${targetApk.name} with Android Package Installer:\n" +
                                "1. Click 'Open Permission Settings' below.\n" +
                                "2. Turn ON 'Allow from this source' for RNA. TV.\n" +
                                "3. Press Back on your remote to start installation.",
                            color = Color(0xFFE2E8F0),
                            fontSize = 14.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            ExplorerHeaderButton(
                                label = "Cancel",
                                icon = Icons.Default.Close,
                                testTag = "apk_perm_cancel_btn",
                                onClick = {
                                    showApkPermissionDialog = false
                                    pendingApkInstallFile = null
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Box(modifier = Modifier.focusRequester(grantBtnFocus)) {
                                ExplorerHeaderButton(
                                    label = "Open Permission Settings",
                                    icon = Icons.Default.Settings,
                                    accentColor = Color(0xFF10B981),
                                    testTag = "apk_perm_grant_btn",
                                    onClick = {
                                        openUnknownSourcesPermissionSettings(context)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Long-Press File Context Menu Dialog (Focus-Trapping Dialog so background files never move)
        if (contextMenuTargetFile != null) {
            val target = contextMenuTargetFile!!
            val firstOptionFocus = remember { FocusRequester() }
            val menuScroll = rememberScrollState()
            var ignoredInitialKeyUp by remember { mutableStateOf(false) }

            LaunchedEffect(target.absolutePath) {
                delay(60L)
                runCatching { firstOptionFocus.requestFocus() }
            }

            Dialog(onDismissRequest = { contextMenuTargetFile = null }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF111827),
                    border = BorderStroke(1.5.dp, Color(0x38FFFFFF)),
                    modifier = Modifier
                        .width(460.dp)
                        .heightIn(max = 540.dp)
                        .onPreviewKeyEvent { ev ->
                            val code = ev.nativeKeyEvent.keyCode
                            val isOk = code == KeyEvent.KEYCODE_DPAD_CENTER ||
                                code == KeyEvent.KEYCODE_ENTER ||
                                code == KeyEvent.KEYCODE_NUMPAD_ENTER
                            if (isOk && !ignoredInitialKeyUp) {
                                if (ev.nativeKeyEvent.action == KeyEvent.ACTION_UP) {
                                    ignoredInitialKeyUp = true
                                }
                                true
                            } else {
                                false
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = target.name,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (target.isDirectory) "Folder" else formatBytesSize(target.length()),
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(menuScroll),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.focusRequester(firstOptionFocus)) {
                                ExplorerMenuOptionButton(
                                    label = if (isApkFile(target)) "Install APK" else "Open",
                                    icon = Icons.AutoMirrored.Filled.OpenInNew
                                ) {
                                    contextMenuTargetFile = null
                                    handleFileActivate(target)
                                }
                            }

                            if (!target.isDirectory) {
                                ExplorerMenuOptionButton(
                                    label = "Open with",
                                    icon = Icons.Default.Share
                                ) {
                                    contextMenuTargetFile = null
                                    launchOpenWithChooser(context, target)
                                }
                            }

                            if (isImageFile(target) && onSelectWallpaperImage != null) {
                                ExplorerMenuOptionButton(
                                    label = "Set as Static Wallpaper",
                                    icon = Icons.Default.Wallpaper
                                ) {
                                    contextMenuTargetFile = null
                                    onSelectWallpaperImage("file://${target.absolutePath}")
                                }
                            }

                            if (isVideoFile(target) && onSelectLiveWallpaperVideo != null) {
                                ExplorerMenuOptionButton(
                                    label = "Set as Live Video Wallpaper",
                                    icon = Icons.Default.VideoLibrary
                                ) {
                                    contextMenuTargetFile = null
                                    onSelectLiveWallpaperVideo("file://${target.absolutePath}")
                                }
                            }

                            ExplorerMenuOptionButton(
                                label = "Rename",
                                icon = Icons.Default.Edit
                            ) {
                                contextMenuTargetFile = null
                                renameInputText = target.name
                                renameTargetFile = target
                                showRenameKeyboard = true
                            }

                            ExplorerMenuOptionButton(
                                label = "Copy",
                                icon = Icons.Default.ContentCopy
                            ) {
                                clipboardFile = target
                                clipboardIsMove = false
                                contextMenuTargetFile = null
                                Toast.makeText(context, "Copied ${target.name}", Toast.LENGTH_SHORT).show()
                            }

                            ExplorerMenuOptionButton(
                                label = "Move",
                                icon = Icons.Default.DriveFileMove
                            ) {
                                clipboardFile = target
                                clipboardIsMove = true
                                contextMenuTargetFile = null
                                Toast.makeText(context, "Ready to move ${target.name}", Toast.LENGTH_SHORT).show()
                            }

                            ExplorerMenuOptionButton(
                                label = "Compress to .ZIP",
                                icon = Icons.Default.FolderZip
                            ) {
                                contextMenuTargetFile = null
                                scope.launch(Dispatchers.IO) {
                                    val zipFile = File(target.parentFile ?: context.filesDir, "${target.nameWithoutExtension}.zip")
                                    val ok = compressFileOrDirectoryToZip(target, zipFile)
                                    withContext(Dispatchers.Main) {
                                        refreshTrigger++
                                        Toast.makeText(
                                            context,
                                            if (ok) "Created ${zipFile.name}" else "Failed to compress",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }

                            ExplorerMenuOptionButton(
                                label = "Properties",
                                icon = Icons.Default.Info
                            ) {
                                contextMenuTargetFile = null
                                propertiesTargetFile = target
                            }

                            ExplorerMenuOptionButton(
                                label = "Delete",
                                icon = Icons.Default.Delete,
                                isDestructive = true
                            ) {
                                contextMenuTargetFile = null
                                scope.launch(Dispatchers.IO) {
                                    val deleted = target.deleteRecursively()
                                    withContext(Dispatchers.Main) {
                                        refreshTrigger++
                                        Toast.makeText(
                                            context,
                                            if (deleted) "Deleted ${target.name}" else "Could not delete ${target.name}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }

                            ExplorerMenuOptionButton(
                                label = "Cancel",
                                icon = Icons.Default.Close
                            ) {
                                contextMenuTargetFile = null
                            }
                        }
                    }
                }
            }
        }

        // Rename Modal + Default System Keyboard (or RNA TV Keyboard if set as system default IME)
        if (renameTargetFile != null) {
            val target = renameTargetFile!!
            val view = LocalView.current
            val keyboardController = LocalSoftwareKeyboardController.current
            val renameFieldFocusRequester = remember { FocusRequester() }

            LaunchedEffect(target.absolutePath) {
                delay(80L)
                if (isRnaKeyboardDefaultIme(context)) {
                    showRenameKeyboard = true
                } else {
                    openDeviceDefaultKeyboard(
                        context = context,
                        view = view,
                        focusRequester = renameFieldFocusRequester,
                        keyboardController = keyboardController
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f)),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 48.dp)
                        .width(480.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Rename File",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = renameInputText,
                        onValueChange = { renameInputText = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF38BDF8).copy(alpha = 0.6f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(renameFieldFocusRequester)
                            .tvKeyboardFieldTrigger(
                                context = context,
                                view = view,
                                focusRequester = renameFieldFocusRequester,
                                keyboardController = keyboardController,
                                onOpenRnaKeyboard = { showRenameKeyboard = true }
                            )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        ExplorerHeaderButton(
                            label = "Cancel",
                            icon = Icons.Default.Close,
                            testTag = "rename_cancel_btn",
                            onClick = {
                                renameTargetFile = null
                                showRenameKeyboard = false
                            }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        ExplorerHeaderButton(
                            label = "Save",
                            icon = Icons.Default.Check,
                            accentColor = Color(0xFF10B981),
                            testTag = "rename_save_btn",
                            onClick = {
                                val newName = renameInputText.trim()
                                if (newName.isNotEmpty()) {
                                    val dst = File(target.parentFile ?: context.filesDir, newName)
                                    target.renameTo(dst)
                                    refreshTrigger++
                                }
                                renameTargetFile = null
                                showRenameKeyboard = false
                            }
                        )
                    }
                }

                RnaTvKeyboardOverlay(
                    visible = showRenameKeyboard && isRnaKeyboardDefaultIme(context),
                    currentText = renameInputText,
                    onTextChange = { renameInputText = it },
                    onCommitText = { renameInputText += it },
                    onBackspace = {
                        if (renameInputText.isNotEmpty()) {
                            renameInputText = renameInputText.dropLast(1)
                        }
                    },
                    onSubmit = {
                        val newName = renameInputText.trim()
                        if (newName.isNotEmpty()) {
                            val dst = File(target.parentFile ?: context.filesDir, newName)
                            target.renameTo(dst)
                            refreshTrigger++
                        }
                        renameTargetFile = null
                        showRenameKeyboard = false
                    },
                    onDismiss = {
                        renameTargetFile = null
                        showRenameKeyboard = false
                    }
                )
            }
        }

        // File Properties Dialog
        if (propertiesTargetFile != null) {
            val f = propertiesTargetFile!!
            val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US) }
            val closeBtnFocus = remember { FocusRequester() }
            LaunchedEffect(f.absolutePath) {
                delay(50L)
                runCatching { closeBtnFocus.requestFocus() }
            }
            Dialog(onDismissRequest = { propertiesTargetFile = null }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF111827),
                    border = BorderStroke(1.5.dp, Color(0x38FFFFFF)),
                    modifier = Modifier.width(500.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Properties", color = Color(0xFF38BDF8), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                        Text("Name: ${f.name}", color = Color.White, fontSize = 14.sp)
                        Text("Path: ${f.absolutePath}", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                        Text("Type: ${if (f.isDirectory) "Folder" else f.extension.uppercase().ifEmpty { "File" }}", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                        Text("Size: ${if (f.isDirectory) "${f.listFiles()?.size ?: 0} items" else formatBytesSize(f.length())}", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                        Text("Modified: ${fmt.format(Date(f.lastModified()))}", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                        Text("Permissions: ${if (f.canRead()) "Read " else ""}${if (f.canWrite()) "Write" else ""}", color = Color.White.copy(alpha = 0.75f), fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            if (!f.isDirectory) {
                                ExplorerHeaderButton(
                                    label = "Open with",
                                    icon = Icons.Default.Share,
                                    testTag = "properties_open_with_btn",
                                    onClick = {
                                        val target = f
                                        propertiesTargetFile = null
                                        launchOpenWithChooser(context, target)
                                    }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Box(modifier = Modifier.focusRequester(closeBtnFocus)) {
                                ExplorerHeaderButton(
                                    label = "Close",
                                    icon = Icons.Default.Check,
                                    testTag = "properties_close_btn",
                                    onClick = { propertiesTargetFile = null }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Built-in Full-Screen Image Viewer
        if (activeImageViewerFile != null) {
            BuiltInImageViewerOverlay(
                file = activeImageViewerFile!!,
                onClose = { activeImageViewerFile = null }
            )
        }

        // Built-in Full-Screen Audio Player
        if (activeAudioPlayerFile != null) {
            BuiltInAudioPlayerOverlay(
                file = activeAudioPlayerFile!!,
                onClose = { activeAudioPlayerFile = null }
            )
        }

        // Built-in Full-Screen Video / .MKV / .MOV Player
        if (activeVideoPlayerFile != null) {
            BuiltInVideoPlayerOverlay(
                file = activeVideoPlayerFile!!,
                onClose = { activeVideoPlayerFile = null }
            )
        }
    }
}

private fun compressFileOrDirectoryToZip(source: File, destinationZip: File): Boolean {
    return try {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(destinationZip))).use { zos ->
            fun zipRecursive(file: File, baseName: String) {
                if (file.isDirectory) {
                    val children = file.listFiles() ?: return
                    for (child in children) {
                        zipRecursive(child, "$baseName/${child.name}")
                    }
                } else {
                    FileInputStream(file).use { fis ->
                        BufferedInputStream(fis).use { origin ->
                            val entry = ZipEntry(baseName)
                            zos.putNextEntry(entry)
                            origin.copyTo(zos, 8192)
                            zos.closeEntry()
                        }
                    }
                }
            }
            zipRecursive(source, source.name)
        }
        true
    } catch (_: Exception) {
        false
    }
}

@Composable
private fun ExplorerPillTabButton(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "PillTabScale"
    )
    Surface(
        shape = RoundedCornerShape(50),
        color = when {
            isFocused -> Color.White
            selected -> Color(0xFF38BDF8)
            else -> Color(0xFF182036)
        },
        border = BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused || selected) Color(0xFF38BDF8) else Color(0x28FFFFFF)
        ),
        modifier = Modifier
            .scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isFocused || selected) Color(0xFF090D16) else Color.White,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                color = if (isFocused || selected) Color(0xFF090D16) else Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun InstalledAppExplorerCard(
    app: ExplorerInstalledAppItem,
    isGrid: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "InstalledAppScale"
    )

    if (isGrid) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .height(142.dp)
                .scale(scale)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isFocused) Color(0xFF1E2B45) else Color(0xFF111829))
                .border(
                    width = if (isFocused) 2.5.dp else 1.dp,
                    color = if (isFocused) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap,
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = app.label,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = app.label,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = formatBytesSize(app.sizeBytes),
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .scale(scale)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isFocused) Color(0xFF1E2B45) else Color(0xFF111829))
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (app.iconBitmap != null) {
                    Image(
                        bitmap = app.iconBitmap,
                        contentDescription = app.label,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = app.label,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(36.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${app.packageName} • v${app.versionName} • ${formatBytesSize(app.sizeBytes)}",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun DashboardTileCard(
    tile: StorageVolumeTile,
    isGrid: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "DashboardTileScale"
    )

    if (isGrid) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .height(154.dp)
                .scale(scale)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (isFocused) {
                        Brush.verticalGradient(
                            colors = listOf(
                                tile.accentColor.copy(alpha = 0.28f),
                                Color(0xFF1A2540)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF141C30),
                                Color(0xFF0E1424)
                            )
                        )
                    }
                )
                .border(
                    width = if (isFocused) 2.5.dp else 1.dp,
                    color = if (isFocused) Color.White else tile.accentColor.copy(alpha = 0.28f),
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(12.dp)
                .testTag("explorer_tile_${tile.id}"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isFocused) tile.accentColor else tile.accentColor.copy(alpha = 0.20f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (isFocused) Color.White else tile.accentColor.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tile.icon,
                    contentDescription = tile.title,
                    tint = if (isFocused) Color(0xFF080C14) else tile.accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = tile.title,
                color = Color.White,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (tile.subtitle.isNotBlank()) {
                Text(
                    text = tile.subtitle,
                    color = if (isFocused) Color(0xFFE2E8F0) else Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
            if (tile.isStorageBar && tile.totalBytes > 0L) {
                Spacer(modifier = Modifier.height(7.dp))
                val ratio = (tile.usedBytes.toFloat() / tile.totalBytes.toFloat()).coerceIn(0.06f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(tile.accentColor)
                    )
                }
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .scale(scale)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    if (isFocused) {
                        Brush.horizontalGradient(
                            colors = listOf(
                                tile.accentColor.copy(alpha = 0.26f),
                                Color(0xFF1A2540)
                            )
                        )
                    } else {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF141C30),
                                Color(0xFF0E1424)
                            )
                        )
                    }
                )
                .border(
                    width = if (isFocused) 2.5.dp else 1.dp,
                    color = if (isFocused) Color.White else tile.accentColor.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(22.dp)
                )
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .testTag("explorer_list_tile_${tile.id}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isFocused) tile.accentColor else tile.accentColor.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tile.icon,
                        contentDescription = tile.title,
                        tint = if (isFocused) Color(0xFF080C14) else tile.accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(tile.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    if (tile.subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(tile.subtitle, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    }
                    if (tile.isStorageBar && tile.totalBytes > 0L) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val ratio = (tile.usedBytes.toFloat() / tile.totalBytes.toFloat()).coerceIn(0.06f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.45f)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(ratio)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(tile.accentColor)
                            )
                        }
                    }
                }
            }
            Surface(
                shape = CircleShape,
                color = if (isFocused) tile.accentColor else Color(0x1FFFFFFF),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = if (isFocused) Color(0xFF080C14) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExplorerFileCard(
    file: File,
    isGrid: Boolean,
    enableLongPressMenu: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var keyDownTime by remember { mutableLongStateOf(0L) }
    var longPressFired by remember { mutableStateOf(false) }

    // Real Image & Video Frame Preview Thumbnail (shown in both Grid View and List View)
    val mediaPreview by produceState<ImageBitmap?>(
        initialValue = ExplorerThumbnailCache.bitmaps[file.absolutePath],
        key1 = file.absolutePath
    ) {
        if (value != null) return@produceState
        if (isImageFile(file)) {
            value = withContext(Dispatchers.IO) {
                try {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeFile(file.absolutePath, opts)?.asImageBitmap()?.also {
                        ExplorerThumbnailCache.bitmaps[file.absolutePath] = it
                    }
                } catch (_: Exception) {
                    null
                }
            }
        } else if (isVideoFile(file)) {
            value = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(file.absolutePath)
                    val rawBmp = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.getFrameAtTime(0L)
                    rawBmp?.asImageBitmap()?.also {
                        ExplorerThumbnailCache.bitmaps[file.absolutePath] = it
                    }
                } catch (_: Exception) {
                    null
                } finally {
                    try {
                        retriever.release()
                    } catch (_: Exception) {}
                }
            }
        }
    }

    val (icon, tint) = when {
        file.isDirectory -> Icons.Default.Folder to Color(0xFF38BDF8)
        isImageFile(file) -> Icons.Default.Image to Color(0xFFEC4899)
        isVideoFile(file) -> Icons.Default.Movie to Color(0xFFA855F7)
        isAudioFile(file) -> Icons.Default.AudioFile to Color(0xFFF59E0B)
        isApkFile(file) -> Icons.Default.Android to Color(0xFF10B981)
        isRnaFile(file) -> Icons.Default.Apps to Color(0xFF38BDF8)
        file.extension.equals("zip", ignoreCase = true) -> Icons.Default.FolderZip to Color(0xFF10B981)
        else -> Icons.AutoMirrored.Filled.InsertDriveFile to Color(0xFF94A3B8)
    }

    val keyModifier = if (enableLongPressMenu) {
        Modifier.onPreviewKeyEvent { event ->
            val code = event.nativeKeyEvent.keyCode
            val isCenter = code == KeyEvent.KEYCODE_DPAD_CENTER ||
                code == KeyEvent.KEYCODE_ENTER ||
                code == KeyEvent.KEYCODE_NUMPAD_ENTER ||
                code == KeyEvent.KEYCODE_BUTTON_A
            if (!isCenter) return@onPreviewKeyEvent false

            if (event.type == KeyEventType.KeyDown) {
                if (event.nativeKeyEvent.repeatCount == 0) {
                    keyDownTime = System.currentTimeMillis()
                    longPressFired = false
                } else if (!longPressFired && System.currentTimeMillis() - keyDownTime >= 420L) {
                    longPressFired = true
                    onLongPress()
                }
                true
            } else if (event.type == KeyEventType.KeyUp) {
                if (!longPressFired) {
                    onClick()
                }
                longPressFired = false
                true
            } else {
                false
            }
        }
    } else {
        Modifier
    }

    if (isGrid) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isFocused) Color(0xFF1E2B45) else Color(0xFF111829))
                .border(
                    width = if (isFocused) 2.5.dp else 1.dp,
                    color = if (isFocused) Color.White else Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(16.dp)
                )
                .then(keyModifier)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF090D18)),
                contentAlignment = Alignment.Center
            ) {
                if (mediaPreview != null) {
                    Image(
                        bitmap = mediaPreview!!,
                        contentDescription = file.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isVideoFile(file)) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = file.name,
                        tint = tint,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = file.name,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (file.isDirectory) "Folder" else formatBytesSize(file.length()),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (isFocused) Color(0xFF1E2B45) else Color(0xFF111829))
                .border(
                    width = if (isFocused) 2.5.dp else 1.dp,
                    color = if (isFocused) Color.White else Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(14.dp)
                )
                .then(keyModifier)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (mediaPreview != null) {
                    Box(
                        modifier = Modifier
                            .size(width = 64.dp, height = 42.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(0xFF090D18)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = mediaPreview!!,
                            contentDescription = file.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isVideoFile(file)) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(tint.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = file.name,
                            tint = tint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (file.isDirectory) "Folder" else formatBytesSize(file.length()),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.45f)
            )
        }
    }
}

@Composable
private fun ExplorerHeaderButton(
    label: String,
    icon: ImageVector,
    accentColor: Color = Color(0xFF38BDF8),
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isFocused) accentColor else Color(0xFF1E293B))
            .border(if (isFocused) 2.dp else 1.5.dp, if (isFocused) Color.White else Color.White.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isFocused) Color(0xFF090D16) else Color.White,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = if (isFocused) Color(0xFF090D16) else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ExplorerMenuOptionButton(
    label: String,
    icon: ImageVector,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val tint = if (isDestructive) Color(0xFFEF4444) else Color.White
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isFocused) Color(0xFF38BDF8) else Color(0xFF1E293B))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isFocused) Color(0xFF090D16) else tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = if (isFocused) Color(0xFF090D16) else tint,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun BuiltInImageViewerOverlay(
    file: File,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var zoom by remember { mutableFloatStateOf(1f) }
    var rotation by remember { mutableFloatStateOf(0f) }
    val bitmap by produceState<ImageBitmap?>(initialValue = null, file.absolutePath) {
        value = withContext(Dispatchers.IO) {
            try {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070B))
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = file.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = zoom,
                        scaleY = zoom,
                        rotationZ = rotation
                    )
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Unable to decode image ${file.name}", color = Color.White)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(file.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ExplorerHeaderButton(
                    label = "Zoom (${String.format(Locale.US, "%.1fx", zoom)})",
                    icon = Icons.Default.ZoomIn,
                    testTag = "img_viewer_zoom_btn",
                    onClick = { zoom = if (zoom >= 2.5f) 1f else zoom + 0.5f }
                )
                ExplorerHeaderButton(
                    label = "Rotate 90°",
                    icon = Icons.Default.RotateRight,
                    testTag = "img_viewer_rotate_btn",
                    onClick = { rotation = (rotation + 90f) % 360f }
                )
                ExplorerHeaderButton(
                    label = "Open with",
                    icon = Icons.Default.Share,
                    testTag = "img_viewer_open_with_btn",
                    onClick = { launchOpenWithChooser(context, file) }
                )
                ExplorerHeaderButton(
                    label = "Close",
                    icon = Icons.Default.Close,
                    testTag = "img_viewer_close_btn",
                    onClick = onClose
                )
            }
        }
    }
}

@Composable
internal fun BuiltInAudioPlayerOverlay(
    file: File,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(1) }
    var isLooping by remember { mutableStateOf(false) }
    val playBtnFocus = remember { FocusRequester() }
    val seekBarFocus = remember { FocusRequester() }
    var isSeekBarFocused by remember { mutableStateOf(false) }

    var isAudioPrepared by remember(file.absolutePath) { mutableStateOf(false) }

    val mediaPlayer = remember(file.absolutePath) {
        MediaPlayer().apply {
            setOnErrorListener { _, _, _ ->
                isAudioPrepared = false
                isPlaying = false
                true
            }
            try {
                setDataSource(file.absolutePath)
                prepare()
                isAudioPrepared = true
                durationMs = duration.coerceAtLeast(1)
                start()
                isPlaying = true
            } catch (_: Exception) {
                isAudioPrepared = false
                isPlaying = false
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(60L)
        runCatching { playBtnFocus.requestFocus() }
    }

    DisposableEffect(mediaPlayer) {
        onDispose {
            isAudioPrepared = false
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
            } catch (_: Exception) {}
            try {
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(mediaPlayer, isPlaying, isAudioPrepared) {
        while (isPlaying && isAudioPrepared) {
            try {
                if (mediaPlayer.isPlaying) {
                    currentPosMs = mediaPlayer.currentPosition
                }
            } catch (_: Exception) {}
            delay(250L)
        }
    }

    val discScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.05f else 0.96f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "AudioDiscScale"
    )

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1E1B4B).copy(alpha = 0.94f),
                            Color(0xFF090D16).copy(alpha = 0.97f)
                        )
                    )
                )
                .onPreviewKeyEvent { ev ->
                    if (ev.type == KeyEventType.KeyDown && ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                        onClose()
                        true
                    } else false
                },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(34.dp),
                color = Color(0xFF11182B),
                border = BorderStroke(
                    width = 1.8.dp,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF38BDF8), Color(0xFFA855F7), Color(0xFFF59E0B))
                    )
                ),
                modifier = Modifier
                    .width(680.dp)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Top Expressive Studio Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF38BDF8).copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isPlaying) "HI-RES AUDIO • PLAYING" else "HI-RES AUDIO • PAUSED",
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF))
                        ) {
                            Text(
                                text = "${file.extension.uppercase().ifBlank { "AUDIO" }} • ${formatBytesSize(file.length())}",
                                color = Color(0xFFCBD5E1),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Expressive Vinyl / Equalizer Disc
                    Box(
                        modifier = Modifier
                            .size(124.dp)
                            .scale(discScale)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    colors = listOf(
                                        Color(0xFF38BDF8).copy(alpha = 0.32f),
                                        Color(0xFFA855F7).copy(alpha = 0.32f),
                                        Color(0xFFF59E0B).copy(alpha = 0.32f),
                                        Color(0xFF38BDF8).copy(alpha = 0.32f)
                                    )
                                )
                            )
                            .border(2.5.dp, Color(0xFF38BDF8).copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0B1020))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (isPlaying) Color(0xFF38BDF8) else Color(0xFFF59E0B),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = file.nameWithoutExtension.ifBlank { file.name },
                            color = Color.White,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = file.parentFile?.name ?: "Local Storage",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Time & D-Pad Scrubbable Seekbar (Press Up from Play/Pause to focus Seekbar, Left/Right to scrub)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatDurationMs(currentPosMs.toLong()),
                                color = Color(0xFF38BDF8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isSeekBarFocused) "D-Pad Left / Right to Scrub" else "Press Up on D-Pad to Scrub",
                                color = if (isSeekBarFocused) Color(0xFF10B981) else Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatDurationMs(durationMs.toLong()),
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        val progressFraction = (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(seekBarFocus)
                                .onFocusChanged { isSeekBarFocused = it.isFocused }
                                .onPreviewKeyEvent { ev ->
                                    if (ev.type == KeyEventType.KeyDown) {
                                        val code = ev.nativeKeyEvent.keyCode
                                        val step = if (ev.nativeKeyEvent.repeatCount > 4) 15_000 else 5_000
                                        when (code) {
                                            KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                if (isAudioPrepared) {
                                                    runCatching {
                                                        val target = (currentPosMs - step).coerceAtLeast(0)
                                                        mediaPlayer.seekTo(target)
                                                        currentPosMs = target
                                                    }
                                                }
                                                true
                                            }
                                            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                if (isAudioPrepared) {
                                                    runCatching {
                                                        val target = (currentPosMs + step).coerceAtMost(durationMs)
                                                        mediaPlayer.seekTo(target)
                                                        currentPosMs = target
                                                    }
                                                }
                                                true
                                            }
                                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                runCatching { playBtnFocus.requestFocus() }.isSuccess
                                            }
                                            else -> false
                                        }
                                    } else false
                                }
                                .focusable()
                                .padding(vertical = 6.dp)
                                .testTag("audio_seekbar")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (isSeekBarFocused) 12.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = if (isSeekBarFocused) 0.32f else 0.16f))
                                    .then(
                                        if (isSeekBarFocused) {
                                            Modifier.border(1.5.dp, Color.White, CircleShape)
                                        } else Modifier
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFraction)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = if (isSeekBarFocused) {
                                                    listOf(Color(0xFF10B981), Color(0xFF38BDF8))
                                                } else {
                                                    listOf(Color(0xFF38BDF8), Color(0xFFA855F7))
                                                }
                                            )
                                        )
                                )
                            }
                        }
                    }

                    // Transport Controls Row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExplorerHeaderButton(
                            label = "-10s",
                            icon = Icons.Default.Replay10,
                            testTag = "audio_rw_btn",
                            onClick = {
                                if (isAudioPrepared) {
                                    runCatching {
                                        val target = (mediaPlayer.currentPosition - 10_000).coerceAtLeast(0)
                                        mediaPlayer.seekTo(target)
                                        currentPosMs = target
                                    }
                                }
                            }
                        )
                        Box(
                            modifier = Modifier
                                .focusRequester(playBtnFocus)
                                .onPreviewKeyEvent { ev ->
                                    if (ev.type == KeyEventType.KeyDown && ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                        runCatching { seekBarFocus.requestFocus() }.isSuccess
                                    } else false
                                }
                        ) {
                            ExplorerHeaderButton(
                                label = if (isPlaying) "Pause" else "Play",
                                icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                accentColor = Color(0xFF10B981),
                                testTag = "audio_play_pause_btn",
                                onClick = {
                                    if (isAudioPrepared) {
                                        runCatching {
                                            if (mediaPlayer.isPlaying) {
                                                mediaPlayer.pause()
                                                isPlaying = false
                                            } else {
                                                mediaPlayer.start()
                                                isPlaying = true
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        ExplorerHeaderButton(
                            label = "+10s",
                            icon = Icons.Default.Forward10,
                            testTag = "audio_ff_btn",
                            onClick = {
                                if (isAudioPrepared) {
                                    runCatching {
                                        val target = (mediaPlayer.currentPosition + 10_000).coerceAtMost(durationMs)
                                        mediaPlayer.seekTo(target)
                                        currentPosMs = target
                                    }
                                }
                            }
                        )
                        ExplorerHeaderButton(
                            label = if (isLooping) "Loop: ON" else "Loop: OFF",
                            icon = Icons.Default.Repeat,
                            accentColor = if (isLooping) Color(0xFFF59E0B) else Color(0xFF38BDF8),
                            testTag = "audio_loop_btn",
                            onClick = {
                                if (isAudioPrepared) {
                                    runCatching {
                                        isLooping = !isLooping
                                        mediaPlayer.isLooping = isLooping
                                    }
                                }
                            }
                        )
                        ExplorerHeaderButton(
                            label = "Open with",
                            icon = Icons.Default.Share,
                            testTag = "audio_open_with_btn",
                            onClick = { launchOpenWithChooser(context, file) }
                        )
                        ExplorerHeaderButton(
                            label = "Close",
                            icon = Icons.Default.Close,
                            accentColor = Color(0xFFEF4444),
                            testTag = "audio_close_btn",
                            onClick = onClose
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full-Screen Video / .MKV / .MOV / Blu-ray Player with:
 * - Full-screen Dialog focus isolation + persistent playerRootFocusRequester so D-Pad OK / Center NEVER leaks to File Explorer when controls auto-hide
 * - 10-Second Auto-Hide Controls on D-Pad inactivity
 * - D-Pad Focusable Seekbar with Left/Right Press-and-Hold Rewind / Fast-Forward
 * - Audio Track Selector (`MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO` + `MediaExtractor`)
 * - Built-in Embedded Subtitle Track Selector for `.mkv` / `.mp4` (`MEDIA_TRACK_TYPE_SUBTITLE` / `MEDIA_TRACK_TYPE_TIMEDTEXT` + `MediaExtractor`)
 * - Optional Subtitle File Import (`.srt` / `.vtt` / `.ass` / `.ssa`) — never auto-loads random external subtitles
 */
@Composable
fun BuiltInVideoPlayerOverlay(
    file: File,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mediaPlayerRef by remember { mutableStateOf<MediaPlayer?>(null) }
    var isVideoPrepared by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(1L) }

    // 10-Second Auto-Hide Controls State + Persistent Root Focus Catcher when Controls Hide
    var controlsVisible by remember { mutableStateOf(true) }
    var lastControlActivityRealtimeMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val playerRootFocusRequester = remember { FocusRequester() }
    val playPauseFocusRequester = remember { FocusRequester() }
    val seekBarFocusRequester = remember { FocusRequester() }
    val topCloseFocusRequester = remember { FocusRequester() }

    // Audio Tracks
    var audioTracks by remember { mutableStateOf<List<MediaTrackOption>>(emptyList()) }
    var selectedAudioTrackIndex by remember { mutableIntStateOf(-1) }
    var showAudioDialog by remember { mutableStateOf(false) }

    // Embedded Subtitle Tracks (.mkv / .mp4) + Optional Imported External Subtitle File
    var embeddedSubtitleTracks by remember { mutableStateOf<List<MediaTrackOption>>(emptyList()) }
    var selectedEmbeddedSubtitleIndex by remember { mutableIntStateOf(-1) }
    var embeddedTimedTextCue by remember { mutableStateOf<String?>(null) }
    var parsedEmbeddedSubtitles by remember(file.absolutePath) { mutableStateOf<List<SubtitleCue>>(emptyList()) }
    var isExtractingEmbeddedSubtitles by remember { mutableStateOf(false) }

    var activeExternalSubtitleFile by remember { mutableStateOf<File?>(null) }
    var parsedExternalSubtitles by remember { mutableStateOf<List<SubtitleCue>>(emptyList()) }
    var subtitlesEnabled by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showSubtitleFileImporterDialog by remember { mutableStateOf(false) }
    var availableDeviceSubtitleFiles by remember { mutableStateOf<List<File>>(emptyList()) }

    // Subtitle Customization State (defaults aligned with option chips so tick marks match immediately)
    var subtitleFontSizeSp by remember { mutableIntStateOf(22) }
    var subtitleColor by remember { mutableStateOf(Color.White) }
    var subtitleBgAlpha by remember { mutableFloatStateOf(0.55f) }

    fun notifyPlayerUserActivity() {
        lastControlActivityRealtimeMs = SystemClock.elapsedRealtime()
        if (!controlsVisible) {
            controlsVisible = true
            scope.launch {
                delay(45L)
                runCatching { playPauseFocusRequester.requestFocus() }
            }
        }
    }

    fun selectEmbeddedSubtitleTrack(track: MediaTrackOption) {
        subtitlesEnabled = true
        activeExternalSubtitleFile = null
        selectedEmbeddedSubtitleIndex = track.index
        embeddedTimedTextCue = null
        if (track.isEmbeddedPlayerTrack) {
            runCatching { mediaPlayerRef?.selectTrack(track.index) }
        }
        isExtractingEmbeddedSubtitles = true
        scope.launch {
            val cues = withContext(Dispatchers.IO) {
                extractEmbeddedSubtitleCues(
                    file = file,
                    targetExtractorTrackIndex = track.extractorTrackIndex,
                    subtitleOrdinal = track.subtitleOrdinal
                )
            }
            if (selectedEmbeddedSubtitleIndex == track.index) {
                parsedEmbeddedSubtitles = cues
                isExtractingEmbeddedSubtitles = false
            }
        }
    }

    // Whenever controls auto-hide, immediately move focus to the persistent playerRootFocusRequester
    // so D-Pad OK / Center / Directions are caught by the video player and NEVER leak to the underlying File Explorer!
    LaunchedEffect(controlsVisible) {
        if (!controlsVisible) {
            runCatching { playerRootFocusRequester.requestFocus() }
        } else {
            delay(45L)
            runCatching { playPauseFocusRequester.requestFocus() }
        }
    }

    // Auto-hide controls after 10 seconds of inactivity while playing
    LaunchedEffect(controlsVisible, isPlaying, showAudioDialog, showSubtitleDialog, showSubtitleFileImporterDialog) {
        if (!controlsVisible || !isPlaying || showAudioDialog || showSubtitleDialog || showSubtitleFileImporterDialog) {
            return@LaunchedEffect
        }
        while (controlsVisible && isPlaying && !showAudioDialog && !showSubtitleDialog && !showSubtitleFileImporterDialog) {
            delay(500L)
            if (SystemClock.elapsedRealtime() - lastControlActivityRealtimeMs >= 10_000L) {
                controlsVisible = false
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(80L)
        runCatching { playPauseFocusRequester.requestFocus() }
    }

    // Parse external subtitle file ONLY when the user explicitly imports one
    LaunchedEffect(activeExternalSubtitleFile) {
        val sub = activeExternalSubtitleFile
        if (sub != null && sub.exists()) {
            parsedExternalSubtitles = withContext(Dispatchers.IO) { parseSrtOrVttFile(sub) }
        } else {
            parsedExternalSubtitles = emptyList()
        }
    }

    LaunchedEffect(mediaPlayerRef, isPlaying, isVideoPrepared) {
        while (isPlaying && isVideoPrepared) {
            try {
                val mp = mediaPlayerRef
                if (mp != null && mp.isPlaying) {
                    currentPosMs = mp.currentPosition.toLong()
                }
            } catch (_: Exception) {}
            delay(180L)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            isVideoPrepared = false
            try {
                val mp = mediaPlayerRef
                if (mp != null && mp.isPlaying) {
                    mp.stop()
                }
            } catch (_: Exception) {}
            try {
                mediaPlayerRef?.release()
            } catch (_: Exception) {}
            mediaPlayerRef = null
        }
    }

    val currentSubtitleText = remember(
        currentPosMs,
        parsedExternalSubtitles,
        parsedEmbeddedSubtitles,
        embeddedTimedTextCue,
        subtitlesEnabled,
        activeExternalSubtitleFile,
        selectedEmbeddedSubtitleIndex
    ) {
        if (!subtitlesEnabled) {
            null
        } else if (activeExternalSubtitleFile != null && parsedExternalSubtitles.isNotEmpty()) {
            findSubtitleCueAtPosition(parsedExternalSubtitles, currentPosMs)
        } else if (selectedEmbeddedSubtitleIndex >= 0) {
            findSubtitleCueAtPosition(parsedEmbeddedSubtitles, currentPosMs) ?: embeddedTimedTextCue
        } else {
            null
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(playerRootFocusRequester)
                .onPreviewKeyEvent { ev ->
                    if (ev.type == KeyEventType.KeyDown) {
                        val code = ev.nativeKeyEvent.keyCode
                        if (code == KeyEvent.KEYCODE_BACK) {
                            if (showSubtitleFileImporterDialog) {
                                showSubtitleFileImporterDialog = false
                                return@onPreviewKeyEvent true
                            }
                            if (showSubtitleDialog) {
                                showSubtitleDialog = false
                                return@onPreviewKeyEvent true
                            }
                            if (showAudioDialog) {
                                showAudioDialog = false
                                return@onPreviewKeyEvent true
                            }
                            onClose()
                            return@onPreviewKeyEvent true
                        }
                        if (!controlsVisible) {
                            notifyPlayerUserActivity()
                            return@onPreviewKeyEvent true
                        }
                        lastControlActivityRealtimeMs = SystemClock.elapsedRealtime()
                    } else if (ev.type == KeyEventType.KeyUp) {
                        // Also consume KeyUp when waking controls so OK KeyUp doesn't trigger an unintended click
                        if (!controlsVisible) {
                            notifyPlayerUserActivity()
                            return@onPreviewKeyEvent true
                        }
                    }
                    false
                }
                .focusable()
        ) {
            AndroidView(
                factory = { ctx ->
                    SurfaceView(ctx).apply {
                        isFocusable = false
                        isFocusableInTouchMode = false
                        holder.addCallback(object : SurfaceHolder.Callback {
                            override fun surfaceCreated(holder: SurfaceHolder) {
                                try {
                                    isVideoPrepared = false
                                    val mp = MediaPlayer()
                                    mediaPlayerRef = mp
                                    mp.setDisplay(holder)
                                    mp.setDataSource(file.absolutePath)
                                    mp.setOnErrorListener { _, _, _ ->
                                        isVideoPrepared = false
                                        isPlaying = false
                                        true
                                    }
                                    mp.setOnTimedTextListener { _, timedText: TimedText? ->
                                        embeddedTimedTextCue = timedText?.text?.trim()?.takeIf { it.isNotEmpty() }
                                    }
                                    mp.setOnPreparedListener { player ->
                                        if (mediaPlayerRef !== player) return@setOnPreparedListener
                                        isVideoPrepared = true
                                        durationMs = player.duration.toLong().coerceAtLeast(1L)
                                        runCatching {
                                            player.start()
                                            isPlaying = true
                                        }

                                        val discoveredAudio = mutableListOf<MediaTrackOption>()
                                        val discoveredSubs = mutableListOf<MediaTrackOption>()

                                        // Inspect container via MediaExtractor first so we capture exact MKV/MP4 subtitle track indices
                                        val extractorAudioTracks = mutableListOf<Triple<Int, String, String>>()
                                        val extractorSubtitleTracks = mutableListOf<Triple<Int, String, String>>()
                                        try {
                                            val extractor = MediaExtractor()
                                            extractor.setDataSource(file.absolutePath)
                                            for (i in 0 until extractor.trackCount) {
                                                val fmt = extractor.getTrackFormat(i)
                                                val mime = fmt.getString(MediaFormat.KEY_MIME).orEmpty()
                                                val lang = if (fmt.containsKey(MediaFormat.KEY_LANGUAGE)) {
                                                    fmt.getString(MediaFormat.KEY_LANGUAGE)
                                                        ?.takeIf { it.isNotBlank() && it != "und" } ?: "Default"
                                                } else "Default"
                                                if (mime.startsWith("audio/")) {
                                                    extractorAudioTracks.add(Triple(i, mime, lang))
                                                } else if (isSubtitleMimeType(mime)) {
                                                    extractorSubtitleTracks.add(Triple(i, mime, lang))
                                                }
                                            }
                                            extractor.release()
                                        } catch (_: Exception) {}

                                        // Discover MediaPlayer tracks and link embedded subtitle tracks to their MediaExtractor index
                                        try {
                                            var playerSubOrdinal = 0
                                            player.trackInfo?.forEachIndexed { idx, info ->
                                                val lang = info.language.takeIf { it.isNotBlank() && it != "und" } ?: "Default"
                                                when (info.trackType) {
                                                    MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO -> {
                                                        discoveredAudio.add(
                                                            MediaTrackOption(
                                                                index = idx,
                                                                label = "Audio Track ${discoveredAudio.size + 1} ($lang)",
                                                                language = lang,
                                                                isEmbeddedPlayerTrack = true
                                                            )
                                                        )
                                                    }
                                                    MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT,
                                                    MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE -> {
                                                        val matchedExtractorIdx = extractorSubtitleTracks.getOrNull(playerSubOrdinal)?.first
                                                        discoveredSubs.add(
                                                            MediaTrackOption(
                                                                index = idx,
                                                                label = "Built-in Subtitle ${discoveredSubs.size + 1} ($lang)",
                                                                language = lang,
                                                                isEmbeddedPlayerTrack = true,
                                                                extractorTrackIndex = matchedExtractorIdx,
                                                                subtitleOrdinal = playerSubOrdinal
                                                            )
                                                        )
                                                        playerSubOrdinal++
                                                    }
                                                }
                                            }
                                        } catch (_: Exception) {}

                                        // Add any container tracks missed by MediaPlayer (common for Matroska .mkv S_TEXT/UTF8 & ASS tracks)
                                        extractorAudioTracks.forEach { (trackIdx, mime, lang) ->
                                            if (discoveredAudio.none { it.index == trackIdx }) {
                                                discoveredAudio.add(
                                                    MediaTrackOption(
                                                        index = trackIdx,
                                                        label = "Audio Track ${discoveredAudio.size + 1} • ${mime.substringAfter("/").uppercase()} ($lang)",
                                                        language = lang,
                                                        isEmbeddedPlayerTrack = false,
                                                        extractorTrackIndex = trackIdx
                                                    )
                                                )
                                            }
                                        }
                                        extractorSubtitleTracks.forEachIndexed { ordinal, (trackIdx, mime, lang) ->
                                            val alreadyLinked = discoveredSubs.any { it.extractorTrackIndex == trackIdx }
                                            if (!alreadyLinked) {
                                                val shortCodec = mime.substringAfter("/").uppercase().take(10)
                                                discoveredSubs.add(
                                                    MediaTrackOption(
                                                        index = 1000 + trackIdx,
                                                        label = "Built-in Subtitle ${discoveredSubs.size + 1} • $shortCodec ($lang)",
                                                        language = lang,
                                                        isEmbeddedPlayerTrack = false,
                                                        extractorTrackIndex = trackIdx,
                                                        subtitleOrdinal = ordinal
                                                    )
                                                )
                                            }
                                        }

                                        audioTracks = discoveredAudio
                                        if (discoveredAudio.isNotEmpty() && selectedAudioTrackIndex == -1) {
                                            selectedAudioTrackIndex = discoveredAudio.first().index
                                        }
                                        embeddedSubtitleTracks = discoveredSubs
                                    }
                                    mp.prepareAsync()
                                } catch (e: Exception) {
                                    Toast.makeText(ctx, "Playback error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                            override fun surfaceDestroyed(holder: SurfaceHolder) {
                                isVideoPrepared = false
                                try {
                                    mediaPlayerRef?.release()
                                    mediaPlayerRef = null
                                } catch (_: Exception) {}
                            }
                        })
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Synchronized Subtitle Overlay (supports MKV embedded extracted cues, MediaPlayer timed text, and imported SRT/VTT/ASS)
            if (!currentSubtitleText.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (controlsVisible) 172.dp else 44.dp, start = 56.dp, end = 56.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = subtitleBgAlpha))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = currentSubtitleText,
                        color = subtitleColor,
                        fontSize = subtitleFontSizeSp.sp,
                        lineHeight = (subtitleFontSizeSp + 6).sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Top Floating Cinema Header Bar (Auto-Hides with HUD)
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn() + slideInVertically { -it / 2 },
                exit = fadeOut() + slideOutVertically { -it / 2 },
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xE6020617),
                                    Color(0x88020617),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 32.dp, vertical = 22.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0EA5E9).copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.65f))
                        ) {
                            Text(
                                text = file.extension.uppercase().ifBlank { "VIDEO" },
                                color = Color(0xFF7DD3FC),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.nameWithoutExtension.ifBlank { file.name },
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isPlaying) "NOW PLAYING" else "PAUSED",
                                    color = if (isPlaying) Color(0xFF34D399) else Color(0xFFFBBF24),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                                if (subtitlesEnabled) {
                                    Text(
                                        text = if (isExtractingEmbeddedSubtitles) "• Loading Subtitles..." else "• Subtitles Active",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val remainingMs = (durationMs - currentPosMs).coerceAtLeast(0L)
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xAA0F172A),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
                        ) {
                            Text(
                                text = "-${formatDurationMs(remainingMs)} left",
                                color = Color(0xFFCBD5E1),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .focusRequester(topCloseFocusRequester)
                                .onPreviewKeyEvent { ev ->
                                    if (ev.type == KeyEventType.KeyDown && ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                                        notifyPlayerUserActivity()
                                        runCatching { seekBarFocusRequester.requestFocus() }.isSuccess
                                    } else {
                                        false
                                    }
                                }
                        ) {
                            ExplorerHeaderButton(
                                label = "Close",
                                icon = Icons.Default.Close,
                                accentColor = Color(0xFFF43F5E),
                                testTag = "video_close_btn",
                                onClick = onClose
                            )
                        }
                    }
                }
            }

            // Bottom Floating Glassmorphic Cinema Transport Dock (Auto-Hides after 10 seconds of inactivity)
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                var isSeekBarFocused by remember { mutableStateOf(false) }
                val progressFraction = (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC020617), Color(0xF0020617))
                            )
                        )
                        .padding(horizontal = 28.dp, vertical = 20.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        color = Color(0xDC0B1220),
                        border = BorderStroke(
                            1.2.dp,
                            if (isSeekBarFocused) Color(0xFF34D399).copy(alpha = 0.65f) else Color(0x3338BDF8)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Seekbar Row with Elapsed / Scrubber / Total Time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = formatDurationMs(currentPosMs),
                                    color = if (isSeekBarFocused) Color(0xFF34D399) else Color(0xFF38BDF8),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                // D-Pad Focusable Cinema Seekbar (Hold Left/Right on D-Pad to Rewind / Fast-Forward)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(seekBarFocusRequester)
                                        .onFocusChanged {
                                            isSeekBarFocused = it.isFocused
                                            if (it.isFocused) notifyPlayerUserActivity()
                                        }
                                        .onPreviewKeyEvent { ev ->
                                            if (ev.type == KeyEventType.KeyDown) {
                                                notifyPlayerUserActivity()
                                                val code = ev.nativeKeyEvent.keyCode
                                                val repeat = ev.nativeKeyEvent.repeatCount
                                                val stepMs = when {
                                                    repeat > 12 -> 30_000L
                                                    repeat > 4 -> 15_000L
                                                    else -> 5_000L
                                                }
                                                when (code) {
                                                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                        val mp = mediaPlayerRef
                                                        if (mp != null && isVideoPrepared) {
                                                            runCatching {
                                                                val next = (currentPosMs - stepMs).coerceAtLeast(0L)
                                                                mp.seekTo(next.toInt())
                                                                currentPosMs = next
                                                            }
                                                        }
                                                        true
                                                    }
                                                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                        val mp = mediaPlayerRef
                                                        if (mp != null && isVideoPrepared) {
                                                            runCatching {
                                                                val next = (currentPosMs + stepMs).coerceAtMost(durationMs)
                                                                mp.seekTo(next.toInt())
                                                                currentPosMs = next
                                                            }
                                                        }
                                                        true
                                                    }
                                                    KeyEvent.KEYCODE_DPAD_UP -> {
                                                        runCatching { topCloseFocusRequester.requestFocus() }.isSuccess
                                                    }
                                                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                        runCatching { playPauseFocusRequester.requestFocus() }.isSuccess
                                                    }
                                                    else -> false
                                                }
                                            } else {
                                                false
                                            }
                                        }
                                        .focusable()
                                        .padding(vertical = 8.dp)
                                        .testTag("video_seekbar"),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (isSeekBarFocused) 12.dp else 7.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = if (isSeekBarFocused) 0.28f else 0.16f))
                                            .then(
                                                if (isSeekBarFocused) {
                                                    Modifier.border(1.5.dp, Color.White, CircleShape)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(progressFraction)
                                                .fillMaxHeight()
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = if (isSeekBarFocused) {
                                                            listOf(Color(0xFF38BDF8), Color(0xFF10B981))
                                                        } else {
                                                            listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
                                                        }
                                                    )
                                                )
                                        )
                                    }
                                }

                                Text(
                                    text = formatDurationMs(durationMs),
                                    color = Color(0xFF94A3B8),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Transport & Cinema Actions Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.onPreviewKeyEvent { ev ->
                                            if (ev.type == KeyEventType.KeyDown && ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                                notifyPlayerUserActivity()
                                                runCatching { seekBarFocusRequester.requestFocus() }.isSuccess
                                            } else false
                                        }
                                    ) {
                                        ExplorerHeaderButton(
                                            label = "-10s",
                                            icon = Icons.Default.Replay10,
                                            testTag = "video_rw_btn",
                                            onClick = {
                                                notifyPlayerUserActivity()
                                                val mp = mediaPlayerRef ?: return@ExplorerHeaderButton
                                                if (!isVideoPrepared) return@ExplorerHeaderButton
                                                runCatching {
                                                    val next = (mp.currentPosition - 10_000).coerceAtLeast(0)
                                                    mp.seekTo(next)
                                                    currentPosMs = next.toLong()
                                                }
                                            }
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .focusRequester(playPauseFocusRequester)
                                            .onPreviewKeyEvent { ev ->
                                                if (ev.type == KeyEventType.KeyDown && ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                                    notifyPlayerUserActivity()
                                                    runCatching { seekBarFocusRequester.requestFocus() }.isSuccess
                                                } else false
                                            }
                                    ) {
                                        ExplorerHeaderButton(
                                            label = if (isPlaying) "Pause" else "Play",
                                            icon = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            accentColor = Color(0xFF10B981),
                                            testTag = "video_play_pause_btn",
                                            onClick = {
                                                notifyPlayerUserActivity()
                                                val mp = mediaPlayerRef ?: return@ExplorerHeaderButton
                                                if (!isVideoPrepared) return@ExplorerHeaderButton
                                                runCatching {
                                                    if (mp.isPlaying) {
                                                        mp.pause()
                                                        isPlaying = false
                                                    } else {
                                                        mp.start()
                                                        isPlaying = true
                                                    }
                                                }
                                            }
                                        )
                                    }
                                    Box(
                                        modifier = Modifier.onPreviewKeyEvent { ev ->
                                            if (ev.type == KeyEventType.KeyDown && ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                                notifyPlayerUserActivity()
                                                runCatching { seekBarFocusRequester.requestFocus() }.isSuccess
                                            } else false
                                        }
                                    ) {
                                        ExplorerHeaderButton(
                                            label = "+10s",
                                            icon = Icons.Default.Forward10,
                                            testTag = "video_ff_btn",
                                            onClick = {
                                                notifyPlayerUserActivity()
                                                val mp = mediaPlayerRef ?: return@ExplorerHeaderButton
                                                if (!isVideoPrepared) return@ExplorerHeaderButton
                                                runCatching {
                                                    val next = (mp.currentPosition + 10_000).coerceAtMost(durationMs.toInt())
                                                    mp.seekTo(next)
                                                    currentPosMs = next.toLong()
                                                }
                                            }
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.onPreviewKeyEvent { ev ->
                                            if (ev.type == KeyEventType.KeyDown && ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                                notifyPlayerUserActivity()
                                                runCatching { seekBarFocusRequester.requestFocus() }.isSuccess
                                            } else false
                                        }
                                    ) {
                                        ExplorerHeaderButton(
                                            label = "Audio (${audioTracks.size.coerceAtLeast(1)})",
                                            icon = Icons.Default.GraphicEq,
                                            testTag = "video_audio_tracks_btn",
                                            onClick = {
                                                notifyPlayerUserActivity()
                                                showAudioDialog = true
                                            }
                                        )
                                    }
                                    Box(
                                        modifier = Modifier.onPreviewKeyEvent { ev ->
                                            if (ev.type == KeyEventType.KeyDown && ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                                notifyPlayerUserActivity()
                                                runCatching { seekBarFocusRequester.requestFocus() }.isSuccess
                                            } else false
                                        }
                                    ) {
                                        ExplorerHeaderButton(
                                            label = if (subtitlesEnabled) "Subtitles ✓" else "Subtitles",
                                            icon = Icons.Default.Subtitles,
                                            accentColor = if (subtitlesEnabled) Color(0xFF10B981) else Color(0xFF38BDF8),
                                            testTag = "video_subtitles_btn",
                                            onClick = {
                                                notifyPlayerUserActivity()
                                                showSubtitleDialog = true
                                            }
                                        )
                                    }
                                    Box(
                                        modifier = Modifier.onPreviewKeyEvent { ev ->
                                            if (ev.type == KeyEventType.KeyDown && ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                                notifyPlayerUserActivity()
                                                runCatching { seekBarFocusRequester.requestFocus() }.isSuccess
                                            } else false
                                        }
                                    ) {
                                        ExplorerHeaderButton(
                                            label = "Open with",
                                            icon = Icons.AutoMirrored.Filled.OpenInNew,
                                            testTag = "video_open_with_btn",
                                            onClick = {
                                                notifyPlayerUserActivity()
                                                launchOpenWithChooser(context, file)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Audio Track Selector Dialog (Focus-Trapping & D-Pad Scrollable)
            if (showAudioDialog) {
                val firstAudioFocus = remember { FocusRequester() }
                val audioScroll = rememberScrollState()
                LaunchedEffect(Unit) {
                    delay(50L)
                    runCatching { firstAudioFocus.requestFocus() }
                }
                Dialog(onDismissRequest = { showAudioDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF0B1220),
                        border = BorderStroke(1.5.dp, Color(0x4438BDF8)),
                        modifier = Modifier
                            .width(470.dp)
                            .heightIn(max = 520.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Select Audio Track", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                            Column(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .verticalScroll(audioScroll),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (audioTracks.isEmpty()) {
                                    Text("✓ Default Primary Audio Stream Active", color = Color(0xFF34D399), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                } else {
                                    audioTracks.forEachIndexed { idx, track ->
                                        val isChosen = selectedAudioTrackIndex == track.index
                                        Box(modifier = if (idx == 0) Modifier.focusRequester(firstAudioFocus) else Modifier) {
                                            CinemaSelectableTrackRow(
                                                label = track.label,
                                                selected = isChosen,
                                                icon = Icons.Default.VolumeUp,
                                                testTag = "audio_track_$idx",
                                                onClick = {
                                                    try {
                                                        if (track.isEmbeddedPlayerTrack) {
                                                            mediaPlayerRef?.selectTrack(track.index)
                                                        }
                                                        selectedAudioTrackIndex = track.index
                                                    } catch (_: Exception) {}
                                                    showAudioDialog = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Box(modifier = if (audioTracks.isEmpty()) Modifier.focusRequester(firstAudioFocus) else Modifier) {
                                    ExplorerHeaderButton(
                                        label = "Done",
                                        icon = Icons.Default.Check,
                                        accentColor = Color(0xFF10B981),
                                        testTag = "audio_track_done_btn",
                                        onClick = { showAudioDialog = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Subtitle Track & Customizer Dialog (D-Pad Scrollable + Working MKV Subtitles + Clear Tick Marks)
            if (showSubtitleDialog) {
                val firstSubFocus = remember { FocusRequester() }
                val subScroll = rememberScrollState()
                LaunchedEffect(Unit) {
                    delay(50L)
                    runCatching { firstSubFocus.requestFocus() }
                }
                Dialog(onDismissRequest = { showSubtitleDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF0B1220),
                        border = BorderStroke(1.5.dp, Color(0x4438BDF8)),
                        modifier = Modifier
                            .width(580.dp)
                            .heightIn(max = 580.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Subtitles & Captions", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                                if (isExtractingEmbeddedSubtitles) {
                                    Text(
                                        text = "Extracting MKV track...",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .verticalScroll(subScroll),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(modifier = Modifier.focusRequester(firstSubFocus)) {
                                    CinemaSelectableTrackRow(
                                        label = if (!subtitlesEnabled) "Subtitles: Off" else "Turn Off Subtitles",
                                        selected = !subtitlesEnabled,
                                        icon = Icons.Default.SubtitlesOff,
                                        testTag = "sub_track_off",
                                        onClick = {
                                            subtitlesEnabled = false
                                            selectedEmbeddedSubtitleIndex = -1
                                            activeExternalSubtitleFile = null
                                            parsedEmbeddedSubtitles = emptyList()
                                            embeddedTimedTextCue = null
                                        }
                                    )
                                }

                                // Built-in / Embedded Subtitle Tracks (.mkv / .mp4)
                                if (embeddedSubtitleTracks.isNotEmpty()) {
                                    Text(
                                        text = "Built-in Video Subtitles (${embeddedSubtitleTracks.size})",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    embeddedSubtitleTracks.forEachIndexed { idx, track ->
                                        val isSelected = subtitlesEnabled && activeExternalSubtitleFile == null && selectedEmbeddedSubtitleIndex == track.index
                                        val suffix = if (isSelected && parsedEmbeddedSubtitles.isNotEmpty()) {
                                            " • ${parsedEmbeddedSubtitles.size} cues"
                                        } else ""
                                        CinemaSelectableTrackRow(
                                            label = "${track.label}$suffix",
                                            selected = isSelected,
                                            icon = Icons.Default.ClosedCaption,
                                            testTag = "sub_embedded_$idx",
                                            onClick = { selectEmbeddedSubtitleTrack(track) }
                                        )
                                    }
                                } else if (file.extension.equals("mkv", ignoreCase = true)) {
                                    Text(
                                        text = "No embedded subtitle stream detected in this .mkv file",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }

                                // Active Imported External Subtitle File (if user imported one)
                                if (activeExternalSubtitleFile != null) {
                                    val isExtSelected = subtitlesEnabled && activeExternalSubtitleFile != null
                                    CinemaSelectableTrackRow(
                                        label = "Imported: ${activeExternalSubtitleFile!!.name}",
                                        selected = isExtSelected,
                                        icon = Icons.Default.Subtitles,
                                        testTag = "sub_external_active",
                                        onClick = {
                                            subtitlesEnabled = true
                                            selectedEmbeddedSubtitleIndex = -1
                                        }
                                    )
                                }

                                // Optional Import Subtitle File Button
                                ExplorerMenuOptionButton(
                                    label = "Import Subtitle File (.srt / .vtt / .ass)",
                                    icon = Icons.Default.FileOpen
                                ) {
                                    scope.launch {
                                        availableDeviceSubtitleFiles = scanFilesRecursive(context, ::isSubtitleFile)
                                        showSubtitleFileImporterDialog = true
                                    }
                                }

                                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                                Text("Subtitle Font Size", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(16 to "Small", 22 to "Medium", 28 to "Large", 34 to "XL").forEach { (sz, lbl) ->
                                        CinemaOptionTickChip(
                                            label = lbl,
                                            selected = subtitleFontSizeSp == sz,
                                            testTag = "sub_size_$sz",
                                            onClick = { subtitleFontSizeSp = sz }
                                        )
                                    }
                                }

                                Text("Subtitle Color", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        Color.White to "White",
                                        Color(0xFFFACC15) to "Yellow",
                                        Color(0xFF38BDF8) to "Cyan",
                                        Color(0xFF4ADE80) to "Green"
                                    ).forEach { (col, lbl) ->
                                        CinemaOptionTickChip(
                                            label = lbl,
                                            selected = subtitleColor == col,
                                            swatchColor = col,
                                            testTag = "sub_color_$lbl",
                                            onClick = { subtitleColor = col }
                                        )
                                    }
                                }

                                Text("Background Opacity", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(0f to "Clear", 0.55f to "Semi", 0.9f to "Solid").forEach { (alpha, lbl) ->
                                        CinemaOptionTickChip(
                                            label = lbl,
                                            selected = kotlin.math.abs(subtitleBgAlpha - alpha) < 0.05f,
                                            testTag = "sub_bg_$lbl",
                                            onClick = { subtitleBgAlpha = alpha }
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                ExplorerHeaderButton(
                                    label = "Done",
                                    icon = Icons.Default.Check,
                                    accentColor = Color(0xFF10B981),
                                    testTag = "sub_dialog_done_btn",
                                    onClick = { showSubtitleDialog = false }
                                )
                            }
                        }
                    }
                }
            }

            // Optional Subtitle File Importer Dialog
            if (showSubtitleFileImporterDialog) {
                val firstImportFocus = remember { FocusRequester() }
                val importScroll = rememberScrollState()
                LaunchedEffect(Unit) {
                    delay(50L)
                    runCatching { firstImportFocus.requestFocus() }
                }
                Dialog(onDismissRequest = { showSubtitleFileImporterDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF0B1220),
                        border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                        modifier = Modifier
                            .width(500.dp)
                            .heightIn(max = 520.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Import Subtitle File",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .verticalScroll(importScroll),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (availableDeviceSubtitleFiles.isEmpty()) {
                                    Text(
                                        text = "No .srt, .vtt, or .ass subtitle files found on storage.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp
                                    )
                                } else {
                                    availableDeviceSubtitleFiles.forEachIndexed { index, subFile ->
                                        Box(modifier = if (index == 0) Modifier.focusRequester(firstImportFocus) else Modifier) {
                                            ExplorerMenuOptionButton(
                                                label = subFile.name,
                                                icon = Icons.Default.Subtitles
                                            ) {
                                                activeExternalSubtitleFile = subFile
                                                selectedEmbeddedSubtitleIndex = -1
                                                parsedEmbeddedSubtitles = emptyList()
                                                subtitlesEnabled = true
                                                showSubtitleFileImporterDialog = false
                                                Toast.makeText(context, "Loaded ${subFile.name}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            }
                            Box(modifier = if (availableDeviceSubtitleFiles.isEmpty()) Modifier.focusRequester(firstImportFocus) else Modifier) {
                                ExplorerHeaderButton(
                                    label = "Cancel",
                                    icon = Icons.Default.Close,
                                    testTag = "sub_import_cancel_btn",
                                    onClick = { showSubtitleFileImporterDialog = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CinemaSelectableTrackRow(
    label: String,
    selected: Boolean,
    icon: ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "trackRowScale"
    )
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = when {
            isFocused -> Color(0xFF1E3A5F)
            selected -> Color(0xFF064E3B).copy(alpha = 0.65f)
            else -> Color(0xFF111C30)
        },
        border = BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = when {
                isFocused -> Color.White
                selected -> Color(0xFF34D399)
                else -> Color.White.copy(alpha = 0.10f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color(0xFF34D399) else Color(0xFF38BDF8),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = if (selected || isFocused) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF10B981)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color(0xFF022C22),
                        modifier = Modifier
                            .padding(3.dp)
                            .size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CinemaOptionTickChip(
    label: String,
    selected: Boolean,
    swatchColor: Color? = null,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "subChipScale"
    )
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = when {
            isFocused -> Color(0xFF1E3A5F)
            selected -> Color(0xFF065F46)
            else -> Color(0xFF131D31)
        },
        border = BorderStroke(
            width = if (isFocused) 2.dp else 1.2.dp,
            color = when {
                isFocused -> Color.White
                selected -> Color(0xFF34D399)
                else -> Color.White.copy(alpha = 0.14f)
            }
        ),
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (swatchColor != null) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                        .border(1.dp, Color.Black.copy(alpha = 0.45f), CircleShape)
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color(0xFF34D399),
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = label,
                color = if (selected || isFocused) Color.White else Color(0xFFCBD5E1),
                fontSize = 13.sp,
                fontWeight = if (selected || isFocused) FontWeight.ExtraBold else FontWeight.Medium
            )
        }
    }
}

private fun isSubtitleMimeType(mime: String): Boolean {
    return mime.startsWith("text/", ignoreCase = true) ||
        mime.contains("subrip", ignoreCase = true) ||
        mime.contains("subtitle", ignoreCase = true) ||
        mime.contains("ttml", ignoreCase = true) ||
        mime.contains("vtt", ignoreCase = true) ||
        mime.contains("ssa", ignoreCase = true) ||
        mime.contains("ass", ignoreCase = true) ||
        mime.contains("tx3g", ignoreCase = true)
}

private fun findSubtitleCueAtPosition(cues: List<SubtitleCue>, posMs: Long): String? {
    if (cues.isEmpty()) return null
    val active = cues.filter { posMs in it.startMs..it.endMs }
    return if (active.isEmpty()) null else active.joinToString("\n") { it.text }
}

private fun extractEmbeddedSubtitleCues(
    file: File,
    targetExtractorTrackIndex: Int?,
    subtitleOrdinal: Int
): List<SubtitleCue> {
    val rawSamples = mutableListOf<Pair<Long, String>>()
    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(file.absolutePath)
        val subTrackIndices = mutableListOf<Int>()
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME).orEmpty()
            if (isSubtitleMimeType(mime)) {
                subTrackIndices.add(i)
            }
        }
        val trackToUse = when {
            targetExtractorTrackIndex != null && targetExtractorTrackIndex in subTrackIndices -> targetExtractorTrackIndex
            subtitleOrdinal in subTrackIndices.indices -> subTrackIndices[subtitleOrdinal]
            subTrackIndices.isNotEmpty() -> subTrackIndices.first()
            else -> return emptyList()
        }

        val trackMime = extractor.getTrackFormat(trackToUse).getString(MediaFormat.KEY_MIME).orEmpty()
        extractor.selectTrack(trackToUse)
        val buffer = ByteBuffer.allocate(256 * 1024)
        var safetyCounter = 0

        while (safetyCounter < 50_000) {
            buffer.clear()
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break
            val sampleTimeUs = extractor.sampleTime
            val startMs = (sampleTimeUs / 1000L).coerceAtLeast(0L)
            if (sampleSize > 0) {
                val bytes = ByteArray(sampleSize)
                buffer.position(0)
                buffer.get(bytes)
                val decoded = decodeSubtitleSamplePayload(bytes, trackMime)
                rawSamples.add(startMs to decoded)
            } else {
                // Empty packet marks end-of-cue in some Matroska/MP4 subtitle streams
                rawSamples.add(startMs to "")
            }
            if (!extractor.advance()) break
            safetyCounter++
        }
    } catch (_: Exception) {
    } finally {
        runCatching { extractor.release() }
    }

    if (rawSamples.isEmpty()) return emptyList()

    val cues = mutableListOf<SubtitleCue>()
    for (i in rawSamples.indices) {
        val (startMs, text) = rawSamples[i]
        if (text.isBlank()) continue
        val nextSampleTime = rawSamples.getOrNull(i + 1)?.first
        val estimatedDurationMs = (1800L + (text.length * 48L)).coerceIn(2200L, 6500L)
        val endMs = if (nextSampleTime != null && nextSampleTime > startMs) {
            if (rawSamples[i + 1].second.isBlank()) {
                nextSampleTime
            } else {
                minOf(nextSampleTime - 40L, startMs + estimatedDurationMs).coerceAtLeast(startMs + 900L)
            }
        } else {
            startMs + estimatedDurationMs
        }
        cues.add(SubtitleCue(startMs = startMs, endMs = endMs, text = text))
    }
    return cues
}

private fun decodeSubtitleSamplePayload(bytes: ByteArray, mime: String): String {
    // Handle MP4 tx3g / 3gpp 2-byte big-endian length prefix if present
    val rawText = if (mime.contains("3gpp", ignoreCase = true) || mime.contains("tx3g", ignoreCase = true)) {
        if (bytes.size >= 2) {
            val len = ((bytes[0].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)
            if (len in 1..(bytes.size - 2)) {
                String(bytes, 2, len, StandardCharsets.UTF_8)
            } else {
                String(bytes, StandardCharsets.UTF_8)
            }
        } else {
            ""
        }
    } else {
        String(bytes, StandardCharsets.UTF_8)
    }

    var text = rawText.replace("\u0000", "").replace("\r\n", "\n").trim()
    if (text.isEmpty()) return ""

    // Matroska S_TEXT/ASS & S_TEXT/SSA samples format:
    // ReadOrder,Layer,Style,Name,MarginL,MarginR,MarginV,Effect,Text (8 commas before dialogue text)
    if (mime.contains("ass", ignoreCase = true) || mime.contains("ssa", ignoreCase = true) || text.startsWith("Dialogue:", ignoreCase = true)) {
        val commaCountTarget = if (text.startsWith("Dialogue:", ignoreCase = true)) 9 else 8
        var commasFound = 0
        var cutIdx = -1
        for (idx in text.indices) {
            if (text[idx] == ',') {
                commasFound++
                if (commasFound == commaCountTarget) {
                    cutIdx = idx + 1
                    break
                }
            }
        }
        if (cutIdx in 1 until text.length) {
            text = text.substring(cutIdx)
        }
    }

    return text
        .replace(Regex("\\{[^}]*\\}"), "")
        .replace(Regex("<[^>]*>"), "")
        .replace("\\N", "\n")
        .replace("\\n", "\n")
        .replace("\\h", " ")
        .lines()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString("\n")
        .trim()
}

private fun formatDurationMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hrs = totalSec / 3600L
    val mins = (totalSec % 3600L) / 60L
    val secs = totalSec % 60L
    return if (hrs > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}

private fun parseSrtOrVttFile(file: File): List<SubtitleCue> {
    val cues = mutableListOf<SubtitleCue>()
    try {
        val content = file.readText()
        if (file.extension.equals("ass", ignoreCase = true) || file.extension.equals("ssa", ignoreCase = true)) {
            content.lines().forEach { line ->
                if (line.startsWith("Dialogue:", ignoreCase = true)) {
                    val parts = line.substringAfter(":").split(",", limit = 10)
                    if (parts.size >= 10) {
                        val start = parseTimestampToMs(parts[1].trim())
                        val end = parseTimestampToMs(parts[2].trim())
                        val cleaned = parts[9]
                            .replace(Regex("\\{[^}]*\\}"), "")
                            .replace("\\N", "\n")
                            .replace("\\n", "\n")
                            .trim()
                        if (cleaned.isNotEmpty()) {
                            cues.add(SubtitleCue(start, end, cleaned))
                        }
                    }
                }
            }
            if (cues.isNotEmpty()) return cues
        }

        val blocks = content.replace("\r\n", "\n").split("\n\n")
        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val timeLineIndex = lines.indexOfFirst { it.contains("-->") }
            if (timeLineIndex >= 0 && timeLineIndex + 1 < lines.size) {
                val timeLine = lines[timeLineIndex]
                val parts = timeLine.split("-->")
                if (parts.size == 2) {
                    val start = parseTimestampToMs(parts[0].trim())
                    val end = parseTimestampToMs(parts[1].trim().substringBefore(" "))
                    val text = lines.subList(timeLineIndex + 1, lines.size)
                        .joinToString("\n")
                        .replace(Regex("<[^>]*>"), "")
                        .trim()
                    if (text.isNotEmpty()) {
                        cues.add(SubtitleCue(start, end, text))
                    }
                }
            }
        }
    } catch (_: Exception) {}
    return cues
}

private fun parseTimestampToMs(raw: String): Long {
    return try {
        val clean = raw.replace(",", ".")
        val segments = clean.split(":")
        when (segments.size) {
            3 -> {
                val h = segments[0].toLong()
                val m = segments[1].toLong()
                val sParts = segments[2].split(".")
                val s = sParts[0].toLong()
                val ms = sParts.getOrNull(1)?.padEnd(3, '0')?.take(3)?.toLong() ?: 0L
                (h * 3600_000L) + (m * 60_000L) + (s * 1000L) + ms
            }
            2 -> {
                val m = segments[0].toLong()
                val sParts = segments[1].split(".")
                val s = sParts[0].toLong()
                val ms = sParts.getOrNull(1)?.padEnd(3, '0')?.take(3)?.toLong() ?: 0L
                (m * 60_000L) + (s * 1000L) + ms
            }
            else -> 0L
        }
    } catch (_: Exception) {
        0L
    }
}
