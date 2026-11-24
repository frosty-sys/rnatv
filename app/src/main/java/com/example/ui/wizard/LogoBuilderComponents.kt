package com.example.ui.wizard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VioletSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

data class PresetVectorLogo(
    val id: String,
    val label: String,
    val glyph: String,
    val bgStartHex: String,
    val bgEndHex: String,
    val fgHex: String
)

val PRESET_VECTOR_LOGOS = listOf(
    PresetVectorLogo("vec_globe", "Web Globe", "🌐", "#1A237E", "#3949AB", "#E8EAF6"),
    PresetVectorLogo("vec_play", "Cinema Play", "▶", "#880E4F", "#D81B60", "#FCE4EC"),
    PresetVectorLogo("vec_game", "Game Pad", "🎮", "#311B92", "#5E35B1", "#EDE7F6"),
    PresetVectorLogo("vec_code", "Dev Code", "</>", "#004D40", "#00897B", "#E0F2F1"),
    PresetVectorLogo("vec_music", "Audio Wave", "♫", "#4A148C", "#8E24AA", "#F3E5F5"),
    PresetVectorLogo("vec_bolt", "Fast App", "⚡", "#E65100", "#FB8C00", "#FFF3E0"),
    PresetVectorLogo("vec_rocket", "Launch", "🚀", "#0D47A1", "#1E88E5", "#E3F2FD"),
    PresetVectorLogo("vec_tv", "TV Stream", "📺", "#1B5E20", "#43A047", "#E8F5E9"),
    PresetVectorLogo("vec_star", "Featured", "★", "#F57F17", "#FDD835", "#212121"),
    PresetVectorLogo("vec_cloud", "Cloud Hub", "☁", "#01579B", "#039BE5", "#E1F5FE"),
    PresetVectorLogo("vec_book", "Reader", "📖", "#3E2723", "#6D4C41", "#EFEBE9"),
    PresetVectorLogo("vec_atom", "Science", "⚛", "#006064", "#00ACC1", "#E0F7FA"),
    PresetVectorLogo("vec_shield", "Privacy", "🛡", "#263238", "#546E7A", "#ECEFF1"),
    PresetVectorLogo("vec_spark", "AI Spark", "✦", "#283593", "#7E57C2", "#EDE7F6"),
    PresetVectorLogo("vec_compass", "Explorer", "🧭", "#1A237E", "#00ACC1", "#E0F7FA"),
    PresetVectorLogo("vec_terminal", "Console", ">_", "#12131A", "#282A37", "#A6F2D1")
)

val ALPHABET_BG_PALETTE = listOf(
    "#1E293B" to Color(0xFF1E293B),
    "#312E81" to Color(0xFF312E81),
    "#1E3A8A" to Color(0xFF1E3A8A),
    "#064E3B" to Color(0xFF064E3B),
    "#701A75" to Color(0xFF701A75),
    "#7F1D1D" to Color(0xFF7F1D1D),
    "#78350F" to Color(0xFF78350F),
    "#0F172A" to Color(0xFF0F172A),
    "#3B0764" to Color(0xFF3B0764),
    "#14532D" to Color(0xFF14532D)
)

val ALPHABET_TEXT_PALETTE = listOf(
    "#FFFFFF" to Color.White,
    "#BAC3FF" to CyanPrimary,
    "#A6F2D1" to EmeraldSuccess,
    "#D0BCFF" to VioletSecondary,
    "#FFD54F" to AmberAccent,
    "#FFB4AB" to CoralAccent
)

/**
 * Generates a high-DPI PNG data URI ("data:image/png;base64,...") from a glyph or alphabet letters.
 */
fun generateCircularLogoDataUri(
    text: String,
    bgStartColor: Int,
    bgEndColor: Int,
    textColor: Int
): String {
    val size = 256
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, size.toFloat(), size.toFloat(),
            bgStartColor, bgEndColor, Shader.TileMode.CLAMP
        )
    }
    // Draw full-bleed square background so any of the 19 App Icon Shapes clips cleanly without a circle inside
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

    val cleanText = text.trim().take(3).ifEmpty { "A" }
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        textSize = when (cleanText.length) {
            1 -> 118f
            2 -> 94f
            else -> 76f
        }
    }

    val bounds = Rect()
    textPaint.getTextBounds(cleanText, 0, cleanText.length, bounds)
    val yPos = size / 2f - bounds.exactCenterY()
    canvas.drawText(cleanText, size / 2f, yPos, textPaint)

    val out = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
    val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    return "data:image/png;base64,$base64"
}

