package com.example.ui.tv

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

data class ControllerKeyLinkSpec(
    val bitIndex: Int,
    val buttonName: String,
    val subtitle: String,
    val defaultXboxKeyCode: Int,
    val defaultXboxKeyLabel: String,
    val defaultGenericButtonKeyCode: Int,
    val defaultGenericButtonLabel: String
)

data class KeyCodePresetOption(
    val keyCode: Int,
    val label: String,
    val category: String
)

object RnaKeyLinksRegistry {
    private const val PREFS_NAME = "rna_tv_key_links_prefs"
    private const val KEY_CUSTOM_MAP_JSON = "custom_key_links_json"
    private const val KEY_EMIT_BOTH_XBOX_AND_GENERIC = "emit_both_xbox_and_generic"
    const val KEY_CONTROLLER_OVER_KEYBOARD_IME = "control_ime_controller_bridge"

    val defaultSpecs: List<ControllerKeyLinkSpec> = listOf(
        ControllerKeyLinkSpec(XboxButtonBits.BIT_A, "A Button (Bottom)", "Confirm / Action / Jump", KeyEvent.KEYCODE_BUTTON_A, "KEYCODE_BUTTON_A (96)", KeyEvent.KEYCODE_BUTTON_1, "KEYCODE_BUTTON_1 (188)"),
        ControllerKeyLinkSpec(XboxButtonBits.BIT_B, "B Button (Right)", "Back / Cancel / Crouch", KeyEvent.KEYCODE_BUTTON_B, "KEYCODE_BUTTON_B (97)", KeyEvent.KEYCODE_BUTTON_2, "KEYCODE_BUTTON_2 (189)"),
        ControllerKeyLinkSpec(XboxButtonBits.BIT_X, "X Button (Left)", "Primary Attack / Reload", KeyEvent.KEYCODE_BUTTON_X, "KEYCODE_BUTTON_X (99)", KeyEvent.KEYCODE_BUTTON_3, "KEYCODE_BUTTON_3 (190)"),
        ControllerKeyLinkSpec(XboxButtonBits.BIT_Y, "Y Button (Top)", "Switch Weapon / Special", KeyEvent.KEYCODE_BUTTON_Y, "KEYCODE_BUTTON_Y (100)", KeyEvent.KEYCODE_BUTTON_4, "KEYCODE_BUTTON_4 (191)"),
        ControllerKeyLinkSpec(XboxButtonBits.BIT_LB, "LB (Left Bumper)", "Left Shoulder L1", KeyEvent.KEYCODE_BUTTON_L1, "KEYCODE_BUTTON_L1 (102)", KeyEvent.KEYCODE_BUTTON_5, "KEYCODE_BUTTON_5 (192)"),
        ControllerKeyLinkSpec(XboxButtonBits.BIT_RB, "RB (Right Bumper)", "Right Shoulder R1", KeyEvent.KEYCODE_BUTTON_R1, "KEYCODE_BUTTON_R1 (103)", KeyEvent.KEYCODE_BUTTON_6, "KEYCODE_BUTTON_6 (193)"),
        ControllerKeyLinkSpec(XboxButtonBits.BIT_LT, "LT (Left Trigger)", "Left Trigger L2", KeyEvent.KEYCODE_BUTTON_L2, "KEYCODE_BUTTON_L2 (104)", KeyEvent.KEYCODE_BUTTON_7, "KEYCODE_BUTTON_7 (194)"),
        ControllerKeyLinkSpec(XboxButtonBits.BIT_RT, "RT (Right Trigger)", "Right Trigger R2", KeyEvent.KEYCODE_BUTTON_R2, "KEYCODE_BUTTON_R2 (105)", KeyEvent.KEYCODE_BUTTON_8, "KEYCODE_BUTTON_8 (195)"),
        ControllerKeyLinkSpec(XboxButtonBits.BIT_SELECT, "View / Select Button", "Select / Map / Back", KeyEvent.KEYCODE_BUTTON_SELECT, "KEYCODE_BUTTON_SELECT (109)", KeyEvent.KEYCODE_BUTTON_9, "KEYCODE_BUTTON_9 (196)"),
        ControllerKeyLinkSpec(XboxButtonBits.BIT_START, "Menu / Start Button", "Start / Pause Game", KeyEvent.KEYCODE_BUTTON_START, "KEYCODE_BUTTON_START (108)", KeyEvent.KEYCODE_BUTTON_10, "KEYCODE_BUTTON_10 (197)"),
        ControllerKeyLinkSpec(XboxButtonBits.BIT_THUMBL, "L3 (Left Stick Click)", "Thumbstick Left Press", KeyEvent.KEYCODE_BUTTON_THUMBL, "KEYCODE_BUTTON_THUMBL (106)", KeyEvent.KEYCODE_BUTTON_11, "KEYCODE_BUTTON_11 (198)"),
        ControllerKeyLinkSpec(XboxButtonBits.BIT_THUMBR, "R3 (Right Stick Click)", "Thumbstick Right Press", KeyEvent.KEYCODE_BUTTON_THUMBR, "KEYCODE_BUTTON_THUMBR (107)", KeyEvent.KEYCODE_BUTTON_12, "KEYCODE_BUTTON_12 (199)"),
        ControllerKeyLinkSpec(XboxButtonBits.BIT_DPAD_UP, "D-Pad Up", "Directional Up", KeyEvent.KEYCODE_DPAD_UP, "KEYCODE_DPAD_UP (19)", KeyEvent.KEYCODE_BUTTON_13, "KEYCODE_BUTTON_13 (200)"),
        ControllerKeyLinkSpec(XboxButtonBits.BIT_DPAD_DOWN, "D-Pad Down", "Directional Down", KeyEvent.KEYCODE_DPAD_DOWN, "KEYCODE_DPAD_DOWN (20)", KeyEvent.KEYCODE_BUTTON_14, "KEYCODE_BUTTON_14 (201)"),
        ControllerKeyLinkSpec(XboxButtonBits.BIT_DPAD_LEFT, "D-Pad Left", "Directional Left", KeyEvent.KEYCODE_DPAD_LEFT, "KEYCODE_DPAD_LEFT (21)", KeyEvent.KEYCODE_BUTTON_15, "KEYCODE_BUTTON_15 (202)"),
        ControllerKeyLinkSpec(XboxButtonBits.BIT_DPAD_RIGHT, "D-Pad Right", "Directional Right", KeyEvent.KEYCODE_DPAD_RIGHT, "KEYCODE_DPAD_RIGHT (22)", KeyEvent.KEYCODE_BUTTON_16, "KEYCODE_BUTTON_16 (203)"),
        ControllerKeyLinkSpec(XboxButtonBits.BIT_MODE, "Xbox / RNA Guide", "Guide / Home Button", KeyEvent.KEYCODE_BUTTON_MODE, "KEYCODE_BUTTON_MODE (110)", KeyEvent.KEYCODE_HOME, "KEYCODE_HOME (3)")
    )

