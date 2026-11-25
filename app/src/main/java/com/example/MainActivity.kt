package com.example

import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.RnaApp
import com.example.ui.MainViewModel
import com.example.ui.tabs.HomeScreen
import com.example.ui.tabs.ManageScreen
import com.example.ui.tabs.MoreScreen
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.tv.GoogleTvLauncherScreen
import com.example.ui.tv.KEY_SPLASH_SCREEN_ENABLED
import com.example.ui.tv.TvPerformanceMode
import com.example.ui.tv.TvProfilePersistenceStore
import com.example.ui.tv.getMainTvProfileWallpaperUri
import com.example.ui.tv.loadTvProfiles
import com.example.ui.wizard.AppCreationWizardContent
import com.example.ui.wizard.AppCreationWizardDialog
import com.example.util.configureAppCheck
import java.io.File
import java.lang.ref.WeakReference
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_TV_LAUNCHER = "extra_open_tv_launcher"
        const val EXTRA_OPEN_TV_BROWSER_URL = "extra_open_tv_browser_url"
        const val EXTRA_RESET_TO_HOME_TAB = "extra_reset_to_home_tab"
        const val EXTRA_BOOT_WARMUP_RETURN_TO_PREVIOUS = "extra_boot_warmup_return_to_previous"
        const val EXTRA_BOOT_WARMUP_RETURN_PACKAGE = "extra_boot_warmup_return_package"

        @Volatile
        internal var hasShownInitialWallpaperSplashInProcess: Boolean = false

        @Volatile
        private var activeInstanceRef: WeakReference<MainActivity>? = null

        val activeInstance: MainActivity?
            get() = activeInstanceRef?.get()?.takeIf { !it.isFinishing && !it.isDestroyed }
    }

    private val viewModel: MainViewModel by viewModels()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingBootWarmupReturnRunnable: Runnable? = null
    private var skipInitialSplashForBootWarmup: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        activeInstanceRef = WeakReference(this)
        skipInitialSplashForBootWarmup =
            intent?.getBooleanExtra(EXTRA_BOOT_WARMUP_RETURN_TO_PREVIOUS, false) == true
        TvProfilePersistenceStore.ensureProfilesSyncedOnBootOrUpdate(this)
        configureAndroidSplashScreenApi()
        super.onCreate(savedInstanceState)
        // Fool Android TV so the stock system screensaver never interrupts RNA. TV or its 4K Ambient Mode
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        handleIncomingIntent(intent)

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                MainAppScreen(
                    viewModel = viewModel,
                    skipInitialSplash = skipInitialSplashForBootWarmup,
                    onLaunchApp = { app ->
                        launchAppContainer(app)
                    },
                    onLaunchDirectUrl = { url, title ->
                        launchDirectUrlContainer(url, title)
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        cancelPendingBootWarmupReturn()
        if (activeInstanceRef?.get() === this) {
            activeInstanceRef = null
        }
        super.onDestroy()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (pendingBootWarmupReturnRunnable != null && event.action == KeyEvent.ACTION_DOWN) {
            cancelPendingBootWarmupReturn()
        }
        return super.dispatchKeyEvent(event)
    }

    /**
     * Integrates the Android 12+ platform SplashScreen API (android.window.SplashScreen) so
     * the system window splash screen hands off smoothly into the Main Profile Wallpaper Splash Screen
     * (or dismisses immediately if disabled in Control Panel).
     */
    private fun configureAndroidSplashScreenApi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val prefs = getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
                val splashEnabled = prefs.getBoolean(KEY_SPLASH_SCREEN_ENABLED, true)
                val perfMode = prefs.getBoolean("performance_mode_enabled", false)
                splashScreen.setOnExitAnimationListener { splashScreenView ->
                    if (!splashEnabled || perfMode || skipInitialSplashForBootWarmup) {
                        splashScreenView.remove()
                    } else {
                        val fadeOut = ObjectAnimator.ofFloat(splashScreenView, View.ALPHA, 1f, 0f).apply {
                            duration = 220L
                            addListener(object : android.animation.AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: android.animation.Animator) {
                                    splashScreenView.remove()
                                }
                            })
                        }
                        fadeOut.start()
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun cancelPendingBootWarmupReturn() {
        pendingBootWarmupReturnRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingBootWarmupReturnRunnable = null
    }

    private fun scheduleBootWarmupReturn(targetPkg: String) {
        cancelPendingBootWarmupReturn()
        val prefs = getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, MODE_PRIVATE)
        if (!prefs.getBoolean("tv_first_run_setup_completed_v1", false)) return
        if (!RnaHomeInterceptorService.isEligiblePreviousAppPackage(this, targetPkg)) return

        val runnable = Runnable {
            pendingBootWarmupReturnRunnable = null
            if (!isFinishing && !isDestroyed) {
                com.example.ui.tv.RnaTriggersExecutor.completeBootWarmupReturnToPrevious(
                    activity = this@MainActivity,
                    targetPackage = targetPkg
                )
            }
        }
        pendingBootWarmupReturnRunnable = runnable
        window.decorView.post {
            if (pendingBootWarmupReturnRunnable === runnable) {
                runnable.run()
            }
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val isBootWarmupReturn = intent.getBooleanExtra(EXTRA_BOOT_WARMUP_RETURN_TO_PREVIOUS, false)
        if (isBootWarmupReturn) {
            viewModel.setTvLauncherMode(true)
            val returnPkg = intent.getStringExtra(EXTRA_BOOT_WARMUP_RETURN_PACKAGE).orEmpty()
            scheduleBootWarmupReturn(returnPkg)
        } else {
            cancelPendingBootWarmupReturn()
        }
        val browserUrl = intent.getStringExtra(EXTRA_OPEN_TV_BROWSER_URL)
        if (!browserUrl.isNullOrBlank()) {
            getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString("pending_external_browser_url", browserUrl)
                .apply()
            viewModel.setTvLauncherMode(true)
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_TV_LAUNCHER, false) ||
            intent.categories?.contains(Intent.CATEGORY_HOME) == true ||
            intent.categories?.contains(Intent.CATEGORY_LEANBACK_LAUNCHER) == true
        ) {
            viewModel.setTvLauncherMode(true)
        }
        if (intent.getBooleanExtra(EXTRA_RESET_TO_HOME_TAB, false)) {
            viewModel.setTvLauncherMode(true)
            com.example.ui.tv.KeyRemapperBridge.notifyNavigateToHomeTabInMainActivity()
        }
        intent.data?.let { uri ->
            val scheme = uri.scheme?.lowercase()
            if (scheme == "http" || scheme == "https") {
                getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putString("pending_external_browser_url", uri.toString())
                    .apply()
                viewModel.setTvLauncherMode(true)
            } else {
                viewModel.handleImportedRnaFile(uri)
            }
        }
    }

    private fun launchAppContainer(app: RnaApp) {
        viewModel.recordAppLaunched(app.packageName, app.id)
        val intent = Intent(this, WebContainerActivity::class.java).apply {
            putExtra(WebContainerActivity.EXTRA_APP_ID, app.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        }
        startActivity(intent)
    }

    private fun launchDirectUrlContainer(url: String, title: String) {
        val intent = Intent(this, WebContainerActivity::class.java).apply {
            putExtra(WebContainerActivity.EXTRA_DIRECT_URL, url)
            putExtra(WebContainerActivity.EXTRA_DIRECT_TITLE, title)
            putExtra(WebContainerActivity.EXTRA_BROWSER_MODE, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        }
        startActivity(intent)
    }
}

@Composable
fun MainAppScreen(
    viewModel: MainViewModel,
    skipInitialSplash: Boolean = false,
    onLaunchApp: (RnaApp) -> Unit,
    onLaunchDirectUrl: (String, String) -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val tvCustomizations by viewModel.tvCustomizations.collectAsState()
    val isTvLauncherMode by viewModel.isTvLauncherMode.collectAsState()
    val showCreationWizard by viewModel.showCreationWizard.collectAsState()
    val pendingInstallerApp by viewModel.pendingInstallerApp.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val showRecentAppsRow by viewModel.showRecentAppsRow.collectAsState()
    val singleRowHomeApps by viewModel.singleRowHomeApps.collectAsState()
    val recentAppTimestamps by viewModel.recentAppTimestamps.collectAsState()

    var installerProgressState by remember { mutableStateOf("PROMPT") }
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Android Splash Screen API Component State: displays Main profile's wallpaper during initial app loading
    var showInitialWallpaperSplash by remember {
        val splashEnabled = prefs.getBoolean(KEY_SPLASH_SCREEN_ENABLED, true)
        val shouldShow = splashEnabled && !skipInitialSplash && !MainActivity.hasShownInitialWallpaperSplashInProcess
        if (shouldShow || skipInitialSplash) {
            MainActivity.hasShownInitialWallpaperSplashInProcess = true
        }
        mutableStateOf(shouldShow)
    }

    LaunchedEffect(showInitialWallpaperSplash) {
        if (showInitialWallpaperSplash) {
            val waitMs = if (TvPerformanceMode.isEnabled) 450L else 1150L
            delay(waitMs)
            showInitialWallpaperSplash = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isTvLauncherMode) {
            // Ad-Free Google TV Launcher Mode
            GoogleTvLauncherScreen(
                rnaApps = installedApps,
                tvCustomizations = tvCustomizations,
                showRecentAppsRow = showRecentAppsRow,
                onToggleRecentAppsRow = { enabled -> viewModel.setShowRecentAppsRow(enabled) },
                singleRowHomeApps = singleRowHomeApps,
                onToggleSingleRowHomeApps = { single -> viewModel.setSingleRowHomeApps(single) },
                recentAppTimestamps = recentAppTimestamps,
                onRecordAppLaunched = { pkg, rnaId -> viewModel.recordAppLaunched(pkg, rnaId) },
                onRemoveRecentApp = { pkg -> viewModel.removeRecentApp(pkg) },
                onClearRecentApps = { viewModel.clearRecentApps() },
                onLaunchRnaApp = onLaunchApp,
                onLaunchDirectUrl = onLaunchDirectUrl,
                onInstallRnaUri = { uri -> viewModel.handleImportedRnaFile(uri) },
                onUpdateRnaApp = { updated -> viewModel.updateAppDirectly(updated) },
                onUninstallRnaApp = { appToRemove -> viewModel.uninstallApp(appToRemove) },
                onExportRnaApp = { appToExport -> viewModel.exportApp(appToExport) },
                onSaveTvCustomization = { entity -> viewModel.saveTvCustomization(entity) },
                onOpenRnaStudio = { tab ->
                    viewModel.selectTab(tab)
                    viewModel.setTvLauncherMode(false)
                },
                onOpenCreateWizard = {
                    viewModel.openCreationWizard()
                },
                onConvertWebsiteToRnaApp = { convertedApp ->
                    installerProgressState = "PROMPT"
                    viewModel.openInstallerWithApp(convertedApp)
                }
            )
        } else {
            // rna. Studio / Manager Mode (Supports Sidebar on TV/Wide screens & BottomBar on Phones)
            BackHandler(enabled = selectedTab != 0) {
                viewModel.selectTab(0)
            }
            BackHandler(enabled = com.example.ui.tv.ControlPanelReturnState.returnToFunctionsFromStudio) {
                viewModel.setTvLauncherMode(true)
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useSidebar = maxWidth >= 600.dp

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        RnaTopBar(
                            isDarkTheme = isDarkTheme,
                            onToggleDark = { viewModel.toggleDarkTheme() },
                            onEnterTvLauncher = { viewModel.setTvLauncherMode(true) }
                        )
                    },
                    bottomBar = {
                        if (!useSidebar) {
                            RnaBottomNavigationBar(
                                selectedTab = selectedTab,
                                onTabSelect = { viewModel.selectTab(it) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useSidebar) {
                            RnaSidebarNavigationRail(
                                selectedTab = selectedTab,
                                onTabSelect = { viewModel.selectTab(it) },
                                onEnterTvLauncher = { viewModel.setTvLauncherMode(true) }
                            )
                        }

                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            AnimatedContent(
                                targetState = selectedTab,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "TabContentTransition"
                            ) { tab ->
                                when (tab) {
                                    0 -> HomeScreen(
                                        installedApps = installedApps,
                                        showRecentAppsRow = showRecentAppsRow,
                                        recentAppTimestamps = recentAppTimestamps,
                                        onRecordAppLaunched = { pkg, rnaId -> viewModel.recordAppLaunched(pkg, rnaId) },
                                        onCreateClick = { viewModel.openCreationWizard() },
                                        onImportRna = { uri -> viewModel.handleImportedRnaFile(uri) },
                                        onLaunchApp = onLaunchApp,
                                        onOpenTvLauncherMode = { viewModel.setTvLauncherMode(true) }
                                    )
                                    1 -> Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.TopCenter
                                    ) {
                                        AppCreationWizardContent(
                                            isModal = false,
                                            onSaveOnly = { app ->
                                                viewModel.saveAppToManageOnly(app)
                                            },
                                            onAppConfigured = { app, _, _ ->
                                                installerProgressState = "PROMPT"
                                                viewModel.openInstallerWithApp(app)
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    2 -> ManageScreen(
                                        installedApps = installedApps,
                                        onInstallClick = { app ->
                                            installerProgressState = "PROMPT"
                                            viewModel.openInstallerWithApp(app)
                                        },
                                        onUpdateApp = { app, oldSlot, enableSlot ->
                                            viewModel.updateApp(app, oldSlot, enableSlot)
                                        },
                                        onExportApp = { app -> viewModel.exportApp(app) },
                                        onUninstallApp = { app -> viewModel.uninstallApp(app) }
                                    )
                                    3 -> MoreScreen(
                                        installedApps = installedApps,
                                        isDarkTheme = isDarkTheme,
                                        onToggleDarkTheme = { viewModel.toggleDarkTheme() },
                                        showRecentAppsRow = showRecentAppsRow,
                                        onToggleRecentAppsRow = { enabled -> viewModel.setShowRecentAppsRow(enabled) },
                                        onClearRecentApps = { viewModel.clearRecentApps() },
                                        onInstallRnaUri = { uri -> viewModel.handleImportedRnaFile(uri) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // App Creation Wizard Modal Dialog (Accessible from Home FAB or Google TV Launcher)
        if (showCreationWizard) {
            AppCreationWizardDialog(
                onDismiss = { viewModel.closeCreationWizard() },
                onSaveOnly = { app ->
                    viewModel.saveAppToManageOnly(app)
                },
                onAppConfigured = { app, _, _ ->
                    viewModel.closeCreationWizard()
                    installerProgressState = "PROMPT"
                    viewModel.openInstallerWithApp(app)
                }
            )
        }

        // Mock Android Package Installer Modal Dialog
        pendingInstallerApp?.let { app ->
            var installedResult by remember(app.id) { mutableStateOf<RnaApp?>(null) }
            Dialog(
                onDismissRequest = {
                    viewModel.closeInstaller()
                    installerProgressState = "PROMPT"
                },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xCC080B12)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        PackageInstallerCard(
                            app = installedResult ?: app,
                            installState = installerProgressState,
                            onCancel = {
                                viewModel.closeInstaller()
                                installerProgressState = "PROMPT"
                            },
                            onInstall = {
                                installerProgressState = "INSTALLING"
                                viewModel.installApp(
                                    app = app,
                                    useSlotAlias = true,
                                    pinShortcut = true
                                ) { installed ->
                                    installedResult = installed
                                    installerProgressState = "COMPLETE"
                                }
                            },
                            onOpen = {
                                val toLaunch = installedResult ?: app
                                viewModel.closeInstaller()
                                installerProgressState = "PROMPT"
                                onLaunchApp(toLaunch)
                            },
                            onDone = {
                                viewModel.closeInstaller()
                                installerProgressState = "PROMPT"
                            }
                        )
                    }
                }
            }
        }

        // Android Splash Screen API Overlay: Displays the Wallpaper of the Main Profile during initial app loading
        AnimatedVisibility(
            visible = showInitialWallpaperSplash,
            enter = fadeIn(animationSpec = tween(if (TvPerformanceMode.isEnabled) 0 else 150)),
            exit = if (TvPerformanceMode.isEnabled) {
                fadeOut(animationSpec = tween(0))
            } else {
                fadeOut(animationSpec = tween(480, easing = FastOutSlowInEasing)) +
                    scaleOut(targetScale = 1.04f, animationSpec = tween(480, easing = FastOutSlowInEasing))
            }
        ) {
            RnaMainProfileSplashScreen(
                prefs = prefs
            )
        }
    }
}

/**
 * Android Splash Screen API Component that displays the Wallpaper of the Main Profile
 * during initial app loading. Can be turned ON or OFF in the Control Panel (ON by default).
 */
@Composable
fun RnaMainProfileSplashScreen(
    prefs: android.content.SharedPreferences
) {
    val context = LocalContext.current
    val mainProfile = remember {
        val all = loadTvProfiles(prefs)
        all.firstOrNull {
            it.id == "default" || it.id == "main" || it.name.equals("Main", ignoreCase = true)
        } ?: all.firstOrNull()
    }
    val mainWallpaperUri = remember {
        getMainTvProfileWallpaperUri(prefs)
    }

    // Synchronous Frame-1 decode for bundled asset wallpapers or local files so there is zero blank flash
    val predecodedBitmap: ImageBitmap? = remember(mainWallpaperUri) {
        try {
            when {
                mainWallpaperUri.startsWith("file:///android_asset/") -> {
                    val assetPath = mainWallpaperUri.removePrefix("file:///android_asset/")
                    context.assets.open(assetPath).use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }
                mainWallpaperUri.startsWith("file://") -> {
                    val path = Uri.parse(mainWallpaperUri).path
                    if (!path.isNullOrBlank() && File(path).exists()) {
                        BitmapFactory.decodeFile(path)?.asImageBitmap()
                    } else {
                        null
                    }
                }
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    var zoomStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        zoomStarted = true
    }
    val backdropScale by animateFloatAsState(
        targetValue = if (zoomStarted && !TvPerformanceMode.isEnabled) 1.0f else 1.04f,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "SplashWallpaperZoom"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0C14))
            .testTag("rna_main_profile_splash_screen")
    ) {
        if (predecodedBitmap != null) {
            Image(
                bitmap = predecodedBitmap,
                contentDescription = "Main Profile Wallpaper Splash",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = backdropScale
                        scaleY = backdropScale
                    }
            )
        } else {
            AsyncImage(
                model = mainWallpaperUri,
                contentDescription = "Main Profile Wallpaper Splash",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = backdropScale
                        scaleY = backdropScale
                    }
            )
        }

        // Subtle cinematic vignette so the Main Profile's wallpaper shines while keeping branding legible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0x44080A12),
                        0.55f to Color(0x22080A12),
                        1.0f to Color(0xCC080A12)
                    )
                )
        )

        // Bottom-center glassmorphic splash pill showing "Loading RNA. TV" with the RNA icon only
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xD9121624),
            border = BorderStroke(1.2.dp, Color(0x48FFFFFF)),
            shadowElevation = 20.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1A2035),
                    border = BorderStroke(1.5.dp, CyanPrimary),
                    modifier = Modifier.size(40.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground_img),
                        contentDescription = "RNA. TV",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                Text(
                    text = "Loading RNA. TV",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    letterSpacing = 0.6.sp
                )

                Spacer(modifier = Modifier.width(6.dp))

                CircularProgressIndicator(
                    color = CyanPrimary,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun RnaTopBar(
    isDarkTheme: Boolean,
    onToggleDark: () -> Unit,
    onEnterTvLauncher: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Circular RNA. Logo & Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(38.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground_img),
                        contentDescription = "RNA. Logo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "RNA.",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        letterSpacing = 0.4.sp
                    )
                    Text(
                        text = "Run Native/Web Apps • M3 Expressive",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Switch to RNA. TV Launcher Mode button
                IconButton(
                    onClick = onEnterTvLauncher,
                    modifier = Modifier.testTag("topbar_tv_launcher_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = "RNA. TV Launcher Mode",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Dark Mode Theme Toggle
                IconButton(
                    onClick = onToggleDark,
                    modifier = Modifier.testTag("topbar_theme_toggle")
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Toggle Theme",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun RnaSidebarNavigationRail(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    onEnterTvLauncher: () -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxHeight()
            .testTag("tv_sidebar_navigation_rail")
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        NavigationRailItem(
            selected = selectedTab == 0,
            onClick = { onTabSelect(0) },
            icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontWeight = FontWeight.Bold) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        NavigationRailItem(
            selected = selectedTab == 1,
            onClick = { onTabSelect(1) },
            icon = { Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Create") },
            label = { Text("Create", fontWeight = FontWeight.Bold) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        NavigationRailItem(
            selected = selectedTab == 2,
            onClick = { onTabSelect(2) },
            icon = { Icon(imageVector = Icons.Default.Tune, contentDescription = "Manage") },
            label = { Text("Manage", fontWeight = FontWeight.Bold) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        NavigationRailItem(
            selected = selectedTab == 3,
            onClick = { onTabSelect(3) },
            icon = { Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = "More") },
            label = { Text("More", fontWeight = FontWeight.Bold) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
        Spacer(modifier = Modifier.weight(1f))
        NavigationRailItem(
            selected = false,
            onClick = onEnterTvLauncher,
            icon = { Icon(imageVector = Icons.Default.Tv, contentDescription = "RNA. TV") },
            label = { Text("RNA. TV", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun RnaBottomNavigationBar(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .testTag("main_navigation_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelect(0) },
            icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_home")
        )

        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelect(1) },
            icon = { Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Create") },
            label = { Text("Create", fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_create")
        )

        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelect(2) },
            icon = { Icon(imageVector = Icons.Default.Tune, contentDescription = "Manage") },
            label = { Text("Manage", fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_manage")
        )

        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelect(3) },
            icon = { Icon(imageVector = Icons.Default.MoreHoriz, contentDescription = "More") },
            label = { Text("More", fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_more")
        )
    }
}
