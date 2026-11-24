package com.example

import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.tv.RnaTvKeyboardOverlay
import com.example.ui.tv.isRnaKeyboardDefaultIme
import com.example.ui.tv.setRnaKeyboardPreferredDefault

/**
 * Live bridge between RNA. TV / External Apps and the standalone [RnaTvKeyboardActivity].
 * Keeping the keyboard in its own dedicated Activity isolates D-Pad focus from RNA. TV's
 * launcher/browser/wizard screens and lets the bottom-center keyboard float cleanly over any app.
 */
object RnaTvKeyboardBridge {
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    var isActivityOpen: Boolean = false

    val currentTextState = mutableStateOf("")
    val fieldHintState = mutableStateOf("")
    var maxLength: Int = Int.MAX_VALUE

    private var onTextUpdatedCallback: ((String) -> Unit)? = null
    private var onSubmitCallback: ((String) -> Unit)? = null
    private var onDismissCallback: (() -> Unit)? = null

    fun openForInAppField(
        context: Context,
        initialText: String,
        fieldHint: String = "",
        maxChars: Int = Int.MAX_VALUE,
        onTextUpdated: (String) -> Unit,
        onSubmit: (String) -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        if (!RnaTvInputMethodService.isDefaultSystemIme(context)) {
            onDismiss()
            return
        }
        currentTextState.value = initialText
        fieldHintState.value = fieldHint
        maxLength = maxChars
        onTextUpdatedCallback = onTextUpdated
        onSubmitCallback = onSubmit
        onDismissCallback = onDismiss

        val intent = Intent(context, RnaTvKeyboardActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
            putExtra(RnaTvKeyboardActivity.EXTRA_MODE, RnaTvKeyboardActivity.MODE_IN_APP_FIELD)
            putExtra(RnaTvKeyboardActivity.EXTRA_INITIAL_TEXT, initialText)
            putExtra(RnaTvKeyboardActivity.EXTRA_FIELD_HINT, fieldHint)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openForExternalApp(
        context: Context,
        initialText: String = "",
        fieldHint: String = "Type in App..."
    ) {
        if (!RnaTvInputMethodService.isDefaultSystemIme(context)) {
            return
        }
        currentTextState.value = initialText
        fieldHintState.value = fieldHint
        maxLength = Int.MAX_VALUE
        onTextUpdatedCallback = null
        onSubmitCallback = null
        onDismissCallback = null

        val intent = Intent(context, RnaTvKeyboardActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
            putExtra(RnaTvKeyboardActivity.EXTRA_MODE, RnaTvKeyboardActivity.MODE_EXTERNAL_APP)
            putExtra(RnaTvKeyboardActivity.EXTRA_INITIAL_TEXT, initialText)
            putExtra(RnaTvKeyboardActivity.EXTRA_FIELD_HINT, fieldHint)
        }
        runCatching { context.startActivity(intent) }
    }

    fun openStandaloneSettings(context: Context) {
        val intent = Intent(context, RnaTvKeyboardActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
            putExtra(RnaTvKeyboardActivity.EXTRA_MODE, RnaTvKeyboardActivity.MODE_STANDALONE_SETTINGS)
        }
        runCatching { context.startActivity(intent) }
    }

    fun notifyTextChanged(updated: String) {
        val clamped = if (maxLength < Int.MAX_VALUE) updated.take(maxLength) else updated
        currentTextState.value = clamped
        val cb = onTextUpdatedCallback
        if (cb != null) {
            mainHandler.post { cb.invoke(clamped) }
        }
    }

    fun notifySubmit(finalText: String) {
        val clamped = if (maxLength < Int.MAX_VALUE) finalText.take(maxLength) else finalText
        currentTextState.value = clamped
        val textCb = onTextUpdatedCallback
        val submitCb = onSubmitCallback
        mainHandler.post {
            textCb?.invoke(clamped)
            submitCb?.invoke(clamped)
        }
    }

    fun notifyDismiss() {
        val dismissCb = onDismissCallback
        mainHandler.post {
            dismissCb?.invoke()
        }
    }
}

/**
 * Separate Android Activity for RNA TV Keyboard.
 * - Anchored strictly at the BOTTOM CENTER of the screen (`Alignment.BottomCenter`).
 * - Translucent non-dimming window so the underlying app or screen remains completely visible.
 * - Works across ALL apps (via `RnaTvInputMethodService` + `RnaHomeInterceptorService` accessibility text injection)
 *   as well as inside RNA. TV without interfering with RNA. TV's main Activity or focus tree.
 */
class RnaTvKeyboardActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MODE = "extra_rna_keyboard_mode"
        const val EXTRA_INITIAL_TEXT = "extra_rna_keyboard_initial_text"
        const val EXTRA_FIELD_HINT = "extra_rna_keyboard_field_hint"

        const val MODE_IN_APP_FIELD = "mode_in_app_field"
        const val MODE_EXTERNAL_APP = "mode_external_app"
        const val MODE_STANDALONE_SETTINGS = "mode_standalone_settings"
    }

