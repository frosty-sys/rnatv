package com.example.ui.tv

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.MainActivity
import com.example.RnaAiActivity
import com.example.RnaHomeInterceptorService
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VioletSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

const val PREF_KEY_TRIGGERS_MACROS_JSON = "rna_tv_triggers_macros_v1"
const val KEY_OPEN_RNA_AT_BOOT = "control_open_rna_at_boot"
private const val PREF_KEY_LAST_HANDLED_BOOT_COUNT = "rna_last_handled_boot_count_v1"

enum class RnaTriggerType(val id: String, val label: String) {
    DEVICE_BOOT("DEVICE_BOOT", "When Device Turns On"),
    SCREEN_ON("SCREEN_ON", "When Screen Turns On"),
    APP_OPEN("APP_OPEN", "When App Opens");

    companion object {
        fun fromId(id: String): RnaTriggerType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: APP_OPEN
        }
    }
}

enum class RnaTriggerActionType(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String
) {
    BOOT_WARM_RNA_TV(
        id = "BOOT_WARM_RNA_TV",
        title = "Open RNA. TV & Return to Previous App",
        subtitle = "Opens RNA. TV on boot, then immediately reopens the last non-launcher app unless Open RNA TV at Boot is on",
        category = "System"
    ),
    OPEN_PREVIOUS_APP(
        id = "OPEN_PREVIOUS_APP",
        title = "Open Previous App",
        subtitle = "Reopens the previously opened app if it is not the Android TV launcher",
        category = "Navigation"
    ),
    SET_VOLUME_PERCENT(
        id = "SET_VOLUME_PERCENT",
        title = "Set Volume to Exact %",
        subtitle = "Adjusts TV media volume to a specific percentage (e.g., 21%)",
        category = "Audio"
    ),
    OPEN_APP(
        id = "OPEN_APP",
        title = "Open Any App",
        subtitle = "Launches any installed TV app, RNA AI, or web app",
        category = "Apps"
    ),
    PRESS_BUTTON(
        id = "PRESS_BUTTON",
        title = "Press Remote / Hardware Button",
        subtitle = "Sends Back, Home, OK, D-Pad, Play/Pause, Mute, or Volume keys",
        category = "Remote"
    ),
    RNA_HOME(
        id = "RNA_HOME",
        title = "Open RNA. TV Home",
        subtitle = "Brings RNA. TV Launcher to the foreground",
        category = "Navigation"
    ),
    QUICK_APPS_TASKBAR(
        id = "QUICK_APPS_TASKBAR",
        title = "Open Quick Apps Taskbar",
        subtitle = "Shows the dark-mode mini all-apps taskbar at the bottom",
        category = "Overlays"
    ),
    RECENT_APPS_DRAWER(
        id = "RECENT_APPS_DRAWER",
        title = "Open Recent Apps Drawer",
        subtitle = "Opens the Recent Apps switcher overlay",
        category = "Overlays"
    ),
    LIVE_VOICE_AI(
        id = "LIVE_VOICE_AI",
        title = "Open EdVanced Voice AI",
        subtitle = "Launches the RNA Voice Assistant overlay",
        category = "AI"
    ),
    AUTONOMOUS_AGENT(
        id = "AUTONOMOUS_AGENT",
        title = "Start Autonomous Agent",
        subtitle = "Activates the RNA Autonomous Agent overlay",
        category = "AI"
    ),
    RNA_AI_APP(
        id = "RNA_AI_APP",
        title = "Open RNA. AI App",
        subtitle = "Launches the full-screen RNA AI chat suite",
        category = "AI"
    ),
    RNA_BROWSER(
        id = "RNA_BROWSER",
        title = "Open RNA. Browser",
        subtitle = "Opens the built-in TV web browser",
        category = "Navigation"
    ),
    DELAY_MS(
        id = "DELAY_MS",
        title = "Wait / Delay",
        subtitle = "Pauses before running the next action in the chain",
        category = "Flow"
    );

    companion object {
        fun fromId(id: String): RnaTriggerActionType {
            if (id.equals("WARM_RNA_BACKGROUND", ignoreCase = true)) {
                return BOOT_WARM_RNA_TV
            }
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: RNA_HOME
        }
    }
}

data class RnaTriggerActionItem(
    val id: String = UUID.randomUUID().toString(),
    val type: RnaTriggerActionType,
    val targetPackage: String = "",
    val targetLabel: String = "",
    val keyCode: Int = 0,
    val volumePercent: Int = 35,
    val delayMs: Long = 1000L
) {
    fun displayTitle(): String = when (type) {
        RnaTriggerActionType.SET_VOLUME_PERCENT -> "Set Volume to ${volumePercent.coerceIn(0, 100)}%"
        RnaTriggerActionType.OPEN_APP -> "Open App: ${targetLabel.ifBlank { targetPackage.ifBlank { "App" } }}"
        RnaTriggerActionType.PRESS_BUTTON -> "Press Button: ${formatTriggerButtonLabel(keyCode)}"
        RnaTriggerActionType.DELAY_MS -> {
            val sec = delayMs / 1000.0
            if (delayMs % 1000L == 0L) "Wait ${delayMs / 1000L}s" else String.format(Locale.US, "Wait %.1fs", sec)
        }
        else -> type.title
    }

    fun displaySubtitle(): String = when (type) {
        RnaTriggerActionType.SET_VOLUME_PERCENT -> "Media stream volume automatically set to ${volumePercent.coerceIn(0, 100)}%"
        RnaTriggerActionType.OPEN_APP -> targetPackage.ifBlank { type.subtitle }
        RnaTriggerActionType.PRESS_BUTTON -> "Dispatches remote key event (KeyCode $keyCode)"
        RnaTriggerActionType.DELAY_MS -> "Delays $delayMs ms before executing the following action"
        else -> type.subtitle
    }
}

data class RnaTriggerRule(
    val id: String,
    val type: RnaTriggerType,
    val title: String,
    val targetPackage: String = "",
    val targetAppLabel: String = "",
    val enabled: Boolean = true,
    val isBuiltIn: Boolean = false,
    val actions: List<RnaTriggerActionItem> = emptyList()
)

private val TRIGGER_BUTTON_PRESETS: List<Pair<Int, String>> = listOf(
    KeyEvent.KEYCODE_BACK to "Back",
    KeyEvent.KEYCODE_HOME to "Home",
    KeyEvent.KEYCODE_DPAD_CENTER to "OK / Select",
    KeyEvent.KEYCODE_DPAD_UP to "D-Pad Up",
    KeyEvent.KEYCODE_DPAD_DOWN to "D-Pad Down",
    KeyEvent.KEYCODE_DPAD_LEFT to "D-Pad Left",
    KeyEvent.KEYCODE_DPAD_RIGHT to "D-Pad Right",
    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to "Play / Pause",
    KeyEvent.KEYCODE_VOLUME_MUTE to "Mute / Unmute",
    KeyEvent.KEYCODE_VOLUME_UP to "Volume Up",
    KeyEvent.KEYCODE_VOLUME_DOWN to "Volume Down",
    KeyEvent.KEYCODE_MENU to "Menu"
)

fun formatTriggerButtonLabel(keyCode: Int): String {
    return TRIGGER_BUTTON_PRESETS.firstOrNull { it.first == keyCode }?.second
        ?: KeyRemapperBridge.formatKeyCodeName(keyCode)
}

object RnaTriggersStore {
    const val DEFAULT_BOOT_ID = "default_device_boot"
    const val DEFAULT_SCREEN_ON_ID = "default_screen_on"

