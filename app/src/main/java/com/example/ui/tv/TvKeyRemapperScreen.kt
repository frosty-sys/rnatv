package com.example.ui.tv

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.example.R
import com.example.RnaHomeInterceptorService
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VioletSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

const val PREF_KEY_REMAPPED_KEYS_JSON = "rna_tv_remapped_keys_v2"
const val PREF_KEY_PROMPTED_OVERLAY_PERM = "rna_tv_prompted_overlay_perm_v1"

enum class KeyPressGestureType(
    val id: String,
    val title: String,
    val subtitle: String,
    val shortBadge: String
) {
    SINGLE_PRESS(
        id = "SINGLE_PRESS",
        title = "Single Press",
        subtitle = "Runs on a normal quick tap of this button",
        shortBadge = "1x"
    ),
    DOUBLE_PRESS(
        id = "DOUBLE_PRESS",
        title = "Double Press",
        subtitle = "Runs when you press this button twice quickly",
        shortBadge = "2x"
    ),
    TRIPLE_PRESS(
        id = "TRIPLE_PRESS",
        title = "Triple Press",
        subtitle = "Runs when you press this button three times quickly",
        shortBadge = "3x"
    ),
    LONG_PRESS(
        id = "LONG_PRESS",
        title = "Long Press",
        subtitle = "Runs when you press and hold this button (~0.5s)",
        shortBadge = "Hold"
    )
}

enum class RemappedKeyActionType(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String
) {
    NONE_SYSTEM_DEFAULT(
        id = "NONE_SYSTEM_DEFAULT",
        title = "None (System Default)",
        subtitle = "No custom override — lets Android TV perform the button's default behavior",
        category = "Default"
    ),
    RNA_HOME(
        id = "RNA_HOME",
        title = "RNA Home Button",
        subtitle = "Opens RNA Launcher immediately or returns to Home tab from Apps/Browser",
        category = "Core Navigation"
    ),
    RECENT_APPS_DRAWER(
        id = "RECENT_APPS_DRAWER",
        title = "Recent Apps Drawer",
        subtitle = "Opens the Recent Apps drawer over any app (uses Usage Access)",
        category = "System Overlays"
    ),
    QUICK_APPS_TASKBAR(
        id = "QUICK_APPS_TASKBAR",
        title = "Small All-Apps Taskbar (Dark Mode)",
        subtitle = "Pops up a sleek dark-mode taskbar with all apps at the bottom of the screen over any app",
        category = "System Overlays"
    ),
    OPEN_APP(
        id = "OPEN_APP",
        title = "Open Any App",
        subtitle = "Launches a specific installed TV app, RNA AI, or RNA web app",
        category = "Apps"
    ),
    DPAD_UP(
        id = "DPAD_UP",
        title = "D-Pad Up",
        subtitle = "Sends directional Up navigation",
        category = "D-Pad & Remote"
    ),
    DPAD_DOWN(
        id = "DPAD_DOWN",
        title = "D-Pad Down",
        subtitle = "Sends directional Down navigation",
        category = "D-Pad & Remote"
    ),
    DPAD_LEFT(
        id = "DPAD_LEFT",
        title = "D-Pad Left",
        subtitle = "Sends directional Left navigation",
        category = "D-Pad & Remote"
    ),
    DPAD_RIGHT(
        id = "DPAD_RIGHT",
        title = "D-Pad Right",
        subtitle = "Sends directional Right navigation",
        category = "D-Pad & Remote"
    ),
    DPAD_OK(
        id = "DPAD_OK",
        title = "D-Pad OK / Select",
        subtitle = "Clicks or activates the currently focused item",
        category = "D-Pad & Remote"
    ),
    BACK(
        id = "BACK",
        title = "Back Button",
        subtitle = "Performs system Back navigation",
        category = "D-Pad & Remote"
    ),
    LIVE_VOICE_AI(
        id = "LIVE_VOICE_AI",
        title = "RNA Live Voice AI",
        subtitle = "Opens the EdVanced 1.1 Pro Voice Assistant bar over any app",
        category = "AI & Browser"
    ),
    AUTONOMOUS_AGENT(
        id = "AUTONOMOUS_AGENT",
        title = "RNA Autonomous Agent",
        subtitle = "Starts full-screen Autonomous Agent mode over any app",
        category = "AI & Browser"
    ),
    RNA_AI_APP(
        id = "RNA_AI_APP",
        title = "Open RNA AI App",
        subtitle = "Launches the standalone full-screen RNA AI chat application",
        category = "AI & Browser"
    ),
    RNA_BROWSER(
        id = "RNA_BROWSER",
        title = "Open RNA Browser",
        subtitle = "Opens the built-in full-screen RNA TV Browser",
        category = "AI & Browser"
    ),
    VOLUME_UP(
        id = "VOLUME_UP",
        title = "Volume Up",
        subtitle = "Increases TV media volume",
        category = "Media & Audio"
    ),
    VOLUME_DOWN(
        id = "VOLUME_DOWN",
        title = "Volume Down",
        subtitle = "Decreases TV media volume",
        category = "Media & Audio"
    ),
    VOLUME_MUTE(
        id = "VOLUME_MUTE",
        title = "Mute / Unmute Audio",
        subtitle = "Toggles TV media audio mute",
        category = "Media & Audio"
    ),
    MEDIA_PLAY_PAUSE(
        id = "MEDIA_PLAY_PAUSE",
        title = "Play / Pause Media",
        subtitle = "Toggles media playback",
        category = "Media & Audio"
    );

    companion object {
        fun fromId(id: String): RemappedKeyActionType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NONE_SYSTEM_DEFAULT
        }
    }
}

