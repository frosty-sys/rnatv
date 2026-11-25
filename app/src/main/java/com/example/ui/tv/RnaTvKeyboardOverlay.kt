package com.example.ui.tv

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.Locale
import kotlin.math.roundToInt

private enum class TvKeyboardPage {
    ABC,
    SYM_123,
    SYM_ALT
}

private data class QwertyKeySpec(
    val primary: String,
    val secondaryHint: String = ""
)

private val NUMBER_ROW = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")

private val ABC_ROW_1 = listOf(
    QwertyKeySpec("q", "%"),
    QwertyKeySpec("w", "\\"),
    QwertyKeySpec("e", "|"),
    QwertyKeySpec("r", "="),
    QwertyKeySpec("t", "["),
    QwertyKeySpec("y", "]"),
    QwertyKeySpec("u", "<"),
    QwertyKeySpec("i", ">"),
    QwertyKeySpec("o", "{"),
    QwertyKeySpec("p", "}")
)

private val ABC_ROW_2 = listOf(
    QwertyKeySpec("a", "@"),
    QwertyKeySpec("s", "#"),
    QwertyKeySpec("d", "₹"),
    QwertyKeySpec("f", "_"),
    QwertyKeySpec("g", "&"),
    QwertyKeySpec("h", "-"),
    QwertyKeySpec("j", "+"),
    QwertyKeySpec("k", "("),
    QwertyKeySpec("l", ")")
)

private val ABC_ROW_3 = listOf(
    QwertyKeySpec("z", "*"),
    QwertyKeySpec("x", "\""),
    QwertyKeySpec("c", "'"),
    QwertyKeySpec("v", ":"),
    QwertyKeySpec("b", ";"),
    QwertyKeySpec("n", "!"),
    QwertyKeySpec("m", "?")
)

private val SYM123_ROW_2 = listOf("@", "#", "₹", "_", "&", "-", "+", "(", ")", "/")
private val SYM123_ROW_3 = listOf("*", "\"", "'", ":", ";", "!", "?")

private val SYMALT_ROW_1 = listOf("~", "`", "|", "•", "√", "π", "÷", "×", "§", "Δ")
private val SYMALT_ROW_2 = listOf("€", "¥", "$", "¢", "^", "°", "=", "{", "}", "\\")
private val SYMALT_ROW_3 = listOf("%", "©", "®", "™", "✓", "[", "]")

object RnaClipboardHistoryStore {
    private const val PREFS_NAME = "rna_tv_clipboard_prefs"
    private const val KEY_ITEMS = "clipboard_items_json"

    fun getItems(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedList = mutableListOf<String>()

        val raw = prefs.getString(KEY_ITEMS, null)
        if (!raw.isNullOrBlank()) {
            try {
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val s = arr.optString(i).trim()
                    if (s.isNotEmpty() && !storedList.contains(s)) {
                        storedList.add(s)
                    }
                }
            } catch (_: Exception) {}
        }

        val systemClip = readSystemClipboardText(context)
        if (!systemClip.isNullOrEmpty()) {
            val needsPersist = storedList.firstOrNull() != systemClip
            storedList.remove(systemClip)
            storedList.add(0, systemClip)
            if (needsPersist) {
                persistList(context, storedList)
            }
        }
        return storedList.take(15)
    }

    fun readSystemClipboardText(context: Context): String? {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = cm?.primaryClip ?: return null
            if (clip.itemCount <= 0) return null
            clip.getItemAt(0)?.coerceToText(context)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }

    fun addItem(context: Context, text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        val current = getItems(context).toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        persistList(context, current)
        try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            cm?.setPrimaryClip(ClipData.newPlainText("RNA Clipboard", trimmed))
        } catch (_: Exception) {}
        return true
    }

    private fun persistList(context: Context, items: List<String>) {
        val arr = JSONArray()
        items.take(15).forEach { arr.put(it) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ITEMS, arr.toString())
            .apply()
    }
}

/**
 * Deterministic D-Pad controller for `RnaTvKeyboardOverlay` so `RnaTvInputMethodService`
 * can navigate and press every key in 100% of external Android TV apps without ever stealing
 * Window focus (`FLAG_NOT_FOCUSABLE`) from the underlying app's `EditText` / `InputConnection`.
 */
object RnaTvKeyboardDpadController {
    var onKeyEventCallback: ((keyCode: Int, action: Int, repeatCount: Int) -> Boolean)? = null

    fun dispatchHardwareKey(keyCode: Int, action: Int, repeatCount: Int = 0): Boolean {
        return onKeyEventCallback?.invoke(keyCode, action, repeatCount) ?: false
    }
}

/**
 * Strictly returns true ONLY when `RnaTvInputMethodService` is selected as the active system
 * default Input Method (`Settings.Secure.DEFAULT_INPUT_METHOD`).
 * When false, the RNA Keyboard NEVER appears anywhere—the system default keyboard (e.g. Gboard) is used.
 */
fun isRnaKeyboardDefaultIme(context: Context): Boolean {
    return com.example.RnaTvInputMethodService.isDefaultSystemIme(context)
}

fun setRnaKeyboardPreferredDefault(context: Context, enabled: Boolean) {
    context.getSharedPreferences(
        com.example.RnaTvInputMethodService.PREFS_NAME,
        Context.MODE_PRIVATE
    ).edit()
        .putBoolean(com.example.RnaTvInputMethodService.KEY_PREFER_RNA_KEYBOARD_DEFAULT, enabled)
        .apply()
}

private fun findDialogWindowFromView(view: android.view.View?): android.view.Window? {
    var current: android.view.ViewParent? = view?.parent
    while (current != null) {
        if (current is androidx.compose.ui.window.DialogWindowProvider) {
            return current.window
        }
        current = current.parent
    }
    return null
}

/**
 * Call inside any Compose `Dialog { ... }` that contains text fields so the Dialog's Window
 * clears `FLAG_NOT_FOCUSABLE` and `FLAG_ALT_FOCUSABLE_IM`, allowing Gboard or any system default
 * keyboard to appear over the Dialog when RNA Keyboard is not set as default.
 */
@Composable
fun EnableSystemKeyboardInDialog() {
    val dialogView = androidx.compose.ui.platform.LocalView.current
    val context = LocalContext.current
    SideEffect {
        try {
            findDialogWindowFromView(dialogView)?.let { win ->
                win.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                win.clearFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                win.setSoftInputMode(
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED or
                        android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                )
            }
            (context as? android.app.Activity)?.window?.let { actWin ->
                actWin.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                actWin.clearFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            }
        } catch (_: Exception) {
        }
    }
}

/**
 * Focuses the target text field and opens the device's active default system keyboard
 * (e.g. Gboard TV / Leanback Keyboard, or `RnaTvInputMethodService` if selected as system default).
 */