    private fun defaultSeedRules(): List<RnaTriggerRule> = listOf(
        RnaTriggerRule(
            id = DEFAULT_BOOT_ID,
            type = RnaTriggerType.DEVICE_BOOT,
            title = "When Device Turns On",
            enabled = true,
            isBuiltIn = true,
            actions = listOf(
                RnaTriggerActionItem(
                    id = "default_boot_warm_rna",
                    type = RnaTriggerActionType.BOOT_WARM_RNA_TV
                )
            )
        ),
        RnaTriggerRule(
            id = DEFAULT_SCREEN_ON_ID,
            type = RnaTriggerType.SCREEN_ON,
            title = "When Screen Turns On",
            enabled = true,
            isBuiltIn = true,
            actions = emptyList()
        )
    )

    fun loadRules(prefs: SharedPreferences): List<RnaTriggerRule> {
        val raw = prefs.getString(PREF_KEY_TRIGGERS_MACROS_JSON, null)
        if (raw.isNullOrBlank()) {
            val seeded = defaultSeedRules()
            saveRules(prefs, seeded)
            return seeded
        }
        val parsed = runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<RnaTriggerRule>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id", UUID.randomUUID().toString())
                val type = RnaTriggerType.fromId(obj.optString("type", "APP_OPEN"))
                val title = obj.optString("title", type.label)
                val targetPackage = obj.optString("targetPackage", "")
                val targetAppLabel = obj.optString("targetAppLabel", "")
                val enabled = obj.optBoolean("enabled", true)
                val isBuiltIn = obj.optBoolean("isBuiltIn", id == DEFAULT_BOOT_ID || id == DEFAULT_SCREEN_ON_ID)
                val actionsArr = obj.optJSONArray("actions") ?: JSONArray()
                val actions = mutableListOf<RnaTriggerActionItem>()
                for (j in 0 until actionsArr.length()) {
                    val aObj = actionsArr.optJSONObject(j) ?: continue
                    actions.add(
                        RnaTriggerActionItem(
                            id = aObj.optString("id", UUID.randomUUID().toString()),
                            type = RnaTriggerActionType.fromId(aObj.optString("type", "RNA_HOME")),
                            targetPackage = aObj.optString("targetPackage", ""),
                            targetLabel = aObj.optString("targetLabel", ""),
                            keyCode = aObj.optInt("keyCode", 0),
                            volumePercent = aObj.optInt("volumePercent", 35).coerceIn(0, 100),
                            delayMs = aObj.optLong("delayMs", 1000L).coerceAtLeast(100L)
                        )
                    )
                }
                list.add(
                    RnaTriggerRule(
                        id = id,
                        type = type,
                        title = title,
                        targetPackage = targetPackage,
                        targetAppLabel = targetAppLabel,
                        enabled = enabled,
                        isBuiltIn = isBuiltIn,
                        actions = actions
                    )
                )
            }
            list
        }.getOrDefault(emptyList())

        // Always guarantee the 2 default system triggers exist at the top
        val out = parsed.toMutableList()
        if (out.none { it.id == DEFAULT_BOOT_ID || it.type == RnaTriggerType.DEVICE_BOOT }) {
            out.add(0, defaultSeedRules()[0])
        }
        if (out.none { it.id == DEFAULT_SCREEN_ON_ID || it.type == RnaTriggerType.SCREEN_ON }) {
            val insertIdx = if (out.isNotEmpty()) 1 else 0
            out.add(insertIdx, defaultSeedRules()[1])
        }
        return out
    }

    fun saveRules(prefs: SharedPreferences, rules: List<RnaTriggerRule>) {
        val arr = JSONArray()
        rules.forEach { rule ->
            val actionsArr = JSONArray()
            rule.actions.forEach { action ->
                actionsArr.put(
                    JSONObject().apply {
                        put("id", action.id)
                        put("type", action.type.id)
                        put("targetPackage", action.targetPackage)
                        put("targetLabel", action.targetLabel)
                        put("keyCode", action.keyCode)
                        put("volumePercent", action.volumePercent)
                        put("delayMs", action.delayMs)
                    }
                )
            }
            arr.put(
                JSONObject().apply {
                    put("id", rule.id)
                    put("type", rule.type.id)
                    put("title", rule.title)
                    put("targetPackage", rule.targetPackage)
                    put("targetAppLabel", rule.targetAppLabel)
                    put("enabled", rule.enabled)
                    put("isBuiltIn", rule.isBuiltIn)
                    put("actions", actionsArr)
                }
            )
        }
        prefs.edit().putString(PREF_KEY_TRIGGERS_MACROS_JSON, arr.toString()).commit()
    }

    fun upsertRule(prefs: SharedPreferences, rule: RnaTriggerRule): List<RnaTriggerRule> {
        val current = loadRules(prefs).toMutableList()
        val idx = current.indexOfFirst { it.id == rule.id }
        if (idx >= 0) {
            current[idx] = rule
        } else {
            current.add(rule)
        }
        saveRules(prefs, current)
        return current
    }

    fun deleteRule(prefs: SharedPreferences, ruleId: String): List<RnaTriggerRule> {
        val current = loadRules(prefs).filterNot { it.id == ruleId && !it.isBuiltIn }
        saveRules(prefs, current)
        return current
    }

    fun findOrCreateAppTrigger(
        prefs: SharedPreferences,
        packageName: String,
        appLabel: String
    ): Pair<List<RnaTriggerRule>, RnaTriggerRule> {
        val rules = loadRules(prefs)
        val existing = rules.firstOrNull {
            it.type == RnaTriggerType.APP_OPEN && it.targetPackage.equals(packageName, ignoreCase = true)
        }
        if (existing != null) return rules to existing
        val created = RnaTriggerRule(
            id = "app_trigger_${packageName.lowercase(Locale.US)}",
            type = RnaTriggerType.APP_OPEN,
            title = "When $appLabel Opens",
            targetPackage = packageName,
            targetAppLabel = appLabel,
            enabled = true,
            isBuiltIn = false,
            actions = emptyList()
        )
        val updated = upsertRule(prefs, created)
        return updated to created
    }

    fun buildPromptSummary(prefs: SharedPreferences): String {
        val rules = loadRules(prefs)
        if (rules.isEmpty()) return "None"
        return rules.joinToString("\n") { rule ->
            val actionsDesc = if (rule.actions.isEmpty()) {
                "No actions yet"
            } else {
                rule.actions.mapIndexed { idx, a -> "${idx + 1}. ${a.displayTitle()}" }.joinToString(" -> ")
            }
            val targetKey = when (rule.type) {
                RnaTriggerType.DEVICE_BOOT -> "device_boot"
                RnaTriggerType.SCREEN_ON -> "screen_on"
                RnaTriggerType.APP_OPEN -> "app:${rule.targetPackage}"
            }
            "  - ${rule.title} ($targetKey) [${if (rule.enabled) "ON" else "OFF"}]: $actionsDesc"
        }
    }
}

object RnaTriggersExecutor {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile
    private var hasHandledBootInProcess: Boolean = false
    @Volatile
    private var pendingBootExecution: Boolean = false

    fun isBootExecutionPending(): Boolean = pendingBootExecution

    fun handleDeviceBootOnce(context: Context, fromAccessibilityService: Boolean = false) {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
        val currentBootCount = runCatching {
            Settings.Global.getInt(appCtx.contentResolver, Settings.Global.BOOT_COUNT, -1)
        }.getOrDefault(-1)

        if (hasHandledBootInProcess) return
        if (currentBootCount > 0 && prefs.getInt(PREF_KEY_LAST_HANDLED_BOOT_COUNT, -1) == currentBootCount) {
            hasHandledBootInProcess = true
            pendingBootExecution = false
            return
        }

        val canLaunchActivitiesInBg = fromAccessibilityService ||
            RnaHomeInterceptorService.instance != null ||
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            runCatching { Settings.canDrawOverlays(appCtx) }.getOrDefault(false)

        if (!canLaunchActivitiesInBg) {
            if (!pendingBootExecution) {
                pendingBootExecution = true
                // Fallback if AccessibilityService is disabled on this device so non-UI triggers still run
                mainHandler.postDelayed({
                    if (pendingBootExecution && !hasHandledBootInProcess) {
                        executeBootFlowNow(appCtx, prefs, currentBootCount)
                    }
                }, 5000L)
            }
            return
        }

        executeBootFlowNow(appCtx, prefs, currentBootCount)
    }

