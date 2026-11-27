package com.example

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.tv.RnaKeyLinksRegistry
import com.example.ui.tv.RnaTvKeyboardDpadController
import com.example.ui.tv.RnaTvKeyboardOverlay

/**
 * Unified System-Wide Android TV InputMethodService ("RNA TV Keyboard").
 *
 * Hosts the EXACT same Jetpack Compose [RnaTvKeyboardOverlay] used inside RNA. TV
 * via [ComposeView] + [ServiceComposeLifecycleOwner], while preserving `FLAG_NOT_FOCUSABLE`
 * on the IME window so external apps retain `EditText` focus & `currentInputConnection`
 * and D-Pad keys drive [RnaTvKeyboardDpadController] deterministically across 100% of TV apps.
 */
class RnaTvInputMethodService : InputMethodService() {

    companion object {
        const val PREFS_NAME = "rna_tv_launcher_prefs"
        const val KEY_PREFER_RNA_KEYBOARD_DEFAULT = "prefer_rna_keyboard_default"

        @Volatile
        var activeInstance: RnaTvInputMethodService? = null
            private set

        val instance: RnaTvInputMethodService?
            get() = activeInstance

        fun isDefaultSystemIme(context: Context): Boolean {
            return try {
                val defaultIme = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.DEFAULT_INPUT_METHOD
                ).orEmpty()
                defaultIme.contains(context.packageName, ignoreCase = true) ||
                    defaultIme.contains("RnaTvInputMethodService", ignoreCase = true)
            } catch (_: Exception) {
                false
            }
        }