/**
 * Scales and encodes any Android Bitmap into an offline-safe PNG data URI ("data:image/png;base64,...").
 */
fun bitmapToPngDataUri(source: Bitmap, maxDimension: Int = 256): String {
    val w = source.width.coerceAtLeast(1)
    val h = source.height.coerceAtLeast(1)
    val scaled = if (w > maxDimension || h > maxDimension) {
        val ratio = maxDimension.toFloat() / maxOf(w, h).toFloat()
        val targetW = (w * ratio).toInt().coerceAtLeast(1)
        val targetH = (h * ratio).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(source, targetW, targetH, true)
    } else {
        source
    }
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.PNG, 95, out)
    if (scaled !== source) {
        runCatching { scaled.recycle() }
    }
    val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    return "data:image/png;base64,$base64"
}

/**
 * Decodes a "data:image/png;base64,..." URI directly into an ImageBitmap offline without Coil.
 */
fun decodeDataUriToImageBitmap(uri: String?): ImageBitmap? {
    if (uri.isNullOrBlank()) return null
    return try {
        val base64Part = if (uri.startsWith("data:image", ignoreCase = true) && uri.contains(",")) {
            uri.substringAfter(",")
        } else if (!uri.contains("://") && uri.length > 64) {
            uri
        } else {
            return null
        }
        val bytes = Base64.decode(base64Part, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

/**
 * Extracts a high-res favicon URL for a given website URL so websites can automatically fetch their logo.
 */
fun buildFaviconUrlForWebsite(websiteUrl: String): String? {
    return try {
        val normalized = if (websiteUrl.startsWith("http://") || websiteUrl.startsWith("https://")) {
            websiteUrl
        } else {
            "https://$websiteUrl"
        }
        val host = URI(normalized).host?.removePrefix("www.") ?: return null
        if (host.isBlank() || !host.contains(".")) return null
        "https://www.google.com/s2/favicons?domain=$host&sz=256"
    } catch (_: Exception) {
        null
    }
}

private fun downloadImageBitmapFromUrl(urlString: String): Bitmap? {
    return try {
        val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
            connectTimeout = 4_500
            readTimeout = 5_500
            instanceFollowRedirects = true
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            )
        }
        val code = conn.responseCode
        if (code !in 200..299) {
            conn.disconnect()
            return null
        }
        val bytes = conn.inputStream.use { it.readBytes() }
        conn.disconnect()
        if (bytes.size < 64) return null
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        if (decoded.width < 12 || decoded.height < 12) {
            runCatching { decoded.recycle() }
            return null
        }
        decoded
    } catch (_: Exception) {
        null
    }
}

/**
 * Converts any chosen logo URI (data URI, picked gallery content:// URI, file:// URI, or remote https:// favicon)
 * into a permanent, offline-safe PNG data URI so converted web apps never lose their icon after restarts or offline.
 */
suspend fun resolveWebsiteLogoToPersistentDataUri(
    context: Context,
    candidateUri: String?,
    websiteUrl: String?,
    fallbackTitle: String,
    capturedWebViewFavicon: Bitmap? = null
): String = withContext(Dispatchers.IO) {
    val trimmed = candidateUri?.trim().orEmpty()

    // 1. Already an embedded data URI
    if (trimmed.startsWith("data:image", ignoreCase = true) && trimmed.contains(",")) {
        return@withContext trimmed
    }

    // 2. Local content:// or file:// image picked from gallery/files -> encode permanently so permissions never expire
    if (trimmed.startsWith("content://") || trimmed.startsWith("file://")) {
        val localBmp = runCatching {
            if (trimmed.startsWith("file://")) {
                val path = Uri.parse(trimmed).path
                if (!path.isNullOrBlank() && File(path).exists()) BitmapFactory.decodeFile(path) else null
            } else {
                context.contentResolver.openInputStream(Uri.parse(trimmed))?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        }.getOrNull()
        if (localBmp != null) {
            val encoded = bitmapToPngDataUri(localBmp, 256)
            runCatching { localBmp.recycle() }
            return@withContext encoded
        }
    }

    // 3. Remote http(s) favicon or page icon -> download and embed as PNG data URI
    val remoteCandidates = buildList {
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            add(trimmed)
        }
        val normSite = websiteUrl?.trim()?.takeIf { it.isNotBlank() }
        if (normSite != null) {
            val fullSite = if (normSite.startsWith("http://") || normSite.startsWith("https://")) normSite else "https://$normSite"
            val host = runCatching { URI(fullSite).host?.removePrefix("www.") }.getOrNull()
            if (!host.isNullOrBlank() && host.contains(".")) {
                add("https://www.google.com/s2/favicons?domain=$host&sz=256")
                add("https://icons.duckduckgo.com/ip3/$host.ico")
                add("https://$host/apple-touch-icon.png")
            }
        }
    }.distinct()

    for (urlCandidate in remoteCandidates) {
        val bmp = downloadImageBitmapFromUrl(urlCandidate)
        if (bmp != null) {
            val encoded = bitmapToPngDataUri(bmp, 256)
            runCatching { bmp.recycle() }
            return@withContext encoded
        }
    }

    // 4. Live WebView favicon captured via WebChromeClient.onReceivedIcon
    if (capturedWebViewFavicon != null && !capturedWebViewFavicon.isRecycled &&
        capturedWebViewFavicon.width >= 16 && capturedWebViewFavicon.height >= 16
    ) {
        return@withContext bitmapToPngDataUri(capturedWebViewFavicon, 256)
    }

    // 5. Crisp monogram fallback derived from the app's title (works 100% offline)
    val words = fallbackTitle.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val initials = when {
        words.size >= 2 -> "${words[0].first()}${words[1].first()}".uppercase()
        words.size == 1 -> words[0].take(2).uppercase()
        else -> "WA"
    }
    generateCircularLogoDataUri(
        text = initials,
        bgStartColor = android.graphics.Color.parseColor("#1E3A8A"),
        bgEndColor = android.graphics.Color.parseColor("#312E81"),
        textColor = android.graphics.Color.parseColor("#FFFFFF")
    )
}

/**
 * Unified Logo Selector supporting:
 * 1. Upload Custom Photo
 * 2. Auto-Fetch Website Favicon (when URL is available)
 * 3. 16 Preset Vector/SVG-style Badges
 * 4. Alphabet / Monogram Customizer (Letter + Text Color + Background Color)
 * Fully D-Pad navigable on Android TV with clear focus highlights.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RnaLogoSelectorCard(
    headerTitle: String,
    headerSubtitle: String,
    currentLogoUri: String?,
    websiteUrlForFavicon: String? = null,
    defaultTitleForAlphabet: String = "RNA",
    onSelectLogoUri: (String) -> Unit,
    onPickFromGallery: () -> Unit,
    showUploadButton: Boolean = true
) {
    var modeTab by remember { mutableIntStateOf(0) } // 0: Preset Icons, 1: Alphabet Builder
    var customLetters by remember(defaultTitleForAlphabet) {
        mutableStateOf(defaultTitleForAlphabet.trim().take(2).uppercase().ifEmpty { "R" })
    }
    var selectedBgHex by remember { mutableStateOf(ALPHABET_BG_PALETTE[1].first) }
    var selectedFgHex by remember { mutableStateOf(ALPHABET_TEXT_PALETTE[1].first) }
    val decodedPreviewBitmap = remember(currentLogoUri) { decodeDataUriToImageBitmap(currentLogoUri) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2030)),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Color(0x28FFFFFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Current Logo Preview + Upload & Auto-Favicon Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF262E45),
                        border = BorderStroke(2.dp, CyanPrimary),
                        modifier = Modifier.size(56.dp)
                    ) {
                        when {
                            decodedPreviewBitmap != null -> {
                                Image(
                                    bitmap = decodedPreviewBitmap,
                                    contentDescription = headerTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                            !currentLogoUri.isNullOrBlank() -> {
                                AsyncImage(
                                    model = currentLogoUri,
                                    contentDescription = headerTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                            else -> {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = CyanPrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(headerTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(headerSubtitle, color = Color(0xFF8697B2), fontSize = 11.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val faviconUrl = websiteUrlForFavicon?.let { buildFaviconUrlForWebsite(it) }
                    if (faviconUrl != null) {
                        var siteIconFocused by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (siteIconFocused) EmeraldSuccess else EmeraldSuccess.copy(alpha = 0.14f),
                            border = BorderStroke(
                                width = if (siteIconFocused) 2.dp else 1.dp,
                                color = if (siteIconFocused) Color.White else EmeraldSuccess
                            ),
                            modifier = Modifier
                                .onFocusChanged { siteIconFocused = it.isFocused }
                                .clickable { onSelectLogoUri(faviconUrl) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    tint = if (siteIconFocused) Color(0xFF0E2419) else EmeraldSuccess,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Site Icon",
                                    color = if (siteIconFocused) Color(0xFF0E2419) else EmeraldSuccess,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }

                    if (showUploadButton) {
                        var uploadFocused by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (uploadFocused) CyanPrimary else CyanPrimary.copy(alpha = 0.14f),
                            border = BorderStroke(
                                width = if (uploadFocused) 2.dp else 1.dp,
                                color = if (uploadFocused) Color.White else CyanPrimary
                            ),
                            modifier = Modifier
                                .onFocusChanged { uploadFocused = it.isFocused }
                                .clickable { onPickFromGallery() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = if (uploadFocused) Color(0xFF10182B) else CyanPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Upload",
                                    color = if (uploadFocused) Color(0xFF10182B) else CyanPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // Sub-tabs: [ Preset Logos ]  [ Alphabet Logo Builder ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var tab0Focused by remember { mutableStateOf(false) }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        tab0Focused -> CyanPrimary
                        modeTab == 0 -> CyanPrimary.copy(alpha = 0.2f)
                        else -> Color(0xFF141824)
                    },
                    border = BorderStroke(
                        width = if (tab0Focused) 2.dp else 1.dp,
                        color = when {
                            tab0Focused -> Color.White
                            modeTab == 0 -> CyanPrimary
                            else -> Color(0x22FFFFFF)
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { tab0Focused = it.isFocused }
                        .clickable { modeTab = 0 }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = when {
                                tab0Focused -> Color(0xFF10182B)
                                modeTab == 0 -> CyanPrimary
                                else -> Color(0xFF90909E)
                            },
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Preset Vector Logos",
                            color = when {
                                tab0Focused -> Color(0xFF10182B)
                                modeTab == 0 -> Color.White
                                else -> Color(0xFF90909E)
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                var tab1Focused by remember { mutableStateOf(false) }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        tab1Focused -> VioletSecondary
                        modeTab == 1 -> VioletSecondary.copy(alpha = 0.2f)
                        else -> Color(0xFF141824)
                    },
                    border = BorderStroke(
                        width = if (tab1Focused) 2.dp else 1.dp,
                        color = when {
                            tab1Focused -> Color.White
                            modeTab == 1 -> VioletSecondary
                            else -> Color(0x22FFFFFF)
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { tab1Focused = it.isFocused }
                        .clickable { modeTab = 1 }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FontDownload,
                            contentDescription = null,
                            tint = when {
                                tab1Focused -> Color(0xFF1F1038)
                                modeTab == 1 -> VioletSecondary
                                else -> Color(0xFF90909E)
                            },
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Alphabet Logo Builder",
                            color = when {
                                tab1Focused -> Color(0xFF1F1038)
                                modeTab == 1 -> Color.White
                                else -> Color(0xFF90909E)
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (modeTab == 0) {
                // Horizontal row of 16 preset vector-style logos with TV focus highlights
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(PRESET_VECTOR_LOGOS, key = { it.id }) { preset ->
                        val bgStart = remember(preset.bgStartHex) {
                            android.graphics.Color.parseColor(preset.bgStartHex)
                        }
                        val bgEnd = remember(preset.bgEndHex) {
                            android.graphics.Color.parseColor(preset.bgEndHex)
                        }
                        val fg = remember(preset.fgHex) {
                            android.graphics.Color.parseColor(preset.fgHex)
                        }
                        var presetFocused by remember { mutableStateOf(false) }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (presetFocused) Color(0xFF283149) else Color.Transparent)
                                .border(
                                    width = if (presetFocused) 2.dp else 0.dp,
                                    color = if (presetFocused) CyanPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .onFocusChanged { presetFocused = it.isFocused }
                                .clickable {
                                    val uri = generateCircularLogoDataUri(
                                        text = preset.glyph,
                                        bgStartColor = bgStart,
                                        bgEndColor = bgEnd,
                                        textColor = fg
                                    )
                                    onSelectLogoUri(uri)
                                }
                                .padding(horizontal = 6.dp, vertical = 5.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(bgStart),
                                border = BorderStroke(
                                    width = if (presetFocused) 2.dp else 1.5.dp,
                                    color = if (presetFocused) Color.White else Color(fg).copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = preset.glyph,
                                        color = Color(fg),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = preset.label,
                                color = if (presetFocused) Color.White else Color(0xFFB5B6C6),
                                fontSize = 9.5.sp,
                                fontWeight = if (presetFocused) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        }
                    }
                }
            } else {
                // Alphabet / Monogram Customizer (Letters + Background Color + Text Color)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = customLetters,
                            onValueChange = { customLetters = it.take(3).uppercase() },
                            label = { Text("Letters (1–3)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VioletSecondary,
                                unfocusedBorderColor = Color(0xFF28324E),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        var applyFocused by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (applyFocused) Color.White else VioletSecondary,
                            border = BorderStroke(if (applyFocused) 2.dp else 1.dp, if (applyFocused) CyanPrimary else VioletSecondary),
                            modifier = Modifier
                                .onFocusChanged { applyFocused = it.isFocused }
                                .clickable {
                                    val bgInt = android.graphics.Color.parseColor(selectedBgHex)
                                    val fgInt = android.graphics.Color.parseColor(selectedFgHex)
                                    val generatedUri = generateCircularLogoDataUri(
                                        text = customLetters.ifBlank { "A" },
                                        bgStartColor = bgInt,
                                        bgEndColor = bgInt,
                                        textColor = fgInt
                                    )
                                    onSelectLogoUri(generatedUri)
                                }
                        ) {
                            Text(
                                text = "Apply Alphabet Logo",
                                color = Color(0xFF1F1038),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.5.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            )
                        }
                    }

                    Text("Background Color:", color = Color(0xFFA0B0C8), fontSize = 11.sp)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ALPHABET_BG_PALETTE.forEach { (hex, col) ->
                            val isSelected = selectedBgHex.equals(hex, ignoreCase = true)
                            var swatchFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        width = when {
                                            swatchFocused -> 3.dp
                                            isSelected -> 2.5.dp
                                            else -> 1.dp
                                        },
                                        color = when {
                                            swatchFocused -> CyanPrimary
                                            isSelected -> Color.White
                                            else -> Color(0x44FFFFFF)
                                        },
                                        shape = CircleShape
                                    )
                                    .onFocusChanged { swatchFocused = it.isFocused }
                                    .clickable {
                                        selectedBgHex = hex
                                        val bgInt = col.toArgb()
                                        val fgInt = android.graphics.Color.parseColor(selectedFgHex)
                                        onSelectLogoUri(
                                            generateCircularLogoDataUri(
                                                text = customLetters.ifBlank { "A" },
                                                bgStartColor = bgInt,
                                                bgEndColor = bgInt,
                                                textColor = fgInt
                                            )
                                        )
                                    }
                            )
                        }
                    }

                    Text("Letter Color:", color = Color(0xFFA0B0C8), fontSize = 11.sp)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ALPHABET_TEXT_PALETTE.forEach { (hex, col) ->
                            val isSelected = selectedFgHex.equals(hex, ignoreCase = true)
                            var swatchFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        width = when {
                                            swatchFocused -> 3.dp
                                            isSelected -> 2.5.dp
                                            else -> 1.dp
                                        },
                                        color = when {
                                            swatchFocused -> EmeraldSuccess
                                            isSelected -> CyanPrimary
                                            else -> Color.Black
                                        },
                                        shape = CircleShape
                                    )
                                    .onFocusChanged { swatchFocused = it.isFocused }
                                    .clickable {
                                        selectedFgHex = hex
                                        val bgInt = android.graphics.Color.parseColor(selectedBgHex)
                                        val fgInt = col.toArgb()
                                        onSelectLogoUri(
                                            generateCircularLogoDataUri(
                                                text = customLetters.ifBlank { "A" },
                                                bgStartColor = bgInt,
                                                bgEndColor = bgInt,
                                                textColor = fgInt
                                            )
                                        )
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