    private val modeState = mutableStateOf(MODE_STANDALONE_SETTINGS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RnaTvKeyboardBridge.isActivityOpen = true
        configureBottomCenterTranslucentWindow()
        applyIntent(intent)

        if (modeState.value != MODE_STANDALONE_SETTINGS && !RnaTvInputMethodService.isDefaultSystemIme(this)) {
            closeKeyboardActivity()
            return
        }

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val activeMode by modeState
                val text by RnaTvKeyboardBridge.currentTextState
                val hint by RnaTvKeyboardBridge.fieldHintState
                var isDefaultImeNow by remember {
                    mutableStateOf(RnaTvInputMethodService.isDefaultSystemIme(context))
                }
                LaunchedEffect(Unit) {
                    while (true) {
                        isDefaultImeNow = RnaTvInputMethodService.isDefaultSystemIme(context)
                        kotlinx.coroutines.delay(500L)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (activeMode == MODE_STANDALONE_SETTINGS) {
                            RnaKeyboardStandaloneConfigCard(
                                onClose = { closeKeyboardActivity() }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (isDefaultImeNow) {
                            RnaTvKeyboardOverlay(
                                visible = true,
                                currentText = text,
                                fieldHint = when (activeMode) {
                                    MODE_STANDALONE_SETTINGS -> "Test typing with RNA Keyboard..."
                                    else -> hint.ifBlank { "Type with D-Pad..." }
                                },
                                showTextPreviewBar = true,
                                onTextChange = { updated ->
                                    handleFullTextUpdate(activeMode, updated)
                                },
                                onCommitChar = { chunk ->
                                    handleCommitChunk(activeMode, chunk)
                                },
                                onBackspace = {
                                    handleBackspace(activeMode)
                                },
                                onMoveCursorLeft = {
                                    if (activeMode == MODE_EXTERNAL_APP) {
                                        RnaTvInputMethodService.instance?.moveCursorLeftFromActivity()
                                    }
                                },
                                onMoveCursorRight = {
                                    if (activeMode == MODE_EXTERNAL_APP) {
                                        RnaTvInputMethodService.instance?.moveCursorRightFromActivity()
                                    }
                                },
                                onSearchOrSubmit = {
                                    handleSubmit(activeMode)
                                },
                                onDismiss = {
                                    RnaTvKeyboardBridge.notifyDismiss()
                                    closeKeyboardActivity()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureBottomCenterTranslucentWindow()
        applyIntent(intent)
    }

    private fun configureBottomCenterTranslucentWindow() {
        try {
            window?.let { win ->
                win.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
                win.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                win.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
                win.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
                win.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                win.setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                win.setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
            }
        } catch (_: Exception) {
        }
    }

    private fun applyIntent(intent: Intent?) {
        val incomingMode = intent?.getStringExtra(EXTRA_MODE)
            ?: if (intent?.action == Intent.ACTION_MAIN) MODE_STANDALONE_SETTINGS else MODE_IN_APP_FIELD
        modeState.value = incomingMode

        if (intent?.hasExtra(EXTRA_INITIAL_TEXT) == true) {
            RnaTvKeyboardBridge.currentTextState.value = intent.getStringExtra(EXTRA_INITIAL_TEXT).orEmpty()
        }
        if (intent?.hasExtra(EXTRA_FIELD_HINT) == true) {
            RnaTvKeyboardBridge.fieldHintState.value = intent.getStringExtra(EXTRA_FIELD_HINT).orEmpty()
        }
    }

    private fun handleFullTextUpdate(mode: String, updated: String) {
        RnaTvKeyboardBridge.notifyTextChanged(updated)
        if (mode == MODE_EXTERNAL_APP) {
            val committedByIme = RnaTvInputMethodService.instance?.replaceAllTextFromActivity(updated) == true
            if (!committedByIme) {
                RnaHomeInterceptorService.instance?.injectTextIntoExternalApp(updated, submitAfter = false)
            }
        }
    }

    private fun handleCommitChunk(mode: String, chunk: String) {
        val next = RnaTvKeyboardBridge.currentTextState.value + chunk
        RnaTvKeyboardBridge.notifyTextChanged(next)
        if (mode == MODE_EXTERNAL_APP) {
            val committedByIme = RnaTvInputMethodService.instance?.commitChunkFromActivity(chunk) == true
            if (!committedByIme) {
                RnaHomeInterceptorService.instance?.injectTextIntoExternalApp(
                    RnaTvKeyboardBridge.currentTextState.value,
                    submitAfter = false
                )
            }
        }
    }

    private fun handleBackspace(mode: String) {
        val current = RnaTvKeyboardBridge.currentTextState.value
        val next = if (current.isNotEmpty()) current.dropLast(1) else ""
        RnaTvKeyboardBridge.notifyTextChanged(next)
        if (mode == MODE_EXTERNAL_APP) {
            val handledByIme = RnaTvInputMethodService.instance?.backspaceFromActivity() == true
            if (!handledByIme) {
                RnaHomeInterceptorService.instance?.injectTextIntoExternalApp(next, submitAfter = false)
            }
        }
    }

    private fun handleSubmit(mode: String) {
        val finalText = RnaTvKeyboardBridge.currentTextState.value
        when (mode) {
            MODE_IN_APP_FIELD -> {
                RnaTvKeyboardBridge.notifySubmit(finalText)
                closeKeyboardActivity()
            }
            MODE_EXTERNAL_APP -> {
                val handledByIme = RnaTvInputMethodService.instance?.submitFromActivity() == true
                if (!handledByIme) {
                    RnaHomeInterceptorService.instance?.injectTextIntoExternalApp(finalText, submitAfter = true)
                }
                closeKeyboardActivity()
            }
            MODE_STANDALONE_SETTINGS -> {
                closeKeyboardActivity()
            }
        }
    }

    private fun closeKeyboardActivity() {
        RnaTvKeyboardBridge.isActivityOpen = false
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        RnaTvKeyboardBridge.isActivityOpen = false
        super.onDestroy()
    }
}

@Composable
private fun RnaKeyboardStandaloneConfigCard(
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var isRnaDefault by remember { mutableStateOf(isRnaKeyboardDefaultIme(context)) }
    val isSystemImeSelected = remember(isRnaDefault) {
        RnaTvInputMethodService.isDefaultSystemIme(context)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF2151925)),
        border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
        modifier = Modifier.width(560.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Keyboard,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "RNA TV Keyboard (Bottom-Center)",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isRnaDefault) {
                                if (isSystemImeSelected) "Active as System Default Keyboard across all TV apps"
                                else "Active across RNA. TV & Accessibility-linked TV apps"
                            } else {
                                "Optional — Currently using System Default Keyboard (e.g. Gboard)"
                            },
                            color = if (isRnaDefault) Color(0xFF38BDF8) else Color(0xFFB5B8C8),
                            fontSize = 11.5.sp
                        )
                    }
                }

                var closeFocused by remember { mutableStateOf(false) }
                Surface(
                    shape = CircleShape,
                    color = if (closeFocused) Color.White else Color(0xFF252A3D),
                    modifier = Modifier
                        .size(32.dp)
                        .onFocusChanged { closeFocused = it.isFocused }
                        .clickable { onClose() }
                        .focusable()
                        .testTag("rna_keyboard_activity_close_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (closeFocused) Color(0xFF090D16) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: Toggle RNA Keyboard Default Mode
                var toggleFocused by remember { mutableStateOf(false) }
                val toggleScale by animateFloatAsState(
                    targetValue = if (toggleFocused) 1.04f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "KbToggleScale"
                )
                Button(
                    onClick = {
                        val next = !isRnaDefault
                        setRnaKeyboardPreferredDefault(context, next)
                        isRnaDefault = isRnaKeyboardDefaultIme(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            toggleFocused -> Color.White
                            isRnaDefault -> Color(0xFF38BDF8)
                            else -> Color(0xFF222638)
                        },
                        contentColor = if (toggleFocused || isRnaDefault) Color(0xFF090D16) else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.15f)
                        .scale(toggleScale)
                        .onFocusChanged { toggleFocused = it.isFocused }
                        .focusable()
                        .testTag("rna_keyboard_toggle_default_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isRnaDefault) "RNA Default: ON" else "Use System / Gboard",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                // Button 2: Open Android System Input Method Picker
                var pickerFocused by remember { mutableStateOf(false) }
                val pickerScale by animateFloatAsState(
                    targetValue = if (pickerFocused) 1.04f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "KbPickerScale"
                )
                OutlinedButton(
                    onClick = {
                        setRnaKeyboardPreferredDefault(context, true)
                        isRnaDefault = true
                        try {
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                            imm?.showInputMethodPicker()
                        } catch (_: Exception) {
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (pickerFocused) Color.White else Color(0xFF1E2235),
                        contentColor = if (pickerFocused) Color(0xFF090D16) else Color.White
                    ),
                    border = BorderStroke(1.dp, if (pickerFocused) Color(0xFF38BDF8) else Color(0x44FFFFFF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .scale(pickerScale)
                        .onFocusChanged { pickerFocused = it.isFocused }
                        .focusable()
                        .testTag("rna_keyboard_ime_picker_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Switch IME", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                // Button 3: Open Android TV Keyboard Settings
                var settingsFocused by remember { mutableStateOf(false) }
                val settingsScale by animateFloatAsState(
                    targetValue = if (settingsFocused) 1.04f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "KbSettingsScale"
                )
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            try {
                                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(fallback)
                            } catch (_: Exception) {
                            }
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (settingsFocused) Color.White else Color(0xFF1E2235),
                        contentColor = if (settingsFocused) Color(0xFF090D16) else Color.White
                    ),
                    border = BorderStroke(1.dp, if (settingsFocused) Color(0xFF38BDF8) else Color(0x44FFFFFF)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .scale(settingsScale)
                        .onFocusChanged { settingsFocused = it.isFocused }
                        .focusable()
                        .testTag("rna_keyboard_system_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("TV Settings", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
    }
}
