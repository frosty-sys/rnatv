package com.example

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.tv.TvAppRamCache
import com.example.ui.tv.TvVoiceAssistantOverlay
import com.example.ui.tv.UnifiedLauncherApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Translucent, standalone singleInstance overlay Activity (`taskAffinity=""`) that renders ONLY
 * the bottom RNA Voice Assistant bar directly on top of whatever app the user is currently watching
 * (e.g., YouTube, Netflix, etc.) without bringing the RNA Home screen (`MainActivity`) to the front.
 * Pressing Back simply finishes this overlay Activity, leaving the user right inside YouTube!
 */
class RnaAssistantOverlayActivity : ComponentActivity() {

    companion object {
        const val EXTRA_INITIAL_WAKE_QUERY = "extra_initial_wake_query"
        @Volatile
        var isOverlayCurrentlyActive: Boolean = false
    }

    private var activityStartElapsedMs: Long = 0L
    private var initialWakeQueryState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isOverlayCurrentlyActive = true
        activityStartElapsedMs = SystemClock.elapsedRealtime()

        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0.35f)

        initialWakeQueryState = intent?.getStringExtra(EXTRA_INITIAL_WAKE_QUERY)

        val prefs = getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)

        setContent {
            var ttsEnabled by remember {
                mutableStateOf(prefs.getBoolean("ai_tts_enabled", true))
            }
            var overlayApps by remember {
                mutableStateOf<List<UnifiedLauncherApp>>(emptyList())
            }

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

                    overlayApps = resolved.mapNotNull { info ->
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

            val isAgentModeActive by com.example.ui.tv.RnaAutonomousAgentController.isAgentModeActive.collectAsState()
            var showVoiceSheet by remember { mutableStateOf(true) }
            val performanceMode = remember { prefs.getBoolean("performance_mode_enabled", false) }

            LaunchedEffect(isAgentModeActive) {
                if (isAgentModeActive) {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                } else if (!showVoiceSheet) {
                    closeOverlaySilently()
                }
            }

            MyApplicationTheme(darkTheme = true) {
                if (isAgentModeActive) {
                    com.example.ui.tv.RnaAgentGlowAndHudOverlay(
                        performanceMode = performanceMode
                    )
                } else if (showVoiceSheet) {
                    TvVoiceAssistantOverlay(
                        visible = true,
                        initialWakeQuery = initialWakeQueryState,
                        allApps = overlayApps,
                        ttsEnabled = ttsEnabled,
                        onToggleTts = { enabled ->
                            ttsEnabled = enabled
                            prefs.edit().putBoolean("ai_tts_enabled", enabled).apply()
                        },
                        onLaunchAppByPackageOrName = { targetPkgOrName ->
                            launchTargetAppFromOverlay(targetPkgOrName, overlayApps)
                            closeOverlaySilently()
                        },
                        onOpenUrlInBrowser = { targetUrl ->
                            openUrlInRnaBrowserFromOverlay(targetUrl)
                            closeOverlaySilently()
                        },
                        onDismiss = {
                            closeOverlaySilently()
                        },
                        onEnterAgentMode = {
                            showVoiceSheet = false
                            com.example.ui.tv.RnaAutonomousAgentController.onLaunchAppCallback = { pkg ->
                                launchTargetAppFromOverlay(pkg, overlayApps)
                                closeOverlaySilently()
                            }
                            com.example.ui.tv.RnaAutonomousAgentController.onOpenUrlInBrowserCallback = { url ->
                                openUrlInRnaBrowserFromOverlay(url)
                                closeOverlaySilently()
                            }
                            // If Accessibility Service is active, its TYPE_ACCESSIBILITY_OVERLAY renders
                            // the persistent non-blocking Agent glow and controls over any external app,
                            // so finish this Activity so D-Pad keys reach the underlying app directly!
                            if (RnaHomeInterceptorService.isAccessibilityServiceEnabled(this@RnaAssistantOverlayActivity)) {
                                closeOverlaySilently()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        activityStartElapsedMs = SystemClock.elapsedRealtime()
        initialWakeQueryState = intent.getStringExtra(EXTRA_INITIAL_WAKE_QUERY)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            val elapsed = SystemClock.elapsedRealtime() - activityStartElapsedMs
            if (elapsed < 650L) {
                return true
            }
            if (event.action == KeyEvent.ACTION_UP) {
                closeOverlaySilently()
                return true
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun openUrlInRnaBrowserFromOverlay(targetUrl: String) {
        val prefs = getSharedPreferences(RnaHomeInterceptorService.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("pending_external_browser_url", targetUrl).apply()
        val browserIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_OPEN_TV_LAUNCHER, true)
            putExtra(MainActivity.EXTRA_OPEN_TV_BROWSER_URL, targetUrl)
        }
        startActivity(browserIntent)
    }

    private fun launchTargetAppFromOverlay(
        targetPkgOrName: String,
        apps: List<UnifiedLauncherApp>
    ) {
        val trimmed = targetPkgOrName.trim()
        // Never launch an external browser app — always route browser requests to RNA Browser!
        if (com.example.ui.tv.RnaAutonomousAgentController.isExternalBrowserNameOrPackage(trimmed)) {
            openUrlInRnaBrowserFromOverlay("https://www.google.com")
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
        if (com.example.ui.tv.RnaAutonomousAgentController.isExternalBrowserNameOrPackage(pkgToLaunch)) {
            openUrlInRnaBrowserFromOverlay("https://www.google.com")
            return
        }
        val pm: PackageManager = packageManager
        val launchIntent = pm.getLeanbackLaunchIntentForPackage(pkgToLaunch)
            ?: pm.getLaunchIntentForPackage(pkgToLaunch)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        } else {
            Toast.makeText(
                this,
                "App \"$trimmed\" not installed — opening in RNA Browser",
                Toast.LENGTH_SHORT
            ).show()
            val searchUrl = "https://www.google.com/search?q=${Uri.encode(trimmed)}"
            openUrlInRnaBrowserFromOverlay(searchUrl)
        }
    }

    private fun closeOverlaySilently() {
        isOverlayCurrentlyActive = false
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun onDestroy() {
        isOverlayCurrentlyActive = false
        super.onDestroy()
    }
}
