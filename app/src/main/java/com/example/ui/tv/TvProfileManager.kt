package com.example.ui.tv

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.RnaHomeInterceptorService
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VioletSecondary
import com.example.ui.wizard.decodeDataUriToImageBitmap
import com.example.ui.wizard.generateCircularLogoDataUri
import java.io.File
import java.io.FileOutputStream
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class TvUserProfile(
    val id: String,
    val name: String,
    val avatarUri: String,
    val wallpaperUri: String,
    val isolateBrowserData: Boolean = false,
    val wallpaperModeType: String = "STATIC",
    val liveWallpaperUri: String = "",
    val appIconShape: String = "Circle",
    val singleRowHomeApps: Boolean = false,
    val showRecentAppsRow: Boolean = true,
    val ambientIdleMinutes: Int = 5
)

data class PresetProfileAvatar(
    val id: String,
    val label: String,
    val glyph: String,
    val bgTopHex: Int,
    val bgBottomHex: Int,
    val fgHex: Int = 0xFFFFFFFF.toInt()
) {
    val presetUri: String get() = "preset://$id"

    val dataUri: String by lazy {
        generateCircularLogoDataUri(
            text = glyph,
            bgStartColor = bgTopHex,
            bgEndColor = bgBottomHex,
            textColor = fgHex
        )
    }
}

/**
 * 28 Offline-First Stylized Preset Profile Avatars for RNA. TV Multi-User Profiles.
 * Rendered 100% locally via Compose Canvas & offline bitmaps with zero network dependency.
 */
val PRESET_PROFILE_AVATARS: List<PresetProfileAvatar> = listOf(
    PresetProfileAvatar("av_01", "Cyber Nova", "★", 0xFF00B4DB.toInt(), 0xFF0083B0.toInt()),
    PresetProfileAvatar("av_02", "Neon Samurai", "⚡", 0xFF7F00FF.toInt(), 0xFFE100FF.toInt()),
    PresetProfileAvatar("av_03", "Astro Explorer", "🚀", 0xFFFF416C.toInt(), 0xFFFF4B2B.toInt()),
    PresetProfileAvatar("av_04", "Emerald Gamer", "🎮", 0xFF11998E.toInt(), 0xFF38EF7D.toInt(), 0xFF071B15.toInt()),
    PresetProfileAvatar("av_05", "Cinema Director", "🎬", 0xFFF7971E.toInt(), 0xFFFFD200.toInt(), 0xFF1A1405.toInt()),
    PresetProfileAvatar("av_06", "Synthwave DJ", "🎧", 0xFF8E2DE2.toInt(), 0xFF4A00E0.toInt()),
    PresetProfileAvatar("av_07", "Solar Crown", "👑", 0xFFFF8008.toInt(), 0xFFFFC837.toInt(), 0xFF1F1200.toInt()),
    PresetProfileAvatar("av_08", "Arctic Wolf", "🐺", 0xFF2193B0.toInt(), 0xFF6DD5ED.toInt(), 0xFF081C24.toInt()),
    PresetProfileAvatar("av_09", "Midnight Owl", "🦉", 0xFF141E30.toInt(), 0xFF243B55.toInt(), 0xFF00E5FF.toInt()),
    PresetProfileAvatar("av_10", "Crimson Dragon", "🐉", 0xFFCB2D3E.toInt(), 0xFFEF473A.toInt()),
    PresetProfileAvatar("av_11", "Zen Lotus", "🪷", 0xFFDA4453.toInt(), 0xFF89216B.toInt()),
    PresetProfileAvatar("av_12", "Quantum Atom", "⚛", 0xFF0052D4.toInt(), 0xFF6FB1FC.toInt()),
    PresetProfileAvatar("av_13", "Pixel Robot", "🤖", 0xFF3A1C71.toInt(), 0xFFD76D77.toInt()),
    PresetProfileAvatar("av_14", "Golden Lion", "🦁", 0xFFF12711.toInt(), 0xFFF5AF19.toInt()),
    PresetProfileAvatar("av_15", "Ocean Dolphin", "🐬", 0xFF1A2980.toInt(), 0xFF26D0CE.toInt()),
    PresetProfileAvatar("av_16", "Cosmic Alien", "👽", 0xFF00F260.toInt(), 0xFF0575E6.toInt()),
    PresetProfileAvatar("av_17", "Fire Phoenix", "🔥", 0xFFFE8C00.toInt(), 0xFFF83600.toInt()),
    PresetProfileAvatar("av_18", "Diamond VIP", "💎", 0xFF2980B9.toInt(), 0xFF6DD5FA.toInt(), 0xFF071A26.toInt()),
    PresetProfileAvatar("av_19", "Electric Guitar", "🎸", 0xFFB92B27.toInt(), 0xFF1565C0.toInt()),
    PresetProfileAvatar("av_20", "Turbo Racer", "🏎", 0xFFED213A.toInt(), 0xFF93291E.toInt()),
    PresetProfileAvatar("av_21", "Starlight Fox", "🦊", 0xFFFF512F.toInt(), 0xFFF09819.toInt()),
    PresetProfileAvatar("av_22", "Galaxy Butterfly", "🦋", 0xFF654EA3.toInt(), 0xFFEAAFC8.toInt()),
    PresetProfileAvatar("av_23", "Stealth Ninja", "🥷", 0xFF0F2027.toInt(), 0xFF2C5364.toInt(), 0xFF00E676.toInt()),
    PresetProfileAvatar("av_24", "Popcorn Night", "🍿", 0xFFE52D27.toInt(), 0xFFB31217.toInt()),
    PresetProfileAvatar("av_25", "Emerald Panda", "🐼", 0xFF134E5E.toInt(), 0xFF71B280.toInt()),
    PresetProfileAvatar("av_26", "Lightning Bolt", "🌩", 0xFF4776E6.toInt(), 0xFF8E54E9.toInt()),
    PresetProfileAvatar("av_27", "Prism Unicorn", "🦄", 0xFFEC008C.toInt(), 0xFFFC6767.toInt()),
    PresetProfileAvatar("av_28", "Code Architect", "</>", 0xFF16222A.toInt(), 0xFF3A6073.toInt(), 0xFF00E5FF.toInt())
)

/**
 * Resolves a PresetProfileAvatar from any preset URI, ID, or legacy data URI so all preset profiles
 * are 100% offline available.
 */
fun resolveOfflinePresetAvatar(avatarUri: String?): PresetProfileAvatar? {
    if (avatarUri.isNullOrBlank()) return PRESET_PROFILE_AVATARS.first()
    val cleanId = avatarUri.removePrefix("preset://").trim()
    PRESET_PROFILE_AVATARS.firstOrNull { it.id.equals(cleanId, ignoreCase = true) }?.let { return it }
    PRESET_PROFILE_AVATARS.firstOrNull { it.dataUri == avatarUri }?.let { return it }
    // If a legacy external DiceBear/http avatar URL was stored earlier, map it deterministically to an offline preset
    if (avatarUri.contains("dicebear", ignoreCase = true) || avatarUri.contains("robohash", ignoreCase = true)) {
        val idx = (avatarUri.hashCode().and(0x7FFFFFFF)) % PRESET_PROFILE_AVATARS.size
        return PRESET_PROFILE_AVATARS[idx]
    }
    return null
}

