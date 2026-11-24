package com.example.ui.tabs

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import com.example.data.model.RnaApp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.VioletSecondary
import com.example.ui.tv.formatRelativeUsageTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class HomeRecentAppItem(
    val uniqueKey: String,
    val title: String,
    val packageName: String,
    val isRnaWebApp: Boolean,
    val rnaApp: RnaApp? = null,
    val iconUri: String? = null,
    val bitmap: ImageBitmap? = null,
    val lastUsedTimestamp: Long = 0L
)

@Composable
fun HomeScreen(
    installedApps: List<RnaApp>,
    showRecentAppsRow: Boolean = true,
    recentAppTimestamps: Map<String, Long> = emptyMap(),
    onRecordAppLaunched: (packageName: String, rnaAppId: String?) -> Unit = { _, _ -> },
    onCreateClick: () -> Unit,
    onImportRna: (Uri) -> Unit,
    onLaunchApp: (RnaApp) -> Unit,
    onOpenTvLauncherMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { onImportRna(it) }
    }

    val activeInstalled = installedApps.filter { it.isInstalled }
    var showRemoteControllerModal by remember { mutableStateOf(false) }

    // Load native installed apps for the Recent Apps row on HomeScreen as well
    var nativeAppItems by remember { mutableStateOf<List<HomeRecentAppItem>>(emptyList()) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val usageMap = try {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                val now = System.currentTimeMillis()
                usm?.queryAndAggregateUsageStats(now - 7L * 24 * 3600 * 1000L, now)
                    ?.mapValues { it.value.lastTimeUsed }
                    ?: emptyMap()
            } catch (_: Exception) {
                emptyMap()
            }

            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolved = pm.queryIntentActivities(mainIntent, 0)
                .distinctBy { it.activityInfo.packageName }
                .filter { it.activityInfo.packageName != context.packageName }

            nativeAppItems = resolved.mapNotNull { info ->
                try {
                    val pkg = info.activityInfo.packageName
                    val label = info.loadLabel(pm).toString()
                    val icon: Drawable = info.loadIcon(pm)
                    val bmp = try {
                        icon.toBitmap(112, 112).asImageBitmap()
                    } catch (_: Exception) {
                        null
                    }
                    HomeRecentAppItem(
                        uniqueKey = "pkg_$pkg",
                        title = label,
                        packageName = pkg,
                        isRnaWebApp = false,
                        rnaApp = null,
                        iconUri = null,
                        bitmap = bmp,
                        lastUsedTimestamp = usageMap[pkg] ?: 0L
                    )
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    val recentItems = remember(activeInstalled, nativeAppItems, recentAppTimestamps) {
        val merged = mutableListOf<HomeRecentAppItem>()
        activeInstalled.forEach { rna ->
            val ts = recentAppTimestamps[rna.packageName] ?: rna.lastLaunchedAt
            merged.add(
                HomeRecentAppItem(
                    uniqueKey = "rna_${rna.id}",
                    title = rna.title,
                    packageName = rna.packageName,
                    isRnaWebApp = true,
                    rnaApp = rna,
                    iconUri = rna.iconBase64 ?: rna.bannerBase64,
                    bitmap = null,
                    lastUsedTimestamp = ts
                )
            )
        }
        nativeAppItems.forEach { nativeItem ->
            val ts = recentAppTimestamps[nativeItem.packageName] ?: nativeItem.lastUsedTimestamp
            merged.add(nativeItem.copy(lastUsedTimestamp = ts))
        }
        val explicitlyUsed = merged.filter { it.lastUsedTimestamp > 0L }.sortedByDescending { it.lastUsedTimestamp }
        if (explicitlyUsed.isNotEmpty()) {
            explicitlyUsed.take(8)
        } else {
            merged.take(5)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_screen_list"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                HomeHeroSection(
                    onCreateClick = onCreateClick,
                    onImportClick = {
                        filePickerLauncher.launch(arrayOf("*/*"))
                    },
                    onOpenTvLauncherMode = onOpenTvLauncherMode
                )
            }

            // Recent Apps Row (controlled by RNA Settings toggle)
            item {
                AnimatedVisibility(
                    visible = showRecentAppsRow && recentItems.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "RECENT APPS",
                                    color = Color(0xFFA0B0C8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Text(
                                text = "Quick Resume",
                                color = CyanPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("home_recent_apps_row")
                        ) {
                            items(recentItems, key = { it.uniqueKey }) { item ->
                                HomeRecentAppChip(
                                    item = item,
                                    onClick = {
                                        onRecordAppLaunched(item.packageName, item.rnaApp?.id)
                                        if (item.isRnaWebApp && item.rnaApp != null) {
                                            onLaunchApp(item.rnaApp)
                                        } else {
                                            val pm = context.packageManager
                                            val intent = pm.getLaunchIntentForPackage(item.packageName)
                                                ?: pm.getLeanbackLaunchIntentForPackage(item.packageName)
                                            if (intent != null) {
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(intent)
                                            } else {
                                                Toast.makeText(context, "Cannot launch ${item.title}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INSTALLED RNA. APPLICATIONS",
                        color = Color(0xFFA0B0C8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${activeInstalled.size} Installed",
                        color = CyanPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (activeInstalled.isEmpty()) {
                item {
                    EmptyAppsCard(onCreateClick = onCreateClick)
                }
            } else {
                items(activeInstalled, key = { it.id }) { app ->
                    InstalledAppCard(
                        app = app,
                        onLaunch = {
                            onRecordAppLaunched(app.packageName, app.id)
                            onLaunchApp(app)
                        }
                    )
                }
            }
        }

        // Prominent Remote Button + Create Action Button positioned at the BOTTOM-LEFT
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 24.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Remote Button right above the Create button
            FloatingActionButton(
                onClick = { showRemoteControllerModal = true },
                containerColor = Color(0xFF1D2338),
                contentColor = CyanPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .size(56.dp)
                    .border(1.5.dp, CyanPrimary.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                    .testTag("home_remote_controller_fab")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsRemote,
                        contentDescription = "RNA TV Remote & Xbox Controller",
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Remote",
                        color = CyanPrimary,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Create Button
            FloatingActionButton(
                onClick = onCreateClick,
                containerColor = CyanPrimary,
                contentColor = Color(0xFF1B2654),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .size(68.dp)
                    .testTag("home_create_app_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create New App",
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        if (showRemoteControllerModal) {
            RnaPhoneControllerModal(
                onDismiss = { showRemoteControllerModal = false }
            )
        }
    }
}

@Composable
private fun HomeRecentAppChip(
    item: HomeRecentAppItem,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF171A26),
        border = BorderStroke(1.dp, Color(0xFF283046)),
        modifier = Modifier
            .width(196.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF22273B),
                border = BorderStroke(1.dp, if (item.isRnaWebApp) CyanPrimary else Color(0x44FFFFFF)),
                modifier = Modifier.size(40.dp)
            ) {
                when {
                    !item.iconUri.isNullOrBlank() -> {
                        AsyncImage(
                            model = item.iconUri,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                    item.bitmap != null -> {
                        Image(
                            bitmap = item.bitmap,
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                    else -> {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = item.title.take(2).uppercase(),
                                color = CyanPrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${if (item.isRnaWebApp) "RNA." else "App"} • ${formatRelativeUsageTime(item.lastUsedTimestamp)}",
                    color = Color(0xFF8697B2),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HomeHeroSection(
    onCreateClick: () -> Unit,
    onImportClick: () -> Unit,
    onOpenTvLauncherMode: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191B24)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF191B24),
                            Color(0xFF222534)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_tv_banner),
                    contentDescription = "RNA. Banner",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(125.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "RNA. Native & Web App Runner",
                    color = CyanPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hybrid Android & Ad-Free RNA. TV Container",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Package websites, HTML, TypeScript & Vite folders into standalone apps with circular logos, D-Pad spatial focus, and Virtual Mouse + Edge Scroll.",
                    color = Color(0xFFC6C5D0),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onCreateClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyanPrimary,
                            contentColor = Color(0xFF1B2654)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_hero_create_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Create App",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = onImportClick,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282A37)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("home_import_rna_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Import .rna",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onOpenTvLauncherMode,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_tv_launcher_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        tint = VioletSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enter RNA. TV Launcher Mode",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun InstalledAppCard(
    app: RnaApp,
    onLaunch: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) CyanPrimary else Color(0xFF202638),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onLaunch() }
            .testTag("installed_app_card_${app.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFocused) Color(0xFF1B2132) else Color(0xFF131622)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular App Logo (Google TV style)
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1D2336),
                    border = BorderStroke(2.dp, CyanPrimary.copy(alpha = 0.65f)),
                    modifier = Modifier.size(54.dp)
                ) {
                    if (!app.iconBase64.isNullOrBlank()) {
                        AsyncImage(
                            model = app.iconBase64,
                            contentDescription = app.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (app.sourceType) {
                                    "URL" -> Icons.Default.Android
                                    "CODE" -> Icons.Default.Widgets
                                    else -> Icons.Default.FolderOpen
                                },
                                contentDescription = null,
                                tint = CyanPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${app.packageName} • v${app.version}",
                        color = Color(0xFF8697B2),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (app.assignedSlot != null) {
                            BadgePill(text = "Slot ${app.assignedSlot}", color = EmeraldSuccess)
                        } else if (app.isPinnedShortcut) {
                            BadgePill(text = "Pinned", color = CyanPrimary)
                        }

                        if (app.isTvEnabled) {
                            BadgePill(text = "TV D-Pad/Mouse", color = VioletSecondary)
                        }

                        if (app.forceDarkMode) {
                            BadgePill(text = "Dark Force", color = AmberAccent)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = onLaunch,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary),
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("launch_app_button_${app.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Launch",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgePill(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun EmptyAppsCard(onCreateClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141724))
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Widgets,
                contentDescription = null,
                tint = Color(0xFF43516E),
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "No RNA. Web Apps Installed",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Click the Create button at the bottom-left to build your first web app or import a .rna bundle.",
                color = Color(0xFFA0B0C8),
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary)
            ) {
                Text("Create First App", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