fun openDeviceDefaultKeyboard(
    context: Context,
    view: android.view.View? = null,
    focusRequester: FocusRequester? = null,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController? = null
) {
    try {
        findDialogWindowFromView(view)?.let { win ->
            win.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            win.clearFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            win.setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                    android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
            )
        }
        (context as? android.app.Activity)?.window?.let { actWin ->
            actWin.clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            actWin.clearFlags(android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        }
    } catch (_: Exception) {}

    try {
        focusRequester?.requestFocus()
    } catch (_: Exception) {}
    try {
        keyboardController?.show()
    } catch (_: Exception) {}

    fun triggerImm() {
        try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            if (view != null) {
                view.requestFocus()
                val shown = imm?.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT) ?: false
                if (!shown) {
                    @Suppress("DEPRECATION")
                    imm?.showSoftInput(view, android.view.inputmethod.InputMethodManager.SHOW_FORCED)
                }
            } else {
                @Suppress("DEPRECATION")
                imm?.toggleSoftInput(android.view.inputmethod.InputMethodManager.SHOW_FORCED, 0)
            }
        } catch (_: Exception) {}
    }

    triggerImm()
    view?.postDelayed({
        try {
            focusRequester?.requestFocus()
            keyboardController?.show()
        } catch (_: Exception) {}
        triggerImm()
    }, 65L)
}

/**
 * Attaches to an `OutlinedTextField` on Android TV without wrapping it in `.clickable`
 * (which would intercept D-Pad OK before BasicTextField can open Gboard).
 * - If RNA Keyboard is the active system default IME, pressing D-Pad OK opens the RNA Keyboard.
 * - If RNA Keyboard is NOT the system default IME, pressing D-Pad OK NEVER opens RNA Keyboard
 *   and instead opens the system default keyboard (e.g. Gboard).
 */
fun Modifier.tvKeyboardFieldTrigger(
    context: Context,
    view: android.view.View?,
    focusRequester: FocusRequester?,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    onOpenRnaKeyboard: () -> Unit
): Modifier = this.onPreviewKeyEvent { event ->
    val code = event.nativeKeyEvent.keyCode
    val isOkKey = code == KeyEvent.KEYCODE_DPAD_CENTER ||
        code == KeyEvent.KEYCODE_ENTER ||
        code == KeyEvent.KEYCODE_NUMPAD_ENTER ||
        code == KeyEvent.KEYCODE_BUTTON_A
    if (!isOkKey) return@onPreviewKeyEvent false

    if (isRnaKeyboardDefaultIme(context)) {
        if (event.type == KeyEventType.KeyUp) {
            onOpenRnaKeyboard()
        }
        true
    } else {
        if (event.type == KeyEventType.KeyUp) {
            openDeviceDefaultKeyboard(
                context = context,
                view = view,
                focusRequester = focusRequester,
                keyboardController = keyboardController
            )
        }
        false
    }
}

private data class KeyboardActionSlot(
    val onClick: () -> Unit,
    val onLongClick: (() -> Unit)? = null
)

@Composable
fun RnaTvKeyboardOverlay(
    visible: Boolean,
    currentText: String = "",
    fieldHint: String = "",
    showTextPreviewBar: Boolean = false,
    onTextChange: (String) -> Unit = {},
    onCommitChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onMoveCursorLeft: (() -> Unit)? = null,
    onMoveCursorRight: (() -> Unit)? = null,
    onSearchOrSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    RnaTvKeyboardOverlay(
        visible = visible,
        currentText = currentText,
        fieldHint = fieldHint,
        showTextPreviewBar = showTextPreviewBar,
        onTextChange = onTextChange,
        onCommitText = { chunk ->
            onCommitChar(chunk)
        },
        onBackspace = onBackspace,
        onMoveCursorLeft = onMoveCursorLeft,
        onMoveCursorRight = onMoveCursorRight,
        onSubmit = onSearchOrSubmit,
        onDismiss = onDismiss
    )
}