/**
 * 100% Offline-First Profile Avatar Renderer:
 * - Renders all 28 preset avatars using pure Compose Canvas gradients, geometric rings & crisp glyphs (zero network!).
 * - Decodes any Base64 "data:image/..." URI directly in memory via BitmapFactory.
 * - Uses AsyncImage only when the user explicitly selects a local gallery photo (content:// or file://).
 */
@Composable
fun TvProfileAvatarImage(
    avatarUri: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val preset = remember(avatarUri) { resolveOfflinePresetAvatar(avatarUri) }
    if (preset != null) {
        val topColor = Color(preset.bgTopHex)
        val bottomColor = Color(preset.bgBottomHex)
        val fgColor = Color(preset.fgHex)
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(topColor, bottomColor)
                    )
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                // Subtle decorative inner orbital rings
                drawCircle(
                    color = fgColor.copy(alpha = 0.22f),
                    radius = r * 0.86f,
                    style = Stroke(width = (r * 0.06f).coerceAtLeast(1.5f))
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.14f),
                    radius = r * 0.62f,
                    center = Offset(size.width * 0.35f, size.height * 0.32f)
                )
            }
            Text(
                text = preset.glyph,
                color = fgColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val decodedBitmap = remember(avatarUri) { decodeDataUriToImageBitmap(avatarUri) }
    if (decodedBitmap != null) {
        Image(
            bitmap = decodedBitmap,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(CircleShape)
        )
        return
    }

    AsyncImage(
        model = avatarUri,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(CircleShape)
    )
}

private const val KEY_TV_PROFILES_V2 = "rna_tv_profiles_v2"
private const val KEY_TV_PROFILES_LEGACY = "rna_tv_profiles"
private const val KEY_ACTIVE_PROFILE_ID = "rna_tv_active_profile_id"
private const val BACKUP_PROFILES_FILENAME = "rna_tv_profiles_persistent.json"
private const val BACKUP_ACTIVE_ID_FILENAME = "rna_tv_active_profile_id.txt"

/**
 * Multi-tier durable persistence store for RNA. TV user profiles so existing user-configured
 * profiles, avatars, and wallpapers reliably survive device reboots (BOOT_COMPLETED / Direct Boot)
 * and APK updates (MY_PACKAGE_REPLACED) without ever being reset to defaults.
 */
object TvProfilePersistenceStore {
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun getDeviceProtectedPrefs(context: Context): SharedPreferences? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val dpCtx = context.createDeviceProtectedStorageContext()
                dpCtx.getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun readRawProfilesJsonWithFallback(prefs: SharedPreferences): String? {
        // 1. Primary SharedPreferences
        val primary = prefs.getString(KEY_TV_PROFILES_V2, null)
        if (!primary.isNullOrBlank()) return primary

        val ctx = appContext
        // 2. Internal persistent backup file (survives any SharedPreferences cache clear)
        if (ctx != null) {
            try {
                val backupFile = File(ctx.filesDir, BACKUP_PROFILES_FILENAME)
                if (backupFile.exists()) {
                    val fileText = backupFile.readText().trim()
                    if (fileText.startsWith("[")) {
                        prefs.edit().putString(KEY_TV_PROFILES_V2, fileText).commit()
                        return fileText
                    }
                }
            } catch (_: Exception) {
            }

            // 3. Device-Protected Storage (available immediately during Direct Boot / BOOT_COMPLETED)
            try {
                val dpJson = getDeviceProtectedPrefs(ctx)?.getString(KEY_TV_PROFILES_V2, null)
                if (!dpJson.isNullOrBlank()) {
                    prefs.edit().putString(KEY_TV_PROFILES_V2, dpJson).commit()
                    return dpJson
                }
            } catch (_: Exception) {
            }
        }

        // 4. Legacy SharedPreferences key migration
        val legacy = prefs.getString(KEY_TV_PROFILES_LEGACY, null)
        if (!legacy.isNullOrBlank()) {
            prefs.edit().putString(KEY_TV_PROFILES_V2, legacy).commit()
            return legacy
        }

        return null
    }

    fun writeRawProfilesJsonDurable(prefs: SharedPreferences, jsonString: String) {
        // Synchronous commit to guarantee disk flush before any reboot or update
        prefs.edit().putString(KEY_TV_PROFILES_V2, jsonString).commit()
        val ctx = appContext ?: return
        try {
            val backupFile = File(ctx.filesDir, BACKUP_PROFILES_FILENAME)
            backupFile.writeText(jsonString)
        } catch (_: Exception) {
        }
        try {
            getDeviceProtectedPrefs(ctx)?.edit()?.putString(KEY_TV_PROFILES_V2, jsonString)?.commit()
        } catch (_: Exception) {
        }
    }

    fun readActiveProfileIdWithFallback(prefs: SharedPreferences): String {
        val primary = prefs.getString(KEY_ACTIVE_PROFILE_ID, null)
        if (!primary.isNullOrBlank()) return primary

        val ctx = appContext
        if (ctx != null) {
            try {
                val idFile = File(ctx.filesDir, BACKUP_ACTIVE_ID_FILENAME)
                if (idFile.exists()) {
                    val savedId = idFile.readText().trim()
                    if (savedId.isNotBlank()) {
                        prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, savedId).commit()
                        return savedId
                    }
                }
            } catch (_: Exception) {
            }
            try {
                val dpId = getDeviceProtectedPrefs(ctx)?.getString(KEY_ACTIVE_PROFILE_ID, null)
                if (!dpId.isNullOrBlank()) {
                    prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, dpId).commit()
                    return dpId
                }
            } catch (_: Exception) {
            }
        }
        return "default"
    }

    fun writeActiveProfileIdDurable(prefs: SharedPreferences, profileId: String) {
        prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, profileId).commit()
        val ctx = appContext ?: return
        try {
            File(ctx.filesDir, BACKUP_ACTIVE_ID_FILENAME).writeText(profileId)
        } catch (_: Exception) {
        }
        try {
            getDeviceProtectedPrefs(ctx)?.edit()?.putString(KEY_ACTIVE_PROFILE_ID, profileId)?.commit()
        } catch (_: Exception) {
        }
    }

    fun ensureProfilesSyncedOnBootOrUpdate(context: Context) {
        init(context)
        try {
            val prefs = try {
                context.getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
            } catch (_: Exception) {
                getDeviceProtectedPrefs(context) ?: return
            }
            val existingRaw = readRawProfilesJsonWithFallback(prefs)
            if (!existingRaw.isNullOrBlank()) {
                writeRawProfilesJsonDurable(prefs, existingRaw)
            }
            val activeId = readActiveProfileIdWithFallback(prefs)
            writeActiveProfileIdDurable(prefs, activeId)
        } catch (_: Exception) {
        }
    }

    /**
     * Copies any user-picked custom photo (content:// URI) into permanent internal files storage
     * so custom avatars and wallpapers never lose URI read permissions after a reboot or app update.
     */
    fun persistCustomProfileMediaUri(context: Context, sourceUri: Uri, prefix: String): String {
        val rawStr = sourceUri.toString()
        if (rawStr.startsWith("file://") || rawStr.startsWith("preset://") || rawStr.startsWith("data:")) {
            return rawStr
        }
        return try {
            val mediaDir = File(context.filesDir, "profile_media").apply { mkdirs() }
            val destFile = File(mediaDir, "${prefix}_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0L) {
                Uri.fromFile(destFile).toString()
            } else {
                rawStr
            }
        } catch (_: Exception) {
            rawStr
        }
    }
}

