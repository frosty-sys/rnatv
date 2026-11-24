package com.example.ui.tabs

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.DownloadListener
import android.webkit.URLUtil
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.RnaApp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VioletSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun MoreScreen(
    installedApps: List<RnaApp>,
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    showRecentAppsRow: Boolean = true,
    onToggleRecentAppsRow: (Boolean) -> Unit = {},
    onClearRecentApps: () -> Unit = {},
    onInstallRnaUri: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    var subSection by remember { mutableIntStateOf(0) } // 0: Settings & Hub, 1: Explore, 2: File Explorer, 3: About
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxSize()) {
        // Sub-tabs navigation row
        Surface(
            color = Color(0xFF141724),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MoreSubTabItem(
                    title = "Settings",
                    icon = Icons.Default.Settings,
                    selected = subSection == 0,
                    onClick = { subSection = 0 },
                    modifier = Modifier.weight(1f)
                )
                MoreSubTabItem(
                    title = "Explore",
                    icon = Icons.Default.Explore,
                    selected = subSection == 1,
                    onClick = { subSection = 1 },
                    modifier = Modifier.weight(1f)
                )
                MoreSubTabItem(
                    title = "Files",
                    icon = Icons.Default.FolderOpen,
                    selected = subSection == 2,
                    onClick = { subSection = 2 },
                    modifier = Modifier.weight(1f)
                )
                MoreSubTabItem(
                    title = "About",
                    icon = Icons.Default.Info,
                    selected = subSection == 3,
                    onClick = { subSection = 3 },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (subSection) {
                0 -> SettingsAndOverviewSubScreen(
                    installedApps = installedApps,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = onToggleDarkTheme,
                    showRecentAppsRow = showRecentAppsRow,
                    onToggleRecentAppsRow = onToggleRecentAppsRow,
                    onClearRecentApps = onClearRecentApps
                )
                1 -> ExploreDirectorySubScreen(
                    onRnaDownloaded = { uri ->
                        onInstallRnaUri(uri)
                    }
                )
                2 -> InternalFileExplorerSubScreen(
                    onInstallFile = { file ->
                        onInstallRnaUri(Uri.fromFile(file))
                    }
                )
                3 -> AboutSubScreen()
            }
        }
    }
}

