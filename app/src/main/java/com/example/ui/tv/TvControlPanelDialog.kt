package com.example.ui.tv

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.RnaHomeInterceptorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

private val ControlCyan = Color(0xFF38BDF8)
private val ControlViolet = Color(0xFFC084FC)
private val ControlAmber = Color(0xFFFBBF24)
private val ControlEmerald = Color(0xFF34D399)
private val ControlIdleMinuteOptions = listOf(0, 1, 3, 5, 10, 15, 20, 25, 30)

private data class ControlPanelSidebarTab(
    val index: Int,
    val title: String,
    val icon: ImageVector,
    val accent: Color
)

private data class ExpressiveFunctionSpec(
    val title: String,
    val badge: String,
    val icon: ImageVector,
    val containerColor: Color,
    val iconContainerColor: Color,
    val accentColor: Color,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
internal fun TvControlPanelDialogContent(
    context: Context,
    showRecentAppsRow: Boolean,
    onToggleRecentAppsRow: (Boolean) -> Unit,
    singleRowHomeApps: Boolean,
    onToggleSingleRowHomeApps: (Boolean) -> Unit,
    onClearRecentApps: () -> Unit,
    performanceModeEnabled: Boolean,
    onTogglePerformanceMode: (Boolean) -> Unit,
    browserFastPerformanceMode: Boolean = true,
    onToggleBrowserFastPerformanceMode: (Boolean) -> Unit = {},
    splashScreenEnabled: Boolean,
    onToggleSplashScreen: (Boolean) -> Unit,
    selectedAppIconShape: TvAppIconShape,
    onSelectAppIconShape: (TvAppIconShape) -> Unit,
    liveWallpaperPreviewEnabled: Boolean = false,
    onToggleLiveWallpaperPreview: (Boolean) -> Unit = {},
    liveWallpaperResolution: LiveWallpaperResolution,
    onSelectLiveWallpaperResolution: (LiveWallpaperResolution) -> Unit,
    ambientIdleMinutes: Int,
    onSelectAmbientIdleMinutes: (Int) -> Unit,
    askWhoIsWatchingOnBoot: Boolean = false,
    onToggleAskWhoIsWatchingOnBoot: (Boolean) -> Unit = {},
    usbBootAutoBackEnabled: Boolean,
    onToggleUsbBootAutoBack: (Boolean) -> Unit,
    replaceGoogleAssistantEnabled: Boolean,
    onToggleReplaceGoogleAssistant: (Boolean) -> Unit,
    aiTtsEnabled: Boolean,
    onToggleAiTts: (Boolean) -> Unit,
    rnaAiAppEnabled: Boolean,
    onToggleRnaAiAppEnabled: (Boolean) -> Unit,
    onOpenRnaAiSettings: () -> Unit,
    onOpenKeyRemapper: () -> Unit,
    onOpenKeyLinksEditor: () -> Unit,
    onOpenVoiceAssistant: () -> Unit,
    onOpenUpdatesDrawer: () -> Unit,
    onOpenWallpapers: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenRnaStudioSettings: () -> Unit,
    onOpenAndroidSettings: () -> Unit,
    sleepTimerRemainingSeconds: Int,
    onSetSleepTimerMinutes: (Int) -> Unit,
    nightWarmthIntensity: Float,
    onChangeNightWarmth: (Float) -> Unit,
    onLaunchAmbientMode: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // 0: Home & Layout, 1: Icon Shapes, 2: Display & Ambient, 3: System & AI, 4: Functions
    var selectedTab by remember { mutableIntStateOf(0) }
    var isTestingNetwork by remember { mutableStateOf(false) }
    var downloadMbps by remember { mutableStateOf<Double?>(null) }
    var latencyMs by remember { mutableStateOf<Long?>(null) }
    var localIpAddress by remember { mutableStateOf("Detecting...") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            localIpAddress = resolveLocalLanIp()
        }
    }

    fun runRealSpeedTest() {
        if (isTestingNetwork) return
        isTestingNetwork = true
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val pingStart = System.nanoTime()
                    val pingConn = (URL("https://www.google.com/generate_204").openConnection() as HttpURLConnection).apply {
                        connectTimeout = 4000
                        readTimeout = 4000
                    }
                    pingConn.responseCode
                    pingConn.disconnect()
                    val pingElapsedMs = ((System.nanoTime() - pingStart) / 1_000_000L).coerceAtLeast(1L)
                    latencyMs = pingElapsedMs

                    val dlUrl = URL("https://images.unsplash.com/photo-1534447677768-be436bb09401?w=2400&q=90")
                    val dlStart = System.nanoTime()
                    val dlConn = (dlUrl.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 6000
                        readTimeout = 6000
                        useCaches = false
                    }
                    var totalBytes = 0L
                    dlConn.inputStream.use { input ->
                        val buffer = ByteArray(16 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            totalBytes += read
                        }
                    }
                    dlConn.disconnect()
                    val elapsedSec = ((System.nanoTime() - dlStart) / 1_000_000_000.0).coerceAtLeast(0.05)
                    val bits = totalBytes * 8.0
                    downloadMbps = (bits / elapsedSec) / 1_000_000.0
                } catch (_: Exception) {
                    latencyMs = -1L
                } finally {
                    isTestingNetwork = false
                }
            }
        }
    }

    val sidebarTabs = remember {
        listOf(
            ControlPanelSidebarTab(0, "Home & Layout", Icons.Default.ViewAgenda, ControlCyan),
            ControlPanelSidebarTab(1, "Icon Shapes", Icons.Default.Widgets, ControlViolet),
            ControlPanelSidebarTab(2, "Display & Ambient", Icons.Default.Wallpaper, ControlEmerald),
            ControlPanelSidebarTab(3, "System & AI", Icons.Default.AutoAwesome, ControlAmber),
            ControlPanelSidebarTab(4, "Functions", Icons.Default.SportsEsports, ControlCyan)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141620)),
            border = BorderStroke(1.5.dp, Color(0x38FFFFFF)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .testTag("tv_rna_settings_dialog")
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // LEFT SIDEBAR (Same Two-Pane Architecture as Edit Profile Dialog)
                Column(
                    modifier = Modifier
                        .width(276.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF1A1C28))
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Sidebar Header with small 'X' Close button at the top near "Control Panel"
                        var closeFocused by remember { mutableStateOf(false) }
                        val closeScale by animateFloatAsState(
                            targetValue = if (closeFocused) 1.12f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            label = "sidebar_top_close_scale"
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = ControlCyan.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, ControlCyan),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = ControlCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Control Panel",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = if (closeFocused) Color(0xFFEF4444) else Color(0xFF252838),
                                border = BorderStroke(
                                    width = if (closeFocused) 2.dp else 1.dp,
                                    color = if (closeFocused) Color.White else Color(0x38FFFFFF)
                                ),
                                modifier = Modifier
                                    .size(32.dp)
                                    .scale(closeScale)
                                    .onFocusChanged { closeFocused = it.isFocused }
                                    .focusable()
                                    .clickable { onDismiss() }
                                    .testTag("control_panel_sidebar_close_x_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Control Panel",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                        }
                            }
                        }

                        // Sidebar Category Tabs
                        sidebarTabs.forEach { tab ->
                            var tabFocused by remember { mutableStateOf(false) }
                            val isSelected = selectedTab == tab.index
                            val scale by animateFloatAsState(
                                targetValue = if (tabFocused) 1.03f else 1f,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "sidebar_tab_scale"
                            )
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = when {
                                    tabFocused -> Color.White
                                    isSelected -> tab.accent.copy(alpha = 0.20f)
                                    else -> Color.Transparent
                                },
                                border = BorderStroke(
                                    width = if (tabFocused) 2.dp else if (isSelected) 1.5.dp else 0.dp,
                                    color = when {
                                        tabFocused -> Color.White
                                        isSelected -> tab.accent
                                        else -> Color.Transparent
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(scale)
                                    .onFocusChanged { tabFocused = it.isFocused }
                                    .clickable { selectedTab = tab.index }
                                    .focusable()
                                    .testTag("control_panel_tab_${tab.index}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = null,
                                        tint = when {
                                            tabFocused -> Color(0xFF10131C)
                                            isSelected -> tab.accent
                                            else -> Color(0xFF9E9FAF)
                                        },
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = tab.title,
                                        color = when {
                                            tabFocused -> Color(0xFF10131C)
                                            isSelected -> Color.White
                                            else -> Color(0xFFC7C8D8)
                                        },
                                        fontWeight = if (isSelected || tabFocused) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // RIGHT CONTENT PANE
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 28.dp, vertical = 24.dp)
                ) {
                    val activeTabObj = sidebarTabs.getOrElse(selectedTab) { sidebarTabs.first() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = activeTabObj.icon,
                                contentDescription = null,
                                tint = activeTabObj.accent,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = activeTabObj.title,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        // Quick Top Shortcuts for compatibility & rapid access
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF222534),
                                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                                modifier = Modifier
                                    .clickable { selectedTab = 4 }
                                    .testTag("tv_control_panel_functions_quick_chip")
                            ) {
                                Text(
                                    text = if (selectedTab == 4) "All Functions" else "Open Functions Tab",
                                    color = ControlCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        when (selectedTab) {
                            // =========================================================================
                            // TAB 0: HOME & LAYOUT
                            // =========================================================================
                            0 -> {
                                item {
                                    val homePrefs = remember {
                                        context.getSharedPreferences(
                                            com.example.RnaHomeInterceptorService.PREFS_NAME,
                                            Context.MODE_PRIVATE
                                        )
                                    }
                                    var defaultLauncherEnabled by remember {
                                        mutableStateOf(
                                            homePrefs.getBoolean(
                                                com.example.RnaHomeInterceptorService.KEY_INTERCEPT_HOME,
                                                true
                                            )
                                        )
                                    }
                                    ControlToggleRow(
                                        title = "Set RNA. TV as Default Launcher",
                                        subtitle = "Intercepts Home & stock Android TV launcher automatically with zero delay",
                                        icon = Icons.Default.Tv,
                                        iconTint = if (defaultLauncherEnabled) ControlEmerald else Color(0xFF9E9FAF),
                                        checked = defaultLauncherEnabled,
                                        onToggle = {
                                            val next = !defaultLauncherEnabled
                                            defaultLauncherEnabled = next
                                            homePrefs.edit()
                                                .putBoolean(
                                                    com.example.RnaHomeInterceptorService.KEY_INTERCEPT_HOME,
                                                    next
                                                )
                                                .apply()
                                        },
                                        rowTestTag = "tv_settings_default_launcher_row",
                                        switchTestTag = "tv_settings_default_launcher_switch"
                                    )
                                }
                                item {
                                    ControlToggleRow(
                                        title = "Single Row Apps on Home Tab",
                                        icon = Icons.Default.ViewAgenda,
                                        iconTint = ControlCyan,
                                        checked = singleRowHomeApps,
                                        onToggle = { onToggleSingleRowHomeApps(!singleRowHomeApps) },
                                        switchTestTag = "tv_settings_single_row_apps_switch"
                                    )
                                }
                                item {
                                    ControlToggleRow(
                                        title = "Show 'Recent Apps' Row on Launcher",
                                        icon = Icons.Default.History,
                                        iconTint = ControlCyan,
                                        checked = showRecentAppsRow,
                                        onToggle = { onToggleRecentAppsRow(!showRecentAppsRow) },
                                        switchTestTag = "tv_settings_recent_apps_switch"
                                    )
                                }
                                item {
                                    ControlToggleRow(
                                        title = "Startup Wallpaper Splash Screen",
                                        icon = Icons.Default.Wallpaper,
                                        iconTint = if (splashScreenEnabled) ControlCyan else Color(0xFF9E9FAF),
                                        checked = splashScreenEnabled,
                                        onToggle = { onToggleSplashScreen(!splashScreenEnabled) },
                                        rowTestTag = "tv_settings_splash_screen_row",
                                        switchTestTag = "tv_settings_splash_screen_switch"
                                    )
                                }
                                item {
                                    ControlToggleRow(
                                        title = "Ask 'Who is Watching?' on Startup",
                                        icon = Icons.Default.Tv,
                                        iconTint = if (askWhoIsWatchingOnBoot) ControlCyan else Color(0xFF9E9FAF),
                                        checked = askWhoIsWatchingOnBoot,
                                        onToggle = { onToggleAskWhoIsWatchingOnBoot(!askWhoIsWatchingOnBoot) },
                                        rowTestTag = "tv_settings_who_is_watching_row",
                                        switchTestTag = "tv_settings_who_is_watching_switch"
                                    )
                                }
                            }

                            // =========================================================================
                            // TAB 1: ICON SHAPES (TV-SPECIFICALLY DESIGNED 19-SHAPE STUDIO)
                            // =========================================================================
                            1 -> {
                                // Live 3-Tile TV Preview Header Banner
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(22.dp),
                                        color = Color(0xFF1D2030),
                                        border = BorderStroke(1.dp, ControlViolet.copy(alpha = 0.45f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 22.dp, vertical = 18.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "ACTIVE TV ICON SHAPE",
                                                    color = ControlViolet,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 0.9.sp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = selectedAppIconShape.label,
                                                    color = Color.White,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }

                                            // 3 Sample Live TV Tiles Previewing the Current Shape
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val sampleTiles = listOf(
                                                    Triple("RNA. TV", Color(0xFF0284C7), Color(0xFF38BDF8)),
                                                    Triple("Cinema", Color(0xFF7C3AED), Color(0xFFC084FC)),
                                                    Triple("Music", Color(0xFF059669), Color(0xFF34D399))
                                                )
                                                sampleTiles.forEach { (name, c1, c2) ->
                                                    val tileShape = resolveComposeShapeForAppIcon(selectedAppIconShape)
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(
                                                                    width = if (selectedAppIconShape.isWideBanner) 86.dp else 52.dp,
                                                                    height = 52.dp
                                                                )
                                                                .clip(tileShape)
                                                                .background(Brush.linearGradient(listOf(c1, c2))),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                text = name.take(3).uppercase(),
                                                                color = Color.White,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 12.sp
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                            text = name,
                                                            color = Color(0xFFC7C8D8),
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 3-Column TV Grid for All 19 Shapes
                                val chunkedShapes = TvAppIconShape.entries.chunked(3)
                                itemsIndexed(chunkedShapes) { rowIdx, shapeRow ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .then(
                                                if (rowIdx == 0) Modifier.testTag("tv_settings_app_icon_shapes_row")
                                                else Modifier
                                            ),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        shapeRow.forEach { shapeOption ->
                                            var cardFocused by remember { mutableStateOf(false) }
                                            val isSelected = selectedAppIconShape == shapeOption
                                            val scale by animateFloatAsState(
                                                targetValue = if (cardFocused) 1.04f else 1f,
                                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                label = "shape_card_scale"
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = when {
                                                    cardFocused -> Color.White
                                                    isSelected -> Color(0xFF252B42)
                                                    else -> Color(0xFF1B1E2B)
                                                },
                                                border = BorderStroke(
                                                    width = if (cardFocused) 2.5.dp else if (isSelected) 2.dp else 1.dp,
                                                    color = when {
                                                        cardFocused -> Color.White
                                                        isSelected -> ControlCyan
                                                        else -> Color(0x28FFFFFF)
                                                    }
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .scale(scale)
                                                    .onFocusChanged { cardFocused = it.isFocused }
                                                    .clickable { onSelectAppIconShape(shapeOption) }
                                                    .focusable()
                                                    .testTag("tv_shape_chip_${shapeOption.id}")
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
                                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(
                                                                    width = if (shapeOption.isWideBanner) 58.dp else 40.dp,
                                                                    height = 40.dp
                                                                )
                                                                .clip(resolveComposeShapeForAppIcon(shapeOption))
                                                                .background(
                                                                    when {
                                                                        cardFocused -> Color(0xFF10131C)
                                                                        isSelected -> ControlCyan
                                                                        else -> ControlViolet.copy(alpha = 0.75f)
                                                                    }
                                                                )
                                                        )
                                                        Text(
                                                            text = shapeOption.label,
                                                            color = if (cardFocused) Color(0xFF10131C) else Color.White,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 14.sp,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }

                                                    if (isSelected) {
                                                        Surface(
                                                            shape = CircleShape,
                                                            color = if (cardFocused) Color(0xFF10131C) else ControlCyan,
                                                            modifier = Modifier.size(22.dp)
                                                        ) {
                                                            Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Check,
                                                                    contentDescription = "Selected",
                                                                    tint = if (cardFocused) Color.White else Color(0xFF10131C),
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        repeat(3 - shapeRow.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }

                            // =========================================================================
                            // TAB 2: DISPLAY & AMBIENT (INCLUDES PLAY LIVE WALLPAPER PREVIEWS OFF BY DEFAULT)
                            // =========================================================================
                            2 -> {
                                // 1. Play Live Wallpaper Previews Toggle (OFF by default)
                                item {
                                    ControlToggleRow(
                                        title = "Play Live Wallpaper Previews in Gallery",
                                        icon = Icons.Default.PlayArrow,
                                        iconTint = if (liveWallpaperPreviewEnabled) ControlEmerald else Color(0xFF9E9FAF),
                                        checked = liveWallpaperPreviewEnabled,
                                        onToggle = { onToggleLiveWallpaperPreview(!liveWallpaperPreviewEnabled) },
                                        rowTestTag = "tv_settings_live_wallpaper_preview_row",
                                        switchTestTag = "tv_settings_live_wallpaper_preview_toggle"
                                    )
                                }

                                // 2. Live Wallpaper Playback Definition
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = Color(0xFF1D2030),
                                        border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp)) {
                                            Text(
                                                text = "Live Wallpaper Playback Definition (${liveWallpaperResolution.label})",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                LiveWallpaperResolution.entries.forEach { res ->
                                                    val isSelected = liveWallpaperResolution == res
                                                    Button(
                                                        onClick = { onSelectLiveWallpaperResolution(res) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (isSelected) ControlCyan else Color(0xFF141620),
                                                            contentColor = if (isSelected) Color(0xFF10131C) else Color(0xFFE4E1E9)
                                                        ),
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(res.label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 3. Auto 4K Ambient Mode Idle Timer
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(18.dp),
                                        color = Color(0xFF1D2030),
                                        border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(18.dp)) {
                                            Text(
                                                text = "Auto 4K Ambient Mode Timer (${if (ambientIdleMinutes <= 0) "Off" else "${ambientIdleMinutes} min"})",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                itemsIndexed(ControlIdleMinuteOptions) { _, mins ->
                                                    val isSelected = ambientIdleMinutes == mins
                                                    val label = if (mins == 0) "Off" else "${mins}m"
                                                    Button(
                                                        onClick = { onSelectAmbientIdleMinutes(mins) },
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (isSelected) ControlCyan else Color(0xFF141620),
                                                            contentColor = if (isSelected) Color(0xFF10131C) else Color(0xFFE4E1E9)
                                                        ),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 4. Night Eye-Care Warmth Filter & Sleep Timer
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = Color(0xFF1D2030),
                                            border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(18.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(imageVector = Icons.Default.NightlightRound, contentDescription = null, tint = ControlAmber)
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "Night Eye-Care Filter",
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp
                                                        )
                                                    }
                                                    Text(
                                                        text = "${(nightWarmthIntensity * 100).roundToInt()}%",
                                                        color = ControlAmber,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    listOf(0f to "Off", 0.25f to "25%", 0.45f to "45%", 0.65f to "65%").forEach { (preset, label) ->
                                                        val active = kotlin.math.abs(nightWarmthIntensity - preset) < 0.08f
                                                        OutlinedButton(
                                                            onClick = { onChangeNightWarmth(preset) },
                                                            colors = ButtonDefaults.outlinedButtonColors(
                                                                containerColor = if (active) ControlAmber else Color(0xFF141620),
                                                                contentColor = if (active) Color(0xFF10131C) else Color.White
                                                            ),
                                                            shape = RoundedCornerShape(12.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = Color(0xFF1D2030),
                                            border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(18.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(imageVector = Icons.Default.Bedtime, contentDescription = null, tint = ControlViolet)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = if (sleepTimerRemainingSeconds > 0)
                                                            "Sleep Timer (${sleepTimerRemainingSeconds / 60}m ${sleepTimerRemainingSeconds % 60}s)"
                                                        else "Sleep Timer",
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    listOf(0 to "Off", 15 to "15m", 30 to "30m", 60 to "60m", 90 to "90m").forEach { (mins, label) ->
                                                        val isSelected = (mins == 0 && sleepTimerRemainingSeconds == 0) ||
                                                            (mins > 0 && (sleepTimerRemainingSeconds in ((mins - 1) * 60 + 1)..(mins * 60)))
                                                        Button(
                                                            onClick = { onSetSleepTimerMinutes(mins) },
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = if (isSelected) ControlCyan else Color(0xFF141620),
                                                                contentColor = if (isSelected) Color(0xFF10131C) else Color(0xFFE4E1E9)
                                                            ),
                                                            shape = RoundedCornerShape(12.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // =========================================================================
                            // TAB 3: SYSTEM & AI (System settings on top, AI settings underneath)
                            // =========================================================================
                            3 -> {
                                item {
                                    ControlToggleRow(
                                        title = "Performance Mode",
                                        icon = Icons.Default.Speed,
                                        iconTint = ControlCyan,
                                        checked = performanceModeEnabled,
                                        onToggle = { onTogglePerformanceMode(!performanceModeEnabled) },
                                        rowTestTag = "tv_settings_performance_mode_row",
                                        switchTestTag = "tv_settings_performance_mode_switch"
                                    )
                                }
                                item {
                                    ControlToggleRow(
                                        title = "Performance mode for browser",
                                        icon = Icons.Default.Speed,
                                        iconTint = ControlEmerald,
                                        checked = browserFastPerformanceMode,
                                        onToggle = { onToggleBrowserFastPerformanceMode(!browserFastPerformanceMode) },
                                        rowTestTag = "tv_settings_browser_performance_mode_row",
                                        switchTestTag = "tv_settings_browser_performance_mode_switch"
                                    )
                                }
                                item {
                                    var imeControllerBridgeEnabled by remember {
                                        mutableStateOf(RnaKeyLinksRegistry.isControllerOverKeyboardImeEnabled(context))
                                    }
                                    ControlToggleRow(
                                        title = "Use RNA Keyboard IME for Remote & Xbox Buttons (Optional)",
                                        icon = Icons.Default.Keyboard,
                                        iconTint = if (imeControllerBridgeEnabled) ControlCyan else Color(0xFF9E9FAF),
                                        checked = imeControllerBridgeEnabled,
                                        onToggle = {
                                            val next = !imeControllerBridgeEnabled
                                            imeControllerBridgeEnabled = next
                                            RnaKeyLinksRegistry.setControllerOverKeyboardImeEnabled(context, next)
                                        },
                                        rowTestTag = "tv_settings_ime_controller_bridge_row",
                                        switchTestTag = "tv_settings_ime_controller_bridge_switch"
                                    )
                                }
                                item {
                                    ControlToggleRow(
                                        title = "Auto-Back on USB Boot Detection (< 1 Minute of Boot)",
                                        icon = Icons.Default.Usb,
                                        iconTint = ControlAmber,
                                        checked = usbBootAutoBackEnabled,
                                        onToggle = { onToggleUsbBootAutoBack(!usbBootAutoBackEnabled) },
                                        rowTestTag = "tv_settings_usb_boot_auto_back_row",
                                        switchTestTag = "tv_settings_usb_boot_auto_back_switch"
                                    )
                                }
                                item {
                                    ControlToggleRow(
                                        title = "RNA. AI App",
                                        icon = Icons.Default.AutoAwesome,
                                        iconTint = ControlCyan,
                                        checked = rnaAiAppEnabled,
                                        onToggle = { onToggleRnaAiAppEnabled(!rnaAiAppEnabled) },
                                        rowTestTag = "tv_settings_enable_rnaai_app_row",
                                        switchTestTag = "tv_settings_enable_rnaai_app_switch"
                                    )
                                }
                                item {
                                    ControlToggleRow(
                                        title = "AI Voice Spoken Replies (Text-to-Speech)",
                                        icon = Icons.Default.RecordVoiceOver,
                                        iconTint = ControlEmerald,
                                        checked = aiTtsEnabled,
                                        onToggle = { onToggleAiTts(!aiTtsEnabled) },
                                        rowTestTag = "tv_settings_ai_tts_row",
                                        switchTestTag = "tv_settings_ai_tts_switch"
                                    )
                                }
                                item {
                                    ControlToggleRow(
                                        title = "Use EdVanced AI instead of Google Assistant",
                                        subtitle = "Voice Key & Assistant interception (Requires RNA. TV Accessibility Service)",
                                        icon = Icons.Default.AutoAwesome,
                                        iconTint = ControlViolet,
                                        checked = replaceGoogleAssistantEnabled,
                                        onToggle = {
                                            val nextState = !replaceGoogleAssistantEnabled
                                            onToggleReplaceGoogleAssistant(nextState)
                                            if (nextState && !RnaHomeInterceptorService.isAccessibilityServiceEnabled(context)) {
                                                Toast.makeText(
                                                    context,
                                                    "Enable RNA. TV Accessibility Service so Google Assistant can be replaced over any app",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        },
                                        rowTestTag = "tv_settings_replace_google_assistant_row",
                                        switchTestTag = "tv_settings_replace_google_assistant_switch"
                                    )
                                }
                                item {
                                    ControlPanelAiRouterSection(context = context)
                                }
                            }

                            // =========================================================================
                            // TAB 4: FUNCTIONS (MATERIAL 3 EXPRESSIVE TONAL SPOT ACTION CARDS)
                            // =========================================================================
                            4 -> {
                                val functionCards = buildList {
                                    add(
                                        ExpressiveFunctionSpec(
                                            title = if (com.example.BuildConfig.IS_FDROID) "System Updates" else "System Updates & More Apps",
                                            badge = if (com.example.BuildConfig.IS_FDROID) "F-DROID" else "UPDATES",
                                            icon = Icons.Default.SystemUpdateAlt,
                                            containerColor = Color(0xFF1A283B),
                                            iconContainerColor = Color(0xFF0284C7),
                                            accentColor = ControlCyan,
                                            testTag = "tv_settings_open_updates_drawer_btn",
                                            onClick = onOpenUpdatesDrawer
                                        )
                                    )
                                    add(
                                        ExpressiveFunctionSpec(
                                            title = "Remote Key Remapper",
                                            badge = "REMAPPER",
                                            icon = Icons.Default.Gamepad,
                                            containerColor = Color(0xFF192B32),
                                            iconContainerColor = Color(0xFF0891B2),
                                            accentColor = Color(0xFF22D3EE),
                                            testTag = "tv_open_key_remapper_btn",
                                            onClick = onOpenKeyRemapper
                                        )
                                    )
                                    add(
                                        ExpressiveFunctionSpec(
                                            title = "Key Link Editor",
                                            badge = "CONTROLLER",
                                            icon = Icons.Default.SportsEsports,
                                            containerColor = Color(0xFF261E3A),
                                            iconContainerColor = Color(0xFF7C3AED),
                                            accentColor = ControlViolet,
                                            testTag = "tv_open_key_links_editor_btn",
                                            onClick = onOpenKeyLinksEditor
                                        )
                                    )
                                    add(
                                        ExpressiveFunctionSpec(
                                            title = "RNA TV Keyboard",
                                            badge = "SYSTEM IME",
                                            icon = Icons.Default.Keyboard,
                                            containerColor = Color(0xFF1E293B),
                                            iconContainerColor = Color(0xFF2563EB),
                                            accentColor = Color(0xFF60A5FA),
                                            testTag = "tv_open_rna_keyboard_ime_settings_btn",
                                            onClick = { com.example.RnaTvKeyboardBridge.openStandaloneSettings(context) }
                                        )
                                    )
                                    add(
                                        ExpressiveFunctionSpec(
                                            title = "Wallpapers Gallery",
                                            badge = "4K & LIVE",
                                            icon = Icons.Default.Wallpaper,
                                            containerColor = Color(0xFF192E2B),
                                            iconContainerColor = Color(0xFF059669),
                                            accentColor = ControlEmerald,
                                            testTag = "tv_open_wallpapers_function_btn",
                                            onClick = onOpenWallpapers
                                        )
                                    )
                                    add(
                                        ExpressiveFunctionSpec(
                                            title = "Devices & Remote",
                                            badge = "CONNECT",
                                            icon = Icons.Default.Devices,
                                            containerColor = Color(0xFF2A2136),
                                            iconContainerColor = Color(0xFF9333EA),
                                            accentColor = ControlViolet,
                                            testTag = "tv_open_devices_function_btn",
                                            onClick = onOpenDevices
                                        )
                                    )
                                    add(
                                        ExpressiveFunctionSpec(
                                            title = "Launch 4K Ambient Mode",
                                            badge = "SCREENSAVER",
                                            icon = Icons.Default.WbTwilight,
                                            containerColor = Color(0xFF2D251C),
                                            iconContainerColor = Color(0xFFD97706),
                                            accentColor = ControlAmber,
                                            testTag = "tv_control_panel_ambient_mode_icon_btn",
                                            onClick = onLaunchAmbientMode
                                        )
                                    )
                                    add(
                                        ExpressiveFunctionSpec(
                                            title = "EdVanced 1.1 Pro Voice AI",
                                            badge = "GEMINI LIVE",
                                            icon = Icons.Default.Mic,
                                            containerColor = Color(0xFF1A293A),
                                            iconContainerColor = Color(0xFF0284C7),
                                            accentColor = ControlCyan,
                                            testTag = "tv_settings_open_ai_assistant_btn",
                                            onClick = onOpenVoiceAssistant
                                        )
                                    )
                                    if (rnaAiAppEnabled) {
                                        add(
                                            ExpressiveFunctionSpec(
                                                title = "Customize RNA AI & Agent Glow",
                                                badge = "AI STUDIO",
                                                icon = Icons.Default.AutoAwesome,
                                                containerColor = Color(0xFF281D3A),
                                                iconContainerColor = Color(0xFF7C3AED),
                                                accentColor = ControlViolet,
                                                testTag = "tv_settings_customize_rnaai_btn",
                                                onClick = onOpenRnaAiSettings
                                            )
                                        )
                                    }
                                    add(
                                        ExpressiveFunctionSpec(
                                            title = "Open RNA. Studio Settings",
                                            badge = "STUDIO",
                                            icon = Icons.Default.Edit,
                                            containerColor = Color(0xFF1F2437),
                                            iconContainerColor = Color(0xFF4F46E5),
                                            accentColor = Color(0xFF818CF8),
                                            testTag = "tv_open_rna_studio_function_btn",
                                            onClick = onOpenRnaStudioSettings
                                        )
                                    )
                                    add(
                                        ExpressiveFunctionSpec(
                                            title = "Open Android System Settings",
                                            badge = "ANDROID OS",
                                            icon = Icons.Default.Settings,
                                            containerColor = Color(0xFF202531),
                                            iconContainerColor = Color(0xFF475569),
                                            accentColor = Color(0xFF94A3B8),
                                            testTag = "tv_open_android_settings_function_btn",
                                            onClick = onOpenAndroidSettings
                                        )
                                    )
                                    add(
                                        ExpressiveFunctionSpec(
                                            title = "Clear Recent History",
                                            badge = "CLEANUP",
                                            icon = Icons.Default.DeleteOutline,
                                            containerColor = Color(0xFF2E1E26),
                                            iconContainerColor = Color(0xFFE11D48),
                                            accentColor = Color(0xFFFB7185),
                                            testTag = "tv_clear_recent_apps_button",
                                            onClick = onClearRecentApps
                                        )
                                    )
                                }

                                val rows = functionCards.chunked(2)
                                itemsIndexed(rows) { _, pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        pair.forEach { spec ->
                                            ExpressiveFunctionCard(
                                                spec = spec,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (pair.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }

                                // Live 4K Network Diagnostics Function Card
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = Color(0xFF182625),
                                        border = BorderStroke(1.5.dp, ControlEmerald.copy(alpha = 0.45f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(20.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(
                                                        shape = RoundedCornerShape(14.dp),
                                                        color = Color(0xFF059669),
                                                        modifier = Modifier.size(44.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Default.Speed,
                                                                contentDescription = null,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(22.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(14.dp))
                                                    Column {
                                                        Text(
                                                            text = "Live 4K Network Speed Test",
                                                            color = Color.White,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 15.sp
                                                        )
                                                        Text(
                                                            text = "LAN IP: $localIpAddress",
                                                            color = ControlEmerald,
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }
                                                }

                                                Button(
                                                    onClick = { runRealSpeedTest() },
                                                    enabled = !isTestingNetwork,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = ControlEmerald,
                                                        contentColor = Color(0xFF092218)
                                                    ),
                                                    shape = RoundedCornerShape(16.dp)
                                                ) {
                                                    Text(
                                                        text = if (isTestingNetwork) "Testing..." else "Run Speed Test",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 12.5.sp
                                                    )
                                                }
                                            }

                                            if (isTestingNetwork) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                LinearProgressIndicator(
                                                    color = ControlEmerald,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            } else if (downloadMbps != null || latencyMs != null) {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Surface(
                                                        shape = RoundedCornerShape(14.dp),
                                                        color = Color(0xFF111B1B),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp)) {
                                                            Text("Download Speed", color = Color(0xFF9E9FAF), fontSize = 11.sp)
                                                            Text(
                                                                text = downloadMbps?.let { String.format(Locale.US, "%.1f Mbps", it) } ?: "N/A",
                                                                color = ControlEmerald,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 16.sp
                                                            )
                                                        }
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(14.dp),
                                                        color = Color(0xFF111B1B),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp)) {
                                                            Text("Ping Latency", color = Color(0xFF9E9FAF), fontSize = 11.sp)
                                                            Text(
                                                                text = if (latencyMs != null && latencyMs!! > 0) "${latencyMs} ms" else "Offline",
                                                                color = ControlCyan,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                fontSize = 16.sp
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
        }
    }
}

@Composable
private fun ControlToggleRow(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    checked: Boolean,
    onToggle: () -> Unit,
    subtitle: String? = null,
    rowTestTag: String? = null,
    switchTestTag: String? = null
) {
    var rowFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (rowFocused) 1.015f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "toggle_row_scale"
    )
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (rowFocused) Color(0xFF2B3046) else Color(0xFF1D2030),
        border = BorderStroke(
            width = if (rowFocused) 2.dp else 1.dp,
            color = if (rowFocused) Color.White else Color(0x28FFFFFF)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .onFocusChanged { rowFocused = it.isFocused }
            .clickable { onToggle() }
            .focusable()
            .then(if (rowTestTag != null) Modifier.testTag(rowTestTag) else Modifier)
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
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconTint.copy(alpha = 0.16f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        color = Color(0xFFF2F3FA),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            color = Color(0xFF9E9FAF),
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF10131C),
                    checkedTrackColor = iconTint
                ),
                modifier = if (switchTestTag != null) Modifier.testTag(switchTestTag) else Modifier
            )
        }
    }
}

/**
 * Android Material 3 Expressive Tonal Spot Action Card for the Functions Tab.
 */
@Composable
private fun ExpressiveFunctionCard(
    spec: ExpressiveFunctionSpec,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.035f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "expressive_func_scale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (focused) Color.White else spec.containerColor,
        animationSpec = tween(150),
        label = "expressive_func_bg"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = bgColor,
        border = BorderStroke(
            width = if (focused) 2.5.dp else 1.2.dp,
            color = if (focused) Color.White else spec.accentColor.copy(alpha = 0.42f)
        ),
        modifier = modifier
            .scale(scale)
            .onFocusChanged { focused = it.isFocused }
            .clickable { spec.onClick() }
            .focusable()
            .testTag(spec.testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (focused) Color(0xFF10131C) else spec.iconContainerColor,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = spec.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = spec.badge,
                        color = if (focused) Color(0xFF475569) else spec.accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = spec.title,
                        color = if (focused) Color(0xFF10131C) else Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = CircleShape,
                color = if (focused) Color(0xFF10131C) else Color(0x26FFFFFF),
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun resolveLocalLanIp(): String {
    return try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val intf = interfaces.nextElement()
            val addrs = intf.inetAddresses
            while (addrs.hasMoreElements()) {
                val addr = addrs.nextElement()
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    return addr.hostAddress ?: "192.168.1.100"
                }
            }
        }
        "Connected"
    } catch (_: Exception) {
        "Connected"
    }
}