private fun buildDefaultOfflineProfiles(defaultWallpaper: String): List<TvUserProfile> {
    return listOf(
        TvUserProfile(
            id = "default",
            name = "Main",
            avatarUri = PRESET_PROFILE_AVATARS[0].presetUri,
            wallpaperUri = defaultWallpaper,
            isolateBrowserData = false,
            wallpaperModeType = "STATIC",
            liveWallpaperUri = "",
            appIconShape = "Circle",
            singleRowHomeApps = false,
            showRecentAppsRow = true,
            ambientIdleMinutes = 5
        )
    )
}

fun loadTvProfiles(prefs: SharedPreferences): List<TvUserProfile> {
    val raw = TvProfilePersistenceStore.readRawProfilesJsonWithFallback(prefs)
    val defaultWallpaper = prefs.getString("wallpaper_uri", null) ?: PRESET_WALLPAPER_ASSET_URIS.first()
    if (raw.isNullOrBlank()) {
        val defaults = buildDefaultOfflineProfiles(defaultWallpaper)
        saveTvProfiles(prefs, defaults)
        return defaults
    }
    return try {
        val arr = JSONArray(raw)
        val list = mutableListOf<TvUserProfile>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val rawAvatar = obj.optString("avatarUri", PRESET_PROFILE_AVATARS.first().presetUri)
            // Normalize any preset avatar to its compact offline "preset://av_XX" URI
            val normalizedAvatar = resolveOfflinePresetAvatar(rawAvatar)?.presetUri ?: rawAvatar
            list.add(
                TvUserProfile(
                    id = obj.optString("id", "default"),
                    name = obj.optString("name", "Profile ${i + 1}").take(10),
                    avatarUri = normalizedAvatar,
                    wallpaperUri = obj.optString("wallpaperUri", defaultWallpaper).ifBlank { defaultWallpaper },
                    isolateBrowserData = obj.optBoolean("isolateBrowserData", false),
                    wallpaperModeType = obj.optString("wallpaperModeType", "STATIC").ifBlank { "STATIC" },
                    liveWallpaperUri = obj.optString("liveWallpaperUri", ""),
                    appIconShape = obj.optString("appIconShape", "Circle").ifBlank { "Circle" },
                    singleRowHomeApps = obj.optBoolean("singleRowHomeApps", false),
                    showRecentAppsRow = obj.optBoolean("showRecentAppsRow", true),
                    ambientIdleMinutes = obj.optInt("ambientIdleMinutes", 5)
                )
            )
        }
        if (list.isEmpty()) {
            val defaults = buildDefaultOfflineProfiles(defaultWallpaper)
            saveTvProfiles(prefs, defaults)
            defaults
        } else {
            // Ensure all backup tiers stay in sync with the loaded user profiles
            saveTvProfiles(prefs, list)
            list
        }
    } catch (_: Exception) {
        buildDefaultOfflineProfiles(defaultWallpaper)
    }
}

fun saveTvProfiles(prefs: SharedPreferences, profiles: List<TvUserProfile>) {
    val arr = JSONArray()
    profiles.forEach { p ->
        arr.put(
            JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("avatarUri", p.avatarUri)
                put("wallpaperUri", p.wallpaperUri)
                put("isolateBrowserData", p.isolateBrowserData)
                put("wallpaperModeType", p.wallpaperModeType)
                put("liveWallpaperUri", p.liveWallpaperUri)
                put("appIconShape", p.appIconShape)
                put("singleRowHomeApps", p.singleRowHomeApps)
                put("showRecentAppsRow", p.showRecentAppsRow)
                put("ambientIdleMinutes", p.ambientIdleMinutes)
            }
        )
    }
    TvProfilePersistenceStore.writeRawProfilesJsonDurable(prefs, arr.toString())
}

fun getActiveTvProfileId(prefs: SharedPreferences): String {
    return TvProfilePersistenceStore.readActiveProfileIdWithFallback(prefs)
}

fun setActiveTvProfileId(prefs: SharedPreferences, profileId: String) {
    TvProfilePersistenceStore.writeActiveProfileIdDurable(prefs, profileId)
}

/**
 * Resolves the Wallpaper URI configured for the Main profile ("default" / "main" or first profile)
 * for use by the Android Splash Screen API component during initial app loading.
 */
fun getMainTvProfileWallpaperUri(prefs: SharedPreferences): String {
    val profiles = loadTvProfiles(prefs)
    val mainProfile = profiles.firstOrNull {
        it.id == "default" || it.id == "main" || it.name.equals("Main", ignoreCase = true)
    } ?: profiles.firstOrNull()
    return mainProfile?.wallpaperUri?.takeIf { it.isNotBlank() }
        ?: prefs.getString("wallpaper_uri", null)?.takeIf { it.isNotBlank() }
        ?: PRESET_WALLPAPER_ASSET_URIS.first()
}

private enum class TvProfileFlowMode {
    WHO_IS_WATCHING,
    CREATE_3_STEPS,
    EDIT_SIDEBAR
}

private enum class TvProfilePhotoPickerPopup {
    NONE,
    CHOOSER_3_OPTIONS,
    FILE_EXPLORER_UPLOAD,
    EMOJI_WINDOW,
    IN_APP_WALLPAPERS_WINDOW
}

