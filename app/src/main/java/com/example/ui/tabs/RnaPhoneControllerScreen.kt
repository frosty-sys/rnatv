package com.example.ui.tabs

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VioletSecondary
import com.example.ui.tv.ExportedTvAppInfo
import com.example.ui.tv.RnaControllerNetworkBridge
import com.example.ui.tv.XboxButtonBits
import com.example.ui.wizard.decodeDataUriToImageBitmap
import kotlinx.coroutines.delay
import org.json.JSONArray
import kotlin.math.roundToInt

private const val PREF_SAVED_REMOTE_SHORTCUT_PKGS = "rna_phone_remote_shortcut_pkgs"
private const val PREF_XBOX_PPSSPP_MODE = "rna_xbox_ppsspp_mode_v1"

@Composable
fun RnaPhoneControllerModal(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = remember {
        context.getSharedPreferences("rna_controller_prefs", Context.MODE_PRIVATE)
    }

    val isConnected by RnaControllerNetworkBridge.phoneConnectedState.collectAsState()
    val assignedSlot by RnaControllerNetworkBridge.phoneAssignedSlot.collectAsState()
    val statusMsg by RnaControllerNetworkBridge.phoneConnectionStatusMessage.collectAsState()
    val tvVolumePercent by RnaControllerNetworkBridge.phoneTvVolumePercent.collectAsState()
    val availableTvApps by RnaControllerNetworkBridge.phoneAvailableTvApps.collectAsState()

    // Mode: "REMOTE" (Portrait Material 3 Remote) vs "CONTROLLER" (Landscape Xbox / PPSSPP Controller)
    var currentMode by remember { mutableStateOf("REMOTE") }
    var showModeDropdown by remember { mutableStateOf(false) }
    var isPpssppMode by remember {
        mutableStateOf(prefs.getBoolean(PREF_XBOX_PPSSPP_MODE, false))
    }

    // Controller Name (set on phone and displayed on TV)
    var controllerName by remember {
        mutableStateOf(RnaControllerNetworkBridge.getSavedPhoneControllerName(context))
    }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    // Pairing 4-digit PIN & Optional Manual TV IP
    var pinInput by remember { mutableStateOf("") }
    var manualTvIp by remember {
        mutableStateOf(RnaControllerNetworkBridge.getSavedTvIp(prefs).orEmpty())
    }
    var showManualIpField by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }

    // Keyboard Modal for sending text to open TV text box
    var showKeyboardDialog by remember { mutableStateOf(false) }
    var keyboardText by remember { mutableStateOf("") }

    // TV App Shortcuts Picker Modal
    var showAddTvAppsDialog by remember { mutableStateOf(false) }
    val pinnedAppPackages = remember {
        mutableStateListOf<String>().apply {
            val savedJson = prefs.getString(PREF_SAVED_REMOTE_SHORTCUT_PKGS, null)
            if (!savedJson.isNullOrBlank()) {
                runCatching {
                    val arr = JSONArray(savedJson)
                    for (i in 0 until arr.length()) {
                        val pkg = arr.optString(i)
                        if (pkg.isNotBlank()) add(pkg)
                    }
                }
            }
        }
    }

    fun savePinnedPackages() {
        val arr = JSONArray()
        pinnedAppPackages.forEach { arr.put(it) }
        prefs.edit().putString(PREF_SAVED_REMOTE_SHORTCUT_PKGS, arr.toString()).apply()
    }

    // Automatically attempt zero-PIN auto-reconnect if previously paired on the same Wi-Fi or Hotspot
    LaunchedEffect(Unit) {
        if (!isConnected) {
            RnaControllerNetworkBridge.attemptPhoneAutoReconnect(context)
        }
    }

    // Seed initial pinned TV apps once TV sends its app list if none saved yet
    LaunchedEffect(availableTvApps) {
        if (pinnedAppPackages.isEmpty() && availableTvApps.isNotEmpty()) {
            availableTvApps.take(4).forEach { app ->
                if (app.packageName !in pinnedAppPackages) {
                    pinnedAppPackages.add(app.packageName)
                }
            }
            savePinnedPackages()
        }
    }

    // Automatically switch phone orientation to Landscape when in Xbox Controller Mode, and restore on exit!
    DisposableEffect(currentMode, isConnected) {
        if (isConnected && currentMode == "CONTROLLER") {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDismiss()
    }

    Dialog(
        onDismissRequest = {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF0B0E18),
                            Color(0xFF121626),
                            Color(0xFF0D101B)
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("rna_phone_controller_modal")
        ) {
            if (!isConnected) {
                // ── PAIRING SCREEN (4-DIGIT PIN + CONTROLLER NAME + AUTO-RECONNECT) ──
                PhoneControllerPairingView(
                    controllerName = controllerName,
                    onEditControllerName = { showEditNameDialog = true },
                    pinInput = pinInput,
                    onPinChange = { next ->
                        if (next.length <= 4 && next.all { it.isDigit() }) {
                            pinInput = next
                        }
                    },
                    manualTvIp = manualTvIp,
                    showManualIpField = showManualIpField,
                    onToggleManualIpField = { showManualIpField = !showManualIpField },
                    onManualTvIpChange = { manualTvIp = it },
                    statusMessage = statusMsg,
                    isConnecting = isConnecting,
                    onConnectClick = {
                        if (pinInput.length == 4) {
                            isConnecting = true
                            RnaControllerNetworkBridge.pairPhoneWithPin(
                                context = context,
                                pinCode = pinInput,
                                controllerName = controllerName,
                                mode = currentMode,
                                optionalTargetIp = manualTvIp.takeIf { showManualIpField && it.isNotBlank() }
                            ) { _, _ ->
                                isConnecting = false
                            }
                        }
                    },
                    onClose = {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        onDismiss()
                    }
                )
            } else if (currentMode == "CONTROLLER") {
                // ── LANDSCAPE XBOX / PPSSPP CONTROLLER MODE (ZERO-LAG UDP + STICK BOXES) ──
                XboxLandscapeGamepadView(
                    controllerName = controllerName,
                    assignedSlot = assignedSlot,
                    currentMode = currentMode,
                    isPpssppMode = isPpssppMode,
                    onTogglePpssppMode = { enabled ->
                        isPpssppMode = enabled
                        prefs.edit().putBoolean(PREF_XBOX_PPSSPP_MODE, enabled).apply()
                    },
                    showModeDropdown = showModeDropdown,
                    onToggleModeDropdown = { showModeDropdown = it },
                    onSelectMode = { chosenMode ->
                        showModeDropdown = false
                        if (chosenMode == "API_KEY") {
                            showApiKeyDialog = true
                        } else {
                            currentMode = chosenMode
                            RnaControllerNetworkBridge.updatePhoneModeAndName(
                                context = context,
                                controllerName = controllerName,
                                mode = chosenMode
                            )
                        }
                    },
                    onOpenEditName = { showEditNameDialog = true },
                    onDisconnect = {
                        RnaControllerNetworkBridge.disconnectPhoneFromTv()
                    },
                    onClose = {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        onDismiss()
                    }
                )
            } else {
                // ── MATERIAL 3 EXPRESSIVE TV REMOTE MODE (PORTRAIT) ──
                ExpressiveTvRemoteView(
                    controllerName = controllerName,
                    assignedSlot = assignedSlot,
                    currentMode = currentMode,
                    showModeDropdown = showModeDropdown,
                    onToggleModeDropdown = { showModeDropdown = it },
                    onSelectMode = { chosenMode ->
                        showModeDropdown = false
                        if (chosenMode == "API_KEY") {
                            showApiKeyDialog = true
                        } else {
                            currentMode = chosenMode
                            RnaControllerNetworkBridge.updatePhoneModeAndName(
                                context = context,
                                controllerName = controllerName,
                                mode = chosenMode
                            )
                        }
                    },
                    tvVolumePercent = tvVolumePercent,
                    availableTvApps = availableTvApps,
                    pinnedAppPackages = pinnedAppPackages,
                    onOpenAddTvApps = { showAddTvAppsDialog = true },
                    onRemovePinnedApp = { pkg ->
                        pinnedAppPackages.remove(pkg)
                        savePinnedPackages()
                    },
                    onOpenKeyboard = { showKeyboardDialog = true },
                    onOpenEditName = { showEditNameDialog = true },
                    onDisconnect = {
                        RnaControllerNetworkBridge.disconnectPhoneFromTv()
                    },
                    onClose = {
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        onDismiss()
                    }
                )
            }

            // ── SUBMIT GOOGLE AI API KEY TO TV DIALOG (OPENED FROM CONTROLLER DROPDOWN) ──
            if (showApiKeyDialog) {
                var draftApiKey by remember {
                    mutableStateOf(com.example.ui.tv.RnaFreeAiRouter.getUserGeminiApiKey(context))
                }
                var draftModel by remember {
                    mutableStateOf(com.example.ui.tv.RnaFreeAiRouter.getSelectedGeminiModel(context))
                }
                Dialog(onDismissRequest = { showApiKeyDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = Color(0xFF171B2B),
                        border = BorderStroke(1.5.dp, CyanPrimary.copy(alpha = 0.65f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = CyanPrimary)
                                Column {
                                    Text(
                                        text = "Google AI API Key for TV",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Paste your Gemini API key on your phone to beam it directly to RNA. TV",
                                        color = Color(0xFFAAB4CF),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = draftApiKey,
                                onValueChange = { draftApiKey = it },
                                singleLine = true,
                                label = { Text("Google Gemini API Key (AIza...)", color = Color(0xFFAAB4CF)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CyanPrimary,
                                    unfocusedBorderColor = Color(0x44FFFFFF)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("phone_controller_gemini_api_key_input")
                            )

                            Text(
                                text = "Preferred Gemini Model:",
                                color = CyanPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                com.example.ui.tv.RnaFreeAiRouter.AVAILABLE_GEMINI_MODELS.forEach { (modelId, label) ->
                                    val isSel = draftModel == modelId
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSel) CyanPrimary.copy(alpha = 0.25f) else Color(0xFF101524),
                                        border = BorderStroke(1.dp, if (isSel) CyanPrimary else Color(0x33FFFFFF)),
                                        modifier = Modifier.clickable { draftModel = modelId }
                                    ) {
                                        Text(
                                            text = label.substringBefore(" ("),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        draftApiKey = ""
                                        com.example.ui.tv.RnaFreeAiRouter.saveUserGeminiApiKey(context, "", autoActivate = false)
                                        RnaControllerNetworkBridge.sendGeminiApiKeyFromPhone("", draftModel)
                                        Toast.makeText(context, "Cleared API key on TV (Free AI Fallback active)", Toast.LENGTH_SHORT).show()
                                        showApiKeyDialog = false
                                    }
                                ) {
                                    Text("Use Free AI Instead", color = AmberAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { showApiKeyDialog = false }) {
                                        Text("Cancel", color = Color(0xFFAAB4CF))
                                    }
                                    Button(
                                        onClick = {
                                            val clean = draftApiKey.trim()
                                            com.example.ui.tv.RnaFreeAiRouter.saveUserGeminiApiKey(
                                                context = context,
                                                apiKey = clean,
                                                autoActivate = clean.isNotBlank()
                                            )
                                            com.example.ui.tv.RnaFreeAiRouter.setSelectedGeminiModel(context, draftModel)
                                            RnaControllerNetworkBridge.sendGeminiApiKeyFromPhone(clean, draftModel)
                                            Toast.makeText(
                                                context,
                                                if (clean.isNotBlank()) "Sent Google AI API Key to TV!" else "Switched TV to Free AI Fallback Chain",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            showApiKeyDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = CyanPrimary,
                                            contentColor = Color(0xFF0A0E17)
                                        ),
                                        modifier = Modifier.testTag("phone_controller_send_api_key_btn")
                                    ) {
                                        Text("Send to TV", fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── EDIT CONTROLLER NAME DIALOG ──
            if (showEditNameDialog) {
                var draftName by remember(controllerName) { mutableStateOf(controllerName) }
                Dialog(onDismissRequest = { showEditNameDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = Color(0xFF171B2B),
                        border = BorderStroke(1.5.dp, CyanPrimary.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Controller Name",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "This name is displayed on RNA TV under Connected Controllers:",
                                color = Color(0xFFB5B7C8),
                                fontSize = 12.5.sp
                            )
                            OutlinedTextField(
                                value = draftName,
                                onValueChange = { draftName = it.take(22) },
                                singleLine = true,
                                label = { Text("Phone Controller Name") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CyanPrimary,
                                    unfocusedBorderColor = Color(0x55FFFFFF)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("phone_controller_name_input")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(onClick = { showEditNameDialog = false }) {
                                    Text("Cancel", color = Color.White)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Button(
                                    onClick = {
                                        val finalName = draftName.trim().ifBlank { "RNA Phone" }.take(22)
                                        controllerName = finalName
                                        RnaControllerNetworkBridge.updatePhoneModeAndName(
                                            context = context,
                                            controllerName = finalName,
                                            mode = currentMode
                                        )
                                        showEditNameDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyanPrimary,
                                        contentColor = Color(0xFF0E1324)
                                    )
                                ) {
                                    Text("Save Name", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }

            // ── SEND KEYBOARD TEXT TO TV DIALOG ──
            if (showKeyboardDialog) {
                Dialog(onDismissRequest = { showKeyboardDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = Color(0xFF171B2B),
                        border = BorderStroke(1.5.dp, CyanPrimary.copy(alpha = 0.65f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Keyboard,
                                    contentDescription = null,
                                    tint = CyanPrimary
                                )
                                Column {
                                    Text(
                                        text = "TV Remote Keyboard",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "Type below and tap Send to fill the active text box on RNA TV",
                                        color = Color(0xFFA9ABBC),
                                        fontSize = 11.5.sp
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = keyboardText,
                                onValueChange = { keyboardText = it },
                                placeholder = { Text("Type text to send to TV...") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CyanPrimary,
                                    unfocusedBorderColor = Color(0x55FFFFFF)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("phone_remote_keyboard_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        keyboardText = ""
                                        showKeyboardDialog = false
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Close", color = Color.White)
                                }

                                Button(
                                    onClick = {
                                        if (keyboardText.isNotEmpty()) {
                                            RnaControllerNetworkBridge.sendTextToTvOpenTextBox(keyboardText)
                                            keyboardText = ""
                                            showKeyboardDialog = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CyanPrimary,
                                        contentColor = Color(0xFF0E1324)
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.testTag("phone_remote_keyboard_send_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Send to TV", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }

            // ── ADD TV APPS AS SHORTCUT BUTTONS DIALOG ──
            if (showAddTvAppsDialog) {
                Dialog(onDismissRequest = { showAddTvAppsDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = Color(0xFF151929),
                        border = BorderStroke(1.5.dp, CyanPrimary.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(500.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Add TV Apps to Remote",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "Tap any TV app to pin or unpin it as a quick-launch button",
                                        color = Color(0xFFA9ABBC),
                                        fontSize = 11.5.sp
                                    )
                                }
                                IconButton(onClick = { showAddTvAppsDialog = false }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0x26FFFFFF))
                            Spacer(modifier = Modifier.height(10.dp))

                            if (availableTvApps.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Waiting for TV app catalog...",
                                        color = Color(0xFF9E9FAF),
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(
                                        items = availableTvApps,
                                        key = { it.packageName }
                                    ) { tvApp ->
                                        val isPinned = tvApp.packageName in pinnedAppPackages
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isPinned) Color(0xFF232B44) else Color(0xFF1B1F30),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isPinned) CyanPrimary else Color(0x28FFFFFF)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (isPinned) {
                                                        pinnedAppPackages.remove(tvApp.packageName)
                                                    } else {
                                                        pinnedAppPackages.add(tvApp.packageName)
                                                    }
                                                    savePinnedPackages()
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    TvExportedAppIcon(
                                                        app = tvApp,
                                                        modifier = Modifier.size(40.dp)
                                                    )
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = tvApp.title,
                                                            color = Color.White,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = tvApp.packageName,
                                                            color = Color(0xFF8F92A6),
                                                            fontSize = 10.5.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }

                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (isPinned) CyanPrimary else Color(0xFF282D42),
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = if (isPinned) Icons.Default.Check else Icons.Default.Add,
                                                            contentDescription = null,
                                                            tint = if (isPinned) Color(0xFF0E1324) else Color.White,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { showAddTvAppsDialog = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanPrimary,
                                    contentColor = Color(0xFF0E1324)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Done", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneControllerPairingView(
    controllerName: String,
    onEditControllerName: () -> Unit,
    pinInput: String,
    onPinChange: (String) -> Unit,
    manualTvIp: String,
    showManualIpField: Boolean,
    onToggleManualIpField: () -> Unit,
    onManualTvIpChange: (String) -> Unit,
    statusMessage: String,
    isConnecting: Boolean,
    onConnectClick: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
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
                    color = CyanPrimary.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, CyanPrimary),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SettingsRemote,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "RNA Controller",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Material 3 Expressive Remote & Xbox Gamepad",
                        color = CyanPrimary,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.testTag("phone_controller_close_btn")
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Controller Identity Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF171C2D),
            border = BorderStroke(1.dp, Color(0x38FFFFFF)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditControllerName() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "YOUR CONTROLLER NAME ON TV",
                        color = CyanPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = controllerName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF242B44)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            tint = CyanPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("Rename", color = CyanPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 4-Digit Code Card
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF151929),
            border = BorderStroke(1.5.dp, CyanPrimary.copy(alpha = 0.55f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(32.dp)
                )

                Text(
                    text = "Enter 4-Digit TV Code",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = "On RNA TV, open Devices (top bar) → RNA Controller to view the 4-digit pairing code.",
                    color = Color(0xFFB5B7C8),
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )

                // Expressive 4-Digit Boxes Preview
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val char = pinInput.getOrNull(i)?.toString() ?: "•"
                        val isFilled = i < pinInput.length
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isFilled) Color(0xFF222A44) else Color(0xFF101320),
                            border = BorderStroke(
                                width = if (isFilled) 2.dp else 1.dp,
                                color = if (isFilled) CyanPrimary else Color(0x44FFFFFF)
                            ),
                            modifier = Modifier.size(width = 58.dp, height = 68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = char,
                                    color = if (isFilled) Color.White else Color(0xFF5D627A),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = onPinChange,
                    label = { Text("4-Digit Pairing Code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = Color(0x44FFFFFF)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("phone_controller_pin_input")
                )

                // Optional Direct IP Override for complex routers
                Text(
                    text = if (showManualIpField) "Hide Manual TV IP" else "Optional: Enter TV IP Address Manually",
                    color = CyanPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onToggleManualIpField() }
                )

                AnimatedVisibility(visible = showManualIpField) {
                    OutlinedTextField(
                        value = manualTvIp,
                        onValueChange = onManualTvIpChange,
                        label = { Text("TV Local IP (e.g. 192.168.1.50)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = Color(0x44FFFFFF)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Button(
                    onClick = onConnectClick,
                    enabled = pinInput.length == 4 && !isConnecting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = Color(0xFF0E1324)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("phone_controller_pair_btn")
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            color = Color(0xFF0E1324),
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Connecting to RNA TV...", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    } else {
                        Icon(imageVector = Icons.Default.SettingsRemote, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pair & Open Remote", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                    }
                }

                if (statusMessage.isNotBlank()) {
                    Text(
                        text = statusMessage,
                        color = AmberAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Clean, Uncluttered Material 3 Portrait Phone Remote Mode:
 * - Minimal top bar (Mode selector + live status subtitle on left, overflow menu & close on right)
 * - Sculpted circular D-Pad wheel with tactile center OK disc
 * - Borderless primary nav row (Back, Home, Keyboard)
 * - Slim single-row volume capsule with mute tap + slider
 * - Unified smart utility bar (Recents, Taskbar, RNA AI)
 * - Compact TV Apps dock with clean Edit mode toggle
 */
@Composable
private fun ExpressiveTvRemoteView(
    controllerName: String,
    assignedSlot: Int,
    currentMode: String,
    showModeDropdown: Boolean,
    onToggleModeDropdown: (Boolean) -> Unit,
    onSelectMode: (String) -> Unit,
    tvVolumePercent: Int,
    availableTvApps: List<ExportedTvAppInfo>,
    pinnedAppPackages: List<String>,
    onOpenAddTvApps: () -> Unit,
    onRemovePinnedApp: (String) -> Unit,
    onOpenKeyboard: () -> Unit,
    onOpenEditName: () -> Unit,
    onDisconnect: () -> Unit,
    onClose: () -> Unit
) {
    val view = LocalView.current
    var sliderVolume by remember(tvVolumePercent) { mutableFloatStateOf(tvVolumePercent.toFloat()) }
    var lastNonZeroVolume by remember { mutableFloatStateOf(tvVolumePercent.coerceAtLeast(25).toFloat()) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var isEditingApps by remember { mutableStateOf(false) }

    val pinnedApps = remember(availableTvApps, pinnedAppPackages.toList()) {
        pinnedAppPackages.mapNotNull { pkg ->
            availableTvApps.find { it.packageName == pkg }
        }
    }

    LaunchedEffect(pinnedApps.isEmpty()) {
        if (pinnedApps.isEmpty()) {
            isEditingApps = false
        }
    }

    val surfaceBase = Color(0xFF151B29)
    val surfaceElevated = Color(0xFF1C2436)
    val hairline = Color.White.copy(alpha = 0.07f)
    val textPrimary = Color(0xFFF2F5FA)
    val textMuted = Color(0xFF8B97AD)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0A0D14),
                        Color(0xFF0E131F),
                        Color(0xFF090C13)
                    )
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 440.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Minimalist Top Bar (Mode Selector + Subtle Status on Left, Overflow & Close on Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = surfaceBase,
                            border = BorderStroke(1.dp, hairline),
                            modifier = Modifier
                                .clickable { onToggleModeDropdown(true) }
                                .testTag("phone_controller_mode_dropdown_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SettingsRemote,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (currentMode == "REMOTE") "TV Remote" else "Xbox Controller",
                                    color = textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch Mode",
                                    tint = textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showModeDropdown,
                            onDismissRequest = { onToggleModeDropdown(false) },
                            modifier = Modifier.background(Color(0xFF171E2E))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.SettingsRemote, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                                        Text("TV Remote (Portrait)", color = textPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                onClick = { onSelectMode("REMOTE") }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.Gamepad, contentDescription = null, tint = EmeraldSuccess, modifier = Modifier.size(18.dp))
                                        Text("Xbox / PPSSPP Pad (Landscape)", color = textPrimary, fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                onClick = { onSelectMode("CONTROLLER") }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.Key, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(18.dp))
                                        Text("Google AI API Key", color = AmberAccent, fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                onClick = { onSelectMode("API_KEY") }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenEditName() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(EmeraldSuccess)
                        )
                        Text(
                            text = "$controllerName · P$assignedSlot",
                            color = textMuted,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Remote Options",
                                tint = textMuted,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            modifier = Modifier.background(Color(0xFF171E2E))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(18.dp))
                                        Text("Rename Controller", color = textPrimary)
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onOpenEditName()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.Key, contentDescription = null, tint = AmberAccent, modifier = Modifier.size(18.dp))
                                        Text("Google AI API Key", color = textPrimary)
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onSelectMode("API_KEY")
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.LinkOff, contentDescription = null, tint = CoralAccent, modifier = Modifier.size(18.dp))
                                        Text("Unpair TV", color = CoralAccent)
                                    }
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    onDisconnect()
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Remote",
                            tint = textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 2. Sculpted Circular D-Pad Wheel (Focal Point)
            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .size(244.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1D263A),
                                Color(0xFF141B2A),
                                Color(0xFF101623)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.09f), CircleShape)
                    .testTag("phone_remote_dpad_pad"),
                contentAlignment = Alignment.Center
            ) {
                // Subtle inner guide ring
                Box(
                    modifier = Modifier
                        .size(106.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0C101A))
                )

                // UP
                SculptedDpadDirectionButton(
                    icon = Icons.Default.KeyboardArrowUp,
                    label = "Up",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                        .size(width = 88.dp, height = 64.dp)
                        .testTag("phone_remote_dpad_up"),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        RnaControllerNetworkBridge.sendRemoteCommandFromPhone("DPAD_UP")
                    }
                )

                // DOWN
                SculptedDpadDirectionButton(
                    icon = Icons.Default.KeyboardArrowDown,
                    label = "Down",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .size(width = 88.dp, height = 64.dp)
                        .testTag("phone_remote_dpad_down"),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        RnaControllerNetworkBridge.sendRemoteCommandFromPhone("DPAD_DOWN")
                    }
                )

                // LEFT
                SculptedDpadDirectionButton(
                    icon = Icons.Default.KeyboardArrowLeft,
                    label = "Left",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                        .size(width = 64.dp, height = 88.dp)
                        .testTag("phone_remote_dpad_left"),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        RnaControllerNetworkBridge.sendRemoteCommandFromPhone("DPAD_LEFT")
                    }
                )

                // RIGHT
                SculptedDpadDirectionButton(
                    icon = Icons.Default.KeyboardArrowRight,
                    label = "Right",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .size(width = 64.dp, height = 88.dp)
                        .testTag("phone_remote_dpad_right"),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        RnaControllerNetworkBridge.sendRemoteCommandFromPhone("DPAD_RIGHT")
                    }
                )

                // CENTER OK DISC
                var okPressed by remember { mutableStateOf(false) }
                Surface(
                    shape = CircleShape,
                    color = if (okPressed) CyanPrimary else surfaceElevated,
                    border = BorderStroke(
                        width = if (okPressed) 2.dp else 1.5.dp,
                        color = if (okPressed) Color.White else CyanPrimary.copy(alpha = 0.55f)
                    ),
                    shadowElevation = if (okPressed) 2.dp else 10.dp,
                    modifier = Modifier
                        .size(84.dp)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                okPressed = true
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                RnaControllerNetworkBridge.sendRemoteCommandFromPhone("DPAD_OK")
                                waitForUpOrCancellation()
                                okPressed = false
                            }
                        }
                        .testTag("phone_remote_dpad_ok")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "OK",
                            color = if (okPressed) Color(0xFF0A0E17) else textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            // 3. Borderless Primary Navigation Row (Back • Home • Keyboard)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TonalRemotePillButton(
                    label = "Back",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    containerColor = surfaceBase,
                    contentColor = textPrimary,
                    testTag = "phone_remote_back_btn",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        RnaControllerNetworkBridge.sendRemoteCommandFromPhone("BACK")
                    }
                )

                TonalRemotePillButton(
                    label = "Home",
                    icon = Icons.Default.Home,
                    containerColor = CyanPrimary.copy(alpha = 0.16f),
                    contentColor = CyanPrimary,
                    testTag = "phone_remote_home_btn",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        RnaControllerNetworkBridge.sendRemoteCommandFromPhone("HOME")
                    }
                )

                TonalRemotePillButton(
                    label = "Keyboard",
                    icon = Icons.Default.Keyboard,
                    containerColor = surfaceBase,
                    contentColor = textPrimary,
                    testTag = "phone_remote_keyboard_btn",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        onOpenKeyboard()
                    }
                )
            }

            // 4. Slim Single-Row Volume Capsule
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = surfaceBase,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("phone_remote_volume_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 6.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            if (sliderVolume > 0.5f) {
                                lastNonZeroVolume = sliderVolume
                                sliderVolume = 0f
                            } else {
                                sliderVolume = lastNonZeroVolume.coerceIn(15f, 100f)
                            }
                            RnaControllerNetworkBridge.sendVolumeSliderFromPhone(sliderVolume.roundToInt())
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (sliderVolume <= 0.5f) {
                                Icons.AutoMirrored.Filled.VolumeOff
                            } else {
                                Icons.AutoMirrored.Filled.VolumeUp
                            },
                            contentDescription = "Toggle Mute",
                            tint = if (sliderVolume <= 0.5f) textMuted else CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Slider(
                        value = sliderVolume,
                        onValueChange = { next ->
                            sliderVolume = next
                            if (next > 0.5f) lastNonZeroVolume = next
                            RnaControllerNetworkBridge.sendVolumeSliderFromPhone(next.roundToInt())
                        },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = textPrimary,
                            activeTrackColor = CyanPrimary,
                            inactiveTrackColor = Color(0xFF263047)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("phone_remote_volume_slider")
                    )

                    Text(
                        text = "${sliderVolume.roundToInt()}%",
                        color = textMuted,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }

            // 5. Unified Smart Utility Dock (Recents • Taskbar • RNA AI)
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = surfaceBase,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DockedUtilitySegment(
                        label = "Recents",
                        icon = Icons.Default.History,
                        tint = textPrimary,
                        testTag = "phone_remote_recent_apps_btn",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            RnaControllerNetworkBridge.sendRemoteCommandFromPhone("RECENT_APPS")
                        }
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(hairline)
                    )

                    DockedUtilitySegment(
                        label = "Taskbar",
                        icon = Icons.Default.ViewCarousel,
                        tint = textPrimary,
                        testTag = "phone_remote_taskbar_btn",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            RnaControllerNetworkBridge.sendRemoteCommandFromPhone("TASKBAR")
                        }
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(hairline)
                    )

                    DockedUtilitySegment(
                        label = "RNA AI",
                        icon = Icons.Default.AutoAwesome,
                        tint = CyanPrimary,
                        testTag = "phone_remote_ai_bar_btn",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            RnaControllerNetworkBridge.sendRemoteCommandFromPhone("RNA_AI_BAR")
                        }
                    )
                }
            }

            // 6. Clean Compact TV Apps Dock
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TV APPS",
                        color = textMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (pinnedApps.isNotEmpty()) {
                            Text(
                                text = if (isEditingApps) "Done" else "Edit",
                                color = if (isEditingApps) CyanPrimary else textMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { isEditingApps = !isEditingApps }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = surfaceElevated,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { onOpenAddTvApps() }
                                .testTag("phone_remote_add_tv_apps_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add TV Apps",
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (pinnedApps.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = surfaceBase,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { onOpenAddTvApps() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pin TV apps for one-tap launch",
                                color = textMuted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(
                            items = pinnedApps,
                            key = { it.packageName }
                        ) { app ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(68.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        if (isEditingApps) {
                                            onRemovePinnedApp(app.packageName)
                                        } else {
                                            RnaControllerNetworkBridge.sendLaunchTvAppFromPhone(app.packageName)
                                        }
                                    }
                                    .padding(vertical = 4.dp)
                                    .testTag("phone_remote_tv_app_btn_${app.packageName}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Surface(
                                        shape = CircleShape,
                                        color = surfaceBase,
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.padding(4.dp)
                                        ) {
                                            TvExportedAppIcon(
                                                app = app,
                                                modifier = Modifier.size(44.dp)
                                            )
                                        }
                                    }

                                    if (isEditingApps) {
                                        Surface(
                                            shape = CircleShape,
                                            color = CoralAccent,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove ${app.title}",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = app.title,
                                    color = textMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
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
}

@Composable
private fun SculptedDpadDirectionButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            onClick()
            delay(320L)
            while (isPressed) {
                onClick()
                delay(115L)
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(CircleShape)
            .background(if (isPressed) CyanPrimary.copy(alpha = 0.2f) else Color.Transparent)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    waitForUpOrCancellation()
                    isPressed = false
                }
            }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isPressed) CyanPrimary else Color(0xFFD2DAE8),
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun TonalRemotePillButton(
    label: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(27.dp),
        color = containerColor,
        modifier = modifier
            .height(54.dp)
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(19.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = contentColor,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DockedUtilitySegment(
    label: String,
    icon: ImageVector,
    tint: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable { onClick() }
            .testTag(testTag),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = tint.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

/**
 * Landscape Xbox / PPSSPP Controller View:
 * - Automatically locks screen to Landscape.
 * - Includes a very tiny `PPSSPP` toggle in the center column that remaps every button & Left Stick
 *   to PPSSPP's built-in default controls (×/○/□/△, L/R, SELECT/START, D-Pad, Analog I/J/K/L,
 *   MENU/ESC, FF/TAB, SAVE/F2, LOAD/F4) with true held key injection and zero Accessibility hijack.
 */
@Composable
private fun XboxLandscapeGamepadView(
    controllerName: String,
    assignedSlot: Int,
    currentMode: String,
    isPpssppMode: Boolean,
    onTogglePpssppMode: (Boolean) -> Unit,
    showModeDropdown: Boolean,
    onToggleModeDropdown: (Boolean) -> Unit,
    onSelectMode: (String) -> Unit,
    onOpenEditName: () -> Unit,
    onDisconnect: () -> Unit,
    onClose: () -> Unit
) {
    val view = LocalView.current

    var leftStickX by remember { mutableFloatStateOf(0f) }
    var leftStickY by remember { mutableFloatStateOf(0f) }
    var rightStickX by remember { mutableFloatStateOf(0f) }
    var rightStickY by remember { mutableFloatStateOf(0f) }
    var leftTrigger by remember { mutableFloatStateOf(0f) }
    var rightTrigger by remember { mutableFloatStateOf(0f) }
    var hatX by remember { mutableFloatStateOf(0f) }
    var hatY by remember { mutableFloatStateOf(0f) }
    var buttonsMask by remember { mutableIntStateOf(0) }

    val pspBlue = Color(0xFF5DA9FF)
    val pspPink = Color(0xFFF48FB1)
    val pspGreen = Color(0xFF4ADE80)
    val pspRed = Color(0xFFFF6B6B)
    val latestIsPpssppMode by rememberUpdatedState(isPpssppMode)

    // Cleanly release any held buttons/sticks when leaving landscape gamepad mode
    DisposableEffect(Unit) {
        onDispose {
            RnaControllerNetworkBridge.clearActivePhoneGamepadState(isPpssppMode = false)
        }
    }

    fun pushGamepadFrame(ppssppOverride: Boolean = latestIsPpssppMode) {
        RnaControllerNetworkBridge.sendXboxGamepadFrameUdp(
            lx = leftStickX,
            ly = leftStickY,
            rx = rightStickX,
            ry = rightStickY,
            lt = leftTrigger,
            rt = rightTrigger,
            hatX = hatX,
            hatY = hatY,
            buttonsMask = buttonsMask,
            isPpssppMode = ppssppOverride
        )
    }

    fun handleButtonPress(keyCode: Int, maskBit: Int, isDown: Boolean) {
        if (isDown) {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            buttonsMask = buttonsMask or maskBit
        } else {
            buttonsMask = buttonsMask and maskBit.inv()
        }
        RnaControllerNetworkBridge.sendXboxButtonEventImmediate(
            keyCode = keyCode,
            isDown = isDown
        )
        pushGamepadFrame()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF161C2E),
                        Color(0xFF0B0E17)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("xbox_controller_landscape_root"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── LEFT SIDE: [LEFT STICK SQUARE BOX + D-PAD] WITH SMALL SIDE BUTTONS [LT/MENU, LB/L, L3/SAVE] ──
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Stick Square Box (Top) + D-Pad Cluster (Bottom)
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BlankSquareAnalogStickBox(
                    stickLabel = if (isPpssppMode) "PSP STICK" else "L-STICK",
                    accentColor = if (isPpssppMode) pspBlue else CyanPrimary,
                    currentX = leftStickX,
                    currentY = leftStickY,
                    onAxisChanged = { nx, ny ->
                        leftStickX = nx
                        leftStickY = ny
                        pushGamepadFrame()
                    },
                    modifier = Modifier
                        .size(120.dp)
                        .testTag("xbox_left_analog_square_box")
                )

                // Bottom Left: D-Pad (UNDER the Left Stick Box)
                Box(
                    modifier = Modifier
                        .size(124.dp)
                        .testTag("xbox_dpad_cluster")
                ) {
                    // D-Pad UP
                    GamepadHoldButton(
                        label = "▲",
                        accentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(width = 42.dp, height = 40.dp)
                            .testTag("xbox_dpad_up"),
                        onPressChange = { pressed ->
                            hatY = if (pressed) -1f else 0f
                            handleButtonPress(KeyEvent.KEYCODE_DPAD_UP, 1 shl XboxButtonBits.BIT_DPAD_UP, pressed)
                        }
                    )
                    // D-Pad DOWN
                    GamepadHoldButton(
                        label = "▼",
                        accentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .size(width = 42.dp, height = 40.dp)
                            .testTag("xbox_dpad_down"),
                        onPressChange = { pressed ->
                            hatY = if (pressed) 1f else 0f
                            handleButtonPress(KeyEvent.KEYCODE_DPAD_DOWN, 1 shl XboxButtonBits.BIT_DPAD_DOWN, pressed)
                        }
                    )
                    // D-Pad LEFT
                    GamepadHoldButton(
                        label = "◀",
                        accentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(width = 40.dp, height = 42.dp)
                            .testTag("xbox_dpad_left"),
                        onPressChange = { pressed ->
                            hatX = if (pressed) -1f else 0f
                            handleButtonPress(KeyEvent.KEYCODE_DPAD_LEFT, 1 shl XboxButtonBits.BIT_DPAD_LEFT, pressed)
                        }
                    )
                    // D-Pad RIGHT
                    GamepadHoldButton(
                        label = "▶",
                        accentColor = Color.White,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(width = 40.dp, height = 42.dp)
                            .testTag("xbox_dpad_right"),
                        onPressChange = { pressed ->
                            hatX = if (pressed) 1f else 0f
                            handleButtonPress(KeyEvent.KEYCODE_DPAD_RIGHT, 1 shl XboxButtonBits.BIT_DPAD_RIGHT, pressed)
                        }
                    )
                }
            }

            // Small Left Side Buttons (LT/MENU, LB/L, L3/SAVE)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GamepadHoldButton(
                    label = if (isPpssppMode) "MENU" else "LT",
                    subLabel = if (isPpssppMode) "ESC" else null,
                    accentColor = AmberAccent,
                    modifier = Modifier
                        .size(width = 48.dp, height = 38.dp)
                        .testTag("xbox_btn_lt"),
                    onPressChange = { pressed ->
                        leftTrigger = if (pressed) 1f else 0f
                        handleButtonPress(KeyEvent.KEYCODE_BUTTON_L2, 1 shl XboxButtonBits.BIT_LT, pressed)
                    }
                )
                GamepadHoldButton(
                    label = if (isPpssppMode) "L" else "LB",
                    accentColor = if (isPpssppMode) pspBlue else CyanPrimary,
                    modifier = Modifier
                        .size(width = 48.dp, height = 38.dp)
                        .testTag("xbox_btn_lb"),
                    onPressChange = { pressed ->
                        handleButtonPress(KeyEvent.KEYCODE_BUTTON_L1, 1 shl XboxButtonBits.BIT_LB, pressed)
                    }
                )
                GamepadHoldButton(
                    label = if (isPpssppMode) "SAVE" else "L3",
                    subLabel = if (isPpssppMode) "F2" else null,
                    accentColor = if (isPpssppMode) EmeraldSuccess else CyanPrimary,
                    modifier = Modifier
                        .size(width = 48.dp, height = 38.dp)
                        .testTag("xbox_btn_l3"),
                    onPressChange = { pressed ->
                        handleButtonPress(KeyEvent.KEYCODE_BUTTON_THUMBL, 1 shl XboxButtonBits.BIT_THUMBL, pressed)
                    }
                )
            }
        }

        // ── CENTER COLUMN: TOP DROPDOWN + TINY PPSSPP TOGGLE + GUIDE / SELECT / START + BADGE ──
        Column(
            modifier = Modifier
                .width(216.dp)
                .fillMaxHeight()
                .padding(horizontal = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Mode Dropdown + Close button + Very Tiny PPSSPP Toggle
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF1F253B),
                            border = BorderStroke(1.dp, if (isPpssppMode) pspBlue else EmeraldSuccess),
                            modifier = Modifier.clickable { onToggleModeDropdown(true) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Gamepad,
                                    contentDescription = null,
                                    tint = if (isPpssppMode) pspBlue else EmeraldSuccess,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (isPpssppMode) "PPSSPP #$assignedSlot" else "Controller #$assignedSlot",
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = if (isPpssppMode) pspBlue else EmeraldSuccess,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showModeDropdown,
                            onDismissRequest = { onToggleModeDropdown(false) },
                            modifier = Modifier.background(Color(0xFF1A2035))
                        ) {
                            DropdownMenuItem(
                                text = { Text("TV Remote Mode (Portrait)", color = Color.White, fontWeight = FontWeight.Bold) },
                                onClick = { onSelectMode("REMOTE") }
                            )
                            DropdownMenuItem(
                                text = { Text("Xbox / PPSSPP Mode (Landscape)", color = EmeraldSuccess, fontWeight = FontWeight.Bold) },
                                onClick = { onSelectMode("CONTROLLER") }
                            )
                            DropdownMenuItem(
                                text = { Text("Google AI API Key", color = AmberAccent, fontWeight = FontWeight.Bold) },
                                onClick = { onSelectMode("API_KEY") }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onDisconnect, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.LinkOff, contentDescription = "Unpair", tint = CoralAccent, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Very Tiny PPSSPP Remap Toggle Pill
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (isPpssppMode) pspBlue.copy(alpha = 0.22f) else Color(0xFF151929),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isPpssppMode) pspBlue else Color(0x28FFFFFF)
                    ),
                    modifier = Modifier
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            val next = !isPpssppMode
                            buttonsMask = 0
                            leftStickX = 0f
                            leftStickY = 0f
                            rightStickX = 0f
                            rightStickY = 0f
                            leftTrigger = 0f
                            rightTrigger = 0f
                            hatX = 0f
                            hatY = 0f
                            onTogglePpssppMode(next)
                            RnaControllerNetworkBridge.clearActivePhoneGamepadState(isPpssppMode = next)
                        }
                        .testTag("xbox_ppsspp_mode_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isPpssppMode) pspBlue else Color(0xFF5A6278))
                        )
                        Text(
                            text = if (isPpssppMode) "PPSSPP ON" else "PPSSPP",
                            color = if (isPpssppMode) pspBlue else Color(0xFF8B93AA),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.7.sp
                        )
                    }
                }
            }

            // Center Cluster: RNA Guide Button + SELECT & START
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                var guidePressed by remember { mutableStateOf(false) }
                Surface(
                    shape = CircleShape,
                    color = if (guidePressed) CyanPrimary else Color(0xFF1A2136),
                    border = BorderStroke(2.5.dp, if (guidePressed) Color.White else if (isPpssppMode) pspBlue else CyanPrimary),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .size(66.dp)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                guidePressed = true
                                handleButtonPress(KeyEvent.KEYCODE_BUTTON_MODE, 1 shl XboxButtonBits.BIT_MODE, true)
                                waitForUpOrCancellation()
                                guidePressed = false
                                handleButtonPress(KeyEvent.KEYCODE_BUTTON_MODE, 1 shl XboxButtonBits.BIT_MODE, false)
                            }
                        }
                        .testTag("xbox_btn_rna_guide")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground_img),
                            contentDescription = "RNA Guide Button",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                // Back (Select) & Start Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GamepadHoldButton(
                        label = if (isPpssppMode) "SELECT" else "BACK",
                        subLabel = if (isPpssppMode) "V" else "Select",
                        accentColor = Color(0xFFB5B8CE),
                        modifier = Modifier
                            .width(78.dp)
                            .height(38.dp)
                            .testTag("xbox_btn_select"),
                        onPressChange = { pressed ->
                            handleButtonPress(KeyEvent.KEYCODE_BUTTON_SELECT, 1 shl XboxButtonBits.BIT_SELECT, pressed)
                        }
                    )

                    GamepadHoldButton(
                        label = "START",
                        subLabel = if (isPpssppMode) "Space" else "Menu",
                        accentColor = Color(0xFFB5B8CE),
                        modifier = Modifier
                            .width(78.dp)
                            .height(38.dp)
                            .testTag("xbox_btn_start"),
                        onPressChange = { pressed ->
                            handleButtonPress(KeyEvent.KEYCODE_BUTTON_START, 1 shl XboxButtonBits.BIT_START, pressed)
                        }
                    )
                }
            }

            // Bottom Player Slot & Controller Name Badge
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF151929),
                border = BorderStroke(1.dp, if (isPpssppMode) pspBlue.copy(alpha = 0.35f) else Color(0x2EFFFFFF)),
                modifier = Modifier.clickable { onOpenEditName() }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$controllerName • Player $assignedSlot",
                        color = if (isPpssppMode) pspBlue else CyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (isPpssppMode) "PPSSPP Native Keymap Active" else "Xbox Wireless Controller Spoof Active",
                        color = Color(0xFF8F92A6),
                        fontSize = 9.sp
                    )
                }
            }
        }

        // ── RIGHT SIDE: SMALL SIDE BUTTONS [RT/FF, RB/R, R3/LOAD] + [△/○/×/□ OR Y/B/A/X + RIGHT STICK] ──
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Small Right Side Buttons (RT/FF, RB/R, R3/LOAD)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GamepadHoldButton(
                    label = if (isPpssppMode) "FF" else "RT",
                    subLabel = if (isPpssppMode) "TAB" else null,
                    accentColor = AmberAccent,
                    modifier = Modifier
                        .size(width = 48.dp, height = 38.dp)
                        .testTag("xbox_btn_rt"),
                    onPressChange = { pressed ->
                        rightTrigger = if (pressed) 1f else 0f
                        handleButtonPress(KeyEvent.KEYCODE_BUTTON_R2, 1 shl XboxButtonBits.BIT_RT, pressed)
                    }
                )
                GamepadHoldButton(
                    label = if (isPpssppMode) "R" else "RB",
                    accentColor = if (isPpssppMode) pspBlue else CyanPrimary,
                    modifier = Modifier
                        .size(width = 48.dp, height = 38.dp)
                        .testTag("xbox_btn_rb"),
                    onPressChange = { pressed ->
                        handleButtonPress(KeyEvent.KEYCODE_BUTTON_R1, 1 shl XboxButtonBits.BIT_RB, pressed)
                    }
                )
                GamepadHoldButton(
                    label = if (isPpssppMode) "LOAD" else "R3",
                    subLabel = if (isPpssppMode) "F4" else null,
                    accentColor = VioletSecondary,
                    modifier = Modifier
                        .size(width = 48.dp, height = 38.dp)
                        .testTag("xbox_btn_r3"),
                    onPressChange = { pressed ->
                        handleButtonPress(KeyEvent.KEYCODE_BUTTON_THUMBR, 1 shl XboxButtonBits.BIT_THUMBR, pressed)
                    }
                )
            }

            // Face Buttons Diamond Cluster (Top) + Right Stick Square Box (Bottom)
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .testTag("xbox_xyab_cluster")
                ) {
                    // Top: △ (Triangle) or Y
                    XboxFaceButton(
                        letter = if (isPpssppMode) "△" else "Y",
                        accentColor = if (isPpssppMode) pspGreen else AmberAccent,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .testTag("xbox_btn_y"),
                        onPressChange = { pressed ->
                            handleButtonPress(KeyEvent.KEYCODE_BUTTON_Y, 1 shl XboxButtonBits.BIT_Y, pressed)
                        }
                    )
                    // Bottom: × (Cross) or A
                    XboxFaceButton(
                        letter = if (isPpssppMode) "×" else "A",
                        accentColor = if (isPpssppMode) pspBlue else EmeraldSuccess,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .testTag("xbox_btn_a"),
                        onPressChange = { pressed ->
                            handleButtonPress(KeyEvent.KEYCODE_BUTTON_A, 1 shl XboxButtonBits.BIT_A, pressed)
                        }
                    )
                    // Left: □ (Square) or X
                    XboxFaceButton(
                        letter = if (isPpssppMode) "□" else "X",
                        accentColor = if (isPpssppMode) pspPink else CyanPrimary,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .testTag("xbox_btn_x"),
                        onPressChange = { pressed ->
                            handleButtonPress(KeyEvent.KEYCODE_BUTTON_X, 1 shl XboxButtonBits.BIT_X, pressed)
                        }
                    )
                    // Right: ○ (Circle) or B
                    XboxFaceButton(
                        letter = if (isPpssppMode) "○" else "B",
                        accentColor = if (isPpssppMode) pspRed else CoralAccent,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .testTag("xbox_btn_b"),
                        onPressChange = { pressed ->
                            handleButtonPress(KeyEvent.KEYCODE_BUTTON_B, 1 shl XboxButtonBits.BIT_B, pressed)
                        }
                    )
                }

                // Bottom Right: Blank Square Box for Right Analog Stick
                BlankSquareAnalogStickBox(
                    stickLabel = if (isPpssppMode) "CAM" else "R-STICK",
                    accentColor = VioletSecondary,
                    currentX = rightStickX,
                    currentY = rightStickY,
                    onAxisChanged = { nx, ny ->
                        rightStickX = nx
                        rightStickY = ny
                        pushGamepadFrame()
                    },
                    modifier = Modifier
                        .size(120.dp)
                        .testTag("xbox_right_analog_square_box")
                )
            }
        }
    }
}

/**
 * Blank Square Touch Box for Left & Right Analog Sticks:
 * - Dragging anywhere inside the square immediately streams normalized (-1.0..+1.0) X/Y axis values
 *   with zero lag and automatically springs back to (0, 0) when released.
 */
@Composable
private fun BlankSquareAnalogStickBox(
    stickLabel: String,
    accentColor: Color,
    currentX: Float,
    currentY: Float,
    onAxisChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    val currentOnAxisChanged by rememberUpdatedState(onAxisChanged)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF121624))
            .border(
                width = 2.dp,
                color = if (currentX != 0f || currentY != 0f) accentColor else accentColor.copy(alpha = 0.45f),
                shape = RoundedCornerShape(18.dp)
            )
            .onSizeChanged { boxSize = it }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    fun updateFromOffset(pos: Offset) {
                        val w = boxSize.width.coerceAtLeast(1).toFloat()
                        val h = boxSize.height.coerceAtLeast(1).toFloat()
                        val nx = (((pos.x / w) * 2f) - 1f).coerceIn(-1f, 1f)
                        val ny = (((pos.y / h) * 2f) - 1f).coerceIn(-1f, 1f)
                        currentOnAxisChanged(nx, ny)
                    }
                    updateFromOffset(down.position)
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull()
                        if (change != null && change.pressed) {
                            updateFromOffset(change.position)
                            change.consume()
                        }
                    } while (event.changes.any { it.pressed })
                    currentOnAxisChanged(0f, 0f)
                }
            }
    ) {
        // Subtle corner label so the square box stays clean & blank while indicating L/R stick
        Text(
            text = stickLabel,
            color = accentColor.copy(alpha = 0.35f),
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
        )

        // Live thumb nub indicator inside the square box
        val maxOffsetPx = (boxSize.width * 0.34f).coerceAtLeast(1f)
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset {
                    IntOffset(
                        x = (currentX * maxOffsetPx).roundToInt(),
                        y = (currentY * maxOffsetPx).roundToInt()
                    )
                }
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (currentX != 0f || currentY != 0f) accentColor else accentColor.copy(alpha = 0.35f)
                )
                .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
        )
    }
}