data class KeyGestureAction(
    val actionType: RemappedKeyActionType = RemappedKeyActionType.NONE_SYSTEM_DEFAULT,
    val targetPackage: String = "",
    val targetAppTitle: String = ""
) {
    val isAssigned: Boolean
        get() = actionType != RemappedKeyActionType.NONE_SYSTEM_DEFAULT

    fun displaySummary(): String = when {
        actionType == RemappedKeyActionType.NONE_SYSTEM_DEFAULT -> "None (System Default)"
        actionType == RemappedKeyActionType.OPEN_APP && targetAppTitle.isNotBlank() -> "Open App: $targetAppTitle"
        else -> actionType.title
    }
}

data class RemappedKeyBinding(
    val keyCode: Int,
    val keyLabel: String,
    val actionType: RemappedKeyActionType = RemappedKeyActionType.NONE_SYSTEM_DEFAULT,
    val targetPackage: String = "",
    val targetAppTitle: String = "",
    val enabled: Boolean = true,
    val singlePress: KeyGestureAction = KeyGestureAction(actionType, targetPackage, targetAppTitle),
    val doublePress: KeyGestureAction = KeyGestureAction(),
    val triplePress: KeyGestureAction = KeyGestureAction(),
    val longPress: KeyGestureAction = KeyGestureAction()
) {
    fun actionFor(gesture: KeyPressGestureType): KeyGestureAction = when (gesture) {
        KeyPressGestureType.SINGLE_PRESS -> singlePress
        KeyPressGestureType.DOUBLE_PRESS -> doublePress
        KeyPressGestureType.TRIPLE_PRESS -> triplePress
        KeyPressGestureType.LONG_PRESS -> longPress
    }

    fun withGestureAction(gesture: KeyPressGestureType, action: KeyGestureAction): RemappedKeyBinding {
        return when (gesture) {
            KeyPressGestureType.SINGLE_PRESS -> copy(
                actionType = action.actionType,
                targetPackage = action.targetPackage,
                targetAppTitle = action.targetAppTitle,
                singlePress = action
            )
            KeyPressGestureType.DOUBLE_PRESS -> copy(doublePress = action)
            KeyPressGestureType.TRIPLE_PRESS -> copy(triplePress = action)
            KeyPressGestureType.LONG_PRESS -> copy(longPress = action)
        }
    }

    fun hasMultiGestureConfigured(): Boolean {
        return doublePress.isAssigned || triplePress.isAssigned || longPress.isAssigned
    }

    fun hasAnyAssignedAction(): Boolean {
        return singlePress.isAssigned || doublePress.isAssigned || triplePress.isAssigned || longPress.isAssigned
    }
}

/**
 * Global bridge between AccessibilityService hardware key interception, MainActivity, and the Key Remapper UI.
 */
object KeyRemapperBridge {
    @Volatile
    var isCapturingKey: Boolean = false

    var onKeyCapturedCallback: ((Int) -> Unit)? = null

    // Invoked when the user presses their remapped Home button while already inside MainActivity
    // (e.g. on the Apps tab or Browser tab) so it immediately switches to the Home tab (tvTab = 0)!
    var onNavigateToHomeTabInMainActivity: (() -> Unit)? = null

    fun notifyNavigateToHomeTabInMainActivity() {
        onNavigateToHomeTabInMainActivity?.invoke()
    }

    var onOpenRecentAppsInMainActivity: (() -> Unit)? = null
    var onOpenQuickTaskbarInMainActivity: (() -> Unit)? = null

    fun notifyKeyCaptured(keyCode: Int): Boolean {
        if (!isCapturingKey) return false
        val cb = onKeyCapturedCallback ?: return false
        cb.invoke(keyCode)
        return true
    }

