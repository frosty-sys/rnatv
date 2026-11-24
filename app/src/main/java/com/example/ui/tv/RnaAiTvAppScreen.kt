package com.example.ui.tv

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val RNAAI_PACKAGE_NAME = "com.rna.ai"
const val RNAAI_UNIQUE_KEY = "rna_builtin_rnaai"
const val KEY_RNAAI_ENABLED_IN_CONTROL_PANEL = "rna_ai_app_enabled_in_control_panel"

private const val PREF_KEY_RNAAI_SESSIONS = "rna_ai_saved_chat_sessions_v1"
private const val PREF_KEY_RNAAI_SETTINGS = "rna_ai_custom_settings_v1"
private const val RNAAI_MODEL_NAME = "gemini-3.5-flash-lite"

data class RnaAiChatMessage(
    val id: String,
    val role: String, // "user" or "ai"
    val text: String,
    val actionSummary: String? = null,
    val modelUsedBadge: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class RnaAiChatSession(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messages: List<RnaAiChatMessage>,
    val lastModelUsed: String? = null
)

data class RnaAiColorPreset(
    val id: String,
    val name: String,
    val primaryHex: String,
    val secondaryHex: String
)

val RNAAI_COLOR_PRESETS = listOf(
    RnaAiColorPreset("electric_blue", "Electric Blue", "#38BDF8", "#2563EB"),
    RnaAiColorPreset("cyan_neon", "RNA Cyan", "#BAC3FF", "#4F46E5"),
    RnaAiColorPreset("emerald_matrix", "Emerald Pulse", "#34D399", "#059669"),
    RnaAiColorPreset("violet_galaxy", "Cosmic Violet", "#C084FC", "#7C3AED"),
    RnaAiColorPreset("crimson_nova", "Crimson Glow", "#FB7185", "#E11D48"),
    RnaAiColorPreset("amber_solar", "Solar Gold", "#FBBF24", "#D97706"),
    RnaAiColorPreset("arctic_white", "Pure Arctic", "#F8FAFC", "#94A3B8")
)

const val YT_BEHAVIOR_YOUTUBE_APP = "YOUTUBE_APP"
const val YT_BEHAVIOR_ASK_DIALOG = "ASK_DIALOG"
const val YT_BEHAVIOR_BROWSER_ONLY = "BROWSER_ONLY"

data class RnaAiCustomizationSettings(
    val chatbotTone: String = "Helpful & Concise",
    val chatbotCustomInstructions: String = "",
    val agentCustomInstructions: String = "",
    val agentSpeedMode: String = "Balanced", // "Fast", "Balanced", "Careful"
    val agentAutoScreenshot: Boolean = true,
    val appThemeColorHex: String = "#38BDF8",
    val agentGlowColorHex: String = "#38BDF8",
    val agentSecondaryGlowHex: String = "#2563EB",
    val youtubeLinkBehavior: String = YT_BEHAVIOR_YOUTUBE_APP
)

internal fun isYouTubeUrl(rawUrl: String?): Boolean {
    if (rawUrl.isNullOrBlank()) return false
    val lower = rawUrl.trim().lowercase(Locale.US)
    return lower.contains("youtube.com") || lower.contains("youtu.be") || lower.contains("m.youtube.com")
}

internal fun launchYouTubeAppForUrl(context: Context, rawUrl: String): Boolean {
    val normalized = rawUrl.trim().let {
        if (it.startsWith("http://", true) || it.startsWith("https://", true)) it else "https://$it"
    }
    val uri = runCatching { Uri.parse(normalized) }.getOrNull() ?: return false
    val pm = context.packageManager
    val ytPackages = listOf(
        "com.google.android.youtube.tv",
        "com.google.android.youtube",
        "com.liskovsoft.smarttubetv.beta",
        "com.liskovsoft.smarttubetv2.beta",
        "com.teamsmart.videomanager.tv"
    )
    for (pkg in ytPackages) {
        val isInstalled = runCatching { pm.getPackageInfo(pkg, 0) }.isSuccess
        if (!isInstalled) continue
        val launched = runCatching {
            val viewIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
            true
        }.getOrDefault(false)
        if (launched) return true
    }
    return runCatching {
        val genericIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val resolved = pm.resolveActivity(genericIntent, 0)
        if (resolved != null && resolved.activityInfo.packageName != context.packageName) {
            context.startActivity(genericIntent)
            true
        } else {
            false
        }
    }.getOrDefault(false)
}

fun parseComposeColor(hex: String, fallback: Color = Color(0xFF38BDF8)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

fun loadRnaAiSettings(prefs: SharedPreferences): RnaAiCustomizationSettings {
    val raw = prefs.getString(PREF_KEY_RNAAI_SETTINGS, null) ?: return RnaAiCustomizationSettings()
    return try {
        val obj = JSONObject(raw)
        RnaAiCustomizationSettings(
            chatbotTone = obj.optString("chatbotTone", "Helpful & Concise"),
            chatbotCustomInstructions = obj.optString("chatbotCustomInstructions", ""),
            agentCustomInstructions = obj.optString("agentCustomInstructions", ""),
            agentSpeedMode = obj.optString("agentSpeedMode", "Balanced"),
            agentAutoScreenshot = obj.optBoolean("agentAutoScreenshot", true),
            appThemeColorHex = obj.optString("appThemeColorHex", "#38BDF8"),
            agentGlowColorHex = obj.optString("agentGlowColorHex", "#38BDF8"),
            agentSecondaryGlowHex = obj.optString("agentSecondaryGlowHex", "#2563EB"),
            youtubeLinkBehavior = obj.optString("youtubeLinkBehavior", YT_BEHAVIOR_YOUTUBE_APP)
                .takeIf { it in setOf(YT_BEHAVIOR_YOUTUBE_APP, YT_BEHAVIOR_ASK_DIALOG, YT_BEHAVIOR_BROWSER_ONLY) }
                ?: YT_BEHAVIOR_YOUTUBE_APP
        )
    } catch (_: Exception) {
        RnaAiCustomizationSettings()
    }
}

fun saveRnaAiSettings(prefs: SharedPreferences, settings: RnaAiCustomizationSettings) {
    val obj = JSONObject().apply {
        put("chatbotTone", settings.chatbotTone)
        put("chatbotCustomInstructions", settings.chatbotCustomInstructions)
        put("agentCustomInstructions", settings.agentCustomInstructions)
        put("agentSpeedMode", settings.agentSpeedMode)
        put("agentAutoScreenshot", settings.agentAutoScreenshot)
        put("appThemeColorHex", settings.appThemeColorHex)
        put("agentGlowColorHex", settings.agentGlowColorHex)
        put("agentSecondaryGlowHex", settings.agentSecondaryGlowHex)
        put("youtubeLinkBehavior", settings.youtubeLinkBehavior)
    }
    prefs.edit().putString(PREF_KEY_RNAAI_SETTINGS, obj.toString()).apply()
    RnaAutonomousAgentController.applyCustomizationSettings(settings)
}

fun loadRnaAiChatSessions(prefs: SharedPreferences): List<RnaAiChatSession> {
    val raw = prefs.getString(PREF_KEY_RNAAI_SESSIONS, null) ?: return emptyList()
    return try {
        val arr = JSONArray(raw)
        val out = mutableListOf<RnaAiChatSession>()
        for (i in 0 until arr.length()) {
            val sObj = arr.optJSONObject(i) ?: continue
            val msgArr = sObj.optJSONArray("messages") ?: JSONArray()
            val msgs = mutableListOf<RnaAiChatMessage>()
            for (j in 0 until msgArr.length()) {
                val mObj = msgArr.optJSONObject(j) ?: continue
                msgs.add(
                    RnaAiChatMessage(
                        id = mObj.optString("id", "m_${i}_$j"),
                        role = mObj.optString("role", "user"),
                        text = mObj.optString("text", ""),
                        actionSummary = mObj.optString("actionSummary", "").takeIf { it.isNotBlank() },
                        modelUsedBadge = mObj.optString("modelUsedBadge", "").takeIf { it.isNotBlank() },
                        timestamp = mObj.optLong("timestamp", 0L)
                    )
                )
            }
            val savedModel = sObj.optString("lastModelUsed", "").takeIf { it.isNotBlank() }
                ?: msgs.lastOrNull { it.role == "ai" && !it.modelUsedBadge.isNullOrBlank() }?.modelUsedBadge
            out.add(
                RnaAiChatSession(
                    id = sObj.optString("id", "chat_$i"),
                    title = sObj.optString("title", "Chat"),
                    updatedAt = sObj.optLong("updatedAt", System.currentTimeMillis()),
                    messages = msgs,
                    lastModelUsed = savedModel
                )
            )
        }
        out.sortedByDescending { it.updatedAt }
    } catch (_: Exception) {
        emptyList()
    }
}

fun saveRnaAiChatSessions(prefs: SharedPreferences, sessions: List<RnaAiChatSession>) {
    val arr = JSONArray()
    sessions.take(35).forEach { session ->
        val msgArr = JSONArray()
        session.messages.takeLast(40).forEach { m ->
            msgArr.put(
                JSONObject().apply {
                    put("id", m.id)
                    put("role", m.role)
                    put("text", m.text)
                    if (!m.actionSummary.isNullOrBlank()) put("actionSummary", m.actionSummary)
                    if (!m.modelUsedBadge.isNullOrBlank()) put("modelUsedBadge", m.modelUsedBadge)
                    put("timestamp", m.timestamp)
                }
            )
        }
        val resolvedLastModel = session.lastModelUsed
            ?: session.messages.lastOrNull { it.role == "ai" && !it.modelUsedBadge.isNullOrBlank() }?.modelUsedBadge
        arr.put(
            JSONObject().apply {
                put("id", session.id)
                put("title", session.title)
                put("updatedAt", session.updatedAt)
                if (!resolvedLastModel.isNullOrBlank()) put("lastModelUsed", resolvedLastModel)
                put("messages", msgArr)
            }
        )
    }
    prefs.edit().putString(PREF_KEY_RNAAI_SESSIONS, arr.toString()).apply()
}

private data class ExplorePromptItem(
    val category: String,
    val title: String,
    val prompt: String,
    val isActionOrAgent: Boolean = false
)

private val EXPLORE_PROMPTS = listOf(
    ExplorePromptItem(
        category = "TV Actions",
        title = "Search YouTube in RNA Browser",
        prompt = "Open YouTube in the RNA browser and search for 4K HDR nature documentaries",
        isActionOrAgent = true
    ),
    ExplorePromptItem(
        category = "Autonomous Agent",
        title = "Autonomous Web Research",
        prompt = "Use the autonomous agent to open Google in the browser and search for the top movies of 2026",
        isActionOrAgent = true
    ),
    ExplorePromptItem(
        category = "Entertainment",
        title = " Tonight's Sci-Fi Movie Picks",
        prompt = "Recommend 5 mind-bending sci-fi movies to watch on TV tonight with a short plot hook for each."
    ),
    ExplorePromptItem(
        category = "Brainstorm",
        title = "Explain Quantum Computing Simply",
        prompt = "Explain how quantum computers work in simple, vivid terms suitable for a 2-minute TV read."
    ),
    ExplorePromptItem(
        category = "TV Actions",
        title = "Open Wikipedia in RNA Browser",
        prompt = "Open https://en.wikipedia.org in the RNA browser",
        isActionOrAgent = true
    ),
    ExplorePromptItem(
        category = "Gaming & Tech",
        title = "Optimize Smart TV Picture Settings",
        prompt = "What are the best calibration settings for 4K HDR movies vs low-latency gaming on a modern TV?"
    )
)

/**
 * Full-Screen TV Gemini-Style AI Application ("RNAAI" — Powered by EdVanced 1.1 Pro).
 * Features:
 * - Left TV Sidebar: New Chat, Incognito Mode toggle, Explore Tab, Chat Tab, Live Button, Settings, and Local Storage Chat History.
 * - Multi-turn contextual conversation using saved local chat history.
 * - Full action support: can open installed apps, open websites in the internal RNA Browser (never external browsers), or start Autonomous Agent tasks.
 * - Dedicated Settings tab to customize both Chatbot and Autonomous Agent behaviors, theme colors, and the Agent's screen overlay glow color.
 */
@Composable
fun RnaAiTvAppScreen(
    prefs: SharedPreferences,
    allApps: List<UnifiedLauncherApp>,
    initialTab: Int = 0, // 0: Chat, 1: Explore, 2: Settings
    onLaunchAppByPackageOrName: (String) -> Unit,
    onOpenUrlInRnaBrowser: (String) -> Unit,
    onLaunchLiveVoiceAssistant: () -> Unit,
    onStartAutonomousAgentWithGoal: (String) -> Unit,
    onExitApp: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var customSettings by remember { mutableStateOf(loadRnaAiSettings(prefs)) }
    LaunchedEffect(customSettings) {
        RnaAutonomousAgentController.applyCustomizationSettings(customSettings)
    }

    val accentColor = remember(customSettings.appThemeColorHex) {
        parseComposeColor(customSettings.appThemeColorHex, Color(0xFF38BDF8))
    }
    val glowPreviewColor = remember(customSettings.agentGlowColorHex) {
        parseComposeColor(customSettings.agentGlowColorHex, Color(0xFF38BDF8))
    }

    var savedSessions by remember { mutableStateOf(loadRnaAiChatSessions(prefs)) }
    var activeSessionId by remember {
        mutableStateOf(savedSessions.firstOrNull()?.id ?: "chat_${System.currentTimeMillis()}")
    }
    var isIncognito by remember { mutableStateOf(false) }
    var incognitoMessages by remember { mutableStateOf<List<RnaAiChatMessage>>(emptyList()) }

    val activeMessages: List<RnaAiChatMessage> = remember(isIncognito, incognitoMessages, savedSessions, activeSessionId) {
        if (isIncognito) {
            incognitoMessages
        } else {
            savedSessions.find { it.id == activeSessionId }?.messages ?: emptyList()
        }
    }

    var activeSection by remember { mutableIntStateOf(initialTab) } // 0: Chat, 1: Explore, 2: Settings
    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var isListeningMic by remember { mutableStateOf(false) }
    var liveMicTranscript by remember { mutableStateOf("") }

    val newChatFocusRequester = remember { FocusRequester() }
    val exploreNavFocusRequester = remember { FocusRequester() }
    val settingsNavFocusRequester = remember { FocusRequester() }
    val firstExploreCardFocusRequester = remember { FocusRequester() }
    val firstSettingsControlFocusRequester = remember { FocusRequester() }
    val chatInputFocusRequester = remember { FocusRequester() }
    val micButtonFocusRequester = remember { FocusRequester() }
    val sendButtonFocusRequester = remember { FocusRequester() }
    val liveButtonFocusRequester = remember { FocusRequester() }
    val messageListState = rememberLazyListState()

    fun focusRightPanePrimaryControl() {
        when (activeSection) {
            0 -> runCatching { chatInputFocusRequester.requestFocus() }
            1 -> runCatching { firstExploreCardFocusRequester.requestFocus() }
            2 -> runCatching { firstSettingsControlFocusRequester.requestFocus() }
        }
    }

    BackHandler(enabled = true) {
        if (activeSection != 0) {
            activeSection = 0
        } else {
            onExitApp()
        }
    }

    LaunchedEffect(Unit) {
        delay(80L)
        if (initialTab == 0) {
            runCatching { chatInputFocusRequester.requestFocus() }
        } else {
            runCatching { newChatFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(activeMessages.size) {
        if (activeMessages.isNotEmpty()) {
            runCatching { messageListState.animateScrollToItem(activeMessages.size - 1) }
        }
    }

    fun appendMessageToCurrentChat(msg: RnaAiChatMessage) {
        if (isIncognito) {
            incognitoMessages = incognitoMessages + msg
        } else {
            val existing = savedSessions.find { it.id == activeSessionId }
            val updatedMsgs = (existing?.messages ?: emptyList()) + msg
            val computedTitle = existing?.title?.takeIf { it != "New Chat" && it.isNotBlank() }
                ?: updatedMsgs.firstOrNull { it.role == "user" }?.text?.take(34)?.trim()
                ?: "Chat"
            val updatedModel = msg.modelUsedBadge?.takeIf { it.isNotBlank() } ?: existing?.lastModelUsed
            val updatedSession = RnaAiChatSession(
                id = activeSessionId,
                title = computedTitle,
                updatedAt = System.currentTimeMillis(),
                messages = updatedMsgs,
                lastModelUsed = updatedModel
            )
            val nextList = listOf(updatedSession) + savedSessions.filterNot { it.id == activeSessionId }
            savedSessions = nextList
            saveRnaAiChatSessions(prefs, nextList)
        }
    }

    fun startNewChat(enableIncognito: Boolean = false) {
        isIncognito = enableIncognito
        if (enableIncognito) {
            incognitoMessages = emptyList()
        } else {
            activeSessionId = "chat_${System.currentTimeMillis()}"
        }
        activeSection = 0
    }

    fun deleteSavedSession(sessionId: String) {
        val next = savedSessions.filterNot { it.id == sessionId }
        savedSessions = next
        saveRnaAiChatSessions(prefs, next)
        if (activeSessionId == sessionId) {
            activeSessionId = next.firstOrNull()?.id ?: "chat_${System.currentTimeMillis()}"
        }
    }

    val lastUsedModelSummary by RnaFreeAiRouter.lastUsedModelSummary.collectAsState()
    var showFallbackManagerDialog by remember { mutableStateOf(false) }
    var pendingYouTubeLinkUrl by remember { mutableStateOf<String?>(null) }

    // Pre-warm singleton TTS immediately so first voice playback has zero initialization lag
    LaunchedEffect(Unit) {
        RnaTtsController.preWarm(context)
    }

    fun speakMessageText(text: String) {
        if (text.isBlank()) return
        val cleanSpoken = text
            .replace(Regex("```[\\s\\S]*?```"), " code block ")
            .replace(Regex("[#*_`~>]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        RnaTtsController.speak(context, cleanSpoken)
    }

    fun handleAiBrowserUrl(url: String) {
        if (isYouTubeUrl(url)) {
            when (customSettings.youtubeLinkBehavior) {
                YT_BEHAVIOR_YOUTUBE_APP -> {
                    if (!launchYouTubeAppForUrl(context, url)) {
                        onOpenUrlInRnaBrowser(url)
                    }
                }
                YT_BEHAVIOR_ASK_DIALOG -> {
                    pendingYouTubeLinkUrl = url
                }
                else -> {
                    onOpenUrlInRnaBrowser(url)
                }
            }
        } else {
            onOpenUrlInRnaBrowser(url)
        }
    }

    fun sendUserMessage(rawQuery: String) {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty() || isThinking) return
        inputText = ""
        activeSection = 0

        val userMsg = RnaAiChatMessage(
            id = "u_${System.currentTimeMillis()}",
            role = "user",
            text = trimmed
        )
        val historySnapshot = activeMessages + userMsg
        appendMessageToCurrentChat(userMsg)
        isThinking = true

        scope.launch {
            val response = generateRnaAiChatResponse(
                context = context,
                history = historySnapshot,
                installedApps = allApps,
                settings = customSettings
            )
            isThinking = false

            var actionSummary: String? = response.toolsSummary
            if (!response.openBrowserUrl.isNullOrBlank()) {
                val url = response.openBrowserUrl
                val openLabel = if (isYouTubeUrl(url) && customSettings.youtubeLinkBehavior == YT_BEHAVIOR_YOUTUBE_APP) {
                    "Opened in YouTube App: $url"
                } else {
                    "Opened Link: $url"
                }
                actionSummary = listOfNotNull(actionSummary, openLabel).joinToString(" • ")
            } else if (!response.openAppTarget.isNullOrBlank()) {
                actionSummary = listOfNotNull(actionSummary, "Opened App: ${response.openAppTarget}").joinToString(" • ")
            } else if (!response.agentGoal.isNullOrBlank()) {
                actionSummary = listOfNotNull(actionSummary, "Started Autonomous Agent: ${response.agentGoal}").joinToString(" • ")
            }

            val aiMsg = RnaAiChatMessage(
                id = "a_${System.currentTimeMillis()}",
                role = "ai",
                text = response.replyText,
                actionSummary = actionSummary,
                modelUsedBadge = response.modelUsedBadge
            )
            appendMessageToCurrentChat(aiMsg)

            // Execute any requested actions
            if (!response.openBrowserUrl.isNullOrBlank()) {
                delay(280L)
                handleAiBrowserUrl(response.openBrowserUrl)
            } else if (!response.openAppTarget.isNullOrBlank()) {
                val target = response.openAppTarget.trim()
                val lower = target.lowercase(Locale.US)
                if (lower in listOf("browser", "chrome", "internet", "web", "google chrome", "firefox")) {
                    delay(280L)
                    onOpenUrlInRnaBrowser("https://www.google.com")
                } else {
                    delay(280L)
                    onLaunchAppByPackageOrName(target)
                }
            } else if (!response.agentGoal.isNullOrBlank()) {
                delay(280L)
                onStartAutonomousAgentWithGoal(response.agentGoal)
            }
        }
    }

    // Google SpeechRecognizer for Microphone button (Reliable auto-submit on end-of-speech & silence)
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var speechSessionSeq by remember { mutableIntStateOf(0) }
    var latestPartialTranscript by remember { mutableStateOf("") }
    var hasSubmittedSpeechForSession by remember { mutableStateOf(false) }
    var silenceAutoSubmitJob by remember { mutableStateOf<Job?>(null) }
    var endOfSpeechFallbackJob by remember { mutableStateOf<Job?>(null) }

    fun cancelSpeechJobs() {
        silenceAutoSubmitJob?.cancel()
        silenceAutoSubmitJob = null
        endOfSpeechFallbackJob?.cancel()
        endOfSpeechFallbackJob = null
    }

    fun stopMicListening() {
        cancelSpeechJobs()
        isListeningMic = false
        liveMicTranscript = ""
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
    }

    fun finalizeAndSubmitSpeech(sessionId: Int, finalText: String?) {
        if (sessionId != speechSessionSeq || hasSubmittedSpeechForSession) return
        val candidate = finalText?.trim()?.takeIf { it.isNotEmpty() && it != "Listening..." }
            ?: latestPartialTranscript.trim().takeIf { it.isNotEmpty() && it != "Listening..." }
        hasSubmittedSpeechForSession = true
        cancelSpeechJobs()
        isListeningMic = false
        liveMicTranscript = ""
        latestPartialTranscript = ""
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        if (!candidate.isNullOrBlank()) {
            sendUserMessage(candidate)
        }
    }

    fun startGoogleSpeechRecognizerInternal() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Voice recognition unavailable on this device — use the text box", Toast.LENGTH_SHORT).show()
            return
        }
        val activeSession = speechSessionSeq + 1
        speechSessionSeq = activeSession
        latestPartialTranscript = ""
        hasSubmittedSpeechForSession = false
        cancelSpeechJobs()

        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (activeSession != speechSessionSeq) return
                    isListeningMic = true
                    liveMicTranscript = "Listening..."
                }
                override fun onBeginningOfSpeech() {
                    if (activeSession != speechSessionSeq) return
                    isListeningMic = true
                }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    if (activeSession != speechSessionSeq || hasSubmittedSpeechForSession) return
                    silenceAutoSubmitJob?.cancel()
                    // Give Google SpeechRecognizer 450ms to deliver onResults; otherwise auto-submit captured partial!
                    if (latestPartialTranscript.isNotBlank()) {
                        liveMicTranscript = latestPartialTranscript
                    }
                    endOfSpeechFallbackJob?.cancel()
                    endOfSpeechFallbackJob = scope.launch {
                        delay(450L)
                        if (activeSession == speechSessionSeq && !hasSubmittedSpeechForSession) {
                            finalizeAndSubmitSpeech(activeSession, latestPartialTranscript)
                        }
                    }
                }
                override fun onError(error: Int) {
                    if (activeSession != speechSessionSeq) return
                    if (!hasSubmittedSpeechForSession && latestPartialTranscript.isNotBlank()) {
                        finalizeAndSubmitSpeech(activeSession, latestPartialTranscript)
                    } else {
                        cancelSpeechJobs()
                        isListeningMic = false
                        liveMicTranscript = ""
                        latestPartialTranscript = ""
                    }
                }
                override fun onResults(results: Bundle?) {
                    if (activeSession != speechSessionSeq) return
                    val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.trim().orEmpty()
                    finalizeAndSubmitSpeech(activeSession, spoken)
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    if (activeSession != speechSessionSeq || hasSubmittedSpeechForSession) return
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.trim().orEmpty()
                    if (partial.isNotEmpty()) {
                        latestPartialTranscript = partial
                        liveMicTranscript = partial
                        // If Google stays open after user stops speaking, auto-submit after 1.35s of silence
                        silenceAutoSubmitJob?.cancel()
                        silenceAutoSubmitJob = scope.launch {
                            delay(1350L)
                            if (activeSession == speechSessionSeq && !hasSubmittedSpeechForSession) {
                                finalizeAndSubmitSpeech(activeSession, latestPartialTranscript)
                            }
                        }
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
            }
            isListeningMic = true
            liveMicTranscript = "Listening..."
            recognizer.startListening(intent)
        } catch (_: Exception) {
            isListeningMic = false
            liveMicTranscript = ""
        }
    }

    fun startMicListening() {
        stopMicListening()
        RnaTtsController.stop()
        startGoogleSpeechRecognizerInternal()
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startMicListening()
    }

    fun triggerMicButton() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            if (isListeningMic) {
                if (latestPartialTranscript.isNotBlank()) {
                    finalizeAndSubmitSpeech(speechSessionSeq, latestPartialTranscript)
                } else {
                    stopMicListening()
                }
            } else {
                startMicListening()
            }
        } else {
            micPermLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopMicListening()
            RnaTtsController.stop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.14f),
                        Color(0xFF0B0E17),
                        Color(0xFF07090F)
                    )
                )
            )
            .testTag("rna_ai_fullscreen_app")
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // ── 1. Left TV Sidebar (New Chat, Incognito, Explore, Chat, Live, Settings + Previous Chats) ──
            Surface(
                color = Color(0xF0111420),
                border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                modifier = Modifier
                    .width(276.dp)
                    .fillMaxHeight()
                    .testTag("rna_ai_left_sidebar")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    // Brand Header + Back to Launcher Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = accentColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.5.dp, accentColor),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = com.example.R.drawable.ic_launcher_foreground_img),
                                    contentDescription = "RNA AI Logo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                            Column {
                                Text(
                                    text = "RNA AI",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    letterSpacing = 0.6.sp
                                )
                                Text(
                                    text = "EdVanced 1.1 Pro",
                                    color = accentColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        RnaAiSidebarIconBtn(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Exit RNA AI to Home",
                            accentColor = accentColor,
                            onClick = onExitApp,
                            testTag = "rna_ai_exit_btn"
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Primary Action Buttons: New Chat & Incognito
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RnaAiSidebarPillButton(
                            title = "New Chat",
                            icon = Icons.Default.Add,
                            selected = !isIncognito && activeMessages.isEmpty() && activeSection == 0,
                            accentColor = accentColor,
                            onClick = { startNewChat(enableIncognito = false) },
                            testTag = "rna_ai_new_chat_btn",
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(newChatFocusRequester)
                        )

                        RnaAiSidebarPillButton(
                            title = "Incognito",
                            icon = Icons.Default.VisibilityOff,
                            selected = isIncognito,
                            accentColor = Color(0xFFF59E0B),
                            onClick = { startNewChat(enableIncognito = !isIncognito) },
                            testTag = "rna_ai_incognito_btn",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Navigation Tabs in Sidebar: Chat, Explore, Live, Settings
                    RnaAiSidebarNavRow(
                        title = "Chat",
                        subtitle = if (isIncognito) "Incognito Session (Not Saved)" else "Contextual AI Conversation",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        selected = activeSection == 0,
                        accentColor = accentColor,
                        onClick = {
                            activeSection = 0
                            scope.launch {
                                delay(60L)
                                focusRightPanePrimaryControl()
                            }
                        },
                        onMoveRight = { focusRightPanePrimaryControl() },
                        testTag = "rna_ai_nav_chat"
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    RnaAiSidebarNavRow(
                        title = "History",
                        subtitle = if (savedSessions.isEmpty()) "No saved history yet" else "${savedSessions.size} saved conversation${if (savedSessions.size == 1) "" else "s"}",
                        icon = Icons.Default.History,
                        selected = activeSection == 1,
                        accentColor = accentColor,
                        onClick = {
                            activeSection = 1
                            scope.launch {
                                delay(60L)
                                runCatching { firstExploreCardFocusRequester.requestFocus() }
                            }
                        },
                        onMoveRight = {
                            if (activeSection != 1) {
                                activeSection = 1
                                scope.launch {
                                    delay(60L)
                                    runCatching { firstExploreCardFocusRequester.requestFocus() }
                                }
                            } else {
                                runCatching { firstExploreCardFocusRequester.requestFocus() }
                            }
                        },
                        testTag = "rna_ai_nav_chats",
                        modifier = Modifier.focusRequester(exploreNavFocusRequester)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    RnaAiSidebarNavRow(
                        title = "Live AI & Agent",
                        subtitle = "Voice Bar & Screen Agent",
                        icon = Icons.Default.GraphicEq,
                        selected = false,
                        accentColor = Color(0xFF34D399),
                        onClick = onLaunchLiveVoiceAssistant,
                        onMoveRight = { focusRightPanePrimaryControl() },
                        testTag = "rna_ai_nav_live"
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    RnaAiSidebarNavRow(
                        title = "AI & Glow Settings",
                        subtitle = "Models, API Key, YouTube & Glow",
                        icon = Icons.Default.Settings,
                        selected = activeSection == 2,
                        accentColor = accentColor,
                        onClick = {
                            activeSection = 2
                            scope.launch {
                                delay(60L)
                                runCatching { firstSettingsControlFocusRequester.requestFocus() }
                            }
                        },
                        onMoveRight = {
                            if (activeSection != 2) {
                                activeSection = 2
                                scope.launch {
                                    delay(60L)
                                    runCatching { firstSettingsControlFocusRequester.requestFocus() }
                                }
                            } else {
                                runCatching { firstSettingsControlFocusRequester.requestFocus() }
                            }
                        },
                        testTag = "rna_ai_nav_settings",
                        modifier = Modifier.focusRequester(settingsNavFocusRequester)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Material 3 Expressive Active Fallback Model Card in Sidebar
                    var modelCardFocused by remember { mutableStateOf(false) }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (modelCardFocused) Color.White else Color(0xFF161C2E),
                        border = BorderStroke(
                            width = if (modelCardFocused) 2.dp else 1.dp,
                            color = if (modelCardFocused) accentColor else Color(0xFF34D399).copy(alpha = 0.55f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { modelCardFocused = it.isFocused }
                            .clickable { showFallbackManagerDialog = true }
                            .focusable()
                            .testTag("rna_ai_sidebar_model_router_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "ACTIVE AI ROUTER",
                                    color = if (modelCardFocused) Color(0xFF0F172A) else Color(0xFF34D399),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.7.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Manage AI Models",
                                    tint = if (modelCardFocused) Color(0xFF0F172A) else accentColor,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = lastUsedModelSummary,
                                color = if (modelCardFocused) Color(0xFF0F172A) else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Saved Previous Chats Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT CHATS (LOCAL)",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        if (savedSessions.isNotEmpty()) {
                            Text(
                                text = "${savedSessions.size}",
                                color = accentColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Scrollable Previous Chats List
                    if (savedSessions.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Your chats are stored locally on this TV.\nStart chatting to build history!",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .testTag("rna_ai_history_list"),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(savedSessions, key = { it.id }) { session ->
                                val isCurrent = !isIncognito && session.id == activeSessionId && activeSection == 0
                                var itemFocused by remember { mutableStateOf(false) }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = when {
                                        itemFocused -> Color.White
                                        isCurrent -> accentColor.copy(alpha = 0.2f)
                                        else -> Color(0xFF181C2C)
                                    },
                                    border = BorderStroke(
                                        width = 1.dp,
                                        color = if (itemFocused || isCurrent) accentColor else Color(0x18FFFFFF)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { itemFocused = it.isFocused }
                                        .clickable {
                                            isIncognito = false
                                            activeSessionId = session.id
                                            activeSection = 0
                                        }
                                        .focusable()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = session.title,
                                                color = if (itemFocused) Color(0xFF0F172A) else Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = if (isCurrent || itemFocused) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${session.messages.size} msgs",
                                                color = if (itemFocused) Color(0xFF475569) else Color(0xFF94A3B8),
                                                fontSize = 9.5.sp
                                            )
                                        }
                                        IconButton(
                                            onClick = { deleteSavedSession(session.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete Chat",
                                                tint = if (itemFocused) Color(0xFFEF4444) else Color(0xFF94A3B8),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── 2. Right Main Content Area (Chat / Explore / Settings) ──
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 28.dp, vertical = 20.dp)
            ) {
                when (activeSection) {
                    // ── SECTION 1: SAVED CHATS LIBRARY TAB ──
                    1 -> {
                        val dateFormatter = remember { SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()) }
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "History",
                                        color = Color.White,
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.3).sp
                                    )
                                    Text(
                                        text = if (savedSessions.isEmpty()) {
                                            "Your past conversations will appear here automatically."
                                        } else {
                                            "${savedSessions.size} saved conversation${if (savedSessions.size == 1) "" else "s"} • Select any chat to resume"
                                        },
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(top = 3.dp)
                                    )
                                }

                                RnaAiSidebarPillButton(
                                    title = "New Chat",
                                    icon = Icons.Default.Add,
                                    selected = false,
                                    accentColor = accentColor,
                                    onClick = { startNewChat(enableIncognito = false) },
                                    testTag = "rna_ai_chats_tab_new_chat_btn",
                                    modifier = if (savedSessions.isEmpty()) {
                                        Modifier.focusRequester(firstExploreCardFocusRequester)
                                    } else {
                                        Modifier
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            if (savedSessions.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = Color(0xFF121726),
                                    border = BorderStroke(1.dp, Color(0x22FFFFFF)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = accentColor.copy(alpha = 0.16f),
                                            border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.6f)),
                                            modifier = Modifier.size(64.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.History,
                                                    contentDescription = null,
                                                    tint = accentColor,
                                                    modifier = Modifier.size(30.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = "No History Yet",
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Start a conversation in Chat and it will be saved here with its AI model.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .testTag("rna_ai_chats_tab_list"),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(bottom = 20.dp)
                                ) {
                                    items(savedSessions.size, key = { savedSessions[it].id }) { index ->
                                        val session = savedSessions[index]
                                        val isCurrent = !isIncognito && session.id == activeSessionId
                                        val lastMsgPreview = session.messages.lastOrNull()?.text
                                            ?.replace(Regex("\\s+"), " ")
                                            ?.trim()
                                            .orEmpty()
                                        val savedModel = session.lastModelUsed
                                            ?: session.messages.lastOrNull { it.role == "ai" && !it.modelUsedBadge.isNullOrBlank() }?.modelUsedBadge
                                        var cardFocused by remember { mutableStateOf(false) }
                                        var delFocused by remember { mutableStateOf(false) }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = when {
                                                    cardFocused -> Color.White
                                                    isCurrent -> accentColor.copy(alpha = 0.16f)
                                                    else -> Color(0xFF14192A)
                                                },
                                                border = BorderStroke(
                                                    width = if (cardFocused || isCurrent) 2.dp else 1.dp,
                                                    color = when {
                                                        cardFocused -> accentColor
                                                        isCurrent -> accentColor.copy(alpha = 0.85f)
                                                        else -> Color(0x26FFFFFF)
                                                    }
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .then(
                                                        if (index == 0) Modifier.focusRequester(firstExploreCardFocusRequester)
                                                        else Modifier
                                                    )
                                                    .onFocusChanged { cardFocused = it.isFocused }
                                                    .onPreviewKeyEvent { ev ->
                                                        if (ev.type == KeyEventType.KeyDown &&
                                                            ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                                                        ) {
                                                            runCatching { exploreNavFocusRequester.requestFocus() }
                                                            true
                                                        } else false
                                                    }
                                                    .clickable {
                                                        isIncognito = false
                                                        activeSessionId = session.id
                                                        activeSection = 0
                                                    }
                                                    .focusable()
                                                    .testTag("rna_ai_saved_chat_card_$index")
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 20.dp, vertical = 15.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = session.title,
                                                            color = if (cardFocused) Color(0xFF0F172A) else Color.White,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 16.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Text(
                                                            text = dateFormatter.format(Date(session.updatedAt)),
                                                            color = if (cardFocused) Color(0xFF475569) else Color(0xFF94A3B8),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }

                                                    if (lastMsgPreview.isNotBlank()) {
                                                        Text(
                                                            text = lastMsgPreview,
                                                            color = if (cardFocused) Color(0xFF334155) else Color(0xFFCBD5E1),
                                                            fontSize = 12.5.sp,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = if (cardFocused) Color(0xFFE2E8F0) else Color(0xFF1F2740)
                                                        ) {
                                                            Text(
                                                                text = "${session.messages.size} message${if (session.messages.size == 1) "" else "s"}",
                                                                color = if (cardFocused) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                                                                fontSize = 10.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                            )
                                                        }
                                                        if (!savedModel.isNullOrBlank()) {
                                                            Surface(
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = accentColor.copy(alpha = if (cardFocused) 0.25f else 0.16f)
                                                            ) {
                                                                Text(
                                                                    text = savedModel,
                                                                    color = if (cardFocused) Color(0xFF0F172A) else accentColor,
                                                                    fontSize = 10.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (delFocused) Color(0xFFEF4444) else Color(0xFF1A1E2E),
                                                border = BorderStroke(
                                                    width = if (delFocused) 2.dp else 1.dp,
                                                    color = if (delFocused) Color.White else Color(0x38EF4444)
                                                ),
                                                modifier = Modifier
                                                    .size(52.dp)
                                                    .onFocusChanged { delFocused = it.isFocused }
                                                    .clickable { deleteSavedSession(session.id) }
                                                    .focusable()
                                                    .testTag("rna_ai_delete_chat_$index")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Delete chat ${session.title}",
                                                        tint = if (delFocused) Color.White else Color(0xFFF87171),
                                                        modifier = Modifier.size(22.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── SECTION 2: RNAAI & AGENT CUSTOMIZATION SETTINGS ──
                    2 -> {
                        RnaAiSettingsSection(
                            settings = customSettings,
                            accentColor = accentColor,
                            glowPreviewColor = glowPreviewColor,
                            firstControlFocusRequester = firstSettingsControlFocusRequester,
                            onMoveLeftToSidebar = {
                                runCatching { settingsNavFocusRequester.requestFocus() }
                            },
                            onUpdateSettings = { updated ->
                                customSettings = updated
                                saveRnaAiSettings(prefs, updated)
                            }
                        )
                    }

                    // ── SECTION 0: MAIN GEMINI-STYLE TV CHAT INTERFACE ──
                    else -> {
                        val currentSavedSession = savedSessions.find { it.id == activeSessionId }
                        val currentChatModelLabel = if (isIncognito) {
                            incognitoMessages.lastOrNull { it.role == "ai" && !it.modelUsedBadge.isNullOrBlank() }?.modelUsedBadge
                        } else {
                            currentSavedSession?.lastModelUsed
                                ?: currentSavedSession?.messages?.lastOrNull { it.role == "ai" && !it.modelUsedBadge.isNullOrBlank() }?.modelUsedBadge
                        }

                        // Top Status Header (Classier glassmorphic bar)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xD9121726),
                            border = BorderStroke(1.dp, Color(0x20FFFFFF)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = if (isIncognito) "Incognito Chat" else (currentSavedSession?.title ?: "New Chat"),
                                            color = Color.White,
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.Black,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isIncognito) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0x33F59E0B),
                                                border = BorderStroke(1.dp, Color(0xFFF59E0B))
                                            ) {
                                                Text(
                                                    text = "History Off",
                                                    color = Color(0xFFFBBF24),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (!currentChatModelLabel.isNullOrBlank()) {
                                        Text(
                                            text = "Model: $currentChatModelLabel",
                                            color = accentColor.copy(alpha = 0.9f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Quick AI Models, Live & Agent Mode Buttons in Top-Right
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    RnaAiSidebarPillButton(
                                        title = "Models & Key",
                                        icon = Icons.Default.Tune,
                                        selected = false,
                                        accentColor = Color(0xFF38BDF8),
                                        onClick = { showFallbackManagerDialog = true },
                                        testTag = "rna_ai_header_models_btn"
                                    )
                                    RnaAiSidebarPillButton(
                                        title = "Live AI",
                                        icon = Icons.Default.GraphicEq,
                                        selected = false,
                                        accentColor = Color(0xFF34D399),
                                        onClick = onLaunchLiveVoiceAssistant,
                                        testTag = "rna_ai_header_live_btn"
                                    )
                                    RnaAiSidebarPillButton(
                                        title = "Agent Mode",
                                        icon = Icons.Default.SmartToy,
                                        selected = false,
                                        accentColor = glowPreviewColor,
                                        onClick = {
                                            onStartAutonomousAgentWithGoal("")
                                        },
                                        testTag = "rna_ai_header_agent_btn"
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Messages Area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            if (activeMessages.isEmpty()) {
                                // Classy Empty State Hero Welcome
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .widthIn(max = 720.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = accentColor.copy(alpha = 0.14f),
                                        border = BorderStroke(2.dp, accentColor.copy(alpha = 0.85f)),
                                        shadowElevation = 18.dp,
                                        modifier = Modifier.size(76.dp)
                                    ) {
                                        Image(
                                            painter = painterResource(id = com.example.R.drawable.ic_launcher_foreground_img),
                                            contentDescription = "RNA AI Logo",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        )
                                    }
                                    Text(
                                        text = if (isIncognito) "RNA AI Incognito Mode" else "How can RNA AI help tonight?",
                                        color = Color.White,
                                        fontSize = 27.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-0.4).sp
                                    )
                                    Text(
                                        text = "Ask anything with full memory, launch TV apps, search YouTube, or start the Autonomous TV Agent.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 13.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        items(EXPLORE_PROMPTS.take(4)) { suggestion ->
                                            var chipFocused by remember { mutableStateOf(false) }
                                            Surface(
                                                shape = RoundedCornerShape(16.dp),
                                                color = if (chipFocused) Color.White else Color(0xFF151C2E),
                                                border = BorderStroke(
                                                    width = if (chipFocused) 2.dp else 1.dp,
                                                    color = if (chipFocused) accentColor else Color(0x2EFFFFFF)
                                                ),
                                                modifier = Modifier
                                                    .onFocusChanged { chipFocused = it.isFocused }
                                                    .clickable { sendUserMessage(suggestion.prompt) }
                                                    .focusable()
                                            ) {
                                                Text(
                                                    text = suggestion.title.trim(),
                                                    color = if (chipFocused) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(
                                    state = messageListState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("rna_ai_messages_list"),
                                    verticalArrangement = Arrangement.spacedBy(14.dp),
                                    contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                                ) {
                                    items(activeMessages.size, key = { activeMessages[it].id }) { msgIndex ->
                                        val msg = activeMessages[msgIndex]
                                        val isUser = msg.role == "user"
                                        var bubbleFocused by remember { mutableStateOf(false) }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(
                                                    topStart = 22.dp,
                                                    topEnd = 22.dp,
                                                    bottomStart = if (isUser) 22.dp else 6.dp,
                                                    bottomEnd = if (isUser) 6.dp else 22.dp
                                                ),
                                                color = when {
                                                    bubbleFocused && isUser -> accentColor.copy(alpha = 0.34f)
                                                    bubbleFocused && !isUser -> Color(0xFF1C2338)
                                                    isUser -> accentColor.copy(alpha = 0.20f)
                                                    else -> Color(0xFF131828)
                                                },
                                                border = BorderStroke(
                                                    width = if (bubbleFocused) 2.dp else 1.dp,
                                                    color = when {
                                                        bubbleFocused -> Color.White
                                                        isUser -> accentColor.copy(alpha = 0.65f)
                                                        else -> Color(0x28FFFFFF)
                                                    }
                                                ),
                                                shadowElevation = if (bubbleFocused) 12.dp else 2.dp,
                                                modifier = Modifier
                                                    .widthIn(max = 720.dp)
                                                    .onFocusChanged { bubbleFocused = it.isFocused }
                                                    .onPreviewKeyEvent { ev ->
                                                        if (ev.type == KeyEventType.KeyDown) {
                                                            when (ev.nativeKeyEvent.keyCode) {
                                                                KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                                    runCatching { newChatFocusRequester.requestFocus() }
                                                                    true
                                                                }
                                                                KeyEvent.KEYCODE_DPAD_DOWN -> {
                                                                    if (msgIndex == activeMessages.lastIndex) {
                                                                        runCatching { chatInputFocusRequester.requestFocus() }
                                                                        true
                                                                    } else false
                                                                }
                                                                else -> false
                                                            }
                                                        } else false
                                                    }
                                                    .clickable {
                                                        if (!isUser) {
                                                            speakMessageText(msg.text)
                                                        }
                                                    }
                                                    .focusable()
                                                    .testTag("rna_ai_message_bubble_$msgIndex")
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Icon(
                                                                imageVector = if (isUser) Icons.AutoMirrored.Filled.Chat else Icons.Default.AutoAwesome,
                                                                contentDescription = null,
                                                                tint = if (isUser) Color.White else accentColor,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Text(
                                                                text = if (isUser) {
                                                                    "You"
                                                                } else {
                                                                    val savedBadge = msg.modelUsedBadge?.takeIf { it.isNotBlank() }
                                                                    if (savedBadge != null) "RNA AI • $savedBadge" else "RNA AI"
                                                                },
                                                                color = if (isUser) Color(0xFFE2E8F0) else accentColor,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }

                                                        if (!isUser) {
                                                            var ttsBtnFocused by remember { mutableStateOf(false) }
                                                            Surface(
                                                                shape = RoundedCornerShape(50),
                                                                color = if (ttsBtnFocused) Color.White else Color(0xFF222A42),
                                                                border = BorderStroke(
                                                                    1.dp,
                                                                    if (ttsBtnFocused) accentColor else Color(0x33FFFFFF)
                                                                ),
                                                                modifier = Modifier
                                                                    .onFocusChanged { ttsBtnFocused = it.isFocused }
                                                                    .clickable { speakMessageText(msg.text) }
                                                                    .focusable()
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                                        contentDescription = "Listen to AI reply",
                                                                        tint = if (ttsBtnFocused) Color(0xFF0F172A) else accentColor,
                                                                        modifier = Modifier.size(12.dp)
                                                                    )
                                                                    Text(
                                                                        text = if (bubbleFocused) "OK to Listen" else "Listen",
                                                                        color = if (ttsBtnFocused) Color(0xFF0F172A) else Color.White,
                                                                        fontSize = 9.5.sp,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }

                                                    if (isUser) {
                                                        Text(
                                                            text = msg.text,
                                                            color = Color.White,
                                                            fontSize = 14.5.sp,
                                                            lineHeight = 21.sp
                                                        )
                                                    } else {
                                                        RnaFormattedMarkdownText(
                                                            markdown = msg.text,
                                                            accentColor = accentColor
                                                        )
                                                    }

                                                    if (!msg.actionSummary.isNullOrBlank()) {
                                                        Surface(
                                                            shape = RoundedCornerShape(10.dp),
                                                            color = Color(0xFF0F2922),
                                                            border = BorderStroke(1.dp, Color(0xFF34D399))
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = null,
                                                                    tint = Color(0xFF34D399),
                                                                    modifier = Modifier.size(13.dp)
                                                                )
                                                                Text(
                                                                    text = msg.actionSummary,
                                                                    color = Color(0xFFA7F3D0),
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (isThinking) {
                                        item {
                                            var thinkingSeconds by remember { mutableStateOf(0) }
                                            LaunchedEffect(Unit) {
                                                val startedAtMs = System.currentTimeMillis()
                                                while (true) {
                                                    thinkingSeconds = ((System.currentTimeMillis() - startedAtMs) / 1000L).toInt()
                                                    delay(250L)
                                                }
                                            }
                                            val secText = if (thinkingSeconds == 1) "1 sec" else "$thinkingSeconds secs"
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.padding(8.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    color = accentColor,
                                                    strokeWidth = 2.5.dp,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = "Thinking($secText)...",
                                                    color = accentColor,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // ── Bottom Input Bar: Text Box + Microphone + Chat/Send Button + Live Button ──
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xF0141826),
                            border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.65f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rna_ai_bottom_input_bar")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = if (isListeningMic && liveMicTranscript.isNotBlank()) liveMicTranscript else inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = {
                                        Text(
                                            text = "Ask RNA AI anything, open a site in RNA Browser, or command the TV...",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 13.sp
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            keyboardController?.hide()
                                            sendUserMessage(inputText)
                                        }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = accentColor,
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(chatInputFocusRequester)
                                        .onPreviewKeyEvent { keyEvent ->
                                            val code = keyEvent.nativeKeyEvent.keyCode
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                when (code) {
                                                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                        runCatching { micButtonFocusRequester.requestFocus() }
                                                        true
                                                    }
                                                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                        runCatching { newChatFocusRequester.requestFocus() }
                                                        true
                                                    }
                                                    KeyEvent.KEYCODE_DPAD_CENTER,
                                                    KeyEvent.KEYCODE_ENTER,
                                                    KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                                                        keyboardController?.show()
                                                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                                        @Suppress("DEPRECATION")
                                                        imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                                                        true
                                                    }
                                                    else -> false
                                                }
                                            } else {
                                                false
                                            }
                                        }
                                        .clickable {
                                            runCatching { chatInputFocusRequester.requestFocus() }
                                            keyboardController?.show()
                                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                            @Suppress("DEPRECATION")
                                            imm?.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
                                        }
                                        .testTag("rna_ai_text_input")
                                )

                                // Microphone Button (Speak)
                                RnaAiBottomActionButton(
                                    icon = Icons.Default.Mic,
                                    label = if (isListeningMic) "Stop" else "Speak",
                                    containerColor = if (isListeningMic) Color(0xFFEF4444) else Color(0xFF1E293B),
                                    contentColor = Color.White,
                                    borderColor = if (isListeningMic) Color.White else accentColor,
                                    onClick = { triggerMicButton() },
                                    testTag = "rna_ai_mic_button",
                                    modifier = Modifier
                                        .focusRequester(micButtonFocusRequester)
                                        .onPreviewKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                when (keyEvent.nativeKeyEvent.keyCode) {
                                                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                        runCatching { chatInputFocusRequester.requestFocus() }
                                                        true
                                                    }
                                                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                        runCatching { sendButtonFocusRequester.requestFocus() }
                                                        true
                                                    }
                                                    else -> false
                                                }
                                            } else false
                                        }
                                )

                                // Chat / Send Button
                                RnaAiBottomActionButton(
                                    icon = Icons.AutoMirrored.Filled.Send,
                                    label = "Chat",
                                    containerColor = accentColor,
                                    contentColor = Color(0xFF090D16),
                                    borderColor = Color.White,
                                    onClick = {
                                        keyboardController?.hide()
                                        sendUserMessage(inputText)
                                    },
                                    testTag = "rna_ai_send_chat_button",
                                    modifier = Modifier
                                        .focusRequester(sendButtonFocusRequester)
                                        .onPreviewKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown) {
                                                when (keyEvent.nativeKeyEvent.keyCode) {
                                                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                                                        runCatching { micButtonFocusRequester.requestFocus() }
                                                        true
                                                    }
                                                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                                                        runCatching { liveButtonFocusRequester.requestFocus() }
                                                        true
                                                    }
                                                    else -> false
                                                }
                                            } else false
                                        }
                                )

                                // Live Button (Takes you to the Live Voice AI & Autonomous Agent bar)
                                RnaAiBottomActionButton(
                                    icon = Icons.Default.GraphicEq,
                                    label = "Live",
                                    containerColor = Color(0xFF065F46),
                                    contentColor = Color(0xFFA7F3D0),
                                    borderColor = Color(0xFF34D399),
                                    onClick = onLaunchLiveVoiceAssistant,
                                    testTag = "rna_ai_live_button",
                                    modifier = Modifier
                                        .focusRequester(liveButtonFocusRequester)
                                        .onPreviewKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyDown && keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                                                runCatching { sendButtonFocusRequester.requestFocus() }
                                                true
                                            } else false
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating App Creation Hover Card (for $start ... app-tools;)
        RnaAppCreationHoverCardOverlay(
            context = context,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        if (showFallbackManagerDialog) {
            FreeAiFallbackManagerDialog(
                context = context,
                onDismiss = { showFallbackManagerDialog = false }
            )
        }

        // D-Pad Navigable Dialog when "Ask Every Time" is enabled for YouTube links
        pendingYouTubeLinkUrl?.let { ytUrl ->
            val ytAppFocus = remember { FocusRequester() }
            LaunchedEffect(ytUrl) {
                delay(60L)
                runCatching { ytAppFocus.requestFocus() }
            }
            Dialog(
                onDismissRequest = { pendingYouTubeLinkUrl = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xFA121726),
                    border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.7f)),
                    shadowElevation = 24.dp,
                    modifier = Modifier
                        .widthIn(min = 420.dp, max = 540.dp)
                        .testTag("rna_ai_youtube_chooser_dialog")
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
                                shape = CircleShape,
                                color = Color(0x33EF4444),
                                border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Open YouTube Link",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = ytUrl,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        var ytFocused by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (ytFocused) Color.White else Color(0xFF1F1622),
                            border = BorderStroke(
                                width = if (ytFocused) 2.5.dp else 1.dp,
                                color = if (ytFocused) Color(0xFFEF4444) else Color(0x66EF4444)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(ytAppFocus)
                                .onFocusChanged { ytFocused = it.isFocused }
                                .clickable {
                                    pendingYouTubeLinkUrl = null
                                    if (!launchYouTubeAppForUrl(context, ytUrl)) {
                                        onOpenUrlInRnaBrowser(ytUrl)
                                    }
                                }
                                .focusable()
                                .testTag("yt_chooser_open_app_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = if (ytFocused) Color(0xFFDC2626) else Color(0xFFEF4444),
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "Open in YouTube App",
                                        color = if (ytFocused) Color(0xFF0F172A) else Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Launch directly in Android TV YouTube",
                                        color = if (ytFocused) Color(0xFF475569) else Color(0xFF94A3B8),
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }

                        var browserFocused by remember { mutableStateOf(false) }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (browserFocused) Color.White else Color(0xFF161E31),
                            border = BorderStroke(
                                width = if (browserFocused) 2.5.dp else 1.dp,
                                color = if (browserFocused) accentColor else Color(0x38FFFFFF)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { browserFocused = it.isFocused }
                                .clickable {
                                    pendingYouTubeLinkUrl = null
                                    onOpenUrlInRnaBrowser(ytUrl)
                                }
                                .focusable()
                                .testTag("yt_chooser_open_browser_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = if (browserFocused) Color(0xFF0F172A) else accentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "Open in RNA Browser",
                                        color = if (browserFocused) Color(0xFF0F172A) else Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Stay inside RNA. TV's built-in browser",
                                        color = if (browserFocused) Color(0xFF475569) else Color(0xFF94A3B8),
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
}

/**
 * Classy Markdown Renderer for RNA AI TV chat responses.
 * Renders headings (#, ##, ###), fenced code blocks (```), bullet & numbered lists,
 * **bold**, *italic*, and `inline code` cleanly instead of raw markdown characters.
 */
@Composable
private fun RnaFormattedMarkdownText(
    markdown: String,
    accentColor: Color
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.CodeBlock -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0A0F1D),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                            if (block.language.isNotBlank()) {
                                Text(
                                    text = block.language.uppercase(Locale.US),
                                    color = accentColor,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Text(
                                text = block.code,
                                color = Color(0xFFE2E8F0),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
                is MarkdownBlock.Heading -> {
                    val fontSize = when (block.level) {
                        1 -> 18.sp
                        2 -> 16.5.sp
                        else -> 15.sp
                    }
                    Text(
                        text = buildInlineMarkdownString(block.text, accentColor),
                        color = Color.White,
                        fontSize = fontSize,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = (fontSize.value + 6f).sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                is MarkdownBlock.BulletItem -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = block.marker,
                            color = accentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            lineHeight = 21.sp
                        )
                        Text(
                            text = buildInlineMarkdownString(block.text, accentColor),
                            color = Color(0xFFF1F5F9),
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildInlineMarkdownString(block.text, accentColor),
                        color = Color(0xFFF8FAFC),
                        fontSize = 14.5.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class BulletItem(val marker: String, val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
}

private fun parseMarkdownBlocks(raw: String): List<MarkdownBlock> {
    val lines = raw.replace("\r\n", "\n").lines()
    val result = mutableListOf<MarkdownBlock>()
    var inCode = false
    var codeLang = ""
    val codeBuffer = StringBuilder()
    val numberedRegex = Regex("^(\\d+\\.)\\s+(.*)$")

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("```")) {
            if (inCode) {
                result.add(MarkdownBlock.CodeBlock(codeLang, codeBuffer.toString().trimEnd()))
                codeBuffer.clear()
                codeLang = ""
                inCode = false
            } else {
                inCode = true
                codeLang = trimmed.removePrefix("```").trim()
            }
            continue
        }
        if (inCode) {
            codeBuffer.appendLine(line)
            continue
        }
        if (trimmed.isEmpty()) continue
        when {
            trimmed.startsWith("### ") -> result.add(MarkdownBlock.Heading(3, trimmed.removePrefix("### ").trim()))
            trimmed.startsWith("## ") -> result.add(MarkdownBlock.Heading(2, trimmed.removePrefix("## ").trim()))
            trimmed.startsWith("# ") -> result.add(MarkdownBlock.Heading(1, trimmed.removePrefix("# ").trim()))
            trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ") -> {
                result.add(MarkdownBlock.BulletItem("•", trimmed.drop(2).trim()))
            }
            numberedRegex.matches(trimmed) -> {
                val m = numberedRegex.find(trimmed)!!
                result.add(MarkdownBlock.BulletItem(m.groupValues[1], m.groupValues[2].trim()))
            }
            else -> result.add(MarkdownBlock.Paragraph(trimmed))
        }
    }
    if (inCode && codeBuffer.isNotEmpty()) {
        result.add(MarkdownBlock.CodeBlock(codeLang, codeBuffer.toString().trimEnd()))
    }
    return result
}

private fun buildInlineMarkdownString(text: String, accentColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length
        while (i < len) {
            when {
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end > i + 2) {
                        withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = Color.White)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text[i] == '`' -> {
                    val end = text.indexOf('`', i + 1)
                    if (end > i + 1) {
                        withStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                color = accentColor,
                                background = Color(0xFF0A0F1D)
                            )
                        ) {
                            append(" ${text.substring(i + 1, end)} ")
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                text[i] == '*' && (i + 1 < len && text[i + 1] != '*') -> {
                    val end = text.indexOf('*', i + 1)
                    if (end > i + 1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Color(0xFFE2E8F0))) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}

@Composable
private fun RnaAiSettingsSection(
    settings: RnaAiCustomizationSettings,
    accentColor: Color,
    glowPreviewColor: Color,
    firstControlFocusRequester: FocusRequester? = null,
    onMoveLeftToSidebar: (() -> Unit)? = null,
    onUpdateSettings: (RnaAiCustomizationSettings) -> Unit
) {
    val context = LocalContext.current
    var chatbotInstructionsDraft by remember(settings.chatbotCustomInstructions) {
        mutableStateOf(settings.chatbotCustomInstructions)
    }
    var agentInstructionsDraft by remember(settings.agentCustomInstructions) {
        mutableStateOf(settings.agentCustomInstructions)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("rna_ai_settings_panel"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                text = "RNA AI & Autonomous Agent Settings",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Configure Free AI Model Fallback Order, Custom Google Gemini API Key, YouTube Link Handling, and Agent Glow.",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // 0. FREE AI FALLBACK ROUTER & CUSTOM GEMINI API KEY SECTION
        item {
            ControlPanelAiRouterSection(context = context)
        }

        // 0.5. YOUTUBE LINK HANDLING SETTING (Default: Open in YouTube App)
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141826),
                border = BorderStroke(1.5.dp, Color(0x55EF4444)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color(0xFFEF4444)
                        )
                        Column {
                            Text(
                                text = "YouTube Link Handling",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Choose what happens when RNA AI opens a YouTube video or search link",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    val ytOptions = listOf(
                        Triple(YT_BEHAVIOR_YOUTUBE_APP, "Open in YouTube App (Default)", "Opens YouTube links directly in YouTube TV without asking"),
                        Triple(YT_BEHAVIOR_ASK_DIALOG, "Ask Every Time", "Show a TV popup asking whether to open in YouTube App or RNA Browser"),
                        Triple(YT_BEHAVIOR_BROWSER_ONLY, "Open in RNA Browser Only", "Always load YouTube links inside the built-in RNA Browser")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ytOptions.forEachIndexed { idx, (modeId, title, desc) ->
                            val isSelected = settings.youtubeLinkBehavior == modeId
                            var optFocused by remember { mutableStateOf(false) }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = when {
                                    optFocused -> Color.White
                                    isSelected -> Color(0x33EF4444)
                                    else -> Color(0xFF1D2336)
                                },
                                border = BorderStroke(
                                    width = if (optFocused || isSelected) 2.dp else 1.dp,
                                    color = when {
                                        optFocused -> Color(0xFFEF4444)
                                        isSelected -> Color(0xFFEF4444)
                                        else -> Color(0x28FFFFFF)
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { optFocused = it.isFocused }
                                    .onPreviewKeyEvent { ev ->
                                        if (idx == 0 &&
                                            onMoveLeftToSidebar != null &&
                                            ev.type == KeyEventType.KeyDown &&
                                            ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                                        ) {
                                            onMoveLeftToSidebar()
                                            true
                                        } else false
                                    }
                                    .clickable {
                                        onUpdateSettings(settings.copy(youtubeLinkBehavior = modeId))
                                    }
                                    .focusable()
                                    .testTag("rna_ai_yt_behavior_$modeId")
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = title,
                                            color = if (optFocused) Color(0xFF0F172A) else Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.5.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = if (optFocused) Color(0xFFDC2626) else Color(0xFF34D399),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = desc,
                                        color = if (optFocused) Color(0xFF475569) else Color(0xFF94A3B8),
                                        fontSize = 10.5.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. AGENT SCREEN OVERLAY GLOW COLOR & APP THEME COLORS
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141826),
                border = BorderStroke(1.5.dp, glowPreviewColor.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = glowPreviewColor
                        )
                        Column {
                            Text(
                                text = "Agent Screen Overlay Glow Color",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Choose the border glow color displayed around the screen when the Autonomous Agent is active (including over external apps)",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(RNAAI_COLOR_PRESETS.size) { idx ->
                            val preset = RNAAI_COLOR_PRESETS[idx]
                            val presetColor = parseComposeColor(preset.primaryHex)
                            val isSelected = settings.agentGlowColorHex.equals(preset.primaryHex, ignoreCase = true)
                            var focused by remember { mutableStateOf(false) }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (focused) Color.White else Color(0xFF1E2436),
                                border = BorderStroke(
                                    width = if (isSelected || focused) 2.5.dp else 1.dp,
                                    color = if (isSelected || focused) presetColor else Color(0x33FFFFFF)
                                ),
                                modifier = Modifier
                                    .then(
                                        if (idx == 0 && firstControlFocusRequester != null) {
                                            Modifier.focusRequester(firstControlFocusRequester)
                                        } else Modifier
                                    )
                                    .onFocusChanged { focused = it.isFocused }
                                    .onPreviewKeyEvent { ev ->
                                        if (idx == 0 &&
                                            onMoveLeftToSidebar != null &&
                                            ev.type == KeyEventType.KeyDown &&
                                            ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_LEFT
                                        ) {
                                            onMoveLeftToSidebar()
                                            true
                                        } else false
                                    }
                                    .clickable {
                                        onUpdateSettings(
                                            settings.copy(
                                                agentGlowColorHex = preset.primaryHex,
                                                agentSecondaryGlowHex = preset.secondaryHex
                                            )
                                        )
                                    }
                                    .focusable()
                                    .testTag("agent_glow_color_${preset.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(presetColor)
                                    )
                                    Text(
                                        text = preset.name,
                                        color = if (focused) Color(0xFF0F172A) else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = if (focused) Color(0xFF0F172A) else presetColor,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "RNA AI App Theme Accent Color",
                        color = Color(0xFFE2E8F0),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(RNAAI_COLOR_PRESETS) { preset ->
                            val presetColor = parseComposeColor(preset.primaryHex)
                            val isSelected = settings.appThemeColorHex.equals(preset.primaryHex, ignoreCase = true)
                            var focused by remember { mutableStateOf(false) }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (focused) Color.White else Color(0xFF1E2436),
                                border = BorderStroke(
                                    width = if (isSelected || focused) 2.5.dp else 1.dp,
                                    color = if (isSelected || focused) presetColor else Color(0x33FFFFFF)
                                ),
                                modifier = Modifier
                                    .onFocusChanged { focused = it.isFocused }
                                    .clickable {
                                        onUpdateSettings(settings.copy(appThemeColorHex = preset.primaryHex))
                                    }
                                    .focusable()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(presetColor)
                                    )
                                    Text(
                                        text = preset.name,
                                        color = if (focused) Color(0xFF0F172A) else Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. AUTONOMOUS AGENT BEHAVIOR CUSTOMIZATION
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141826),
                border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = accentColor
                        )
                        Column {
                            Text(
                                text = "Autonomous Agent Behavior & Speed",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Configure how the Autonomous Agent navigates apps, takes screenshots, and follows custom rules",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("Fast", "Balanced", "Careful").forEach { speed ->
                            val selected = settings.agentSpeedMode == speed
                            Button(
                                onClick = { onUpdateSettings(settings.copy(agentSpeedMode = speed)) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) accentColor else Color(0xFF1E2436),
                                    contentColor = if (selected) Color(0xFF090D16) else Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Speed: $speed", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automatic Visual Screenshot Verification",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Allow the Agent to request screen snapshots to verify UI state across apps and browser",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.5.sp
                            )
                        }
                        Switch(
                            checked = settings.agentAutoScreenshot,
                            onCheckedChange = { onUpdateSettings(settings.copy(agentAutoScreenshot = it)) },
                            colors = SwitchDefaults.colors(checkedTrackColor = accentColor)
                        )
                    }

                    OutlinedTextField(
                        value = agentInstructionsDraft,
                        onValueChange = {
                            agentInstructionsDraft = it
                            onUpdateSettings(settings.copy(agentCustomInstructions = it))
                        },
                        label = { Text("Custom Agent Behavior Instructions (Optional)") },
                        placeholder = { Text("e.g., Always prefer searching inside RNA Browser and confirm before clicking purchase buttons") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 3. CHATBOT & VOICE ASSISTANT BEHAVIOR CUSTOMIZATION
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF141826),
                border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = accentColor
                        )
                        Column {
                            Text(
                                text = "Chatbot & Voice Assistant Persona",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Customize how the RNA AI Chatbot and bottom AI Assistant bar respond to your questions",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val tones = listOf("Helpful & Concise", "Detailed & Deep", "Creative & Witty", "Technical Pro")
                        items(tones) { tone ->
                            val selected = settings.chatbotTone == tone
                            Button(
                                onClick = { onUpdateSettings(settings.copy(chatbotTone = tone)) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) accentColor else Color(0xFF1E2436),
                                    contentColor = if (selected) Color(0xFF090D16) else Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(tone, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = chatbotInstructionsDraft,
                        onValueChange = {
                            chatbotInstructionsDraft = it
                            onUpdateSettings(settings.copy(chatbotCustomInstructions = it))
                        },
                        label = { Text("Custom Chatbot System Prompt / Behavior") },
                        placeholder = { Text("e.g., Answer in bullet points and always suggest a related movie or web link") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun RnaAiSidebarIconBtn(
    icon: ImageVector,
    contentDescription: String,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        shape = CircleShape,
        color = if (focused) Color.White else Color(0xFF1E2436),
        border = BorderStroke(1.dp, if (focused) accentColor else Color(0x33FFFFFF)),
        modifier = Modifier
            .size(34.dp)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (focused) Color(0xFF0F172A) else Color.White,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun RnaAiSidebarPillButton(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused && !TvPerformanceMode.isEnabled) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "SidebarPillScale"
    )
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = when {
            focused -> Color.White
            selected -> accentColor.copy(alpha = 0.25f)
            else -> Color(0xFF1A2032)
        },
        border = BorderStroke(
            width = if (focused || selected) 1.5.dp else 1.dp,
            color = if (focused || selected) accentColor else Color(0x28FFFFFF)
        ),
        modifier = modifier
            .scale(scale)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (focused) Color(0xFF0F172A) else accentColor,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = title,
                color = if (focused) Color(0xFF0F172A) else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun RnaAiSidebarNavRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    onMoveRight: (() -> Unit)? = null,
    testTag: String,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = when {
            focused -> Color.White
            selected -> accentColor.copy(alpha = 0.22f)
            else -> Color.Transparent
        },
        border = BorderStroke(
            width = if (focused || selected) 1.5.dp else 0.dp,
            color = if (focused || selected) accentColor else Color.Transparent
        ),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { keyEvent ->
                if (onMoveRight != null &&
                    keyEvent.type == KeyEventType.KeyDown &&
                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
                ) {
                    onMoveRight()
                    true
                } else {
                    false
                }
            }
            .clickable { onClick() }
            .focusable()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (focused) Color(0xFF0F172A) else accentColor,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (focused) Color(0xFF0F172A) else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = subtitle,
                    color = if (focused) Color(0xFF475569) else Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RnaAiBottomActionButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused && !TvPerformanceMode.isEnabled) 1.07f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "BottomBtnScale"
    )
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (focused) Color.White else containerColor,
        border = BorderStroke(if (focused) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .scale(scale)
            .onFocusChanged { focused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (focused) Color(0xFF0F172A) else contentColor,
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = label,
                color = if (focused) Color(0xFF0F172A) else contentColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp
            )
        }
    }
}

private data class RnaAiParsedResponse(
    val replyText: String,
    val openAppTarget: String? = null,
    val openBrowserUrl: String? = null,
    val agentGoal: String? = null,
    val toolsSummary: String? = null,
    val modelUsedBadge: String? = null
)

private suspend fun generateRnaAiChatResponse(
    context: Context,
    history: List<RnaAiChatMessage>,
    installedApps: List<UnifiedLauncherApp>,
    settings: RnaAiCustomizationSettings
): RnaAiParsedResponse = withContext(Dispatchers.IO) {
    val rnaToolsSystemSection = RnaAiToolsEngine.buildRnaToolsAndAppToolsSystemPrompt(context, installedApps)
    val conversationContext = history.takeLast(14).joinToString("\n") { m ->
        val speaker = if (m.role == "user") "USER" else "RNAAI"
        "$speaker: ${m.text}"
    }

    val prompt = """
        You are RNAAI (powered by EdVanced 1.1 Pro), the full-screen Smart TV AI companion inside RNA. TV.
        Tone preference: ${settings.chatbotTone}
        ${if (settings.chatbotCustomInstructions.isNotBlank()) "Custom User Behavior Instructions: ${settings.chatbotCustomInstructions}" else ""}

        $rnaToolsSystemSection

        CAPABILITIES & ACTION TAGS:
        1. Answer any question conversationally using the previous chat history context.
        2. NEVER open an external browser app on the device. Whenever the user asks to open a website, search the web, or open something in the browser, ALWAYS use the internal RNA Browser tag:
           [OPEN_BROWSER: https://...]
           (For search queries, use [OPEN_BROWSER: https://www.google.com/search?q=...] or for YouTube in browser use [OPEN_BROWSER: https://www.youtube.com/results?search_query=...])
        3. If the user asks to launch an installed TV app, you can use `${'$'}start [package_name] rna-tools;` or `[OPEN_APP: package_or_app_name]`.
        4. If the user asks to change a wallpaper, profile, or Control Panel setting, emit the matching `${'$'}start [command] rna-tools;` directive.
        5. If the user asks to create or build an app, emit `${'$'}start app-name.[Name] app-tools;`, `${'$'}start app-name.[Name].img-gen app-tools;`, and either `${'$'}start app-url.[url] app-tools;` or `${'$'}start app-html.{<!DOCTYPE html>...} app-tools;` (never dump raw HTML outside the `app-html.{...}` directive).
        6. If the user asks the autonomous agent to perform a multi-step action on the screen, include:
           [AGENT_GOAL: <goal description>]

        CONVERSATION HISTORY:
        $conversationContext

        Respond to the latest USER message now:
    """.trimIndent()

    var rawOutput = ""
    var modelBadge: String? = null
    val latestUserMessageText = history.lastOrNull { it.role == "user" }?.text?.trim()
    try {
        val outcome = RnaFreeAiRouter.generateTextOrVision(
            context = context,
            prompt = prompt,
            temperature = 0.35f,
            source = "RNA. AI Chat",
            userDisplayPrompt = latestUserMessageText
        )
        rawOutput = outcome.text
        modelBadge = "${outcome.modelUsedName} (${outcome.providerUsed} • ${outcome.reliabilityScore}% rel)"
    } catch (e: Exception) {
        rawOutput = e.localizedMessage
            ?: "All free AI models are currently busy. You can manage fallback order or add your own Gemini API key in Control Panel > System & AI."
    }

    val toolsReport = withContext(Dispatchers.Main) {
        RnaAiToolsEngine.processAndExecuteTools(context, rawOutput)
    }

    val openBrowserRegex = Regex("""\[OPEN_BROWSER:\s*([^\]]+)\]""", RegexOption.IGNORE_CASE)
    val openAppRegex = Regex("""\[OPEN_APP:\s*([^\]]+)\]""", RegexOption.IGNORE_CASE)
    val agentGoalRegex = Regex("""\[AGENT_GOAL:\s*([^\]]+)\]""", RegexOption.IGNORE_CASE)

    val browserUrl = openBrowserRegex.find(toolsReport.cleanedText)?.groupValues?.getOrNull(1)?.trim()
    val appTarget = openAppRegex.find(toolsReport.cleanedText)?.groupValues?.getOrNull(1)?.trim()
    val agentGoal = agentGoalRegex.find(toolsReport.cleanedText)?.groupValues?.getOrNull(1)?.trim()

    val cleanReply = toolsReport.cleanedText
        .replace(openBrowserRegex, "")
        .replace(openAppRegex, "")
        .replace(agentGoalRegex, "")
        .trim()
        .ifBlank {
            if (toolsReport.executedSummaries.isNotEmpty()) {
                "Done! ${toolsReport.executedSummaries.joinToString(", ")}."
            } else {
                "Done! Executing that for you now."
            }
        }

    RnaAiParsedResponse(
        replyText = cleanReply,
        openAppTarget = appTarget,
        openBrowserUrl = browserUrl,
        agentGoal = agentGoal,
        toolsSummary = toolsReport.executedSummaries.takeIf { it.isNotEmpty() }?.joinToString(" • "),
        modelUsedBadge = modelBadge
    )
}

