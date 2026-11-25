package com.example.ui.tv

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.Html
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.BuildConfig
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VioletSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.PI
import kotlin.math.sin

private const val RNA_UPDATE_APK_URL = "https://edvanced.ct.ws/update/rna.apk"
private const val RNA_UPDATE_DESC_URL = "https://edvanced.ct.ws/update/description.html"
private const val BROWSER_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

@Volatile
internal var cachedCtWsTestCookie: String? = null

/**
 * Solves InfinityFree / ByetHost AES anti-bot challenge:
 * Extracts a (key), b (IV), c (ciphertext) from toNumbers("..."), decrypts via AES-128-CBC with NoPadding,
 * and sets the __test cookie so real files (such as rna.apk and description.html) can be downloaded.
 */
internal fun solveInfinityFreeChallenge(html: String): String? {
    return try {
        val pattern = Pattern.compile("""toNumbers\(["']([a-fA-F0-9]{32})["']\)""")
        val matcher = pattern.matcher(html)
        val hexStrings = mutableListOf<String>()
        while (matcher.find()) {
            val h = matcher.group(1)
            if (!h.isNullOrBlank()) hexStrings.add(h)
        }
        if (hexStrings.size < 3) return null
        val keyBytes = hexToByteArray(hexStrings[0])
        val ivBytes = hexToByteArray(hexStrings[1])
        val cipherBytes = hexToByteArray(hexStrings[2])

        val keySpec = SecretKeySpec(keyBytes, "AES")
        val ivSpec = IvParameterSpec(ivBytes)
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val decrypted = cipher.doFinal(cipherBytes)
        val cookieHex = byteArrayToHex(decrypted)
        cachedCtWsTestCookie = cookieHex
        cookieHex
    } catch (_: Exception) {
        null
    }
}

private fun hexToByteArray(hex: String): ByteArray {
    val len = hex.length
    val data = ByteArray(len / 2)
    var i = 0
    while (i < len) {
        data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
        i += 2
    }
    return data
}

private fun byteArrayToHex(bytes: ByteArray): String {
    val sb = StringBuilder(bytes.size * 2)
    for (b in bytes) {
        sb.append(String.format(Locale.US, "%02x", b.toInt() and 0xFF))
    }
    return sb.toString()
}

internal fun ensureInfinityFreeBypass(targetUrl: String = RNA_UPDATE_APK_URL) {
    if (!cachedCtWsTestCookie.isNullOrBlank()) return
    try {
        val conn = openResilientConnection(targetUrl, connectTimeoutMs = 7000, readTimeoutMs = 9000, forceCookie = null)
        val code = conn.responseCode
        if (code == HttpURLConnection.HTTP_OK) {
            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            if (raw.contains("toNumbers") && (raw.contains("slowAES") || raw.contains("aes.js"))) {
                solveInfinityFreeChallenge(raw)
            }
        } else {
            conn.disconnect()
        }
    } catch (_: Exception) {
    }
}

data class StoreRemoteAppItem(
    val index: Int,
    val name: String,
    val logoUrl: String,
    val descriptionUrl: String,
    val fallbackDescriptionUrl: String,
    val apkUrl: String,
    val fallbackApkUrl: String? = null,
    val initialDescription: String = ""
)

sealed interface ApkDownloadStatus {
    data object Idle : ApkDownloadStatus
    data class Downloading(
        val downloadedMb: Float,
        val remainingMb: Float,
        val totalMb: Float,
        val progressFraction: Float
    ) : ApkDownloadStatus
    data class Completed(
        val apkFile: File,
        val downloadedMb: Float
    ) : ApkDownloadStatus
    data class Error(val message: String) : ApkDownloadStatus
}

private enum class UpdatesDrawerScreen {
    RNA_UPDATER,
    MORE_APPS_LIST,
    MORE_APP_DETAIL
}

/**
 * Checks whether the device currently has an active internet connection.
 */
internal fun hasActiveInternetConnection(context: Context): Boolean {
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (_: Exception) {
        false
    }
}

/**
 * Opens an HTTP/HTTPS connection following cross-protocol redirects, setting a browser User-Agent,
 * and automatically injecting the InfinityFree anti-bot __test cookie when communicating with edvanced.ct.ws.
 */
internal fun openResilientConnection(
    urlString: String,
    connectTimeoutMs: Int = 10000,
    readTimeoutMs: Int = 15000,
    forceCookie: String? = null
): HttpURLConnection {
    var currentUrl = urlString
    var redirects = 0
    val activeCookie = forceCookie ?: cachedCtWsTestCookie
    while (redirects < 6) {
        val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            setRequestProperty("User-Agent", BROWSER_USER_AGENT)
            setRequestProperty("Accept", "*/*")
            if (!activeCookie.isNullOrBlank()) {
                setRequestProperty("Cookie", "__test=$activeCookie")
            }
        }
        val code = conn.responseCode
        if (code in 300..399) {
            val location = conn.getHeaderField("Location")
            conn.disconnect()
            if (!location.isNullOrBlank()) {
                currentUrl = URL(URL(currentUrl), location).toString()
                redirects++
                continue
            }
        }
        return conn
    }
    return (URL(currentUrl).openConnection() as HttpURLConnection).apply {
        connectTimeout = connectTimeoutMs
        readTimeout = readTimeoutMs
        setRequestProperty("User-Agent", BROWSER_USER_AGENT)
        setRequestProperty("Accept", "*/*")
        if (!activeCookie.isNullOrBlank()) {
            setRequestProperty("Cookie", "__test=$activeCookie")
        }
    }
}

/**
 * Fetches raw text from an HTML/text endpoint, handling the InfinityFree anti-bot challenge
 * and cleaning out wrapper tags.
 */
