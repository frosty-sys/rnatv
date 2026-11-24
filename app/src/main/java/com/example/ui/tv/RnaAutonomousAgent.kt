package com.example.ui.tv

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.AudioManager
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.CyanPrimary
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private const val AGENT_TAG = "RnaAutonomousAgent"
private const val AGENT_MODEL_NAME = "gemini-3.5-flash-lite"

/**
 * Represents a clickable button, link, or input field extracted live from the active website DOM.
 */
data class SiteToolElement(
    val slugId: String,
    val tag: String,
    val inputType: String,
    val label: String
)

/**
 * Unified command model covering all autonomous Agentic actions:
 * - `$start screenshot tool;`
 * - `$start d-pad.ok tool;`, `$start d-pad.up tool;`, `$start d-pad.down tool;`, `$start d-pad.left tool;`, `$start d-pad.right tool;`
 * - `$start d-pad.ok[hold] tool;`, `$start d-pad.ok[double] tool;`
 * - `$start volume.up tool;`, `$start volume.down tool;`, `$start home tool;`, `$start back tool;`
 * - `$start [button-name] browser-tools;`
 * - `$start [element-slug] site-tools;`
 * - `$start {text} type;`
 * - `$start [package] app;`
 * - `$start [url] browser;`
 */
sealed interface AgenticCommand {
    data object TakeScreenshot : AgenticCommand
    data class HardwareTool(val rawAction: String) : AgenticCommand
    data class BrowserTool(val toolName: String) : AgenticCommand
    data class SiteTool(val elementSlug: String) : AgenticCommand
    data class TypeText(val textToType: String) : AgenticCommand
    data class OpenApp(val packageOrName: String) : AgenticCommand
    data class OpenBrowserUrl(val url: String) : AgenticCommand
}

data class AgentStepOutput(
    val spokenSummary: String,
    val commands: List<AgenticCommand>,
    val isGoalComplete: Boolean
)

/**
 * Live bridge between the RNA Browser (`TvBrowserTab`) and the Autonomous AI Agent.
 */
object BrowserAgentBridge {
    @Volatile
    var isBrowserVisible: Boolean = false
    @Volatile
    var currentUrl: String = ""
    @Volatile
    var currentTitle: String = ""
    @Volatile
    var currentInputMode: String = "DPAD"
    @Volatile
    var cursorX: Float = 0f
    @Volatile
    var cursorY: Float = 0f
    @Volatile
    var mouseSpeedMultiplier: Float = 1.0f
    @Volatile
    var suppressUserInputPopupUntilMs: Long = 0L

    private val _siteTools = MutableStateFlow<List<SiteToolElement>>(emptyList())
    val siteTools: StateFlow<List<SiteToolElement>> = _siteTools.asStateFlow()

    var onExecuteBrowserTool: ((String) -> Boolean)? = null
    var onExecuteSiteTool: ((String) -> Boolean)? = null
    var onExecuteTypeInBrowser: ((String) -> Boolean)? = null
    var onExecuteBrowserKeyOrHold: ((String) -> Boolean)? = null
    var onCaptureWebViewBitmap: (() -> Bitmap?)? = null
    var onRequestFocusWebCanvas: (() -> Boolean)? = null

    fun updateSiteTools(list: List<SiteToolElement>) {
        _siteTools.value = list
    }

    fun clearBridge() {
        isBrowserVisible = false
        suppressUserInputPopupUntilMs = 0L
        _siteTools.value = emptyList()
        onExecuteBrowserTool = null
        onExecuteSiteTool = null
        onExecuteTypeInBrowser = null
        onExecuteBrowserKeyOrHold = null
        onCaptureWebViewBitmap = null
        onRequestFocusWebCanvas = null
    }
}

/**
 * Global Autonomous Agent Controller that persists across Launcher, Browser, RNA Web Apps,
 * and external apps (via RnaHomeInterceptorService overlay).
 */