    val presetKeyCodes: List<KeyCodePresetOption> = buildList {
        // 1. Generic Hardware Buttons BUTTON_1 .. BUTTON_16 (for PPSSPP, RetroArch, TV Quick Actions Pro)
        val genericCodes = listOf(
            KeyEvent.KEYCODE_BUTTON_1 to "BUTTON_1 (188)",
            KeyEvent.KEYCODE_BUTTON_2 to "BUTTON_2 (189)",
            KeyEvent.KEYCODE_BUTTON_3 to "BUTTON_3 (190)",
            KeyEvent.KEYCODE_BUTTON_4 to "BUTTON_4 (191)",
            KeyEvent.KEYCODE_BUTTON_5 to "BUTTON_5 (192)",
            KeyEvent.KEYCODE_BUTTON_6 to "BUTTON_6 (193)",
            KeyEvent.KEYCODE_BUTTON_7 to "BUTTON_7 (194)",
            KeyEvent.KEYCODE_BUTTON_8 to "BUTTON_8 (195)",
            KeyEvent.KEYCODE_BUTTON_9 to "BUTTON_9 (196)",
            KeyEvent.KEYCODE_BUTTON_10 to "BUTTON_10 (197)",
            KeyEvent.KEYCODE_BUTTON_11 to "BUTTON_11 (198)",
            KeyEvent.KEYCODE_BUTTON_12 to "BUTTON_12 (199)",
            KeyEvent.KEYCODE_BUTTON_13 to "BUTTON_13 (200)",
            KeyEvent.KEYCODE_BUTTON_14 to "BUTTON_14 (201)",
            KeyEvent.KEYCODE_BUTTON_15 to "BUTTON_15 (202)",
            KeyEvent.KEYCODE_BUTTON_16 to "BUTTON_16 (203)"
        )
        genericCodes.forEach { (code, name) -> add(KeyCodePresetOption(code, name, "Generic Gamepad (BUTTON_1..16)")) }

        // 2. Xbox Official Keycodes
        val xboxCodes = listOf(
            KeyEvent.KEYCODE_BUTTON_A to "XBOX_A (96)",
            KeyEvent.KEYCODE_BUTTON_B to "XBOX_B (97)",
            KeyEvent.KEYCODE_BUTTON_X to "XBOX_X (99)",
            KeyEvent.KEYCODE_BUTTON_Y to "XBOX_Y (100)",
            KeyEvent.KEYCODE_BUTTON_L1 to "XBOX_LB / L1 (102)",
            KeyEvent.KEYCODE_BUTTON_R1 to "XBOX_RB / R1 (103)",
            KeyEvent.KEYCODE_BUTTON_L2 to "XBOX_LT / L2 (104)",
            KeyEvent.KEYCODE_BUTTON_R2 to "XBOX_RT / R2 (105)",
            KeyEvent.KEYCODE_BUTTON_THUMBL to "XBOX_L3 / THUMBL (106)",
            KeyEvent.KEYCODE_BUTTON_THUMBR to "XBOX_R3 / THUMBR (107)",
            KeyEvent.KEYCODE_BUTTON_START to "XBOX_START (108)",
            KeyEvent.KEYCODE_BUTTON_SELECT to "XBOX_SELECT (109)",
            KeyEvent.KEYCODE_BUTTON_MODE to "XBOX_MODE / GUIDE (110)"
        )
        xboxCodes.forEach { (code, name) -> add(KeyCodePresetOption(code, name, "Xbox Controller Keycodes")) }

        // 3. TV Remote & Keyboard Keycodes
        val remoteCodes = listOf(
            KeyEvent.KEYCODE_DPAD_UP to "DPAD_UP (19)",
            KeyEvent.KEYCODE_DPAD_DOWN to "DPAD_DOWN (20)",
            KeyEvent.KEYCODE_DPAD_LEFT to "DPAD_LEFT (21)",
            KeyEvent.KEYCODE_DPAD_RIGHT to "DPAD_RIGHT (22)",
            KeyEvent.KEYCODE_DPAD_CENTER to "DPAD_CENTER / OK (23)",
            KeyEvent.KEYCODE_ENTER to "ENTER (66)",
            KeyEvent.KEYCODE_BACK to "BACK (4)",
            KeyEvent.KEYCODE_SPACE to "SPACE (62)",
            KeyEvent.KEYCODE_ESCAPE to "ESCAPE (111)",
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to "MEDIA_PLAY_PAUSE (85)",
            KeyEvent.KEYCODE_F1 to "F1 (131)",
            KeyEvent.KEYCODE_F2 to "F2 (132)",
            KeyEvent.KEYCODE_F3 to "F3 (133)",
            KeyEvent.KEYCODE_F4 to "F4 (134)",
            KeyEvent.KEYCODE_Z to "KEY_Z (54)",
            KeyEvent.KEYCODE_X to "KEY_X (52)",
            KeyEvent.KEYCODE_C to "KEY_C (31)",
            KeyEvent.KEYCODE_V to "KEY_V (50)"
        )
        remoteCodes.forEach { (code, name) -> add(KeyCodePresetOption(code, name, "TV Remote & Keyboard")) }
    }