internal suspend fun fetchRemoteRawText(url: String, fallbackUrl: String? = null): String? =
    withContext(Dispatchers.IO) {
        ensureInfinityFreeBypass(url)
        val candidates = listOfNotNull(
            url,
            url.replaceFirst("https://", "http://"),
            fallbackUrl,
            fallbackUrl?.replaceFirst("https://", "http://")
        ).distinct()

        for (candidate in candidates) {
            try {
                var conn = openResilientConnection(candidate, connectTimeoutMs = 8000, readTimeoutMs = 10000)
                var code = conn.responseCode
                var raw = if (code == HttpURLConnection.HTTP_OK) {
                    conn.inputStream.bufferedReader().use { it.readText() }.trim()
                } else null
                conn.disconnect()

                if (raw != null && raw.contains("toNumbers") && (raw.contains("slowAES") || raw.contains("aes.js"))) {
                    val solved = solveInfinityFreeChallenge(raw)
                    if (solved != null) {
                        conn = openResilientConnection(candidate, connectTimeoutMs = 8000, readTimeoutMs = 10000)
                        code = conn.responseCode
                        raw = if (code == HttpURLConnection.HTTP_OK) {
                            conn.inputStream.bufferedReader().use { it.readText() }.trim()
                        } else null
                        conn.disconnect()
                    }
                }

                val finalHost = conn.url?.host?.lowercase(Locale.US).orEmpty()
                if (code == HttpURLConnection.HTTP_OK &&
                    (finalHost.contains("edvanced.ct.ws") || finalHost.isEmpty()) &&
                    raw != null && isValidServerTextPayload(raw)
                ) {
                    return@withContext cleanRawDescriptionText(raw)
                }
            } catch (_: Exception) {
            }
        }
        null
    }

private fun isValidServerTextPayload(raw: String): Boolean {
    if (raw.isBlank()) return false
    val lower = raw.lowercase(Locale.US)
    if (lower.contains("404 not found") ||
        lower.contains("<title>404") ||
        lower.contains("error 404") ||
        lower.contains("page not found") ||
        lower.contains("/aes.js") ||
        lower.contains("slowaes.decrypt") ||
        lower.contains("infinityfree.net/errors")
    ) {
        return false
    }
    return true
}

private fun cleanRawDescriptionText(raw: String): String {
    val trimmed = raw.trim()
    return if (trimmed.contains("<") && trimmed.contains(">")) {
        val parsed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(trimmed, Html.FROM_HTML_MODE_COMPACT).toString()
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(trimmed).toString()
        }
        parsed.trim().ifBlank { trimmed }
    } else {
        trimmed
    }
}

/**
 * Checks whether an image/logo URL exists on the server.
 */
private fun checkRemoteAssetExists(url: String): Boolean {
    val candidates = listOf(url, url.replaceFirst("https://", "http://")).distinct()
    for (candidate in candidates) {
        try {
            val conn = openResilientConnection(candidate, connectTimeoutMs = 4500, readTimeoutMs = 4500)
            val code = conn.responseCode
            val contentType = conn.contentType?.lowercase(Locale.US).orEmpty()
            val finalHost = conn.url?.host?.lowercase(Locale.US).orEmpty()
            conn.disconnect()
            if (code == HttpURLConnection.HTTP_OK &&
                finalHost.contains("edvanced.ct.ws") &&
                !contentType.contains("text/html")
            ) {
                return true
            }
        } catch (_: Exception) {
        }
    }
    return false
}

/**
 * Strictly verifies whether a real APK file exists on the remote server by checking HTTP 200,
 * rejecting HTML/text challenge or 404 pages, and verifying the 4-byte ZIP/APK magic header (PK\x03\x04).
 */
internal fun checkRemoteApkExists(url: String, fallbackUrl: String? = null): Boolean {
    ensureInfinityFreeBypass(url)
    val candidates = listOfNotNull(
        url,
        url.replaceFirst("https://", "http://"),
        fallbackUrl,
        fallbackUrl?.replaceFirst("https://", "http://")
    ).distinct()
    for (candidate in candidates) {
        try {
            var conn = openResilientConnection(candidate, connectTimeoutMs = 8000, readTimeoutMs = 10000)
            var code = conn.responseCode
            var contentType = conn.contentType?.lowercase(Locale.US).orEmpty()

            if (code == HttpURLConnection.HTTP_OK && contentType.contains("text/html")) {
                val html = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                if (html.contains("toNumbers") && (html.contains("slowAES") || html.contains("aes.js"))) {
                    val solved = solveInfinityFreeChallenge(html)
                    if (solved != null) {
                        conn = openResilientConnection(candidate, connectTimeoutMs = 8000, readTimeoutMs = 10000)
                        code = conn.responseCode
                        contentType = conn.contentType?.lowercase(Locale.US).orEmpty()
                    }
                }
            }

            val finalHost = conn.url?.host?.lowercase(Locale.US).orEmpty()
            if (code == HttpURLConnection.HTTP_OK &&
                (finalHost.contains("edvanced.ct.ws") || finalHost.isEmpty()) &&
                !contentType.contains("text/html")
            ) {
                val magic = ByteArray(4)
                val readBytes = conn.inputStream.use { stream ->
                    var offset = 0
                    while (offset < 4) {
                        val r = stream.read(magic, offset, 4 - offset)
                        if (r <= 0) break
                        offset += r
                    }
                    offset
                }
                conn.disconnect()
                if (readBytes == 4 &&
                    magic[0] == 0x50.toByte() &&
                    magic[1] == 0x4B.toByte() &&
                    magic[2] == 0x03.toByte() &&
                    magic[3] == 0x04.toByte()
                ) {
                    return true
                }
            } else {
                conn.disconnect()
            }
        } catch (_: Exception) {
        }
    }
    return false
}

/**
 * Resolves the target file in the device's Downloads folder (`Environment.DIRECTORY_DOWNLOADS`),
 * falling back to app-specific external Downloads if direct write is restricted by OS policy.
 */
internal fun resolveDeviceDownloadsFile(context: Context, fileName: String): File {
    return try {
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!publicDownloads.exists()) {
            publicDownloads.mkdirs()
        }
        val candidate = File(publicDownloads, fileName)
        // Test if writable or creatable
        if (publicDownloads.canWrite() || (!candidate.exists() && runCatching { candidate.createNewFile(); candidate.delete() }.getOrDefault(false))) {
            candidate
        } else {
            val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: File(context.filesDir, "Downloads").apply { mkdirs() }
            if (!fallbackDir.exists()) fallbackDir.mkdirs()
            File(fallbackDir, fileName)
        }
    } catch (_: Exception) {
        val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "Downloads").apply { mkdirs() }
        if (!fallbackDir.exists()) fallbackDir.mkdirs()
        File(fallbackDir, fileName)
    }
}

/**
 * Opens the Android system settings screen to allow installing unknown apps for RNA,
 * and/or launches the system package installer for the downloaded APK file.
 */