object RnaAutonomousAgentController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isAgentModeActive = MutableStateFlow(false)
    val isAgentModeActive: StateFlow<Boolean> = _isAgentModeActive.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    private val _statusText = MutableStateFlow("Agent Ready — Press Mic to tell EdVanced 1.1 Pro what to do")
    val statusText: StateFlow<String> = _statusText.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _glowColorHex = MutableStateFlow("#38BDF8")
    val glowColorHex: StateFlow<String> = _glowColorHex.asStateFlow()

    private val _secondaryGlowColorHex = MutableStateFlow("#2563EB")
    val secondaryGlowColorHex: StateFlow<String> = _secondaryGlowColorHex.asStateFlow()

    private var agentSpeedMode: String = "Balanced"
    private var agentAutoScreenshot: Boolean = true
    private var agentCustomInstructions: String = ""

    // Context history & pending command queue so Pause/Resume and Mic Interruption continue seamlessly
    private val conversationHistory = mutableListOf<String>()
    private val pendingCommandsQueue = ArrayDeque<AgenticCommand>()
    private var currentGoal: String = ""
    private var latestScreenshotBitmap: Bitmap? = null
    private var agentLoopJob: Job? = null

    // Callbacks wired by active host (Launcher / Overlay / Accessibility Service)
    var activeActivityRef: WeakReference<Activity>? = null
    var onLaunchAppCallback: ((String) -> Unit)? = null
    var onOpenUrlInBrowserCallback: ((String) -> Unit)? = null
    var onNavigateHomeCallback: (() -> Unit)? = null
    var onSpeakReplyCallback: ((String) -> Unit)? = null
    var onPerformGlobalKeyOrAction: ((String) -> Boolean)? = null
    var onPerformAccessibilityType: ((String) -> Boolean)? = null
    var onTakeAccessibilityScreenshot: ((suspend () -> Bitmap?)?) = null
    var onSyncSystemOverlayCallback: (() -> Unit)? = null
    var onStartSystemOverlayMicCallback: (() -> Unit)? = null
    var onReturnFocusToAgentBarCallback: (() -> Unit)? = null
    var installedAppsSnapshot: List<UnifiedLauncherApp> = emptyList()

    fun applyCustomizationSettings(settings: RnaAiCustomizationSettings) {
        _glowColorHex.value = settings.agentGlowColorHex.ifBlank { "#38BDF8" }
        _secondaryGlowColorHex.value = settings.agentSecondaryGlowHex.ifBlank { "#2563EB" }
        agentSpeedMode = settings.agentSpeedMode
        agentAutoScreenshot = settings.agentAutoScreenshot
        agentCustomInstructions = settings.agentCustomInstructions
        notifySystemOverlay()
    }

    private fun notifySystemOverlay() {
        scope.launch(Dispatchers.Main) {
            onSyncSystemOverlayCallback?.invoke()
        }
    }

    fun isAgentActivelyControlling(): Boolean = _isAgentModeActive.value && !_isPaused.value

    fun enterAgentMode() {
        _isAgentModeActive.value = true
        _isPaused.value = false
        _isWorking.value = false
        _isListening.value = false
        _liveTranscript.value = ""
        _statusText.value = "Agent Mode Active — Press Mic to give your instruction"
        notifySystemOverlay()
    }

    fun exitAgentMode() {
        agentLoopJob?.cancel()
        agentLoopJob = null
        pendingCommandsQueue.clear()
        conversationHistory.clear()
        currentGoal = ""
        latestScreenshotBitmap = null
        _isListening.value = false
        _isWorking.value = false
        _isPaused.value = false
        _isAgentModeActive.value = false
        notifySystemOverlay()
    }

    fun togglePauseResume() {
        if (!_isAgentModeActive.value) return
        val nextPaused = !_isPaused.value
        _isPaused.value = nextPaused
        if (nextPaused) {
            _statusText.value = "Agent Paused — Press Play to continue from where it left off"
        } else {
            _statusText.value = "Resuming Agent with context..."
            if (currentGoal.isNotBlank() && (agentLoopJob == null || agentLoopJob?.isActive != true)) {
                startOrResumeAutonomousLoop()
            }
        }
        notifySystemOverlay()
    }

    /**
     * Called when the user presses the Microphone button in Agent Mode.
     * If the AI is currently working, this immediately interrupts the AI so it listens to the user
     * and then continues its work with the updated instruction!
     */
    fun prepareForUserVoiceInterruption() {
        agentLoopJob?.cancel()
        agentLoopJob = null
        _isWorking.value = false
        _isPaused.value = false
        _liveTranscript.value = ""
        _statusText.value = if (currentGoal.isBlank()) {
            "Listening... Tell the Agent what to do"
        } else {
            "Interrupted — Listening to your update..."
        }
        notifySystemOverlay()
    }

    fun setListeningState(listening: Boolean, partialText: String = "") {
        _isListening.value = listening
        if (partialText.isNotBlank()) {
            _liveTranscript.value = partialText
        }
        notifySystemOverlay()
    }

    fun submitUserInstruction(instruction: String) {
        val cleaned = instruction.trim()
        if (cleaned.isEmpty()) return
        _isListening.value = false
        _liveTranscript.value = cleaned
        _isPaused.value = false

        if (currentGoal.isBlank()) {
            currentGoal = cleaned
            conversationHistory.add("USER GOAL: $cleaned")
        } else {
            currentGoal = "$currentGoal | USER UPDATE: $cleaned"
            conversationHistory.add("USER INTERRUPTION / NEW INSTRUCTION: $cleaned")
        }
        notifySystemOverlay()
        startOrResumeAutonomousLoop()
    }

    private fun startOrResumeAutonomousLoop() {
        agentLoopJob?.cancel()
        agentLoopJob = scope.launch {
            _isWorking.value = true
            notifySystemOverlay()
            var stepCount = 0
            val maxAutonomousSteps = 8

            try {
                while (isActive && _isAgentModeActive.value && stepCount < maxAutonomousSteps) {
                    // Wait while paused
                    while (_isPaused.value && isActive && _isAgentModeActive.value) {
                        delay(200L)
                    }
                    if (!isActive || !_isAgentModeActive.value) break

                    // 1. If there are remaining queued commands from before a pause, execute them first in order!
                    if (pendingCommandsQueue.isNotEmpty()) {
                        val requestedScreenshot = drainCommandQueueRespectingPause()
                        if (!requestedScreenshot && pendingCommandsQueue.isEmpty()) {
                            // Continue to next reasoning turn if needed
                        }
                    }

                    while (_isPaused.value && isActive && _isAgentModeActive.value) {
                        delay(200L)
                    }
                    if (!isActive || !_isAgentModeActive.value) break

                    stepCount++
                    val thinkStartedAtMs = System.currentTimeMillis()
                    _statusText.value = "Thinking(0 secs)... (Step $stepCount)"
                    notifySystemOverlay()

                    val thinkingTimerJob = launch {
                        while (isActive) {
                            val secs = ((System.currentTimeMillis() - thinkStartedAtMs) / 1000L).toInt()
                            val secLabel = if (secs == 1) "1 sec" else "$secs secs"
                            _statusText.value = "Thinking($secLabel)... (Step $stepCount)"
                            notifySystemOverlay()
                            delay(500L)
                        }
                    }

                    val stepResult = try {
                        withTimeout(60_000L) {
                            queryAgentModel(
                                goal = currentGoal,
                                history = conversationHistory.takeLast(12),
                                screenshot = latestScreenshotBitmap
                            )
                        }
                    } finally {
                        thinkingTimerJob.cancel()
                    }
                    // Consume screenshot once sent
                    latestScreenshotBitmap = null

                    if (stepResult.spokenSummary.isNotBlank()) {
                        _statusText.value = stepResult.spokenSummary
                        notifySystemOverlay()
                        onSpeakReplyCallback?.invoke(stepResult.spokenSummary)
                    }

                    conversationHistory.add("AGENT RESPONSE: ${stepResult.spokenSummary} | COMMANDS: ${stepResult.commands.size}")

                    if (stepResult.commands.isEmpty()) {
                        _isWorking.value = false
                        notifySystemOverlay()
                        break
                    }

                    pendingCommandsQueue.addAll(stepResult.commands)
                    val requestedScreenshot = drainCommandQueueRespectingPause()

                    // Continue autonomous loop if AI explicitly asked for a screenshot or opened a browser/site tool to inspect next
                    val shouldContinueLoop = requestedScreenshot || (!stepResult.isGoalComplete && stepCount < maxAutonomousSteps)
                    if (!shouldContinueLoop) {
                        _isWorking.value = false
                        notifySystemOverlay()
                        break
                    }
                    val stepDelay = when (agentSpeedMode) {
                        "Fast" -> 220L
                        "Careful" -> 700L
                        else -> 420L
                    }
                    delay(stepDelay)
                }
            } catch (e: TimeoutCancellationException) {
                _statusText.value = "Agent step timed out — press Mic or Play to retry."
                notifySystemOverlay()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(AGENT_TAG, "Autonomous Agent error", e)
                _statusText.value = "Agent error: ${e.localizedMessage ?: "Check connection"}"
                notifySystemOverlay()
            } finally {
                if (!_isPaused.value) {
                    _isWorking.value = false
                    notifySystemOverlay()
                }
            }
        }
    }

    fun isExternalBrowserNameOrPackage(target: String): Boolean {
        val lower = target.trim().lowercase(Locale.US)
        return lower in setOf(
            "browser",
            "web browser",
            "internet",
            "chrome",
            "google chrome",
            "com.android.chrome",
            "com.android.browser",
            "org.mozilla.firefox",
            "firefox",
            "opera",
            "edge",
            "com.microsoft.emmx",
            "com.opera.browser",
            "com.brave.browser",
            "webview"
        ) || lower.contains(".browser") || lower.contains("chrome")
    }

    /**
     * Processes all queued commands sequentially in order, pausing immediately if the user presses Pause.
     * Returns true if a `$start screenshot tool;` was executed so the loop immediately feeds the screenshot back to the AI.
     */
    private suspend fun drainCommandQueueRespectingPause(): Boolean {
        var tookScreenshot = false
        while (pendingCommandsQueue.isNotEmpty() && _isAgentModeActive.value) {
            while (_isPaused.value && _isAgentModeActive.value) {
                delay(200L)
            }
            if (!_isAgentModeActive.value) break

            val cmd = pendingCommandsQueue.removeFirst()
            when (cmd) {
                is AgenticCommand.TakeScreenshot -> {
                    _statusText.value = "Taking screen snapshot..."
                    notifySystemOverlay()
                    delay(250L)
                    val bmp = captureCurrentScreenBitmap()
                    latestScreenshotBitmap = bmp
                    conversationHistory.add(
                        if (bmp != null) "TOOL RESULT: Screenshot captured (${bmp.width}x${bmp.height})."
                        else "TOOL RESULT: Screenshot attempted (unavailable on current surface)."
                    )
                    tookScreenshot = true
                }

                is AgenticCommand.HardwareTool -> {
                    _statusText.value = "Pressing ${cmd.rawAction}..."
                    notifySystemOverlay()
                    if (BrowserAgentBridge.isBrowserVisible) {
                        BrowserAgentBridge.suppressUserInputPopupUntilMs = android.os.SystemClock.elapsedRealtime() + 1800L
                    }
                    executeHardwareToolCommand(cmd.rawAction)
                    conversationHistory.add("TOOL EXECUTED: \$start ${cmd.rawAction} tool;")
                    delay(260L)
                }

                is AgenticCommand.BrowserTool -> {
                    _statusText.value = "Browser tool: ${cmd.toolName}..."
                    notifySystemOverlay()
                    BrowserAgentBridge.suppressUserInputPopupUntilMs = android.os.SystemClock.elapsedRealtime() + 2000L
                    val ok = BrowserAgentBridge.onExecuteBrowserTool?.invoke(cmd.toolName) ?: false
                    conversationHistory.add("BROWSER-TOOL EXECUTED: \$start ${cmd.toolName} browser-tools; (success=$ok)")
                    delay(350L)
                }

                is AgenticCommand.SiteTool -> {
                    _statusText.value = "Clicking site element: ${cmd.elementSlug}..."
                    notifySystemOverlay()
                    BrowserAgentBridge.suppressUserInputPopupUntilMs = android.os.SystemClock.elapsedRealtime() + 2000L
                    val ok = BrowserAgentBridge.onExecuteSiteTool?.invoke(cmd.elementSlug) ?: false
                    conversationHistory.add("SITE-TOOL EXECUTED: \$start ${cmd.elementSlug} site-tools; (success=$ok)")
                    delay(450L)
                }

                is AgenticCommand.TypeText -> {
                    _statusText.value = "Typing \"${cmd.textToType}\"..."
                    notifySystemOverlay()
                    if (BrowserAgentBridge.isBrowserVisible) {
                        BrowserAgentBridge.suppressUserInputPopupUntilMs = android.os.SystemClock.elapsedRealtime() + 2000L
                    }
                    val typedInBrowser = if (BrowserAgentBridge.isBrowserVisible) {
                        BrowserAgentBridge.onExecuteTypeInBrowser?.invoke(cmd.textToType) ?: false
                    } else false
                    val typedInSystem = if (!typedInBrowser) {
                        executeSystemTextInput(cmd.textToType)
                    } else true
                    conversationHistory.add("TYPE EXECUTED: \$start {${cmd.textToType}} type; (success=$typedInSystem)")
                    delay(350L)
                }

                is AgenticCommand.OpenApp -> {
                    if (isExternalBrowserNameOrPackage(cmd.packageOrName)) {
                        // Never open an external browser app on the device; always open the RNA Browser tab inside RNA!
                        _statusText.value = "Opening RNA Browser..."
                        notifySystemOverlay()
                        onOpenUrlInBrowserCallback?.invoke("https://www.google.com")
                        conversationHistory.add("BROWSER OPENED IN RNA APP: \$start https://www.google.com browser;")
                        delay(900L)
                    } else {
                        _statusText.value = "Opening app ${cmd.packageOrName}..."
                        notifySystemOverlay()
                        onLaunchAppCallback?.invoke(cmd.packageOrName)
                        conversationHistory.add("APP OPENED: \$start ${cmd.packageOrName} app;")
                        delay(900L)
                    }
                }

                is AgenticCommand.OpenBrowserUrl -> {
                    val targetUrl = if (cmd.url.startsWith("http://") || cmd.url.startsWith("https://")) {
                        cmd.url
                    } else if (cmd.url.contains(".") && !cmd.url.contains(" ")) {
                        "https://${cmd.url}"
                    } else {
                        "https://www.google.com/search?q=${android.net.Uri.encode(cmd.url)}"
                    }
                    _statusText.value = "Opening in RNA Browser..."
                    notifySystemOverlay()
                    onOpenUrlInBrowserCallback?.invoke(targetUrl)
                    conversationHistory.add("BROWSER OPENED IN RNA APP: \$start $targetUrl browser;")
                    delay(1200L)
                }
            }
        }
        return tookScreenshot
    }

    private suspend fun captureCurrentScreenBitmap(): Bitmap? = withContext(Dispatchers.Main) {
        try {
            // 1. Prefer accessibility screenshot if service is connected (works over any app!)
            val a11yShot = onTakeAccessibilityScreenshot?.invoke()
            if (a11yShot != null) return@withContext scaleDownBitmap(a11yShot)

            // 2. If Browser is visible and has a WebView, capture WebView + window
            val act = activeActivityRef?.get()
            if (act != null) {
                val rootView = act.window?.decorView?.rootView
                if (rootView != null && rootView.width > 0 && rootView.height > 0) {
                    val bmp = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    rootView.draw(canvas)
                    return@withContext scaleDownBitmap(bmp)
                }
            }
            val wvBmp = BrowserAgentBridge.onCaptureWebViewBitmap?.invoke()
            if (wvBmp != null) return@withContext scaleDownBitmap(wvBmp)
        } catch (e: Exception) {
            Log.w(AGENT_TAG, "Screenshot capture fallback failed", e)
        }
        null
    }

    private fun scaleDownBitmap(src: Bitmap): Bitmap {
        val maxW = 960
        val maxH = 540
        if (src.width <= maxW && src.height <= maxH) return src
        val ratio = minOf(maxW.toFloat() / src.width, maxH.toFloat() / src.height)
        val w = (src.width * ratio).toInt().coerceAtLeast(1)
        val h = (src.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private suspend fun executeHardwareToolCommand(rawAction: String) = withContext(Dispatchers.Main) {
        val normalized = rawAction.trim().lowercase(Locale.US).replace(" ", "")
        val isHold = normalized.contains("[hold]")
        val isDouble = normalized.contains("[double]")
        val baseKey = normalized
            .replace("[hold]", "")
            .replace("[double]", "")
            .replace("d-pad.ok.left", "d-pad.left")

        // If Browser is active and can handle D-Pad / Hold OK / Mouse move directly:
        if (BrowserAgentBridge.isBrowserVisible) {
            val handledByBrowser = BrowserAgentBridge.onExecuteBrowserKeyOrHold?.invoke(normalized) ?: false
            if (handledByBrowser) return@withContext
        }

        // Delegate to Accessibility Service if available
        if (onPerformGlobalKeyOrAction?.invoke(normalized) == true) {
            return@withContext
        }

        val act = activeActivityRef?.get()
        val audioManager = act?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

        when (baseKey) {
            "volume.up", "d-pad.volume_up", "volume_up" -> {
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
                return@withContext
            }
            "volume.down", "d-pad.volume_down", "volume_down" -> {
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
                return@withContext
            }
            "volume.mute", "mute" -> {
                audioManager?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_TOGGLE_MUTE,
                    AudioManager.FLAG_SHOW_UI
                )
                return@withContext
            }
            "home", "d-pad.home" -> {
                onNavigateHomeCallback?.invoke()
                return@withContext
            }
            "back", "d-pad.back" -> {
                val now = SystemClock.uptimeMillis()
                act?.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0))
                act?.dispatchKeyEvent(KeyEvent(now, now + 20L, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0))
                return@withContext
            }
        }

        val keyCode = when (baseKey) {
            "d-pad.ok", "ok", "enter", "center" -> KeyEvent.KEYCODE_DPAD_CENTER
            "d-pad.up", "up" -> KeyEvent.KEYCODE_DPAD_UP
            "d-pad.down", "down" -> KeyEvent.KEYCODE_DPAD_DOWN
            "d-pad.left", "left" -> KeyEvent.KEYCODE_DPAD_LEFT
            "d-pad.right", "right" -> KeyEvent.KEYCODE_DPAD_RIGHT
            "menu", "d-pad.menu" -> KeyEvent.KEYCODE_MENU
            "play_pause", "media.play_pause" -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            else -> null
        } ?: return@withContext

        if (act != null) {
            if (isHold) {
                val downTime = SystemClock.uptimeMillis()
                act.dispatchKeyEvent(KeyEvent(downTime, downTime, KeyEvent.ACTION_DOWN, keyCode, 0))
                delay(460L)
                act.dispatchKeyEvent(KeyEvent(downTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keyCode, 1))
                act.dispatchKeyEvent(KeyEvent(downTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0))
            } else if (isDouble) {
                repeat(2) {
                    val now = SystemClock.uptimeMillis()
                    act.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
                    act.dispatchKeyEvent(KeyEvent(now, now + 25L, KeyEvent.ACTION_UP, keyCode, 0))
                    delay(85L)
                }
            } else {
                val now = SystemClock.uptimeMillis()
                act.dispatchKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
                act.dispatchKeyEvent(KeyEvent(now, now + 30L, KeyEvent.ACTION_UP, keyCode, 0))
            }
        }
    }

    private suspend fun executeSystemTextInput(text: String): Boolean = withContext(Dispatchers.Main) {
        if (onPerformAccessibilityType?.invoke(text) == true) {
            return@withContext true
        }
        val act = activeActivityRef?.get() ?: return@withContext false
        val focusedView = act.currentFocus
        if (focusedView is android.widget.EditText) {
            focusedView.setText(text)
            focusedView.setSelection(text.length)
            return@withContext true
        }
        false
    }

    /**
     * Parses all agentic `$start ...;` commands in order from the AI's response.
     */
    fun parseAgentResponse(rawText: String): AgentStepOutput {
        val commands = mutableListOf<AgenticCommand>()
        val generalRegex = Regex(
            pattern = "\\\$start\\s+(?:\\{([^}]*)\\}\\s+type|(.+?)\\s+(tool|browser-tools|site-tools|type|app|browser))\\s*;?",
            option = RegexOption.IGNORE_CASE
        )

        generalRegex.findAll(rawText).forEach { match ->
            val bracedText = match.groups[1]?.value
            if (bracedText != null) {
                commands.add(AgenticCommand.TypeText(bracedText))
            } else {
                val rawArg = match.groups[2]?.value?.trim().orEmpty()
                val category = match.groups[3]?.value?.trim()?.lowercase(Locale.US).orEmpty()
                when (category) {
                    "tool" -> {
                        if (rawArg.equals("screenshot", ignoreCase = true)) {
                            commands.add(AgenticCommand.TakeScreenshot)
                        } else if (rawArg.isNotEmpty()) {
                            commands.add(AgenticCommand.HardwareTool(rawArg))
                        }
                    }
                    "browser-tools" -> {
                        if (rawArg.isNotEmpty()) commands.add(AgenticCommand.BrowserTool(rawArg.lowercase(Locale.US)))
                    }
                    "site-tools" -> {
                        if (rawArg.isNotEmpty()) commands.add(AgenticCommand.SiteTool(rawArg))
                    }
                    "type" -> {
                        val cleaned = rawArg.removePrefix("{").removeSuffix("}")
                        commands.add(AgenticCommand.TypeText(cleaned))
                    }
                    "app" -> {
                        if (rawArg.isNotEmpty()) commands.add(AgenticCommand.OpenApp(rawArg))
                    }
                    "browser" -> {
                        if (rawArg.isNotEmpty()) commands.add(AgenticCommand.OpenBrowserUrl(rawArg))
                    }
                }
            }
        }

        val doneMarker = rawText.contains("[DONE]", ignoreCase = true) ||
            (commands.isEmpty() && !rawText.contains("\$start", ignoreCase = true))

        val stripped = rawText
            .replace(generalRegex, "")
            .replace(Regex("\\\$start[^\\n;]*;?", RegexOption.IGNORE_CASE), "")
            .replace("[DONE]", "", ignoreCase = true)
            .replace(Regex("\\s+"), " ")
            .trim()

        val displaySummary = if (stripped.isNotBlank()) {
            stripped
        } else if (commands.isNotEmpty()) {
            "Executing ${commands.size} autonomous action(s)..."
        } else {
            "Task completed."
        }

        return AgentStepOutput(
            spokenSummary = displaySummary,
            commands = commands,
            isGoalComplete = doneMarker && !commands.any { it is AgenticCommand.TakeScreenshot }
        )
    }

    private suspend fun queryAgentModel(
        goal: String,
        history: List<String>,
        screenshot: Bitmap?
    ): AgentStepOutput = withContext(Dispatchers.IO) {
        val actCtx = activeActivityRef?.get()?.applicationContext
        val rnaToolsSystemSection = if (actCtx != null) {
            RnaAiToolsEngine.buildRnaToolsAndAppToolsSystemPrompt(actCtx, installedAppsSnapshot)
        } else {
            buildString {
                installedAppsSnapshot.take(60).forEach { app ->
                    appendLine("- \"${app.title}\" -> ${app.packageName}")
                }
                appendLine("- \"YouTube\" -> com.google.android.youtube.tv")
                appendLine("- \"Netflix\" -> com.netflix.mediaclient")
                appendLine("- \"Prime Video\" -> com.amazon.amazonvideo.livingroom")
                appendLine("- \"Disney+\" -> com.disney.disneyplus")
            }
        }

        val browserContext = if (BrowserAgentBridge.isBrowserVisible) {
            val siteToolsText = BrowserAgentBridge.siteTools.value.take(70).joinToString("\n") {
                "  * \$start ${it.slugId} site-tools;  (${it.tag}${if (it.inputType.isNotBlank()) " type=${it.inputType}" else ""}: \"${it.label}\")"
            }.ifBlank { "  (No site elements indexed yet — page may still be loading)" }

            """
            ACTIVE SCREEN: RNA BROWSER TAB
            - Current URL: ${BrowserAgentBridge.currentUrl}
            - Page Title: ${BrowserAgentBridge.currentTitle}
            - Active Browser Control Mode: ${BrowserAgentBridge.currentInputMode} (Default is DPAD mode. Holding OK via `${'$'}start d-pad.ok[hold] tool;` switches DPAD -> MOUSE -> SCROLL mode. Pressing Back steps back SCROLL -> MOUSE -> DPAD).
            - Virtual Mouse Cursor Position: (${BrowserAgentBridge.cursorX.toInt()}, ${BrowserAgentBridge.cursorY.toInt()}), Speed Multiplier: ${BrowserAgentBridge.mouseSpeedMultiplier}x (1 D-Pad step in MOUSE mode moves ~${(18 * BrowserAgentBridge.mouseSpeedMultiplier).toInt()}px; or use `${'$'}start mouse.move[dx,dy] tool;`).
            - Available Browser Control Functions (`browser-tools`):
              * ${'$'}start home browser-tools; (Return to Home tab)
              * ${'$'}start previous-page browser-tools; (Go back one page)
              * ${'$'}start next-page browser-tools; (Go forward one page)
              * ${'$'}start reload browser-tools; (Reload current webpage)
              * ${'$'}start search-bar browser-tools; (Focus browser URL/search bar)
              * ${'$'}start scroll-down browser-tools; / ${'$'}start scroll-up browser-tools;
              * ${'$'}start mode-dpad browser-tools; / ${'$'}start mode-mouse browser-tools; / ${'$'}start mode-scroll browser-tools;
              * ${'$'}start zoom-in browser-tools; / ${'$'}start zoom-out browser-tools;
            - Live Clickable Website Elements & Input Fields (`site-tools` extracted from current webpage DOM):
            $siteToolsText
            """.trimIndent()
        } else {
            """
            ACTIVE SCREEN: TV LAUNCHER / APP SURFACE
            - Default navigation is D-Pad directional focus.
            """.trimIndent()
        }

        val prompt = """
            You are the Autonomous RNA TV Agent, powered by EdVanced 1.1 Pro.
            NEVER mention "Gemini" or "Google". You are powered by EdVanced 1.1 Pro.
            Keep your spoken status message under 18 words.

            AUTONOMOUS COMMAND SYNTAX (you can chain multiple commands in exact execution order in a single response):
            1. Screenshot Tool (to inspect the current screen visually):
               ${'$'}start screenshot tool;
            2. D-Pad, Hold, Double-Press & Hardware Remote Tools (`tool`):
               - Single press:
                 ${'$'}start d-pad.ok tool;
                 ${'$'}start d-pad.up tool;
                 ${'$'}start d-pad.down tool;
                 ${'$'}start d-pad.left tool; (or ${'$'}start d-pad.ok.left tool;)
                 ${'$'}start d-pad.right tool;
               - Press and hold (e.g. long-press app/widget or switch D-Pad -> Mouse -> Scroll mode in Browser):
                 ${'$'}start d-pad.ok[hold] tool;
               - Double press:
                 ${'$'}start d-pad.ok[double] tool;
               - System & TV Remote keys:
                 ${'$'}start volume.up tool;
                 ${'$'}start volume.down tool;
                 ${'$'}start volume.mute tool;
                 ${'$'}start home tool;
                 ${'$'}start back tool;
                 ${'$'}start menu tool;
                 ${'$'}start play_pause tool;
            3. Browser Toolbar Controls (`browser-tools`):
               ${'$'}start home browser-tools;
               ${'$'}start previous-page browser-tools;
               ${'$'}start next-page browser-tools;
               ${'$'}start reload browser-tools;
               ${'$'}start search-bar browser-tools;
               ${'$'}start scroll-down browser-tools;
               ${'$'}start scroll-up browser-tools;
            4. Website DOM Buttons & Input Fields (`site-tools`):
               To click or focus any button/input on the active website, use its exact slug from the Live Clickable Website Elements list:
               ${'$'}start [element-slug] site-tools;
            5. Typing Text into Any Field (`type`):
               To type text into the focused input field (in an app or website), wrap the text inside curly braces {}:
               ${'$'}start {your text here} type;
            6. Open App or Website directly:
               ${'$'}start [package-name] app;
               ${'$'}start [https://url] browser;
               CRITICAL BROWSER RULE: Whenever the user asks to open something in the browser, open the browser, or search the web, NEVER open any external browser app on the device (never use `${'$'}start chrome app;` or `${'$'}start browser app;`). ALWAYS use `${'$'}start https://... browser;` so it opens directly inside the RNA Browser tab of the RNA app itself!

            ${if (agentCustomInstructions.isNotBlank()) "USER CUSTOM AGENT INSTRUCTIONS: $agentCustomInstructions\n" else ""}
            When the user's goal is completely fulfilled, include [DONE] at the end of your reply.
            If you need to see the screen after performing actions or after opening a page/app, include `${'$'}start screenshot tool;` at the end of your commands so you receive the updated screen!

            $browserContext

            $rnaToolsSystemSection

            RECENT EXECUTION HISTORY:
            ${history.joinToString("\n")}

            CURRENT USER GOAL:
            "$goal"
        """.trimIndent()

        val routerCtx = actCtx ?: throw IllegalStateException("Context unavailable for RnaFreeAiRouter")
        val outcome = RnaFreeAiRouter.generateTextOrVision(
            context = routerCtx,
            prompt = prompt,
            imageBitmap = screenshot,
            temperature = 0.2f,
            source = "Autonomous Agent",
            userDisplayPrompt = goal
        )
        val rawResponse = outcome.text

        val cleanedForAgent = if (actCtx != null) {
            val report = withContext(Dispatchers.Main) {
                RnaAiToolsEngine.processAndExecuteTools(actCtx, rawResponse)
            }
            report.cleanedText
        } else {
            rawResponse
        }

        parseAgentResponse(cleanedForAgent)
    }
}