@Composable
fun RnaTvKeyboardOverlay(
    visible: Boolean,
    currentText: String = "",
    fieldHint: String = "",
    showTextPreviewBar: Boolean = false,
    resetKey: Int = 0,
    onCopyRequested: (() -> String?)? = null,
    onTextChange: ((String) -> Unit)? = null,
    onCommitText: (String) -> Unit,
    onBackspace: () -> Unit,
    onMoveCursorLeft: (() -> Unit)? = null,
    onMoveCursorRight: (() -> Unit)? = null,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Strict guarantee: NEVER display the RNA Keyboard whatsoever if it is not set as the system default keyboard.
    if (!isRnaKeyboardDefaultIme(context)) {
        LaunchedEffect(visible) {
            if (visible) {
                onDismiss()
            }
        }
        return
    }

    val isHostedInImeService = context is com.example.RnaTvInputMethodService
    val isHostedInDedicatedKeyboardComponent =
        context is com.example.RnaTvKeyboardActivity || isHostedInImeService

    // CRITICAL RULE: If NOT inside the standalone settings test screen, and RNA Keyboard is NOT
    // set as the system default IME, NEVER show the RNA Keyboard under any circumstance!
    if (!isHostedInDedicatedKeyboardComponent) {
        val currentOnTextChange by rememberUpdatedState(onTextChange)
        val currentOnCommitText by rememberUpdatedState(onCommitText)
        val currentOnSubmit by rememberUpdatedState(onSubmit)
        val currentOnDismiss by rememberUpdatedState(onDismiss)
        val latestText by rememberUpdatedState(currentText)

        LaunchedEffect(visible) {
            if (visible) {
                com.example.RnaTvKeyboardBridge.openForInAppField(
                    context = context,
                    initialText = latestText,
                    fieldHint = fieldHint,
                    onTextUpdated = { updated ->
                        val textUpdater = currentOnTextChange
                        if (textUpdater != null) {
                            textUpdater(updated)
                        } else if (updated.length > latestText.length && updated.startsWith(latestText)) {
                            currentOnCommitText(updated.substring(latestText.length))
                        }
                    },
                    onSubmit = {
                        currentOnSubmit()
                    },
                    onDismiss = {
                        currentOnDismiss()
                    }
                )
            }
        }

        LaunchedEffect(currentText, visible) {
            if (visible && com.example.RnaTvKeyboardBridge.isActivityOpen) {
                if (com.example.RnaTvKeyboardBridge.currentTextState.value != currentText) {
                    com.example.RnaTvKeyboardBridge.currentTextState.value = currentText
                }
            }
        }
        return
    }

    val scope = rememberCoroutineScope()

    var page by remember { mutableStateOf(TvKeyboardPage.ABC) }
    var isShifted by remember { mutableStateOf(false) }
    var showClipboardDrawer by remember { mutableStateOf(false) }
    var clipboardItems by remember { mutableStateOf(RnaClipboardHistoryStore.getItems(context)) }

    // Track in-app cursor index when editing currentText so ◀ and ▶ move between characters seamlessly
    var cursorOffsetFromEnd by remember { mutableIntStateOf(0) }
    val clampedOffsetFromEnd = cursorOffsetFromEnd.coerceIn(0, currentText.length)
    val cursorIndex = (currentText.length - clampedOffsetFromEnd).coerceIn(0, currentText.length)

    fun insertAtCursor(chunk: String) {
        if (onTextChange != null && currentText.isNotEmpty() && clampedOffsetFromEnd > 0) {
            val updated = currentText.substring(0, cursorIndex) + chunk + currentText.substring(cursorIndex)
            onTextChange(updated)
        } else {
            onCommitText(chunk)
        }
    }

    fun deleteAtCursor() {
        if (onTextChange != null && currentText.isNotEmpty() && clampedOffsetFromEnd > 0) {
            if (cursorIndex > 0) {
                val updated = currentText.substring(0, cursorIndex - 1) + currentText.substring(cursorIndex)
                onTextChange(updated)
            }
        } else {
            onBackspace()
        }
    }

    fun stepCursorLeft() {
        if (onMoveCursorLeft != null) {
            onMoveCursorLeft()
        }
        if (currentText.isNotEmpty()) {
            cursorOffsetFromEnd = (clampedOffsetFromEnd + 1).coerceAtMost(currentText.length)
        }
    }

    fun stepCursorRight() {
        if (onMoveCursorRight != null) {
            onMoveCursorRight()
        }
        if (currentText.isNotEmpty()) {
            cursorOffsetFromEnd = (clampedOffsetFromEnd - 1).coerceAtLeast(0)
        }
    }

    var isListeningNormal by remember { mutableStateOf(false) }
    var isListeningAi by remember { mutableStateOf(false) }
    var isAiProcessing by remember { mutableStateOf(false) }
    var voiceStatusMessage by remember { mutableStateOf<String?>(null) }

    val speechRecognizerRef = remember { mutableStateOf<SpeechRecognizer?>(null) }
    val firstKeyFocusRequester = remember { FocusRequester() }

    // Guard BackHandler so it NEVER crashes when hosted in InputMethodService where no ComponentActivity exists
    val backDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    if (visible && backDispatcherOwner != null) {
        BackHandler {
            onDismiss()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                speechRecognizerRef.value?.destroy()
            } catch (_: Exception) {}
        }
    }

    // Live sync with Android system clipboard whenever anything is copied across any app
    DisposableEffect(visible) {
        if (!visible) {
            onDispose { }
        } else {
            val cm = runCatching {
                context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            }.getOrNull()
            val listener = ClipboardManager.OnPrimaryClipChangedListener {
                clipboardItems = RnaClipboardHistoryStore.getItems(context)
            }
            runCatching { cm?.addPrimaryClipChangedListener(listener) }
            onDispose {
                runCatching { cm?.removePrimaryClipChangedListener(listener) }
            }
        }
    }

    // Auto-clear transient status feedback (e.g. "Copied") once voice/AI is idle
    LaunchedEffect(voiceStatusMessage, isListeningNormal, isListeningAi, isAiProcessing) {
        val msg = voiceStatusMessage
        if (!msg.isNullOrBlank() && !isListeningNormal && !isListeningAi && !isAiProcessing) {
            kotlinx.coroutines.delay(2200L)
            if (voiceStatusMessage == msg && !isListeningNormal && !isListeningAi && !isAiProcessing) {
                voiceStatusMessage = null
            }
        }
    }

    fun performCopyAction() {
        val resolvedText = onCopyRequested?.invoke()?.trim()?.takeIf { it.isNotEmpty() }
            ?: currentText.trim().takeIf { it.isNotEmpty() }
        if (resolvedText != null) {
            RnaClipboardHistoryStore.addItem(context, resolvedText)
            clipboardItems = RnaClipboardHistoryStore.getItems(context)
            voiceStatusMessage = "Copied"
        } else {
            // Re-sync in case host app copied via system context menu
            val latestSystemClip = RnaClipboardHistoryStore.readSystemClipboardText(context)
            if (!latestSystemClip.isNullOrEmpty()) {
                clipboardItems = RnaClipboardHistoryStore.getItems(context)
                voiceStatusMessage = "Copied"
            } else {
                voiceStatusMessage = "Nothing to copy"
            }
        }
    }

    fun startVoiceCapture(useAiPolish: Boolean) {
        try {
            speechRecognizerRef.value?.destroy()
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                voiceStatusMessage = "Voice recognition service not available on this TV"
                return
            }
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizerRef.value = recognizer
            isListeningNormal = !useAiPolish
            isListeningAi = useAiPolish
            voiceStatusMessage = if (useAiPolish) "Listening (AI Smart Phrasing)..." else "Listening..."

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListeningNormal = false
                    isListeningAi = false
                }
                override fun onError(error: Int) {
                    isListeningNormal = false
                    isListeningAi = false
                    voiceStatusMessage = null
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
                override fun onResults(results: Bundle?) {
                    isListeningNormal = false
                    isListeningAi = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val rawTranscript = matches?.firstOrNull()?.trim().orEmpty()
                    if (rawTranscript.isEmpty()) {
                        voiceStatusMessage = null
                        return
                    }
                    if (!useAiPolish) {
                        voiceStatusMessage = null
                        insertAtCursor(if (currentText.isNotEmpty() && !currentText.endsWith(" ")) " $rawTranscript" else rawTranscript)
                        RnaClipboardHistoryStore.addItem(context, rawTranscript)
                    } else {
                        isAiProcessing = true
                        voiceStatusMessage = "AI phrasing..."
                        scope.launch {
                            val polished = try {
                                withContext(Dispatchers.IO) {
                                    val prompt = """
                                        Rewrite the following spoken voice transcript into clean, properly punctuated, natural text with fixed grammar.
                                        Return ONLY the corrected text itself with no quotes or explanation.
                                        Transcript: $rawTranscript
                                    """.trimIndent()
                                    RnaFreeAiRouter.generateTextOrVision(
                                        context = context,
                                        prompt = prompt,
                                        temperature = 0.2f
                                    ).text.trim().takeIf { it.isNotBlank() } ?: rawTranscript
                                }
                            } catch (_: Exception) {
                                rawTranscript
                            }
                            isAiProcessing = false
                            voiceStatusMessage = null
                            insertAtCursor(if (currentText.isNotEmpty() && !currentText.endsWith(" ")) " $polished" else polished)
                            RnaClipboardHistoryStore.addItem(context, polished)
                        }
                    }
                }
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            recognizer.startListening(intent)
        } catch (e: Exception) {
            isListeningNormal = false
            isListeningAi = false
            voiceStatusMessage = "Mic unavailable"
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DETERMINISTIC 2D VIRTUAL GRID CONTROLLER FOR UNIVERSAL TV D-PAD NAVIGATION
    // Row 0: Top Bar [Clipboard, (Copy), Mic, AI Mic, Hide]
    // Row 1 (when clipboard drawer open & non-empty): Clipboard history chips
    // Main Key Rows (baseRow .. baseRow + 4):
    //   baseRow + 0: 10 keys (1..0 or ~..Δ)
    //   baseRow + 1: 10 keys (q..p or @../ or €..\)
    //   baseRow + 2: 9 keys on ABC (a..l) | 9 keys on SYM123 (=\<, *..?, ⌫) | 9 keys on SYMALT (?123, %..], ⌫)
    //   baseRow + 3 (ABC only): 9 keys (⇧, z..m, ⌫)
    //   bottomRow: 7 keys (?123/ABC, ',', ◀, Space, ▶, '.', Search)
    // ─────────────────────────────────────────────────────────────────────────
    val hasCopyButton = currentText.isNotBlank() || onCopyRequested != null
    val hasClipboardRow = showClipboardDrawer && clipboardItems.isNotEmpty()
    val baseKeyRow = if (hasClipboardRow) 2 else 1

    // Build the 2D table of KeyboardActionSlots for the current state
    val actionRows = remember(
        page,
        isShifted,
        showClipboardDrawer,
        clipboardItems,
        currentText,
        clampedOffsetFromEnd,
        hasCopyButton
    ) {
        val rows = mutableListOf<List<KeyboardActionSlot>>()

        // Row 0: Top Bar
        val topRow = mutableListOf<KeyboardActionSlot>()
        topRow.add(
            KeyboardActionSlot(
                onClick = {
                    clipboardItems = RnaClipboardHistoryStore.getItems(context)
                    showClipboardDrawer = !showClipboardDrawer
                }
            )
        )
        if (hasCopyButton) {
            topRow.add(
                KeyboardActionSlot(
                    onClick = { performCopyAction() }
                )
            )
        }
        topRow.add(KeyboardActionSlot(onClick = { startVoiceCapture(useAiPolish = false) }))
        topRow.add(KeyboardActionSlot(onClick = { startVoiceCapture(useAiPolish = true) }))
        topRow.add(KeyboardActionSlot(onClick = onDismiss))
        rows.add(topRow)

        // Optional Row 1: Clipboard items
        if (hasClipboardRow) {
            rows.add(
                clipboardItems.map { clipItem ->
                    KeyboardActionSlot(
                        onClick = {
                            insertAtCursor(clipItem)
                            showClipboardDrawer = false
                        }
                    )
                }
            )
        }

        when (page) {
            TvKeyboardPage.ABC -> {
                // Row base+0: 1..0
                rows.add(
                    NUMBER_ROW.map { num ->
                        KeyboardActionSlot(onClick = { insertAtCursor(num) })
                    }
                )
                // Row base+1: q..p
                rows.add(
                    ABC_ROW_1.map { spec ->
                        val ch = if (isShifted) spec.primary.uppercase() else spec.primary
                        KeyboardActionSlot(
                            onClick = {
                                insertAtCursor(ch)
                                if (isShifted) isShifted = false
                            },
                            onLongClick = {
                                if (spec.secondaryHint.isNotEmpty()) insertAtCursor(spec.secondaryHint)
                            }
                        )
                    }
                )
                // Row base+2: a..l
                rows.add(
                    ABC_ROW_2.map { spec ->
                        val ch = if (isShifted) spec.primary.uppercase() else spec.primary
                        KeyboardActionSlot(
                            onClick = {
                                insertAtCursor(ch)
                                if (isShifted) isShifted = false
                            },
                            onLongClick = {
                                if (spec.secondaryHint.isNotEmpty()) insertAtCursor(spec.secondaryHint)
                            }
                        )
                    }
                )
                // Row base+3: Shift, z..m, Backspace
                val row3 = mutableListOf<KeyboardActionSlot>()
                row3.add(KeyboardActionSlot(onClick = { isShifted = !isShifted }))
                ABC_ROW_3.forEach { spec ->
                    val ch = if (isShifted) spec.primary.uppercase() else spec.primary
                    row3.add(
                        KeyboardActionSlot(
                            onClick = {
                                insertAtCursor(ch)
                                if (isShifted) isShifted = false
                            },
                            onLongClick = {
                                if (spec.secondaryHint.isNotEmpty()) insertAtCursor(spec.secondaryHint)
                            }
                        )
                    )
                }
                row3.add(KeyboardActionSlot(onClick = { deleteAtCursor() }))
                rows.add(row3)

                // Row base+4: Bottom row
                rows.add(
                    listOf(
                        KeyboardActionSlot(onClick = { page = TvKeyboardPage.SYM_123 }),
                        KeyboardActionSlot(onClick = { insertAtCursor(",") }),
                        KeyboardActionSlot(onClick = { stepCursorLeft() }),
                        KeyboardActionSlot(onClick = { insertAtCursor(" ") }),
                        KeyboardActionSlot(onClick = { stepCursorRight() }),
                        KeyboardActionSlot(onClick = { insertAtCursor(".") }),
                        KeyboardActionSlot(onClick = onSubmit)
                    )
                )
            }

            TvKeyboardPage.SYM_123 -> {
                rows.add(NUMBER_ROW.map { num -> KeyboardActionSlot(onClick = { insertAtCursor(num) }) })
                rows.add(SYM123_ROW_2.map { sym -> KeyboardActionSlot(onClick = { insertAtCursor(sym) }) })
                val row3 = mutableListOf<KeyboardActionSlot>()
                row3.add(KeyboardActionSlot(onClick = { page = TvKeyboardPage.SYM_ALT }))
                SYM123_ROW_3.forEach { sym ->
                    row3.add(KeyboardActionSlot(onClick = { insertAtCursor(sym) }))
                }
                row3.add(KeyboardActionSlot(onClick = { deleteAtCursor() }))
                rows.add(row3)
                rows.add(
                    listOf(
                        KeyboardActionSlot(onClick = { page = TvKeyboardPage.ABC }),
                        KeyboardActionSlot(onClick = { insertAtCursor(",") }),
                        KeyboardActionSlot(onClick = { stepCursorLeft() }),
                        KeyboardActionSlot(onClick = { insertAtCursor(" ") }),
                        KeyboardActionSlot(onClick = { stepCursorRight() }),
                        KeyboardActionSlot(onClick = { insertAtCursor(".") }),
                        KeyboardActionSlot(onClick = onSubmit)
                    )
                )
            }

            TvKeyboardPage.SYM_ALT -> {
                rows.add(SYMALT_ROW_1.map { sym -> KeyboardActionSlot(onClick = { insertAtCursor(sym) }) })
                rows.add(SYMALT_ROW_2.map { sym -> KeyboardActionSlot(onClick = { insertAtCursor(sym) }) })
                val row3 = mutableListOf<KeyboardActionSlot>()
                row3.add(KeyboardActionSlot(onClick = { page = TvKeyboardPage.SYM_123 }))
                SYMALT_ROW_3.forEach { sym ->
                    row3.add(KeyboardActionSlot(onClick = { insertAtCursor(sym) }))
                }
                row3.add(KeyboardActionSlot(onClick = { deleteAtCursor() }))
                rows.add(row3)
                rows.add(
                    listOf(
                        KeyboardActionSlot(onClick = { page = TvKeyboardPage.ABC }),
                        KeyboardActionSlot(onClick = { insertAtCursor("<") }),
                        KeyboardActionSlot(onClick = { stepCursorLeft() }),
                        KeyboardActionSlot(onClick = { insertAtCursor(" ") }),
                        KeyboardActionSlot(onClick = { stepCursorRight() }),
                        KeyboardActionSlot(onClick = { insertAtCursor(">") }),
                        KeyboardActionSlot(onClick = onSubmit)
                    )
                )
            }
        }
        rows
    }

    var selectedRow by remember { mutableIntStateOf(1) }
    var selectedCol by remember { mutableIntStateOf(0) }
    var centerDownTimestamp by remember { mutableLongStateOf(0L) }
    var centerLongPressConsumed by remember { mutableStateOf(false) }

    // Keep (selectedRow, selectedCol) valid whenever rows change
    val safeRow = selectedRow.coerceIn(0, (actionRows.size - 1).coerceAtLeast(0))
    val safeCol = selectedCol.coerceIn(0, (actionRows.getOrNull(safeRow)?.size?.minus(1) ?: 0).coerceAtLeast(0))

    val currentActionRows by rememberUpdatedState(actionRows)
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    fun handleGridKeyEvent(keyCode: Int, action: Int, repeatCount: Int): Boolean {
        val rows = currentActionRows
        if (rows.isEmpty()) return false
        val r = selectedRow.coerceIn(0, rows.size - 1)
        val c = selectedCol.coerceIn(0, (rows[r].size - 1).coerceAtLeast(0))

        when (keyCode) {
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                if (action == KeyEvent.ACTION_UP) {
                    currentOnDismiss()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    selectedRow = r
                    selectedCol = if (c > 0) c - 1 else rows[r].size - 1
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    selectedRow = r
                    selectedCol = if (c < rows[r].size - 1) c + 1 else 0
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    val targetRow = (r - 1).coerceAtLeast(0)
                    val oldSize = rows[r].size
                    val newSize = rows[targetRow].size
                    val mappedCol = if (oldSize <= 1 || newSize <= 1) {
                        0
                    } else {
                        ((c.toFloat() / (oldSize - 1).toFloat()) * (newSize - 1).toFloat()).roundToInt()
                            .coerceIn(0, newSize - 1)
                    }
                    selectedRow = targetRow
                    selectedCol = mappedCol
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (action == KeyEvent.ACTION_DOWN) {
                    val targetRow = (r + 1).coerceAtMost(rows.size - 1)
                    val oldSize = rows[r].size
                    val newSize = rows[targetRow].size
                    val mappedCol = if (oldSize <= 1 || newSize <= 1) {
                        0
                    } else {
                        ((c.toFloat() / (oldSize - 1).toFloat()) * (newSize - 1).toFloat()).roundToInt()
                            .coerceIn(0, newSize - 1)
                    }
                    selectedRow = targetRow
                    selectedCol = mappedCol
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_BUTTON_A -> {
                val slot = rows.getOrNull(r)?.getOrNull(c) ?: return true
                if (action == KeyEvent.ACTION_DOWN) {
                    if (repeatCount == 0) {
                        centerDownTimestamp = System.currentTimeMillis()
                        centerLongPressConsumed = false
                    } else if (!centerLongPressConsumed && slot.onLongClick != null &&
                        System.currentTimeMillis() - centerDownTimestamp >= 420L
                    ) {
                        centerLongPressConsumed = true
                        slot.onLongClick.invoke()
                    }
                } else if (action == KeyEvent.ACTION_UP) {
                    if (!centerLongPressConsumed) {
                        slot.onClick.invoke()
                    }
                    centerLongPressConsumed = false
                }
                return true
            }
        }
        return false
    }

    DisposableEffect(visible) {
        if (visible) {
            RnaTvKeyboardDpadController.onKeyEventCallback = { code, act, rep ->
                handleGridKeyEvent(code, act, rep)
            }
        }
        onDispose {
            RnaTvKeyboardDpadController.onKeyEventCallback = null
        }
    }

    LaunchedEffect(visible, resetKey) {
        if (visible) {
            page = TvKeyboardPage.ABC
            isShifted = false
            showClipboardDrawer = false
            clipboardItems = RnaClipboardHistoryStore.getItems(context)
            selectedRow = 1
            selectedCol = 0
            if (!isHostedInImeService) {
                kotlinx.coroutines.delay(50L)
                try {
                    firstKeyFocusRequester.requestFocus()
                } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(page) {
        if (visible) {
            selectedRow = baseKeyRow
            selectedCol = 0
        }
    }

    fun isSlotSelected(r: Int, c: Int): Boolean = (safeRow == r && safeCol == c)

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .onPreviewKeyEvent { event ->
                    val native = event.nativeKeyEvent
                    handleGridKeyEvent(native.keyCode, native.action, native.repeatCount)
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(0.95f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1B1C1F))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (showTextPreviewBar || currentText.isNotEmpty() || fieldHint.isNotEmpty()) {
                    val previewWithCursor = if (currentText.isNotEmpty()) {
                        currentText.substring(0, cursorIndex) + "|" + currentText.substring(cursorIndex)
                    } else {
                        fieldHint.ifEmpty { "Type with D-Pad..." }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF111318))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = previewWithCursor,
                            color = if (currentText.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.45f),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Top Bar (Row 0): Clipboard on the left, Normal Voice + AI Voice + Hide on the right
                val voiceStartIdx = if (hasCopyButton) 2 else 1
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KeyboardTopBarPill(
                            label = "Clipboard",
                            icon = Icons.Default.ContentPaste,
                            isActive = showClipboardDrawer,
                            isDpadSelected = isSlotSelected(0, 0),
                            testTag = "keyboard_clipboard_btn",
                            onClick = {
                                selectedRow = 0
                                selectedCol = 0
                                clipboardItems = RnaClipboardHistoryStore.getItems(context)
                                showClipboardDrawer = !showClipboardDrawer
                            }
                        )

                        if (hasCopyButton) {
                            KeyboardTopBarPill(
                                label = "Copy",
                                icon = Icons.Default.ContentCopy,
                                isActive = false,
                                isDpadSelected = isSlotSelected(0, 1),
                                testTag = "keyboard_copy_current_btn",
                                onClick = {
                                    selectedRow = 0
                                    selectedCol = 1
                                    performCopyAction()
                                }
                            )
                        }

                        if (voiceStatusMessage != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isAiProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFFA8C7FA)
                                    )
                                }
                                Text(
                                    text = voiceStatusMessage ?: "",
                                    color = Color(0xFFA8C7FA),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Right side: Two Voice Input Buttons (Normal Voice + AI Voice) & Hide Keyboard
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KeyboardVoiceCircleButton(
                            icon = Icons.Default.Mic,
                            label = null,
                            isActive = isListeningNormal,
                            isDpadSelected = isSlotSelected(0, voiceStartIdx),
                            activeColor = Color(0xFF38BDF8),
                            testTag = "keyboard_normal_voice_btn",
                            onClick = {
                                selectedRow = 0
                                selectedCol = voiceStartIdx
                                startVoiceCapture(useAiPolish = false)
                            }
                        )
                        KeyboardVoiceCircleButton(
                            icon = Icons.Default.AutoAwesome,
                            label = "AI",
                            isActive = isListeningAi || isAiProcessing,
                            isDpadSelected = isSlotSelected(0, voiceStartIdx + 1),
                            activeColor = Color(0xFFC084FC),
                            testTag = "keyboard_ai_voice_btn",
                            onClick = {
                                selectedRow = 0
                                selectedCol = voiceStartIdx + 1
                                startVoiceCapture(useAiPolish = true)
                            }
                        )
                        KeyboardVoiceCircleButton(
                            icon = Icons.Default.KeyboardHide,
                            label = null,
                            isActive = false,
                            isDpadSelected = isSlotSelected(0, voiceStartIdx + 2),
                            activeColor = Color.White,
                            testTag = "keyboard_hide_btn",
                            onClick = {
                                selectedRow = 0
                                selectedCol = voiceStartIdx + 2
                                onDismiss()
                            }
                        )
                    }
                }

                // Expandable Clipboard History Strip (Row 1 when open & non-empty)
                AnimatedVisibility(visible = showClipboardDrawer) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF282A2E))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (clipboardItems.isEmpty()) {
                            Text(
                                text = "Clipboard is empty",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        } else {
                            clipboardItems.forEachIndexed { idx, item ->
                                val chipInteraction = remember { MutableInteractionSource() }
                                val chipFocused by chipInteraction.collectIsFocusedAsState()
                                val chipSelected = chipFocused || isSlotSelected(1, idx)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (chipSelected) Color(0xFFA8C7FA) else Color(0xFF383B40))
                                        .clickable(
                                            interactionSource = chipInteraction,
                                            indication = null
                                        ) {
                                            selectedRow = 1
                                            selectedCol = idx
                                            insertAtCursor(item)
                                            showClipboardDrawer = false
                                        }
                                        .focusable(interactionSource = chipInteraction)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("clipboard_chip_$idx")
                                ) {
                                    Text(
                                        text = item.take(36),
                                        color = if (chipSelected) Color(0xFF111827) else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Keyboard Rows by Active Page
                when (page) {
                    TvKeyboardPage.ABC -> {
                        // Row baseKeyRow + 0: 1 2 3 4 5 6 7 8 9 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            NUMBER_ROW.forEachIndexed { index, num ->
                                TvKeyButton(
                                    label = num,
                                    isDpadSelected = isSlotSelected(baseKeyRow, index),
                                    modifier = Modifier
                                        .weight(1f)
                                        .then(if (index == 0) Modifier.focusRequester(firstKeyFocusRequester) else Modifier),
                                    height = 38.dp,
                                    onClick = {
                                        selectedRow = baseKeyRow
                                        selectedCol = index
                                        insertAtCursor(num)
                                    }
                                )
                            }
                        }

                        // Row baseKeyRow + 1: q..p with superscript hints
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            ABC_ROW_1.forEachIndexed { index, spec ->
                                val displayChar = if (isShifted) spec.primary.uppercase() else spec.primary
                                TvKeyButton(
                                    label = displayChar,
                                    superscriptHint = spec.secondaryHint,
                                    isDpadSelected = isSlotSelected(baseKeyRow + 1, index),
                                    modifier = Modifier.weight(1f),
                                    height = 40.dp,
                                    onClick = {
                                        selectedRow = baseKeyRow + 1
                                        selectedCol = index
                                        insertAtCursor(displayChar)
                                        if (isShifted) isShifted = false
                                    },
                                    onLongClick = {
                                        if (spec.secondaryHint.isNotEmpty()) {
                                            insertAtCursor(spec.secondaryHint)
                                        }
                                    }
                                )
                            }
                        }

                        // Row baseKeyRow + 2: a..l indented slightly like Gboard TV
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            ABC_ROW_2.forEachIndexed { index, spec ->
                                val displayChar = if (isShifted) spec.primary.uppercase() else spec.primary
                                TvKeyButton(
                                    label = displayChar,
                                    superscriptHint = spec.secondaryHint,
                                    isDpadSelected = isSlotSelected(baseKeyRow + 2, index),
                                    modifier = Modifier.weight(1f),
                                    height = 40.dp,
                                    onClick = {
                                        selectedRow = baseKeyRow + 2
                                        selectedCol = index
                                        insertAtCursor(displayChar)
                                        if (isShifted) isShifted = false
                                    },
                                    onLongClick = {
                                        if (spec.secondaryHint.isNotEmpty()) {
                                            insertAtCursor(spec.secondaryHint)
                                        }
                                    }
                                )
                            }
                        }

                        // Row baseKeyRow + 3: Shift | z..m | Backspace
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            TvKeyButton(
                                label = "⇧",
                                isDpadSelected = isSlotSelected(baseKeyRow + 3, 0),
                                modifier = Modifier.weight(1.4f),
                                height = 40.dp,
                                isActionKey = true,
                                isToggled = isShifted,
                                onClick = {
                                    selectedRow = baseKeyRow + 3
                                    selectedCol = 0
                                    isShifted = !isShifted
                                }
                            )
                            ABC_ROW_3.forEachIndexed { index, spec ->
                                val displayChar = if (isShifted) spec.primary.uppercase() else spec.primary
                                TvKeyButton(
                                    label = displayChar,
                                    superscriptHint = spec.secondaryHint,
                                    isDpadSelected = isSlotSelected(baseKeyRow + 3, index + 1),
                                    modifier = Modifier.weight(1f),
                                    height = 40.dp,
                                    onClick = {
                                        selectedRow = baseKeyRow + 3
                                        selectedCol = index + 1
                                        insertAtCursor(displayChar)
                                        if (isShifted) isShifted = false
                                    },
                                    onLongClick = {
                                        if (spec.secondaryHint.isNotEmpty()) {
                                            insertAtCursor(spec.secondaryHint)
                                        }
                                    }
                                )
                            }
                            TvKeyButton(
                                label = "⌫",
                                icon = Icons.AutoMirrored.Filled.Backspace,
                                isDpadSelected = isSlotSelected(baseKeyRow + 3, 8),
                                modifier = Modifier.weight(1.4f),
                                height = 40.dp,
                                isActionKey = true,
                                onClick = {
                                    selectedRow = baseKeyRow + 3
                                    selectedCol = 8
                                    deleteAtCursor()
                                }
                            )
                        }

                        // Row baseKeyRow + 4 (Bottom Row): ?123 | , | ◀ | English (Space) | ▶ | . | Search/Enter
                        val bottomR = baseKeyRow + 4
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            TvKeyButton(
                                label = "?123",
                                isDpadSelected = isSlotSelected(bottomR, 0),
                                modifier = Modifier.weight(1.3f),
                                height = 40.dp,
                                isActionKey = true,
                                isPill = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 0
                                    page = TvKeyboardPage.SYM_123
                                }
                            )
                            TvKeyButton(
                                label = ",",
                                isDpadSelected = isSlotSelected(bottomR, 1),
                                modifier = Modifier.weight(0.85f),
                                height = 40.dp,
                                isActionKey = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 1
                                    insertAtCursor(",")
                                }
                            )
                            TvKeyButton(
                                label = "◀",
                                isDpadSelected = isSlotSelected(bottomR, 2),
                                modifier = Modifier
                                    .weight(0.95f)
                                    .testTag("keyboard_cursor_left_btn"),
                                height = 40.dp,
                                isActionKey = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 2
                                    stepCursorLeft()
                                }
                            )
                            TvKeyButton(
                                label = "English",
                                isDpadSelected = isSlotSelected(bottomR, 3),
                                modifier = Modifier.weight(3.5f),
                                height = 40.dp,
                                isActionKey = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 3
                                    insertAtCursor(" ")
                                }
                            )
                            TvKeyButton(
                                label = "▶",
                                isDpadSelected = isSlotSelected(bottomR, 4),
                                modifier = Modifier
                                    .weight(0.95f)
                                    .testTag("keyboard_cursor_right_btn"),
                                height = 40.dp,
                                isActionKey = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 4
                                    stepCursorRight()
                                }
                            )
                            TvKeyButton(
                                label = ".",
                                isDpadSelected = isSlotSelected(bottomR, 5),
                                modifier = Modifier.weight(0.85f),
                                height = 40.dp,
                                isActionKey = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 5
                                    insertAtCursor(".")
                                }
                            )
                            TvKeyButton(
                                label = "Search",
                                icon = Icons.Default.Search,
                                isDpadSelected = isSlotSelected(bottomR, 6),
                                modifier = Modifier.weight(1.4f),
                                height = 40.dp,
                                isPrimaryAccent = true,
                                isPill = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 6
                                    onSubmit()
                                }
                            )
                        }
                    }

                    TvKeyboardPage.SYM_123 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            NUMBER_ROW.forEachIndexed { index, num ->
                                TvKeyButton(
                                    label = num,
                                    isDpadSelected = isSlotSelected(baseKeyRow, index),
                                    modifier = Modifier
                                        .weight(1f)
                                        .then(if (index == 0) Modifier.focusRequester(firstKeyFocusRequester) else Modifier),
                                    height = 38.dp,
                                    onClick = {
                                        selectedRow = baseKeyRow
                                        selectedCol = index
                                        insertAtCursor(num)
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            SYM123_ROW_2.forEachIndexed { index, sym ->
                                TvKeyButton(
                                    label = sym,
                                    isDpadSelected = isSlotSelected(baseKeyRow + 1, index),
                                    modifier = Modifier.weight(1f),
                                    height = 40.dp,
                                    onClick = {
                                        selectedRow = baseKeyRow + 1
                                        selectedCol = index
                                        insertAtCursor(sym)
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            TvKeyButton(
                                label = "=\\<",
                                isDpadSelected = isSlotSelected(baseKeyRow + 2, 0),
                                modifier = Modifier.weight(1.4f),
                                height = 40.dp,
                                isActionKey = true,
                                onClick = {
                                    selectedRow = baseKeyRow + 2
                                    selectedCol = 0
                                    page = TvKeyboardPage.SYM_ALT
                                }
                            )
                            SYM123_ROW_3.forEachIndexed { index, sym ->
                                TvKeyButton(
                                    label = sym,
                                    isDpadSelected = isSlotSelected(baseKeyRow + 2, index + 1),
                                    modifier = Modifier.weight(1f),
                                    height = 40.dp,
                                    onClick = {
                                        selectedRow = baseKeyRow + 2
                                        selectedCol = index + 1
                                        insertAtCursor(sym)
                                    }
                                )
                            }
                            TvKeyButton(
                                label = "⌫",
                                icon = Icons.AutoMirrored.Filled.Backspace,
                                isDpadSelected = isSlotSelected(baseKeyRow + 2, 8),
                                modifier = Modifier.weight(1.4f),
                                height = 40.dp,
                                isActionKey = true,
                                onClick = {
                                    selectedRow = baseKeyRow + 2
                                    selectedCol = 8
                                    deleteAtCursor()
                                }
                            )
                        }

                        val bottomR = baseKeyRow + 3
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            TvKeyButton(
                                label = "ABC",
                                isDpadSelected = isSlotSelected(bottomR, 0),
                                modifier = Modifier.weight(1.3f),
                                height = 40.dp,
                                isActionKey = true,
                                isPill = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 0
                                    page = TvKeyboardPage.ABC
                                }
                            )
                            TvKeyButton(
                                label = ",",
                                isDpadSelected = isSlotSelected(bottomR, 1),
                                modifier = Modifier.weight(0.85f),
                                height = 40.dp,
                                isActionKey = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 1
                                    insertAtCursor(",")
                                }
                            )
                            TvKeyButton(
                                label = "◀",
                                isDpadSelected = isSlotSelected(bottomR, 2),
                                modifier = Modifier.weight(0.95f),
                                height = 40.dp,
                                isActionKey = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 2
                                    stepCursorLeft()
                                }
                            )
                            TvKeyButton(
                                label = "English",
                                isDpadSelected = isSlotSelected(bottomR, 3),
                                modifier = Modifier.weight(3.5f),
                                height = 40.dp,
                                isActionKey = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 3
                                    insertAtCursor(" ")
                                }
                            )
                            TvKeyButton(
                                label = "▶",
                                isDpadSelected = isSlotSelected(bottomR, 4),
                                modifier = Modifier.weight(0.95f),
                                height = 40.dp,
                                isActionKey = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 4
                                    stepCursorRight()
                                }
                            )
                            TvKeyButton(
                                label = ".",
                                isDpadSelected = isSlotSelected(bottomR, 5),
                                modifier = Modifier.weight(0.85f),
                                height = 40.dp,
                                isActionKey = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 5
                                    insertAtCursor(".")
                                }
                            )
                            TvKeyButton(
                                label = "Search",
                                icon = Icons.Default.Search,
                                isDpadSelected = isSlotSelected(bottomR, 6),
                                modifier = Modifier.weight(1.4f),
                                height = 40.dp,
                                isPrimaryAccent = true,
                                isPill = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 6
                                    onSubmit()
                                }
                            )
                        }
                    }

                    TvKeyboardPage.SYM_ALT -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            SYMALT_ROW_1.forEachIndexed { index, sym ->
                                TvKeyButton(
                                    label = sym,
                                    isDpadSelected = isSlotSelected(baseKeyRow, index),
                                    modifier = Modifier
                                        .weight(1f)
                                        .then(if (index == 0) Modifier.focusRequester(firstKeyFocusRequester) else Modifier),
                                    height = 38.dp,
                                    onClick = {
                                        selectedRow = baseKeyRow
                                        selectedCol = index
                                        insertAtCursor(sym)
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            SYMALT_ROW_2.forEachIndexed { index, sym ->
                                TvKeyButton(
                                    label = sym,
                                    isDpadSelected = isSlotSelected(baseKeyRow + 1, index),
                                    modifier = Modifier.weight(1f),
                                    height = 40.dp,
                                    onClick = {
                                        selectedRow = baseKeyRow + 1
                                        selectedCol = index
                                        insertAtCursor(sym)
                                    }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            TvKeyButton(
                                label = "?123",
                                isDpadSelected = isSlotSelected(baseKeyRow + 2, 0),
                                modifier = Modifier.weight(1.4f),
                                height = 40.dp,
                                isActionKey = true,
                                onClick = {
                                    selectedRow = baseKeyRow + 2
                                    selectedCol = 0
                                    page = TvKeyboardPage.SYM_123
                                }
                            )
                            SYMALT_ROW_3.forEachIndexed { index, sym ->
                                TvKeyButton(
                                    label = sym,
                                    isDpadSelected = isSlotSelected(baseKeyRow + 2, index + 1),
                                    modifier = Modifier.weight(1f),
                                    height = 40.dp,
                                    onClick = {
                                        selectedRow = baseKeyRow + 2
                                        selectedCol = index + 1
                                        insertAtCursor(sym)
                                    }
                                )
                            }
                            TvKeyButton(
                                label = "⌫",
                                icon = Icons.AutoMirrored.Filled.Backspace,
                                isDpadSelected = isSlotSelected(baseKeyRow + 2, 8),
                                modifier = Modifier.weight(1.4f),
                                height = 40.dp,
                                isActionKey = true,
                                onClick = {
                                    selectedRow = baseKeyRow + 2
                                    selectedCol = 8
                                    deleteAtCursor()
                                }
                            )
                        }

                        val bottomR = baseKeyRow + 3
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            TvKeyButton(
                                label = "ABC",
                                isDpadSelected = isSlotSelected(bottomR, 0),
                                modifier = Modifier.weight(1.3f),
                                height = 40.dp,
                                isActionKey = true,
                                isPill = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 0
                                    page = TvKeyboardPage.ABC
                                }
                            )
                            TvKeyButton(
                                label = "<",
                                isDpadSelected = isSlotSelected(bottomR, 1),
                                modifier = Modifier.weight(0.85f),
                                height = 40.dp,
                                isActionKey = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 1
                                    insertAtCursor("<")
                                }
                            )
                            TvKeyButton(
                                label = "◀",
                                isDpadSelected = isSlotSelected(bottomR, 2),
                                modifier = Modifier.weight(0.95f),
                                height = 40.dp,
                                isActionKey = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 2
                                    stepCursorLeft()
                                }
                            )
                            TvKeyButton(
                                label = "English",
                                isDpadSelected = isSlotSelected(bottomR, 3),
                                modifier = Modifier.weight(3.5f),
                                height = 40.dp,
                                isActionKey = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 3
                                    insertAtCursor(" ")
                                }
                            )
                            TvKeyButton(
                                label = "▶",
                                isDpadSelected = isSlotSelected(bottomR, 4),
                                modifier = Modifier.weight(0.95f),
                                height = 40.dp,
                                isActionKey = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 4
                                    stepCursorRight()
                                }
                            )
                            TvKeyButton(
                                label = ">",
                                isDpadSelected = isSlotSelected(bottomR, 5),
                                modifier = Modifier.weight(0.85f),
                                height = 40.dp,
                                isActionKey = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 5
                                    insertAtCursor(">")
                                }
                            )
                            TvKeyButton(
                                label = "Search",
                                icon = Icons.Default.Search,
                                isDpadSelected = isSlotSelected(bottomR, 6),
                                modifier = Modifier.weight(1.4f),
                                height = 40.dp,
                                isPrimaryAccent = true,
                                isPill = true,
                                onClick = {
                                    selectedRow = bottomR
                                    selectedCol = 6
                                    onSubmit()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyboardTopBarPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    isDpadSelected: Boolean = false,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val highlighted = isFocused || isDpadSelected
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    highlighted -> Color.White
                    isActive -> Color(0xFF3B82F6).copy(alpha = 0.35f)
                    else -> Color.Transparent
                }
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (highlighted) Color(0xFF111827) else Color.White,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = if (highlighted) Color(0xFF111827) else Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun KeyboardVoiceCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String?,
    isActive: Boolean,
    isDpadSelected: Boolean = false,
    activeColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val highlighted = isFocused || isDpadSelected
    Row(
        modifier = Modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(
                when {
                    highlighted -> Color.White
                    isActive -> activeColor
                    else -> Color(0xFF33353A)
                }
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = if (label != null) 10.dp else 8.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label ?: "Voice input",
            tint = if (highlighted || isActive) Color(0xFF111827) else Color.White,
            modifier = Modifier.size(16.dp)
        )
        if (label != null) {
            Text(
                text = label,
                color = if (highlighted || isActive) Color(0xFF111827) else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TvKeyButton(
    label: String,
    modifier: Modifier = Modifier,
    superscriptHint: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    height: Dp = 40.dp,
    isActionKey: Boolean = false,
    isPrimaryAccent: Boolean = false,
    isPill: Boolean = false,
    isToggled: Boolean = false,
    isDpadSelected: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val highlighted = isFocused || isDpadSelected

    val shape = if (isPill) CircleShape else RoundedCornerShape(8.dp)
    val bgColor = when {
        highlighted -> Color.White
        isPrimaryAccent -> Color(0xFFA8C7FA)
        isToggled -> Color(0xFF4B5563)
        isActionKey -> Color(0xFF3C4043)
        else -> Color(0xFF303236)
    }
    val contentColor = when {
        highlighted -> Color(0xFF111827)
        isPrimaryAccent -> Color(0xFF0B192C)
        else -> Color.White
    }

    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        if (superscriptHint.isNotEmpty()) {
            Text(
                text = superscriptHint,
                color = contentColor.copy(alpha = 0.65f),
                fontSize = 9.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 4.dp)
            )
        }

        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Text(
                text = label,
                color = contentColor,
                fontSize = if (label.length > 2) 13.sp else 16.sp,
                fontWeight = if (isActionKey || isPrimaryAccent || highlighted) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}