    fun formatKeyCodeName(keyCode: Int): String {
        val raw = KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
        return when (keyCode) {
            KeyEvent.KEYCODE_HOME -> "HOME Button (Key $keyCode)"
            KeyEvent.KEYCODE_MENU -> "MENU Button (Key $keyCode)"
            KeyEvent.KEYCODE_GUIDE -> "GUIDE Button (Key $keyCode)"
            KeyEvent.KEYCODE_INFO -> "INFO Button (Key $keyCode)"
            KeyEvent.KEYCODE_TV -> "TV Button (Key $keyCode)"
            KeyEvent.KEYCODE_TV_INPUT -> "INPUT Button (Key $keyCode)"
            KeyEvent.KEYCODE_BOOKMARK -> "BOOKMARK / FAVORITE (Key $keyCode)"
            KeyEvent.KEYCODE_CHANNEL_UP -> "CHANNEL UP (Key $keyCode)"
            KeyEvent.KEYCODE_CHANNEL_DOWN -> "CHANNEL DOWN (Key $keyCode)"
            KeyEvent.KEYCODE_PROG_RED -> "RED Button (Key $keyCode)"
            KeyEvent.KEYCODE_PROG_GREEN -> "GREEN Button (Key $keyCode)"
            KeyEvent.KEYCODE_PROG_YELLOW -> "YELLOW Button (Key $keyCode)"
            KeyEvent.KEYCODE_PROG_BLUE -> "BLUE Button (Key $keyCode)"
            else -> "$raw (Key $keyCode)"
        }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun requestUsageStatsPermission(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (_: Exception) {
                Toast.makeText(context, "Enable Usage Access for RNA in Android Settings > Security & Restrictions", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    fun ensureOverlayPermissionOnFirstLiveUse(context: Context, prefs: SharedPreferences): Boolean {
        if (hasOverlayPermission(context)) return true
        val alreadyPrompted = prefs.getBoolean(PREF_KEY_PROMPTED_OVERLAY_PERM, false)
        if (!alreadyPrompted) {
            prefs.edit().putBoolean(PREF_KEY_PROMPTED_OVERLAY_PERM, true).apply()
            requestOverlayPermission(context)
            return false
        }
        return false
    }

    fun requestOverlayPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Toast.makeText(
                    context,
                    "Allow 'Display over other apps' so RNA Live AI & Overlays can work on top of any app",
                    Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) {
                try {
                    val fallback = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallback)
                } catch (_: Exception) {
                    Toast.makeText(
                        context,
                        "Enable 'Display over other apps' for RNA in Android Settings > Apps > Special app access",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}

private fun parseGestureAction(obj: JSONObject?): KeyGestureAction {
    if (obj == null) return KeyGestureAction()
    return KeyGestureAction(
        actionType = RemappedKeyActionType.fromId(obj.optString("actionType", "NONE_SYSTEM_DEFAULT")),
        targetPackage = obj.optString("targetPackage", ""),
        targetAppTitle = obj.optString("targetAppTitle", "")
    )
}

private fun KeyGestureAction.toJson(): JSONObject = JSONObject().apply {
    put("actionType", actionType.id)
    put("targetPackage", targetPackage)
    put("targetAppTitle", targetAppTitle)
}

fun loadRemappedKeyBindings(prefs: SharedPreferences): List<RemappedKeyBinding> {
    val raw = prefs.getString(PREF_KEY_REMAPPED_KEYS_JSON, null)
    if (raw.isNullOrBlank()) {
        return emptyList()
    }
    return try {
        val arr = JSONArray(raw)
        val out = mutableListOf<RemappedKeyBinding>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val keyCode = obj.optInt("keyCode", -1)
            if (keyCode <= 0) continue
            val legacyActionType = RemappedKeyActionType.fromId(obj.optString("actionType", "NONE_SYSTEM_DEFAULT"))
            val legacyPkg = obj.optString("targetPackage", "")
            val legacyTitle = obj.optString("targetAppTitle", "")
            val single = if (obj.has("singlePress")) {
                parseGestureAction(obj.optJSONObject("singlePress"))
            } else {
                KeyGestureAction(legacyActionType, legacyPkg, legacyTitle)
            }
            val doubleA = parseGestureAction(obj.optJSONObject("doublePress"))
            val tripleA = parseGestureAction(obj.optJSONObject("triplePress"))
            val longA = parseGestureAction(obj.optJSONObject("longPress"))
            out.add(
                RemappedKeyBinding(
                    keyCode = keyCode,
                    keyLabel = obj.optString("keyLabel", KeyRemapperBridge.formatKeyCodeName(keyCode)),
                    actionType = single.actionType,
                    targetPackage = single.targetPackage,
                    targetAppTitle = single.targetAppTitle,
                    enabled = obj.optBoolean("enabled", true),
                    singlePress = single,
                    doublePress = doubleA,
                    triplePress = tripleA,
                    longPress = longA
                )
            )
        }
        out
    } catch (_: Exception) {
        emptyList()
    }
}

fun saveRemappedKeyBindings(prefs: SharedPreferences, list: List<RemappedKeyBinding>) {
    val arr = JSONArray()
    list.forEach { item ->
        arr.put(
            JSONObject().apply {
                put("keyCode", item.keyCode)
                put("keyLabel", item.keyLabel)
                put("actionType", item.singlePress.actionType.id)
                put("targetPackage", item.singlePress.targetPackage)
                put("targetAppTitle", item.singlePress.targetAppTitle)
                put("enabled", item.enabled)
                put("singlePress", item.singlePress.toJson())
                put("doublePress", item.doublePress.toJson())
                put("triplePress", item.triplePress.toJson())
                put("longPress", item.longPress.toJson())
            }
        )
    }
    prefs.edit().putString(PREF_KEY_REMAPPED_KEYS_JSON, arr.toString()).apply()
}

/**
 * Full-Screen TV Quick Actions Pro-Style Button Remapper Screen.
 * Allows capturing the user's remote Home button (or any button on the remote) and remapping it to:
 * - RNA Home (opens RNA Launcher with zero delay or returns to Home tab from Apps/Browser)
 * - Recent Apps Drawer (with Usage Access permission check)
 * - Small Dark-Mode All-Apps Taskbar Underneath (on top of any app)
 * - Open Any App (from the full list of installed TV apps, RNA AI, and RNA web apps)
 * - D-Pad Up / Down / Left / Right / OK / Back / Media / Voice AI / Agent
 */
@Composable
fun TvKeyRemapperScreen(
    prefs: SharedPreferences,
    allApps: List<UnifiedLauncherApp>,
    onTestExecuteAction: (RemappedKeyBinding) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var bindings by remember { mutableStateOf(loadRemappedKeyBindings(prefs)) }

    var showCaptureKeyModal by remember { mutableStateOf(false) }
    var captureForHomeQuickSetup by remember { mutableStateOf(false) }
    var capturedKeyCodeForConfig by remember { mutableIntStateOf(-1) }
    var showSelectGestureModal by remember { mutableStateOf(false) }
    var activeGestureForConfig by remember { mutableStateOf(KeyPressGestureType.SINGLE_PRESS) }
    var showSelectActionModal by remember { mutableStateOf(false) }
    var showSelectAppModal by remember { mutableStateOf(false) }

    var isAccessibilityActive by remember {
        mutableStateOf(RnaHomeInterceptorService.isAccessibilityServiceEnabled(context))
    }
    var hasUsageAccess by remember {
        mutableStateOf(KeyRemapperBridge.hasUsageStatsPermission(context))
    }
    var hasOverlayAccess by remember {
        mutableStateOf(KeyRemapperBridge.hasOverlayPermission(context))
    }

    LaunchedEffect(Unit) {
        while (true) {
            isAccessibilityActive = RnaHomeInterceptorService.isAccessibilityServiceEnabled(context)
            hasUsageAccess = KeyRemapperBridge.hasUsageStatsPermission(context)
            hasOverlayAccess = KeyRemapperBridge.hasOverlayPermission(context)
            delay(1500L)
        }
    }

    fun upsertBinding(newBinding: RemappedKeyBinding) {
        val updated = bindings.filterNot { it.keyCode == newBinding.keyCode } + newBinding
        bindings = updated
        saveRemappedKeyBindings(prefs, updated)
    }

    fun deleteBinding(keyCode: Int) {
        val updated = bindings.filterNot { it.keyCode == keyCode }
        bindings = updated
        saveRemappedKeyBindings(prefs, updated)
    }

    BackHandler(enabled = true) {
        when {
            showSelectAppModal -> showSelectAppModal = false
            showSelectActionModal -> showSelectActionModal = false
            showCaptureKeyModal -> {
                KeyRemapperBridge.isCapturingKey = false
                KeyRemapperBridge.onKeyCapturedCallback = null
                showCaptureKeyModal = false
            }
            else -> onDismiss()
        }
    }

    val addKeyFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(80L)
        runCatching { addKeyFocusRequester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0C0F1A), Color(0xFF121626), Color(0xFF0A0C14))
                )
            )
            .testTag("tv_key_remapper_fullscreen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp, vertical = 26.dp)
        ) {
            // ── Top Header Bar ──
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
                        shape = CircleShape,
                        color = CyanPrimary.copy(alpha = 0.2f),
                        border = BorderStroke(1.5.dp, CyanPrimary),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Gamepad,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Remapper",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    var addBtnFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            captureForHomeQuickSetup = false
                            showCaptureKeyModal = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (addBtnFocused) Color.White else CyanPrimary,
                            contentColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .focusRequester(addKeyFocusRequester)
                            .onFocusChanged { addBtnFocused = it.isFocused }
                            .testTag("remapper_add_key_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Key", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }

                    var closeBtnFocused by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.5.dp, if (closeBtnFocused) Color.White else Color(0x44FFFFFF)),
                        modifier = Modifier
                            .onFocusChanged { closeBtnFocused = it.isFocused }
                            .testTag("remapper_close_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Close", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Permission & Quick Home Button Remap Cards ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 1: Instant Home Button Capture & Remap (Replaces delayed window detection!)
                val hasHomeRemap = bindings.any { it.actionType == RemappedKeyActionType.RNA_HOME && it.enabled }
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF181D30),
                    border = BorderStroke(1.5.dp, if (hasHomeRemap) EmeraldSuccess else CyanPrimary),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = if (hasHomeRemap) EmeraldSuccess else CyanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (hasHomeRemap)
                                        "Remote Home Button Remapped to RNA"
                                    else
                                        "Remap Your Remote's Home Button",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Press your remote's Home button to capture and remap it directly into the RNA button so the stock launcher never opens.",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.5.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        var capHomeFocused by remember { mutableStateOf(false) }
                        Button(
                            onClick = {
                                captureForHomeQuickSetup = true
                                showCaptureKeyModal = true
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (capHomeFocused) Color.White else (if (hasHomeRemap) EmeraldSuccess else CyanPrimary),
                                contentColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .onFocusChanged { capHomeFocused = it.isFocused }
                                .testTag("remapper_capture_home_btn")
                        ) {
                            Text(
                                text = if (hasHomeRemap) "Re-Capture Home Key" else "Capture Home Key",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Card 2: Required System Permissions Status (Accessibility, Usage Access, Display Over Other Apps)
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF181D30),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "PERMISSIONS FOR REMAPPING & OVERLAYS",
                            color = CyanPrimary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.7.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PermissionChipButton(
                                label = if (isAccessibilityActive) "Key Service: ON" else "Enable Key Service",
                                granted = isAccessibilityActive,
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        })
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            PermissionChipButton(
                                label = if (hasUsageAccess) "Usage Access: ON" else "Grant Usage Access",
                                granted = hasUsageAccess,
                                onClick = { KeyRemapperBridge.requestUsageStatsPermission(context) },
                                modifier = Modifier.weight(1f)
                            )
                            PermissionChipButton(
                                label = if (hasOverlayAccess) "Overlay: ON" else "Grant Overlay",
                                granted = hasOverlayAccess,
                                onClick = { KeyRemapperBridge.requestOverlayPermission(context) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "ACTIVE REMAPPED REMOTE BUTTONS (${bindings.size})",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (bindings.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFF151928),
                    border = BorderStroke(1.dp, Color(0x24FFFFFF)),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Remote Buttons Remapped Yet",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Click 'Capture Home Key' above to remap your remote's Home button to RNA Home, or click '+ Add Key' to remap any button to Recent Apps, the Dark-Mode Quick Taskbar, D-Pad keys, or any installed app.",
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(620.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("remapper_bindings_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(bindings, key = { it.keyCode }) { item ->
                        var rowFocused by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (rowFocused) Color(0xFF242B45) else Color(0xFF161A2B),
                            border = BorderStroke(
                                width = if (rowFocused) 2.dp else 1.dp,
                                color = if (rowFocused) Color.White else CyanPrimary.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { rowFocused = it.isFocused }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = CyanPrimary.copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, CyanPrimary)
                                    ) {
                                        Text(
                                            text = item.keyLabel,
                                            color = CyanPrimary,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }

                                    Text(
                                        text = "➔",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        val configuredSummary = KeyPressGestureType.entries
                                            .filter { item.actionFor(it).isAssigned }
                                            .joinToString("  •  ") { g ->
                                                "${g.shortBadge}: ${item.actionFor(g).displaySummary()}"
                                            }
                                        Text(
                                            text = configuredSummary.ifBlank { "All gestures set to None (System Default)" },
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 14.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            KeyPressGestureType.entries.forEach { gesture ->
                                                val assigned = item.actionFor(gesture).isAssigned
                                                Surface(
                                                    shape = RoundedCornerShape(7.dp),
                                                    color = if (assigned) CyanPrimary.copy(alpha = 0.2f) else Color(0xFF21273C),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (assigned) CyanPrimary.copy(alpha = 0.8f) else Color(0x22FFFFFF)
                                                    )
                                                ) {
                                                    Text(
                                                        text = "${gesture.shortBadge}: ${if (assigned) "Set" else "Default"}",
                                                        color = if (assigned) CyanPrimary else Color(0xFF94A3B8),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            capturedKeyCodeForConfig = item.keyCode
                                            showSelectGestureModal = true
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Edit Presses", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                                    }

                                    Switch(
                                        checked = item.enabled,
                                        onCheckedChange = { checked ->
                                            upsertBinding(item.copy(enabled = checked))
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF0F172A),
                                            checkedTrackColor = EmeraldSuccess
                                        )
                                    )

                                    IconButton(
                                        onClick = { deleteBinding(item.keyCode) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Mapping",
                                            tint = Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Helper to update a specific press gesture on a binding without wiping its other gestures
        fun updateGestureOnKey(
            keyCode: Int,
            gesture: KeyPressGestureType,
            action: KeyGestureAction
        ) {
            val existing = bindings.firstOrNull { it.keyCode == keyCode } ?: RemappedKeyBinding(
                keyCode = keyCode,
                keyLabel = KeyRemapperBridge.formatKeyCodeName(keyCode),
                enabled = true
            )
            upsertBinding(existing.withGestureAction(gesture, action).copy(enabled = true))
        }

        // ── MODAL 1: Press Any Remote Button to Capture ──
        if (showCaptureKeyModal) {
            CaptureRemoteKeyDialog(
                isHomeQuickSetup = captureForHomeQuickSetup,
                onKeyCaptured = { keyCode ->
                    KeyRemapperBridge.isCapturingKey = false
                    KeyRemapperBridge.onKeyCapturedCallback = null
                    showCaptureKeyModal = false
                    if (captureForHomeQuickSetup) {
                        updateGestureOnKey(
                            keyCode = keyCode,
                            gesture = KeyPressGestureType.SINGLE_PRESS,
                            action = KeyGestureAction(RemappedKeyActionType.RNA_HOME)
                        )
                        Toast.makeText(
                            context,
                            "Remapped ${KeyRemapperBridge.formatKeyCodeName(keyCode)} (Single Press) to RNA Home!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        capturedKeyCodeForConfig = keyCode
                        showSelectGestureModal = true
                    }
                },
                onDismiss = {
                    KeyRemapperBridge.isCapturingKey = false
                    KeyRemapperBridge.onKeyCapturedCallback = null
                    showCaptureKeyModal = false
                }
            )
        }

        // ── MODAL 1.5: Choose Press Type (Single Press / Double Press / Triple Press / Long Press) ──
        if (showSelectGestureModal && capturedKeyCodeForConfig > 0) {
            val currentBinding = bindings.firstOrNull { it.keyCode == capturedKeyCodeForConfig } ?: RemappedKeyBinding(
                keyCode = capturedKeyCodeForConfig,
                keyLabel = KeyRemapperBridge.formatKeyCodeName(capturedKeyCodeForConfig),
                enabled = true
            )
            SelectPressGestureDialog(
                binding = currentBinding,
                onSelectGesture = { gesture ->
                    activeGestureForConfig = gesture
                    showSelectGestureModal = false
                    showSelectActionModal = true
                },
                onTestGesture = { gesture ->
                    val act = currentBinding.actionFor(gesture)
                    if (act.isAssigned) {
                        onTestExecuteAction(
                            currentBinding.copy(
                                actionType = act.actionType,
                                targetPackage = act.targetPackage,
                                targetAppTitle = act.targetAppTitle,
                                singlePress = act
                            )
                        )
                    } else {
                        Toast.makeText(context, "${gesture.title} uses System Default", Toast.LENGTH_SHORT).show()
                    }
                },
                onDismiss = { showSelectGestureModal = false }
            )
        }

        // ── MODAL 2: Select What the Chosen Gesture Does (Includes None / System Default) ──
        if (showSelectActionModal && capturedKeyCodeForConfig > 0) {
            SelectRemapActionDialog(
                keyCode = capturedKeyCodeForConfig,
                gestureLabel = activeGestureForConfig.title,
                onSelectAction = { chosenAction ->
                    when (chosenAction) {
                        RemappedKeyActionType.OPEN_APP -> {
                            showSelectActionModal = false
                            showSelectAppModal = true
                        }
                        RemappedKeyActionType.RECENT_APPS_DRAWER -> {
                            if (!KeyRemapperBridge.hasUsageStatsPermission(context)) {
                                Toast.makeText(
                                    context,
                                    "Please grant Usage Access so RNA can show Recent Apps",
                                    Toast.LENGTH_LONG
                                ).show()
                                KeyRemapperBridge.requestUsageStatsPermission(context)
                            }
                            if (!KeyRemapperBridge.hasOverlayPermission(context)) {
                                KeyRemapperBridge.requestOverlayPermission(context)
                            }
                            updateGestureOnKey(
                                capturedKeyCodeForConfig,
                                activeGestureForConfig,
                                KeyGestureAction(chosenAction)
                            )
                            showSelectActionModal = false
                            showSelectGestureModal = true
                        }
                        RemappedKeyActionType.QUICK_APPS_TASKBAR -> {
                            if (!KeyRemapperBridge.hasOverlayPermission(context)) {
                                KeyRemapperBridge.requestOverlayPermission(context)
                            }
                            updateGestureOnKey(
                                capturedKeyCodeForConfig,
                                activeGestureForConfig,
                                KeyGestureAction(chosenAction)
                            )
                            showSelectActionModal = false
                            showSelectGestureModal = true
                        }
                        else -> {
                            updateGestureOnKey(
                                capturedKeyCodeForConfig,
                                activeGestureForConfig,
                                KeyGestureAction(chosenAction)
                            )
                            showSelectActionModal = false
                            showSelectGestureModal = true
                        }
                    }
                },
                onDismiss = {
                    showSelectActionModal = false
                    showSelectGestureModal = true
                }
            )
        }

        // ── MODAL 3: Pick Any Installed App for "Open Any App" ──
        if (showSelectAppModal && capturedKeyCodeForConfig > 0) {
            SelectAppForKeyRemapDialog(
                allApps = allApps,
                onSelectApp = { app ->
                    updateGestureOnKey(
                        capturedKeyCodeForConfig,
                        activeGestureForConfig,
                        KeyGestureAction(
                            actionType = RemappedKeyActionType.OPEN_APP,
                            targetPackage = app.packageName,
                            targetAppTitle = app.title
                        )
                    )
                    showSelectAppModal = false
                    showSelectGestureModal = true
                    Toast.makeText(
                        context,
                        "${KeyRemapperBridge.formatKeyCodeName(capturedKeyCodeForConfig)} (${activeGestureForConfig.title}) -> ${app.title}",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onDismiss = {
                    showSelectAppModal = false
                    showSelectGestureModal = true
                }
            )
        }
    }
}

@Composable
private fun PermissionChipButton(
    label: String,
    granted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = when {
            focused -> Color.White
            granted -> EmeraldSuccess.copy(alpha = 0.16f)
            else -> AmberAccent.copy(alpha = 0.16f)
        },
        border = BorderStroke(
            1.dp,
            if (focused) Color.White else if (granted) EmeraldSuccess else AmberAccent
        ),
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .focusable()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (granted) Icons.Default.Check else Icons.Default.Security,
                contentDescription = null,
                tint = if (focused) Color(0xFF0F172A) else if (granted) EmeraldSuccess else AmberAccent,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = if (focused) Color(0xFF0F172A) else Color.White,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CaptureRemoteKeyDialog(
    isHomeQuickSetup: Boolean,
    onKeyCaptured: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val captureBoxFocusRequester = remember { FocusRequester() }

    DisposableEffect(Unit) {
        KeyRemapperBridge.isCapturingKey = true
        KeyRemapperBridge.onKeyCapturedCallback = { code ->
            onKeyCaptured(code)
        }
        onDispose {
            KeyRemapperBridge.isCapturingKey = false
            KeyRemapperBridge.onKeyCapturedCallback = null
        }
    }

    LaunchedEffect(Unit) {
        delay(60L)
        runCatching { captureBoxFocusRequester.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF5121626)),
            border = BorderStroke(2.dp, CyanPrimary),
            modifier = Modifier
                .width(620.dp)
                .focusRequester(captureBoxFocusRequester)
                .focusable()
                .onPreviewKeyEvent { ev ->
                    if (ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                        val code = ev.nativeKeyEvent.keyCode
                        // Allow Back to cancel the modal unless they hold it; capture any other key!
                        if (code != KeyEvent.KEYCODE_BACK &&
                            code != KeyEvent.KEYCODE_DPAD_CENTER &&
                            code != KeyEvent.KEYCODE_ENTER
                        ) {
                            onKeyCaptured(code)
                            return@onPreviewKeyEvent true
                        }
                    }
                    false
                }
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = CyanPrimary.copy(alpha = 0.2f),
                    border = BorderStroke(1.5.dp, CyanPrimary),
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isHomeQuickSetup) Icons.Default.Home else Icons.Default.Gamepad,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isHomeQuickSetup)
                        "Press Your Remote's Home Button Now"
                    else
                        "Press Any Button on Your Remote Now",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isHomeQuickSetup)
                        "Listening for your remote's Home key... As soon as you press it, RNA will capture that key and remap it directly into the RNA Home button so the system launcher never opens."
                    else
                        "Press the remote button you want to remap (or select a preset hardware key below).",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Preset buttons in case the user wants to bind HOME, MENU, GUIDE, TV, BOOKMARK, or COLOR keys with D-Pad OK
                val presetKeys = listOf(
                    KeyEvent.KEYCODE_HOME to "HOME Key",
                    KeyEvent.KEYCODE_MENU to "MENU Key",
                    KeyEvent.KEYCODE_GUIDE to "GUIDE Key",
                    KeyEvent.KEYCODE_TV to "TV Key",
                    KeyEvent.KEYCODE_INFO to "INFO Key",
                    KeyEvent.KEYCODE_BOOKMARK to "BOOKMARK Key",
                    KeyEvent.KEYCODE_PROG_RED to "RED Key",
                    KeyEvent.KEYCODE_PROG_GREEN to "GREEN Key",
                    KeyEvent.KEYCODE_PROG_YELLOW to "YELLOW Key",
                    KeyEvent.KEYCODE_PROG_BLUE to "BLUE Key"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presetKeys) { (code, label) ->
                        var chipFocused by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (chipFocused) Color.White else Color(0xFF1E243B),
                            border = BorderStroke(1.dp, if (chipFocused) CyanPrimary else Color(0x33FFFFFF)),
                            modifier = Modifier
                                .onFocusChanged { chipFocused = it.isFocused }
                                .clickable { onKeyCaptured(code) }
                                .focusable()
                        ) {
                            Text(
                                text = label,
                                color = if (chipFocused) Color(0xFF0F172A) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SelectPressGestureDialog(
    binding: RemappedKeyBinding,
    onSelectGesture: (KeyPressGestureType) -> Unit,
    onTestGesture: (KeyPressGestureType) -> Unit,
    onDismiss: () -> Unit
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(70L)
        runCatching { firstFocus.requestFocus() }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF8121626)),
            border = BorderStroke(2.dp, CyanPrimary),
            modifier = Modifier
                .width(680.dp)
                .testTag("select_press_gesture_dialog")
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Configure ${binding.keyLabel}",
                            color = Color.White,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Pick a press type below to assign an action (or None / System Default):",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp
                        )
                    }
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(14.dp)) {
                        Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    KeyPressGestureType.entries.forEachIndexed { idx, gesture ->
                        val assignedAction = binding.actionFor(gesture)
                        var focused by remember { mutableStateOf(false) }
                        val accent = when (gesture) {
                            KeyPressGestureType.SINGLE_PRESS -> CyanPrimary
                            KeyPressGestureType.DOUBLE_PRESS -> VioletSecondary
                            KeyPressGestureType.TRIPLE_PRESS -> AmberAccent
                            KeyPressGestureType.LONG_PRESS -> EmeraldSuccess
                        }
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (focused) Color.White else Color(0xFF1A2036),
                            border = BorderStroke(
                                width = if (focused) 2.5.dp else 1.dp,
                                color = if (focused) accent else if (assignedAction.isAssigned) accent.copy(alpha = 0.65f) else Color(0x28FFFFFF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (idx == 0) Modifier.focusRequester(firstFocus) else Modifier)
                                .onFocusChanged { focused = it.isFocused }
                                .clickable { onSelectGesture(gesture) }
                                .testTag("press_gesture_option_${gesture.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (focused) Color(0xFF0F172A) else accent.copy(alpha = 0.22f),
                                        border = BorderStroke(1.dp, accent),
                                        modifier = Modifier.width(58.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = gesture.shortBadge,
                                                color = if (focused) Color.White else accent,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = gesture.title,
                                            color = if (focused) Color(0xFF0F172A) else Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.5.sp
                                        )
                                        Text(
                                            text = assignedAction.displaySummary(),
                                            color = if (focused) Color(0xFF334155) else if (assignedAction.isAssigned) accent else Color(0xFF94A3B8),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (focused) accent else Color(0xFF242C48)
                                ) {
                                    Text(
                                        text = "Select",
                                        color = if (focused) Color(0xFF090D16) else Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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
private fun SelectRemapActionDialog(
    keyCode: Int,
    gestureLabel: String = "Single Press",
    onSelectAction: (RemappedKeyActionType) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val firstActionFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(80L)
            runCatching { firstActionFocus.requestFocus() }
        }
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF5121626)),
            border = BorderStroke(1.5.dp, CyanPrimary),
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .fillMaxHeight(0.84f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$gestureLabel • ${KeyRemapperBridge.formatKeyCodeName(keyCode)}",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Select what this press gesture should do (or choose None for System Default):",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp
                        )
                    }
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(14.dp)) {
                        Text("Back", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(RemappedKeyActionType.entries.size) { index ->
                        val actionType = RemappedKeyActionType.entries[index]
                        var focused by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (focused) Color.White else Color(0xFF1A1F33),
                            border = BorderStroke(
                                width = if (focused) 2.dp else 1.dp,
                                color = if (focused) CyanPrimary else Color(0x28FFFFFF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (index == 0) Modifier.focusRequester(firstActionFocus) else Modifier)
                                .onFocusChanged { focused = it.isFocused }
                                .clickable { onSelectAction(actionType) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = actionType.title,
                                        color = if (focused) Color(0xFF0F172A) else Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = actionType.subtitle,
                                        color = if (focused) Color(0xFF334155) else Color(0xFF94A3B8),
                                        fontSize = 12.sp
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (focused) Color(0xFF0F172A) else CyanPrimary.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = actionType.category,
                                        color = if (focused) Color.White else CyanPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
private fun SelectAppForKeyRemapDialog(
    allApps: List<UnifiedLauncherApp>,
    onSelectApp: (UnifiedLauncherApp) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val firstAppFocus = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(80L)
            runCatching { firstAppFocus.requestFocus() }
        }
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF5121626)),
            border = BorderStroke(1.5.dp, CyanPrimary),
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .fillMaxHeight(0.84f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Select App to Open",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Choose any installed app, RNA AI, or RNA web app:",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp
                        )
                    }
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(14.dp)) {
                        Text("Back", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allApps.size, key = { allApps[it].uniqueKey }) { index ->
                        val app = allApps[index]
                        var focused by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (focused) Color.White else Color(0xFF1A1F33),
                            border = BorderStroke(
                                width = if (focused) 2.dp else 1.dp,
                                color = if (focused) CyanPrimary else Color(0x28FFFFFF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (index == 0) Modifier.focusRequester(firstAppFocus) else Modifier)
                                .onFocusChanged { focused = it.isFocused }
                                .clickable { onSelectApp(app) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                ) {
                                    RemapperAppIconThumb(app = app)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.title,
                                        color = if (focused) Color(0xFF0F172A) else Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = app.packageName,
                                        color = if (focused) Color(0xFF334155) else Color(0xFF94A3B8),
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

@Composable
fun RemapperAppIconThumb(app: UnifiedLauncherApp) {
    val bmp = remember(app.uniqueKey, app.cachedBitmap) {
        app.cachedBitmap
            ?: app.customLogoUri?.let { TvAppRamCache.iconBitmaps[it] }
            ?: TvAppRamCache.iconBitmaps[app.packageName]
            ?: app.nativeIcon?.let {
                runCatching { it.toBitmap(96, 96).asImageBitmap() }.getOrNull()
            }
    }
    when {
        app.packageName == RNAAI_PACKAGE_NAME -> {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground_img),
                contentDescription = app.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        bmp != null -> {
            Image(
                bitmap = bmp,
                contentDescription = app.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        !app.customLogoUri.isNullOrBlank() -> {
            AsyncImage(
                model = app.customLogoUri,
                contentDescription = app.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        else -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF282D46))
            ) {
                Text(
                    text = app.title.take(2).uppercase(),
                    color = CyanPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Remappable Overlay 1: Recent Apps Drawer (Uses Usage Access permission to show recently used apps).
 */
@Composable
fun TvRecentAppsDrawerOverlay(
    allApps: List<UnifiedLauncherApp>,
    onLaunchApp: (UnifiedLauncherApp) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasUsageAccess by remember {
        mutableStateOf(KeyRemapperBridge.hasUsageStatsPermission(context))
    }
    var recentAppsList by remember { mutableStateOf<List<UnifiedLauncherApp>>(emptyList()) }
    val firstFocusRequester = remember { FocusRequester() }

    LaunchedEffect(hasUsageAccess, allApps) {
        withContext(Dispatchers.IO) {
            val usageMap = if (hasUsageAccess) {
                try {
                    val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                    val now = System.currentTimeMillis()
                    usm?.queryAndAggregateUsageStats(now - 7L * 24 * 3600 * 1000L, now)
                        ?.mapValues { it.value.lastTimeUsed }
                        ?: emptyMap()
                } catch (_: Exception) {
                    emptyMap()
                }
            } else {
                emptyMap()
            }

            val merged = allApps.map { app ->
                val sysTs = usageMap[app.packageName] ?: 0L
                val bestTs = maxOf(app.lastUsedTimestamp, sysTs)
                app.copy(lastUsedTimestamp = bestTs)
            }
            val withUsage = merged.filter { it.lastUsedTimestamp > 0L }.sortedByDescending { it.lastUsedTimestamp }
            recentAppsList = if (withUsage.isNotEmpty()) withUsage.take(16) else merged.take(12)
        }
        delay(80L)
        runCatching { firstFocusRequester.requestFocus() }
    }

    BackHandler(enabled = true) {
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x9906080E))
            .clickable { onDismiss() }
            .testTag("tv_recent_apps_drawer_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xF5101422),
            border = BorderStroke(1.5.dp, CyanPrimary.copy(alpha = 0.7f)),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                        Column {
                            Text(
                                text = "Recent Apps Drawer",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (hasUsageAccess)
                                    "Sorted by real-time Android TV Usage Access activity"
                                else
                                    "Grant Usage Access to track recently used apps across your TV",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!hasUsageAccess) {
                            Button(
                                onClick = {
                                    KeyRemapperBridge.requestUsageStatsPermission(context)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AmberAccent,
                                    contentColor = Color(0xFF0F172A)
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Grant Usage Access", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(14.dp)) {
                            Text("Close", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(recentAppsList.size, key = { idx -> recentAppsList[idx].uniqueKey }) { idx ->
                        val app = recentAppsList[idx]
                        var itemFocused by remember { mutableStateOf(false) }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(104.dp)
                                .then(if (idx == 0) Modifier.focusRequester(firstFocusRequester) else Modifier)
                                .onFocusChanged { itemFocused = it.isFocused }
                                .clickable {
                                    onLaunchApp(app)
                                }
                                .focusable()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF1D2236),
                                border = BorderStroke(
                                    width = if (itemFocused) 3.dp else 1.5.dp,
                                    color = if (itemFocused) Color.White else CyanPrimary.copy(alpha = 0.45f)
                                ),
                                shadowElevation = if (itemFocused) 16.dp else 4.dp,
                                modifier = Modifier.size(76.dp)
                            ) {
                                RemapperAppIconThumb(app = app)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = app.title,
                                color = if (itemFocused) Color.White else Color(0xFFE2E8F0),
                                fontWeight = if (itemFocused) FontWeight.ExtraBold else FontWeight.Medium,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Remappable Overlay 2: Small Dark-Mode All-Apps Taskbar Underneath (Appears at the bottom of the screen over any app).
 */
@Composable
fun TvQuickAppsTaskbarOverlay(
    allApps: List<UnifiedLauncherApp>,
    onLaunchApp: (UnifiedLauncherApp) -> Unit,
    onDismiss: () -> Unit
) {
    val firstFocusRequester = remember { FocusRequester() }

    LaunchedEffect(allApps) {
        delay(60L)
        runCatching { firstFocusRequester.requestFocus() }
    }

    BackHandler(enabled = true) {
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable { onDismiss() }
            .testTag("tv_quick_apps_taskbar_overlay"),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp, bottomStart = 22.dp, bottomEnd = 22.dp),
            color = Color(0xF20A0D16),
            border = BorderStroke(1.5.dp, Color(0x44BAC3FF)),
            shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(bottom = 16.dp)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Quick Apps Taskbar",
                            color = Color(0xFFE2E8F0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Press OK to launch • Press Back to hide",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allApps.size, key = { idx -> allApps[idx].uniqueKey }) { idx ->
                        val app = allApps[idx]
                        var focused by remember { mutableStateOf(false) }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(72.dp)
                                .then(if (idx == 0) Modifier.focusRequester(firstFocusRequester) else Modifier)
                                .onFocusChanged { focused = it.isFocused }
                                .clickable { onLaunchApp(app) }
                                .focusable()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF161B2C),
                                border = BorderStroke(
                                    width = if (focused) 2.5.dp else 1.dp,
                                    color = if (focused) Color.White else Color(0x33FFFFFF)
                                ),
                                shadowElevation = if (focused) 12.dp else 2.dp,
                                modifier = Modifier.size(52.dp)
                            ) {
                                RemapperAppIconThumb(app = app)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = app.title,
                                color = if (focused) Color.White else Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                fontWeight = if (focused) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