    fun isEmitBothEnabled(context: Context?): Boolean {
        if (context == null) return true
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_EMIT_BOTH_XBOX_AND_GENERIC, true)
    }

    fun setEmitBothEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_EMIT_BOTH_XBOX_AND_GENERIC, enabled)
            .apply()
    }

    fun isControllerOverKeyboardImeEnabled(context: Context?): Boolean {
        if (context == null) return false
        return context.getSharedPreferences(com.example.RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_CONTROLLER_OVER_KEYBOARD_IME, false)
    }

    fun setControllerOverKeyboardImeEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(com.example.RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CONTROLLER_OVER_KEYBOARD_IME, enabled)
            .apply()
    }

    fun getCustomOverrides(context: Context?): Map<Int, Int> {
        if (context == null) return emptyMap()
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_MAP_JSON, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val map = mutableMapOf<Int, Int>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val bit = k.toIntOrNull() ?: continue
                map[bit] = obj.optInt(k)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun setCustomOverride(context: Context, bitIndex: Int, keyCode: Int) {
        val current = getCustomOverrides(context).toMutableMap()
        current[bitIndex] = keyCode
        val obj = JSONObject()
        current.forEach { (k, v) -> obj.put(k.toString(), v) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_MAP_JSON, obj.toString())
            .apply()
    }

    fun resetAllToDefaults(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_CUSTOM_MAP_JSON)
            .putBoolean(KEY_EMIT_BOTH_XBOX_AND_GENERIC, true)
            .apply()
    }

    /**
     * Returns the list of Android hardware keycodes to emit when a controller button bit changes.
     * By default, emits BOTH the Xbox keycode (or custom override) AND `KEYCODE_BUTTON_1..16` so
     * emulators (PPSSPP, RetroArch) and remappers (TV Quick Actions Pro) register physical keys immediately.
     */
    fun getEffectiveKeyCodesForBit(context: Context?, bitIndex: Int): List<Int> {
        val overrides = getCustomOverrides(context)
        val primary = overrides[bitIndex]
            ?: XboxButtonBits.bitToKeyCode[bitIndex]
            ?: KeyEvent.KEYCODE_BUTTON_1
        val generic = XboxButtonBits.bitToGenericButtonKeyCode[bitIndex]
        val emitBoth = isEmitBothEnabled(context)
        return if (emitBoth && generic != null && generic != primary) {
            listOf(primary, generic)
        } else {
            listOf(primary)
        }
    }

    fun formatKeyCodeName(keyCode: Int): String {
        val preset = presetKeyCodes.firstOrNull { it.keyCode == keyCode }
        if (preset != null) return preset.label
        val nativeName = KeyEvent.keyCodeToString(keyCode)
        return "$nativeName ($keyCode)"
    }
}