@Composable
private fun AgentActivityResultBridge(
    onSpeechResult: (String?) -> Unit,
    onMicPermissionResult: (Boolean) -> Unit,
    onBindLaunchers: (
        launchSpeechIntent: (android.content.Intent) -> Unit,
        launchMicPermission: (String) -> Unit
    ) -> Unit
) {
    val fallbackSpeechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { res ->
        val spoken = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim()
        onSpeechResult(spoken)
    }
    val micPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onMicPermissionResult(granted)
    }
    DisposableEffect(Unit) {
        onBindLaunchers(
            { intent -> fallbackSpeechLauncher.launch(intent) },
            { perm -> micPermLauncher.launch(perm) }
        )
        onDispose { }
    }
}

/**
 * Unified Full-Screen Soft Glow + Bottom-Center Floating Agent Controls (`[X] [Pause/Play] [Mic] [Keyboard]`)
 * shared across BOTH RNA. TV and ALL external Android TV apps (hosted via [RnaHomeInterceptorService]).
 */
@Composable
fun RnaAgentGlowAndHudOverlay(
    performanceMode: Boolean = false,
    isServiceOverlay: Boolean = false,
    onKeyboardInputExpandedChanged: ((Boolean) -> Unit)? = null,
    onServiceFallbackMicLaunch: (() -> Unit)? = null,
    onDismissServiceOverlay: (() -> Unit)? = null
) {
    val isAgentActive by RnaAutonomousAgentController.isAgentModeActive.collectAsState()
    if (!isAgentActive) return

    val isPaused by RnaAutonomousAgentController.isPaused.collectAsState()
    val isListening by RnaAutonomousAgentController.isListening.collectAsState()
    val isWorking by RnaAutonomousAgentController.isWorking.collectAsState()
    val statusText by RnaAutonomousAgentController.statusText.collectAsState()
    val liveTranscript by RnaAutonomousAgentController.liveTranscript.collectAsState()

    val context = LocalContext.current
    val hasActivityResultOwner = LocalActivityResultRegistryOwner.current != null
    var showTextInputFallback by remember { mutableStateOf(false) }
    var typedInstruction by remember { mutableStateOf("") }
    var speechRecognizerRef by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isUserBrowsingWhileAgentPaused by remember { mutableStateOf(false) }

    val micFocusRequester = remember { FocusRequester() }
    val pauseFocusRequester = remember { FocusRequester() }
    val closeFocusRequester = remember { FocusRequester() }
    val inlineInputFocusRequester = remember { FocusRequester() }
    val browseWebsiteBtnFocusRequester = remember { FocusRequester() }

    var launchSpeechIntentCallback by remember { mutableStateOf<((android.content.Intent) -> Unit)?>(null) }
    var launchMicPermissionCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }

    LaunchedEffect(Unit) {
        if (!isServiceOverlay) {
            val prefs = context.getSharedPreferences(com.example.RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
            KeyRemapperBridge.ensureOverlayPermissionOnFirstLiveUse(context, prefs)
        }
    }

    LaunchedEffect(showTextInputFallback) {
        onKeyboardInputExpandedChanged?.invoke(showTextInputFallback)
        if (showTextInputFallback) {
            delay(80L)
            runCatching { inlineInputFocusRequester.requestFocus() }
        }
    }

    LaunchedEffect(isPaused) {
        if (!isPaused) {
            isUserBrowsingWhileAgentPaused = false
        }
    }

    DisposableEffect(Unit) {
        if (!isServiceOverlay) {
            RnaAutonomousAgentController.onReturnFocusToAgentBarCallback = {
                isUserBrowsingWhileAgentPaused = false
                runCatching { pauseFocusRequester.requestFocus() }
            }
        }
        onDispose {
            if (!isServiceOverlay && RnaAutonomousAgentController.onReturnFocusToAgentBarCallback != null) {
                RnaAutonomousAgentController.onReturnFocusToAgentBarCallback = null
            }
        }
    }

    val agentVoiceScope = rememberCoroutineScope()
    var agentVoiceSeq by remember { mutableStateOf(0) }
    var agentVoiceSubmitted by remember { mutableStateOf(false) }
    var agentLatestPartial by remember { mutableStateOf("") }
    var agentSilenceJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var agentEndFallbackJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun cancelAgentSpeechJobs() {
        agentSilenceJob?.cancel()
        agentSilenceJob = null
        agentEndFallbackJob?.cancel()
        agentEndFallbackJob = null
    }

    fun finalizeAgentSpeech(sessionId: Int, text: String?) {
        if (sessionId != agentVoiceSeq || agentVoiceSubmitted) return
        val chosen = text?.trim()?.takeIf { it.isNotEmpty() }
            ?: agentLatestPartial.trim().takeIf { it.isNotEmpty() }
        agentVoiceSubmitted = true
        cancelAgentSpeechJobs()
        agentLatestPartial = ""
        RnaAutonomousAgentController.setListeningState(false)
        runCatching {
            speechRecognizerRef?.stopListening()
            speechRecognizerRef?.cancel()
        }
        if (!chosen.isNullOrBlank()) {
            RnaAutonomousAgentController.submitUserInstruction(chosen)
        }
    }

    fun startAgentMicListening() {
        RnaAutonomousAgentController.prepareForUserVoiceInterruption()
        WakeWordAudioSilencer.playMicReadyChime(context)
        cancelAgentSpeechJobs()

        val activeSession = agentVoiceSeq + 1
        agentVoiceSeq = activeSession
        agentVoiceSubmitted = false
        agentLatestPartial = ""

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                runCatching {
                    speechRecognizerRef?.stopListening()
                    speechRecognizerRef?.cancel()
                    speechRecognizerRef?.destroy()
                }
                speechRecognizerRef = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            if (activeSession != agentVoiceSeq) return
                            RnaAutonomousAgentController.setListeningState(true)
                        }
                        override fun onBeginningOfSpeech() {
                            if (activeSession != agentVoiceSeq) return
                            RnaAutonomousAgentController.setListeningState(true)
                        }
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            if (activeSession != agentVoiceSeq || agentVoiceSubmitted) return
                            agentSilenceJob?.cancel()
                            agentEndFallbackJob?.cancel()
                            agentEndFallbackJob = agentVoiceScope.launch {
                                delay(450L)
                                if (activeSession == agentVoiceSeq && !agentVoiceSubmitted) {
                                    finalizeAgentSpeech(activeSession, agentLatestPartial)
                                }
                            }
                        }
                        override fun onError(error: Int) {
                            if (activeSession != agentVoiceSeq) return
                            if (!agentVoiceSubmitted && agentLatestPartial.isNotBlank()) {
                                finalizeAgentSpeech(activeSession, agentLatestPartial)
                            } else {
                                cancelAgentSpeechJobs()
                                RnaAutonomousAgentController.setListeningState(false)
                                showTextInputFallback = true
                            }
                        }
                        override fun onResults(results: Bundle?) {
                            if (activeSession != agentVoiceSeq) return
                            val top = results
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                ?.trim()
                            finalizeAgentSpeech(activeSession, top)
                        }
                        override fun onPartialResults(partialResults: Bundle?) {
                            if (activeSession != agentVoiceSeq || agentVoiceSubmitted) return
                            val part = partialResults
                                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                                ?.firstOrNull()
                                ?.trim()
                                .orEmpty()
                            if (part.isNotBlank()) {
                                agentLatestPartial = part
                                RnaAutonomousAgentController.setListeningState(true, part)
                                agentSilenceJob?.cancel()
                                agentSilenceJob = agentVoiceScope.launch {
                                    delay(1350L)
                                    if (activeSession == agentVoiceSeq && !agentVoiceSubmitted) {
                                        finalizeAgentSpeech(activeSession, agentLatestPartial)
                                    }
                                }
                            }
                        }
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
                val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
                }
                RnaAutonomousAgentController.setListeningState(true)
                speechRecognizerRef?.startListening(intent)
                return
            } catch (_: Exception) {
            }
        }

        val speechLauncher = launchSpeechIntentCallback
        if (speechLauncher != null) {
            try {
                val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Tell RNA Autonomous Agent what to do")
                }
                speechLauncher(intent)
                return
            } catch (_: Exception) {
            }
        }

        RnaAutonomousAgentController.setListeningState(false)
        if (onServiceFallbackMicLaunch != null) {
            onServiceFallbackMicLaunch()
        } else {
            showTextInputFallback = true
        }
    }

    if (hasActivityResultOwner) {
        AgentActivityResultBridge(
            onSpeechResult = { spoken ->
                RnaAutonomousAgentController.setListeningState(false)
                if (!spoken.isNullOrBlank()) {
                    RnaAutonomousAgentController.submitUserInstruction(spoken)
                }
            },
            onMicPermissionResult = { granted ->
                if (granted) {
                    startAgentMicListening()
                } else {
                    showTextInputFallback = true
                }
            },
            onBindLaunchers = { speechLauncher, permLauncher ->
                launchSpeechIntentCallback = speechLauncher
                launchMicPermissionCallback = permLauncher
            }
        )
    }

    fun triggerMicButtonAction() {
        val hasAudioPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasAudioPerm) {
            startAgentMicListening()
        } else {
            val permLauncher = launchMicPermissionCallback
            if (permLauncher != null) {
                permLauncher(Manifest.permission.RECORD_AUDIO)
            } else if (onServiceFallbackMicLaunch != null) {
                RnaAutonomousAgentController.prepareForUserVoiceInterruption()
                onServiceFallbackMicLaunch()
            } else {
                showTextInputFallback = true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                speechRecognizerRef?.stopListening()
                speechRecognizerRef?.cancel()
                speechRecognizerRef?.destroy()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!isServiceOverlay) {
            delay(80L)
            runCatching { micFocusRequester.requestFocus() }
        }
    }

    val glowAlpha = if (performanceMode) {
        0.55f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "AgentSoftBlueGlow")
        val animated by infiniteTransition.animateFloat(
            initialValue = 0.38f,
            targetValue = 0.78f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "GlowAlpha"
        )
        animated
    }

    val glowColorHex by RnaAutonomousAgentController.glowColorHex.collectAsState()
    val secondaryGlowHex by RnaAutonomousAgentController.secondaryGlowColorHex.collectAsState()

    val softBlue = remember(glowColorHex) {
        parseComposeColor(glowColorHex, Color(0xFF2979FF))
    }
    val electricCyan = remember(secondaryGlowHex, glowColorHex) {
        parseComposeColor(secondaryGlowHex, softBlue)
    }

    LaunchedEffect(Unit) {
        RnaAutonomousAgentController.onStartSystemOverlayMicCallback = {
            triggerMicButtonAction()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("rna_agent_glow_and_hud_overlay")
    ) {
        // 1. Soft Blue Glow around all 4 edges of the screen
        ComposeCanvas(modifier = Modifier.fillMaxSize()) {
            val edgeThickness = 42.dp.toPx()
            val w = size.width
            val h = size.height

            // Top soft blue glow
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        softBlue.copy(alpha = glowAlpha),
                        electricCyan.copy(alpha = glowAlpha * 0.45f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = edgeThickness
                ),
                topLeft = Offset(0f, 0f),
                size = Size(w, edgeThickness)
            )
            // Bottom soft blue glow
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        electricCyan.copy(alpha = glowAlpha * 0.45f),
                        softBlue.copy(alpha = glowAlpha)
                    ),
                    startY = h - edgeThickness,
                    endY = h
                ),
                topLeft = Offset(0f, h - edgeThickness),
                size = Size(w, edgeThickness)
            )
            // Left soft blue glow
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        softBlue.copy(alpha = glowAlpha),
                        electricCyan.copy(alpha = glowAlpha * 0.45f),
                        Color.Transparent
                    ),
                    startX = 0f,
                    endX = edgeThickness
                ),
                topLeft = Offset(0f, 0f),
                size = Size(edgeThickness, h)
            )
            // Right soft blue glow
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        electricCyan.copy(alpha = glowAlpha * 0.45f),
                        softBlue.copy(alpha = glowAlpha)
                    ),
                    startX = w - edgeThickness,
                    endX = w
                ),
                topLeft = Offset(w - edgeThickness, 0f),
                size = Size(edgeThickness, h)
            )
        }

        // Floating App Creation Hover Card (for $start ... app-tools;)
        RnaAppCreationHoverCardOverlay(
            context = context,
            modifier = Modifier.align(Alignment.TopEnd)
        )

        // 1B. Center "Browse Website" Popup Button when Agent is Paused on a Website
        val showBrowseWebsiteCenterPopup = isPaused && BrowserAgentBridge.isBrowserVisible && !isUserBrowsingWhileAgentPaused
        if (showBrowseWebsiteCenterPopup) {
            var browseBtnFocused by remember { mutableStateOf(false) }
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (browseBtnFocused) Color.White else Color(0xF210172A),
                border = BorderStroke(
                    width = if (browseBtnFocused) 3.dp else 2.dp,
                    color = if (browseBtnFocused) electricCyan else Color(0xFF38BDF8)
                ),
                shadowElevation = 18.dp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .focusRequester(browseWebsiteBtnFocusRequester)
                    .onFocusChanged { browseBtnFocused = it.isFocused }
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown &&
                            keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                        ) {
                            runCatching { pauseFocusRequester.requestFocus() }
                            true
                        } else {
                            false
                        }
                    }
                    .clickable {
                        isUserBrowsingWhileAgentPaused = true
                        BrowserAgentBridge.onRequestFocusWebCanvas?.invoke()
                    }
                    .focusable()
                    .testTag("rna_agent_browse_website_center_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Browse Website",
                        tint = if (browseBtnFocused) Color(0xFF0F172A) else electricCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Browse Website",
                        color = if (browseBtnFocused) Color(0xFF0F172A) else Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                }
            }
        }

        // 2. Bottom-Center Floating Agent Control Pill: [X (Close)]  [Pause/Resume]  [Microphone]
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Compact status pill above the 3 buttons
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xE6101524),
                border = BorderStroke(1.dp, electricCyan.copy(alpha = 0.6f)),
                modifier = Modifier.widthIn(min = 260.dp, max = 680.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SmartToy,
                        contentDescription = null,
                        tint = electricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    if (isWorking && !isPaused) {
                        CircularProgressIndicator(
                            color = electricCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = if (liveTranscript.isNotBlank() && isListening)
                            "\"$liveTranscript\""
                        else if (isUserBrowsingWhileAgentPaused)
                            "Browsing Website — Scroll to bottom & press Down to return to AI Bar"
                        else
                            statusText,
                        color = Color.White,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Optional inline Type bar when keyboard input is toggled or speech hardware is unavailable
            AnimatedVisibility(visible = showTextInputFallback) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xF0121829),
                    border = BorderStroke(1.dp, electricCyan),
                    modifier = Modifier
                        .widthIn(max = 520.dp)
                        .fillMaxWidth(0.92f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = typedInstruction,
                            onValueChange = { typedInstruction = it },
                            placeholder = {
                                Text(
                                    "Tell the Agent what to do...",
                                    color = Color(0xFF9AA4C2),
                                    fontSize = 12.sp
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (typedInstruction.isNotBlank()) {
                                        val cmd = typedInstruction.trim()
                                        typedInstruction = ""
                                        showTextInputFallback = false
                                        RnaAutonomousAgentController.submitUserInstruction(cmd)
                                    }
                                },
                                onDone = {
                                    if (typedInstruction.isNotBlank()) {
                                        val cmd = typedInstruction.trim()
                                        typedInstruction = ""
                                        showTextInputFallback = false
                                        RnaAutonomousAgentController.submitUserInstruction(cmd)
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = electricCyan,
                                unfocusedBorderColor = Color(0xFF344063),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(inlineInputFocusRequester)
                                .onPreviewKeyEvent { ev ->
                                    if (ev.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK &&
                                        ev.type == KeyEventType.KeyUp
                                    ) {
                                        showTextInputFallback = false
                                        true
                                    } else {
                                        false
                                    }
                                }
                                .testTag("rna_agent_type_input")
                        )
                        Button(
                            onClick = {
                                if (typedInstruction.isNotBlank()) {
                                    val cmd = typedInstruction.trim()
                                    typedInstruction = ""
                                    showTextInputFallback = false
                                    RnaAutonomousAgentController.submitUserInstruction(cmd)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = electricCyan,
                                contentColor = Color(0xFF0B101D)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("rna_agent_send_btn")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Run", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }
                }
            }

            val handleUpToBrowseButton: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyDown &&
                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP &&
                    showBrowseWebsiteCenterPopup
                ) {
                    runCatching { browseWebsiteBtnFocusRequester.requestFocus() }
                    true
                } else if (keyEvent.type == KeyEventType.KeyDown &&
                    keyEvent.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP &&
                    isPaused && BrowserAgentBridge.isBrowserVisible
                ) {
                    isUserBrowsingWhileAgentPaused = true
                    BrowserAgentBridge.onRequestFocusWebCanvas?.invoke() ?: false
                } else {
                    false
                }
            }

            // Main Bottom-Center 4-Button Agent Dock: [X]  [Pause/Resume]  [Mic]  [Keyboard]
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xEB0E1424),
                border = BorderStroke(1.5.dp, softBlue),
                shadowElevation = 14.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. X Button (Close Agent Mode)
                    var closeFocused by remember { mutableStateOf(false) }
                    Surface(
                        shape = CircleShape,
                        color = if (closeFocused) Color.White else Color(0xFF261C2E),
                        border = BorderStroke(
                            width = if (closeFocused) 2.dp else 1.dp,
                            color = if (closeFocused) Color.White else Color(0xFFFF5252)
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .focusRequester(closeFocusRequester)
                            .onFocusChanged { closeFocused = it.isFocused }
                            .onPreviewKeyEvent(handleUpToBrowseButton)
                            .clickable {
                                showTextInputFallback = false
                                runCatching { speechRecognizerRef?.stopListening() }
                                RnaAutonomousAgentController.exitAgentMode()
                                onDismissServiceOverlay?.invoke()
                            }
                            .focusable()
                            .testTag("rna_agent_close_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Agent Mode",
                                tint = if (closeFocused) Color(0xFF12131A) else Color(0xFFFF6E6E),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // 2. Pause / Resume Button (In between Close X and Microphone)
                    var pauseFocused by remember { mutableStateOf(false) }
                    Surface(
                        shape = CircleShape,
                        color = when {
                            pauseFocused -> Color.White
                            isPaused -> Color(0xFFFFB300)
                            else -> Color(0xFF1A2540)
                        },
                        border = BorderStroke(
                            width = if (pauseFocused) 2.dp else 1.dp,
                            color = if (pauseFocused) Color.White else Color(0xFF64B5F6)
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .focusRequester(pauseFocusRequester)
                            .onFocusChanged { pauseFocused = it.isFocused }
                            .onPreviewKeyEvent(handleUpToBrowseButton)
                            .clickable {
                                isUserBrowsingWhileAgentPaused = false
                                RnaAutonomousAgentController.togglePauseResume()
                            }
                            .focusable()
                            .testTag("rna_agent_pause_resume_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (isPaused) "Resume Agent" else "Pause Agent",
                                tint = if (pauseFocused || isPaused) Color(0xFF10131C) else Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // 3. Microphone Button (Speak goal or interrupt AI while working)
                    var micFocused by remember { mutableStateOf(false) }
                    Surface(
                        shape = CircleShape,
                        color = when {
                            micFocused -> Color.White
                            isListening -> Color(0xFF00E5FF)
                            else -> Color(0xFF2979FF)
                        },
                        border = BorderStroke(
                            width = if (micFocused) 2.5.dp else 1.5.dp,
                            color = if (micFocused) electricCyan else Color.White.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier
                            .size(52.dp)
                            .focusRequester(micFocusRequester)
                            .onFocusChanged { micFocused = it.isFocused }
                            .onPreviewKeyEvent(handleUpToBrowseButton)
                            .clickable {
                                showTextInputFallback = false
                                triggerMicButtonAction()
                            }
                            .focusable()
                            .testTag("rna_agent_mic_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Speak to Agent or Interrupt",
                                tint = if (micFocused || isListening) Color(0xFF0E1320) else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Compact Keyboard button for typing instructions when desired
                    var kbFocused by remember { mutableStateOf(false) }
                    Surface(
                        shape = CircleShape,
                        color = if (kbFocused) Color.White else Color(0xFF18223A),
                        border = BorderStroke(1.dp, if (kbFocused) Color.White else Color(0x55FFFFFF)),
                        modifier = Modifier
                            .size(48.dp)
                            .onFocusChanged { kbFocused = it.isFocused }
                            .onPreviewKeyEvent(handleUpToBrowseButton)
                            .clickable {
                                RnaAutonomousAgentController.prepareForUserVoiceInterruption()
                                showTextInputFallback = !showTextInputFallback
                            }
                            .focusable()
                            .testTag("rna_agent_keyboard_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = "Type to Agent",
                                tint = if (kbFocused) Color(0xFF10131C) else Color(0xFFD6E4FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
