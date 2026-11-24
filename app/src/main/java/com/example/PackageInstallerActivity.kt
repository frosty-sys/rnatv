package com.example

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import com.example.data.model.RnaApp
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.wizard.decodeDataUriToImageBitmap
import com.example.util.RnaPackageEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PackageInstallerActivity : ComponentActivity() {

    private var candidateApp by mutableStateOf<RnaApp?>(null)
    private var installState by mutableStateOf("PROMPT") // "PROMPT", "INSTALLING", "COMPLETE", "ERROR"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val uri = intent.data
        if (uri != null) {
            val app = RnaPackageEngine.parseRnaPackage(this, uri)
            if (app != null) {
                candidateApp = app
            } else {
                Toast.makeText(this, "Failed to parse .rna package", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0x99000000)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        candidateApp?.let { app ->
                            PackageInstallerCard(
                                app = app,
                                installState = installState,
                                onCancel = { finish() },
                                onInstall = {
                                    installState = "INSTALLING"
                                    lifecycleScope.launch {
                                        delay(450)
                                        val repo = (application as RnaApplication).repository
                                        val installed = repo.installApp(app, useSlotAlias = true, pinShortcut = true)
                                        candidateApp = installed
                                        installState = "COMPLETE"
                                    }
                                },
                                onOpen = {
                                    val intent = Intent(this@PackageInstallerActivity, WebContainerActivity::class.java).apply {
                                        putExtra(WebContainerActivity.EXTRA_APP_ID, app.id)
                                    }
                                    startActivity(intent)
                                    finish()
                                },
                                onDone = { finish() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PackageInstallerCard(
    app: RnaApp,
    installState: String,
    onCancel: () -> Unit,
    onInstall: () -> Unit,
    onOpen: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val installBtnFocus = remember { FocusRequester() }
    val openBtnFocus = remember { FocusRequester() }
    val iconUri = app.iconBase64 ?: app.bannerBase64
    val decodedIconBitmap = remember(iconUri) { decodeDataUriToImageBitmap(iconUri) }

    LaunchedEffect(installState) {
        delay(60L)
        when (installState) {
            "PROMPT" -> runCatching { installBtnFocus.requestFocus() }
            "COMPLETE" -> runCatching { openBtnFocus.requestFocus() }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .widthIn(max = 560.dp)
            .padding(vertical = 24.dp)
            .testTag("package_installer_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF161922)
        ),
        border = BorderStroke(1.5.dp, Color(0x33FFFFFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Android Package Installer Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = CyanPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Android Package Installer",
                        color = Color(0xFFA0AFC6),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "RNA Container Engine",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Real App Icon & Identity
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF222736),
                border = BorderStroke(2.dp, CyanPrimary.copy(alpha = 0.6f)),
                modifier = Modifier.size(80.dp)
            ) {
                when {
                    decodedIconBitmap != null -> {
                        Image(
                            bitmap = decodedIconBitmap,
                            contentDescription = app.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(22.dp))
                        )
                    }
                    !iconUri.isNullOrBlank() -> {
                        AsyncImage(
                            model = iconUri,
                            contentDescription = app.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(22.dp))
                        )
                    }
                    else -> {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = "App Icon",
                                tint = CyanPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = app.title,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = "${app.packageName} • v${app.version}",
                color = Color(0xFF8697B2),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (app.sourceType == "URL" && app.sourceUriOrContent.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = app.sourceUriOrContent,
                    color = CyanPrimary,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF232A3B))
            Spacer(modifier = Modifier.height(16.dp))

            // Content Switcher for Installer Stages
            AnimatedContent(
                targetState = installState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "InstallStageTransition"
            ) { state ->
                when (state) {
                    "PROMPT" -> {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Do you want to install this application? It will get access to:",
                                color = Color(0xFFD4DEED),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            // Permissions List
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                app.permissions.forEach { perm ->
                                    PermissionItemRow(perm = perm)
                                }
                            }

                            Spacer(modifier = Modifier.height(22.dp))

                            // TV-Focusable Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                var cancelFocused by remember { mutableStateOf(false) }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (cancelFocused) Color(0xFF334155) else Color(0xFF1F2433),
                                    border = BorderStroke(
                                        width = if (cancelFocused) 2.5.dp else 1.dp,
                                        color = if (cancelFocused) Color.White else Color(0x44FFFFFF)
                                    ),
                                    modifier = Modifier
                                        .onFocusChanged { cancelFocused = it.isFocused }
                                        .clickable { onCancel() }
                                        .testTag("installer_cancel_button")
                                ) {
                                    Text(
                                        text = "Cancel",
                                        color = if (cancelFocused) Color.White else Color(0xFFA0AFC6),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 11.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                var installFocused by remember { mutableStateOf(false) }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (installFocused) Color.White else CyanPrimary,
                                    border = BorderStroke(
                                        width = if (installFocused) 2.5.dp else 1.dp,
                                        color = if (installFocused) EmeraldSuccess else CyanPrimary
                                    ),
                                    modifier = Modifier
                                        .focusRequester(installBtnFocus)
                                        .onFocusChanged { installFocused = it.isFocused }
                                        .clickable { onInstall() }
                                        .testTag("installer_install_button")
                                ) {
                                    Text(
                                        text = "Install",
                                        color = Color(0xFF0A0E17),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 11.dp)
                                    )
                                }
                            }
                        }
                    }

                    "INSTALLING" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Installing...",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = CyanPrimary,
                                trackColor = Color(0xFF222736)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Saving web app container and adding to RNA. TV Launcher...",
                                color = Color(0xFF8697B2),
                                fontSize = 12.sp
                            )
                        }
                    }

                    "COMPLETE" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Installed",
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "App installed.",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Added to your RNA. TV Home & Apps Drawer",
                                color = Color(0xFFA0AFC6),
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                var doneFocused by remember { mutableStateOf(false) }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (doneFocused) Color(0xFF334155) else Color(0xFF1F2433),
                                    border = BorderStroke(
                                        width = if (doneFocused) 2.5.dp else 1.dp,
                                        color = if (doneFocused) Color.White else Color(0x44FFFFFF)
                                    ),
                                    modifier = Modifier
                                        .onFocusChanged { doneFocused = it.isFocused }
                                        .clickable { onDone() }
                                        .testTag("installer_done_button")
                                ) {
                                    Text(
                                        text = "Done",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp)
                                    )
                                }

                                var openFocused by remember { mutableStateOf(false) }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (openFocused) Color.White else EmeraldSuccess,
                                    border = BorderStroke(
                                        width = if (openFocused) 2.5.dp else 1.dp,
                                        color = if (openFocused) CyanPrimary else EmeraldSuccess
                                    ),
                                    modifier = Modifier
                                        .focusRequester(openBtnFocus)
                                        .onFocusChanged { openFocused = it.isFocused }
                                        .clickable { onOpen() }
                                        .testTag("installer_open_button")
                                ) {
                                    Text(
                                        text = "Open App",
                                        color = Color(0xFF0A0E17),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 11.dp)
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

@Composable
fun PermissionItemRow(perm: String) {
    val (icon, title, desc) = when (perm) {
        "CAMERA" -> Triple(Icons.Default.CameraAlt, "Take pictures and record video", "Allows web app to access device camera")
        "MICROPHONE", "RECORD_AUDIO" -> Triple(Icons.Default.Mic, "Record audio", "Allows web app to access device microphone")
        "LOCATION", "ACCESS_FINE_LOCATION" -> Triple(Icons.Default.MyLocation, "Precise location", "Access GPS location for mapping/geo features")
        "STORAGE", "MANAGE_EXTERNAL_STORAGE" -> Triple(Icons.Default.Folder, "Files and media", "Read and store offline container files")
        "VIBRATE" -> Triple(Icons.Default.Vibration, "Control vibration", "Allows haptic feedback in games")
        else -> Triple(Icons.Default.Wifi, "Full network access", "Allows app to create network sockets and load web URLs")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF222736),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CyanPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = desc,
                color = Color(0xFF7E8EA8),
                fontSize = 11.sp
            )
        }
    }
}