    private fun executeBootFlowNow(
        appCtx: Context,
        prefs: SharedPreferences,
        currentBootCount: Int
    ) {
        if (hasHandledBootInProcess) return
        hasHandledBootInProcess = true
        pendingBootExecution = false
        if (currentBootCount > 0) {
            prefs.edit().putInt(PREF_KEY_LAST_HANDLED_BOOT_COUNT, currentBootCount).commit()
        }

        val bootRules = RnaTriggersStore.loadRules(prefs).filter {
            it.enabled && it.type == RnaTriggerType.DEVICE_BOOT
        }
        val handledByBootRule = bootRules.any { rule ->
            rule.actions.any {
                it.type == RnaTriggerActionType.BOOT_WARM_RNA_TV ||
                    it.type == RnaTriggerActionType.RNA_HOME
            }
        }

        bootRules.forEach { rule ->
            if (rule.actions.isNotEmpty()) {
                executeRuleActions(appCtx, rule, isBootTrigger = true)
            }
        }

        if (!handledByBootRule) {
            scope.launch {
                performBootWarmupRnaTv(appCtx, isBootTrigger = true)
            }
        }
    }

    fun executeTriggersForType(context: Context, triggerType: RnaTriggerType) {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
        val matching = RnaTriggersStore.loadRules(prefs).filter { it.enabled && it.type == triggerType }
        matching.forEach { rule ->
            if (rule.actions.isNotEmpty()) {
                executeRuleActions(appCtx, rule, isBootTrigger = triggerType == RnaTriggerType.DEVICE_BOOT)
            }
        }
    }

    fun executeTriggersForAppOpened(context: Context, openedPackage: String) {
        if (openedPackage.isBlank()) return
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
        val matching = RnaTriggersStore.loadRules(prefs).filter {
            it.enabled &&
                it.type == RnaTriggerType.APP_OPEN &&
                it.targetPackage.equals(openedPackage, ignoreCase = true)
        }
        matching.forEach { rule ->
            if (rule.actions.isNotEmpty()) {
                executeRuleActions(appCtx, rule, isBootTrigger = false)
            }
        }
    }

