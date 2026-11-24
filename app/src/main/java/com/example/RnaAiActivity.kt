package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.tv.KeyRemapperBridge
import com.example.ui.tv.RemappedKeyActionType
import com.example.ui.tv.RnaAgentGlowAndHudOverlay
import com.example.ui.tv.RnaAiTvAppScreen
import com.example.ui.tv.RnaAutonomousAgentController
import com.example.ui.tv.TvAppRamCache
import com.example.ui.tv.UnifiedLauncherApp
import com.example.ui.tv.loadRemappedKeyBindings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Standalone full-screen Activity for the "RNA AI" TV app (powered by EdVanced 1.1 Pro).
 * Running as its own Activity isolates the window and D-Pad focus hierarchy completely so the
 * Home screen underneath never steals focus when navigating to the chat input or microphone button.
 */
class RnaAiActivity : ComponentActivity() {

    companion object {
        const val EXTRA_INITIAL_TAB = "extra_initial_tab"
    }

    private var initialTabState by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        initialTabState = intent?.getIntExtra(EXTRA_INITIAL_TAB, 0) ?: 0

        val prefs = getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)

        setContent {
            val scope = rememberCoroutineScope()
            var installedApps by remember { mutableStateOf<List<UnifiedLauncherApp>>(emptyList()) }
            val isAgentModeActive by RnaAutonomousAgentController.isAgentModeActive.collectAsState()
            val performanceMode = remember { prefs.getBoolean("performance_mode_enabled", false) }

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    val pm = packageManager
                    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                    val leanbackIntent = Intent(Intent.ACTION_MAIN, null).apply {
                        addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
                    }
                    val resolved = (pm.queryIntentActivities(mainIntent, 0) +
                        pm.queryIntentActivities(leanbackIntent, 0))
                        .distinctBy { it.activityInfo.packageName }
                        .filter { it.activityInfo.packageName != packageName }

                    installedApps = resolved.mapNotNull { info ->
                        try {
                            val pkg = info.activityInfo.packageName
                            val label = info.loadLabel(pm).toString()
                            val icon = info.loadIcon(pm)
                            val bmp = TvAppRamCache.iconBitmaps[pkg] ?: runCatching {
                                icon.toBitmap(96, 96).asImageBitmap().also {
                                    TvAppRamCache.iconBitmaps[pkg] = it
                                }
                            }.getOrNull()
                            UnifiedLauncherApp(
                                uniqueKey = "pkg_$pkg",
                                title = label,
                                packageName = pkg,
                                isRnaWebApp = false,
                                rnaApp = null,
                                customLogoUri = null,
                                nativeIcon = icon,
                                cachedBitmap = bmp,
                                isFavorite = false,
                                favoriteOrder = 0,
                                allAppsOrder = 0,
                                lastUsedTimestamp = 0L
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
            }

            MyApplicationTheme(darkTheme = true) {
                Box(modifier = Modifier.fillMaxSize()) {
                    RnaAiTvAppScreen(
                        prefs = prefs,
                        allApps = installedApps,
                        initialTab = initialTabState,
                        onLaunchAppByPackageOrName = { targetPkgOrName ->
                            launchTargetApp(targetPkgOrName, installedApps)
                        },
                        onOpenUrlInRnaBrowser = { targetUrl ->
                            openUrlInRnaBrowser(targetUrl)
                        },
                        onLaunchLiveVoiceAssistant = {
                            KeyRemapperBridge.ensureOverlayPermissionOnFirstLiveUse(this@RnaAiActivity, prefs)
                            val liveIntent = Intent(this@RnaAiActivity, RnaAssistantOverlayActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            }
                            startActivity(liveIntent)
                        },
                        onStartAutonomousAgentWithGoal = { goal ->
                            KeyRemapperBridge.ensureOverlayPermissionOnFirstLiveUse(this@RnaAiActivity, prefs)
                            RnaAutonomousAgentController.onLaunchAppCallback = { pkg ->
                                launchTargetApp(pkg, installedApps)
                            }
                            RnaAutonomousAgentController.onOpenUrlInBrowserCallback = { url ->
                                openUrlInRnaBrowser(url)
                            }
                            RnaAutonomousAgentController.enterAgentMode()
                            if (goal.isNotBlank()) {
                                RnaAutonomousAgentController.submitUserInstruction(goal)
                            }
                        },
                        onExitApp = {
                            finish()
                        }
                    )

                    if (isAgentModeActive) {
                        RnaAgentGlowAndHudOverlay(
                            performanceMode = performanceMode
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        initialTabState = intent.getIntExtra(EXTRA_INITIAL_TAB, 0)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val prefs = getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
        val bindings = loadRemappedKeyBindings(prefs)
        val matched = bindings.firstOrNull { it.enabled && it.keyCode == event.keyCode }
        if (matched != null && matched.actionType == RemappedKeyActionType.RNA_HOME) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                navigateToRnaHome()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun navigateToRnaHome() {
        val homeIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_OPEN_TV_LAUNCHER, true)
            putExtra(MainActivity.EXTRA_RESET_TO_HOME_TAB, true)
        }
        startActivity(homeIntent)
        finish()
    }

    private fun openUrlInRnaBrowser(targetUrl: String) {
        val prefs = getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("pending_external_browser_url", targetUrl).apply()
        val browserIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_OPEN_TV_LAUNCHER, true)
            putExtra(MainActivity.EXTRA_OPEN_TV_BROWSER_URL, targetUrl)
        }
        startActivity(browserIntent)
        finish()
    }

    private fun launchTargetApp(
        targetPkgOrName: String,
        apps: List<UnifiedLauncherApp>
    ) {
        val trimmed = targetPkgOrName.trim()
        if (RnaAutonomousAgentController.isExternalBrowserNameOrPackage(trimmed)) {
            openUrlInRnaBrowser("https://www.google.com")
            return
        }
        val matched = apps.firstOrNull {
            it.packageName.equals(trimmed, ignoreCase = true)
        } ?: apps.firstOrNull {
            it.title.equals(trimmed, ignoreCase = true) ||
                it.title.contains(trimmed, ignoreCase = true) ||
                trimmed.contains(it.title, ignoreCase = true)
        }
        val pkgToLaunch = matched?.packageName ?: trimmed
        if (RnaAutonomousAgentController.isExternalBrowserNameOrPackage(pkgToLaunch)) {
            openUrlInRnaBrowser("https://www.google.com")
            return
        }
        val pm: PackageManager = packageManager
        val launchIntent = pm.getLeanbackLaunchIntentForPackage(pkgToLaunch)
            ?: pm.getLaunchIntentForPackage(pkgToLaunch)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
            finish()
        } else {
            Toast.makeText(
                this,
                "App \"$trimmed\" not installed — opening in RNA Browser",
                Toast.LENGTH_SHORT
            ).show()
            val searchUrl = "https://www.google.com/search?q=${Uri.encode(trimmed)}"
            openUrlInRnaBrowser(searchUrl)
        }
    }
}
