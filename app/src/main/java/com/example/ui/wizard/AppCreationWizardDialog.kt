package com.example.ui.wizard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.RnaApp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CoralAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VioletSecondary
import java.io.File
import java.util.UUID

@Composable
fun AppCreationWizardDialog(
    onDismiss: () -> Unit,
    onSaveOnly: (RnaApp) -> Unit = {},
    onAppConfigured: (RnaApp, useSlotAlias: Boolean, pinShortcut: Boolean) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD90A0D14)),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                AppCreationWizardContent(
                    isModal = true,
                    onDismiss = onDismiss,
                    onSaveOnly = onSaveOnly,
                    onAppConfigured = onAppConfigured,
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .padding(vertical = 20.dp)
                )
            }
        }
    }
}

@Composable
fun AppCreationWizardContent(
    isModal: Boolean = false,
    onDismiss: () -> Unit = {},
    onSaveOnly: (RnaApp) -> Unit = {},
    onAppConfigured: (RnaApp, useSlotAlias: Boolean, pinShortcut: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(1) }

    // Step 1: Source ("URL", "CODE", "HTML_FILE", "BUNDLE")
    var sourceType by remember { mutableStateOf("URL") }
    var remoteUrl by remember { mutableStateOf("https://") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var rawCode by remember {
        mutableStateOf(
            """<!DOCTYPE html>
<html>
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        body { background: #10131E; color: #00E5FF; font-family: sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; }
        .box { text-align: center; padding: 28px; border: 1px solid #28324E; border-radius: 20px; background: #171B2B; }
        button { background: #00E5FF; color: #000; border: none; padding: 12px 24px; border-radius: 12px; font-weight: bold; margin-top: 14px; cursor: pointer; }
    </style>
</head>
<body>
    <div class="box">
        <h2>Custom RNA. App</h2>
        <p>Running in isolated RNA. Native Container with D-Pad & Mouse Support</p>
        <button onclick="alert('Hello from RNA.!')">Test Action</button>
    </div>
</body>
</html>"""
        )
    }

    // Step 2: Metadata & Circular Logo + TV Logo
    var appTitle by remember { mutableStateOf("My Web App") }
    var packageName by remember { mutableStateOf("com.rna.customapp") }
    var versionName by remember { mutableStateOf("1.0.0") }
    var selectedLogoUri by remember { mutableStateOf<String?>(null) }
    var enableTvControls by remember { mutableStateOf(true) }
    var selectedTvLogoUri by remember { mutableStateOf<String?>(null) }

    // Step 3: Controls, Top Loading Bar Customization & Permissions
    var defaultInputMode by remember { mutableStateOf("DPAD") }
    var mouseSpeed by remember { mutableFloatStateOf(1.5f) }
    var forceDarkMode by remember { mutableStateOf(false) }
    var showLoadingBar by remember { mutableStateOf(true) }
    var loadingBarColor by remember { mutableStateOf("#00E5FF") }
    var useSlotAlias by remember { mutableStateOf(true) }
    var pinShortcut by remember { mutableStateOf(true) }

    val selectedPermissions = remember {
        mutableStateListOf("INTERNET", "VIBRATE")
    }

    // File Pickers
    val htmlFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val text = context.contentResolver.openInputStream(it)?.bufferedReader()?.readText()
                if (!text.isNullOrBlank()) {
                    rawCode = text
                    selectedFileName = it.lastPathSegment ?: "index.html"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val folderOrZipPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        treeUri?.let {
            selectedFileName = it.lastPathSegment ?: "Project Folder (HTML/TS/Vite)"
        }
    }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedLogoUri = it.toString() }
    }

    val tvLogoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedTvLogoUri = it.toString() }
    }

    Card(
        modifier = modifier.testTag("app_creation_wizard_dialog"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141722)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(22.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CREATE RNA. WEB APPLICATION",
                        color = CyanPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = when (step) {
                            1 -> "Step 1: Website, HTML, or TS/Vite Folder"
                            2 -> "Step 2: Circular App Logo & Android TV Controls"
                            else -> "Step 3: Loading Bar, D-Pad/Mouse & Permissions"
                        },
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isModal) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFA0B0C8)
                        )
                    }
                }
            }

            // Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(if (step >= i) CyanPrimary else Color(0xFF22283A))
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "WizardStepTransition"
            ) { currentStep ->
                when (currentStep) {
                    1 -> Step1SourceSelection(
                        sourceType = sourceType,
                        onSourceTypeChange = { sourceType = it },
                        remoteUrl = remoteUrl,
                        onUrlChange = { remoteUrl = it },
                        rawCode = rawCode,
                        onRawCodeChange = { rawCode = it },
                        selectedFileName = selectedFileName,
                        onPickHtmlFile = { htmlFilePicker.launch(arrayOf("text/html", "*/*")) },
                        onPickFolder = { folderOrZipPicker.launch(null) }
                    )
                    2 -> Step2BrandingAndTv(
                        title = appTitle,
                        onTitleChange = {
                            appTitle = it
                            packageName = "com.rna.${it.lowercase().replace("[^a-z0-9]".toRegex(), "")}"
                        },
                        pkg = packageName,
                        onPkgChange = { packageName = it },
                        version = versionName,
                        onVersionChange = { versionName = it },
                        websiteUrl = if (sourceType == "URL") remoteUrl else null,
                        selectedLogoUri = selectedLogoUri,
                        onSelectLogoUri = { selectedLogoUri = it },
                        onPickLogo = { logoPicker.launch("image/*") },
                        enableTvControls = enableTvControls,
                        onTvControlsToggle = { enableTvControls = it },
                        selectedTvLogoUri = selectedTvLogoUri,
                        onSelectTvLogoUri = { selectedTvLogoUri = it },
                        onPickTvLogo = { tvLogoPicker.launch("image/*") }
                    )
                    3 -> Step3ControlsAndLoadingBar(
                        defaultInputMode = defaultInputMode,
                        onInputModeChange = { defaultInputMode = it },
                        mouseSpeed = mouseSpeed,
                        onMouseSpeedChange = { mouseSpeed = it },
                        forceDarkMode = forceDarkMode,
                        onForceDarkModeChange = { forceDarkMode = it },
                        showLoadingBar = showLoadingBar,
                        onShowLoadingBarChange = { showLoadingBar = it },
                        loadingBarColor = loadingBarColor,
                        onLoadingBarColorChange = { loadingBarColor = it },
                        useSlotAlias = useSlotAlias,
                        onUseSlotAliasChange = { useSlotAlias = it },
                        pinShortcut = pinShortcut,
                        onPinShortcutChange = { pinShortcut = it },
                        selectedPermissions = selectedPermissions
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFF22273A))
            Spacer(modifier = Modifier.height(14.dp))

            // Footer Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier.testTag("wizard_prev_step")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Back", color = Color.White)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (step < 3) {
                    Button(
                        onClick = { step++ },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                        modifier = Modifier.testTag("wizard_next_step")
                    ) {
                        Text("Next", color = Color.Black, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Black)
                    }
                } else {
                    val buildAppModel: () -> RnaApp = {
                        val finalSourceType = if (sourceType == "HTML_FILE") "CODE" else sourceType
                        val content = when (sourceType) {
                            "URL" -> remoteUrl
                            "CODE", "HTML_FILE" -> rawCode
                            else -> rawCode
                        }
                        RnaApp(
                            id = UUID.randomUUID().toString(),
                            packageName = packageName.ifBlank { "com.rna.custom" },
                            title = appTitle.ifBlank { "Untitled App" },
                            version = versionName.ifBlank { "1.0.0" },
                            sourceType = finalSourceType,
                            sourceUriOrContent = content,
                            iconBase64 = selectedLogoUri,
                            bannerBase64 = selectedTvLogoUri ?: selectedLogoUri,
                            isTvEnabled = enableTvControls,
                            defaultInputMode = defaultInputMode,
                            mouseSpeed = mouseSpeed,
                            forceDarkMode = forceDarkMode,
                            showLoadingBar = showLoadingBar,
                            loadingBarColor = loadingBarColor,
                            permissions = selectedPermissions.toList()
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                onSaveOnly(buildAppModel())
                            },
                            modifier = Modifier.testTag("wizard_save_only_button")
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save to Manage", color = Color.White, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                onAppConfigured(buildAppModel(), useSlotAlias, pinShortcut)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                            modifier = Modifier.testTag("wizard_finish_button")
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Install App", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Step1SourceSelection(
    sourceType: String,
    onSourceTypeChange: (String) -> Unit,
    remoteUrl: String,
    onUrlChange: (String) -> Unit,
    rawCode: String,
    onRawCodeChange: (String) -> Unit,
    selectedFileName: String?,
    onPickHtmlFile: () -> Unit,
    onPickFolder: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Choose Web Source (Website, HTML File, Raw Code, or TS/Vite Folder):",
            color = Color(0xFFCCD7E8),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SourceTypeCard(
                title = "Website",
                icon = Icons.Default.Language,
                selected = sourceType == "URL",
                onClick = { onSourceTypeChange("URL") },
                modifier = Modifier.weight(1f)
            )
            SourceTypeCard(
                title = "HTML File",
                icon = Icons.Default.Description,
                selected = sourceType == "HTML_FILE",
                onClick = { onSourceTypeChange("HTML_FILE") },
                modifier = Modifier.weight(1f)
            )
            SourceTypeCard(
                title = "Code",
                icon = Icons.Default.Code,
                selected = sourceType == "CODE",
                onClick = { onSourceTypeChange("CODE") },
                modifier = Modifier.weight(1f)
            )
            SourceTypeCard(
                title = "TS / Vite Folder",
                icon = Icons.Default.FolderZip,
                selected = sourceType == "BUNDLE",
                onClick = { onSourceTypeChange("BUNDLE") },
                modifier = Modifier.weight(1f)
            )
        }

        when (sourceType) {
            "URL" -> {
                OutlinedTextField(
                    value = remoteUrl,
                    onValueChange = onUrlChange,
                    label = { Text("Website URL") },
                    placeholder = { Text("https://example.com") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanPrimary,
                        unfocusedBorderColor = Color(0xFF28324E),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("wizard_input_url")
                )
            }
            "HTML_FILE" -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2030)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = selectedFileName ?: "Select an .html file from storage",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onPickHtmlFile,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            Text("Choose HTML File", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            "CODE" -> {
                Column {
                    Text(
                        text = "Paste HTML, CSS, TypeScript / JavaScript:",
                        color = Color(0xFF8B9BB4),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = rawCode,
                        onValueChange = onRawCodeChange,
                        minLines = 7,
                        maxLines = 11,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanPrimary,
                            unfocusedBorderColor = Color(0xFF28324E),
                            focusedTextColor = Color(0xFFE0E7FF),
                            unfocusedTextColor = Color(0xFFE0E7FF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wizard_input_code")
                    )
                }
            }
            "BUNDLE" -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2030)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.FolderZip, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = selectedFileName ?: "HTML / TypeScript / Vite Project Folder",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select a folder containing index.html, TypeScript, Vite build assets, or offline scripts.",
                            color = Color(0xFFA0B0C8),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )
                        Button(
                            onClick = onPickFolder,
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            Text("Select Project Folder", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceTypeCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) CyanPrimary.copy(alpha = 0.15f) else Color(0xFF1B2030),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, CyanPrimary) else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262D42)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) CyanPrimary else Color(0xFF8697B2),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = if (selected) Color.White else Color(0xFFA0B0C8),
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun Step2BrandingAndTv(
    title: String,
    onTitleChange: (String) -> Unit,
    pkg: String,
    onPkgChange: (String) -> Unit,
    version: String,
    onVersionChange: (String) -> Unit,
    websiteUrl: String?,
    selectedLogoUri: String?,
    onSelectLogoUri: (String) -> Unit,
    onPickLogo: () -> Unit,
    enableTvControls: Boolean,
    onTvControlsToggle: (Boolean) -> Unit,
    selectedTvLogoUri: String?,
    onSelectTvLogoUri: (String) -> Unit,
    onPickTvLogo: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Circular App Logo Builder & Picker (16 Preset Vectors + Alphabet Monogram + Website Favicon + Upload)
        RnaLogoSelectorCard(
            headerTitle = "Circular App Logo",
            headerSubtitle = "Choose from 16 preset vector logos, build an alphabet monogram, fetch website favicon, or upload photo",
            currentLogoUri = selectedLogoUri,
            websiteUrlForFavicon = websiteUrl,
            defaultTitleForAlphabet = title.take(2).uppercase(),
            onSelectLogoUri = onSelectLogoUri,
            onPickFromGallery = onPickLogo
        )

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("App Title") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanPrimary,
                unfocusedBorderColor = Color(0xFF28324E),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("wizard_input_title")
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = pkg,
                onValueChange = onPkgChange,
                label = { Text("Package ID") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = Color(0xFF28324E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.weight(1.4f)
            )
            OutlinedTextField(
                value = version,
                onValueChange = onVersionChange,
                label = { Text("Version") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = Color(0xFF28324E),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.weight(0.6f)
            )
        }

        // Android TV Controls Toggle + TV Circular Logo Submission
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2030)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Tv, contentDescription = null, tint = CyanPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Turn On Android TV Controls", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("D-Pad focus first -> Hold OK for Mouse -> Hold OK for Scroll -> Back reverses step-by-step", color = Color(0xFF8697B2), fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = enableTvControls,
                        onCheckedChange = onTvControlsToggle,
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary)
                    )
                }

                AnimatedVisibility(visible = enableTvControls) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        HorizontalDivider(color = Color(0xFF28324E))
                        Spacer(modifier = Modifier.height(10.dp))
                        RnaLogoSelectorCard(
                            headerTitle = "Android TV Circular Logo",
                            headerSubtitle = "Visible on RNA. TV Home & Apps row (defaults to App Logo if not set separately)",
                            currentLogoUri = selectedTvLogoUri ?: selectedLogoUri,
                            websiteUrlForFavicon = websiteUrl,
                            defaultTitleForAlphabet = title.take(2).uppercase(),
                            onSelectLogoUri = onSelectTvLogoUri,
                            onPickFromGallery = onPickTvLogo
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Step3ControlsAndLoadingBar(
    defaultInputMode: String,
    onInputModeChange: (String) -> Unit,
    mouseSpeed: Float,
    onMouseSpeedChange: (Float) -> Unit,
    forceDarkMode: Boolean,
    onForceDarkModeChange: (Boolean) -> Unit,
    showLoadingBar: Boolean,
    onShowLoadingBarChange: (Boolean) -> Unit,
    loadingBarColor: String,
    onLoadingBarColorChange: (String) -> Unit,
    useSlotAlias: Boolean,
    onUseSlotAliasChange: (Boolean) -> Unit,
    pinShortcut: Boolean,
    onPinShortcutChange: (Boolean) -> Unit,
    selectedPermissions: MutableList<String>
) {
    val presetColors = listOf(
        "#00E5FF" to CyanPrimary,
        "#7C4DFF" to VioletSecondary,
        "#00E676" to EmeraldSuccess,
        "#FFB300" to AmberAccent,
        "#FF5252" to CoralAccent,
        "#FFFFFF" to Color.White
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Top Loading Bar Customization Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2030)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Top Loading Progress Bar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Customize color of the top loading bar or remove it entirely", color = Color(0xFF8697B2), fontSize = 11.sp)
                    }
                    Switch(
                        checked = showLoadingBar,
                        onCheckedChange = onShowLoadingBarChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary),
                        modifier = Modifier.testTag("wizard_loading_bar_switch")
                    )
                }

                AnimatedVisibility(visible = showLoadingBar) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text("Loading Bar Color:", color = Color(0xFFA0B0C8), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            presetColors.forEach { (hex, composeColor) ->
                                val isSelected = loadingBarColor.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(composeColor)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                        .clickable { onLoadingBarColorChange(hex) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Default TV Input Mode & Mouse Speed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (defaultInputMode == "DPAD") CyanPrimary.copy(alpha = 0.2f) else Color(0xFF1B2030),
                border = if (defaultInputMode == "DPAD") androidx.compose.foundation.BorderStroke(1.5.dp, CyanPrimary) else null,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onInputModeChange("DPAD") }
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Tv, contentDescription = null, tint = CyanPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("D-Pad Spatial", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text("Hold OK -> Mouse", color = Color(0xFF8697B2), fontSize = 10.sp)
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (defaultInputMode == "MOUSE") CyanPrimary.copy(alpha = 0.2f) else Color(0xFF1B2030),
                border = if (defaultInputMode == "MOUSE") androidx.compose.foundation.BorderStroke(1.5.dp, CyanPrimary) else null,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onInputModeChange("MOUSE") }
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.Mouse, contentDescription = null, tint = CyanPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Mouse + Edge Scroll", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Text("Hold OK -> Scroll", color = Color(0xFF8697B2), fontSize = 10.sp)
                }
            }
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Virtual Mouse Speed:", color = Color(0xFFA0B0C8), fontSize = 12.sp)
                Text("${String.format("%.1f", mouseSpeed)}x", color = CyanPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Slider(
                value = mouseSpeed,
                onValueChange = onMouseSpeedChange,
                valueRange = 0.5f..3.0f,
                steps = 4,
                colors = SliderDefaults.colors(
                    thumbColor = CyanPrimary,
                    activeTrackColor = CyanPrimary
                )
            )
        }

        // 3. Force Dark Mode Switch
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2030)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.DarkMode, contentDescription = null, tint = VioletSecondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Force Website Dark Mode", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Applies dark theme even if webpage doesn't support it natively", color = Color(0xFF8697B2), fontSize = 11.sp)
                    }
                }
                Switch(
                    checked = forceDarkMode,
                    onCheckedChange = onForceDarkModeChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = VioletSecondary)
                )
            }
        }

        // 4. Permissions
        Text("Hardware Permissions:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val perms = listOf("CAMERA", "RECORD_AUDIO", "ACCESS_FINE_LOCATION", "STORAGE", "VIBRATE")
            perms.forEach { permKey ->
                val checked = selectedPermissions.contains(permKey)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (checked) CyanPrimary.copy(alpha = 0.2f) else Color(0xFF1B2030),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (checked) CyanPrimary else Color(0xFF28324E)
                    ),
                    modifier = Modifier.clickable {
                        if (checked) selectedPermissions.remove(permKey)
                        else selectedPermissions.add(permKey)
                    }
                ) {
                    Text(
                        text = permKey,
                        color = if (checked) CyanPrimary else Color(0xFFA0B0C8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
