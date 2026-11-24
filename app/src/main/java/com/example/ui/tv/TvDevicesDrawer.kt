package com.example.ui.tv

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VioletSecondary
import kotlinx.coroutines.delay

/**
 * Left-Side Devices Drawer (similar to Updates drawer):
 * - Opened by clicking the top-bar "Devices" button.
 * - Features:
 *   1. Wi-Fi option -> opens system Wi-Fi settings
 *   2. Bluetooth option underneath -> opens system Bluetooth settings
 *   3. RNA Controller button underneath -> shows a 4-digit pairing number for phones on the same Wi-Fi/Hotspot,
 *      connected controller names, modes (TV Remote vs Xbox Controller), and an option to open a second 4-digit
 *      pairing number for connecting Controller 2 (max 2 controllers).
 */
@Composable
fun TvDevicesSideDrawer(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val context = LocalContext.current
    val firstItemFocusRequester = remember { FocusRequester() }

    var showRnaControllerPanel by remember { mutableStateOf(true) }

    val pinCode1 by RnaControllerNetworkBridge.pinCodeSlot1.collectAsState()
    val pinCode2 by RnaControllerNetworkBridge.pinCodeSlot2.collectAsState()
    val isSecondSlotPinVisible by RnaControllerNetworkBridge.isSecondSlotPinVisible.collectAsState()
    val connectedControllers by RnaControllerNetworkBridge.connectedControllers.collectAsState()
    val tvLocalIp by RnaControllerNetworkBridge.tvLocalIp.collectAsState()
    val lastActionStatus by RnaControllerNetworkBridge.lastActionStatus.collectAsState()

    LaunchedEffect(Unit) {
        RnaControllerNetworkBridge.ensureTvControllerServerStarted(context)
        RnaControllerNetworkBridge.refreshTvIpAddresses()
        delay(80L)
        firstItemFocusRequester.safeRequestFocus()
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x9906080E))
                .onPreviewKeyEvent { keyEvent ->
                    val code = keyEvent.nativeKeyEvent.keyCode
                    if (code == android.view.KeyEvent.KEYCODE_DPAD_LEFT || code == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) {
                        // Consume horizontal escapes so focus stays strictly inside the Devices drawer
                        true
                    } else {
                        false
                    }
                }
                .clickable { onDismiss() }
                .testTag("tv_devices_drawer_scrim")
        ) {
            Surface(
                shape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                color = Color(0xF8121521),
                border = BorderStroke(1.5.dp, CyanPrimary.copy(alpha = 0.45f)),
                shadowElevation = 28.dp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(460.dp)
                    .fillMaxHeight()
                    .clickable(enabled = false) {}
                    .testTag("tv_devices_side_drawer")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 22.dp)
                ) {
                    // Drawer Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = CyanPrimary.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, CyanPrimary),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Devices,
                                        contentDescription = null,
                                        tint = CyanPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Devices & Controllers",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        var closeFocused by remember { mutableStateOf(false) }
                        Surface(
                            shape = CircleShape,
                            color = if (closeFocused) Color.White else Color(0xFF232738),
                            border = BorderStroke(1.dp, if (closeFocused) CyanPrimary else Color(0x44FFFFFF)),
                            modifier = Modifier
                                .size(38.dp)
                                .onFocusChanged { closeFocused = it.isFocused }
                                .clickable { onDismiss() }
                                .focusable()
                                .testTag("tv_devices_close_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Devices Menu",
                                    tint = if (closeFocused) Color(0xFF10131C) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0x26FFFFFF))
                    Spacer(modifier = Modifier.height(14.dp))

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            // 1. Wi-Fi Option (opens system Wi-Fi settings)
                            item {
                                TvDevicesMenuButtonCard(
                                    title = "Wi-Fi",
                                    subtitle = "Open Android system Wi-Fi & network settings",
                                    icon = Icons.Default.Wifi,
                                    accentColor = CyanPrimary,
                                    badgeText = tvLocalIp,
                                    testTag = "tv_devices_wifi_button",
                                    modifier = Modifier.focusRequester(firstItemFocusRequester),
                                    onClick = {
                                        launchSystemWifiSettings(context)
                                    }
                                )
                            }

                            // 2. Bluetooth Option underneath (opens Android TV Remotes & Accessories / Bluetooth settings)
                            item {
                                TvDevicesMenuButtonCard(
                                    title = "Bluetooth & Accessories",
                                    subtitle = "Open Android TV Remotes & Accessories / Bluetooth settings",
                                    icon = Icons.Default.Bluetooth,
                                    accentColor = VioletSecondary,
                                    badgeText = "Remotes",
                                    testTag = "tv_devices_bluetooth_button",
                                    onClick = {
                                        launchSystemBluetoothSettings(context)
                                    }
                                )
                            }

                            // 3. RNA Controller Button underneath
                            item {
                                TvDevicesMenuButtonCard(
                                    title = "RNA Controller",
                                    subtitle = "Use phones on Wi-Fi or Hotspot as TV Remote & Xbox Gamepads",
                                    icon = Icons.Default.Gamepad,
                                    accentColor = EmeraldSuccess,
                                    badgeText = "${connectedControllers.size}/${RnaControllerNetworkBridge.MAX_CONTROLLERS} Paired",
                                    isSelected = showRnaControllerPanel,
                                    testTag = "tv_devices_rna_controller_button",
                                    onClick = {
                                        showRnaControllerPanel = !showRnaControllerPanel
                                        if (showRnaControllerPanel) {
                                            RnaControllerNetworkBridge.refreshTvIpAddresses()
                                        }
                                    }
                                )
                            }

                            // RNA Controller Expanded Panel (4-Digit Pairing Codes + Connected Controllers + 2nd Controller Slot)
                            item {
                                AnimatedVisibility(
                                    visible = showRnaControllerPanel,
                                    enter = expandVertically(tween(220)) + fadeIn(tween(180)),
                                    exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(22.dp),
                                        color = Color(0xFF181C2C),
                                        border = BorderStroke(1.5.dp, EmeraldSuccess.copy(alpha = 0.55f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("tv_rna_controller_panel")
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(18.dp),
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "RNA CONTROLLER PAIRING",
                                                        color = EmeraldSuccess,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        letterSpacing = 0.8.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "Open the RNA app on your phone, tap Remote above Create, and enter this 4-digit code:",
                                                        color = Color(0xFFC5C7D6),
                                                        fontSize = 11.5.sp,
                                                        lineHeight = 15.sp
                                                    )
                                                }
                                            }

                                            // Primary 4-Digit Pairing Code Box (Controller 1)
                                            val slot1Controller = connectedControllers.find { it.slotIndex == 1 }
                                            PairingCodeDisplayCard(
                                                slotTitle = "CONTROLLER 1 (PLAYER 1)",
                                                pinCode = pinCode1,
                                                connectedDevice = slot1Controller,
                                                accentColor = CyanPrimary,
                                                onRegeneratePin = {
                                                    RnaControllerNetworkBridge.regeneratePinForSlot(context, 1)
                                                },
                                                onDisconnect = {
                                                    slot1Controller?.let {
                                                        RnaControllerNetworkBridge.disconnectController(it.deviceId)
                                                    }
                                                },
                                                testTag = "tv_rna_controller_pin_slot_1"
                                            )

                                            // Connect 2nd Controller Button / Slot 2 Code (Max 2 Controllers)
                                            val slot2Controller = connectedControllers.find { it.slotIndex == 2 }
                                            if (!isSecondSlotPinVisible && slot2Controller == null) {
                                                var addSecondFocused by remember { mutableStateOf(false) }
                                                Button(
                                                    onClick = {
                                                        RnaControllerNetworkBridge.enableSecondControllerSlot(context)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (addSecondFocused) Color.White else Color(0xFF252A3F),
                                                        contentColor = if (addSecondFocused) Color(0xFF10131C) else VioletSecondary
                                                    ),
                                                    shape = RoundedCornerShape(16.dp),
                                                    border = BorderStroke(
                                                        1.5.dp,
                                                        if (addSecondFocused) Color.White else VioletSecondary
                                                    ),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .onFocusChanged { addSecondFocused = it.isFocused }
                                                        .testTag("tv_rna_controller_add_second_btn")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Connect 2nd Controller (Player 2 Code)",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 12.5.sp
                                                    )
                                                }
                                            } else {
                                                PairingCodeDisplayCard(
                                                    slotTitle = "CONTROLLER 2 (PLAYER 2 • MAX 2)",
                                                    pinCode = pinCode2,
                                                    connectedDevice = slot2Controller,
                                                    accentColor = VioletSecondary,
                                                    onRegeneratePin = {
                                                        RnaControllerNetworkBridge.regeneratePinForSlot(context, 2)
                                                    },
                                                    onDisconnect = {
                                                        slot2Controller?.let {
                                                            RnaControllerNetworkBridge.disconnectController(it.deviceId)
                                                        }
                                                    },
                                                    testTag = "tv_rna_controller_pin_slot_2"
                                                )
                                            }

                                            // System-Wide External App Control Status / Enable Button
                                            val isSystemControllerServiceActive = com.example.RnaHomeInterceptorService.isAccessibilityServiceEnabled(context)
                                            if (!isSystemControllerServiceActive) {
                                                var enableAccFocused by remember { mutableStateOf(false) }
                                                Button(
                                                    onClick = {
                                                        runCatching {
                                                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            }
                                                            context.startActivity(intent)
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (enableAccFocused) Color.White else AmberAccent,
                                                        contentColor = Color(0xFF10131C)
                                                    ),
                                                    shape = RoundedCornerShape(14.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .onFocusChanged { enableAccFocused = it.isFocused }
                                                        .testTag("tv_rna_controller_enable_external_apps_btn")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.SettingsRemote,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Enable System-Wide Control for All External Apps",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 11.5.sp
                                                    )
                                                }
                                            }

                                            // Network & Auto-Reconnect Info Pill
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = Color(0xFF121522),
                                                border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "TV Network IP: $tvLocalIp",
                                                            color = CyanPrimary,
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                            text = "UDP Zero-Lag Active",
                                                            color = EmeraldSuccess,
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Text(
                                                        text = "Once paired, your phone automatically reconnects on the same Wi-Fi or Hotspot without needing the 4-digit code again.",
                                                        color = Color(0xFF9E9FAF),
                                                        fontSize = 10.5.sp,
                                                        lineHeight = 14.sp
                                                    )
                                                    if (lastActionStatus.isNotBlank()) {
                                                        Text(
                                                            text = "Status: $lastActionStatus",
                                                            color = AmberAccent,
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
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

                            // Connected Controllers List
                            if (connectedControllers.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "CONNECTED PHONE CONTROLLERS (${connectedControllers.size}/2)",
                                        color = CyanPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.8.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                items(
                                    items = connectedControllers,
                                    key = { it.deviceId }
                                ) { controller ->
                                    ConnectedControllerRowCard(
                                        controller = controller,
                                        onDisconnect = {
                                            RnaControllerNetworkBridge.disconnectController(controller.deviceId)
                                        }
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
private fun TvDevicesMenuButtonCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    badgeText: String,
    isSelected: Boolean = false,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused && !TvPerformanceMode.isEnabled) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "DevicesMenuCardScale"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = when {
            isFocused -> Color(0xFF282E45)
            isSelected -> Color(0xFF1E2336)
            else -> Color(0xFF181B28)
        },
        border = BorderStroke(
            width = if (isFocused) 2.5.dp else 1.dp,
            color = when {
                isFocused -> Color.White
                isSelected -> accentColor.copy(alpha = 0.7f)
                else -> Color(0x2EFFFFFF)
            }
        ),
        shadowElevation = if (isFocused) 12.dp else 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .focusable()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.65f)),
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

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = Color(0xFF9E9FAF),
                        fontSize = 11.5.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
            ) {
                Text(
                    text = badgeText,
                    color = accentColor,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PairingCodeDisplayCard(
    slotTitle: String,
    pinCode: String,
    connectedDevice: ConnectedPhoneController?,
    accentColor: Color,
    onRegeneratePin: () -> Unit,
    onDisconnect: () -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF111420),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = slotTitle,
                    color = accentColor,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.7.sp
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (connectedDevice != null) EmeraldSuccess.copy(alpha = 0.2f) else AmberAccent.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = if (connectedDevice != null) "CONNECTED: ${connectedDevice.controllerName}" else "WAITING FOR PHONE",
                        color = if (connectedDevice != null) EmeraldSuccess else AmberAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // 4-Digit Code Display + Refresh button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val digits = pinCode.padEnd(4, '0').take(4).toList()
                    digits.forEach { digitChar ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1D2236),
                            border = BorderStroke(1.5.dp, accentColor),
                            modifier = Modifier.size(width = 44.dp, height = 50.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = digitChar.toString(),
                                    color = Color.White,
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    var refreshFocused by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = onRegeneratePin,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (refreshFocused) Color.White else Color(0x44FFFFFF)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.onFocusChanged { refreshFocused = it.isFocused }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "New Code",
                            tint = CyanPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Code", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (connectedDevice != null) {
                        OutlinedButton(
                            onClick = onDisconnect,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CoralAccent.copy(alpha = 0.7f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LinkOff,
                                contentDescription = "Disconnect",
                                tint = CoralAccent,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            if (connectedDevice != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (connectedDevice.mode == RnaPhoneControllerMode.XBOX_GAMEPAD) Icons.Default.Gamepad else Icons.Default.SettingsRemote,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "${connectedDevice.controllerName} • ${if (connectedDevice.mode == RnaPhoneControllerMode.XBOX_GAMEPAD) "Xbox Wireless Controller #${connectedDevice.slotIndex}" else "TV Remote Mode"}",
                            color = Color(0xFFE4E1E9),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = connectedDevice.ipAddress,
                        color = Color(0xFF9E9FAF),
                        fontSize = 10.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectedControllerRowCard(
    controller: ConnectedPhoneController,
    onDisconnect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1A1E2E),
        border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = EmeraldSuccess.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, EmeraldSuccess),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (controller.mode == RnaPhoneControllerMode.XBOX_GAMEPAD) Icons.Default.Gamepad else Icons.Default.Smartphone,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = controller.controllerName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (controller.mode == RnaPhoneControllerMode.XBOX_GAMEPAD)
                            "Spoofing Xbox Controller #${controller.slotIndex} (Device ID ${controller.virtualDeviceId})"
                        else
                            "Slot #${controller.slotIndex} • TV Remote & App Launcher",
                        color = CyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            OutlinedButton(
                onClick = onDisconnect,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CoralAccent.copy(alpha = 0.6f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text("Disconnect", color = CoralAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun launchSystemWifiSettings(context: Context) {
    val candidates = listOf(
        Intent(Settings.ACTION_WIFI_SETTINGS),
        Intent(Settings.ACTION_WIRELESS_SETTINGS),
        Intent(Settings.ACTION_SETTINGS)
    )
    for (intent in candidates) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        } catch (_: Exception) {
        }
    }
    Toast.makeText(context, "Opening System Wi-Fi Settings", Toast.LENGTH_SHORT).show()
}

private fun launchSystemBluetoothSettings(context: Context) {
    val candidates = listOf(
        Intent("com.google.android.tv.settings.ACCESSORIES_SETTINGS"),
        Intent("com.android.tv.settings.ACCESSORIES_SETTINGS"),
        Intent().setClassName("com.android.tv.settings", "com.android.tv.settings.accessories.AddAccessoryActivity"),
        Intent().setClassName("com.android.tv.settings", "com.android.tv.settings.MainSettings"),
        Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
        Intent(Settings.ACTION_SETTINGS)
    )
    for (intent in candidates) {
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        } catch (_: Exception) {
        }
    }
    Toast.makeText(context, "Opening Remotes & Accessories Settings", Toast.LENGTH_SHORT).show()
}