        fun isEnabledInSystemImeList(context: Context): Boolean {
            return try {
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                val enabledList = imm?.enabledInputMethodList.orEmpty()
                if (enabledList.any { it.packageName == context.packageName }) {
                    return true
                }
                val secureEnabled = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_INPUT_METHODS
                ).orEmpty()
                secureEnabled.contains(context.packageName, ignoreCase = true)
            } catch (_: Exception) {
                false
            }
        }
    }

    private val composeLifecycleOwner = ServiceComposeLifecycleOwner()

    private var rootFrame: FrameLayout? = null
    private var keyboardComposeView: ComposeView? = null

    private val isKeyboardVisibleState = mutableStateOf(false)
    private val currentPreviewTextState = mutableStateOf("")
    private val currentFieldHintState = mutableStateOf("Type with D-Pad...")
    private val keyboardSessionResetKey = mutableIntStateOf(0)

    // Optional in-app callbacks when RNA. TV binds a custom state handler
    var inAppTextChangeCallback: ((String) -> Unit)? = null
    var inAppCommitChunkCallback: ((String) -> Unit)? = null
    var inAppBackspaceCallback: (() -> Unit)? = null
    var inAppSubmitCallback: (() -> Unit)? = null
    var inAppDismissCallback: (() -> Unit)? = null

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        composeLifecycleOwner.onCreate()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onEvaluateInputViewShown(): Boolean {
        super.onEvaluateInputViewShown()
        return isDefaultSystemIme(this)
    }

    override fun onShowInputRequested(flags: Int, configChange: Boolean): Boolean {
        if (!isDefaultSystemIme(this)) return false
        super.onShowInputRequested(flags, configChange)
        return true
    }

    fun requestShowFromAccessibility() {
        if (!isDefaultSystemIme(this)) return
        try {
            requestShowSelf(InputMethodManager.SHOW_IMPLICIT)
        } catch (_: Exception) {
        }
    }

    override fun onComputeInsets(outInsets: Insets?) {
        super.onComputeInsets(outInsets)
        if (outInsets == null) return
        val root = rootFrame
        if (root != null && isInputViewShown) {
            val kbHeight = keyboardComposeView?.height?.takeIf { it > 0 } ?: dp(290)
            val topY = (root.height - kbHeight).coerceAtLeast(0)
            outInsets.contentTopInsets = topY
            outInsets.visibleTopInsets = topY
            outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT
        } else {
            val h = root?.height ?: 0
            outInsets.contentTopInsets = h
            outInsets.visibleTopInsets = h
            outInsets.touchableInsets = Insets.TOUCHABLE_INSETS_CONTENT
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onCreateInputView(): View {
        composeLifecycleOwner.onCreate()
        composeLifecycleOwner.attachToWindowDecorView(window?.window)

        val outer = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(Color.TRANSPARENT)
        }
        composeLifecycleOwner.attachToView(outer)

        val composeView = ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            composeLifecycleOwner.attachToView(this)
            setContent {
                MyApplicationTheme {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .background(androidx.compose.ui.graphics.Color.Transparent),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        RnaTvKeyboardOverlay(
                            visible = isKeyboardVisibleState.value,
                            currentText = currentPreviewTextState.value,
                            fieldHint = currentFieldHintState.value,
                            showTextPreviewBar = true,
                            resetKey = keyboardSessionResetKey.intValue,
                            onCopyRequested = { copyTextFromInputConnection() },
                            onTextChange = null,
                            onCommitText = { chunk -> commitChunkFromActivity(chunk) },
                            onBackspace = { backspaceFromActivity() },
                            onMoveCursorLeft = { moveCursorLeftFromActivity() },
                            onMoveCursorRight = { moveCursorRightFromActivity() },
                            onSubmit = {
                                submitFromActivity()
                                requestHideSelf(0)
                            },
                            onDismiss = {
                                requestHideSelf(0)
                            }
                        )
                    }
                }
            }
        }

        outer.addView(composeView)
        rootFrame = outer
        keyboardComposeView = composeView
        return outer
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        activeInstance = this

        if (!isDefaultSystemIme(this)) {
            requestHideSelf(0)
            return
        }

        composeLifecycleOwner.attachToWindowDecorView(window?.window)
        rootFrame?.let { composeLifecycleOwner.attachToView(it) }
        keyboardComposeView?.let { composeLifecycleOwner.attachToView(it) }
        composeLifecycleOwner.onResume()

        // Configure bottom-center non-dimming window WITHOUT clearing FLAG_NOT_FOCUSABLE
        // so that the target app's EditText / SearchView keeps window focus and InputConnection!
        try {
            window?.window?.let { win ->
                win.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                win.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                win.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
            }
        } catch (_: Exception) {
        }

        val hintFromEditor = info?.hintText?.toString()?.trim().orEmpty()
        currentFieldHintState.value = hintFromEditor.ifEmpty { "Type with D-Pad..." }
        syncTextFromInputConnection()

        if (!restarting) {
            keyboardSessionResetKey.intValue += 1
        }
        isKeyboardVisibleState.value = true
    }

    override fun onWindowShown() {
        super.onWindowShown()
        composeLifecycleOwner.attachToWindowDecorView(window?.window)
        composeLifecycleOwner.onResume()
        if (isDefaultSystemIme(this)) {
            isKeyboardVisibleState.value = true
        }
    }

    override fun onWindowHidden() {
        isKeyboardVisibleState.value = false
        composeLifecycleOwner.onPause()
        super.onWindowHidden()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (isInputViewShown) {
            syncTextFromInputConnection()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        isKeyboardVisibleState.value = false
        composeLifecycleOwner.onPause()
        inAppDismissCallback?.invoke()
        super.onFinishInputView(finishingInput)
    }

    private fun readFullTextFromInputConnection(): String? {
        val ic = currentInputConnection ?: return null
        try {
            val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)?.text?.toString()
            if (extracted != null) {
                return extracted
            }
        } catch (_: Exception) {
        }
        return try {
            val before = ic.getTextBeforeCursor(4000, 0)?.toString().orEmpty()
            val selected = ic.getSelectedText(0)?.toString().orEmpty()
            val after = ic.getTextAfterCursor(4000, 0)?.toString().orEmpty()
            if (before.isNotEmpty() || selected.isNotEmpty() || after.isNotEmpty()) {
                before + selected + after
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun syncTextFromInputConnection() {
        val textFromHost = readFullTextFromInputConnection()
        if (textFromHost != null) {
            currentPreviewTextState.value = textFromHost
        }
    }

    private fun copyTextFromInputConnection(): String? {
        val ic = currentInputConnection
        if (ic != null) {
            val selected = runCatching { ic.getSelectedText(0)?.toString()?.trim() }.getOrNull()
            if (!selected.isNullOrEmpty()) {
                return selected
            }
            val fullText = readFullTextFromInputConnection()?.trim()
            if (!fullText.isNullOrEmpty()) {
                return fullText
            }
            // Fallback for custom views/WebViews that only honor standard context copy action
            runCatching { ic.performContextMenuAction(android.R.id.copy) }
        }
        return currentPreviewTextState.value.trim().takeIf { it.isNotEmpty() }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (isInputViewShown && isDefaultSystemIme(this)) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_ESCAPE,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER,
                KeyEvent.KEYCODE_BUTTON_A -> {
                    val handled = RnaTvKeyboardDpadController.dispatchHardwareKey(
                        keyCode = keyCode,
                        action = KeyEvent.ACTION_DOWN,
                        repeatCount = event.repeatCount
                    )
                    if (handled) return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (isInputViewShown && isDefaultSystemIme(this)) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_ESCAPE -> {
                    val handled = RnaTvKeyboardDpadController.dispatchHardwareKey(
                        keyCode = keyCode,
                        action = KeyEvent.ACTION_UP,
                        repeatCount = event.repeatCount
                    )
                    if (!handled) {
                        requestHideSelf(0)
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER,
                KeyEvent.KEYCODE_BUTTON_A -> {
                    val handled = RnaTvKeyboardDpadController.dispatchHardwareKey(
                        keyCode = keyCode,
                        action = KeyEvent.ACTION_UP,
                        repeatCount = event.repeatCount
                    )
                    if (handled) return true
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    fun replaceAllTextFromActivity(updated: String): Boolean {
        currentPreviewTextState.value = updated
        inAppTextChangeCallback?.invoke(updated)
        val ic = currentInputConnection
            ?: return RnaHomeInterceptorService.instance?.injectTextIntoExternalApp(updated, submitAfter = false) == true
        return try {
            val extracted = runCatching {
                ic.getExtractedText(ExtractedTextRequest(), 0)?.text?.toString()
            }.getOrNull()
            ic.beginBatchEdit()
            ic.finishComposingText()
            if (extracted != null) {
                ic.deleteSurroundingText(extracted.length + 64, extracted.length + 64)
            } else {
                ic.deleteSurroundingText(256, 256)
            }
            ic.commitText(updated, 1)
            ic.endBatchEdit()
            syncTextFromInputConnection()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun commitChunkFromActivity(text: String): Boolean {
        if (text.isEmpty()) return false
        currentPreviewTextState.value += text
        inAppCommitChunkCallback?.invoke(text)
        inAppTextChangeCallback?.invoke(currentPreviewTextState.value)

        val ic = currentInputConnection
        if (ic != null) {
            val committed = try {
                ic.finishComposingText()
                ic.commitText(text, 1)
            } catch (_: Exception) {
                false
            }
            if (!committed) {
                text.forEach { ch ->
                    runCatching { sendKeyChar(ch) }
                }
            }
            syncTextFromInputConnection()
            return true
        }
        return RnaHomeInterceptorService.instance?.injectTextIntoExternalApp(
            currentPreviewTextState.value,
            submitAfter = false
        ) == true
    }

    fun backspaceFromActivity(): Boolean {
        if (currentPreviewTextState.value.isNotEmpty()) {
            currentPreviewTextState.value = currentPreviewTextState.value.dropLast(1)
        }
        inAppBackspaceCallback?.invoke()
        inAppTextChangeCallback?.invoke(currentPreviewTextState.value)

        val ic = currentInputConnection
        if (ic != null) {
            return try {
                ic.finishComposingText()
                val selected = ic.getSelectedText(0)
                if (!selected.isNullOrEmpty()) {
                    ic.commitText("", 1)
                } else {
                    val deleted = ic.deleteSurroundingText(1, 0)
                    if (!deleted) {
                        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL)
                    }
                }
                syncTextFromInputConnection()
                true
            } catch (_: Exception) {
                runCatching { sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL) }
                false
            }
        }
        return RnaHomeInterceptorService.instance?.injectTextIntoExternalApp(
            currentPreviewTextState.value,
            submitAfter = false
        ) == true
    }

    fun moveCursorLeftFromActivity(): Boolean {
        return try {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_LEFT)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun moveCursorRightFromActivity(): Boolean {
        return try {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_DPAD_RIGHT)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun submitFromActivity(): Boolean {
        inAppSubmitCallback?.invoke()
        val ic = currentInputConnection
        if (ic != null) {
            return try {
                ic.finishComposingText()
                val actionId = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                    ?: EditorInfo.IME_ACTION_NONE
                val handled = if (actionId != EditorInfo.IME_ACTION_NONE && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
                    ic.performEditorAction(actionId)
                } else {
                    ic.performEditorAction(EditorInfo.IME_ACTION_SEARCH) ||
                        ic.performEditorAction(EditorInfo.IME_ACTION_GO) ||
                        ic.performEditorAction(EditorInfo.IME_ACTION_DONE)
                }
                if (!handled) {
                    sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                }
                true
            } catch (_: Exception) {
                runCatching { sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER) }
                false
            }
        }
        return RnaHomeInterceptorService.instance?.injectTextIntoExternalApp(
            currentPreviewTextState.value,
            submitAfter = true
        ) == true
    }

    private val heldKeyDownTimes = java.util.concurrent.ConcurrentHashMap<Int, Long>()

    fun injectHardwareKeyEvent(keyCode: Int, isGamepad: Boolean = false): Boolean {
        if (!RnaKeyLinksRegistry.isControllerOverKeyboardImeEnabled(this)) return false
        return dispatchControllerButtonKey(keyCode)
    }

    /**
     * Dispatches a true held ACTION_DOWN or ACTION_UP hardware KeyEvent via InputConnection.
     * Used by PPSSPP mode so players can hold buttons (Cross/Circle/L/R/D-Pad/Analog I-J-K-L)
     * smoothly without premature 15ms auto-releases.
     */
    fun injectHardwareKeyAction(
        keyCode: Int,
        isDown: Boolean,
        isGamepad: Boolean = false,
        bypassImeToggle: Boolean = false
    ): Boolean {
        if (!bypassImeToggle && !RnaKeyLinksRegistry.isControllerOverKeyboardImeEnabled(this)) return false
        val ic = currentInputConnection ?: return false
        return try {
            val now = SystemClock.uptimeMillis()
            val downTime = if (isDown) {
                heldKeyDownTimes[keyCode] = now
                now
            } else {
                heldKeyDownTimes.remove(keyCode) ?: now
            }
            val source = when {
                isGamepad -> InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_KEYBOARD
                keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                    keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                    keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                    keyCode == KeyEvent.KEYCODE_DPAD_RIGHT -> InputDevice.SOURCE_DPAD or InputDevice.SOURCE_KEYBOARD
                else -> InputDevice.SOURCE_KEYBOARD
            }
            val ev = KeyEvent(
                downTime,
                now,
                if (isDown) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP,
                keyCode,
                0,
                0,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                KeyEvent.FLAG_FROM_SYSTEM,
                source
            )
            ic.sendKeyEvent(ev)
        } catch (_: Exception) {
            false
        }
    }

    fun commitRemoteText(text: String): Boolean {
        return commitChunkFromActivity(text)
    }

    fun dispatchControllerButtonKey(keyCode: Int): Boolean {
        if (!RnaKeyLinksRegistry.isControllerOverKeyboardImeEnabled(this)) return false
        val ic = currentInputConnection ?: return false
        return try {
            val now = SystemClock.uptimeMillis()
            val down = KeyEvent(
                now,
                now,
                KeyEvent.ACTION_DOWN,
                keyCode,
                0,
                0,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                KeyEvent.FLAG_FROM_SYSTEM,
                InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_KEYBOARD
            )
            val up = KeyEvent(
                now,
                now + 15L,
                KeyEvent.ACTION_UP,
                keyCode,
                0,
                0,
                KeyCharacterMap.VIRTUAL_KEYBOARD,
                0,
                KeyEvent.FLAG_FROM_SYSTEM,
                InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_KEYBOARD
            )
            val sentDown = ic.sendKeyEvent(down)
            val sentUp = ic.sendKeyEvent(up)
            if (!sentDown && !sentUp) {
                sendDownUpKeyEvents(keyCode)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun onDestroy() {
        isKeyboardVisibleState.value = false
        composeLifecycleOwner.onDestroy()
        if (activeInstance === this) {
            activeInstance = null
        }
        rootFrame = null
        keyboardComposeView = null
        super.onDestroy()
    }
}