internal fun openFDroidForAppUpdate(context: Context) {
    val packageName = context.packageName
    val fdroidWebUrl = "https://f-droid.org/packages/$packageName/"
    val candidateIntents = buildList {
        add(
            Intent(Intent.ACTION_VIEW, Uri.parse(fdroidWebUrl)).apply {
                setPackage("org.fdroid.fdroid")
            }
        )
        add(
            Intent(Intent.ACTION_VIEW, Uri.parse(fdroidWebUrl)).apply {
                setPackage("org.fdroid.basic")
            }
        )
        add(Intent(Intent.ACTION_VIEW, Uri.parse("fdroid.app://details?id=$packageName")))
        context.packageManager.getLaunchIntentForPackage("org.fdroid.fdroid")?.let { add(it) }
        context.packageManager.getLaunchIntentForPackage("org.fdroid.basic")?.let { add(it) }
        add(Intent(Intent.ACTION_VIEW, Uri.parse(fdroidWebUrl)))
    }
    for (intent in candidateIntents) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        } catch (_: Exception) {
        }
    }
    Toast.makeText(
        context,
        "Open F-Droid on your TV to check for RNA. TV updates.",
        Toast.LENGTH_LONG
    ).show()
}

internal fun openAllowInstallUnknownAppsSettings(context: Context) {
    if (BuildConfig.IS_FDROID) return
    val intents = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            add(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }
        add(Intent(Settings.ACTION_SECURITY_SETTINGS))
        add(Intent(Settings.ACTION_SETTINGS))
    }
    for (intent in intents) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        } catch (_: Exception) {
        }
    }
    Toast.makeText(context, "Open Settings > Security > Install Unknown Apps", Toast.LENGTH_LONG).show()
}

internal fun promptInstallApkFile(context: Context, apkFile: File) {
    if (BuildConfig.IS_FDROID) return
    if (!apkFile.exists()) {
        Toast.makeText(context, "APK file no longer exists", Toast.LENGTH_SHORT).show()
        return
    }
    // Check if permission to install packages is already granted on Android O+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val canInstall = runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
        if (!canInstall) {
            Toast.makeText(
                context,
                "Please allow permission to install files from Settings",
                Toast.LENGTH_LONG
            ).show()
            openAllowInstallUnknownAppsSettings(context)
        }
    }
    try {
        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    } catch (_: Exception) {
        openAllowInstallUnknownAppsSettings(context)
    }
}

/**
 * Left-Side Android TV Style Sliding Drawer for RNA Updates & "More Apps" Storefront.
 */