@Composable
private fun MoreSubTabItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) CyanPrimary.copy(alpha = 0.2f) else Color.Transparent,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary) else null,
        modifier = modifier
            .clickable { onClick() }
            .testTag("more_subtab_${title.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) CyanPrimary else Color(0xFF8697B2),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                color = if (selected) Color.White else Color(0xFF8697B2),
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SettingsAndOverviewSubScreen(
    installedApps: List<RnaApp>,
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    showRecentAppsRow: Boolean,
    onToggleRecentAppsRow: (Boolean) -> Unit,
    onClearRecentApps: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_overview_list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "GLOBAL PREFERENCES",
                color = CyanPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // Recent Apps Row on Launcher Switcher Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141724))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleRecentAppsRow(!showRecentAppsRow) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = CyanPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = CyanPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Recent Apps Row on Launcher",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Quickly resume last used RNA. web apps and normal apps",
                                    color = Color(0xFF8697B2),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Switch(
                            checked = showRecentAppsRow,
                            onCheckedChange = { onToggleRecentAppsRow(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = CyanPrimary),
                            modifier = Modifier.testTag("settings_recent_apps_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onClearRecentApps,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_clear_recent_apps_button")
                    ) {
                        Text("Clear Recent Apps History", color = Color(0xFFE4E1E9), fontSize = 12.sp)
                    }
                }
            }
        }

        // Dark Theme Switcher Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141724))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = VioletSecondary.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = VioletSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text("Dark Mode Theme", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("RNA interface appearance", color = Color(0xFF8697B2), fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleDarkTheme() },
                        colors = SwitchDefaults.colors(checkedThumbColor = VioletSecondary),
                        modifier = Modifier.testTag("settings_dark_mode_switch")
                    )
                }
            }
        }

        // Pre-allocated Activity Aliases Pool Status
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141724))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "App Drawer Activity Aliases Pool",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        val usedCount = installedApps.count { it.assignedSlot != null }
                        Text(
                            text = "$usedCount / 10 Active",
                            color = CyanPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "Pre-defined AndroidManifest aliases (Slot1..Slot10) sitting directly in the system App Drawer and Leanback row.",
                        color = Color(0xFF8697B2),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    // 10 Slot Badges Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (slot in 1..5) {
                            val assignedApp = installedApps.firstOrNull { it.assignedSlot == slot }
                            SlotBadge(slot = slot, app = assignedApp, modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (slot in 6..10) {
                            val assignedApp = installedApps.firstOrNull { it.assignedSlot == slot }
                            SlotBadge(slot = slot, app = assignedApp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Advanced Android Hardware & System Settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141724))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "System Permissions & Access",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Packaged web apps can take full advantage of camera, microphone, geolocation and storage hardware.",
                        color = Color(0xFF8697B2),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = CyanPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open App Permissions in Android Settings", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotBadge(slot: Int, app: RnaApp?, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (app != null) EmeraldSuccess.copy(alpha = 0.15f) else Color(0xFF1E2333),
        border = if (app != null) androidx.compose.foundation.BorderStroke(1.dp, EmeraldSuccess) else null,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "S$slot",
                color = if (app != null) EmeraldSuccess else Color(0xFF6B7B96),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (app != null) "In-Use" else "Free",
                color = if (app != null) Color.White else Color(0xFF4A556B),
                fontSize = 9.sp
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ExploreDirectorySubScreen(
    onRnaDownloaded: (Uri) -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var exploreWebView by remember { mutableStateOf<WebView?>(null) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            factory = { ctx ->
                WebView(ctx).apply {
                    exploreWebView = this
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                        }
                    }

                    // Download intercept listener for .rna files!
                    setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                        scope.launch {
                            val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
                            Toast.makeText(context, "Downloading $fileName...", Toast.LENGTH_SHORT).show()
                            val destFile = File(context.getExternalFilesDir(null) ?: context.filesDir, fileName)

                            withContext(Dispatchers.IO) {
                                try {
                                    val connection = URL(url).openConnection() as HttpURLConnection
                                    connection.connect()
                                    connection.inputStream.use { input ->
                                        FileOutputStream(destFile).use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            if (destFile.exists()) {
                                Toast.makeText(context, "Downloaded $fileName! Opening installer...", Toast.LENGTH_SHORT).show()
                                onRnaDownloaded(Uri.fromFile(destFile))
                            }
                        }
                    }

                    loadUrl("http://edvanced.ct.ws/exp.html")
                }
            }
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x800A0D14)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CyanPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading RNA Directory (edvanced.ct.ws)...", color = Color.White, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun InternalFileExplorerSubScreen(
    onInstallFile: (File) -> Unit
) {
    val context = LocalContext.current
    var rnaFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var isScanning by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val list = mutableListOf<File>()
            // Search app internal and external directories
            val searchDirs = listOfNotNull(
                context.getExternalFilesDir(null),
                context.filesDir,
                context.cacheDir,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            )

            searchDirs.forEach { dir ->
                if (dir.exists()) {
                    dir.walkTopDown().maxDepth(3).forEach { file ->
                        if (file.isFile && (file.name.endsWith(".rna", ignoreCase = true) || file.name.endsWith(".zip", ignoreCase = true))) {
                            list.add(file)
                        }
                    }
                }
            }
            rnaFiles = list
            isScanning = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("internal_file_explorer_list"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "INTERNAL FILE EXPLORER",
                color = CyanPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = "Scanned .rna Packages",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Browsing device storage for .rna packages to preview and install.",
                color = Color(0xFFA0AFC6),
                fontSize = 12.sp
            )
        }

        if (isScanning) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CyanPrimary)
                }
            }
        } else if (rnaFiles.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141724))
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF5B6C8C), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No .rna files found", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Download packages from the Explore Directory or export an app from the Manage tab to see it here.",
                            color = Color(0xFFA0B0C8),
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(rnaFiles) { file ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141724))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CyanPrimary.copy(alpha = 0.15f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = Icons.Default.Folder, contentDescription = null, tint = CyanPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(file.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("${file.length() / 1024} KB • ${file.parentFile?.name}", color = Color(0xFF8697B2), fontSize = 11.sp)
                            }
                        }

                        Button(
                            onClick = { onInstallFile(file) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
                        ) {
                            Text("Install", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AboutSubScreen() {
    val context = LocalContext.current
    var isWebLoading by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 80.dp)
    ) {
        // Embedded WebView for edvanced.ct.ws/about with fallback
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isWebLoading = false
                            }
                        }
                        loadUrl("http://edvanced.ct.ws/about")
                    }
                }
            )

            if (isWebLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D0F15)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CyanPrimary)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Loading About Page...", color = Color.White, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
