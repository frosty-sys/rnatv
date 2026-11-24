package com.example.ui.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.RnaApp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VioletSecondary

@Composable
fun ManageScreen(
    installedApps: List<RnaApp>,
    onInstallClick: (RnaApp) -> Unit,
    onUpdateApp: (RnaApp, oldSlot: Int?, enableSlot: Boolean) -> Unit,
    onExportApp: (RnaApp) -> Unit,
    onUninstallApp: (RnaApp) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedAppId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("manage_screen_list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "MANAGE & INSTALL SAVED APPS",
                    color = CyanPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Saved & Installed RNA. Packages",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Click Install on any saved package to open the Android Package Installer, edit loading bar colors, or export .rna bundles.",
                    color = Color(0xFFA0AFC6),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            }
        }

        if (installedApps.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141724))
                ) {
                    Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No saved apps yet. Create one in the Create tab!", color = Color(0xFF8697B2))
                    }
                }
            }
        } else {
            items(installedApps, key = { it.id }) { app ->
                val isExpanded = expandedAppId == app.id
                ManageAppItem(
                    app = app,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedAppId = if (isExpanded) null else app.id
                    },
                    onInstallClick = { onInstallClick(app) },
                    onSave = { updatedApp, enableSlot ->
                        onUpdateApp(updatedApp, app.assignedSlot, enableSlot)
                        expandedAppId = null
                    },
                    onExport = { onExportApp(app) },
                    onUninstall = { onUninstallApp(app) }
                )
            }
        }
    }
}