@Composable
fun TvUpdatesSideDrawer(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf(UpdatesDrawerScreen.RNA_UPDATER) }

    // ── RNA Updater State ──
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var hasCheckedUpdate by remember { mutableStateOf(false) }
    var updateDescription by remember { mutableStateOf("") }
    var updateCheckError by remember { mutableStateOf<String?>(null) }
    var rnaDownloadStatus by remember { mutableStateOf<ApkDownloadStatus>(ApkDownloadStatus.Idle) }
    var rnaDownloadJob by remember { mutableStateOf<Job?>(null) }

    // ── More Apps (1..20) State ──
    var isLoadingMoreApps by remember { mutableStateOf(false) }
    var isOfflineMoreApps by remember { mutableStateOf(false) }
    var availableStoreApps by remember { mutableStateOf<List<StoreRemoteAppItem>>(emptyList()) }
    var hasLoadedMoreAppsOnce by remember { mutableStateOf(false) }

    // ── Selected Store App Detail State ──
    var selectedStoreApp by remember { mutableStateOf<StoreRemoteAppItem?>(null) }
    var selectedAppDescription by remember { mutableStateOf("") }
    var isLoadingAppDescription by remember { mutableStateOf(false) }
    var storeAppDownloadStatus by remember { mutableStateOf<ApkDownloadStatus>(ApkDownloadStatus.Idle) }
    var storeAppDownloadJob by remember { mutableStateOf<Job?>(null) }

    val checkUpdatesBtnFocus = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (visible) {
            delay(80L)
            checkUpdatesBtnFocus.safeRequestFocus()
            if (!BuildConfig.IS_FDROID && updateDescription.isBlank()) {
                scope.launch {
                    val desc = fetchRemoteRawText(RNA_UPDATE_DESC_URL)
                    if (!desc.isNullOrBlank()) {
                        updateDescription = desc
                    }
                }
            }
        }
    }

    BackHandler(enabled = visible) {
        when (currentScreen) {
            UpdatesDrawerScreen.MORE_APP_DETAIL -> {
                storeAppDownloadJob?.cancel()
                currentScreen = UpdatesDrawerScreen.MORE_APPS_LIST
            }
            UpdatesDrawerScreen.MORE_APPS_LIST -> {
                currentScreen = UpdatesDrawerScreen.RNA_UPDATER
            }
            UpdatesDrawerScreen.RNA_UPDATER -> {
                onDismiss()
            }
        }
    }

    fun startApkDownload(
        primaryUrl: String,
        fallbackUrl: String? = null,
        targetFileName: String,
        missingMessage: String = "No app file found in server",
        onStatusChange: (ApkDownloadStatus) -> Unit
    ): Job {
        if (BuildConfig.IS_FDROID) {
            return scope.launch {}
        }
        return scope.launch {
            if (!hasActiveInternetConnection(context)) {
                onStatusChange(ApkDownloadStatus.Error("No internet connection."))
                return@launch
            }

            onStatusChange(
                ApkDownloadStatus.Downloading(
                    downloadedMb = 0f,
                    remainingMb = 0f,
                    totalMb = 0f,
                    progressFraction = 0.02f
                )
            )

            withContext(Dispatchers.IO) {
                ensureInfinityFreeBypass(primaryUrl)
                val urlsToTry = listOfNotNull(
                    primaryUrl,
                    primaryUrl.replaceFirst("https://", "http://"),
                    fallbackUrl,
                    fallbackUrl?.replaceFirst("https://", "http://")
                ).distinct()

                for (candidateUrl in urlsToTry) {
                    try {
                        var conn = openResilientConnection(candidateUrl, connectTimeoutMs = 12000, readTimeoutMs = 25000)
                        var responseCode = conn.responseCode
                        var contentType = conn.contentType?.lowercase(Locale.US).orEmpty()

                        if (responseCode == HttpURLConnection.HTTP_OK && contentType.contains("text/html")) {
                            val html = conn.inputStream.bufferedReader().use { it.readText() }
                            conn.disconnect()
                            if (html.contains("toNumbers") && (html.contains("slowAES") || html.contains("aes.js"))) {
                                val solved = solveInfinityFreeChallenge(html)
                                if (solved != null) {
                                    conn = openResilientConnection(candidateUrl, connectTimeoutMs = 12000, readTimeoutMs = 25000)
                                    responseCode = conn.responseCode
                                    contentType = conn.contentType?.lowercase(Locale.US).orEmpty()
                                }
                            }
                        }

                        if (responseCode != HttpURLConnection.HTTP_OK || contentType.contains("text/html")) {
                            conn.disconnect()
                            continue
                        }

                        val totalBytes = conn.contentLengthLong
                        val destFile = resolveDeviceDownloadsFile(context, targetFileName)

                        var downloadedBytes = 0L
                        val buffer = ByteArray(32 * 1024)
                        var verifiedMagicHeader = false
                        conn.inputStream.use { input ->
                            // Read first 4 bytes to guarantee it is a real APK/ZIP (PK\x03\x04) before writing
                            val magic = ByteArray(4)
                            var mOff = 0
                            while (mOff < 4) {
                                val r = input.read(magic, mOff, 4 - mOff)
                                if (r <= 0) break
                                mOff += r
                            }
                            if (mOff == 4 &&
                                magic[0] == 0x50.toByte() &&
                                magic[1] == 0x4B.toByte() &&
                                magic[2] == 0x03.toByte() &&
                                magic[3] == 0x04.toByte()
                            ) {
                                verifiedMagicHeader = true
                                FileOutputStream(destFile).use { output ->
                                    output.write(magic, 0, 4)
                                    downloadedBytes += 4
                                    var read: Int
                                    while (input.read(buffer).also { read = it } != -1 && isActive) {
                                        output.write(buffer, 0, read)
                                        downloadedBytes += read

                                        val dlMb = downloadedBytes.toFloat() / (1024f * 1024f)
                                        val totMb = if (totalBytes > 0L) {
                                            totalBytes.toFloat() / (1024f * 1024f)
                                        } else {
                                            (dlMb + 2.5f)
                                        }
                                        val remMb = (totMb - dlMb).coerceAtLeast(0f)
                                        val frac = if (totalBytes > 0L) {
                                            (downloadedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0.02f, 0.99f)
                                        } else {
                                            (dlMb / totMb).coerceIn(0.08f, 0.92f)
                                        }

                                        onStatusChange(
                                            ApkDownloadStatus.Downloading(
                                                downloadedMb = dlMb,
                                                remainingMb = remMb,
                                                totalMb = totMb,
                                                progressFraction = frac
                                            )
                                        )
                                    }
                                    output.flush()
                                }
                            }
                        }
                        conn.disconnect()

                        if (!verifiedMagicHeader) {
                            runCatching { if (destFile.exists()) destFile.delete() }
                            continue
                        }

                        val finalMb = (downloadedBytes.toFloat() / (1024f * 1024f)).coerceAtLeast(0.01f)
                        onStatusChange(
                            ApkDownloadStatus.Completed(
                                apkFile = destFile,
                                downloadedMb = finalMb
                            )
                        )

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Saved ${destFile.name} to Downloads folder (${String.format(Locale.US, "%.1f", finalMb)} MB)",
                                Toast.LENGTH_LONG
                            ).show()
                            promptInstallApkFile(context, destFile)
                        }
                        return@withContext
                    } catch (_: Exception) {
                    }
                }

                onStatusChange(ApkDownloadStatus.Error(missingMessage))
            }
        }
    }

    fun checkAndDownloadRnaUpdate() {
        if (BuildConfig.IS_FDROID) return
        if (isCheckingUpdate || rnaDownloadStatus is ApkDownloadStatus.Downloading) return
        isCheckingUpdate = true
        updateCheckError = null
        scope.launch {
            if (!hasActiveInternetConnection(context)) {
                isCheckingUpdate = false
                updateCheckError = "No internet connection"
                return@launch
            }

            hasCheckedUpdate = true
            isCheckingUpdate = false

            // Concurrently fetch the description from https://edvanced.ct.ws/update/description.html
            val descJob = async(Dispatchers.IO) {
                fetchRemoteRawText(RNA_UPDATE_DESC_URL)
            }

            // Immediately start downloading https://edvanced.ct.ws/update/rna.apk into Downloads folder
            rnaDownloadJob?.cancel()
            rnaDownloadJob = startApkDownload(
                primaryUrl = RNA_UPDATE_APK_URL,
                targetFileName = "rna.apk",
                missingMessage = "No app file found in server",
                onStatusChange = { status ->
                    rnaDownloadStatus = status
                    if (status is ApkDownloadStatus.Error && updateDescription.isBlank()) {
                        updateDescription = "No app file and no description found in server"
                    }
                }
            )

            val desc = descJob.await()
            if (!desc.isNullOrBlank()) {
                updateDescription = desc
            } else if (rnaDownloadStatus is ApkDownloadStatus.Error) {
                updateDescription = "No app file and no description found in server"
            }
        }
    }

    fun loadMoreAppsCatalog() {
        if (isLoadingMoreApps) return
        isLoadingMoreApps = true
        isOfflineMoreApps = false
        scope.launch {
            if (!hasActiveInternetConnection(context)) {
                isOfflineMoreApps = true
                isLoadingMoreApps = false
                hasLoadedMoreAppsOnce = true
                availableStoreApps = emptyList()
                return@launch
            }

            val discovered = withContext(Dispatchers.IO) {
                // Warm up the InfinityFree AES cookie once before probing app slots in parallel
                ensureInfinityFreeBypass(RNA_UPDATE_DESC_URL)
                coroutineScope {
                    (1..20).map { idx ->
                        async {
                            val nameUrl = "https://edvanced.ct.ws/update/app${idx}/app${idx}name.html"
                            val nameUrlSpaced = "https://edvanced.ct.ws/update/app%20${idx}/app${idx}name.html"
                            val logoUrl = "https://edvanced.ct.ws/update/app${idx}/app${idx}logo.png"
                            val descUrlNoSpace = "https://edvanced.ct.ws/update/app${idx}/app${idx}description.html"
                            val descUrlWithSpace = "https://edvanced.ct.ws/update/app%20${idx}/app${idx}description.html"
                            val apkUrl = "https://edvanced.ct.ws/update/app${idx}/app${idx}.apk"
                            val fallbackApkUrl = if (idx == 1) {
                                "https://edvanced.ct.ws/update/app2/app2.apk"
                            } else {
                                "https://edvanced.ct.ws/update/app%20${idx}/app${idx}.apk"
                            }

                            val fetchedName = fetchRemoteRawText(nameUrl, nameUrlSpaced)
                            if (!fetchedName.isNullOrBlank()) {
                                StoreRemoteAppItem(
                                    index = idx,
                                    name = fetchedName.lines().firstOrNull()?.trim()?.take(48) ?: "App $idx",
                                    logoUrl = logoUrl,
                                    descriptionUrl = descUrlNoSpace,
                                    fallbackDescriptionUrl = descUrlWithSpace,
                                    apkUrl = apkUrl,
                                    fallbackApkUrl = fallbackApkUrl
                                )
                            } else if (checkRemoteAssetExists(logoUrl)) {
                                StoreRemoteAppItem(
                                    index = idx,
                                    name = "RNA Partner App $idx",
                                    logoUrl = logoUrl,
                                    descriptionUrl = descUrlNoSpace,
                                    fallbackDescriptionUrl = descUrlWithSpace,
                                    apkUrl = apkUrl,
                                    fallbackApkUrl = fallbackApkUrl
                                )
                            } else {
                                null
                            }
                        }
                    }.awaitAll().filterNotNull().sortedBy { it.index }
                }
            }

            availableStoreApps = discovered
            isLoadingMoreApps = false
            hasLoadedMoreAppsOnce = true
        }
    }

    fun selectStoreApp(item: StoreRemoteAppItem) {
        selectedStoreApp = item
        selectedAppDescription = item.initialDescription
        storeAppDownloadStatus = ApkDownloadStatus.Idle
        currentScreen = UpdatesDrawerScreen.MORE_APP_DETAIL
        isLoadingAppDescription = true
        scope.launch {
            val desc = fetchRemoteRawText(
                url = item.descriptionUrl,
                fallbackUrl = item.fallbackDescriptionUrl
            )
            val apkExists = withContext(Dispatchers.IO) {
                checkRemoteApkExists(item.apkUrl, item.fallbackApkUrl)
            }
            when {
                desc == null && !apkExists -> {
                    selectedAppDescription = "No app file and no description found in server"
                    storeAppDownloadStatus = ApkDownloadStatus.Error("No app file and no description found in server")
                }
                desc == null && apkExists -> {
                    selectedAppDescription = "No description found in server"
                }
                desc != null && !apkExists -> {
                    selectedAppDescription = desc
                    storeAppDownloadStatus = ApkDownloadStatus.Error("No app file found in server")
                }
                else -> {
                    selectedAppDescription = desc!!
                }
            }
            isLoadingAppDescription = false
        }
    }

    Dialog(
        onDismissRequest = {
            when (currentScreen) {
                UpdatesDrawerScreen.MORE_APP_DETAIL -> {
                    storeAppDownloadJob?.cancel()
                    currentScreen = UpdatesDrawerScreen.MORE_APPS_LIST
                }
                UpdatesDrawerScreen.MORE_APPS_LIST -> {
                    currentScreen = UpdatesDrawerScreen.RNA_UPDATER
                }
                UpdatesDrawerScreen.RNA_UPDATER -> {
                    onDismiss()
                }
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x9906080E))
                .onPreviewKeyEvent { keyEvent ->
                    val code = keyEvent.nativeKeyEvent.keyCode
                    // Trap horizontal D-Pad escape so focus never leaks into Favorites/Home behind the drawer
                    if (code == android.view.KeyEvent.KEYCODE_DPAD_LEFT || code == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                        true
                    } else {
                        false
                    }
                }
                .clickable { onDismiss() }
                .testTag("tv_updates_drawer_scrim")
        ) {
            // Left-side Android TV settings-style panel
            Surface(
                shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                color = Color(0xFA121521),
                border = BorderStroke(1.5.dp, Color(0x33FFFFFF)),
                shadowElevation = 28.dp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(460.dp)
                    .fillMaxHeight()
                    .clickable(enabled = false) {}
                    .testTag("tv_updates_left_drawer")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 22.dp)
                ) {
                        // Top Drawer Navigation Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (currentScreen != UpdatesDrawerScreen.RNA_UPDATER) {
                                    IconButton(
                                        onClick = {
                                            when (currentScreen) {
                                                UpdatesDrawerScreen.MORE_APP_DETAIL -> {
                                                    currentScreen = UpdatesDrawerScreen.MORE_APPS_LIST
                                                }
                                                UpdatesDrawerScreen.MORE_APPS_LIST -> {
                                                    currentScreen = UpdatesDrawerScreen.RNA_UPDATER
                                                }
                                                else -> {}
                                            }
                                        },
                                        modifier = Modifier.testTag("tv_updates_back_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back",
                                            tint = CyanPrimary
                                        )
                                    }
                                } else {
                                    Surface(
                                        shape = CircleShape,
                                        color = CyanPrimary.copy(alpha = 0.18f),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.SystemUpdateAlt,
                                                contentDescription = null,
                                                tint = CyanPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                Column {
                                    Text(
                                        text = when (currentScreen) {
                                            UpdatesDrawerScreen.RNA_UPDATER -> "System Updates"
                                            UpdatesDrawerScreen.MORE_APPS_LIST -> "More Apps"
                                            UpdatesDrawerScreen.MORE_APP_DETAIL -> selectedStoreApp?.name ?: "App Details"
                                        },
                                        color = Color.White,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("tv_updates_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Updates Panel",
                                    tint = Color(0xFFE4E1E9)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0x26FFFFFF))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Animated Screen Content Inside Left Drawer
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            label = "UpdatesDrawerScreenSwitch"
                        ) { screen ->
                            when (screen) {
                                UpdatesDrawerScreen.RNA_UPDATER -> {
                                    RnaMainUpdaterContent(
                                        context = context,
                                        checkUpdatesBtnFocus = checkUpdatesBtnFocus,
                                        isCheckingUpdate = isCheckingUpdate,
                                        hasCheckedUpdate = hasCheckedUpdate,
                                        updateDescription = updateDescription,
                                        updateCheckError = updateCheckError,
                                        downloadStatus = rnaDownloadStatus,
                                        onCheckForUpdates = { checkAndDownloadRnaUpdate() },
                                        onDeleteDownloadedApk = { file ->
                                            val deleted = runCatching { file.delete() }.getOrDefault(false)
                                            if (deleted || !file.exists()) {
                                                rnaDownloadStatus = ApkDownloadStatus.Idle
                                                Toast.makeText(context, "Deleted ${file.name} from Downloads", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onOpenMoreApps = {
                                            currentScreen = UpdatesDrawerScreen.MORE_APPS_LIST
                                            if (!hasLoadedMoreAppsOnce) {
                                                loadMoreAppsCatalog()
                                            }
                                        }
                                    )
                                }

                                UpdatesDrawerScreen.MORE_APPS_LIST -> {
                                    MoreAppsListView(
                                        isLoading = isLoadingMoreApps,
                                        isOffline = isOfflineMoreApps,
                                        apps = availableStoreApps,
                                        onRefresh = { loadMoreAppsCatalog() },
                                        onSelectApp = { selectStoreApp(it) }
                                    )
                                }

                                UpdatesDrawerScreen.MORE_APP_DETAIL -> {
                                    val app = selectedStoreApp
                                    if (app != null) {
                                        MoreAppDetailView(
                                            context = context,
                                            app = app,
                                            description = selectedAppDescription,
                                            isLoadingDescription = isLoadingAppDescription,
                                            downloadStatus = storeAppDownloadStatus,
                                            onDownloadClick = {
                                                storeAppDownloadJob?.cancel()
                                                val missingMsg = if (selectedAppDescription.startsWith("No ")) {
                                                    "No app file and no description found in server"
                                                } else {
                                                    "No app file found in server"
                                                }
                                                storeAppDownloadJob = startApkDownload(
                                                    primaryUrl = app.apkUrl,
                                                    fallbackUrl = app.fallbackApkUrl,
                                                    targetFileName = "app${app.index}.apk",
                                                    missingMessage = missingMsg,
                                                    onStatusChange = { storeAppDownloadStatus = it }
                                                )
                                            },
                                            onDeleteDownloadedApk = { file ->
                                                val deleted = runCatching { file.delete() }.getOrDefault(false)
                                                if (deleted || !file.exists()) {
                                                    storeAppDownloadStatus = ApkDownloadStatus.Idle
                                                    Toast.makeText(context, "Deleted ${file.name} from Downloads", Toast.LENGTH_SHORT).show()
                                                }
                                            }
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

@Composable
private fun RnaMainUpdaterContent(
    context: Context,
    checkUpdatesBtnFocus: FocusRequester,
    isCheckingUpdate: Boolean,
    hasCheckedUpdate: Boolean,
    updateDescription: String,
    updateCheckError: String?,
    downloadStatus: ApkDownloadStatus,
    onCheckForUpdates: () -> Unit,
    onDeleteDownloadedApk: (File) -> Unit,
    onOpenMoreApps: () -> Unit
) {
    val appIconBitmap = remember(context) {
        runCatching {
            context.packageManager.getApplicationIcon(context.packageName)
                .toBitmap(144, 144)
                .asImageBitmap()
        }.getOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. RNA Identity Card (Logo + Name + Version)
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF1A1E2E),
            border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF252A40),
                    border = BorderStroke(1.5.dp, CyanPrimary),
                    modifier = Modifier.size(64.dp)
                ) {
                    if (appIconBitmap != null) {
                        Image(
                            bitmap = appIconBitmap,
                            contentDescription = "RNA Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "RNA.",
                                color = CyanPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RNA. TV",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (BuildConfig.IS_FDROID) {
                            "Version ${BuildConfig.VERSION_NAME} • F-Droid Edition"
                        } else {
                            "Next-Gen Hybrid Android TV Container"
                        },
                        color = CyanPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (BuildConfig.IS_FDROID) {
                            "Updates managed by F-Droid"
                        } else {
                            "Update server: edvanced.ct.ws"
                        },
                        color = Color(0xFF9092A4),
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (BuildConfig.IS_FDROID) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF191D2C),
                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tv_fdroid_updates_info_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "UPDATES ARE MANAGED BY F-DROID",
                            color = EmeraldSuccess,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Text(
                        text = "This F-Droid edition disables in-app APK downloads and self-updates. Use the F-Droid client on your device to check for and install the latest RNA. TV updates.",
                        color = Color(0xFFE4E6F0),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }

            var openFdroidFocused by remember { mutableStateOf(false) }
            val openFdroidScale by animateFloatAsState(
                targetValue = if (openFdroidFocused) 1.03f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "OpenFdroidScale"
            )
            Button(
                onClick = { openFDroidForAppUpdate(context) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (openFdroidFocused) Color.White else CyanPrimary,
                    contentColor = Color(0xFF10131C)
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .scale(openFdroidScale)
                    .focusRequester(checkUpdatesBtnFocus)
                    .onFocusChanged { openFdroidFocused = it.isFocused }
                    .testTag("tv_open_fdroid_updates_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Open F-Droid to Update",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
            return@Column
        }

        // 2. Check for Updates Button
        var checkFocused by remember { mutableStateOf(false) }
        val checkScale by animateFloatAsState(
            targetValue = if (checkFocused) 1.03f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "CheckUpdatesScale"
        )
        Button(
            onClick = onCheckForUpdates,
            enabled = !isCheckingUpdate && downloadStatus !is ApkDownloadStatus.Downloading,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (checkFocused) Color.White else CyanPrimary,
                contentColor = Color(0xFF10131C)
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .scale(checkScale)
                .focusRequester(checkUpdatesBtnFocus)
                .onFocusChanged { checkFocused = it.isFocused }
                .testTag("tv_check_updates_button")
        ) {
            if (isCheckingUpdate || downloadStatus is ApkDownloadStatus.Downloading) {
                CircularProgressIndicator(
                    color = Color(0xFF10131C),
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Connecting & Downloading...", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            } else {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (downloadStatus is ApkDownloadStatus.Completed) "Download rna.apk Again" else "Download Update (rna.apk)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }

        if (updateCheckError != null) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF2D1C24),
                border = BorderStroke(1.dp, Color(0xFFFF8A80)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = updateCheckError,
                    color = Color(0xFFFF8A80),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        // 3. Pulled Description Card from https://edvanced.ct.ws/update/description.html
        if (hasCheckedUpdate || updateDescription.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF191D2C),
                border = BorderStroke(1.dp, Color(0x2EFFFFFF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tv_update_description_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "WHAT'S NEW IN THIS UPDATE",
                        color = CyanPrimary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = updateDescription,
                        color = Color(0xFFE4E6F0),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }

        // 4. Material 3 Expressive APK Download Progress & Install / Delete Controls
        ExpressiveApkDownloadCard(
            context = context,
            title = "RNA. Update Package (rna.apk)",
            downloadStatus = downloadStatus,
            onRetry = onCheckForUpdates,
            onDeleteFile = onDeleteDownloadedApk
        )

        // 5. "More Apps" Button right under the APK & Description
        var moreAppsFocused by remember { mutableStateOf(false) }
        val moreAppsScale by animateFloatAsState(
            targetValue = if (moreAppsFocused) 1.03f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "MoreAppsBtnScale"
        )
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (moreAppsFocused) Color.White else Color(0xFF21263A),
            border = BorderStroke(
                width = if (moreAppsFocused) 2.dp else 1.5.dp,
                color = if (moreAppsFocused) CyanPrimary else VioletSecondary.copy(alpha = 0.7f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .scale(moreAppsScale)
                .onFocusChanged { moreAppsFocused = it.isFocused }
                .clickable { onOpenMoreApps() }
                .focusable()
                .testTag("tv_more_apps_button")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (moreAppsFocused) Color(0xFF141724) else VioletSecondary.copy(alpha = 0.2f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = if (moreAppsFocused) CyanPrimary else VioletSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "More Apps",
                            color = if (moreAppsFocused) Color(0xFF11131A) else Color.White,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Browse & download companion TV apps",
                            color = if (moreAppsFocused) Color(0xFF424658) else Color(0xFFA2A4B8),
                            fontSize = 11.5.sp
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (moreAppsFocused) Color(0xFF11131A) else Color.White
                )
            }
        }
    }
}

/**
 * Material 3 Expressive Style Download & Installation Card:
 * Shows animated expressive wave progress, MBs downloaded, MBs left,
 * permission prompt to open Settings, Install button, and Delete APK file option.
 */
@Composable
private fun ExpressiveApkDownloadCard(
    context: Context,
    title: String,
    downloadStatus: ApkDownloadStatus,
    onRetry: () -> Unit,
    onDeleteFile: (File) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ExpressiveWave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    when (downloadStatus) {
        is ApkDownloadStatus.Idle -> {}

        is ApkDownloadStatus.Downloading -> {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1B2135),
                border = BorderStroke(1.5.dp, CyanPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tv_expressive_download_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Expressive pulsing badge
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CyanPrimary.copy(alpha = 0.22f),
                            modifier = Modifier
                                .size(42.dp)
                                .scale(pulseScale)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Updating • Saving to Downloads",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = title,
                                color = Color(0xFFAAB0C6),
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = CyanPrimary
                        ) {
                            Text(
                                text = "${(downloadStatus.progressFraction * 100).toInt()}%",
                                color = Color(0xFF10131C),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Material 3 Expressive Wavy Progress Canvas
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        val centerY = h / 2f
                        val progX = (w * downloadStatus.progressFraction.coerceIn(0.04f, 1f))

                        // Unfilled track
                        drawRoundRect(
                            color = Color(0xFF2B324B),
                            topLeft = Offset(0f, centerY - 4.dp.toPx()),
                            size = Size(w, 8.dp.toPx()),
                            cornerRadius = CornerRadius(12f, 12f)
                        )

                        // Expressive sinusoidal wave path for active progress
                        val wavePath = Path()
                        val amplitude = 4.5.dp.toPx()
                        val wavelength = 36.dp.toPx().coerceAtLeast(16f)
                        var x = 0f
                        wavePath.moveTo(0f, centerY)
                        while (x <= progX) {
                            val y = centerY + amplitude * sin((x / wavelength) * (2f * PI.toFloat()) + wavePhase)
                            wavePath.lineTo(x, y)
                            x += 4f
                        }
                        drawPath(
                            path = wavePath,
                            brush = Brush.horizontalGradient(
                                colors = listOf(CyanPrimary, VioletSecondary, EmeraldSuccess)
                            ),
                            style = Stroke(width = 5.5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // MBs Downloaded & MBs Left Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.2f MB downloaded", downloadStatus.downloadedMb),
                            color = CyanPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale.US, "%.2f MB left", downloadStatus.remainingMb),
                            color = EmeraldSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        is ApkDownloadStatus.Completed -> {
            val installBtnFocusRequester = remember { FocusRequester() }
            var installFocused by remember { mutableStateOf(false) }
            var allowSettingsFocused by remember { mutableStateOf(false) }
            var deleteApkFocused by remember { mutableStateOf(false) }

            LaunchedEffect(downloadStatus.apkFile.absolutePath) {
                try {
                    installBtnFocusRequester.requestFocus()
                } catch (_: Exception) {
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF172724),
                border = BorderStroke(1.5.dp, EmeraldSuccess),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tv_apk_download_completed_card")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(26.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Downloaded to Downloads (${String.format(Locale.US, "%.2f MB", downloadStatus.downloadedMb)})",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp
                            )
                            Text(
                                text = "Allow permission to install files from Settings, then install:",
                                color = Color(0xFFB5C7C1),
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    Button(
                        onClick = { promptInstallApkFile(context, downloadStatus.apkFile) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (installFocused) Color.White else EmeraldSuccess,
                            contentColor = Color(0xFF0B2218)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(installBtnFocusRequester)
                            .onFocusChanged { installFocused = it.isFocused }
                            .focusable()
                            .testTag("tv_install_downloaded_apk_btn")
                    ) {
                        Icon(imageVector = Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Install APK Now", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { openAllowInstallUnknownAppsSettings(context) },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(if (allowSettingsFocused) 2.dp else 1.dp, if (allowSettingsFocused) Color.White else CyanPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (allowSettingsFocused) CyanPrimary.copy(alpha = 0.25f) else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { allowSettingsFocused = it.isFocused }
                                .focusable()
                                .testTag("tv_allow_install_permission_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Allow in Settings", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onDeleteFile(downloadStatus.apkFile) },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(if (deleteApkFocused) 2.dp else 1.dp, if (deleteApkFocused) Color.White else Color(0xFFFF8A80)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (deleteApkFocused) Color(0xFFFF8A80).copy(alpha = 0.25f) else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .onFocusChanged { deleteApkFocused = it.isFocused }
                                .focusable()
                                .testTag("tv_delete_downloaded_apk_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = Color(0xFFFF8A80),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete APK File", color = Color(0xFFFF8A80), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        is ApkDownloadStatus.Error -> {
            var retryFocused by remember { mutableStateOf(false) }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF2B1B22),
                border = BorderStroke(1.dp, Color(0xFFFF8A80)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = downloadStatus.message,
                        color = Color(0xFFFF8A80),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedButton(
                        onClick = onRetry,
                        border = BorderStroke(if (retryFocused) 2.dp else 1.dp, if (retryFocused) CyanPrimary else Color.White),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (retryFocused) Color.White.copy(alpha = 0.16f) else Color.Transparent
                        ),
                        modifier = Modifier
                            .onFocusChanged { retryFocused = it.isFocused }
                            .focusable()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Download", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreAppsListView(
    isLoading: Boolean,
    isOffline: Boolean,
    apps: List<StoreRemoteAppItem>,
    onRefresh: () -> Unit,
    onSelectApp: (StoreRemoteAppItem) -> Unit
) {
    val context = LocalContext.current
    val firstAppFocusRequester = remember { FocusRequester() }
    val emptyActionFocusRequester = remember { FocusRequester() }
    val loadingFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isLoading, isOffline, apps.size) {
        try {
            when {
                isLoading -> loadingFocusRequester.requestFocus()
                isOffline || apps.isEmpty() -> emptyActionFocusRequester.requestFocus()
                apps.isNotEmpty() -> firstAppFocusRequester.requestFocus()
            }
        } catch (_: Exception) {
        }
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(loadingFocusRequester)
                    .focusable(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(
                        color = CyanPrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Checking available apps on server...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        isOffline -> {
            var retryFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = Color(0xFFFF8A80),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "No internet connection",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Please check your TV Wi-Fi or Ethernet connection and try again.",
                        color = Color(0xFF9E9FAF),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onRefresh,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (retryFocused) Color.White else CyanPrimary,
                            contentColor = Color(0xFF10131C)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .focusRequester(emptyActionFocusRequester)
                            .onFocusChanged { retryFocused = it.isFocused }
                            .focusable()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        apps.isEmpty() -> {
            var refreshFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = CyanPrimary,
                        modifier = Modifier.size(46.dp)
                    )
                    Text(
                        text = "No apps available",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "There are currently no additional apps published on the update server.",
                        color = Color(0xFF9E9FAF),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    OutlinedButton(
                        onClick = onRefresh,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(if (refreshFocused) 2.dp else 1.dp, if (refreshFocused) CyanPrimary else Color.White),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (refreshFocused) CyanPrimary.copy(alpha = 0.22f) else Color.Transparent
                        ),
                        modifier = Modifier
                            .focusRequester(emptyActionFocusRequester)
                            .onFocusChanged { refreshFocused = it.isFocused }
                            .focusable()
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Refresh", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("tv_more_apps_list"),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(apps, key = { _, it -> it.index }) { idx, appItem ->
                    var itemFocused by remember { mutableStateOf(false) }
                    val scale by animateFloatAsState(
                        targetValue = if (itemFocused) 1.03f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "MoreAppRowScale"
                    )

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (itemFocused) Color.White else Color(0xFF1B1F30),
                        border = BorderStroke(
                            width = if (itemFocused) 2.5.dp else 1.dp,
                            color = if (itemFocused) CyanPrimary else Color(0x28FFFFFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .then(
                                if (idx == 0) Modifier.focusRequester(firstAppFocusRequester) else Modifier
                            )
                            .onFocusChanged { itemFocused = it.isFocused }
                            .clickable { onSelectApp(appItem) }
                            .focusable()
                            .testTag("tv_more_app_item_${appItem.index}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF262B40),
                                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                                modifier = Modifier.size(50.dp)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(appItem.logoUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = appItem.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = appItem.name,
                                    color = if (itemFocused) Color(0xFF11131A) else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Press OK to view & download",
                                    color = if (itemFocused) Color(0xFF424658) else Color(0xFF9E9FAF),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = if (itemFocused) Color(0xFF11131A) else CyanPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoreAppDetailView(
    context: Context,
    app: StoreRemoteAppItem,
    description: String,
    isLoadingDescription: Boolean,
    downloadStatus: ApkDownloadStatus,
    onDownloadClick: () -> Unit,
    onDeleteDownloadedApk: (File) -> Unit
) {
    val downloadBtnFocusRequester = remember { FocusRequester() }

    LaunchedEffect(app.index) {
        try {
            downloadBtnFocusRequester.requestFocus()
        } catch (_: Exception) {
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Logo + Name Header Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF1B1F30),
            border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF262B40),
                    border = BorderStroke(1.5.dp, CyanPrimary),
                    modifier = Modifier.size(68.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(app.logoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = app.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "RNA. Verified Application Package",
                        color = CyanPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Description Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF181C2B),
            border = BorderStroke(1.dp, Color(0x28FFFFFF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DESCRIPTION",
                    color = CyanPrimary,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (isLoadingDescription) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = CyanPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Loading description...",
                            color = Color(0xFFB5B8C8),
                            fontSize = 12.5.sp
                        )
                    }
                } else {
                    Text(
                        text = description.ifBlank { "No description provided." },
                        color = Color(0xFFE4E6F0),
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
        }

        // Download App Button
        var dlFocused by remember { mutableStateOf(false) }
        val dlScale by animateFloatAsState(
            targetValue = if (dlFocused) 1.03f else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "MoreAppDlBtnScale"
        )
        Button(
            onClick = onDownloadClick,
            enabled = downloadStatus !is ApkDownloadStatus.Downloading,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (dlFocused) Color.White else CyanPrimary,
                contentColor = Color(0xFF10131C)
            ),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .scale(dlScale)
                .focusRequester(downloadBtnFocusRequester)
                .onFocusChanged { dlFocused = it.isFocused }
                .focusable()
                .testTag("tv_more_app_download_btn")
        ) {
            Icon(
                imageVector = Icons.Default.CloudDownload,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (downloadStatus is ApkDownloadStatus.Completed) "Download APK Again" else "Download ${app.name}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }

        // Expressive Progress, Install Prompt & Delete File Button
        ExpressiveApkDownloadCard(
            context = context,
            title = "${app.name} (app${app.index}.apk)",
            downloadStatus = downloadStatus,
            onRetry = onDownloadClick,
            onDeleteFile = onDeleteDownloadedApk
        )
    }
}