/**
 * Full-Screen Key Link Editor opened from Control Panel -> "Key Link Editor".
 */
@Composable
fun TvKeyLinksEditorOverlay(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val context = LocalContext.current
    var emitBoth by remember { mutableStateOf(RnaKeyLinksRegistry.isEmitBothEnabled(context)) }
    var imeControllerBridge by remember { mutableStateOf(RnaKeyLinksRegistry.isControllerOverKeyboardImeEnabled(context)) }
    var customOverrides by remember { mutableStateOf(RnaKeyLinksRegistry.getCustomOverrides(context)) }
    var editingSpec by remember { mutableStateOf<ControllerKeyLinkSpec?>(null) }
    val firstRowFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(80L)
        runCatching { firstRowFocusRequester.requestFocus() }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = {
            if (editingSpec != null) {
                editingSpec = null
            } else {
                onDismiss()
            }
        },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF070A12), Color(0xFF0F172A), Color(0xFF090D16))
                    )
                )
                .testTag("tv_key_links_editor_fullscreen")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp, vertical = 22.dp)
            ) {
                // Top Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        KeyLinksActionButton(
                            label = "Back",
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            testTag = "key_links_back_btn",
                            onClick = onDismiss
                        )
                        Text(
                            text = "Key Link Editor",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KeyLinksActionButton(
                            label = "Enable RNA Keyboard IME",
                            icon = Icons.Default.Keyboard,
                            accentColor = Color(0xFF10B981),
                            testTag = "key_links_enable_ime_btn",
                            onClick = {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                } catch (_: Exception) {}
                            }
                        )
                        KeyLinksActionButton(
                            label = "Select Keyboard",
                            icon = Icons.Default.SwapHoriz,
                            accentColor = Color(0xFFA855F7),
                            testTag = "key_links_switch_ime_btn",
                            onClick = {
                                try {
                                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                    imm?.showInputMethodPicker()
                                } catch (_: Exception) {}
                            }
                        )
                        KeyLinksActionButton(
                            label = "Reset Defaults",
                            icon = Icons.Default.RestartAlt,
                            accentColor = Color(0xFFF59E0B),
                            testTag = "key_links_reset_btn",
                            onClick = {
                                RnaKeyLinksRegistry.resetAllToDefaults(context)
                                customOverrides = RnaKeyLinksRegistry.getCustomOverrides(context)
                                emitBoth = RnaKeyLinksRegistry.isEmitBothEnabled(context)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Dual Emission Mode Banner + Optional Controller over Keyboard IME Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val bannerInteraction = remember { MutableInteractionSource() }
                    val bannerFocused by bannerInteraction.collectIsFocusedAsState()
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (bannerFocused) Color(0xFF1E293B) else Color(0xFF111827))
                            .border(
                                width = if (bannerFocused) 2.dp else 1.dp,
                                color = if (bannerFocused) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(interactionSource = bannerInteraction, indication = null) {
                                emitBoth = !emitBoth
                                RnaKeyLinksRegistry.setEmitBothEnabled(context, emitBoth)
                            }
                            .focusable(interactionSource = bannerInteraction)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("key_links_dual_emit_toggle"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dual Keycode Emission (BUTTON_1..16 + Xbox)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = emitBoth,
                            onCheckedChange = null
                        )
                    }

                    val imeBannerInteraction = remember { MutableInteractionSource() }
                    val imeBannerFocused by imeBannerInteraction.collectIsFocusedAsState()
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (imeBannerFocused) Color(0xFF1E293B) else Color(0xFF111827))
                            .border(
                                width = if (imeBannerFocused) 2.dp else 1.dp,
                                color = if (imeBannerFocused) Color(0xFFA855F7) else Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(interactionSource = imeBannerInteraction, indication = null) {
                                imeControllerBridge = !imeControllerBridge
                                RnaKeyLinksRegistry.setControllerOverKeyboardImeEnabled(context, imeControllerBridge)
                            }
                            .focusable(interactionSource = imeBannerInteraction)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("key_links_ime_controller_toggle"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Controller over Keyboard IME (Optional)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = imeControllerBridge,
                            onCheckedChange = null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // List of all 17 Controller Buttons with their Xbox + Generic BUTTON_1..16 + Custom Override Keycodes
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    items(RnaKeyLinksRegistry.defaultSpecs, key = { it.bitIndex }) { spec ->
                        val isFirstSpec = spec.bitIndex == RnaKeyLinksRegistry.defaultSpecs.first().bitIndex
                        val overrideCode = customOverrides[spec.bitIndex]
                        val rowInteraction = remember { MutableInteractionSource() }
                        val rowFocused by rowInteraction.collectIsFocusedAsState()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (isFirstSpec) {
                                        Modifier.focusRequester(firstRowFocusRequester)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (rowFocused) Color(0xFF1E293B) else Color(0xFF111827))
                                .border(
                                    width = if (rowFocused) 2.dp else 1.dp,
                                    color = if (rowFocused) Color.White else Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable(interactionSource = rowInteraction, indication = null) {
                                    editingSpec = spec
                                }
                                .focusable(interactionSource = rowInteraction)
                                .padding(horizontal = 18.dp, vertical = 12.dp)
                                .testTag("key_link_row_${spec.bitIndex}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = spec.buttonName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = spec.subtitle,
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 11.5.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Primary / Custom Keycode Badge
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (overrideCode != null) Color(0xFFF59E0B).copy(alpha = 0.2f) else Color(0xFF38BDF8).copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, if (overrideCode != null) Color(0xFFF59E0B) else Color(0xFF38BDF8))
                                ) {
                                    Text(
                                        text = if (overrideCode != null) {
                                            "Custom: ${RnaKeyLinksRegistry.formatKeyCodeName(overrideCode)}"
                                        } else {
                                            spec.defaultXboxKeyLabel
                                        },
                                        color = if (overrideCode != null) Color(0xFFFBBF24) else Color(0xFF38BDF8),
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }

                                // Secondary Generic BUTTON_1..16 Badge
                                if (emitBoth) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.16f),
                                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.6f))
                                    ) {
                                        Text(
                                            text = "+ ${spec.defaultGenericButtonLabel}",
                                            color = Color(0xFF34D399),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Customize Keycode",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Focus-Trapping Modal Dialog to assign any BUTTON_1..16, Xbox Keycode, or Keyboard Keycode
            editingSpec?.let { spec ->
                val modalFirstFocus = remember(spec.bitIndex) { FocusRequester() }
                val modalCloseFocus = remember(spec.bitIndex) { FocusRequester() }
                val currentCode = customOverrides[spec.bitIndex] ?: spec.defaultXboxKeyCode
                val initialIndex = remember(spec.bitIndex, currentCode) {
                    RnaKeyLinksRegistry.presetKeyCodes.indexOfFirst { it.keyCode == currentCode }.coerceAtLeast(0)
                }
                val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState(
                    initialFirstVisibleItemIndex = (initialIndex - 3).coerceAtLeast(0)
                )

                LaunchedEffect(spec.bitIndex) {
                    kotlinx.coroutines.delay(90L)
                    if (!runCatching { modalFirstFocus.requestFocus(); true }.getOrDefault(false)) {
                        runCatching { modalCloseFocus.requestFocus() }
                    }
                }

                androidx.compose.ui.window.Dialog(
                    onDismissRequest = { editingSpec = null },
                    properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    BackHandler(enabled = true) {
                        editingSpec = null
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.84f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(0.84f)
                                .fillMaxHeight(0.82f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFF0F1626))
                                .border(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.45f), RoundedCornerShape(24.dp))
                                .padding(24.dp)
                                .testTag("key_link_edit_popup_dialog"),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Customize Keycode for ${spec.buttonName}",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "Use D-Pad to browse BUTTON_1..16, Xbox, or Remote keycodes • Press OK to assign",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                KeyLinksActionButton(
                                    label = "Close",
                                    icon = Icons.Default.Close,
                                    accentColor = Color(0xFFEF4444),
                                    testTag = "key_link_modal_close_btn",
                                    modifier = Modifier.focusRequester(modalCloseFocus),
                                    onClick = { editingSpec = null }
                                )
                            }

                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(3),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(vertical = 4.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .testTag("key_link_modal_options_grid")
                            ) {
                                items(RnaKeyLinksRegistry.presetKeyCodes.size, key = { RnaKeyLinksRegistry.presetKeyCodes[it].keyCode }) { idx ->
                                    val option = RnaKeyLinksRegistry.presetKeyCodes[idx]
                                    val optInteraction = remember { MutableInteractionSource() }
                                    val optFocused by optInteraction.collectIsFocusedAsState()
                                    val isSelected = currentCode == option.keyCode

                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = when {
                                            optFocused -> Color.White
                                            isSelected -> Color(0xFF1D3568)
                                            else -> Color(0xFF1A2236)
                                        },
                                        border = BorderStroke(
                                            width = if (optFocused) 2.5.dp else if (isSelected) 2.dp else 1.dp,
                                            color = when {
                                                optFocused -> Color(0xFF38BDF8)
                                                isSelected -> Color(0xFF38BDF8)
                                                else -> Color.White.copy(alpha = 0.10f)
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (idx == initialIndex) Modifier.focusRequester(modalFirstFocus)
                                                else Modifier
                                            )
                                            .clickable(interactionSource = optInteraction, indication = null) {
                                                RnaKeyLinksRegistry.setCustomOverride(context, spec.bitIndex, option.keyCode)
                                                customOverrides = RnaKeyLinksRegistry.getCustomOverrides(context)
                                                editingSpec = null
                                            }
                                            .testTag("key_link_option_${option.keyCode}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = option.label,
                                                    color = if (optFocused) Color(0xFF090D16) else Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = option.category,
                                                    color = if (optFocused) Color(0xFF334155) else Color.White.copy(alpha = 0.6f),
                                                    fontSize = 10.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected Keycode",
                                                    tint = if (optFocused) Color(0xFF0284C7) else Color(0xFF34D399),
                                                    modifier = Modifier.size(18.dp)
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
    }
}

@Composable
private fun KeyLinksActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color = Color(0xFF38BDF8),
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) accentColor else Color(0xFF1E293B))
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isFocused) Color(0xFF090D16) else Color.White,
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = label,
            color = if (isFocused) Color(0xFF090D16) else Color.White,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