@Composable
private fun ManageAppItem(
    app: RnaApp,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onInstallClick: () -> Unit,
    onSave: (RnaApp, enableSlot: Boolean) -> Unit,
    onExport: () -> Unit,
    onUninstall: () -> Unit
) {
    var editTitle by remember(app) { mutableStateOf(app.title) }
    var editPkg by remember(app) { mutableStateOf(app.packageName) }
    var editVersion by remember(app) { mutableStateOf(app.version) }
    var editUrlOrContent by remember(app) { mutableStateOf(app.sourceUriOrContent) }
    var editLogoUri by remember(app) { mutableStateOf(app.iconBase64 ?: app.bannerBase64) }
    var editDefaultInputMode by remember(app) { mutableStateOf(app.defaultInputMode) }
    var editMouseSpeed by remember(app) { mutableFloatStateOf(app.mouseSpeed) }
    var editForceDark by remember(app) { mutableStateOf(app.forceDarkMode) }
    var editShowLoadingBar by remember(app) { mutableStateOf(app.showLoadingBar) }
    var editLoadingBarColor by remember(app) { mutableStateOf(app.loadingBarColor) }
    var editEnableSlot by remember(app) { mutableStateOf(app.assignedSlot != null) }

    val customLogoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { editLogoUri = it.toString() }
    }

    val presetColors = listOf(
        "#00E5FF" to CyanPrimary,
        "#7C4DFF" to VioletSecondary,
        "#00E676" to EmeraldSuccess,
        "#FFB300" to AmberAccent,
        "#FF5252" to CoralAccent,
        "#FFFFFF" to Color.White
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("manage_app_item_${app.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141724))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Circular Logo
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E2436),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, CyanPrimary.copy(alpha = 0.6f)),
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (!editLogoUri.isNullOrBlank()) {
                            AsyncImage(
                                model = editLogoUri,
                                contentDescription = app.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Widgets,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = app.title,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${app.packageName} • v${app.version}",
                            color = Color(0xFF8697B2),
                            fontSize = 11.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            if (app.isInstalled && app.assignedSlot != null) {
                                Surface(shape = RoundedCornerShape(8.dp), color = EmeraldSuccess.copy(alpha = 0.15f)) {
                                    Text(
                                        text = "Installed (Slot ${app.assignedSlot})",
                                        color = EmeraldSuccess,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Surface(shape = RoundedCornerShape(8.dp), color = AmberAccent.copy(alpha = 0.15f)) {
                                    Text(
                                        text = "Saved Package",
                                        color = AmberAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Surface(shape = RoundedCornerShape(8.dp), color = CyanPrimary.copy(alpha = 0.12f)) {
                                Text(
                                    text = "Default: ${app.defaultInputMode}",
                                    color = CyanPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Prominent Install Button right on the Manage card!
                Button(
                    onClick = onInstallClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (app.isInstalled) Color(0xFF222A40) else CyanPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("manage_install_button_${app.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.InstallMobile,
                        contentDescription = null,
                        tint = if (app.isInstalled) CyanPrimary else Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (app.isInstalled) "Re-Install" else "Install",
                        color = if (app.isInstalled) Color.White else Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.testTag("expand_app_button_${app.id}")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = Color(0xFFA0AFC6)
                    )
                }
            }

            // Expanded Full Configuration Editor
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HorizontalDivider(color = Color(0xFF222738))

                    // Full Logo Studio (Preset Vectors, Alphabet Monogram, Website Favicon, or Custom Image)
                    com.example.ui.wizard.RnaLogoSelectorCard(
                        headerTitle = "App & TV Circular Logo",
                        headerSubtitle = "Pick a preset vector icon, build an alphabet monogram, fetch website favicon, or upload photo",
                        currentLogoUri = editLogoUri,
                        websiteUrlForFavicon = if (app.sourceType == "URL") editUrlOrContent else null,
                        defaultTitleForAlphabet = editTitle.take(2).uppercase(),
                        onSelectLogoUri = { editLogoUri = it },
                        onPickFromGallery = { customLogoPicker.launch("image/*") }
                    )

                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Application Title") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = Color(0xFF262D42),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = editPkg,
                            onValueChange = { editPkg = it },
                            label = { Text("Package Name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = Color(0xFF262D42),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1.4f)
                        )
                        OutlinedTextField(
                            value = editVersion,
                            onValueChange = { editVersion = it },
                            label = { Text("Version") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = Color(0xFF262D42),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(0.6f)
                        )
                    }

                    OutlinedTextField(
                        value = editUrlOrContent,
                        onValueChange = { editUrlOrContent = it },
                        label = {
                            Text(if (app.sourceType == "URL") "Website URL" else "HTML / TypeScript Source Code")
                        },
                        singleLine = app.sourceType == "URL",
                        minLines = if (app.sourceType == "URL") 1 else 4,
                        maxLines = if (app.sourceType == "URL") 1 else 8,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = Color(0xFF262D42),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Default TV Input Mode Selector (D-Pad Default vs Mouse Default)
                    Text("Default TV & Web Input Mode:", color = Color(0xFFD6DEEE), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (editDefaultInputMode == "DPAD") CyanPrimary.copy(alpha = 0.2f) else Color(0xFF1B2030),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (editDefaultInputMode == "DPAD") CyanPrimary else Color(0x28FFFFFF)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { editDefaultInputMode = "DPAD" }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("D-Pad Default", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Hold OK -> Mouse -> Hold OK -> Scroll", color = Color(0xFF8E9BB4), fontSize = 10.sp)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (editDefaultInputMode == "MOUSE") CyanPrimary.copy(alpha = 0.2f) else Color(0xFF1B2030),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (editDefaultInputMode == "MOUSE") CyanPrimary else Color(0x28FFFFFF)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { editDefaultInputMode = "MOUSE" }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Mouse Default", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Hold OK -> D-Pad -> Hold OK -> Scroll", color = Color(0xFF8E9BB4), fontSize = 10.sp)
                            }
                        }
                    }

                    // Top Loading Bar Toggle & Color Picker
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Show Top Loading Bar", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Or turn off to remove the top loading bar entirely", color = Color(0xFF8697B2), fontSize = 11.sp)
                        }
                        Switch(
                            checked = editShowLoadingBar,
                            onCheckedChange = { editShowLoadingBar = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
                        )
                    }

                    AnimatedVisibility(visible = editShowLoadingBar) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bar Color:", color = Color(0xFFA0B0C8), fontSize = 12.sp)
                            presetColors.forEach { (hex, c) ->
                                val selected = editLoadingBarColor.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) Color.White else Color.Black,
                                            shape = CircleShape
                                        )
                                        .clickable { editLoadingBarColor = hex }
                                )
                            }
                        }
                    }

                    // Mouse Speed Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Virtual Mouse Speed Multiplier:", color = Color(0xFFA0B0C8), fontSize = 12.sp)
                            Text("${String.format("%.1f", editMouseSpeed)}x", color = CyanPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Slider(
                            value = editMouseSpeed,
                            onValueChange = { editMouseSpeed = it },
                            valueRange = 0.5f..3.0f,
                            steps = 4,
                            colors = SliderDefaults.colors(
                                thumbColor = CyanPrimary,
                                activeTrackColor = CyanPrimary
                            )
                        )
                    }

                    // Force Dark Mode Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Force Website Dark Mode", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Inject forced dark theme on webpage", color = Color(0xFF8697B2), fontSize = 11.sp)
                        }
                        Switch(
                            checked = editForceDark,
                            onCheckedChange = { editForceDark = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = VioletSecondary)
                        )
                    }

                    HorizontalDivider(color = Color(0xFF222738))

                    // Action Buttons (Export, Uninstall, Save)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onExport,
                                modifier = Modifier.testTag("manage_export_${app.id}")
                            ) {
                                Icon(imageVector = Icons.Default.IosShare, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export .rna", color = Color.White, fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = onUninstall,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralAccent),
                                modifier = Modifier.testTag("manage_uninstall_${app.id}")
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = CoralAccent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Uninstall", color = CoralAccent, fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = {
                                val updated = app.copy(
                                    title = editTitle.ifBlank { app.title },
                                    packageName = editPkg.ifBlank { app.packageName },
                                    version = editVersion.ifBlank { app.version },
                                    sourceUriOrContent = editUrlOrContent,
                                    iconBase64 = editLogoUri,
                                    bannerBase64 = editLogoUri,
                                    defaultInputMode = editDefaultInputMode,
                                    mouseSpeed = editMouseSpeed,
                                    forceDarkMode = editForceDark,
                                    showLoadingBar = editShowLoadingBar,
                                    loadingBarColor = editLoadingBarColor
                                )
                                onSave(updated, editEnableSlot)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            modifier = Modifier.testTag("manage_save_${app.id}")
                        ) {
                            Text("Save All Changes", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