@Composable
fun TvProfileSwitcherDialog(
    profiles: List<TvUserProfile>,
    activeProfile: TvUserProfile,
    initialCreateNew: Boolean = false,
    onSelectProfile: (TvUserProfile) -> Unit,
    onSaveProfile: (TvUserProfile, isNew: Boolean) -> Unit,
    onDeleteProfile: (TvUserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val nameFieldFocusRequester = remember { FocusRequester() }
    val firstProfileFocusRequester = remember { FocusRequester() }
    val createNextBtnFocusRequester = remember { FocusRequester() }
    val sidebarFirstTabFocusRequester = remember { FocusRequester() }

    var flowMode by remember(initialCreateNew) {
        mutableStateOf(
            if (initialCreateNew) TvProfileFlowMode.CREATE_3_STEPS else TvProfileFlowMode.WHO_IS_WATCHING
        )
    }

    var editingProfile by remember { mutableStateOf<TvUserProfile?>(null) }
    var createStep by remember(initialCreateNew) { mutableIntStateOf(0) } // 0: Name, 1: Logo, 2: Settings
    var editSidebarTab by remember { mutableIntStateOf(0) } // 0: Name & Logo, 1: Settings

    var draftName by remember(initialCreateNew) {
        mutableStateOf(if (initialCreateNew) "Profile ${profiles.size + 1}".take(10) else "")
    }
    var draftAvatarUri by remember(initialCreateNew) {
        mutableStateOf(
            if (initialCreateNew) {
                PRESET_PROFILE_AVATARS[profiles.size % PRESET_PROFILE_AVATARS.size].presetUri
            } else {
                PRESET_PROFILE_AVATARS.first().presetUri
            }
        )
    }
    var draftIsolateBrowser by remember(initialCreateNew) { mutableStateOf(initialCreateNew) }
    var photoPickerPopup by remember { mutableStateOf(TvProfilePhotoPickerPopup.NONE) }
    var showRnaKeyboardOverlay by remember { mutableStateOf(false) }

    fun triggerDefaultKeyboardForName(
        activeView: android.view.View? = view,
        activeKbController: androidx.compose.ui.platform.SoftwareKeyboardController? = keyboardController
    ) {
        if (isRnaKeyboardDefaultIme(context)) {
            showRnaKeyboardOverlay = true
        } else {
            openDeviceDefaultKeyboard(
                context = context,
                view = activeView,
                focusRequester = nameFieldFocusRequester,
                keyboardController = activeKbController
            )
        }
    }

    fun startCreateNewProfile() {
        editingProfile = null
        draftName = "Profile ${profiles.size + 1}".take(10)
        draftAvatarUri = PRESET_PROFILE_AVATARS[profiles.size % PRESET_PROFILE_AVATARS.size].presetUri
        draftIsolateBrowser = true
        createStep = 0
        flowMode = TvProfileFlowMode.CREATE_3_STEPS
    }

    fun startEditExistingProfile(profile: TvUserProfile) {
        editingProfile = profile
        draftName = profile.name.take(10)
        draftAvatarUri = profile.avatarUri.ifBlank { PRESET_PROFILE_AVATARS.first().presetUri }
        draftIsolateBrowser = profile.isolateBrowserData
        editSidebarTab = 0
        flowMode = TvProfileFlowMode.EDIT_SIDEBAR
    }

    fun commitProfileSave(isNew: Boolean) {
        val cleanName = draftName.trim().take(10).ifEmpty { "Profile" }
        val finalAvatarUri = draftAvatarUri.ifBlank { PRESET_PROFILE_AVATARS.first().presetUri }
        val updated = if (!isNew && editingProfile != null) {
            editingProfile!!.copy(
                name = cleanName,
                avatarUri = finalAvatarUri,
                isolateBrowserData = draftIsolateBrowser
            )
        } else {
            TvUserProfile(
                id = "profile_" + UUID.randomUUID().toString().take(8),
                name = cleanName,
                avatarUri = finalAvatarUri,
                wallpaperUri = activeProfile.wallpaperUri.ifBlank { PRESET_WALLPAPER_ASSET_URIS.first() },
                isolateBrowserData = draftIsolateBrowser
            )
        }
        onSaveProfile(updated, isNew)
        flowMode = TvProfileFlowMode.WHO_IS_WATCHING
        editingProfile = null
    }

    LaunchedEffect(flowMode, createStep) {
        delay(80L)
        when (flowMode) {
            TvProfileFlowMode.WHO_IS_WATCHING -> {
                runCatching { firstProfileFocusRequester.requestFocus() }
            }
            TvProfileFlowMode.CREATE_3_STEPS -> {
                if (createStep == 0) {
                    runCatching { nameFieldFocusRequester.requestFocus() }
                } else {
                    runCatching { createNextBtnFocusRequester.requestFocus() }
                }
            }
            TvProfileFlowMode.EDIT_SIDEBAR -> {
                runCatching { sidebarFirstTabFocusRequester.requestFocus() }
            }
        }
    }

    BackHandler {
        when {
            showRnaKeyboardOverlay -> showRnaKeyboardOverlay = false
            photoPickerPopup != TvProfilePhotoPickerPopup.NONE -> photoPickerPopup = TvProfilePhotoPickerPopup.NONE
            flowMode == TvProfileFlowMode.CREATE_3_STEPS -> {
                if (createStep > 0) {
                    createStep--
                } else if (initialCreateNew) {
                    onDismiss()
                } else {
                    flowMode = TvProfileFlowMode.WHO_IS_WATCHING
                }
            }
            flowMode == TvProfileFlowMode.EDIT_SIDEBAR -> {
                editingProfile = null
                flowMode = TvProfileFlowMode.WHO_IS_WATCHING
            }
            else -> onDismiss()
        }
    }

    Dialog(
        onDismissRequest = {
            if (flowMode != TvProfileFlowMode.WHO_IS_WATCHING && !initialCreateNew) {
                editingProfile = null
                flowMode = TvProfileFlowMode.WHO_IS_WATCHING
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
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
                        colors = listOf(Color(0xFF161D33), Color(0xFF070A12))
                    )
                )
                .testTag("tv_profile_switcher_dialog")
        ) {
            AnimatedContent(
                targetState = flowMode,
                transitionSpec = {
                    fadeIn(tween(220)) togetherWith fadeOut(tween(160))
                },
                label = "ProfileFlowModeTransition"
            ) { mode ->
                when (mode) {
                    // ─────────────────────────────────────────────────────────────────────
                    // 1. UNIFIED "WHO IS WATCHING?" SCREEN + SMALL PEN BUTTON UNDER PROFILES
                    // ─────────────────────────────────────────────────────────────────────
                    TvProfileFlowMode.WHO_IS_WATCHING -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 36.dp, vertical = 28.dp)
                                .testTag("tv_who_is_watching_boot_overlay")
                        ) {
                            Text(
                                text = "Who is watching?",
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(38.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(30.dp),
                                contentPadding = PaddingValues(horizontal = 32.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                itemsIndexed(profiles, key = { _, p -> p.id }) { index, prof ->
                                    val isCurrent = prof.id == activeProfile.id
                                    var cardFocused by remember { mutableStateOf(false) }
                                    var penFocused by remember { mutableStateOf(false) }

                                    val cardScale by animateFloatAsState(
                                        targetValue = if (cardFocused) 1.12f else 1f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        label = "WhoWatchingCardScale"
                                    )
                                    val penScale by animateFloatAsState(
                                        targetValue = if (penFocused) 1.16f else 1f,
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                        label = "WhoWatchingPenScale"
                                    )

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Main Profile Selection Card
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .then(
                                                    if (index == 0) Modifier.focusRequester(firstProfileFocusRequester)
                                                    else Modifier
                                                )
                                                .scale(cardScale)
                                                .onFocusChanged { cardFocused = it.isFocused }
                                                .clickable {
                                                    onSelectProfile(prof)
                                                    onDismiss()
                                                }
                                                .focusable()
                                                .testTag("boot_profile_${prof.id}")
                                        ) {
                                            Box(contentAlignment = Alignment.BottomEnd) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFF1E2438),
                                                    border = BorderStroke(
                                                        width = when {
                                                            cardFocused -> 4.dp
                                                            isCurrent -> 3.dp
                                                            else -> 2.dp
                                                        },
                                                        color = when {
                                                            cardFocused -> Color.White
                                                            isCurrent -> CyanPrimary
                                                            else -> Color(0x44FFFFFF)
                                                        }
                                                    ),
                                                    shadowElevation = if (cardFocused) 22.dp else 6.dp,
                                                    modifier = Modifier.size(116.dp)
                                                ) {
                                                    TvProfileAvatarImage(
                                                        avatarUri = prof.avatarUri,
                                                        contentDescription = prof.name,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                                if (isCurrent) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = EmeraldSuccess,
                                                        border = BorderStroke(2.dp, Color(0xFF0A0D16)),
                                                        modifier = Modifier.size(26.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "Active Profile",
                                                                tint = Color(0xFF07130E),
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = prof.name,
                                                color = if (cardFocused) Color.White else Color(0xFFC8CAD8),
                                                fontSize = 18.sp,
                                                fontWeight = if (cardFocused || isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Small Pen / Edit Button Directly Underneath Each Profile
                                        Surface(
                                            shape = CircleShape,
                                            color = if (penFocused) Color.White else Color(0xFF232940),
                                            border = BorderStroke(
                                                width = if (penFocused) 2.5.dp else 1.dp,
                                                color = if (penFocused) CyanPrimary else Color(0x40FFFFFF)
                                            ),
                                            shadowElevation = if (penFocused) 12.dp else 2.dp,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .scale(penScale)
                                                .onFocusChanged { penFocused = it.isFocused }
                                                .clickable { startEditExistingProfile(prof) }
                                                .focusable()
                                                .testTag("tv_profile_edit_btn_${prof.id}")
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit ${prof.name}",
                                                    tint = if (penFocused) Color(0xFF0E121D) else CyanPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // "+ Add Profile" Card
                                if (profiles.size < 8) {
                                    item {
                                        var addFocused by remember { mutableStateOf(false) }
                                        val addScale by animateFloatAsState(
                                            targetValue = if (addFocused) 1.12f else 1f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            label = "AddProfileScale"
                                        )
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier
                                                .scale(addScale)
                                                .onFocusChanged { addFocused = it.isFocused }
                                                .clickable { startCreateNewProfile() }
                                                .focusable()
                                                .testTag("boot_add_profile_btn")
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (addFocused) Color.White else Color(0xFF1E2438),
                                                border = BorderStroke(
                                                    width = if (addFocused) 4.dp else 2.dp,
                                                    color = if (addFocused) CyanPrimary else Color(0x44FFFFFF)
                                                ),
                                                shadowElevation = if (addFocused) 20.dp else 6.dp,
                                                modifier = Modifier
                                                    .size(116.dp)
                                                    .testTag("tv_create_new_profile_card")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Add Profile",
                                                        tint = if (addFocused) Color(0xFF0E121D) else CyanPrimary,
                                                        modifier = Modifier.size(44.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = "Add Profile",
                                                color = if (addFocused) CyanPrimary else Color(0xFFC8CAD8),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ─────────────────────────────────────────────────────────────────────
                    // 2. 3-STEP CREATE PROFILE WIZARD (Step 1: Name -> Step 2: Logo -> Step 3: Settings)
                    // ─────────────────────────────────────────────────────────────────────
                    TvProfileFlowMode.CREATE_3_STEPS -> {
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xF5141828)),
                            border = BorderStroke(1.5.dp, CyanPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth(0.76f)
                                .fillMaxHeight(0.78f)
                                .testTag("tv_profile_create_wizard_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 32.dp, vertical = 26.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Step Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = when (createStep) {
                                                0 -> "Create Profile • Step 1: Name"
                                                1 -> "Create Profile • Step 2: Profile Photo"
                                                else -> "Create Profile • Step 3: Settings"
                                            },
                                            color = Color.White,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    // 3-Step Pill Indicator
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("1. Name", "2. Photo", "3. Settings").forEachIndexed { idx, label ->
                                            val active = createStep == idx
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = if (active) CyanPrimary else Color(0xFF22283C),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (active) Color.White else Color(0x33FFFFFF)
                                                )
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (active) Color(0xFF0E121D) else Color(0xFFB5B9CA),
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Step Body Content
                                AnimatedContent(
                                    targetState = createStep,
                                    transitionSpec = {
                                        fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(vertical = 22.dp),
                                    label = "CreateProfileStepBody"
                                ) { stepIdx ->
                                    when (stepIdx) {
                                        0 -> {
                                            // STEP 1: Profile Name (Uses device default keyboard — NO extra letter bar)
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                OutlinedTextField(
                                                    value = draftName,
                                                    onValueChange = { draftName = it.take(10) },
                                                    label = { Text("Profile Name") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                                    keyboardActions = KeyboardActions(
                                                        onDone = { keyboardController?.hide() }
                                                    ),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = CyanPrimary,
                                                        unfocusedBorderColor = Color(0xFF3E4766),
                                                        focusedTextColor = Color.White,
                                                        unfocusedTextColor = Color.White,
                                                        focusedContainerColor = Color(0xFF1D2233),
                                                        unfocusedContainerColor = Color(0xFF181C2A)
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.75f)
                                                        .focusRequester(nameFieldFocusRequester)
                                                        .tvKeyboardFieldTrigger(
                                                            context = context,
                                                            view = dialogView,
                                                            focusRequester = nameFieldFocusRequester,
                                                            keyboardController = dialogKeyboardController,
                                                            onOpenRnaKeyboard = { showRnaKeyboardOverlay = true }
                                                        )
                                                        .testTag("profile_name_input")
                                                )

                                                Spacer(modifier = Modifier.height(16.dp))

                                                TvDpadDialogButton(
                                                    text = "Open Keyboard",
                                                    icon = Icons.Default.Keyboard,
                                                    containerColor = Color(0xFF252B3F),
                                                    contentColor = CyanPrimary,
                                                    onClick = { triggerDefaultKeyboardForName(dialogView, dialogKeyboardController) },
                                                    testTag = "profile_open_keyboard_btn"
                                                )
                                            }
                                        }

                                        1 -> {
                                            // STEP 2: Choose Profile Photo (Upload, Emoji, or In-App Circular Static Wallpapers)
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFF20263B),
                                                    border = BorderStroke(3.5.dp, CyanPrimary),
                                                    shadowElevation = 16.dp,
                                                    modifier = Modifier.size(110.dp)
                                                ) {
                                                    TvProfileAvatarImage(
                                                        avatarUri = draftAvatarUri,
                                                        contentDescription = "Selected Profile Photo",
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(22.dp))

                                                TvDpadDialogButton(
                                                    text = "Choose Profile Photo",
                                                    icon = Icons.Default.AddPhotoAlternate,
                                                    containerColor = CyanPrimary,
                                                    contentColor = Color(0xFF0E121D),
                                                    onClick = {
                                                        photoPickerPopup = TvProfilePhotoPickerPopup.CHOOSER_3_OPTIONS
                                                    },
                                                    testTag = "choose_profile_photo_btn"
                                                )

                                                Spacer(modifier = Modifier.height(18.dp))

                                                ProfilePhotoThreeOptionRow(
                                                    onSelectUpload = {
                                                        photoPickerPopup = TvProfilePhotoPickerPopup.FILE_EXPLORER_UPLOAD
                                                    },
                                                    onSelectEmoji = {
                                                        photoPickerPopup = TvProfilePhotoPickerPopup.EMOJI_WINDOW
                                                    },
                                                    onSelectInAppWallpapers = {
                                                        photoPickerPopup = TvProfilePhotoPickerPopup.IN_APP_WALLPAPERS_WINDOW
                                                    }
                                                )
                                            }
                                        }

                                        else -> {
                                            // STEP 3: Settings (Separate Browser Data)
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                SeparateBrowserDataToggleCard(
                                                    checked = draftIsolateBrowser,
                                                    onCheckedChange = { draftIsolateBrowser = it },
                                                    modifier = Modifier.fillMaxWidth(0.85f)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Bottom Wizard Navigation
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TvDpadDialogButton(
                                        text = if (createStep == 0) "Cancel" else "Back",
                                        icon = if (createStep == 0) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                        containerColor = Color(0xFF252A3C),
                                        contentColor = Color.White,
                                        onClick = {
                                            if (createStep > 0) {
                                                createStep--
                                            } else if (initialCreateNew) {
                                                onDismiss()
                                            } else {
                                                flowMode = TvProfileFlowMode.WHO_IS_WATCHING
                                            }
                                        }
                                    )

                                    TvDpadDialogButton(
                                        text = if (createStep < 2) "Next" else "Create Profile",
                                        icon = if (createStep < 2) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                                        containerColor = CyanPrimary,
                                        contentColor = Color(0xFF0E121D),
                                        onClick = {
                                            if (createStep < 2) {
                                                createStep++
                                            } else {
                                                commitProfileSave(isNew = true)
                                            }
                                        },
                                        modifier = Modifier.focusRequester(createNextBtnFocusRequester),
                                        testTag = "tv_profile_bottom_save_btn"
                                    )
                                }
                            }
                        }
                    }

                    // ─────────────────────────────────────────────────────────────────────
                    // 3. LEFT SIDEBAR PROFILE EDIT UI (Opened by clicking the small Pen button)
                    // ─────────────────────────────────────────────────────────────────────
                    TvProfileFlowMode.EDIT_SIDEBAR -> {
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xF5131726)),
                            border = BorderStroke(1.5.dp, CyanPrimary.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth(0.84f)
                                .fillMaxHeight(0.82f)
                                .testTag("tv_profile_edit_sidebar_card")
                        ) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                // LEFT SIDEBAR: 1. Name & Logo, 2. Settings, and Apply + 'X' buttons right under the sidebar
                                Surface(
                                    color = Color(0xFF191F32),
                                    modifier = Modifier
                                        .width(250.dp)
                                        .fillMaxHeight()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(22.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF252C44),
                                                border = BorderStroke(2.dp, CyanPrimary),
                                                modifier = Modifier.size(46.dp)
                                            ) {
                                                TvProfileAvatarImage(
                                                    avatarUri = draftAvatarUri,
                                                    contentDescription = draftName,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = draftName.ifBlank { "Profile" },
                                                    color = Color.White,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Edit Profile",
                                                    color = CyanPrimary,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = Color(0x26FFFFFF))

                                        // Sidebar Option 1: Name & Logo
                                        SidebarTabButton(
                                            title = "Name & Logo",
                                            icon = Icons.Default.Person,
                                            selected = editSidebarTab == 0,
                                            onClick = { editSidebarTab = 0 },
                                            modifier = Modifier.focusRequester(sidebarFirstTabFocusRequester),
                                            testTag = "profile_sidebar_tab_name_logo"
                                        )

                                        // Sidebar Option 2: Settings
                                        SidebarTabButton(
                                            title = "Settings",
                                            icon = Icons.Default.Settings,
                                            selected = editSidebarTab == 1,
                                            onClick = { editSidebarTab = 1 },
                                            testTag = "profile_sidebar_tab_settings"
                                        )

                                        Spacer(modifier = Modifier.height(6.dp))
                                        HorizontalDivider(color = Color(0x26FFFFFF))
                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Directly under the sidebar on the left side: Apply and 'X' buttons
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TvDpadDialogButton(
                                                text = "Apply",
                                                icon = Icons.Default.Check,
                                                containerColor = CyanPrimary,
                                                contentColor = Color(0xFF0E121D),
                                                onClick = { commitProfileSave(isNew = false) },
                                                modifier = Modifier.weight(1f),
                                                testTag = "tv_profile_bottom_save_btn"
                                            )

                                            var cancelFocused by remember { mutableStateOf(false) }
                                            val cancelScale by animateFloatAsState(
                                                targetValue = if (cancelFocused) 1.08f else 1f,
                                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                label = "ProfileEditCancelXScale"
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (cancelFocused) Color.White else Color(0xFF282F46),
                                                border = BorderStroke(
                                                    width = if (cancelFocused) 2.5.dp else 1.dp,
                                                    color = if (cancelFocused) CoralAccent else Color(0x38FFFFFF)
                                                ),
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .scale(cancelScale)
                                                    .onFocusChanged { cancelFocused = it.isFocused }
                                                    .clickable {
                                                        editingProfile = null
                                                        flowMode = TvProfileFlowMode.WHO_IS_WATCHING
                                                    }
                                                    .focusable()
                                                    .testTag("profile_edit_cancel_x_btn")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Cancel",
                                                        tint = if (cancelFocused) Color(0xFF10131C) else Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // RIGHT CONTENT PANE
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(28.dp)
                                ) {
                                    AnimatedContent(
                                        targetState = editSidebarTab,
                                        transitionSpec = {
                                            fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                                        },
                                        label = "EditProfileSidebarContent"
                                    ) { tabIdx ->
                                        if (tabIdx == 0) {
                                            // TAB 0: NAME & LOGO
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.spacedBy(22.dp)
                                            ) {
                                                Text(
                                                    text = "Name & Logo",
                                                    color = Color.White,
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = Color(0xFF22283E),
                                                        border = BorderStroke(3.dp, CyanPrimary),
                                                        modifier = Modifier.size(86.dp)
                                                    ) {
                                                        TvProfileAvatarImage(
                                                            avatarUri = draftAvatarUri,
                                                            contentDescription = draftName,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    }

                                                    OutlinedTextField(
                                                        value = draftName,
                                                        onValueChange = { draftName = it.take(10) },
                                                        label = { Text("Profile Name") },
                                                        singleLine = true,
                                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                                        keyboardActions = KeyboardActions(
                                                            onDone = { keyboardController?.hide() }
                                                        ),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = CyanPrimary,
                                                            unfocusedBorderColor = Color(0xFF3E4766),
                                                            focusedTextColor = Color.White,
                                                            unfocusedTextColor = Color.White,
                                                            focusedContainerColor = Color(0xFF1D2233),
                                                            unfocusedContainerColor = Color(0xFF181C2A)
                                                        ),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .focusRequester(nameFieldFocusRequester)
                                                            .tvKeyboardFieldTrigger(
                                                                context = context,
                                                                view = dialogView,
                                                                focusRequester = nameFieldFocusRequester,
                                                                keyboardController = dialogKeyboardController,
                                                                onOpenRnaKeyboard = { showRnaKeyboardOverlay = true }
                                                            )
                                                            .testTag("profile_name_input")
                                                    )

                                                    TvDpadDialogButton(
                                                        text = "Keyboard",
                                                        icon = Icons.Default.Keyboard,
                                                        containerColor = Color(0xFF252B3F),
                                                        contentColor = CyanPrimary,
                                                        onClick = { triggerDefaultKeyboardForName(dialogView, dialogKeyboardController) }
                                                    )
                                                }

                                                TvDpadDialogButton(
                                                    text = "Choose Profile Photo",
                                                    icon = Icons.Default.AddPhotoAlternate,
                                                    containerColor = CyanPrimary,
                                                    contentColor = Color(0xFF0E121D),
                                                    onClick = {
                                                        photoPickerPopup = TvProfilePhotoPickerPopup.CHOOSER_3_OPTIONS
                                                    },
                                                    testTag = "choose_profile_photo_btn"
                                                )

                                                ProfilePhotoThreeOptionRow(
                                                    onSelectUpload = {
                                                        photoPickerPopup = TvProfilePhotoPickerPopup.FILE_EXPLORER_UPLOAD
                                                    },
                                                    onSelectEmoji = {
                                                        photoPickerPopup = TvProfilePhotoPickerPopup.EMOJI_WINDOW
                                                    },
                                                    onSelectInAppWallpapers = {
                                                        photoPickerPopup = TvProfilePhotoPickerPopup.IN_APP_WALLPAPERS_WINDOW
                                                    }
                                                )
                                            }
                                        } else {
                                            // TAB 1: SETTINGS (Only Separate Browser Data + Delete if >1 profile)
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                verticalArrangement = Arrangement.spacedBy(20.dp)
                                            ) {
                                                Text(
                                                    text = "Settings",
                                                    color = Color.White,
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )

                                                SeparateBrowserDataToggleCard(
                                                    checked = draftIsolateBrowser,
                                                    onCheckedChange = { draftIsolateBrowser = it },
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                if (editingProfile != null && profiles.size > 1) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    TvDpadDialogButton(
                                                        text = "Delete Profile",
                                                        icon = Icons.Default.DeleteOutline,
                                                        containerColor = Color(0xFF3B1D26),
                                                        contentColor = CoralAccent,
                                                        onClick = {
                                                            val target = editingProfile
                                                            editingProfile = null
                                                            flowMode = TvProfileFlowMode.WHO_IS_WATCHING
                                                            if (target != null) {
                                                                onDeleteProfile(target)
                                                            }
                                                        },
                                                        testTag = "tv_profile_edit_bottom_delete_btn"
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

            // ─────────────────────────────────────────────────────────────────────
            // POPUP WINDOWS FOR "CHOOSE PROFILE PHOTO" (3 Options: Upload, Emoji, In-App Photos)
            // ─────────────────────────────────────────────────────────────────────
            when (photoPickerPopup) {
                TvProfilePhotoPickerPopup.NONE -> {}

                TvProfilePhotoPickerPopup.CHOOSER_3_OPTIONS -> {
                    Dialog(onDismissRequest = { photoPickerPopup = TvProfilePhotoPickerPopup.NONE }) {
                        Card(
                            shape = RoundedCornerShape(26.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF171C2E)),
                            border = BorderStroke(1.5.dp, CyanPrimary),
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .testTag("profile_photo_3_options_dialog")
                        ) {
                            Column(
                                modifier = Modifier.padding(26.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Choose Profile Photo",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    TvDpadDialogButton(
                                        text = "Close",
                                        icon = Icons.Default.Close,
                                        containerColor = Color(0xFF252B3F),
                                        contentColor = Color.White,
                                        onClick = { photoPickerPopup = TvProfilePhotoPickerPopup.NONE }
                                    )
                                }

                                ProfilePhotoThreeOptionRow(
                                    onSelectUpload = {
                                        photoPickerPopup = TvProfilePhotoPickerPopup.FILE_EXPLORER_UPLOAD
                                    },
                                    onSelectEmoji = {
                                        photoPickerPopup = TvProfilePhotoPickerPopup.EMOJI_WINDOW
                                    },
                                    onSelectInAppWallpapers = {
                                        photoPickerPopup = TvProfilePhotoPickerPopup.IN_APP_WALLPAPERS_WINDOW
                                    }
                                )
                            }
                        }
                    }
                }

                TvProfilePhotoPickerPopup.FILE_EXPLORER_UPLOAD -> {
                    Dialog(
                        onDismissRequest = { photoPickerPopup = TvProfilePhotoPickerPopup.NONE },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        RnaFileExplorerScreen(
                            pickerMode = FileExplorerPickerMode.IMAGES_FOR_WALLPAPER,
                            onSelectWallpaperImage = { pickedFileUri ->
                                val persisted = TvProfilePersistenceStore.persistCustomProfileMediaUri(
                                    context = context,
                                    sourceUri = Uri.parse(pickedFileUri),
                                    prefix = "avatar"
                                )
                                draftAvatarUri = persisted
                                photoPickerPopup = TvProfilePhotoPickerPopup.NONE
                            },
                            onFilePicked = { file ->
                                val persisted = TvProfilePersistenceStore.persistCustomProfileMediaUri(
                                    context = context,
                                    sourceUri = Uri.fromFile(file),
                                    prefix = "avatar"
                                )
                                draftAvatarUri = persisted
                                photoPickerPopup = TvProfilePhotoPickerPopup.NONE
                            },
                            onClose = { photoPickerPopup = TvProfilePhotoPickerPopup.NONE }
                        )
                    }
                }

                TvProfilePhotoPickerPopup.EMOJI_WINDOW -> {
                    Dialog(
                        onDismissRequest = { photoPickerPopup = TvProfilePhotoPickerPopup.NONE },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xF5141828)),
                            border = BorderStroke(1.5.dp, CyanPrimary),
                            modifier = Modifier
                                .fillMaxWidth(0.76f)
                                .fillMaxHeight(0.76f)
                                .testTag("profile_emoji_picker_window")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(26.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Select Emoji Profile Photo",
                                        color = Color.White,
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    TvDpadDialogButton(
                                        text = "Close",
                                        icon = Icons.Default.Close,
                                        containerColor = Color(0xFF252B3F),
                                        contentColor = Color.White,
                                        onClick = { photoPickerPopup = TvProfilePhotoPickerPopup.NONE }
                                    )
                                }

                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(7),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    itemsIndexed(PRESET_PROFILE_AVATARS, key = { _, p -> p.id }) { _, preset ->
                                        val selected = resolveOfflinePresetAvatar(draftAvatarUri)?.id == preset.id
                                        var avatarFocused by remember { mutableStateOf(false) }
                                        val avatarScale by animateFloatAsState(
                                            targetValue = if (avatarFocused) 1.15f else 1f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            label = "EmojiAvatarFocusScale"
                                        )
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(64.dp)
                                                .scale(avatarScale)
                                                .onFocusChanged { avatarFocused = it.isFocused }
                                                .clip(CircleShape)
                                                .border(
                                                    width = when {
                                                        avatarFocused -> 3.5.dp
                                                        selected -> 2.5.dp
                                                        else -> 1.dp
                                                    },
                                                    color = when {
                                                        avatarFocused -> Color.White
                                                        selected -> CyanPrimary
                                                        else -> Color(0x33FFFFFF)
                                                    },
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    draftAvatarUri = preset.presetUri
                                                    photoPickerPopup = TvProfilePhotoPickerPopup.NONE
                                                }
                                                .focusable()
                                                .testTag("preset_profile_avatar_${preset.id}")
                                        ) {
                                            TvProfileAvatarImage(
                                                avatarUri = preset.presetUri,
                                                contentDescription = preset.label,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                TvProfilePhotoPickerPopup.IN_APP_WALLPAPERS_WINDOW -> {
                    Dialog(
                        onDismissRequest = { photoPickerPopup = TvProfilePhotoPickerPopup.NONE },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Card(
                            shape = RoundedCornerShape(28.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xF5141828)),
                            border = BorderStroke(1.5.dp, CyanPrimary),
                            modifier = Modifier
                                .fillMaxWidth(0.78f)
                                .fillMaxHeight(0.78f)
                                .testTag("profile_wallpapers_circular_picker_window")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(26.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "In-App Photos",
                                        color = Color.White,
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    TvDpadDialogButton(
                                        text = "Close",
                                        icon = Icons.Default.Close,
                                        containerColor = Color(0xFF252B3F),
                                        contentColor = Color.White,
                                        onClick = { photoPickerPopup = TvProfilePhotoPickerPopup.NONE }
                                    )
                                }

                                // All Static Wallpapers (not live ones) cropped in a circular shape
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(5),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(18.dp)
                                ) {
                                    itemsIndexed(PRESET_WALLPAPER_ASSET_URIS) { idx, wallpaperUri ->
                                        val selected = draftAvatarUri == wallpaperUri
                                        var itemFocused by remember { mutableStateOf(false) }
                                        val itemScale by animateFloatAsState(
                                            targetValue = if (itemFocused) 1.14f else 1f,
                                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            label = "CircularWallpaperAvatarScale"
                                        )
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(92.dp)
                                                .scale(itemScale)
                                                .onFocusChanged { itemFocused = it.isFocused }
                                                .clip(CircleShape)
                                                .border(
                                                    width = when {
                                                        itemFocused -> 3.5.dp
                                                        selected -> 2.5.dp
                                                        else -> 1.5.dp
                                                    },
                                                    color = when {
                                                        itemFocused -> Color.White
                                                        selected -> CyanPrimary
                                                        else -> Color(0x40FFFFFF)
                                                    },
                                                    shape = CircleShape
                                                )
                                                .clickable {
                                                    draftAvatarUri = wallpaperUri
                                                    photoPickerPopup = TvProfilePhotoPickerPopup.NONE
                                                }
                                                .focusable()
                                                .testTag("in_app_circular_wallpaper_avatar_$idx")
                                        ) {
                                            AsyncImage(
                                                model = wallpaperUri,
                                                contentDescription = "Wallpaper Avatar $idx",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Only show the custom RNA Keyboard Overlay if RNA TV Keyboard is set as the system default IME
            RnaTvKeyboardOverlay(
                visible = showRnaKeyboardOverlay,
                currentText = draftName,
                fieldHint = "Profile Name",
                showTextPreviewBar = true,
                onTextChange = { draftName = it.take(10) },
                onCommitChar = { draftName = (draftName + it).take(10) },
                onBackspace = {
                    if (draftName.isNotEmpty()) {
                        draftName = draftName.dropLast(1)
                    }
                },
                onSearchOrSubmit = { showRnaKeyboardOverlay = false },
                onDismiss = { showRnaKeyboardOverlay = false }
            )
        }
    }
}

@Composable
private fun ProfilePhotoThreeOptionRow(
    onSelectUpload: () -> Unit,
    onSelectEmoji: () -> Unit,
    onSelectInAppWallpapers: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ProfilePhotoOptionCard(
            title = "Upload",
            icon = Icons.Default.FolderOpen,
            accentColor = CyanPrimary,
            onClick = onSelectUpload,
            modifier = Modifier.weight(1f),
            testTag = "profile_photo_option_upload"
        )
        ProfilePhotoOptionCard(
            title = "Emoji",
            icon = Icons.Default.EmojiEmotions,
            accentColor = AmberAccent,
            onClick = onSelectEmoji,
            modifier = Modifier.weight(1f),
            testTag = "profile_photo_option_emoji"
        )
        ProfilePhotoOptionCard(
            title = "In-App Photos",
            icon = Icons.Default.Wallpaper,
            accentColor = VioletSecondary,
            onClick = onSelectInAppWallpapers,
            modifier = Modifier.weight(1f),
            testTag = "profile_photo_option_wallpapers"
        )
    }
}

@Composable
private fun ProfilePhotoOptionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ProfilePhotoOptionScale"
    )
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isFocused) Color.White else Color(0xFF1E2438),
        border = BorderStroke(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = if (isFocused) accentColor else Color(0x38FFFFFF)
        ),
        modifier = modifier
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (isFocused) Color(0xFF121624) else accentColor.copy(alpha = 0.18f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                color = if (isFocused) Color(0xFF10131C) else Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SidebarTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "SidebarTabBtnScale"
    )
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = when {
            isFocused -> Color.White
            selected -> CyanPrimary.copy(alpha = 0.22f)
            else -> Color(0xFF21283E)
        },
        border = BorderStroke(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = when {
                isFocused -> CyanPrimary
                selected -> CyanPrimary
                else -> Color(0x28FFFFFF)
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = when {
                    isFocused -> Color(0xFF10131C)
                    selected -> CyanPrimary
                    else -> Color.White
                },
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                color = when {
                    isFocused -> Color(0xFF10131C)
                    selected -> CyanPrimary
                    else -> Color.White
                },
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun SeparateBrowserDataToggleCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "SeparateBrowserDataScale"
    )
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isFocused) Color(0xFF252D44) else Color(0xFF1B2030),
        border = BorderStroke(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = if (isFocused) Color.White else Color(0x28FFFFFF)
        ),
        modifier = modifier
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onCheckedChange(!checked) }
            .focusable()
            .testTag("profile_separate_browser_data_toggle")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = EmeraldSuccess
                )
                Text(
                    text = "Separate Browser Data",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = { onCheckedChange(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF10131C),
                    checkedTrackColor = EmeraldSuccess
                )
            )
        }
    }
}

@Composable
private fun TvDpadDialogButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.06f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "TvDpadBtnScale"
    )
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isFocused) Color.White else containerColor,
        border = BorderStroke(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = if (isFocused) CyanPrimary else Color(0x38FFFFFF)
        ),
        modifier = modifier
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isFocused) Color(0xFF10131C) else contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = if (isFocused) Color(0xFF10131C) else contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}