    fun executeRuleActions(
        context: Context,
        rule: RnaTriggerRule,
        isBootTrigger: Boolean = false
    ) {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
        scope.launch {
            for (action in rule.actions) {
                when (action.type) {
                    RnaTriggerActionType.DELAY_MS -> {
                        delay(action.delayMs.coerceIn(100L, 30_000L))
                    }
                    RnaTriggerActionType.BOOT_WARM_RNA_TV -> {
                        performBootWarmupRnaTv(appCtx, isBootTrigger = isBootTrigger)
                        delay(250L)
                    }
                    RnaTriggerActionType.OPEN_PREVIOUS_APP -> {
                        openPreviousNonLauncherApp(appCtx)
                        delay(250L)
                    }
                    RnaTriggerActionType.SET_VOLUME_PERCENT -> {
                        mainHandler.post {
                            applyExactVolumePercent(appCtx, action.volumePercent)
                        }
                        delay(120L)
                    }
                    RnaTriggerActionType.PRESS_BUTTON -> {
                        mainHandler.post {
                            dispatchTriggerButton(appCtx, action.keyCode)
                        }
                        delay(180L)
                    }
                    RnaTriggerActionType.OPEN_APP -> {
                        if (action.targetPackage.isNotBlank()) {
                            mainHandler.post {
                                val service = RnaHomeInterceptorService.instance
                                if (service != null) {
                                    service.launchAppPackageFromService(action.targetPackage)
                                } else {
                                    val pm = appCtx.packageManager
                                    val intent = pm.getLeanbackLaunchIntentForPackage(action.targetPackage)
                                        ?: pm.getLaunchIntentForPackage(action.targetPackage)
                                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    if (intent != null) runCatching { appCtx.startActivity(intent) }
                                }
                            }
                            delay(250L)
                        }
                    }
                    RnaTriggerActionType.RNA_HOME -> {
                        mainHandler.post {
                            val service = RnaHomeInterceptorService.instance
                            if (service != null) {
                                service.launchRnaTvHomeFromService()
                            } else {
                                launchRnaTvForeground(appCtx)
                            }
                        }
                        delay(200L)
                    }
                    RnaTriggerActionType.QUICK_APPS_TASKBAR -> {
                        mainHandler.post {
                            RnaHomeInterceptorService.instance?.launchTaskbarOverlayFromService()
                                ?: KeyRemapperBridge.onOpenQuickTaskbarInMainActivity?.invoke()
                        }
                        delay(200L)
                    }
                    RnaTriggerActionType.RECENT_APPS_DRAWER -> {
                        mainHandler.post {
                            RnaHomeInterceptorService.instance?.launchRecentAppsOverlayFromService()
                                ?: KeyRemapperBridge.onOpenRecentAppsInMainActivity?.invoke()
                        }
                        delay(200L)
                    }
                    RnaTriggerActionType.LIVE_VOICE_AI -> {
                        mainHandler.post {
                            KeyRemapperBridge.ensureOverlayPermissionOnFirstLiveUse(appCtx, prefs)
                            RnaHomeInterceptorService.instance?.launchRnaAssistantOverlayFromTrigger()
                        }
                        delay(200L)
                    }
                    RnaTriggerActionType.AUTONOMOUS_AGENT -> {
                        mainHandler.post {
                            KeyRemapperBridge.ensureOverlayPermissionOnFirstLiveUse(appCtx, prefs)
                            RnaAutonomousAgentController.enterAgentMode()
                        }
                        delay(200L)
                    }
                    RnaTriggerActionType.RNA_AI_APP -> {
                        mainHandler.post {
                            runCatching {
                                appCtx.startActivity(
                                    Intent(appCtx, RnaAiActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                    }
                                )
                            }
                        }
                        delay(200L)
                    }
                    RnaTriggerActionType.RNA_BROWSER -> {
                        mainHandler.post {
                            runCatching {
                                appCtx.startActivity(
                                    Intent(appCtx, MainActivity::class.java).apply {
                                        addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                                        )
                                        putExtra(MainActivity.EXTRA_OPEN_TV_LAUNCHER, true)
                                        putExtra(MainActivity.EXTRA_OPEN_TV_BROWSER_URL, "https://www.google.com")
                                    }
                                )
                            }
                        }
                        delay(200L)
                    }
                }
            }
        }
    }

    /**
     * Opens RNA. TV at boot to warm the Activity and launcher state:
     * - When "Open RNA TV at Boot" is enabled: opens RNA. TV directly and stays on RNA. TV.
     * - When "Open RNA TV at Boot" is disabled (default): opens RNA. TV and immediately reopens
     *   the last opened app right at that instant if it is not the Android TV launcher.
     */
    suspend fun performBootWarmupRnaTv(context: Context, isBootTrigger: Boolean = false) {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
        val openRnaAtBoot = prefs.getBoolean(KEY_OPEN_RNA_AT_BOOT, false)

        runCatching {
            TvProfilePersistenceStore.ensureProfilesSyncedOnBootOrUpdate(appCtx)
        }

        if (openRnaAtBoot) {
            mainHandler.post {
                val service = RnaHomeInterceptorService.instance
                if (service != null) {
                    service.launchRnaTvForBootWarmup(returnToPrevious = false, returnPackage = "")
                } else {
                    launchRnaTvForeground(appCtx)
                }
            }
            return
        }

        var previousNonLauncherPkg = resolvePreviousNonLauncherPackage(
            context = appCtx,
            allowFallbackWhenInRna = !isBootTrigger
        )
        if (isBootTrigger && previousNonLauncherPkg.isBlank()) {
            val liveRoot = runCatching {
                RnaHomeInterceptorService.instance?.rootInActiveWindow?.packageName?.toString().orEmpty()
            }.getOrDefault("")
            if (!RnaHomeInterceptorService.isStockTvLauncherPackage(liveRoot)) {
                // Give OEM Live TV / HDMI input a brief moment to attach its window right after BOOT_COMPLETED
                delay(1200L)
                previousNonLauncherPkg = resolvePreviousNonLauncherPackage(
                    context = appCtx,
                    allowFallbackWhenInRna = false
                )
            }
        }
        val shouldReturnToPrevious = previousNonLauncherPkg.isNotBlank() &&
            RnaHomeInterceptorService.isEligiblePreviousAppPackage(appCtx, previousNonLauncherPkg)

        mainHandler.post {
            val service = RnaHomeInterceptorService.instance
            if (service != null) {
                service.launchRnaTvForBootWarmup(
                    returnToPrevious = shouldReturnToPrevious,
                    returnPackage = previousNonLauncherPkg
                )
            } else {
                launchRnaTvForBootWarmupFallback(
                    context = appCtx,
                    returnToPrevious = shouldReturnToPrevious,
                    returnPackage = previousNonLauncherPkg
                )
            }
        }
    }

    fun openPreviousNonLauncherApp(context: Context) {
        val appCtx = context.applicationContext
        val previousPkg = resolvePreviousNonLauncherPackage(
            context = appCtx,
            allowFallbackWhenInRna = true
        )
        if (!RnaHomeInterceptorService.isEligiblePreviousAppPackage(appCtx, previousPkg)) return
        mainHandler.post {
            val launchedByService = RnaHomeInterceptorService.instance?.launchPreviousAppFromService(previousPkg) == true
            if (!launchedByService) {
                val launchedDirect = launchPreviousAppFromContext(appCtx, previousPkg)
                if (!launchedDirect) {
                    MainActivity.activeInstance?.moveTaskToBack(true)
                }
            }
        }
    }

    fun completeBootWarmupReturnToPrevious(activity: MainActivity, targetPackage: String) {
        if (!RnaHomeInterceptorService.isEligiblePreviousAppPackage(activity, targetPackage)) return
        val launchedByService = RnaHomeInterceptorService.instance?.launchPreviousAppFromService(targetPackage) == true
        if (launchedByService) return
        val launchedDirect = launchPreviousAppFromContext(activity, targetPackage)
        if (!launchedDirect) {
            runCatching { activity.moveTaskToBack(true) }
        }
    }

    private fun resolvePreviousNonLauncherPackage(
        context: Context,
        allowFallbackWhenInRna: Boolean
    ): String {
        val service = RnaHomeInterceptorService.instance
        if (service != null) {
            val fromService = service.getCurrentOrLastNonLauncherPackage(
                allowFallbackWhenInRna = allowFallbackWhenInRna
            )
            if (fromService.isNotBlank()) return fromService
            // If Accessibility explicitly saw the stock Android TV launcher active right now, never reopen it
            val liveRoot = runCatching { service.rootInActiveWindow?.packageName?.toString().orEmpty() }.getOrDefault("")
            if (RnaHomeInterceptorService.isStockTvLauncherPackage(liveRoot)) return ""
        }

        // UsageStatsManager fallback when Accessibility hasn't observed a window transition yet
        return resolveLatestUsageStatsNonLauncherPackage(context)
    }

    private fun resolveLatestUsageStatsNonLauncherPackage(context: Context): String {
        return runCatching {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return ""
            val now = System.currentTimeMillis()
            val events = usm.queryEvents(now - 90_000L, now)
            val event = UsageEvents.Event()
            var latestRelevantPkg = ""
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val isForegroundEvent = event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                if (!isForegroundEvent) continue
                val pkg = event.packageName.orEmpty()
                if (pkg.isBlank() || pkg == context.packageName || pkg == "android" || pkg == "com.android.systemui") {
                    continue
                }
                latestRelevantPkg = pkg
            }
            if (RnaHomeInterceptorService.isEligiblePreviousAppPackage(context, latestRelevantPkg)) {
                latestRelevantPkg
            } else {
                ""
            }
        }.getOrDefault("")
    }

    private fun launchPreviousAppFromContext(context: Context, targetPkg: String): Boolean {
        if (!RnaHomeInterceptorService.isEligiblePreviousAppPackage(context, targetPkg)) return false
        return runCatching {
            val pm = context.packageManager
            val intent = pm.getLeanbackLaunchIntentForPackage(targetPkg)
                ?: pm.getLaunchIntentForPackage(targetPkg)
                ?: run {
                    val mainMatches = pm.queryIntentActivities(
                        Intent(Intent.ACTION_MAIN).setPackage(targetPkg),
                        0
                    )
                    val activityInfo = mainMatches.firstOrNull { it.activityInfo?.exported == true }?.activityInfo
                    if (activityInfo != null) {
                        Intent(Intent.ACTION_MAIN).apply {
                            component = ComponentName(activityInfo.packageName, activityInfo.name)
                        }
                    } else {
                        null
                    }
                }
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                context.startActivity(intent)
                true
            } else {
                false
            }
        }.getOrDefault(false)
    }

    private fun launchRnaTvForBootWarmupFallback(
        context: Context,
        returnToPrevious: Boolean,
        returnPackage: String
    ) {
        val appCtx = context.applicationContext
        val intent = Intent(appCtx, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
            putExtra(MainActivity.EXTRA_OPEN_TV_LAUNCHER, true)
            putExtra(MainActivity.EXTRA_RESET_TO_HOME_TAB, true)
            putExtra(MainActivity.EXTRA_BOOT_WARMUP_RETURN_TO_PREVIOUS, returnToPrevious)
            if (returnToPrevious && returnPackage.isNotBlank()) {
                putExtra(MainActivity.EXTRA_BOOT_WARMUP_RETURN_PACKAGE, returnPackage)
            }
        }
        runCatching { appCtx.startActivity(intent) }
    }

    fun launchRnaTvForeground(context: Context) {
        val appCtx = context.applicationContext
        val intent = Intent(appCtx, MainActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
            putExtra(MainActivity.EXTRA_OPEN_TV_LAUNCHER, true)
            putExtra(MainActivity.EXTRA_RESET_TO_HOME_TAB, true)
            putExtra(MainActivity.EXTRA_BOOT_WARMUP_RETURN_TO_PREVIOUS, false)
        }
        runCatching { appCtx.startActivity(intent) }
    }

    fun applyExactVolumePercent(context: Context, percent: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val clamped = percent.coerceIn(0, 100)
        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val targetIndex = ((clamped / 100f) * maxVol).roundToInt().coerceIn(0, maxVol)
        runCatching {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, targetIndex, AudioManager.FLAG_SHOW_UI)
        }
    }

    private fun dispatchTriggerButton(context: Context, keyCode: Int) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val service = RnaHomeInterceptorService.instance
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                am?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                am?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            }
            KeyEvent.KEYCODE_VOLUME_MUTE -> {
                am?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                am?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
                am?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
            }
            KeyEvent.KEYCODE_HOME -> {
                if (service != null) service.launchRnaTvHomeFromService() else launchRnaTvForeground(context)
            }
            else -> {
                service?.dispatchPhysicalKeyOrGamepadActionOverExternalApp(keyCode)
            }
        }
    }
}