@Composable
private fun XboxFaceButton(
    letter: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onPressChange: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val currentOnPressChange by rememberUpdatedState(onPressChange)
    Surface(
        shape = CircleShape,
        color = if (isPressed) accentColor else Color(0xFF1B2034),
        border = BorderStroke(2.dp, if (isPressed) Color.White else accentColor),
        shadowElevation = if (isPressed) 4.dp else 10.dp,
        modifier = modifier
            .size(44.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    currentOnPressChange(true)
                    waitForUpOrCancellation()
                    isPressed = false
                    currentOnPressChange(false)
                }
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = letter,
                color = if (isPressed) Color(0xFF0B0E18) else accentColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun GamepadHoldButton(
    label: String,
    subLabel: String? = null,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onPressChange: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val currentOnPressChange by rememberUpdatedState(onPressChange)
    Surface(
        shape = RoundedCornerShape(11.dp),
        color = if (isPressed) accentColor else Color(0xFF191E30),
        border = BorderStroke(1.5.dp, if (isPressed) Color.White else accentColor.copy(alpha = 0.65f)),
        modifier = modifier.pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                currentOnPressChange(true)
                waitForUpOrCancellation()
                isPressed = false
                currentOnPressChange(false)
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = if (isPressed) Color(0xFF0C101C) else Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (!subLabel.isNullOrBlank()) {
                Text(
                    text = subLabel,
                    color = if (isPressed) Color(0xFF0C101C) else accentColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TvExportedAppIcon(
    app: ExportedTvAppInfo,
    modifier: Modifier = Modifier
) {
    val decodedBitmap = remember(app.packageName, app.iconBase64) {
        app.iconBase64?.takeIf { it.isNotBlank() }?.let { decodeDataUriToImageBitmap(it) }
    }

    if (decodedBitmap != null) {
        Image(
            bitmap = decodedBitmap,
            contentDescription = app.title,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(CircleShape)
        )
    } else if (app.packageName == "com.rna.tv.ai.app") {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground_img),
            contentDescription = app.title,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(CircleShape)
        )
    } else {
        Surface(
            shape = CircleShape,
            color = Color(0xFF2A3250),
            border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.6f)),
            modifier = modifier
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = app.title.take(2).uppercase(),
                    color = CyanPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