/**
 * Full-Screen TV D-Pad Studio for Triggers & Macros (Opened from Control Panel -> Functions).
 */
@Composable
fun TvTriggersAndMacrosScreen(
    prefs: SharedPreferences,
    allApps: List<UnifiedLauncherApp>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var rules by remember { mutableStateOf(RnaTriggersStore.loadRules(prefs)) }
    var selectedRuleId by remember { mutableStateOf(rules.firstOrNull()?.id ?: RnaTriggersStore.DEFAULT_BOOT_ID) }
    val selectedRule = rules.firstOrNull { it.id == selectedRuleId } ?: rules.firstOrNull()

    var showAddAppTriggerDialog by remember { mutableStateOf(false) }
    var showAddActionDialog by remember { mutableStateOf(false) }
    var showPickAppForActionDialog by remember { mutableStateOf(false) }
    var showPickButtonForActionDialog by remember { mutableStateOf(false) }
    var showPickVolumeDialog by remember { mutableStateOf(false) }
    var editingVolumeActionId by remember { mutableStateOf<String?>(null) }
    var showPickDelayDialog by remember { mutableStateOf(false) }
    var editingDelayActionId by remember { mutableStateOf<String?>(null) }
    var actionMenuTargetIndex by remember { mutableIntStateOf(-1) }
    var actionMenuOpenedViaLongPress by remember { mutableStateOf(false) }

    val firstTriggerFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(80L)
        runCatching { firstTriggerFocus.requestFocus() }
    }

    fun updateSelectedRule(transform: (RnaTriggerRule) -> RnaTriggerRule) {
        val current = selectedRule ?: return
        val updatedRule = transform(current)
        rules = RnaTriggersStore.upsertRule(prefs, updatedRule)
        selectedRuleId = updatedRule.id
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        BackHandler(enabled = true) {
            onDismiss()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF131A2E), Color(0xFF070A13))
                    )
                )
                .padding(22.dp)
                .testTag("tv_triggers_macros_screen")
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // ── LEFT SIDEBAR: Triggers List + Add App Trigger ──
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF101524),
                    border = BorderStroke(1.5.dp, CyanPrimary.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Top Header with compact X close button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = CyanPrimary.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, CyanPrimary),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = CyanPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Triggers & Macros",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${rules.size} Automations",
                                        color = CyanPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            var closeFocused by remember { mutableStateOf(false) }
                            Surface(
                                shape = CircleShape,
                                color = if (closeFocused) CoralAccent else CoralAccent.copy(alpha = 0.16f),
                                border = BorderStroke(1.5.dp, if (closeFocused) Color.White else CoralAccent),
                                modifier = Modifier
                                    .size(38.dp)
                                    .onFocusChanged { closeFocused = it.isFocused }
                                    .clickable { onDismiss() }
                                    .testTag("triggers_close_x_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Triggers & Macros",
                                        tint = if (closeFocused) Color(0xFF0B0E17) else CoralAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Add App Trigger Button
                        var addTriggerFocused by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (addTriggerFocused) Color.White else CyanPrimary,
                            border = BorderStroke(if (addTriggerFocused) 2.5.dp else 1.dp, Color.White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .onFocusChanged { addTriggerFocused = it.isFocused }
                                .clickable { showAddAppTriggerDialog = true }
                                .testTag("btn_add_app_trigger")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color(0xFF090D16),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Add App Trigger",
                                    color = Color(0xFF090D16),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.5.sp
                                )
                            }
                        }

                        // Triggers List
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
                        ) {
                            itemsIndexed(rules, key = { _, r -> r.id }) { idx, rule ->
                                val isSel = rule.id == selectedRule?.id
                                var focused by remember { mutableStateOf(false) }
                                val accent = when (rule.type) {
                                    RnaTriggerType.DEVICE_BOOT -> EmeraldSuccess
                                    RnaTriggerType.SCREEN_ON -> AmberAccent
                                    RnaTriggerType.APP_OPEN -> VioletSecondary
                                }
                                val icon = when (rule.type) {
                                    RnaTriggerType.DEVICE_BOOT -> Icons.Default.PowerSettingsNew
                                    RnaTriggerType.SCREEN_ON -> Icons.Default.Tv
                                    RnaTriggerType.APP_OPEN -> Icons.Default.Apps
                                }
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = when {
                                        focused -> accent
                                        isSel -> accent.copy(alpha = 0.22f)
                                        else -> Color(0xFF171D2F)
                                    },
                                    border = BorderStroke(
                                        width = if (focused || isSel) 2.dp else 1.dp,
                                        color = when {
                                            focused -> Color.White
                                            isSel -> accent
                                            else -> Color(0x22FFFFFF)
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(if (idx == 0) Modifier.focusRequester(firstTriggerFocus) else Modifier)
                                        .onFocusChanged {
                                            focused = it.isFocused
                                            if (it.isFocused) selectedRuleId = rule.id
                                        }
                                        .clickable { selectedRuleId = rule.id }
                                        .testTag("trigger_sidebar_item_${rule.id}")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(11.dp),
                                            color = if (focused) Color(0x28090D16) else accent.copy(alpha = 0.2f),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = if (focused) Color(0xFF090D16) else accent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = rule.title,
                                                color = if (focused) Color(0xFF090D16) else Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${rule.actions.size} action${if (rule.actions.size == 1) "" else "s"} • ${if (rule.enabled) "ON" else "OFF"}",
                                                color = if (focused) Color(0xCC090D16) else Color(0xFF94A3B8),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Voice AI Tip Banner
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF151C30),
                            border = BorderStroke(1.dp, VioletSecondary.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = VioletSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Voice AI ready: Say \"Set volume to 21% when Netflix opens\"",
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }

                // ── RIGHT PANE: Selected Trigger Action Chain Editor ──
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFF101524),
                    border = BorderStroke(1.5.dp, Color(0x28FFFFFF)),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (selectedRule != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header Card
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = Color(0xFF171E33),
                                border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedRule.title,
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 21.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Enable / Disable Toggle Chip
                                        var toggleFocused by remember { mutableStateOf(false) }
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (toggleFocused) Color.White else if (selectedRule.enabled) EmeraldSuccess.copy(alpha = 0.2f) else Color(0xFF222940),
                                            border = BorderStroke(1.5.dp, if (toggleFocused) CyanPrimary else if (selectedRule.enabled) EmeraldSuccess else Color(0x33FFFFFF)),
                                            modifier = Modifier
                                                .onFocusChanged { toggleFocused = it.isFocused }
                                                .clickable {
                                                    updateSelectedRule { it.copy(enabled = !it.enabled) }
                                                }
                                                .testTag("btn_toggle_trigger_enabled")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = if (selectedRule.enabled) "Active" else "Paused",
                                                    color = if (toggleFocused) Color(0xFF090D16) else Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 12.sp
                                                )
                                                Switch(
                                                    checked = selectedRule.enabled,
                                                    onCheckedChange = null,
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color(0xFF090D16),
                                                        checkedTrackColor = EmeraldSuccess
                                                    )
                                                )
                                            }
                                        }

                                        // Test Run Button
                                        var testFocused by remember { mutableStateOf(false) }
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (testFocused) Color.White else CyanPrimary.copy(alpha = 0.2f),
                                            border = BorderStroke(1.5.dp, if (testFocused) Color.White else CyanPrimary),
                                            modifier = Modifier
                                                .height(44.dp)
                                                .onFocusChanged { testFocused = it.isFocused }
                                                .clickable {
                                                    if (selectedRule.actions.isEmpty()) {
                                                        Toast.makeText(context, "Add an action first to test this trigger", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        RnaTriggersExecutor.executeRuleActions(context, selectedRule)
                                                        Toast.makeText(context, "Running ${selectedRule.actions.size} action(s)...", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                .testTag("btn_test_trigger")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    tint = if (testFocused) Color(0xFF090D16) else CyanPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "Test",
                                                    color = if (testFocused) Color(0xFF090D16) else Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 12.5.sp
                                                )
                                            }
                                        }

                                        if (!selectedRule.isBuiltIn) {
                                            var delFocused by remember { mutableStateOf(false) }
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (delFocused) CoralAccent else CoralAccent.copy(alpha = 0.16f),
                                                border = BorderStroke(1.5.dp, if (delFocused) Color.White else CoralAccent),
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .onFocusChanged { delFocused = it.isFocused }
                                                    .clickable {
                                                        rules = RnaTriggersStore.deleteRule(prefs, selectedRule.id)
                                                        selectedRuleId = rules.firstOrNull()?.id ?: RnaTriggersStore.DEFAULT_BOOT_ID
                                                    }
                                                    .testTag("btn_delete_trigger")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Delete App Trigger",
                                                        tint = if (delFocused) Color(0xFF090D16) else CoralAccent,
                                                        modifier = Modifier.size(19.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Action Sequence Toolbar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ORDERED ACTION SEQUENCE (${selectedRule.actions.size})",
                                    color = CyanPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                var addActionFocused by remember { mutableStateOf(false) }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (addActionFocused) Color.White else VioletSecondary,
                                    border = BorderStroke(if (addActionFocused) 2.5.dp else 1.dp, Color.White),
                                    modifier = Modifier
                                        .height(44.dp)
                                        .onFocusChanged { addActionFocused = it.isFocused }
                                        .clickable { showAddActionDialog = true }
                                        .testTag("btn_add_trigger_action")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color(0xFF090D16),
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Text(
                                            text = "Add Action",
                                            color = Color(0xFF090D16),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            // Ordered Action List
                            if (selectedRule.actions.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(22.dp),
                                    color = Color(0xFF151B2E),
                                    border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = VioletSecondary,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "No Actions Configured Yet",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Select '+ Add Action' above to chain actions like Set Volume %, Open App, Press Button, Wait/Delay, or Quick Taskbar.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .testTag("trigger_actions_list"),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(bottom = 16.dp)
                                ) {
                                    itemsIndexed(selectedRule.actions, key = { _, a -> a.id }) { idx, action ->
                                        TriggerActionSequenceCard(
                                            index = idx,
                                            totalCount = selectedRule.actions.size,
                                            action = action,
                                            onOpenOptionsMenu = { viaLongPress ->
                                                actionMenuOpenedViaLongPress = viaLongPress
                                                actionMenuTargetIndex = idx
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── MODAL A: Press-and-Hold Action Options Menu (Move Up / Move Down / Edit / Remove) ──
            if (selectedRule != null && actionMenuTargetIndex in selectedRule.actions.indices) {
                val targetAction = selectedRule.actions[actionMenuTargetIndex]
                TriggerActionOptionsDialog(
                    action = targetAction,
                    index = actionMenuTargetIndex,
                    totalCount = selectedRule.actions.size,
                    openedViaLongPress = actionMenuOpenedViaLongPress,
                    onMoveEarlier = {
                        if (actionMenuTargetIndex > 0) {
                            updateSelectedRule { r ->
                                val mutable = r.actions.toMutableList()
                                val item = mutable.removeAt(actionMenuTargetIndex)
                                mutable.add(actionMenuTargetIndex - 1, item)
                                r.copy(actions = mutable)
                            }
                            actionMenuTargetIndex -= 1
                        }
                    },
                    onMoveLater = {
                        if (actionMenuTargetIndex < selectedRule.actions.lastIndex) {
                            updateSelectedRule { r ->
                                val mutable = r.actions.toMutableList()
                                val item = mutable.removeAt(actionMenuTargetIndex)
                                mutable.add(actionMenuTargetIndex + 1, item)
                                r.copy(actions = mutable)
                            }
                            actionMenuTargetIndex += 1
                        }
                    },
                    onEditValue = when (targetAction.type) {
                        RnaTriggerActionType.SET_VOLUME_PERCENT -> {
                            {
                                editingVolumeActionId = targetAction.id
                                actionMenuTargetIndex = -1
                                showPickVolumeDialog = true
                            }
                        }
                        RnaTriggerActionType.DELAY_MS -> {
                            {
                                editingDelayActionId = targetAction.id
                                actionMenuTargetIndex = -1
                                showPickDelayDialog = true
                            }
                        }
                        else -> null
                    },
                    onRemove = {
                        val removeIdx = actionMenuTargetIndex
                        actionMenuTargetIndex = -1
                        updateSelectedRule { r ->
                            val mutable = r.actions.toMutableList()
                            if (removeIdx in mutable.indices) mutable.removeAt(removeIdx)
                            r.copy(actions = mutable)
                        }
                    },
                    onDismiss = { actionMenuTargetIndex = -1 }
                )
            }

            // ── MODAL B: Add App Trigger Picker ──
            if (showAddAppTriggerDialog) {
                TriggerAppPickerDialog(
                    title = "Choose App for New Trigger",
                    subtitle = "Actions will run automatically whenever this app opens:",
                    allApps = allApps,
                    onSelectApp = { app ->
                        val (updatedList, createdRule) = RnaTriggersStore.findOrCreateAppTrigger(
                            prefs = prefs,
                            packageName = app.packageName,
                            appLabel = app.title
                        )
                        rules = updatedList
                        selectedRuleId = createdRule.id
                        showAddAppTriggerDialog = false
                    },
                    onDismiss = { showAddAppTriggerDialog = false }
                )
            }

            // ── MODAL C: Choose Action Type to Add ──
            if (showAddActionDialog && selectedRule != null) {
                AddTriggerActionDialog(
                    onSelectType = { chosenType ->
                        showAddActionDialog = false
                        when (chosenType) {
                            RnaTriggerActionType.OPEN_APP -> showPickAppForActionDialog = true
                            RnaTriggerActionType.PRESS_BUTTON -> showPickButtonForActionDialog = true
                            RnaTriggerActionType.SET_VOLUME_PERCENT -> {
                                editingVolumeActionId = null
                                showPickVolumeDialog = true
                            }
                            RnaTriggerActionType.DELAY_MS -> {
                                editingDelayActionId = null
                                showPickDelayDialog = true
                            }
                            else -> {
                                updateSelectedRule { r ->
                                    r.copy(actions = r.actions + RnaTriggerActionItem(type = chosenType))
                                }
                            }
                        }
                    },
                    onDismiss = { showAddActionDialog = false }
                )
            }

            // ── MODAL D: Pick App for OPEN_APP Action ──
            if (showPickAppForActionDialog && selectedRule != null) {
                TriggerAppPickerDialog(
                    title = "Select App to Launch",
                    subtitle = "Choose which app this trigger action should open:",
                    allApps = allApps,
                    onSelectApp = { app ->
                        updateSelectedRule { r ->
                            r.copy(
                                actions = r.actions + RnaTriggerActionItem(
                                    type = RnaTriggerActionType.OPEN_APP,
                                    targetPackage = app.packageName,
                                    targetLabel = app.title
                                )
                            )
                        }
                        showPickAppForActionDialog = false
                    },
                    onDismiss = { showPickAppForActionDialog = false }
                )
            }

            // ── MODAL E: Pick Remote Button for PRESS_BUTTON Action ──
            if (showPickButtonForActionDialog && selectedRule != null) {
                PickTriggerButtonDialog(
                    onSelectKeyCode = { code ->
                        updateSelectedRule { r ->
                            r.copy(
                                actions = r.actions + RnaTriggerActionItem(
                                    type = RnaTriggerActionType.PRESS_BUTTON,
                                    keyCode = code
                                )
                            )
                        }
                        showPickButtonForActionDialog = false
                    },
                    onDismiss = { showPickButtonForActionDialog = false }
                )
            }

            // ── MODAL F: Pick Exact Volume % (0..100%) ──
            if (showPickVolumeDialog && selectedRule != null) {
                val initialPct = selectedRule.actions.firstOrNull { it.id == editingVolumeActionId }?.volumePercent ?: 25
                PickTriggerVolumeDialog(
                    initialPercent = initialPct,
                    onConfirmPercent = { pct ->
                        val editId = editingVolumeActionId
                        if (editId != null) {
                            updateSelectedRule { r ->
                                r.copy(
                                    actions = r.actions.map { a ->
                                        if (a.id == editId) a.copy(volumePercent = pct) else a
                                    }
                                )
                            }
                        } else {
                            updateSelectedRule { r ->
                                r.copy(
                                    actions = r.actions + RnaTriggerActionItem(
                                        type = RnaTriggerActionType.SET_VOLUME_PERCENT,
                                        volumePercent = pct
                                    )
                                )
                            }
                        }
                        editingVolumeActionId = null
                        showPickVolumeDialog = false
                    },
                    onDismiss = {
                        editingVolumeActionId = null
                        showPickVolumeDialog = false
                    }
                )
            }

            // ── MODAL G: Pick Delay Duration ──
            if (showPickDelayDialog && selectedRule != null) {
                val initialDelay = selectedRule.actions.firstOrNull { it.id == editingDelayActionId }?.delayMs ?: 1000L
                PickTriggerDelayDialog(
                    initialDelayMs = initialDelay,
                    onConfirmDelay = { ms ->
                        val editId = editingDelayActionId
                        if (editId != null) {
                            updateSelectedRule { r ->
                                r.copy(
                                    actions = r.actions.map { a ->
                                        if (a.id == editId) a.copy(delayMs = ms) else a
                                    }
                                )
                            }
                        } else {
                            updateSelectedRule { r ->
                                r.copy(
                                    actions = r.actions + RnaTriggerActionItem(
                                        type = RnaTriggerActionType.DELAY_MS,
                                        delayMs = ms
                                    )
                                )
                            }
                        }
                        editingDelayActionId = null
                        showPickDelayDialog = false
                    },
                    onDismiss = {
                        editingDelayActionId = null
                        showPickDelayDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun TriggerActionSequenceCard(
    index: Int,
    totalCount: Int,
    action: RnaTriggerActionItem,
    onOpenOptionsMenu: (openedViaLongPress: Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    var focused by remember { mutableStateOf(false) }
    var holdJob by remember { mutableStateOf<Job?>(null) }
    var longPressTriggered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.02f else 1f,
        label = "trigger_action_card_scale"
    )
    val accent = when (action.type) {
        RnaTriggerActionType.BOOT_WARM_RNA_TV -> EmeraldSuccess
        RnaTriggerActionType.OPEN_PREVIOUS_APP -> CyanPrimary
        RnaTriggerActionType.SET_VOLUME_PERCENT -> AmberAccent
        RnaTriggerActionType.OPEN_APP -> VioletSecondary
        RnaTriggerActionType.DELAY_MS -> Color(0xFF38BDF8)
        else -> CyanPrimary
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (focused) Color(0xFF252F4D) else Color(0xFF171E32),
        border = BorderStroke(
            width = if (focused) 2.5.dp else 1.dp,
            color = if (focused) Color.White else accent.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { ev ->
                val code = ev.nativeKeyEvent.keyCode
                val isOk = code == KeyEvent.KEYCODE_DPAD_CENTER ||
                    code == KeyEvent.KEYCODE_ENTER ||
                    code == KeyEvent.KEYCODE_NUMPAD_ENTER ||
                    code == KeyEvent.KEYCODE_BUTTON_A
                if (code == KeyEvent.KEYCODE_MENU && ev.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
                    onOpenOptionsMenu(false)
                    return@onPreviewKeyEvent true
                }
                if (isOk) {
                    when (ev.nativeKeyEvent.action) {
                        KeyEvent.ACTION_DOWN -> {
                            if (ev.nativeKeyEvent.repeatCount == 0) {
                                longPressTriggered = false
                                holdJob?.cancel()
                                holdJob = scope.launch {
                                    delay(420L)
                                    longPressTriggered = true
                                    onOpenOptionsMenu(true)
                                }
                            }
                            true
                        }
                        KeyEvent.ACTION_UP -> {
                            holdJob?.cancel()
                            if (!longPressTriggered) {
                                onOpenOptionsMenu(false)
                            }
                            longPressTriggered = false
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }
            .clickable { onOpenOptionsMenu(false) }
            .testTag("trigger_action_item_$index")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = if (focused) Color.White else accent.copy(alpha = 0.22f),
                border = BorderStroke(1.5.dp, if (focused) accent else accent.copy(alpha = 0.7f)),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${index + 1}",
                        color = if (focused) Color(0xFF090D16) else accent,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.displayTitle(),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = action.displaySubtitle(),
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (focused) accent else Color(0xFF222B45)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Reorder or Remove",
                        tint = if (focused) Color(0xFF090D16) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (totalCount > 1) "Hold OK • Reorder" else "Hold OK • Options",
                        color = if (focused) Color(0xFF090D16) else Color(0xFFCBD5E1),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TriggerActionOptionsDialog(
    action: RnaTriggerActionItem,
    index: Int,
    totalCount: Int,
    openedViaLongPress: Boolean = false,
    onMoveEarlier: () -> Unit,
    onMoveLater: () -> Unit,
    onEditValue: (() -> Unit)?,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    // Match Home's AppContextMenuDialog: ignore the first OK release/click right after press-and-hold
    var ignoredFirstOk by remember(openedViaLongPress) { mutableStateOf(!openedViaLongPress) }
    val firstOptionFocus = remember { FocusRequester() }

    fun guardFirstOk(block: () -> Unit) {
        if (!ignoredFirstOk) {
            ignoredFirstOk = true
            return
        }
        block()
    }

    LaunchedEffect(Unit) {
        delay(60L)
        runCatching { firstOptionFocus.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF813182A)),
            border = BorderStroke(2.dp, CyanPrimary),
            modifier = Modifier
                .width(500.dp)
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
                .testTag("trigger_action_options_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Step #${index + 1}: ${action.displayTitle()}",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    maxLines = 2
                )
                Text(
                    text = "Reorder when this action runs or remove it from this trigger:",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))

                var focusAssigned = false
                if (index > 0) {
                    focusAssigned = true
                    TriggerPopupOptionRow(
                        label = "Move Earlier (Before Step #$index)",
                        icon = Icons.Default.ArrowUpward,
                        accent = CyanPrimary,
                        onClick = { guardFirstOk(onMoveEarlier) },
                        modifier = Modifier.focusRequester(firstOptionFocus)
                    )
                }
                if (index < totalCount - 1) {
                    val mod = if (!focusAssigned) {
                        focusAssigned = true
                        Modifier.focusRequester(firstOptionFocus)
                    } else Modifier
                    TriggerPopupOptionRow(
                        label = "Move Later (After Step #${index + 2})",
                        icon = Icons.Default.ArrowDownward,
                        accent = VioletSecondary,
                        onClick = { guardFirstOk(onMoveLater) },
                        modifier = mod
                    )
                }
                if (onEditValue != null) {
                    val mod = if (!focusAssigned) {
                        focusAssigned = true
                        Modifier.focusRequester(firstOptionFocus)
                    } else Modifier
                    TriggerPopupOptionRow(
                        label = if (action.type == RnaTriggerActionType.SET_VOLUME_PERCENT) "Edit Volume Percentage" else "Edit Wait Duration",
                        icon = Icons.Default.Timer,
                        accent = AmberAccent,
                        onClick = { guardFirstOk(onEditValue) },
                        modifier = mod
                    )
                }
                run {
                    val mod = if (!focusAssigned) {
                        focusAssigned = true
                        Modifier.focusRequester(firstOptionFocus)
                    } else Modifier
                    TriggerPopupOptionRow(
                        label = "Remove Action",
                        icon = Icons.Default.DeleteOutline,
                        accent = CoralAccent,
                        onClick = { guardFirstOk(onRemove) },
                        modifier = mod
                    )
                }
                TriggerPopupOptionRow(
                    label = "Done",
                    icon = Icons.Default.Check,
                    accent = EmeraldSuccess,
                    onClick = { guardFirstOk(onDismiss) }
                )
            }
        }
    }
}

@Composable
private fun TriggerPopupOptionRow(
    label: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(15.dp),
        color = if (focused) Color.White else accent.copy(alpha = 0.16f),
        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) accent else accent.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (focused) Color(0xFF090D16) else accent,
                modifier = Modifier.size(19.dp)
            )
            Text(
                text = label,
                color = if (focused) Color(0xFF090D16) else Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun AddTriggerActionDialog(
    onSelectType: (RnaTriggerActionType) -> Unit,
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
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF8121626)),
            border = BorderStroke(1.5.dp, CyanPrimary),
            modifier = Modifier
                .fillMaxWidth(0.76f)
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
                            text = "Add Trigger Action",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Choose an action to append to this trigger's sequence:",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp
                        )
                    }
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(14.dp)) {
                        Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(RnaTriggerActionType.entries) { idx, actionType ->
                        var focused by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (focused) Color.White else Color(0xFF1A2036),
                            border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) CyanPrimary else Color(0x28FFFFFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (idx == 0) Modifier.focusRequester(firstFocus) else Modifier)
                                .onFocusChanged { focused = it.isFocused }
                                .clickable { onSelectType(actionType) }
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
                                        color = if (focused) Color(0xFF090D16) else Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.5.sp
                                    )
                                    Text(
                                        text = actionType.subtitle,
                                        color = if (focused) Color(0xFF334155) else Color(0xFF94A3B8),
                                        fontSize = 11.5.sp
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (focused) Color(0xFF090D16) else CyanPrimary.copy(alpha = 0.16f)
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
private fun TriggerAppPickerDialog(
    title: String,
    subtitle: String,
    allApps: List<UnifiedLauncherApp>,
    onSelectApp: (UnifiedLauncherApp) -> Unit,
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
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF8121626)),
            border = BorderStroke(1.5.dp, VioletSecondary),
            modifier = Modifier
                .fillMaxWidth(0.76f)
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
                            text = title,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Text(
                            text = subtitle,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.5.sp
                        )
                    }
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(14.dp)) {
                        Text("Back", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(allApps, key = { _, app -> app.uniqueKey }) { idx, app ->
                        var focused by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (focused) Color.White else Color(0xFF1A2036),
                            border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) VioletSecondary else Color(0x28FFFFFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (idx == 0) Modifier.focusRequester(firstFocus) else Modifier)
                                .onFocusChanged { focused = it.isFocused }
                                .clickable { onSelectApp(app) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                ) {
                                    RemapperAppIconThumb(app = app)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.title,
                                        color = if (focused) Color(0xFF090D16) else Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        color = if (focused) Color(0xFF334155) else Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
private fun PickTriggerButtonDialog(
    onSelectKeyCode: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(60L)
        runCatching { firstFocus.requestFocus() }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF8121626)),
            border = BorderStroke(1.5.dp, CyanPrimary),
            modifier = Modifier
                .width(560.dp)
                .fillMaxHeight(0.78f)
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
                    Text(
                        text = "Choose Remote Button to Press",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp
                    )
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(14.dp)) {
                        Text("Cancel", color = Color.White)
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(TRIGGER_BUTTON_PRESETS) { idx, (code, label) ->
                        var focused by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (focused) Color.White else Color(0xFF1A2036),
                            border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) CyanPrimary else Color(0x28FFFFFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (idx == 0) Modifier.focusRequester(firstFocus) else Modifier)
                                .onFocusChanged { focused = it.isFocused }
                                .clickable { onSelectKeyCode(code) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 13.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    color = if (focused) Color(0xFF090D16) else Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Key $code",
                                    color = if (focused) Color(0xFF334155) else Color(0xFF94A3B8),
                                    fontSize = 11.5.sp
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
private fun PickTriggerVolumeDialog(
    initialPercent: Int,
    onConfirmPercent: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var percent by remember { mutableIntStateOf(initialPercent.coerceIn(0, 100)) }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(60L)
        runCatching { firstFocus.requestFocus() }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF8121626)),
            border = BorderStroke(2.dp, AmberAccent),
            modifier = Modifier.width(560.dp)
        ) {
            Column(
                modifier = Modifier.padding(26.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = AmberAccent,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "Set Exact Volume: $percent%",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(-10 to "-10%", -1 to "-1%", 1 to "+1%", 10 to "+10%").forEachIndexed { idx, (delta, lbl) ->
                        var focused by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (focused) Color.White else Color(0xFF1F2740),
                            border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) AmberAccent else Color(0x33FFFFFF)),
                            modifier = Modifier
                                .then(if (idx == 2) Modifier.focusRequester(firstFocus) else Modifier)
                                .onFocusChanged { focused = it.isFocused }
                                .clickable { percent = (percent + delta).coerceIn(0, 100) }
                        ) {
                            Text(
                                text = lbl,
                                color = if (focused) Color(0xFF090D16) else Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf(0, 10, 20, 25, 35, 50, 75, 100)) { preset ->
                        val sel = percent == preset
                        var focused by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (focused) Color.White else if (sel) AmberAccent else Color(0xFF1A2036),
                            modifier = Modifier
                                .onFocusChanged { focused = it.isFocused }
                                .clickable { percent = preset }
                        ) {
                            Text(
                                text = "$preset%",
                                color = if (focused || sel) Color(0xFF090D16) else Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(14.dp)) {
                        Text("Cancel", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onConfirmPercent(percent) },
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = Color(0xFF090D16)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Save Volume $percent%", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun PickTriggerDelayDialog(
    initialDelayMs: Long,
    onConfirmDelay: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val presets = listOf(500L to "0.5s", 1000L to "1s", 2000L to "2s", 3000L to "3s", 5000L to "5s", 10000L to "10s")
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(60L)
        runCatching { firstFocus.requestFocus() }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xF8121626)),
            border = BorderStroke(2.dp, CyanPrimary),
            modifier = Modifier.width(500.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select Wait / Delay Duration",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp
                )
                presets.forEachIndexed { idx, (ms, label) ->
                    var focused by remember { mutableStateOf(false) }
                    val isSel = initialDelayMs == ms
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (focused) Color.White else if (isSel) CyanPrimary.copy(alpha = 0.25f) else Color(0xFF1A2036),
                        border = BorderStroke(if (focused) 2.dp else 1.dp, if (focused) CyanPrimary else Color(0x28FFFFFF)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (idx == 0) Modifier.focusRequester(firstFocus) else Modifier)
                            .onFocusChanged { focused = it.isFocused }
                            .clickable { onConfirmDelay(ms) }
                    ) {
                        Text(
                            text = "Wait $label ($ms ms)",
                            color = if (focused) Color(0xFF090D16) else Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }
                }
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel", color = Color.White)
                }
            }
        }
    }
}
